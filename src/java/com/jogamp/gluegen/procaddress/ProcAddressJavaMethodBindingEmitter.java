/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
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
package com.jogamp.gluegen.procaddress;

import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.FunctionEmitter;
import com.jogamp.gluegen.JavaMethodBindingEmitter;

import java.io.*;

/** A specialization of JavaMethodBindingEmitter with knowledge of how
to call through a function pointer. */
public class ProcAddressJavaMethodBindingEmitter extends JavaMethodBindingEmitter {

    protected boolean callThroughProcAddress;
    protected boolean changeNameAndArguments;

    protected String getProcAddressTableExpr;
    protected ProcAddressEmitter emitter;

    public ProcAddressJavaMethodBindingEmitter(final JavaMethodBindingEmitter methodToWrap, final boolean callThroughProcAddress,
            final String getProcAddressTableExpr, final boolean changeNameAndArguments, final ProcAddressEmitter emitter) {

        super(methodToWrap);

        this.callThroughProcAddress = callThroughProcAddress;
        this.getProcAddressTableExpr = getProcAddressTableExpr;
        this.changeNameAndArguments = changeNameAndArguments;
        this.emitter = emitter;

        if (callThroughProcAddress) {
            setCommentEmitter(new WrappedMethodCommentEmitter());
        }

        if (methodToWrap.getBinding().hasContainingType()) {
            throw new IllegalArgumentException(
                    "Cannot create proc. address wrapper; method has containing type: \""
                    + methodToWrap.getBinding() + "\"");
        }
    }

    public ProcAddressJavaMethodBindingEmitter(final ProcAddressJavaMethodBindingEmitter methodToWrap) {
        this(methodToWrap, methodToWrap.callThroughProcAddress, methodToWrap.getProcAddressTableExpr,
             methodToWrap.changeNameAndArguments, methodToWrap.emitter);
    }

    @Override
    public String getImplName() {
        final String res = super.getImplName();
        if (changeNameAndArguments) {
            return ProcAddressEmitter.WRAP_PREFIX + res;
        }
        return res;
    }

    @Override
    protected int emitArguments(final PrintWriter writer) {
        int numEmitted = super.emitArguments(writer);
        if (callThroughProcAddress) {
            if (changeNameAndArguments) {
                if (numEmitted > 0) {
                    writer.print(", ");
                }

                writer.print("long procAddress");
                ++numEmitted;
            }
        }

        return numEmitted;
    }

    @Override
    protected String getNativeImplMethodName() {
        final String name = super.getNativeImplMethodName();
        if (callThroughProcAddress) {
            return ProcAddressEmitter.WRAP_PREFIX + name;
        }
        return name;
    }

    @Override
    protected void emitPreCallSetup(final MethodBinding binding, final PrintWriter writer) {
        super.emitPreCallSetup(binding, writer);

        if (callThroughProcAddress) {
            final String procAddressVariable = ProcAddressEmitter.PROCADDRESS_VAR_PREFIX + binding.getNativeName();
            writer.println("    final long __addr_ = " + getProcAddressTableExpr + "." + procAddressVariable + ";");
            writer.println("    if (__addr_ == 0) {");
            writer.format("      throw new %s(String.format(\"Method \\\"%%s\\\" not available\", \"%s\"));%n",
                          emitter.unsupportedExceptionType(), binding.getName());
            writer.println("    }");
        }
    }

    @Override
    protected int emitCallArguments(final MethodBinding binding, final PrintWriter writer) {
        int numEmitted = super.emitCallArguments(binding, writer);
        if (callThroughProcAddress) {
            if (numEmitted > 0) {
                writer.print(", ");
            }
            writer.print("__addr_");
            ++numEmitted;
        }

        return numEmitted;
    }

    /** This class emits the comment for the wrapper method */
    public class WrappedMethodCommentEmitter extends JavaMethodBindingEmitter.DefaultCommentEmitter {

        @Override
        protected void emitBeginning(final FunctionEmitter methodEmitter, final PrintWriter writer) {
            writer.print("Entry point (through function pointer) to C language function: <br> ");
        }
    }
}
