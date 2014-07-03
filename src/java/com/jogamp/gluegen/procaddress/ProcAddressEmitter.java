/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2013 JogAmp Community. All rights reserved.
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

import java.io.File;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jogamp.gluegen.CMethodBindingEmitter;
import com.jogamp.gluegen.CodeGenUtils;
import com.jogamp.gluegen.FunctionEmitter;
import com.jogamp.gluegen.JavaConfiguration;
import com.jogamp.gluegen.JavaEmitter;
import com.jogamp.gluegen.JavaMethodBindingEmitter;
import com.jogamp.gluegen.MethodBinding;
import com.jogamp.gluegen.cgram.types.FunctionSymbol;
import com.jogamp.gluegen.cgram.types.Type;
import com.jogamp.gluegen.cgram.types.TypeDictionary;
import com.jogamp.gluegen.runtime.FunctionAddressResolver;
import com.jogamp.gluegen.runtime.ProcAddressTable;

/**
 * A subclass of JavaEmitter that modifies the normal emission of C
 * and Java code to allow dynamic lookups of the C entry points
 * associated with the Java methods.
 */
public class ProcAddressEmitter extends JavaEmitter {

    /** Must be synchronized w/ ProcAddressTable.PROCADDRESS_VAR_PREFIX !!! */
    public static final String PROCADDRESS_VAR_PREFIX = "_addressof_";
    protected static final String WRAP_PREFIX = "dispatch_";
    private TypeDictionary typedefDictionary;
    protected PrintWriter tableWriter;
    protected Set<String> emittedTableEntries;
    protected String tableClassPackage;
    protected String tableClassName;

