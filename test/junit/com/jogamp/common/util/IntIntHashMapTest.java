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

        assertTrue("'put' to slow", intmapPutTime <= mapPutTime);
        assertTrue("'get' to slow", intmapGetTime <= mapGetTime);
        assertTrue("'remove' to slow", intmapRemoveTime <= mapRemoveTime);
    }


}
