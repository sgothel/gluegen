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

import com.jogamp.common.os.*;
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
public abstract class AbstractLongBuffer extends AbstractBuffer {

    protected long[] backup;

    protected HashMap/*<aptr, buffer>*/ dataMap = new HashMap();

    static {
        NativeLibrary.ensureNativeLibLoaded();
    }

    protected AbstractLongBuffer(ByteBuffer bb, int elementSize) {
        super(bb, elementSize);

        backup = new long[capacity];
    }

    final void updateBackup() {
        for (int i = 0; i < capacity; i++) {
            backup[i] = get(i);
        }
    }

    public final boolean hasArray() {
        return true;
    }

    public final long[] array() {
        return backup;
    }

    /** Absolute get method. Get the pointer value at the given index */
    public abstract long get(int idx);

    /** Relative get method. Get the pointer value at the current position and increment the position by one. */
    public final long get() {
        long r = get(position);
        position++;
        return r;
    }

    /** 
     * Relative bulk get method. Copy the pointer values <code> [ position .. position+length [</code> 
     * to the destination array <code> [ dest[offset] .. dest[offset+length] [ </code>
     * and increment the position by <code>length</code>. */
    public final AbstractLongBuffer get(long[] dest, int offset, int length) {
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
    public abstract AbstractLongBuffer put(int index, long value);

    /** Relative put method. Put the pointer value at the current position and increment the position by one. */
    public final AbstractLongBuffer put(long value) {
        put(position, value);
        position++;
        return this;
    }

    /** 
     * Relative bulk put method. Put the pointer values <code> [ src[offset] .. src[offset+length] [</code> 
     * at the current position and increment the position by <code>length</code>. */
    public final AbstractLongBuffer put(long[] src, int offset, int length) {
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

    /** 
     * Relative bulk get method. Copy the source values <code> src[position .. capacity] [</code> 
     * to this buffer and increment the position by <code>capacity-position</code>. */
    public AbstractLongBuffer put(AbstractLongBuffer src) {
        if (remaining() < src.remaining()) {
            throw new IndexOutOfBoundsException();
        }
        while (src.hasRemaining()) {
                 put(src.get()); 
        }
        return this;
    }
}
