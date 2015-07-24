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
 * Created on Sunday, July 04 2010 20:00
 */
package com.jogamp.common.nio;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;

import static org.junit.Assert.*;

/**
 * @author Michael Bien
 */
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BuffersTest extends SingletonJunitCase {

    @Test
    public void positionLimitCapacityAfterArrayAllocation() {
        byte[] byteData = { 1, 2, 3, 4, 5, 6, 7, 8 };
        final ByteBuffer byteBuffer = Buffers.newDirectByteBuffer(byteData);
        assertEquals(0, byteBuffer.position());
        assertEquals(8, byteBuffer.limit());
        assertEquals(8, byteBuffer.capacity());
        assertEquals(5, byteBuffer.get(4));

        double[] doubleData = { 1, 2 };
        final DoubleBuffer doubleBuffer = Buffers.newDirectDoubleBuffer(doubleData);
        assertEquals(0, doubleBuffer.position());
        assertEquals(2, doubleBuffer.limit());
        assertEquals(2, doubleBuffer.capacity());

        float[] floatData = { 1.0f, 2.0f, 3.0f };
        final FloatBuffer floatBuffer = Buffers.newDirectFloatBuffer(floatData);
        assertEquals(0, floatBuffer.position());
        assertEquals(3, floatBuffer.limit());
        assertEquals(3, floatBuffer.capacity());

        int[] intData = { 1, 2, 3, 4 };
        final IntBuffer intBuffer = Buffers.newDirectIntBuffer(intData);
        assertEquals(0, intBuffer.position());
        assertEquals(4, intBuffer.limit());
        assertEquals(4, intBuffer.capacity());
        assertEquals(3, intBuffer.get(2));

        long[] longData = { 1, 2, 3, 4, 5 };
        final LongBuffer longBuffer = Buffers.newDirectLongBuffer(longData);
        assertEquals(0, longBuffer.position());
        assertEquals(5, longBuffer.limit());
        assertEquals(5, longBuffer.capacity());
        assertEquals(4, longBuffer.get(3));

        short[] shortData = { 1, 2, 3, 4, 5, 6 };
        final ShortBuffer shortBuffer = Buffers.newDirectShortBuffer(shortData);
        assertEquals(0, shortBuffer.position());
        assertEquals(6, shortBuffer.limit());
        assertEquals(6, shortBuffer.capacity());
        assertEquals(4, shortBuffer.get(3));

        char[] charData = { 1, 2, 3, 4, 5, 6, 7 };
        final CharBuffer charBuffer = Buffers.newDirectCharBuffer(charData);
        assertEquals(0, charBuffer.position());
        assertEquals(7, charBuffer.limit());
        assertEquals(7, charBuffer.capacity());
        assertEquals(6, charBuffer.get(5));
    }

    @Test
    public void slice() {

        final IntBuffer buffer = Buffers.newDirectIntBuffer(6);
        buffer.put(new int[]{1,2,3,4,5,6}).rewind();

        final IntBuffer threefour = Buffers.slice(buffer, 2, 2);

        assertEquals(3, threefour.get(0));
        assertEquals(4, threefour.get(1));
        assertEquals(2, threefour.capacity());

        assertEquals(0, buffer.position());
        assertEquals(6, buffer.limit());

        final IntBuffer fourfivesix = Buffers.slice(buffer, 3, 3);

        assertEquals(4, fourfivesix.get(0));
        assertEquals(5, fourfivesix.get(1));
        assertEquals(6, fourfivesix.get(2));
        assertEquals(3, fourfivesix.capacity());

        assertEquals(0, buffer.position());
        assertEquals(6, buffer.limit());

        final IntBuffer onetwothree = Buffers.slice(buffer, 0, 3);

        assertEquals(1, onetwothree.get(0));
        assertEquals(2, onetwothree.get(1));
        assertEquals(3, onetwothree.get(2));
        assertEquals(3, onetwothree.capacity());

        assertEquals(0, buffer.position());
        assertEquals(6, buffer.limit());

        // is it really sliced?
        buffer.put(2, 42);

        assertEquals(42, buffer.get(2));
        assertEquals(42, onetwothree.get(2));


    }

}
