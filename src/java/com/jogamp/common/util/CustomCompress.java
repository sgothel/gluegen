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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * All in memory inflater / deflator for small chunks using streams
 * <p>
 * Stream header of deflated data:
 * <ul>
 *   <li>4 bytes magic 0xDEF1A7E0 (Big Endian)</li>
 *   <li>4 bytes integer deflated-size (Big Endian)</li>
 *   <li>4 bytes integer inflated-size (Big Endian)</li>
 *   <li>deflated bytes</li>
 * </ul>
 * </p>
 */
public class CustomCompress {
    /** Start of stream header for deflated data */
    public static final int MAGIC = 0xDEF1A7E0;

    /**
     *
     * @param in {@link InputStream} at start of stream header, i.e. position {@link #MAGIC}.
     * @return the inflated bytes from the stream
     * @throws IOException if an I/O or deflation exception occurs
     * @throws IllegalArgumentException if {@code inLen} &le; 0 or {@code outLen} &le; 0, as read from header
     */
    public static byte[] inflateFromStream(final InputStream in)
            throws IOException, ArrayIndexOutOfBoundsException, IllegalArgumentException
    {
        final int inLen;
        final int outLen;
        {
            final DataInputStream din = new DataInputStream(in);
            final int _magic = din.readInt();
            if( _magic != MAGIC ) {
                throw new IOException("wrong magic: "+Integer.toHexString(_magic)+", expected "+Integer.toHexString(MAGIC));
            }
            inLen = din.readInt();
            outLen = din.readInt();
        }
        return inflateFromStream(in, inLen, outLen, new byte[outLen], 0);
    }

    /**
     *
     * @param in {@link InputStream} at start of deflated bytes, i.e. after the stream header.
     * @param inLen number of deflated bytes in stream {@code in}
     * @param outLen number of inflated {@code output} bytes at {@code outOff}
     * @param output sink for deflated bytes
     * @param outOff offset to {@code output}
     * @return the inflated bytes from the stream, passing {@code output} for chaining
     * @throws IOException if an I/O or deflation exception occurs
     * @throws ArrayIndexOutOfBoundsException if {@code outOff} and {@code outLen} exceeds {@code output}
     * @throws IllegalArgumentException if {@code inLen} &le; 0 or {@code outLen} &le; 0
     */
    public static byte[] inflateFromStream(final InputStream in, final int inLen, final int outLen,
                                           final byte[] output, final int outOff)
                                                   throws IOException, ArrayIndexOutOfBoundsException, IllegalArgumentException
    {
        if (inLen <= 0 || outLen <= 0 ) {
            throw new IllegalArgumentException("Length[input "+inLen+", output "+outLen+"]");
        }
        if (outOff < 0 || output.length < outOff + outLen) {
            throw new ArrayIndexOutOfBoundsException("output.length "+output.length+", offset "+outOff+", length "+outLen);
        }
        final byte[] input = new byte[inLen];
        int numBytes = 0;
        try {
            while (true) {
                final int remBytes = inLen - numBytes;
                int count;
                if ( 0 >= remBytes || (count = in.read(input, numBytes, remBytes)) == -1 ) {
                    break;
                }
                numBytes += count;
            }
        } finally {
            in.close();
        }
        if( inLen != numBytes ) {
            throw new IOException("Got "+numBytes+" bytes != expected "+inLen);
        }
        try {
            final Inflater inflater = new Inflater();
            inflater.setInput(input, 0, inLen);
            final int outSize = inflater.inflate(output, outOff, outLen);
            inflater.end();
            if( outLen != outSize ) {
                throw new IOException("Got inflated "+outSize+" bytes != expected "+outLen);
            }
        } catch(final DataFormatException dfe) {
            throw new IOException(dfe);
        }
        return output;
    }

    /**
     * @param input raw input bytes
     * @param inOff offset to {@code input}
     * @param inLen number of {@code input} bytes at {@code inOff}
     * @param level compression level 0-9 or {@link Deflater#DEFAULT_COMPRESSION}
     * @param out sink for deflated bytes
     * @return number of deflated bytes written, not including the header.
     * @throws IOException if an I/O or deflation exception occurs
     * @throws ArrayIndexOutOfBoundsException if {@code inOff} and {@code inLen} exceeds {@code input}
     * @throws IllegalArgumentException if {@code inLen} &le; 0
     */
    public static int deflateToStream(final byte[] input, final int inOff, final int inLen,
                                      final int level, final OutputStream out) throws IOException, ArrayIndexOutOfBoundsException, IllegalArgumentException {
        if (inLen <= 0 ) {
            throw new IllegalArgumentException("Length[input "+inLen+"]");
        }
        if (inOff < 0 || input.length < inOff + inLen) {
            throw new ArrayIndexOutOfBoundsException("input.length "+input.length+", offset "+inOff+", length "+inLen);
        }
        final byte[] output = new byte[inLen];
        final Deflater deflater = new Deflater(level);
        deflater.setInput(input, inOff, inLen);
        deflater.finish();
        final int outSize = deflater.deflate(output, 0, inLen);
        deflater.end();
        {
            final DataOutputStream dout = new DataOutputStream(out);
            dout.writeInt(CustomCompress.MAGIC);
            dout.writeInt(outSize);
            dout.writeInt(inLen);
        }
        out.write(output, 0, outSize);
        return outSize;
    }

}
