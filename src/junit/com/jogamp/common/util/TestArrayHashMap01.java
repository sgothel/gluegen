/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestArrayHashMap01 extends SingletonJunitCase {

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

    void populate(final Map<Integer, Dummy> l, final int start, final int len,
                  final int i2, final int i3, final int expectedPlusSize) {
        final int oldSize = l.size();
        for(int pos = start+len-1; pos>=start; pos--) {
            l.put(pos, new Dummy(pos, i2, i3));
        }
        Assert.assertEquals(expectedPlusSize, l.size() - oldSize);
    }
    boolean checkOrder(final List<Dummy> l, final int startIdx, final int start, final int len) {
        for(int i=0; i<len; i++) {
            final Dummy d = l.get(startIdx+i);
            final int i1 = start+len-1-i;
            if( d.i1 != i1 ) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void test01ArrayHashMapWithNullValue() {
        testArrayHashMapImpl(true);
    }
    @Test
    public void test02ArrayHashSetWithoutNullValue() {
        testArrayHashMapImpl(false);
    }
    void testArrayHashMapImpl(final boolean supportNullValue) {
        final ArrayHashMap<Integer, Dummy> l =
                new ArrayHashMap<Integer, Dummy>(supportNullValue,
                                        ArrayHashSet.DEFAULT_INITIAL_CAPACITY,
                                        ArrayHashSet.DEFAULT_LOAD_FACTOR);
        Assert.assertEquals(supportNullValue, l.supportsNullValue());
        final int p7_22_34_key, p7_22_34_idx;
        final Dummy p7_22_34_orig;
        final int p6_22_34_key, p6_22_34_idx;
        final Dummy p6_22_34_orig;
        {
            populate(l, 10, 100, 22, 34, 100); // [109 .. 10]
            Assert.assertTrue(checkOrder(l.getData(), 0, 10, 100));
            populate(l, 10, 100, 22, 34,   0); // [109 .. 10]
            Assert.assertTrue(checkOrder(l.getData(), 0, 10, 100));
            populate(l,  6,   5, 22, 34,   4); // [  9 ..  6], 10 already exists
            Assert.assertTrue(checkOrder(l.getData(), 100, 6, 4));
            p7_22_34_idx = l.size() - 2;
            p7_22_34_key = 7;
            p7_22_34_orig = l.get(p7_22_34_key);
            p6_22_34_idx = l.size() - 1;
            p6_22_34_key = 6;
            p6_22_34_orig = l.get(p6_22_34_key);
        }
        Assert.assertNotNull(p7_22_34_orig);
        Assert.assertEquals(7, p7_22_34_orig.i1);
        Assert.assertEquals(l.getData().get(p7_22_34_idx), p7_22_34_orig);
        Assert.assertNotNull(p6_22_34_orig);
        Assert.assertEquals(6, p6_22_34_orig.i1);
        Assert.assertEquals(l.getData().get(p6_22_34_idx), p6_22_34_orig);

        final Dummy p7_22_34_other = new Dummy(7, 22, 34);
        Assert.assertEquals(p7_22_34_other, p7_22_34_orig);
        Assert.assertTrue(p7_22_34_other.hashCode() == p7_22_34_orig.hashCode());
        Assert.assertTrue(p7_22_34_other != p7_22_34_orig); // diff reference
        final Dummy p6_22_34_other = new Dummy(6, 22, 34);
        Assert.assertEquals(p6_22_34_other, p6_22_34_orig);
        Assert.assertTrue(p6_22_34_other.hashCode() == p6_22_34_orig.hashCode());
        Assert.assertTrue(p6_22_34_other != p6_22_34_orig); // diff reference

        // fast identity ..
        Dummy q = l.get(p6_22_34_key);
        Assert.assertNotNull(q);
        Assert.assertEquals(p6_22_34_other, q);
        Assert.assertTrue(p6_22_34_other.hashCode() == q.hashCode());
        Assert.assertTrue(p6_22_34_other != q); // diff reference
        Assert.assertTrue(p6_22_34_orig == q); // same reference

        Assert.assertTrue(l.containsValue(q));
        Assert.assertTrue(l.containsValue(p6_22_34_other)); // add equivalent

        q = l.put(p6_22_34_key, p6_22_34_other); // override w/ diff hash-obj
        Assert.assertNotNull(q);
        Assert.assertEquals(p6_22_34_other, q);
        Assert.assertTrue(p6_22_34_other.hashCode() == q.hashCode());
        Assert.assertTrue(p6_22_34_other != q); // diff reference new != old (q)
        Assert.assertTrue(p6_22_34_orig == q); // same reference orig == old (q)
        Assert.assertTrue(checkOrder(l.getData(), 0, 10, 100));
        Assert.assertTrue(checkOrder(l.getData(), 100, 6, 4));

        final Dummy p1_2_3 = new Dummy(1, 2, 3); // a new one ..
        q = l.put(1, p1_2_3); // added test
        Assert.assertNull(q);

        final Dummy pNull = null;
        NullPointerException npe = null;
        try {
            q = l.put(0, pNull);
            Assert.assertNull(q);
        } catch (final NullPointerException _npe) { npe = _npe; }
        if( l.supportsNullValue() ) {
            Assert.assertNull(npe);
        } else {
            Assert.assertNotNull(npe);
        }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestArrayHashMap01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
