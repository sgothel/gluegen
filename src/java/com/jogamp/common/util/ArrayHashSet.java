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
 *  <li> Java 1.5 compatible</li>
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
public class ArrayHashSet<E>
    implements Cloneable, Collection<E>, List<E>
{
    /**
     * Default load factor: {@value}
     */
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    /**
     * The default initial capacity: {@value}
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 16;

    private final HashMap<E,E> map; // object -> object
    private final ArrayList<E> data; // list of objects
    private final boolean supportNullValue;

    /**
     * @deprecated Use {@link #ArrayHashSet(boolean, int, float)}
     */
    public ArrayHashSet() {
        this(true, DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * @param initialCapacity
     * @deprecated Use {@link #ArrayHashSet(boolean, int, float)}
     */
    public ArrayHashSet(final int initialCapacity) {
        this(true, initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * @param initialCapacity
     * @param loadFactor
     * @deprecated Use {@link #ArrayHashSet(boolean, int, float)}
     */
    public ArrayHashSet(final int initialCapacity, final float loadFactor) {
        this(true, initialCapacity, loadFactor);
    }

    /**
     *
     * @param supportNullValue Use {@code true} for default behavior, i.e. {@code null} can be a valid value.
     *                         Use {@code false} if {@code null} is not a valid value,
     *                         here {@link #remove(E)} and {@link #getOrAdd(Object)} will be optimized.
     * @param initialCapacity use {@link #DEFAULT_INITIAL_CAPACITY} for default
     * @param loadFactor use {@link #DEFAULT_LOAD_FACTOR} for default
     * @see #supportsNullValue()
     */
    public ArrayHashSet(final boolean supportNullValue, final int initialCapacity, final float loadFactor) {
        this.map  = new HashMap<E,E>(initialCapacity, loadFactor);
        this.data = new ArrayList<E>(initialCapacity);
        this.supportNullValue = supportNullValue;
    }

    /**
     * @return a shallow copy of this ArrayHashSet, elements are not copied.
     */
    public ArrayHashSet(final ArrayHashSet<E> o) {
        map = new HashMap<E, E>(o.map);
        data = new ArrayList<E>(o.data);
        supportNullValue = o.supportNullValue;
    }

    /**
     * Returns {@code true} for default behavior, i.e. {@code null} can be a valid value.
     * <p>
     * Returns {@code false} if {@code null} is not a valid value,
     * here {@link #remove(E)} and {@link #getOrAdd(Object)} are optimized operations.
     * </p>
     * @see #ArrayHashSet(boolean, int, float)
     */
    public final boolean supportsNullValue() { return supportNullValue; }

    //
    // Cloneable
    //

    /**
     * @return a shallow copy of this ArrayHashSet, elements are not copied.
     */
    @Override
    public final Object clone() {
        return new ArrayHashSet<E>(this);
    }

    /** Returns this object ordered ArrayList. Use w/ care, it's not a copy. */
    public final ArrayList<E> getData() { return data; }
    /** Returns this object hash map. Use w/ care, it's not a copy. */
    public final HashMap<E,E> getMap() { return map; }

    @Override
    public final String toString() { return data.toString(); }

    //
    // Collection
    //

    @Override
    public final void clear() {
        data.clear();
        map.clear();
    }

    /**
     * Add element at the end of this list, if it is not contained yet.
     * <br>
     * This is an O(1) operation
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return true if the element was added to this list,
     *         otherwise false (already contained).
     * @throws NullPointerException if {@code element} is {@code null} but {@link #supportsNullValue()} == {@code false}
     */
    @Override
    public final boolean add(final E element) throws NullPointerException {
        if( !supportNullValue ) {
            checkNull(element);
        }
        if( !map.containsKey(element) ) {
            // !exists
            if(null != map.put(element, element)) {
                // slips a valid null ..
                throw new InternalError("Already existing, but checked before: "+element);
            }
            if(!data.add(element)) {
                throw new InternalError("Couldn't add element: "+element);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove element from this list.
     * <br>
     * This is an O(1) operation, in case the element does not exist,
     * otherwise O(n).
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return true if the element was removed from this list,
     *         otherwise false (not contained).
     * @throws NullPointerException if {@code element} is {@code null} but {@link #supportsNullValue()} == {@code false}
     */
    @Override
    public final boolean remove(final Object element) throws NullPointerException {
        if( supportNullValue ) {
            if( map.containsKey(element) ) {
                // exists
                map.remove(element);
                if ( !data.remove(element) ) {
                    throw new InternalError("Couldn't remove prev mapped element: "+element);
                }
                return true;
            }
        } else {
            checkNull(element);
            if ( null != map.remove(element) ) {
                // exists
                if ( !data.remove(element) ) {
                    throw new InternalError("Couldn't remove prev mapped element: "+element);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Add all elements of given {@link java.util.Collection} at the end of this list.
     * <br>
     * This is an O(n) operation, over the given Collection size.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return true if at least one element was added to this list,
     *         otherwise false (completely container).
     */
    @Override
    public final boolean addAll(final Collection<? extends E> c) {
        boolean mod = false;
        for (final E o : c) {
            mod |= add(o);
        }
        return mod;
    }

    /**
     * Test for containment
     * <br>
     * This is an O(1) operation.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return true if the given element is contained by this list using fast hash map,
     *         otherwise false.
     */
    @Override
    public final boolean contains(final Object element) {
        return map.containsKey(element);
    }

    /**
     * Test for containment of given {@link java.util.Collection}
     * <br>
     * This is an O(n) operation, over the given Collection size.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return true if the given Collection is completly contained by this list using hash map,
     *         otherwise false.
     */
    @Override
    public final boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!this.contains(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove all elements of given {@link java.util.Collection} from this list.
     * <br>
     * This is an O(n) operation.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return true if at least one element of this list was removed,
     *         otherwise false.
     */
    @Override
    public final boolean removeAll(final Collection<?> c) {
        boolean mod = false;
        for (final Object o : c) {
            mod |= this.remove(o);
        }
        return mod;
    }

    /**
     * Retain all elements of the given {@link java.util.Collection} c, ie
     * remove all elements not contained by the given {@link java.util.Collection} c.
     * <br>
     * This is an O(n) operation.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return true if at least one element of this list was removed,
     *         otherwise false.
     */
    @Override
    public final boolean retainAll(final Collection<?> c) {
        boolean mod = false;
        for (final Object o : c) {
            if (!c.contains(o)) {
                mod |= this.remove(o);
            }
        }
        return mod;
    }

    /**
     * This is an O(n) operation.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return true if arrayHashSet is of type ArrayHashSet and all entries are equal
     * Performance: arrayHashSet(1)
     */
    @Override
    public final boolean equals(final Object arrayHashSet) {
        if ( !(arrayHashSet instanceof ArrayHashSet) ) {
            return false;
        }
        return data.equals(((ArrayHashSet<?>)arrayHashSet).data);
    }

    /**
     * This is an O(n) operation over the size of this list.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return the hash code of this list as define in {@link java.util.List#hashCode()},
     * ie hashing all elements of this list.
     */
    @Override
    public final int hashCode() {
        return data.hashCode();
    }

    @Override
    public final boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public final Iterator<E> iterator() {
        return data.iterator();
    }

    @Override
    public final int size() {
        return data.size();
    }

    @Override
    public final Object[] toArray() {
        return data.toArray();
    }

    @Override
    public final <T> T[] toArray(final T[] a) {
        return data.toArray(a);
    }

    //
    // List
    //

    @Override
    public final E get(final int index) {
        return data.get(index);
    }

    @Override
    public final int indexOf(final Object element) {
        return data.indexOf(element);
    }

    /**
     * Add element at the given index in this list, if it is not contained yet.
     * <br>
     * This is an O(1) operation
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @throws IllegalArgumentException if the given element was already contained
     * @throws NullPointerException if {@code element} is {@code null} but {@link #supportsNullValue()} == {@code false}
     */
    @Override
    public final void add(final int index, final E element) throws IllegalArgumentException, NullPointerException {
        if( !supportNullValue ) {
            checkNull(element);
        }
        if ( map.containsKey(element) ) {
            throw new IllegalArgumentException("Element "+element+" is already contained");
        }
        if(null != map.put(element, element)) {
            // slips a valid null ..
            throw new InternalError("Already existing, but checked before: "+element);
        }
        // !exists
        data.add(index, element);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * @throws UnsupportedOperationException
     */
    @Override
    public final boolean addAll(final int index, final Collection<? extends E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public final E set(final int index, final E element) {
        final E old = remove(index);
        if(null!=old) {
            add(index, element);
        }
        return old;
    }

    /**
     * Remove element at given index from this list.
     * <br>
     * This is an O(n) operation.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return the removed object
     */
    @Override
    public final E remove(final int index) {
        final E o = get(index);
        if( null!=o && remove(o) ) {
            return o;
        }
        return null;
    }

    /**
     * Since this list is unique, equivalent to {@link #indexOf(java.lang.Object)}.
     * <br>
     * This is an O(n) operation.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return index of element, or -1 if not found
     */
    @Override
    public final int lastIndexOf(final Object o) {
        return indexOf(o);
    }

    @Override
    public final ListIterator<E> listIterator() {
        return data.listIterator();
    }

    @Override
    public final ListIterator<E> listIterator(final int index) {
        return data.listIterator(index);
    }

    @Override
    public final List<E> subList(final int fromIndex, final int toIndex) {
        return data.subList(fromIndex, toIndex);
    }

    //
    // ArrayHashSet
    //

    /**
     * @return a shallow copy of this ArrayHashSet's ArrayList, elements are not copied.
     */
    public final ArrayList<E> toArrayList() {
        return new ArrayList<E>(data);
    }

    /**
     * Identity method allowing to get the identical object, using the internal hash map.
     * <br>
     * This is an O(1) operation.
     *
     * @param element hash source to find the identical Object within this list
     * @return object from this list, identical to the given <code>key</code> hash code,
     * or null if not contained
     */
    public final E get(final Object element) {
        return map.get(element);
    }

    /**
     * Identity method allowing to get the identical object, using the internal hash map.<br>
     * If the <code>element</code> is not yet contained, add it.
     * <br>
     * This is an O(1) operation.
     *
     * @param element hash source to find the identical Object within this list
     * @return object from this list, identical to the given <code>key</code> hash code,
     * or add the given <code>key</code> and return it.
     * @throws NullPointerException if {@code element} is {@code null} but {@link #supportsNullValue()} == {@code false}
     */
    public final E getOrAdd(final E element) throws NullPointerException {
        if( supportNullValue ) {
            if( map.containsKey(element) ) {
                // existent
                return map.get(element);
            }
        } else {
            checkNull(element);
            final E identity = map.get(element);
            if(null != identity) {
                // existent
                return identity;
            }
        }
        // !existent
        if(!this.add(element)) {
            throw new InternalError("Element not mapped, but contained in list: "+element);
        }
        return element;
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
    public final boolean containsSafe(final Object element) {
        return data.contains(element);
    }

    private static final void checkNull(final Object element) throws NullPointerException {
        if( null == element ) {
            throw new NullPointerException("Null element not supported");
        }
    }
}
