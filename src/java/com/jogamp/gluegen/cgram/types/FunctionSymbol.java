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
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */
package com.jogamp.gluegen.cgram.types;

import java.util.List;

import com.jogamp.gluegen.ASTLocusTag;
import com.jogamp.gluegen.ASTLocusTag.ASTLocusTagProvider;
import com.jogamp.gluegen.cgram.types.AliasedSymbol.AliasedSymbolImpl;
import com.jogamp.gluegen.cgram.types.TypeComparator.AliasedSemanticSymbol;
import com.jogamp.gluegen.cgram.types.TypeComparator.SemanticEqualityOp;


/**
 * Describes a function symbol, which includes the name and
 * type. Since we are currently only concerned with processing
 * functions this is the only symbol type, though plausibly more
 * types should be added and a true symbol table constructed during parsing.
 * <p>
 * Note: Since C does not support method-overloading polymorphism like C++ or Java,
 * we ignore the {@link FunctionType} attribute in {@link #equals(Object)} and {@link #hashCode()}.<br/>
 * Hence we assume all method occurrences w/ same name are of equal or compatible type. <br/>
 * Deep comparison can be performed via {@link #isCompletelyEqual(Object o)};
 * </p>
 **/
public class FunctionSymbol extends AliasedSymbolImpl implements AliasedSemanticSymbol, ASTLocusTagProvider {

    private final FunctionType type;
    private final ASTLocusTag astLocus;

    public FunctionSymbol(final String name, final FunctionType type) {
        super(name);
        this.type = type;
        this.astLocus = null;
    }

    public FunctionSymbol(final String name, final FunctionType type, final ASTLocusTag locus) {
        super(name);
        this.type = type;
        this.astLocus = locus;
    }

    /** Shallow'ish copy, only aliased names are re-created. */
    public static FunctionSymbol cloneWithDeepAliases(final FunctionSymbol o) {
        return new FunctionSymbol(o);
    }
    /** Warning: Shallow'ish copy, only aliased names are re-created. */
    private FunctionSymbol(final FunctionSymbol o) {
        super(o);
        this.type = o.type;
        this.astLocus = o.astLocus;
    }

    @Override
    public ASTLocusTag getASTLocusTag() { return astLocus; }

    /** Returns the type of this function. Do not add arguments to it
    directly; use addArgument instead. */
    public FunctionType getType() {
        return type;
    }

    /** Returns the return type of this function. */
    public Type getReturnType() {
        return type.getReturnType();
    }

    public int getNumArguments() {
        return type.getNumArguments();
    }

    /** Returns the name of the <i>i</i>th argument. May return null if
    no argument names were available during parsing. */
    public String getArgumentName(final int i) {
        return type.getArgumentName(i);
    }

    /** Returns the type of the <i>i</i>th argument. */
    public Type getArgumentType(final int i) {
        return type.getArgumentType(i);
    }

    /** Add an argument's name and type. Use null for unknown argument
    names. */
    public void addArgument(final Type argumentType, final String argumentName) {
        type.addArgument(argumentType, argumentName);
    }

    @Override
    public String toString() {
        return getType().toString(getName(), false);
    }

    /** Helper routine for emitting native javadoc tags */
    public String toString(final boolean emitNativeTag) {
        return getType().toString(getName(), emitNativeTag);
    }

    @Override
    public int hashCode() {
        if (getName() == null) {
            return 0;
        }
        return getName().hashCode();
    }

    @Override
    public boolean equals(final Object arg) {
        if (arg == this) {
            return true;
        }
        if ( !(arg instanceof FunctionSymbol) ) {
            return false;
        }
        final FunctionSymbol other = (FunctionSymbol) arg;
        if (getName() == null && other.getName() != null) {
            return false;
        }
        return getName().equals(other.getName());
    }

    @Override
    public int hashCodeSemantics() {
        return type.hashCodeSemantics();
    }
    @Override
    public final boolean equalSemantics(final SemanticEqualityOp arg) {
        if (arg == this) {
            return true;
        }
        if ( !(arg instanceof FunctionSymbol) ) {
            return false;
        }
        final FunctionSymbol other = (FunctionSymbol) arg;
        return type.equalSemantics(other.type);
    }


    public static boolean containsExactly(final List<FunctionSymbol> l, final FunctionSymbol s) {
        return exactIndexOf(l, s) >= 0;
    }

    public static int exactIndexOf(final List<FunctionSymbol> l, final FunctionSymbol s) {
        final int size = l.size();
        for (int i = 0; i < size; i++) {
            final FunctionSymbol e = l.get(i);
            if( null == s && null == e ||
                s.equals( e ) && s.type.equals(e.type) ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Compares the function type as well, since {@link #equals(Object)}
     * and {@link #hashCode()} won't.
     */
    public boolean exactlyEqual(final Object arg) {
        if( !this.equals(arg) ) {
            return false;
        }
        return type.equals( ((FunctionSymbol)arg).type );
    }
}
