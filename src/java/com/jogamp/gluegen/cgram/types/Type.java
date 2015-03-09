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

import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.gluegen.ASTLocusTag.ASTLocusTagProvider;
import com.jogamp.gluegen.ASTLocusTag;
import com.jogamp.gluegen.GlueGen;
import com.jogamp.gluegen.TypeConfig;
import com.jogamp.gluegen.cgram.types.TypeComparator.SemanticEqualityOp;

/** Models a C type. Primitive types include int, float, and
    double. All types have an associated name. Structs and unions are
    modeled as "compound" types -- composed of fields of primitive or
    other types. */
public abstract class Type implements SemanticEqualityOp, ASTLocusTagProvider {
  public final boolean relaxedEqSem;
  private final int cvAttributes;
  final ASTLocusTag astLocus;
  private String name;
  private SizeThunk size;
  private int typedefCVAttributes;
  private boolean isTypedef;
  private boolean hasCachedHash;
  private int cachedHash;
  private boolean hasCachedSemanticHash;
  private int cachedSemanticHash;

  protected Type(final String name, final SizeThunk size, final int cvAttributes, final ASTLocusTag astLocus) {
    setName(name); // -> clearCache()
    this.relaxedEqSem = TypeConfig.relaxedEqualSemanticsTest();
    this.cvAttributes = cvAttributes;
    this.astLocus = astLocus;
    this.size = size;
    this.typedefCVAttributes = 0;
    this.isTypedef = false;
  }
  Type(final Type o, final int cvAttributes, final ASTLocusTag astLocus) {
    this.relaxedEqSem = o.relaxedEqSem;
    this.cvAttributes = cvAttributes;
    this.astLocus = astLocus;
    this.name = o.name;
    this.size = o.size;
    this.typedefCVAttributes = o.typedefCVAttributes;
    this.isTypedef = o.isTypedef;
    clearCache();
  }

  protected final void clearCache() {
    hasCachedHash = false;
    cachedHash = 0;
    hasCachedSemanticHash = false;
    cachedSemanticHash = 0;
  }

  /**
   * Return a variant of this type matching the given const/volatile
   * attributes. May return this object if the attributes match.
   */
  public final Type newCVVariant(final int cvAttributes) {
    if (this.cvAttributes == cvAttributes) {
        return this;
    } else {
        return newVariantImpl(true, cvAttributes, astLocus);
    }
  }

  /**
   * Clones this instance using a new {@link ASTLocusTag}.
   */
  public Type clone(final ASTLocusTag newLoc) {
    return newVariantImpl(true, cvAttributes, newLoc);
  }

  /**
   * Create a new variant of this type matching the given parameter
   * <p>
   * Implementation <i>must</i> use {@link Type}'s copy-ctor: {@link #Type(Type, int, ASTLocusTag)}!
   * </p>
   * @param newCVVariant true if new variant is intended to have new <i>cvAttributes</i>
   * @param cvAttributes the <i>cvAttributes</i> to be used
   * @param astLocus the {@link ASTLocusTag} to be used
   */
  abstract Type newVariantImpl(final boolean newCVVariant, final int cvAttributes, final ASTLocusTag astLocus);

  @Override
  public final ASTLocusTag getASTLocusTag() { return astLocus; }

  public boolean isAnon() { return null == name; }

  /** Returns the name of this type. The returned string is suitable
      for use as a type specifier for native C. Does not include any const/volatile
      attributes. */
  public final String getCName() { return getCName(false); }

  /** Returns the name of this type, optionally including
      const/volatile attributes. The returned string is suitable for
      use as a type specifier for native C. */
  public String getCName(final boolean includeCVAttrs) { return getName(includeCVAttrs); }

  /** Returns the name of this type. The returned string is suitable
      for use as a type specifier for Java. Does not include any const/volatile
      attributes. */
  public final String getName() { return getName(false); }

  /** Returns the name of this type, optionally including
      const/volatile attributes. The returned string is suitable for
      use as a type specifier for Java. */
  public String getName(final boolean includeCVAttrs) {
    if (!includeCVAttrs) {
      return name;
    }
    return getCVAttributesString() + name;
  }

  /**
   * Returns a string representation of this type.
   * The returned string is suitable for use as a type specifier for native C.
   * It does contain an expanded description of structs/unions,
   * hence may not be suitable for type declarations.
   */
  @Override
  public String toString() {
    return getCName(true);
  }


