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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Michael Bien
 * @author Sven Gothel 
 */
public class IntObjectHashMapTest {

    public static class IntCloneable implements Cloneable {
        private int i;         
        
        public IntCloneable(int i) { this.i = i; }    
        
        public int intValue() { return i; }
        
        @Override
        public Object clone() {
            return new IntCloneable(i);
        }        
        
        @Override
        public boolean equals(Object obj) {
            if(this == obj)  { return true; }
            if (obj instanceof IntCloneable) {
                IntCloneable v = (IntCloneable)obj;
                return i == v.i ;
            }
            return false;            
        }
    }
    
    private static int iterations;
    private static int[] rndKeys;
    private static IntCloneable[] rndValues;

    @BeforeClass
    public static void init() {

        iterations = 20000;
        final int keySeed = 42;
        final int valueSeed = 23;

        Random keyRnd = new Random(/*keySeed*/);
        Random valueRnd = new Random(/*valueSeed*/);

        rndKeys = new int[iterations];
        rndValues = new IntCloneable[iterations];
        for (int i = 0; i < iterations; i++) {
            rndValues[i] = new IntCloneable(valueRnd.nextInt());
            rndKeys[i] = keyRnd.nextInt();
        }
    }

    /**
     * Test of put method, of class IntObjectHashMap.
     */
    @Test
    public void testPutRemove() {

        final IntObjectHashMap intmap = new IntObjectHashMap();
        final HashMap<Integer, IntCloneable> map = new HashMap<Integer, IntCloneable>();

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

        for (Entry<Integer, IntCloneable> entry : map.entrySet()) {
            assertTrue(intmap.containsKey(entry.getKey()));
            assertTrue(intmap.containsValue(entry.getValue()));
        }

        int i = 0;
        for (Entry<Integer, IntCloneable> entry : map.entrySet()) {
            assertEquals(entry.getValue(), intmap.remove(entry.getKey()));
            assertEquals(map.size() - i - 1, intmap.size());
            i++;
        }

    }
    
    @Test
    public void iteratorTest() {

        final IntObjectHashMap intmap = new IntObjectHashMap(iterations);

        for (int i = 0; i < iterations; i++) {
            intmap.put(rndKeys[i], rndValues[i]);
        }
        
        Iterator<IntObjectHashMap.Entry> iterator = intmap.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());

        int n = 0;
        while (iterator.hasNext()) {
            IntObjectHashMap.Entry entry = iterator.next();
            assertNotNull(entry);
            n++;
        }
        assertEquals(intmap.size(), n);

//        out.println(intmap);

    }

    @Test
    public void cloneTest() {

        final IntObjectHashMap intmap = new IntObjectHashMap(iterations);

        for (int i = 0; i < iterations; i++) {
            intmap.put(rndKeys[i], rndValues[i]);
        }
        
        final IntObjectHashMap intmapCopy = (IntObjectHashMap) intmap.clone();
        
        assertEquals(intmap.size(), intmapCopy.size());
        assertEquals(intmap.getKeyNotFoundValue(), intmapCopy.getKeyNotFoundValue());
        
        Iterator<IntObjectHashMap.Entry> iterator = intmap.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());

        Iterator<IntObjectHashMap.Entry> iteratorCopy = intmapCopy.iterator();
        assertNotNull(iteratorCopy);
        assertTrue(iteratorCopy.hasNext());
        
        int n = 0;
        while (iterator.hasNext()) {
            assertTrue(iteratorCopy.hasNext());
            IntObjectHashMap.Entry entry = iterator.next();
            IntObjectHashMap.Entry entryCopy = iteratorCopy.next();
            assertNotNull(entry);
            assertNotNull(entryCopy);
            assertEquals(entry.key, entryCopy.key);
            assertEquals(entry.value, entryCopy.value);
            n++;
        }
        assertTrue(!iteratorCopy.hasNext());

        assertEquals(intmap.size(), n);
        assertEquals(intmapCopy.size(), n);

        for (int i = 0; i < iterations; i++) {
            assertTrue(intmap.containsValue(rndValues[i]));
            assertTrue(intmap.containsKey(rndKeys[i]));
            assertTrue(intmapCopy.containsValue(rndValues[i]));
            assertTrue(intmapCopy.containsKey(rndKeys[i]));
        }
        
//        out.println(intmap);

    }

    public static void main(String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(IntObjectHashMapTest.class.getName());
    }

}
