/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.gluegen;

import java.nio.*;

import com.jogamp.gluegen.cgram.types.*;

/**
 * Describes a java-side representation of a type that is used to represent
 * the same data on both the Java-side and C-side during a JNI operation. Also
 * contains some utility methods for creating common types.
 */
public class JavaType {

 /*
  * Represents C arrays that will / can be represented
  * with NIO buffers (resolved down to another JavaType later in processing)
  */
  private enum C_PTR {
      VOID, CHAR, SHORT, INT32, INT64, FLOAT, DOUBLE;
  }

  private final Class<?> clazz; // Primitive types and other types representable as Class objects
  private final String structName;  // Types we're generating glue code for (i.e., C structs)
  private final Type   elementType; // Element type if this JavaType represents a C array
  private final C_PTR  primitivePointerType;
  private final boolean opaqued;

  private static JavaType nioBufferType;
  private static JavaType nioByteBufferType;
  private static JavaType nioShortBufferType;
  private static JavaType nioIntBufferType;
  private static JavaType nioLongBufferType;
  private static JavaType nioPointerBufferType;
  private static JavaType nioFloatBufferType;
  private static JavaType nioDoubleBufferType;
  private static JavaType nioByteBufferArrayType;

  @Override
  public boolean equals(final Object arg) {
    if ((arg == null) || (!(arg instanceof JavaType))) {
      return false;
    }
    final JavaType t = (JavaType) arg;
    return (this == t ||
            (t.clazz == clazz &&
             ((structName == null ? t.structName == null : structName.equals(t.structName)) ||
              ((structName != null) && (t.structName != null) && (structName.equals(t.structName)))) &&
             ((elementType == t.elementType) ||
              (elementType != null) && (t.elementType != null) && (elementType.equals(t.elementType))) &&
             (primitivePointerType == t.primitivePointerType)));
  }

  @Override
  public int hashCode() {
    if (clazz == null) {
      if (structName == null) {
        return 0;
      }
      return structName.hashCode();
    }
    return clazz.hashCode();
  }

  public JavaType getElementType() {
       return new JavaType(elementType);
  }
  public Type getElementCType() {
       return elementType;
  }

  /** Creates a JavaType corresponding to the given opaque Java type. This
      can be used to represent arrays of primitive values or Strings;
      the emitters understand how to perform proper conversion from
      the corresponding C type. */
  public static JavaType createForOpaqueClass(final Class<?> clazz) {
    return new JavaType(clazz, true);
  }

  /** Creates a JavaType corresponding to the given Java type. This
      can be used to represent arrays of primitive values or Strings;
      the emitters understand how to perform proper conversion from
      the corresponding C type. */
  public static JavaType createForClass(final Class<?> clazz) {
    return new JavaType(clazz, false);
  }

  /** Creates a JavaType corresponding to the specified C CompoundType
      name; for example, if "Foo" is supplied, then this JavaType
      represents a "Foo *" by way of a StructAccessor. */
  public static JavaType createForCStruct(final String name) {
    return new JavaType(name);
  }

  /** Creates a JavaType corresponding to an array of the given
      element type. This is used to represent arrays of "Foo **" which
      should be mapped to Foo[] in Java. */
  public static JavaType createForCArray(final Type elementType) {
    return new JavaType(elementType);
  }

  public static JavaType createForCVoidPointer() {
    return new JavaType(C_PTR.VOID);
  }

  public static JavaType createForCCharPointer() {
    return new JavaType(C_PTR.CHAR);
  }

  public static JavaType createForCShortPointer() {
    return new JavaType(C_PTR.SHORT);
  }

  public static JavaType createForCInt32Pointer() {
    return new JavaType(C_PTR.INT32);
  }

  public static JavaType createForCInt64Pointer() {
    return new JavaType(C_PTR.INT64);
  }

  public static JavaType createForCFloatPointer() {
    return new JavaType(C_PTR.FLOAT);
  }

  public static JavaType createForCDoublePointer() {
    return new JavaType(C_PTR.DOUBLE);
  }

  public static JavaType createForJNIEnv() {
    return createForCStruct("JNIEnv");
  }

  public static JavaType forNIOBufferClass() {
    if (nioBufferType == null) {
      nioBufferType = createForClass(java.nio.Buffer.class);
    }
    return nioBufferType;
  }

  public static JavaType forNIOByteBufferClass() {
    if (nioByteBufferType == null) {
      nioByteBufferType = createForClass(java.nio.ByteBuffer.class);
    }
    return nioByteBufferType;
  }

  public static JavaType forNIOShortBufferClass() {
    if (nioShortBufferType == null) {
      nioShortBufferType = createForClass(java.nio.ShortBuffer.class);
    }
    return nioShortBufferType;
  }