  private static StringBuilder append(final StringBuilder sb, final String val, final boolean prepComma) {
      if( prepComma ) {
          sb.append(", ");
      }
      sb.append(val);
      return sb;
  }
  // For debugging
  public final String getDebugString() {
    final StringBuilder sb = new StringBuilder();
    boolean prepComma = false;
    sb.append("CType[");
    sb.append("(").append(getClass().getSimpleName()).append(") ");
    if( isTypedef() ) {
        sb.append("typedef ");
    }
    if( null != name ) {
        sb.append("'").append(name).append("'");
    } else {
        sb.append("ANON");
    }
    final Type targetType = getTargetType();
    if( null != targetType && this != targetType ) {
        sb.append(" -> ");
        if (!targetType.isFunction()) {
            sb.append("(" + targetType.toString() + ") * " + getCVAttributesString());
        } else {
            sb.append(((FunctionType) targetType).toString(null /* functionName */, null /* callingConvention */, false, true));
        }
    }
    if( GlueGen.debug() ) {
        sb.append(", o=0x"+Integer.toHexString(objHash()));
    }
    sb.append(", size ");
    prepComma=true;
    if( null != size ) {
        final long mdSize;
        {
            long _mdSize = -1;
            try {
                _mdSize = size.computeSize(MachineDataInfo.StaticConfig.LP64_UNIX.md);
            } catch (final Exception e) {}
            mdSize = _mdSize;
        }
        sb.append("[fixed ").append(size.hasFixedNativeSize()).append(", lnx64 ").append(mdSize).append("]");
    } else {
        sb.append(" ZERO");
    }
    append(sb, "[", prepComma); prepComma=false;
    {
        append(sb, "const[", prepComma); prepComma=false;
        {
            if( isConstTypedef() ) {
                append(sb, "type ", prepComma);  prepComma=true;
            }
            if( isConstRaw() ) {
                append(sb, "inst -> ", prepComma);  prepComma=false;
            }
            if( isConst() ) {
                append(sb, "true]", prepComma);
            } else {
                append(sb, "false]", prepComma);
            }
            prepComma=true;
        }
        if( isVolatile() ) {
            append(sb, "volatile ", prepComma);  prepComma=true;
        }
        if( isPointer() ) {
            append(sb, "pointer*"+pointerDepth(), prepComma); prepComma=true;
        }
        if( isArray() ) {
            append(sb, "array*"+arrayDimension(), prepComma); prepComma=true;
        }
        if( isBit() ) {
            append(sb, "bit", prepComma); prepComma=true;
        }
        if( isCompound() ) {
            append(sb, "struct{", prepComma).append(asCompound().getStructName()).append(": ").append(asCompound().getNumFields());
            append(sb, "}", prepComma); prepComma=true;
        }
        if( isDouble() ) {
            append(sb, "double", prepComma); prepComma=true;
        }
        if( isEnum() ) {
            final EnumType eT = asEnum();
            append(sb, "enum ", prepComma).append(" [").append(eT.getUnderlyingType()).append("] {").append(eT.getNumEnumerates()).append(": ");
            eT.appendEnums(sb, false);
            prepComma=true;
        }
        if( isFloat() ) {
            append(sb, "float", prepComma); prepComma=true;
        }
        if( isFunction() ) {
            append(sb, "function", prepComma); prepComma=true;
        }
        if( isFunctionPointer() ) {
            append(sb, "funcPointer", prepComma); prepComma=true;
        }
        if( isInt() ) {
            append(sb, "int", prepComma); prepComma=true;
        }
        if( isVoid() ) {
            append(sb, "void", prepComma); prepComma=true;
        }
        sb.append("]");
    }
    sb.append("]");
    return sb.toString();
  }
  private final int objHash() { return super.hashCode(); }


  /**
   * Returns {@code true} if given {@code name} is not {@code null}
   * and has a length &gt; 0. In this case this instance's names will
   * be set to the internalized version.
   * <p>
   * Otherwise method returns {@code false}
   * and this instance's name will be set to {@code null}.
   * </p>
   * <p>
   * Method issues {@link #clearCache()}, to force re-evaluation
   * of hashes.
   * </p>
   */
  private final boolean setName(final String name) {
    clearCache();
    if( null == name || 0 == name.length() ) {
      this.name = name;
      return false;
    } else {
      this.name = name.intern();
      return true;
    }
  }

