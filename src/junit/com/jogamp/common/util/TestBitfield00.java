/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

import org.junit.Test;
import org.junit.Assert;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test basic bitfield operations for {@link Bitfield}
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBitfield00 extends SingletonJunitCase {

    @Test
    public void test01_BitCount32_One() {
        final String[] pyramid32bit_one = BitDemoData.pyramid32bit_one;
        for(int i=0; i<pyramid32bit_one.length; i++) {
            final int val0 = 1 << i;
            final int oneBitCountI = Integer.bitCount(val0);
            final String pattern0 = pyramid32bit_one[i];
            final int val1 = BitDemoData.toInteger(pattern0);
            final String pattern1 = BitDemoData.toBinaryString(val0, 32);
            final int oneBitCount0 = BitDemoData.getOneBitCount(pattern0);
            final int oneBitCount1 = Bitfield.Util.bitCount(val0);
            final String msg = String.format("Round %02d: 0x%08x %s, c %d / %d%n        : 0x%08x %s, c %d%n",
                    i, val0, pattern0, oneBitCount0, oneBitCountI, val1, pattern1, oneBitCount1);

            Assert.assertEquals(msg, val0, val1);
            Assert.assertEquals(msg, pattern0, pattern1);

            Assert.assertEquals(msg, oneBitCount0, oneBitCountI);
            Assert.assertEquals(msg, oneBitCount0, oneBitCount1);
        }
    }

    @SuppressWarnings("unused")
    @Test
    public void test02_BitCount32_Samples() {
        final long MAX = BitDemoData.UNSIGNED_INT_MAX_VALUE;
        final long MAX_minus = MAX-0x1FF;
        final long MAX_half = MAX/2;
        final long MAX_half_minus = MAX_half-0x1FF;
        final long MAX_half_plus = MAX_half+0x1FF;

        if( false ) {
            for(long l=0; l<=MAX; l++) {
                test_BitCount32_Samples(l);
            }
        }
        for(long l=0; l>=0x1FF; l++) {
            test_BitCount32_Samples(l);
        }
        for(long l=MAX_half_minus; l<=MAX_half_plus; l++) {
            test_BitCount32_Samples(l);
        }
        for(long l=MAX_minus; l<=MAX; l++) {
            test_BitCount32_Samples(l);
        }
    }
    static void test_BitCount32_Samples(final long l) {
        final int oneBitCountL = Long.bitCount(l);
        final int val0 = (int)l;
        final int oneBitCountI = Integer.bitCount(val0);
        final int oneBitCount1 = Bitfield.Util.bitCount(val0);
        final String msg = String.format("Round 0x%08x, c %d / %d / %d", val0,
                oneBitCountL, oneBitCountI, oneBitCount1);
        Assert.assertEquals(msg, oneBitCountI, oneBitCountL);
        Assert.assertEquals(msg, oneBitCountI, oneBitCount1);
    }

    static int[] testDataOneBit = new int[] {
            0,0, 1,1, 2,1, 3,2, 4,1, 5,2, 6,2, 7,3,
            8,1, 9,2, 10,2, 11,3, 12,2, 13,3, 14,3, 15,4, 16,1, 17,2,
            0x3F,6, 0x40,1, 0x41,2, 0x7f,7, 0x80,1, 0x81,2, 0xfe,7, 0xff,8,
            0x4000,1, 0x4001,2, 0x7000,3, 0x7fff,15,
            0x0FFFFFF0,24,
            0x55555555,16,
            0x7F53F57B,23, 0xFEA7EAF6,23, /* << 1 */
            0x80000000, 1,
            0xAAAAAAAA,16,
            0xC0C0C0C0, 8,
            0xFF000000, 8,
            0xFFFFFFFF,32
    };
    @Test
    public void test03_BitCount32_Data() {
        for(int i = 0; i<testDataOneBit.length; i+=2) {
            test_BitCount32_Data(testDataOneBit[i], testDataOneBit[i+1]);
        }
    }
    static void test_BitCount32_Data(final int i, final int expOneBits) {
        final int oneBitCountI = Integer.bitCount(i);
        final int oneBitCount1 = Bitfield.Util.bitCount(i);
        final String msg = String.format("Round 0x%08x, c %d / %d", i,
                                    oneBitCountI, oneBitCount1);
        Assert.assertEquals(msg, oneBitCount1, expOneBits);
        Assert.assertEquals(msg, oneBitCountI, oneBitCount1);
    }

    @Test
    public void test10_Setup() {
        final int bitSize32 = 32;
        final int bitSize128 = 128;
        final Bitfield bf1 = Bitfield.Factory.create(bitSize32);
        Assert.assertEquals(bitSize32, bf1.size());
        Assert.assertTrue(bf1 instanceof jogamp.common.util.Int32Bitfield);
        final Bitfield bf2 = Bitfield.Factory.create(bitSize128);
        Assert.assertEquals(bitSize128, bf2.size());
        Assert.assertTrue(bf2 instanceof jogamp.common.util.Int32ArrayBitfield);

        // verify no bit is set
        Assert.assertEquals(0, bf1.bitCount());
        Assert.assertEquals(0, bf2.bitCount());

        bf1.clearField(true);
        bf2.clearField(true);
        Assert.assertEquals(bf1.size(), bf1.bitCount());
        Assert.assertEquals(bf2.size(), bf2.bitCount());

        bf1.clearField(false);
        bf2.clearField(false);
        Assert.assertEquals(0, bf1.bitCount());
        Assert.assertEquals(0, bf2.bitCount());
    }
    static class TestDataBF {
        final int bitSize;
        final int val;
        final String pattern;
        public TestDataBF(final int bitSize, final int value, final String pattern) {
            this.bitSize = bitSize;
            this.val = value;
            this.pattern = pattern;
        }
    }
    static TestDataBF[] testDataBF32Bit = {
        new TestDataBF(32, BitDemoData.testIntMSB, BitDemoData.testStringMSB),
        new TestDataBF(32, BitDemoData.testIntMSB_rev, BitDemoData.testStringMSB_rev),
        new TestDataBF(32, BitDemoData.testIntLSB, BitDemoData.testStringLSB),
        new TestDataBF(32, BitDemoData.testIntLSB_revByte, BitDemoData.testStringLSB_revByte),

        // H->L    : 0x04030201: 00000100 00000011 00000010 00000001
        new TestDataBF(32, 0x04030201, "00000100000000110000001000000001"),

        // H->L    : 0xAFFECAFE: 10101111 11111110 11001010 11111110
        new TestDataBF(32, 0xAFFECAFE, "10101111111111101100101011111110"),
        // H->L    : 0xDEADBEEF: 11011110 10101101 10111110 11101111
        new TestDataBF(32, 0xDEADBEEF, "11011110101011011011111011101111")
    };
    static TestDataBF[] testDataBF16Bit = {
        // H->L    : 0x0201: 00000100 00000011 00000010 00000001
        new TestDataBF(16, 0x0201, "0000001000000001"),
        // H->L    : 0x0403: 00000100 00000011
        new TestDataBF(16, 0x0403, "0000010000000011"),

        // H->L    : 0xAFFE: 10101111 11111110
        new TestDataBF(16, 0xAFFE, "1010111111111110"),
        // H->L    : 0xCAFE: 11001010 11111110
        new TestDataBF(16, 0xCAFE, "1100101011111110"),

        // H->L    : 0xDEADBEEF: 11011110 10101101 10111110 11101111
        new TestDataBF(16, 0xDEAD, "1101111010101101"),
        new TestDataBF(16, 0xBEEF, "1011111011101111")
    };
    static TestDataBF[] testDataBF3Bit = {
        new TestDataBF(3, 0x01, "001"),
        new TestDataBF(3, 0x02, "010"),
        new TestDataBF(3, 0x05, "101")
    };

    @Test
    public void test20_ValidateTestData() {
        for(int i=0; i<testDataBF32Bit.length; i++) {
            test_ValidateTestData( testDataBF32Bit[i] );
        }
        for(int i=0; i<testDataBF16Bit.length; i++) {
            test_ValidateTestData( testDataBF16Bit[i] );
        }
        for(int i=0; i<testDataBF3Bit.length; i++) {
            test_ValidateTestData( testDataBF3Bit[i] );
        }
    }
    static void test_ValidateTestData(final TestDataBF d) {
        final int oneBitCount0 = Bitfield.Util.bitCount(d.val);
        final int oneBitCount1 = BitDemoData.getOneBitCount(d.pattern);
        Assert.assertEquals(oneBitCount1, oneBitCount0);
        final String pattern0 = BitDemoData.toBinaryString(d.val, d.bitSize);
        Assert.assertEquals(d.pattern, pattern0);
        final int val1 = BitDemoData.toInteger(d.pattern);
        Assert.assertEquals(d.val, val1);
        Assert.assertEquals(d.bitSize, pattern0.length());
    }

    static void assertEquals(final Bitfield bf, final int bf_off, final int v, final String pattern, final int oneBitCount) {
        final int len = pattern.length();
        for(int i=0; i<len; i++) {
            final boolean exp0 = 0 != ( v & ( 1 << i ) );
            final boolean exp1 = '1' == pattern.charAt(len-1-i);
            final boolean has = bf.get(i+bf_off);
            final String msg = String.format("Pos %04d: Value 0x%08x / %s, c %d", i, v, pattern, oneBitCount);
            Assert.assertEquals(msg, exp0, has);
            Assert.assertEquals(msg, exp1, has);
        }
    }

    @Test
    public void test21_Aligned32bit() {
        for(int i=0; i<testDataBF32Bit.length; i++) {
            test_Aligned32bit( testDataBF32Bit[i] );
        }
        for(int i=0; i<testDataBF16Bit.length; i++) {
            test_Aligned32bit( testDataBF16Bit[i] );
        }
        for(int i=0; i<testDataBF3Bit.length; i++) {
            test_Aligned32bit( testDataBF3Bit[i] );
        }
    }
    static int get32BitStorageSize(final int bits) {
        final int units = Math.max(1, ( bits + 31 ) >>> 5);
        return units << 5;
    }
    static void test_Aligned32bit(final TestDataBF d) {
        final int oneBitCount = Bitfield.Util.bitCount(d.val);

        final Bitfield bf1 = Bitfield.Factory.create(d.bitSize);
        Assert.assertEquals(get32BitStorageSize(d.bitSize), bf1.size());
        final Bitfield bf2 = Bitfield.Factory.create(d.bitSize+128);
        Assert.assertEquals(get32BitStorageSize(d.bitSize+128), bf2.size());

        bf1.put32( 0, d.bitSize, d.val);
        Assert.assertEquals(d.val, bf1.get32( 0, d.bitSize));
        Assert.assertEquals(oneBitCount, bf1.bitCount());
        assertEquals(bf1, 0, d.val, d.pattern, oneBitCount);

        bf2.put32( 0, d.bitSize, d.val);
        Assert.assertEquals(d.val, bf2.get32( 0, d.bitSize));
        Assert.assertEquals(oneBitCount*1, bf2.bitCount());
        assertEquals(bf2, 0, d.val, d.pattern, oneBitCount);
        bf2.put32(64, d.bitSize, d.val);
        Assert.assertEquals(d.val, bf2.get32(64, d.bitSize));
        Assert.assertEquals(oneBitCount*2, bf2.bitCount());
        assertEquals(bf2, 64, d.val, d.pattern, oneBitCount);

        Assert.assertEquals(d.val, bf2.copy32(0, 96, d.bitSize));
        Assert.assertEquals(d.val, bf2.get32(96, d.bitSize));
        Assert.assertEquals(oneBitCount*3, bf2.bitCount());
        assertEquals(bf2, 96, d.val, d.pattern, oneBitCount);
    }

    @Test
    public void test21_Unaligned() {
        for(int i=0; i<testDataBF32Bit.length; i++) {
            test_Unaligned(testDataBF32Bit[i]);
        }
        for(int i=0; i<testDataBF16Bit.length; i++) {
            test_Unaligned(testDataBF16Bit[i]);
        }
        for(int i=0; i<testDataBF3Bit.length; i++) {
            test_Unaligned( testDataBF3Bit[i] );
        }
    }
    static void test_Unaligned(final TestDataBF d) {
            final Bitfield bf1 = Bitfield.Factory.create(d.bitSize);
            final Bitfield bf2 = Bitfield.Factory.create(d.bitSize+128);
            Assert.assertEquals(get32BitStorageSize(d.bitSize), bf1.size());
            Assert.assertEquals(get32BitStorageSize(d.bitSize+128), bf2.size());
            test_Unaligned( d, bf1 );
            test_Unaligned( d, bf2 );
    }
    static void test_Unaligned(final TestDataBF d, final Bitfield bf) {
        final int maxBitpos = bf.size()-d.bitSize;
        for(int i=0; i<=maxBitpos; i++) {
            bf.clearField(false);
            test_Unaligned(d, bf, i);
        }
    }
    static void test_Unaligned(final TestDataBF d, final Bitfield bf, final int lowBitnum) {
        final int maxBitpos = bf.size()-d.bitSize;
        final int oneBitCount = Bitfield.Util.bitCount(d.val);

        final String msg = String.format("Value 0x%08x / %s, l %d/%d, c %d, lbPos %d -> %d",
                d.val, d.pattern, d.bitSize, bf.size(), oneBitCount, lowBitnum, maxBitpos);

        //
        // via put32
        //
        bf.put32( lowBitnum, d.bitSize, d.val);
        for(int i=0; i<d.bitSize; i++) {
            Assert.assertEquals(msg+", bitpos "+i, 0 != ( d.val & ( 1 << i ) ), bf.get(lowBitnum+i));
        }
        Assert.assertEquals(msg, d.val, bf.get32( lowBitnum, d.bitSize));
        Assert.assertEquals(msg, oneBitCount, bf.bitCount());
        assertEquals(bf, lowBitnum, d.val, d.pattern, oneBitCount);

        //
        // via copy32
        //
        if( lowBitnum < maxBitpos ) {
            // copy bits 1 forward
            // clear trailing orig bit
            Assert.assertEquals(msg, d.val, bf.copy32(lowBitnum, lowBitnum+1, d.bitSize));
            bf.clear(lowBitnum);
            Assert.assertEquals(msg, d.val, bf.get32( lowBitnum+1, d.bitSize));
            Assert.assertEquals(msg, oneBitCount, bf.bitCount());
            assertEquals(bf, lowBitnum+1, d.val, d.pattern, oneBitCount);
        }

        // test put() return value (previous value)
        bf.clearField(false);
        Assert.assertEquals(msg+", bitpos "+0, false, bf.put(lowBitnum+0, true));
        Assert.assertEquals(msg+", bitpos "+0,  true, bf.put(lowBitnum+0, false));

        //
        // via put
        //
        for(int i=0; i<d.bitSize; i++) {
            Assert.assertEquals(msg+", bitpos "+i, false, bf.put(lowBitnum+i, 0 != ( d.val & ( 1 << i ) )));
        }
        Assert.assertEquals(msg, d.val, bf.get32(lowBitnum, d.bitSize));
        for(int i=0; i<d.bitSize; i++) {
            Assert.assertEquals(msg+", bitpos "+i, 0 != ( d.val & ( 1 << i ) ), bf.get(lowBitnum+i));
        }
        Assert.assertEquals(msg, oneBitCount, bf.bitCount());
        assertEquals(bf, lowBitnum, d.val, d.pattern, oneBitCount);

        //
        // via copy
        //
        if( lowBitnum < maxBitpos ) {
            // copy bits 1 forward
            // clear trailing orig bit
            for(int i=d.bitSize-1; i>=0; i--) {
                Assert.assertEquals(msg+", bitpos "+i, 0 != ( d.val & ( 1 << i ) ),
                                                       bf.copy(lowBitnum+i, lowBitnum+1+i) );
            }
            bf.clear(lowBitnum);
            Assert.assertEquals(msg, d.val, bf.get32( lowBitnum+1, d.bitSize));
            for(int i=0; i<d.bitSize; i++) {
                Assert.assertEquals(msg+", bitpos "+i, 0 != ( d.val & ( 1 << i ) ), bf.get(lowBitnum+1+i));
            }
            Assert.assertEquals(msg, oneBitCount, bf.bitCount());
            assertEquals(bf, lowBitnum+1, d.val, d.pattern, oneBitCount);
        }

        //
        // via set/clear
        //
        bf.clearField(false);
        for(int i=0; i<d.bitSize; i++) {
            if( 0 != ( d.val & ( 1 << i ) ) ) {
                bf.set(lowBitnum+i);
            } else {
                bf.clear(lowBitnum+i);
            }
        }
        Assert.assertEquals(msg, d.val, bf.get32(lowBitnum, d.bitSize));
        for(int i=0; i<d.bitSize; i++) {
            Assert.assertEquals(msg+", bitpos "+i, 0 != ( d.val & ( 1 << i ) ), bf.get(lowBitnum+i));
        }
        Assert.assertEquals(msg, oneBitCount, bf.bitCount());
        assertEquals(bf, lowBitnum, d.val, d.pattern, oneBitCount);

        //
        // Validate 'other bits' put32/get32
        //
        bf.clearField(false);
        bf.put32( lowBitnum, d.bitSize, d.val);
        checkOtherBits(d, bf, lowBitnum, msg, 0);

        bf.clearField(true);
        bf.put32( lowBitnum, d.bitSize, d.val);
        checkOtherBits(d, bf, lowBitnum, msg, Bitfield.UNSIGNED_INT_MAX_VALUE);
    }

    static void checkOtherBits(final TestDataBF d, final Bitfield bf, final int lowBitnum, final String msg, final int expBits) {
        final int highBitnum = lowBitnum + d.bitSize - 1;
        // System.err.println(msg+": [0"+".."+"("+lowBitnum+".."+highBitnum+").."+(bf.size()-1)+"]");
        for(int i=0; i<lowBitnum; i+=32) {
            final int len = Math.min(32, lowBitnum-i);
            final int val = bf.get32(i, len);
            final int exp = expBits & Bitfield.Util.getBitMask(len);
            // System.err.println("    <"+i+".."+(i+len-1)+">, exp "+BitDemoData.toHexString(exp));
            Assert.assertEquals(msg+", bitpos "+i, exp, val);
        }
        for(int i=highBitnum+1; i<bf.size(); i+=32) {
            final int len = Math.min(32, bf.size() - i);
            final int val = bf.get32(i, len);
            final int exp = expBits & Bitfield.Util.getBitMask(len);
            // System.err.println("        <"+i+".."+(i+len-1)+">, exp "+BitDemoData.toHexString(exp));
            Assert.assertEquals(msg+", bitpos "+i, exp, val);
        }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestBitfield00.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
