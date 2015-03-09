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

import java.util.*;

import com.jogamp.gluegen.ASTLocusTag;

/** Models all compound types, i.e., those containing fields: structs
    and unions. The boolean type accessors indicate how the type is
    really defined. */

public abstract class CompoundType extends MemoryLayoutType implements Cloneable, AliasedSymbol {
  // The name "foo" in the construct "struct foo { ... }";
  private final String structName;
  private ArrayList<Field> fields;
  private boolean visiting;
  private boolean bodyParsed;

  @Override
  public void rename(final String newName) {
      throw new UnsupportedOperationException();
  }
  @Override
  public void addAliasedName(final String origName) {
      throw new UnsupportedOperationException();
  }
  @Override
  public boolean hasAliases() {
      return false;
  }
  @Override
  public Set<String> getAliasedNames() {
      return null;
  }
  @Override
  public String getAliasedString() {
      return toString();
  }
  @Override
  public String getOrigName() {
      return getName();
  }
  /**
   * @param structName struct name of this CompoundType, i.e. the "foo" in the
                       construct {@code struct foo { int a, ... };} or {@code struct foo;} <i>even</i> for anonymous structs.
   * @param size
   * @param kind
   * @param cvAttributes
   * @return
   */
  public static CompoundType create(final String structName, final SizeThunk size,
                                    final CompoundTypeKind kind, final int cvAttributes,
                                    final ASTLocusTag astLocus)
  {
    final CompoundType res;
    switch (kind) {
      case STRUCT:
          res = new StructType(null, size, cvAttributes, structName, astLocus);
          break;
      case UNION:
          res = new UnionType(null, size, cvAttributes, structName, astLocus);
          break;
      default:
          throw new RuntimeException("OO relation "+kind+" / Compount not yet supported");
    }
    return res;
  }

  CompoundType(final String name, final SizeThunk size, final int cvAttributes,
               final String structName, final ASTLocusTag astLocus) {
    super(null == name ? structName : name, size, cvAttributes, astLocus);
    this.structName = structName;
  }

  CompoundType(final CompoundType o, final int cvAttributes, final ASTLocusTag astLocus) {
    super(o, cvAttributes, astLocus);
    this.structName = o.structName;
    if(null != o.fields) {
        fields = new ArrayList<Field>(o.fields);
    }
    bodyParsed = o.bodyParsed;
  }

  @Override
  protected int hashCodeImpl() {
      // 31 * x == (x << 5) - x
      final int hash = 31 + ( null != structName ? structName.hashCode() : 0 );
      return ((hash << 5) - hash) + TypeComparator.listsHashCode(fields);
  }

  @Override
  protected boolean equalsImpl(final Type arg) {
    final CompoundType ct = (CompoundType) arg;
    return ( (structName == null ? ct.structName == null : structName.equals(ct.structName)) ||
             (structName != null && structName.equals(ct.structName))
           ) &&
           TypeComparator.listsEqual(fields, ct.fields);
  }

  @Override
  protected int hashCodeSemanticsImpl() {
      // 31 * x == (x << 5) - x
      return TypeComparator.listsHashCodeSemantics(fields);
  }

  @Override
  protected boolean equalSemanticsImpl(final Type arg) {
    final CompoundType ct = (CompoundType) arg;
    return TypeComparator.listsEqualSemantics(fields, ct.fields);
  }

  /** Returns the struct name of this CompoundType, i.e. the "foo" in
      the construct "struct foo { ... };". */
  public String getStructName() {
    return structName;
  }

  @Override
  public CompoundType asCompound() { return this; }

  @Override
  public String getCName(final boolean includeCVAttrs) {
      if( isTypedef() ) {
          return getName(includeCVAttrs);
      } else {
          return (isStruct() ? "struct " : "union ")+getName(includeCVAttrs);
      }
  }

  ArrayList<Field> getFields() { return fields; }
  void setFields(final ArrayList<Field> fields) { this.fields = fields; clearCache(); }

  /** Returns the number of fields in this type. */
  public int   getNumFields() {
    return ((fields == null) ? 0 : fields.size());
  }

  /** Returns the <i>i</i>th field of this type. */
  public Field getField(final int i) {
    return fields.get(i);
  }

  /** Adds a field to this type. */
  public void addField(final Field f) {
    if (bodyParsed) {
      throw new IllegalStateException("Body of this CompoundType has been already closed");
    }
    if (fields == null) {
      fields = new ArrayList<Field>();
    }
    fields.add(f);
    clearCache();
  }

  /**
   * Indicates to this CompoundType that its body has been parsed and
   * that no more {@link #addField} operations will be made.
   * @throws IllegalStateException If called twice.
   */
  public void setBodyParsed() throws IllegalStateException {
    if (bodyParsed) {
        throw new IllegalStateException("Body of this CompoundType has been already closed");
    }
    bodyParsed = true;
  }

  /** Indicates whether this type was declared as a struct. */
  public abstract boolean isStruct();
  /** Indicates whether this type was declared as a union. */
  public abstract boolean isUnion();

  @Override
  public String toString() {
    final String cvAttributesString = getCVAttributesString();
    final String cname = getCName();
    if ( null != cname ) {
      return cvAttributesString + cname;
    } else if (getStructName() != null) {
      return cvAttributesString + "struct " + getStructName();
    } else {
      return cvAttributesString + getStructString();
    }
  }

  @Override
  public void visit(final TypeVisitor arg) {
    if (visiting) {
      return;
    }
    try {
      visiting = true;
      super.visit(arg);
      final int n = getNumFields();
      for (int i = 0; i < n; i++) {
        getField(i).getType().visit(arg);
      }
    } finally {
      visiting = false;
    }
    return;
  }

  public String getStructString() {
    if (visiting) {
      if (getName() != null) {
        return getName();
      }
      return "struct {/*Recursive type reference*/}";
    }

    try {
      visiting = true;
      final String kind = (isStruct() ? "struct {" : "union {");
      final StringBuilder res = new StringBuilder();
      res.append(kind);
      final int n = getNumFields();
      for (int i = 0; i < n; i++) {
        res.append(" ");
        res.append(getField(i));
      }
      res.append(" }");
      return res.toString();
    } finally {
      visiting = false;
    }
  }
}
