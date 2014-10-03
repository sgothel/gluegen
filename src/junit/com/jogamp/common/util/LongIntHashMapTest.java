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

/**
 * Created on Sunday, March 28 2010 21:01
 */
package com.jogamp.common.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.SingletonJunitCase;

import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 *
 * @author Michael Bien
 * @author Simon Goller
 */
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LongIntHashMapTest extends SingletonJunitCase {

    private static int iterations;
    private static LongIntUniqueRndValues pairs;

    @BeforeClass
    public static void init() {
        iterations = ( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) ? 20 : 10000;
        pairs = new LongIntUniqueRndValues(iterations);
    }

    /**
     * Test of put method, of class LongIntHashMap.
     */
    @Test
    public void testPutRemove() {

        final LongIntHashMap intmap = new LongIntHashMap();
        final HashMap<Long, Integer> map = new HashMap<Long, Integer>();

        // put
        for (int i = 0; i < iterations; i++) {
            intmap.put(pairs.keys[i], pairs.values[i]);

            assertTrue(intmap.containsValue(pairs.values[i]));
            assertTrue(intmap.containsKey(pairs.keys[i]));
        }

        for (int i = 0; i < iterations; i++) {
            map.put(pairs.keys[i], pairs.values[i]);
        }

        assertEquals(map.size(), intmap.size());

        for (final Entry<Long, Integer> entry : map.entrySet()) {
            assertTrue(intmap.containsKey(entry.getKey()));
            assertTrue(intmap.containsValue(entry.getValue()));
        }

        int i = 0;
        for (final Entry<Long, Integer> entry : map.entrySet()) {
            assertEquals((int)entry.getValue(), intmap.remove(entry.getKey()));
            assertEquals(map.size() - i - 1, intmap.size());
            i++;
        }

    }

    @Test
    public void iteratorTest() {

        final LongIntHashMap map = new LongIntHashMap(iterations);

        for (int i = 0; i < iterations; i++) {
            map.put(pairs.keys[i], pairs.values[i]);
        }

        final Iterator<LongIntHashMap.Entry> iterator = map.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());

        int n = 0;
        while (iterator.hasNext()) {
            final LongIntHashMap.Entry entry = iterator.next();
            assertNotNull(entry);
            n++;
        }
        assertEquals(map.size(), n);

//        out.println(intmap);

    }

    @Test
    public void benchmark() {
        benchmark(true);
        benchmark(false);
    }

    void benchmark(final boolean warmup) {

        // simple benchmark
        final LongIntHashMap intmap      = new LongIntHashMap(1024);
        final HashMap<Long, Integer> map = new HashMap<Long, Integer>(1024);

        out.println(intmap.getClass().getName()+" vs "+map.getClass().getName()+
                " warmup: " + warmup);

        out.println("put");
        long time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            intmap.put(pairs.keys[i], pairs.values[i]);
        }
        final long intmapPutTime = (nanoTime() - time);
        out.println("   iimap: " + intmapPutTime/1000000.0f+"ms");


        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.put(pairs.keys[i], pairs.values[i]);
        }
        final long mapPutTime = (nanoTime() - time);
        out.println("   map:   " + mapPutTime/1000000.0f+"ms");


        System.out.println();
        System.out.println("get");
        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            intmap.get(pairs.keys[i]);
        }
        final long intmapGetTime = (nanoTime() - time);
        out.println("   iimap: " + intmapGetTime/1000000.0f+"ms");

        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.get(pairs.keys[i]);
        }
        final long mapGetTime = (nanoTime() - time);
        out.println("   map:   " + mapGetTime/1000000.0f+"ms");


        out.println();
        out.println("remove");
        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            intmap.remove(pairs.keys[i]);
        }
        assertEquals(0, intmap.size());
        final long intmapRemoveTime = (nanoTime() - time);
        out.println("   iimap: " + intmapRemoveTime/1000000.0f+"ms");

        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.remove(pairs.keys[i]);
        }
        assertEquals(0, map.size());
        final long mapRemoveTime = (nanoTime() - time);
        out.println("   map:   " + mapRemoveTime/1000000.0f+"ms");

        if(!warmup) {
            // In case the 1st class map magically improves
            // we add a tolerance around 50% since this would be hardly a bug.
            // The main goal of this primitve map is memory efficiency.
            // high and not O(1) assertTrue("'put' too slow", intmapPutTime <= mapPutTime + mapPutTime/4 );
            assertTrue("'get' too slow", intmapGetTime <= mapGetTime + mapGetTime/2 );
            assertTrue("'remove' too slow", intmapRemoveTime <= mapRemoveTime + mapRemoveTime/2 );
        }
    }

    public static void main(final String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(LongIntHashMapTest.class.getName());
    }

}
