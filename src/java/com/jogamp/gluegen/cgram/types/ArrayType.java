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

package com.jogamp.gluegen.cgram.types;

import com.jogamp.gluegen.ASTLocusTag;

/** Represents an array type. This differs from a pointer type in C
    syntax by the use of "[]" rather than "*". The length may or may
    not be known; if the length is unknown then a negative number
    should be passed in to the constructor. */

public class ArrayType extends MemoryLayoutType implements Cloneable {
  private final Type elementType;
  private final int length;

  public ArrayType(final Type elementType, final SizeThunk sizeInBytes, final int length,
                   final int cvAttributes) {
    this(elementType, sizeInBytes, length, cvAttributes, null);
  }
  public ArrayType(final Type elementType, final SizeThunk sizeInBytes, final int length,
                   final int cvAttributes, final ASTLocusTag astLocus) {
    super(elementType.getName() + " *", sizeInBytes, cvAttributes, astLocus);
    this.elementType = elementType;
    this.length      = length;
  }
  private ArrayType(final ArrayType o, final int cvAttributes, final ASTLocusTag astLocus) {
    super(o, cvAttributes, astLocus);
    elementType = o.elementType;
    length      = o.length;
  }

  @Override
  Type newVariantImpl(final boolean newCVVariant, final int cvAttributes, final ASTLocusTag astLocus) {
    return new ArrayType(this, cvAttributes, astLocus);
  }

  @Override
  protected int hashCodeImpl() {
    // 31 * x == (x << 5) - x
    final int hash = elementType.hashCode();
    return ((hash << 5) - hash) + length;
  }

  @Override
  protected boolean equalsImpl(final Type arg) {
    final ArrayType t = (ArrayType) arg;
    return elementType.equals(t.elementType) &&
           length == t.length;
  }

  @Override
  protected int hashCodeSemanticsImpl() {
    // 31 * x == (x << 5) - x
    final int hash = elementType.hashCodeSemantics();
    return ((hash << 5) - hash) + length;
  }

  @Override
  protected boolean equalSemanticsImpl(final Type arg) {
    final ArrayType t = (ArrayType) arg;
    return elementType.equalSemantics(t.elementType) &&
           length == t.length;
  }

  @Override
  public boolean isAnon() { return elementType.isAnon(); }

  @Override
  public String getName(final boolean includeCVAttrs) {
    return elementType.getName() + " *";
  }

  @Override
  public final ArrayType asArray()      { return this; }

  public Type    getElementType() { return elementType; }
  public int     getLength()      { return length;      }
  public boolean hasLength()      { return length >= 0; }

  @Override
  public final Type getBaseElementType() {
    return elementType.getBaseElementType();
  }

  @Override
  public final int arrayDimension() {
    return 1 + elementType.arrayDimension();
  }

  /** Recompute the size of this array if necessary. This needs to be
      done when the base element type is a compound type after layouting. */
  void recomputeSize() {
    final ArrayType arrayElementType = getElementType().asArray();
    if (arrayElementType != null) {
      arrayElementType.recomputeSize();
    }
    super.setSize(SizeThunk.mul(SizeThunk.constant(getLength()), elementType.getSize()));
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(final String variableName) {
    final StringBuilder buf = new StringBuilder();
    if(elementType.isConst()) {
        buf.append("const ");
    }
    buf.append(elementType.getCName());
    if (variableName != null) {
      buf.append(" ");
      buf.append(variableName);
    }
    buf.append("[");
    buf.append(length);
    buf.append("]");
    return buf.toString();
  }

  @Override
  public void visit(final TypeVisitor arg) {
    super.visit(arg);
    elementType.visit(arg);
  }
}
