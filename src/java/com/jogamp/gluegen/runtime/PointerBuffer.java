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
 * Created on Saturday, March 27 2010 11:55
 */
package com.jogamp.gluegen.runtime;

import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.util.HashMap;

/**
 * Hardware independent container for native pointer arrays.
 *
 * The native values (NIO direct ByteBuffer) might be 32bit or 64bit wide,
 * depending of the CPU pointer width.
 *
 * @author Michael Bien
 * @author Sven Gothel
 */
public abstract class PointerBuffer implements NativeBuffer/*<PointerBuffer>*/ {

    protected final ByteBuffer bb;
    protected int capacity;
    protected int position;
    protected long[] backup;

    protected HashMap/*<aptr, buffer>*/ dataMap = new HashMap();

    static {
        NativeLibrary.ensureNativeLibLoaded();
    }

    protected PointerBuffer(ByteBuffer bb) {
        this.bb = bb;
    }

    public static PointerBuffer allocate(int size) {
        if (Platform.isJavaSE()) {
            return new PointerBufferSE(ByteBuffer.wrap(new byte[elementSize() * size]));
        } else {
            return new PointerBufferME_CDC_FP(ByteBuffer.wrap(new byte[elementSize() * size]));
        }
    }

    public static PointerBuffer allocateDirect(int size) {
        if (Platform.isJavaSE()) {
            return new PointerBufferSE(Buffers.newDirectByteBuffer(elementSize() * size));
        } else {
            return new PointerBufferME_CDC_FP(Buffers.newDirectByteBuffer(elementSize() * size));
        }
    }

    public static PointerBuffer wrap(ByteBuffer src) {
        PointerBuffer res;
        if (Platform.isJavaSE()) {
            res = new PointerBufferSE(src);
        } else {
            res = new PointerBufferME_CDC_FP(src);
        }
        res.updateBackup();
        return res;

    }

    void updateBackup() {
        for (int i = 0; i < capacity; i++) {
            backup[i] = get(i);
        }
    }

    int arrayOffset() {
        return 0;
    }

    public static int elementSize() {
        return Platform.is32Bit() ? Buffers.SIZEOF_INT : Buffers.SIZEOF_LONG;
    }

    public int limit() {
        return capacity;
    }

    public int capacity() {
        return capacity;
    }

    public int position() {
        return position;
    }

    public PointerBuffer position(int newPos) {
        if (0 > newPos || newPos >= capacity) {
            throw new IndexOutOfBoundsException("Sorry to interrupt, but the position "+newPos+" was out of bounds. " +
                                                "My capacity is "+capacity()+".");
        }
        position = newPos;
        return this;
    }

    public int remaining() {
        return capacity - position;
    }

    public boolean hasRemaining() {
        return position < capacity;
    }

    public PointerBuffer rewind() {
        position = 0;
        return this;
    }

    boolean hasArray() {
        return true;
    }

    public long[] array() {
        return backup;
    }

    public ByteBuffer getBuffer() {
        return bb;
    }

    public boolean isDirect() {
        return bb.isDirect();
    }

    public long get() {
        long r = get(position);
        position++;
        return r;
    }

    public abstract long get(int idx);

    /** put the pointer value at position index */
    public abstract PointerBuffer put(int index, long value);

    /** put the pointer value at the end */
    public abstract PointerBuffer put(long value);

    /** Put the address of the given direct Buffer at the given position
        of this pointer array.
        Adding a reference of the given direct Buffer to this object. */
    public PointerBuffer referenceBuffer(int index, Buffer bb) {
        if(null==bb) {
            throw new RuntimeException("Buffer is null");
        }
        if(!bb.isDirect()) {
            throw new RuntimeException("Buffer is not direct");
        }
        long bbAddr = getDirectBufferAddressImpl(bb);
        if(0==bbAddr) {
            throw new RuntimeException("Couldn't determine native address of given Buffer: "+bb);
        }

        put(index, bbAddr);
        dataMap.put(new Long(bbAddr), bb);
        return this;
    }

    /** Put the address of the given direct Buffer at the end
        of this pointer array.
        Adding a reference of the given direct Buffer to this object. */
    public PointerBuffer referenceBuffer(Buffer bb) {
        referenceBuffer(position, bb);
        position++;
        return this;
    }

    public Buffer getReferencedBuffer(int index) {
        long addr = get(index);
        return (Buffer) dataMap.get(new Long(addr));
    }

    public Buffer getReferencedBuffer() {
        Buffer bb = getReferencedBuffer(position);
        position++;
        return bb;
    }

    private native long getDirectBufferAddressImpl(Object directBuffer);

    public PointerBuffer put(PointerBuffer src) {
        if (remaining() < src.remaining()) {
            throw new IndexOutOfBoundsException();
        }
        while (src.hasRemaining()) {
                 put(src.get()); 
        }
        return this;
    }

    public String toString() {
        return "PointerBuffer[capacity "+capacity+", position "+position+", elementSize "+elementSize()+", ByteBuffer.capacity "+bb.capacity()+"]";
    }

}
