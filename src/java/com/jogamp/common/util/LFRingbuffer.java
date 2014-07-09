/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.common.util;

import java.io.PrintStream;
import java.lang.reflect.Array;

/**
 * Simple implementation of {@link Ringbuffer},
 * exposing <i>lock-free</i>
 * {@link #get() get*(..)} and {@link #put(Object) put*(..)} methods.
 * <p>
 * Implementation utilizes the <i>Always Keep One Slot Open</i>,
 * hence implementation maintains an internal array of <code>capacity</code> <i>plus one</i>!
 * </p>
 * <p>
 * Implementation is thread safe if:
 * <ul>
 *   <li>{@link #get() get*(..)} operations are performed from one thread only.</li>
 *   <li>{@link #put(Object) put*(..)} operations are performed from one thread only.</li>
 *   <li>{@link #get() get*(..)} and {@link #put(Object) put*(..)} thread may be the same.</li>
 * </ul>
 * </p>
 * <p>
 * Following methods utilize global synchronization:
 * <ul>
 *  <li>{@link #resetFull(Object[])}</li>
 *  <li>{@link #clear()}</li>
 *  <li>{@link #growEmptyBuffer(Object[])}</li>
 * </ul>
 * User needs to synchronize above methods w/ the lock-free
 * w/ {@link #get() get*(..)} and {@link #put(Object) put*(..)} methods,
 * e.g. by controlling their threads before invoking the above.
 * </p>
 * <p>
 * Characteristics:
 * <ul>
 *   <li>Read position points to the last read element.</li>
 *   <li>Write position points to the last written element.</li>
 * </ul>
 * <table border="1">
 *   <tr><td>Empty</td><td>writePos == readPos</td><td>size == 0</td></tr>
 *   <tr><td>Full</td><td>writePos == readPos - 1</td><td>size == capacity</td></tr>
 * </table>
 * </p>
 */
public class LFRingbuffer<T> implements Ringbuffer<T> {

    private final Object syncRead = new Object();
    private final Object syncWrite = new Object();
    private final Object syncGlobal = new Object();
    private /* final */ volatile T[] array;     // not final due to grow
    private /* final */ volatile int capacityPlusOne;  // not final due to grow
    private volatile int readPos;
    private volatile int writePos;
    private volatile int size;

    @Override
    public final String toString() {
        return "LFRingbuffer<?>[filled "+size+" / "+(capacityPlusOne-1)+", writePos "+writePos+", readPos "+readPos+"]";
    }

    @Override
    public final void dump(final PrintStream stream, final String prefix) {
        stream.println(prefix+" "+toString()+" {");
        for(int i=0; i<capacityPlusOne; i++) {
            stream.println("\t["+i+"]: "+array[i]);
        }
        stream.println("}");
    }

    /**
     * Create a full ring buffer instance w/ the given array's net capacity and content.
     * <p>
     * Example for a 10 element Integer array:
     * <pre>
     *  Integer[] source = new Integer[10];
     *  // fill source with content ..
     *  Ringbuffer<Integer> rb = new LFRingbuffer<Integer>(source);
     * </pre>
     * </p>
     * <p>
     * {@link #isFull()} returns true on the newly created full ring buffer.
     * </p>
     * <p>
     * Implementation will allocate an internal array with size of array <code>copyFrom</code> <i>plus one</i>,
     * and copy all elements from array <code>copyFrom</code> into the internal array.
     * </p>
     * @param copyFrom mandatory source array determining ring buffer's net {@link #capacity()} and initial content.
     * @throws IllegalArgumentException if <code>copyFrom</code> is <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public LFRingbuffer(final T[] copyFrom) throws IllegalArgumentException {
        capacityPlusOne = copyFrom.length + 1;
        array = (T[]) newArray(copyFrom.getClass(), capacityPlusOne);
        resetImpl(true, copyFrom);
    }

    /**
     * Create an empty ring buffer instance w/ the given net <code>capacity</code>.
     * <p>
     * Example for a 10 element Integer array:
     * <pre>
     *  Ringbuffer<Integer> rb = new LFRingbuffer<Integer>(10, Integer[].class);
     * </pre>
     * </p>
     * <p>
     * {@link #isEmpty()} returns true on the newly created empty ring buffer.
     * </p>
     * <p>
     * Implementation will allocate an internal array of size <code>capacity</code> <i>plus one</i>.
     * </p>
     * @param arrayType the array type of the created empty internal array.
     * @param capacity the initial net capacity of the ring buffer
     */
    public LFRingbuffer(final Class<? extends T[]> arrayType, final int capacity) {
        capacityPlusOne = capacity+1;
        array = newArray(arrayType, capacityPlusOne);
        resetImpl(false, null /* empty, nothing to copy */ );
    }

