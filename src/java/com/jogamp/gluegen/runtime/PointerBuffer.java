/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package com.jogamp.gluegen.runtime;

import java.nio.*;

public class PointerBuffer {
  private ByteBuffer   bb;
  private Buffer       pb;
  private int capacity, position;
  private long[] backup;

  private PointerBuffer(ByteBuffer bb) {
    this.bb = bb;

    if(CPU.is32Bit()) {
       this.pb = bb.asIntBuffer();
    }else{
       this.pb = bb.asLongBuffer();
    }

    capacity = bb.capacity() / elementSize();

    position=0;
    backup = new long[capacity];
  }

  public final int limit() {
    return capacity;
  }
  public final int capacity() {
    return capacity;
  }

  public final int position() {
    return position;
  }

  public final PointerBuffer position(int newPos) {
    if(0>newPos || newPos>=capacity) {
        throw new IndexOutOfBoundsException();
    }
    position = newPos;
    return this;
  }

  public final int remaining() {
    return capacity - position;
  }

  public final boolean hasRemaining() {
    return position < capacity;
  }

  public final PointerBuffer rewind() {
    position=0;
    return this;
  }

  int   arrayOffset() { return 0; }

  boolean   hasArray() { return true; }

  public long[] array() {
    return backup;
  }

  public static PointerBuffer allocate(int size) {
      return new PointerBuffer(ByteBuffer.wrap(new byte[elementSize()* size]));
  }

  public static PointerBuffer allocateDirect(int size) {
      return new PointerBuffer(BufferFactory.newDirectByteBuffer(elementSize() * size));
  }

  public static PointerBuffer wrap(ByteBuffer src) {
    PointerBuffer res =  new PointerBuffer(src);
    res.updateBackup();
    return res;
  }

  public ByteBuffer getBuffer() {
    return bb;
  }

  public boolean isDirect() {
    return bb.isDirect();
  }

  public long get(int idx) {
    if(0>idx || idx>=capacity) {
        throw new IndexOutOfBoundsException();
    }
    if(CPU.is32Bit()) {
        return ((IntBuffer)pb).get(idx);
    } else {
        return ((LongBuffer)pb).get(idx);
    }
  }

  public long get() {
    long r = get(position);
    position++;
    return r;
  }

  public PointerBuffer put(int idx, long v) {
    if(0>idx || idx>=capacity) {
        throw new IndexOutOfBoundsException();
    }
    backup[idx] = v;
    if(CPU.is32Bit()) {
        ((IntBuffer)pb).put(idx, (int)v);
    } else {
        ((LongBuffer)pb).put(idx, v);
    }
    return this;
  }

  public PointerBuffer put(long v) {
    put(position, v);
    position++;
    return this;
  }

  private void updateBackup() {
    for (int i = 0; i < capacity; i++) {
      backup[i] = get(i);
    }
  }

  public static int elementSize() {
    return CPU.is32Bit() ? BufferFactory.SIZEOF_INT : BufferFactory.SIZEOF_LONG;
  }
}
