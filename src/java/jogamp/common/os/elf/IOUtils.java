/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package jogamp.common.os.elf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class IOUtils {
    static final long MAX_INT_VALUE = ( (long) Integer.MAX_VALUE & 0xffffffffL ) ;

    static String toHexString(int i) { return "0x"+Integer.toHexString(i); }

    static String toHexString(long i) { return "0x"+Long.toHexString(i); }

    static int shortToInt(short s) {
        return (int)s & 0x0000ffff;
    }

    static int long2Int(final long v) {
        if( MAX_INT_VALUE < v ) {
            throw new IllegalArgumentException("Read uint32 value "+toHexString(v)+" > int32-max "+toHexString(MAX_INT_VALUE));
        }
        return (int)v;
    }

    static void checkBounds(final byte[] sb, final int offset, final int remaining) {
        if( offset + remaining > sb.length ) {
            throw new IndexOutOfBoundsException("Buffer of size "+sb.length+" cannot hold offset "+offset+" + remaining "+remaining);
        }
    }

    static void readBytes(final RandomAccessFile in, final byte[] out, final int offset, final int len)
            throws IOException, IllegalArgumentException
    {
        in.readFully(out, offset, len);
    }

    static void seek(final RandomAccessFile in, long newPos) throws IOException {
        in.seek(newPos);
    }

    static int readUInt32(final byte[] in, final int offset) {
        final int v = readInt32(in, offset);
        if( 0 > v ) {
            throw new IllegalArgumentException("Read uint32 value "+toHexString(v)+" > int32-max "+toHexString(MAX_INT_VALUE));
        }
        return v;
        /** Need to fix endian for below path ..
        checkBounds(in, offset, 4);
        final byte[] uint = new byte[] { 0, 0, 0, 0, in[offset+0],  in[offset+1],  in[offset+2],  in[offset+3] };
        final ByteBuffer b = ByteBuffer.wrap(uint, 0, 8).order(ByteOrder.nativeOrder());
        return b.asLongBuffer().get(0); */
    }

    static int readInt32(final byte[] in, final int offset) {
        checkBounds(in, offset, 4);
        final ByteBuffer b = ByteBuffer.wrap(in, offset, 4).order(ByteOrder.nativeOrder());
        return b.asIntBuffer().get(0);
    }

    /**
     * @param sb byte source buffer to parse
     * @param offset offset within byte source buffer to start parsing
     * @param remaining remaining numbers of bytes to parse beginning w/ <code>sb_off</code>,
     *                  which shall not exceed <code>sb.length - offset</code>.
     * @param offset_post optional integer array holding offset post parsing
     * @return the parsed string
     * @throws IndexOutOfBoundsException if <code>offset + remaining > sb.length</code>.
     */
    static String getString(final byte[] sb, final int offset, final int remaining, int[] offset_post) throws IndexOutOfBoundsException {
        checkBounds(sb, offset, remaining);
        int strlen = 0;
        for(; strlen < remaining && sb[strlen + offset] != 0; strlen++) { }
        final String s = 0 < strlen ? new String(sb, offset, strlen) : "" ;
        if( null != offset_post ) {
            offset_post[0] = offset + strlen + 1; // incl. EOS
        }
        return s;
    }

    /**
     * @param sb byte source buffer to parse
     * @param offset offset within byte source buffer to start parsing
     * @param remaining remaining numbers of bytes to parse beginning w/ <code>sb_off</code>,
     *                  which shall not exceed <code>sb.length - offset</code>.
     * @return the number of parsed strings
     * @throws IndexOutOfBoundsException if <code>offset + remaining > sb.length</code>.
     */
    static int getStringCount(final byte[] sb, int offset, final int remaining) throws IndexOutOfBoundsException {
        checkBounds(sb, offset, remaining);
        int strnum=0;
        for(int i=0; i < remaining; i++) {
            for(; i < remaining && sb[i + offset] != 0; i++) { }
            strnum++;
        }
        return strnum;
    }

    /**
     * @param sb byte source buffer to parse
     * @param offset offset within byte source buffer to start parsing
     * @param remaining remaining numbers of bytes to parse beginning w/ <code>sb_off</code>,
     *                  which shall not exceed <code>sb.length - offset</code>.
     * @return the parsed strings
     * @throws IndexOutOfBoundsException if <code>offset + remaining > sb.length</code>.
     */
    public static String[] getStrings(final byte[] sb, int offset, final int remaining) throws IndexOutOfBoundsException  {
        final int strnum = getStringCount(sb, offset, remaining);
        // System.err.println("XXX: strnum "+strnum+", sb_off "+sb_off+", sb_len "+sb_len);

        final String[] sa = new String[strnum];
        final int[] io_off = new int[] { offset };
        for(int i=0; i < strnum; i++) {
            // System.err.print("XXX: str["+i+"] ["+io_off[0]);
            sa[i] = getString(sb, io_off[0], remaining - io_off[0], io_off);
            // System.err.println(".. "+io_off[0]+"[ "+sa[i]);
        }
        return sa;
    }

}
