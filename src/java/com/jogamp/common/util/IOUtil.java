/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import jogamp.common.Debug;
import jogamp.common.os.AndroidUtils;
import jogamp.common.os.PlatformPropsImpl;

import com.jogamp.common.net.AssetURLContext;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.MachineDescription;
import com.jogamp.common.os.Platform;

public class IOUtil {
    public static final boolean DEBUG = Debug.debug("IOUtil");

    /** {@value} */
    public static final String SCHEME_SEPARATOR = ":";
    /** {@value} */
    public static final String FILE_SCHEME = "file";
    /** {@value} */
    public static final String HTTP_SCHEME = "http";
    /** {@value} */
    public static final String HTTPS_SCHEME = "https";
    /** {@value} */
    public static final String JAR_SCHEME = "jar";
    /** A JAR subprotocol is separeted from the JAR entry w/ this separator {@value}. Even if no class is specified '!/' must follow!. */
    public static final String JAR_SCHEME_SEPARATOR = "!";

    /** Std. temporary directory property key <code>java.io.tmpdir</code>. */
    private static final String java_io_tmpdir_propkey = "java.io.tmpdir";
    private static final String user_home_propkey = "user.home";
    private static final String XDG_CACHE_HOME_envkey = "XDG_CACHE_HOME";

    /** Subdirectory within platform's temporary root directory where all JogAmp related temp files are being stored: {@code jogamp} */
    public static final String tmpSubDir = "jogamp";

    private IOUtil() {}

    /**
     * Since usage of {@link java.io.FileOutputStream} is considered security critical,
     * we need to check it's availability for each use.
     * <p>
     * In case a SecurityManager is installed, privileged access is required.
     * </p>
     *
     * @return the constructor of {@link java.io.FileOutputStream} if available and
     *         no SecurityManager is installed or privileged access is granted.
     *         Otherwise null.
     *
     */
    private static final Constructor<?> getFOSCtor() {
        Constructor<?> _fosCtor;
        Throwable _t;
        try {
            _fosCtor = ReflectionUtil.getConstructor("java.io.FileOutputStream", new Class<?>[] { File.class }, IOUtil.class.getClassLoader());
            _t = null;
        } catch (Throwable t) {
            _fosCtor = null;
            _t = t;
        }
        if(DEBUG) {
            System.err.println("IOUtil: java.io.FileOutputStream available: "+(null != _fosCtor));
            if(null!=_t) {
                _t.printStackTrace();
            }
        }
        return _fosCtor;
    }

    /***
     *
     * STREAM COPY STUFF
     *
     */

    /**
     * Copy the specified URL resource to the specified output file. The total
     * number of bytes written is returned. Both streams are closed upon completion.
     *
     * @param conn the open URLConnection
     * @param outFile the destination
     * @return
     * @throws IOException
     */
    public static int copyURLConn2File(URLConnection conn, File outFile) throws IOException {
        conn.connect();  // redundant

        int totalNumBytes = 0;
        InputStream in = new BufferedInputStream(conn.getInputStream());
        try {
            totalNumBytes = copyStream2File(in, outFile, conn.getContentLength());
        } finally {
            in.close();
        }
        return totalNumBytes;
    }

