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
 * Implementation uses one 32bit integer field for storage.
 * </p>
 */
public class Int32Bitfield implements Bitfield {
    /** Unit size in bits, here 32 bits for one int unit. */
    private static final int UNIT_SIZE = 32;
    private int storage;

    public Int32Bitfield() {
        this.storage = 0;
    }

    @Override
    public int size() {
        return UNIT_SIZE;
    }

    @Override
    public final void clearField(final boolean bit) {
        if( bit ) {
            storage = Bitfield.UNSIGNED_INT_MAX_VALUE;
        } else {
            storage = 0;
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
        check(UNIT_SIZE-length+1, lowBitnum);
        final int left = 32 - lowBitnum;             // remaining bits of first chunk
        if( 32 == left ) {
            // fast path
            final int m = Util.getBitMask(length);   // mask of chunk
            return m & storage;
        } else {
            // slow path
            final int l = Math.min(length, left);    // length of first chunk < 32
            final int m = ( 1 << l ) - 1;            // mask of first chunk
            return m & ( storage >>> lowBitnum );
        }
    }
    @Override
    public final void put32(final int lowBitnum, final int length, final int data) throws IndexOutOfBoundsException {
        if( 0 > length || length > 32 ) {
            throw new IndexOutOfBoundsException("length should be within [0..32], but is "+length);
        }
        check(UNIT_SIZE-length+1, lowBitnum);
        final int left = 32 - lowBitnum;             // remaining bits of first chunk storage
        if( 32 == left ) {
            // fast path
            final int m = Util.getBitMask(length);   // mask of chunk
            storage = ( ( ~m ) & storage )           // keep non-written storage bits
                      | ( m & data );                // overwrite storage w/ used data bits
        } else {
            // slow path
            final int l = Math.min(length, left);    // length of first chunk < 32
            final int m = ( 1 << l ) - 1;            // mask of first chunk
            storage = ( ( ~( m << lowBitnum ) ) & storage ) // keep non-written storage bits
                      | ( ( m & data ) << lowBitnum );      // overwrite storage w/ used data bits
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
        check(UNIT_SIZE, bitnum);
        return 0 != ( storage & ( 1 << bitnum ) ) ;
    }
    @Override
    public final boolean put(final int bitnum, final boolean bit) throws IndexOutOfBoundsException {
        check(UNIT_SIZE, bitnum);
        final int m = 1 << bitnum;
        final boolean prev = 0 != ( storage & m ) ;
        if( prev != bit ) {
            if( bit ) {
                storage |=  m;
            } else {
                storage &= ~m;
            }
        }
        return prev;
    }
    @Override
    public final void set(final int bitnum) throws IndexOutOfBoundsException {
        check(UNIT_SIZE, bitnum);
        final int m = 1 << bitnum;
        storage |=  m;
    }
    @Override
    public final void clear (final int bitnum) throws IndexOutOfBoundsException {
        check(UNIT_SIZE, bitnum);
        final int m = 1 << bitnum;
        storage &= ~m;
    }
    @Override
    public final boolean copy(final int srcBitnum, final int dstBitnum) throws IndexOutOfBoundsException {
        check(UNIT_SIZE, srcBitnum);
        check(UNIT_SIZE, dstBitnum);
        // get
        final boolean bit = 0 != ( storage & ( 1 << srcBitnum ) ) ;
        // put
        final int m = 1 << dstBitnum;
        if( bit ) {
            storage |=  m;
        } else {
            storage &= ~m;
        }
        return bit;
    }

    @Override
    public int bitCount() {
        return Bitfield.Util.bitCount(storage);
    }
}
