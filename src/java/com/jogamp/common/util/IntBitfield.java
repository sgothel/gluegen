/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

/**
 * @deprecated Use {@link Bitfield} implementations via {@link Bitfield.Factory#create(int)}.
 * <p>
 * Simple bitfield holder class using an int[] storage.
 * </p>
 * <p>
 * IntBitfield allows convenient access of a wide field of transient bits using efficient storage in O(1).
 * </p>
 * <p>
 * It can be used e.g. to map key-codes to pressed-state etc.
 * </p>
 */
public class IntBitfield {
    /** Unit size in bits, here 32 bits for one int unit. */
    public static final int UNIT_SIZE = 32;

    private static final long UNIT_SHIFT_L = 5L;
    private static final int UNIT_SHIFT_I = 5;

    private final int[] storage;
    private final long bitsCountL;
    private final int bitsCountI;

    /**
     * @param bitCount
     */
    public IntBitfield(final long bitCount) {
        final int units = (int) Math.max(1L, ( bitCount + 31L ) >>> UNIT_SHIFT_L);
        this.storage = new int[units];
        this.bitsCountL = (long)units << UNIT_SHIFT_L ;
        this.bitsCountI = bitsCountL > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)bitsCountL;
    }

    /**
     * @param bitCount
     */
    public IntBitfield(final int bitCount) {
        final int units = Math.max(1, ( bitCount + 31 ) >>> UNIT_SHIFT_I);
        this.storage = new int[units];
        this.bitsCountI = units << UNIT_SHIFT_I;
        this.bitsCountL = bitsCountI;
    }

    private final void check(final long bitnum) {
        if( 0 > bitnum || bitnum >= bitsCountL ) {
            throw new ArrayIndexOutOfBoundsException("Bitnum should be within [0.."+(bitsCountL-1)+"], but is "+bitnum);
        }
    }
    private final void check(final int bitnum) {
        if( 0 > bitnum || bitnum >= bitsCountI ) {
            throw new ArrayIndexOutOfBoundsException("Bitnum should be within [0.."+(bitsCountI-1)+"], but is "+bitnum);
        }
    }

    /** Return the capacity of this bit field, i.e. the number of bits stored int this field. */
    public final long capacity() { return bitsCountL; }

    /** Return <code>true</code> if the bit at position <code>bitnum</code> is set, otherwise <code>false</code>. */
    public final boolean get(final long bitnum) {
        check(bitnum);
        final int u = (int) ( bitnum >>> UNIT_SHIFT_L );
        final int b = (int) ( bitnum - ( (long)u << UNIT_SHIFT_L ) );
        return 0 != ( storage[u] & ( 1 << b ) ) ;
    }

    /** Return <code>true</code> if the bit at position <code>bitnum</code> is set, otherwise <code>false</code>. */
    public final boolean get(final int bitnum) {
        check(bitnum);
        final int u = bitnum >>> UNIT_SHIFT_I;
        final int b = bitnum - ( u << UNIT_SHIFT_I );
        return 0 != ( storage[u] & ( 1 << b ) ) ;
    }

    /**
     * Set or clear the bit at position <code>bitnum</code> according to <code>bit</code>
     * and return the previous value.
     */
    public final boolean put(final long bitnum, final boolean bit) {
        check(bitnum);
        final int u = (int) ( bitnum >>> UNIT_SHIFT_L );
        final int b = (int) ( bitnum - ( (long)u << UNIT_SHIFT_L ) );
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

    /**
     * Set or clear the bit at position <code>bitnum</code> according to <code>bit</code>
     * and return the previous value.
     */
    public final boolean put(final int bitnum, final boolean bit) {
        check(bitnum);
        final int u = bitnum >>> UNIT_SHIFT_I;
        final int b = bitnum - ( u << UNIT_SHIFT_I );
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
    /**
     * @deprecated Use {@link Bitfield.Util#bitCount(int)}.
     */
    public static final int getBitCount(final int n) {
        return Bitfield.Util.bitCount(n);
    }

    /**
     * Returns the number of set bits within this bitfield.
     * <p>
     * Utilizes {#link {@link #getBitCount(int)}}.
     * </p>
     */
    public long getBitCount() {
        long c = 0;
        for(int i = storage.length-1; i>=0; i--) {
            c += Bitfield.Util.bitCount(storage[i]);
        }
        return c;
    }
}
