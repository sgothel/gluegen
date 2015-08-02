/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

import jogamp.common.util.SyncedBitfield;

/**
 * Simple bitfield interface for efficient bit storage access in O(1).
 * @since 2.3.2
 */
public interface Bitfield {
    /** Maximum 32 bit Unsigned Integer Value: {@code 0xffffffff} == {@value}. */
    public static final int UNSIGNED_INT_MAX_VALUE = 0xffffffff;

    /**
     * Bit operation utilities (static).
     */
    public static class Util {
        /**
         * Returns the 32 bit mask of n-bits, i.e. n low order 1’s.
         * <p>
         * Implementation handles n == 32.
         * </p>
         * @throws IndexOutOfBoundsException if {@code b} is out of bounds, i.e. &gt; 32
         */
        public static int getBitMask(final int n) {
            if( 32 > n ) {
                return ( 1 << n ) - 1;
            } else if ( 32 == n ) {
                return UNSIGNED_INT_MAX_VALUE;
            } else {
                throw new IndexOutOfBoundsException("n <= 32 expected, is "+n);
            }
        }

        /**
         * Returns the number of set bits within given 32bit integer in O(1)
         * using a <i>HAKEM 169 Bit Count</i> inspired implementation:
         * <pre>
         *   http://www.inwap.com/pdp10/hbaker/hakmem/hakmem.html
         *   http://home.pipeline.com/~hbaker1/hakmem/hacks.html#item169
         *   http://tekpool.wordpress.com/category/bit-count/
         *   http://www.hackersdelight.org/
         * </pre>
         */
        public static final int bitCount(int n) {
            // Note: Original used 'unsigned int',
            // hence we use the unsigned right-shift '>>>'
            /**
             * Original does not work due to lack of 'unsigned' right-shift and modulo,
             * we need 2-complementary solution, i.e. 'signed'.
                int c = n;
                c -= (n >>> 1) & 033333333333;
                c -= (n >>> 2) & 011111111111;
                return ( (c + ( c >>> 3 ) ) & 030707070707 ) & 0x3f; // % 63
            */
            // Hackers Delight, Figure 5-2, pop1 of pop.c.txt
            n = n - ((n >>> 1) & 0x55555555);
            n = (n & 0x33333333) + ((n >>> 2) & 0x33333333);
            n = (n + (n >>> 4)) & 0x0f0f0f0f;
            n = n + (n >>> 8);
            n = n + (n >>> 16);
            return n & 0x3f;
        }
    }
    /**
     * Simple {@link Bitfield} factory for returning the efficient implementation.
     */
    public static class Factory {
        /**
         * Creates am efficient {@link Bitfield} instance based on the requested {@code storageBitSize}.
         * <p>
         * Implementation returns a plain 32 bit integer field implementation for
         * {@code storageBitSize} &le; 32 bits or an 32 bit integer array implementation otherwise.
         * </p>
         */
        public static Bitfield create(final int storageBitSize) {
            if( 32 >= storageBitSize ) {
                return new jogamp.common.util.Int32Bitfield();
            } else {
                return new jogamp.common.util.Int32ArrayBitfield(storageBitSize);
            }
        }
        /**
         * Creates a synchronized {@link Bitfield} by wrapping the given {@link Bitfield} instance.
         */
        public static Bitfield synchronize(final Bitfield impl) {
            return new SyncedBitfield(impl);
        }
    }
    /**
     * Returns the storage size in bit units, e.g. 32 bit for implementations using one {@code int} field.
     */
    int size();


    /**
     * Set all bits of this bitfield to the given value {@code bit}.
     */
    void clearField(final boolean bit);

    /**
     * Returns {@code length} bits from this storage,
     * starting with the lowest bit from the storage position {@code lowBitnum}.
     * @param lowBitnum storage bit position of the lowest bit, restricted to [0..{@link #size()}-{@code length}].
     * @param length number of bits to read, constrained to [0..32].
     * @throws IndexOutOfBoundsException if {@code rightBitnum} is out of bounds
     * @see #put32(int, int, int)
     */
    int get32(final int lowBitnum, final int length) throws IndexOutOfBoundsException;

    /**
     * Puts {@code length} bits of given {@code data} into this storage,
     * starting w/ the lowest bit to the storage position {@code lowBitnum}.
     * @param lowBitnum storage bit position of the lowest bit, restricted to [0..{@link #size()}-{@code length}].
     * @param length number of bits to write, constrained to [0..32].
     * @param data the actual bits to be put into this storage
     * @throws IndexOutOfBoundsException if {@code rightBitnum} is out of bounds
     * @see #get32(int, int)
     */
    void put32(final int lowBitnum, final int length, final int data) throws IndexOutOfBoundsException;

    /**
     * Copies {@code length} bits at position {@code srcLowBitnum} to position {@code dstLowBitnum}
     * and returning the bits.
     * <p>
     * Implementation shall operate as if invoking {@link #get32(int, int)}
     * and then {@link #put32(int, int, int)} sequentially.
     * </p>
     * @param srcLowBitnum source bit number, restricted to [0..{@link #size()}-1].
     * @param dstLowBitnum destination bit number, restricted to [0..{@link #size()}-1].
     * @throws IndexOutOfBoundsException if {@code bitnum} is out of bounds
     * @see #get32(int, int)
     * @see #put32(int, int, int)
     */
    int copy32(final int srcLowBitnum, final int dstLowBitnum, final int length) throws IndexOutOfBoundsException;

    /**
     * Return <code>true</code> if the bit at position <code>bitnum</code> is set, otherwise <code>false</code>.
     * @param bitnum bit number, restricted to [0..{@link #size()}-1].
     * @throws IndexOutOfBoundsException if {@code bitnum} is out of bounds
     */
    boolean get(final int bitnum) throws IndexOutOfBoundsException;

    /**
     * Set or clear the bit at position <code>bitnum</code> according to <code>bit</code>
     * and return the previous value.
     * @param bitnum bit number, restricted to [0..{@link #size()}-1].
     * @throws IndexOutOfBoundsException if {@code bitnum} is out of bounds
     */
    boolean put(final int bitnum, final boolean bit) throws IndexOutOfBoundsException;

    /**
     * Set the bit at position <code>bitnum</code> according to <code>bit</code>.
     * @param bitnum bit number, restricted to [0..{@link #size()}-1].
     * @throws IndexOutOfBoundsException if {@code bitnum} is out of bounds
     */
    void set(final int bitnum) throws IndexOutOfBoundsException;

    /**
     * Clear the bit at position <code>bitnum</code> according to <code>bit</code>.
     * @param bitnum bit number, restricted to [0..{@link #size()}-1].
     * @throws IndexOutOfBoundsException if {@code bitnum} is out of bounds
     */
    void clear(final int bitnum) throws IndexOutOfBoundsException;

    /**
     * Copies the bit at position {@code srcBitnum} to position {@code dstBitnum}
     * and returning <code>true</code> if the bit is set, otherwise <code>false</code>.
     * @param srcBitnum source bit number, restricted to [0..{@link #size()}-1].
     * @param dstBitnum destination bit number, restricted to [0..{@link #size()}-1].
     * @throws IndexOutOfBoundsException if {@code bitnum} is out of bounds
     */
    boolean copy(final int srcBitnum, final int dstBitnum) throws IndexOutOfBoundsException;

    /**
     * Returns the number of one bits within this bitfield.
     * <p>
     * Utilizes {#link {@link Bitfield.Util#bitCount(int)}}.
     * </p>
     */
    int bitCount();
}