    /**
     * Copy the specified input stream to the specified output file. The total
     * number of bytes written is returned. Both streams are closed upon completion.
     *
     * @param in the source
     * @param outFile the destination
     * @param totalNumBytes informal number of expected bytes, maybe used for user feedback while processing. -1 if unknown
     * @return
     * @throws IOException
     */
    public static int copyStream2File(InputStream in, File outFile, int totalNumBytes) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
        try {
            totalNumBytes = copyStream2Stream(in, out, totalNumBytes);
        } finally {
            out.close();
        }
        return totalNumBytes;
    }

    /**
     * Copy the specified input stream to the specified output stream. The total
     * number of bytes written is returned.
     *
     * @param in the source
     * @param out the destination
     * @param totalNumBytes informal number of expected bytes, maybe used for user feedback while processing. -1 if unknown
     * @return
     * @throws IOException
     */
    public static int copyStream2Stream(InputStream in, OutputStream out, int totalNumBytes) throws IOException {
        return copyStream2Stream(Platform.getMachineDescription().pageSizeInBytes(), in, out, totalNumBytes);
    }

    /**
     * Copy the specified input stream to the specified output stream. The total
     * number of bytes written is returned.
     *
     * @param bufferSize the intermediate buffer size, should be {@link MachineDescription#pageSizeInBytes()} for best performance.
     * @param in the source
     * @param out the destination
     * @param totalNumBytes informal number of expected bytes, maybe used for user feedback while processing. -1 if unknown
     * @return
     * @throws IOException
     */
    public static int copyStream2Stream(int bufferSize, InputStream in, OutputStream out, int totalNumBytes) throws IOException {
        final byte[] buf = new byte[bufferSize];
        int numBytes = 0;
        while (true) {
            int count;
            if ((count = in.read(buf)) == -1) {
                break;
            }
            out.write(buf, 0, count);
            numBytes += count;
        }
        return numBytes;
    }

    /**
     * Copy the specified input stream to a byte array, which is being returned.
     */
    public static byte[] copyStream2ByteArray(InputStream stream) throws IOException {
        // FIXME: Shall enforce a BufferedInputStream ?
        if( !(stream instanceof BufferedInputStream) ) {
            stream = new BufferedInputStream(stream);
        }
        int totalRead = 0;
        int avail = stream.available();
        byte[] data = new byte[avail];
        int numRead = 0;
        do {
            if (totalRead + avail > data.length) {
                final byte[] newData = new byte[totalRead + avail];
                System.arraycopy(data, 0, newData, 0, totalRead);
                data = newData;
            }
            numRead = stream.read(data, totalRead, avail);
            if (numRead >= 0) {
                totalRead += numRead;
            }
            avail = stream.available();
        } while (avail > 0 && numRead >= 0);

        // just in case the announced avail > totalRead
        if (totalRead != data.length) {
            final byte[] newData = new byte[totalRead];
            System.arraycopy(data, 0, newData, 0, totalRead);
            data = newData;
        }
        return data;
    }

    /**
     * Copy the specified input stream to a NIO ByteBuffer w/ native byte order, which is being returned.
     * <p>The implementation creates the ByteBuffer w/ {@link #copyStream2ByteArray(InputStream)}'s returned byte array.</p>
     *
     * @param stream input stream, which will be wrapped into a BufferedInputStream, if not already done.
     */
    public static ByteBuffer copyStream2ByteBuffer(InputStream stream) throws IOException {
        return copyStream2ByteBuffer(stream, -1);
    }

    /**
     * Copy the specified input stream to a NIO ByteBuffer w/ native byte order, which is being returned.
     * <p>The implementation creates the ByteBuffer w/ {@link #copyStream2ByteArray(InputStream)}'s returned byte array.</p>
     *
     * @param stream input stream, which will be wrapped into a BufferedInputStream, if not already done.
     * @param initialCapacity initial buffer capacity in bytes, if &gt; available bytes
     */
    public static ByteBuffer copyStream2ByteBuffer(InputStream stream, int initialCapacity) throws IOException {
        if( !(stream instanceof BufferedInputStream) ) {
            stream = new BufferedInputStream(stream);
        }
        int avail = stream.available();
        if( initialCapacity < avail ) {
            initialCapacity = avail;
        }
        final MachineDescription machine = Platform.getMachineDescription();
        ByteBuffer data = Buffers.newDirectByteBuffer( machine.pageAlignedSize( initialCapacity ) );
        byte[] chunk = new byte[machine.pageSizeInBytes()];
        int chunk2Read = Math.min(machine.pageSizeInBytes(), avail);
        int numRead = 0;
        do {
            if (avail > data.remaining()) {
                final ByteBuffer newData = Buffers.newDirectByteBuffer(
                                               machine.pageAlignedSize(data.position() + avail) );
                newData.put(data);
                data = newData;
            }

            numRead = stream.read(chunk, 0, chunk2Read);
            if (numRead > 0) {
                data.put(chunk, 0, numRead);
            }
            avail = stream.available();
            chunk2Read = Math.min(machine.pageSizeInBytes(), avail);
        } while ( numRead > 0 ); // EOS: -1 == numRead, EOF maybe reached earlier w/ 0 == numRead

        data.flip();
        return data;
    }

    /***
     *
     * RESOURCE / FILE NAME STUFF
     *
     */

    /**
     *
     * @param path
     * @param startWithSlash
     * @param endWithSlash
     * @return
     * @throws URISyntaxException if path is empty or has no parent directory available while resolving <code>../</code>
     */
    public static String slashify(String path, boolean startWithSlash, boolean endWithSlash) throws URISyntaxException {
        String p = path.replace('\\', '/'); // unify file separator
        if (startWithSlash && !p.startsWith("/")) {
            p = "/" + p;
        }
        if (endWithSlash && !p.endsWith("/")) {
            p = p + "/";
        }
        return cleanPathString(p);
    }

    /**
     * Using the simple conversion via File -> URI, assuming proper characters.
     * @throws URISyntaxException if path is empty or has no parent directory available while resolving <code>../</code>
     * @throws URISyntaxException if the resulting string does not comply w/ an RFC 2396 URI
     */
    public static URI toURISimple(File file) throws URISyntaxException {
        return new URI(FILE_SCHEME, null, encodeToURI(slashify(file.getAbsolutePath(), true, file.isDirectory())), null);
    }

    /**
     * Using the simple conversion via File -> URI, assuming proper characters.
     * @throws URISyntaxException if path is empty or has no parent directory available while resolving <code>../</code>
     * @throws URISyntaxException if the resulting string does not comply w/ an RFC 2396 URI
     */
    public static URI toURISimple(String protocol, String file, boolean isDirectory) throws URISyntaxException {
        return new URI(protocol, null, encodeToURI(slashify(file, true, isDirectory)), null);
    }

    /**
     * Returns the lowercase suffix of the given file name (the text
     * after the last '.' in the file name). Returns null if the file
     * name has no suffix. Only operates on the given file name;
     * performs no I/O operations.
     *
     * @param file name of the file
     * @return lowercase suffix of the file name
     * @throws NullPointerException if file is null
     */

    public static String getFileSuffix(File file) {
        return getFileSuffix(file.getName());
    }

    /**
     * Returns the lowercase suffix of the given file name (the text
     * after the last '.' in the file name). Returns null if the file
     * name has no suffix. Only operates on the given file name;
     * performs no I/O operations.
     *
     * @param filename name of the file
     * @return lowercase suffix of the file name
     * @throws NullPointerException if filename is null
     */
    public static String getFileSuffix(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) {
            return null;
        }
        return toLowerCase(filename.substring(lastDot + 1));
    }
    private static String toLowerCase(String arg) {
        if (arg == null) {
            return null;
        }

        return arg.toLowerCase();
    }

    /***
     * @param file
     * @param allowOverwrite
     * @return outputStream The resulting output stream
     * @throws IOException if the file already exists and <code>allowOverwrite</code> is false,
     *                     the class {@link java.io.FileOutputStream} is not accessible or
     *                     the user does not have sufficient rights to access the local filesystem.
     */
    public static FileOutputStream getFileOutputStream(File file, boolean allowOverwrite) throws IOException {
        final Constructor<?> fosCtor = getFOSCtor();
        if(null == fosCtor) {
            throw new IOException("Cannot open file (" + file + ") for writing, FileOutputStream feature not available.");
        }
        if (file.exists() && !allowOverwrite) {
            throw new IOException("File already exists (" + file + ") and overwrite=false");
        }
        try {
            return (FileOutputStream) fosCtor.newInstance(new Object[] { file });
        } catch (Exception e) {
            throw new IOException("error opening " + file + " for write. ", e);
        }
    }

    public static String getClassFileName(String clazzBinName) {
        // or return clazzBinName.replace('.', File.separatorChar) + ".class"; ?
        return clazzBinName.replace('.', '/') + ".class";
    }

    /**
     * @param clazzBinName com.jogamp.common.util.cache.TempJarCache
     * @param cl ClassLoader to locate the JarFile
     * @return jar:file:/usr/local/projects/JOGL/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/util/cache/TempJarCache.class
     * @throws IOException if the jar file could not been found by the ClassLoader
     */
    public static URL getClassURL(String clazzBinName, ClassLoader cl) throws IOException {
        final URL url = cl.getResource(getClassFileName(clazzBinName));
        if(null == url) {
            throw new IOException("Cannot not find: "+clazzBinName);
        }
        return url;
    }

    /**
     * Returns the basename of the given fname w/o directory part
     * @throws URISyntaxException if path is empty or has no parent directory available while resolving <code>../</code>
     */
    public static String getBasename(String fname) throws URISyntaxException {
        fname = slashify(fname, false, false);
        int lios = fname.lastIndexOf('/');  // strip off dirname
        if(lios>=0) {
            fname = fname.substring(lios+1);
        }
        return fname;
    }

    /**
     * Returns unified '/' dirname including the last '/'
     * @throws URISyntaxException if path is empty or has no parent directory available while resolving <code>../</code>
     */
    public static String getDirname(String fname) throws URISyntaxException {
        fname = slashify(fname, false, false);
        int lios = fname.lastIndexOf('/');  // strip off dirname
        if(lios>=0) {
            fname = fname.substring(0, lios+1);
        }
        return fname;
    }

    /**
     * The URI's <code><i>protocol</i>:/some/path/gluegen-rt.jar</code>
     * parent dirname URI <code><i>protocol</i>:/some/path/</code> will be returned.
     * <p>
     * <i>protocol</i> may be "file", "http", etc..
     * </p>
     *
     * @param uri "<i>protocol</i>:/some/path/gluegen-rt.jar"
     * @return "<i>protocol</i>:/some/path/"
     * @throws IllegalArgumentException if the URI doesn't match the expected formatting, or is null
     * @throws URISyntaxException
     */
    public static URI getURIDirname(URI uri) throws IllegalArgumentException, URISyntaxException {
        if(null == uri) {
            throw new IllegalArgumentException("URI is null");
        }
        String uriS = uri.toString();
        if( DEBUG ) {
            System.out.println("getURIDirname "+uri+", extForm: "+uriS);
        }
        return new URI( getURIDirname(uriS) );
    }

    /**
     * The URI's <code><i>protocol</i>:/some/path/gluegen-rt.jar</code>
     * parent dirname URI <code><i>protocol</i>:/some/path/</code> will be returned.
     * <p>
     * <i>protocol</i> may be "file", "http", etc..
     * </p>
     *
     * @param uri "<i>protocol</i>:/some/path/gluegen-rt.jar" (URI encoded)
     * @return "<i>protocol</i>:/some/path/"
     * @throws IllegalArgumentException if the URI doesn't match the expected formatting, or is null
     * @throws URISyntaxException
     */
    public static String getURIDirname(String uriS) throws IllegalArgumentException, URISyntaxException {
        if(null == uriS) {
            throw new IllegalArgumentException("uriS is null");
        }
        // from
        //   file:/some/path/gluegen-rt.jar  _or_ rsrc:gluegen-rt.jar
        // to
        //   file:/some/path/                _or_ rsrc:
        int idx = uriS.lastIndexOf('/');
        if(0 > idx) {
            // no abs-path, check for protocol terminator ':'
            idx = uriS.lastIndexOf(':');
            if(0 > idx) {
                throw new IllegalArgumentException("URI does not contain protocol terminator ':', in <"+uriS+">");
            }
        }
        uriS = uriS.substring(0, idx+1); // exclude jar name, include terminal '/' or ':'

        if( DEBUG ) {
            System.out.println("getJarURIDirname res: "+uriS);
        }
        return uriS;
    }

    /**
     * Converts an {@link URI} to an {@link URL} while using a non encoded path
     * for <i>file scheme</i>, i.e. <code>file:/</code>.
     * Otherwise the default {@link URL} translation {@link URI#toURL()} is being used.
     * <p>
     * The following cases are considered:
     * <ul>
     *   <li><i>file schema</i> is converted via <code>new File(uri).getPath()</code>.</li>
     *   <li><i>jar scheme</i>
     *   <ul>
     *     <li>sub-protocol is being converted as above, if <i>file scheme</i>.</li>
     *     <li>JAR entry is not converted but preserved.</li>
     *   </ul></li>
     * </ul>
     * </p>
     * <p>
     * Tested w/ unit test <code>com.jogamp.common.util.TestIOUtilURIHandling</code>
     * </p>
     * @param uri
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws URISyntaxException
     */
    public static URL toURL(final URI uri) throws IOException, IllegalArgumentException, URISyntaxException {
        URL url = null;
        final String uriSchema = uri.getScheme();
        final boolean isJar = IOUtil.JAR_SCHEME.equals(uriSchema);
        final URI specificURI = isJar ? JarUtil.getJarSubURI(uri) : uri;
        final boolean hasJarSubURI = specificURI != uri;
        if( DEBUG ) {
            System.out.println("IOUtil.toURL.0: isJAR "+isJar+", hasSubURI "+hasJarSubURI+Platform.getNewline()+
                               "\t, uri "+uri+Platform.getNewline()+
                               "\t -> "+specificURI.toString());
        }
        int mode = 0;
        if( IOUtil.FILE_SCHEME.equals( specificURI.getScheme() ) ) {
            File f;
            try {
                f = new File(specificURI);
            } catch(Exception iae) {
                if( DEBUG ) {
                    System.out.println("Catched "+iae.getClass().getSimpleName()+": new File("+specificURI+") failed: "+iae.getMessage());
                    iae.printStackTrace();
                }
                f = null;
            }
            if( null != f ) {
                String urlS = null;
                try {
                    final String fPath = f.getPath();
                    final String fPathUriS = encodeFilePathToURI(fPath);
                    /**
                     * Below 'url = f.toURI().toURL()' Doesn't work, since it uses encoded path,
                     * but we need the decoded path due to subsequent file access.
                     *   URI:       jar:file:/C:/gluegen/build-x86_64%20%c3%b6%c3%a4%20lala/gluegen-rt.jar!/
                     *   File:      file:/C:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar
                     *   URI:  fUri file:/C:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar
                     *   URL:  fUrl file:/C:/gluegen/build-x86_64%20öä%20lala/gluegen-rt.jar
                     *
                     * Goal:        file:/C:/gluegen/build-x86_64 öä lala/gluegen-rt.jar!/
                     */
                    if(DEBUG) {
                        try {
                            final URI fUri = f.toURI();
                            final URL fUrl = fUri.toURL();
                            System.out.println("IOUtil.toURL.1b: fUri "+fUri+Platform.getNewline()+
                                               "\t, fUrl "+fUrl);
                        } catch (Exception ee) {
                            System.out.println("Catched "+ee.getClass().getSimpleName()+": f.toURI().toURL() failed: "+ee.getMessage());
                            ee.printStackTrace();
                        }
                    }
                    if( !hasJarSubURI ) {
                        urlS = IOUtil.FILE_SCHEME+IOUtil.SCHEME_SEPARATOR+fPathUriS;
                        if( DEBUG ) {
                            System.out.println("IOUtil.toURL.1: fPath "+fPath+Platform.getNewline()+
                                               "\t -> "+fPathUriS+Platform.getNewline()+
                                               "\t -> "+urlS);
                        }
                        url = new URL(urlS);
                        mode = 1;
                    } else {
                        final String jarEntry = JarUtil.getJarEntry(uri);
                        final String post = isJar ? IOUtil.JAR_SCHEME_SEPARATOR + jarEntry : "";
                        urlS = uriSchema+IOUtil.SCHEME_SEPARATOR+IOUtil.FILE_SCHEME+IOUtil.SCHEME_SEPARATOR+fPathUriS+post;
                        if( DEBUG ) {
                            System.out.println("IOUtil.toURL.2: fPath "+fPath+Platform.getNewline()+
                                               "\t -> "+fPathUriS+Platform.getNewline()+
                                               "\t, jarEntry "+jarEntry+Platform.getNewline()+
                                               "\t, post "+post+Platform.getNewline()+
                                               "\t -> "+urlS);
                        }
                        url = new URL(urlS);
                        mode = 2;
                    }
                } catch (Exception mue) {
                    if( DEBUG ) {
                        System.out.println("Catched "+mue.getClass().getSimpleName()+": new URL("+urlS+") failed: "+mue.getMessage());
                        mue.printStackTrace();
                    }
                }
            }
        }
        if( null == url ) {
            try {
                url = uri.toURL();
                mode = 3;
            } catch (Exception e) {
                if( DEBUG ) {
                    System.out.println("Catched "+e.getClass().getSimpleName()+": "+uri+".toURL() failed: "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        if( DEBUG ) {
            System.err.println("IOUtil.toURL.X: mode "+mode+", "+uri+Platform.getNewline()+
                               "\t -> "+url);
        }
        return url;
    }

    /***
     *
     * RESOURCE LOCATION STUFF
     *
     */

    /**
     * Locating a resource using {@link #getResource(String, ClassLoader)}:
     * <ul>
     *   <li><i>relative</i>: <code>context</code>'s package name-path plus <code>resourcePath</code> via <code>context</code>'s ClassLoader.
     *       This allows locations relative to JAR- and other URLs.
     *       The <code>resourcePath</code> may start with <code>../</code> to navigate to parent folder.</li>
     *   <li><i>absolute</i>: <code>context</code>'s ClassLoader and the <code>resourcePath</code> as is (filesystem)</li>
     * </ul>
     *
     * <p>
     * Returns the resolved and open URLConnection or null if not found.
     * </p>
     *
     * @see #getResource(String, ClassLoader)
     * @see ClassLoader#getResource(String)
     * @see ClassLoader#getSystemResource(String)
     */
    public static URLConnection getResource(Class<?> context, String resourcePath) {
        if(null == resourcePath) {
            return null;
        }
        ClassLoader contextCL = (null!=context)?context.getClassLoader():IOUtil.class.getClassLoader();
        URLConnection conn = null;
        if(null != context) {
            // scoping the path within the class's package
            final String className = context.getName().replace('.', '/');
            final int lastSlash = className.lastIndexOf('/');
            if (lastSlash >= 0) {
                final String pkgName = className.substring(0, lastSlash + 1);
                conn = getResource(pkgName + resourcePath, contextCL);
                if(DEBUG) {
                    System.err.println("IOUtil: found <"+resourcePath+"> within class package <"+pkgName+"> of given class <"+context.getName()+">: "+(null!=conn));
                }
            }
        } else if(DEBUG) {
            System.err.println("IOUtil: null context");
        }
        if(null == conn) {
            conn = getResource(resourcePath, contextCL);
            if(DEBUG) {
                System.err.println("IOUtil: found <"+resourcePath+"> by classloader: "+(null!=conn));
            }
        }
        return conn;
    }

    /**
     * Locating a resource using the ClassLoader's facilities.
     * <p>
     * Returns the resolved and connected URLConnection or null if not found.
     * </p>
     *
     * @see ClassLoader#getResource(String)
     * @see ClassLoader#getSystemResource(String)
     * @see URL#URL(String)
     * @see File#File(String)
     */
    public static URLConnection getResource(String resourcePath, ClassLoader cl) {
        if(null == resourcePath) {
            return null;
        }
        if(DEBUG) {
            System.err.println("IOUtil: locating <"+resourcePath+">, has cl: "+(null!=cl));
        }
        if(resourcePath.startsWith(AssetURLContext.asset_protocol_prefix)) {
            try {
                return AssetURLContext.createURL(resourcePath, cl).openConnection();
            } catch (IOException ioe) {
                if(DEBUG) {
                    System.err.println("IOUtil: Catched Exception:");
                    ioe.printStackTrace();
                }
                return null;
            }
        } else {
            try {
                return AssetURLContext.resolve(resourcePath, cl);
            } catch (IOException ioe) {
                if(DEBUG) {
                    System.err.println("IOUtil: Catched Exception:");
                    ioe.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Generates a path for the 'relativeFile' relative to the 'baseLocation'.
     *
     * @param baseLocation denotes a directory
     * @param relativeFile denotes a relative file to the baseLocation
     * @throws URISyntaxException if path is empty or has no parent directory available while resolving <code>../</code>
     */
    public static String getRelativeOf(File baseLocation, String relativeFile) throws URISyntaxException {
        if(null == relativeFile) {
            return null;
        }

        if (baseLocation != null) {
            final File file = new File(baseLocation, relativeFile);
            // Handle things on Windows
            return slashify(file.getPath(), false, false);
        }
        return null;
    }

    /**
     * @param path assuming a slashified path beginning with "/" as it's root directory, either denotes a file or directory.
     * @return parent of path
     * @throws URISyntaxException if path is empty or has no parent directory available
     */
    public static String getParentOf(String path) throws URISyntaxException {
        final int pl = null!=path ? path.length() : 0;
        if(pl == 0) {
            throw new IllegalArgumentException("path is empty <"+path+">");
        }

        final int e = path.lastIndexOf("/");
        if( e < 0 ) {
            throw new URISyntaxException(path, "path contains no '/'");
        }
        if( e == 0 ) {
            // path is root directory
            throw new URISyntaxException(path, "path has no parents");
        }
        if( e <  pl - 1 ) {
            // path is file, return it's parent directory
            return path.substring(0, e+1);
        }
        final int j = path.lastIndexOf("!") + 1; // '!' Separates JARFile entry -> local start of path
        // path is a directory ..
        final int p = path.lastIndexOf("/", e-1);
        if( p >= j) {
            return path.substring(0, p+1);
        }
        throw new URISyntaxException(path, "parent of path contains no '/'");
    }

    /**
     * @param path assuming a slashified path beginning with "/" as it's root directory, either denotes a file or directory.
     * @return clean path string where <code>../</code> and <code>./</code> is resolved.
     * @throws URISyntaxException if path is empty or has no parent directory available while resolving <code>../</code>
     */
    public static String cleanPathString(String path) throws URISyntaxException {
        int idx;
        while ( ( idx = path.indexOf("../") ) >= 0 ) {
            path = getParentOf(path.substring(0, idx)) + path.substring(idx+3);
        }
        while ( ( idx = path.indexOf("./") ) >= 0 ) {
            path = path.substring(0, idx) + path.substring(idx+2);
        }
        return path;
    }

    /**
     * Generates a URI for the <i>relativePath</i> relative to the <i>baseURI</i>,
     * hence the result is a absolute location.
     * <p>
     * Impl. operates on the <i>scheme-specific-part</i>, and hence is sub-protocol savvy.
     * </p>
     * <p>
     * In case <i>baseURI</i> is not a path ending w/ '/', it's a assumed to be a file and it's parent is being used.
     * </p>
     *
     * @param baseURI denotes a URI to a directory ending w/ '/', or a file. In the latter case the file's directory is being used.
     * @param relativePath denotes a relative file to the baseLocation's parent directory
     * @throws URISyntaxException if path is empty or has no parent directory available while resolving <code>../</code>
     */
    public static URI getRelativeOf(URI baseURI, String relativePath) throws URISyntaxException {
        return compose(baseURI.getScheme(), baseURI.getSchemeSpecificPart(), encodeToURI(relativePath), baseURI.getFragment());
    }

    /**
     * Wraps {@link #getRelativeOf(URI, String)} for convenience.
     * @throws IOException
     */
    public static URL getRelativeOf(URL baseURL, String relativePath) throws IOException {
        try {
            return getRelativeOf(baseURL.toURI(), relativePath).toURL();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    /**
     * Generates a URI for the <i>relativePath</i> relative to the <i>schemeSpecificPart</i>,
     * hence the result is a absolute location.
     * <p>
     * <i>schemeSpecificPart</i>'s query, if exist is split to <i>path</i> and <i>query</i>.
     * </p>
     * <p>
     * In case <i>path</i> is not a path ending w/ '/', it's a assumed to be a file and it's parent is being used.
     * </p>
     *
     * @param scheme scheme of the resulting URI
     * @param schemeSpecificPart may include a query, which is separated while processing (URI encoded)
     * @param relativePath denotes a relative file to the baseLocation's parent directory (URI encoded)
     * @param fragment the URI fragment (URI encoded)
     * @throws URISyntaxException if path is empty or has no parent directory available while resolving <code>../</code>
     * @see #encodeToURI(String)
     */
    public static URI compose(String scheme, String schemeSpecificPart, String relativePath, String fragment) throws URISyntaxException {
        // cut off optional query in scheme-specific-part
        final String query;
        final int queryI = schemeSpecificPart.lastIndexOf('?');
        if( queryI >= 0 ) {
            query = schemeSpecificPart.substring(queryI+1);
            schemeSpecificPart = schemeSpecificPart.substring(0, queryI);
        } else {
            query = null;
        }
        if( null != relativePath ) {
            if( !schemeSpecificPart.endsWith("/") ) {
                schemeSpecificPart = getParentOf(schemeSpecificPart);
            }
            schemeSpecificPart = schemeSpecificPart + relativePath;
        }
        schemeSpecificPart = cleanPathString( schemeSpecificPart );
        return new URI(scheme, null == query ? schemeSpecificPart : schemeSpecificPart + "?" + query, fragment);
    }

    private static final Pattern patternSpaceRaw = Pattern.compile(" ");
    private static final Pattern patternSpaceEnc = Pattern.compile("%20");

    /**
     * Escapes characters not complying w/ RFC 2396 and the {@link URI#URI(String)} ctor.
     * <ul>
     *   <li>SPACE -> %20</li>
     * </ul>
     */
    public static String encodeToURI(String s) {
        return patternSpaceRaw.matcher(s).replaceAll("%20");
    }

    /**
     * Reverses escaping of characters as performed via {@link #encodeToURI(String)}.
     */
    public static String decodeFromURI(String s) {
        return patternSpaceEnc.matcher(s).replaceAll(" ");
    }

    private static final Pattern patternSingleBS = Pattern.compile("\\\\{1,}");

    /**
     * Escapes file path characters not complying w/ RFC 2396 and the {@link URI#URI(String)} ctor.
     * <p>
     * Processes input filePath if {@link File#separatorChar} <code> != '/'</code>
     * as follows:
     * <ul>
     *   <li>backslash -> slash</li>
     *   <li>ensure starting with slash</li>
     * </ul>
     * </p>
     * <p>
     * Even though Oracle's JarURLStreamHandler can handle backslashes and
     * erroneous URIs w/ e.g. Windows file 'syntax', other may not (Netbeans).<br>
     * See Bug 857 - http://jogamp.org/bugzilla/show_bug.cgi?id=857
     * </p>
     */
    public static String encodeFilePathToURI(String filePath) {
        if( !File.separator.equals("/") ) {
            final String r = patternSingleBS.matcher(filePath).replaceAll("/");
            if( !r.startsWith("/") ) {
                return "/" + r;
            } else {
                return r;
            }
        }
        return filePath;
    }

    /**
     * Returns the connected URLConnection, or null if not url is not available
     */
    public static URLConnection openURL(URL url) {
        return openURL(url, ".");
    }

    /**
     * Returns the connected URLConnection, or null if not url is not available
     */
    public static URLConnection openURL(URL url, String dbgmsg) {
        if(null!=url) {
            try {
                final URLConnection c = url.openConnection();
                c.connect(); // redundant
                if(DEBUG) {
                    System.err.println("IOUtil: urlExists("+url+") ["+dbgmsg+"] - true");
                }
                return c;
            } catch (IOException ioe) {
                if(DEBUG) {
                    System.err.println("IOUtil: urlExists("+url+") ["+dbgmsg+"] - false - "+ioe.getClass().getSimpleName()+": "+ioe.getMessage());
                    ioe.printStackTrace();
                }
            }
        } else if(DEBUG) {
            System.err.println("IOUtil: no url - urlExists(null) ["+dbgmsg+"]");
        }

        return null;
    }

    private static String getShellSuffix() {
        switch(PlatformPropsImpl.OS_TYPE) {
            case WINDOWS:
              return ".bat";
            default:
              return ".sh";
        }
    }

    private static boolean getOSHasNoexecFS() {
        switch(PlatformPropsImpl.OS_TYPE) {
            case WINDOWS:
            case OPENKODE:
              return false;

            default:
              return true;
        }
    }

    /**
     * @see <a href="http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html">Free-Desktop - XDG Base Directory Specification</a>
     */
    private static boolean getOSHasFreeDesktopXDG() {
        switch(PlatformPropsImpl.OS_TYPE) {
            case ANDROID:
            case MACOS:
            case WINDOWS:
            case OPENKODE:
              return false;

            default:
              return true;
        }
    }

    /**
     * Test whether {@code file} exists and matches the given requirements
     *
     * @param file
     * @param shallBeDir
     * @param shallBeWritable
     * @return
     */
    public static boolean testFile(File file, boolean shallBeDir, boolean shallBeWritable) {
        if (!file.exists()) {
            if(DEBUG) {
                System.err.println("IOUtil.testFile: <"+file.getAbsolutePath()+">: does not exist");
            }
            return false;
        }
        if (shallBeDir && !file.isDirectory()) {
            if(DEBUG) {
                System.err.println("IOUtil.testFile: <"+file.getAbsolutePath()+">: is not a directory");
            }
            return false;
        }
        if (shallBeWritable && !file.canWrite()) {
            if(DEBUG) {
                System.err.println("IOUtil.testFile: <"+file.getAbsolutePath()+">: is not writable");
            }
            return false;
        }
        return true;
    }

    /**
     * Returns true if the given {@code dir}
     * <ol>
     *   <li>exists, and</li>
     *   <li>is a directory, and</li>
     *   <li>is writeable, and</li>
     *   <li>files can be executed from the directory</li>
     * </ol>
     *
     * @throws SecurityException if file creation and process execution is not allowed within the current security context
     * @param dir
     */
    public static boolean testDirExec(File dir)
            throws SecurityException
    {
        if (!testFile(dir, true, true)) {
            return false;
        }
        if(!getOSHasNoexecFS()) {
            return true;
        }

        File exetst;
        try {
            exetst = File.createTempFile("jogamp_exe_tst", getShellSuffix(), dir);
        } catch (SecurityException se) {
            throw se; // fwd Security exception
        } catch (IOException e) {
            if(DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
        int ok = -1;
        if(exetst.setExecutable(true)) {
            try {
                Process pr = Runtime.getRuntime().exec(exetst.getCanonicalPath());
                pr.waitFor() ;
                ok = pr.exitValue();
            } catch (SecurityException se) {
                throw se; // fwd Security exception
            } catch (Throwable t) {
                ok = -2;
                if(DEBUG) {
                    System.err.println("IOUtil.testDirExec: <"+exetst.getAbsolutePath()+">: "+t.getMessage());
                    // t.printStackTrace();
                }
            }
        }
        exetst.delete();
        return 0 == ok;
    }

    private static File testDirImpl(File dir, boolean create, boolean executable)
            throws SecurityException
    {
        if (create && !dir.exists()) {
            dir.mkdirs();
        }
        if( executable ) {
            if(testDirExec(dir)) {
                return dir;
            }
        } else if(testFile(dir, true, true)) {
            return dir;
        }
        return null;
    }

    /**
     * Returns the directory {@code dir}, which is processed and tested as described below.
     * <ol>
     *   <li>If {@code create} is {@code true} and the directory does not exist yet, it is created incl. all sub-directories.</li>
     *   <li>If {@code dirName} exists, but is not a directory, {@code null} is being returned.</li>
     *   <li>If the directory does not exist or is not writeable, {@code null} is being returned.</li>
     *   <li>If {@code executable} is {@code true} and files cannot be executed from the directory, {@code null} is being returned.</li>
     * </ol>
     *
     * @param dir the directory to process
     * @param create true if the directory shall be created if not existing
     * @param executable true if the user intents to launch executables from the temporary directory, otherwise false.
     * @throws SecurityException if file creation and process execution is not allowed within the current security context
     */
    public static File testDir(final File dir, final boolean create, final boolean executable)
        throws SecurityException
    {
        return testDirImpl(dir, create, executable);
    }

    private static boolean isStringSet(String s) { return null != s && 0 < s.length(); }

    /**
     * This methods finds [and creates] an available temporary sub-directory:
     * <pre>
           File tmpBaseDir = null;
           if(null != testDir(tmpRoot, true, executable)) { // check tmpRoot first
               for(int i = 0; null == tmpBaseDir && i<=9999; i++) {
                   final String tmpDirSuffix = String.format("_%04d", i); // 4 digits for iteration
                   tmpBaseDir = testDir(new File(tmpRoot, tmpSubDirPrefix+tmpDirSuffix), true, executable);
               }
           } else {
               tmpBaseDir = null;
           }
           return tmpBaseDir;
     * </pre>
     * <p>
     * The iteration through [0000-9999] ensures that the code is multi-user save.
     * </p>
     * @param tmpRoot
     * @param executable
     * @param tmpDirPrefix
     * @return a temporary directory, writable by this user
     * @throws SecurityException
     */
    private static File getSubTempDir(File tmpRoot, String tmpSubDirPrefix, boolean executable)
        throws SecurityException
    {
       File tmpBaseDir = null;
       if(null != testDirImpl(tmpRoot, true /* create */, executable)) { // check tmpRoot first
           for(int i = 0; null == tmpBaseDir && i<=9999; i++) {
               final String tmpDirSuffix = String.format("_%04d", i); // 4 digits for iteration
               tmpBaseDir = testDirImpl(new File(tmpRoot, tmpSubDirPrefix+tmpDirSuffix), true /* create */, executable);
           }
       }
       return tmpBaseDir;
    }

    /**
     * Returns a platform independent writable directory for temporary files
     * consisting of the platform's {@code temp-root} + {@link #tmpSubDir},
     * e.g. {@code /tmp/jogamp_0000/}.
     * <p>
     * On standard Java the {@code temp-root} folder is specified by <code>java.io.tempdir</code>.
     * </p>
     * <p>
     * On Android the {@code temp-root} folder is relative to the applications local folder
     * (see {@link Context#getDir(String, int)}) is returned, if
     * the Android application/activity has registered it's Application Context
     * via {@link jogamp.common.os.android.StaticContext.StaticContext#init(Context, ClassLoader) StaticContext.init(..)}.
     * This allows using the temp folder w/o the need for <code>sdcard</code>
     * access, which would be the <code>java.io.tempdir</code> location on Android!
     * </p>
     * <p>
     * In case {@code temp-root} is the users home folder,
     * a dot is being prepended to {@link #tmpSubDir}, i.e.: {@code /home/user/.jogamp_0000/}.
     * </p>
     * @param executable true if the user intents to launch executables from the temporary directory, otherwise false.
     * @throws RuntimeException if no temporary directory could be determined
     * @throws SecurityException if access to <code>java.io.tmpdir</code> is not allowed within the current security context
     *
     * @see PropertyAccess#getProperty(String, boolean)
     * @see Context#getDir(String, int)
     */
    public static File getTempDir(final boolean executable)
        throws SecurityException, RuntimeException
    {
        if(!tempRootSet) { // volatile: ok
            synchronized(IOUtil.class) {
                if(!tempRootSet) {
                    tempRootSet = true;
                    {
                        final File ctxTempDir = AndroidUtils.getTempRoot(); // null if ( !Android || no android-ctx )
                        if(null != ctxTempDir) {
                            tempRootNoexec = getSubTempDir(ctxTempDir, tmpSubDir, false /* executable, see below */);
                            tempRootExec = tempRootNoexec; // FIXME: Android temp root is always executable (?)
                            return tempRootExec;
                        }
                    }

                    final String java_io_tmpdir = PropertyAccess.getProperty(java_io_tmpdir_propkey, false);
                    final String user_home = PropertyAccess.getProperty(user_home_propkey, false);

                    final String xdg_cache_home;
                    {
                        String _xdg_cache_home;
                        if( getOSHasFreeDesktopXDG() ) {
                            _xdg_cache_home = System.getenv(XDG_CACHE_HOME_envkey);
                            if( !isStringSet(_xdg_cache_home) && isStringSet(user_home) ) {
                                _xdg_cache_home = user_home + File.separator + ".cache" ; // default
                            }
                        } else {
                            _xdg_cache_home = null;
                        }
                        xdg_cache_home = _xdg_cache_home;
                    }

                    // 1) java.io.tmpdir/jogamp
                    if( null == tempRootExec && isStringSet(java_io_tmpdir) ) {
                        tempRootExec = getSubTempDir(new File(java_io_tmpdir), tmpSubDir, true /* executable */);
                    }

                    // 2) $XDG_CACHE_HOME/jogamp
                    if(null == tempRootExec && isStringSet(xdg_cache_home)) {
                        tempRootExec = getSubTempDir(new File(xdg_cache_home), tmpSubDir, true /* executable */);
                    }

                    // 3) $HOME/.jogamp
                    if(null == tempRootExec && isStringSet(user_home)) {
                        tempRootExec = getSubTempDir(new File(user_home), "." + tmpSubDir, true /* executable */);
                    }


                    if(null != tempRootExec) {
                        tempRootNoexec = tempRootExec;
                    } else {
                        // 1) java.io.tmpdir/jogamp
                        if( null == tempRootNoexec && isStringSet(java_io_tmpdir) ) {
                            tempRootNoexec = getSubTempDir(new File(java_io_tmpdir), tmpSubDir, false /* executable */);
                        }

                        // 2) $XDG_CACHE_HOME/jogamp
                        if(null == tempRootNoexec && isStringSet(xdg_cache_home)) {
                            tempRootNoexec = getSubTempDir(new File(xdg_cache_home), tmpSubDir, false /* executable */);
                        }

                        // 3) $HOME/.jogamp
                        if(null == tempRootNoexec && isStringSet(user_home)) {
                            tempRootNoexec = getSubTempDir(new File(user_home), "." + tmpSubDir, false /* executable */);
                        }
                    }

                    if(DEBUG) {
                        System.err.println("IOUtil.getTempRoot(): temp dirs: exec: "+tempRootExec.getAbsolutePath()+", noexec: "+tempRootNoexec.getAbsolutePath());
                    }
                }
            }
        }
        final File r = executable ? tempRootExec : tempRootNoexec ;
        if(null == r) {
            throw new RuntimeException("Could not determine a temporary directory");
        }
        final FilePermission fp = new FilePermission(r.getAbsolutePath(), "read,write,delete");
        SecurityUtil.checkPermission(fp);
        return r;
    }
    private static File tempRootExec = null; // writeable and executable
    private static File tempRootNoexec = null; // writeable, maybe executable
    private static volatile boolean tempRootSet = false;

    /**
     * Utilizing {@link File#createTempFile(String, String, File)} using
     * {@link #getTempDir(boolean)} as the directory parameter, ie. location
     * of the root temp folder.
     *
     * @see File#createTempFile(String, String)
     * @see File#createTempFile(String, String, File)
     * @see #getTempDir(boolean)
     *
     * @param prefix
     * @param suffix
     * @param executable true if the temporary root folder needs to hold executable files, otherwise false.
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws SecurityException
     */
    public static File createTempFile(String prefix, String suffix, boolean executable)
        throws IllegalArgumentException, IOException, SecurityException
    {
        return File.createTempFile( prefix, suffix, getTempDir(executable) );
    }

    public static void close(Closeable stream, boolean throwRuntimeException) throws RuntimeException {
        if(null != stream) {
            try {
                stream.close();
            } catch (IOException e) {
                if(throwRuntimeException) {
                    throw new RuntimeException(e);
                } else if(DEBUG) {
                    System.err.println("Catched Exception: ");
                    e.printStackTrace();
                }
            }
        }
    }
}
