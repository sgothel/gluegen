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

import java.util.ArrayList;
import java.util.NoSuchElementException;

import com.jogamp.gluegen.ASTLocusTag;
import com.jogamp.gluegen.cgram.types.TypeComparator.SemanticEqualityOp;


/** Describes enumerated types. Enumerations are like ints except that
they have a set of named values. */
public class EnumType extends IntType implements Cloneable {

    private static class Enum implements TypeComparator.SemanticEqualityOp {
        final String name;
        final long value;

        Enum(final String name, final long value) {
            this.name = name;
            this.value = value;
        }

        String getName() {
            return name;
        }

        long getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            // 31 * x == (x << 5) - x
            final int hash = name.hashCode();
            return ((hash << 5) - hash) + (int)(value ^ (value >>> 32));
        }

        @Override
        public boolean equals(final Object arg) {
            if (arg == this) {
                return true;
            } else if ( !(arg instanceof Enum) ) {
                return false;
            }
            final Enum t = (Enum) arg;
            return name.equals(t.name) &&
                   value == t.value;
        }

        @Override
        public int hashCodeSemantics() {
            return hashCode();
        }

        @Override
        public boolean equalSemantics(final SemanticEqualityOp arg) {
            return equals(arg);
        }

        public String toString() { return name+" = "+value; }
    }

    private final IntType underlyingType;
    private ArrayList<Enum> enums;

    public EnumType(final String name) {
        super(name, SizeThunk.LONG, false, CVAttributes.CONST);
        this.underlyingType = new IntType(name, SizeThunk.LONG, false, CVAttributes.CONST);
    }

    public EnumType(final String name, final SizeThunk enumSizeInBytes, final ASTLocusTag astLocus) {
        super(name, enumSizeInBytes, false, CVAttributes.CONST, astLocus);
        this.underlyingType = new IntType(name, enumSizeInBytes, false, CVAttributes.CONST, astLocus);
    }

    private EnumType(final EnumType o, final int cvAttributes, final ASTLocusTag astLocus) {
        super(o, cvAttributes, astLocus);
        underlyingType = o.underlyingType;
        if(null != o.enums) {
            enums = new ArrayList<Enum>(o.enums);
        }
    }

    @Override
    Type newVariantImpl(final boolean newCVVariant, final int cvAttributes, final ASTLocusTag astLocus) {
        return new EnumType(this, cvAttributes, astLocus);
    }

    @Override
    protected int hashCodeImpl() {
      // 31 * x == (x << 5) - x
      int hash = super.hashCodeImpl();
      hash = ((hash << 5) - hash) + underlyingType.hashCode();
      return ((hash << 5) - hash) + TypeComparator.listsHashCode(enums);
    }

    @Override
    protected boolean equalsImpl(final Type arg) {
        final EnumType t = (EnumType) arg;
        return super.equalsImpl(arg) &&
                underlyingType.equals(t.underlyingType) &&
               TypeComparator.listsEqual(enums, t.enums);
    }

    @Override
    protected int hashCodeSemanticsImpl() {
      // 31 * x == (x << 5) - x
      int hash = super.hashCodeSemanticsImpl();
      hash = ((hash << 5) - hash) + underlyingType.hashCodeSemantics();
      return ((hash << 5) - hash) + TypeComparator.listsHashCodeSemantics(enums);
    }

    @Override
    protected boolean equalSemanticsImpl(final Type arg) {
        final EnumType t = (EnumType) arg;
        return super.equalSemanticsImpl(arg) &&
                underlyingType.equalSemantics(t.underlyingType) &&
               TypeComparator.listsEqualSemantics(enums, t.enums);
    }

    @Override
    public EnumType asEnum() {
        return this;
    }

    public Type getUnderlyingType() { return this.underlyingType; }

    public void addEnum(final String name, final long val) {
        if (enums == null) {
            enums = new ArrayList<Enum>();
        }
        enums.add(new Enum(name, val));
        clearCache();
    }

    /** Number of enumerates defined in this enum. */
    public int getNumEnumerates() {
        return enums.size();
    }

    /** Fetch <i>i</i>th (0..getNumEnumerates() - 1) name */
    public String getEnumName(final int i) {
        return (enums.get(i)).getName();
    }

    /** Fetch <i>i</i>th (0..getNumEnumerates() - 1) value */
    public long getEnumValue(final int i) {
        return (enums.get(i)).getValue();
    }

    /** Fetch the value of the enumerate with the given name. */
    public long getEnumValue(final String name) {
        for (int i = 0; i < enums.size(); ++i) {
            final Enum n = (enums.get(i));
            if (n.getName().equals(name)) {
                return n.getValue();
            }
        }
        throw new NoSuchElementException(
                "No enumerate named \"" + name + "\" in EnumType \""
                + getName() + "\"");
    }

    /** Does this enum type contain an enumerate with the given name? */
    public boolean containsEnumerate(final String name) {
        for (int i = 0; i < enums.size(); ++i) {
            if ((enums.get(i)).getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Remove the enumerate with the given name. Returns true if it was found
     * and removed; false if it was not found.
     */
    public boolean removeEnumerate(final String name) {
        for (int i = 0; i < enums.size(); ++i) {
            final Enum e = enums.get(i);
            if (e.getName().equals(name)) {
                enums.remove(e);
                clearCache();
                return true;
            }
        }
        return false;
    }

    public StringBuilder appendEnums(final StringBuilder sb, final boolean cr) {
        for(int i=0; i<enums.size(); i++) {
            sb.append(enums.get(i)).append(", ");
            if( cr ) {
                sb.append(String.format("%n"));
            }
        }
        sb.append("}");
        return sb;
    }

    @Override
    public void visit(final TypeVisitor arg) {
        super.visit(arg);
        underlyingType.visit(arg);
    }
}
