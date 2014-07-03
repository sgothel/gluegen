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

import java.nio.Buffer;

/**
 * @author Sven Gothel
 * @author Michael Bien
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractBuffer<B extends AbstractBuffer> implements NativeBuffer<B> {

    protected final Buffer buffer;
    protected final int elementSize;
    protected final int capacity;
    protected int position;

    static {
        Platform.initSingleton(); // loads native gluegen-rt library
    }

    /**
     * capacity and elementSize should be match the equation w/ target buffer type
     * <pre>
     *    capacity = elementSizeInBytes(buffer) * buffer.capacity() ) / elementSize
     * </pre>
     * @param buffer shall be in target format.
     * @param elementSize the target element size in bytes.
     * @param capacity the target capacity in elements of size <code>elementSize</code>.
     */
    protected AbstractBuffer(final Buffer buffer, final int elementSize, final int capacity) {
        this.buffer = buffer;
        this.elementSize = elementSize;
        this.capacity = capacity;

        this.position = 0;
    }

    @Override
    public final int elementSize() {
        return elementSize;
    }

    @Override
    public final int limit() {
        return capacity;
    }

    @Override
    public final int capacity() {
        return capacity;
    }

    @Override
    public final int position() {
        return position;
    }

    @Override
    public final B position(final int newPos) {
        if (0 > newPos || newPos >= capacity) {
            throw new IndexOutOfBoundsException("Sorry to interrupt, but the position "+newPos+" was out of bounds. " +
                                                "My capacity is "+capacity()+".");
        }
        position = newPos;
        return (B)this;
    }

    @Override
    public final int remaining() {
        return capacity - position;
    }

    @Override
    public final boolean hasRemaining() {
        return position < capacity;
    }

    @Override
    public final B rewind() {
        position = 0;
        return (B) this;
    }

    @Override
    public final Buffer getBuffer() {
        return buffer;
    }

    @Override
    public final boolean isDirect() {
        return buffer.isDirect();
    }

    @Override
    public final boolean hasArray() {
        return buffer.hasArray();
    }

    @Override
    public final int arrayOffset() {
        if( hasArray() ) {
            return buffer.arrayOffset();
        } else {
            return 0;
        }
    }

    @Override
    public Object array() throws UnsupportedOperationException {
        return buffer.array();
    }

    @Override
    public String toString() {
        return "AbstractBuffer[direct "+isDirect()+", hasArray "+hasArray()+", capacity "+capacity+", position "+position+", elementSize "+elementSize+", buffer[capacity "+buffer.capacity()+", lim "+buffer.limit()+", pos "+buffer.position()+"]]";
    }

}
