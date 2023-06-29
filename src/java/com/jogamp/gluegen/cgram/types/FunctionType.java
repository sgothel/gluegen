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
import com.jogamp.gluegen.CodeGenUtils;

/** Describes a function type, used to model both function
declarations and (via PointerType) function pointers. */
public class FunctionType extends Type implements Cloneable {

    private final Type returnType;
    private ArrayList<Type> argumentTypes;
    private ArrayList<String> argumentNames;

    public FunctionType(final String name, final SizeThunk size, final Type returnType,
                        final int cvAttributes) {
        this(name, size, returnType, cvAttributes, null);
    }
    public FunctionType(final String name, final SizeThunk size, final Type returnType,
                        final int cvAttributes, final ASTLocusTag astLocus) {
        super(name, size, cvAttributes, astLocus);
        this.returnType = returnType;
    }

    private FunctionType(final FunctionType o, final ASTLocusTag astLocus) {
        super(o, o.getCVAttributes(), astLocus);
        returnType = o.returnType;
        if(null != o.argumentTypes) {
            argumentTypes = new ArrayList<Type>(o.argumentTypes);
        }
        if(null != o.argumentNames) {
            argumentNames = new ArrayList<String>(o.argumentNames);
        }
    }

    @Override
    Type newVariantImpl(final boolean newCVVariant, final int cvAttributes, final ASTLocusTag astLocus) {
        if( newCVVariant ) {
            // Functions don't have const/volatile attributes
            return this;
        } else {
            return new FunctionType(this, astLocus);
        }
    }

    @Override
    protected int hashCodeImpl() {
        // 31 * x == (x << 5) - x
        final int hash = returnType.hashCode();
        return ((hash << 5) - hash) + TypeComparator.listsHashCode(argumentTypes);
    }

    @Override
    protected boolean equalsImpl(final Type arg) {
        final FunctionType t = (FunctionType) arg;
        return returnType.equals(t.returnType) &&
               TypeComparator.listsEqual(argumentTypes, t.argumentTypes);
    }

    @Override
    protected int hashCodeSemanticsImpl() {
        // 31 * x == (x << 5) - x
        final int hash = returnType.hashCodeSemantics();
        return ((hash << 5) - hash) + TypeComparator.listsHashCodeSemantics(argumentTypes);
    }

    @Override
    protected boolean equalSemanticsImpl(final Type arg) {
        final FunctionType t = (FunctionType) arg;
        return returnType.equalSemantics(t.returnType) &&
               TypeComparator.listsEqualSemantics(argumentTypes, t.argumentTypes);
    }

    @Override
    public FunctionType asFunction() {
        return this;
    }

    /** Returns the return type of this function. */
    public Type getReturnType() {
        return returnType;
    }

    public int getNumArguments() {
        return ((argumentTypes == null) ? 0 : argumentTypes.size());
    }

    /** Returns the name of the <i>i</i>th argument. May return null if
    no argument names were available during parsing. */
    public String getArgumentName(final int i) {
        return argumentNames.get(i);
    }

    /** Returns the type of the <i>i</i>th argument. */
    public Type getArgumentType(final int i) {
        return argumentTypes.get(i);
    }

    /**
     * Returns the function parameter list, i.e. a comma separated list of argument type and name.
     * @param buf StringBuilder instance
     * @param callingConvention optional calling-convention
     * @return given StringBuilder instance
     */
    public StringBuilder getParameterList(final StringBuilder buf, final String callingConvention) {
        final int n = getNumArguments();
        final boolean[] needsComma = { false };
        for (int i = 0; i < n; i++) {
            final Type t = getArgumentType(i);
            if( t.isVoid() ) {
                // nop
            } else if( t.isTypedef() ) {
                CodeGenUtils.addParameterToList(buf, t.getName(), needsComma);
                final String argumentName = getArgumentName(i);
                if (argumentName != null) {
                    buf.append(" ");
                    buf.append(argumentName);
                }
            } else  if ( t.isFunctionPointer() ) {
                final FunctionType ft = t.getTargetFunction();
                CodeGenUtils.addParameterToList(buf, ft.toString(getArgumentName(i), callingConvention, false, true), needsComma);
            } else if (t.isArray()) {
                CodeGenUtils.addParameterToList(buf, t.asArray().toString(getArgumentName(i)), needsComma);
            } else {
                CodeGenUtils.addParameterToList(buf, t.getCName(true), needsComma);
                final String argumentName = getArgumentName(i);
                if (argumentName != null) {
                    buf.append(" ");
                    buf.append(argumentName);
                }
            }
        }
        return buf;
    }

    /**
     * Add an argument's name and type. Use null for unknown argument names.
     */
    public void addArgument(final Type argumentType, final String argumentName) {
        if (argumentTypes == null) {
            argumentTypes = new ArrayList<Type>();
            argumentNames = new ArrayList<String>();
        }
        argumentTypes.add(argumentType);
        argumentNames.add(argumentName);
        clearCache();
    }

    public void setArgumentName(final int i, final String name) {
        argumentNames.set(i, name);
        clearCache();
    }

    @Override
    public String toString() {
        return toString(null, false);
    }

    public String toString(final String functionName, final boolean emitNativeTag) {
        return toString(functionName, null, emitNativeTag, false);
    }
    public String toString(final String functionName, final boolean emitNativeTag, final boolean isPointer) {
        return toString(functionName, null, emitNativeTag, isPointer);
    }

    public String toString(final String functionName, final String callingConvention,
                           final boolean emitNativeTag, final boolean isPointer) {
        final StringBuilder res = new StringBuilder();
        res.append(getReturnType().getCName(true));
        res.append(" ");
        if (isPointer) {
            res.append("(");
            if (callingConvention != null) {
                res.append(callingConvention);
            }
            res.append("*");
        }
        if (functionName != null) {
            if (emitNativeTag) {
                // Emit @native tag for javadoc purposes
                res.append("{@native ");
            }
            res.append(functionName);
            if (emitNativeTag) {
                res.append("}");
            }
        }
        if (isPointer) {
            res.append(")");
        }
        res.append("(");
        getParameterList(res, callingConvention);
        res.append(")");
        return res.toString();
    }

    @Override
    public void visit(final TypeVisitor arg) {
        super.visit(arg);
        returnType.visit(arg);
        final int n = getNumArguments();
        for (int i = 0; i < n; i++) {
            getArgumentType(i).visit(arg);
        }
    }
}