  public static JavaType forNIOIntBufferClass() {
    if (nioIntBufferType == null) {
      nioIntBufferType = createForClass(java.nio.IntBuffer.class);
    }
    return nioIntBufferType;
  }

  public static JavaType forNIOLongBufferClass() {
    if (nioLongBufferType == null) {
      nioLongBufferType = createForClass(java.nio.LongBuffer.class);
    }
    return nioLongBufferType;
  }

  public static JavaType forNIOPointerBufferClass()  {
    if(nioPointerBufferType == null)
        nioPointerBufferType = createForClass(com.jogamp.common.nio.PointerBuffer.class);
    return nioPointerBufferType;
  }

  public static JavaType forNIOFloatBufferClass() {
    if (nioFloatBufferType == null) {
      nioFloatBufferType = createForClass(java.nio.FloatBuffer.class);
    }
    return nioFloatBufferType;
  }

  public static JavaType forNIODoubleBufferClass() {
    if (nioDoubleBufferType == null) {
      nioDoubleBufferType = createForClass(java.nio.DoubleBuffer.class);
    }
    return nioDoubleBufferType;
  }

  public static JavaType forNIOByteBufferArrayClass() {
    if (nioByteBufferArrayType == null) {
      final ByteBuffer[] tmp = new ByteBuffer[0];
      nioByteBufferArrayType = createForClass(tmp.getClass());
    }
    return nioByteBufferArrayType;
  }

  /**
   * Returns the Java Class corresponding to this type. Returns null if this
   * object corresponds to a C primitive array type.
   */
  public Class<?> getJavaClass() {
    return clazz;
  }

  /**
   * Returns the Java type name corresponding to this type.
   */
  public String getName() {
    if (clazz != null) {
      if (clazz.isArray()) {
        return arrayName(clazz);
      }
      return clazz.getName();
    }
    if (elementType != null) {
      return elementType.getName();
    }
    return structName;
  }

  /**
   * Returns the descriptor (internal type signature) corresponding to
   * this type.
   */
  public String getDescriptor() {
    // FIXME: this is not completely accurate at this point (for
    // example, it knows nothing about the packages for compound
    // types)
    if (clazz != null) {
      return descriptor(clazz);
    }
    if (elementType != null) {
      if(elementType.getName()==null) {
           throw new RuntimeException("elementType.name is null: "+getDebugString());
      }
      return "[" + descriptor(elementType.getName());
    }
    if( null != structName ) {
        return descriptor(structName);
    }
    return "ANON_NIO";
  }

  /** Returns the String corresponding to the JNI type for this type,
      or NULL if it can't be represented (i.e., it's a boxing class
      that we need to call getBuffer() on.) */
  public String jniTypeName() {
    if (isCompoundTypeWrapper()) {
      // These are sent down as Buffers (e.g., jobject)
      return "jobject";
    }

    if (isArrayOfCompoundTypeWrappers()) {
      // These are returned as arrays of ByteBuffers (e.g., jobjectArray)
      return "jobjectArray /* of ByteBuffer */";
    }

    if (clazz == null) {
      return null;
    }

    if (isVoid()) {
      return "void";
    }

    if (isPrimitive()) {
      return "j" + clazz.getName();
    }

    if (isPrimitiveArray() || isNIOBuffer()) {
      // We now pass primitive arrays and buffers uniformly down to native code as java.lang.Object.
      return "jobject";
    }

    if (isArray()) {
      if (isStringArray()) {
        return "jobjectArray /*elements are String*/";
      }

      final Class<?> elementType = clazz.getComponentType();

      if (isNIOBufferArray()) {
        return "jobjectArray /*elements are " + elementType.getName() + "*/";
      }

      if (elementType.isArray()) {
        // Type is array-of-arrays-of-something

        if (elementType.getComponentType().isPrimitive()) {
          // Type is an array-of-arrays-of-primitive
          return "jobjectArray /* elements are " + elementType.getComponentType() + "[]*/";
          //return "jobjectArray";
        } else {
          throw new RuntimeException("Multi-dimensional arrays of types that are not primitives or Strings are not supported.");
        }
      }

      // Some unusual type that we don't handle
      throw new RuntimeException("Unexpected and unsupported array type: \"" + this + "\"");
    }

    if (isString()) {
      return "jstring";
    }

    return "jobject";
  }

  public boolean isOpaqued() { return opaqued; }

  public boolean isNIOBuffer() {
    return clazz != null && ( java.nio.Buffer.class.isAssignableFrom(clazz) ||
                              com.jogamp.common.nio.NativeBuffer.class.isAssignableFrom(clazz)) ;
  }

