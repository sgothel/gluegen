
/*
 * Copyright 2011 JogAmp Community. All rights reserved.
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
 * Created on Sunday, February 13 2011 15:17
 */
package com.jogamp.common.nio;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Buffer factory attempting to reduce buffer creation overhead.
 * Direct ByteBuffers must be page aligned which increases creation overhead of
 * small buffers significantly.
 * This factory can be used as fixed size static or or dynamic allocating
 * factory. The initial size and allocation size is configurable.
 * <p>
 * Fixed size factories may be used in systems with hard realtime requirements
 * and/or predictable memory usage.
 * </p>
 * <p>
 * concurrency info:<br/>
 * <ul>
 * <li>all create methods are threadsafe</li>
 * <li>factories created with create(...) are <b>not</b> threadsafe</li>
 * <li>factories created with createSynchronized(...) are threadsafe</li>
 * </ul>
 * </p>
 *
 * @author Michael Bien
 */
public class CachedBufferFactory {

    /**
     * default size for internal buffer allocation.
     */
    public static final int DEFAULT_ALLOCATION_SIZE = 1024 * 1024;

    private final int ALLOCATION_SIZE;
    private ByteBuffer currentBuffer;

    private CachedBufferFactory() {
        this(DEFAULT_ALLOCATION_SIZE, DEFAULT_ALLOCATION_SIZE);
    }

    private CachedBufferFactory(final int initialSize, final int allocationSize) {
        currentBuffer = Buffers.newDirectByteBuffer(initialSize);
        ALLOCATION_SIZE = allocationSize;
    }


    /**
     * Creates a factory with initial size and allocation size set to
     * {@link #DEFAULT_ALLOCATION_SIZE}.
     */
    public static CachedBufferFactory create() {
        return new CachedBufferFactory();
    }

    /**
     * Creates a factory with the specified initial size. The allocation size is set to
     * {@link #DEFAULT_ALLOCATION_SIZE}.
     */
    public static CachedBufferFactory create(final int initialSize) {
        return new CachedBufferFactory(initialSize, DEFAULT_ALLOCATION_SIZE);
    }

    /**
     * Creates a factory with the specified initial size. The allocation size is set to
     * {@link #DEFAULT_ALLOCATION_SIZE}.
     * @param fixed Creates a fixed size factory which will handle overflows (initial size)
     * with RuntimeExceptions.
     */
    public static CachedBufferFactory create(final int initialSize, final boolean fixed) {
        return new CachedBufferFactory(initialSize, fixed?-1:DEFAULT_ALLOCATION_SIZE);
    }

    /**
     * Creates a factory with the specified initial size and allocation size.
     */
    public static CachedBufferFactory create(final int initialSize, final int allocationSize) {
        return new CachedBufferFactory(initialSize, allocationSize);
    }


    /**
     * Synchronized version of {@link #create()}.
     */
    public static CachedBufferFactory createSynchronized() {
        return new SynchronizedCachedBufferFactory();
    }

    /**
     * Synchronized version of {@link #create(int)}.
     */
    public static CachedBufferFactory createSynchronized(final int initialSize) {
        return new SynchronizedCachedBufferFactory(initialSize, DEFAULT_ALLOCATION_SIZE);
    }

    /**
     * Synchronized version of {@link #create(int, boolean)}.
     */
    public static CachedBufferFactory createSynchronized(final int initialSize, final boolean fixed) {
        return new SynchronizedCachedBufferFactory(initialSize, fixed?-1:DEFAULT_ALLOCATION_SIZE);
    }

    /**
     * Synchronized version of {@link #create(int, int)}.
     */
    public static CachedBufferFactory createSynchronized(final int initialSize, final int allocationSize) {
        return new CachedBufferFactory(initialSize, allocationSize);
    }

    /**
     * Returns true only if this factory does not allow to allocate more buffers
     * as limited by the initial size.
     */
    public boolean isFixed() {
        return ALLOCATION_SIZE == -1;
    }

    /**
     * Returns the allocation size used to create new internal buffers.
     * 0 means that the buffer will not grows, see {@link #isFixed()}.
     */
    public int getAllocationSize() {
        return ALLOCATION_SIZE;
    }

    /**
     * @return true if buffer cannot grow, otherwise false
     */
    private void checkIfFixed() {
        if(ALLOCATION_SIZE == 0) {
            throw new RuntimeException("fixed size buffer factory ran out ouf bounds.");
        }
    }

    public void destroy() {
        if(null != currentBuffer) {
            currentBuffer.clear();
            currentBuffer = null;
        }
    }
    public ByteBuffer newDirectByteBuffer(final int size) {

        // if large enough... just create it
        if (size > currentBuffer.capacity()) {
            checkIfFixed();
            return Buffers.newDirectByteBuffer(size);
        }

        // create new internal buffer if the old is running full
        if (size > currentBuffer.remaining()) {
            checkIfFixed();
            currentBuffer = Buffers.newDirectByteBuffer(ALLOCATION_SIZE);
        }

        currentBuffer.limit(currentBuffer.position() + size);
        final ByteBuffer result = currentBuffer.slice().order(currentBuffer.order());
        currentBuffer.position(currentBuffer.limit());
        currentBuffer.limit(currentBuffer.capacity());
        return result;
    }


