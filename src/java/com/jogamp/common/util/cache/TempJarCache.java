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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.JarUtil;

public class TempJarCache {
    // A HashMap of native libraries that can be loaded with System.load()
    // The key is the string name of the library as passed into the loadLibrary
    // call; it is the file name without the directory or the platform-dependent
    // library prefix and suffix. The value is the absolute path name to the
    // unpacked library file in nativeTmpDir.
    private static Map<String, String> nativeLibMap;

    // Set of native jar files added
    private static Set<JarFile> nativeLibJars;
    private static Set<JarFile> classFileJars;
    private static Set<JarFile> resourceFileJars;

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
                        nativeLibJars = new HashSet<JarFile>();
                        classFileJars = new HashSet<JarFile>();
                        resourceFileJars = new HashSet<JarFile>();
                    }
                }
            }
        }
        return !staticInitError;
    }
    
    /**
     * 
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
    
    public static boolean contains(JarFile jarFile) throws IOException {
        checkInitialized();
        return nativeLibJars.contains(jarFile);
    }    

    /**
     * Adds native libraries, if not yet added.
     * 
     * @param certClass if class is certified, the JarFile entries needs to have the same certificate 
     * @param jarFile
     * 
     * @return
     * @throws IOException
     * @throws SecurityException
     */
    public static final boolean addNativeLibs(Class<?> certClass, JarFile jarFile) throws IOException, SecurityException {        
        checkInitialized();
        if(!nativeLibJars.contains(jarFile)) {
            validateCertificates(certClass, jarFile);
            JarUtil.extract(tmpFileCache.getTempDir(), nativeLibMap, jarFile, 
                            true, false, false); 
            nativeLibJars.add(jarFile);
            return true;
        }
        return false;
    }
    
    /**
     * Adds native classes, if not yet added.
     * 
     * TODO class access pending
     * needs Classloader.defineClass(..) access, ie. own derivation - will do when needed ..
     * 
     * @param certClass if class is certified, the JarFile entries needs to have the same certificate 
     * @param jarFile
     *  
     * @return
     * @throws IOException
     * @throws SecurityException
     */
    public static final boolean addClasses(Class<?> certClass, JarFile jarFile) throws IOException, SecurityException {
        checkInitialized();
        if(!classFileJars.contains(jarFile)) {
            validateCertificates(certClass, jarFile);
            JarUtil.extract(tmpFileCache.getTempDir(), null, jarFile, 
                            false, true, false); 
            classFileJars.add(jarFile);
            return true;
        }
        return false;
    }
    
    /**
     * Adds native resources, if not yet added.
     * 
     * @param certClass if class is certified, the JarFile entries needs to have the same certificate 
     * @param jarFile
     * 
     * @return
     * @throws IOException
     * @throws SecurityException
     */
    public static final boolean addResources(Class<?> certClass, JarFile jarFile) throws IOException, SecurityException {        
        checkInitialized();
        if(!resourceFileJars.contains(jarFile)) {
            validateCertificates(certClass, jarFile);
            JarUtil.extract(tmpFileCache.getTempDir(), null, jarFile, 
                            false, false, true); 
            resourceFileJars.add(jarFile);
            return true;
        }
        return false;
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
     *  
     * @return
     * @throws IOException
     * @throws SecurityException
     */
    public static final boolean addAll(Class<?> certClass, JarFile jarFile) throws IOException, SecurityException {
        checkInitialized();
        if(!nativeLibJars.contains(jarFile) || 
           !classFileJars.contains(jarFile) || 
           !resourceFileJars.contains(jarFile)) {
            final boolean extractNativeLibraries = !nativeLibJars.contains(jarFile);
            final boolean extractClassFiles = !classFileJars.contains(jarFile);
            final boolean extractOtherFiles = !resourceFileJars.contains(jarFile);
            validateCertificates(certClass, jarFile);
            JarUtil.extract(tmpFileCache.getTempDir(), nativeLibMap, jarFile, 
                            extractNativeLibraries, extractClassFiles, extractOtherFiles);
            if(extractNativeLibraries) {
                nativeLibJars.add(jarFile);
            }
            if(extractClassFiles) {
                classFileJars.add(jarFile);
            }
            if(extractOtherFiles) {
                resourceFileJars.add(jarFile);
            }
            return true;
        }
        return false;
    }
    
    public static final String findLibrary(String libName) {
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
    
    public static final String findResource(String name) {
        checkInitialized();
        final File f = new File(tmpFileCache.getTempDir(), name);
        if(f.exists()) {
            return f.getAbsolutePath();
        }
        return null;
    }
    
    public static final URL getResource(String name) throws MalformedURLException {
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
    public static final boolean bootstrapNativeLib(Class<?> certClass, String libBaseName, JarFile jarFile) 
            throws IOException, SecurityException {
        checkInitialized();
        if(!nativeLibJars.contains(jarFile) && !nativeLibMap.containsKey(libBaseName) ) {                    
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
                            int count;
                            if ((count = in.read(buf)) == -1) { break; }
                            out.write(buf, 0, count);
                            numBytes += count;
                        }
                    } finally { in.close(); out.close(); }
                    if (numBytes>0) {
                        nativeLibMap.put(libBaseName, destFile.getAbsolutePath());
                        nativeLibJars.add(jarFile);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private static void validateCertificates(Class<?> certClass, JarFile jarFile) throws IOException, SecurityException {
        if(null == certClass) {
            throw new IllegalArgumentException("certClass is null");
        }
        final Certificate[] rootCerts = 
                certClass.getProtectionDomain().getCodeSource().getCertificates();
        if( null != rootCerts && rootCerts.length>0 ) {
            // Only validate the jarFile's certs with ours, if we have any.
            // Otherwise we may run uncertified JARs (application).
            // In case one tries to run uncertified JARs, the wrapping applet/JNLP
            // SecurityManager will kick in and throw a SecurityException.
            JarUtil.validateCertificates(rootCerts, jarFile);
        }                        
    }    
}
