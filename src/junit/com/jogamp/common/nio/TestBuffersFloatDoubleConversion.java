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

package com.jogamp.common.nio;

import java.io.IOException;
import org.junit.Assert;

import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBuffersFloatDoubleConversion extends SingletonJunitCase {

    public static boolean cmpFloatArray(final float[] d1, final int d1_offset, final float[] d2, final int d2_offset, final int len) {
        if( d1.length - d1_offset < len) {
            throw new RuntimeException("d1 too small len "+len+" > "+d1.length+" - "+d1_offset);
        }
        if( d2.length - d2_offset < len) {
            throw new RuntimeException("d2 too small len "+len+" > "+d2.length+" - "+d2_offset);
        }
        boolean ok = true;
        for(int i=0; ok && i<len; i++) {
            ok = d1[d1_offset+i] == d2[d2_offset+i] ;
        }
        return ok;
    }

    public static boolean cmpDoubleArray(final double[] d1, final int d1_offset, final double[] d2, final int d2_offset, final int len) {
        if( d1.length - d1_offset < len) {
            throw new RuntimeException("d1 too small len "+len+" > "+d1.length+" - "+d1_offset);
        }
        if( d2.length - d2_offset < len) {
            throw new RuntimeException("d2 too small len "+len+" > "+d2.length+" - "+d2_offset);
        }
        boolean ok = true;
        for(int i=0; ok && i<len; i++) {
            ok = d1[d1_offset+i] == d2[d2_offset+i] ;
        }
        return ok;
    }

    public static void incrFloatArray(final float[] data, final int offset, final int len) {
        if( data.length - offset < len) {
            throw new RuntimeException("data too small len "+len+" > "+data.length+" - "+offset);
        }
        for(int i=0; i<len; i++) {
            data[offset+i] += 1;
        }
    }

    public static void incrDoubleArray(final double[] data, final int offset, final int len) {
        if( data.length - offset < len) {
            throw new RuntimeException("data too small len "+len+" > "+data.length+" - "+offset);
        }
        for(int i=0; i<len; i++) {
            data[offset+i] += 1;
        }
    }

    public static void setFloatArray(final float[] data, final int offset, final int len) {
        if( data.length - offset < len) {
            throw new RuntimeException("data too small len "+len+" > "+data.length+" - "+offset);
        }
        for(int i=0; i<len; i++) {
            data[offset+i] = i;
        }
    }

    public static void setDoubleArray(final double[] data, final int offset, final int len) {
        if( data.length - offset < len) {
            throw new RuntimeException("data too small len "+len+" > "+data.length+" - "+offset);
        }
        for(int i=0; i<len; i++) {
            data[offset+i] = i;
        }
    }

    public static void doItDoubleArray01(final double[] data, final int offset, final int len) {
        final float[] f_data = Buffers.getFloatArray(data, offset, null, 0, len);
        incrFloatArray(f_data, 0, len);
        Buffers.getDoubleArray(f_data, 0, data, offset, len);
    }

    @Test
    public void testDoubleArray2FloatArrayAndBack01() {
        final int offset = 50;
        final int len = 20;

        // reference 1
        final float[] fa_ref = new float[100];
        setFloatArray(fa_ref, offset, len);
        incrFloatArray(fa_ref, offset, len);

        // reference 2
        final double[] da_ref = new double[100];
        setDoubleArray(da_ref, offset, len);
        incrDoubleArray(da_ref, offset, len);

        // test 1: forth and back .. double -> float -> double
        {
            final double[] da1 = new double[100];
            setDoubleArray(da1, offset, len);
            incrDoubleArray(da1, offset, len);

            // conv_forth: double[offset..len] -> float[0..len]
            final float[] f_da1 = Buffers.getFloatArray(da1, offset, null, 0, len);
            Assert.assertTrue(cmpFloatArray(fa_ref, offset, f_da1, 0, len));

            // conv_back: float[0..len] -> double[offset..len]
            Buffers.getDoubleArray(f_da1, 0, da1, offset, len);
            Assert.assertTrue(cmpDoubleArray(da_ref, offset, da1, offset, len));
        }

        // test 2: forth, incr, back .. double -> float -> incr -> double
        {
            final double[] da1 = new double[100];
            setDoubleArray(da1, offset, len);

            doItDoubleArray01(da1, offset, len);
            Assert.assertTrue(cmpDoubleArray(da_ref, offset, da1, offset, len));
        }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestBuffersFloatDoubleConversion.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
