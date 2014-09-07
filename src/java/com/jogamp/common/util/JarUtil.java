/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.Platform;

import jogamp.common.Debug;

public class JarUtil {
    private static final boolean DEBUG = Debug.debug("JarUtil");

    private static final int BUFFER_SIZE = 4096;

    /**
     * Interface allowing users to provide an URL resolver that will convert custom classloader
     * URLs like Eclipse/OSGi <i>bundleresource:</i> URLs to normal <i>jar:</i> URLs.
     * <p>
     * This might be required for custom classloader where the URI protocol is unknown
     * to the standard runtime environment.
     * </p>
     * <p>
     * Note: The provided resolver is only utilized if a given URI's protocol could not be resolved.
     * I.e. it will not be invoked for known protocols like <i>http</i>, <i>https</i>, <i>jar</i> or <i>file</i>.
     * </p>
     */
    public interface Resolver {
        URL resolve(URL url);
    }

    private static Resolver resolver;

    /**
     * Setting a custom {@link Resolver} instance.
     *
     * @param r {@link Resolver} to use after querying class file URLs from the classloader.
     * @throws IllegalArgumentException if the passed resolver is <code>null</code>
     * @throws IllegalStateException if the resolver has already been set.
     * @throws SecurityException if the security manager doesn't have the setFactory
     * permission
     */
    public static void setResolver(final Resolver r) throws IllegalArgumentException, IllegalStateException, SecurityException {
        if(r == null) {
            throw new IllegalArgumentException("Null Resolver passed");
        }

        if(resolver != null) {
            throw new IllegalStateException("Resolver already set!");
        }

        final SecurityManager security = System.getSecurityManager();
        if(security != null) {
            security.checkSetFactory();
        }

        resolver = r;
    }

    /**
     * Returns <code>true</code> if the Class's <code>"com.jogamp.common.GlueGenVersion"</code>
     * is loaded from a JarFile and hence has a Jar URI like
     * URI <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class"</code>.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     *
     * @param clazzBinName "com.jogamp.common.GlueGenVersion"
     * @param cl
     * @return true if the class is loaded from a Jar file, otherwise false.
     * @see {@link #getJarURI(String, ClassLoader)}
     */
    public static boolean hasJarURI(final String clazzBinName, final ClassLoader cl) {
        try {
            return null != getJarURI(clazzBinName, cl);
        } catch (final Exception e) { /* ignore */ }
        return false;
    }

