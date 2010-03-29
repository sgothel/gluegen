/*
 * Copyright (c) 2010, Michael Bien, Sven Gothel
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Michael Bien nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Michael Bien BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * Created on Sunday, March 28 2010 14:43
 */
package com.jogamp.gluegen.runtime;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Utility class for querying platform specific properties.
 * @author Michael Bien, Sven Gothel
 */
public class Platform {

    public static final boolean JAVA_SE;
    public static final boolean LITTLE_ENDIAN;

    private final static boolean is32Bit;
    private final static int pointerSizeInBits;
    private final static String os, arch;

    static {
        NativeLibrary.ensureNativeLibLoaded();

        // We don't seem to need an AccessController.doPrivileged() block
        // here as these system properties are visible even to unsigned
        // applets
        os =  System.getProperty("os.name");
        arch = System.getProperty("os.arch");

        pointerSizeInBits = getPointerSizeInBitsImpl();

        // Try to use Sun's sun.arch.data.model first ..
        if ( 32 == pointerSizeInBits || 64 == pointerSizeInBits ) {
            is32Bit = ( 32 == pointerSizeInBits );
        }else {
            String os_lc = os.toLowerCase();
            String arch_lc = arch.toLowerCase();

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
              is32Bit = true;
            } else if ((os_lc.startsWith("windows") && arch_lc.equals("amd64")) ||
                      (os_lc.startsWith("linux") && arch_lc.equals("amd64")) ||
                      (os_lc.startsWith("linux") && arch_lc.equals("x86_64")) ||
                      (os_lc.startsWith("linux") && arch_lc.equals("ia64")) ||
                      (os_lc.startsWith("mac os_lc") && arch_lc.equals("x86_64")) ||
                      (os_lc.startsWith("darwin") && arch_lc.equals("x86_64")) ||
                      (os_lc.startsWith("sunos_lc") && arch_lc.equals("sparcv9")) ||
                      (os_lc.startsWith("sunos_lc") && arch_lc.equals("amd64"))) {
              is32Bit = false;
            }else{
              throw new RuntimeException("Please port CPU detection (32/64 bit) to your platform (" + os_lc + "/" + arch_lc + ")");
            }
        }

        // fast path
        boolean se = System.getProperty("java.runtime.name").indexOf("Java SE") != -1;

        if(!se) {
            try{
                Class.forName("java.nio.LongBuffer");
                Class.forName("java.nio.DoubleBuffer");
                se = true;
            }catch(ClassNotFoundException ex) {
                se = false;
            }
        }
        JAVA_SE = se;

        // byte order
        ByteBuffer tst_b = Buffers.newDirectByteBuffer(Buffers.SIZEOF_INT); // 32bit in native order
        IntBuffer tst_i = tst_b.asIntBuffer();
        ShortBuffer tst_s = tst_b.asShortBuffer();
        tst_i.put(0, 0x0A0B0C0D);
        LITTLE_ENDIAN = 0x0C0D == tst_s.get(0);
    }

    private Platform() {}

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
        return os;
    }

    /**
     * Returns the CPU architecture String.
     */
    public static String getArch() {
        return arch;
    }

    /**
     * Returns true if this JVM is a 32bit JVM.
     */
    public static boolean is32Bit() {
        return is32Bit;
    }

    public static int getPointerSizeInBits() {
        return pointerSizeInBits;
    }

}


