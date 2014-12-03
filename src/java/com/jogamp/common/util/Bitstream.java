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
package com.jogamp.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import jogamp.common.Debug;

/**
 * Versatile Bitstream implementation supporting:
 * <ul>
 *   <li>Utilize I/O operations on I/O streams, buffers and arrays</li>
 *   <li>Consider MSBfirst / LSBfirst mode</li>
 *   <li>Linear bit R/W operations</li>
 *   <li>Bulk R/W operations w/ endian related type conversion</li>
 *   <li>Allow mark/reset and switching streams and input/output mode</li>
 *   <li>Optimized operations</li>
 * </ul>
 */
public class Bitstream<T> {
    private static final boolean DEBUG = Debug.debug("Bitstream");

    /** End of stream marker, {@value} or 0xFFFFFFFF */
    public static final int EOS = -1;

    /**
     * General byte stream.
     */
    public static interface ByteStream<T> {
        /** Sets the underlying stream, without {@link #close()}ing the previous one. */
        void setStream(final T stream);

        /** Returns the underlying stream */
        T getStream();

        /**
         * Closing the underlying stream, implies {@link #flush()}.
         * <p>
         * Implementation will <code>null</code> the stream references,
         * hence {@link #setStream(Object)} must be called before re-using instance.
         * </p>
         * @throws IOException
         */
        void close() throws IOException;

        /**
         * Synchronizes all underlying {@link #canOutput() output stream} operations, or do nothing.
         * @throws IOException
         */
        void flush() throws IOException;

        /** Return true if stream can handle input, i.e. {@link #read()}. */
        boolean canInput();

        /** Return true if stream can handle output, i.e. {@link #write(byte)} */
        boolean canOutput();

        /**
         * Returns the byte position in the stream.
         */
        long position();

        /**
         * Sets this stream's position.
         * <p>
         * A set mark is cleared if &gt; new position.
         * </p>
         * <p>
         * Returns {@link Bitstream#EOS} is end-of-stream is reached,
         * otherwise the new position.
         * </p>
         * <p>
         * Known supporting implementation is {@link ByteBufferStream} and {@link ByteArrayStream}.
         * </p>
         *
         * @param newPosition The new positive position.
         *
         * @return The new set position or {@link Bitstream#EOS} if end-of-stream is reached.
         *
         * @throws UnsupportedOperationException if not supported, i.e. {@link ByteInputStream} or {@link ByteOutputStream}
         * @throws IllegalArgumentException If the {@code newPosition} is negative
         */
        long position(long newPosition) throws UnsupportedOperationException, IllegalArgumentException;

        /**
         * It is implementation dependent, whether backward skip giving a negative number is supported or not.
         * @param n number of bytes to skip
         * @return actual skipped bytes
         * @throws IOException
         */
        long skip(final long n) throws IOException;

        /**
         * Set {@code markpos} to current position, allowing the stream to be {@link #reset()}.
         * @param readlimit maximum number of bytes able to read before invalidating the {@code markpos}.
         * @throws UnsupportedOperationException if not supported, i.e. if stream is not an {@link #canInput() input stream}.
         */
        void mark(final int readLimit) throws UnsupportedOperationException;

        /**
         * Reset stream position to <i>markpos</i> as set via {@link #mark(int)}.
         * <p>
         * <i>markpos</i> is kept, hence {@link #reset()} can be called multiple times.
         * </p>
         * @throws UnsupportedOperationException if not supported, i.e. if stream is not an {@link #canInput() input stream}.
         * @throws IllegalStateException if <i>markpos</i> has not been set via {@link #mark(int)} or reset operation failed.
         * @throws IOException if reset operation failed.
         */
        void reset() throws UnsupportedOperationException, IllegalStateException, IOException;

        /**
         * Reads one byte from the stream.
         * <p>
         * Returns {@link Bitstream#EOS} is end-of-stream is reached,
         * otherwise the resulting value.
         * </p>
         * @throws IOException
         * @throws UnsupportedOperationException if not supported, i.e. if stream is not an {@link #canInput() input stream}.
         */
        int read() throws UnsupportedOperationException, IOException;

        /**
         * Writes one byte, to the stream.
         * <p>
         * Returns {@link Bitstream#EOS} is end-of-stream is reached,
         * otherwise the written value.
         * </p>
         * @throws IOException
         * @throws UnsupportedOperationException if not supported, i.e. if stream is not an {@link #canOutput() output stream}.
         */
        int write(final byte val) throws UnsupportedOperationException, IOException;
    }

    /**
     * Specific {@link ByteStream byte stream}.
     * <p>
     * Can handle {@link #canInput() input} and {@link #canOutput() output} operations.
     * </p>
     */
    public static class ByteArrayStream implements ByteStream<byte[]> {
        private byte[] media;
        private int pos;
        private int posMark;

        public ByteArrayStream(final byte[] stream) {
            setStream(stream);
        }

        @Override
        public void setStream(final byte[] stream) {
            media = stream;
            pos = 0;
            posMark = -1;
        }

        @Override
        public byte[] getStream() { return media; }

        @Override
        public void close() {
            media = null;
        }
        @Override
        public void flush() {
            // NOP
        }

        @Override
        public boolean canInput() { return true; }

        @Override
        public boolean canOutput() { return true; }

        @Override
        public long position() { return pos; }

        @Override
        public long position(final long newPosition) throws UnsupportedOperationException, IllegalArgumentException {
            if( newPosition >= media.length ) {
                return Bitstream.EOS;
            }
            pos = (int)newPosition;
            if( posMark > pos ) {
                posMark = -1;
            }
            return pos;
        }

        @Override
        public long skip(final long n) {
            final long skip;
            if( n >= 0 ) {
                final int remaining = media.length - pos;
                skip = Math.min(remaining, (int)n);
            } else {
                final int n2 = (int)n * -1;
                skip = -1 * Math.min(pos, n2);
            }
            pos += skip;
            return skip;
        }

        @Override
        public void mark(final int readLimit) {
            posMark = pos;
        }

