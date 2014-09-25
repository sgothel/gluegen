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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.util.IOUtil;
import com.jogamp.junit.util.JunitTracer;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Testing serial read of {@link ByteBufferInputStream} and {@link MappedByteBufferInputStream},
 * i.e. basic functionality only.
 * <p>
 * Focusing on comparison with {@link BufferedInputStream} regarding
 * performance, used memory heap and used virtual memory.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestByteBufferInputStream extends JunitTracer {
    /** {@value} */
    static final int buffer__8KiB = 1 << 13;

    /** {@value} */
    static final int halfMiB = 1 << 19;
    /** {@value} */
    static final int oneMiB = 1 << 20;
    /** {@value} */
    static final int tenMiB = 1 << 24;
    /** {@value} */
    static final int hunMiB = 1 << 27;
    /** {@value} */
    static final int halfGiB = 1 << 29;
    /** {@value} */
    static final int oneGiB = 1 << 30;
    /** {@value} */
    static final int twoGiB = Integer.MAX_VALUE;
    /** {@value} */
    static final long fourGiB = 4L << 30L;

    static final String fileHalfMiB = "./testHalfMiB.bin" ;
    static final String fileOneMiB = "./testOneMiB.bin" ;
    static final String fileTenMiB = "./testTenMiB.bin" ;
    static final String fileHunMiB = "./testHunMiB.bin" ;
    static final String fileHalfGiB = "./testHalfGiB.bin" ;
    static final String fileOneGiB = "./testOneGiB.bin" ;
    static final String fileTwoGiB = "./testTwoGiB.bin" ;
    static final String fileFourGiB = "./testFourGiB.bin" ;
    static final String fileOut = "./testOut.bin" ;

    static final String printPrecision = "%8.3f";
    static final double mib = 1024.0*1024.0;


    @BeforeClass
    public static void setup() throws IOException {
        final Runtime runtime = Runtime.getRuntime();
        System.err.printf("Total Memory : "+printPrecision+" MiB%n", runtime.totalMemory() / mib);
        System.err.printf("Max Memory   : "+printPrecision+" MiB%n", runtime.maxMemory() / mib);

        setup(fileHalfMiB, halfMiB);
        setup(fileOneMiB, oneMiB);
        setup(fileTenMiB, tenMiB);
        setup(fileHunMiB, hunMiB);
        setup(fileHalfGiB, halfGiB);
        setup(fileOneGiB, oneGiB);
        setup(fileTwoGiB, twoGiB);
        setup(fileFourGiB, fourGiB);
    }
    static void setup(final String fname, final long size) throws IOException {
        final File file = new File(fname);
        final RandomAccessFile out = new RandomAccessFile(file, "rws");
        out.setLength(size);
        out.close();
    }

    @AfterClass
    public static void cleanup() {
        cleanup(fileHalfMiB);
        cleanup(fileOneMiB);
        cleanup(fileTenMiB);
        cleanup(fileHunMiB);
        cleanup(fileHalfGiB);
        cleanup(fileOneGiB);
        cleanup(fileTwoGiB);
        cleanup(fileFourGiB);
        cleanup(fileOut);
    }
    static void cleanup(final String fname) {
        final File file = new File(fname);
        file.delete();
    }

    @Test
    public void test01MixedIntSize() throws IOException {
        testCopyIntSize1Impl(fileHalfMiB, halfMiB);

        testCopyIntSize1Impl(fileOneMiB, oneMiB);

        testCopyIntSize1Impl(fileTenMiB, tenMiB);

        testCopyIntSize1Impl(fileHunMiB, hunMiB);

        testCopyIntSize1Impl(fileHalfGiB, halfGiB);

        testCopyIntSize1Impl(fileOneGiB, oneGiB);

        // testCopyIntSize1Impl(fileTwoGiB, twoGiB);
    }

    static enum SrcType { COPY, MMAP1, MMAP2_NONE, MMAP2_SOFT, MMAP2_HARD };

    @Test
    public void test11MMapFlushNone() throws IOException {
        testCopyIntSize1Impl2(0, SrcType.MMAP2_NONE, 0, fileOneGiB, oneGiB);
        testCopyIntSize1Impl2(0, SrcType.MMAP2_NONE, 0, fileTwoGiB, twoGiB);
        testCopyIntSize1Impl2(0, SrcType.MMAP2_NONE, 0, fileFourGiB, fourGiB);
    }

    @Test
    public void test12MMapFlushSoft() throws IOException {
        testCopyIntSize1Impl2(0, SrcType.MMAP2_SOFT, 0, fileOneGiB, oneGiB);
        testCopyIntSize1Impl2(0, SrcType.MMAP2_SOFT, 0, fileTwoGiB, twoGiB);
        testCopyIntSize1Impl2(0, SrcType.MMAP2_SOFT, 0, fileFourGiB, fourGiB);
    }

    @Test
    public void test13MMapFlushHard() throws IOException {
        testCopyIntSize1Impl2(0, SrcType.MMAP2_HARD, 0, fileOneGiB, oneGiB);
        testCopyIntSize1Impl2(0, SrcType.MMAP2_HARD, 0, fileTwoGiB, twoGiB);
        testCopyIntSize1Impl2(0, SrcType.MMAP2_HARD, 0, fileFourGiB, fourGiB);
    }

    void testCopyIntSize1Impl(final String testFileName, final long expSize) throws IOException {
        testCopyIntSize1Impl(SrcType.COPY, buffer__8KiB, testFileName, expSize);
        testCopyIntSize1Impl(SrcType.COPY,       hunMiB, testFileName, expSize);
        testCopyIntSize1Impl(SrcType.MMAP1,           0, testFileName, expSize);
        testCopyIntSize1Impl(SrcType.MMAP2_SOFT,           0, testFileName, expSize);
        System.err.println();
    }
    void testCopyIntSize1Impl(final SrcType srcType, final int reqBufferSize, final String testFileName, final long expSize) throws IOException {
        if( testCopyIntSize1Impl2(0, srcType, reqBufferSize, testFileName, expSize) ) {
            if( testCopyIntSize1Impl2(1, srcType, reqBufferSize, testFileName, expSize) ) {
                // testCopyIntSize1Impl2(2, srcType, reqBufferSize, testFileName, expSize);
            }
        }
        System.err.println();
    }
    boolean testCopyIntSize1Impl2(final int iter, final SrcType srcType, final int reqBufferSize, final String testFileName, final long expSize) throws IOException {
        final int expSizeI = (int) ( expSize <= Integer.MAX_VALUE ? expSize : Integer.MAX_VALUE );
        final int bufferSize = reqBufferSize < expSizeI ? reqBufferSize : expSizeI;
        final File testFile = new File(testFileName);
        final long hasSize1 = testFile.length();
        final long t0 = System.currentTimeMillis();
        Assert.assertEquals(expSize, hasSize1);

        final Runtime runtime = Runtime.getRuntime();
        final long[] usedMem0 = { 0 };
        final long[] freeMem0 = { 0 };
        final long[] usedMem1 = { 0 };
        final long[] freeMem1 = { 0 };

        final String prefix = "test #"+iter+" "+String.format(printPrecision+" MiB", expSize/mib);
        System.err.printf("%s: mode %-5s, bufferSize %9d: BEGIN%n", prefix, srcType.toString(), bufferSize);
        dumpMem(prefix+" before", runtime, -1, -1, usedMem0, freeMem0 );

        final IOException[] ioe = { null };
        OutOfMemoryError oome = null;
        InputStream bis = null;
        FileInputStream fis = null;
        FileChannel fic = null;
        boolean isMappedByteBufferInputStream = false;
        try {
            fis = new FileInputStream(testFile);
            if( SrcType.COPY == srcType ) {
                if( hasSize1 > Integer.MAX_VALUE ) {
                    fis.close();
                    throw new IllegalArgumentException("file size > MAX_INT for "+srcType+": "+hasSize1+" of "+testFile);
                }
                bis = new BufferedInputStream(fis, bufferSize);
            } else if( SrcType.MMAP1 == srcType ) {
                if( hasSize1 > Integer.MAX_VALUE ) {
                    fis.close();
                    throw new IllegalArgumentException("file size > MAX_INT for "+srcType+": "+hasSize1+" of "+testFile);
                }
                fic = fis.getChannel();
                final MappedByteBuffer fmap = fic.map(FileChannel.MapMode.READ_ONLY, 0, hasSize1); // survives channel/stream closing until GC'ed!
                bis = new ByteBufferInputStream(fmap);
            } else {
                isMappedByteBufferInputStream = true;
                MappedByteBufferInputStream.CacheMode cmode;
                switch(srcType) {
                    case MMAP2_NONE: cmode = MappedByteBufferInputStream.CacheMode.FLUSH_NONE;
                                     break;
                    case MMAP2_SOFT: cmode = MappedByteBufferInputStream.CacheMode.FLUSH_PRE_SOFT;
                                     break;
                    case MMAP2_HARD: cmode = MappedByteBufferInputStream.CacheMode.FLUSH_PRE_HARD;
                                     break;
                    default:         fis.close();
                                     throw new InternalError("XX: "+srcType);
                }
                final MappedByteBufferInputStream mis = MappedByteBufferInputStream.create(fis.getChannel(), cmode);
                Assert.assertEquals(expSize, mis.remaining());
                Assert.assertEquals(expSize, mis.length());
                Assert.assertEquals(0, mis.position());
                bis = mis;
            }
        } catch (final IOException e) {
            ioe[0] = e;
        } catch (final OutOfMemoryError m) {
            oome = m; // oops :)
        }
        try {
            if( null != ioe[0] || null != oome ) {
                if( null != oome ) {
                    System.err.printf("%s: mode %-5s, bufferSize %9d: OutOfMemoryError %s%n",
                                      prefix, srcType.toString(), bufferSize, oome.getMessage());
                    return false;
                } else {
                    Assert.assertNull(ioe[0]);
                }
            }
            Assert.assertEquals(expSizeI, bis.available());

            final long t1 = System.currentTimeMillis();

            final File out = new File(fileOut);
            IOUtil.copyStream2File(bis, out, -1);
            final long t2 = System.currentTimeMillis();

            final String suffix;
            if( isMappedByteBufferInputStream ) {
                suffix = ", cacheMode "+((MappedByteBufferInputStream)bis).getCacheMode();
            } else {
                suffix = "";
            }
            System.err.printf("%s: mode %-5s, bufferSize %9d: total %5d, setup %5d, copy %5d ms%s%n",
                              prefix, srcType.toString(), bufferSize, t2-t0, t1-t0, t2-t1, suffix);

            Assert.assertEquals(expSize, out.length());
            out.delete();

            Assert.assertEquals(0, bis.available());
            if( isMappedByteBufferInputStream ) {
                final MappedByteBufferInputStream mis = (MappedByteBufferInputStream)bis;
                Assert.assertEquals(0, mis.remaining());
                Assert.assertEquals(expSize, mis.length());
                Assert.assertEquals(expSize, mis.position());
            }
            dumpMem(prefix+" after ", runtime, usedMem0[0], freeMem0[0], usedMem1, freeMem1 );
            System.gc();
            try {
                Thread.sleep(500);
            } catch (final InterruptedException e) { }
            dumpMem(prefix+" gc'ed ", runtime, usedMem0[0], freeMem0[0], usedMem1, freeMem1 );
        } finally {
            if( null != fic ) {
                fic.close();
            }
            if( null != fis ) {
                fis.close();
            }
            bis.close();
            System.err.printf("%s: mode %-5s, bufferSize %9d: END%n", prefix, srcType.toString(), bufferSize);
            System.err.println();
        }
        return true;
    }

    static void dumpMem(final String pre,
                        final Runtime runtime, final long usedMem0,
                        final long freeMem0, final long[] usedMemN,
                        final long[] freeMemN )
    {
        usedMemN[0] = runtime.totalMemory() - runtime.freeMemory();
        freeMemN[0] = runtime.freeMemory();

        System.err.printf("%s Used Memory  : "+printPrecision, pre, usedMemN[0] / mib);
        if( 0 < usedMem0 ) {
            System.err.printf(", delta "+printPrecision, (usedMemN[0]-usedMem0) / mib);
        }
        System.err.println(" MiB");
        /**
        System.err.printf("%s Free Memory  : "+printPrecision, pre, freeMemN[0] / mib);
        if( 0 < freeMem0 ) {
            System.err.printf(", delta "+printPrecision, (freeMemN[0]-freeMem0) / mib);
        }
        System.err.println(" MiB"); */
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestByteBufferInputStream.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