  public boolean isNIOByteBuffer() {
    return (clazz == java.nio.ByteBuffer.class);
  }

  public boolean isNIOByteBufferArray() {
    return (this == nioByteBufferArrayType);
  }

  public boolean isNIOBufferArray() {
    return (isArray() && (java.nio.Buffer.class.isAssignableFrom(clazz.getComponentType())));
  }

  public boolean isNIOLongBuffer() {
    return (clazz == java.nio.LongBuffer.class);
  }

  public boolean isNIOPointerBuffer()  {
    return (clazz == com.jogamp.common.nio.PointerBuffer.class);
  }

  public boolean isString() {
    return (clazz == java.lang.String.class);
  }

  public boolean isArray() {
    return ((clazz != null) && clazz.isArray());
  }

  public boolean isFloatArray() {
     return (clazz != null && clazz.isArray() && clazz.getComponentType() == Float.TYPE);
  }

  public boolean isDoubleArray() {
     return (clazz != null && clazz.isArray() && clazz.getComponentType() == Double.TYPE);
  }

  public boolean isByteArray() {
     return (clazz != null && clazz.isArray() && clazz.getComponentType() == Byte.TYPE);
  }

  public boolean isIntArray() {
     return (clazz != null && clazz.isArray() && clazz.getComponentType() == Integer.TYPE);
  }

  public boolean isShortArray() {
     return (clazz != null && clazz.isArray() && clazz.getComponentType() == Short.TYPE);
  }

  public boolean isLongArray() {
     return (clazz != null && clazz.isArray() && clazz.getComponentType() == Long.TYPE);
  }

  public boolean isStringArray() {
     return (clazz != null && clazz.isArray() && clazz.getComponentType() == java.lang.String.class);
  }


  public boolean isPrimitive() {
    return ((clazz != null) && !isArray() && clazz.isPrimitive() && (clazz != Void.TYPE));
  }

  public boolean isPrimitiveArray() {
    return (isArray() && (clazz.getComponentType().isPrimitive()));
  }

  public boolean isShort() {
    return (clazz == Short.TYPE);
  }

  public boolean isFloat() {
    return (clazz == Float.TYPE);
  }

  public boolean isDouble() {
    return (clazz == Double.TYPE);
  }

  public boolean isByte() {
    return (clazz == Byte.TYPE);
  }

  public boolean isLong() {
    return (clazz == Long.TYPE);
  }

  public boolean isInt() {
    return (clazz == Integer.TYPE);
  }

  public boolean isVoid() {
    return (clazz == Void.TYPE);
  }

  public boolean isCompoundTypeWrapper() {
    return (clazz == null && structName != null && !isJNIEnv());
  }

  public boolean isArrayOfCompoundTypeWrappers() {
    return elementType != null;
  }


  public boolean isCPrimitivePointerType() {
    return primitivePointerType != null;
  }

  public boolean isCVoidPointerType() {
    return C_PTR.VOID.equals(primitivePointerType);
  }

  public boolean isCCharPointerType() {
    return C_PTR.CHAR.equals(primitivePointerType);
  }

  public boolean isCShortPointerType() {
    return C_PTR.SHORT.equals(primitivePointerType);
  }

  public boolean isCInt32PointerType() {
    return C_PTR.INT32.equals(primitivePointerType);
  }

  public boolean isCInt64PointerType() {
    return C_PTR.INT64.equals(primitivePointerType);
  }

  public boolean isCFloatPointerType() {
    return C_PTR.FLOAT.equals(primitivePointerType);
  }

  public boolean isCDoublePointerType() {
    return C_PTR.DOUBLE.equals(primitivePointerType);
  }

  public boolean isJNIEnv() {
    return clazz == null && "JNIEnv".equals(structName);
  }

  @Override
  public Object clone() {
    return new JavaType(primitivePointerType, clazz, structName, elementType);
  }