    /**
     * The Class's <code>"com.jogamp.common.GlueGenVersion"</code>
     * URI <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class"</code>
     * will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     *
     * @param clazzBinName "com.jogamp.common.GlueGenVersion"
     * @param cl ClassLoader to locate the JarFile
     * @return "jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class"
     * @throws IllegalArgumentException if the URI doesn't match the expected formatting or null arguments
     * @throws IOException if the class's Jar file could not been found by the ClassLoader
     * @throws URISyntaxException if the URI could not be translated into a RFC 2396 URI
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static URI getJarURI(final String clazzBinName, final ClassLoader cl) throws IllegalArgumentException, IOException, URISyntaxException {
        if(null == clazzBinName || null == cl) {
            throw new IllegalArgumentException("null arguments: clazzBinName "+clazzBinName+", cl "+cl);
        }
        final URI uri;
        final URL url;
        {
            url = IOUtil.getClassURL(clazzBinName, cl);
            final String scheme = url.getProtocol();
            if( null != resolver &&
                !scheme.equals( IOUtil.JAR_SCHEME ) &&
                !scheme.equals( IOUtil.FILE_SCHEME ) &&
                !scheme.equals( IOUtil.HTTP_SCHEME ) &&
                !scheme.equals( IOUtil.HTTPS_SCHEME ) )
            {
                final URL _url = resolver.resolve( url );
                uri = _url.toURI();
                if(DEBUG) {
                    System.err.println("getJarURI Resolver: "+url+"\n\t-> "+_url+"\n\t-> "+uri);
                }
            } else {
                uri = url.toURI();
                if(DEBUG) {
                    System.err.println("getJarURI Default "+url+"\n\t-> "+uri);
                }
            }
        }
        // test name ..
        if( !uri.getScheme().equals( IOUtil.JAR_SCHEME ) ) {
            throw new IllegalArgumentException("URI is not using scheme "+IOUtil.JAR_SCHEME+": <"+uri+">");
        }
        if(DEBUG) {
            System.err.println("getJarURI res: "+clazzBinName+" -> "+url+" -> "+uri);
        }
        return uri;
    }


    /**
     * The Class's Jar URI <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * Jar basename <code>gluegen-rt.jar</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     *
     * @param classJarURI as retrieved w/ {@link #getJarURI(String, ClassLoader) getJarURI("com.jogamp.common.GlueGenVersion", cl).toURI()},
     *                    i.e. <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * @return <code>gluegen-rt.jar</code>
     * @throws IllegalArgumentException if the URI doesn't match the expected formatting or is null
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static String getJarBasename(final URI classJarURI) throws IllegalArgumentException {
        if(null == classJarURI) {
            throw new IllegalArgumentException("URI is null");
        }
        if( !classJarURI.getScheme().equals(IOUtil.JAR_SCHEME) ) {
            throw new IllegalArgumentException("URI is not using scheme "+IOUtil.JAR_SCHEME+": <"+classJarURI+">");
        }
        String uriS = classJarURI.getSchemeSpecificPart();

        // from
        //   file:/some/path/gluegen-rt.jar!/com/jogamp/common/util/cache/TempJarCache.class
        // to
        //   file:/some/path/gluegen-rt.jar
        int idx = uriS.lastIndexOf(IOUtil.JAR_SCHEME_SEPARATOR);
        if (0 <= idx) {
            uriS = uriS.substring(0, idx); // exclude '!/'
        } else {
            throw new IllegalArgumentException("URI does not contain jar uri terminator '!', in <"+classJarURI+">");
        }

        // from
        //   file:/some/path/gluegen-rt.jar
        // to
        //   gluegen-rt.jar
        idx = uriS.lastIndexOf('/');
        if(0 > idx) {
            // no abs-path, check for protocol terminator ':'
            idx = uriS.lastIndexOf(':');
            if(0 > idx) {
                throw new IllegalArgumentException("URI does not contain protocol terminator ':', in <"+classJarURI+">");
            }
        }
        uriS = uriS.substring(idx+1); // just the jar name

        if(0 >= uriS.lastIndexOf(".jar")) {
            throw new IllegalArgumentException("No Jar name in <"+classJarURI+">");
        }
        if(DEBUG) {
            System.err.println("getJarName res: "+uriS);
        }
        return uriS;
    }

    /**
     * The Class's <code>com.jogamp.common.GlueGenVersion</code>
     * URI <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * Jar basename <code>gluegen-rt.jar</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     *
     * @param clazzBinName <code>com.jogamp.common.GlueGenVersion</code>
     * @param cl
     * @return <code>gluegen-rt.jar</code>
     * @throws IllegalArgumentException if the URI doesn't match the expected formatting
     * @throws IOException if the class's Jar file could not been found by the ClassLoader.
     * @throws URISyntaxException if the URI could not be translated into a RFC 2396 URI
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static String getJarBasename(final String clazzBinName, final ClassLoader cl) throws IllegalArgumentException, IOException, URISyntaxException {
        return getJarBasename( getJarURI(clazzBinName, cl) );
    }

    /**
     * The Class's Jar URI <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * Jar file's sub URI <code><i>sub_protocol</i>:/some/path/gluegen-rt.jar</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     *
     * @param classJarURI as retrieved w/ {@link #getJarURI(String, ClassLoader) getJarURI("com.jogamp.common.GlueGenVersion", cl).toURI()},
     *                    i.e. <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * @return <code><i>sub_protocol</i>:/some/path/gluegen-rt.jar</code>
     * @throws IllegalArgumentException if the URI doesn't match the expected formatting or is null
     * @throws URISyntaxException if the URI could not be translated into a RFC 2396 URI
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static URI getJarSubURI(final URI classJarURI) throws IllegalArgumentException, URISyntaxException {
        if(null == classJarURI) {
            throw new IllegalArgumentException("URI is null");
        }
        if( !classJarURI.getScheme().equals(IOUtil.JAR_SCHEME) ) {
            throw new IllegalArgumentException("URI is not a using scheme "+IOUtil.JAR_SCHEME+": <"+classJarURI+">");
        }

        // from
        //   file:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class
        // to
        //   file:/some/path/gluegen-rt.jar
        final String uriSSP = classJarURI.getRawSchemeSpecificPart();
        final int idx = uriSSP.lastIndexOf(IOUtil.JAR_SCHEME_SEPARATOR);
        final String uriSSPJar;
        if (0 <= idx) {
            uriSSPJar = uriSSP.substring(0, idx); // exclude '!/'
        } else {
            throw new IllegalArgumentException("JAR URI does not contain jar uri terminator '!', uri <"+classJarURI+">");
        }
        if(0 >= uriSSPJar.lastIndexOf(".jar")) {
            throw new IllegalArgumentException("No Jar name in <"+classJarURI+">");
        }
        if(DEBUG) {
            System.err.println("getJarSubURI res: "+classJarURI+" -> "+uriSSP+" -> "+uriSSPJar);
        }
        return new URI(uriSSPJar);
    }

    /**
     * The Class's Jar URI <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * Jar file's entry <code>/com/jogamp/common/GlueGenVersion.class</code> will be returned.
     *
     * @param classJarURI as retrieved w/ {@link #getJarURI(String, ClassLoader) getJarURI("com.jogamp.common.GlueGenVersion", cl).toURI()},
     *                    i.e. <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * @return <code>/com/jogamp/common/GlueGenVersion.class</code>
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     * @deprecated Useless
     */
    public static String getJarEntry(final URI classJarURI) {
        if(null == classJarURI) {
            throw new IllegalArgumentException("URI is null");
        }
        if( !classJarURI.getScheme().equals(IOUtil.JAR_SCHEME) ) {
            throw new IllegalArgumentException("URI is not a using scheme "+IOUtil.JAR_SCHEME+": <"+classJarURI+">");
        }
        final String uriSSP = classJarURI.getSchemeSpecificPart();
        // Uri TODO ? final String uriSSP = classJarURI.getRawSchemeSpecificPart();

        // from
        //   file:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class
        // to
        //   /com/jogamp/common/GlueGenVersion.class
        final int idx = uriSSP.lastIndexOf(IOUtil.JAR_SCHEME_SEPARATOR);
        if (0 <= idx) {
            final String res = uriSSP.substring(idx+1); // right of '!'
            // Uri TODO ? final String res = Uri.decode(uriSSP.substring(idx+1)); // right of '!'
            if(DEBUG) {
                System.err.println("getJarEntry res: "+classJarURI+" -> "+uriSSP+" -> "+idx+" -> "+res);
            }
            return res;
        } else {
            throw new IllegalArgumentException("JAR URI does not contain jar uri terminator '!', uri <"+classJarURI+">");
        }
    }

