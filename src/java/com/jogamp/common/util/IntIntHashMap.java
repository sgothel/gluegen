/*
 * Copyright (c) 2010, Michael Bien
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of JogAmp nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Michael Bien BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Created on Sunday, March 28 2010 21:01
 */
package com.jogamp.common.util;

/**
 * Fast HashMap with int keys and int values.
 * Original code is based on the <a href="http://code.google.com/p/skorpios/"> skorpios project</a>
 * released under new BSD license.
 * @author Michael Bien
 * @see IntObjectHashMap
 */
public class IntIntHashMap {

    private final float loadFactor;

    private Entry[] table;
    
    private int size;
    private int mask;
    private int capacity;
    private int threshold;

    public IntIntHashMap() {
        this(16, 0.75f);
    }

    public IntIntHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    public IntIntHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity > 1 << 30) {
            throw new IllegalArgumentException("initialCapacity is too large.");
        }
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be greater than zero.");
        }
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("initialCapacity must be greater than zero.");
        }
        capacity = 1;
        while (capacity < initialCapacity) {
            capacity <<= 1;
        }
        this.loadFactor = loadFactor;
        this.threshold = (int) (capacity * loadFactor);
        this.table = new Entry[capacity];
        this.mask = capacity - 1;
    }

    public boolean containsValue(int value) {
        Entry[] table = this.table;
        for (int i = table.length; i-- > 0;) {
            for (Entry e = table[i]; e != null; e = e.next) {
                if (e.value == value) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsKey(int key) {
        int index = key & mask;
        for (Entry e = table[index]; e != null; e = e.next) {
            if (e.key == key) {
                return true;
            }
        }
        return false;
    }

    public int get(int key) {
        int index = key & mask;
        for (Entry e = table[index]; e != null; e = e.next) {
            if (e.key == key) {
                return e.value;
            }
        }
        return 0;
    }

    public int put(int key, int value) {
        int index = key & mask;
        // Check if key already exists.
        for (Entry e = table[index]; e != null; e = e.next) {
            if (e.key != key) {
                continue;
            }
            int oldValue = e.value;
            e.value = value;
            return oldValue;
        }
        table[index] = new Entry(key, value, table[index]);
        if (size++ >= threshold) {
            // Rehash.
            int newCapacity = 2 * capacity;
            Entry[] newTable = new Entry[newCapacity];
            Entry[] src = table;
            int bucketmask = newCapacity - 1;
            for (int j = 0; j < src.length; j++) {
                Entry e = src[j];
                if (e != null) {
                    src[j] = null;
                    do {
                        Entry next = e.next;
                        index = e.key & bucketmask;
                        e.next = newTable[index];
                        newTable[index] = e;
                        e = next;
                    } while (e != null);
                }
            }
            table = newTable;
            capacity = newCapacity;
            threshold = (int) (newCapacity * loadFactor);
            mask = capacity - 1;
        }
        return 0;
    }

    public int remove(int key) {
        int index = key & mask;
        Entry prev = table[index];
        Entry e = prev;
        while (e != null) {
            Entry next = e.next;
            if (e.key == key) {
                size--;
                if (prev == e) {
                    table[index] = next;
                } else {
                    prev.next = next;
                }
                return e.value;
            }
            prev = e;
            e = next;
        }
        return 0;
    }

    public int size() {
        return size;
    }

    public void clear() {
        Entry[] table = this.table;
        for (int index = table.length; --index >= 0;) {
            table[index] = null;
        }
        size = 0;
    }

    private final static class Entry {

        private final int key;
        private int value;
        private Entry next;

        private Entry(int k, int v, Entry n) {
            key = k;
            value = v;
            next = n;
        }
    }
}
