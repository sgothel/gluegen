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

import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Testing {@link MappedByteBufferInputStream} and {@link MappedByteBufferOutputStream}
 * direct stream to stream copy via mapped buffers.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestByteBufferCopyStream extends SingletonJunitCase {

    static void testImpl(final String srcFileName, final long size,
                         final MappedByteBufferInputStream.CacheMode srcCacheMode, final int srcSliceShift,
                         final String dstFileName,
                         final MappedByteBufferInputStream.CacheMode dstCacheMode, final int dstSliceShift ) throws IOException {
        final Runtime runtime = Runtime.getRuntime();
        final long[] usedMem0 = { 0 };
        final long[] freeMem0 = { 0 };
        final long[] usedMem1 = { 0 };
        final long[] freeMem1 = { 0 };
        final String prefix = "test "+String.format(TestByteBufferInputStream.PrintPrecision+" MiB", size/TestByteBufferInputStream.MIB);
        TestByteBufferInputStream.dumpMem(prefix+" before", runtime, -1, -1, usedMem0, freeMem0 );

        final File srcFile = new File(srcFileName);
        srcFile.delete();
        srcFile.createNewFile();
        srcFile.deleteOnExit();

        final RandomAccessFile input;
        {
            final RandomAccessFile _input = new RandomAccessFile(srcFile, "rw");
            _input.setLength(size);
            _input.close();
            input = new RandomAccessFile(srcFile, "r");
        }
        final MappedByteBufferInputStream mis = new MappedByteBufferInputStream(input.getChannel(),
                                                                                FileChannel.MapMode.READ_ONLY,
                                                                                srcCacheMode,
                                                                                srcSliceShift);
        Assert.assertEquals(size, input.length());
        Assert.assertEquals(size, mis.length());
        Assert.assertEquals(0, mis.position());
        Assert.assertEquals(size, mis.remaining());

        final File dstFile = new File(dstFileName);
        dstFile.delete();
        dstFile.createNewFile();
        dstFile.deleteOnExit();
        final RandomAccessFile output = new RandomAccessFile(dstFile, "rw");
        final MappedByteBufferInputStream.FileResizeOp szOp = new MappedByteBufferInputStream.FileResizeOp() {
            @Override
            public void setLength(final long newSize) throws IOException {
                output.setLength(newSize);
            }
        };
        final MappedByteBufferOutputStream mos = new MappedByteBufferOutputStream(output.getChannel(),
                                                                                  FileChannel.MapMode.READ_WRITE,
                                                                                  dstCacheMode,
                                                                                  srcSliceShift, szOp);
        Assert.assertEquals(0, output.length());
        Assert.assertEquals(0, mos.length());
        Assert.assertEquals(0, mos.position());
        Assert.assertEquals(0, mos.remaining());

        OutOfMemoryError oome = null;
        IOException ioe = null;

        try {
            mos.write(mis, mis.remaining());

            Assert.assertEquals(size, input.length());
            Assert.assertEquals(size, output.length());
            Assert.assertEquals(size, mis.length());
            Assert.assertEquals(size, mos.length());
            Assert.assertEquals(size, mis.position());
            Assert.assertEquals(size, mos.position());
            Assert.assertEquals(0, mis.remaining());
            Assert.assertEquals(0, mos.remaining());

        } catch (final IOException e) {
            if( e.getCause() instanceof OutOfMemoryError ) {
                oome = (OutOfMemoryError) e.getCause(); // oops
            } else {
                ioe = e;
            }
        } catch (final OutOfMemoryError m) {
            oome = m; // oops
        } finally {
            mos.close();
            mis.close();
            input.close();
            output.close();
            srcFile.delete();
            dstFile.delete();
            TestByteBufferInputStream.dumpMem(prefix+" after ", runtime, usedMem0[0], freeMem0[0], usedMem1, freeMem1 );
            System.gc();
            try {
                Thread.sleep(500);
            } catch (final InterruptedException e) { }
            TestByteBufferInputStream.dumpMem(prefix+" gc'ed ", runtime, usedMem0[0], freeMem0[0], usedMem1, freeMem1 );
        }
        if( null != ioe || null != oome ) {
            if( null != oome ) {
                System.err.printf("%s: OutOfMemoryError.2 %s%n", prefix, oome.getMessage());
                oome.printStackTrace();
            } else {
                Assert.assertNull(ioe);
            }
        }
    }

    /** {@value} */
    static final long halfMiB = 1L << 19;
    /** {@value} */
    static final long quaterGiB = 1L << 28;
    /** {@value} */
    static final long quaterPlusGiB = quaterGiB + halfMiB;
    /** {@value} */
    static final long halfGiB = 1L << 29;
    /** {@value} */
    static final long halfPlusGiB = halfGiB + halfMiB;
    /** {@value} */
    static final long oneGiB = 1L << 30;
    /** {@value} */
    static final long onePlusGiB = oneGiB + halfMiB;
    /** {@value} */
    static final long twoGiB = ( 2L << 30 );
    /** {@value} */
    static final long twoPlusGiB = twoGiB + halfMiB;

    /** {@value} */
    static final long lala = ( 1L << 27 );

    @Test
    public void test00() throws IOException {
        final long size;
        if( !manualTest && Platform.OSType.MACOS == Platform.getOSType() ) {
            size = quaterGiB;
        } else {
            size = twoPlusGiB;
        }
        final int srcSliceShift = MappedByteBufferInputStream.DEFAULT_SLICE_SHIFT;
        final int dstSliceShift = MappedByteBufferInputStream.DEFAULT_SLICE_SHIFT;
        testImpl(getSimpleTestName(".")+"_In.bin", size, MappedByteBufferInputStream.CacheMode.FLUSH_PRE_HARD, srcSliceShift,
                getSimpleTestName(".")+"_Out.bin", MappedByteBufferInputStream.CacheMode.FLUSH_PRE_HARD, dstSliceShift );
    }

    @Test
    public void test01() throws IOException {
        final long size;
        if( !manualTest && Platform.OSType.MACOS == Platform.getOSType() ) {
            size = quaterGiB;
        } else {
            size = twoPlusGiB;
        }
        final int srcSliceShift = MappedByteBufferInputStream.DEFAULT_SLICE_SHIFT;
        final int dstSliceShift = MappedByteBufferInputStream.DEFAULT_SLICE_SHIFT;
        testImpl(getSimpleTestName(".")+"_In.bin", size, MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT, srcSliceShift,
                 getSimpleTestName(".")+"_Out.bin", MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT, dstSliceShift );
    }

    @Test
    public void test02() throws IOException {
        final long size;
        if( !manualTest && Platform.OSType.MACOS == Platform.getOSType() ) {
            size = quaterPlusGiB;
        } else {
            size = halfPlusGiB;
        }
        final int srcSliceShift = 27; // 125M bytes per slice
        final int dstSliceShift = 27; // 125M bytes per slice
        testImpl(getSimpleTestName(".")+"_In.bin", size, MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT, srcSliceShift,
                 getSimpleTestName(".")+"_Out.bin", MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT, dstSliceShift );
    }

    @Test
    public void test11() throws IOException {
        final int srcSliceShift = 26; //  64M bytes per slice
        final int dstSliceShift = 25; //  32M bytes per slice
        final long size = quaterPlusGiB;
        testImpl(getSimpleTestName(".")+"_In.bin", size, MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT, srcSliceShift,
                 getSimpleTestName(".")+"_Out.bin", MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT, dstSliceShift );
    }

    @Test
    public void test12() throws IOException {
        final int srcSliceShift = 25; //  32M bytes per slice
        final int dstSliceShift = 26; //  64M bytes per slice
        final long size = quaterPlusGiB;
        testImpl(getSimpleTestName(".")+"_In.bin", size, MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT, srcSliceShift,
                 getSimpleTestName(".")+"_Out.bin", MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT, dstSliceShift );
    }

    static boolean manualTest = false;

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-manual")) {
                manualTest = true;
            }
        }
        final String tstname = TestByteBufferCopyStream.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
