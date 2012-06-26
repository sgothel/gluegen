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
package com.jogamp.common.util.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jogamp.common.Debug;

import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.SecurityUtil;

public class TempJarCache {
    private static final boolean DEBUG = Debug.debug("TempJarCache");
    
    // A HashMap of native libraries that can be loaded with System.load()
    // The key is the string name of the library as passed into the loadLibrary
    // call; it is the file name without the directory or the platform-dependent
    // library prefix and suffix. The value is the absolute path name to the
    // unpacked library file in nativeTmpDir.
    private static Map<String, String> nativeLibMap;

    public enum LoadState {
        LOOKED_UP, LOADED;
        
        public boolean compliesWith(LoadState o2) {            
            return null != o2 ? compareTo(o2) >= 0 : false;
        }
    }
    private static boolean testLoadState(LoadState has, LoadState exp) {
        if(null == has) {
            return null == exp;
        }
        return has.compliesWith(exp);
    }
    
    // Set of jar files added
    private static Map<URL, LoadState> nativeLibJars;
    private static Map<URL, LoadState> classFileJars;
    private static Map<URL, LoadState> resourceFileJars;

    private static TempFileCache tmpFileCache;
    
    private static boolean staticInitError = false;
    private static volatile boolean isInit = false;
    
    /**
     * Documented way to kick off static initialization.
     * 
     * @return true is static initialization was successful
     */
    public static boolean initSingleton() {
        if (!isInit) { // volatile: ok
            synchronized (TempJarCache.class) {
                if (!isInit) {
                    isInit = true;
                    staticInitError = !TempFileCache.initSingleton();
            
                    if(!staticInitError) {
                        tmpFileCache = new TempFileCache();
                        staticInitError = !tmpFileCache.isValid(); 
                    }
                    
                    if(!staticInitError) {
                        // Initialize the collections of resources
                        nativeLibMap = new HashMap<String, String>();
                        nativeLibJars = new HashMap<URL, LoadState>();
                        classFileJars = new HashMap<URL, LoadState>();
                        resourceFileJars = new HashMap<URL, LoadState>();
                    }
                    if(DEBUG) {
                        System.err.println("TempJarCache.initSingleton(): ok "+(false==staticInitError)+", "+ tmpFileCache.getTempDir());
                    }
                }
            }
        }
        return !staticInitError;
    }
    
    /**
     * This is <b>not recommended</b> since the JNI libraries may still be
     * in use by the ClassLoader they are loaded via {@link System#load(String)}.
     * </p>
     * <p>
     * In JogAmp, JNI native libraries loaded and registered by {@link JNILibLoaderBase} 
     * derivations, where the native JARs might be loaded via {@link JNILibLoaderBase#addNativeJarLibs(Class, String) }. 
     * </p>
     * <p>
     * The only valid use case to shutdown the TempJarCache is at bootstrapping,
     * i.e. when no native library is guaranteed to be loaded. This could be useful
     * if bootstrapping needs to find the proper native library type.
     * </p> 
     *
    public static void shutdown() {
        if (isInit) { // volatile: ok
            synchronized (TempJarCache.class) {
                if (isInit) {
                    if(DEBUG) {
                        System.err.println("TempJarCache.shutdown(): real "+(false==staticInitError)+", "+ tmpFileCache.getTempDir());
                    }
                    isInit = false;
                    if(!staticInitError) {
                        nativeLibMap.clear();
                        nativeLibMap = null;
                        nativeLibJars.clear();
                        nativeLibJars = null;
                        classFileJars.clear();
                        classFileJars = null;
                        resourceFileJars.clear();
                        resourceFileJars = null;
                        
                        tmpFileCache.destroy();
                        tmpFileCache = null;
                    }
                }
            }
        }
    } */
    
    /**
     * @return true if this class has been properly initialized, ie. is in use, otherwise false.
     */
    public static boolean isInitialized() {
        return isInit && !staticInitError;
    }
    
    /* package */ static void checkInitialized() {
        if(!isInit) {
            throw new RuntimeException("initSingleton() has to be called first.");
        }
    }
    
    public static TempFileCache getTempFileCache() {
        checkInitialized();
        return tmpFileCache;
    }
    
    public synchronized static boolean checkNativeLibs(URL jarURL, LoadState exp) throws IOException {
        checkInitialized();
        if(null == jarURL) {
            throw new IllegalArgumentException("jarURL is null");
        }
        return testLoadState(nativeLibJars.get(jarURL), exp);
    }    