    public ByteBuffer newDirectByteBuffer(final byte[] values, final int offset, final int lenght) {
        return (ByteBuffer)newDirectByteBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public ByteBuffer newDirectByteBuffer(final byte[] values, final int offset) {
        return newDirectByteBuffer(values, offset, values.length-offset);
    }

    public ByteBuffer newDirectByteBuffer(final byte[] values) {
        return newDirectByteBuffer(values, 0);
    }

    public DoubleBuffer newDirectDoubleBuffer(final int numElements) {
        return newDirectByteBuffer(numElements * Buffers.SIZEOF_DOUBLE).asDoubleBuffer();
    }

    public DoubleBuffer newDirectDoubleBuffer(final double[] values, final int offset, final int lenght) {
        return (DoubleBuffer)newDirectDoubleBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public DoubleBuffer newDirectDoubleBuffer(final double[] values, final int offset) {
        return newDirectDoubleBuffer(values, offset, values.length - offset);
    }

    public DoubleBuffer newDirectDoubleBuffer(final double[] values) {
        return newDirectDoubleBuffer(values, 0);
    }

    public FloatBuffer newDirectFloatBuffer(final int numElements) {
        return newDirectByteBuffer(numElements * Buffers.SIZEOF_FLOAT).asFloatBuffer();
    }

    public FloatBuffer newDirectFloatBuffer(final float[] values, final int offset, final int lenght) {
        return (FloatBuffer)newDirectFloatBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public FloatBuffer newDirectFloatBuffer(final float[] values, final int offset) {
        return newDirectFloatBuffer(values, offset, values.length - offset);
    }

    public FloatBuffer newDirectFloatBuffer(final float[] values) {
        return newDirectFloatBuffer(values, 0);
    }

    public IntBuffer newDirectIntBuffer(final int numElements) {
        return newDirectByteBuffer(numElements * Buffers.SIZEOF_INT).asIntBuffer();
    }

    public IntBuffer newDirectIntBuffer(final int[] values, final int offset, final int lenght) {
        return (IntBuffer)newDirectIntBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public IntBuffer newDirectIntBuffer(final int[] values, final int offset) {
        return newDirectIntBuffer(values, offset, values.length - offset);
    }

    public IntBuffer newDirectIntBuffer(final int[] values) {
        return newDirectIntBuffer(values, 0);
    }

    public LongBuffer newDirectLongBuffer(final int numElements) {
        return newDirectByteBuffer(numElements * Buffers.SIZEOF_LONG).asLongBuffer();
    }

    public LongBuffer newDirectLongBuffer(final long[] values, final int offset, final int lenght) {
        return (LongBuffer)newDirectLongBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public LongBuffer newDirectLongBuffer(final long[] values, final int offset) {
        return newDirectLongBuffer(values, offset, values.length - offset);
    }

    public LongBuffer newDirectLongBuffer(final long[] values) {
        return newDirectLongBuffer(values, 0);
    }

    public ShortBuffer newDirectShortBuffer(final int numElements) {
        return newDirectByteBuffer(numElements * Buffers.SIZEOF_SHORT).asShortBuffer();
    }

    public ShortBuffer newDirectShortBuffer(final short[] values, final int offset, final int lenght) {
        return (ShortBuffer)newDirectShortBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public ShortBuffer newDirectShortBuffer(final short[] values, final int offset) {
        return newDirectShortBuffer(values, offset, values.length - offset);
    }

    public ShortBuffer newDirectShortBuffer(final short[] values) {
        return newDirectShortBuffer(values, 0);
    }

    public CharBuffer newDirectCharBuffer(final int numElements) {
        return newDirectByteBuffer(numElements * Buffers.SIZEOF_SHORT).asCharBuffer();
    }

    public CharBuffer newDirectCharBuffer(final char[] values, final int offset, final int lenght) {
        return (CharBuffer)newDirectCharBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public CharBuffer newDirectCharBuffer(final char[] values, final int offset) {
        return newDirectCharBuffer(values, offset, values.length - offset);
    }

    public CharBuffer newDirectCharBuffer(final char[] values) {
        return newDirectCharBuffer(values, 0);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CachedBufferFactory other = (CachedBufferFactory) obj;
        if (this.ALLOCATION_SIZE != other.ALLOCATION_SIZE) {
            return false;
        }
        if (this.currentBuffer != other.currentBuffer && (this.currentBuffer == null || !this.currentBuffer.equals(other.currentBuffer))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getName()+"[static:"+isFixed()+" alloc size:"+getAllocationSize()+"]";
    }


    // nothing special, just synchronized
    private static class SynchronizedCachedBufferFactory extends CachedBufferFactory {

        private SynchronizedCachedBufferFactory() {
            super();
        }

        private SynchronizedCachedBufferFactory(final int size, final int step) {
            super(size, step);
        }

        @Override
        public synchronized ByteBuffer newDirectByteBuffer(final int size) {
            return super.newDirectByteBuffer(size);
        }

    }

}
