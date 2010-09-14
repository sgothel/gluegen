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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 *
 * @author Michael Bien
 */
public class IntIntHashMapTest {

    private static int iterations;
    private static int[] rndKeys;
    private static int[] rndValues;

    @BeforeClass
    public static void init() {

        iterations = 20000;
        final int keySeed = 42;
        final int valueSeed = 23;

        Random keyRnd = new Random(/*keySeed*/);
        Random valueRnd = new Random(/*valueSeed*/);

        rndKeys = new int[iterations];
        rndValues = new int[iterations];
        for (int i = 0; i < iterations; i++) {
            rndValues[i] = valueRnd.nextInt();
            rndKeys[i] = keyRnd.nextInt();
        }

    }

    /**
     * Test of put method, of class IntIntHashMap.
     */
    @Test
    public void testPutRemove() {

        final IntIntHashMap intmap = new IntIntHashMap();
        final HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

        // put
        for (int i = 0; i < iterations; i++) {
            intmap.put(rndKeys[i], rndValues[i]);

            assertTrue(intmap.containsValue(rndValues[i]));
            assertTrue(intmap.containsKey(rndKeys[i]));
        }

        for (int i = 0; i < iterations; i++) {
            map.put(rndKeys[i], rndValues[i]);
        }

        assertEquals(map.size(), intmap.size());

        for (Entry<Integer, Integer> entry : map.entrySet()) {
            assertTrue(intmap.containsKey(entry.getKey()));
            assertTrue(intmap.containsValue(entry.getValue()));
        }

        int i = 0;
        for (Entry<Integer, Integer> entry : map.entrySet()) {
            assertEquals((int)entry.getValue(), intmap.remove(entry.getKey()));
            assertEquals(map.size() - i - 1, intmap.size());
            i++;
        }

    }
    
    @Test
    public void iteratorTest() {

        final IntIntHashMap intmap = new IntIntHashMap(iterations);

        for (int i = 0; i < iterations; i++) {
            intmap.put(rndKeys[i], rndValues[i]);
        }
        
        Iterator iterator = intmap.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());

        int n = 0;
        while (iterator.hasNext()) {
            IntIntHashMap.Entry entry = (IntIntHashMap.Entry)iterator.next();
            assertNotNull(entry);
            n++;
        }
        assertEquals(intmap.size(), n);

//        out.println(intmap);

    }

    @Test
    public void benchmark() {
        benchmark(true);
        benchmark(false);
    }

    void benchmark(boolean warmup) {

        // simple benchmark
        final IntIntHashMap intmap          = new IntIntHashMap(1024);
        final HashMap<Integer, Integer> map = new HashMap<Integer, Integer>(1024);

        out.println(intmap.getClass().getName()+" vs "+map.getClass().getName());

        out.println("put");
        long time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            intmap.put(rndKeys[i], rndValues[i]);
        }
        long intmapPutTime = (nanoTime() - time);
        out.println("   iimap: " + intmapPutTime/1000000.0f+"ms");


        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.put(rndKeys[i], rndValues[i]);
        }
        long mapPutTime = (nanoTime() - time);
        out.println("   map:   " + mapPutTime/1000000.0f+"ms");


        System.out.println();
        System.out.println("get");
        long intmapGetTime = (nanoTime() - time);
        out.println("   iimap: " + intmapGetTime/1000000.0f+"ms");
        for (int i = 0; i < iterations; i++) {
            intmap.get(rndValues[i]);
        }
        
        long mapGetTime = (nanoTime() - time);
        out.println("   map:   " + mapGetTime/1000000.0f+"ms");
        for (int i = 0; i < iterations; i++) {
            map.get(rndValues[i]);
        }


        out.println();
        out.println("remove");
        long intmapRemoveTime = (nanoTime() - time);
        out.println("   iimap: " + intmapRemoveTime/1000000.0f+"ms");
        for (int i = 0; i < iterations; i++) {
            intmap.remove(rndValues[i]);
        }

        long mapRemoveTime = (nanoTime() - time);
        out.println("   map:   " + mapRemoveTime/1000000.0f+"ms");
        for (int i = 0; i < iterations; i++) {
            map.remove(rndValues[i]);
        }

        if(!warmup) {
            assertTrue("'put' too slow", intmapPutTime <= mapPutTime);
            assertTrue("'get' too slow", intmapGetTime <= mapGetTime);
            assertTrue("'remove' too slow", intmapRemoveTime <= mapRemoveTime);
        }
    }


}
