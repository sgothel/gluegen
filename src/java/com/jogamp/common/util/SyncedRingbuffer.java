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
    public final void dump(PrintStream stream, String prefix) {
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
     *  Ringbuffer<Integer> rb = new SyncedRingbuffer<Integer>(source, new Ringbuffer.AllocEmptyArray<Integer>() {
     *      public Integer[] newArray(int size) {
     *          return new Integer[size];
     *      } } );
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
     * @param allocEmptyArray implementation hook to allocate a new empty array of generic type T
     * @throws IllegalArgumentException if <code>copyFrom</code> is <code>null</code>   
     */
    public SyncedRingbuffer(T[] copyFrom, AllocEmptyArray<T> allocEmptyArray) throws IllegalArgumentException {
        capacity = copyFrom.length;
        array = allocEmptyArray.newArray(capacity);
        resetImpl(true, copyFrom);
    }
    
    /** 
     * Create an empty ring buffer instance w/ the given net <code>capacity</code>.
     * <p> 
     * Example for a 10 element Integer array:
     * <pre>
     *  Ringbuffer<Integer> rb = new SyncedRingbuffer<Integer>(10, new Ringbuffer.AllocEmptyArray<Integer>() {
     *      public Integer[] newArray(int size) {
     *          return new Integer[size];
     *      } } );
     * </pre>
     * </p>
     * <p>
     * {@link #isEmpty()} returns true on the newly created empty ring buffer.
     * </p>
     * <p>
     * Implementation will allocate an internal array of size <code>capacity</code>.
     * </p>
     * @param capacity the initial net capacity of the ring buffer
     * @param allocEmptyArray implementation hook to allocate a new empty array of generic type T
     */
    public SyncedRingbuffer(int capacity, AllocEmptyArray<T> allocEmptyArray) {
        this.capacity = capacity;
        this.array = allocEmptyArray.newArray(capacity);
        resetImpl(false, null);
    }
    
    @Override
    public final T[] getInternalArray() { return array; }
    
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
    public final void resetFull(T[] copyFrom) throws IllegalArgumentException {
        resetImpl(true, copyFrom);
    }
    
    private final void resetImpl(boolean full, T[] copyFrom) throws IllegalArgumentException {
        synchronized ( syncGlobal ) {
            if( null != copyFrom ) {
                if( copyFrom.length != capacity() ) {
                    throw new IllegalArgumentException("copyFrom array length "+copyFrom.length+" != capacity "+this);
                }
                System.arraycopy(copyFrom, 0, array, 0, copyFrom.length);
            } else if( full ) {
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
        } catch (InterruptedException ie) { throw new RuntimeException(ie); }
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
        } catch (InterruptedException ie) { throw new RuntimeException(ie); }
    }
    @Override
    public final T peekBlocking() throws InterruptedException {
        return getImpl(true, true);
    }
    
    private final T getImpl(boolean blocking, boolean peek) throws InterruptedException {
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
    public final boolean put(T e) {
        try {
            return putImpl(e, false, false);
        } catch (InterruptedException ie) { throw new RuntimeException(ie); }
    }
    
    /** 
     * {@inheritDoc}
     * <p>
     * Implementation stores the element at the current write position and advances it, if not full.
     * </p>
     */
    @Override
    public final void putBlocking(T e) throws InterruptedException {
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
    public final boolean putSame(boolean blocking) throws InterruptedException {
        return putImpl(null, true, blocking);
    }
    
    private final boolean putImpl(T e, boolean sameRef, boolean blocking) throws InterruptedException {
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
    public final void waitForFreeSlots(int count) throws InterruptedException {
        synchronized ( syncGlobal ) {
            if( capacity - size < count ) {
                while( capacity - size < count ) {
                    syncGlobal.wait();
                }
            }
        }
    }
    
    @Override
    public void growBuffer(T[] newElements, int amount, AllocEmptyArray<T> allocEmptyArray) throws IllegalStateException, IllegalArgumentException {
        synchronized ( syncGlobal ) {
            final boolean isFull = capacity == size;
            final boolean isEmpty = 0 == size;
            if( !isFull && !isEmpty ) {
                throw new IllegalStateException("Buffer neither full nor empty: "+this);
            }
            if( readPos != writePos ) {
                throw new InternalError("R/W pos not equal: "+this);
            }
            if( null != newElements && amount < newElements.length ) {
                throw new IllegalArgumentException("amount "+amount+" < newElements "+newElements.length);
            }
            final int newCapacity = capacity + amount;
            final T[] oldArray = array;
            final T[] newArray = allocEmptyArray.newArray(newCapacity);
            
            if( isFull ) {
                // writePos == readPos
                readPos += amount; // warp readPos to the end of the new data location
                
                if(writePos > 0) {
                    System.arraycopy(oldArray,        0, newArray,        0, writePos);
                }
                if( null != newElements && newElements.length > 0 ) {
                    System.arraycopy(newElements,     0, newArray, writePos, newElements.length);
                }
                final int tail = capacity-writePos;
                if( tail > 0 ) {
                    System.arraycopy(oldArray, writePos, newArray,  readPos, tail);
                }
            } else /* if ( isEmpty ) */ {
                // writePos == readPos
                writePos += amount; // warp writePos to the end of the new data location
                
                if( readPos > 0 ) {
                    System.arraycopy(oldArray,        0, newArray,        0, readPos);
                }
                if( null != newElements && newElements.length > 0 ) {
                    System.arraycopy(newElements,     0, newArray,  readPos, newElements.length);
                }
                final int tail = capacity-readPos;
                if( tail > 0 ) {
                    System.arraycopy(oldArray,  readPos, newArray, writePos, tail);
                }
                size = amount;
            }
            
            capacity = newCapacity;
            array = newArray;
        }
    }
}
