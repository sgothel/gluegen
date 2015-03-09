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

/** Represents a bitfield in a struct. */

public class BitType extends IntType implements Cloneable {
  private final IntType underlyingType;
  private final int sizeInBits;
  private final int offset;

  public BitType(final IntType underlyingType, final int sizeInBits, final int lsbOffset,
                 final int cvAttributes, final ASTLocusTag astLocus) {
    super(underlyingType.getName(), underlyingType.getSize(), underlyingType.isUnsigned(), cvAttributes, astLocus);
    this.underlyingType = underlyingType;
    this.sizeInBits = sizeInBits;
    this.offset = lsbOffset;
  }

  private BitType(final BitType o, final int cvAttributes, final ASTLocusTag astLocus) {
      super(o, cvAttributes, astLocus);
      underlyingType = o.underlyingType;
      sizeInBits = o.sizeInBits;
      offset = o.offset;
  }

  @Override
  Type newVariantImpl(final boolean newCVVariant, final int cvAttributes, final ASTLocusTag astLocus) {
    return new BitType(this, cvAttributes, astLocus);
  }

  @Override
  protected int hashCodeImpl() {
      // 31 * x == (x << 5) - x
      int hash = super.hashCodeImpl();
      hash = ((hash << 5) - hash) + underlyingType.hashCode();
      hash = ((hash << 5) - hash) + sizeInBits;
      return ((hash << 5) - hash) + offset;
  }

  @Override
  protected boolean equalsImpl(final Type arg) {
      final BitType t = (BitType) arg;
      return super.equalsImpl(arg) &&
             underlyingType.equals(t.underlyingType) &&
             sizeInBits == t.sizeInBits &&
             offset == t.offset;
  }

  @Override
  protected int hashCodeSemanticsImpl() {
      // 31 * x == (x << 5) - x
      int hash = super.hashCodeSemanticsImpl();
      hash = ((hash << 5) - hash) + underlyingType.hashCodeSemantics();
      hash = ((hash << 5) - hash) + sizeInBits;
      return ((hash << 5) - hash) + offset;
  }

  @Override
  protected boolean equalSemanticsImpl(final Type arg) {
      final BitType t = (BitType) arg;
      return super.equalSemanticsImpl(arg) &&
             underlyingType.equalSemantics(t.underlyingType) &&
             sizeInBits == t.sizeInBits &&
             offset == t.offset;
  }

  @Override
  public BitType asBit() { return this; }

  /** Size in bits of this type. */
  public int getSizeInBits() {
    return sizeInBits;
  }

  /** Offset from the least-significant bit (LSB) of the LSB of this
      type */
  public int getOffset() {
    return offset;
  }

  @Override
  public void visit(final TypeVisitor arg) {
    super.visit(arg);
    underlyingType.visit(arg);
  }
}
