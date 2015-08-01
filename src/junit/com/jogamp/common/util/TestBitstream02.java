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

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.nio.Buffers;
import com.jogamp.junit.util.SingletonJunitCase;

import static com.jogamp.common.util.BitDemoData.*;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test {@link Bitstream} w/ int8 read/write access w/ semantics
 * as well as with aligned and unaligned access.
 * <ul>
 *  <li>{@link Bitstream#readInt8(boolean, boolean)}</li>
 *  <li>{@link Bitstream#writeInt8(boolean, boolean, byte)}</li>
 * </ul>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBitstream02 extends SingletonJunitCase {

    @Test
    public void test01Int8BitsAligned() throws IOException {
        test01Int8BitsAlignedImpl((byte)0);
        test01Int8BitsAlignedImpl((byte)1);
        test01Int8BitsAlignedImpl((byte)7);
        test01Int8BitsAlignedImpl(Byte.MIN_VALUE);
        test01Int8BitsAlignedImpl(Byte.MAX_VALUE);
        test01Int8BitsAlignedImpl((byte)0xff);
    }
    void test01Int8BitsAlignedImpl(final byte val8) throws IOException {
        // Test with buffer defined value
        final ByteBuffer bb = ByteBuffer.allocate(Buffers.SIZEOF_BYTE);
        System.err.println("XXX Test01Int8BitsAligned: value "+val8+", "+toHexBinaryString(val8, 8));
        bb.put(0, val8);

        final Bitstream.ByteBufferStream bbs = new Bitstream.ByteBufferStream(bb);
        final Bitstream<ByteBuffer> bs = new Bitstream<ByteBuffer>(bbs, false /* outputMode */);
        {
            final byte r8 = (byte) bs.readUInt8();
            System.err.println("Read8.1 "+r8+", "+toHexBinaryString(r8, 8));
            Assert.assertEquals(val8, r8);
        }

        // Test with written bitstream value
        bs.setStream(bs.getSubStream(), true /* outputMode */);
        bs.writeInt8(val8);
        bs.setStream(bs.getSubStream(), false /* outputMode */); // switch to input-mode, implies flush()
        {
            final byte r8 = (byte) bs.readUInt8();
            System.err.println("Read8.2 "+r8+", "+toHexBinaryString(r8, 8));
            Assert.assertEquals(val8, r8);
        }
    }

    @Test
    public void test02Int8BitsUnaligned() throws IOException {
        test02Int8BitsUnalignedImpl(0);
        test02Int8BitsUnalignedImpl(1);
        test02Int8BitsUnalignedImpl(7);
        test02Int8BitsUnalignedImpl(8);
        test02Int8BitsUnalignedImpl(15);
        test02Int8BitsUnalignedImpl(24);
        test02Int8BitsUnalignedImpl(25);
    }
    void test02Int8BitsUnalignedImpl(final int preBits) throws IOException {
        test02Int8BitsUnalignedImpl(preBits, (byte)0);
        test02Int8BitsUnalignedImpl(preBits, (byte)1);
        test02Int8BitsUnalignedImpl(preBits, (byte)7);
        test02Int8BitsUnalignedImpl(preBits, Byte.MIN_VALUE);
        test02Int8BitsUnalignedImpl(preBits, Byte.MAX_VALUE);
        test02Int8BitsUnalignedImpl(preBits, (byte)0xff);
    }
    void test02Int8BitsUnalignedImpl(final int preBits, final byte val8) throws IOException {
        final int preBytes = ( preBits + 7 ) >>> 3;
        final int byteCount = preBytes + Buffers.SIZEOF_BYTE;
        final ByteBuffer bb = ByteBuffer.allocate(byteCount);
        System.err.println("XXX Test02Int8BitsUnaligned: preBits "+preBits+", value "+val8+", "+toHexBinaryString(val8, 8));

        // Test with written bitstream value
        final Bitstream.ByteBufferStream bbs = new Bitstream.ByteBufferStream(bb);
        final Bitstream<ByteBuffer> bs = new Bitstream<ByteBuffer>(bbs, true /* outputMode */);
        bs.writeBits31(preBits, 0);
        bs.writeInt8(val8);
        bs.setStream(bs.getSubStream(), false /* outputMode */); // switch to input-mode, implies flush()

        final int rPre = (short) bs.readBits31(preBits);
        final byte r8 = (byte) bs.readUInt8();
        System.err.println("ReadPre "+rPre+", "+toBinaryString(rPre, preBits));
        System.err.println("Read8 "+r8+", "+toHexBinaryString(r8, 8));
        Assert.assertEquals(val8, r8);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestBitstream02.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
