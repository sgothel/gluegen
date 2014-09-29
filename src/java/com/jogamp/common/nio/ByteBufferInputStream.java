/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.common.nio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/**
 * An {@link InputStream} implementation based on an underlying {@link ByteBuffer}
 * supporting {@link #markSupported() mark}.
 * <p>
 * May be utilized as well with a {@link MappedByteBuffer memory-mapped} {@link FileChannel#map(MapMode, long, long) FileChannel}
 * using a size &le; {@link Integer#MAX_VALUE}.<br>
 * This becomes efficient with files &ge; 10 MiB, depending on the platform
 * and whether the traditional method uses a {@link BufferedInputStream} supporting {@code mark} incl. it's buffer size.<br>
 * See test case {@code com.jogamp.common.nio.TestByteBufferInputStream}.
 * </p>
 * @since 2.3.0
 */
public class ByteBufferInputStream extends InputStream {
    private final ByteBuffer buf;
    private int mark;

    /**
     * Creates a new byte-buffer input stream.
     *
     * @param buf the underlying byte buffer.
     */
    public ByteBufferInputStream(final ByteBuffer buf) {
        this.buf = buf;
        this.mark = -1;
    }

    @Override
    public final int available() {
        return buf.remaining();
    }

    /**
     * <i>This implementation supports {@code mark}.</i>
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public final boolean markSupported() {
        return true;
    }

    /**
     * <i>This implementation supports {@code mark}.</i>
     * <p>
     * {@inheritDoc}
     * </p>
     * @see #markSupported()
     */
    @Override
    public final synchronized void mark(final int unused) {
        mark = buf.position();
    }

    /**
     * <i>This implementation supports {@code mark}.</i>
     * <p>
     * {@inheritDoc}
     * </p>
     * @see #markSupported()
     */
    @Override
    public final synchronized void reset() throws IOException {
        if ( mark == -1 ) {
            throw new IOException();
        }
        buf.position(mark);
    }

    @Override
    public final long skip(final long n) throws IOException {
        if( 0 > n ) {
            return 0;
        }
        final int s = (int) Math.min( buf.remaining(), n );
        buf.position(buf.position() + s);
        return s;
    }

    @Override
    public final int read() {
        if ( ! buf.hasRemaining() ) {
            return -1;
        }
        return buf.get() & 0xFF;
    }

    @Override
    public final int read(final byte[] b, final int off, final int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if( off < 0 ||
                   len < 0 ||
                   off > b.length ||
                   off + len > b.length ||
                   off + len < 0
                 ) {
            throw new IndexOutOfBoundsException("offset "+off+", length "+len+", b.length "+b.length);
        } else if ( 0 == len ) {
            return 0;
        }
        final int totalRem = buf.remaining();
        if ( 0 == totalRem ) {
            return -1;
        }
        final int maxLen = Math.min(totalRem, len);
        if( buf.hasArray() ) {
            System.arraycopy(buf.array(), buf.arrayOffset() + buf.position(), b, off, maxLen);
            buf.position( buf.position() + maxLen );
        } else {
            buf.get(b, off, maxLen);
        }
        return maxLen;
    }

    // @Override
    public final int read(final ByteBuffer b, final int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (len < 0 || len > b.remaining()) {
            throw new IndexOutOfBoundsException("length "+len+", b "+b);
        } else if ( 0 == len ) {
            return 0;
        }
        final int remaining = buf.remaining();
        if ( 0 == remaining ) {
            return -1;
        }
        final int maxLen = Math.min(remaining, len);
        if( buf.hasArray() && b.hasArray() ) {
            System.arraycopy(buf.array(), buf.arrayOffset() + buf.position(), b.array(), b.arrayOffset() + b.position(), maxLen);
            buf.position( buf.position() + maxLen );
            b.position( b.position() + maxLen );
        } else if( maxLen == remaining ) {
            b.put(buf);
        } else {
            final int _limit = buf.limit();
            buf.limit(maxLen);
            try {
                b.put(buf);
            } finally {
                buf.limit(_limit);
            }
        }
        return maxLen;
    }

    public final ByteBuffer getBuffer() { return buf; }
}
