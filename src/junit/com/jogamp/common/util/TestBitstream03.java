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

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.nio.Buffers;
import com.jogamp.junit.util.SingletonJunitCase;

import static com.jogamp.common.util.BitDemoData.*;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test {@link Bitstream} w/ int16 read/write access w/ semantics
 * as well as with aligned and unaligned access.
 * <ul>
 *  <li>{@link Bitstream#readUInt16(boolean)}</li>
 *  <li>{@link Bitstream#writeInt16(boolean, short)}</li>
 * </ul>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBitstream03 extends SingletonJunitCase {

    @Test
    public void test01Int16BitsAligned() throws IOException {
        test01Int16BitsImpl(null);
        test01Int16BitsImpl(ByteOrder.BIG_ENDIAN);
        test01Int16BitsImpl(ByteOrder.LITTLE_ENDIAN);
    }
    void test01Int16BitsImpl(final ByteOrder byteOrder) throws IOException {
        test01Int16BitsAlignedImpl(byteOrder, (short)0);
        test01Int16BitsAlignedImpl(byteOrder, (short)1);
        test01Int16BitsAlignedImpl(byteOrder, (short)7);
        test01Int16BitsAlignedImpl(byteOrder, (short)0x0fff);
        test01Int16BitsAlignedImpl(byteOrder, Short.MIN_VALUE);
        test01Int16BitsAlignedImpl(byteOrder, Short.MAX_VALUE);
        test01Int16BitsAlignedImpl(byteOrder, (short)0xffff);
    }
    void test01Int16BitsAlignedImpl(final ByteOrder byteOrder, final short val16) throws IOException {
        // Test with buffer defined value
        final ByteBuffer bb = ByteBuffer.allocate(Buffers.SIZEOF_SHORT);
        if( null != byteOrder ) {
            bb.order(byteOrder);
        }
        final boolean bigEndian = ByteOrder.BIG_ENDIAN == bb.order();
        System.err.println("XXX Test01Int16BitsAligned: byteOrder "+byteOrder+" (bigEndian "+bigEndian+"), value "+val16+", "+toHexBinaryString(val16, 16));
        bb.putShort(0, val16);
        dumpData("TestData.1: ", bb, 0, 2);

        final Bitstream.ByteBufferStream bbs = new Bitstream.ByteBufferStream(bb);
        final Bitstream<ByteBuffer> bs = new Bitstream<ByteBuffer>(bbs, false /* outputMode */);
        {
            final short r16 = (short) bs.readUInt16(bigEndian);
            System.err.println("Read16.1 "+r16+", "+toHexBinaryString(r16, 16));
            Assert.assertEquals(val16, r16);
        }

        // Test with written bitstream value
        bs.setStream(bs.getSubStream(), true /* outputMode */);
        bs.writeInt16(bigEndian, val16);
        bs.setStream(bs.getSubStream(), false /* outputMode */); // switch to input-mode, implies flush()
        dumpData("TestData.2: ", bb, 0, 2);
        {
            final short r16 = (short) bs.readUInt16(bigEndian);
            System.err.println("Read16.2 "+r16+", "+toHexBinaryString(r16, 16));
            Assert.assertEquals(val16, r16);
        }
    }

    @Test
    public void test02Int16BitsUnaligned() throws IOException {
        test02Int16BitsUnalignedImpl(null);
        test02Int16BitsUnalignedImpl(ByteOrder.BIG_ENDIAN);
        test02Int16BitsUnalignedImpl(ByteOrder.LITTLE_ENDIAN);
    }
    void test02Int16BitsUnalignedImpl(final ByteOrder byteOrder) throws IOException {
        test02Int16BitsUnalignedImpl(byteOrder, 0);
        test02Int16BitsUnalignedImpl(byteOrder, 1);
        test02Int16BitsUnalignedImpl(byteOrder, 7);
        test02Int16BitsUnalignedImpl(byteOrder, 8);
        test02Int16BitsUnalignedImpl(byteOrder, 15);
        test02Int16BitsUnalignedImpl(byteOrder, 24);
        test02Int16BitsUnalignedImpl(byteOrder, 25);
    }
    void test02Int16BitsUnalignedImpl(final ByteOrder byteOrder, final int preBits) throws IOException {
        test02Int16BitsUnalignedImpl(byteOrder, preBits, (short)0);
        test02Int16BitsUnalignedImpl(byteOrder, preBits, (short)1);
        test02Int16BitsUnalignedImpl(byteOrder, preBits, (short)7);
        test02Int16BitsUnalignedImpl(byteOrder, preBits, (short)0x0fff);
        test02Int16BitsUnalignedImpl(byteOrder, preBits, Short.MIN_VALUE);
        test02Int16BitsUnalignedImpl(byteOrder, preBits, Short.MAX_VALUE);
        test02Int16BitsUnalignedImpl(byteOrder, preBits, (short)0xffff);
    }
    void test02Int16BitsUnalignedImpl(final ByteOrder byteOrder, final int preBits, final short val16) throws IOException {
        final int preBytes = ( preBits + 7 ) >>> 3;
        final int byteCount = preBytes + Buffers.SIZEOF_SHORT;
        final ByteBuffer bb = ByteBuffer.allocate(byteCount);
        if( null != byteOrder ) {
            bb.order(byteOrder);
        }
        final boolean bigEndian = ByteOrder.BIG_ENDIAN == bb.order();
        System.err.println("XXX Test02Int16BitsUnaligned: byteOrder "+byteOrder+" (bigEndian "+bigEndian+"), preBits "+preBits+", value "+val16+", "+toHexBinaryString(val16, 16));

        // Test with written bitstream value
        final Bitstream.ByteBufferStream bbs = new Bitstream.ByteBufferStream(bb);
        final Bitstream<ByteBuffer> bs = new Bitstream<ByteBuffer>(bbs, true /* outputMode */);
        bs.writeBits31(preBits, 0);
        bs.writeInt16(bigEndian, val16);
        bs.setStream(bs.getSubStream(), false /* outputMode */); // switch to input-mode, implies flush()
        dumpData("TestData.1: ", bb, 0, byteCount);

        final int rPre = (short) bs.readBits31(preBits);
        final short r16 = (short) bs.readUInt16(bigEndian);
        System.err.println("ReadPre "+rPre+", "+toBinaryString(rPre, preBits));
        System.err.println("Read16 "+r16+", "+toHexBinaryString(r16, 16));
        Assert.assertEquals(val16, r16);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestBitstream03.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
