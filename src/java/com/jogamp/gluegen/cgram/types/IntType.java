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

public class IntType extends PrimitiveType implements Cloneable {

    private final boolean unsigned;
    private boolean typedefUnsigned;

    public IntType(final String name, final SizeThunk size, final boolean unsigned, final int cvAttributes) {
        this(name, size, unsigned, cvAttributes, null);
    }

    public IntType(final String name, final SizeThunk size,
                   final boolean unsigned, final int cvAttributes,
                   final ASTLocusTag astLocus) {
        super(name, size, cvAttributes, astLocus);
        this.unsigned = unsigned;
        this.typedefUnsigned = false;
    }

    /**
     * Only for HeaderParser!
     *
     * @param name  the name
     * @param size  the size
     * @param unsigned true if this instance is unsigned, not the <i>typedef</i>!
     * @param cvAttributes the cvAttributes for this instance, not for the <i>typedef</i>!
     * @param isTypedef true if this instance is a <i>typedef</i> variant
     * @param typedefUnsigned true if the <i>typedef</i> itself is unsigned
     * @param astLocus the location in source code
     */
    public IntType(final String name, final SizeThunk size,
                   final boolean unsigned, final int cvAttributes,
                   final boolean isTypedef, final boolean typedefUnsigned,
                   final ASTLocusTag astLocus) {
        super(name, size, cvAttributes, astLocus);
        this.unsigned = unsigned;
        if( isTypedef ) {
            // the 'cvAttributes' are intended for this instance, not the 'typedef cvAttributes'!
            setTypedef(0);
            this.typedefUnsigned = typedefUnsigned;
        } else {
            this.typedefUnsigned = false;
        }
    }

    IntType(final IntType o, final int cvAttributes, final ASTLocusTag astLocus) {
        super(o, cvAttributes, astLocus);
        this.unsigned = o.unsigned;
        this.typedefUnsigned = o.typedefUnsigned;
    }

    @Override
    Type newVariantImpl(final boolean newCVVariant, final int cvAttributes, final ASTLocusTag astLocus) {
        return new IntType(this, cvAttributes, astLocus);
    }

    @Override
    protected int hashCodeImpl() {
      // 31 * x == (x << 5) - x
      int hash = 1;
      hash = ((hash << 5) - hash) + ( unsigned ? 1 : 0 );
      return ((hash << 5) - hash) + ( typedefUnsigned ? 1 : 0 );
    }

    @Override
    protected boolean equalsImpl(final Type arg) {
        final IntType t = (IntType) arg;
        return unsigned == t.unsigned &&
               typedefUnsigned == t.typedefUnsigned;
    }

    @Override
    protected int hashCodeSemanticsImpl() {
      // 31 * x == (x << 5) - x
      int hash = 1;
      if( !relaxedEqSem ) {
        hash = ((hash << 5) - hash) + ( unsigned ? 1 : 0 );
        hash = ((hash << 5) - hash) + ( typedefUnsigned ? 1 : 0 );
      }
      return hash;
    }

    @Override
    protected boolean equalSemanticsImpl(final Type arg) {
        final IntType t = (IntType) arg;
        return relaxedEqSem ||
               ( unsigned == t.unsigned &&
                 typedefUnsigned == t.typedefUnsigned
               );
    }

    @Override
    public IntType asInt() {
        return this;
    }

    /** Indicates whether this type is unsigned */
    public boolean isUnsigned() {
        return unsigned;
    }

    @Override
    public String getCName(final boolean includeCVAttrs) {
        if ( !unsigned || typedefUnsigned ) {
            return super.getCName(includeCVAttrs);
        } else {
            return "unsigned "+super.getCName(includeCVAttrs);
        }
    }

    @Override
    public String toString() {
        return getCVAttributesString() + ( unsigned && !typedefUnsigned ? "unsigned " : "") + getCName();
    }

    @Override
    public boolean setTypedefName(final String name) {
        if( super.setTypedefName(name) ) {
            typedefUnsigned = unsigned;
            return true;
        } else {
            return false;
        }
    }
}
