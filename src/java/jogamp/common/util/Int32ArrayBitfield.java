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
        final int units = Math.max(1, ( storageBitSize + 31 ) >>> UNIT_SHIFT);
        this.storage = new int[units]; // initialized w/ default '0'
        this.bitSize = units << UNIT_SHIFT;
    }

    @Override
    public int size() {
        return bitSize;
    }

    @Override
    public final void clearField(final boolean bit) {
        final int v;
        if( bit ) {
            v = Bitfield.UNSIGNED_INT_MAX_VALUE;
        } else {
            v = 0;
        }
        for(int i=storage.length-1; i>=0; i--) {
            storage[i] = v;
        }
    }

    private static final void check(final int size, final int bitnum) throws IndexOutOfBoundsException {
        if( 0 > bitnum || bitnum >= size ) {
            throw new IndexOutOfBoundsException("Bitnum should be within [0.."+(size-1)+"], but is "+bitnum);
        }
    }

    @Override
    public final int get32(final int lowBitnum, final int length) throws IndexOutOfBoundsException {
        if( 0 > length || length > 32 ) {
            throw new IndexOutOfBoundsException("length should be within [0..32], but is "+length);
        }
        check(bitSize-length+1, lowBitnum);
        final int u = lowBitnum >>> UNIT_SHIFT;
        final int left = 32 - ( lowBitnum - ( u << UNIT_SHIFT ) ); // remaining bits of first chunk storage
        if( 32 == left ) {
            // fast path
            final int m = Util.getBitMask(length);   // mask of chunk
            return m & storage[u];
        } else {
            // slow path
            final int l = Math.min(length, left);    // length of first chunk < 32
            final int m = ( 1 << l ) - 1;            // mask of first chunk
            final int d = m & ( storage[u] >>> lowBitnum );
            final int l2 = length - l;               // length of last chunk < 32
            if( l2 > 0 ) {
                final int m2 = ( 1 << l2 ) - 1;      // mask of last chunk
                return d | ( ( m2 & storage[u+1] ) << l );
            } else {
                return d;
            }
        }
    }
    @Override
    public final void put32(final int lowBitnum, final int length, final int data) throws IndexOutOfBoundsException {
        if( 0 > length || length > 32 ) {
            throw new IndexOutOfBoundsException("length should be within [0..32], but is "+length);
        }
        check(bitSize-length+1, lowBitnum);
        final int u = lowBitnum >>> UNIT_SHIFT;
        final int left = 32 - ( lowBitnum - ( u << UNIT_SHIFT ) ); // remaining bits of first chunk storage
        if( 32 == left ) {
            // fast path
            final int m = Util.getBitMask(length);   // mask of chunk
            storage[u] = ( ( ~m ) & storage[u] )     // keep non-written storage bits
                         | ( m & data );             // overwrite storage w/ used data bits
        } else {
            // slow path
            final int l = Math.min(length, left);    // length of first chunk < 32
            final int m = ( 1 << l ) - 1;            // mask of first chunk
            storage[u] = ( ( ~( m << lowBitnum ) ) & storage[u] ) // keep non-written storage bits
                         | ( ( m & data ) << lowBitnum );         // overwrite storage w/ used data bits
            final int l2 = length - l;               // length of last chunk < 32
            if( l2 > 0 ) {
                final int m2 = ( 1 << l2 ) - 1;      // mask of last chunk
                storage[u+1] = ( ( ~m2 ) & storage[u+1] ) // keep non-written storage bits
                               | ( m2 & ( data >>> l ) ); // overwrite storage w/ used data bits
            }
        }
    }
    @Override
    public final int copy32(final int srcBitnum, final int dstBitnum, final int length) throws IndexOutOfBoundsException {
        final int data = get32(srcBitnum, length);
        put32(dstBitnum, length, data);
        return data;
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
    public final void clear(final int bitnum) throws IndexOutOfBoundsException {
        check(bitSize, bitnum);
        final int u = bitnum >>> UNIT_SHIFT;
        final int b = bitnum - ( u << UNIT_SHIFT );
        final int m = 1 << b;
        storage[u] &= ~m;
    }
    @Override
    public final boolean copy(final int srcBitnum, final int dstBitnum) throws IndexOutOfBoundsException {
        check(bitSize, srcBitnum);
        check(bitSize, dstBitnum);
        final boolean bit;
        // get
        {
            final int u = srcBitnum >>> UNIT_SHIFT;
            final int b = srcBitnum - ( u << UNIT_SHIFT );
            bit = 0 != ( storage[u] & ( 1 << b ) ) ;
        }
        // put
        final int u = dstBitnum >>> UNIT_SHIFT;
        final int b = dstBitnum - ( u << UNIT_SHIFT );
        final int m = 1 << b;
        if( bit ) {
            storage[u] |=  m;
        } else {
            storage[u] &= ~m;
        }
        return bit;
    }

    @Override
    public int bitCount() {
        int c = 0;
        for(int i = storage.length-1; i>=0; i--) {
            c += Bitfield.Util.bitCount(storage[i]);
        }
        return c;
    }
}
