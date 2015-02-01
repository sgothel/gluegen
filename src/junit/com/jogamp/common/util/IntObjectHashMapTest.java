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

import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.SingletonJunitCase;

import static org.junit.Assert.*;

/**
 *
 * @author Michael Bien
 * @author Sven Gothel
 */
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IntObjectHashMapTest extends SingletonJunitCase {

    private static int iterations;
    private static IntIntObjUniqueRndValues pairs;

    @BeforeClass
    public static void init() {
        iterations = ( Platform.getCPUType().family == Platform.CPUFamily.ARM ) ? 20 : 10000;
        pairs = new IntIntObjUniqueRndValues(iterations);
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
            intmap.put(pairs.keys[i], pairs.values[i]);

            assertTrue(intmap.containsValue(pairs.values[i]));
            assertTrue(intmap.containsKey(pairs.keys[i]));
        }

        for (int i = 0; i < iterations; i++) {
            map.put(pairs.keys[i], pairs.values[i]);
        }

        assertEquals(map.size(), intmap.size());

        for (final Entry<Integer, IntCloneable> entry : map.entrySet()) {
            assertTrue(intmap.containsKey(entry.getKey()));
            assertTrue(intmap.containsValue(entry.getValue()));
        }

        int i = 0;
        for (final Entry<Integer, IntCloneable> entry : map.entrySet()) {
            assertEquals(entry.getValue(), intmap.remove(entry.getKey()));
            assertEquals(map.size() - i - 1, intmap.size());
            i++;
        }

    }

    @Test
    public void iteratorTest() {

        final IntObjectHashMap intmap = new IntObjectHashMap(iterations);

        for (int i = 0; i < iterations; i++) {
            intmap.put(pairs.keys[i], pairs.values[i]);
        }

        final Iterator<IntObjectHashMap.Entry> iterator = intmap.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());

        int n = 0;
        while (iterator.hasNext()) {
            final IntObjectHashMap.Entry entry = iterator.next();
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
            intmap.put(pairs.keys[i], pairs.values[i]);
        }

        final IntObjectHashMap intmapCopy = (IntObjectHashMap) intmap.clone();

        assertEquals(intmap.size(), intmapCopy.size());
        assertEquals(intmap.getKeyNotFoundValue(), intmapCopy.getKeyNotFoundValue());

        final Iterator<IntObjectHashMap.Entry> iterator = intmap.iterator();
        assertNotNull(iterator);
        assertTrue(iterator.hasNext());

        final Iterator<IntObjectHashMap.Entry> iteratorCopy = intmapCopy.iterator();
        assertNotNull(iteratorCopy);
        assertTrue(iteratorCopy.hasNext());

        int n = 0;
        while (iterator.hasNext()) {
            assertTrue(iteratorCopy.hasNext());
            final IntObjectHashMap.Entry entry = iterator.next();
            final IntObjectHashMap.Entry entryCopy = iteratorCopy.next();
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
            assertTrue(intmap.containsValue(pairs.values[i]));
            assertTrue(intmap.containsKey(pairs.keys[i]));
            assertTrue(intmapCopy.containsValue(pairs.values[i]));
            assertTrue(intmapCopy.containsKey(pairs.keys[i]));
        }

//        out.println(intmap);

    }

    public static void main(final String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(IntObjectHashMapTest.class.getName());
    }

}
