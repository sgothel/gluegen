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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlContext;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import jogamp.common.Debug;
import jogamp.common.os.android.StaticContext;

import android.content.Context;

import com.jogamp.common.net.AssetURLContext;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.os.MachineDescription;
import com.jogamp.common.os.Platform;

public class IOUtil {
    public static final boolean DEBUG = Debug.debug("IOUtil");
    
    /** Std. temporary directory property key <code>java.io.tmpdir</code> */
    public static final String java_io_tmpdir_propkey = "java.io.tmpdir";

    private static final Constructor<?> fosCtor;
    
    static {
        Constructor<?> _fosCtor;
        try {
            _fosCtor = ReflectionUtil.getConstructor("java.io.FileOutputStream", new Class<?>[] { File.class }, IOUtil.class.getClassLoader());
        } catch (Throwable t) {
            if(DEBUG) { t.printStackTrace(); }
            _fosCtor = null;
        }
        fosCtor = _fosCtor;
    }
    
    private IOUtil() {}
    
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
        final byte[] buf = new byte[Platform.getMachineDescription().pageSizeInBytes()];
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
     */
    public static ByteBuffer copyStream2ByteBuffer(InputStream stream) throws IOException {
        // FIXME: Shall enforce a BufferedInputStream ?
        if( !(stream instanceof BufferedInputStream) ) {
            stream = new BufferedInputStream(stream);
        }
        int avail = stream.available();
        final MachineDescription machine = Platform.getMachineDescription(); 
        ByteBuffer data = Buffers.newDirectByteBuffer( machine.pageAlignedSize(avail) );
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
            if (numRead >= 0) {
                data.put(chunk, 0, numRead);
            }
            avail = stream.available();
            chunk2Read = Math.min(machine.pageSizeInBytes(), avail);
        } while (avail > 0 && numRead >= 0);

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
     * @throws RuntimeException if final path is empty or has no parent directory available while resolving <code>../</code> 
     */
    public static String slashify(String path, boolean startWithSlash, boolean endWithSlash) throws RuntimeException {
        String p = path.replace('\\', '/'); // unify file seperator     
        if (startWithSlash && !p.startsWith("/")) {
            p = "/" + p;
        }
        if (endWithSlash && !p.endsWith("/")) {
            p = p + "/";
        }
        try {
            return cleanPathString(p);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }        
    }
    
    /** Using the proper advertised conversion via File -> URI -> URL */
    public static URL toURL(File file) throws MalformedURLException {
        return file.toURI().toURL();
    }
    
    /** Using the simple conversion via File -> URL, assuming proper characters. */
    public static URL toURLSimple(File file) throws MalformedURLException {
        return new URL("file", "", slashify(file.getAbsolutePath(), true, file.isDirectory()));        
    }
    
