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

public class VoidType extends Type implements Cloneable {

    public VoidType(final int cvAttributes, final ASTLocusTag astLocus) {
        this("void", cvAttributes, astLocus);
    }

    private VoidType(final String name, final int cvAttributes, final ASTLocusTag astLocus) {
        super(name, null, cvAttributes, astLocus);
    }

    private VoidType(final VoidType o, final int cvAttributes, final ASTLocusTag astLocus) {
        super(o, cvAttributes, astLocus);
    }

    @Override
    Type newVariantImpl(final boolean newCVVariant, final int cvAttributes, final ASTLocusTag astLocus) {
        return new VoidType(this, cvAttributes, astLocus);
    }

    @Override
    public VoidType asVoid() {
        return this;
    }

    @Override
    protected int hashCodeImpl() {
        return 0;
    }

    @Override
    protected boolean equalsImpl(final Type t) {
        return true;
    }

    @Override
    protected int hashCodeSemanticsImpl() {
        return 0;
    }

    @Override
    protected boolean equalSemanticsImpl(final Type t) {
        return true;
    }
}
