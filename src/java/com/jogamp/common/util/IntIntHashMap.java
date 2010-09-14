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

import java.util.Arrays;
import java.util.Iterator;

/*
 * Note: this map is used as template for other maps.
 */

/**
 * Fast HashMap for primitive data. Optimized for being GC friendly.
 * Original code is based on the <a href="http://code.google.com/p/skorpios/"> skorpios project</a>
 * released under new BSD license.
 *
 * @author Michael Bien
 * @author Simon Goller
 * 
 * @see IntObjectHashMap
 * @see IntLongHashMap
 * @see LongObjectHashMap
 * @see LongLongHashMap
 * @see LongIntHashMap
 */
public class /*name*/IntIntHashMap/*name*/ implements Iterable {

    private final float loadFactor;

    private Entry[] table;

    private int size;
    private int mask;
    private int capacity;
    private int threshold;
    private /*value*/int/*value*/ keyNotFoundValue = /*null*/-1/*null*/;

    public /*name*/IntIntHashMap/*name*/() {
        this(16, 0.75f);
    }

    public /*name*/IntIntHashMap/*name*/(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    public /*name*/IntIntHashMap/*name*/(int initialCapacity, float loadFactor) {
        if (initialCapacity > 1 << 30) {
            throw new IllegalArgumentException("initialCapacity is too large.");
        }
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be greater than zero.");
        }
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("loadFactor must be greater than zero.");
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

    public boolean containsValue(/*value*/int/*value*/ value) {
        Entry[] t = this.table;
        for (int i = t.length; i-- > 0;) {
            for (Entry e = t[i]; e != null; e = e.next) {
                if (e.value == value) {
                    return true;
                }
            }
        }
        return false;
    }

//    @SuppressWarnings(value="cast")
    public boolean containsKey(/*key*/int/*key*/ key) {
        Entry[] t = this.table;
        int index = (int) (key & mask);
        for (Entry e = t[index]; e != null; e = e.next) {
            if (e.key == key) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@link #getKeyNotFoundValue} if this map contains no mapping for the key.
     */
//    @SuppressWarnings(value="cast")
    public /*value*/int/*value*/ get(/*key*/int/*key*/ key) {
        Entry[] t = this.table;
        int index = (int) (key & mask);
        for (Entry e = t[index]; e != null; e = e.next) {
            if (e.key == key) {
                return e.value;
            }
        }
        return keyNotFoundValue;
    }

    /**
     * Maps the key to the specified value. If a mapping to this key already exists,
     * the previous value will be returned (otherwise {@link #getKeyNotFoundValue}).
     */
//    @SuppressWarnings(value="cast")
    public /*value*/int/*value*/ put(/*key*/int/*key*/ key, /*value*/int/*value*/ value) {
        Entry[] t = this.table;
        int index = (int) (key & mask);
        // Check if key already exists.
        for (Entry e = t[index]; e != null; e = e.next) {
            if (e.key != key) {
                continue;
            }
            /*value*/int/*value*/ oldValue = e.value;
            e.value = value;
            return oldValue;
        }
        t[index] = new Entry(key, value, t[index]);

        if (size++ >= threshold) {
            // Rehash.
            int newCapacity = 2 * capacity;
            Entry[] newTable = new Entry[newCapacity];
            /*key*/int/*key*/ bucketmask = newCapacity - 1;
            for (int j = 0; j < t.length; j++) {
                Entry e = t[j];
                if (e != null) {
                    t[j] = null;
                    do {
                        Entry next = e.next;
                        index = (int) (e.key & bucketmask);
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
        return keyNotFoundValue;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     */
//    @SuppressWarnings(value="cast")
    public void putAll(/*name*/IntIntHashMap/*name*/ source) {
        Iterator itr = source.iterator();
        while(itr.hasNext()) {
            Entry e = (Entry) itr.next();
            put(e.key, e.value);
        }
    }

    /**
     * Removes the key-value mapping from this map.
     * Returns the previously mapped value or {@link #getKeyNotFoundValue} if no such mapping exists.
     */
//    @SuppressWarnings(value="cast")
    public /*value*/int/*value*/ remove(/*key*/int/*key*/ key) {
        Entry[] t = this.table;
        int index = (int) (key & mask);
        Entry prev = t[index];
        Entry e = prev;
        while (e != null) {
            Entry next = e.next;
            if (e.key == key) {
                size--;
                if (prev == e) {
                    t[index] = next;
                } else {
                    prev.next = next;
                }
                return e.value;
            }
            prev = e;
            e = next;
        }
        return keyNotFoundValue;
    }

    /**
     * Returns the current number of key-value mappings in this map.
     */
    public int size() {
        return size;
    }

    /**
     * Clears the entire map. The size is 0 after this operation.
     */
    public void clear() {
        Arrays.fill(table, null);
        size = 0;
    }

    /**
     * Returns a new {@link Iterator}.
     * Note: this Iterator does not yet support removal of elements.
     */
//    @Override
    public Iterator/*<Entry>*/ iterator() {
        return new EntryIterator(table);
    }

    /**
     * Sets the new key not found value.
     * For primitive types (int, long) the default is -1,
     * for Object types, the default is null.
     *
     * @return the previous key not found value
     * @see #get
     * @see #put 
     */
    public /*value*/int/*value*/ setKeyNotFoundValue(/*value*/int/*value*/ newKeyNotFoundValue) {
        /*value*/int/*value*/ t = keyNotFoundValue;
        keyNotFoundValue = newKeyNotFoundValue;
        return t;
    }

    /**
     * Returns the value which is returned if no value has been found for the specified key.
     * @see #get
     * @see #put
     */
    public /*value*/int/*value*/ getKeyNotFoundValue() {
        return keyNotFoundValue;
    }

//    @Override
    public String toString() {
        // TODO use StringBuilder as soon we are at language level 5
        String str = "{";
        Iterator itr = iterator();
        while(itr.hasNext()) {
            str += itr.next();
            if(itr.hasNext()) {
                str += ", ";
            }
        }
        str += "}";
        return str;
    }
    
    private final static class EntryIterator implements Iterator/*<Entry>*/ {

        private final Entry[] entries;
        
        private int index;
        private Entry next;
            
        private EntryIterator(Entry[] entries){
            this.entries = entries;
            // load next
            next();
        }

//        @Override
        public boolean hasNext() {
            return next != null;
        }

//        @Override
        public Object next() {
            Entry current = next;

            if(current != null && current.next != null) {
                next = current.next;
            }else{
                while(index < entries.length) {
                    Entry e = entries[index++];
                    if(e != null) {
                        next = e;
                        return current;
                    }
                }
                next = null;
            }

            return current;
        }

//        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
    /**
     * An entry mapping a key to a value.
     */
    public final static class Entry {

        public final /*key*/int/*key*/ key;
        public /*value*/int/*value*/ value;
        
        private Entry next;

        private Entry(/*key*/int/*key*/ k, /*value*/int/*value*/ v, Entry n) {
            key = k;
            value = v;
            next = n;
        }

        /**
         * Returns the key of this entry.
         */
        public /*key*/int/*key*/ getKey() {
            return key;
        }

        /**
         * Returns the value of this entry.
         */
        public /*value*/int/*value*/ getValue() {
            return value;
        }

        /**
         * Sets the value for this entry.
         */
        public void setValue(/*value*/int/*value*/ value) {
            this.value = value;
        }

//        @Override
        public String toString() {
            return "["+key+":"+value+"]";
        }

    }
}
