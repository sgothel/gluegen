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
package jogamp.common.util;

import com.jogamp.common.util.Bitfield;

/**
 * Simple bitfield interface for efficient storage access in O(1).
 * <p>
 * Implementation uses a 32bit integer array for storage.
 * </p>
 */
public class Int32ArrayBitfield implements Bitfield {
    private static final int UNIT_SHIFT = 5;
    private final int[] storage;
    private final int bitSize;

    /**
     * @param storageBitSize
     */
    public Int32ArrayBitfield(final int storageBitSize) {
        final int units = Math.max(1, ( storageBitSize + 7 ) >>> UNIT_SHIFT);
        this.storage = new int[units];
        this.bitSize = units << UNIT_SHIFT;
    }

    @Override
    public int getStorageBitSize() {
        return bitSize;
    }

    private final void check(final int limit, final int bitnum) throws IndexOutOfBoundsException {
        if( 0 > bitnum || bitnum >= limit ) {
            throw new IndexOutOfBoundsException("Bitnum should be within [0.."+(limit-1)+"], but is "+bitnum);
        }
    }

    @Override
    public final int getInt32(final int rightBitnum) throws IndexOutOfBoundsException {
        check(bitSize-31, rightBitnum);
        if( 0 == rightBitnum % 32 ) {
            // fast path
            return storage[rightBitnum >>> UNIT_SHIFT];
        } else {
            // slow path
            throw new UnsupportedOperationException("todo: non-32bit alignment");
        }
    }

    @Override
    public final void putInt32(final int rightBitnum, final int mask) throws IndexOutOfBoundsException {
        check(bitSize-31, rightBitnum);
        if( 0 == rightBitnum % 32 ) {
            // fast path
            storage[rightBitnum >>> UNIT_SHIFT] = mask;
        } else {
            // slow path
            throw new UnsupportedOperationException("todo: non-32bit alignment");
        }
    }

    @Override
    public final boolean get(final int bitnum) throws IndexOutOfBoundsException {
        check(bitSize, bitnum);
        final int u = bitnum >>> UNIT_SHIFT;
        final int b = bitnum - ( u << UNIT_SHIFT );
        return 0 != ( storage[u] & ( 1 << b ) ) ;
    }

    @Override
    public final boolean put(final int bitnum, final boolean bit) throws IndexOutOfBoundsException {
        check(bitSize, bitnum);
        final int u = bitnum >>> UNIT_SHIFT;
        final int b = bitnum - ( u << UNIT_SHIFT );
        final int m = 1 << b;
        final boolean prev = 0 != ( storage[u] & m ) ;
        if( prev != bit ) {
            if( bit ) {
                storage[u] |=  m;
            } else {
                storage[u] &= ~m;
            }
        }
        return prev;
    }
    @Override
    public final void set(final int bitnum) throws IndexOutOfBoundsException {
        check(bitSize, bitnum);
        final int u = bitnum >>> UNIT_SHIFT;
        final int b = bitnum - ( u << UNIT_SHIFT );
        final int m = 1 << b;
        storage[u] |=  m;
    }
    @Override
    public final void clear (final int bitnum) throws IndexOutOfBoundsException {
        check(bitSize, bitnum);
        final int u = bitnum >>> UNIT_SHIFT;
        final int b = bitnum - ( u << UNIT_SHIFT );
        final int m = 1 << b;
        storage[u] &= ~m;
    }

    @Override
    public int getBitCount() {
        int c = 0;
        for(int i = storage.length-1; i>=0; i--) {
            c += Bitfield.Util.getBitCount(storage[i]);
        }
        return c;
    }
}
