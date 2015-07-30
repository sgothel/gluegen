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
    public int getStorageBitSize() {
        return UNIT_SIZE;
    }

    private final void check(final int limit, final int bitnum) throws IndexOutOfBoundsException {
        if( 0 > bitnum || bitnum >= limit ) {
            throw new IndexOutOfBoundsException("Bitnum should be within [0.."+(limit-1)+"], but is "+bitnum);
        }
    }

    @Override
    public final int getInt32(final int rightBitnum) throws IndexOutOfBoundsException {
        check(UNIT_SIZE-31, rightBitnum);
        return storage;
    }

    @Override
    public final void putInt32(final int rightBitnum, final int mask) throws IndexOutOfBoundsException {
        check(UNIT_SIZE-31, rightBitnum);
        storage = mask;
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
    public int getBitCount() {
        return Bitfield.Util.getBitCount(storage);
    }
}