    public static URL toURLSimple(String protocol, String file, boolean isDirectory) throws MalformedURLException {
        return new URL(protocol, "", slashify(file, true, isDirectory));        
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

    /***
     * @param file
     * @param allowOverwrite
     * @return outputStream The resulting output stream
     * @throws IOException if the file already exists and <code>allowOverwrite</code> is false, 
     *                     the class <code>java.io.FileOutputStream</code> is not accessible or
     *                     the user does not have sufficient rights to access the local filesystem. 
     */
    public static FileOutputStream getFileOutputStream(File file, boolean allowOverwrite) throws IOException {
        if(null == fosCtor) {
            throw new IOException("Cannot open file (" + file + ") for writing, feature not available.");
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
    
    public static String getClassFileName(String clazzBinName) throws IOException {
        // or return clazzBinName.replace('.', File.pathSeparatorChar) + ".class"; ?            
        return clazzBinName.replace('.', '/') + ".class";            
    }
    
    /**
     * @param clazzBinName com.jogamp.common.util.cache.TempJarCache 
     * @param cl
     * @return jar:file:/usr/local/projects/JOGL/gluegen/build-x86_64/gluegen-rt.jar!/com/jogamp/common/util/cache/TempJarCache.class
     * @throws IOException
     */
    public static URL getClassURL(String clazzBinName, ClassLoader cl) throws IOException {
        return cl.getResource(getClassFileName(clazzBinName));
    }
        
    /**
     * Returns the basename of the given fname w/o directory part
     */
    public static String getBasename(String fname) {
        fname = slashify(fname, false, false);
        int lios = fname.lastIndexOf('/');  // strip off dirname
        if(lios>=0) {
            fname = fname.substring(lios+1);
        }
        return fname;
    }
    
    /**
     * Returns unified '/' dirname including the last '/'
     */
    public static String getDirname(String fname) {
        fname = slashify(fname, false, false);
        int lios = fname.lastIndexOf('/');  // strip off dirname
        if(lios>=0) {
            fname = fname.substring(0, lios+1);
        }
        return fname;
    }
    
    private static String toLowerCase(String arg) {
        if (arg == null) {
            return null;
        }

        return arg.toLowerCase();
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
                conn = getResource(className.substring(0, lastSlash + 1) + resourcePath, contextCL);
            }
            if(DEBUG) {
                System.err.println("IOUtil: found <"+resourcePath+"> within class package: "+(null!=conn));
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
     */
    public static String getRelativeOf(File baseLocation, String relativeFile) {
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
     * @throws MalformedURLException if path is empty or has parent no directory available 
     */
    public static String getParentOf(String path) throws MalformedURLException {
        final int pl = null!=path ? path.length() : 0;
        if(pl == 0) {
            throw new MalformedURLException("path is empty <"+path+">");
        }
        
        final int e = path.lastIndexOf("/");
        if( e < 0 ) {
            throw new MalformedURLException("path contains no '/' <"+path+">");
        }
        if( e == 0 ) {
            // path is root directory
            throw new MalformedURLException("path has no parents <"+path+">");
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
        throw new MalformedURLException("parent of path contains no '/' <"+path+">");
    }
    
    /**
     * @param path assuming a slashified path beginning with "/" as it's root directory, either denotes a file or directory.
     * @return clean path string where <code>../</code> and <code>./</code> is resolved. 
     * @throws MalformedURLException if path is empty or has no parent directory available while resolving <code>../</code>
     */
    public static String cleanPathString(String path) throws MalformedURLException {
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
     * Generates a path for the 'relativeFile' relative to the 'baseLocation',
     * hence the result is a absolute location.
     * 
     * @param baseLocation denotes a URL to a directory if ending w/ '/', otherwise we assume a file
     * @param relativeFile denotes a relative file to the baseLocation's parent directory
     * @throws MalformedURLException 
     */
    public static URL getRelativeOf(URL baseLocation, String relativeFile) throws MalformedURLException {    
        final String scheme = baseLocation.getProtocol();
        final String auth = baseLocation.getAuthority();
        String path = baseLocation.getPath();
        String query = baseLocation.getQuery();
        String fragment = baseLocation.getRef();
        
        if(!path.endsWith("/")) {
            path = getParentOf(path);
        }
        return compose(scheme, auth, path, relativeFile, query, fragment);
    }
    
    public static URL compose(String scheme, String auth, String path1, String path2, String query, String fragment) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        if(null!=scheme) {
            sb.append(scheme);
            sb.append(":");
        }
        if(null!=auth) {
            sb.append("//");
            sb.append(auth);
        }
        if(null!=path1) {
            sb.append(path1);
        }
        if(null!=path2) {
            sb.append(path2);
        }
        if(null!=query) {
            sb.append("?");
            sb.append(query);
        }
        if(null!=fragment) {
            sb.append("#");
            sb.append(fragment);
        }
        return new URL(cleanPathString(sb.toString()));
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
    
    /**
     * Utilizing {@link File#createTempFile(String, String, File)} using
     * {@link #getTempRoot(AccessControlContext)} as the directory parameter, ie. location 
     * of the root temp folder.
     * 
     * @see File#createTempFile(String, String)
     * @see File#createTempFile(String, String, File)
     * @see #getTempRoot(AccessControlContext)
     * 
     * @param prefix
     * @param suffix
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws SecurityException
     */
    public static File createTempFile(String prefix, String suffix, AccessControlContext acc) 
        throws IllegalArgumentException, IOException, SecurityException 
    {        
        return File.createTempFile( prefix, suffix, getTempRoot(acc) );
    }
    
    /**
     * Returns a platform independent writable directory for temporary files. 
     * <p>
     * On standard Java, the folder specified by <code>java.io.tempdir</code>
     * is returned.
     * </p> 
     * <p>
     * On Android a <code>temp</code> folder relative to the applications local folder 
     * (see {@link Context#getDir(String, int)}) is returned, if
     * the Android application/activity has registered it's Application Context
     * via {@link StaticContext#init(Context, ClassLoader)}.
     * This allows using the temp folder w/o the need for <code>sdcard</code>
     * access, which would be the <code>java.io.tempdir</code> location on Android!
     * </p>
     * @param acc The security {@link AccessControlContext} to access <code>java.io.tmpdir</code> 
     * 
     * @throws SecurityException if access to <code>java.io.tmpdir</code> is not allowed within the current security context
     * @throws RuntimeException is the property <code>java.io.tmpdir</code> or the resulting temp directory is invalid
     *  
     * @see PropertyAccess#getProperty(String, boolean, java.security.AccessControlContext)
     * @see StaticContext#init(Context, ClassLoader)
     * @see Context#getDir(String, int)
     */
    public static File getTempRoot(AccessControlContext acc)
        throws SecurityException, RuntimeException
    {
        if(AndroidVersion.isAvailable) {
            final Context ctx = StaticContext.getContext();
            if(null != ctx) {
                final File tmpRoot = ctx.getDir("temp", Context.MODE_WORLD_READABLE);
                if(null==tmpRoot|| !tmpRoot.isDirectory() || !tmpRoot.canWrite()) {
                    throw new RuntimeException("Not a writable directory: '"+tmpRoot+"', retrieved Android static context");
                }
                if(DEBUG) {
                    System.err.println("IOUtil.getTempRoot(Android): temp dir: "+tmpRoot.getAbsolutePath());
                }
                return tmpRoot;
            }
        }
        final String tmpRootName = PropertyAccess.getProperty(java_io_tmpdir_propkey, false, acc);
        if(null == tmpRootName || 0 == tmpRootName.length()) {
            throw new RuntimeException("Property '"+java_io_tmpdir_propkey+"' value is empty: <"+tmpRootName+">");
        }
        final File tmpRoot = new File(tmpRootName);
        if(null==tmpRoot || !tmpRoot.isDirectory() || !tmpRoot.canWrite()) {
            throw new RuntimeException("Not a writable directory: '"+tmpRoot+"', retrieved by propery '"+java_io_tmpdir_propkey+"'");
        }
        if(DEBUG) {
            System.err.println("IOUtil.getTempRoot(isAndroid: "+AndroidVersion.isAvailable+"): temp dir: "+tmpRoot.getAbsolutePath());
        }
        return tmpRoot;
    }
    
    /**
     * This methods finds [and creates] a temporary directory:
     * <pre>
     *    for(tempBaseDir = tempRootDir + tmpDirPrefix + _ + [000000-999999]) {
     *      if(tempBaseDir.isDirectory()) {
     *          if(tempBaseDir.canWrite()) {
     *              return tempBaseDir;
     *          }
     *      } else {
     *          tempBaseDir.mkdir();
     *          return tempBaseDir;
     *      }
     *    }
     * </pre>
     * The <code>tempRootDir</code> is retrieved by {@link #getTempRoot(AccessControlContext)}.
     * <p>
     * The iteration through [000000-999999] ensures that the code is multi-user save.
     * </p>
     * @param tmpDirPrefix
     * @return a temporary directory, writable by this user
     * @throws IOException
     * @throws SecurityException
     */
    public static File getTempDir(String tmpDirPrefix, AccessControlContext acc)
        throws IOException, SecurityException
    {
       final File tempRoot = IOUtil.getTempRoot(acc);
       
       for(int i = 0; i<=999999; i++) {
           final String tmpDirSuffix = String.format("_%06d", i); // 6 digits for iteration
           final File tmpBaseDir = new File(tempRoot, tmpDirPrefix+tmpDirSuffix);
           if (tmpBaseDir.isDirectory()) {
               // existing directory
               if(tmpBaseDir.canWrite()) {
                   // can write - OK
                   return tmpBaseDir; 
               }
               // not writable, hence used by another user - continue
           } else {
               // non existing directory, create and validate it
               tmpBaseDir.mkdir();
               if (!tmpBaseDir.isDirectory()) {
                   throw new IOException("Cannot create temp base directory " + tmpBaseDir);
               }
               if(!tmpBaseDir.canWrite()) {
                   throw new IOException("Cannot write to created temp base directory " + tmpBaseDir);
               }
               return tmpBaseDir; // created and writable - OK
           }
       }
       throw new IOException("Could not create temp directory @ "+tempRoot.getAbsolutePath()+tmpDirPrefix+"_*");        
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
