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

package com.jogamp.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/** 
 * Hashed ArrayList implementation of the List and Collection interface.
 *
 * Implementation properties are:
 *  <ul>
 *  <li> Unique elements utilizing {@link java.lang.Object#hashCode()} for O(1) operations, see below.</li>
 *  <li> Provides {@link java.util.List} functionality,
 *       ie {@link java.util.List#indexOf(java.lang.Object)}
 *       and {@link java.util.List#get(int)}, hence object identity can be implemented.</li>
 *  <li> Object identity via {@link #get(java.lang.Object)}</li>
 *  <li> Java 1.3 compatible</li>
 *  </ul>
 *
 * O(1) operations:
 * <ul>
 * <li> adding new element(s) </li>
 * <li> test for containment </li>
 * <li> identity </li>
 * <li> trying to remove non existent elements </li>
 * </ul>
 *
 * O(n) operations:
 * <ul>
 * <li> removing existing elements</li>
 * </ul>
 *
 * For thread safety, the application shall decorate access to instances via
 * {@link com.jogamp.common.util.locks.RecursiveLock}.
 *
*/

public class ArrayHashSet
    implements Cloneable, Collection, List
{
    HashMap   map  = new HashMap();   // object -> object
    ArrayList data = new ArrayList(); // list of objects

    public ArrayHashSet() {
        clear();
    }

    //
    // Cloneable
    //

    /**
     * @return a shallow copy of this ArrayHashSet, elements are not copied.
     */
    public final Object clone() {
        ArrayList clonedList = (ArrayList)data.clone();

        ArrayHashSet newObj = new ArrayHashSet();
        newObj.addAll(clonedList);

        return newObj;
    }

    //
    // Collection
    //

    public final void clear() {
        data.clear();
        map.clear();
    }

    /**
     * Add element at the end of this list, if it is not contained yet.
     * <br>
     * This is an O(1) operation
     *
     * @return true if the element was added to this list,
     *         otherwise false (already contained).
     */
    public final boolean add(Object element) {
        boolean exists = map.containsKey(element);
        if(!exists) {
            if(null != map.put(element, element)) {
                throw new InternalError("Already existing, but checked before: "+element);
            }
            if(!data.add(element)) {
                throw new InternalError("Couldn't add element: "+element);
            }
        }
        return !exists;
    }

    /**
     * Remove element from this list.
     * <br>
     * This is an O(1) operation, in case it does not exist,
     * otherwise O(n).
     *
     * @return true if the element was removed from this list,
     *         otherwise false (not contained).
     */
    public final boolean remove(Object element) {
        if ( null != map.remove(element) ) {
            if ( ! data.remove(element) ) {
                throw new InternalError("Couldn't remove prev mapped element: "+element);
            }
            return true;
        }
        return false;
    }

    /**
     * Add all elements of given {@link java.util.Collection} at the end of this list.
     * <br>
     * This is an O(n) operation, over the given Collection size.
     *
     * @return true if at least one element was added to this list,
     *         otherwise false (completely container).
     */
    public final boolean addAll(Collection c) {
        boolean mod = false;
        for (Iterator iter = c.iterator(); iter.hasNext(); ) {
            mod = mod || add(iter.next()) ;
        }
        return mod;
    }

    /**
     * Test for containment
     * <br>
     * This is an O(1) operation.
     *
     * @return true if the given element is contained by this list using fast hash map,
     *         otherwise false.
     */
    public final boolean contains(Object element) {
        return map.containsKey(element);
    }

    /**
     * Test for containment of given {@link java.util.Collection}
     * <br>
     * This is an O(n) operation, over the given Collection size.
     *
     * @return true if the given Collection is completly contained by this list using hash map,
     *         otherwise false.
     */
    public final boolean containsAll(Collection c) {
        for (Iterator iter = c.iterator(); iter.hasNext(); ) {
            if (! this.contains(iter.next()) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove all elements of given {@link java.util.Collection} from this list.
     * <br>
     * This is an O(n) operation.
     *
     * @return true if at least one element of this list was removed,
     *         otherwise false.
     */
    public final boolean removeAll(Collection c) {
        boolean mod = false;
        for (Iterator iter = c.iterator(); iter.hasNext(); ) {
            mod = this.remove(iter.next()) || mod;
        }
        return mod;
    }

    /**
     * Retain all elements of the given {@link java.util.Collection} c, ie
     * remove all elements not contained by the given {@link java.util.Collection} c.
     * <br>
     * This is an O(n) operation.
     *
     * @return true if at least one element of this list was removed,
     *         otherwise false.
     */
    public final boolean retainAll(Collection c) {
        boolean mod = false;
        for (Iterator iter = this.iterator(); iter.hasNext(); ) {
            Object o = iter.next();
            if (! c.contains(o) ) {
                mod = this.remove(o) || mod;
            }
        }
        return mod;
    }

    /**
     * This is an O(n) operation.
     *
     * @return true if arrayHashSet is of type ArrayHashSet and all entries are equal
     * Performance: arrayHashSet(1)
     */
    public final boolean equals(Object arrayHashSet) {
        if ( !(arrayHashSet instanceof ArrayHashSet) ) {
            return false;
        }
        return data.equals(((ArrayHashSet)arrayHashSet).data);
    }

    /**
     * This is an O(n) operation over the size of this list.
     *
     * @return the hash code of this list as define in {@link java.util.List#hashCode()},
     * ie hashing all elements of this list.
     */
    public final int hashCode() {
        return data.hashCode();
    }

    public final boolean isEmpty() {
        return data.isEmpty();
    }

    public final Iterator iterator() {
        return data.iterator();
    }

    public final int size() {
        return data.size();
    }

    public final Object[] toArray() {
        return data.toArray();
    }

    public final Object[] toArray(Object[] a) {
        return data.toArray(a);
    }

    //
    // List
    //

    public final Object get(int index) {
        return data.get(index);
    }

    public final int indexOf(Object element) {
        return data.indexOf(element);
    }

    /**
     * Add element at the given index in this list, if it is not contained yet.
     * <br>
     * This is an O(1) operation
     *
     * @throws IllegalArgumentException if the given element was already contained
     */
    public final void add(int index, Object element) {
        if ( map.containsKey(element) ) {
            throw new IllegalArgumentException("Element "+element+" is already contained");
        }
        if(null != map.put(element, element)) {
            throw new InternalError("Already existing, but checked before: "+element);
        }
        data.add(index, element);
    }

    /**
     * @throws UnsupportedOperationException
     */
    public final boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public final Object set(int index, Object element) {
        Object old = remove(index);
        if(null!=old) {
            add(index, element);
        }
        return old;
    }

    /**
     * Remove element at given index from this list.
     * <br>
     * This is an O(n) operation.
     *
     * @return the removed object
     */
    public final Object remove(int index) {
        Object o = get(index);
        if( null!=o && remove(o) ) {
            return o;
        }
        return null;
    }

    /**
     * Since this list is unique, equivalent to {@link #indexOf(java.lang.Object)}.
     * <br>
     * This is an O(n) operation.
     *
     * @return index of element, or -1 if not found
     */
    public final int lastIndexOf(Object o) {
        return indexOf(o);
    }

    public final ListIterator listIterator() {
        return data.listIterator();
    }

    public final ListIterator listIterator(int index) {
        return data.listIterator(index);
    }

    public final List subList(int fromIndex, int toIndex) {
        return data.subList(fromIndex, toIndex);
    }

    //
    // ArrayHashSet
    //

    /**
     * @return a shallow copy of this ArrayHashSet's ArrayList, elements are not copied.
     */
    public final ArrayList toArrayList() {
        return (ArrayList) data.clone();
    }

    /**
     * Identity method allowing to get the identical object, using the internal hash map.
     * <br>
     * This is an O(1) operation.
     * 
     * @param key hash source to find the identical Object within this list
     * @return object from this list, identical to the given <code>key</code> hash code,
     * or null if not contained
     */
    public final Object get(Object key) {
        return map.get(key);
    }

    /**
     * Identity method allowing to get the identical object, using the internal hash map.<br>
     * If the <code>key</code> is not yet contained, add it.
     * <br>
     * This is an O(1) operation.
     *
     * @param key hash source to find the identical Object within this list
     * @return object from this list, identical to the given <code>key</code> hash code,
     * or add the given <code>key</code> and return it.
     */
    public final Object getOrAdd(Object key) {
        Object identity = get(key);
        if(null == identity) {
            // object not contained yet, add it
            if(!this.add(key)) {
                throw new InternalError("Key not mapped, but contained in list: "+key);
            }
            identity = key;
        }
        return identity;
    }

    /**
     * Test for containment
     * <br>
     * This is an O(n) operation, using equals operation over the list.
     * <br>
     * You may utilize this method to verify your hash values,<br>
     * ie {@link #contains(java.lang.Object)} and {@link #containsSafe(java.lang.Object)}
     * shall have the same result.<br>
     *
     * @return true if the given element is contained by this list using slow equals operation,
     *         otherwise false.
     */
    public final boolean containsSafe(Object element) {
        return data.contains(element);
    }

}