  @Override
  public String toString() {
    return getName();
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void append(final StringBuilder sb, final String val, final boolean prepComma) {
      if( prepComma ) {
          sb.append(", ");
      }
      sb.append(val);
  }
  // For debugging
  public String getDebugString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("JType[");
    boolean prepComma = false;
    {
        final String javaTypeName = getName();
        if( null != javaTypeName ) {
            append(sb, javaTypeName, false);
        } else {
            append(sb, "ANON", false);
        }
        sb.append(" / ");
        if( null != structName ) {
            append(sb, "'"+structName+"'", prepComma); prepComma=true;
        } else {
            append(sb, "NIL", prepComma); prepComma=true;
        }
    }
    if( null != clazz ) {
        append(sb, "clazz = "+clazz.getName(), prepComma); prepComma=true;
    }
    if( null != elementType ) {
        append(sb, "elementType = "+elementType, prepComma); prepComma=true;
    }
    if( null != primitivePointerType ) {
        append(sb, "primitivePointerType = "+primitivePointerType, prepComma); prepComma=true;
    }
    append(sb, "is[", prepComma); prepComma=false;
    {
        if( isOpaqued() ) {
            append(sb, "opaque", prepComma); prepComma=true;
        }
        if( isArray() ) {
            append(sb, "array", prepComma); prepComma=true;
        }
        if( isArrayOfCompoundTypeWrappers() ) {
            append(sb, "compoundArray", prepComma); prepComma=true;
        }
        if( isCompoundTypeWrapper() ) {
            append(sb, "compound", prepComma); prepComma=true;
        }
        if( isArray() ) {
            append(sb, "array", prepComma); prepComma=true;
        }
        if( isPrimitive() ) {
            append(sb, "primitive", prepComma); prepComma=true;
        }
        if( isPrimitiveArray() ) {
            append(sb, "primitiveArray", prepComma); prepComma=true;
        }
        if( isNIOBuffer() ) {
            append(sb, "nioBuffer", prepComma); prepComma=true;
        }
        if( isNIOBufferArray() ) {
            append(sb, "nioBufferArray", prepComma); prepComma=true;
        }
        if( isCPrimitivePointerType() ) {
            append(sb, "C-Primitive-Pointer", prepComma); prepComma=true;
        }
    }
    append(sb, "], descriptor '"+getDescriptor()+"']", prepComma); prepComma=true;
    return sb.toString();
  }

  /**
   * Constructs a representation for a type corresponding to the given Class
   * argument.
   */
  private JavaType(final Class<?> clazz, final boolean opaqued) {
    this.primitivePointerType = null;
    this.clazz = clazz;
    this.structName = null;
    this.elementType = null;
    this.opaqued = opaqued;
  }

  /** Constructs a type representing a named C struct. */
  private JavaType(final String structName) {
    this.primitivePointerType = null;
    this.clazz = null;
    this.structName = structName;
    this.elementType = null;
    this.opaqued = false;
  }

  /** Constructs a type representing a pointer to a C primitive
      (integer, floating-point, or void pointer) type. */
  private JavaType(final C_PTR primitivePointerType) {
    this.primitivePointerType = primitivePointerType;
    this.clazz = null;
    this.structName = null;
    this.elementType = null;
    this.opaqued = false;
  }

  /** Constructs a type representing an array of C pointers. */
  private JavaType(final Type elementType) {
    this.primitivePointerType = null;
    this.clazz = null;
    this.structName = null;
    this.elementType = elementType;
    this.opaqued = false;
  }

  /** clone only */
  private JavaType(final C_PTR primitivePointerType, final Class<?> clazz, final String name, final Type elementType) {
    this.primitivePointerType = primitivePointerType;
    this.clazz = clazz;
    this.structName = name;
    this.elementType = elementType;
    this.opaqued = false;
  }

  private String arrayName(Class<?> clazz) {
    final StringBuilder buf = new StringBuilder();
    int arrayCount = 0;
    while (clazz.isArray()) {
      ++arrayCount;
      clazz = clazz.getComponentType();
    }
    buf.append(clazz.getName());
    while (--arrayCount >= 0) {
      buf.append("[]");
    }
    return buf.toString();
  }

  private String arrayDescriptor(Class<?> clazz) {
    final StringBuilder buf = new StringBuilder();
    while (clazz.isArray()) {
      buf.append("[");
      clazz = clazz.getComponentType();
    }
    buf.append(descriptor(clazz));
    return buf.toString();
  }

  private String descriptor(final Class<?> clazz) {
    if (clazz.isPrimitive()) {
      if (clazz == Boolean.TYPE) return "Z";
      if (clazz == Byte.TYPE)    return "B";
      if (clazz == Double.TYPE)  return "D";
      if (clazz == Float.TYPE)   return "F";
      if (clazz == Integer.TYPE) return "I";
      if (clazz == Long.TYPE)    return "J";
      if (clazz == Short.TYPE)   return "S";
      if (clazz == Void.TYPE)    return "V";
      throw new RuntimeException("Unexpected primitive type " + clazz.getName());
    }
    if (clazz.isArray()) {
      return arrayDescriptor(clazz);
    }
    return descriptor(clazz.getName());
  }

  private String descriptor(final String referenceTypeName) {
    return "L" + referenceTypeName.replace('.', '/') + ";";
  }
}