    /**
     * The Class's <code>com.jogamp.common.GlueGenVersion</code>
     * URI <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * Jar file's sub URI <code><i>sub_protocol</i>:/some/path/gluegen-rt.jar</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     *
     * @param clazzBinName <code>com.jogamp.common.GlueGenVersion</code>
     * @param cl
     * @return <code><i>sub_protocol</i>:/some/path/gluegen-rt.jar</code>
     * @throws IllegalArgumentException if the URI doesn't match the expected formatting
     * @throws IOException if the class's Jar file could not been found by the ClassLoader
     * @throws URISyntaxException if the URI could not be translated into a RFC 2396 URI
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static URI getJarSubURI(final String clazzBinName, final ClassLoader cl) throws IllegalArgumentException, IOException, URISyntaxException {
        return getJarSubURI( getJarURI(clazzBinName, cl) );
    }

    /**
     * The Class's <code>"com.jogamp.common.GlueGenVersion"</code>
     * URI <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class"</code>
     * Jar file URI <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     *
     * @param clazzBinName "com.jogamp.common.GlueGenVersion"
     * @param cl
     * @return "jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/"
     * @throws IllegalArgumentException if the URI doesn't match the expected formatting or null arguments
     * @throws IOException if the class's Jar file could not been found by the ClassLoader
     * @throws URISyntaxException if the URI could not be translated into a RFC 2396 URI
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static URI getJarFileURI(final String clazzBinName, final ClassLoader cl) throws IllegalArgumentException, IOException, URISyntaxException {
        if(null == clazzBinName || null == cl) {
            throw new IllegalArgumentException("null arguments: clazzBinName "+clazzBinName+", cl "+cl);
        }
        final URI uri = new URI(IOUtil.JAR_SCHEME+IOUtil.SCHEME_SEPARATOR+getJarSubURI(clazzBinName, cl).toString()+"!/");
        if(DEBUG) {
            System.err.println("getJarFileURI res: "+uri);
        }
        return uri;
    }

    /**
     * @param baseUri file:/some/path/
     * @param jarFileName gluegen-rt.jar (URI encoded)
     * @return jar:file:/some/path/gluegen-rt.jar!/
     * @throws URISyntaxException
     * @throws IllegalArgumentException null arguments
     */
    public static URI getJarFileURI(final URI baseUri, final String jarFileName) throws IllegalArgumentException, URISyntaxException {
        if(null == baseUri || null == jarFileName) {
            throw new IllegalArgumentException("null arguments: baseURI "+baseUri+", jarFileName "+jarFileName);
        }
        return new URI(IOUtil.JAR_SCHEME+IOUtil.SCHEME_SEPARATOR+baseUri.toString()+jarFileName+"!/");
    }