    public synchronized static boolean checkClasses(URL jarURL, LoadState exp) throws IOException {
        checkInitialized();
        if(null == jarURL) {
            throw new IllegalArgumentException("jarURL is null");
        }
        return testLoadState(classFileJars.get(jarURL), exp);
    }    

    public synchronized static boolean checkResources(URL jarURL, LoadState exp) throws IOException {
        checkInitialized();
        if(null == jarURL) {
            throw new IllegalArgumentException("jarURL is null");
        }
        return testLoadState(resourceFileJars.get(jarURL), exp);
    }
    
    /**
     * Adds native libraries, if not yet added.
     * 
     * @param certClass if class is certified, the JarFile entries needs to have the same certificate 
     * @param jarURL
     * @throws IOException if the <code>jarURL</code> could not be loaded or a previous load attempt failed
     * @throws SecurityException
     */
    public synchronized static final void addNativeLibs(Class<?> certClass, URL jarURL) throws IOException, SecurityException {        
        final LoadState nativeLibJarsLS = nativeLibJars.get(jarURL);
        if( !testLoadState(nativeLibJarsLS, LoadState.LOOKED_UP) ) { 
            nativeLibJars.put(jarURL, LoadState.LOOKED_UP);
            final JarFile jarFile = JarUtil.getJarFile(jarURL);
            if(DEBUG) {
                System.err.println("TempJarCache: addNativeLibs: "+jarURL+": nativeJar "+jarFile.getName());
            }
            validateCertificates(certClass, jarFile);
            JarUtil.extract(tmpFileCache.getTempDir(), nativeLibMap, jarFile, 
                            true, false, false); 
            nativeLibJars.put(jarURL, LoadState.LOADED);
        } else if( !testLoadState(nativeLibJarsLS, LoadState.LOADED) ) {
            throw new IOException("TempJarCache: addNativeLibs: "+jarURL+", previous load attempt failed");
        }
    }
    
    /**
     * Adds native classes, if not yet added.
     * 
     * TODO class access pending
     * needs Classloader.defineClass(..) access, ie. own derivation - will do when needed ..
     * 
     * @param certClass if class is certified, the JarFile entries needs to have the same certificate 
     * @param jarFile
     * @throws IOException if the <code>jarURL</code> could not be loaded or a previous load attempt failed
     * @throws SecurityException
     */
    public synchronized static final void addClasses(Class<?> certClass, URL jarURL) throws IOException, SecurityException {
        final LoadState classFileJarsLS = classFileJars.get(jarURL);
        if( !testLoadState(classFileJarsLS, LoadState.LOOKED_UP) ) { 
            classFileJars.put(jarURL, LoadState.LOOKED_UP);
            final JarFile jarFile = JarUtil.getJarFile(jarURL);
            if(DEBUG) {
                System.err.println("TempJarCache: addClasses: "+jarURL+": nativeJar "+jarFile.getName());
            }
            validateCertificates(certClass, jarFile);
            JarUtil.extract(tmpFileCache.getTempDir(), null, jarFile, 
                            false, true, false); 
            classFileJars.put(jarURL, LoadState.LOADED);
        } else if( !testLoadState(classFileJarsLS, LoadState.LOADED) ) {
            throw new IOException("TempJarCache: addClasses: "+jarURL+", previous load attempt failed");
        }
    }
    
    /**
     * Adds native resources, if not yet added.
     * 
     * @param certClass if class is certified, the JarFile entries needs to have the same certificate 
     * @param jarFile
     * @return
     * @throws IOException if the <code>jarURL</code> could not be loaded or a previous load attempt failed
     * @throws SecurityException
     */
    public synchronized static final void addResources(Class<?> certClass, URL jarURL) throws IOException, SecurityException {        
        final LoadState resourceFileJarsLS = resourceFileJars.get(jarURL);
        if( !testLoadState(resourceFileJarsLS, LoadState.LOOKED_UP) ) { 
            resourceFileJars.put(jarURL, LoadState.LOOKED_UP);
            final JarFile jarFile = JarUtil.getJarFile(jarURL);
            if(DEBUG) {
                System.err.println("TempJarCache: addResources: "+jarURL+": nativeJar "+jarFile.getName());
            }
            validateCertificates(certClass, jarFile);
            JarUtil.extract(tmpFileCache.getTempDir(), null, jarFile, 
                            false, false, true); 
            resourceFileJars.put(jarURL, LoadState.LOADED);
        } else if( !testLoadState(resourceFileJarsLS, LoadState.LOADED) ) {
            throw new IOException("TempJarCache: addResources: "+jarURL+", previous load attempt failed");
        }
    }
    