        @Override
        public void reset() throws IllegalStateException {
            if( 0 > posMark ) {
                throw new IllegalStateException("markpos not set");
            }
            if(DEBUG) { System.err.println("rewind: "+pos+" -> "+posMark); }
            pos = posMark;
        }

        @Override
        public int read() {
            final int r;
            if( media.length > pos ) {
                r = 0xff & media[pos++];
            } else {
                r = -1; // EOS
            }
            if( DEBUG ) {
                if( EOS != r ) {
                    System.err.println("u8["+(pos-1)+"] -> "+toHexBinString(true, r, 8));
                } else {
                    System.err.println("u8["+(pos-0)+"] -> EOS");
                }
            }
            return r;
        }

        @Override
        public int write(final byte val) {
            final int r;
            if( media.length > pos ) {
                media[pos++] = val;
                r = 0xff & val;
            } else {
                r = -1; // EOS
            }
            if( DEBUG ) {
                if( EOS != r ) {
                    System.err.println("u8["+(pos-1)+"] <- "+toHexBinString(true, r, 8));
                } else {
                    System.err.println("u8["+(pos-0)+"] <- EOS");
                }
            }
            return r;
        }
    }

    /**
     * Specific {@link ByteStream byte stream}.
     * <p>
     * Can handle {@link #canInput() input} and {@link #canOutput() output} operations.
     * </p>
     */
    public static class ByteBufferStream implements ByteStream<ByteBuffer> {
        private ByteBuffer media;
        private int pos;
        private int posMark;

        public ByteBufferStream(final ByteBuffer stream) {
            setStream(stream);
        }

        @Override
        public void setStream(final ByteBuffer stream) {
            media = stream;
            pos = 0;
            posMark = -1;
        }

        @Override
        public ByteBuffer getStream() { return media; }

        @Override
        public void close() {
            media = null;
        }
        @Override
        public void flush() {
            // NOP
        }

        @Override
        public boolean canInput() { return true; }

        @Override
        public boolean canOutput() { return true; }

        @Override
        public long position() { return pos; }

        @Override
        public long position(final long newPosition) throws UnsupportedOperationException, IllegalArgumentException {
            if( newPosition >= media.limit() ) {
                return Bitstream.EOS;
            }
            media.position((int)newPosition);
            pos = (int)newPosition;
            if( posMark > pos ) {
                posMark = -1;
            }
            return pos;
        }

        @Override
        public long skip(final long n) {
            final long skip;
            if( n >= 0 ) {
                final int remaining = media.limit() - pos;
                skip = Math.min(remaining, (int)n);
            } else {
                final int n2 = (int)n * -1;
                skip = -1 * Math.min(pos, n2);
            }
            pos += skip;
            return skip;
        }

        @Override
        public void mark(final int readLimit) {
            posMark = pos;
        }

        @Override
        public void reset() throws IllegalStateException {
            if( 0 > posMark ) {
                throw new IllegalStateException("markpos not set");
            }
            if(DEBUG) { System.err.println("rewind: "+pos+" -> "+posMark); }
            media.position(posMark);
            pos = posMark;
        }

        @Override
        public int read() {
            final int r;
            if( media.limit() > pos ) {
                r = 0xff & media.get(pos++);
            } else {
                r = -1; // EOS
            }
            if( DEBUG ) {
                if( EOS != r ) {
                    System.err.println("u8["+(pos-1)+"] -> "+toHexBinString(true, r, 8));
                } else {
                    System.err.println("u8["+(pos-0)+"] -> EOS");
                }
            }
            return r;
        }

        @Override
        public int write(final byte val) {
            final int r;
            if( media.limit() > pos ) {
                media.put(pos++, val);
                r = 0xff & val;
            } else {
                r = -1; // EOS
            }
            if( DEBUG ) {
                if( EOS != r ) {
                    System.err.println("u8["+(pos-1)+"] <- "+toHexBinString(true, r, 8));
                } else {
                    System.err.println("u8["+(pos-0)+"] <- EOS");
                }
            }
            return r;
        }
    }

    /**
     * Specific {@link ByteStream byte stream}.
     * <p>
     * Can handle {@link #canInput() input} operations only.
     * </p>
     */
    public static class ByteInputStream implements ByteStream<InputStream> {
        private BufferedInputStream media;
        private long pos;
        private long posMark;

        public ByteInputStream(final InputStream stream) {
            setStream(stream);
        }

        @Override
        public void setStream(final InputStream stream) {
            if( stream instanceof BufferedInputStream ) {
                media = (BufferedInputStream) stream;
            } else if( null != stream ) {
                media = new BufferedInputStream(stream);
            } else {
                media = null;
            }
            pos = 0;
            posMark = -1;
        }

        @Override
        public InputStream getStream() { return media; }

        @Override
        public void close() throws IOException {
            if( null != media ) {
                media.close();
                media = null;
            }
        }
        @Override
        public void flush() {
            // NOP
        }

        @Override
        public boolean canInput() { return true; }

        @Override
        public boolean canOutput() { return false; }

        @Override
        public long position() { return pos; }

        @Override
        public long position(final long newPosition) throws UnsupportedOperationException, IllegalArgumentException {
            throw new UnsupportedOperationException("N/a for "+getClass().getCanonicalName());
        }

        @Override
        public long skip(final long n) throws IOException {
            final long skip = media.skip(n);
            pos += skip;
            return skip;
        }

        @Override
        public void mark(final int readLimit) {
            media.mark(readLimit);
            posMark = pos;
        }

        @Override
        public void reset() throws IllegalStateException, IOException {
            if( 0 > posMark ) {
                throw new IllegalStateException("markpos not set");
            }
            if(DEBUG) { System.err.println("rewind: "+pos+" -> "+posMark); }
            media.reset();
            pos = posMark;
        }

        @Override
        public int read() throws IOException {
            final int r = media.read();
            if(DEBUG) {
                if( EOS != r ) {
                    System.err.println("u8["+pos+"] -> "+toHexBinString(true, r, 8));
                } else {
                    System.err.println("u8["+pos+"] -> EOS");
                }
            }
            if( EOS != r ) {
                pos++;
            }
            return r;
        }

