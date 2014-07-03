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

/** Models all compound types, i.e., those containing fields: structs
    and unions. The boolean type accessors indicate how the type is
    really defined. */

public abstract class CompoundType extends MemoryLayoutType implements Cloneable {
  // The name "foo" in the construct "struct foo { ... }";
  private String structName;
  private ArrayList<Field> fields;
  private boolean visiting;
  private boolean bodyParsed;
  private boolean computedHashcode;
  private int     hashcode;

  CompoundType(final String name, final SizeThunk size, final int cvAttributes, final String structName) {
    super(name, size, cvAttributes);
    this.structName = structName;
  }

  public static CompoundType create(final String name, final SizeThunk size, final CompoundTypeKind kind, final int cvAttributes) {
    switch (kind) {
      case STRUCT:
          return new StructType(name, size, cvAttributes);
      case UNION:
          return new UnionType(name, size, cvAttributes);
      default:
          throw new RuntimeException("OO relation "+kind+" / Compount not yet supported");
    }
  }

  @Override
  public Object clone() {
    final CompoundType n = (CompoundType) super.clone();
    if(null!=this.fields) {
        n.fields = new ArrayList<Field>(this.fields);
    }
    return n;
  }

  @Override
  public int hashCode() {
    if (computedHashcode) {
      return hashcode;
    }

    if (structName != null) {
      hashcode = structName.hashCode();
    } else if (getName() != null) {
      hashcode = getName().hashCode();
    } else {
      hashcode = 0;
    }

    computedHashcode = true;
    return hashcode;
  }

  @Override
  public boolean equals(final Object arg) {
    if (arg == this) return true;
    if (arg == null || !(arg instanceof CompoundType)) {
      return false;
    }
    final CompoundType t = (CompoundType) arg;
    return super.equals(arg) &&
        ((structName == null ? t.structName == null : structName.equals(t.structName)) ||
         (structName != null && structName.equals(t.structName))) &&
        listsEqual(fields, t.fields);
  }

  /** Returns the struct name of this CompoundType, i.e. the "foo" in
      the construct "struct foo { ... };". */
  public String getStructName() {
    return structName;
  }

  /** Sets the struct name of this CompoundType, i.e. the "foo" in the
      construct "struct foo { ... };". */
  public void setStructName(final String structName) {
    this.structName = structName;
  }

  @Override
  public void setSize(final SizeThunk size) {
    super.setSize(size);
  }

  @Override
  public CompoundType asCompound() { return this; }

  ArrayList<Field> getFields() { return fields; }
  void setFields(final ArrayList<Field> fields) { this.fields = fields; }

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
      throw new RuntimeException("Body of this CompoundType has already been parsed; should not be adding more fields");
    }
    if (fields == null) {
      fields = new ArrayList<Field>();
    }
    fields.add(f);
  }

  /** Indicates to this CompoundType that its body has been parsed and
      that no more {@link #addField} operations will be made. */
  public void setBodyParsed() {
    bodyParsed = true;
  }

  /** Indicates whether this type was declared as a struct. */
  public abstract boolean isStruct();
  /** Indicates whether this type was declared as a union. */
  public abstract boolean isUnion();

  @Override
  public String toString() {
    final String cvAttributesString = getCVAttributesString();
    if (getName() != null) {
      return cvAttributesString + getName();
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
        final Field f = getField(i);
        f.getType().visit(arg);
      }
    } finally {
      visiting = false;
    }
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
