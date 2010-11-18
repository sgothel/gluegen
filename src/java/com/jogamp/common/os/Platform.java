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
 
/*
 * Created on Sunday, March 28 2010 14:43
 */
package com.jogamp.common.os;

import com.jogamp.common.nio.Buffers;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Utility class for querying platform specific properties.
 * @author Michael Bien
 * @author Sven Gothel
 */
public class Platform {

    public static final boolean JAVA_SE;
    public static final boolean LITTLE_ENDIAN;
    public static final String OS;
    public static final String OS_VERSION;
    public static final String ARCH;
    public static final String JAVA_VENDOR;
    public static final String JAVA_VENDOR_URL;
    public static final String JAVA_VERSION;
    public static final String NEWLINE;

    private static final boolean is32Bit;
    private static final int pointerSizeInBits;

    static {
        
        // We don't seem to need an AccessController.doPrivileged() block
        // here as these system properties are visible even to unsigned
        // applets
        OS =  System.getProperty("os.name");
        OS_VERSION =  System.getProperty("os.version");
        ARCH = System.getProperty("os.arch");
        JAVA_VENDOR = System.getProperty("java.vendor");
        JAVA_VENDOR_URL = System.getProperty("java.vendor.url");
        JAVA_VERSION = System.getProperty("java.version");
        NEWLINE = System.getProperty("line.separator");

        JAVA_SE = initIsJavaSE();
        LITTLE_ENDIAN = initByteOrder();

        boolean libsLoaded = true;
        try{
            NativeLibrary.ensureNativeLibLoaded();
        }catch (UnsatisfiedLinkError err){
            libsLoaded = false;
        }
        
        if(libsLoaded) {
            pointerSizeInBits = getPointerSizeInBitsImpl();
        }else{
            pointerSizeInBits = -1;
        }

        is32Bit = initArch();

    }

    private Platform() {}

    private static boolean initArch() throws RuntimeException {
        if ( 32 == pointerSizeInBits || 64 == pointerSizeInBits ) {
            return 32 == pointerSizeInBits;
        }else {
            String os_lc = OS.toLowerCase();
            String arch_lc = ARCH.toLowerCase();

            if ((os_lc.startsWith("windows") && arch_lc.equals("x86")) ||
                (os_lc.startsWith("windows") && arch_lc.equals("arm")) ||
                (os_lc.startsWith("linux") && arch_lc.equals("i386")) ||
                (os_lc.startsWith("linux") && arch_lc.equals("x86")) ||
                (os_lc.startsWith("mac os_lc") && arch_lc.equals("ppc")) ||
                (os_lc.startsWith("mac os_lc") && arch_lc.equals("i386")) ||
                (os_lc.startsWith("darwin") && arch_lc.equals("ppc")) ||
                (os_lc.startsWith("darwin") && arch_lc.equals("i386")) ||
                (os_lc.startsWith("sunos_lc") && arch_lc.equals("sparc")) ||
                (os_lc.startsWith("sunos_lc") && arch_lc.equals("x86")) ||
                (os_lc.startsWith("freebsd") && arch_lc.equals("i386")) ||
                (os_lc.startsWith("hp-ux") && arch_lc.equals("pa_risc2.0"))) {
                return true;
            } else if ((os_lc.startsWith("windows") && arch_lc.equals("amd64")) ||
                      (os_lc.startsWith("linux") && arch_lc.equals("amd64")) ||
                      (os_lc.startsWith("linux") && arch_lc.equals("x86_64")) ||
                      (os_lc.startsWith("linux") && arch_lc.equals("ia64")) ||
                      (os_lc.startsWith("mac os_lc") && arch_lc.equals("x86_64")) ||
                      (os_lc.startsWith("darwin") && arch_lc.equals("x86_64")) ||
                      (os_lc.startsWith("sunos_lc") && arch_lc.equals("sparcv9")) ||
                      (os_lc.startsWith("sunos_lc") && arch_lc.equals("amd64"))) {
                return false;
            }else{
              throw new RuntimeException("Please port CPU detection (32/64 bit) to your platform (" + os_lc + "/" + arch_lc + ")");
            }
        }
    }

    private static boolean initIsJavaSE() {

        // the fast path, check property Java SE instead of traversing through the ClassLoader
        String java_runtime_name = (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              return System.getProperty("java.runtime.name");
            }
          });
        if(java_runtime_name.indexOf("Java SE") != -1) {
            return true;
        }

        // probe for classes we need on a SE environment
        try {
            Class.forName("java.nio.LongBuffer");
            Class.forName("java.nio.DoubleBuffer");
            return true;
        } catch(ClassNotFoundException ex) {
            // continue with Java SE check
        }

        return false;
    }

    private static boolean initByteOrder() {
        ByteBuffer tst_b = Buffers.newDirectByteBuffer(Buffers.SIZEOF_INT); // 32bit in native order
        IntBuffer tst_i = tst_b.asIntBuffer();
        ShortBuffer tst_s = tst_b.asShortBuffer();
        tst_i.put(0, 0x0A0B0C0D);
        return 0x0C0D == tst_s.get(0);
    }

    private static native int getPointerSizeInBitsImpl();

    
    /**
     * Returns true only if this program is running on the Java Standard Edition.
     */
    public static boolean isJavaSE() {
        return JAVA_SE;
    }

    /**
     * Returns true only if this system uses little endian byte ordering.
     */
    public static boolean isLittleEndian() {
        return LITTLE_ENDIAN;
    }

    /**
     * Returns the OS name.
     */
    public static String getOS() {
        return OS;
    }

    /**
     * Returns the OS version.
     */
    public static String getOSVersion() {
        return OS_VERSION;
    }


    /**
     * Returns the CPU architecture String.
     */
    public static String getArch() {
        return ARCH;
    }

    /**
     * Returns the JAVA.
     */
    public static String getJavaVendor() {
        return JAVA_VENDOR;
    }

    /**
     * Returns the JAVA vendor url.
     */
    public static String getJavaVendorURL() {
        return JAVA_VENDOR_URL;
    }

    /**
     * Returns the JAVA vendor.
     */
    public static String getJavaVersion() {
        return JAVA_VERSION;
    }

    /**
     * Returns the JAVA vendor.
     */
    public static String getNewline() {
        return NEWLINE;
    }

    /**
     * Returns true if this JVM is a 32bit JVM.
     */
    public static boolean is32Bit() {
        return is32Bit;
    }

    /**
     * Returns true if this JVM is a 64bit JVM.
     */
    public static boolean is64Bit() {
        return !is32Bit;
    }

    public static int getPointerSizeInBits() {
        return pointerSizeInBits;
    }

    public static int getPointerSizeInBytes() {
        return pointerSizeInBits/8;
    }

}

