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

public class PointerType extends Type implements Cloneable {

    private final Type targetType;

    public PointerType(final SizeThunk size, final Type targetType, final int cvAttributes) {
        this(size, targetType, cvAttributes, null);
    }
    public PointerType(final SizeThunk size, final Type targetType, final int cvAttributes, final ASTLocusTag astLocus) {
        // can pass null for the final name parameter because the PointerType's getName()
        // completely replaces superclass behavior
        super(targetType.getName() + " *", size, cvAttributes, astLocus);
        this.targetType = targetType;
    }

    private PointerType(final PointerType o, final int cvAttributes, final ASTLocusTag astLocus) {
        super(o, cvAttributes, astLocus);
        targetType = o.targetType;
    }

    @Override
    Type newVariantImpl(final boolean newCVVariant, final int cvAttributes, final ASTLocusTag astLocus) {
        return new PointerType(this, cvAttributes, astLocus);
    }

    @Override
    protected int hashCodeImpl() {
      return targetType.hashCode();
    }

    @Override
    protected boolean equalsImpl(final Type arg) {
        final PointerType t = (PointerType) arg;
        return targetType.equals(t.targetType);
    }

    @Override
    protected int hashCodeSemanticsImpl() {
      return targetType.hashCodeSemantics();
    }

    @Override
    protected boolean equalSemanticsImpl(final Type arg) {
        final PointerType pt = (PointerType) arg;
        return targetType.equalSemantics(pt.targetType);
    }

    @Override
    public boolean isAnon() {
        if ( isTypedef() ) {
            return super.isAnon();
        } else {
            return targetType.isAnon();
        }
    }

    @Override
    public String getName(final boolean includeCVAttrs) {
        if ( isTypedef() ) {
            return super.getName(includeCVAttrs);
        } else if (!includeCVAttrs) {
            return targetType.getName(includeCVAttrs) + " *";
        } else {
            return targetType.getName(includeCVAttrs) + " * " + getCVAttributesString();
        }
    }

    @Override
    public String getCName(final boolean includeCVAttrs) {
        if ( isTypedef() ) {
            return super.getCName(includeCVAttrs);
        } else if (!includeCVAttrs) {
            return targetType.getCName(includeCVAttrs) + " *";
        } else {
            return targetType.getCName(includeCVAttrs) + " * " + getCVAttributesString();
        }
    }

    @Override
    public final PointerType asPointer() {
        return this;
    }

    @Override
    public final Type getTargetType() {
        return targetType;
    }

    @Override
    public final Type getBaseElementType() {
        return targetType.getBaseElementType();
    }

    @Override
    public final boolean isFunctionPointer() {
        return targetType.isFunction();
    }

    @Override
    public final int pointerDepth() {
        return 1 + targetType.pointerDepth();
    }

    @Override
    public String toString() {
        if ( isTypedef() ) {
            return super.getCName(true);
        } else {
            return toStringInt();
        }
    }
    private String toStringInt() {
        if (!targetType.isFunction()) {
            return targetType.getCName(true) + " * " + getCVAttributesString();
        } else {
            // return toString(null, null); // this is a pointer to an unnamed function
            return ((FunctionType) targetType).toString(null /* functionName */, null /* callingConvention */, false, true);
        }
    }

    /** For use only when printing function pointers. Calling convention
    string (i.e., "__stdcall") is optional and is generally only
    needed on Windows. */
    public String toString(final String functionName, final String callingConvention) {
        if (!targetType.isFunction()) {
            throw new RuntimeException("<Internal error or misuse> This method is only for use when printing function pointers");
        }
        return ((FunctionType) targetType).toString(functionName, callingConvention, false, true);
    }

    @Override
    public void visit(final TypeVisitor arg) {
        super.visit(arg);
        targetType.visit(arg);
    }
}
