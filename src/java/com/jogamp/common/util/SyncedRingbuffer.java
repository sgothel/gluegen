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
 * Simple synchronized implementation of {@link Ringbuffer}.
 * <p>
 * All methods utilize global synchronization.
 * </p>
 * <p>
 * Characteristics:
 * <ul>
 *   <li>Read position points to the next read element.</li>
 *   <li>Write position points to the next write element.</li>
 * </ul>
 * <table border="1">
 *   <tr><td>Empty</td><td>writePos == readPos</td><td>size == 0</td></tr>
 *   <tr><td>Full</td><td>writePos == readPos</td><td>size == capacity</td></tr>
 * </table>
 * </p>
 */
public class SyncedRingbuffer<T> implements Ringbuffer<T> {

    private final Object syncGlobal = new Object();
    private /* final */ T[] array;     // not final due to grow
    private /* final */ int capacity;  // not final due to grow
    private int readPos;
    private int writePos;
    private int size;

    @Override
    public final String toString() {
        return "SyncedRingbuffer<?>[filled "+size+" / "+capacity+", writePos "+writePos+", readPos "+readPos+"]";
    }

    @Override
    public final void dump(final PrintStream stream, final String prefix) {
        stream.println(prefix+" "+toString()+" {");
        for(int i=0; i<capacity; i++) {
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
     *  Ringbuffer<Integer> rb = new SyncedRingbuffer<Integer>(source);
     * </pre>
     * </p>
     * <p>
     * {@link #isFull()} returns true on the newly created full ring buffer.
     * </p>
     * <p>
     * Implementation will allocate an internal array with size of array <code>copyFrom</code>
     * and copy all elements from array <code>copyFrom</code> into the internal array.
     * </p>
     * @param copyFrom mandatory source array determining ring buffer's net {@link #capacity()} and initial content.
     * @throws IllegalArgumentException if <code>copyFrom</code> is <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public SyncedRingbuffer(final T[] copyFrom) throws IllegalArgumentException {
        capacity = copyFrom.length;
        array = (T[]) newArray(copyFrom.getClass(), capacity);
        resetImpl(true, copyFrom);
    }

    /**
     * Create an empty ring buffer instance w/ the given net <code>capacity</code>.
     * <p>
     * Example for a 10 element Integer array:
     * <pre>
     *  Ringbuffer<Integer> rb = new SyncedRingbuffer<Integer>(10, Integer[].class);
     * </pre>
     * </p>
     * <p>
     * {@link #isEmpty()} returns true on the newly created empty ring buffer.
     * </p>
     * <p>
     * Implementation will allocate an internal array of size <code>capacity</code>.
     * </p>
     * @param arrayType the array type of the created empty internal array.
     * @param capacity the initial net capacity of the ring buffer
     */
    public SyncedRingbuffer(final Class<? extends T[]> arrayType, final int capacity) {
        this.capacity = capacity;
        this.array = newArray(arrayType, capacity);
        resetImpl(false, null /* empty, nothing to copy */ );
    }

    @Override
    public final int capacity() { return capacity; }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation sets read and write position to zero.
     * </p>
     */
    @Override
    public final void clear() {
        synchronized ( syncGlobal ) {
            resetImpl(false, null);
            for(int i=0; i<capacity; i++) {
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
                if( copyFrom.length != capacity() ) {
                    throw new IllegalArgumentException("copyFrom array length "+copyFrom.length+" != capacity "+this);
                }
                System.arraycopy(copyFrom, 0, array, 0, copyFrom.length);
            } else if ( full ) {
                throw new IllegalArgumentException("copyFrom array is null");
            }
            readPos = 0;
            writePos = 0;
            size = full ? capacity : 0;
        }
    }

    @Override
    public final int size() {
        synchronized ( syncGlobal ) {
            return size;
        }
    }

    @Override
    public final int getFreeSlots() {
        synchronized ( syncGlobal ) {
            return capacity - size;
        }
    }

    @Override
    public final boolean isEmpty() {
        synchronized ( syncGlobal ) {
            return 0 == size;
        }
    }

    @Override
    public final boolean isFull() {
        synchronized ( syncGlobal ) {
            return capacity == size;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation returns the element at the current read position and advances it, if not empty.
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
     * Implementation returns the element at the current read position and advances it, if not empty.
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
        synchronized( syncGlobal ) {
            if( 0 == size ) {
                if( blocking ) {
                    while( 0 == size ) {
                        syncGlobal.wait();
                    }
                } else {
                    return null;
                }
            }
            final int localReadPos = readPos;
            final T r = array[localReadPos];
            if( !peek ) {
                array[localReadPos] = null;
                size--;
                readPos = (localReadPos + 1) % capacity;
                syncGlobal.notifyAll(); // notify waiting putter
            }
            return r;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation stores the element at the current write position and advances it, if not full.
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
     * Implementation stores the element at the current write position and advances it, if not full.
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
     * Implementation keeps the element at the current write position and advances it, if not full.
     * </p>
     */
    @Override
    public final boolean putSame(final boolean blocking) throws InterruptedException {
        return putImpl(null, true, blocking);
    }

    private final boolean putImpl(final T e, final boolean sameRef, final boolean blocking) throws InterruptedException {
        synchronized( syncGlobal ) {
            if( capacity == size ) {
                if( blocking ) {
                    while( capacity == size ) {
                        syncGlobal.wait();
                    }
                } else {
                    return false;
                }
            }
            final int localWritePos = writePos;
            if( !sameRef ) {
                array[localWritePos] = e;
            }
            size++;
            writePos = (localWritePos + 1) % capacity;
            syncGlobal.notifyAll(); // notify waiting getter
            return true;
        }
    }

    @Override
    public final void waitForFreeSlots(final int count) throws InterruptedException {
        synchronized ( syncGlobal ) {
            if( capacity - size < count ) {
                while( capacity - size < count ) {
                    syncGlobal.wait();
                }
            }
        }
    }


    @Override
    public final void growEmptyBuffer(final T[] newElements) throws IllegalStateException, IllegalArgumentException {
        synchronized ( syncGlobal ) {
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

            final int growAmount = newElements.length;
            final int newCapacity = capacity + growAmount;
            final T[] oldArray = array;
            final T[] newArray = newArray(arrayTypeInternal, newCapacity);

            // writePos == readPos
            writePos += growAmount; // warp writePos to the end of the new data location

            if( readPos > 0 ) {
                System.arraycopy(oldArray,        0, newArray,        0, readPos);
            }
            if( growAmount > 0 ) {
                System.arraycopy(newElements,     0, newArray,  readPos, growAmount);
            }
            final int tail = capacity-readPos;
            if( tail > 0 ) {
                System.arraycopy(oldArray,  readPos, newArray, writePos, tail);
            }
            size = growAmount;

            capacity = newCapacity;
            array = newArray;
        }
    }

    @Override
    public final void growFullBuffer(final int growAmount) throws IllegalStateException, IllegalArgumentException {
        synchronized ( syncGlobal ) {
            if( 0 > growAmount ) {
                throw new IllegalArgumentException("amount "+growAmount+" < 0 ");
            }
            if( capacity != size ) {
                throw new IllegalStateException("Buffer is not full: "+this);
            }
            if( readPos != writePos ) {
                throw new InternalError("R/W pos not equal: "+this);
            }
            @SuppressWarnings("unchecked")
            final Class<? extends T[]> arrayTypeInternal = (Class<? extends T[]>) array.getClass();

            final int newCapacity = capacity + growAmount;
            final T[] oldArray = array;
            final T[] newArray = newArray(arrayTypeInternal, newCapacity);

            // writePos == readPos
            readPos += growAmount; // warp readPos to the end of the new data location

            if(writePos > 0) {
                System.arraycopy(oldArray,        0, newArray,        0, writePos);
            }
            final int tail = capacity-writePos;
            if( tail > 0 ) {
                System.arraycopy(oldArray, writePos, newArray,  readPos, tail);
            }

            capacity = newCapacity;
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
