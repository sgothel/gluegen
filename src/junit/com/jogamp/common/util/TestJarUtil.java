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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.net.URIDumpUtil;
import com.jogamp.common.net.Uri;
import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.util.cache.TempCacheReg;
import com.jogamp.common.util.cache.TempFileCache;
import com.jogamp.common.util.cache.TempJarCache;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestJarUtil extends SingletonJunitCase {
    static TempFileCache fileCache;

    @BeforeClass
    public static void init() {
        if(AndroidVersion.isAvailable) {
            // ClassLoader -> JarURL doesn't work w/ Dalvik
            setTestSupported(false);
            // we allow basic TempFileCache initialization (test) ..
        }
        // may already been initialized by other test
        // Assert.assertFalse(TempCacheReg.isTempFileCacheUsed());
        Assert.assertTrue(TempFileCache.initSingleton());
        Assert.assertTrue(TempCacheReg.isTempFileCacheUsed());

        fileCache = new TempFileCache();
        Assert.assertTrue(fileCache.isValid());
        System.err.println("tmp dir: "+fileCache.getTempDir());
    }

    static class TestClassLoader extends URLClassLoader {
        public TestClassLoader(final URL[] urls) {
            super(urls);
        }
        public TestClassLoader(final URL[] urls, final ClassLoader parent) {
            super(urls, parent);
        }
    }

    void validateJarFile(final JarFile jarFile) throws IllegalArgumentException, IOException {
        Assert.assertNotNull(jarFile);
        Assert.assertTrue("jarFile has zero entries: "+jarFile, jarFile.size()>0);
        final Enumeration<JarEntry> entries = jarFile.entries();
        System.err.println("Entries of "+jarFile.getName()+": ");
        int i = 0;
        while(entries.hasMoreElements()) {
            System.err.println(i+": "+entries.nextElement().getName());
            i++;
        }
    }

    void validateJarFileURL(final Uri jarFileURI) throws IllegalArgumentException, IOException, URISyntaxException {
        Assert.assertNotNull(jarFileURI);
        final URL jarFileURL = jarFileURI.toURL();
        final URLConnection aURLc = jarFileURL.openConnection();
        Assert.assertTrue("jarFileURI/URL has zero content: "+jarFileURL, aURLc.getContentLength()>0);
        System.err.println("URLConnection: "+aURLc);
        Assert.assertTrue("Not a JarURLConnection: "+aURLc, (aURLc instanceof JarURLConnection) );
        final JarURLConnection jURLc = (JarURLConnection) aURLc;
        final JarFile jarFile = jURLc.getJarFile();
        validateJarFile(jarFile);
    }

    void validateJarUtil(final String expJarName, final String clazzBinName, final ClassLoader cl) throws IllegalArgumentException, IOException, URISyntaxException {
        final Uri.Encoded expJarNameE = Uri.Encoded.cast(expJarName);
        final Uri.Encoded jarName= JarUtil.getJarBasename(clazzBinName, cl);
        Assert.assertNotNull(jarName);
        Assert.assertEquals(expJarNameE, jarName);

        final Uri jarUri = JarUtil.getJarUri(clazzBinName, cl);
        Assert.assertNotNull(jarUri);
        System.err.println("1 - jarUri:");
        URIDumpUtil.showUri(jarUri);

        final Uri jarSubUri = jarUri.getContainedUri();
        Assert.assertNotNull(jarSubUri);
        System.err.println("2 - jarSubUri:");
        URIDumpUtil.showUri(jarSubUri);

        final URL jarSubURL= jarSubUri.toURL();
        final URLConnection urlConn = jarSubURL.openConnection();
        Assert.assertTrue("jarSubURL has zero content: "+jarSubURL, urlConn.getContentLength()>0);
        System.err.println("URLConnection of jarSubURL: "+urlConn);

        final Uri jarFileURL = JarUtil.getJarFileUri(clazzBinName, cl);
        validateJarFileURL(jarFileURL);

        final JarFile jarFile = JarUtil.getJarFile(clazzBinName, cl);
        validateJarFile(jarFile);
    }

    @Test
    public void testJarUtilFlat01() throws IOException, IllegalArgumentException, URISyntaxException {
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        validateJarUtil("TestJarsInJar.jar", "ClassInJar0", this.getClass().getClassLoader());
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    @Test
    public void testJarUtilJarInJar01() throws IOException, ClassNotFoundException, IllegalArgumentException, URISyntaxException {
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        Assert.assertTrue(TempJarCache.initSingleton());
        Assert.assertTrue(TempCacheReg.isTempJarCacheUsed());
        Assert.assertTrue(TempJarCache.isInitialized());

        final ClassLoader rootCL = this.getClass().getClassLoader();

        // Get containing JAR file "TestJarsInJar.jar" and add it to the TempJarCache
        TempJarCache.addAll(GlueGenVersion.class, JarUtil.getJarFileUri("ClassInJar0", rootCL));

        // Fetch and load the contained "ClassInJar1.jar"
        final URL ClassInJar1_jarFileURL = JarUtil.getJarFileUri(TempJarCache.getResourceUri("ClassInJar1.jar")).toURL();
        final ClassLoader cl = new URLClassLoader(new URL[] { ClassInJar1_jarFileURL }, rootCL);
        Assert.assertNotNull(cl);
        validateJarUtil("ClassInJar1.jar", "ClassInJar1", cl);
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    @Test
    public void testJarUtilJarInJar02() throws IOException, ClassNotFoundException, IllegalArgumentException, URISyntaxException {
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        Assert.assertTrue(TempJarCache.initSingleton());
        Assert.assertTrue(TempCacheReg.isTempJarCacheUsed());
        Assert.assertTrue(TempJarCache.isInitialized());

        final ClassLoader rootCL = this.getClass().getClassLoader();

        // Get containing JAR file "TestJarsInJar.jar" and add it to the TempJarCache
        TempJarCache.addAll(GlueGenVersion.class, JarUtil.getJarFileUri("ClassInJar0", rootCL));

        // Fetch and load the contained "ClassInJar1.jar"
        final URL ClassInJar2_jarFileURL = JarUtil.getJarFileUri(TempJarCache.getResourceUri("sub/ClassInJar2.jar")).toURL();
        final ClassLoader cl = new URLClassLoader(new URL[] { ClassInJar2_jarFileURL }, rootCL);
        Assert.assertNotNull(cl);
        validateJarUtil("ClassInJar2.jar", "ClassInJar2", cl);
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    /**
     * Tests JarUtil's ability to resolve non-JAR URLs with a custom resolver. Meant to be used
     * in cases like an OSGi plugin, where all classes are loaded with custom classloaders and
     * therefore return URLs that don't start with "jar:". Adapted from test 02 above.
     * @throws URISyntaxException
     * @throws IllegalArgumentException
     */
    @Test
    public void testJarUtilJarInJar03() throws IOException, ClassNotFoundException, IllegalArgumentException, URISyntaxException {
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        Assert.assertTrue(TempJarCache.initSingleton());
        Assert.assertTrue(TempCacheReg.isTempJarCacheUsed());
        Assert.assertTrue(TempJarCache.isInitialized());

        /** This classloader mimics what OSGi's does -- it takes jar: URLs and makes them into bundleresource: URLs
         * where the JAR is not directly accessible anymore. Here I leave the JAR name at the end of the URL so I can
         * retrieve it later in the resolver, but OSGi obscures it completely and returns URLs like
         * "bundleresource:4.fwk1990213994:1/Something.class" where the JAR name not present. */
        class CustomClassLoader extends ClassLoader {
            CustomClassLoader() {
                super(TestJarUtil.this.getClass().getClassLoader());
            }

            /** Override normal method to return un-resolvable URL. */
            public URL getResource(final String name) {
                final URL url = super.getResource(name);
                if(url == null)
                    return(null);
                URL urlReturn = null;
                try {
                    // numbers to mimic OSGi -- can be anything
                    urlReturn = new URL("bundleresource", "4.fwk1990213994", 1, url.getFile(),
                        new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(final URL u) throws IOException {
                                return null;
                            }
                        });
                } catch(final MalformedURLException e) {
                    // shouldn't happen, since I create the URL correctly above
                    Assert.assertTrue(false);
                }
                return urlReturn;
            }
        };

        /* This resolver converts bundleresource: URLs back into jar: URLs. OSGi does this by consulting
         * opaque bundle data inside its custom classloader to find the stored JAR path; we do it here
         * by simply retrieving the JAR name from where we left it at the end of the URL. */
        JarUtil.setResolver( new JarUtil.Resolver() {
            public URL resolve( final URL url ) {
                if( url.getProtocol().equals("bundleresource") ) {
                    try {
                        return new URL( Uri.JAR_SCHEME, "", url.getFile() );
                    } catch(final MalformedURLException e) {
                        return url;
                    }
                } else {
                    return url;
                }
            }
        } );

        final ClassLoader rootCL = new CustomClassLoader();

        // Get containing JAR file "TestJarsInJar.jar" and add it to the TempJarCache
        TempJarCache.addAll(GlueGenVersion.class, JarUtil.getJarFileUri("ClassInJar0", rootCL));

        // Fetch and load the contained "ClassInJar1.jar"
        final URL ClassInJar2_jarFileURL = JarUtil.getJarFileUri(TempJarCache.getResourceUri("sub/ClassInJar2.jar")).toURL();
        final ClassLoader cl = new URLClassLoader(new URL[] { ClassInJar2_jarFileURL }, rootCL);
        Assert.assertNotNull(cl);
        validateJarUtil("ClassInJar2.jar", "ClassInJar2", cl);
        System.err.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestJarUtil.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
