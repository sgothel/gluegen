/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import org.junit.Assert;

import org.junit.Test;

import static com.jogamp.common.util.ValueConv.*;

/**
 * Testing ValueConv's value conversion of primitive types
 */
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestValueConversion {

    @Test
    public void testBaseFloat() {
        Assert.assertEquals(Byte.MAX_VALUE, float_to_byte( 1.0f, true));
        Assert.assertEquals(Byte.MIN_VALUE, float_to_byte(-1.0f, true));
        Assert.assertEquals( 1.0f, byte_to_float( Byte.MAX_VALUE, true), 0.0);
        Assert.assertEquals(-1.0f, byte_to_float( Byte.MIN_VALUE, true), 0.0);

        Assert.assertEquals(Short.MAX_VALUE, float_to_short( 1.0f, true));
        Assert.assertEquals(Short.MIN_VALUE, float_to_short(-1.0f, true));
        Assert.assertEquals( 1.0f, short_to_float( Short.MAX_VALUE, true), 0.0);
        Assert.assertEquals(-1.0f, short_to_float( Short.MIN_VALUE, true), 0.0);

        Assert.assertEquals(Integer.MAX_VALUE, float_to_int( 1.0f, true));
        Assert.assertEquals(Integer.MIN_VALUE, float_to_int(-1.0f, true));
        Assert.assertEquals( 1.0f, int_to_float( Integer.MAX_VALUE, true), 0.0);
        Assert.assertEquals(-1.0f, int_to_float( Integer.MIN_VALUE, true), 0.0);

        Assert.assertEquals((byte)0xff, float_to_byte( 1.0f, false));
        Assert.assertEquals( 1.0f, byte_to_float( (byte)0xff, false), 0.0);

        Assert.assertEquals((short)0xffff, float_to_short( 1.0f, false));
        Assert.assertEquals( 1.0f, short_to_float( (short)0xffff, false), 0.0);

        Assert.assertEquals(0xffffffff, float_to_int( 1.0f, false));
        Assert.assertEquals( 1.0f, int_to_float( 0xffffffff, false), 0.0);
    }

    @Test
    public void testBaseDouble() {
        Assert.assertEquals(Byte.MAX_VALUE, double_to_byte( 1.0, true));
        Assert.assertEquals(Byte.MIN_VALUE, double_to_byte(-1.0, true));
        Assert.assertEquals( 1.0, byte_to_double( Byte.MAX_VALUE, true), 0.0);
        Assert.assertEquals(-1.0, byte_to_double( Byte.MIN_VALUE, true), 0.0);

        Assert.assertEquals(Short.MAX_VALUE, double_to_short( 1.0, true));
        Assert.assertEquals(Short.MIN_VALUE, double_to_short(-1.0, true));
        Assert.assertEquals( 1.0, short_to_double( Short.MAX_VALUE, true), 0.0);
        Assert.assertEquals(-1.0, short_to_double( Short.MIN_VALUE, true), 0.0);

        Assert.assertEquals(Integer.MAX_VALUE, double_to_int( 1.0, true));
        Assert.assertEquals(Integer.MIN_VALUE, double_to_int(-1.0, true));
        Assert.assertEquals( 1.0, int_to_double( Integer.MAX_VALUE, true), 0.0);
        Assert.assertEquals(-1.0, int_to_double( Integer.MIN_VALUE, true), 0.0);

        Assert.assertEquals((byte)0xff, double_to_byte( 1.0, false));
        Assert.assertEquals( 1.0, byte_to_double( (byte)0xff, false), 0.0);

        Assert.assertEquals((short)0xffff, double_to_short( 1.0, false));
        Assert.assertEquals( 1.0, short_to_double( (short)0xffff, false), 0.0);

        Assert.assertEquals(0xffffffff, double_to_int( 1.0, false));
        Assert.assertEquals( 1.0, int_to_double( 0xffffffff, false), 0.0);
    }

    @Test
    public void testConversion() {
        final byte sb0 = 127;
        final byte sb1 = -128;

        final float sf0 = byte_to_float(sb0, true);
        final float sf1 = byte_to_float(sb1, true);
        final short ss0 = byte_to_short(sb0, true, true);
        final short ss1 = byte_to_short(sb1, true, true);
        final int si0 = byte_to_int(sb0, true, true);
        final int si1 = byte_to_int(sb1, true, true);

        Assert.assertEquals(1.0f, sf0, 0.0);
        Assert.assertEquals(-1.0f, sf1, 0.0);
        Assert.assertEquals(Short.MAX_VALUE, ss0);
        Assert.assertEquals(Short.MIN_VALUE, ss1);
        Assert.assertEquals(Integer.MAX_VALUE, si0);
        Assert.assertEquals(Integer.MIN_VALUE, si1);

        Assert.assertEquals(sb0, short_to_byte(ss0, true, true));
        Assert.assertEquals(sb1, short_to_byte(ss1, true, true));
        Assert.assertEquals(sb0, int_to_byte(si0, true, true));
        Assert.assertEquals(sb1, int_to_byte(si1, true, true));

        final byte ub0 = (byte) 0xff;
        final float uf0 = byte_to_float(ub0, false);
        final short us0 = byte_to_short(ub0, false, false);
        final int ui0 = byte_to_int(ub0, false, false);

        Assert.assertEquals(1.0f, uf0, 0.0);
        Assert.assertEquals((short)0xffff, us0);
        Assert.assertEquals(0xffffffff, ui0);

        Assert.assertEquals(ub0, short_to_byte(us0, false, false));
        Assert.assertEquals(us0, int_to_short(ui0, false, false));
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestValueConversion.class.getName());
    }

}
