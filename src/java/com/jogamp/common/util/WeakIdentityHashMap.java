/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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
 *
 * Original source code of this class taken from Apache Avro
 * <https://github.com/apache/avro/blob/master/lang/java/avro/src/main/java/org/apache/avro/util/WeakIdentityHashMap.java>
 * commit 70260919426f89825ca148f5ee815f3b2cf4764d.
 * Up until commit 70260919426f89825ca148f5ee815f3b2cf4764d,
 * this code has been licensed as described below:
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.jogamp.common.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implements a combination of WeakHashMap and IdentityHashMap. Useful for
 * caches that need to key off of a == comparison instead of a .equals.
 *
 * <b> This class is not a general-purpose Map implementation! While this class
 * implements the Map interface, it intentionally violates Map's general
 * contract, which mandates the use of the equals method when comparing objects.
 * This class is designed for use only in the rare cases wherein
 * reference-equality semantics are required.
 *
 * Note that this implementation is not synchronized. </b>
 */
public class WeakIdentityHashMap<K, V> implements Map<K, V> {
  private final ReferenceQueue<K> queue = new ReferenceQueue<>();
  private final Map<IdentityWeakReference<K>, V> backingStore;

  /**
   * See {@link HashMap#HashMap()}
   */
  public WeakIdentityHashMap() {
      backingStore = new HashMap<>();
  }

  /**
   * See {@link HashMap#HashMap(int, float)}
   * <p>
   * Usable slots before resize are {@code capacity * loadFactor}.
   * </p>
   * <p>
   * Capacity for n-slots w/o resize would be {@code (float)n/loadFactor + 1.0f}, see {@link #capacityForRequiredSize(int, float[])}.
   * </p>
   * @param initialCapacity default value would be 16, i.e. 12 slots @ 0.75f loadFactor before resize
   * @param loadFactor default value would be 0.75f
   * @see #capacityForRequiredSize(int, float[])
   * @see #createWithRequiredSize(int, float)
   */
  public WeakIdentityHashMap(final int initialCapacity, final float loadFactor) {
      backingStore = new HashMap<>(initialCapacity, loadFactor);
  }

  /**
   * Static creation method using {@link #capacityForRequiredSize(int, float[])}
   * to instantiate a new {@link WeakIdentityHashMap} via {@link #WeakIdentityHashMap(int, float)}.
   *
   * @param requiredSize the user desired n-slots before resize
   * @param loadFactor given loadFactor, which might be increased a little to avoid next PowerOf2 bloat
   * @return the new {@link WeakIdentityHashMap} instance
   */
  @SuppressWarnings("rawtypes")
  public static WeakIdentityHashMap<?, ?> createWithRequiredSize(final int requiredSize, final float loadFactor) {
      final float[] lf = { loadFactor };
      final int icap = capacityForRequiredSize(requiredSize, lf);
      return new WeakIdentityHashMap(icap, lf[0]);
  }

  /**
   * Returns the [initial] capacity using the given {@code loadFactor}
   * and {@code requiredSize}.
   * <p>
   * General calculation is {@code (float)requiredSize/loadFactor + 1.0f}, using {@code loadFactor := 0.75f}.
   * </p>
   * <p>
   * In case above computed capacity is {@link Bitfield.Util#isPowerOf2(int)},
   * the given {@code loadFactor} will be increased to avoid next PowerOf2 table size initialization.
   * </p>
   * @param requiredSize the user desired n-slots before resize
   * @param loadFactor given loadFactor, which might be increased a little to avoid next PowerOf2 bloat
   * @return the [initial] capacity to be used for {@link #WeakIdentityHashMap(int, float)}
   */
  public static int capacityForRequiredSize(final int requiredSize, final float[] loadFactor) {
      if( requiredSize >= Bitfield.Util.MAX_POWER_OF_2 ) {
          return Integer.MAX_VALUE;
      }
      float lf = loadFactor[0];
      int c0 = (int)( requiredSize/lf + 1.0f );
      if( !Bitfield.Util.isPowerOf2(c0) || 0.86f <= lf ) {
          return c0;
      }
      do {
          lf += 0.01f;
          c0 = (int)( requiredSize/lf + 1.0f );
      } while( Bitfield.Util.isPowerOf2(c0) && 0.86f > lf );

      loadFactor[0] = lf;
      return c0;
  }

