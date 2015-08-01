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

import java.nio.ByteBuffer;

public class BitDemoData {
    public static final long UNSIGNED_INT_MAX_VALUE = 0xffffffffL;

    public static final String[] pyramid32bit_one = {
                                               "00000000000000000000000000000001",
                                               "00000000000000000000000000000010",
                                               "00000000000000000000000000000100",
                                               "00000000000000000000000000001000",
                                               "00000000000000000000000000010000",
                                               "00000000000000000000000000100000",
                                               "00000000000000000000000001000000",
                                               "00000000000000000000000010000000",
                                               "00000000000000000000000100000000",
                                               "00000000000000000000001000000000",
                                               "00000000000000000000010000000000",
                                               "00000000000000000000100000000000",
                                               "00000000000000000001000000000000",
                                               "00000000000000000010000000000000",
                                               "00000000000000000100000000000000",
                                               "00000000000000001000000000000000",
                                               "00000000000000010000000000000000",
                                               "00000000000000100000000000000000",
                                               "00000000000001000000000000000000",
                                               "00000000000010000000000000000000",
                                               "00000000000100000000000000000000",
                                               "00000000001000000000000000000000",
                                               "00000000010000000000000000000000",
                                               "00000000100000000000000000000000",
                                               "00000001000000000000000000000000",
                                               "00000010000000000000000000000000",
                                               "00000100000000000000000000000000",
                                               "00001000000000000000000000000000",
                                               "00010000000000000000000000000000",
                                               "00100000000000000000000000000000",
                                               "01000000000000000000000000000000",
                                               "10000000000000000000000000000000"
                                             };

    //
    // MSB -> LSB over whole data
    //
    public static final byte[] testBytesMSB = new byte[] { (byte)0xde, (byte)0xaf, (byte)0xca, (byte)0xfe };
    public static final int testIntMSB = 0xdeafcafe; //           11011110    10101111    11001010    11111110
    public static final String[] testStringsMSB = new String[] { "11011110", "10101111", "11001010", "11111110" };
    public static final String testStringMSB = testStringsMSB[0]+testStringsMSB[1]+testStringsMSB[2]+testStringsMSB[3];

    //
    // MSB -> LSB, reverse bit-order over each byte of testBytesLSB
    //
    public static final byte[] testBytesMSB_rev = new byte[] { (byte)0xfe, (byte)0xca, (byte)0xaf, (byte)0xde };
    public static final int testIntMSB_rev = 0xfecaafde;
    public static final String[] testStringsMSB_rev = new String[] { "11111110", "11001010", "10101111", "11011110" };
    public static final String testStringMSB_rev = testStringsMSB_rev[0]+testStringsMSB_rev[1]+testStringsMSB_rev[2]+testStringsMSB_rev[3];

    //
    // LSB -> MSB over whole data
    //
    public static final byte[] testBytesLSB = new byte[] { (byte)0x7f, (byte)0x53, (byte)0xf5, (byte)0x7b };
    public static final int testIntLSB = 0x7f53f57b;
    public static final String[] testStringsLSB = new String[] { "01111111", "01010011", "11110101", "01111011" };
    public static final String testStringLSB = testStringsLSB[0]+testStringsLSB[1]+testStringsLSB[2]+testStringsLSB[3];

    //
    // LSB -> MSB, reverse bit-order over each byte of testBytesMSB
    //
    public static final byte[] testBytesLSB_revByte = new byte[] { (byte)0x7b, (byte)0xf5, (byte)0x53, (byte)0x7f };
    public static final int testIntLSB_revByte = 0x7bf5537f;
    public static final String[] testStringsLSB_revByte = new String[] { "01111011", "11110101", "01010011", "01111111" };
    public static final String testStringLSB_revByte = testStringsLSB_revByte[0]+testStringsLSB_revByte[1]+testStringsLSB_revByte[2]+testStringsLSB_revByte[3];

    public static final void dumpData(final String prefix, final byte[] data, final int offset, final int len) {
        for(int i=0; i<len; ) {
            System.err.printf("%s: %03d: ", prefix, i);
            for(int j=0; j<8 && i<len; j++, i++) {
                final int v = 0xFF & data[offset+i];
                System.err.printf(toHexBinaryString(v, 8)+", ");
            }
            System.err.println("");
        }
    }
    public static final void dumpData(final String prefix, final ByteBuffer data, final int offset, final int len) {
        for(int i=0; i<len; ) {
            System.err.printf("%s: %03d: ", prefix, i);
            for(int j=0; j<8 && i<len; j++, i++) {
                final int v = 0xFF & data.get(offset+i);
                System.err.printf(toHexBinaryString(v, 8)+", ");
            }
            System.err.println("");
        }
    }

    public static int getOneBitCount(final String pattern) {
        int c=0;
        for(int i=0; i<pattern.length(); i++) {
            if( '1' == pattern.charAt(i) ) {
                c++;
            }
        }
        return c;
    }
    public static long toLong(final String bitPattern) {
        return Long.valueOf(bitPattern, 2).longValue();
    }
    public static int toInteger(final String bitPattern) {
        final long res = Long.valueOf(bitPattern, 2).longValue();
        if( res > UNSIGNED_INT_MAX_VALUE ) {
            throw new NumberFormatException("Exceeds "+toHexString(UNSIGNED_INT_MAX_VALUE)+": "+toHexString(res)+" - source "+bitPattern);
        }
        return (int)res;
    }

    public static String toHexString(final int v) {
        return "0x"+Integer.toHexString(v);
    }
    public static String toHexString(final long v) {
        return "0x"+Long.toHexString(v);
    }
    public static final String strZeroPadding=  "0000000000000000000000000000000000000000000000000000000000000000"; // 64
    public static String toBinaryString(final int v, final int bitCount) {
        if( 0 == bitCount ) {
            return "";
        }
        final int mask = (int) ( ( 1L << bitCount ) - 1L );
        final String s0 = Integer.toBinaryString( mask & v );
        return strZeroPadding.substring(0, bitCount-s0.length())+s0;
    }
    public static String toBinaryString(final long v, final int bitCount) {
        if( 0 == bitCount ) {
            return "";
        }
        final long mask = ( 1L << bitCount ) - 1L;
        final String s0 = Long.toBinaryString( mask & v );
        return strZeroPadding.substring(0, bitCount-s0.length())+s0;
    }
    public static String toHexBinaryString(final long v, final int bitCount) {
        final int nibbles = 0 == bitCount ? 2 : ( bitCount + 3 ) / 4;
        return String.format("[%0"+nibbles+"X, %s]", v, toBinaryString(v, bitCount));
    }
    public static String toHexBinaryString(final int v, final int bitCount) {
        final int nibbles = 0 == bitCount ? 2 : ( bitCount + 3 ) / 4;
        return String.format("[%0"+nibbles+"X, %s]", v, toBinaryString(v, bitCount));
    }
    public static String toHexBinaryString(final short v, final int bitCount) {
        final int nibbles = 0 == bitCount ? 2 : ( bitCount + 3 ) / 4;
        return String.format("[%0"+nibbles+"X, %s]", v, toBinaryString(v, bitCount));
    }
}
