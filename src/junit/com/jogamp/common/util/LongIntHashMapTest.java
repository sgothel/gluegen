/**
 * Created on Sunday, March 28 2010 21:01
 */
package com.jogamp.common.util;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static java.lang.System.*;

/**
 *
 * @author Michael Bien
 * @author Simon Goller
 */
public class LongIntHashMapTest {

    private static int iterations;
    private static long[] rndKeys;
    private static int[] rndValues;

    @BeforeClass
    public static void init() {

        iterations = 20000;
        final int keySeed = 42;
        final int valueSeed = 23;

        Random keyRnd = new Random(/*keySeed*/);
        Random valueRnd = new Random(/*valueSeed*/);

        rndKeys = new long[iterations];
        rndValues = new int[iterations];
        for (int i = 0; i < iterations; i++) {
            rndValues[i] = valueRnd.nextInt();
            rndKeys[i] = keyRnd.nextLong();
        }

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
            intmap.put(rndKeys[i], rndValues[i]);

            assertTrue(intmap.containsValue(rndValues[i]));
            assertTrue(intmap.containsKey(rndKeys[i]));
        }

        for (int i = 0; i < iterations; i++) {
            map.put(rndKeys[i], rndValues[i]);
        }

        assertEquals(map.size(), intmap.size());

        for (Entry<Long, Integer> entry : map.entrySet()) {
            assertTrue(intmap.containsKey(entry.getKey()));
            assertTrue(intmap.containsValue(entry.getValue()));
        }

        int i = 0;
        for (Entry<Long, Integer> entry : map.entrySet()) {
            assertEquals((int)entry.getValue(), intmap.remove(entry.getKey()));
            assertEquals(map.size() - i - 1, intmap.size());
            i++;
        }

    }

    @Test
    public void iteratorTest() {

        final LongIntHashMap map = new LongIntHashMap(iterations);

        for (int i = 0; i < iterations; i++) {
            map.put(rndKeys[i], rndValues[i]);
        }

        Iterator iterator = map.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());

        int n = 0;
        while (iterator.hasNext()) {
            LongIntHashMap.Entry entry = (LongIntHashMap.Entry)iterator.next();
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

    void benchmark(boolean warmup) {

        // simple benchmark
        final LongIntHashMap intmap      = new LongIntHashMap(1024);
        final HashMap<Long, Integer> map = new HashMap<Long, Integer>(1024);

        out.println(intmap.getClass().getName()+" vs "+map.getClass().getName()+
                " warmup: " + warmup);

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
        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            intmap.get(rndValues[i]);
        }
        long intmapGetTime = (nanoTime() - time);
        out.println("   iimap: " + intmapGetTime/1000000.0f+"ms");

        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.get(rndValues[i]);
        }
        long mapGetTime = (nanoTime() - time);
        out.println("   map:   " + mapGetTime/1000000.0f+"ms");


        out.println();
        out.println("remove");
        time = nanoTime();
        for (int i = 0; i < iterations; i++) {
            intmap.remove(rndValues[i]);
        }
        long intmapRemoveTime = (nanoTime() - time);
        out.println("   iimap: " + intmapRemoveTime/1000000.0f+"ms");

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
