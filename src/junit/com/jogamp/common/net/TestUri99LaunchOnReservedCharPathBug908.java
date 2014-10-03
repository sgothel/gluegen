/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

package com.jogamp.common.net;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.junit.util.SingletonJunitCase;
import com.jogamp.junit.util.MiscUtils;

/**
 * Bug 908: Automated test, launching GlueGen jar file from an <i>odd pathname</i>.
 * <p>
 * The currently used jar folder is copied into [1]
 * <pre>
 *    [1] build/test/build/TestNetIOURIReservedCharsBug908/test01TempJarCacheOddPath/A\$-B\^-C~-D#-E\]-F\[-öä
 *    [2] build/test/build/TestNetIOURIReservedCharsBug908/test01TempJarCacheOddPath/A\$-B\^-C~-D#-E\]-F\[-öä/gluegen-rt.jar
 * </pre>
 * A ClassLoader w/ the URL [2] is used to issue Platform.initSingleton(),
 * i.e. issues a whole initialization sequence w/ native jar loading in the new ClassPath.
 * </p>
 * <p>
 * The manual test below on the created odd folder [1] has also succeeded:
 * <pre>
 * java \
 *     -Djogamp.debug.IOUtil -Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache \
 *     -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache \
 *     -cp ../build-x86_64/test/build/TestNetIOURIReservedCharsBug908/test01TempJarCacheOddPath/A\$-B\^-C~-D#-E\]-F\[-öä/gluegen-rt.jar \
 *     com.jogamp.common.GlueGenVersion
 * </pre>
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUri99LaunchOnReservedCharPathBug908 extends SingletonJunitCase {
    static class TestClassLoader extends URLClassLoader {
        public TestClassLoader(final URL[] urls) {
            super(urls);
        }
        public TestClassLoader(final URL[] urls, final ClassLoader parent) {
            super(urls, parent);
        }
    }


    @Test
    public void test00TempJarCacheSimplePath() throws IOException, IllegalArgumentException, URISyntaxException {
        testTempJarCacheOddJarPathImpl("simpletons/");
    }

    @Test
    public void test01TempJarCacheOddPath() throws IOException, IllegalArgumentException, URISyntaxException {
        // Bug 908, issues w/ windows file path char: $ ^ ~ # [ ]
        testTempJarCacheOddJarPathImpl("A$-B^-C~-D#-E]-F[-öä/");
                                    // "A$-B%5E-C~-D#-E]-F[-%C3%B6%C3%A4/");    <- Firefox URI encoding! '#' -> [1]
                                    //   "A$-B%5E-C~-D%23-E]-F[-%C3%B6%C3%A4/"); <- '[' ']' -> [2]
                                    // "A$-B%5E-C~-D%23-E%5D-F%5B-%C3%B6%C3%A4/");
        /**
         * [1] '#'
            java.lang.IllegalArgumentException: URI has a fragment component
                   at java.io.File.<init>(Unknown Source)
                   at com.jogamp.common.net.TestNetIOURIReservedCharsBug908.testTempJarCacheOddJarPathImpl(TestNetIOURIReservedCharsBug908.java:101)
           [2] '[' ']'
            java.net.URISyntaxException: Illegal character in path at index 137: file:/usr/local/projects/JOGL/gluegen/build-x86_64/test/build/TestNetIOURIReservedCharsBug908/test01TempJarCacheOddPath/A$-B%5E-C~-D%23-E]-F[-%C3%B6%C3%A4/
                    at java.net.URI$Parser.fail(Unknown Source)
                    at java.net.URI$Parser.checkChars(Unknown Source)
                    at java.net.URI$Parser.parseHierarchical(Unknown Source)
                    at java.net.URI$Parser.parse(Unknown Source)
                    at java.net.URI.<init>(Unknown Source)
                    at com.jogamp.common.net.TestNetIOURIReservedCharsBug908.testTempJarCacheOddJarPathImpl(TestNetIOURIReservedCharsBug908.java:106)

         */

    }
    private void testTempJarCacheOddJarPathImpl(final String subPathUTF) throws IOException, IllegalArgumentException, URISyntaxException {
        if(AndroidVersion.isAvailable) { System.err.println("n/a on Android"); return; }

        final Uri.Encoded subPathEncoded = new Uri.Encoded(subPathUTF, Uri.PATH_LEGAL);
        final String reservedCharPathUnencoded = "test/build/"+getClass().getSimpleName()+"/"+getTestMethodName()+"/"+subPathUTF;
        final Uri.Encoded reservedCharPathEncoded = Uri.Encoded.cast("test/build/"+getClass().getSimpleName()+"/"+getTestMethodName()+"/").concat(subPathEncoded);

        System.err.println("0 Unencoded:             "+reservedCharPathUnencoded);
        System.err.println("0 Encoded:               "+reservedCharPathEncoded);

        // jar:file:/dir1/dir2/gluegen-rt.jar!/
        final Uri jarFileURI = JarUtil.getJarFileUri(Platform.class.getName(), getClass().getClassLoader());
        System.err.println("1 jarFileURI:            "+jarFileURI.toString());
        // gluegen-rt.jar
        final Uri.Encoded jarBasename = JarUtil.getJarBasename(jarFileURI);
        System.err.println("2 jarBasename:           "+jarBasename);

        // file:/dir1/build/gluegen-rt.jar
        final Uri fileURI = jarFileURI.getContainedUri();
        System.err.println("3 fileURI:               "+fileURI.toString());
        // file:/dir1/build/
        final Uri fileFolderURI = fileURI.getParent();
        System.err.println("4 fileFolderURI:         "+fileFolderURI.toString());
        // file:/dir1/build/test/build/A$-B^-C~-D#-E]-F[/
        final Uri fileNewFolderURI = fileFolderURI.concat(reservedCharPathEncoded);
        System.err.println("5 fileNewFolderURI:      "+fileNewFolderURI.toString());

        final File srcFolder = fileFolderURI.toFile();
        final File dstFolder = fileNewFolderURI.toFile();
        System.err.println("6 srcFolder:             "+srcFolder.toString());
        System.err.println("7 dstFolder:             "+dstFolder.toString());
        try {
            final MiscUtils.CopyStats copyStats = MiscUtils.copy(srcFolder, dstFolder, 1, true);
            copyStats.dump("Copy ", true);
            Assert.assertEquals(1, copyStats.totalFolders);
            Assert.assertTrue(copyStats.totalBytes > 0);
            Assert.assertEquals(0, copyStats.currentDepth);

            final URI jarFileNewFolderURI = new URI(fileNewFolderURI.toString()+jarBasename);
            System.err.println("8 jarFileNewFolderURI:   "+jarFileNewFolderURI.toString());

            final URL[] urls = new URL[] { jarFileNewFolderURI.toURL() };
            System.err.println("url: "+urls[0]);

            final ClassLoader cl = new TestClassLoader(urls, null);
            ReflectionUtil.callStaticMethod(Platform.class.getName(), "initSingleton", null, null, cl);
        } finally {
            // cleanup ? Skip this for now ..
            // dstFolder.delete();
        }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestUri99LaunchOnReservedCharPathBug908.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
