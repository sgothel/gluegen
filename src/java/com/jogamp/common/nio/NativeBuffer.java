/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
 * Created on Tuesday, March 30 2010 18:22
 */
package com.jogamp.common.nio;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Hardware independent container for various kinds of buffers.
 * <p>
 * Implementations follow {@link Buffer} semantics, e.g.
 * <pre>
 *       0 <= position <= limit <= capacity
 * </pre>
 * </p>
 * @author Sven Gothel
 * @author Michael Bien
 */
@SuppressWarnings("rawtypes")
public interface NativeBuffer<B extends NativeBuffer> {

    /** Returns byte size of one element */
    public int elementSize();

    /** Returns this buffer's element limit. */
    public int limit();

    /** Sets this buffer's element limit. */
    public B limit(int newLim);

    /** Returns this buffer's element capacity. */
    public int capacity();

    /** Returns this buffer's element position. */
    public int position();

    /** Sets this buffer's element position. */
    public B position(int newPos);

    /** Returns this buffer's remaining element, i.e. limit - position. */
    public int remaining();

    /** Returns {@link #remaining()} > 0 */
    public boolean hasRemaining();

    /** Sets the limit to the capacity and the position to zero. */
    public B clear();

    /** Sets the limit to the current position and the position to zero. */
    public B flip();

    /** Sets the position to zero. */
    public B rewind();

    /**
     * @return true if this buffer has a primitive backup array, otherwise false
     */
    public boolean hasArray();

    /**
     * @return the array offset of the optional primitive backup array of the buffer if {@link #hasArray()} is true,
     *         otherwise 0.
     */
    public int arrayOffset();

    /**
     * @return the primitive backup array of the buffer if {@link #hasArray()} is true,
     *         otherwise it throws {@link java.lang.UnsupportedOperationException}.
     *         The returned primitive array maybe of type <code>int[]</code> or <code>long[]</code>, etc ..
     * @throws UnsupportedOperationException if this object has no backup array
     * @see #hasArray()
     */
    public Object array() throws UnsupportedOperationException ;

    /** Returns the underlying buffer object. */
    public Buffer getBuffer();
    /** Return true if the underlying buffer is NIO direct, otherwise false. */
    public boolean isDirect();
    /** Returns the native address of the underlying buffer if {@link #isDirect()}, otherwise {@code 0}. */
    public long getDirectBufferAddress();
    /**
     * Store the {@link #getDirectBufferAddress()} into the given {@link ByteBuffer} using relative put.
     * <p>
     * The native pointer value is stored either as a 32bit (int) or 64bit (long) wide value,
     * depending of the CPU pointer width.
     * </p>
     */
    public void storeDirectAddress(final ByteBuffer directDest);
    /**
     * Store the {@link #getDirectBufferAddress()} into the given {@link ByteBuffer} using absolute put.
     * <p>
     * The native pointer value is stored either as a 32bit (int) or 64bit (long) wide value,
     * depending of the CPU pointer width.
     * </p>
     **/
    public void storeDirectAddress(final ByteBuffer directDest, final int destOffset);

    /**
     * Relative bulk get method. Copy the source values <code> src[position .. capacity] [</code>
     * to this buffer and increment the position by <code>capacity-position</code>.
     */
    public B put(B src);
}