    /**
     * Adds all types, native libraries, class files and other files (resources)
     * if not yet added.
     *  
     * TODO class access pending
     * needs Classloader.defineClass(..) access, ie. own derivation - will do when needed ..
     * 
     * @param certClass if class is certified, the JarFile entries needs to have the same certificate 
     * @param jarFile
     * @throws IOException if the <code>jarURL</code> could not be loaded or a previous load attempt failed
     * @throws SecurityException
     */
    public synchronized static final void addAll(Class<?> certClass, URL jarURL) throws IOException, SecurityException {
        checkInitialized();
        if(null == jarURL) {
            throw new IllegalArgumentException("jarURL is null");
        }
        final LoadState nativeLibJarsLS = nativeLibJars.get(jarURL);
        final LoadState classFileJarsLS = classFileJars.get(jarURL);
        final LoadState resourceFileJarsLS = resourceFileJars.get(jarURL);
        if( !testLoadState(nativeLibJarsLS, LoadState.LOOKED_UP) || 
            !testLoadState(classFileJarsLS, LoadState.LOOKED_UP) || 
            !testLoadState(resourceFileJarsLS, LoadState.LOOKED_UP) ) {
            
            final boolean extractNativeLibraries = !testLoadState(nativeLibJarsLS, LoadState.LOADED);
            final boolean extractClassFiles = !testLoadState(classFileJarsLS, LoadState.LOADED);
            final boolean extractOtherFiles = !testLoadState(resourceFileJarsLS, LoadState.LOOKED_UP);
            
            // mark looked-up (those who are not loaded)
            if(extractNativeLibraries) {
                nativeLibJars.put(jarURL, LoadState.LOOKED_UP);
            }
            if(extractClassFiles) {
                classFileJars.put(jarURL, LoadState.LOOKED_UP);
            }
            if(extractOtherFiles) {
                resourceFileJars.put(jarURL, LoadState.LOOKED_UP);
            }
            
            final JarFile jarFile = JarUtil.getJarFile(jarURL);
            if(DEBUG) {
                System.err.println("TempJarCache: addAll: "+jarURL+": nativeJar "+jarFile.getName());
            }
            validateCertificates(certClass, jarFile);
            JarUtil.extract(tmpFileCache.getTempDir(), nativeLibMap, jarFile, 
                            extractNativeLibraries, extractClassFiles, extractOtherFiles);
            
            // mark loaded (those were just loaded)
            if(extractNativeLibraries) {
                nativeLibJars.put(jarURL, LoadState.LOADED);
            }
            if(extractClassFiles) {
                classFileJars.put(jarURL, LoadState.LOADED);
            }
            if(extractOtherFiles) {
                resourceFileJars.put(jarURL, LoadState.LOADED);
            }
        } else if( !testLoadState(nativeLibJarsLS, LoadState.LOADED) || 
                   !testLoadState(classFileJarsLS, LoadState.LOADED) || 
                   !testLoadState(resourceFileJarsLS, LoadState.LOADED) ) {
            throw new IOException("TempJarCache: addAll: "+jarURL+", previous load attempt failed");            
        }
    }
    
    public synchronized static final String findLibrary(String libName) {
        checkInitialized();
        // try with mapped library basename first
        String path = nativeLibMap.get(libName);
        if(null == path) {
            // if valid library name, try absolute path in temp-dir
            if(null != NativeLibrary.isValidNativeLibraryName(libName, false)) {    
                final File f = new File(tmpFileCache.getTempDir(), libName);
                if(f.exists()) {
                    path = f.getAbsolutePath();
                }
            }
        }
        return path;
    }
    
    /** TODO class access pending
     * needs Classloader.defineClass(..) access, ie. own derivation - will do when needed .. 
    public static Class<?> findClass(String name, ClassLoader cl) throws IOException, ClassFormatError {
        checkInitialized();
        final File f = new File(nativeTmpFileCache.getTempDir(), IOUtil.getClassFileName(name));
        if(f.exists()) {
            Class.forName(fname, initialize, loader)
            URL url = new URL(f.getAbsolutePath());
            byte[] b = IOUtil.copyStream2ByteArray(new BufferedInputStream( url.openStream() ));
            MyClassLoader mcl = new MyClassLoader(cl);
            return mcl.defineClass(name, b, 0, b.length);
        }
        return null;
    } */
    
