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

import java.util.*;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.junit.util.JunitTracer;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestArrayHashSet01 extends JunitTracer {

    public static class Dummy {
        int i1, i2, i3;

        public Dummy(final int i1, final int i2, final int i3) {
            this.i1 = i1;
            this.i2 = i2;
            this.i3 = i3;
        }

        public boolean equals(final Object o) {
            if(o instanceof Dummy) {
                final Dummy d = (Dummy)o;
                return this.i1 == d.i1 &&
                       this.i2 == d.i2 &&
                       this.i3 == d.i3 ;
            }
            return false;
        }

        public final int hashCode() {
            // 31 * x == (x << 5) - x
            int hash = 31 + i1;
            hash = ((hash << 5) - hash) + i2;
            hash = ((hash << 5) - hash) + i3;
            return hash;
        }

        public String toString() {
            return "Dummy["+super.toString()+": "+i1+", "+i2+", "+i3+"]";
        }
    }

    public void populate(final List<Dummy> l, final int start, final int len, final int i2, final int i3, final int expectedPlusSize) {
        final int oldSize = l.size();
        int pos = start+len-1;
        while(pos>=start) {
            l.add(new Dummy(pos--, i2, i3));
        }
        Assert.assertEquals(expectedPlusSize, l.size() - oldSize);
    }

    @Test
    public void test01ArrayHashSet() {
        final ArrayHashSet<Dummy> l = new ArrayHashSet<Dummy>();
        populate(l, 10, 100, 22, 34, 100); // [10 .. 109]
        populate(l, 10, 100, 22, 34,   0); // [10 .. 109]
        populate(l,  6,   5, 22, 34,   4); // [ 6 .. 9], 10 already exists

        final Dummy p6_22_34 = new Dummy(6, 22, 34);

        // slow get on position ..
        final int i = l.indexOf(p6_22_34);
        Dummy q = l.get(i);
        Assert.assertNotNull(q);
        Assert.assertEquals(p6_22_34, q);
        Assert.assertTrue(p6_22_34.hashCode() == q.hashCode());
        Assert.assertTrue(p6_22_34 != q); // diff reference

        // fast identity ..
        q = l.get(p6_22_34);
        Assert.assertNotNull(q);
        Assert.assertEquals(p6_22_34, q);
        Assert.assertTrue(p6_22_34.hashCode() == q.hashCode());
        Assert.assertTrue(p6_22_34 != q); // diff reference

        Assert.assertTrue(!l.add(q)); // add same
        Assert.assertTrue(!l.add(p6_22_34)); // add equivalent

        q = l.getOrAdd(p6_22_34); // not added test
        Assert.assertNotNull(q);
        Assert.assertEquals(p6_22_34, q);
        Assert.assertTrue(p6_22_34.hashCode() == q.hashCode());
        Assert.assertTrue(p6_22_34 != q); // diff reference

        final Dummy p1_2_3 = new Dummy(1, 2, 3); // a new one ..
        q = l.getOrAdd(p1_2_3); // added test
        Assert.assertNotNull(q);
        Assert.assertEquals(p1_2_3, q);
        Assert.assertTrue(p1_2_3.hashCode() == q.hashCode());
        Assert.assertTrue(p1_2_3 == q); // _same_ reference, since getOrAdd added it
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestArrayHashSet01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
