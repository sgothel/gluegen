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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.jogamp.common.os.NativeLibrary;

import jogamp.common.Debug;

public class JarUtil {
    private static final boolean DEBUG = Debug.debug("JarUtil");

    /**
     * Returns <code>true</code> if the Class's <code>"com.jogamp.common.GlueGenVersion"</code>
     * is loaded from a JarFile and hence has a Jar URL like 
     * URL <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class"</code>.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     * 
     * @param clazzBinName "com.jogamp.common.GlueGenVersion" 
     * @param cl
     * @return true if the class is loaded from a Jar file, otherwise false.
     * @see {@link #getJarURL(String, ClassLoader)}
     */
    public static boolean hasJarURL(String clazzBinName, ClassLoader cl) {
        try {
            final URL url = getJarURL(clazzBinName, cl);
            return null != url;        
        } catch (Exception e) { /* ignore */ }
        return false;
    }
    
    /**
     * The Class's <code>"com.jogamp.common.GlueGenVersion"</code> 
     * URL <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class"</code>
     * will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     * 
     * @param clazzBinName "com.jogamp.common.GlueGenVersion" 
     * @param cl ClassLoader to locate the JarFile
     * @return "jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class"
     * @throws IllegalArgumentException if the URL doesn't match the expected formatting or null arguments
     * @throws IOException if the class's Jar file could not been found by the ClassLoader 
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static URL getJarURL(String clazzBinName, ClassLoader cl) throws IllegalArgumentException, IOException {
        if(null == clazzBinName || null == cl) {
            throw new IllegalArgumentException("null arguments: clazzBinName "+clazzBinName+", cl "+cl);
        }
        final URL url = IOUtil.getClassURL(clazzBinName, cl);
        // test name ..
        final String urlS = url.toExternalForm();
        if(DEBUG) {
            System.out.println("getJarURL "+url+", extForm: "+urlS);
        }
        if(!urlS.startsWith("jar:")) {
            throw new IllegalArgumentException("JAR URL doesn't start with 'jar:', got <"+urlS+">");
        }
        return url;
    }
    
    
    /**
     * The Class's Jar URL <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * Jar basename <code>gluegen-rt.jar</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     * 
     * @param classJarURL as retrieved w/ {@link #getJarURL(String, ClassLoader) getJarURL("com.jogamp.common.GlueGenVersion", cl)}, 
     *                    i.e. <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * @return <code>gluegen-rt.jar</code>
     * @throws IllegalArgumentException if the URL doesn't match the expected formatting or is null 
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static String getJarBasename(URL classJarURL) throws IllegalArgumentException {
        if(null == classJarURL) {
            throw new IllegalArgumentException("URL is null");            
        }
        String urlS = classJarURL.toExternalForm();            
        urlS = urlS.substring(4, urlS.length()); // exclude 'jar:'
        
        // from 
        //   file:/some/path/gluegen-rt.jar!/com/jogamp/common/util/cache/TempJarCache.class
        // to
        //   file:/some/path/gluegen-rt.jar
        int idx = urlS.lastIndexOf('!');
        if (0 <= idx) {
            urlS = urlS.substring(0, idx); // exclude '!/'                
        } else {
            throw new IllegalArgumentException("JAR URL does not contain jar url terminator '!', in <"+classJarURL.toExternalForm()+">, got <"+urlS+">");
        }
        
        // from 
        //   file:/some/path/gluegen-rt.jar
        // to
        //   gluegen-rt.jar
        idx = urlS.lastIndexOf('/');
        if(0 > idx) {
            // no abs-path, check for protocol terminator ':'
            idx = urlS.lastIndexOf(':');
            if(0 > idx) {
                throw new IllegalArgumentException("JAR URL does not contain protocol terminator ':', in <"+classJarURL.toExternalForm()+">, got <"+urlS+">");
            }
        }
        urlS = urlS.substring(idx+1); // just the jar name
        
        if(0 >= urlS.lastIndexOf(".jar")) {
            throw new IllegalArgumentException("No Jar name in <"+classJarURL.toExternalForm()+">, got <"+urlS+">");
        }                    
        if(DEBUG) {
            System.out.println("getJarName res: "+urlS);
        }
        return urlS;
    }

    /**
     * The Class's <code>com.jogamp.common.GlueGenVersion</code> 
     * URL <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * Jar basename <code>gluegen-rt.jar</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     * 
     * @param clazzBinName <code>com.jogamp.common.GlueGenVersion</code>
     * @param cl
     * @return <code>gluegen-rt.jar</code>
     * @throws IllegalArgumentException if the URL doesn't match the expected formatting 
     * @throws IOException if the class's Jar file could not been found by the ClassLoader 
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static String getJarBasename(String clazzBinName, ClassLoader cl) throws IllegalArgumentException, IOException {
        return getJarBasename(getJarURL(clazzBinName, cl));
    }
    
    /**
     * The Class's Jar URL <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * Jar file's sub URL <code><i>sub_protocol</i>:/some/path/gluegen-rt.jar</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     * 
     * @param classJarURL as retrieved w/ {@link #getJarURL(String, ClassLoader) getJarURL("com.jogamp.common.GlueGenVersion", cl)}, 
     *                    i.e. <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * @param cl
     * @return <code><i>sub_protocol</i>:/some/path/gluegen-rt.jar</code>
     * @throws IllegalArgumentException if the URL doesn't match the expected formatting or is null 
     * @throws MalformedURLException if the computed URL specifies an unknown protocol
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static URL getJarSubURL(URL classJarURL) throws IllegalArgumentException, MalformedURLException {
        if(null == classJarURL) {
            throw new IllegalArgumentException("URL is null");            
        }
        String urlS = classJarURL.toExternalForm();
        urlS = urlS.substring(4, urlS.length()); // exclude 'jar:'
        
        // from 
        //   file:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class
        // to
        //   file:/some/path/gluegen-rt.jar
        int idx = urlS.lastIndexOf('!');
        if (0 <= idx) {
            urlS = urlS.substring(0, idx); // exclude '!/'
        } else {
            throw new IllegalArgumentException("JAR URL does not contain jar url terminator '!', url <"+urlS+">");
        }
        
        if(0 >= urlS.lastIndexOf(".jar")) {
            throw new IllegalArgumentException("No Jar name in <"+classJarURL.toExternalForm()+">, got <"+urlS+">");
        }                    
        if(DEBUG) {
            System.out.println("getJarSubURL res: "+urlS);
        }
        return new URL(urlS);
    }

    /**
     * The Class's <code>com.jogamp.common.GlueGenVersion</code> 
     * URL <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     * Jar file's sub URL <code><i>sub_protocol</i>:/some/path/gluegen-rt.jar</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     * 
     * @param clazzBinName <code>com.jogamp.common.GlueGenVersion</code>
     * @param cl
     * @return <code><i>sub_protocol</i>:/some/path/gluegen-rt.jar</code>
     * @throws IllegalArgumentException if the URL doesn't match the expected formatting 
     * @throws IOException if the class's Jar file could not been found by the ClassLoader 
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static URL getJarSubURL(String clazzBinName, ClassLoader cl) throws IllegalArgumentException, IOException {
        return getJarSubURL(getJarURL(clazzBinName, cl));
    }

    /**
     * The Class's <code>"com.jogamp.common.GlueGenVersion"</code> 
     * URL <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class"</code>
     * Jar file URL <code>jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/</code> will be returned.
     * <p>
     * <i>sub_protocol</i> may be "file", "http", etc..
     * </p>
     * 
     * @param clazzBinName "com.jogamp.common.GlueGenVersion" 
     * @param cl
     * @return "jar:<i>sub_protocol</i>:/some/path/gluegen-rt.jar!/"
     * @throws IllegalArgumentException if the URL doesn't match the expected formatting or null arguments 
     * @throws IOException if the class's Jar file could not been found by the ClassLoader 
     * @see {@link IOUtil#getClassURL(String, ClassLoader)}
     */
    public static URL getJarFileURL(String clazzBinName, ClassLoader cl) throws IllegalArgumentException, IOException {
        if(null == clazzBinName || null == cl) {
            throw new IllegalArgumentException("null arguments: clazzBinName "+clazzBinName+", cl "+cl);
        }
        URL url = getJarSubURL(clazzBinName, cl);
        url = new URL("jar:"+url.toExternalForm()+"!/");
        if(DEBUG) {
            System.out.println("getJarFileURL res: "+url);
        }
        return url;
    }