    /**
     * @param jarSubUri file:/some/path/gluegen-rt.jar
     * @return jar:file:/some/path/gluegen-rt.jar!/
     * @throws IllegalArgumentException null arguments
     * @throws URISyntaxException
     */
    public static URI getJarFileURI(final URI jarSubUri) throws IllegalArgumentException, URISyntaxException {
        if(null == jarSubUri) {
            throw new IllegalArgumentException("jarSubURI is null");
        }
        return new URI(IOUtil.JAR_SCHEME+IOUtil.SCHEME_SEPARATOR+jarSubUri.toString()+"!/");
    }

    /**
     * @param jarSubUriS file:/some/path/gluegen-rt.jar (URI encoded)
     * @return jar:file:/some/path/gluegen-rt.jar!/
     * @throws IllegalArgumentException null arguments
     * @throws URISyntaxException
     */
    public static URI getJarFileURI(final String jarSubUriS) throws IllegalArgumentException, URISyntaxException {
        if(null == jarSubUriS) {
            throw new IllegalArgumentException("jarSubURIS is null");
        }
        return new URI(IOUtil.JAR_SCHEME+IOUtil.SCHEME_SEPARATOR+jarSubUriS+"!/");
    }

    /**
     * @param jarFileURI jar:file:/some/path/gluegen-rt.jar!/
     * @param jarEntry com/jogamp/common/GlueGenVersion.class
     * @return jar:file:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class
     * @throws IllegalArgumentException null arguments
     * @throws URISyntaxException
     */
    public static URI getJarEntryURI(final URI jarFileURI, final String jarEntry) throws IllegalArgumentException, URISyntaxException {
        if(null == jarEntry) {
            throw new IllegalArgumentException("jarEntry is null");
        }
        return new URI(jarFileURI.toString()+jarEntry);
    }

    /**
     * @param clazzBinName com.jogamp.common.util.cache.TempJarCache
     * @param cl domain
     * @return JarFile containing the named class within the given ClassLoader
     * @throws IOException if the class's Jar file could not been found by the ClassLoader
     * @throws IllegalArgumentException null arguments
     * @throws URISyntaxException if the URI could not be translated into a RFC 2396 URI
     * @see {@link #getJarFileURI(String, ClassLoader)}
     */
    public static JarFile getJarFile(final String clazzBinName, final ClassLoader cl) throws IOException, IllegalArgumentException, URISyntaxException {
        return getJarFile( getJarFileURI(clazzBinName, cl) );
    }

    /**
     * @param jarFileURI jar:file:/some/path/gluegen-rt.jar!/
     * @return JarFile as named by URI within the given ClassLoader
     * @throws IllegalArgumentException null arguments
     * @throws IOException if the Jar file could not been found
     * @throws URISyntaxException
     */
    public static JarFile getJarFile(final URI jarFileURI) throws IOException, IllegalArgumentException, URISyntaxException {
        if(null == jarFileURI) {
            throw new IllegalArgumentException("null jarFileURI");
        }
        if(DEBUG) {
            System.err.println("getJarFile.0: "+jarFileURI.toString());
        }
        final URL jarFileURL = jarFileURI.toURL();
        if(DEBUG) {
            System.err.println("getJarFile.1: "+jarFileURL.toString());
        }
        final URLConnection urlc = jarFileURL.openConnection();
        if(urlc instanceof JarURLConnection) {
            final JarURLConnection jarConnection = (JarURLConnection)jarFileURL.openConnection();
            final JarFile jarFile = jarConnection.getJarFile();
            if(DEBUG) {
                System.err.println("getJarFile res: "+jarFile.getName());
            }
            return jarFile;
        }
        if(DEBUG) {
            System.err.println("getJarFile res: NULL");
        }
        return null;
    }