    @Override
    public final int capacity() { return capacityPlusOne-1; }

    @Override
    public final void clear() {
        synchronized ( syncGlobal ) {
            resetImpl(false, null);
            for(int i=0; i<capacityPlusOne; i++) {
                this.array[i] = null;
            }
        }
    }

    @Override
    public final void resetFull(final T[] copyFrom) throws IllegalArgumentException {
        resetImpl(true, copyFrom);
    }

    private final void resetImpl(final boolean full, final T[] copyFrom) throws IllegalArgumentException {
        synchronized ( syncGlobal ) {
            if( null != copyFrom ) {
                if( copyFrom.length != capacityPlusOne-1 ) {
                    throw new IllegalArgumentException("copyFrom array length "+copyFrom.length+" != capacity "+this);
                }
                System.arraycopy(copyFrom, 0, array, 0, copyFrom.length);
                array[capacityPlusOne-1] = null; // null 'plus-one' field!
            } else if ( full ) {
                throw new IllegalArgumentException("copyFrom array is null");
            }
            readPos = capacityPlusOne - 1;
            if( full ) {
                writePos = readPos - 1;
                size = capacityPlusOne - 1;
            } else {
                writePos = readPos;
                size = 0;
            }
        }
    }

    @Override
    public final int size() { return size; }

    @Override
    public final int getFreeSlots() { return capacityPlusOne - 1 - size; }

    @Override
    public final boolean isEmpty() { return 0 == size; }

