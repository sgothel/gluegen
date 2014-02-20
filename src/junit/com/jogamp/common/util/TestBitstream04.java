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
import com.jogamp.junit.util.JunitTracer;

import static com.jogamp.common.util.BitstreamData.*;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test {@link Bitstream} w/ int32 read/write access w/ semantics
 * as well as with aligned and unaligned access.
 * <ul>
 *  <li>{@link Bitstream#readInt32(boolean, boolean)}</li>
 *  <li>{@link Bitstream#writeInt32(boolean, boolean, int)}</li>
 * </ul>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBitstream04 extends JunitTracer {

    @Test
    public void test01Int32BitsAligned() throws IOException {
        test01Int32BitsImpl(null);
        test01Int32BitsImpl(ByteOrder.BIG_ENDIAN);
        test01Int32BitsImpl(ByteOrder.LITTLE_ENDIAN);
    }
    void test01Int32BitsImpl(ByteOrder byteOrder) throws IOException {
        test01Int32BitsAlignedImpl(byteOrder, 0);
        test01Int32BitsAlignedImpl(byteOrder, 1);
        test01Int32BitsAlignedImpl(byteOrder, -1);
        test01Int32BitsAlignedImpl(byteOrder, 7);
        test01Int32BitsAlignedImpl(byteOrder, 0x0fffffff);
        test01Int32BitsAlignedImpl(byteOrder, Integer.MIN_VALUE);
        test01Int32BitsAlignedImpl(byteOrder, Integer.MAX_VALUE);
        test01Int32BitsAlignedImpl(byteOrder, 0xffffffff);
    }
    void test01Int32BitsAlignedImpl(ByteOrder byteOrder, int val32) throws IOException {
        // Test with buffer defined value
        final ByteBuffer bb = ByteBuffer.allocate(Buffers.SIZEOF_INT);
        if( null != byteOrder ) {
            bb.order(byteOrder);
        }
        final boolean bigEndian = ByteOrder.BIG_ENDIAN == bb.order();
        System.err.println("XXX Test01Int32BitsAligned: byteOrder "+byteOrder+" (bigEndian "+bigEndian+"), value "+val32+", "+toHexBinaryString(val32, 32));
        System.err.println("XXX Test01Int32BitsAligned: "+toUnsignedBinaryString(val32));
        bb.putInt(0, val32);

        final Bitstream.ByteBufferStream bbs = new Bitstream.ByteBufferStream(bb);
        final Bitstream<ByteBuffer> bs = new Bitstream<ByteBuffer>(bbs, false /* outputMode */);
        {
            final int r32 = bs.readInt32(true /* msbFirst */, bigEndian);
            System.err.println("Read32.1 "+r32+", "+toHexBinaryString(r32, 32)+", "+toUnsignedBinaryString(r32));
            Assert.assertEquals(val32, r32);
        }

        // Test with written bitstream value
        bs.setStream(bs.getSubStream(), true /* outputMode */);
        bs.writeInt32(true /* msbFirst */, bigEndian, val32);
        bs.setStream(bs.getSubStream(), false /* outputMode */); // switch to input-mode, implies flush()
        {
            final int r32 = bs.readInt32(true /* msbFirst */, bigEndian);
            System.err.println("Read32.2 "+r32+", "+toHexBinaryString(r32, 32)+", "+toUnsignedBinaryString(r32));
            Assert.assertEquals(val32, r32);
        }
    }

    @Test
    public void test02Int32BitsUnaligned() throws IOException {
        test02Int32BitsUnalignedImpl(null);
        test02Int32BitsUnalignedImpl(ByteOrder.BIG_ENDIAN);
        test02Int32BitsUnalignedImpl(ByteOrder.LITTLE_ENDIAN);
    }
    void test02Int32BitsUnalignedImpl(ByteOrder byteOrder) throws IOException {
        test02Int32BitsUnalignedImpl(byteOrder, 0);
        test02Int32BitsUnalignedImpl(byteOrder, 1);
        test02Int32BitsUnalignedImpl(byteOrder, 7);
        test02Int32BitsUnalignedImpl(byteOrder, 8);
        test02Int32BitsUnalignedImpl(byteOrder, 15);
        test02Int32BitsUnalignedImpl(byteOrder, 24);
        test02Int32BitsUnalignedImpl(byteOrder, 25);
    }
    void test02Int32BitsUnalignedImpl(ByteOrder byteOrder, final int preBits) throws IOException {
        test02Int32BitsUnalignedImpl(byteOrder, preBits, 0);
        test02Int32BitsUnalignedImpl(byteOrder, preBits, 1);
        test02Int32BitsUnalignedImpl(byteOrder, preBits, -1);
        test02Int32BitsUnalignedImpl(byteOrder, preBits, 7);
        test02Int32BitsUnalignedImpl(byteOrder, preBits, 0x0fffffff);
        test02Int32BitsUnalignedImpl(byteOrder, preBits, Integer.MIN_VALUE);
        test02Int32BitsUnalignedImpl(byteOrder, preBits, Integer.MAX_VALUE);
        test02Int32BitsUnalignedImpl(byteOrder, preBits, 0xffffffff);
    }
    void test02Int32BitsUnalignedImpl(ByteOrder byteOrder, int preBits, int val32) throws IOException {
        final int preBytes = ( preBits + 7 ) >>> 3;
        final int byteCount = preBytes + Buffers.SIZEOF_INT;
        final ByteBuffer bb = ByteBuffer.allocate(byteCount);
        if( null != byteOrder ) {
            bb.order(byteOrder);
        }
        final boolean bigEndian = ByteOrder.BIG_ENDIAN == bb.order();
        System.err.println("XXX Test02Int32BitsUnaligned: byteOrder "+byteOrder+" (bigEndian "+bigEndian+"), preBits "+preBits+", value "+val32+", "+toHexBinaryString(val32, 32));
        System.err.println("XXX Test02Int32BitsUnaligned: "+toUnsignedBinaryString(val32));

        // Test with written bitstream value
        final Bitstream.ByteBufferStream bbs = new Bitstream.ByteBufferStream(bb);
        final Bitstream<ByteBuffer> bs = new Bitstream<ByteBuffer>(bbs, true /* outputMode */);
        bs.writeBits31(true /* msbFirst */, preBits, 0);
        bs.writeInt32(true /* msbFirst */, bigEndian, val32);
        bs.setStream(bs.getSubStream(), false /* outputMode */); // switch to input-mode, implies flush()

        final int rPre = bs.readBits31(true /* msbFirst */, preBits);
        final int r32 = bs.readInt32(true /* msbFirst */, bigEndian);
        System.err.println("ReadPre "+rPre+", "+toBinaryString(rPre, preBits));
        System.err.println("Read32 "+r32+", "+toHexBinaryString(r32, 32)+", "+toUnsignedBinaryString(r32));
        Assert.assertEquals(val32, r32);
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestBitstream04.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