  @Override
  public void clear() {
    backingStore.clear();
    reap();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean containsKey(final Object key) {
    reap();
    return backingStore.containsKey(new IdentityWeakReference<K>((K) key, queue));
  }

  @Override
  public boolean containsValue(final Object value) {
    reap();
    return backingStore.containsValue(value);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    reap();
    final Set<Map.Entry<K, V>> ret = new HashSet<>();
    for (final Map.Entry<IdentityWeakReference<K>, V> ref : backingStore.entrySet()) {
      final K key = ref.getKey().get();
      if( null != key ) {
          final V value = ref.getValue();
          final Map.Entry<K, V> entry = new Map.Entry<K, V>() {
            @Override
            public K getKey() {
              return key;
            }

            @Override
            public V getValue() {
              return value;
            }

            @Override
            public V setValue(final V value) {
              throw new UnsupportedOperationException();
            }
          };
          ret.add(entry);
      }
    }
    return Collections.unmodifiableSet(ret);
  }

  @Override
  public Set<K> keySet() {
    reap();
    final Set<K> ret = new HashSet<>();
    for (final IdentityWeakReference<K> ref : backingStore.keySet()) {
      final K key = ref.get();
      if( null != key ) {
          ret.add(key);
      }
    }
    return Collections.unmodifiableSet(ret);
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof WeakIdentityHashMap)) {
      return false;
    }
    return backingStore.equals(((WeakIdentityHashMap<?, ?>) o).backingStore);
  }

  @SuppressWarnings("unchecked")
  @Override
  public V get(final Object key) {
    reap();
    return backingStore.get(new IdentityWeakReference<K>((K) key, queue));
  }

  @Override
  public V put(final K key, final V value) {
    reap();
    return backingStore.put(new IdentityWeakReference<K>(key, queue), value);
  }

  @Override
  public int hashCode() {
    reap();
    return backingStore.hashCode();
  }

  @Override
  public boolean isEmpty() {
    reap();
    return backingStore.isEmpty();
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> t) {
    final int n = t.size();
    if ( 0 < n ) {
        final float[] lf = { 0.75f };
        final int icap = capacityForRequiredSize(n, lf);
        final Map<IdentityWeakReference<K>, V> t2 = new HashMap<>(icap, lf[0]);
        for (final Map.Entry<? extends K, ? extends V> e : t.entrySet()) {
          t2.put(new IdentityWeakReference<K>(e.getKey(), queue), e.getValue());
        }
        backingStore.putAll(t2);
        reap();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public V remove(final Object key) {
    reap();
    return backingStore.remove(new IdentityWeakReference<K>((K) key, queue));
  }

  @Override
  public int size() {
    reap();
    return backingStore.size();
  }

  @Override
  public Collection<V> values() {
    reap();
    return backingStore.values();
  }

  private synchronized void reap() {
    Object zombie = queue.poll();

    while (zombie != null) {
      @SuppressWarnings("unchecked")
      final IdentityWeakReference<K> victim = (IdentityWeakReference<K>) zombie;
      backingStore.remove(victim);
      zombie = queue.poll();
    }
  }

  private static class IdentityWeakReference<K> extends WeakReference<K> {
    final int hash;

    IdentityWeakReference(final K obj, final ReferenceQueue<K> q) {
      super(obj, q);
      hash = System.identityHashCode(obj);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof IdentityWeakReference)) {
        return false;
      }
      @SuppressWarnings("unchecked")
      final IdentityWeakReference<K> ref = (IdentityWeakReference<K>) o;
      return this.get() == ref.get();
    }
  }
}
