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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;
import static com.jogamp.common.util.BitDemoData.*;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test {@link Bitstream} w/ raw linear and bulk read/write access w/o semantics:
 * <ul>
 *  <li>{@link Bitstream#readBit(boolean)}</li>
 *  <li>{@link Bitstream#writeBit(boolean, int)}</li>
 *  <li>{@link Bitstream#mark(int)}</li>
 *  <li>{@link Bitstream#reset()}</li>
 *  <li>{@link Bitstream#flush()}</li>
 *  <li>{@link Bitstream#readBits31(int)}</li>
 *  <li>{@link Bitstream#writeBits31(int, int)}</li>
 * </ul>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBitstream01 extends SingletonJunitCase {

    Bitstream<ByteBuffer> getTestStream(final boolean msbFirstData, final boolean msbFirstWrite,
                                        final int preBits, final int skipBits, final int postBits) throws IOException {
        final int bitCount = preBits+skipBits+postBits;
        final int byteCount = ( bitCount + 7 ) / 8;
        final ByteBuffer bbTest = ByteBuffer.allocate(byteCount);
        final Bitstream.ByteBufferStream bbsTest = new Bitstream.ByteBufferStream(bbTest);
        final Bitstream<ByteBuffer> bsTest = new Bitstream<ByteBuffer>(bbsTest, true /* outputMode */);
        final String sTest0;
        if( msbFirstData ) {
            sTest0 = testStringMSB.substring(0, preBits+skipBits+postBits);
        } else {
            sTest0 = testStringLSB.substring(0, preBits+skipBits+postBits);
        }
        if( msbFirstData == msbFirstWrite ) {
            for(int i=0; i<bitCount; i++) {
                final int bit = Integer.valueOf(sTest0.substring(i, i+1));
                bsTest.writeBit(msbFirstWrite, bit);
            }
        } else {
            for(int i=bitCount-1; i >= 0; i--) {
                final int bit = Integer.valueOf(sTest0.substring(i, i+1));
                bsTest.writeBit(msbFirstWrite, bit);
            }
        }
        System.err.printf("TestData: msbFirst[data %b, write %b], bits[pre %d, skip %d, post %d = %d]: <%s>%n",
                msbFirstData, msbFirstWrite, preBits, skipBits, postBits, bitCount, sTest0);
        Assert.assertEquals(preBits+skipBits+postBits, bsTest.position());
        bsTest.setStream(bsTest.getSubStream(), false /* outputMode */); // switch to input-mode, implies flush()
        dumpData("TestData: ", bsTest.getSubStream(), 0, bsTest.getSubStream().limit());
        return bsTest;
    }

    String getTestStreamResultAsString(final boolean msbFirstData, final boolean msbFirstAssemble,
                                       final int preBits, final int skipBits, final int postBits) {
        final String pre, post;
        if( msbFirstData ) {
            if( msbFirstAssemble ) {
                pre = testStringMSB.substring(0, preBits);
                post = testStringMSB.substring(preBits+skipBits, preBits+skipBits+postBits);
            } else {
                pre = testStringMSB.substring(postBits+skipBits, preBits+skipBits+postBits);
                post = testStringMSB.substring(0, postBits);
            }
        } else {
            if( msbFirstAssemble ) {
                pre = testStringLSB.substring(0, preBits);
                post = testStringLSB.substring(preBits+skipBits, preBits+skipBits+postBits);
            } else {
                pre = testStringMSB.substring(postBits+skipBits, preBits+skipBits+postBits);
                post = testStringMSB.substring(0, postBits);
            }
        }
        final String r = msbFirstAssemble ? pre + post : post + pre;
        System.err.println("ResultExp: <"+pre+"> + <"+post+"> = <"+r+">");
        return r;
    }

    @Test
    public void test01LinearBitsMSBFirst() throws IOException {
        testLinearBitsImpl(true /* msbFirst */);
    }
    @Test
    public void test02LinearBitsLSBFirst() throws IOException {
        testLinearBitsImpl(false /* msbFirst */);
    }
    void testLinearBitsImpl(final boolean msbFirst) throws IOException {
        testLinearBitsImpl(msbFirst,  0,  0,  1);
        testLinearBitsImpl(msbFirst,  0,  0,  3);
        testLinearBitsImpl(msbFirst,  0,  0,  8);
        testLinearBitsImpl(msbFirst,  0,  0, 10);
        testLinearBitsImpl(msbFirst,  0,  0, 30);
        testLinearBitsImpl(msbFirst,  0,  0, 32);

        testLinearBitsImpl(msbFirst,  3,  0,  3);
        testLinearBitsImpl(msbFirst,  8,  0,  3);
        testLinearBitsImpl(msbFirst,  9,  0,  3);

        testLinearBitsImpl(msbFirst,  0,  1,  1);
        testLinearBitsImpl(msbFirst,  0,  1,  3);
        testLinearBitsImpl(msbFirst,  0,  2,  8);
        testLinearBitsImpl(msbFirst,  0,  8, 10);
        testLinearBitsImpl(msbFirst,  0, 12, 20);
        testLinearBitsImpl(msbFirst,  0, 23,  9);

        testLinearBitsImpl(msbFirst,  1,  1,  1);
        testLinearBitsImpl(msbFirst,  2,  1,  3);
        testLinearBitsImpl(msbFirst,  7,  2,  8);
        testLinearBitsImpl(msbFirst,  8,  8,  8);
        testLinearBitsImpl(msbFirst, 15, 12,  5);
        testLinearBitsImpl(msbFirst, 16, 11,  5);
    }

    String readBits(final boolean msbFirst, final Bitstream<?> copy, final Bitstream<?> input, final int preCount, final int count) throws IOException {
        final StringBuilder sbRead = new StringBuilder();
        int i = 0;
        while( i < count ) {
            final int bit = input.readBit(msbFirst);
            if( Bitstream.EOS == bit ) {
                System.err.printf(" EOS");
                break;
            } else {
                sbRead.append( ( 0 != bit ) ? '1' : '0' );
                i++;
                Assert.assertEquals(i+preCount, input.position());
                if( null != copy ) {
                    copy.writeBit(msbFirst, bit);
                    Assert.assertEquals(i+preCount, copy.position());
                }
            }
        }
        Assert.assertEquals(i+preCount, input.position());
        if( null != copy ) {
            Assert.assertEquals(i+preCount, copy.position());
        }
        return sbRead.toString();
    }

    void testLinearBitsImpl(final boolean msbFirst, final int preBits, final int skipBits, final int postBits) throws IOException {
        final int totalBits = preBits+skipBits+postBits;
        System.err.println("XXX TestLinearBits: msbFirst "+msbFirst+", preBits "+preBits+", skipBits "+skipBits+", postBits "+postBits+", totalBits "+totalBits);

        // prepare bitstream
        System.err.println("Prepare bitstream");
        final Bitstream<ByteBuffer> bsTest = getTestStream(msbFirst, msbFirst, preBits, skipBits, postBits);
        final String sTest = getTestStreamResultAsString(msbFirst, true, preBits, skipBits, postBits);

        // init copy-bitstream
        final int byteCount = ( totalBits + 7 ) / 8;
        final ByteBuffer bbCopy = ByteBuffer.allocate(byteCount);
        final Bitstream.ByteBufferStream bbsCopy = new Bitstream.ByteBufferStream(bbCopy);
        final Bitstream<ByteBuffer> bsCopy = new Bitstream<ByteBuffer>(bbsCopy, true /* outputMode */);

        // read-bitstream .. and copy bits while reading
        System.err.println("Reading bitstream: "+bsTest);
        {
            final String sReadPre = readBits(msbFirst, bsCopy, bsTest, 0, preBits);
            {
                final int skippedBits = (int) bsTest.skip(skipBits);
                Assert.assertEquals(skipBits, skippedBits);
            }
            {
                final int skippedBits = (int) bsCopy.skip(skipBits);
                Assert.assertEquals(skipBits, skippedBits);
            }
            final String sReadPost = readBits(msbFirst, bsCopy, bsTest, preBits+skipBits, postBits);
            final String sRead = sReadPre + sReadPost;
            System.err.println("Read.Test: <"+sReadPre+"> + <"+sReadPost+"> = <"+sRead+">");
            Assert.assertEquals(sTest, sRead);
            Assert.assertEquals(totalBits, bsTest.position());
            Assert.assertEquals(totalBits, bsCopy.position());
        }

        // read copy ..
        bsCopy.setStream(bsCopy.getSubStream(), false /* outputMode */); // switch to input-mode, implies flush()
        dumpData("Copy", bbCopy, 0, bbCopy.limit());
        System.err.println("Reading copy-bitstream: "+bsCopy);
        bsCopy.mark(0); // mark at beginning
        Assert.assertEquals(0, bsCopy.position());
        {
            final String sReadPre1 = readBits(msbFirst, null, bsCopy, 0, preBits);
            {
                final int skippedBits = (int) bsCopy.skip(skipBits);
                Assert.assertEquals(skipBits, skippedBits);
            }
            final String sReadPost1 = readBits(msbFirst, null, bsCopy, preBits+skipBits, postBits);
            final String sRead1 = sReadPre1 + sReadPost1;
            System.err.println("Read.Copy.1: <"+sReadPre1+"> + <"+sReadPost1+"> = <"+sRead1+">");
            Assert.assertEquals(sTest, sRead1);

            bsCopy.reset();
            final String sReadPre2 = readBits(msbFirst, null, bsCopy, 0, preBits);
            Assert.assertEquals(sReadPre1, sReadPre2);
            {
                final int skippedBits = (int) bsCopy.skip(skipBits);
                Assert.assertEquals(skipBits, skippedBits);
            }
            final String sReadPost2 = readBits(msbFirst, null, bsCopy, preBits+skipBits, postBits);
            Assert.assertEquals(sReadPost1, sReadPost2);
            final String sRead2 = sReadPre2 + sReadPost2;
            System.err.println("Read.Copy.2: <"+sReadPre2+"> + <"+sReadPost2+"> = <"+sRead2+">");
            Assert.assertEquals(sTest, sRead2);
            Assert.assertEquals(totalBits, bsCopy.position());
        }
    }

    @Test
    public void test03BulkBits() throws IOException {
        testBulkBitsImpl(0,  0,  1);
        testBulkBitsImpl(0,  0,  3);
        testBulkBitsImpl(0,  0,  8);
        testBulkBitsImpl(0,  0,  10);
        testBulkBitsImpl(0,  0,  30);
        testBulkBitsImpl(0,  0,  31);

        testBulkBitsImpl(3,  0,  3);
        testBulkBitsImpl(8,  0,  3);
        testBulkBitsImpl(9,  0,  3);
        testBulkBitsImpl(5,  0,  6);
        testBulkBitsImpl(5,  0,  8);

        testBulkBitsImpl(0,  1,  1);
        testBulkBitsImpl(3,  6,  4);

        testBulkBitsImpl(0,  1,  3);
        testBulkBitsImpl(0,  2,  8);
        testBulkBitsImpl(0,  8,  10);
        testBulkBitsImpl(0,  12, 20);
        testBulkBitsImpl(0,  23, 9);
        testBulkBitsImpl(0,  1,  31);

        testBulkBitsImpl(1,  1,  1);
        testBulkBitsImpl(2,  1,  3);
        testBulkBitsImpl(7,  2,  8);
        testBulkBitsImpl(8,  8,  8);
        testBulkBitsImpl(15, 12, 5);
        testBulkBitsImpl(16, 11, 5);
        testBulkBitsImpl(5,  6,  5);
        testBulkBitsImpl(5,  6,  8);
    }

    void testBulkBitsImpl(final int preBits, final int skipBits, final int postBits) throws IOException {
        final int totalBits = preBits+skipBits+postBits;
        System.err.println("XXX TestBulkBits: preBits "+preBits+", skipBits "+skipBits+", postBits "+postBits+", totalBits "+totalBits);

        // prepare bitstream
        System.err.println("Prepare bitstream");
        final Bitstream<ByteBuffer> bsTest = getTestStream(true, false, preBits, skipBits, postBits);
        final String sTest = getTestStreamResultAsString(true, false, preBits, skipBits, postBits);

        // init copy-bitstream
        final int byteCount = ( totalBits + 7 ) / 8;
        final ByteBuffer bbCopy = ByteBuffer.allocate(byteCount);
        final Bitstream.ByteBufferStream bbsCopy = new Bitstream.ByteBufferStream(bbCopy);
        final Bitstream<ByteBuffer> bsCopy = new Bitstream<ByteBuffer>(bbsCopy, true /* outputMode */);

        // read-bitstream .. and copy bits while reading
        System.err.println("Reading bitstream: "+bsTest);
        {
            final int readBitsPre = bsTest.readBits31(preBits);
            Assert.assertEquals(readBitsPre, bsCopy.writeBits31(preBits, readBitsPre));

            final int skippedReadBits = (int) bsTest.skip(skipBits);
            final int skippedBitsCopy = (int) bsCopy.skip(skipBits);

            final int readBitsPost = bsTest.readBits31(postBits);
            Assert.assertEquals(readBitsPost, bsCopy.writeBits31(postBits, readBitsPost));
            final String sReadPreLo = toBinaryString(readBitsPre, preBits);
            final String sReadPostHi = toBinaryString(readBitsPost, postBits);
            final String sRead = sReadPostHi + sReadPreLo;
            System.err.println("Read.Test: <"+sReadPreLo+"> + <"+sReadPostHi+"> = <"+sRead+">");

            Assert.assertEquals(skipBits, skippedReadBits);
            Assert.assertEquals(sTest, sRead);
            Assert.assertEquals(totalBits, bsTest.position());
            Assert.assertEquals(skipBits, skippedBitsCopy);
        }

        // read copy ..
        bsCopy.setStream(bsCopy.getSubStream(), false /* outputMode */); // switch to input-mode, implies flush()
        dumpData("Copy", bbCopy, 0, bbCopy.limit());
        System.err.println("Reading copy-bitstream: "+bsCopy);
        Assert.assertEquals(0, bsCopy.position());
        {
            final int copyBitsPre = bsCopy.readBits31(preBits);

            final int skippedCopyBits = (int) bsCopy.skip(skipBits);

            final int copyBitsPost = bsCopy.readBits31(postBits);
            final String sCopyPreLo = toBinaryString(copyBitsPre, preBits);
            final String sCopyPostHi = toBinaryString(copyBitsPost, postBits);
            final String sCopy = sCopyPostHi + sCopyPreLo;
            System.err.println("Copy.Test: <"+sCopyPreLo+"> + <"+sCopyPostHi+"> = <"+sCopy+">");

            Assert.assertEquals(skipBits, skippedCopyBits);
            Assert.assertEquals(sTest, sCopy);
            Assert.assertEquals(totalBits, bsCopy.position());
        }
    }

    @Test
    public void test05ErrorHandling() throws IOException {
        // prepare bitstream
        final Bitstream<ByteBuffer> bsTest = getTestStream(false, false, 0, 0, 0);
        System.err.println("01a: "+bsTest);
        bsTest.close();
        System.err.println("01b: "+bsTest);

        try {
            bsTest.readBit(false);
        } catch (final Exception e) {
            Assert.assertNotNull(e);
        }
        try {
            bsTest.writeBit(false, 1);
        } catch (final Exception e) {
            Assert.assertNotNull(e);
        }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestBitstream01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
