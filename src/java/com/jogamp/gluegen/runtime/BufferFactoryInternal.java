/*

 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.gluegen.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.*;
import sun.misc.Unsafe;

public class BufferFactoryInternal {
  private static final long        addressFieldOffset;
  private static final Constructor directByteBufferConstructor;

  static {
    try {
      Field f = Buffer.class.getDeclaredField("address");
      addressFieldOffset = UnsafeAccess.getUnsafe().objectFieldOffset(f);

      Class directByteBufferClass = Class.forName("java.nio.DirectByteBuffer");
      directByteBufferConstructor = directByteBufferClass.getDeclaredConstructor(new Class[] { Long.TYPE, Integer.TYPE });
      directByteBufferConstructor.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static long getDirectBufferAddress(Buffer buf) {
    return ((buf == null) ? 0 : UnsafeAccess.getUnsafe().getLong(buf, addressFieldOffset));
  }

  public static ByteBuffer newDirectByteBuffer(long address, int capacity) {
    try {
      if (address == 0) {
        return null;
      }
      return (ByteBuffer) directByteBufferConstructor.newInstance(new Object[] { new Long(address), new Integer(capacity) });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static long newCString(String str) {
    byte[] strBytes = str.getBytes();
    long   strBlock = UnsafeAccess.getUnsafe().allocateMemory(strBytes.length+1);
    for (int i = 0; i < strBytes.length; i++) {
      UnsafeAccess.getUnsafe().putByte(strBlock+i, strBytes[i]);
    }
    UnsafeAccess.getUnsafe().putByte(strBlock+strBytes.length, (byte)0); // null termination
    return strBlock;
  }

  public static void freeCString(long cStr) {
    UnsafeAccess.getUnsafe().freeMemory(cStr);
  }

  public static String newJavaString(long cStr) {
    if (cStr == 0) {
      return null;
    }
    int numChars = 0;
    while (UnsafeAccess.getUnsafe().getByte(cStr + numChars) != 0) {
      ++numChars;
    }
    byte[] bytes = new byte[numChars];
    for (int i = 0; i < numChars; i++) {
      bytes[i] = UnsafeAccess.getUnsafe().getByte(cStr + i);
    }
    return new String(bytes);
  }

  public static int arrayBaseOffset(Object array) {
    return UnsafeAccess.getUnsafe().arrayBaseOffset(array.getClass());
  }
  public static int arrayIndexScale(Object array) {
    return UnsafeAccess.getUnsafe().arrayIndexScale(array.getClass());
  }
}
