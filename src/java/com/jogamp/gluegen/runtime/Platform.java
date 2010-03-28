/*
 * Copyright (c) 2010, Michael Bien
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
 * @author Michael Bien
 */
public class Platform {

    public static final boolean JAVA_SE;
    public static final boolean LITTLE_ENDIAN;

    static {
        // platform
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
        ByteBuffer tst_b = BufferFactory.newDirectByteBuffer(BufferFactory.SIZEOF_INT); // 32bit in native order
        IntBuffer tst_i = tst_b.asIntBuffer();
        ShortBuffer tst_s = tst_b.asShortBuffer();
        tst_i.put(0, 0x0A0B0C0D);
        LITTLE_ENDIAN = 0x0C0D == tst_s.get(0);
    }

    private Platform() {}

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
        return System.getProperty("os.name");
    }

    /**
     * Returns the CPU architecture String.
     */
    public static String getArch() {
        return System.getProperty("os.arch");
    }

    /**
     * Returns true if this JVM is a 32bit JVM.
     */
    public static boolean is32Bit() {
        return CPU.is32Bit();
    }

}
