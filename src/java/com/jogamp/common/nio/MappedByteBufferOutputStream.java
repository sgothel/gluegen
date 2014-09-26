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

import java.io.IOException;
import java.io.OutputStream;

public class MappedByteBufferOutputStream extends OutputStream {
    private final MappedByteBufferInputStream parent;

    MappedByteBufferOutputStream(final MappedByteBufferInputStream stream) throws IOException {
        this.parent = stream;
    }

    /**
     * See {@link MappedByteBufferInputStream#setLength(long)}.
     */
    public final synchronized void setLength(final long newTotalSize) throws IOException {
        parent.setLength(newTotalSize);
    }

    /**
     * See {@link MappedByteBufferInputStream#notifyLengthChange(long)}.
     */
    public final synchronized void notifyLengthChange(final long newTotalSize) throws IOException {
        parent.notifyLengthChange(newTotalSize);
    }

    /**
     * See {@link MappedByteBufferInputStream#length()}.
     */
    public final synchronized long length() {
        return parent.length();
    }

    /**
     * See {@link MappedByteBufferInputStream#remaining()}.
     */
    public final synchronized long remaining() throws IOException {
        return parent.remaining();
    }

    /**
     * See {@link MappedByteBufferInputStream#position()}.
     */
    public final synchronized long position() throws IOException {
        return parent.position();
    }

    /**
     * See {@link MappedByteBufferInputStream#position(long)}.
     */
    public final synchronized MappedByteBufferInputStream position( final long newPosition ) throws IOException {
        return parent.position(newPosition);
    }

    /**
     * See {@link MappedByteBufferInputStream#skip(long)}.
     */
    public final synchronized long skip( final long n ) throws IOException {
        return parent.skip(n);
    }

    @Override
    public final synchronized void flush() throws IOException {
        parent.flush();
    }

    @Override
    public final synchronized void close() throws IOException {
        parent.close();
    }

    @Override
    public final synchronized void write(final int b) throws IOException {
        parent.checkOpen();
        final long totalRem = parent.remaining();
        if ( totalRem < 1 ) { // grow if required
            parent.setLength( parent.length() + 1 );
        }
        if ( ! parent.slice( parent.currSlice ).hasRemaining() ) {
            if ( !parent.nextSlice() ) {
                if( MappedByteBufferInputStream.DEBUG ) {
                    System.err.println("EOT write: "+parent.slices[ parent.currSlice ]);
                    parent.dbgDump("EOT write:", System.err);
                }
                throw new IOException("EOT");
            }
        }
        parent.slices[ parent.currSlice ].put( (byte)(b & 0xFF) );
    }

    @Override
    public final synchronized void write(final byte b[], final int off, final int len) throws IOException {
        parent.checkOpen();
        if (b == null) {
            throw new NullPointerException();
        } else if( off < 0 ||
                   off > b.length ||
                   len < 0 ||
                   off + len > b.length ||
                   off + len < 0
                 ) {
            throw new IndexOutOfBoundsException("offset "+off+", length "+len+", b.length "+b.length);
        } else if( 0 == len ) {
            return;
        }
        final long totalRem = parent.remaining();
        if ( totalRem < len ) { // grow if required
            parent.setLength( parent.length() + len - totalRem );
        }
        int written = 0;
        while( written < len ) {
            int currRem = parent.slice( parent.currSlice ).remaining();
            if ( 0 == currRem ) {
                if ( !parent.nextSlice() ) {
                    if( MappedByteBufferInputStream.DEBUG ) {
                        System.err.println("EOT write: offset "+off+", length "+len+", b.length "+b.length);
                        System.err.println("EOT write: written "+written+" / "+len+", currRem "+currRem);
                        System.err.println("EOT write: "+parent.slices[ parent.currSlice ]);
                        parent.dbgDump("EOT write:", System.err);
                    }
                    throw new InternalError("EOT");
                }
                currRem = parent.slice( parent.currSlice ).remaining();
            }
            parent.slices[ parent.currSlice ].put( b, off + written, Math.min( len - written, currRem ) );
            written += Math.min( len - written, currRem );
        }
    }
}