    /**
     * The URL's <code><i>protocol</i>:/some/path/gluegen-rt.jar</code>
     * parent dirname URL <code><i>protocol</i>:/some/path/</code> will be returned.
     * <p>
     * <i>protocol</i> may be "file", "http", etc..
     * </p>
     * 
     * @param aURL "<i>protocol</i>:/some/path/gluegen-rt.jar"
     * @return "<i>protocol</i>:/some/path/"
     * @throws IllegalArgumentException if the URL doesn't match the expected formatting, or is null
     * @throws MalformedURLException
     */
    public static URL getURLDirname(URL aURL) throws IllegalArgumentException, MalformedURLException {
        if(null == aURL) {
            throw new IllegalArgumentException("URL is null");            
        }
        String urlS = aURL.toExternalForm();
        if(DEBUG) {
            System.out.println("getURLDirname "+aURL+", extForm: "+urlS);
        }
        // from 
        //   file:/some/path/gluegen-rt.jar  _or_ rsrc:gluegen-rt.jar
        // to
        //   file:/some/path/                _or_ rsrc:
        int idx = urlS.lastIndexOf('/');
        if(0 > idx) {
            // no abs-path, check for protocol terminator ':'
            idx = urlS.lastIndexOf(':');
            if(0 > idx) {
                throw new IllegalArgumentException("URL does not contain protocol terminator ':', in <"+aURL.toExternalForm()+">, got <"+urlS+">");
            }
        }
        urlS = urlS.substring(0, idx+1); // exclude jar name, include terminal '/' or ':'        
        
        if(DEBUG) {
            System.out.println("getJarURLDirname res: "+urlS);
        }        
        return new URL(urlS);
    }
            
