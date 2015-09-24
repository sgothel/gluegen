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
 * Stream format for deflated data:
 * <ul>
 *   <li>4 bytes magic 0xDEF1A7E0 (Big Endian)</li>
 *   <li>4 bytes integer deflated-size (Big Endian)</li>
 *   <li>4 bytes integer inflated-size (Big Endian)</li>
 *   <li>deflated bytes</li>
 * </ul>
 * </p>
 */
public class CustomCompress {
    public static final int MAGIC = 0xDEF1A7E0;

    public static byte[] inflateFromStream(final InputStream in) throws IOException {
        final int inSize;
        final int outSize;
        {
            final DataInputStream din = new DataInputStream(in);
            final int _magic = din.readInt();
            if( _magic != MAGIC ) {
                throw new IOException("wrong magic: "+Integer.toHexString(_magic)+", expected "+Integer.toHexString(MAGIC));
            }
            inSize = din.readInt();
            outSize = din.readInt();
        }
        if( 0 >= inSize ) {
            throw new IOException("Invalid deflated-size "+inSize);
        }
        if( 0 >= outSize ) {
            throw new IOException("Invalid inflated-size "+outSize);
        }
        final byte[] input = new byte[inSize];
        int numBytes = 0;
        try {
            while (true) {
                final int remBytes = inSize - numBytes;
                int count;
                if ( 0 >= remBytes || (count = in.read(input, numBytes, remBytes)) == -1 ) {
                    break;
                }
                numBytes += count;
            }
        } finally {
            in.close();
        }
        if( inSize != numBytes ) {
            throw new IOException("Got "+numBytes+" bytes != expected "+inSize);
        }
        final byte[] output = new byte[outSize];
        try {
            final Inflater inflater = new Inflater();
            inflater.setInput(input, 0, inSize);
            final int outSize2 = inflater.inflate(output, 0, outSize);
            inflater.end();
            if( outSize != outSize2 ) {
                throw new IOException("Got inflated "+outSize2+" bytes != expected "+outSize);
            }
        } catch(final DataFormatException dfe) {
            throw new IOException(dfe);
        }
        return output;
    }

    public static int deflateToStream(final byte[] input, final OutputStream out) throws IOException {
        final byte[] output = new byte[input.length];
        final Deflater deflater = new Deflater();
        deflater.setInput(input, 0, input.length);
        deflater.finish();
        final int outSize = deflater.deflate(output, 0, output.length);
        deflater.end();
        {
            final DataOutputStream dout = new DataOutputStream(out);
            dout.writeInt(CustomCompress.MAGIC);
            dout.writeInt(outSize);
            dout.writeInt(input.length);
        }
        out.write(output, 0, outSize);
        return outSize;
    }

}