        @Override
        public int write(final byte val) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("not allowed with input stream");
        }
    }

    /**
     * Specific {@link ByteStream byte stream}.
     * <p>
     * Can handle {@link #canOutput() output} operations only.
     * </p>
     */
    public static class ByteOutputStream implements ByteStream<OutputStream> {
        private BufferedOutputStream media;
        private long pos = 0;

        public ByteOutputStream(final OutputStream stream) {
            setStream(stream);
        }

        @Override
        public void setStream(final OutputStream stream) {
            if( stream instanceof BufferedOutputStream ) {
                media = (BufferedOutputStream) stream;
            } else if( null != stream ) {
                media = new BufferedOutputStream(stream);
            } else {
                media = null;
            }
            pos = 0;
        }

        @Override
        public void close() throws IOException {
            if( null != media ) {
                media.close();
                media = null;
            }
        }
        @Override
        public void flush() throws IOException {
            if( null != media ) {
                media.flush();
            }
        }

        @Override
        public boolean canInput() { return false; }

        @Override
        public boolean canOutput() { return true; }

        @Override
        public long position() { return pos; }

        @Override
        public long position(final long newPosition) throws UnsupportedOperationException, IllegalArgumentException {
            throw new UnsupportedOperationException("N/a for "+getClass().getCanonicalName());
        }

        @Override
        public long skip(final long n) throws IOException {
            long i = n;
            while(i > 0) {
                media.write(0);
                i--;
            }
            final long skip = n-i; // should be n
            pos += skip;
            return skip;
        }

        @Override
        public OutputStream getStream() { return media; }

        @Override
        public void mark(final int readLimit) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("not allowed with output stream");
        }

        @Override
        public void reset() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("not allowed with output stream");
        }

        @Override
        public int read() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("not allowed with output stream");
        }

        @Override
        public int write(final byte val) throws IOException {
            final int r = 0xff & val;
            media.write(r);
            if(DEBUG) {
                System.err.println("u8["+pos+"] <- "+toHexBinString(true, r, 8));
            }
            pos++;
            return r;
        }
    }

    private ByteStream<T> bytes;
    /** 8-bit cache of byte stream */
    private int bitBuffer;
    private int bitsDataMark;

    /** See {@link #getBitCount()}. */
    private int bitCount;
    private int bitsCountMark;

    private boolean outputMode;
    private boolean throwIOExceptionOnEOF;

    /**
     * @param stream
     * @param outputMode
     * @throws IllegalArgumentException if requested <i>outputMode</i> doesn't match stream's {@link #canInput()} and {@link #canOutput()}.
     */
    public Bitstream(final ByteStream<T> stream, final boolean outputMode) throws IllegalArgumentException {
        this.bytes = stream;
        this.outputMode = outputMode;
        resetLocal();
        validateMode();
        throwIOExceptionOnEOF = false;
    }

    private final void resetLocal() {
        bitBuffer = 0;
        bitCount = 0;
        bitsDataMark = 0;
        bitsCountMark = -1;
    }
    private final void validateMode() throws IllegalArgumentException {
        if( !canInput() && !canOutput() ) {
            throw new IllegalArgumentException("stream can neither input nor output: "+this);
        }
        if( outputMode && !canOutput() ) {
            throw new IllegalArgumentException("stream cannot output as requested: "+this);
        }
        if( !outputMode && !canInput() ) {
            throw new IllegalArgumentException("stream cannot input as requested: "+this);
        }
    }

    /**
     * Enables or disables throwing an {@link IOException} in case {@link #EOS} appears.
     * <p>
     * Default behavior for I/O methods is not to throw an {@link IOException}, but to return {@link #EOS}.
     * </p>
     */
    public final void setThrowIOExceptionOnEOF(final boolean enable) {
        throwIOExceptionOnEOF = enable;
    }

    /** Returns true if I/O methods throw an {@link IOException} if {@link #EOS} appears, otherwise false (default). */
    public final boolean getThrowIOExceptionOnEOF() { return throwIOExceptionOnEOF; }

    /**
     * Sets the underlying stream, without {@link #close()}ing the previous one.
     * <p>
     * If the previous stream was in {@link #canOutput() output mode},
     * {@link #flush()} is being called.
     * </p>
     * @throws IllegalArgumentException if requested <i>outputMode</i> doesn't match stream's {@link #canInput()} and {@link #canOutput()}.
     * @throws IOException could be caused by {@link #flush()}.
     */
    public final void setStream(final T stream, final boolean outputMode) throws IllegalArgumentException, IOException {
        if( null != bytes && this.outputMode ) {
            flush();
        }
        this.bytes.setStream(stream);
        this.outputMode = outputMode;
        resetLocal();
        validateMode();
    }

    /** Returns the currently used {@link ByteStream}. */
    public final ByteStream<T> getStream() { return bytes; }

    /** Returns the currently used {@link ByteStream}'s {@link ByteStream#getStream()}. */
    public final T getSubStream() { return bytes.getStream(); }

    /**
     * Closing the underlying stream, implies {@link #flush()}.
     * <p>
     * Implementation will <code>null</code> the stream references,
     * hence {@link #setStream(Object)} must be called before re-using instance.
     * </p>
     * <p>
     * If the closed stream was in {@link #canOutput() output mode},
     * {@link #flush()} is being called.
     * </p>
     *
     * @throws IOException
     */
    public final void close() throws IOException {
        if( null != bytes && this.outputMode ) {
            flush();
        }
        bytes.close();
        bytes = null;
        resetLocal();
    }

    /**
     * Synchronizes all underlying {@link ByteStream#canOutput() output stream} operations, or do nothing.
     * <p>
     * Method also flushes incomplete bytes to the underlying {@link ByteStream}
     * and hence skips to the next byte position.
     * </p>
     * @return {@link #EOS} caused by writing, otherwise zero.
     * @throws IllegalStateException if not in output mode or stream closed
     * @throws IOException
     */
    public final int flush() throws IllegalStateException, IOException {
        if( !outputMode || null == bytes ) {
            throw new IllegalStateException("not in output-mode: "+this);
        }
        bytes.flush();
        if( 0 != bitCount ) {
            final int r = bytes.write((byte)bitBuffer);
            bitBuffer = 0;
            bitCount = 0;
            if( EOS == r ) {
                if( throwIOExceptionOnEOF ) {
                    throw new IOException("EOS "+this);
                }
                return EOS;
            }
        }
        return 0;
    }

    /** Return true if stream can handle input, i.e. {@link #readBit(boolean)}. */
    public final boolean canInput() { return null != bytes ? bytes.canInput() : false; }

    /** Return true if stream can handle output, i.e. {@link #writeBit(boolean, int)}. */
    public final boolean canOutput() { return null != bytes ? bytes.canOutput() : false; }

    /**
     * Set {@code markpos} to current position, allowing the stream to be {@link #reset()}.
     * @param readlimit maximum number of bytes able to read before invalidating the {@code markpos}.
     * @throws IllegalStateException if not in input mode or stream closed
     */
    public final void mark(final int readLimit) throws IllegalStateException {
        if( outputMode || null == bytes ) {
            throw new IllegalStateException("not in input-mode: "+this);
        }
        bytes.mark(readLimit);
        bitsDataMark = bitBuffer;
        bitsCountMark = bitCount;
    }

    /**
     * Reset stream position to <i>markpos</i> as set via {@link #mark(int)}.
     * <p>
     * <i>markpos</i> is kept, hence {@link #reset()} can be called multiple times.
     * </p>
     * @throws IllegalStateException if not in input mode or stream closed
     * @throws IllegalStateException if <i>markpos</i> has not been set via {@link #mark(int)} or reset operation failed.
     * @throws IOException if reset operation failed.
     */
    public final void reset() throws IllegalStateException, IOException {
        if( outputMode || null == bytes ) {
            throw new IllegalStateException("not in input-mode: "+this);
        }
        if( 0 > bitsCountMark ) {
            throw new IllegalStateException("markpos not set: "+this);
        }
        bytes.reset();
        bitBuffer = bitsDataMark;
        bitCount = bitsCountMark;
    }

    /**
     * Number of remaining bits in cache to read before next byte-read (input mode)
     * or number of remaining bits to be cached before next byte-write (output mode).
     * <p>
     * Counting down from 7..0 7..0, starting with 0.
     * </p>
     * <p>
     * In input mode, zero indicates reading a new byte and cont. w/ 7.
     * In output mode, the cached byte is written when flipping over to 0.
     * </p>
     */
    public final int getBitCount() { return bitCount; }

    /**
     * Return the last bit number read or written counting from [0..7].
     * If no bit access has been performed, 7 is returned.
     * <p>
     * Returned value is normalized [0..7], i.e. independent from <i>msb</i> or <i>lsb</i> read order.
     * </p>
     */
    public final int getLastBitPos() { return 7 - bitCount; }

    /**
     * Return the next bit number to be read or write counting from [0..7].
     * If no bit access has been performed, 0 is returned.
     * <p>
     * Returned value is normalized [0..7], i.e. independent from <i>msb</i> or <i>lsb</i> read order.
     * </p>
     */
    public final int getBitPosition() {
        if( 0 == bitCount ) {
            return 0;
        } else {
            return 8 - bitCount;
        }
    }

    /**
     * Returns the current bit buffer.
     * @see #getBitCount()
     */
    public final int getBitBuffer() { return bitBuffer; }

    /**
     * Returns the bit position in the stream.
     */
    public final long position() {
        // final long bytePos = bytes.position() - ( !outputMode && 0 != bitCount ? 1 : 0 );
        // return ( bytePos << 3 ) + getBitPosition();
        if( null == bytes ) {
            return EOS;
        } else if( 0 == bitCount ) {
            return bytes.position() << 3;
        } else {
            final long bytePos = bytes.position() - ( outputMode ? 0 : 1 );
            return ( bytePos << 3 ) + 8 - bitCount;
        }
    }

    /**
     * Sets this stream's bit position.
     * <p>
     * A set mark is cleared.
     * </p>
     * <p>
     * Returns {@link Bitstream#EOS} is end-of-stream is reached,
     * otherwise the new position.
     * </p>
     * <p>
     * Known supporting implementation is {@link ByteBufferStream} and {@link ByteArrayStream}.
     * </p>
     *
     * @param newPosition The new positive position.
     *
     * @return The new set position or {@link Bitstream#EOS} if end-of-stream is reached.
     *
     * @throws UnsupportedOperationException if not supported, i.e. {@link ByteInputStream} or {@link ByteOutputStream}
     * @throws IllegalArgumentException If the {@code newPosition} is negative
     * @throws IOException if read error occurs or EOS is reached and {@link #setThrowIOExceptionOnEOF(boolean)} is set to true.
     * @throws IllegalStateException
     */
    public final long position(final long newPosition) throws UnsupportedOperationException, IllegalArgumentException, IllegalStateException, IOException {
        if( 0 > newPosition ) {
            throw new IllegalArgumentException("new position not positive: "+newPosition);
        }
        bytes.position(0); // throws UnsupportedOperationException
        resetLocal();
        if( newPosition > skip(newPosition) ) {
            return EOS;
        }
        return newPosition;
    }

    /**
     * @param msbFirst if true incoming stream bit order is MSB to LSB, otherwise LSB to MSB.
     * @return the read bit or {@link #EOS} if end-of-stream is reached.
     * @throws IOException
     * @throws IllegalStateException if not in input mode or stream closed
     */
    public final int readBit(final boolean msbFirst) throws UnsupportedOperationException, IllegalStateException, IOException {
        if( outputMode || null == bytes ) {
            throw new IllegalStateException("not in input-mode: "+this);
        }
        if ( 0 < bitCount ) {
            bitCount--;
            if( msbFirst ) {
                return  ( bitBuffer >>> bitCount ) & 0x01;
            } else {
                return  ( bitBuffer >>> ( 7 - bitCount ) ) & 0x01;
            }
        } else {
            bitBuffer = bytes.read();
            if( EOS == bitBuffer ) {
                if( throwIOExceptionOnEOF ) {
                    throw new IOException("EOS "+this);
                }
                return EOS;
            } else {
                bitCount=7;
                if( msbFirst ) {
                    return bitBuffer >>> 7;
                } else {
                    return bitBuffer & 0x01;
                }
            }
        }
    }

    /**
     * @param msbFirst if true outgoing stream bit order is MSB to LSB, otherwise LSB to MSB.
     * @param bit
     * @return the currently written byte or {@link #EOS} if end-of-stream is reached.
     * @throws IOException
     * @throws IllegalStateException if not in output mode or stream closed
     */
    public final int writeBit(final boolean msbFirst, final int bit) throws IllegalStateException, IOException {
        if( !outputMode || null == bytes ) {
            throw new IllegalStateException("not in output-mode: "+this);
        }
        if ( 0 < bitCount ) {
            bitCount--;
            if( msbFirst ) {
                bitBuffer |= ( 0x01 & bit ) << bitCount;
            } else {
                bitBuffer |= ( 0x01 & bit ) << ( 7 - bitCount );
            }
            if( 0 == bitCount ) {
                final int r = bytes.write((byte)bitBuffer);
                if( throwIOExceptionOnEOF && EOS == r ) {
                    throw new IOException("EOS "+this);
                }
                return r;
            }
        } else {
            bitCount = 7;
            if( msbFirst ) {
                bitBuffer = ( 0x01 & bit ) << 7;
            } else {
                bitBuffer = 0x01 & bit;
            }
        }
        return bitBuffer;
    }

    /**
     * It is implementation dependent, whether backward skip giving a negative number is supported or not.
     *
     * @param n number of bits to skip
     * @return actual skipped bits
     * @throws IOException if read error occurs or EOS is reached and {@link #setThrowIOExceptionOnEOF(boolean)} is set to true.
     * @throws IllegalStateException if closed
     */
    public long skip(final long n) throws IllegalStateException, IOException {
        if( null == bytes ) {
            throw new IllegalStateException("closed: "+this);
        }
        if( DEBUG ) {
            System.err.println("Bitstream.skip.0: "+n+" - "+toStringImpl());
        }
        if( n > 0 ) {
            if( n <= bitCount ) {
                bitCount -= (int)n;
                if( DEBUG ) {
                    System.err.println("Bitstream.skip.F_N1: "+n+" - "+toStringImpl());
                }
                return n;
            } else { // n > bitCount
                if( outputMode ) {
                    if( 0 < bitCount ) {
                        if( EOS == bytes.write((byte)bitBuffer) ) {
                            return 0;
                        }
                    }
                    bitBuffer = 0;
                }
                final long n2 = n - bitCount;                // subtract cached bits, bitsCount is zero at this point
                final long n3 = n2 >>> 3;                    // bytes to skip
                final long n4 = bytes.skip(n3);              // actual skipped bytes
                final int n5 = (int) ( n2 - ( n3 << 3 ) );   // remaining skip bits == nX % 8
                final long nX = ( n4 << 3 ) + n5 + bitCount; // actual skipped bits
                /**
                if( DEBUG ) {
                    System.err.println("Bitstream.skip.1: n2 "+n2+", n3 "+n3+", n4 "+n4+", n5 "+n5+", nX "+nX+" - "+toStringImpl());
                } */
                if( nX < n ) {
                    // couldn't complete skipping .. EOS .. etc
                    bitCount = 0;
                    bitBuffer = 0;
                    if( DEBUG ) {
                        System.err.println("Bitstream.skip.F_EOS: "+n+" - "+toStringImpl());
                    }
                    if( throwIOExceptionOnEOF ) {
                        throw new IOException("EOS "+this);
                    }
                    return nX;
                }
                bitCount = ( 8 - n5 ) & 7; // % 8
                int notReadBits = 0;
                if( !outputMode && 0 < bitCount ) {
                    bitBuffer = bytes.read();
                    if( EOS == bitBuffer ) {
                        notReadBits = bitCount;
                        bitCount = 0;
                    }
                }
                if( DEBUG ) {
                    System.err.println("Bitstream.skip.F_N2: "+n+", notReadBits "+notReadBits+" - "+toStringImpl());
                }
                return nX - notReadBits;
            }
        } else {
            // Zero skip or backward skip
            // FIXME: Backward skip n < 0
            return 0;
        }
    }

    private static final boolean useFastPathStream = true;
    private static final boolean useFastPathTypes = true;

    /**
     * Return incoming bits as read via {@link #readBit(boolean)} LSB-first as little-endian.
     * <p>
     * The incoming bit order is from low- to most-significant-bit, maintaining bit LSB-first order.
     * </p>
     * @param n number of bits, maximum 31 bits
     * @return the read bits from 0-n in the given order or {@link #EOS}.
     * @throws IllegalStateException if not in input mode or stream closed
     * @throws IllegalArgumentException if n > 31
     * @throws IOException
     */
    public int readBits31(final int n) throws IllegalArgumentException, IOException {
        if( 31 < n ) {
            throw new IllegalArgumentException("n > 31: "+n);
        }
        if( outputMode || null == bytes ) {
            throw new IllegalStateException("not in input-mode: "+this);
        }
        if( 0 == n ) {
            return 0;
        } else {
            if( !useFastPathStream ) {
                // Slow path
                int r = 0;
                for(int i=0; i < n; i++) {
                    final int b = readBit(false /* msbFirst */);
                    if( EOS == b ) {
                        if( throwIOExceptionOnEOF ) {
                            throw new IOException("EOS "+this);
                        }
                        return EOS;
                    }
                    r |= b << i;
                }
                return r;
            } else {
                // fast path
                int c = n;
                final int n1 = Math.min(n, bitCount); // remaining portion
                int r;
                if( 0 < n1 ) {
                    final int m1 = ( 1 << n1 ) - 1;
                    final int s1 = 7 - bitCount + 1; // LSBfirst: right-shift to new bits
                    bitCount -= n1;
                    c -= n1;
                    // MSBfirst: r = ( m1 & ( bitBuffer >>> bitCount ) ) << c;
                    r = ( m1 & ( bitBuffer >>> s1 ) ); // LSBfirst
                    if( 0 == c ) {
                        return r;
                    }
                } else {
                    r = 0;
                }
                assert( 0 == bitCount );
                int s = n1; // LSBfirst: left shift for additional elements
                do {
                    bitBuffer = bytes.read();
                    if( EOS == bitBuffer ) {
                        if( throwIOExceptionOnEOF ) {
                            throw new IOException("EOS "+this);
                        }
                        return EOS;
                    }
                    final int n2 = Math.min(c, 8); // full portion
                    final int m2 = ( 1 << n2 ) - 1;
                    bitCount = 8 - n2;
                    c -= n2;
                    // MSBfirst: r |= ( m2 & ( bitBuffer >>> bitCount ) ) << c;
                    r |= ( m2 & bitBuffer ) << s; // LSBfirst on new bits
                    s += n2;
                } while ( 0 < c );
                return r;
            }
        }
    }

    /**
     * Write the given bits via {@link #writeBit(boolean, int)} LSB-first as little-endian.
     * <p>
     * The outgoing bit order is from low- to most-significant-bit, maintaining bit LSB-first order.
     * </p>
     * @param n number of bits, maximum 31 bits
     * @param bits the bits to write
     * @return the written bits or {@link #EOS}.
     * @throws IllegalStateException if not in output mode or stream closed
     * @throws IllegalArgumentException if n > 31
     * @throws IOException
     */
    public int writeBits31(final int n, final int bits) throws IllegalStateException, IllegalArgumentException, IOException {
        if( 31 < n ) {
            throw new IllegalArgumentException("n > 31: "+n);
        }
        if( !outputMode || null == bytes ) {
            throw new IllegalStateException("not in output-mode: "+this);
        }
        if( 0 < n ) {
            if( !useFastPathStream ) {
                // Slow path
                for(int i=0; i < n; i++) {
                    final int b = writeBit(false /* msbFirst */, ( bits >>> i ) & 0x1);
                    if( EOS == b ) {
                        return EOS;
                    }
                }
            } else {
                // fast path
                int c = n;
                final int n1 = Math.min(n, bitCount); // remaining portion
                if( 0 < n1 ) {
                    final int m1 = ( 1 << n1 ) - 1;
                    final int s1 = 7 - bitCount + 1; // LSBfirst: left-shift to free bit-pos
                    bitCount -= n1;
                    c -= n1;
                    // MSBfirst: bitBuffer |= ( m1 & ( bits >>> c ) ) << bitCount;
                    bitBuffer |= ( m1 & bits ) << s1 ; // LSBfirst
                    if( 0 == bitCount ) {
                        if( EOS == bytes.write((byte)bitBuffer) ) {
                            if( throwIOExceptionOnEOF ) {
                                throw new IOException("EOS "+this);
                            }
                            return EOS;
                        }
                    }
                    if( 0 == c ) {
                        return bits;
                    }
                }
                assert( 0 == bitCount );
                int s = n1; // LSBfirst: left shift for additional elements
                do {
                    final int n2 = Math.min(c, 8); // full portion
                    final int m2 = ( 1 << n2 ) - 1;
                    bitCount = 8 - n2;
                    c -= n2;
                    // MSBfirst: bitBuffer = ( m2 & ( bits >>> c ) ) << bitCount;
                    bitBuffer = ( m2 & ( bits >>> s ) ); // LSBfirst
                    s += n2;
                    if( 0 == bitCount ) {
                        if( EOS == bytes.write((byte)bitBuffer) ) {
                            if( throwIOExceptionOnEOF ) {
                                throw new IOException("EOS "+this);
                            }
                            return EOS;
                        }
                    }
                } while ( 0 < c );
            }
        }
        return bits;
    }

    /**
     * Return incoming <code>uint8_t</code> as read via {@link #readBits31(int)}.
     * <p>
     * In case of a <code>int8_t</code> 2-complement signed value, simply cast the result to <code>byte</code>
     * after checking for {@link #EOS}.
     * </p>
     * @return {@link #EOS} or the 8bit unsigned value within the lower bits.
     * @throws IllegalStateException if not in input mode or stream closed
     * @throws IOException
     */
    public final int readUInt8() throws IllegalStateException, IOException {
        if( 0 == bitCount && useFastPathTypes ) {
            // fast path
            if( outputMode || null == bytes ) {
                throw new IllegalStateException("not in input-mode: "+this);
            }
            final int r = bytes.read();
            if( throwIOExceptionOnEOF && EOS == r ) {
                throw new IOException("EOS "+this);
            }
            return r;
        } else {
            return readBits31(8);
        }
    }

    /**
     * Write the given 8 bits via {@link #writeBits31(int, int)}.
     * @return {@link #EOS} or the written 8bit value.
     * @throws IllegalStateException if not in output mode or stream closed
     * @throws IOException
     */
    public final int writeInt8(final byte int8) throws IllegalStateException, IOException {
        if( 0 == bitCount && useFastPathTypes  ) {
            // fast path
            if( !outputMode || null == bytes ) {
                throw new IllegalStateException("not in output-mode: "+this);
            }
            final int r = bytes.write(int8);
            if( throwIOExceptionOnEOF && EOS == r ) {
                throw new IOException("EOS "+this);
            }
            return r;
        } else {
            return this.writeBits31(8, int8);
        }
    }

    /**
     * Return incoming <code>uint16_t</code> as read via {@link #readBits31(int)} LSB-first as little-endian,
     * hence bytes are swapped if bigEndian.
     * <p>
     * In case of a <code>int16_t</code> 2-complement signed value, simply cast the result to <code>short</code>
     * after checking for {@link #EOS}.
     * </p>
     * @param bigEndian if true, swap incoming bytes to little-endian, otherwise leave them as little-endian.
     * @return {@link #EOS} or the 16bit unsigned value within the lower bits.
     * @throws IllegalStateException if not in input mode or stream closed
     * @throws IOException
     */
    public final int readUInt16(final boolean bigEndian) throws IllegalStateException, IOException {
        if( 0 == bitCount && useFastPathTypes ) {
            // fast path
            if( outputMode || null == bytes ) {
                throw new IllegalStateException("not in input-mode: "+this);
            }
            final int b1 = bytes.read();
            final int b2 = EOS != b1 ? bytes.read() : EOS;
            if( EOS == b2 ) {
                if( throwIOExceptionOnEOF ) {
                    throw new IOException("EOS "+this);
                }
                return EOS;
            } else if( bigEndian ) {
                return b1 << 8 | b2;
            } else {
                return b2 << 8 | b1;
            }
        } else {
            final int i16 = readBits31(16);
            if( EOS == i16 ) {
                return EOS;
            } else if( bigEndian ) {
                final int b1 = 0xff & ( i16 >>> 8 );
                final int b2 = 0xff &   i16;
                return b2 << 8 | b1;
            } else {
                return i16;
            }
        }
    }

    /**
     * Return incoming <code>uint16_t</code> value and swap bytes according to bigEndian.
     * <p>
     * In case of a <code>int16_t</code> 2-complement signed value, simply cast the result to <code>short</code>.
     * </p>
     * @param bigEndian if false, swap incoming bytes to little-endian, otherwise leave them as big-endian.
     * @return the 16bit unsigned value within the lower bits.
     * @throws IndexOutOfBoundsException
     */
    public static final int readUInt16(final boolean bigEndian, final byte[] bytes, final int offset) throws IndexOutOfBoundsException {
        checkBounds(bytes, offset, 2);
        // Make sure we clear any high bits that get set in sign-extension
        final int b1 = bytes[offset] & 0xff;
        final int b2 = bytes[offset+1] & 0xff;
        if( bigEndian ) {
            return b1 << 8 | b2;
        } else {
            return b2 << 8 | b1;
        }
    }

    /**
     * Write the given 16 bits via {@link #writeBits31(int, int)} LSB-first as little-endian,
     * hence bytes are swapped if bigEndian.
     * @param bigEndian if true, swap given bytes to little-endian, otherwise leave them as little-endian.
     * @return {@link #EOS} or the written 16bit value.
     * @throws IllegalStateException if not in output mode or stream closed
     * @throws IOException
     */
    public final int writeInt16(final boolean bigEndian, final short int16) throws IllegalStateException, IOException {
        if( 0 == bitCount && useFastPathTypes ) {
            // fast path
            if( !outputMode || null == bytes ) {
                throw new IllegalStateException("not in output-mode: "+this);
            }
            final byte hi = (byte) ( 0xff & ( int16 >>> 8 ) );
            final byte lo = (byte) ( 0xff &   int16         );
            final byte b1, b2;
            if( bigEndian ) {
                b1 = hi;
                b2 = lo;
            } else {
                b1 = lo;
                b2 = hi;
            }
            if( EOS != bytes.write(b1) ) {
                if( EOS != bytes.write(b2) ) {
                    return int16;
                }
            }
            if( throwIOExceptionOnEOF ) {
                throw new IOException("EOS "+this);
            }
            return EOS;
        } else if( bigEndian ) {
            final int b1 = 0xff & ( int16 >>> 8 );
            final int b2 = 0xff &   int16;
            return writeBits31(16, b2 << 8 | b1);
        } else {
            return writeBits31(16, int16);
        }
    }

    /**
     * Return incoming <code>uint32_t</code> as read via {@link #readBits31(int)} LSB-first as little-endian,
     * hence bytes are swapped if bigEndian.
     * <p>
     * In case of a <code>int32_t</code> 2-complement signed value, simply cast the result to <code>int</code>
     * after checking for {@link #EOS}.
     * </p>
     * @param bigEndian if true, swap incoming bytes to little-endian, otherwise leave them as little-endian.
     * @return {@link #EOS} or the 32bit unsigned value within the lower bits.
     * @throws IllegalStateException if not in input mode or stream closed
     * @throws IOException
     */
    public final long readUInt32(final boolean bigEndian) throws IllegalStateException, IOException {
        if( 0 == bitCount && useFastPathTypes ) {
            // fast path
            if( outputMode || null == bytes ) {
                throw new IllegalStateException("not in input-mode: "+this);
            }
            final int b1 = bytes.read();
            final int b2 = EOS != b1 ? bytes.read() : EOS;
            final int b3 = EOS != b2 ? bytes.read() : EOS;
            final int b4 = EOS != b3 ? bytes.read() : EOS;
            if( EOS == b4 ) {
                if( throwIOExceptionOnEOF ) {
                    throw new IOException("EOS "+this);
                }
                return EOS;
            } else if( bigEndian ) {
                return 0xffffffffL & ( b1 << 24 | b2 << 16 | b3 << 8 | b4 );
            } else {
                return 0xffffffffL & ( b4 << 24 | b3 << 16 | b2 << 8 | b1 );
            }
        } else {
            final int i16a = readBits31(16);
            final int i16b = EOS != i16a ? readBits31(16) : EOS;
            if( EOS == i16b ) {
                return EOS;
            } else if( bigEndian ) {
                final int b1 = 0xff & ( i16b >>> 8 );
                final int b2 = 0xff &   i16b;
                final int b3 = 0xff & ( i16a >>> 8 );
                final int b4 = 0xff &   i16a;
                return 0xffffffffL & ( b4 << 24 | b3 << 16 | b2 << 8 | b1 );
            } else {
                return 0xffffffffL & ( i16b << 16 | i16a );
            }
        }
    }

    /**
     * Return incoming <code>uint32_t</code> and swap bytes according to bigEndian.
     * <p>
     * In case of a <code>int32_t</code> 2-complement signed value, simply cast the result to <code>int</code>.
     * </p>
     * @param bigEndian if false, swap incoming bytes to little-endian, otherwise leave them as big-endian.
     * @return the 32bit unsigned value within the lower bits.
     * @throws IndexOutOfBoundsException
     */
    public static final long readUInt32(final boolean bigEndian, final byte[] bytes, final int offset) throws IndexOutOfBoundsException {
        checkBounds(bytes, offset, 4);
        final int b1 = bytes[offset];
        final int b2 = bytes[offset+1];
        final int b3 = bytes[offset+2];
        final int b4 = bytes[offset+3];
        if( bigEndian ) {
            return 0xffffffffL & ( b1 << 24 | b2 << 16 | b3 << 8 | b4 );
        } else {
            return 0xffffffffL & ( b4 << 24 | b3 << 16 | b2 << 8 | b1 );
        }
    }

    /**
     * Write the given 32 bits via {@link #writeBits31(int, int)} LSB-first as little-endian,
     * hence bytes are swapped if bigEndian.
     * @param bigEndian if true, swap given bytes to little-endian, otherwise leave them as little-endian.
     * @return {@link #EOS} or the written 32bit value.
     * @throws IllegalStateException if not in output mode or stream closed
     * @throws IOException
     */
    public final int writeInt32(final boolean bigEndian, final int int32) throws IllegalStateException, IOException {
        if( 0 == bitCount && useFastPathTypes ) {
            // fast path
            if( !outputMode || null == bytes ) {
                throw new IllegalStateException("not in output-mode: "+this);
            }
            final byte p1 = (byte) ( 0xff & ( int32 >>> 24 ) );
            final byte p2 = (byte) ( 0xff & ( int32 >>> 16 ) );
            final byte p3 = (byte) ( 0xff & ( int32 >>>  8 ) );
            final byte p4 = (byte) ( 0xff &   int32          );
            final byte b1, b2, b3, b4;
            if( bigEndian ) {
                b1 = p1;
                b2 = p2;
                b3 = p3;
                b4 = p4;
            } else {
                b1 = p4;
                b2 = p3;
                b3 = p2;
                b4 = p1;
            }
            if( EOS != bytes.write(b1) ) {
                if( EOS != bytes.write(b2) ) {
                    if( EOS != bytes.write(b3) ) {
                        if( EOS != bytes.write(b4) ) {
                            return int32;
                        }
                    }
                }
            }
            if( throwIOExceptionOnEOF ) {
                throw new IOException("EOS "+this);
            }
            return EOS;
        } else if( bigEndian ) {
            final int p1 = 0xff & ( int32 >>> 24 );
            final int p2 = 0xff & ( int32 >>> 16 );
            final int p3 = 0xff & ( int32 >>>  8 );
            final int p4 = 0xff &   int32         ;
            if( EOS != writeBits31(16, p2 << 8 | p1) ) {
                if( EOS != writeBits31(16, p4 << 8 | p3) ) {
                    return int32;
                }
            }
            return EOS;
        } else {
            final int hi = 0x0000ffff & ( int32 >>> 16 );
            final int lo = 0x0000ffff &   int32 ;
            if( EOS != writeBits31(16, lo) ) {
                if( EOS != writeBits31(16, hi) ) {
                    return int32;
                }
            }
            return EOS;
        }
    }

    /**
     * Reinterpret the given <code>int32_t</code> value as <code>uint32_t</code>,
     * i.e. perform the following cast to <code>long</code>:
     * <pre>
     *   final long l = 0xffffffffL & int32;
     * </pre>
     */
    public static final long toUInt32Long(final int int32) {
        return 0xffffffffL & int32;
    }

    /**
     * Returns the reinterpreted given <code>int32_t</code> value
     * as <code>uint32_t</code> if &le; {@link Integer#MAX_VALUE}
     * as within an <code>int</code> storage.
     * Otherwise return -1.
     */
    public static final int toUInt32Int(final int int32) {
        return uint32LongToInt(toUInt32Long(int32));
    }

    /**
     * Returns the given <code>uint32_t</code> value <code>long</code>
     * value as <code>int</code> if &le; {@link Integer#MAX_VALUE}.
     * Otherwise return -1.
     */
    public static final int uint32LongToInt(final long uint32) {
        if( Integer.MAX_VALUE >= uint32 ) {
            return (int)uint32;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return String.format("Bitstream[%s]", toStringImpl());
    }
    protected String toStringImpl() {
        final String mode;
        final long bpos;
        if( null == bytes ) {
            mode = "closed";
            bpos = -1;
        } else {
            mode = outputMode ? "output" : "input";
            bpos = bytes.position();
        }
        return String.format("%s, pos %d [byteP %d, bitCnt %d], bitbuf %s",
                mode, position(), bpos, bitCount, toHexBinString(true, bitBuffer, 8));
    }

    private static final String strZeroPadding= "0000000000000000000000000000000000000000000000000000000000000000"; // 64
    public static String toBinString(final boolean msbFirst, final int v, final int bitCount) {
        if( 0 == bitCount ) {
            return "";
        }
        if( msbFirst ) {
            final int mask = (int) ( ( 1L << bitCount ) - 1L );
            final String s0 = Integer.toBinaryString( mask & v );
            return strZeroPadding.substring(0, bitCount-s0.length())+s0;
        } else {
            final char[] c = new char[32];
            for(int i=0; i<bitCount; i++) {
                c[i] = 0 != ( v & ( 1 << i ) ) ? '1' : '0';
            }
            final String s0 = new String(c, 0, bitCount);
            return s0+strZeroPadding.substring(0, bitCount-s0.length());
        }
    }
    public static String toHexBinString(final boolean msbFirst, final int v, final int bitCount) {
        final int nibbles = 0 == bitCount ? 2 : ( bitCount + 3 ) / 4;
        return String.format("[0x%0"+nibbles+"X, msbFirst %b, %s]", v, msbFirst, toBinString(msbFirst, v, bitCount));
    }
    public static final String toHexBinString(final boolean msbFirst, final byte[] data, final int offset, final int len) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i=0; i<len; i++) {
            final int v = 0xFF & data[offset+i];
            sb.append(toHexBinString(msbFirst, v, 8)).append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
    public static final String toHexBinString(final boolean msbFirst, final ByteBuffer data, final int offset, final int len) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(int i=0; i<len; i++) {
            final int v = 0xFF & data.get(offset+i);
            sb.append(toHexBinString(msbFirst, v, 8)).append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public static void checkBounds(final byte[] sb, final int offset, final int remaining) throws IndexOutOfBoundsException {
        if( offset + remaining > sb.length ) {
            throw new IndexOutOfBoundsException("Buffer of size "+sb.length+" cannot hold offset "+offset+" + remaining "+remaining);
        }
    }
}