    @Override
    public void beginFunctions(final TypeDictionary typedefDictionary, final TypeDictionary structDictionary, final Map<Type, Type>  canonMap) throws Exception {
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
    public void beginStructs(final TypeDictionary typedefDictionary, final TypeDictionary structDictionary, final Map<Type, Type>  canonMap) throws Exception {
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
    protected List<? extends FunctionEmitter> generateMethodBindingEmitters(final Set<MethodBinding> methodBindingSet, final FunctionSymbol sym) throws Exception {
        return generateMethodBindingEmittersImpl(methodBindingSet, sym);
    }

    protected boolean needsModifiedEmitters(final FunctionSymbol sym) {
        if (!needsProcAddressWrapper(sym) || getConfig().isUnimplemented(getAliasedSymName(sym))) {
            return false;
        }

        return true;
    }

    private List<? extends FunctionEmitter> generateMethodBindingEmittersImpl(final Set<MethodBinding> methodBindingSet, final FunctionSymbol sym) throws Exception {
        final List<? extends FunctionEmitter> defaultEmitters = super.generateMethodBindingEmitters(methodBindingSet, sym);

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

        final ArrayList<FunctionEmitter> modifiedEmitters = new ArrayList<FunctionEmitter>(defaultEmitters.size());

        if (needsProcAddressWrapper(sym)) {
            if (getProcAddressConfig().emitProcAddressTable()) {
                // emit an entry in the GL proc address table for this method.
                emitProcAddressTableEntryForString(getAliasedSymName(sym));
            }
        }
        for (final FunctionEmitter emitter : defaultEmitters) {
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
    protected String getFunctionPointerTypedefName(final FunctionSymbol sym) {
        return getProcAddressConfig().convertToFunctionPointerName(sym.getName());
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

  /** If 'native', enforce 'private native' modifiers. */
  protected void fixSecurityModifiers(final JavaMethodBindingEmitter javaEmitter) {
    if(  javaEmitter.hasModifier(JavaMethodBindingEmitter.NATIVE) &&
        !javaEmitter.hasModifier(JavaMethodBindingEmitter.PRIVATE) )
    {
        javaEmitter.removeModifier(JavaMethodBindingEmitter.PUBLIC);
        javaEmitter.removeModifier(JavaMethodBindingEmitter.PROTECTED);
        javaEmitter.removeModifier(JavaMethodBindingEmitter.NATIVE);
        javaEmitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
        javaEmitter.addModifier(JavaMethodBindingEmitter.NATIVE);
    }
  }

  protected void generateModifiedEmitters(final JavaMethodBindingEmitter baseJavaEmitter, final List<FunctionEmitter> emitters) {
        // See whether we need a proc address entry for this one
        final boolean callThroughProcAddress = needsProcAddressWrapper(baseJavaEmitter.getBinding().getCSymbol());

        // If this emitter doesn't have a body (i.e., is a direct native
        // call with no intervening argument processing), we need to force
        // it to emit a body, and produce another one to act as the entry
        // point
        // FIXME: the negative test against the PRIVATE modifier is a
        // nasty hack to prevent the ProcAddressJavaMethodBindingEmitter
        // from incorrectly introducing method bodies to the private
        // native implementing methods; want this to work at least for
        // public and package-private methods
        final boolean needsJavaWrapper = baseJavaEmitter.signatureOnly() &&
                                        !baseJavaEmitter.hasModifier(JavaMethodBindingEmitter.PRIVATE) &&
                                         baseJavaEmitter.hasModifier(JavaMethodBindingEmitter.NATIVE) &&
                                         callThroughProcAddress;


        {
            final ProcAddressJavaMethodBindingEmitter emitter = new ProcAddressJavaMethodBindingEmitter(baseJavaEmitter,
                    callThroughProcAddress,
                    getProcAddressConfig().getProcAddressTableExpr(),
                    baseJavaEmitter.isForImplementingMethodCall(),
                    this);
            if( needsJavaWrapper ) {
                emitter.setEmitBody(true);
                emitter.removeModifier(JavaMethodBindingEmitter.NATIVE);
            } else if ( callThroughProcAddress ) {
                fixSecurityModifiers(emitter);
            }
            emitters.add(emitter);
        }

        if( needsJavaWrapper ) {
            final ProcAddressJavaMethodBindingEmitter emitter = new ProcAddressJavaMethodBindingEmitter(baseJavaEmitter,
                    callThroughProcAddress,
                    getProcAddressConfig().getProcAddressTableExpr(),
                    true,
                    this);
            emitter.setForImplementingMethodCall(true);
            fixSecurityModifiers(emitter);
            emitters.add(emitter);
        }
    }

    protected void generateModifiedEmitters(final CMethodBindingEmitter baseCEmitter, final List<FunctionEmitter> emitters) {

        final FunctionSymbol cSymbol = baseCEmitter.getBinding().getCSymbol();

        // See whether we need a proc address entry for this one
        final boolean callThroughProcAddress = needsProcAddressWrapper(cSymbol);
        final boolean forceProcAddress = getProcAddressConfig().forceProcAddressGen(cSymbol.getName());

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
        final ProcAddressCMethodBindingEmitter res = new ProcAddressCMethodBindingEmitter(
                baseCEmitter, callThroughProcAddress, forceProcAddress, forcedCallingConvention, this);

        final MessageFormat exp = baseCEmitter.getReturnValueCapacityExpression();
        if (exp != null) {
            res.setReturnValueCapacityExpression(exp);
        }
        emitters.add(res);
    }

    private String getAliasedSymName(final FunctionSymbol sym) {
        String symName = getConfig().getJavaSymbolRename(sym.getName());
        if (null == symName) {
            symName = sym.getName();
        }
        return symName;
    }

    protected boolean needsProcAddressWrapper(final FunctionSymbol sym) {
        final String symName = getAliasedSymName(sym);

        final ProcAddressConfiguration config = getProcAddressConfig();

        // We should only generate code to call through a function pointer
        // if the symbol has an associated function pointer typedef.
        final String funcPointerTypedefName = getFunctionPointerTypedefName(sym);
        boolean shouldWrap = typedefDictionary.containsKey(funcPointerTypedefName);
        //System.err.println(funcPointerTypedefName + " defined: " + shouldWrap);

        if (config.skipProcAddressGen(symName)) {
            shouldWrap = false;
        }

        if (config.forceProcAddressGen(symName)) {
            shouldWrap = true;
        }

        return shouldWrap;
    }

    protected void beginProcAddressTable() throws Exception {
        final ProcAddressConfiguration cfg = getProcAddressConfig();
        tableClassPackage = cfg.tableClassPackage();
        tableClassName = cfg.tableClassName();

        // Table defaults to going into the impl directory unless otherwise overridden
        String implPackageName = tableClassPackage;
        if (implPackageName == null) {
            implPackageName = getImplPackageName();
        }
        final String fullTableClassName = implPackageName + "." + tableClassName;
        final MethodAccess tableClassAccess = cfg.accessControl(fullTableClassName);

        final String jImplRoot = getJavaOutputDir() + File.separator + CodeGenUtils.packageAsPath(implPackageName);

        tableWriter = openFile(jImplRoot + File.separator + tableClassName + ".java", tableClassName);
        emittedTableEntries = new HashSet<String>();

        CodeGenUtils.emitAutogeneratedWarning(tableWriter, this);

        tableWriter.println("package " + implPackageName + ";");
        tableWriter.println();
        for (final String imporT : getConfig().imports()) {
            tableWriter.println("import " + imporT + ";");
        }
        tableWriter.println("import " + ProcAddressTable.class.getName() + ";");
        tableWriter.println("import com.jogamp.common.util.SecurityUtil;");
        tableWriter.println();

        tableWriter.println("/**");
        tableWriter.println(" * This table is a cache of pointers to the dynamically-linkable C library.");
        tableWriter.println(" * @see " + ProcAddressTable.class.getSimpleName());
        tableWriter.println(" */");
        tableWriter.println(tableClassAccess.getJavaName() + " final class " + tableClassName + " extends "+ ProcAddressTable.class.getSimpleName() + " {");
        tableWriter.println();

        for (final String string : getProcAddressConfig().getForceProcAddressGen()) {
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

    protected void emitProcAddressTableEntryForString(final String str) {
        // Deal gracefully with forced proc address generation in the face
        // of having the function pointer typedef in the header file too
        if (emittedTableEntries.contains(str)) {
            return;
        }
        emittedTableEntries.add(str);
        tableWriter.print("  /* pp */ long ");
        tableWriter.print(PROCADDRESS_VAR_PREFIX);
        tableWriter.print(str);
        tableWriter.println(";");
    }

    protected ProcAddressConfiguration getProcAddressConfig() {
        return (ProcAddressConfiguration) getConfig();
    }
}