    public synchronized static final String findResource(String name) {
        checkInitialized();
        final File f = new File(tmpFileCache.getTempDir(), name);
        if(f.exists()) {
            return f.getAbsolutePath();
        }
        return null;
    }
    
    public synchronized static final URL getResource(String name) throws MalformedURLException {
        checkInitialized();
        final File f = new File(tmpFileCache.getTempDir(), name);
        if(f.exists()) {
            return IOUtil.toURLSimple(f);
        }
        return null;
    }
    
    
    /**
     * Bootstrapping version extracting the JAR files root entry containing libBaseName,
     * assuming it's a native library. This is used to get the 'gluegen-rt'
     * native library, hence bootstrapping.
     *  
     * @param certClass if class is certified, the JarFile entries needs to have the same certificate
     *  
     * @throws IOException
     * @throws SecurityException
     */
    public synchronized static final void bootstrapNativeLib(Class<?> certClass, String libBaseName, URL jarURL) 
            throws IOException, SecurityException {
        checkInitialized();
        boolean ok = false;
        int countEntries = 0;
        final LoadState nativeLibJarsLS = nativeLibJars.get(jarURL);
        if( !testLoadState(nativeLibJarsLS, LoadState.LOOKED_UP) && !nativeLibMap.containsKey(libBaseName) ) {
            if(DEBUG) {
                System.err.println("TempJarCache: bootstrapNativeLib(certClass: "+certClass+", libBaseName "+libBaseName+", jarURL "+jarURL+")");
            }
            nativeLibJars.put(jarURL, LoadState.LOOKED_UP);
            final JarFile jarFile = JarUtil.getJarFile(jarURL);
            if(DEBUG) {
                System.err.println("TempJarCache: bootstrapNativeLib: nativeJar "+jarFile.getName());
            }
            validateCertificates(certClass, jarFile);
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = (JarEntry) entries.nextElement();
                final String entryName = entry.getName();
    
                if( entryName.indexOf('/') == -1 &&
                    entryName.indexOf(File.separatorChar) == -1 &&
                    entryName.indexOf(libBaseName) >= 0 ) 
                {
                    final File destFile = new File(tmpFileCache.getTempDir(), entryName);
                    final InputStream in = new BufferedInputStream(jarFile.getInputStream(entry));
                    final OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
                    int numBytes = 0; 
                    try {
                        final byte[] buf = new byte[ 2048 ];
                        while (true) {
                            int countBytes;
                            if ((countBytes = in.read(buf)) == -1) { break; }
                            out.write(buf, 0, countBytes);
                            numBytes += countBytes;
                        }
                    } finally { in.close(); out.close(); }
                    if (numBytes>0) {
                        nativeLibMap.put(libBaseName, destFile.getAbsolutePath());
                        nativeLibJars.put(jarURL, LoadState.LOADED);
                        ok = true;
                        countEntries++;
                    }
                }
            }
        } else if( testLoadState(nativeLibJarsLS, LoadState.LOADED) ) {
            ok = true; // already loaded
        } else {
            throw new IOException("TempJarCache: bootstrapNativeLib: "+jarURL+", previous load attempt failed");
        }
        if(DEBUG) {
            System.err.println("TempJarCache: bootstrapNativeLib() done, count "+countEntries+", ok "+ok);
        }
    }
    
    private static void validateCertificates(Class<?> certClass, JarFile jarFile) throws IOException, SecurityException {
        if(null == certClass) {
            throw new IllegalArgumentException("certClass is null");
        }
        final Certificate[] rootCerts = SecurityUtil.getCerts(certClass);
        if( null != rootCerts ) {
            // Only validate the jarFile's certs with ours, if we have any.
            // Otherwise we may run uncertified JARs (application).
            // In case one tries to run uncertified JARs, the wrapping applet/JNLP
            // SecurityManager will kick in and throw a SecurityException.
            JarUtil.validateCertificates(rootCerts, jarFile);
            if(DEBUG) {
                System.err.println("TempJarCache: validateCertificates: OK - Matching rootCerts in given class "+certClass.getName()+", nativeJar "+jarFile.getName());
            }            
        } else if(DEBUG) {
            System.err.println("TempJarCache: validateCertificates: OK - No rootCerts in given class "+certClass.getName()+", nativeJar "+jarFile.getName());
        }
    }    
}
