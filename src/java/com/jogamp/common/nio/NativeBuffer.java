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
 * Created on Tuesday, March 30 2010 18:22
 */
package com.jogamp.common.nio;

import java.nio.Buffer;

/**
 * Hardware independent container for various kinds of buffers.
 *
 * @author Sven Gothel
 * @author Michael Bien
 */
@SuppressWarnings("rawtypes")
public interface NativeBuffer<B extends NativeBuffer> {

    public int elementSize();

    public int limit();

    public int capacity();

    public int position();

    public B position(int newPos);

    public int remaining();

    public boolean hasRemaining();

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

    public Buffer getBuffer();

    public boolean isDirect();

    public B rewind();

    public B put(int index, long value);

    public B put(long value);

    public B put(B src);

    public long get();

    public long get(int idx);
}
