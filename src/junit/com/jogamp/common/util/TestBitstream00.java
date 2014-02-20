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

package com.jogamp.common.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.junit.Test;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;
import static com.jogamp.common.util.BitstreamData.*;
import com.jogamp.junit.util.JunitTracer;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test basic bit operations for {@link Bitstream}
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBitstream00 extends JunitTracer {

    @Test
    public void test00ShowByteOrder() {
        int i_ff = 0xff;
        byte b_ff = (byte)i_ff;
        System.err.println("i_ff "+i_ff+", "+toHexBinaryString(i_ff, 8));
        System.err.println("b_ff "+b_ff+", "+toHexBinaryString(0xff & b_ff, 8));

        System.err.println("Platform.LITTLE_ENDIAN: "+Platform.isLittleEndian());
        showOrderImpl(null);
        showOrderImpl(ByteOrder.BIG_ENDIAN);
        showOrderImpl(ByteOrder.LITTLE_ENDIAN);

        dumpData("tstMSB.whole", testBytesMSB, 0, testBytesMSB.length);
        dumpData("tstLSB.pbyte", testBytesLSB_revByte, 0, testBytesLSB_revByte.length);
        dumpData("tstLSB.whole", testBytesLSB, 0, testBytesLSB.length);
    }
    void showOrderImpl(ByteOrder byteOrder) {
        final ByteBuffer bb_long = ByteBuffer.allocate(Buffers.SIZEOF_LONG);
        if( null != byteOrder ) {
            bb_long.order(byteOrder);
        }
        System.err.println("Order: "+byteOrder+" -> "+bb_long.order());
        final LongBuffer lb = bb_long.asLongBuffer();
        lb.put(0, 0x0807060504030201L);
        dumpData("long."+byteOrder, bb_long, 0, bb_long.capacity());

        final ByteBuffer bb_int = ByteBuffer.allocate(Buffers.SIZEOF_INT);
        if( null != byteOrder ) {
            bb_int.order(byteOrder);
        }
        final IntBuffer ib = bb_int.asIntBuffer();
        ib.put(0, 0x04030201);
        dumpData("long."+byteOrder, bb_int, 0, bb_int.capacity());

        dumpData("tstMSB.whole", testBytesMSB, 0, testBytesMSB.length);
        dumpData("tstLSB.pbyte", testBytesLSB_revByte, 0, testBytesLSB_revByte.length);
        dumpData("tstLSB.whole", testBytesLSB, 0, testBytesLSB.length);
    }

    @Test
    public void test01ShiftSigned() {
        shiftSigned(0xA0000000); // negative w/ '1010' top-nibble
        shiftSigned(-1);
    }
    void shiftSigned(final int i0) {
        System.err.printf("i0 %012d, %s%n", i0, toHexBinaryString(i0, 32));
        {
            int im = i0;
            for(int i=0; i<32; i++) {
                final int bitA = ( 0 != ( i0 & ( 1 << i ) ) ) ? 1 : 0;
                final int bitB = im & 0x01;
                System.err.printf("[%02d]: bit[%d, %d], im %012d, %s%n", i, bitA, bitB, im, toHexBinaryString(im, 32));
                im = im >>> 1;
            }
        }
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestBitstream00.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