    /**
     * @param baseUrl file:/some/path/
     * @param jarFileName gluegen-rt.jar
     * @return jar:file:/some/path/gluegen-rt.jar!/
     * @throws MalformedURLException
     * @throws IllegalArgumentException null arguments
     */
    public static URL getJarFileURL(URL baseUrl, String jarFileName) throws IOException, MalformedURLException {
        if(null == baseUrl || null == jarFileName) {
            throw new IllegalArgumentException("null arguments: baseUrl "+baseUrl+", jarFileName "+jarFileName);
        }
        return new URL("jar:"+baseUrl.toExternalForm()+jarFileName+"!/");
    }
    
    /**
     * @param jarSubUrl file:/some/path/gluegen-rt.jar
     * @return jar:file:/some/path/gluegen-rt.jar!/
     * @throws MalformedURLException
     * @throws IllegalArgumentException null arguments
     */
    public static URL getJarFileURL(URL jarSubUrl) throws MalformedURLException, IllegalArgumentException {
        if(null == jarSubUrl) {
            throw new IllegalArgumentException("jarSubUrl is null");
        }
        return new URL("jar:"+jarSubUrl.toExternalForm()+"!/");
    }
    
    /**
     * @param jarFileURL jar:file:/some/path/gluegen-rt.jar!/
     * @param jarEntry com/jogamp/common/GlueGenVersion.class
     * @return jar:file:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class
     * @throws MalformedURLException
     * @throws IllegalArgumentException null arguments
     */
    public static URL getJarEntryURL(URL jarFileURL, String jarEntry) throws MalformedURLException, IllegalArgumentException {
        if(null == jarEntry) {
            throw new IllegalArgumentException("jarEntry is null");
        }
        return new URL(jarFileURL.toExternalForm()+jarEntry);
    }
    
    /**
     * @param clazzBinName com.jogamp.common.util.cache.TempJarCache 
     * @param cl domain 
     * @return JarFile containing the named class within the given ClassLoader
     * @throws IOException if the class's Jar file could not been found by the ClassLoader 
     * @throws IllegalArgumentException null arguments
     * @see {@link #getJarFileURL(String, ClassLoader)}
     */
    public static JarFile getJarFile(String clazzBinName, ClassLoader cl) throws IOException, IllegalArgumentException {
        return getJarFile(getJarFileURL(clazzBinName, cl));
    }

