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

import java.applet.Applet;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.AccessControlException;

import com.jogamp.common.net.Uri;
import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.IOUtil;

/**
 * Applet: Provoke AccessControlException while writing to file!
 */
@SuppressWarnings("serial")
public class Applet01 extends Applet {
    static final String java_io_tmpdir_propkey = "java.io.tmpdir";
    static final String java_home_propkey = "java.home";
    static final String os_name_propkey = "os.name";

    static final String tfilename = "test.bin" ;
    static final MachineDataInfo machine = Platform.getMachineDataInfo();
    static final int tsz = machine.pageSizeInBytes();

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

    static void testPropImpl(final String propKey, boolean isSecure) {
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
            if( null != se0 ) {
                throw new Error("AccessControlException thrown on secure property <"+propKey+">", se0);
            }
        } else {
            if( null == se0 ) {
                throw new Error("AccessControlException not thrown on secure property <"+propKey+">");
            }
        }
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
            if( null != se0 ) {
                throw new Error("AccessControlException thrown on secure temp dir", se0);
            }
        } else {
            if( null == se0 ) {
                throw new Error("AccessControlException not thrown on secure temp dir");
            }
        }
    }

    private void testWriteFile() {
        AccessControlException sec01 = null;
        try {
            final File tmp = IOUtil.getTempDir(true);
            System.err.println("Temp: "+tmp);
            final byte[] orig = new byte[tsz];
            final File tfile = new File(tmp, tfilename);
            final OutputStream tout = new BufferedOutputStream(new FileOutputStream(tfile));
            for(int i=0; i<tsz; i++) {
                final byte b = (byte) (i%256);
                orig[i] = b;
                tout.write(b);
            }
            tout.close();
        } catch (final IOException ioe) {
            ioe.printStackTrace();
        } catch (final AccessControlException ace) {
            // GOOD!
            sec01 = ace;
            System.err.println("Expected:"+ace.getMessage());
        }
        if( !usesSecurityManager ) {
            if( null != sec01 ) {
                throw new Error("SecurityException thrown on writing to temp", sec01);
            }
        } else {
            if( null == sec01 ) {
                throw new Error("SecurityException not thrown on writing to temp");
            }
        }
    }

    private void testOpenLibrary(final boolean global) throws URISyntaxException {
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
            Exception sec01 = null;
            try {
                final NativeLibrary nlib = NativeLibrary.open(absLib.toFile().getPath(), cl);
                System.err.println("NativeLibrary: "+nlib);
            } catch (final SecurityException e) {
                sec01 = e;
                if( usesSecurityManager ) {
                    System.err.println("Expected exception for loading native library");
                    System.err.println("Message: "+sec01.getMessage());
                } else {
                    System.err.println("Unexpected exception for loading native library");
                    sec01.printStackTrace();
                }
            }
            if( !usesSecurityManager ) {
                if( null != sec01 ) {
                    throw new Error("SecurityException thrown on loading native library", sec01);
                }
            } else {
                if( null == sec01 ) {
                    throw new Error("SecurityException not thrown on loading native library");
                }
            }
        } else {
            System.err.println("No library found");
        }
    }

    public void init() {

    }

    public void start() {
        Platform.initSingleton();

        {
            testPropImpl(os_name_propkey, true);
        }
        System.err.println("p0: OK");
        {
            testPropImpl(java_home_propkey, false);
        }
        System.err.println("p1: OK");
        {
            testPropImpl(java_io_tmpdir_propkey, false);
        }
        System.err.println("p2: OK");
        {
            testTempDirImpl(false);
        }
        System.err.println("temp0: OK");

        testWriteFile();
        System.err.println("writeFile: OK");

        try {
            testOpenLibrary(true);
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
        System.err.println("lib0: OK");
    }

    public void stop() {

    }
}
