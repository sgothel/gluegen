/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package com.jogamp.gluegen.test.junit.internals;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.gluegen.cgram.types.FloatType;
import com.jogamp.gluegen.cgram.types.IntType;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestType extends SingletonJunitCase {

    @Test
    public void test01Equals() {
        final FloatType f1 = new FloatType("GLfloat", null, 0, null);
        final FloatType f2 = new FloatType("float", null, 0, null);
        final IntType i1 = new IntType("GLint", null, false, 0, null);
        final IntType i2 = new IntType("int", null, false, 0, null);
        final int f1H = f1.hashCode();
        final int f2H = f2.hashCode();
        final int i1H = i1.hashCode();
        final int i2H = i2.hashCode();

        final int f1HS = f1.hashCodeSemantics();
        final int f2HS = f2.hashCodeSemantics();
        final int i1HS = i1.hashCodeSemantics();
        final int i2HS = i2.hashCodeSemantics();

        Assert.assertFalse(f1.getClass().isInstance(null));
        Assert.assertTrue(f1.getClass().isInstance(f2));
        Assert.assertTrue(i1.getClass().isInstance(i2));
        Assert.assertFalse(f1.getClass().isInstance(i2));

        Assert.assertFalse(f1.equals(f2));
        Assert.assertFalse(i1.equals(i2));
        Assert.assertFalse(f1.equals(i2));
        Assert.assertNotEquals(f1H, f2H);
        Assert.assertNotEquals(i1H, i2H);
        Assert.assertNotEquals(f1H, i2H);

        Assert.assertTrue(f1.equalSemantics(f2));
        Assert.assertTrue(i1.equalSemantics(i2));
        Assert.assertFalse(f1.equalSemantics(i2));
        Assert.assertEquals(f1HS, f2HS);
        Assert.assertEquals(i1HS, i2HS);
        Assert.assertNotEquals(f1HS, i2HS);
    }

    public static void main(final String args[]) {
        final String tstname = TestType.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