    @Override
    public final boolean isFull() { return capacityPlusOne - 1 == size; }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation advances the read position and returns the element at it, if not empty.
     * </p>
     */
    @Override
    public final T get() {
        try {
            return getImpl(false, false);
        } catch (final InterruptedException ie) { throw new RuntimeException(ie); }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation advances the read position and returns the element at it, if not empty.
     * </p>
     */
    @Override
    public final T getBlocking() throws InterruptedException {
        return getImpl(true, false);
    }

    @Override
    public final T peek() {
        try {
            return getImpl(false, true);
        } catch (final InterruptedException ie) { throw new RuntimeException(ie); }
    }
    @Override
    public final T peekBlocking() throws InterruptedException {
        return getImpl(true, true);
    }

    private final T getImpl(final boolean blocking, final boolean peek) throws InterruptedException {
        int localReadPos = readPos;
        if( localReadPos == writePos ) {
            if( blocking ) {
                synchronized( syncRead ) {
                    while( localReadPos == writePos ) {
                        syncRead.wait();
                    }
                }
            } else {
                return null;
            }
        }
        localReadPos = (localReadPos + 1) % capacityPlusOne;
        final T r = array[localReadPos];
        if( !peek ) {
            array[localReadPos] = null;
            synchronized ( syncWrite ) {
                size--;
                readPos = localReadPos;
                syncWrite.notifyAll(); // notify waiting putter
            }
        }
        return r;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation advances the write position and stores the given element at it, if not full.
     * </p>
     */
    @Override
    public final boolean put(final T e) {
        try {
            return putImpl(e, false, false);
        } catch (final InterruptedException ie) { throw new RuntimeException(ie); }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation advances the write position and stores the given element at it, if not full.
     * </p>
     */
    @Override
    public final void putBlocking(final T e) throws InterruptedException {
        if( !putImpl(e, false, true) ) {
            throw new InternalError("Blocking put failed: "+this);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation advances the write position and keeps the element at it, if not full.
     * </p>
     */
    @Override
    public final boolean putSame(final boolean blocking) throws InterruptedException {
        return putImpl(null, true, blocking);
    }

    private final boolean putImpl(final T e, final boolean sameRef, final boolean blocking) throws InterruptedException {
        int localWritePos = writePos;
        localWritePos = (localWritePos + 1) % capacityPlusOne;
        if( localWritePos == readPos ) {
            if( blocking ) {
                synchronized( syncWrite ) {
                    while( localWritePos == readPos ) {
                        syncWrite.wait();
                    }
                }
            } else {
                return false;
            }
        }
        if( !sameRef ) {
            array[localWritePos] = e;
        }
        synchronized ( syncRead ) {
            size++;
            writePos = localWritePos;
            syncRead.notifyAll(); // notify waiting getter
        }
        return true;
    }


    @Override
    public final void waitForFreeSlots(final int count) throws InterruptedException {
        synchronized ( syncRead ) {
            if( capacityPlusOne - 1 - size < count ) {
                while( capacityPlusOne - 1 - size < count ) {
                    syncRead.wait();
                }
            }
        }
    }

    @Override
    public final void growEmptyBuffer(final T[] newElements) throws IllegalStateException, IllegalArgumentException {
        synchronized( syncGlobal ) {
            if( null == newElements ) {
                throw new IllegalArgumentException("newElements is null");
            }
            @SuppressWarnings("unchecked")
            final Class<? extends T[]> arrayTypeInternal = (Class<? extends T[]>) array.getClass();
            @SuppressWarnings("unchecked")
            final Class<? extends T[]> arrayTypeNew = (Class<? extends T[]>) newElements.getClass();
            if( arrayTypeInternal != arrayTypeNew ) {
                throw new IllegalArgumentException("newElements array-type mismatch, internal "+arrayTypeInternal+", newElements "+arrayTypeNew);
            }
            if( 0 != size ) {
                throw new IllegalStateException("Buffer is not empty: "+this);
            }
            if( readPos != writePos ) {
                throw new InternalError("R/W pos not equal: "+this);
            }
            if( readPos != writePos ) {
                throw new InternalError("R/W pos not equal at empty: "+this);
            }

            final int growAmount = newElements.length;
            final int newCapacity = capacityPlusOne + growAmount;
            final T[] oldArray = array;
            final T[] newArray = newArray(arrayTypeInternal, newCapacity);

            // writePos == readPos
            writePos += growAmount; // warp writePos to the end of the new data location

            if( readPos >= 0 ) {
                System.arraycopy(oldArray,         0, newArray,          0, readPos+1);
            }
            if( growAmount > 0 ) {
                System.arraycopy(newElements,      0, newArray,  readPos+1, growAmount);
            }
            final int tail = capacityPlusOne-1-readPos;
            if( tail > 0 ) {
                System.arraycopy(oldArray, readPos+1, newArray,  writePos+1, tail);
            }
            size = growAmount;

            capacityPlusOne = newCapacity;
            array = newArray;
        }
    }

    @Override
    public final void growFullBuffer(final int growAmount) throws IllegalStateException, IllegalArgumentException {
        synchronized ( syncGlobal ) {
            if( 0 > growAmount ) {
                throw new IllegalArgumentException("amount "+growAmount+" < 0 ");
            }
            if( capacityPlusOne-1 != size ) {
                throw new IllegalStateException("Buffer is not full: "+this);
            }
            final int wp1 = ( writePos + 1 ) % capacityPlusOne;
            if( wp1 != readPos ) {
                throw new InternalError("R != W+1 pos at full: "+this);
            }
            @SuppressWarnings("unchecked")
            final Class<? extends T[]> arrayTypeInternal = (Class<? extends T[]>) array.getClass();

            final int newCapacity = capacityPlusOne + growAmount;
            final T[] oldArray = array;
            final T[] newArray = newArray(arrayTypeInternal, newCapacity);

            // writePos == readPos - 1
            readPos = ( writePos + 1 + growAmount ) % newCapacity; // warp readPos to the end of the new data location

            if(writePos >= 0) {
                System.arraycopy(oldArray,          0, newArray,          0, writePos+1);
            }
            final int tail = capacityPlusOne-1-writePos;
            if( tail > 0 ) {
                System.arraycopy(oldArray, writePos+1, newArray,   readPos, tail);
            }

            capacityPlusOne = newCapacity;
            array = newArray;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] newArray(final Class<? extends T[]> arrayType, final int length) {
        return ((Object)arrayType == (Object)Object[].class)
            ? (T[]) new Object[length]
            : (T[]) Array.newInstance(arrayType.getComponentType(), length);
    }
}