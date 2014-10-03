/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
import java.nio./*value2*/FloatBuffer/*value2*/;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class /*testname*/TestFloatStack01/*testname*/ extends SingletonJunitCase {

    static final boolean equals(final /*value*/float/*value*/[] b, final int bOffset,
                                final /*value*/float/*value*/[] stack, final int stackOffset, final int length) {
        for(int i=0; i<length; i++) {
            if( b[bOffset+i] != stack[stackOffset+i]) {
                return false;
            }
        }
        return true;
    }


    @Test
    public void test01PrimitiveArray_I32_G02() {
        final int initialSizeElem = 32;
        final int growSizeElem = 2;
        testPrimitiveArrayImpl(initialSizeElem, growSizeElem);
    }

    @Test
    public void test02PrimitiveArray_I00_G32() {
        final int initialSizeElem = 0;
        final int growSizeElem = 32;
        testPrimitiveArrayImpl(initialSizeElem, growSizeElem);
    }

    static private final boolean VERBOSE = false;

    private void testPrimitiveArrayImpl(final int initialSizeElem, final int growSizeElem) {
        final int compNum  = 3;
        final /*value*/float/*value*/[] e0 =
                new /*value*/float/*value*/[] { 0, 1, 2 };
        final /*value*/float/*value*/[] e1 =
                new /*value*/float/*value*/[] { 3, 4, 5 };

        final int totalSizeElem = initialSizeElem+2*growSizeElem;

        final int initialSizeComp = initialSizeElem*compNum;
        final int growSizeComp = growSizeElem*compNum;
        final int totalSizeComp = totalSizeElem*compNum;

        final /*name*/FloatStack/*name*/ fs0 =
                new /*name*/FloatStack/*name*/(initialSizeComp, growSizeComp);

        //
        // PUT
        //
        if(VERBOSE) {
            System.err.println("0: "+fs0);
        }
        for(int i=0; i<totalSizeElem; i++) {
            if(i < initialSizeElem) {
                Assert.assertTrue("Error #"+i+", "+fs0, fs0.remaining() == (initialSizeElem-i)*compNum);
            } else {
                final int j = ( i - initialSizeElem ) % growSizeElem ;
                final int k = ( 0 < j && j < growSizeElem ) ? growSizeElem - j : 0;
                Assert.assertTrue("Error #"+i+"("+j+", "+k+"), "+fs0, fs0.remaining() == k*compNum);
            }
            Assert.assertTrue("Error "+fs0, fs0.position() == i*compNum);

            String s;
            if( 0 == i % 2) {
                if(VERBOSE) {
                    s = Arrays.toString(e0);
                }
                fs0.putOnTop(e0, 0, compNum);
            } else {
                if(VERBOSE) {
                    s = Arrays.toString(e1);
                }
                fs0.putOnTop(e1, 0, compNum);
            }
            if(VERBOSE) {
                System.err.println("#"+i+"/"+totalSizeElem+": "+fs0+" <- "+s);
            }
        }
        if(VERBOSE) {
            System.err.println("X: "+fs0);
        }
        Assert.assertTrue("Error "+fs0, fs0.remaining() == 0);
        Assert.assertTrue("Error "+fs0, fs0.position() == totalSizeComp);

        fs0.setGrowSize(0);
        {
            Exception expectedException = null;
            try {
                fs0.putOnTop(e1, 0, compNum);
            } catch (final Exception e) {
                expectedException = e;
            }
            if(null == expectedException || !(expectedException instanceof IndexOutOfBoundsException) ) {
                Assert.assertTrue("Error "+fs0+", exception "+expectedException, false);
            }
        }

        //
        // GET
        //

        for(int i=0; i<totalSizeElem; i++) {
            Assert.assertTrue("Error "+fs0, fs0.remaining() == i*compNum);
            Assert.assertTrue("Error "+fs0, fs0.position() == (totalSizeElem-i)*compNum);

            final /*value*/float/*value*/[] buf =
                        new /*value*/float/*value*/[compNum];
            fs0.getFromTop(buf, 0, compNum);
            if( 0 == i % 2) {
                Assert.assertTrue("Error "+fs0+", #"+i+": "+Arrays.toString(e1)+" != "+Arrays.toString(buf), Arrays.equals(e1, buf));
            } else {
                Assert.assertTrue("Error "+fs0+", #"+i+": "+Arrays.toString(e0)+" != "+Arrays.toString(buf), Arrays.equals(e0, buf));
            }
        }
        Assert.assertTrue("Error "+fs0, fs0.remaining() == totalSizeComp);
        Assert.assertTrue("Error "+fs0, fs0.position() == 0);

        {
            final /*value*/float/*value*/[] buf =
                                new /*value*/float/*value*/[compNum];
            Exception expectedException = null;
            try {
                fs0.getFromTop(buf, 0, compNum);
            } catch (final Exception e) {
                expectedException = e;
            }
            if(null == expectedException || !(expectedException instanceof IndexOutOfBoundsException) ) {
                Assert.assertTrue("Error "+fs0+", exception "+expectedException, false);
            }
        }
    }

    @Test
    public void test11FloatBuffer_I32_G02() {
        final int initialSizeElem = 32;
        final int growSizeElem = 2;
        testFloatBufferImpl(initialSizeElem, growSizeElem);
    }

    @Test
    public void test12FloatBuffer_I00_G32() {
        final int initialSizeElem = 0;
        final int growSizeElem = 32;
        testFloatBufferImpl(initialSizeElem, growSizeElem);
    }

    private void testFloatBufferImpl(final int initialSizeElem, final int growSizeElem) {
        final int compNum  = 3;
        final /*value2*/FloatBuffer/*value2*/ fb0 =
                /*value2*/FloatBuffer/*value2*/.allocate(3*compNum);

        final /*value*/float/*value*/[] e0 =
                new /*value*/float/*value*/[] { 0, 1, 2 };
        final /*value*/float/*value*/[] e1 =
                new /*value*/float/*value*/[] { 3, 4, 5 };
        final /*value*/float/*value*/[] e2 =
                new /*value*/float/*value*/[] { 6, 7, 8 }; // not put on stack!
        fb0.put(e0);
        fb0.put(e1);
        fb0.put(e2);
        fb0.position(0);

        final int totalSizeElem = initialSizeElem+2*growSizeElem;

        final int initialSizeComp = initialSizeElem*compNum;
        final int growSizeComp = growSizeElem*compNum;
        final int totalSizeComp = totalSizeElem*compNum;

        final /*name*/FloatStack/*name*/ fs0 =
                new /*name*/FloatStack/*name*/(initialSizeComp, growSizeComp);

        //
        // PUT
        //

        for(int i=0; i<totalSizeElem; i++) {
            if( 0 == i ) {
                Assert.assertTrue("Error #"+i+", "+fs0+", "+fb0, fb0.position() == 0);
            } else if( 0 == i % 2) {
                Assert.assertTrue("Error #"+i+", "+fs0+", "+fb0, fb0.position() == 2*compNum);
                fb0.position(0);
            } else {
                Assert.assertTrue("Error #"+i+", "+fs0+", "+fb0, fb0.position() == compNum);
            }
            if(i < initialSizeElem) {
                Assert.assertTrue("Error #"+i+", "+fs0, fs0.remaining() == (initialSizeElem-i)*compNum);
            } else {
                final int j = ( i - initialSizeElem ) % growSizeElem ;
                final int k = ( 0 < j && j < growSizeElem ) ? growSizeElem - j : 0;
                Assert.assertTrue("Error #"+i+"("+j+", "+k+"), "+fs0, fs0.remaining() == k*compNum);
            }
            Assert.assertTrue("Error "+fs0, fs0.position() == i*compNum);

            final int fb0Pos0 = fb0.position();
            fs0.putOnTop(fb0, compNum);
            Assert.assertTrue("Error "+fs0+", "+fb0, fb0.position() == fb0Pos0 + compNum);
        }
        Assert.assertTrue("Error "+fs0, fs0.remaining() == 0);
        Assert.assertTrue("Error "+fs0, fs0.position() == totalSizeComp);

        fs0.setGrowSize(0);
        {
            fb0.position(0);
            Exception expectedException = null;
            try {
                fs0.putOnTop(fb0, compNum);
            } catch (final Exception e) {
                expectedException = e;
            }
            if(null == expectedException || !(expectedException instanceof IndexOutOfBoundsException) ) {
                Assert.assertTrue("Error "+fs0+", exception "+expectedException, false);
            }
            fb0.position(0);
        }

        //
        // GET
        //

        for(int i=0; i<totalSizeElem; i++) {
            Assert.assertTrue("Error "+fs0, fs0.remaining() == i*compNum);
            Assert.assertTrue("Error "+fs0, fs0.position() == (totalSizeElem-i)*compNum);

            final /*value*/float/*value*/[] backing =
                    new /*value*/float/*value*/[compNum];
            final /*value2*/FloatBuffer/*value2*/ buf =
                    /*value2*/FloatBuffer/*value2*/.wrap(backing);

            fs0.getFromTop(buf, compNum);
            if( 0 == i % 2) {
                Assert.assertTrue("Error "+fs0+", #"+i+": "+Arrays.toString(e1)+" != "+Arrays.toString(backing), Arrays.equals(e1, backing));
            } else {
                Assert.assertTrue("Error "+fs0+", #"+i+": "+Arrays.toString(e0)+" != "+Arrays.toString(backing), Arrays.equals(e0, backing));
            }
            Assert.assertTrue("Error "+fs0+", "+buf, buf.position() == compNum);
            buf.position(0);
        }
        Assert.assertTrue("Error "+fs0, fs0.remaining() == totalSizeComp);
        Assert.assertTrue("Error "+fs0, fs0.position() == 0);

        {
            final /*value*/float/*value*/[] backing =
                    new /*value*/float/*value*/[compNum];
            final /*value2*/FloatBuffer/*value2*/ buf
                    = /*value2*/FloatBuffer/*value2*/.wrap(backing);
            Exception expectedException = null;
            try {
                fs0.getFromTop(buf, compNum);
            } catch (final Exception e) {
                expectedException = e;
            }
            if(null == expectedException || !(expectedException instanceof IndexOutOfBoundsException) ) {
                Assert.assertTrue("Error "+fs0+", exception "+expectedException, false);
            }
        }

    }

    public static void main(final String args[]) throws IOException {
        final String tstname = /*testname*/TestFloatStack01/*testname*/.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
