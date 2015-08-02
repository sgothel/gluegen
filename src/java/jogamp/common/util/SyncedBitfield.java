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
 * Simple synchronized {@link Bitfield} by wrapping an existing {@link Bitfield}.
 */
public class SyncedBitfield implements Bitfield {
    private final Bitfield impl;

    public SyncedBitfield(final Bitfield impl) {
        this.impl = impl;
    }

    @Override
    public final synchronized int size() {
        return impl.size();
    }

    @Override
    public final synchronized void clearField(final boolean bit) {
        impl.clearField(bit);
    }

    @Override
    public final synchronized int get32(final int lowBitnum, final int length) throws IndexOutOfBoundsException {
        return impl.get32(lowBitnum, length);
    }

    @Override
    public final synchronized void put32(final int lowBitnum, final int length, final int data) throws IndexOutOfBoundsException {
        impl.put32(lowBitnum, length, data);
    }

    @Override
    public final synchronized int copy32(final int srcLowBitnum, final int dstLowBitnum, final int length) throws IndexOutOfBoundsException {
        return impl.copy32(srcLowBitnum, dstLowBitnum, length);
    }

    @Override
    public final synchronized boolean get(final int bitnum) throws IndexOutOfBoundsException {
        return impl.get(bitnum);
    }

    @Override
    public final synchronized boolean put(final int bitnum, final boolean bit) throws IndexOutOfBoundsException {
        return impl.put(bitnum, bit);
    }

    @Override
    public final synchronized void set(final int bitnum) throws IndexOutOfBoundsException {
        impl.set(bitnum);
    }

    @Override
    public final synchronized void clear(final int bitnum) throws IndexOutOfBoundsException {
        impl.clear(bitnum);
    }

    @Override
    public final synchronized boolean copy(final int srcBitnum, final int dstBitnum) throws IndexOutOfBoundsException {
        return impl.copy(srcBitnum, dstBitnum);
    }

    @Override
    public final synchronized int bitCount() {
        return impl.bitCount();
    }
}