    /**
     * @param jarFileURL jar:file:/some/path/gluegen-rt.jar!/
     * @return JarFile as named by URL within the given ClassLoader
     * @throws IllegalArgumentException null arguments
     * @throws IOException if the Jar file could not been found 
     */
    public static JarFile getJarFile(URL jarFileUrl) throws IOException, IllegalArgumentException {
        if(null == jarFileUrl) {
            throw new IllegalArgumentException("null jarFileUrl");
        }
        if(DEBUG) {
            System.out.println("getJarFile: "+jarFileUrl);
        }        
        URLConnection urlc = jarFileUrl.openConnection();
        if(urlc instanceof JarURLConnection) {
            JarURLConnection jarConnection = (JarURLConnection)jarFileUrl.openConnection();
            JarFile jarFile = jarConnection.getJarFile();
            if(DEBUG) {
                System.out.println("getJarFile res: "+jarFile.getName());
            }        
            return jarFile;
        }
        if(DEBUG) {
            System.out.println("getJarFile res: NULL");
        }        
        return null;
    }
    
    /**
     * Return a map from native-lib-base-name to entry-name.
     */
    public static Map<String, String> getNativeLibNames(JarFile jarFile) {
        if (DEBUG) {
            System.err.println("JarUtil: getNativeLibNames: "+jarFile);
        }

        Map<String,String> nameMap = new HashMap<String, String>();
        Enumeration<JarEntry> entries = jarFile.entries();
        
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
     * @param deepDirectoryTraversal
     * @param extractNativeLibraries
     * @param extractClassFiles
     * @param extractOtherFiles
     * @return
     * @throws IOException
     */
    public static final int extract(File dest, Map<String, String> nativeLibMap, 
                                    JarFile jarFile,
                                    boolean extractNativeLibraries,
                                    boolean extractClassFiles,
                                    boolean extractOtherFiles) throws IOException {

        if (DEBUG) {
            System.err.println("JarUtil: extract: "+jarFile.getName()+" -> "+dest+
                               ", extractNativeLibraries "+extractNativeLibraries+
                               ", extractClassFiles "+extractClassFiles+
                               ", extractOtherFiles "+extractOtherFiles);
        }
        int num = 0;

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();
            String entryName = entry.getName();

            // Match entries with correct prefix and suffix (ignoring case)
            final String libBaseName = NativeLibrary.isValidNativeLibraryName(entryName, false);
            final boolean isNativeLib = null != libBaseName;
            if(isNativeLib && !extractNativeLibraries) {
                if (DEBUG) {
                    System.err.println("JarUtil: JarEntry : " + entryName + " native-lib skipped");
                }
                continue;
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
                destFile.mkdir();
            } else {
                final File destFolder = new File(destFile.getParent());
                if(!destFolder.exists()) {
                    if (DEBUG) {
                        System.err.println("JarUtil: MKDIR (parent): " + entryName + " -> " + destFolder );
                    }                    
                    destFolder.mkdir();
                }
                final InputStream in = new BufferedInputStream(jarFile.getInputStream(entry));
                final OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
                int numBytes = -1; 
                try {
                    numBytes = IOUtil.copyStream2Stream(in, out, -1);
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
     * Validate the certificates for each native Lib in the jar file.
     * Throws an IOException if any certificate is not valid.
     * <pre>
        Certificate[] rootCerts = Something.class.getProtectionDomain().
                                        getCodeSource().getCertificates();
       </pre>
     */
    public static final void validateCertificates(Certificate[] rootCerts, JarFile jarFile) 
            throws IOException, SecurityException {

        if (DEBUG) {
            System.err.println("JarUtil: validateCertificates: "+jarFile.getName());
        }

        if (rootCerts == null || rootCerts.length == 0) {
            throw new IllegalArgumentException("Null certificates passed");
        }

        byte[] buf = new byte[1024];
        Enumeration<JarEntry> entries = jarFile.entries();
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
    private static final void validateCertificate(Certificate[] rootCerts, 
            JarFile jar, JarEntry entry, byte[] buf) throws IOException, SecurityException {

        if (DEBUG) {
            System.err.println("JarUtil: validate JarEntry : " + entry.getName());
        }

        // API states that we must read all of the data from the entry's
        // InputStream in order to be able to get its certificates

        InputStream is = jar.getInputStream(entry);
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