    /**
     * Locates the {@link JarUtil#getJarFileURI(URI) Jar file URI} of a given resource
     * relative to a given class's Jar's URI.
     * <pre>
     *   class's jar url path + cutOffInclSubDir + relResPath,
     * </pre>
     * Example #1
     * <pre>
     *   classFromJavaJar = com.lighting.Test (in: file:/storage/TestLighting.jar)
     *   cutOffInclSubDir = lights/
     *   relResPath       = LightAssets.jar
     *   Result           : file:/storage/lights/LightAssets.jar
     * </pre>
     * Example #2
     * <pre>
     *   classFromJavaJar = com.lighting.Test (in: file:/storage/lights/TestLighting.jar)
     *   cutOffInclSubDir = lights/
     *   relResPath       = LightAssets.jar
     *   Result           : file:/storage/lights/LightAssets.jar
     * </pre>
     *
     * TODO: Enhance documentation!
     *
     * @param classFromJavaJar Used to get the root URI for the class's Jar URI.
     * @param cutOffInclSubDir The <i>cut off</i> included sub-directory prepending the relative resource path.
     *                         If the root URI includes cutOffInclSubDir, it is no more added to the result.
     * @param relResPath The relative resource path. (URI encoded)
     * @return The resulting resource URI, which is not tested.
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws URISyntaxException
     */
    public static URI getRelativeOf(final Class<?> classFromJavaJar, final String cutOffInclSubDir, final String relResPath) throws IllegalArgumentException, IOException, URISyntaxException {
        final ClassLoader cl = classFromJavaJar.getClassLoader();
        final URI classJarURI = JarUtil.getJarURI(classFromJavaJar.getName(), cl);
        if( DEBUG ) {
            System.err.println("JarUtil.getRelativeOf: "+"(classFromJavaJar "+classFromJavaJar+", classJarURI "+classJarURI+
                    ", cutOffInclSubDir "+cutOffInclSubDir+", relResPath "+relResPath+"): ");
        }

        final URI jarSubURI = JarUtil.getJarSubURI( classJarURI );
        if(null == jarSubURI) {
            throw new IllegalArgumentException("JarSubURI is null of: "+classJarURI);
        }
        final String jarUriRoot_s = IOUtil.getURIDirname( jarSubURI.toString() );
        if( DEBUG ) {
            System.err.println("JarUtil.getRelativeOf: "+"uri "+jarSubURI.toString()+" -> "+jarUriRoot_s);
        }

        final String resUri_s;
        if( jarUriRoot_s.endsWith(cutOffInclSubDir) ) {
            resUri_s = jarUriRoot_s+relResPath;
        } else {
            resUri_s = jarUriRoot_s+cutOffInclSubDir+relResPath;
        }
        if( DEBUG ) {
            System.err.println("JarUtil.getRelativeOf: "+"...  -> "+resUri_s);
        }

        final URI resURI = JarUtil.getJarFileURI(resUri_s);
        if( DEBUG ) {
            System.err.println("JarUtil.getRelativeOf: "+"fin "+resURI);
        }
        return resURI;
    }

    /**
     * Return a map from native-lib-base-name to entry-name.
     */
    public static Map<String, String> getNativeLibNames(final JarFile jarFile) {
        if (DEBUG) {
            System.err.println("JarUtil: getNativeLibNames: "+jarFile);
        }

        final Map<String,String> nameMap = new HashMap<String, String>();
        final Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            final String entryName = entry.getName();
            final String baseName =  NativeLibrary.isValidNativeLibraryName(entryName, false);

            if(null != baseName) {
                nameMap.put(baseName, entryName);
            }
        }

