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
import com.jogamp.gluegen.JavaConfiguration;
import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.FunctionEmitter;
import com.jogamp.gluegen.CodeGenUtils;
import com.jogamp.gluegen.JavaMethodBindingEmitter;
import com.jogamp.gluegen.JavaEmitter;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import com.jogamp.gluegen.*;
import com.jogamp.gluegen.cgram.types.*;
import com.jogamp.gluegen.runtime.*;

/**
 * A subclass of JavaEmitter that modifies the normal emission of C
 * and Java code to allow dynamic lookups of the C entry points
 * associated with the Java methods.
 */
public class ProcAddressEmitter extends JavaEmitter {

    public static final String PROCADDRESS_VAR_PREFIX = ProcAddressTable.PROCADDRESS_VAR_PREFIX;
    protected static final String WRAP_PREFIX = "dispatch_";
    private TypeDictionary typedefDictionary;
    protected PrintWriter tableWriter;
    protected Set<String> emittedTableEntries;
    protected String tableClassPackage;
    protected String tableClassName;

    @Override
    public void beginFunctions(TypeDictionary typedefDictionary, TypeDictionary structDictionary, Map<Type, Type>  canonMap) throws Exception {
        this.typedefDictionary = typedefDictionary;

        if (getProcAddressConfig().emitProcAddressTable()) {
            beginProcAddressTable();
        }
        super.beginFunctions(typedefDictionary, structDictionary, canonMap);
    }

    @Override
    public void endFunctions() throws Exception {
        if (getProcAddressConfig().emitProcAddressTable()) {
            endProcAddressTable();
        }
        super.endFunctions();
    }

    @Override
    public void beginStructs(TypeDictionary typedefDictionary, TypeDictionary structDictionary, Map<Type, Type>  canonMap) throws Exception {
        super.beginStructs(typedefDictionary, structDictionary, canonMap);
    }

    public String runtimeExceptionType() {
        return getConfig().runtimeExceptionType();
    }

    public String unsupportedExceptionType() {
        return getConfig().unsupportedExceptionType();
    }

    @Override
    protected JavaConfiguration createConfig() {
        return new ProcAddressConfiguration();
    }

    @Override
    protected List<? extends FunctionEmitter> generateMethodBindingEmitters(Set<MethodBinding> methodBindingSet, FunctionSymbol sym) throws Exception {
        return generateMethodBindingEmittersImpl(methodBindingSet, sym);
    }

    protected boolean needsModifiedEmitters(FunctionSymbol sym) {
        if (!needsProcAddressWrapper(sym)
                || getConfig().isUnimplemented(getAliasedSymName(sym))) {
            return false;
        }

        return true;
    }

    private List<? extends FunctionEmitter> generateMethodBindingEmittersImpl(Set<MethodBinding> methodBindingSet, FunctionSymbol sym) throws Exception {
        List<? extends FunctionEmitter> defaultEmitters = super.generateMethodBindingEmitters(methodBindingSet, sym);

        // if the superclass didn't generate any bindings for the symbol, let's
        // honor that (for example, the superclass might have caught an Ignore
        // direction that matched the symbol's name).
        if (defaultEmitters.isEmpty()) {
            return defaultEmitters;
        }

        // Don't do anything special if this symbol doesn't require
        // modifications
        if (!needsModifiedEmitters(sym)) {
            return defaultEmitters;
        }

        ArrayList<FunctionEmitter> modifiedEmitters = new ArrayList<FunctionEmitter>(defaultEmitters.size());

        if (needsProcAddressWrapper(sym)) {
            if (getProcAddressConfig().emitProcAddressTable()) {
                // emit an entry in the GL proc address table for this method.
                emitProcAddressTableEntryForString(getAliasedSymName(sym));
            }
        }
        for (FunctionEmitter emitter : defaultEmitters) {
            if (emitter instanceof JavaMethodBindingEmitter) {
                generateModifiedEmitters((JavaMethodBindingEmitter)emitter, modifiedEmitters);
            } else if (emitter instanceof CMethodBindingEmitter) {
                generateModifiedEmitters((CMethodBindingEmitter) emitter, modifiedEmitters);
            } else {
                throw new RuntimeException("Unexpected emitter type: " + emitter.getClass().getName());
            }
        }

        return modifiedEmitters;
    }

