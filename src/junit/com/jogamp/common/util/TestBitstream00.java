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
import org.junit.Assert;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;

import static com.jogamp.common.util.BitDemoData.*;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test basic bit operations for {@link Bitstream}
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBitstream00 extends SingletonJunitCase {

    @Test
    public void test00ShowByteOrder() {
        final int i_ff = 0xff;
        final byte b_ff = (byte)i_ff;
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
    void showOrderImpl(final ByteOrder byteOrder) {
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
    public void test01Uint32Conversion() {
        testUInt32Conversion(1, 1);
        testUInt32Conversion(-2, -1);
        testUInt32Conversion(Integer.MAX_VALUE, Integer.MAX_VALUE);
        testUInt32Conversion(0xffff0000, -1);
        testUInt32Conversion(0xffffffff, -1);
    }
    void testUInt32Conversion(final int int32, final int expUInt32Int) {
        final String int32_hStr = toHexString(int32);
        final long l = Bitstream.toUInt32Long(int32);
        final String l_hStr = toHexString(l);
        final int i = Bitstream.toUInt32Int(int32);
        final String i_hStr = toHexString(i);
        System.err.printf("int32_t %012d %10s -> (long) %012d %10s, (int) %012d %10s%n", int32, int32_hStr, l, l_hStr, i, i_hStr);
        Assert.assertEquals(int32_hStr, l_hStr);
        Assert.assertEquals(expUInt32Int, i);
    }

    @Test
    public void test02ShiftSigned() {
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

    @Test
    public void test10ReadWrite_13() throws UnsupportedOperationException, IllegalStateException, IOException {
        // H->L    : 00000011 00000010 00000001   000000110000001000000001
        // H->L rev: 10000000 01000000 11000000   100000000100000011000000
        //
        // L->H    : 00000001 00000010 00000011   000000010000001000000011
        // L->H rev: 11000000 01000000 10000000   110000000100000010000000
        test10ReadWrite1_31Impl(8, 8, 8, 0x030201, "000000110000001000000001");

        // H->L: 00011 000010 00001  0001100001000001
        // L->H: 10000 010000 11000  1000001000011000
        test10ReadWrite1_31Impl(5, 6, 5, 0x1841, "0001100001000001");
    }
    void test10ReadWrite1_31Impl(final int c1, final int c2, final int c3, final int v, final String vStrHigh2LowExp)
                        throws UnsupportedOperationException, IllegalStateException, IOException
    {
        // final Bitstream<ByteBuffer> source = new Bitstream<ByteBuffer>();
        final int bitCount = c1+c2+c3;
        final int byteCount = ( bitCount + 7 ) / 8;
        final String vStrHigh2Low0 = Bitstream.toBinString(true, v, bitCount);
        System.err.printf("test10ReadWrite31 bits %d:%d:%d = %d = %d bytes%n",
                c1, c2, c3, bitCount, byteCount);
        System.err.printf("test10ReadWrite31 %s%n", Bitstream.toHexBinString(true, v, bitCount));
        System.err.printf("test10ReadWrite31 %s%n", Bitstream.toHexBinString(false, v, bitCount));
        Assert.assertEquals(vStrHigh2LowExp, vStrHigh2Low0);

        final ByteBuffer bbRead = ByteBuffer.allocate(byteCount);
        for(int i=0; i<byteCount; i++) {
            final int b = ( v >>> 8*i ) & 0xff;
            bbRead.put(i, (byte) b);
            System.err.printf("testBytes[%d]: %s%n", i, Bitstream.toHexBinString(true, b, 8));
        }
        final Bitstream.ByteBufferStream bbsRead = new Bitstream.ByteBufferStream(bbRead);
        final Bitstream<ByteBuffer> bsRead = new Bitstream<ByteBuffer>(bbsRead, false /* outputMode */);

        String vStrHigh2Low1C1 = "";
        String vStrHigh2Low1C2 = "";
        String vStrHigh2Low1C3 = "";
        String vStrHigh2Low1 = "";
        {
            bsRead.mark(byteCount);
            System.err.println("readBit    (msbFirst false): ");
            int b;
            int i=0;
            String vStrHigh2Low1T = ""; // OK for LSB, MSB segmented
            while( Bitstream.EOS != ( b = bsRead.readBit(false /* msbFirst */) ) ) {
                vStrHigh2Low1T = b + vStrHigh2Low1T;
                if(i < c1) {
                    vStrHigh2Low1C1 = b + vStrHigh2Low1C1;
                } else if(i < c1+c2) {
                    vStrHigh2Low1C2 = b + vStrHigh2Low1C2;
                } else {
                    vStrHigh2Low1C3 = b + vStrHigh2Low1C3;
                }
                i++;
            }
            vStrHigh2Low1 = vStrHigh2Low1C3 + vStrHigh2Low1C2 + vStrHigh2Low1C1;
            System.err.printf("readBit.1 %s, 0x%s%n", vStrHigh2Low1C1, Integer.toHexString(Integer.valueOf(vStrHigh2Low1C1, 2)));
            System.err.printf("readBit.2 %s, 0x%s%n", vStrHigh2Low1C2, Integer.toHexString(Integer.valueOf(vStrHigh2Low1C2, 2)));
            System.err.printf("readBit.3 %s, 0x%s%n", vStrHigh2Low1C3, Integer.toHexString(Integer.valueOf(vStrHigh2Low1C3, 2)));
            System.err.printf("readBit.T %s, ok %b%n%n", vStrHigh2Low1T, vStrHigh2LowExp.equals(vStrHigh2Low1T));
            System.err.printf("readBit.X %s, ok %b%n%n", vStrHigh2Low1, vStrHigh2LowExp.equals(vStrHigh2Low1));
            bsRead.reset();
        }

        {
            String vStrHigh2Low3T = ""; // OK for LSB, MSB segmented
            System.err.println("readBits32: ");
            final int b = bsRead.readBits31(bitCount);
            vStrHigh2Low3T = Bitstream.toBinString(true, b, bitCount);
            System.err.printf("readBits31.T %s, ok %b, %s%n%n", vStrHigh2Low3T, vStrHigh2LowExp.equals(vStrHigh2Low3T), Bitstream.toHexBinString(true, b, bitCount));
            bsRead.reset();
        }

        String vStrHigh2Low2 = "";
        {
            System.err.println("readBits32: ");
            final int bC1 = bsRead.readBits31(c1);
            System.err.printf("readBits31.1 %s%n", Bitstream.toHexBinString(true, bC1, c1));
            final int bC2 = bsRead.readBits31(c2);
            System.err.printf("readBits31.2 %s%n", Bitstream.toHexBinString(true, bC2, c2));
            final int bC3 = bsRead.readBits31(c3);
            System.err.printf("readBits31.3 %s%n", Bitstream.toHexBinString(true, bC3, c3));
            final int b = bC3 << (c1+c2) | bC2 << c1 | bC1;
            vStrHigh2Low2 = Bitstream.toBinString(true, b, bitCount);
            System.err.printf("readBits31.X %s, ok %b, %s%n%n", vStrHigh2Low2, vStrHigh2LowExp.equals(vStrHigh2Low2), Bitstream.toHexBinString(true, b, bitCount));
            bsRead.reset();
        }

        Assert.assertEquals(vStrHigh2LowExp, vStrHigh2Low1);
        Assert.assertEquals(vStrHigh2LowExp, vStrHigh2Low2);

        boolean ok = true;
        {
            final ByteBuffer bbWrite = ByteBuffer.allocate(byteCount);
            final Bitstream.ByteBufferStream bbsWrite = new Bitstream.ByteBufferStream(bbWrite);
            final Bitstream<ByteBuffer> bsWrite = new Bitstream<ByteBuffer>(bbsWrite, true /* outputMode */);
            {
                int b;
                while( Bitstream.EOS != ( b = bsRead.readBit(false)) ) {
                    bsWrite.writeBit(false, b);
                }
            }
            bsRead.reset();
            for(int i=0; i<byteCount; i++) {
                final int bR = bbWrite.get(i);
                final int bW = bbWrite.get(i);
                System.err.printf("readWriteBit   [%d]: read %s, write %s, ok %b%n",
                        i, Bitstream.toHexBinString(true, bR, 8), Bitstream.toHexBinString(true, bW, 8), bR==bW);
                ok = ok && bR==bW;
            }
            Assert.assertTrue(ok);
        }
        {
            final ByteBuffer bbWrite = ByteBuffer.allocate(byteCount);
            final Bitstream.ByteBufferStream bbsWrite = new Bitstream.ByteBufferStream(bbWrite);
            final Bitstream<ByteBuffer> bsWrite = new Bitstream<ByteBuffer>(bbsWrite, true /* outputMode */);
            {
                bsWrite.writeBits31(bitCount, bsRead.readBits31(bitCount));
            }
            bsRead.reset();
            for(int i=0; i<byteCount; i++) {
                final int bR = bbWrite.get(i);
                final int bW = bbWrite.get(i);
                System.err.printf("readWriteBits31[%d]: read %s, write %s, ok %b%n",
                        i, Bitstream.toHexBinString(true, bR, 8), Bitstream.toHexBinString(true, bW, 8), bR==bW);
                ok = ok && bR==bW;
            }
            Assert.assertTrue(ok);
        }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestBitstream00.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
