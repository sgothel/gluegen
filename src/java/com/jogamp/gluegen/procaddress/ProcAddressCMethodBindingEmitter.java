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

import com.jogamp.gluegen.CMethodBindingEmitter;
import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.JavaType;

import java.io.*;

import com.jogamp.gluegen.cgram.types.*;

public class ProcAddressCMethodBindingEmitter extends CMethodBindingEmitter {

  private boolean callThroughProcAddress;
  private boolean hasProcAddrTypedef;

  private String localTypedefCallingConvention;

  private static final String procAddressJavaTypeName = JavaType.createForClass(Long.TYPE).jniTypeName();
  private ProcAddressEmitter emitter;

  public ProcAddressCMethodBindingEmitter(final CMethodBindingEmitter methodToWrap,
                                          final boolean callThroughProcAddress,
                                          final boolean hasProcAddrTypedef,
                                          final String localTypedefCallingConvention,
                                          final ProcAddressEmitter emitter) {

        super(
                new MethodBinding(methodToWrap.getBinding()) {
                    @Override
                    public String getImplName() {
                        if (callThroughProcAddress) {
                            return ProcAddressEmitter.WRAP_PREFIX + super.getImplName();
                        } else {
                            return super.getImplName();
                        }
                    }
                },
                methodToWrap.getDefaultOutput(),
                methodToWrap.getJavaPackageName(),
                methodToWrap.getJavaClassName(),
                methodToWrap.getIsOverloadedBinding(),
                methodToWrap.getIsJavaMethodStatic(),
                true,
                methodToWrap.forIndirectBufferAndArrayImplementation(),
                methodToWrap.getMachineDataInfo(),
                emitter.getConfiguration()
        );
        if (methodToWrap.getReturnValueCapacityExpression() != null) {
            setReturnValueCapacityExpression(methodToWrap.getReturnValueCapacityExpression());
        }
        if (methodToWrap.getReturnValueLengthExpression() != null) {
            setReturnValueLengthExpression(methodToWrap.getReturnValueLengthExpression());
        }
        setTemporaryCVariableDeclarations(methodToWrap.getTemporaryCVariableDeclarations());
        setTemporaryCVariableAssignments(methodToWrap.getTemporaryCVariableAssignments());

        setCommentEmitter(defaultCommentEmitter);

        this.callThroughProcAddress = callThroughProcAddress;
        this.hasProcAddrTypedef = hasProcAddrTypedef;
        this.localTypedefCallingConvention = localTypedefCallingConvention;
        this.emitter = emitter;
    }

    @Override
    protected int emitArguments(final PrintWriter writer) {
        int numEmitted = super.emitArguments(writer);
        if (callThroughProcAddress) {
            if (numEmitted > 0) {
                writer.print(", ");
            }
            writer.print(procAddressJavaTypeName);
            writer.print(" procAddress");
            ++numEmitted;
        }

        return numEmitted;
    }

    @Override
    protected void emitBodyVariableDeclarations(final PrintWriter writer) {
        if (callThroughProcAddress) {
            // create variable for the function pointer with the right type, and set
            // it to the value of the passed-in proc address
            final FunctionSymbol cSym = binding.getCSymbol();

            // Always emit the local typedef, based on our parsing results.
            // In case we do have the public typedef from the original header,
            // we use it for the local var and assign our proc-handle to it,
            // cast to the local typedef.
            // This allows the native C compiler to validate our types!
            final String funcPointerTypedefBaseName = emitter.getFunctionPointerTypedefName(cSym);
            final String funcPointerTypedefLocalName = "_local_" + funcPointerTypedefBaseName;
            final String funcPointerTypedefName;
            if (hasProcAddrTypedef) {
                funcPointerTypedefName = funcPointerTypedefBaseName;
            } else {
                funcPointerTypedefName = funcPointerTypedefLocalName;
            }
            final PointerType funcPtrType = new PointerType(null, cSym.getType(), 0);

            writer.print("  typedef ");
            writer.print(funcPtrType.toString(funcPointerTypedefLocalName, localTypedefCallingConvention));
            writer.println(";");

            writer.print("  ");
            writer.print(funcPointerTypedefName); // Uses public typedef if available!
            writer.print(" ptr_");
            writer.print(getNativeName());
            writer.println(";");
        }

        super.emitBodyVariableDeclarations(writer);
    }

    @Override
    protected void emitBodyVariablePreCallSetup(final PrintWriter writer) {
        super.emitBodyVariablePreCallSetup(writer);

        if (callThroughProcAddress) {
            // set the function pointer to the value of the passed-in procAddress
            // See above notes in emitBodyVariableDeclarations(..)!
            final String funcPointerTypedefBaseName = emitter.getFunctionPointerTypedefName(binding.getCSymbol());
            final String funcPointerTypedefLocalName = "_local_" + funcPointerTypedefBaseName;
            final String funcPointerTypedefName;
            if (hasProcAddrTypedef) {
                funcPointerTypedefName = funcPointerTypedefBaseName;
            } else {
                funcPointerTypedefName = funcPointerTypedefLocalName;
            }

            final String ptrVarName = "ptr_" + getNativeName();

            if (hasProcAddrTypedef) {
                writer.println("  // implicit type validation of "+funcPointerTypedefLocalName+" -> "+funcPointerTypedefName);
            }
            writer.print("  ");
            writer.print(ptrVarName);
            writer.print(" = (");
            writer.print(funcPointerTypedefLocalName);
            writer.println(") (intptr_t) procAddress;");

            writer.println("  assert(" + ptrVarName + " != NULL);");
        }
    }

    @Override
    protected void emitBodyCallCFunction(final PrintWriter writer) {
        if (!callThroughProcAddress) {
            super.emitBodyCallCFunction(writer);
        } else {
            // Make the call to the actual C function
            writer.print("  ");

            // WARNING: this code assumes that the return type has already been
            // typedef-resolved.
            final Type cReturnType = binding.getCReturnType();

            if (!cReturnType.isVoid()) {
                // Note we respect const/volatile in the function return type.
                // However, we cannot have it 'const' for our local variable.
                // See return type in CMethodBindingEmitter.emitBodyVariableDeclarations(..)!
                writer.print("_res = (");
                writer.print(cReturnType.getCName(false));
                writer.print(") ");
            }
            final MethodBinding mBinding = getBinding();
            if (mBinding.hasContainingType()) {
                // FIXME: this can and should be handled and unified with the
                // associated code in the CMethodBindingEmitter
                throw new IllegalStateException("Cannot call through function pointer because binding has containing type: " + mBinding);
            }

            // call throught the run-time function pointer
            writer.print("(* ptr_");
            writer.print(getNativeName());
            writer.print(") ");
            writer.print("(");
            emitBodyPassCArguments(writer);
            writer.println(");");
        }
    }

    @Override
    protected String jniMangle(final MethodBinding binding) {
        final StringBuilder buf = new StringBuilder(super.jniMangle(binding));
        if (callThroughProcAddress) {
            jniMangle(Long.TYPE, buf, false);  // to account for the additional _addr_ parameter
        }
        return buf.toString();
    }
}
