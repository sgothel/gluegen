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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CustomDeflate {
    public static void main(final String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java "+CustomDeflate.class.getName()+" file-in file-out");
        } else {
            final File fileIn = new File(args[0]);
            final File fileOut = new File(args[1]);
            final int inSize;
            {
                final long _inSize = fileIn.length();
                if( 0 >= _inSize || _inSize > Integer.MAX_VALUE ) {
                    throw new IllegalArgumentException("");
                }
                inSize = (int) _inSize;
            }
            final byte[] input = new byte[inSize];
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(fileIn);
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
                out = new FileOutputStream(fileOut);
                CustomCompress.deflateToStream(input, 0, inSize, 9, out);
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if( null != in ) {
                    try { in.close(); } catch (final IOException e) { }
                }
                if( null != out ) {
                    try { out.close(); } catch (final IOException e) { }
                }
            }

            //
            // Test
            //
            in = null;
            out = null;
            try {
                in = new FileInputStream(fileOut);
                final byte[] compare = CustomCompress.inflateFromStream(in);
                if( compare.length != inSize ) {
                    throw new InternalError("Inflated Size Mismatch: Has "+compare.length+", expected "+inSize);
                }
                for(int i=0; i<inSize; i++) {
                    if( input[i] != compare[i] ) {
                        throw new InternalError("Inflated Bytes Mismatch at "+i+"/"+inSize+": Has "+Integer.toHexString(compare[i])+", expected "+Integer.toHexString(input[i]));
                    }
                }
            } catch (final IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if( null != in ) {
                    try { in.close(); } catch (final IOException e) { }
                }
                if( null != out ) {
                    try { out.close(); } catch (final IOException e) { }
                }
            }
        }
    }

}