        return nameMap;
    }

    /**
     * Extract the files of the given jar file.
     * <p>
     * If <code>extractNativeLibraries</code> is true,
     * native libraries are added to the given <code>nativeLibMap</code>
     * with the base name to temp file location.<br>
     * A file is identified as a native library,
     * if it's name complies with the running platform's native library naming scheme.<br>
     * Root entries are favored over non root entries in case of naming collisions.<br>
     * Example on a Unix like machine:<br>
     * <pre>
     *   mylib.jar!/sub1/libsour.so   -> sour  (mapped, unique name)
     *   mylib.jar!/sub1/libsweet.so           (dropped, root entry favored)
     *   mylib.jar!/libsweet.so       -> sweet (mapped, root entry favored)
     *   mylib.jar!/sweet.dll         ->       (dropped, not a unix library name)
     * </pre>
     * </p>
     * <p>
     * In order to be compatible with Java Web Start, we need
     * to extract all root entries from the jar file.<br>
     * In this case, set all flags to true <code>extractNativeLibraries </code>.
     * <code>extractClassFiles</code>, <code>extractOtherFiles</code>.
     * </p>
     *
     * @param dest
     * @param nativeLibMap
     * @param jarFile
     * @param nativeLibraryPath if not null, only extracts native libraries within this path.
     * @param extractNativeLibraries
     * @param extractClassFiles
     * @param extractOtherFiles
     * @param deepDirectoryTraversal
     * @return
     * @throws IOException
     */
    public static final int extract(final File dest, final Map<String, String> nativeLibMap,
                                    final JarFile jarFile,
                                    final String nativeLibraryPath,
                                    final boolean extractNativeLibraries,
                                    final boolean extractClassFiles, final boolean extractOtherFiles) throws IOException {

        if (DEBUG) {
            System.err.println("JarUtil: extract: "+jarFile.getName()+" -> "+dest+
                               ", extractNativeLibraries "+extractNativeLibraries+" ("+nativeLibraryPath+")"+
                               ", extractClassFiles "+extractClassFiles+
                               ", extractOtherFiles "+extractOtherFiles);
        }
        int num = 0;

        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            final String entryName = entry.getName();

            // Match entries with correct prefix and suffix (ignoring case)
            final String libBaseName = NativeLibrary.isValidNativeLibraryName(entryName, false);
            final boolean isNativeLib = null != libBaseName;
            if(isNativeLib) {
                if(!extractNativeLibraries) {
                    if (DEBUG) {
                        System.err.println("JarUtil: JarEntry : " + entryName + " native-lib skipped, skip all native libs");
                    }
                    continue;
                }
                if(null != nativeLibraryPath) {
                    final String nativeLibraryPathS;
                    final String dirnameS;
                    try {
                        nativeLibraryPathS = IOUtil.slashify(nativeLibraryPath, false /* startWithSlash */, true /* endWithSlash */);
                        dirnameS = IOUtil.getDirname(entryName);
                    } catch (final URISyntaxException e) {
                        throw new IOException(e);
                    }
                    if( !nativeLibraryPathS.equals(dirnameS) ) {
                        if (DEBUG) {
                            System.err.println("JarUtil: JarEntry : " + entryName + " native-lib skipped, not in path: "+nativeLibraryPathS);
                        }
                        continue;
                    }
                }
            }

            final boolean isClassFile = entryName.endsWith(".class");
            if(isClassFile && !extractClassFiles) {
                if (DEBUG) {
                    System.err.println("JarUtil: JarEntry : " + entryName + " class-file skipped");
                }
                continue;
            }

            if(!isNativeLib && !isClassFile && !extractOtherFiles) {
                if (DEBUG) {
                    System.err.println("JarUtil: JarEntry : " + entryName + " other-file skipped");
                }
                continue;
            }

            final boolean isDir = entryName.endsWith("/");

            final boolean isRootEntry = entryName.indexOf('/') == -1 &&
                                        entryName.indexOf(File.separatorChar) == -1;

            if (DEBUG) {
                System.err.println("JarUtil: JarEntry : isNativeLib " + isNativeLib +
                                   ", isClassFile " + isClassFile + ", isDir " + isDir +
                                   ", isRootEntry " + isRootEntry );
            }

            final File destFile = new File(dest, entryName);
            if(isDir) {
                if (DEBUG) {
                    System.err.println("JarUtil: MKDIR: " + entryName + " -> " + destFile );
                }
                destFile.mkdirs();
            } else {
                final File destFolder = new File(destFile.getParent());
                if(!destFolder.exists()) {
                    if (DEBUG) {
                        System.err.println("JarUtil: MKDIR (parent): " + entryName + " -> " + destFolder );
                    }
                    destFolder.mkdirs();
                }
                final InputStream in = new BufferedInputStream(jarFile.getInputStream(entry));
                final OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
                int numBytes = -1;
                try {
                    numBytes = IOUtil.copyStream2Stream(BUFFER_SIZE, in, out, -1);
                } finally {
                    in.close();
                    out.close();
                }
                boolean addedAsNativeLib = false;
                if (numBytes>0) {
                    num++;
                    if (isNativeLib && ( isRootEntry || !nativeLibMap.containsKey(libBaseName) ) ) {
                        nativeLibMap.put(libBaseName, destFile.getAbsolutePath());
                        addedAsNativeLib = true;
                        fixNativeLibAttribs(destFile);
                    }
                }
                if (DEBUG) {
                    System.err.println("JarUtil: EXTRACT["+num+"]: [" + libBaseName + " -> ] " + entryName + " -> " + destFile + ": "+numBytes+" bytes, addedAsNativeLib: "+addedAsNativeLib);
                }
            }
        }
        return num;
    }

    /**
     * Mitigate file permission issues of native library files, i.e.:
     * <ul>
     *   <li>Bug 865: Safari >= 6.1 [OSX]: May employ xattr on 'com.apple.quarantine' on 'PluginProcess.app'</li>
     * </ul>
     */
    private final static void fixNativeLibAttribs(final File file) {
        // We tolerate UnsatisfiedLinkError (and derived) to solve the chicken and egg problem
        // of loading gluegen's native library.
        // On Safari(OSX), Bug 865, we simply hope the destination folder is executable.
        if( Platform.OSType.MACOS == Platform.getOSType() ) {
            final String fileAbsPath = file.getAbsolutePath();
            try {
                fixNativeLibAttribs(fileAbsPath);
                if( DEBUG ) {
                    System.err.println("JarUtil.fixNativeLibAttribs: "+fileAbsPath+" - OK");
                }
            } catch (final Throwable t) {
                if( DEBUG ) {
                    System.err.println("JarUtil.fixNativeLibAttribs: "+fileAbsPath+" - "+t.getClass().getSimpleName()+": "+t.getMessage());
                }
            }
        }
    }
    private native static boolean fixNativeLibAttribs(String fname);

    /**
     * Validate the certificates for each native Lib in the jar file.
     * Throws an IOException if any certificate is not valid.
     * <pre>
        Certificate[] rootCerts = Something.class.getProtectionDomain().
                                        getCodeSource().getCertificates();
       </pre>
     */
    public static final void validateCertificates(final Certificate[] rootCerts, final JarFile jarFile)
            throws IOException, SecurityException {

        if (DEBUG) {
            System.err.println("JarUtil: validateCertificates: "+jarFile.getName());
        }

        if (rootCerts == null || rootCerts.length == 0) {
            throw new IllegalArgumentException("Null certificates passed");
        }

        final byte[] buf = new byte[1024];
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if( ! entry.isDirectory() && ! entry.getName().startsWith("META-INF/") ) {
                // only validate non META-INF and non directories
                validateCertificate(rootCerts, jarFile, entry, buf);
            }
        }
    }

    /**
     * Check the certificates with the ones in the jar file
     * (all must match).
     */
    private static final void validateCertificate(final Certificate[] rootCerts,
            final JarFile jar, final JarEntry entry, final byte[] buf) throws IOException, SecurityException {

        if (DEBUG) {
            System.err.println("JarUtil: validate JarEntry : " + entry.getName());
        }

        // API states that we must read all of the data from the entry's
        // InputStream in order to be able to get its certificates

        final InputStream is = jar.getInputStream(entry);
        try {
            while (is.read(buf) > 0) { }
        } finally {
            is.close();
        }

        // Get the certificates for the JAR entry
        final Certificate[] nativeCerts = entry.getCertificates();
        if (nativeCerts == null || nativeCerts.length == 0) {
            throw new SecurityException("no certificate for " + entry.getName() + " in " + jar.getName());
        }

        if( !SecurityUtil.equals(rootCerts, nativeCerts) ) {
            throw new SecurityException("certificates not equal for " + entry.getName() + " in " + jar.getName());
        }
    }
}