  /**
   * Set the typedef name of this type and renders this type a typedef,
   * if given {@code name} has a length.
   * <p>
   * Method issues {@link #clearCache()}, to force re-evaluation
   * of hashes.
   * </p>
   */
  public boolean setTypedefName(final String name) {
    if( setName(name) ) {
        // Capture the const/volatile attributes at the time of typedef so
        // we don't redundantly repeat them in the CV attributes string
        typedefCVAttributes = cvAttributes;
        isTypedef = true;
        return true;
    } else {
        return false;
    }
  }
  final void setTypedef(final int typedefedCVAttributes) {
    this.name = this.name.intern(); // just make sure ..
    this.typedefCVAttributes = typedefedCVAttributes;
    this.isTypedef = true;
    clearCache();
  }
  final int getTypedefCVAttributes() {
    return typedefCVAttributes;
  }

  /**
   * Indicates whether this type is a typedef type,
   * i.e. declared via {@link #setTypedefName(String)}.
   */
  public final boolean isTypedef() {
    return isTypedef;
  }

  /** SizeThunk which computes size of this type in bytes. */
  public final SizeThunk getSize()    { return size; }
  /** Size of this type in bytes according to the given MachineDataInfo. */
  public final long getSize(final MachineDataInfo machDesc) {
    final SizeThunk thunk = getSize();
    if (thunk == null) {
      throw new RuntimeException("No size set for type \"" + getName() + "\"");
    }
    return thunk.computeSize(machDesc);
  }
  /** Set the size of this type; only available for CompoundTypes. */
  final void setSize(final SizeThunk size) {
      this.size = size;
      clearCache();
  }

  /** Casts this to a BitType or returns null if not a BitType. */
  public BitType      asBit()      { return null; }
  /** Casts this to an IntType or returns null if not an IntType. */
  public IntType      asInt()      { return null; }
  /** Casts this to an EnumType or returns null if not an EnumType. */
  public EnumType     asEnum()     { return null; }
  /** Casts this to a FloatType or returns null if not a FloatType. */
  public FloatType    asFloat()    { return null; }
  /** Casts this to a DoubleType or returns null if not a DoubleType. */
  public DoubleType   asDouble()   { return null; }
  /** Casts this to a PointerType or returns null if not a PointerType. */
  public PointerType  asPointer()  { return null; }
  /** Casts this to an ArrayType or returns null if not an ArrayType. */
  public ArrayType    asArray()    { return null; }
  /** Casts this to a CompoundType or returns null if not a CompoundType. */
  public CompoundType asCompound() { return null; }
  /** Casts this to a FunctionType or returns null if not a FunctionType. */
  public FunctionType asFunction() { return null; }
  /** Casts this to a VoidType or returns null if not a VoidType. */
  public VoidType     asVoid()     { return null; }

  /** Indicates whether this is a BitType. */
  public final boolean      isBit()      { return (asBit()      != null); }
  /** Indicates whether this is an IntType. */
  public final boolean      isInt()      { return (asInt()      != null); }
  /** Indicates whether this is an EnumType. */
  public final boolean      isEnum()     { return (asEnum()     != null); }
  /** Indicates whether this is a FloatType. */
  public final boolean      isFloat()    { return (asFloat()    != null); }
  /** Indicates whether this is a DoubleType. */
  public final boolean      isDouble()   { return (asDouble()   != null); }
  /** Indicates whether this is a PointerType. */
  public final boolean      isPointer()  { return (asPointer()  != null); }
  /** Indicates whether this is an ArrayType. */
  public final boolean      isArray()    { return (asArray()    != null); }
  /** Indicates whether this is a CompoundType. */
  public final boolean      isCompound() { return (asCompound() != null); }
  /** Indicates whether this is a FunctionType. */
  public final boolean      isFunction() { return (asFunction() != null); }
  /** Indicates whether this is a VoidType. */
  public final boolean      isVoid()     { return (asVoid()     != null); }

  /** Indicates whether this type is volatile. */
  public final boolean      isVolatile() { return 0 != ( ( cvAttributes & ~typedefCVAttributes ) & CVAttributes.VOLATILE );  }
  /** Indicates whether this type is const. */
  public final boolean      isConst()    { return 0 != ( ( cvAttributes & ~typedefCVAttributes ) & CVAttributes.CONST );  }

  private final boolean isConstTypedef() { return 0 !=                   ( typedefCVAttributes   & CVAttributes.CONST ); }
  private final boolean isConstRaw()     { return 0 !=   ( cvAttributes                          & CVAttributes.CONST ); }

  /** Indicates whether this type is a primitive type. */
  public boolean isPrimitive(){ return false; }

  /** Convenience routine indicating whether this Type is a pointer to
      a function. */
  public boolean isFunctionPointer() {
    return false;
  }

