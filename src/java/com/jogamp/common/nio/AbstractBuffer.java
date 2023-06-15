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
 * Created on Saturday, March 27 2010 11:55
 */
package com.jogamp.common.nio;

import com.jogamp.common.os.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 * @author Sven Gothel
 * @author Michael Bien
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractBuffer<B extends AbstractBuffer> implements NativeBuffer<B> {
    /** Platform dependent pointer size in bytes, i.e. 32bit or 64bit wide, depending of the CPU pointer width. */
    public static final int POINTER_SIZE;

    protected final Buffer buffer;
    protected final int elementSize;
    protected final int capacity;
    protected int position;

    static {
        Platform.initSingleton(); // loads native gluegen_rt library
        POINTER_SIZE = Platform.is32Bit() ? Buffers.SIZEOF_INT : Buffers.SIZEOF_LONG ;
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
    public long getDirectBufferAddress() {
        if( isDirect() ) {
            return Buffers.getDirectBufferAddressImpl(buffer);
        } else {
            return 0;
        }
    }
    @Override
    public void storeDirectAddress(final ByteBuffer directDest) {
        final long addr = getDirectBufferAddress();
        switch(POINTER_SIZE) {
            case 4:
                directDest.putInt(0, (int) ( addr & 0x00000000FFFFFFFFL ) );
                break;
            case 8:
                directDest.putLong(0, addr);
                break;
        }
        directDest.position(directDest.position()+POINTER_SIZE);
    }

    @Override
    public void storeDirectAddress(final ByteBuffer directDest, final int destBytePos) {
        final long addr = getDirectBufferAddress();
        switch(POINTER_SIZE) {
            case 4:
                directDest.putInt(destBytePos, (int) ( addr & 0x00000000FFFFFFFFL ) );
                break;
            case 8:
                directDest.putLong(destBytePos, addr);
                break;
        }
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

    protected String toSubString() {
        return "[direct["+isDirect()+", addr 0x"+Long.toHexString(getDirectBufferAddress())+"], hasArray "+hasArray()+", capacity "+capacity+", position "+position+", elementSize "+elementSize+", buffer[capacity "+buffer.capacity()+", lim "+buffer.limit()+", pos "+buffer.position()+"]]";
    }
    @Override
    public String toString() {
        return "AbstractBuffer"+toSubString();
    }
}
