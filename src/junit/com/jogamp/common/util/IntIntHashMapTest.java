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
    private static float loadFactor;

    @BeforeClass
    public static void init() {

        iterations = 20000;
        final int keySeed = 42;
        final int valueSeed = 23;
        loadFactor = 0.75f;

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
        // TODO: determine if JIT can cause pauses that affect benchmark timing

        // pre-allocate estimated memory required for benchmark tests to ensure
        // that heap allocations do not affect benchmark timings. Memory will
        // be freed at end of local try block when allocation values go out
        // of scope...
        try
        {
            // estimate array size of entries for IntIntHashMap to include
            // memory needed for final + rehash growth based on loadsize and
            // iterations...
            int finalCount = iterations;
            int lastRehash = (int)(iterations * loadFactor);
            int allocationFinal = 1;
            int allocationRehash = 1;
            while (allocationFinal < finalCount)
                allocationFinal <<= 1;
            while (allocationRehash < lastRehash)
                allocationRehash <<= 1;
            int totalArrayAllocations = allocationFinal + allocationRehash;
            // grab memory for entry storage
            Object[] iiAllocation = new Object[totalArrayAllocations];
            Object[] hmAllocation = new Object[totalArrayAllocations];
            // estimate reference storage size.
            byte objRefBytes = 4;
            if (System.getProperty("os.arch").indexOf("64")!=-1)
            {
                objRefBytes = 8;
            }
            // estimate IntIntEntrySize (implemenation dependendent)
            int iiEntrySize =   4           +   // key storage size     (int)
                                4           +   // value storage size   (int)
                                objRefBytes ;   // next entry reference size
            // estimate HashMapEntrySize
            // implementation dependent, based on SUN jdk 1.6.0_20 source for
            // HashMap<Integer,Integer>
            int hmEntrySize = objRefBytes + // key reference size       (Integer)
                              4           + // key storage size         (int)
                              objRefBytes + // value reference size     (Integer)
                              4           + // value storage size       (int)
                              objRefBytes + // next entry reference size
                              4           ; // cached hash entry value  (int)
            for (int i=0; i<totalArrayAllocations; ++i)
            {
                if (i<iterations)
                {
                    iiAllocation[i] = new byte[iiEntrySize];
                    hmAllocation[i] = new byte[hmEntrySize];
                }  else {
                    iiAllocation[i] = null;
                    hmAllocation[i] = null;
                }
            }
        } catch (OutOfMemoryError oome) {
            out.println("May not have enough memory to run benchmark test");
            out.println(oome.getMessage());
            out.println("Total: " + Runtime.getRuntime().totalMemory() +
                    " Max: " + Runtime.getRuntime().maxMemory() +
                    " Free: " + Runtime.getRuntime().freeMemory());
        }
        // have the GarbageCollector release the memory arrays that are no
        // longer in scope
        gc();
        benchmark(true);
        benchmark(false);
    }

    void benchmark(boolean warmup) {

        // simple benchmark
        final IntIntHashMap intmap          = new IntIntHashMap(1024,loadFactor);
        final HashMap<Integer, Integer> map =
                new HashMap<Integer, Integer>(1024,loadFactor);

        out.println(intmap.getClass().getName()+" vs "+map.getClass().getName()+
                " warmup: " + warmup);

        out.println("put");
        // to decrease chance of Garbage Collector needing to run durring test
        // trigger the Garbage Collector prior to timing test.
        gc();
        long time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            intmap.put(rndKeys[i], rndValues[i]);
        }
        long intmapPutTime = (nanoTime() - time);
        out.println("   iimap: " + intmapPutTime/1000000.0f+"ms");

        // to decrease chance of Garbage Collector needing to run durring test
        // trigger the Garbage Collector prior to timing test.
        gc();
        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.put(rndKeys[i], rndValues[i]);
        }
        long mapPutTime = (nanoTime() - time);
        out.println("   map:   " + mapPutTime/1000000.0f+"ms");

        System.out.println();
        System.out.println("get");
        // to decrease chance of Garbage Collector needing to run durring test
        // trigger the Garbage Collector prior to timing test.
        gc();
        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            intmap.get(rndValues[i]);
        }
        long intmapGetTime = (nanoTime() - time);
        out.println("   iimap: " + intmapGetTime/1000000.0f+"ms");

        // to decrease chance of Garbage Collector needing to run durring test
        // trigger the Garbage Collector prior to timing test.
        gc();
        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.get(rndValues[i]);
        }
        long mapGetTime = (nanoTime() - time);
        out.println("   map:   " + mapGetTime/1000000.0f+"ms");


        out.println();
        out.println("remove");
        // to decrease chance of Garbage Collector needing to run durring test
        // trigger the Garbage Collector prior to timing test.
        gc();
        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            intmap.remove(rndValues[i]);
        }
        long intmapRemoveTime = (nanoTime() - time);
        out.println("   iimap: " + intmapRemoveTime/1000000.0f+"ms");

        // to decrease chance of Garbage Collector needing to run durring test
        // trigger the Garbage Collector prior to timing test.
        gc();
        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.remove(rndValues[i]);
        }
        long mapRemoveTime = (nanoTime() - time);
        out.println("   map:   " + mapRemoveTime/1000000.0f+"ms");

        if(!warmup) {
            assertTrue("'put' too slow", intmapPutTime <= mapPutTime);
            assertTrue("'get' too slow", intmapGetTime <= mapGetTime);
            assertTrue("'remove' too slow", intmapRemoveTime <= mapRemoveTime);
        }
    }


}