  /**
   * Checks the base type of pointer-to-pointer, pointer, array or plain for const-ness.
   * <p>
   * Note: Intermediate 'const' qualifier are not considered, e.g. const pointer.
   * </p>
   */
  public final boolean isBaseTypeConst() {
    return getBaseElementType().isConst();
  }

  /** Hashcode for Types. */
  @Override
  public final int hashCode() {
    if( !hasCachedHash ) {
        // 31 * x == (x << 5) - x
        int hash = 31 + ( isTypedef ? 1 : 0 );
        hash = ((hash << 5) - hash) + ( null != size ? size.hashCode() : 0 );
        hash = ((hash << 5) - hash) + cvAttributes;
        hash = ((hash << 5) - hash) + typedefCVAttributes;
        hash = ((hash << 5) - hash) + ( null != name ? name.hashCode() : 0 );
        if( !isTypedef ) {
            hash = ((hash << 5) - hash) + hashCodeImpl();
        }
        cachedHash = hash;
        hasCachedHash = true;
    }
    return cachedHash;
  }
  protected abstract int hashCodeImpl();

  /**
   * Equality test for Types inclusive its given {@link #getName() name}.
   */
  @Override
  public final boolean equals(final Object arg) {
    if (arg == this) {
        return true;
    } else  if ( !getClass().isInstance(arg) ) { // implies null == arg || !(arg instanceof Type)
        return false;
    } else {
        final Type t = (Type)arg;
        if( isTypedef == t.isTypedef &&
            ( ( null != size && size.equals(t.size) ) ||
              ( null == size && null == t.size )
            ) &&
            cvAttributes == t.cvAttributes &&
            typedefCVAttributes == t.typedefCVAttributes &&
            ( null == name ? null == t.name : name.equals(t.name) )
          )
        {
            if( !isTypedef ) {
                return equalsImpl(t);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
  }
  protected abstract boolean equalsImpl(final Type t);

  @Override
  public final int hashCodeSemantics() {
    if( !hasCachedSemanticHash ) {
        // 31 * x == (x << 5) - x
        int hash = 31 + ( null != size ? size.hashCodeSemantics() : 0 );
        if( !relaxedEqSem ) {
            hash = ((hash << 5) - hash) + cvAttributes;
            hash = ((hash << 5) - hash) + typedefCVAttributes;
        }
        hash = ((hash << 5) - hash) + hashCodeSemanticsImpl();
        cachedSemanticHash = hash;
        hasCachedSemanticHash = true;
    }
    return cachedSemanticHash;
  }
  protected abstract int hashCodeSemanticsImpl();

  @Override
  public final boolean equalSemantics(final SemanticEqualityOp arg) {
    if (arg == this) {
        return true;
    } else  if ( !(arg instanceof Type) ||
                 !getClass().isInstance(arg) ) { // implies null == arg
        return false;
    } else {
        final Type t = (Type) arg;
        if( ( ( null != size && size.equalSemantics(t.size) ) ||
              ( null == size && null == t.size )
            ) &&
            ( relaxedEqSem ||
              ( cvAttributes == t.cvAttributes &&
                typedefCVAttributes == t.typedefCVAttributes
              )
            )
          )
        {
            return equalSemanticsImpl(t);
        } else {
            return false;
        }
    }
  }
  protected abstract boolean equalSemanticsImpl(final Type t);

  /**
   * Traverse this {@link Type} and all of its component types; for
   * example, the return type and argument types of a FunctionType.
   */
  public void visit(final TypeVisitor visitor) {
    visitor.visitType(this);
  }

  public final int getCVAttributes() {
    return cvAttributes;
  }

  /** Returns a string indicating the const/volatile attributes of
      this type. */
  public final String getCVAttributesString() {
    if (isConst() && isVolatile()) return "const volatile ";
    if (isConst()) return "const ";
    if (isVolatile()) return "volatile ";
    return "";
  }

  /** Helper method for determining how many pointer indirections this
      type represents (i.e., "void **" returns 2). Returns 0 if this
      type is not a pointer type. */
  public int pointerDepth() {
    return 0;
  }

  /** Helper method for determining how many array dimentions this
      type represents (i.e., "char[][]" returns 2). Returns 0 if this
      type is not an array type. */
  public int arrayDimension() {
    return 0;
  }

  /**
   * Helper method to returns the bottom-most element type of this type.
   * <p>
   * If this is a multidimensional array or pointer method returns the bottom-most element type,
   * otherwise this.
   * </p>
   */
  public Type getBaseElementType() {
      return this;
  }

  /**
   * Helper method to returns the target type of this type, in case another type is being referenced.
   */
  public Type getTargetType() {
      return this;
  }
}
