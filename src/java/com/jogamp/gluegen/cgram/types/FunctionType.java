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

/** Describes a function type, used to model both function
declarations and (via PointerType) function pointers. */
public class FunctionType extends Type implements Cloneable {

    private final Type returnType;
    private ArrayList<Type> argumentTypes;
    private ArrayList<String> argumentNames;

    public FunctionType(final String name, final SizeThunk size, final Type returnType, final int cvAttributes) {
        super(name, size, cvAttributes);
        this.returnType = returnType;
    }

    @Override
    public Object clone() {
        final FunctionType n = (FunctionType) super.clone();
        if(null!=this.argumentTypes) {
            n.argumentTypes = new ArrayList<Type>(this.argumentTypes);
        }
        if(null!=this.argumentNames) {
            n.argumentNames = new ArrayList<String>(this.argumentNames);
        }
        return n;
    }

    @Override
    public boolean equals(final Object arg) {
        if (arg == this) {
            return true;
        }
        if (arg == null || (!(arg instanceof FunctionType))) {
            return false;
        }
        final FunctionType t = (FunctionType) arg;
        return (super.equals(arg)
                && returnType.equals(t.returnType)
                && listsEqual(argumentTypes, t.argumentTypes));
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
     * Add an argument's name and type. Use null for unknown argument names.
     */
    public void addArgument(final Type argumentType, final String argumentName) {
        if (argumentTypes == null) {
            argumentTypes = new ArrayList<Type>();
            argumentNames = new ArrayList<String>();
        }
        argumentTypes.add(argumentType);
        argumentNames.add(argumentName);
    }

    public void setArgumentName(final int i, final String name) {
        argumentNames.set(i, name);
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(final String functionName) {
        return toString(functionName, false);
    }

    public String toString(final String functionName, final boolean emitNativeTag) {
        return toString(functionName, null, emitNativeTag, false);
    }

    String toString(final String functionName, final String callingConvention, final boolean emitNativeTag, final boolean isPointer) {
        final StringBuilder res = new StringBuilder();
        res.append(getReturnType());
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
        final int n = getNumArguments();
        for (int i = 0; i < n; i++) {
            final Type t = getArgumentType(i);
            if (t.isFunctionPointer()) {
                final Type targetType = t.asPointer().getTargetType();
                final FunctionType ft = targetType.asFunction();
                res.append(ft.toString(getArgumentName(i), callingConvention, false, true));
            } else if (t.isArray()) {
                res.append(t.asArray().toString(getArgumentName(i)));
            } else {
                res.append(t);
                final String argumentName = getArgumentName(i);
                if (argumentName != null) {
                    res.append(" ");
                    res.append(argumentName);
                }
            }
            if (i < n - 1) {
                res.append(", ");
            }
        }
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

    @Override
    Type newCVVariant(final int cvAttributes) {
        // Functions don't have const/volatile attributes
        return this;
    }
}
