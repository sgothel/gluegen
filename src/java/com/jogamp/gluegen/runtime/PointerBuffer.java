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

/**
 * Hardware independent container for native long- and pointer arrays.
 * @author Michael Bien
 * @author Sven Gothel
 */
public abstract class PointerBuffer {

    protected final ByteBuffer bb;
    protected int capacity;
    protected int position;
    protected long[] backup;

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
        return CPU.is32Bit() ? Buffers.SIZEOF_INT : Buffers.SIZEOF_LONG;
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

    public abstract PointerBuffer put(int index, long value);

    public abstract PointerBuffer put(long value);

}