    /**
     * Returns the name of the typedef for a pointer to the function
     * represented by the argument as defined by the ProcAddressNameExpr
     * in the .cfg file. For example, in the OpenGL headers, if the
     * argument is the function "glFuncName", the value returned will be
     * "PFNGLFUNCNAMEPROC". This returns a valid string regardless of
     * whether or not the typedef is actually defined.
     */
    protected String getFunctionPointerTypedefName(FunctionSymbol sym) {
        return getProcAddressConfig().convertToFunctionPointerName(sym.getName());
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

  protected void generateModifiedEmitters(JavaMethodBindingEmitter baseJavaEmitter, List<FunctionEmitter> emitters) {
        if (getConfig().manuallyImplement(baseJavaEmitter.getName())) {
            // User will provide Java-side implementation of this routine;
            // pass through any emitters which will produce signatures for
            // it unmodified
            emitters.add(baseJavaEmitter);
            return;
        }

        // See whether we need a proc address entry for this one
        boolean callThroughProcAddress = needsProcAddressWrapper(baseJavaEmitter.getBinding().getCSymbol());

        ProcAddressJavaMethodBindingEmitter emitter =
                new ProcAddressJavaMethodBindingEmitter(baseJavaEmitter,
                callThroughProcAddress,
                getProcAddressConfig().getProcAddressTableExpr(),
                baseJavaEmitter.isForImplementingMethodCall(),
                this);
        emitters.add(emitter);

        // If this emitter doesn't have a body (i.e., is a direct native
        // call with no intervening argument processing), we need to force
        // it to emit a body, and produce another one to act as the entry
        // point
        // FIXME: the negative test against the PRIVATE modifier is a
        // nasty hack to prevent the ProcAddressJavaMethodBindingEmitter
        // from incorrectly introducing method bodies to the private
        // native implementing methods; want this to work at least for
        // public and package-private methods
        if (baseJavaEmitter.signatureOnly()
                && !baseJavaEmitter.hasModifier(JavaMethodBindingEmitter.PRIVATE)
                && baseJavaEmitter.hasModifier(JavaMethodBindingEmitter.NATIVE)
                && callThroughProcAddress) {
            emitter.setEmitBody(true);
            emitter.removeModifier(JavaMethodBindingEmitter.NATIVE);
            emitter = new ProcAddressJavaMethodBindingEmitter(baseJavaEmitter,
                    callThroughProcAddress,
                    getProcAddressConfig().getProcAddressTableExpr(),
                    true,
                    this);
            emitter.setForImplementingMethodCall(true);
            emitters.add(emitter);
        }
    }

    protected void generateModifiedEmitters(CMethodBindingEmitter baseCEmitter, List<FunctionEmitter> emitters) {

        FunctionSymbol cSymbol = baseCEmitter.getBinding().getCSymbol();

        // See whether we need a proc address entry for this one
        boolean callThroughProcAddress = needsProcAddressWrapper(cSymbol);
        boolean forceProcAddress = getProcAddressConfig().forceProcAddressGen(cSymbol.getName());

        String forcedCallingConvention = null;
        if (forceProcAddress) {
            forcedCallingConvention = getProcAddressConfig().getLocalProcAddressCallingConvention(cSymbol.getName());
        }
        // Note that we don't care much about the naming of the C argument
        // variables so to keep things simple we ignore the buffer object
        // property for the binding

        // The C-side JNI binding for this particular function will have an
        // extra final argument, which is the address (the OpenGL procedure
        // address) of the function it needs to call
        ProcAddressCMethodBindingEmitter res = new ProcAddressCMethodBindingEmitter(
                baseCEmitter, callThroughProcAddress, forceProcAddress, forcedCallingConvention, this);

        MessageFormat exp = baseCEmitter.getReturnValueCapacityExpression();
        if (exp != null) {
            res.setReturnValueCapacityExpression(exp);
        }
        emitters.add(res);
    }

    private String getAliasedSymName(FunctionSymbol sym) {
        String symName = getConfig().getJavaSymbolRename(sym.getName());
        if (null == symName) {
            symName = sym.getName();
        }
        return symName;
    }

    protected boolean needsProcAddressWrapper(FunctionSymbol sym) {
        String symName = getAliasedSymName(sym);

        ProcAddressConfiguration config = getProcAddressConfig();

        // We should only generate code to call through a function pointer
        // if the symbol has an associated function pointer typedef.
        String funcPointerTypedefName = getFunctionPointerTypedefName(sym);
        boolean shouldWrap = typedefDictionary.containsKey(funcPointerTypedefName);
        //System.err.println(funcPointerTypedefName + " defined: " + shouldWrap);

        if (config.skipProcAddressGen(symName)) {
            shouldWrap = false;
        }

        if (config.forceProcAddressGen(symName)) {
            shouldWrap = true;
        }

        if (shouldWrap) {
            // Hoist argument names from function pointer if not supplied in prototype
            Type funcPointerType = typedefDictionary.get(funcPointerTypedefName);
            if (funcPointerType != null) {
                FunctionType typedef = funcPointerType.asPointer().getTargetType().asFunction();
                FunctionType fun = sym.getType();
                int numarg = typedef.getNumArguments();
                for (int i = 0; i < numarg; i++) {
                    if (fun.getArgumentName(i) == null) {
                        fun.setArgumentName(i, typedef.getArgumentName(i));
                    }
                }
            }
        }

        return shouldWrap;
    }

    protected void beginProcAddressTable() throws Exception {
        tableClassPackage = getProcAddressConfig().tableClassPackage();
        tableClassName = getProcAddressConfig().tableClassName();

        // Table defaults to going into the impl directory unless otherwise overridden
        String implPackageName = tableClassPackage;
        if (implPackageName == null) {
            implPackageName = getImplPackageName();
        }
        String jImplRoot = getJavaOutputDir() + File.separator + CodeGenUtils.packageAsPath(implPackageName);

        tableWriter = openFile(jImplRoot + File.separator + tableClassName + ".java");
        emittedTableEntries = new HashSet<String>();

        CodeGenUtils.emitAutogeneratedWarning(tableWriter, this);

        tableWriter.println("package " + implPackageName + ";");
        tableWriter.println();
        for (String imporT : getConfig().imports()) {
            tableWriter.println("import " + imporT + ";");
        }
        tableWriter.println("import " + ProcAddressTable.class.getName() + ";");
        tableWriter.println();

        tableWriter.println("/**");
        tableWriter.println(" * This table is a cache of pointers to the dynamically-linkable C library.");
        tableWriter.println(" * @see " + ProcAddressTable.class.getSimpleName());
        tableWriter.println(" */");
        tableWriter.println("public class " + tableClassName + " extends "+ ProcAddressTable.class.getSimpleName() + " {");
        tableWriter.println();

        for (String string : getProcAddressConfig().getForceProcAddressGen()) {
            emitProcAddressTableEntryForString(string);
        }

        tableWriter.println();
        tableWriter.println("  public "+tableClassName+"(){ super(); }");
        tableWriter.println();
        tableWriter.println("  public "+tableClassName+"("+FunctionAddressResolver.class.getName()+" resolver){ super(resolver); }");
        tableWriter.println();

    }

    protected void endProcAddressTable() throws Exception {
        tableWriter.println("} // end of class " + tableClassName);
        tableWriter.flush();
        tableWriter.close();
    }

    protected void emitProcAddressTableEntryForString(String str) {
        // Deal gracefully with forced proc address generation in the face
        // of having the function pointer typedef in the header file too
        if (emittedTableEntries.contains(str)) {
            return;
        }
        emittedTableEntries.add(str);
        tableWriter.print("  public long ");
        tableWriter.print(PROCADDRESS_VAR_PREFIX);
        tableWriter.print(str);
        tableWriter.println(";");
    }

    protected ProcAddressConfiguration getProcAddressConfig() {
        return (ProcAddressConfiguration) getConfig();
    }
}
