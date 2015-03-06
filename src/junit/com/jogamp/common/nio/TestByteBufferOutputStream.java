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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Testing {@link MappedByteBufferInputStream} and {@link MappedByteBufferOutputStream} editing functionality.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestByteBufferOutputStream extends SingletonJunitCase {

    static void testImpl(final String fname,
                         final byte[] payLoad, final long payLoadOffset, final long postPayLoadFiller,
                         final byte[] endBytes,
                         final int sliceShift)
            throws IOException
    {
        testImpl(fname, payLoad, payLoadOffset, postPayLoadFiller, endBytes, sliceShift, false);
        testImpl(fname, payLoad, payLoadOffset, postPayLoadFiller, endBytes, sliceShift, true);
    }
    static void testImpl(final String fname,
                         final byte[] payLoad, final long payLoadOffset, final long postPayLoadFiller,
                         final byte[] endBytes,
                         final int sliceShift, final boolean synchronous)
            throws IOException
    {
        final File file = new File(fname);
        file.delete();
        file.createNewFile();
        file.deleteOnExit();

        try {
            final RandomAccessFile out = new RandomAccessFile(file, "rw");
            final MappedByteBufferInputStream.FileResizeOp szOp = new MappedByteBufferInputStream.FileResizeOp() {
                @Override
                public void setLength(final long newSize) throws IOException {
                    out.setLength(newSize);
                }
            };
            final MappedByteBufferInputStream mis = new MappedByteBufferInputStream(out.getChannel(),
                                                                                    FileChannel.MapMode.READ_WRITE,
                                                                                    MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT,
                                                                                    sliceShift);
            final MappedByteBufferOutputStream mos = mis.getOutputStream(szOp);
            mos.setSynchronous(synchronous);

            try {
                // resize to payLoad start and position to it
                mos.setLength(payLoadOffset);
                Assert.assertEquals(payLoadOffset, out.length());
                Assert.assertEquals(payLoadOffset, mos.length());
                Assert.assertEquals(0, mos.position()); // no change
                mos.position(payLoadOffset);
                Assert.assertEquals(payLoadOffset, mos.position());

                // mark, write-expand payLoad
                mis.mark(1);
                mos.write(payLoad);
                Assert.assertEquals(payLoadOffset+payLoad.length, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position());

                // expand + 1
                mos.setLength(payLoadOffset+payLoad.length+1);
                Assert.assertEquals(payLoadOffset+payLoad.length+1, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position()); // no change

                // expand up-to very end, ahead of write - position to endBytes start
                mos.setLength(payLoadOffset+payLoad.length+postPayLoadFiller+endBytes.length);
                Assert.assertEquals(payLoadOffset+payLoad.length+postPayLoadFiller+endBytes.length, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length+postPayLoadFiller+endBytes.length, mos.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position()); // no change
                mos.skip(postPayLoadFiller);
                Assert.assertEquals(payLoadOffset+payLoad.length+postPayLoadFiller, mos.position());

                // write endBytes (no resize)
                mos.write(endBytes);
                Assert.assertEquals(payLoadOffset+payLoad.length+postPayLoadFiller+endBytes.length, mos.position());

                // Reset to payLoad, read it and verify
                mis.reset();
                Assert.assertEquals(payLoadOffset, mos.position());
                Assert.assertEquals(payLoadOffset, mis.position());
                final byte[] tmp = new byte[payLoad.length];
                Assert.assertEquals(payLoad.length, mis.read(tmp));
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position());
                Assert.assertEquals(payLoadOffset+payLoad.length, mis.position());
                Assert.assertArrayEquals(payLoad, tmp);

                // Shrink to end of payLoad, mark, read >= 0, reset .. redo
                Assert.assertEquals(payLoadOffset+payLoad.length+postPayLoadFiller+endBytes.length, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length+postPayLoadFiller+endBytes.length, mos.length());
                mos.setLength(payLoadOffset+payLoad.length+1);
                Assert.assertEquals(payLoadOffset+payLoad.length+1, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position());
                mis.mark(1);
                Assert.assertTrue(mis.read()>=0);
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.position());
                mis.reset();
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position());
                Assert.assertTrue(mis.read()>=0);
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.position());

                // Shrink -1, read EOS
                mos.setLength(payLoadOffset+payLoad.length);
                Assert.assertEquals(payLoadOffset+payLoad.length, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position());
                Assert.assertEquals(-1, mis.read());

                // Expand + 1, mark, read >= 0, reset .. redo
                mos.setLength(payLoadOffset+payLoad.length+1);
                Assert.assertEquals(payLoadOffset+payLoad.length+1, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position());
                mis.mark(1);
                Assert.assertTrue(mis.read()>=0);
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.position());
                mis.reset();
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position());
                Assert.assertTrue(mis.read()>=0);
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.position());

                // Shrink -1, read EOS, write-expand, reset and verify
                mos.setLength(payLoadOffset+payLoad.length);
                Assert.assertEquals(payLoadOffset+payLoad.length, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.length());
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position());
                Assert.assertEquals(-1, mis.read());
                mos.write('Z'); // expand while writing ..
                Assert.assertEquals(payLoadOffset+payLoad.length+1, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.length());
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.position());
                mis.reset();
                Assert.assertEquals(payLoadOffset+payLoad.length, mos.position());
                Assert.assertEquals(payLoadOffset+payLoad.length, mis.position());
                Assert.assertEquals('Z', mis.read());
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mos.position());
                Assert.assertEquals(payLoadOffset+payLoad.length+1, mis.position());

                // Shrink -2, shall clear mark, test reset failure
                mos.setLength(payLoadOffset+payLoad.length-1);
                Assert.assertEquals(payLoadOffset+payLoad.length-1, out.length());
                Assert.assertEquals(payLoadOffset+payLoad.length-1, mos.length());
                Assert.assertEquals(payLoadOffset+payLoad.length-1, mos.position());
                try {
                    mis.reset();
                    Assert.assertTrue(false); // shall not reach
                } catch( final IOException ioe ) {
                    Assert.assertNotNull(ioe);
                }
                mis.mark(1);

                // ZERO file, test reset failure, read EOS, write-expand
                mos.setLength(0);
                Assert.assertEquals(0, out.length());
                Assert.assertEquals(0, mos.length());
                Assert.assertEquals(0, mos.position());
                try {
                    mis.reset();
                    Assert.assertTrue(false); // shall not reach
                } catch( final IOException ioe ) {
                    Assert.assertNotNull(ioe);
                }
                Assert.assertEquals(-1, mis.read());
                mos.write('Z'); // expand while writing ..
                Assert.assertEquals(1, out.length());
                Assert.assertEquals(1, mos.length());
                Assert.assertEquals(1, mos.position());
                mis.position(0);
                Assert.assertEquals(0, mos.position());
                Assert.assertEquals(0, mis.position());
                Assert.assertEquals('Z', mis.read());
            } finally {
                mos.close();
                mis.close();
                out.close();
            }
        } finally {
            file.delete();
        }
    }

    @Test
    public void test00() throws IOException {
        final int sliceShift = 13; // 8192 bytes per slice
        testImpl(getSimpleTestName(".")+".bin", "123456789AB".getBytes(), 0L, 0L, "EOF".getBytes(), sliceShift);
    }

    @Test
    public void test01() throws IOException {
        final int sliceShift = 13; // 8192 bytes per slice
        testImpl(getSimpleTestName(".")+".bin", "123456789AB".getBytes(), 9000L, 100L, "EOF".getBytes(), sliceShift);
    }

    @Test
    public void test02() throws IOException {
        final int sliceShift = 13; // 8192 bytes per slice
        testImpl(getSimpleTestName(".")+".bin", "123456789AB".getBytes(), 8189L, 9001L, "EOF".getBytes(), sliceShift);
    }

    @Test
    public void test03() throws IOException {
        final int sliceShift = 13; // 8192 bytes per slice
        testImpl(getSimpleTestName(".")+".bin", "123456789AB".getBytes(), 58189L, 109001L, "EOF".getBytes(), sliceShift);
    }

    @Test
    public void test10() throws IOException {
        final int sliceShift = 10; // 1024 bytes per slice
        final byte[] payLoad = new byte[4096];
        for(int i=0; i<payLoad.length; i++) {
            payLoad[i] = (byte)('A' + i%26);
        }
        testImpl(getSimpleTestName(".")+".bin", payLoad, 0L, 0L, "EOF".getBytes(), sliceShift);
    }

    @Test
    public void test11() throws IOException {
        final int sliceShift = 10; // 1024 bytes per slice
        final byte[] payLoad = new byte[4096];
        for(int i=0; i<payLoad.length; i++) {
            payLoad[i] = (byte)('A' + i%26);
        }
        testImpl(getSimpleTestName(".")+".bin", payLoad, 1030L, 99L, "EOF".getBytes(), sliceShift);
    }

    @Test
    public void test12() throws IOException {
        final int sliceShift = 10; // 1024 bytes per slice
        final byte[] payLoad = new byte[4096];
        for(int i=0; i<payLoad.length; i++) {
            payLoad[i] = (byte)('A' + i%26);
        }
        testImpl(getSimpleTestName(".")+".bin", payLoad, 1021L, 1301L, "EOF".getBytes(), sliceShift);
    }

    @Test
    public void test13() throws IOException {
        final int sliceShift = 10; // 1024 bytes per slice
        final byte[] payLoad = new byte[4096];
        for(int i=0; i<payLoad.length; i++) {
            payLoad[i] = (byte)('A' + i%26);
        }
        testImpl(getSimpleTestName(".")+".bin", payLoad, 3021L, 6301L, "EOF".getBytes(), sliceShift);
    }

    static boolean manualTest = false;

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-manual")) {
                manualTest = true;
            }
        }
        final String tstname = TestByteBufferOutputStream.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
