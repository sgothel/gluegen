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
 * Created on Saturday, March 27 2010 11:55
 */
package com.jogamp.common.nio;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.LongObjectHashMap;

/**
 * Hardware independent container for native pointer arrays.
 *
 * The native values (NIO direct ByteBuffer) might be 32bit or 64bit wide,
 * depending of the CPU pointer width.
 *
 * @author Sven Gothel
 * @author Michael Bien
 */
public class PointerBuffer extends AbstractBuffer<PointerBuffer> {
    public static final int ELEMENT_SIZE = Platform.is32Bit() ? Buffers.SIZEOF_INT : Buffers.SIZEOF_LONG ;
    protected LongObjectHashMap dataMap = null;

    static {
        Platform.initSingleton(); // loads native gluegen-rt library
    }

    /** no backup array, use for direct usage only */
    static PointerBuffer create(final ByteBuffer bb) {
        return Platform.is32Bit() ? new PointerBuffer( bb.asIntBuffer() ) : new PointerBuffer( bb.asLongBuffer() );
    }

    /** supports backup array */
    PointerBuffer(final IntBuffer b) {
        super(b, ELEMENT_SIZE, b.capacity());
    }

    /** supports backup array */
    PointerBuffer(final LongBuffer b) {
        super(b, ELEMENT_SIZE, b.capacity());
    }

    private final void validateDataMap() {
        if(null == dataMap) {
            dataMap = new LongObjectHashMap();
            dataMap.setKeyNotFoundValue(null);
        }
    }

    /** Returns a non direct PointerBuffer in native order, having a backup array */
    public static PointerBuffer allocate(final int size) {
        if (Platform.is32Bit()) {
            return new PointerBuffer(IntBuffer.wrap(new int[size]));
        } else {
            return new PointerBuffer(LongBuffer.wrap(new long[size]));
        }
    }

    /** Returns a direct PointerBuffer in native order, w/o backup array */
    public static PointerBuffer allocateDirect(final int size) {
        return create(Buffers.newDirectByteBuffer(ELEMENT_SIZE * size));
    }

    public static PointerBuffer wrap(final ByteBuffer src) {
        return create(src);
    }

    /**
     * @return new PointerBuffer sharing the same buffer data of this instance (identity),
     *         but having an independent position.
     */
    public final PointerBuffer duplicate() {
        PointerBuffer npb;
        if (Platform.is32Bit()) {
            npb = new PointerBuffer((IntBuffer)buffer);
        } else {
            npb = new PointerBuffer((LongBuffer)buffer);
        }
        if(null != dataMap) {
            npb.dataMap = (LongObjectHashMap) dataMap.clone();
        }
        npb.position = position;
        return npb;
    }

    /**
     * Relative bulk get method. Copy the source values <code> src[position .. capacity] [</code>
     * to this buffer and increment the position by <code>capacity-position</code>. */
    @Override
    public final PointerBuffer put(final PointerBuffer src) {
        if (remaining() < src.remaining()) {
            throw new IndexOutOfBoundsException();
        }
        if( null == src.dataMap && null == dataMap ) {
            // fast path no dataMap usage on both
            while (src.hasRemaining()) {
                put(src.get());
            }
        } else {
            while (src.hasRemaining()) {
                 final long addr = src.get();
                 put(addr);
                 if( null != src.dataMap) {
                     final Buffer bb = (Buffer) src.dataMap.get(addr);
                     if(null!=bb) {
                         validateDataMap();
                         dataMap.put(addr, bb);
                     } else if( null != dataMap ) {
                         dataMap.remove(addr);
                     }
                 } else if( null != dataMap ) {
                     dataMap.remove(addr);
                 }
            }
        }
        return this;
    }

    /** Relative get method. Get the pointer value at the current position and increment the position by one. */
    @Override
    public final long get() {
        final long r = get(position);
        position++;
        return r;
    }

    /** Absolute get method. Get the pointer value at the given index */
    @Override
    public final long get(final int idx) {
        if (0 > idx || idx >= capacity) {
            throw new IndexOutOfBoundsException();
        }
        if (Platform.is32Bit()) {
            return ((IntBuffer) buffer).get(idx)  & 0x00000000FFFFFFFFL;
        } else {
            return ((LongBuffer) buffer).get(idx);
        }
    }

    /**
     * Relative bulk get method. Copy the pointer values <code> [ position .. position+length [</code>
     * to the destination array <code> [ dest[offset] .. dest[offset+length] [ </code>
     * and increment the position by <code>length</code>. */
    public final PointerBuffer get(final long[] dest, int offset, int length) {
        if (dest.length<offset+length) {
            throw new IndexOutOfBoundsException();
        }
        if (remaining() < length) {
            throw new IndexOutOfBoundsException();
        }
        while(length>0) {
            dest[offset++] = get(position++);
            length--;
        }
        return this;
    }

    /** Absolute put method. Put the pointer value at the given index */
    @Override
    public final PointerBuffer put(final int idx, final long v) {
        if (0 > idx || idx >= capacity) {
            throw new IndexOutOfBoundsException();
        }
        if (Platform.is32Bit()) {
            ((IntBuffer) buffer).put(idx, (int) v);
        } else {
            ((LongBuffer) buffer).put(idx, v);
        }
        return this;
    }

    /** Relative put method. Put the pointer value at the current position and increment the position by one. */
    @Override
    public final PointerBuffer put(final long value) {
        put(position, value);
        position++;
        return this;
    }

    /**
     * Relative bulk put method. Put the pointer values <code> [ src[offset] .. src[offset+length] [</code>
     * at the current position and increment the position by <code>length</code>. */
    public final PointerBuffer put(final long[] src, int offset, int length) {
        if (src.length<offset+length) {
            throw new IndexOutOfBoundsException();
        }
        if (remaining() < length) {
            throw new IndexOutOfBoundsException();
        }
        while(length>0) {
            put(position++, src[offset++]);
            length--;
        }
        return this;
    }

    /** Put the address of the given direct Buffer at the given position
        of this pointer array.
        Adding a reference of the given direct Buffer to this object.

        @throws IllegalArgumentException if bb is null or not a direct buffer
     */
    public final PointerBuffer referenceBuffer(final int index, final Buffer bb) {
        if(null==bb) {
            throw new IllegalArgumentException("Buffer is null");
        }
        if(!Buffers.isDirect(bb)) {
            throw new IllegalArgumentException("Buffer is not direct");
        }
        final long mask = Platform.is32Bit() ?  0x00000000FFFFFFFFL : 0xFFFFFFFFFFFFFFFFL ;
        final long bbAddr = getDirectBufferAddressImpl(bb) & mask;
        if(0==bbAddr) {
            throw new RuntimeException("Couldn't determine native address of given Buffer: "+bb);
        }
        validateDataMap();
        put(index, bbAddr);
        dataMap.put(bbAddr, bb);
        return this;
    }

    /** Put the address of the given direct Buffer at the end
        of this pointer array.
        Adding a reference of the given direct Buffer to this object. */
    public final PointerBuffer referenceBuffer(final Buffer bb) {
        referenceBuffer(position, bb);
        position++;
        return this;
    }

    public final Buffer getReferencedBuffer(final int index) {
        if(null != dataMap) {
            final long addr = get(index);
            return (Buffer) dataMap.get(addr);
        }
        return null;
    }

    public final Buffer getReferencedBuffer() {
        final Buffer bb = getReferencedBuffer(position);
        position++;
        return bb;
    }

    private native long getDirectBufferAddressImpl(Object directBuffer);

    @Override
    public String toString() {
        return "PointerBuffer:"+super.toString();
    }
}
