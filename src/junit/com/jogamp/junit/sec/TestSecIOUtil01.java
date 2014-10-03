/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.junit.sec;

import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.net.Uri;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.IOUtil;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSecIOUtil01 extends SingletonJunitCase {
    static final String java_io_tmpdir_propkey = "java.io.tmpdir";
    static final String java_home_propkey = "java.home";
    static final String os_name_propkey = "os.name";
    static final boolean usesSecurityManager;

    static {
        if( null == System.getSecurityManager() ) {
            usesSecurityManager = false;
            System.err.println("No SecurityManager Installed");
        } else {
            usesSecurityManager = true;
            System.err.println("SecurityManager Already Installed");
        }
    }

    @BeforeClass
    public static void setup() throws IOException {
        Platform.initSingleton();
    }

    static void testPropImpl01(final String propKey, boolean isSecure) {
        isSecure |= !usesSecurityManager;

        Exception se0 = null;
        try {
            final String p0 = System.getProperty(propKey);
            System.err.println(propKey+": "+p0);
        } catch (final AccessControlException e) {
            se0 = e;
            if( !isSecure ) {
                System.err.println("Expected exception for insecure property <"+propKey+">");
                System.err.println("Message: "+se0.getMessage());
            } else {
                System.err.println("Unexpected exception for secure property <"+propKey+">");
                se0.printStackTrace();
            }
        }
        if( isSecure ) {
            Assert.assertNull("AccessControlException thrown on secure property <"+propKey+">", se0);
        } else {
            Assert.assertNotNull("AccessControlException not thrown on insecure property <"+propKey+">", se0);
        }
    }

    @Test
    public void testProp00_Temp() {
        testPropImpl01(os_name_propkey, true);
    }

    @Test
    public void testProp01_Temp() {
        testPropImpl01(java_home_propkey, false);
    }

    @Test
    public void testProp02_Temp() {
        testPropImpl01(java_io_tmpdir_propkey, false);
    }

    static void testTempDirImpl(boolean isSecure) {
        isSecure |= !usesSecurityManager;

        Exception se0 = null;
        try {
            final File tmp = IOUtil.getTempDir(true);
            System.err.println("Temp: "+tmp);
        } catch (final AccessControlException e) {
            se0 = e;
            if( !isSecure ) {
                System.err.println("Expected exception for insecure temp dir");
                System.err.println("Message: "+se0.getMessage());
            } else {
                System.err.println("Unexpected exception for secure temp dir");
                se0.printStackTrace();
            }
        } catch (final SecurityException e) {
            se0 = e;
            if( !isSecure ) {
                System.err.println("Expected exception for insecure temp dir (2)");
                System.err.println("Message: "+se0.getMessage());
            } else {
                System.err.println("Unexpected exception for secure temp dir (2)");
                se0.printStackTrace();
            }
        } catch (final IOException e) {
            throw new RuntimeException(e); // oops
        }
        if( isSecure ) {
            Assert.assertNull("AccessControlException thrown on secure temp dir", se0);
        } else {
            Assert.assertNotNull("AccessControlException not thrown on insecure temp dir", se0);
        }
    }

    @Test
    public void testTempDir00() {
        testTempDirImpl(false);
    }

    private NativeLibrary openLibraryImpl(final boolean global) throws URISyntaxException {
        final ClassLoader cl = getClass().getClassLoader();
        System.err.println("CL "+cl);

        String libBaseName = null;
        final Class<?> clazz = this.getClass();
        Uri libUri = null;
        try {
            libUri = Uri.valueOf(clazz.getResource("/libtest1.so"));
        } catch (final URISyntaxException e2) {
            // not found .. OK
        }
        if( null != libUri ) {
            libBaseName = "libtest1.so";
        } else {
            try {
                libUri = Uri.valueOf(clazz.getResource("/test1.dll"));
                if( null != libUri ) {
                    libBaseName = "test1.dll";
                }
            } catch (final URISyntaxException e) {
                // not found
            }
        }
        System.err.println("Untrusted Library (URL): "+libUri);

        if( null != libUri ) {
            Uri libDir1 = libUri.getContainedUri();
            System.err.println("libDir1.1: "+libDir1);
            libDir1= libDir1.getParent();
            System.err.println("libDir1.2: "+libDir1);
            System.err.println("Untrusted Library Dir1 (abs): "+libDir1);
            final Uri absLib = libDir1.concat(Uri.Encoded.cast("natives/" + libBaseName));
            Exception se0 = null;
            NativeLibrary nlib = null;
            try {
                nlib = NativeLibrary.open(absLib.toFile().getPath(), cl);
                System.err.println("NativeLibrary: "+nlib);
            } catch (final SecurityException e) {
                se0 = e;
                if( usesSecurityManager ) {
                    System.err.println("Expected exception for loading native library");
                    System.err.println("Message: "+se0.getMessage());
                } else {
                    System.err.println("Unexpected exception for loading native library");
                    se0.printStackTrace();
                }
            }
            if( !usesSecurityManager ) {
                Assert.assertNull("SecurityException thrown on loading native library", se0);
            } else {
                Assert.assertNotNull("SecurityException not thrown on loading native library", se0);
            }
            return nlib;
        } else {
            System.err.println("No library found");
            return null;
        }

    }

    public void testOpenLibrary() throws URISyntaxException {
        final NativeLibrary nlib = openLibraryImpl(true);
        if( null != nlib ) {
            nlib.close();
        }
    }

    public static void main(final String args[]) throws IOException, URISyntaxException {
        TestSecIOUtil01.setup();

        final TestSecIOUtil01 aa = new TestSecIOUtil01();
        aa.testProp00_Temp();
        aa.testProp01_Temp();
        aa.testProp02_Temp();
        aa.testTempDir00();
        aa.testOpenLibrary();
    }

}
