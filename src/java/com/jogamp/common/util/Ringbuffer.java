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
 * Ring buffer interface, a.k.a circular buffer.
 * <p>
 * Caller can chose whether to block until get / put is able to proceed or not.
 * </p>
 * <p>
 * Caller can chose whether to pass an empty array and clear references at get,
 * or using a preset array for circular access of same objects.
 * </p>
 * <p>
 * Synchronization and hence thread safety details belong to the implementation. 
 * </p>
 */
public interface Ringbuffer<T> {
    
    /** 
     * Implementation hook for {@link #growBuffer(Object[], int, AllocEmptyArray)}
     * to pass an implementation of {@link #newArray(int)}. 
     * @param <T> type of array
     */
    public static interface AllocEmptyArray<T> {
        /**
         * Returns a new allocated empty array of generic type T with given size.
         */
        public T[] newArray(int size);
    }
    
    /** Returns a short string representation incl. size/capacity and internal r/w index (impl. dependent). */ 
    public String toString();

    /** Debug functionality - Dumps the contents of the internal array. */
    public void dump(PrintStream stream, String prefix);

    /** 
     * Returns the internal array as-is, i.e. w/o a copy.
     * <p>
     * The layout and size of the internal array is implementation dependent.
     * </p> 
     * <p>
     * Users shall not modify or rely on the returned array.
     * </p> 
     * @deprecated This method should not be required 
     */
    public T[] getInternalArray();

    /** Returns the net capacity of this ring buffer. */
    public int capacity();

    /**
     * Resets the read and write position according to an empty ring buffer 
     * and set all ring buffer slots to <code>null</code>.
     * <p>
     * {@link #isEmpty()} will return <code>true</code> after calling this method.
     * </p>
     */
    public void clear();

    /**
     * Resets the read and write position according to a full ring buffer 
     * and fill all slots w/ elements of array <code>copyFrom</code>.
     * <p>
     * Array's <code>copyFrom</code> elements will be copied into the internal array, 
     * hence it's length must be equal to {@link #capacity()}.
     * </p>
     * @param copyFrom Mandatory array w/ length {@link #capacity()} to be copied into the internal array.
     * @throws IllegalArgumentException if <code>copyFrom</code> is <code>null</code>.   
     * @throws IllegalArgumentException if <code>copyFrom</code>'s length is different from {@link #capacity()}.
     */
    public void resetFull(T[] copyFrom) throws IllegalArgumentException;

    /** Returns the number of elements in this ring buffer. */
    public int size();

    /** Returns the number of free slots available to put.  */
    public int getFreeSlots();

    /** Returns true if this ring buffer is empty, otherwise false. */
    public boolean isEmpty();

    /** Returns true if this ring buffer is full, otherwise false. */
    public boolean isFull();

    /**
     * Dequeues the oldest enqueued element if available, otherwise null.
     * <p>
     * The returned ring buffer slot will be set to <code>null</code> to release the reference
     * and move ownership to the caller.
     * </p>
     * <p>
     * Method is non blocking and returns immediately;.
     * </p>
     * @return the oldest put element if available, otherwise null.  
     */
    public T get();

    /**
     * Dequeues the oldest enqueued element.
     * <p>
     * The returned ring buffer slot will be set to <code>null</code> to release the reference
     * and move ownership to the caller.
     * </p>
     * <p>
     * Methods blocks until an element becomes available via put.
     * </p>
     * @return the oldest put element  
     * @throws InterruptedException 
     */
    public T getBlocking() throws InterruptedException;

    /** 
     * Peeks the next element at the read position w/o modifying pointer, nor blocking.
     * @return <code>null</code> if empty, otherwise the element which would be read next.
     */
    public T peek();

    /** 
     * Peeks the next element at the read position w/o modifying pointer, but w/ blocking.
     * @return <code>null</code> if empty, otherwise the element which would be read next.
     */
    public T peekBlocking() throws InterruptedException;

    /** 
     * Enqueues the given element.
     * <p>
     * Returns true if successful, otherwise false in case buffer is full.
     * </p>
     * <p>
     * Method is non blocking and returns immediately;.
     * </p>
     */
    public boolean put(T e);

    /** 
     * Enqueues the given element.
     * <p>
     * Method blocks until a free slot becomes available via get.
     * </p>
     * @throws InterruptedException 
     */
    public void putBlocking(T e) throws InterruptedException;

    /** 
     * Enqueues the same element at it's write position, if not full.
     * <p>
     * Returns true if successful, otherwise false in case buffer is full.
     * </p>
     * <p>
     * If <code>blocking</code> is true, method blocks until a free slot becomes available via get.
     * </p>
     * @param blocking if true, wait until a free slot becomes available via get.
     * @throws InterruptedException 
     */
    public boolean putSame(boolean blocking) throws InterruptedException;

    /**
     * Blocks until at least <code>count</code> free slots become available.
     * @throws InterruptedException
     */
    public void waitForFreeSlots(int count) throws InterruptedException;

    /**
     * Grows a full or empty ring buffer, increasing it's capacity about the amount.
     * <p>
     * Growing an empty ring buffer increases it's size about the amount, i.e. renders it not empty.
     * The new elements are inserted at the read position, able to be read out via {@link #get()} etc.
     * </p>
     * <p>
     * Growing a full ring buffer leaves the size intact, i.e. renders it not full.
     * The new elements are inserted at the write position, able to be written to via {@link #put(Object)} etc.
     * </p>
     * 
     * @param newElements array of new empty elements the buffer shall grow about, maybe <code>null</code>.
     *        If not <code>null</code>, array size must be <= <code>amount</code>
     * @param amount the amount of elements the buffer shall grow about
     * @param allocEmptyArray implementation hook to allocate a new empty array of generic type T
     * @throws IllegalStateException if buffer is neither full nor empty
     * @throws IllegalArgumentException if newElements is given but is > amount
     */
    public void growBuffer(T[] newElements, int amount,
            AllocEmptyArray<T> allocEmptyArray) throws IllegalStateException,
            IllegalArgumentException;

}