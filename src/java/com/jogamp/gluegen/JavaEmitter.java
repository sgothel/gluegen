/*
 * Copyright (c) 2010-2023 JogAmp Community. All rights reserved.
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

package com.jogamp.gluegen;

import static com.jogamp.gluegen.JavaEmitter.MethodAccess.PACKAGE_PRIVATE;
import static com.jogamp.gluegen.JavaEmitter.MethodAccess.PRIVATE;
import static com.jogamp.gluegen.JavaEmitter.MethodAccess.PROTECTED;
import static com.jogamp.gluegen.JavaEmitter.MethodAccess.PUBLIC;
import static com.jogamp.gluegen.JavaEmitter.MethodAccess.PUBLIC_ABSTRACT;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Level.SEVERE;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jogamp.common.os.MachineDataInfoRuntime;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.common.util.ArrayHashMap;
import com.jogamp.gluegen.ASTLocusTag.ASTLocusTagProvider;
import com.jogamp.gluegen.Logging.LoggerIf;
import com.jogamp.gluegen.cgram.types.AliasedSymbol;
import com.jogamp.gluegen.cgram.types.ArrayType;
import com.jogamp.gluegen.cgram.types.CompoundType;
import com.jogamp.gluegen.cgram.types.Field;
import com.jogamp.gluegen.cgram.types.FunctionSymbol;
import com.jogamp.gluegen.cgram.types.PointerType;
import com.jogamp.gluegen.cgram.types.SizeThunk;
import com.jogamp.gluegen.cgram.types.StructLayout;
import com.jogamp.gluegen.cgram.types.Type;
import com.jogamp.gluegen.cgram.types.TypeComparator.AliasedSemanticSymbol;
import com.jogamp.gluegen.cgram.types.TypeDictionary;

// PROBLEMS:
//  - what if something returns 'const int *'? Could we
//    return an IntBuffer that has read-only behavior? Or do we copy the array
//    (but we don't know its size!). What do we do if it returns a non-const
//    int*? Should the user be allowed to write back to the returned pointer?
//
//  - Non-const array types must be properly released with JNI_COMMIT
//    in order to see side effects if the array was copied.


public class JavaEmitter implements GlueEmitter {

  private StructLayout layout;
  private Map<Type, Type> canonMap;
  protected JavaConfiguration cfg;

  /**
   * Style of code emission. Can emit everything into one class
   * (AllStatic), separate interface and implementing classes
   * (InterfaceAndImpl), only the interface (InterfaceOnly), or only
   * the implementation (ImplOnly).
   */
  public enum EmissionStyle {AllStatic, InterfaceAndImpl, InterfaceOnly, ImplOnly}

  /**
   * Access control for emitted Java methods.
   */
  public enum MethodAccess {
      PUBLIC("public"), PROTECTED("protected"), PRIVATE("private"), PACKAGE_PRIVATE("/* pp */"), PUBLIC_ABSTRACT("abstract");

      public final String getJavaName() { return javaName; }

      MethodAccess(final String javaName) {
          this.javaName = javaName;
      }
      private final String javaName;
  }

  /**
   * Resource ownership.
   */
  public enum Ownership {
      /** Parent ownership of resource, i.e. derived-from and shared-with compounding parent resource. */
      Parent,
      /** Java ownership of resource. */
      Java,
      /** Native ownership of resource. */
      Native,
      /** Ambiguous mixed ownership of resource, i.e. {@Link #Java} or {@link #Native}. */
      Mixed;
  }

  private JavaCodeUnit javaUnit;      // Covers either interface or, in AllStatic mode, everything
  private JavaCodeUnit javaImplUnit;  // Only used in non-AllStatic modes for impl class
  private CCodeUnit cUnit;
  private final MachineDataInfo machDescJava = MachineDataInfo.StaticConfig.LP64_UNIX.md;
  private final MachineDataInfo.StaticConfig[] machDescTargetConfigs = MachineDataInfo.StaticConfig.values();

  protected final LoggerIf LOG;

  public JavaEmitter() {
      LOG = Logging.getLogger(JavaEmitter.class.getPackage().getName(), JavaEmitter.class.getSimpleName());
  }

  @Override
  public void readConfigurationFile(final String filename) throws Exception {
    cfg = createConfig();
    cfg.read(filename);
  }

  @Override
  public JavaConfiguration getConfiguration() { return cfg; }

  class ConstFuncRenamer implements SymbolFilter {
    private List<ConstantDefinition> constants;
    private List<FunctionSymbol> functions;

    @Override
    public List<ConstantDefinition> getConstants() {
      return constants;
    }
    @Override
    public List<FunctionSymbol> getFunctions() {
      return functions;
    }

    private <T extends AliasedSemanticSymbol> List<T> filterSymbolsInt(final List<T> inList,
                                                                       final boolean preserveOrder,
                                                                       final List<T> outList) {
        final JavaConfiguration cfg = getConfig();
        final ArrayHashMap<String, T> symMap =
                new ArrayHashMap<String, T>(false, 100, ArrayHashMap.DEFAULT_LOAD_FACTOR);
        for (final T sym : inList) {
            final String origName = sym.getName();
            final String newName = cfg.getJavaSymbolRename(origName);
            final T dupSym;
            if( null != newName ) {
                // Alias Name
                dupSym = symMap.get(newName);
                if( null != dupSym ) {
                    // only rename to allow 'equalSemantics' to not care ..
                    sym.rename(newName);
                }
            } else {
                // Original Name
                dupSym = symMap.get(origName);
            }
            if( null != dupSym ) {
                // Duplicate alias .. check
                if( !dupSym.equalSemantics(sym) ) {
                    final ASTLocusTag loc;
                    final String preLoc;
                    if( sym instanceof ASTLocusTagProvider ) {
                        loc = ((ASTLocusTagProvider)sym).getASTLocusTag();
                    } else {
                        loc = null;
                    }
                    if( dupSym instanceof ASTLocusTagProvider ) {
                        preLoc = String.format(",%n  %s: previous definition is here",
                                ((ASTLocusTagProvider)dupSym).getASTLocusTag().toString(new StringBuilder(), "note", true));
                    } else {
                        preLoc = "";
                    }
                    final String mode = null != newName ? "alias" : "orig";
                    final String message =
                            String.format("Duplicate Name (%s) w/ incompatible value:%n  this '%s',%n  have '%s'%s",
                                    mode, sym.getAliasedString(), dupSym.getAliasedString(), preLoc);
                    throw new GlueGenException(message, loc);
                }
            }
            if( null != newName ) {
                // Alias Name
                if( null != dupSym ) {
                    // Duplicate alias .. add aliased name
                    dupSym.addAliasedName(origName);
                } else {
                    // No duplicate .. rename and add
                    sym.rename(newName);
                    symMap.put(newName, sym);
                }
            } else {
                // Original Name
                if( null != dupSym ) {
                    // Duplicate orig .. drop
                } else {
                    // No duplicate orig .. add
                    symMap.put(origName, sym);
                }
            }
        }
        outList.addAll(symMap.getData());
        if( !preserveOrder ) {
            // sort constants to make them easier to find in native code
            Collections.sort(outList, new Comparator<T>() {
                @Override
                public int compare(final T o1, final T o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
        }
        return outList;
    }

    @Override
    public void filterSymbols(final List<ConstantDefinition> inConstList, final List<FunctionSymbol> inFuncList) {
        constants = filterSymbolsInt(inConstList, true, new ArrayList<ConstantDefinition>(100));
        functions = filterSymbolsInt(inFuncList, true, new ArrayList<FunctionSymbol>(100));
    }
  }

    @Override
    public void beginEmission(final GlueEmitterControls controls) throws IOException {
        // Handle renaming of constants and functions
        controls.runSymbolFilter(new ConstFuncRenamer());

        // Request emission of any structs requested
        for (final String structs : cfg.forcedStructs()) {
            controls.forceStructEmission(structs);
        }

        if ( !cfg.structsOnly() ) {
            try {
                openCodeUnits();
            } catch (final Exception e) {
                throw new RuntimeException("Unable to open files for writing", e);
            }
            emitAllFileHeaders();
        }
    }

    @Override
    public void endEmission() {
        if ( !cfg.structsOnly() ) {
            emitAllFileFooters();

            try {
                closeWriters();
            } catch (final Exception e) {
                throw new RuntimeException("Unable to close open files", e);
            }
        }
    }

  @Override
  public void beginDefines() throws Exception {
    if ( ( cfg.allStatic() || cfg.emitInterface() ) && !cfg.structsOnly() ) {
      javaUnit().emitln();
    }
  }

  /** Mangle a class, package or function name for JNI usage, i.e. replace all '.' w/ '_' */
  protected static String jniMangle(final String name) {
    return name.replaceAll("_", "_1").replace('.', '_');
  }
  /** Returns the JNI method prefix consisting our of mangled package- and class-name */
  protected static String getJNIMethodNamePrefix(final String javaPackageName, final String javaClassName) {
      return "Java_"+jniMangle(javaPackageName)+"_"+jniMangle(javaClassName);
  }

  private final Map<String, ConstantDefinition.JavaExpr> constMap =
          new HashMap<String, ConstantDefinition.JavaExpr>();

  @Override
  public void emitDefine(final ConstantDefinition def, final String optionalComment) throws Exception  {
    if ( ( cfg.allStatic() || cfg.emitInterface() ) && !cfg.structsOnly() ) {
      // TODO: Some defines (e.g., GL_DOUBLE_EXT in gl.h) are defined in terms
      // of other defines -- should we emit them as references to the original
      // define (not even sure if the lexer supports this)? Right now they're
      // emitted as the numeric value of the original definition. If we decide
      // emit them as references we'll also have to emit them in the correct
      // order. It's probably not an issue right now because the emitter
      // currently only emits only numeric defines -- if it handled #define'd
      // objects it would make a bigger difference.

      if ( !cfg.shouldIgnoreInInterface(def) ) {
        final ConstantDefinition.JavaExpr constExpr = def.computeJavaExpr(constMap);
        constMap.put(def.getName(), constExpr);
        javaUnit().emit("  /** ");
        if (optionalComment != null && optionalComment.length() != 0) {
            javaUnit().emit(optionalComment);
            javaUnit().emit(" - ");
        }
        javaUnit().emit("CType: ");
        if( constExpr.resultType.isUnsigned ) {
            javaUnit().emit("unsigned ");
        }
        javaUnit().emit(constExpr.resultJavaTypeName);
        javaUnit().emitln(" */");
        javaUnit().emitln("  public static final " + constExpr.resultJavaTypeName +
                             " " + def.getName() + " = " + constExpr.javaExpression + ";");
      }
    }
  }

  @Override
  public void endDefines() throws Exception {
  }

  @Override
  public void beginFunctions(final TypeDictionary typedefDictionary,
                             final TypeDictionary structDictionary,
                             final Map<Type, Type> canonMap) throws Exception {

    // this.typedefDictionary = typedefDictionary;
    this.canonMap          = canonMap;

    if ( ( cfg.allStatic() || cfg.emitInterface() ) && !cfg.structsOnly() ) {
      javaUnit().emitln();
    }
  }

  @Override
  public Iterator<FunctionSymbol> emitFunctions(final List<FunctionSymbol> funcsToBind) throws Exception {
    if ( !cfg.structsOnly() ) {
        // Bind all the C funcs to Java methods
        final ArrayList<FunctionEmitter> methodBindingEmitters = new ArrayList<FunctionEmitter>(2*funcsToBind.size());
        {
            int i=0;
            for (final FunctionSymbol cFunc : funcsToBind) {
              // Check to see whether this function should be ignored
              if ( !cfg.shouldIgnoreInImpl(cFunc) ) {
                  methodBindingEmitters.addAll(generateMethodBindingEmitters(cFunc));
                  LOG.log(INFO, cFunc.getASTLocusTag(), "Non-Ignored Impl[{0}]: {1}", i++, cFunc);
              }

            }
        }

        // Emit all the methods
        {
            int i=0;
            for (final FunctionEmitter emitter : methodBindingEmitters) {
              try {
                final FunctionSymbol cFunc = emitter.getCSymbol();
                if ( !emitter.isInterface() || !cfg.shouldIgnoreInInterface(cFunc) ) {
                    emitter.emit();
                    emitter.getUnit().emitln(); // put newline after method body
                    LOG.log(INFO, cFunc.getASTLocusTag(), "Non-Ignored Intf[{0}]: {1}", i++, cFunc);
                }
              } catch (final Exception e) {
                throw new GlueGenException(
                    "Error while emitting binding for \"" + emitter.getCSymbol().getAliasedString() + "\"",
                    emitter.getCSymbol().getASTLocusTag(), e);
              }
            }
        }
    }
    // Return the list of FunctionSymbols that we generated gluecode for
    return funcsToBind.iterator();
  }

  /**
   * Create the object that will read and store configuration information for
   * this JavaEmitter.
   */
  protected JavaConfiguration createConfig() {
    return new JavaConfiguration();
  }

  /**
   * Get the configuration information for this JavaEmitter.
   */
  protected JavaConfiguration getConfig() {
    return cfg;
  }

  /**
   * Generates the public emitters for this MethodBinding which will
   * produce either simply signatures (for the interface class, if
   * any) or function definitions with or without a body (depending on
   * whether or not the implementing function can go directly to
   * native code because it doesn't need any processing of the
   * outgoing arguments).
   */
  protected void generatePublicEmitters(final MethodBinding binding, final List<FunctionEmitter> allEmitters,
                                        final boolean signatureOnly) {
      final FunctionSymbol cSymbol = binding.getCSymbol();
      if ( !signatureOnly && cfg.manuallyImplement(cSymbol) ) {
          // We only generate signatures for manually-implemented methods;
          // user provides the implementation
          return;
      }

      final MethodAccess accessControl;

      if ( !signatureOnly && null != binding.getDelegationImplName() ) {
          // private access for delegation implementation methods
          accessControl = PRIVATE;
      } else {
          accessControl = cfg.accessControl(binding.getName());
      }

      // We should not emit anything except public APIs into interfaces
      if ( signatureOnly && PUBLIC != accessControl ) {
          return;
      }

      // It's possible we may not need a body even if signatureOnly is
      // set to false; for example, if the routine doesn't take any
      // arrays or buffers as arguments
      final boolean isUnimplemented = cfg.isUnimplemented(cSymbol);
      final List<String> prologue = cfg.javaPrologueForMethod(binding, false, false);
      final List<String> epilogue = cfg.javaEpilogueForMethod(binding, false, false);
      final boolean needsBody = isUnimplemented ||
                                binding.needsNIOWrappingOrUnwrapping() ||
                                binding.signatureUsesJavaPrimitiveArrays() ||
                                null != prologue  ||
                                null != epilogue;

      final boolean emitBody = !signatureOnly && needsBody;
      final boolean isNativeMethod = !isUnimplemented && !needsBody && !signatureOnly;

      final CodeUnit unit = ((signatureOnly || cfg.allStatic()) ? javaUnit() : javaImplUnit());

      final JavaMethodBindingEmitter emitter =
              new JavaMethodBindingEmitter(binding,
                      unit,
                      cfg.runtimeExceptionType(),
                      cfg.unsupportedExceptionType(),
                      emitBody,        // emitBody
                      cfg.tagNativeBinding(),
                      false,           // eraseBufferAndArrayTypes
                      cfg.useNIOOnly(binding.getName()),
                      cfg.useNIODirectOnly(binding.getName()),
                      false,           // forDirectBufferImplementation
                      false,           // forIndirectBufferAndArrayImplementation
                      isUnimplemented, // isUnimplemented
                      signatureOnly,   // isInterface
                      isNativeMethod,  // isNativeMethod
                      false,           // isPrivateNativeMethod
                      cfg);
      switch (accessControl) {
          case PUBLIC:     emitter.addModifier(JavaMethodBindingEmitter.PUBLIC); break;
          case PROTECTED:  emitter.addModifier(JavaMethodBindingEmitter.PROTECTED); break;
          case PRIVATE:    emitter.addModifier(JavaMethodBindingEmitter.PRIVATE); break;
          default: break; // package-private adds no modifiers
      }
      if (cfg.allStatic()) {
          emitter.addModifier(FunctionEmitter.STATIC);
      }
      if (isNativeMethod) {
          emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
      }
      emitter.setReturnedArrayLengthExpression(cfg.returnedArrayLength(binding.getName()));
      emitter.setPrologue(prologue);
      emitter.setEpilogue(epilogue);
      allEmitters.add(emitter);
  }

  /**
   * Generates the private emitters for this MethodBinding. On the
   * Java side these will simply produce signatures for native
   * methods. On the C side these will create the emitters which will
   * write the JNI code to interface to the functions. We need to be
   * careful to make the signatures all match up and not produce too
   * many emitters which would lead to compilation errors from
   * creating duplicated methods / functions.
   */
  protected void generatePrivateEmitters(final MethodBinding binding,
                                         final List<FunctionEmitter> allEmitters) {
      final FunctionSymbol cSymbol = binding.getCSymbol();
      if (cfg.manuallyImplement(cSymbol)) {
          // Don't produce emitters for the implementation class
          return;
      }

      final boolean hasPrologueOrEpilogue =
              cfg.javaPrologueForMethod(binding, false, false) != null ||
              cfg.javaEpilogueForMethod(binding, false, false) != null ;

      if ( !cfg.isUnimplemented( cSymbol ) ) {
          // If we already generated a public native entry point for this
          // method, don't emit another one
          //
          // !binding.signatureUsesJavaPrimitiveArrays():
          //   If the binding uses primitive arrays, we are going to emit
          //   the private native entry point for it along with the version
          //   taking only NIO buffers
          if ( !binding.signatureUsesJavaPrimitiveArrays() &&
               ( binding.needsNIOWrappingOrUnwrapping() || hasPrologueOrEpilogue )
             )
          {
              final CodeUnit unit = (cfg.allStatic() ? javaUnit() : javaImplUnit());

              // (Always) emit the entry point taking only direct buffers
              final JavaMethodBindingEmitter emitter =
                      new JavaMethodBindingEmitter(binding,
                              unit,
                              cfg.runtimeExceptionType(),
                              cfg.unsupportedExceptionType(),
                              false, // emitBody
                              cfg.tagNativeBinding(),
                              true,  // eraseBufferAndArrayTypes
                              cfg.useNIOOnly(binding.getName()),
                              cfg.useNIODirectOnly(binding.getName()),
                              true,  // forDirectBufferImplementation
                              false, // forIndirectBufferAndArrayImplementation
                              false, // isUnimplemented
                              false, // isInterface
                              true,  // isNativeMethod
                              true,  // isPrivateNativeMethod
                              cfg);
              emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
              if (cfg.allStatic()) {
                  emitter.addModifier(FunctionEmitter.STATIC);
              }
              emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
              emitter.setReturnedArrayLengthExpression(cfg.returnedArrayLength(binding.getName()));
              allEmitters.add(emitter);
          }

          // Now generate the C emitter(s). We need to produce one for every
          // Java native entry point (public or private). The only
          // situations where we don't produce one are (a) when the method
          // is unimplemented, and (b) when the signature contains primitive
          // arrays, since the latter is handled by the method binding
          // variant taking only NIO Buffers.
          if ( !binding.signatureUsesJavaPrimitiveArrays() ) {
              // Generate a binding without mixed access (NIO-direct, -indirect, array)
              final CMethodBindingEmitter cEmitter =
                      new CMethodBindingEmitter(binding,
                              cUnit(),
                              cfg.implPackageName(),
                              cfg.implClassName(),
                              true, // NOTE: we always disambiguate with a suffix now, so this is optional
                              cfg.allStatic(),
                              (binding.needsNIOWrappingOrUnwrapping() || hasPrologueOrEpilogue),
                              !cfg.useNIODirectOnly(binding.getName()),
                              machDescJava, getConfiguration());
              prepCEmitter(binding.getName(), binding.getJavaReturnType(), cEmitter);
              allEmitters.add(cEmitter);
          }
      }
  }

  protected void prepCEmitter(final String returnSizeLookupName, final JavaType javaReturnType, final CMethodBindingEmitter cEmitter)
  {
      // See whether we need an expression to help calculate the
      // length of any return type
      if (javaReturnType.isNIOBuffer() ||
          javaReturnType.isCompoundTypeWrapper()) {
        // See whether capacity has been specified
        final String capacity = cfg.returnValueCapacity(returnSizeLookupName);
        if (capacity != null) {
          cEmitter.setReturnValueCapacityExpression( new MessageFormat(capacity) );
        }
      } else if (javaReturnType.isArray() ||
                 javaReturnType.isArrayOfCompoundTypeWrappers()) {
        // NOTE: adding a check here because the CMethodBindingEmitter
        // also doesn't yet handle returning scalar arrays. In order
        // to implement this, return the type as a Buffer instead
        // (i.e., IntBuffer, FloatBuffer) and add code as necessary.
        if (javaReturnType.isPrimitiveArray()) {
          throw new RuntimeException("Primitive array return types not yet supported");
        }

        // See whether length has been specified
        final String len = cfg.returnValueLength(returnSizeLookupName);
        if (len != null) {
          cEmitter.setReturnValueLengthExpression( new MessageFormat(len) );
        }
      }
      cEmitter.setTemporaryCVariableDeclarations(cfg.temporaryCVariableDeclarations(returnSizeLookupName));
      cEmitter.setTemporaryCVariableAssignments(cfg.temporaryCVariableAssignments(returnSizeLookupName));
  }

  /**
   * Generate all appropriate Java bindings for the specified C function
   * symbols.
   */
  protected List<? extends FunctionEmitter> generateMethodBindingEmitters(final FunctionSymbol sym) throws Exception {
    final ArrayList<FunctionEmitter> allEmitters = new ArrayList<FunctionEmitter>();
    try {
        if( cfg.emitInterface() ) {
            generateMethodBindingEmittersImpl(allEmitters, sym, true);
        }
        if( cfg.emitImpl() ) {
            generateMethodBindingEmittersImpl(allEmitters, sym, false);
        }
    } catch (final Exception e) {
      throw new GlueGenException("Error while generating bindings for \"" + sym + "\"", sym.getASTLocusTag(), e);
    }

    return allEmitters;
  }
  private void generateMethodBindingEmittersImpl(final ArrayList<FunctionEmitter> allEmitters,
                                                 final FunctionSymbol sym,
                                                 final boolean forInterface) throws Exception
  {
      // Get Java binding for the function
      final MethodBinding mb = bindFunction(sym, forInterface, machDescJava, null, null);

      // JavaTypes representing C pointers in the initial
      // MethodBinding have not been lowered yet to concrete types
      final List<MethodBinding> bindings = expandMethodBinding(mb);

      final HashSet<MethodBinding> methodBindingSet = new HashSet<MethodBinding>();

      for (final MethodBinding binding : bindings) {

        if(!methodBindingSet.add(binding)) {
            // skip .. already exisiting binding ..
            continue;
        }

        if (cfg.allStatic() && binding.hasContainingType()) {
          // This should not currently happen since structs are emitted using a different mechanism
          throw new IllegalArgumentException("Cannot create binding in AllStatic mode because method has containing type: \"" +
                                             binding + "\"");
        }

        // The structure of the generated glue code looks something like this:
        // Simple method (no arrays, void pointers, etc.):
        //   Interface class:
        //     public void fooMethod();
        //   Implementation class:
        //     public native void fooMethod();
        //
        // Method taking void* argument:
        //   Interface class:
        //     public void fooMethod(Buffer arg);
        //   Implementation class:
        //     public void fooMethod(Buffer arg) {
        //       ... bounds checks, etc. ...
        //
        //       boolean arg_direct = arg != null && Buffers.isDirect(arg);
        //
        //       fooMethod0(arg_direct?arg:Buffers.getArray(arg),
        //                  arg_direct?Buffers.getDirectBufferByteOffset(arg):Buffers.getIndirectBufferByteOffset(arg),
        //                  arg_direct,
        //                  ... );
        //     }
        //     private native void fooMethod1(Object arg, int arg_byte_offset, boolean arg_is_direct, ...);
        //
        // Method taking primitive array argument:
        //   Interface class:
        //     public void fooMethod(int[] arg, int arg_offset);
        //     public void fooMethod(IntBuffer arg);
        //   Implementing class:
        //     public void fooMethod(int[] arg, int arg_offset) {
        //       ... range checks, etc. ...
        //       fooMethod1(arg, SIZEOF_INT * arg_offset);
        //     }
        //     public void fooMethod(IntBuffer arg) {
        //       ... bounds checks, etc. ...
        //
        //       boolean arg_direct = BufferFactory.isDirect(arg);
        //
        //       fooMethod1(arg_direct?arg:BufferFactory.getArray(arg),
        //                  arg_direct?BufferFactory.getDirectBufferByteOffset(arg):BufferFactory.getIndirectBufferByteOffset(arg),
        //                  arg_direct,
        //                  ... );
        //     }
        //     private native void fooMethod1(Object arg, int arg_byte_offset, boolean arg_is_direct, ...);
        //
        // Note in particular that the public entry point taking an
        // array is merely a special case of the indirect buffer case.

        if ( forInterface ) {
          generatePublicEmitters(binding, allEmitters, true);
        } else {
          generatePublicEmitters(binding, allEmitters, false);
          generatePrivateEmitters(binding, allEmitters);
        }
      } // end iteration over expanded bindings
    }


  @Override
  public void endFunctions() throws Exception {
    if ( !cfg.structsOnly() ) {
        if (cfg.allStatic() || cfg.emitInterface()) {
            emitCustomJavaCode(javaUnit(), cfg.className());
        }
        if( cfg.allStatic() && cfg.emitImpl()) {
            emitCustomJNICode(cUnit(), cfg.className());
        }
        if (!cfg.allStatic() && cfg.emitImpl()) {
            emitCustomJavaCode(javaImplUnit(), cfg.implClassName());
            emitCustomJNICode(cUnit(), cfg.implClassName());
        }
    }
  }

  @Override
  public void beginStructLayout() throws Exception {}
  @Override
  public void layoutStruct(final CompoundType t) throws Exception {
    getLayout().layout(t);
  }
  @Override
  public void endStructLayout() throws Exception {}

  @Override
  public void beginStructs(final TypeDictionary typedefDictionary,
                           final TypeDictionary structDictionary,
                           final Map<Type, Type> canonMap) throws Exception {
    // this.typedefDictionary = typedefDictionary;
    this.canonMap          = canonMap;
  }

  @Override
  public void emitStruct(final CompoundType structCType, final Type structCTypedefPtr) throws Exception {
    final String structCTypeName, typedefedName;
    final boolean immutableStruct;
    {
        final String _name = structCType.getName();
        if ( null != structCTypedefPtr && null != structCTypedefPtr.getName() ) {
            // always use typedef'ed name if available
            typedefedName = structCTypedefPtr.getName();
            structCTypeName = typedefedName;
        } else {
            // fall back to actual struct type name
            typedefedName = null;
            structCTypeName = _name;
        }
        LOG.log(INFO, structCType.getASTLocusTag(), "Struct emission of structCType {0}", structCType);
        LOG.log(INFO, structCType.getASTLocusTag(),"              structCTypedefPtr {0}", structCTypedefPtr);
        LOG.log(INFO, structCType.getASTLocusTag(),"   : structCTypeName \"{0}\" -> typedefedName \"{1}\" -> \"{2}\"",
                                                                   _name, typedefedName, structCTypeName);
        if ( null == structCTypeName ) {
            LOG.log(INFO, structCType.getASTLocusTag(),
                    "skipping emission of unnamed struct {0} w/o typedef",  structCType);
            return;
        }
        final AliasedSymbol.AliasedSymbolImpl aliases = new AliasedSymbol.AliasedSymbolImpl(structCTypeName);
        aliases.addAliasedName(_name);
        aliases.addAliasedName(typedefedName);
        if ( cfg.shouldIgnoreInInterface(aliases) ) {
            LOG.log(INFO, structCType.getASTLocusTag(),
                    "skipping emission of ignored \"{0}\": {1}", aliases, structCType);
            return;
        }
        immutableStruct = cfg.immutableAccess(aliases);
    }

    if( null != structCTypedefPtr && isOpaque(structCTypedefPtr) ) {
        LOG.log(INFO, structCType.getASTLocusTag(),
                "skipping emission of opaque typedef {0}", structCTypedefPtr);
        return;
    }
    if( isOpaque(structCType) ) {
        LOG.log(INFO, structCType.getASTLocusTag(),
                "skipping emission of opaque c-struct {0}", structCType);
        return;
    }

    final Type containingCType;
    {
        // NOTE: Struct Name Resolution (JavaEmitter, HeaderParser)
        final Type aptr;
        int mode;
        if( null != typedefedName ) {
            aptr = structCTypedefPtr;
            mode = 1;
        } else {
            aptr = new PointerType(SizeThunk.POINTER, structCType, 0);
            aptr.setTypedefName(typedefedName);
            mode = 2;
        }
        containingCType = canonicalize(aptr);
        LOG.log(INFO, structCType.getASTLocusTag(), "containingCType[{0}]: {1} -canon-> {2}", mode, aptr, containingCType);
    }
    final JavaType containingJType = typeToJavaType(containingCType, null);
    if( containingJType.isOpaqued() ) {
        LOG.log(INFO, structCType.getASTLocusTag(),
                "skipping emission of opaque {0}, {1}", containingJType, structCType);
        return;
    }
    if( !containingJType.isCompoundTypeWrapper() ) {
        LOG.log(WARNING, structCType.getASTLocusTag(),
                "skipping emission of non-compound {0}, {1}", containingJType, structCType);
        return;
    }
    final String containingJTypeName = containingJType.getName();
    LOG.log(INFO, structCType.getASTLocusTag(),
            "perform emission of \"{0}\" -> \"{1}\": {2}", structCTypeName, containingJTypeName, structCType);

    if( 0 == structCType.getNumFields() ) {
        LOG.log(INFO, structCType.getASTLocusTag(),
                "emission of \"{0}\" with zero fields {1}", containingJTypeName, structCType);
    }

    // machDescJava global MachineDataInfo is the one used to determine
    // the sizes of the primitive types seen in the public API in Java.
    // For example, if a C long is an element of a struct, it is the size
    // of a Java int on a 32-bit machine but the size of a Java long
    // on a 64-bit machine. To support both of these sizes with the
    // same API, the abstract base class must take and return a Java
    // long from the setter and getter for this field. However the
    // implementation on a 32-bit platform must downcast this to an
    // int and set only an int's worth of data in the struct.
    //
    // The machDescTarget MachineDataInfo is the one used to determine how
    // much data to set in or get from the struct and exactly from
    // where it comes.
    //
    // Note that machDescJava MachineDataInfo is always 64bit unix,
    // which complies w/ Java types.

    boolean needsNativeCode = !cfg.customJNICodeForClass(containingJTypeName).isEmpty();

    // Native code for calls through function pointers gets emitted
    // into the abstract base class; Java code which accesses fields
    // gets emitted into the concrete classes
    for (int i = 0; i < structCType.getNumFields(); i++) {
      final Field field = structCType.getField(i);
      final Type fieldType = field.getType();
      final String cfgFieldName0 = JavaConfiguration.canonicalStructFieldSymbol(containingJTypeName, field.getName());

      if (!cfg.shouldIgnoreInInterface(cfgFieldName0)) {
        final String renamed = cfg.getJavaSymbolRename(cfgFieldName0);
        final String fieldName = renamed==null ? field.getName() : renamed;
        final String cfgFieldName1 = JavaConfiguration.canonicalStructFieldSymbol(containingJTypeName, fieldName);
        final TypeInfo opaqueField = cfg.canonicalNameOpaque(cfgFieldName1);
        final boolean isOpaqueField = null != opaqueField;

        if ( fieldType.isFunctionPointer() && !isOpaqueField ) {
            needsNativeCode = true;
            break;
        }
      }
    }

    final String structClassPkgName = cfg.packageForStruct(structCTypeName);
    final JavaCodeUnit javaUnit;
    final CCodeUnit jniUnit;
    try  {
        {
            final String javaFileName = cfg.javaOutputDir() + File.separator +
                                        CodeGenUtils.packageAsPath(structClassPkgName) +
                                        File.separator + containingJTypeName + ".java";

            javaUnit = openJavaUnit(javaFileName, structClassPkgName, containingJTypeName);
        }

        if( null == javaUnit ) {
            // suppress output if openFile deliberately returns null.
            return;
        }
        if (needsNativeCode) {
            String nRoot = cfg.nativeOutputDir();
            if (cfg.nativeOutputUsesJavaHierarchy()) {
                nRoot += File.separator + CodeGenUtils.packageAsPath(cfg.packageName());
            }
            final String cUnitName = containingJTypeName + "_JNI.c";
            final String fname = nRoot + File.separator + cUnitName;
            jniUnit = openCUnit(fname, cUnitName);
            // jniUnit.emitHeader(structClassPkgName, containingJTypeName, Collections.emptyList());
            jniUnit.emitHeader(structClassPkgName, containingJTypeName, cfg.customCCode());
        } else {
            jniUnit = null;
        }
    } catch(final Exception e)   {
        throw new RuntimeException("Unable to open files for emission of struct class", e);
    }

    javaUnit.emitln();
    javaUnit.emitln("package " + structClassPkgName + ";");
    javaUnit.emitln();
    javaUnit.emitln("import java.nio.*;");
    javaUnit.emitln("import java.nio.charset.Charset;");
    javaUnit.emitln("import java.nio.charset.StandardCharsets;");
    javaUnit.emitln();

    javaUnit.emitln("import " + cfg.gluegenRuntimePackage() + ".*;");
    javaUnit.emitln("import " + DynamicLookupHelper.class.getPackage().getName() + ".*;");
    javaUnit.emitln("import " + Buffers.class.getPackage().getName() + ".*;");
    javaUnit.emitln("import " + MachineDataInfoRuntime.class.getName() + ";");
    javaUnit.emitln();
    final List<String> imports = cfg.imports();
    for (final String str : imports) {
      javaUnit.emit("import ");
      javaUnit.emit(str);
      javaUnit.emitln(";");
    }
    javaUnit.emitln();
    final List<String> javadoc = cfg.javadocForClass(containingJTypeName);
    for (final String doc : javadoc) {
      javaUnit.emitln(doc);
    }
    javaUnit.emit("public class " + containingJTypeName + " ");
    boolean firstIteration = true;
    final List<String> userSpecifiedInterfaces = cfg.implementedInterfaces(containingJTypeName);
    for (final String userInterface : userSpecifiedInterfaces) {
      if (firstIteration) {
        javaUnit.emit("implements ");
      }
      firstIteration = false;
      javaUnit.emit(userInterface);
      javaUnit.emit(" ");
    }
    javaUnit.emitln("{");
    javaUnit.emitln();
    javaUnit.emitln("  StructAccessor accessor;");
    javaUnit.emitln();
    final String cfgMachDescrIdxCode = cfg.returnStructMachineDataInfoIndex(containingJTypeName);
    final String machDescrIdxCode = null != cfgMachDescrIdxCode ? cfgMachDescrIdxCode : "private static final int mdIdx = MachineDataInfoRuntime.getStatic().ordinal();";
    javaUnit.emitln("  "+machDescrIdxCode);
    javaUnit.emitln("  private final MachineDataInfo md;");
    javaUnit.emitln();
    // generate all offset and size arrays
    generateOffsetAndSizeArrays(javaUnit, "  ", containingJTypeName, structCType, null, null); /* w/o offset */
    if( GlueGen.debug() ) {
        System.err.printf("SE.__: structCType %s%n", structCType.getDebugString());
        System.err.printf("SE.__: contCTypeName %s%n", containingCType.getDebugString());
        System.err.printf("SE.__: contJTypeName %s%n", containingJType.getDebugString());
    }
    for (int i = 0; i < structCType.getNumFields(); i++) {
      final Field field = structCType.getField(i);
      final Type fieldType = field.getType();
      final String cfgFieldName0 = JavaConfiguration.canonicalStructFieldSymbol(containingJTypeName, field.getName());
      if ( !cfg.shouldIgnoreInInterface(cfgFieldName0) ) {
        final String renamed = cfg.getJavaSymbolRename(cfgFieldName0);
        final String fieldName = null==renamed ? field.getName() : renamed;
        final String cfgFieldName1 = JavaConfiguration.canonicalStructFieldSymbol(containingJTypeName, fieldName);
        if (fieldType.isFunctionPointer()) {
          if( GlueGen.debug() ) {
            System.err.printf("SE.os.%02d: %s / %s, %s (%s)%n", (i+1), field, cfgFieldName1, fieldType.getDebugString(), "FuncPtr");
          }
          generateOffsetAndSizeArrays(javaUnit, "  ", fieldName, null, field, null); /* w/o size */
          generateOffsetAndSizeArrays(javaUnit, "//", fieldName, fieldType, null, null);
        } else if (fieldType.isCompound()) {
          // FIXME: will need to support this at least in order to
          // handle the union in jawt_Win32DrawingSurfaceInfo (fabricate
          // a name?)
          if (fieldType.getName() == null) {
            throw new GlueGenException("Anonymous structs as fields not supported yet, field \"" +
                                       cfgFieldName1 + "\", "+fieldType.getDebugString(), fieldType.getASTLocusTag());
          }
          if( GlueGen.debug() ) {
            System.err.printf("SE.os.%02d: %s / %s, %s (%s)%n", (i+1), field, cfgFieldName1, fieldType.getDebugString(), "compound");
          }
          generateOffsetAndSizeArrays(javaUnit, "  ", fieldName, fieldType, field, null);
        } else if (fieldType.isArray()) {
            final Type baseElementType = fieldType.getBaseType();
            if( GlueGen.debug() ) {
                System.err.printf("SE.os.%02d: %s / %s, %s (%s)%n", (i+1), field, cfgFieldName1, fieldType.getDebugString(), "array");
                System.err.printf("SE.os.%02d: baseType %s%n", (i+1), baseElementType.getDebugString());
            }
            generateOffsetAndSizeArrays(javaUnit, "  ", fieldName, null, field, null); /* w/o size */
            generateOffsetAndSizeArrays(javaUnit, "//", fieldName, fieldType, null, null);
        } else {
          final JavaType externalJavaType;
          try {
            externalJavaType = typeToJavaType(fieldType, machDescJava);
          } catch (final Exception e) {
            throw new GlueGenException("Error occurred while creating accessor for field \"" +
                                       cfgFieldName1 + "\", "+fieldType.getDebugString(), fieldType.getASTLocusTag(), e);
          }
          if( GlueGen.debug() ) {
              System.err.printf("SE.os.%02d: %s / %s, %s (%s)%n", (i+1), field, cfgFieldName1, fieldType.getDebugString(), "MISC");
              System.err.printf("SE.os.%02d: javaType %s%n", (i+1), externalJavaType.getDebugString());
          }
          if (externalJavaType.isPrimitive()) {
            // Primitive type
            generateOffsetAndSizeArrays(javaUnit, "  ", fieldName, null, field, null); /* w/o size */
            generateOffsetAndSizeArrays(javaUnit, "//", fieldName, fieldType, null, null);
          } else if (externalJavaType.isCPrimitivePointerType()) {
            if( requiresGetCStringLength(fieldType, cfgFieldName1) ) {
                generateOffsetAndSizeArrays(javaUnit, "  ", fieldName, null, field, null); /* w/o size */
                generateOffsetAndSizeArrays(javaUnit, "//", fieldName, fieldType, null, "// "+externalJavaType.getDebugString());
            } else {
                generateOffsetAndSizeArrays(javaUnit, "  ", fieldName, null, field, null); /* w/o size */
                generateOffsetAndSizeArrays(javaUnit, "//", fieldName, fieldType, field, "// "+externalJavaType.getDebugString());
            }
          } else {
            generateOffsetAndSizeArrays(javaUnit, "  ", fieldName, null, field, null); /* w/o size */
            generateOffsetAndSizeArrays(javaUnit, "//", fieldName, fieldType, null, "// "+externalJavaType.getDebugString());
          }
        }
      } else if( GlueGen.debug() ) {
        System.err.printf("SE.os.%02d: %s, %s (IGNORED)%n", (i+1), field, fieldType.getDebugString());
      }
    }
    javaUnit.emitln();
    javaUnit.emitln("  /** Returns true if this generated implementation uses native code, otherwise false. */");
    javaUnit.emitln("  public static boolean usesNativeCode() {");
    javaUnit.emitln("    return "+needsNativeCode+";");
    javaUnit.emitln("  }");
    javaUnit.emitln();

    // getDelegatedImplementation
    if( !cfg.manuallyImplement(JavaConfiguration.canonicalStructFieldSymbol(containingJTypeName, "size")) ) {
        javaUnit.emitln("  /** Returns the aligned total size of a native instance. */");
        javaUnit.emitln("  public static int size() {");
        javaUnit.emitln("    return "+containingJTypeName+"_size[mdIdx];");
        javaUnit.emitln("  }");
        javaUnit.emitln();
    }
    if( !cfg.manuallyImplement(JavaConfiguration.canonicalStructFieldSymbol(containingJTypeName, "create")) ) {
        javaUnit.emitln("  /** Returns a new instance with all bytes set to zero. */");
        javaUnit.emitln("  public static " + containingJTypeName + " create() {");
        javaUnit.emitln("    return create(Buffers.newDirectByteBuffer(size()));");
        javaUnit.emitln("  }");
        javaUnit.emitln();
        javaUnit.emitln("  /** Returns a new instance using the given ByteBuffer having at least {#link size()} bytes capacity. The ByteBuffer will be {@link ByteBuffer#rewind()} and native-order set. */");
        javaUnit.emitln("  public static " + containingJTypeName + " create(java.nio.ByteBuffer buf) {");
        javaUnit.emitln("      return new " + containingJTypeName + "(buf);");
        javaUnit.emitln("  }");
        javaUnit.emitln();
    }
    javaUnit.emitln("  /** Returns new instance dereferencing ByteBuffer at given native address `addr` with size {@link #size()}. */");
    javaUnit.emitln("  public static " + containingJTypeName + " derefPointer(final long addr) {");
    javaUnit.emitln("      return create( ElementBuffer.derefPointer(size(), addr, 1).getByteBuffer() );");
    javaUnit.emitln("  }");
    javaUnit.emitln();
    if( !cfg.manuallyImplement(JavaConfiguration.canonicalStructFieldSymbol(containingJTypeName, containingJTypeName)) ) {
        javaUnit.emitln("  " + containingJTypeName + "(java.nio.ByteBuffer buf) {");
        javaUnit.emitln("    md = MachineDataInfo.StaticConfig.values()[mdIdx].md;");
        javaUnit.emitln("    accessor = new StructAccessor(buf);");
        javaUnit.emitln("  }");
        javaUnit.emitln();
    }
    javaUnit.emitln("  /** Return the underlying native direct ByteBuffer */");
    javaUnit.emitln("  public final java.nio.ByteBuffer getBuffer() {");
    javaUnit.emitln("    return accessor.getBuffer();");
    javaUnit.emitln("  }");
    javaUnit.emitln();
    javaUnit.emitln("  /** Returns the native address of the underlying native ByteBuffer {@link #getBuffer()} */");
    javaUnit.emitln("  public final long getDirectBufferAddress() {");
    javaUnit.emitln("    return accessor.getDirectBufferAddress();");
    javaUnit.emitln("  }");
    javaUnit.emitln();

    final Set<MethodBinding> methodBindingSet = new HashSet<MethodBinding>();

    for (int i = 0; i < structCType.getNumFields(); i++) {
      final Field field = structCType.getField(i);
      final Type fieldType = field.getType();

      final String fqStructFieldName0 = JavaConfiguration.canonicalStructFieldSymbol(containingJTypeName, field.getName()); // containingJTypeName.field.getName()
      if (!cfg.shouldIgnoreInInterface(fqStructFieldName0)) {
        final String renamed = cfg.getJavaSymbolRename(fqStructFieldName0);
        final String fieldName = renamed==null ? field.getName() : renamed;
        final String fqStructFieldName1 = JavaConfiguration.canonicalStructFieldSymbol(containingJTypeName, fieldName); // containingJTypeName.fieldName
        final TypeInfo opaqueFieldType = cfg.typeInfo(fieldType);
        final boolean isOpaqueFieldType = null != opaqueFieldType;
        final TypeInfo opaqueField = cfg.canonicalNameOpaque(fqStructFieldName1);
        final boolean isOpaqueField = null != opaqueField;
        final boolean immutableField = immutableStruct || cfg.immutableAccess(fqStructFieldName1);

        if( GlueGen.debug() ) {
          System.err.printf("SE.ac.%02d: field %s / %s / rename %s -> %s / opaque %b, fieldType %s (opaque %b), immutable[struct %b, field %b]%n", (i+1),
                  field, fqStructFieldName1, renamed, fieldName, isOpaqueField, fieldType.getDebugString(), isOpaqueFieldType,
                  immutableStruct, immutableField);
          System.err.printf("SE.ac.%02d: opaqueFieldType %s%n", (i+1), opaqueFieldType);
        }
        if ( fieldType.isFunctionPointer() && !isOpaqueField ) {
            final FunctionSymbol func = new FunctionSymbol(field.getName(), fieldType.getTargetFunction());
            func.rename(renamed); // null is OK
            final String javaTypeName = "long";
            final String capFieldName = capitalizeString(fieldName);
            if( !immutableField && !fieldType.isConst() ) {
                // Setter
                generateSetterSignature(javaUnit, MethodAccess.PUBLIC, false, false, fieldName, fieldType, Ownership.Parent, containingJTypeName, capFieldName, null, javaTypeName, null, false, false, null, null, null);
                javaUnit.emitln(" {");
                javaUnit.emitln("    accessor.setLongAt(" + fieldName+"_offset[mdIdx], src, md.pointerSizeInBytes());");
                javaUnit.emitln("    return this;");
                javaUnit.emitln("  }");
                javaUnit.emitln();
            }
            // Getter
            generateGetterSignature(javaUnit, false, false, fieldName, fieldType, Ownership.Parent, javaTypeName, capFieldName, null, false, false, null, null);
            javaUnit.emitln(" {");
            javaUnit.emitln("    return accessor.getLongAt(" + fieldName+"_offset[mdIdx], md.pointerSizeInBytes());");
            javaUnit.emitln("  }");
            javaUnit.emitln();
            generateFunctionPointerCode(methodBindingSet, javaUnit, jniUnit, structCTypeName,
                                        containingCType, containingJType, i, func, fqStructFieldName1);
        } else if ( fieldType.isCompound() && !isOpaqueField ) {
          // FIXME: will need to support this at least in order to
          // handle the union in jawt_Win32DrawingSurfaceInfo (fabricate a name?)
          if (fieldType.getName() == null) {
            throw new GlueGenException("Anonymous structs as fields not supported yet (field \"" +
                                       field + "\" in type \"" + structCTypeName + "\")",
                                       fieldType.getASTLocusTag());
          }
          generateGetterSignature(javaUnit, false, false, fieldName, fieldType, Ownership.Parent, fieldType.getName(), capitalizeString(fieldName), null, false, false, null, null);
          javaUnit.emitln(" {");
          javaUnit.emitln("    return " + fieldType.getName() + ".create( accessor.slice( " +
                           fieldName+"_offset[mdIdx], "+fieldName+"_size[mdIdx] ) );");
          javaUnit.emitln("  }");
          javaUnit.emitln();
        } else if ( ( fieldType.isArray() || fieldType.isPointer() ) && !isOpaqueField ) {
          generateArrayGetterSetterCode(javaUnit, structCType, containingJType,
                                        i, field, fieldName, immutableField, fqStructFieldName1);
        } else {
          final JavaType javaType;
          try {
            javaType = typeToJavaType(fieldType, machDescJava);
          } catch (final Exception e) {
            throw new GlueGenException("Error occurred while creating accessor for field \"" +
                                       field.getName() + "\", "+fieldType.getDebugString(), fieldType.getASTLocusTag(), e);
          }
          if ( isOpaqueFieldType || isOpaqueField || javaType.isPrimitive()) {
            // Primitive type
            final boolean fieldTypeNativeSizeFixed = fieldType.getSize().hasFixedNativeSize();
            final String javaTypeName;
            if ( isOpaqueFieldType ) {
              javaTypeName = opaqueFieldType.javaType().getName();
            } else if ( isOpaqueField ) {
              javaTypeName = opaqueField.javaType().getName();
              // javaTypeName = compatiblePrimitiveJavaTypeName(fieldType, javaType, machDescJava);
            } else {
              javaTypeName = javaType.getName();
            }
            final String capJavaTypeName = capitalizeString(javaTypeName);
            final String capFieldName = capitalizeString(fieldName);
            final String sizeDenominator = fieldType.isPointer() ? "pointer" : javaTypeName ;

            LOG.log(FINE, structCType.getASTLocusTag(),
                    "Java.StructEmitter.Primitive: "+field.getName()+", "+fieldType+", "+javaTypeName+", "+
                    ", fixedSize "+fieldTypeNativeSizeFixed+", opaque[t "+isOpaqueFieldType+", f "+isOpaqueField+"], sizeDenominator "+sizeDenominator);

            if( !immutableField && !fieldType.isConst() ) {
                // Setter
                generateSetterSignature(javaUnit, MethodAccess.PUBLIC, false, false, fieldName, fieldType, Ownership.Parent, containingJTypeName, capFieldName, null, javaTypeName, null, false, false, null, null, null);
                javaUnit.emitln(" {");
                if( fieldTypeNativeSizeFixed ) {
                    javaUnit.emitln("    accessor.set" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx], src);");
                } else {
                    javaUnit.emitln("    accessor.set" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx], src, md."+sizeDenominator+"SizeInBytes());");
                }
                javaUnit.emitln("    return this;");
                javaUnit.emitln("  }");
                javaUnit.emitln();
            }

            // Getter
            generateGetterSignature(javaUnit, false, false, fieldName, fieldType, Ownership.Parent, javaTypeName, capFieldName, null, false, false, null, null);
            javaUnit.emitln(" {");
            javaUnit.emit  ("    return ");
            if( fieldTypeNativeSizeFixed ) {
                javaUnit.emitln("accessor.get" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx]);");
            } else {
                javaUnit.emitln("accessor.get" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx], md."+sizeDenominator+"SizeInBytes());");
            }
            javaUnit.emitln("  }");
          } else {
            javaUnit.emitln("  /** UNKNOWN: "+fqStructFieldName1 +": "+fieldType.getDebugString()+", "+javaType.getDebugString()+" */");
          }
          javaUnit.emitln();
        }
      }
    }
    emitCustomJavaCode(javaUnit, containingJTypeName);
    javaUnit.emitTailCode();
    javaUnit.emitln("}");
    javaUnit.close();
    if (needsNativeCode) {
      emitCustomJNICode(jniUnit, containingJTypeName);
      jniUnit.close();
    }
    if( GlueGen.debug() ) {
        System.err.printf("SE.XX: structCType %s%n", structCType.getDebugString());
        System.err.printf("SE.XX: contCTypeName %s%n", containingCType.getDebugString());
        System.err.printf("SE.XX: contJTypeName %s%n", containingJType.getDebugString());
    }
  }
  @Override
  public void endStructs() throws Exception {}

  public static int addStrings2Buffer(StringBuilder buf, final String sep, final String first, final Collection<String> col) {
    int num = 0;
    if(null==buf) {
        buf = new StringBuilder();
    }

    final Iterator<String> iter = col.iterator();
    if(null!=first) {
        buf.append(first);
        if( iter.hasNext() ) {
            buf.append(sep);
        }
        num++;
    }
    while( iter.hasNext() ) {
        buf.append(iter.next());
        if( iter.hasNext() ) {
            buf.append(sep);
        }
        num++;
    }
    return num;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //
  private void generateArrayFieldNote(final CodeUnit unit, final String leadIn, final String leadOut,
                                      final String fieldName, final Type fieldType, final Ownership ownership,
                                      final boolean constElemCount, final boolean maxOneElem, final String elemCountExpr,
                                      final boolean multiline, final boolean startNewPara) {
      final boolean isArray;
      final Type referencedType;
      final String relationship;
      {
          if( fieldType.isFunctionPointer() ) {
              isArray = false;
              referencedType = null;
              relationship = "being";
          } else if( fieldType.isPointer() ) {
              isArray = true;
              referencedType = fieldType.getTargetType();
              relationship = "referencing";
          } else if( fieldType.isArray() ) {
              isArray = true;
              final Type t = fieldType.getArrayBaseOrPointerTargetType();
              if( t != fieldType && t.isPointer() ) {
                  referencedType = t;
              } else {
                  referencedType = null;
              }
              relationship = "being";
          } else {
              isArray = false;
              referencedType = null;
              relationship = "being";
          }
      }
      // isPointer = true;
      if( multiline ) {
          if( startNewPara ) {
              unit.emitln("   * <p>");
          }
          unit.emit  ("   * ");
      }
      if( null != leadIn ) {
          unit.emit(leadIn+" ");
      }
      final String ownershipTerm;
      switch( ownership ) {
          case Parent: ownershipTerm = "a <i>struct</i> owned"; break;
          case Java: ownershipTerm = "a <i>Java</i> owned"; break;
          case Native: ownershipTerm = "a <i>natively</i> owned"; break;
          default: ownershipTerm = "a <i>mixed and ambigously</i> owned (<b>warning</b>)"; break;
      }
      final String what;
      if( fieldType.isFunctionPointer() ) {
          what = "function pointer";
      } else if( isArray ) {
          what = "array";
      } else {
          what = fieldType.getClass().getSimpleName();
      }
      unit.emit("native field <code>"+fieldName+"</code>, "+relationship+" "+ownershipTerm+" "+what);
      if( isArray ) {
          unit.emit(" with "+(constElemCount?"fixed":"variable")+" element count");
          if( null != elemCountExpr ) {
              if( elemCountExpr.startsWith("get") && elemCountExpr.endsWith("()") ) {
                  unit.emit(" of {@link #"+elemCountExpr+"} ");
              } else {
                  unit.emit(" of <code>"+elemCountExpr+"</code> ");
              }
              if( constElemCount || Ownership.Mixed == ownership ) {
                  unit.emit("elements.");
              } else {
                  unit.emit("initial elements.");
              }
          } else {
              unit.emit(".");
          }
      } else {
          unit.emit(".");
      }
      if( multiline ) {
          unit.emitln();
          if( startNewPara ) {
              unit.emitln("   * </p>");
          }
      }
      if( maxOneElem ) {
          if( multiline ) {
              unit.emitln("   * <p>");
              unit.emitln("   * Maximum element count is <code>1</code>.");
              unit.emitln("   * </p>");
          } else {
              unit.emit(" Maximum element count is <code>1</code>.");
          }
      }
      if( multiline ) {
          unit.emitln("   * <p>");
          if( null == referencedType ) {
              unit.emitln("   * Native Field Signature <code>"+fieldType.getSignature(null).toString()+"</code>");
          } else {
              unit.emitln("   * Native Signature:");
              unit.emitln("   * <ul>");
              unit.emitln("   *   <li>field-type <code>"+fieldType.getSignature(null).toString()+"</code></li>");
              unit.emitln("   *   <li>referenced <code>"+referencedType.getSignature(null).toString()+"</code></li>");
              unit.emitln("   * </ul>");
          }
          unit.emitln("   * </p>");
      } else {
          unit.emit(" NativeSig <code>"+fieldType.getSignature(null).toString()+"</code>");
      }
      if( null != leadOut ) {
          unit.emit(" "+leadOut);
      }
      if( !multiline) {
          unit.emitln();
      }
  }
  private void generateIsNullSignature(final CodeUnit unit, final boolean abstractMethod,
                                       final String fieldName, final Type fieldType, final Ownership ownership,
                                       final String capitalizedFieldName, final boolean constElemCount, final boolean maxOneElem, final String elemCountExpr) {
      unit.emitln("  /**");
      unit.emitln("   * Returns `true` if native pointer <code>"+fieldName+"</code> is `null`, otherwise `false`.");
      generateArrayFieldNote(unit, "Corresponds to", null, fieldName, fieldType, ownership, constElemCount, maxOneElem, elemCountExpr, true, true);
      unit.emitln("   */");
      unit.emit("  public " + (abstractMethod ? "abstract " : "final ") + "boolean is" + capitalizedFieldName + "Null()");
  }
  private void generateReleaseSignature(final CodeUnit unit, final boolean abstractMethod,
                                        final String fieldName, final Type fieldType, final Ownership ownership, final String returnTypeName,
                                        final String capitalizedFieldName, final boolean constElemCount, final boolean maxOneElement, final String elemCountExpr) {
      unit.emitln("  /**");
      generateArrayFieldNote(unit, "Releases memory referenced by", null, fieldName, fieldType, ownership, constElemCount, maxOneElement, elemCountExpr, true, false);
      unit.emitln("   */");
      unit.emit("  public " + (abstractMethod ? "abstract " : "final ") + returnTypeName + " release" + capitalizedFieldName + "()");
  }
  private void generateGetterSignature(final CodeUnit unit, final boolean staticMethod, final boolean abstractMethod,
                                       final String fieldName, final Type fieldType, final Ownership ownership, final String returnTypeName,
                                       final String capitalizedFieldName, final String customArgs, final boolean constElemCount,
                                       final boolean maxOneElem, final String elemCountExpr, final String apiDocTail) {
      unit.emitln("  /**");
      generateArrayFieldNote(unit, "Getter for", null, fieldName, fieldType, ownership, constElemCount, maxOneElem, elemCountExpr, true, false);
      if( null != apiDocTail ) {
          unit.emitln("   * "+apiDocTail);
      }
      unit.emitln("   */");
      unit.emit("  public " + (staticMethod ? "static " : "final ") + (abstractMethod ? "abstract " : "") + returnTypeName + " get" + capitalizedFieldName + "(");
      if( null != customArgs ) {
          unit.emit(customArgs);
      }
      unit.emit(")");
  }
  private void generateSetterSignature(final CodeUnit unit, final MethodAccess accessMod, final boolean staticMethod, final boolean abstractMethod,
                                       final String fieldName, final Type fieldType, final Ownership ownership, final String returnTypeName,
                                       final String capitalizedFieldName, final String customArgsPre, final String paramTypeName,
                                       final String customArgsPost, final boolean constElemCount, final boolean maxOneElem, final String elemCountExpr,
                                       final String apiDocDetail, final String apiDocArgs) {
      unit.emitln("  /**");
      generateArrayFieldNote(unit, "Setter for", null, fieldName, fieldType, ownership, constElemCount, maxOneElem, elemCountExpr, true, false);
      if( null != apiDocDetail ) {
          unit.emitln("   * <p>");
          unit.emitln("   * "+apiDocDetail);
          unit.emitln("   * </p>");
      }
      if( null != apiDocArgs ) {
          unit.emitln(apiDocArgs);
      }
      unit.emitln("   */");
      unit.emit("  "+accessMod.getJavaName() + " " + (staticMethod ? "static " : "final ") + (abstractMethod ? "abstract " : "") + returnTypeName + " set" + capitalizedFieldName + "(");
      if( null != customArgsPre ) {
          unit.emit(customArgsPre+", ");
      }
      unit.emit(paramTypeName + " src");
      if( null != customArgsPost ) {
          unit.emit(", "+customArgsPost);
      }
      unit.emit(")");
  }

  private void generateOffsetAndSizeArrays(final CodeUnit unit, final String prefix,
                                           final String fieldName, final Type fieldType,
                                           final Field field, final String postfix) {
      if(null != field) {
          unit.emit(prefix+"private static final int[] "+fieldName+"_offset = new int[] { ");
          for( int i=0; i < machDescTargetConfigs.length; i++ ) {
              if(0<i) {
                  unit.emit(", ");
              }
              unit.emit(field.getOffset(machDescTargetConfigs[i].md) +
                           " /* " + machDescTargetConfigs[i].name() + " */");
          }
          unit.emitln(" };");
      }
      if(null!=fieldType) {
          unit.emit(prefix+"private static final int[] "+fieldName+"_size = new int[] { ");
          for( int i=0; i < machDescTargetConfigs.length; i++ ) {
              if(0<i) {
                  unit.emit(", ");
              }
              unit.emit(fieldType.getSize(machDescTargetConfigs[i].md) +
                           " /* " + machDescTargetConfigs[i].name() + " */");
          }
          unit.emit("  };");
          if( null != postfix ) {
              unit.emitln(postfix);
          } else {
              unit.emitln();
          }
      }
  }

  private void generateFunctionPointerCode(final Set<MethodBinding> methodBindingSet,
          final JavaCodeUnit javaUnit, final CCodeUnit jniUnit,
          final String structCTypeName, final Type containingCType, final JavaType containingJType,
          final int i, final FunctionSymbol funcSym, final String returnSizeLookupName) {
      // Emit method call and associated native code
      final MethodBinding  mb = bindFunction(funcSym, true  /* forInterface */, machDescJava, containingJType, containingCType);
      mb.findThisPointer(); // FIXME: need to provide option to disable this on per-function basis

      // JavaTypes representing C pointers in the initial
      // MethodBinding have not been lowered yet to concrete types
      final List<MethodBinding> bindings = expandMethodBinding(mb);

      final boolean useNIOOnly = true;
      final boolean useNIODirectOnly = true;

      for (final MethodBinding binding : bindings) {
          if(!methodBindingSet.add(binding)) {
              // skip .. already exisiting binding ..
              continue;
          }
          // Emit public Java entry point for calling this function pointer
          JavaMethodBindingEmitter emitter =
                  new JavaMethodBindingEmitter(binding,
                          javaUnit,
                          cfg.runtimeExceptionType(),
                          cfg.unsupportedExceptionType(),
                          true,  // emitBody
                          cfg.tagNativeBinding(),
                          false, // eraseBufferAndArrayTypes
                          useNIOOnly,
                          useNIODirectOnly,
                          false, // forDirectBufferImplementation
                          false, // forIndirectBufferAndArrayImplementation
                          false, // isUnimplemented
                          false, // isInterface
                          false, // isNativeMethod
                          false, // isPrivateNativeMethod
                          cfg);
          emitter.addModifier(JavaMethodBindingEmitter.PUBLIC);
          emitter.addModifier(JavaMethodBindingEmitter.FINAL);
          emitter.emit();
          javaUnit.emitln();

          // Emit private native Java entry point for calling this function pointer
          emitter =
                  new JavaMethodBindingEmitter(binding,
                          javaUnit,
                          cfg.runtimeExceptionType(),
                          cfg.unsupportedExceptionType(),
                          false, // emitBody
                          cfg.tagNativeBinding(),
                          true,  // eraseBufferAndArrayTypes
                          useNIOOnly,
                          useNIODirectOnly,
                          true,  // forDirectBufferImplementation
                          false, // forIndirectBufferAndArrayImplementation
                          false, // isUnimplemented
                          false, // isInterface
                          false,  // isNativeMethod
                          true, cfg);
          emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
          emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
          emitter.emit();
          javaUnit.emitln();

          // Emit (private) C entry point for calling this function pointer
          final CMethodBindingEmitter cEmitter =
                  new CMethodBindingEmitter(binding,
                          jniUnit,
                          javaUnit.pkgName,
                          containingJType.getName(),
                          true, // FIXME: this is optional at this point
                          false,
                          true,
                          false, // forIndirectBufferAndArrayImplementation
                          machDescJava, getConfiguration());
          cEmitter.setIsCStructFunctionPointer(true);
          prepCEmitter(returnSizeLookupName, binding.getJavaReturnType(), cEmitter);
          cEmitter.emit();
          jniUnit.emitln();
      }
  }

  private String getArrayArrayLengthExpr(final ArrayType type, final String returnSizeLookupName, final boolean hasFixedTypeLen[], final int[][] lengthRes) {
      final int[] length = new int[type.arrayDimension()];
      lengthRes[0] = length;
      final StringBuilder lengthExpr = new StringBuilder();
      hasFixedTypeLen[0] = true;
      ArrayType typeIter = type;
      for(int i=0; i<length.length; i++) {
          if( null!=typeIter && typeIter.hasLength() ) {
              length[i] = typeIter.getLength();
              if( 0 < i ) {
                  lengthExpr.append("*");
              }
              lengthExpr.append(length[i]);
          } else {
              length[i] = -1;
              hasFixedTypeLen[0] = false;
          }
          if( null != typeIter ) {
              typeIter = typeIter.getTargetType().asArray();
          }
      }
      final String cfgVal = cfg.returnedArrayLength(returnSizeLookupName);
      if( null != cfgVal ) {
          if( hasFixedTypeLen[0] ) {
              LOG.log(WARNING, type.getASTLocusTag(),
                      "struct array field '"+returnSizeLookupName+"' of '"+type+"' length '"+Arrays.toString(length)+"' overwritten by cfg-expression: "+cfgVal);
          }
          return cfgVal;
      }
      if( hasFixedTypeLen[0] ) {
          return lengthExpr.toString();
      } else {
          LOG.log(WARNING, type.getASTLocusTag(),
                  "struct array field '"+returnSizeLookupName+"' length '"+Arrays.toString(length)+"' without fixed- nor configured-size: {0}", type);
          return null;
      }
  }

  private boolean requiresGetCStringLength(final Type fieldType, final String returnSizeLookupName) {
      if( !cfg.returnsString(returnSizeLookupName) && !cfg.returnsStringOnly(returnSizeLookupName) ) {
          return false;
      }
      final PointerType pointerType = fieldType.asPointer();
      if( null != pointerType ) {
          return null == cfg.returnedArrayLength(returnSizeLookupName);
      }
      return false;
  }

  private static final String GetElemValueApiDocTail = "@return element value of the corresponding field-array";
  private static final String GetElemCountApiDocTail = "@return element count of the corresponding field-array";

  private static final String SetSubArrayArgsPost = "final int srcPos, final int destPos, final int length";
  private static final String SetSubArrayArgsCheck = "    if( 0 > srcPos || 0 > destPos || 0 > length || srcPos + length > src.length ) { throw new IndexOutOfBoundsException(\"src[pos \"+srcPos+\", length \"+src.length+\"], destPos \"+destPos+\", length \"+length); }";
  private static final String SetSubArrayApiDocDetail = "Copies the given source elements into the respective field's existing memory.";
  private static final String SetSubArrayApiDocArgs =
          "   * @param src the source array of elements\n"+
          "   * @param srcPos starting element position within the source array with 'srcPos >= 0` &&  `srcPos + length <= src.length`, otherwise an {@link IndexOutOfBoundsException} is thrown\n"+
          "   * @param destPos starting element position within the destination with 'destPos >= 0` && `destPos + length <= elemCount`, otherwise an exception is thrown\n"+
          "   * @param length the element count to be copied with 'length >= 0` &&  `srcPos + length <= src.length` && `destPos + length <= elemCount`, otherwise an {@link IndexOutOfBoundsException} is thrown\n"+
          "   * @return this instance of chaining";

  private static final String SetArrayArgsPre = "final boolean subset";
  private static final String SetArrayArgsPost = "final int srcPos, final int destPos, final int length";
  private static final String SetArrayArgsCheck = "    if( 0 > srcPos || 0 > destPos || 0 > length || srcPos + length > src.length ) { throw new IndexOutOfBoundsException(\"subset \"+subset+\", src[pos \"+srcPos+\", length \"+src.length+\"], destPos \"+destPos+\", length \"+length); }";
  private static final String SetArrayApiDocDetail = "Copies the given source elements into the respective field, either writing into the existing memory or creating a new memory and referencing it.";
  private static final String SetArrayApiDocArgs =
          "   * @param subset if `true` keeps the underlying memory and only allows to set up to `elemCount` elements. Otherwise may replace the underlying memory if `destPos + length != elemCount`.\n"+
          "   * @param src the source array of elements\n"+
          "   * @param srcPos starting element position within the source array with 'srcPos >= 0` &&  `srcPos + length <= src.length`, otherwise an {@link IndexOutOfBoundsException} is thrown\n"+
          "   * @param destPos starting element position within the destination with 'destPos >= 0`. If `subset == true`, `destPos + length <= elemCount` also must be be `true`. Otherwise an exception is thrown\n"+
          "   * @param length the element count to be copied with 'length >= 0` &&  `srcPos + length <= src.length`, otherwise an {@link IndexOutOfBoundsException} is thrown\n"+
          "   * @return this instance of chaining";

  private static final String SetReplaceArrayArgsPost = "final int srcPos, final int length";
  private static final String SetReplaceArrayArgsCheck = "    if( 0 > srcPos || 0 > length || srcPos + length > src.length ) { throw new IndexOutOfBoundsException(\"src[pos \"+srcPos+\", length \"+src.length+\"], length \"+length); }";
  private static final String SetReplaceArrayApiDocDetail = "Replaces the respective field's memory with a new memory segment containing given source elements and referencing it.";
  private static final String SetReplaceArrayApiDocArgs =
          "   * @param src the source array of elements\n"+
          "   * @param srcPos starting element position within the source array with 'srcPos >= 0` &&  `srcPos + length <= src.length`, otherwise an {@link IndexOutOfBoundsException} is thrown\n"+
          "   * @param length the element count to be copied with 'length >= 0` &&  `srcPos + length <= src.length`, otherwise an {@link IndexOutOfBoundsException} is thrown\n"+
          "   * @return this instance of chaining";

  private static final String GetArrayArgs = "final int destPos, final int length";
  private static final String GetArrayArgsCheck = "    if( 0 > srcPos || 0 > destPos || 0 > length || destPos + length > dest.length ) { throw new IndexOutOfBoundsException(\"dest[pos \"+destPos+\", length \"+dest.length+\"], srcPos \"+srcPos+\", length \"+length); }";


  private void generateArrayGetterSetterCode(final JavaCodeUnit unit,
                                             final CompoundType structCType,
                                             final JavaType containingJType,
                                             final int i, final Field field, final String fieldName,
                                             final boolean immutableAccess,
                                             final String fqStructFieldName) throws Exception {
      final Type fieldType = field.getType();
      final TypeInfo opaqueTypeInfo = cfg.typeInfo(fieldType);
      final boolean isOpaque = null != opaqueTypeInfo;
      final Type baseElemType = fieldType.getArrayBaseOrPointerTargetType();
      if( GlueGen.debug() ) {
          System.err.printf("SE.ac.%02d: fieldName    %s, fqName %s%n", (i+1), fieldName, fqStructFieldName);
          System.err.printf("SE.ac.%02d: structCType  %s, %s%n", (i+1), structCType.toString(), structCType.getSignature(null).toString());
          System.err.printf("SE.ac.%02d: fieldType    %s, %s%n", (i+1), fieldType.toString(), fieldType.getSignature(null).toString());
          System.err.printf("SE.ac.%02d: opaqueInfo   %b, %s%n", (i+1), isOpaque, opaqueTypeInfo);
          System.err.printf("SE.ac.%02d: baseElemType %s, %s%n", (i+1), baseElemType.toString(), baseElemType.getSignature(null).toString());
      }

      //
      // Collect all required information including considering Opaque types
      //
      final String containingJTypeName = containingJType.getName();
      final boolean isStringOnly = cfg.returnsStringOnly(fqStructFieldName); // exclude alternative ByteBuffer representation to String
      final boolean isString = isStringOnly || cfg.returnsString(fqStructFieldName);
      if( isString ) {
          unit.addTailCode(optStringCharsetCode);
      }
      final boolean isPointer;
      final boolean isPrimitive;
      final boolean isConstValue; // Immutable 'const type value', immutable array 'const type value[]', or as mutable pointer 'const type * value'
      final MethodAccess accessMod = MethodAccess.PUBLIC;
      final Ownership ownership;
      final String elemCountExpr;
      final boolean constElemCount; // if true, implies native ownership for pointer referenced memory!
      final boolean staticElemCount;
      final JavaType baseJElemType;
      final String baseJElemTypeName;
      final boolean primElemFixedSize; // Is Primitive element size fixed? If not, use md.*_Size[]
      final boolean baseIsPointer; // Is Primitive element a pointer?
      final String baseElemSizeDenominator;
      final boolean useGetCStringLength;
      final boolean maxOneElement; // zero or one element
      if( isOpaque && opaqueTypeInfo.pointerDepth() <= 1 || ( fieldType.isPrimitive() && !baseElemType.isFunctionPointer() ) ) {
          // Overridden by JavaConfiguration.typeInfo(..), i.e. Opaque!
          // Emulating array w/ 1 element
          isPrimitive = true;
          isPointer = false;
          isConstValue = fieldType.isConst();
          elemCountExpr = "1";
          constElemCount = true;
          ownership = Ownership.Parent;
          staticElemCount = true;
          baseJElemType = null;
          baseJElemTypeName = compatiblePrimitiveJavaTypeName(fieldType, machDescJava);
          primElemFixedSize = false;
          baseIsPointer = fieldType.isPointer();
          baseElemSizeDenominator = baseIsPointer ? "pointer" : baseJElemTypeName ;
          useGetCStringLength = false;
          maxOneElement = true;
      } else {
          if( fieldType.arrayDimension() > 0 ) {
              final int[][] arrayLengthRes = new int[1][];
              final boolean[] _useFixedArrayLen = { false };
              elemCountExpr = getArrayArrayLengthExpr(fieldType.asArray(), fqStructFieldName, _useFixedArrayLen, arrayLengthRes);
              // final int arrayLength = arrayLengthRes[0][0];
              constElemCount = _useFixedArrayLen[0];
              ownership = Ownership.Parent; // a fixed linear array
              staticElemCount = constElemCount;
              // baseCElemType = pointerType.getBaseType();
              isPointer = false;
              useGetCStringLength = false;
          } else {
              final String _elemCountExpr = cfg.returnedArrayLength(fqStructFieldName);
              isPointer = true;
              if( null == _elemCountExpr && isString ) {
                  useGetCStringLength = true;
                  unit.addTailCode(optStringMaxStrnlenCode);
                  elemCountExpr = "Buffers.strnlen(pString, _max_strnlen)+1";
                  constElemCount = false;
                  ownership = Ownership.Java;
                  staticElemCount = constElemCount;
              } else if( null == _elemCountExpr ) {
                  useGetCStringLength = false;
                  elemCountExpr = "0";
                  constElemCount = false;
                  ownership = Ownership.Java;
                  staticElemCount = constElemCount;
              } else {
                  // null != _elemCountExpr
                  useGetCStringLength = false;
                  elemCountExpr = _elemCountExpr;
                  boolean _constElemCount = false;
                  boolean _staticElemCount = false;

                  // try constant intenger 1st
                  try {
                      Integer.parseInt(elemCountExpr);
                      _constElemCount = true;
                      _staticElemCount = true;
                  } catch (final Exception e ) {}
                  if( !_constElemCount ) {
                      // check for const length field
                      if( elemCountExpr.startsWith("get") && elemCountExpr.endsWith("()") ) {
                          final String lenFieldName = decapitalizeString( elemCountExpr.substring(3, elemCountExpr.length()-2) );
                          final Field lenField = structCType.getField(lenFieldName);
                          if( null != lenField ) {
                              _constElemCount = lenField.getType().isConst();
                          }
                          LOG.log(INFO, structCType.getASTLocusTag(),
                                  unit.className+": elemCountExpr "+elemCountExpr+", lenFieldName "+lenFieldName+" -> "+lenField.toString()+", isConst "+_constElemCount);
                      }
                  }
                  constElemCount = _constElemCount;
                  if( constElemCount ) {
                      ownership = Ownership.Native;
                  } else {
                      ownership = Ownership.Mixed;
                  }
                  staticElemCount = _staticElemCount;
              }
          }
          if( null == elemCountExpr ) {
              final String msg = "SKIP unsized array in struct: "+fqStructFieldName+": "+fieldType.getSignature(null).toString();
              unit.emitln("  // "+msg);
              unit.emitln();
              LOG.log(WARNING, structCType.getASTLocusTag(), msg);
              throw new InternalError(msg);
              // return; // FIXME: Remove block unreachable
          }
          boolean _maxOneElement = cfg.maxOneElement(fqStructFieldName);
          if( !_maxOneElement ) {
              try {
                  _maxOneElement = 1 == Integer.parseInt(elemCountExpr);
              } catch (final Exception e ) {}
          }
          maxOneElement = _maxOneElement;
          if( GlueGen.debug() ) {
              System.err.printf("SE.ac.%02d: ownership %s%n", (i+1), ownership);
          }

          if( !baseElemType.hasSize() ) { // like 'void*' -> 'void'
              final String msg = "SKIP unsized field in struct: "+fqStructFieldName+": fieldType "+fieldType.getSignature(null).toString()+", baseType "+baseElemType.getSignature(null).toString();
              unit.emitln("  // "+msg);
              unit.emitln();
              LOG.log(WARNING, structCType.getASTLocusTag(), msg);
              return;
          }

          baseIsPointer = baseElemType.isPointer();
          isConstValue = baseElemType.isConst();
          if( baseIsPointer ) {
              baseJElemType = javaType(Long.TYPE); // forced mapping pointer-pointer -> long
          } else {
              try {
                  baseJElemType = typeToJavaType(baseElemType, machDescJava);
              } catch (final Exception e ) {
                  throw new GlueGenException("Error occurred while creating array/pointer accessor for field \"" +
                                              fqStructFieldName + "\", baseType "+baseElemType.getDebugString()+", topType "+fieldType.getDebugString(),
                                              fieldType.getASTLocusTag(), e);
              }
          }
          baseJElemTypeName = baseJElemType.getName();
          isPrimitive = baseJElemType.isPrimitive() || baseElemType.isPrimitive() || baseElemType.isFunctionPointer();
          primElemFixedSize = isPrimitive ? baseElemType.getSize().hasFixedNativeSize() : false;
          baseElemSizeDenominator = baseIsPointer ? "pointer" : baseJElemTypeName ;
      }
      if( GlueGen.debug() ) {
          System.err.printf("SE.ac.%02d: baseJElemType %s%n", (i+1), (null != baseJElemType ? baseJElemType.getDebugString() : null));
      }
      // Collect fixed primitive-type mapping metrics
      final String primJElemTypeBufferName;
      final int primElemSize;
      final String primElemSizeExpr;
      final boolean isByteBuffer;
      if( isPrimitive ) {
          final Class<? extends Buffer> primJElemTypeBufferClazz = Buffers.typeNameToBufferClass(baseJElemTypeName);
          if( null == primJElemTypeBufferClazz ) {
              final String msg = "Failed to map '"+baseJElemTypeName+"' to Buffer class, field "+field+", j-type "+baseJElemType;
              unit.emitln("  // ERROR: "+msg);
              unit.emitln();
              LOG.log(SEVERE, structCType.getASTLocusTag(), msg);
              throw new InternalError(msg);
          }
          primJElemTypeBufferName = primJElemTypeBufferClazz.getSimpleName();
          primElemSize = Buffers.sizeOfBufferElem(primJElemTypeBufferClazz);
          isByteBuffer = null != primJElemTypeBufferClazz ? ByteBuffer.class.isAssignableFrom(primJElemTypeBufferClazz) : false;
          if( primElemFixedSize ) {
              primElemSizeExpr = String.valueOf(primElemSize);
          } else {
              primElemSizeExpr = "md."+baseElemSizeDenominator+"SizeInBytes()";
          }
      } else {
          primJElemTypeBufferName = null;
          primElemSize = 0;
          isByteBuffer = false;
          primElemSizeExpr = null;
      }

      final String capitalFieldName = capitalizeString(fieldName);
      final boolean ownElemCountHandling;
      final String getElemCountFuncExpr, setElemCountLengthFunc;
      if( constElemCount ) {
          ownElemCountHandling = true;
          getElemCountFuncExpr = "get"+capitalFieldName+"ElemCount()";
          setElemCountLengthFunc = null;
      } else {
          if( useGetCStringLength ) {
              ownElemCountHandling = true;
              getElemCountFuncExpr = "get"+capitalFieldName+"ElemCount()";
              setElemCountLengthFunc = null;
          } else if( elemCountExpr.startsWith("get") && elemCountExpr.endsWith("()") ) {
              ownElemCountHandling = false;
              getElemCountFuncExpr = elemCountExpr;
              setElemCountLengthFunc = "set" + elemCountExpr.substring(3, elemCountExpr.length()-2);
          } else {
              ownElemCountHandling = true;
              getElemCountFuncExpr = "get"+capitalFieldName+"ElemCount()";
              setElemCountLengthFunc = "set"+capitalFieldName+"ElemCount";
          }
      }
      if( GlueGen.debug() ) {
          System.err.printf("SE.ac.%02d: baseJElemTypeName %s%n", (i+1), baseJElemTypeName);
          System.err.printf("SE.ac.%02d: elemCountExpr: %s (const %b, ownership %s), ownArrayLenCpde %b, maxOneElement %b, "+
                            "Primitive[is %b, aptr %b, buffer %s, fixedSize %b, elemSize %d, sizeDenom %s, sizeExpr %s, isByteBuffer %b], "+
                            "isString[%b, only %b, strnlen %b], isPointer %b, isOpaque %b, constVal %b, immutableAccess %b%n",
                  (i+1), elemCountExpr, constElemCount, ownership, ownElemCountHandling, maxOneElement,
                  isPrimitive, baseIsPointer, primJElemTypeBufferName, primElemFixedSize, primElemSize, baseElemSizeDenominator, primElemSizeExpr, isByteBuffer,
                  isString, isStringOnly, useGetCStringLength,
                  isPointer, isOpaque, isConstValue, immutableAccess);
      }

      //
      // Emit ..
      //
      if( ownElemCountHandling ) {
          if( constElemCount ) {
              generateGetterSignature(unit, staticElemCount, false, fieldName, fieldType, ownership, "int", capitalFieldName+"ElemCount", null, constElemCount, maxOneElement, elemCountExpr, GetElemCountApiDocTail);
              unit.emitln(" { return "+elemCountExpr+"; }");
          } else if( useGetCStringLength ) {
              generateGetterSignature(unit, staticElemCount, false, fieldName, fieldType, ownership, "int", capitalFieldName+"ElemCount", null, constElemCount, maxOneElement, elemCountExpr, GetElemCountApiDocTail);
              unit.emitln(" {");
              unit.emitln("    final long pString = PointerBuffer.wrap( accessor.slice(" + fieldName+"_offset[mdIdx],  PointerBuffer.POINTER_SIZE) ).get(0);");
              unit.emitln("    return 0 != pString ? "+elemCountExpr+" : 0;");
              unit.emitln("  }");
          } else {
              unit.emitln("  private int _"+fieldName+"ArrayLen = "+elemCountExpr+"; // "+(constElemCount ? "const" : "initial")+" array length");
              generateGetterSignature(unit, staticElemCount, false, fieldName, fieldType, ownership, "int", capitalFieldName+"ElemCount", null, constElemCount, maxOneElement, elemCountExpr, GetElemCountApiDocTail);
              unit.emitln("  { return _"+fieldName+"ArrayLen; }");
              if( !immutableAccess ) {
                  generateSetterSignature(unit, MethodAccess.PRIVATE, staticElemCount, false, fieldName, fieldType, ownership, "void", capitalFieldName+"ElemCount", null, "int",
                                          null, constElemCount, maxOneElement, elemCountExpr, null, null);
                  unit.emitln("  { _"+fieldName+"ArrayLen = src; }");
              }
          }
          unit.emitln();
      }

      // Null query for pointer
      if( isPointer ) {
          generateIsNullSignature(unit, false, fieldName, fieldType, ownership, capitalFieldName, constElemCount, maxOneElement, elemCountExpr);
          unit.emitln(" {");
          unit.emitln("    return 0 == PointerBuffer.wrap(getBuffer(), "+fieldName+"_offset[mdIdx], 1).get(0);");
          unit.emitln("  }");
          unit.emitln();
          if( !constElemCount && !immutableAccess ) {
              generateReleaseSignature(unit, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, constElemCount, maxOneElement, elemCountExpr);
              unit.emitln(" {");
              unit.emitln("    accessor.setLongAt("+fieldName+"_offset[mdIdx], 0, md.pointerSizeInBytes()); // write nullptr");
              unit.emitln("    _eb"+capitalFieldName+" = null;");
              emitSetElemCount(unit, setElemCountLengthFunc, "0", !useGetCStringLength, capitalFieldName, structCType, "    ");
              unit.emitln("    return this;");
              unit.emitln("  }");
              unit.emitln("  @SuppressWarnings(\"unused\")");
              if( baseIsPointer ) {
                  unit.emitln("  private PointerBuffer _eb"+capitalFieldName+"; // cache new memory buffer ensuring same lifecycle");
              } else {
                  unit.emitln("  private ElementBuffer _eb"+capitalFieldName+"; // cache new memory buffer ensuring same lifecycle");
              }
              unit.emitln();
          }
      }

      // Setter
      if( immutableAccess ) {
          generateArrayFieldNote(unit, "  /** SKIP setter for immutable", " */", fieldName, fieldType, ownership, constElemCount, maxOneElement, elemCountExpr, false, false);
          unit.emitln();
      } else if( isPointer && isConstValue && ( Ownership.Native == ownership || constElemCount ) ) {
          generateArrayFieldNote(unit, "  /** SKIP setter for constValue constElemCount Pointer w/ native ownership", " */", fieldName, fieldType, ownership, constElemCount, maxOneElement, elemCountExpr, false, false);
          unit.emitln();
      } else if( !isPointer && isConstValue ) {
          generateArrayFieldNote(unit, "  /** SKIP setter for constValue Array", " */", fieldName, fieldType, ownership, constElemCount, maxOneElement, elemCountExpr, false, false);
          unit.emitln();
      } else if( isPrimitive ) {
          // Setter Primitive
          if( maxOneElement ) {
              // Setter Primitive Single Pointer + Array
              if( isPointer ) {
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, null, baseJElemTypeName,
                                          null, constElemCount, maxOneElement, elemCountExpr, null, null);
                  if( isConstValue ) {
                      // constElemCount/Ownership.Native excluded: SKIP setter for constValue constElemCount Pointer w/ native ownership
                      if( Ownership.Native == ownership ) {
                          throw new InternalError("Native ownership but adding potential memory-replacement for '"+fqStructFieldName+"': "+fieldType.getSignature(null).toString());
                      }
                      unit.emitln(" {");
                      if( baseIsPointer ) {
                          unit.emitln("    final PointerBuffer eb = PointerBuffer.allocateDirect(1);");
                          unit.emitln("    eb.put(0, src);");
                      } else {
                          unit.emitln("    final ElementBuffer eb = ElementBuffer.allocateDirect("+primElemSizeExpr+", 1);");
                          unit.emit  ("    eb.getByteBuffer()");
                          if( !isByteBuffer ) {
                              unit.emit(".as"+primJElemTypeBufferName+"()");
                          }
                          unit.emitln(".put(0, src);");
                      }
                      unit.emitln("    eb.storeDirectAddress(getBuffer(), "+fieldName+"_offset[mdIdx]);");
                      unit.emitln("    _eb"+capitalFieldName+" = eb;");
                      emitSetElemCount(unit, setElemCountLengthFunc, "1", !useGetCStringLength, capitalFieldName, structCType, "      ");
                      unit.emitln("    return this;");
                      unit.emitln("  }");
                  } else {
                      unit.emitln(" {");
                      unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                      unit.emitln("    if( 1 == elemCount ) {");
                      if( baseIsPointer ) {
                          unit.emitln("      PointerBuffer.derefPointer(getBuffer(), "+fieldName+"_offset[mdIdx], 1)");
                          unit.emitln("        .put(0, src);");
                      } else {
                          unit.emitln("      ElementBuffer.derefPointer("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], 1)");
                          unit.emit  ("        .getByteBuffer()");
                          if( !isByteBuffer ) {
                              unit.emit(".as"+primJElemTypeBufferName+"()");
                          }
                          unit.emitln(".put(0, src);");
                      }
                      unit.emitln("    } else {");
                      if( constElemCount || Ownership.Native == ownership ) {
                          unit.emitln("      throw new RuntimeException(\"Primitive '"+fieldName+"' of "+ownership+" ownership and maxOneElement has "
                                                  +(constElemCount?"const":"")+"elemCount \"+elemCount);");
                          unit.emitln("    }");
                          unit.emitln("    return this;");
                          unit.emitln("  }");
                      } else {
                          if( baseIsPointer ) {
                              unit.emitln("      final PointerBuffer eb = PointerBuffer.allocateDirect(1);");
                              unit.emitln("      eb.put(0, src);");
                          } else {
                              unit.emitln("      final ElementBuffer eb = ElementBuffer.allocateDirect("+primElemSizeExpr+", 1);");
                              unit.emit  ("      eb.getByteBuffer()");
                              if( !isByteBuffer ) {
                                  unit.emit(".as"+primJElemTypeBufferName+"()");
                              }
                              unit.emitln(".put(0, src);");
                          }
                          unit.emitln("      eb.storeDirectAddress(getBuffer(), "+fieldName+"_offset[mdIdx]);");
                          unit.emitln("      _eb"+capitalFieldName+" = eb;");
                          emitSetElemCount(unit, setElemCountLengthFunc, "1", !useGetCStringLength, capitalFieldName, structCType, "      ");
                          unit.emitln("    }");
                          unit.emitln("    return this;");
                          unit.emitln("  }");
                      }
                  }
              } else { // array && !isConstValue
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, null, baseJElemTypeName,
                                          null, constElemCount, maxOneElement, elemCountExpr, null, null);
                  unit.emitln(" {");
                  if( baseIsPointer ) {
                      unit.emitln("    PointerBuffer.wrap(getBuffer(), "+fieldName+"_offset[mdIdx], 1).put(0, src);");
                  } else {
                      unit.emitln("    ElementBuffer.wrap("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], 1)");
                      unit.emit  ("      .getByteBuffer()");
                      if( !isByteBuffer ) {
                          unit.emit(".as"+primJElemTypeBufferName+"()");
                      }
                      unit.emitln(".put(0, src);");
                  }
                  unit.emitln("    return this;");
                  unit.emitln("  }");
              } // else SKIP setter for constValue Array
              unit.emitln();
          } else {
              // Setter Primitive n Pointer + Array
              boolean doneString = false;

              if( isString && isByteBuffer && isPointer ) { // isConst is OK
                  // isConst && constElemCount/Ownership.Native excluded: SKIP setter for constValue constElemCount Pointer w/ native ownership
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, null, "String",
                                          null, constElemCount, maxOneElement, elemCountExpr, null, null);
                  unit.emitln(" {");
                  unit.emitln("    final byte[] srcBytes = src.getBytes(_charset);");
                  if( constElemCount || Ownership.Native == ownership ) {
                      unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                      unit.emitln("    if( srcBytes.length + 1 != elemCount ) { throw new IllegalArgumentException(\"strlen+1 \"+(srcBytes.length+1)+\" != "
                                      +(constElemCount?"const":"")+" elemCount \"+elemCount+\" of "+ownership+" ownership\"); };");
                      unit.emitln("    final ElementBuffer eb = ElementBuffer.derefPointer("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                  } else {
                      unit.emitln("    final ElementBuffer eb = ElementBuffer.allocateDirect("+primElemSizeExpr+", srcBytes.length + 1);");
                  }
                  unit.emitln("    eb.getByteBuffer().put(srcBytes, 0, srcBytes.length).put((byte)0).rewind(); // w/ EOS");
                  if( !constElemCount ) {
                      unit.emitln("    eb.storeDirectAddress(getBuffer(), "+fieldName+"_offset[mdIdx]);");
                      unit.emitln("    _eb"+capitalFieldName+" = eb;");
                      emitSetElemCount(unit, setElemCountLengthFunc, "srcBytes.length + 1", !useGetCStringLength, capitalFieldName, structCType, "    ");
                  }
                  unit.emitln("    return this;");
                  unit.emitln("  }");
                  unit.emitln();
                  doneString = true;
              }
              if( doneString && isStringOnly ) {
                  generateArrayFieldNote(unit, "  /** SKIP setter for String alternative (ByteBuffer)", " */", fieldName, fieldType, ownership, constElemCount, maxOneElement, elemCountExpr, false, false);
              } else if( isConstValue ) {
                  if( isPointer ) {
                      // constElemCount/Ownership.Native excluded: SKIP setter for constValue constElemCount Pointer w/ native ownership
                      generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, null,
                                              baseJElemTypeName+"[]", SetReplaceArrayArgsPost, constElemCount, maxOneElement, elemCountExpr, SetReplaceArrayApiDocDetail, SetReplaceArrayApiDocArgs);
                      if( Ownership.Native == ownership ) {
                          throw new InternalError("Native ownership but adding potential memory-replacement for '"+fqStructFieldName+"': "+fieldType.getSignature(null).toString());
                      }
                      unit.emitln(" {");
                      // JAU01 unit.emitln(SetReplaceArrayArgsCheck);
                      if( baseIsPointer ) {
                          unit.emitln("    final PointerBuffer eb = PointerBuffer.allocateDirect(length);");
                      } else {
                          unit.emitln("    final ElementBuffer eb = ElementBuffer.allocateDirect("+primElemSizeExpr+", length);");
                      }
                      unit.emitln("    eb.put(src, srcPos, 0, length).storeDirectAddress(getBuffer(), "+fieldName+"_offset[mdIdx]);");
                      unit.emitln("    _eb"+capitalFieldName+" = eb;");
                      emitSetElemCount(unit, setElemCountLengthFunc, "length", !useGetCStringLength, capitalFieldName, structCType, "    ");
                      unit.emitln("    return this;");
                      unit.emitln("  }");
                  } // else SKIP setter for constValue Array
              } else if( constElemCount || !isPointer ) {
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, null,
                                          baseJElemTypeName+"[]", SetSubArrayArgsPost, constElemCount, maxOneElement, elemCountExpr, SetSubArrayApiDocDetail, SetSubArrayApiDocArgs);
                  unit.emitln(" {");
                  // JAU01 unit.emitln(SetSubArrayArgsCheck);
                  unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                  // JAU01 unit.emitln("    if( destPos + length > elemCount ) { throw new IndexOutOfBoundsException(\"destPos \"+destPos+\" + length \"+length+\" > elemCount \"+elemCount); };");
                  if( baseIsPointer ) {
                      if( isPointer ) {
                          unit.emitln("    final PointerBuffer eb = PointerBuffer.derefPointer(getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                      } else {
                          unit.emitln("    final PointerBuffer eb = PointerBuffer.wrap(getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                      }
                  } else {
                      if( isPointer ) {
                          unit.emitln("    final ElementBuffer eb = ElementBuffer.derefPointer("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                      } else {
                          unit.emitln("    final ElementBuffer eb = ElementBuffer.wrap("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                      }
                  }
                  unit.emitln("    eb.put(src, srcPos, destPos, length);");
                  unit.emitln("    return this;");
                  unit.emitln("  }");
              } else /* if( !constElemCount && isPointer ) */ {
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, SetArrayArgsPre,
                                          baseJElemTypeName+"[]", SetArrayArgsPost, constElemCount, maxOneElement, elemCountExpr, SetArrayApiDocDetail, SetArrayApiDocArgs);
                  if( Ownership.Native == ownership ) {
                      throw new InternalError("Native ownership but adding potential memory-replacement for '"+fqStructFieldName+"': "+fieldType.getSignature(null).toString());
                  }
                  unit.emitln(" {");
                  // JAU01 unit.emitln(SetArrayArgsCheck);
                  unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                  unit.emitln("    if( subset || destPos + length == elemCount ) {");
                  // JAU01 unit.emitln("      if( destPos + length > elemCount ) { throw new IndexOutOfBoundsException(\"subset \"+subset+\", destPos \"+destPos+\" + length \"+length+\" > elemCount \"+elemCount); };");
                  if( baseIsPointer ) {
                      unit.emitln("      final PointerBuffer eb = PointerBuffer.derefPointer(getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                  } else {
                      unit.emitln("      final ElementBuffer eb = ElementBuffer.derefPointer("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                  }
                  unit.emitln("      eb.put(src, srcPos, destPos, length);");
                  unit.emitln("    } else {");
                  unit.emitln("      final int newElemCount = destPos + length;");
                  if( baseIsPointer ) {
                      unit.emitln("      final PointerBuffer eb = PointerBuffer.allocateDirect(newElemCount);");
                      unit.emitln("      if( 0 < destPos ) {");
                      unit.emitln("        final PointerBuffer pre_eb = PointerBuffer.derefPointer(getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                      unit.emitln("        pre_eb.position(0).limit(destPos);");
                      unit.emitln("        eb.put(pre_eb).rewind();");
                      unit.emitln("      }");
                  } else {
                      unit.emitln("      final ElementBuffer eb = ElementBuffer.allocateDirect("+primElemSizeExpr+", newElemCount);");
                      unit.emitln("      if( 0 < destPos ) {");
                      unit.emitln("        final ElementBuffer pre_eb = ElementBuffer.derefPointer("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                      unit.emitln("        eb.put(pre_eb.getByteBuffer(), 0, 0, destPos);");
                      unit.emitln("      }");
                  }
                  unit.emitln("      eb.put(src, srcPos, destPos, length);");
                  unit.emitln("      eb.storeDirectAddress(getBuffer(), "+fieldName+"_offset[mdIdx]);");
                  unit.emitln("      _eb"+capitalFieldName+" = eb;");
                  emitSetElemCount(unit, setElemCountLengthFunc, "newElemCount", !useGetCStringLength, capitalFieldName, structCType, "      ");
                  unit.emitln("    }");
                  unit.emitln("    return this;");
                  unit.emitln("  }");
              }
              unit.emitln();
          }
      } else {
          // Setter Struct
          if( maxOneElement ) {
              // Setter Struct Single Pointer + Array
              if( isPointer ) {
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, null, baseJElemTypeName,
                                          null, constElemCount, maxOneElement, elemCountExpr, null, null);
                  if( isConstValue ) {
                      // constElemCount/Ownership.Native excluded: SKIP setter for constValue constElemCount Pointer w/ native ownership
                      if( Ownership.Native == ownership ) {
                          throw new InternalError("Native ownership but adding potential memory-replacement for '"+fqStructFieldName+"': "+fieldType.getSignature(null).toString());
                      }
                      unit.emitln(" {");
                      unit.emitln("    final ElementBuffer eb = ElementBuffer.allocateDirect("+baseJElemTypeName+".size(), 1);");
                      unit.emitln("    eb.put(0, src.getBuffer());");
                      unit.emitln("    eb.storeDirectAddress(getBuffer(), "+fieldName+"_offset[mdIdx]);");
                      unit.emitln("    _eb"+capitalFieldName+" = eb;");
                      emitSetElemCount(unit, setElemCountLengthFunc, "1", !useGetCStringLength, capitalFieldName, structCType, "      ");
                      unit.emitln("    return this;");
                      unit.emitln("  }");
                  } else {
                      unit.emitln(" {");
                      unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                      unit.emitln("    if( 1 == elemCount ) {");
                      unit.emitln("      ElementBuffer.derefPointer("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], 1)");
                      unit.emitln("        .put(0, src.getBuffer());");
                      unit.emitln("    } else {");
                      if( constElemCount || Ownership.Native == ownership ) {
                          unit.emitln("      throw new RuntimeException(\"Primitive '"+fieldName+"' of "+ownership+" ownership and maxOneElement has "
                                                  +(constElemCount?"const":"")+"elemCount \"+elemCount);");
                          unit.emitln("    }");
                          unit.emitln("    return this;");
                          unit.emitln("  }");
                      } else {
                          unit.emitln("      final ElementBuffer eb = ElementBuffer.allocateDirect("+baseJElemTypeName+".size(), 1);");
                          unit.emitln("      eb.put(0, src.getBuffer());");
                          unit.emitln("      eb.storeDirectAddress(getBuffer(), "+fieldName+"_offset[mdIdx]);");
                          unit.emitln("      _eb"+capitalFieldName+" = eb;");
                          emitSetElemCount(unit, setElemCountLengthFunc, "1", !useGetCStringLength, capitalFieldName, structCType, "      ");
                          unit.emitln("    }");
                          unit.emitln("    return this;");
                          unit.emitln("  }");
                      }
                  }
              } else if( !isConstValue ) { // array && !isConstValue
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, null, baseJElemTypeName,
                                          null, constElemCount, maxOneElement, elemCountExpr, null, null);
                  unit.emitln(" {");
                  unit.emitln("    ElementBuffer.wrap("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], 1)");
                  unit.emitln("      .put(0, src.getBuffer());");
                  unit.emitln("    return this;");
                  unit.emitln("  }");
              } // else SKIP setter for constValue Array
              unit.emitln();
          } else {
              // Setter Struct n Pointer + Array
              if( isConstValue ) {
                  if( isPointer ) {
                      // constElemCount/Ownership.Native excluded: SKIP setter for constValue constElemCount Pointer w/ native ownership
                      generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, null,
                                              baseJElemTypeName+"[]", SetReplaceArrayArgsPost, constElemCount, maxOneElement, elemCountExpr, SetReplaceArrayApiDocDetail, SetReplaceArrayApiDocArgs);
                      if( Ownership.Native == ownership ) {
                          throw new InternalError("Native ownership but adding potential memory-replacement for '"+fqStructFieldName+"': "+fieldType.getSignature(null).toString());
                      }
                      unit.emitln(" {");
                      unit.emitln(SetReplaceArrayArgsCheck);
                      unit.emitln("    final ElementBuffer eb = ElementBuffer.allocateDirect("+baseJElemTypeName+".size(), length);");
                      unit.emitln("    for(int i=0; i<length; ++i) {");
                      unit.emitln("      eb.put(i, src[srcPos+i].getBuffer());");
                      unit.emitln("    }");
                      unit.emitln("    eb.storeDirectAddress(getBuffer(), "+fieldName+"_offset[mdIdx]);");
                      unit.emitln("    _eb"+capitalFieldName+" = eb;");
                      emitSetElemCount(unit, setElemCountLengthFunc, "length", !useGetCStringLength, capitalFieldName, structCType, "    ");
                      unit.emitln("    return this;");
                      unit.emitln("  }");
                  } // else SKIP setter for constValue Array
              } else if( constElemCount || !isPointer ) {
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, null,
                                          baseJElemTypeName+"[]", SetSubArrayArgsPost, constElemCount, maxOneElement, elemCountExpr, SetSubArrayApiDocDetail, SetSubArrayApiDocArgs);
                  unit.emitln(" {");
                  unit.emitln(SetSubArrayArgsCheck);
                  unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                  unit.emitln("    if( destPos + length > elemCount ) { throw new IndexOutOfBoundsException(\"destPos \"+destPos+\" + length \"+length+\" > elemCount \"+elemCount); };");
                  if( isPointer ) {
                      unit.emitln("    final ElementBuffer eb = ElementBuffer.derefPointer("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                  } else {
                      unit.emitln("    final ElementBuffer eb = ElementBuffer.wrap("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                  }
                  unit.emitln("    for(int i=0; i<length; ++i) {");
                  unit.emitln("      eb.put(destPos+i, src[srcPos+i].getBuffer());");
                  unit.emitln("    }");
                  unit.emitln("    return this;");
                  unit.emitln("  }");
              } else /* if( !constElemCount && isPointer ) */ {
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, SetArrayArgsPre,
                                          baseJElemTypeName+"[]", SetArrayArgsPost, constElemCount, maxOneElement, elemCountExpr, SetArrayApiDocDetail, SetArrayApiDocArgs);
                  if( Ownership.Native == ownership ) {
                      throw new InternalError("Native ownership but adding potential memory-replacement for '"+fqStructFieldName+"': "+fieldType.getSignature(null).toString());
                  }
                  unit.emitln(" {");
                  unit.emitln(SetArrayArgsCheck);
                  unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                  unit.emitln("    if( subset || destPos + length == elemCount ) {");
                  unit.emitln("      if( destPos + length > elemCount ) { throw new IndexOutOfBoundsException(\"subset \"+subset+\", destPos \"+destPos+\" + length \"+length+\" > elemCount \"+elemCount); };");
                  unit.emitln("      final ElementBuffer eb = ElementBuffer.derefPointer("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                  unit.emitln("      for(int i=0; i<length; ++i) {");
                  unit.emitln("        eb.put(destPos+i, src[srcPos+i].getBuffer());");
                  unit.emitln("      }");
                  unit.emitln("    } else {");
                  unit.emitln("      final int newElemCount = destPos + length;");
                  unit.emitln("      final ElementBuffer eb = ElementBuffer.allocateDirect("+baseJElemTypeName+".size(), newElemCount);");

                  unit.emitln("      if( 0 < destPos ) {");
                  unit.emitln("        final ElementBuffer pre_eb = ElementBuffer.derefPointer("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
                  unit.emitln("        eb.put(pre_eb.getByteBuffer(), 0, 0, destPos);");
                  unit.emitln("      }");
                  unit.emitln("      for(int i=0; i<length; ++i) {");
                  unit.emitln("        eb.put(destPos+i, src[srcPos+i].getBuffer());");
                  unit.emitln("      }");
                  unit.emitln("      eb.storeDirectAddress(getBuffer(), "+fieldName+"_offset[mdIdx]);");
                  unit.emitln("      _eb"+capitalFieldName+" = eb;");
                  emitSetElemCount(unit, setElemCountLengthFunc, "newElemCount", !useGetCStringLength, capitalFieldName, structCType, "      ");
                  unit.emitln("    }");
                  unit.emitln("    return this;");
                  unit.emitln("  }");
              }
              unit.emitln();
              if( !isConstValue ) {
                  generateSetterSignature(unit, accessMod, false, false, fieldName, fieldType, ownership, containingJTypeName, capitalFieldName, "final int destPos", baseJElemTypeName,
                                          null, constElemCount, maxOneElement, elemCountExpr, null, null);
                  unit.emitln(" {");
                  unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                  unit.emitln("    if( destPos + 1 > elemCount ) { throw new IndexOutOfBoundsException(\"destPos \"+destPos+\" + 1 > elemCount \"+elemCount); };");
                  if( isPointer ) {
                      unit.emitln("    ElementBuffer.derefPointer("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], elemCount)");
                  } else {
                      unit.emitln("    ElementBuffer.wrap("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], elemCount)");
                  }
                  unit.emitln("      .put(destPos, src.getBuffer());");
                  unit.emitln("    return this;");
                  unit.emitln("  }");
                  unit.emitln();
              }
          }
      }

      // Getter
      if( isPrimitive ) {
          // Getter Primitive Pointer + Array
          if( maxOneElement ) {
              generateGetterSignature(unit, false, false, fieldName, fieldType, ownership, baseJElemTypeName, capitalFieldName,
                                      null, constElemCount, maxOneElement, elemCountExpr, GetElemValueApiDocTail);
              unit.emitln(" {");
              if( baseIsPointer ) {
                  if( isPointer ) {
                      unit.emit  ("    return PointerBuffer.derefPointer(getBuffer(), "+fieldName+"_offset[mdIdx], 1)");
                  } else {
                      unit.emit  ("    return PointerBuffer.wrap(getBuffer(), "+fieldName+"_offset[mdIdx], 1)");
                  }
              } else {
                  if( isPointer ) {
                      unit.emitln("    return ElementBuffer.derefPointer("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], 1)");
                  } else {
                      unit.emitln("    return ElementBuffer.wrap("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], 1)");
                  }
                  unit.emit  ("             .getByteBuffer()");
                  if( !isByteBuffer ) {
                      unit.emit(".as"+primJElemTypeBufferName+"()");
                  }
              }
              unit.emitln(".get(0);");
              unit.emitln("  }");
              unit.emitln();
          } else {
              boolean doneString = false;
              if( isString && isByteBuffer ) {
                  generateGetterSignature(unit, false, false, fieldName, fieldType, ownership, "String", capitalFieldName+(isStringOnly?"":"AsString"),
                                          null, constElemCount, maxOneElement, elemCountExpr, GetElemValueApiDocTail);
                  unit.emitln(" {");
                  unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                  if( isPointer ) {
                      unit.emitln("    final ByteBuffer bb = ElementBuffer.derefPointer("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], elemCount).getByteBuffer();");
                  } else {
                      unit.emitln("    final ByteBuffer bb = ElementBuffer.wrap("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], elemCount).getByteBuffer();");
                  }
                  unit.emitln("    final byte[] ba = new byte[elemCount];");
                  unit.emitln("    int i = -1;");
                  unit.emitln("    while( ++i < elemCount ) {");
                  unit.emitln("      ba[i] = bb.get(i);");
                  unit.emitln("      if( (byte)0 == ba[i] ) break;");
                  unit.emitln("    }");
                  unit.emitln("    return new String(ba, 0, i, _charset);");
                  unit.emitln("  }");
                  unit.emitln();
                  doneString = true;
              }
              if( doneString && isStringOnly ) {
                  generateArrayFieldNote(unit, "  /** SKIP getter for String alternative (ByteBuffer)", " */", fieldName, fieldType, ownership, constElemCount, maxOneElement, elemCountExpr, false, false);
                  unit.emitln();
              } else if( !baseIsPointer ) {
                  generateGetterSignature(unit, false, false, fieldName, fieldType, ownership, primJElemTypeBufferName, capitalFieldName,
                                          null, constElemCount, maxOneElement, elemCountExpr, GetElemValueApiDocTail);
                  unit.emitln(" {");
                  if( isPointer ) {
                      unit.emitln("    return ElementBuffer.derefPointer("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], "+getElemCountFuncExpr+")");
                  } else {
                      unit.emitln("    return ElementBuffer.wrap("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], "+getElemCountFuncExpr+")");
                  }
                  unit.emit  ("             .getByteBuffer()");
                  if( !isByteBuffer ) {
                      unit.emit(".as"+primJElemTypeBufferName+"()");
                  }
                  unit.emitln(";");
                  unit.emitln("  }");
                  unit.emitln();
              }
              if( !doneString ) {
                  generateGetterSignature(unit, false, false, fieldName, fieldType, ownership, baseJElemTypeName+"[]", capitalFieldName,
                                          "final int srcPos, "+baseJElemTypeName+" dest[], "+GetArrayArgs, constElemCount, maxOneElement, elemCountExpr, GetElemValueApiDocTail);
                  unit.emitln(" {");
                  unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
                  if( baseIsPointer ) {
                      if( isPointer ) {
                          unit.emit  ("    PointerBuffer.derefPointer(getBuffer(), "+fieldName+"_offset[mdIdx], elemCount)");
                      } else {
                          unit.emit  ("    PointerBuffer.wrap(getBuffer(), "+fieldName+"_offset[mdIdx], elemCount)");
                      }
                  } else {
                      if( isPointer ) {
                          unit.emit("    ElementBuffer.derefPointer("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], elemCount)");
                      } else {
                          unit.emit("    ElementBuffer.wrap("+primElemSizeExpr+", getBuffer(), "+fieldName+"_offset[mdIdx], elemCount)");
                      }
                  }
                  unit.emitln(".get(srcPos, dest, destPos, length);");
                  unit.emitln("    return dest;");
                  unit.emitln("  }");
                  unit.emitln();
              }
          }
      } else {
          // Getter Struct Pointer + Array
          if( maxOneElement ) {
              generateGetterSignature(unit, false, false, fieldName, fieldType, ownership, baseJElemTypeName, capitalFieldName,
                                      null, constElemCount, maxOneElement, elemCountExpr, GetElemValueApiDocTail);
              unit.emitln(" {");
              unit.emitln("    return "+baseJElemTypeName+".create(");
              if( isPointer ) {
                  unit.emitln("             ElementBuffer.derefPointer("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], 1).getByteBuffer() );");
              } else {
                  unit.emitln("             ElementBuffer.wrap("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], 1).getByteBuffer() );");
              }
              unit.emitln("  }");
              unit.emitln();
          } else {
              generateGetterSignature(unit, false, false, fieldName, fieldType, ownership, baseJElemTypeName+"[]", capitalFieldName,
                                      "final int srcPos, "+baseJElemTypeName+" dest[], "+GetArrayArgs, constElemCount, maxOneElement, elemCountExpr, GetElemValueApiDocTail);
              unit.emitln(" {");
              unit.emitln(GetArrayArgsCheck);
              unit.emitln("    final int elemCount = "+getElemCountFuncExpr+";");
              unit.emitln("    if( srcPos + length > elemCount ) { throw new IndexOutOfBoundsException(\"srcPos \"+srcPos+\" + length \"+length+\" > elemCount \"+elemCount); };");
              if( isPointer ) {
                  unit.emitln("    final ElementBuffer eb = ElementBuffer.derefPointer("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
              } else {
                  unit.emitln("    final ElementBuffer eb = ElementBuffer.wrap("+baseJElemTypeName+".size(), getBuffer(), "+fieldName+"_offset[mdIdx], elemCount);");
              }
              unit.emitln("    for(int i=0; i<length; ++i) {");
              unit.emitln("      dest[destPos+i] = "+baseJElemTypeName+".create( eb.slice(srcPos+i, 1) );");
              unit.emitln("    }");
              unit.emitln("    return dest;");
              unit.emitln("  }");
              unit.emitln();
          }
      }
  }
  private void emitSetElemCount(final JavaCodeUnit unit, final String setElemCountFunc, final String newElemCountExpr, final boolean mandatory, final String capitalFieldName, final Type structCType, final String indentation) {
      if( null != setElemCountFunc ) {
          unit.emitln(indentation+setElemCountFunc+"( "+newElemCountExpr+" );");
      } else if( mandatory ) {
          final String msg = "Missing set"+capitalFieldName+"ElemCount( "+newElemCountExpr+" )";
          unit.emitln(indentation+"// ERROR: "+msg);
          unit.emitln();
          LOG.log(SEVERE, structCType.getASTLocusTag(), msg);
          throw new RuntimeException(msg);
      }
  }

  private JavaType typeToJavaType(final Type cType, final MachineDataInfo curMachDesc) {
      final JavaType jt = typeToJavaTypeImpl(cType, curMachDesc);
      LOG.log(FINE, cType.getASTLocusTag(), "typeToJavaType: {0} -> {1}", cType, jt);
      return jt;
  }
  private boolean isJNIEnvPointer(final Type cType) {
    final PointerType opt = cType.asPointer();
    return (opt != null) &&
           (opt.getTargetType().getName() != null) &&
           (opt.getTargetType().getName().equals("JNIEnv"));
  }
  private JavaType typeToJavaTypeImpl(final Type cType, final MachineDataInfo curMachDesc) {
    // Recognize JNIEnv* case up front
    if( isJNIEnvPointer(cType) ) {
        return JavaType.createForJNIEnv();
    }
    // Opaque specifications override automatic conversions
    // in case the identity is being used .. not if ptr-ptr
    final TypeInfo info = cfg.typeInfo(cType);
    if (info != null) {
      boolean isPointerPointer = false;
      if (cType.pointerDepth() > 0 || cType.arrayDimension() > 0) {
        // t is `<type>*`, `<type>[]` or `<type>[][]`, we need to get <type>
        final Type targetType = cType.getArrayBaseOrPointerTargetType();
        if (cType.pointerDepth() == 2 || cType.arrayDimension() == 2) {
          // Get the target type of the target type (targetType was computer earlier
          // as to be a pointer to the target type, so now we need to get its
          // target type)
          if (targetType.isPointer()) {
            isPointerPointer = true;
            if( GlueGen.debug() ) {
                // t is<type>**, targetType is <type>*, we need to get <type>
                final Type bottomType = targetType.asPointer().getTargetType();
                LOG.log(INFO, cType.getASTLocusTag(), "Opaque Type: {0}, targetType: {1}, bottomType: {2} is ptr-ptr",
                        cType, targetType, bottomType);
            }
          }
        }
      }
      if( !isPointerPointer ) {
          return info.javaType();
      }
    }

    if (cType.isInt() || cType.isEnum()) {
      switch ((int) cType.getSize(curMachDesc)) {
       case 1:  return javaType(Byte.TYPE);
       case 2:  return javaType(Short.TYPE);
       case 4:  return javaType(Integer.TYPE);
       case 8:  return javaType(Long.TYPE);
       default: throw new GlueGenException("Unknown integer type of size " +
                                           cType.getSize(curMachDesc) + " and name " + cType.getName(),
                                           cType.getASTLocusTag());
      }
    } else if (cType.isFloat()) {
      return javaType(Float.TYPE);
    } else if (cType.isDouble()) {
      return javaType(Double.TYPE);
    } else if (cType.isVoid()) {
      return javaType(Void.TYPE);
    } else if (cType.pointerDepth() > 0 || cType.arrayDimension() > 0) {
        // <type>[][]
        final Type targetType = cType.getArrayBaseOrPointerTargetType();

        // Handle Types of form pointer-to-type or array-of-type, like
        // char* or int[]; these are expanded out into Java primitive
        // arrays, NIO buffers, or both in expandMethodBinding
        if (cType.pointerDepth() == 1 || cType.arrayDimension() == 1) {
          if (targetType.isVoid()) {
            return JavaType.createForCVoidPointer();
          } else if( targetType.isFunctionPointer() ) {
              return javaType(Long.TYPE);
          } else if (targetType.isInt()) {
            final SizeThunk targetSizeThunk = targetType.getSize();
            if( null != targetSizeThunk && SizeThunk.POINTER == targetSizeThunk ) {
              // Map intptr_t*, uintptr_t*, ptrdiff_t* and size_t* to PointerBuffer, since referenced memory-size is arch dependent
              return JavaType.forNIOPointerBufferClass();
            }
            switch ((int) targetType.getSize(curMachDesc)) {
              case 1:  return JavaType.createForCCharPointer();
              case 2:  return JavaType.createForCShortPointer();
              case 4:  return JavaType.createForCInt32Pointer();
              case 8:  return JavaType.createForCInt64Pointer();
              default: throw new GlueGenException("Unknown integer array type of size " +
                                                  cType.getSize(curMachDesc) + " and name " + cType.getName()+", "+cType.getDebugString()+
                                                  ", target "+targetType.getDebugString(),
                                                  cType.getASTLocusTag());
            }
          } else if (targetType.isFloat()) {
            return JavaType.createForCFloatPointer();
          } else if (targetType.isDouble()) {
            return JavaType.createForCDoublePointer();
          } else if (targetType.isCompound()) {
            if (cType.isArray()) { // FIXME: Compound and Compound-Arrays
              return JavaType.createForCArray(targetType);
            }
            // Special cases for known JNI types (in particular for converting jawt.h)
            if (cType.getName() != null &&
                cType.getName().equals("jobject")) {
              return javaType(java.lang.Object.class);
            }
            // NOTE: Struct Name Resolution (JavaEmitter, HeaderParser)
            String name;
            if( !targetType.isTypedef() && cType.isTypedef() ) {
                // If compound is not a typedef _and_ containing pointer is typedef, use the latter.
                name = cType.getName();
            } else {
                // .. otherwise try compound name
                name = targetType.getName();
                if( null == name ) {
                  // .. fall back to pointer type name
                  name = cType.getName();
                  if (name == null) {
                    throw new GlueGenException("Couldn't find a proper type name for pointer type " + cType.getDebugString()+
                                               ", target "+targetType.getDebugString(),
                                                cType.getASTLocusTag());
                  }
                }
            }
            return JavaType.createForCStruct(cfg.renameJavaType(name));
          } else {
            throw new GlueGenException("Don't know how to convert pointer/array type " +
                                       cType.getDebugString() + ", target "+targetType.getDebugString(),
                                       cType.getASTLocusTag());
          }
        }
        // Handle Types of form pointer-to-pointer-to-type or
        // array-of-arrays-of-type, like char** or int[][]
        else if (cType.pointerDepth() == 2 || cType.arrayDimension() == 2) {
          // Get the target type of the target type (targetType was computer earlier
          // as to be a pointer to the target type, so now we need to get its
          // target type)
          Type bottomType;
          if (targetType.isPointer()) {
            // t is<type>**, targetType is <type>*, we need to get <type>
            bottomType = targetType.asPointer().getTargetType();
            if( GlueGen.debug() ) {
                LOG.log(INFO, cType.getASTLocusTag(), "typeToJavaType(ptr-ptr): {0}, targetType: {1}, bottomType: {2}",
                        cType.getDebugString(), targetType.getDebugString(), bottomType.getDebugString());
            }
            return JavaType.forNIOPointerBufferClass();
          } else if(targetType.isArray()) {
            // t is<type>[][], targetType is <type>[], we need to get <type>
            bottomType = targetType.getBaseType();
            if( GlueGen.debug() ) {
                LOG.log(INFO, cType.getASTLocusTag(), "typeToJavaType(ptr-ptr.array): {0}, targetType: {1}, bottomType: {2}",
                        cType.getDebugString(), targetType.getDebugString(), bottomType.getDebugString());
            }
          } else {
            bottomType = targetType;
            if( GlueGen.debug() ) {
                LOG.log(INFO, cType.getASTLocusTag(), "typeToJavaType(ptr-ptr.primitive): {0}, targetType: {1}, bottomType: {2}",
                        cType.getDebugString(), targetType.getDebugString(), bottomType.getDebugString());
            }
          }

          // Warning: The below code is not backed up by an implementation,
          //          the only working variant is a ptr-ptr type which results in a PointerBuffer.
          //
          if (bottomType.isPrimitive()) {
            if (bottomType.isInt()) {
              switch ((int) bottomType.getSize(curMachDesc)) {
                case 1: return javaType(ArrayTypes.byteBufferArrayClass);
                case 2: return javaType(ArrayTypes.shortBufferArrayClass);
                case 4: return javaType(ArrayTypes.intBufferArrayClass);
                case 8: return javaType(ArrayTypes.longBufferArrayClass);
                default: throw new GlueGenException("Unknown two-dimensional integer array type of element size " +
                                                    bottomType.getSize(curMachDesc) + " and name " + bottomType.getName()+", "+bottomType.getDebugString(),
                                                    bottomType.getASTLocusTag());
              }
            } else if (bottomType.isFloat()) {
              return javaType(ArrayTypes.floatBufferArrayClass);
            } else if (bottomType.isDouble()) {
              return javaType(ArrayTypes.doubleBufferArrayClass);
            } else {
              throw new GlueGenException("Unexpected primitive type " + bottomType.getDebugString() +
                                         " in two-dimensional array", bottomType.getASTLocusTag());
            }
          } else if (bottomType.isVoid()) {
            return javaType(ArrayTypes.bufferArrayClass);
          } else if (targetType.isPointer() && (targetType.pointerDepth() == 1) &&
                     targetType.asPointer().getTargetType().isCompound()) {
            // Array of pointers; convert as array of StructAccessors
            return JavaType.createForCArray(bottomType);
          } else {
            throw new GlueGenException(
              "Could not convert C type " + cType.getDebugString() + " " +
              "to appropriate Java type; need to add more support for " +
              "depth=2 pointer/array types [debug info: targetType=" +
              targetType.getDebugString() + "]", cType.getASTLocusTag());
          }
        } else {
          // can't handle this type of pointer/array argument
          throw new GlueGenException(
            "Could not convert C pointer/array " + cType.getDebugString() + " to " +
            "appropriate Java type; types with pointer/array depth " +
            "greater than 2 are not yet supported [debug info: " +
            "pointerDepth=" + cType.pointerDepth() + " arrayDimension=" +
            cType.arrayDimension() + " targetType=" + targetType.getDebugString() + "]",
            cType.getASTLocusTag());
        }

    } else if( cType.isCompound() ) { // FIXME: Compound and Compound-Arrays
        String name = cType.getName();
        if (name == null) {
          name = cType.asCompound().getStructName();
          if (name == null) {
              throw new GlueGenException("Couldn't find a proper type name for pointer type " + cType.getDebugString(),
                                         cType.getASTLocusTag());
          }
        }
        return JavaType.createForCStruct(cfg.renameJavaType(name));
    } else {
        throw new GlueGenException(
          "Could not convert C type " + cType.getDebugString() + " (class " +
          cType.getClass().getName() + ") to appropriate Java type",
          cType.getASTLocusTag());
    }
  }

  private StructLayout getLayout() {
    if (layout == null) {
      layout = StructLayout.create(0);
    }
    return layout;
  }

  /**
   * @param filename the class's full filename to open w/ write access
   * @param cUnitName the base c-unit name, i.e. c-file basename with suffix
   * @param generator informal optional object that is creating this unit, used to be mentioned in a warning message if not null.
   * @throws IOException
   */
  protected CCodeUnit openCUnit(final String filename, final String cUnitName) throws IOException {
    return new CCodeUnit(filename, cUnitName, this);
  }

  /**
   * @param filename the class's full filename to open w/ write access
   * @param packageName the package name of the class
   * @param simpleClassName the simple class name, i.e. w/o package name or c-file basename
   * @param generator informal optional object that is creating this unit, used to be mentioned in a warning message if not null.
   * @throws IOException
   */
  protected JavaCodeUnit openJavaUnit(final String filename, final String packageName, final String simpleClassName) throws IOException {
    return new JavaCodeUnit(filename, packageName, simpleClassName, this);
  }

  private boolean isOpaque(final Type type) {
    return null != cfg.typeInfo(type);
  }

  private String compatiblePrimitiveJavaTypeName(final Type fieldType,
                                                 final MachineDataInfo curMachDesc) {
    if ( !fieldType.isInt() && !fieldType.isPointer() && !fieldType.isArray() ) {
      throw new GlueGenException("Can't yet handle opaque definitions of structs' fields to non-integer types (byte, short, int, long, etc.): type: "+
                                 fieldType, fieldType.getASTLocusTag());
    }
    switch ((int) fieldType.getSize(curMachDesc)) {
      case 1:  return "byte";
      case 2:  return "short";
      case 4:  return "int";
      case 8:  return "long";
      default: throw new GlueGenException("Can't handle opaque definitions if the starting type isn't compatible with integral types, type "+
                                          fieldType.getDebugString(), fieldType.getASTLocusTag());
    }
  }

  private void openCodeUnits() throws IOException {
    String jRoot = null;
    if (cfg.allStatic() || cfg.emitInterface()) {
      jRoot = cfg.javaOutputDir() + File.separator +
        CodeGenUtils.packageAsPath(cfg.packageName());
    }
    String jImplRoot = null;
    if (!cfg.allStatic()) {
      jImplRoot =
        cfg.javaOutputDir() + File.separator +
        CodeGenUtils.packageAsPath(cfg.implPackageName());
    }
    String nRoot = cfg.nativeOutputDir();
    if (cfg.nativeOutputUsesJavaHierarchy())
    {
      nRoot +=
        File.separator + CodeGenUtils.packageAsPath(cfg.packageName());
    }

    if (cfg.allStatic() || cfg.emitInterface()) {
      final String javaFileName = jRoot + File.separator + cfg.className() + ".java";
      javaUnit = openJavaUnit(javaFileName, cfg.packageName(), cfg.className());
    }
    if (!cfg.allStatic() && cfg.emitImpl()) {
      final String javaFileName = jImplRoot + File.separator + cfg.implClassName() + ".java";
      javaImplUnit = openJavaUnit(javaFileName, cfg.implPackageName(), cfg.implClassName());
    }
    if (cfg.emitImpl()) {
      final String cUnitName = cfg.implClassName() + "_JNI.c";
      final String cFileName = nRoot + File.separator + cUnitName;
      cUnit = openCUnit(cFileName, cUnitName);
    }
  }

  protected JavaCodeUnit javaUnit() {
    if (!cfg.allStatic() && !cfg.emitInterface()) {
      throw new InternalError("Should not call this");
    }
    return javaUnit;
  }

  protected JavaCodeUnit javaImplUnit() {
    if (cfg.allStatic() || !cfg.emitImpl()) {
      throw new InternalError("Should not call this");
    }
    return javaImplUnit;
  }

  protected CCodeUnit cUnit() {
    if (!cfg.emitImpl()) {
      throw new InternalError("Should not call this");
    }
    return cUnit;
  }

  private void closeWriters() throws IOException {
    if( javaUnit != null ) {
        javaUnit.close();
        javaUnit = null;
    }
    if( javaImplUnit != null ) {
        javaImplUnit.close();
        javaImplUnit = null;
    }
    if( cUnit != null ) {
        cUnit.close();
        cUnit = null;
    }
  }

  /**
   * Returns the value that was specified by the configuration directive
   * "JavaOutputDir", or the default if none was specified.
   */
  protected String getJavaOutputDir() {
    return cfg.javaOutputDir();
  }

  /**
   * Returns the value that was specified by the configuration directive
   * "Package", or the default if none was specified.
   */
  protected String getJavaPackageName() {
    return cfg.packageName();
  }

  /**
   * Returns the value that was specified by the configuration directive
   * "ImplPackage", or the default if none was specified.
   */
  protected String getImplPackageName() {
    return cfg.implPackageName();
  }

  /**
   * Emit all the strings specified in the "CustomJavaCode" parameters of
   * the configuration file.
   */
  protected void emitCustomJavaCode(final CodeUnit unit, final String className) throws Exception  {
    final List<String> code = cfg.customJavaCodeForClass(className);
    if (code.isEmpty())
      return;

    unit.emitln();
    unit.emitln("  // --- Begin CustomJavaCode .cfg declarations");
    for (final String line : code) {
      unit.emitln(line);
    }
    unit.emitln("  // ---- End CustomJavaCode .cfg declarations");
  }

  /**
   * Emit all the strings specified in the "CustomJNICode" parameters of
   * the configuration file.
   */
  protected void emitCustomJNICode(final CodeUnit unit, final String className) throws Exception  {
    final List<String> code = cfg.customJNICodeForClass(className);
    if (code.isEmpty())
      return;

    unit.emitln();
    unit.emitln("  // --- Begin CustomJNICode .cfg declarations");
    for (final String line : code) {
      unit.emitln(line);
    }
    unit.emitln("  // ---- End CustomJNICode .cfg declarations");
  }

  public String[] getClassAccessModifiers(final String classFQName) {
      String[] accessModifiers;
      final MethodAccess acc = cfg.accessControl(classFQName);
      if( PUBLIC_ABSTRACT == acc ) {
          accessModifiers = new String[] { PUBLIC.getJavaName(), PUBLIC_ABSTRACT.getJavaName() };
      } else if( PACKAGE_PRIVATE == acc ) {
          accessModifiers = new String[] { PACKAGE_PRIVATE.getJavaName() };
      } else if( PRIVATE == acc ) {
          throw new IllegalArgumentException("Class access "+classFQName+" cannot be private");
      } else if( PROTECTED == acc ) {
          accessModifiers = new String[] { PROTECTED.getJavaName() };
      } else { // default PUBLIC
          accessModifiers = new String[] { PUBLIC.getJavaName() };
      }
      return accessModifiers;
  }

  /**
   * Write out any header information for the output files (class declaration
   * and opening brace, import statements, etc).
   */
  protected void emitAllFileHeaders() throws IOException {
    try {
      final List<String> imports = new ArrayList<String>(cfg.imports());
      imports.add(cfg.gluegenRuntimePackage()+".*");
      imports.add(DynamicLookupHelper.class.getPackage().getName()+".*");
      imports.add(Buffers.class.getPackage().getName()+".*");
      imports.add(Buffer.class.getPackage().getName()+".*");
      imports.add("java.nio.charset.Charset");
      imports.add("java.nio.charset.StandardCharsets");

      if (cfg.allStatic() || cfg.emitInterface()) {

        String[] interfaces;
        List<String> userSpecifiedInterfaces = null;
        if (cfg.emitInterface()) {
          userSpecifiedInterfaces = cfg.extendedInterfaces(cfg.className());
        } else {
          userSpecifiedInterfaces = cfg.implementedInterfaces(cfg.className());
        }
        interfaces = new String[userSpecifiedInterfaces.size()];
        userSpecifiedInterfaces.toArray(interfaces);

        final List<String> intfDocs = cfg.javadocForClass(cfg.className());
        final CodeGenUtils.EmissionCallback docEmitter =
          new CodeGenUtils.EmissionCallback() {
            @Override
            public void emit(final PrintWriter w) {
              for (final Iterator<String> iter = intfDocs.iterator(); iter.hasNext(); ) {
                w.println(iter.next());
              }
            }
          };

        final String[] accessModifiers = getClassAccessModifiers(cfg.className());
        CodeGenUtils.emitJavaHeaders(
          javaUnit().output,
          cfg.packageName(),
          cfg.className(),
          cfg.allStatic() ? true : false,
          imports,
          accessModifiers,
          interfaces,
          cfg.extendedParentClass(cfg.className()),
          docEmitter);
      }

      if (!cfg.allStatic() && cfg.emitImpl()) {
        final List<String> implDocs = cfg.javadocForClass(cfg.implClassName());
        final CodeGenUtils.EmissionCallback docEmitter =
          new CodeGenUtils.EmissionCallback() {
            @Override
            public void emit(final PrintWriter w) {
              for (final Iterator<String> iter = implDocs.iterator(); iter.hasNext(); ) {
                w.println(iter.next());
              }
            }
          };

        String[] interfaces;
        List<String> userSpecifiedInterfaces = null;
        userSpecifiedInterfaces = cfg.implementedInterfaces(cfg.implClassName());
        int additionalNum = 0;
        if (cfg.className() != null) {
          additionalNum = 1;
        }
        interfaces = new String[additionalNum + userSpecifiedInterfaces.size()];
        userSpecifiedInterfaces.toArray(interfaces);
        if (additionalNum == 1) {
          interfaces[userSpecifiedInterfaces.size()] = cfg.className();
        }

        final String[] accessModifiers = getClassAccessModifiers(cfg.implClassName());
        CodeGenUtils.emitJavaHeaders(
          javaImplUnit().output,
          cfg.implPackageName(),
          cfg.implClassName(),
          true,
          imports,
          accessModifiers,
          interfaces,
          cfg.extendedParentClass(cfg.implClassName()),
          docEmitter);
      }

      if (cfg.emitImpl()) {
        cUnit().emitHeader(getImplPackageName(), cfg.implClassName(), cfg.customCCode());
      }
    } catch (final Exception e) {
      throw new RuntimeException(
        "Error emitting all file headers: cfg.allStatic()=" + cfg.allStatic() +
        " cfg.emitImpl()=" + cfg.emitImpl() + " cfg.emitInterface()=" + cfg.emitInterface(),
        e);
    }

  }

  /**
   * Write out any footer information for the output files (closing brace of
   * class definition, etc).
   */
  protected void emitAllFileFooters() {
    if (cfg.allStatic() || cfg.emitInterface()) {
      javaUnit.emitTailCode();
      javaUnit().emitln("} // end of class " + cfg.className());
    }
    if (!cfg.allStatic() && cfg.emitImpl())  {
      javaImplUnit.emitTailCode();
      javaImplUnit().emitln("} // end of class " + cfg.implClassName());
    }
  }

  private JavaType javaType(final Class<?> c) {
    return JavaType.createForClass(c);
  }

  /** Maps the C types in the specified function to Java types through
      the MethodBinding interface. Note that the JavaTypes in the
      returned MethodBinding are "intermediate" JavaTypes (some
      potentially representing C pointers rather than true Java types)
      and must be lowered to concrete Java types before creating
      emitters for them. */
  private MethodBinding bindFunction(FunctionSymbol sym,
                                     final boolean forInterface,
                                     final MachineDataInfo curMachDesc,
                                     final JavaType containingType, final Type containingCType) {

    final String delegationImplName = null == containingType && null == containingCType ?
                                      cfg.getDelegatedImplementation(sym) : null;
    if( !forInterface && null != delegationImplName ) {
        // We need to reflect the 'delegationImplName' for implementations
        // to allow all subsequent type/cfg checks to hit on AliasedSymbol!
        sym = FunctionSymbol.cloneWithDeepAliases(sym);
        sym.addAliasedName(delegationImplName);
    }
    final String name = sym.getName();
    final JavaType javaReturnType;

    if (cfg.returnsString(sym)) {
      final PointerType prt = sym.getReturnType().asPointer();
      if (prt == null ||
          prt.getTargetType().asInt() == null ||
          prt.getTargetType().getSize(curMachDesc) != 1) {
        throw new GlueGenException(
          "Cannot apply ReturnsString configuration directive to \"" + sym +
          "\". ReturnsString requires native method to have return type \"char *\"",
          sym.getASTLocusTag());
      }
      javaReturnType = javaType(java.lang.String.class);
    } else {
      final JavaType r = cfg.getOpaqueReturnType(sym);
      if( null != r ) {
          javaReturnType = r;
      } else {
          javaReturnType = typeToJavaType(sym.getReturnType(), curMachDesc);
      }
    }

    // List of the indices of the arguments in this function that should be
    // converted from byte[] or short[] to String
    final List<JavaType> javaArgumentTypes = new ArrayList<JavaType>();
    final List<Integer> stringArgIndices = cfg.stringArguments(name);

    for (int i = 0; i < sym.getNumArguments(); i++) {
      final Type cArgType = sym.getArgumentType(i);
      JavaType mappedType = typeToJavaType(cArgType, curMachDesc);
      // System.out.println("C arg type -> \"" + cArgType + "\"" );
      // System.out.println("      Java -> \"" + mappedType + "\"" );

      // Take into account any ArgumentIsString configuration directives that apply
      if (stringArgIndices != null && stringArgIndices.contains(i)) {
        // System.out.println("Forcing conversion of " + binding.getName() + " arg #" + i + " from byte[] to String ");
        if (mappedType.isCVoidPointerType() ||
            mappedType.isCCharPointerType() ||
            mappedType.isCShortPointerType() ||
            mappedType.isNIOPointerBuffer() ||
            (mappedType.isArray() &&
             (mappedType.getJavaClass() == ArrayTypes.byteBufferArrayClass) ||
             (mappedType.getJavaClass() == ArrayTypes.shortBufferArrayClass))) {
          // convert mapped type from:
          //   void*, byte[], and short[] to String
          //   ByteBuffer[] and ShortBuffer[] to String[]
          if (mappedType.isArray() || mappedType.isNIOPointerBuffer()) {
            mappedType = javaType(ArrayTypes.stringArrayClass);
          } else {
            mappedType = javaType(String.class);
          }
        }
        else {
        throw new GlueGenException(
          "Cannot apply ArgumentIsString configuration directive to " +
          "argument " + i + " of \"" + sym + "\": argument type is not " +
          "a \"void*\", \"char *\", \"short *\", \"char**\", or \"short**\" equivalent",
          sym.getASTLocusTag());
        }
      }
      javaArgumentTypes.add(mappedType);
      //System.out.println("During binding of [" + sym + "], added mapping from C type: " + cArgType + " to Java type: " + mappedType);
    }
    final MethodBinding mb = new MethodBinding(sym, delegationImplName,
                                               javaReturnType, javaArgumentTypes,
                                               containingType, containingCType);
    mangleBinding(mb);
    return mb;
  }

  private MethodBinding lowerMethodBindingPointerTypes(final MethodBinding inputBinding,
                                                       final boolean convertToArrays,
                                                       final boolean[] canProduceArrayVariant) {
    MethodBinding result = inputBinding;
    boolean arrayPossible = false;

    // System.out.println("lowerMethodBindingPointerTypes(0): "+result);

    for (int i = 0; i < inputBinding.getNumArguments(); i++) {
      final JavaType t = inputBinding.getJavaArgumentType(i);
      if (t.isCPrimitivePointerType()) {
        if (t.isCVoidPointerType()) {
          // These are always bound to java.nio.Buffer
          result = result.replaceJavaArgumentType(i, JavaType.forNIOBufferClass());
        } else if (t.isCCharPointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.byteArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOByteBufferClass());
          }
        } else if (t.isCShortPointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.shortArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOShortBufferClass());
          }
        } else if (t.isCInt32PointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.intArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOIntBufferClass());
          }
        } else if (t.isCInt64PointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.longArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOLongBufferClass());
          }
        } else if (t.isCFloatPointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.floatArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIOFloatBufferClass());
          }
        } else if (t.isCDoublePointerType()) {
          arrayPossible = true;
          if (convertToArrays) {
            result = result.replaceJavaArgumentType(i, javaType(ArrayTypes.doubleArrayClass));
          } else {
            result = result.replaceJavaArgumentType(i, JavaType.forNIODoubleBufferClass());
          }
        } else {
          throw new GlueGenException("Unknown C pointer type " + t);
        }
      }
    }

    // System.out.println("lowerMethodBindingPointerTypes(1): "+result);

    // Always return primitive pointer types as NIO buffers
    final JavaType t = result.getJavaReturnType();
    if (t.isCPrimitivePointerType()) {
      if (t.isCVoidPointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOByteBufferClass());
      } else if (t.isCCharPointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOByteBufferClass());
      } else if (t.isCShortPointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOShortBufferClass());
      } else if (t.isCInt32PointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOIntBufferClass());
      } else if (t.isCInt64PointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOLongBufferClass());
      } else if (t.isCFloatPointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIOFloatBufferClass());
      } else if (t.isCDoublePointerType()) {
        result = result.replaceJavaArgumentType(-1, JavaType.forNIODoubleBufferClass());
      } else {
        throw new GlueGenException("Unknown C pointer type " + t, result.getCReturnType().getASTLocusTag());
      }
    }

    // System.out.println("lowerMethodBindingPointerTypes(2): "+result);

    if (canProduceArrayVariant != null) {
      canProduceArrayVariant[0] = arrayPossible;
    }

    return result;
  }

  /**
   * Allow specializations to modify the given {@link MethodBinding}
   * before {@link #expandMethodBinding(MethodBinding) expanding} and emission.
   */
  protected void mangleBinding(final MethodBinding binding) {
      // NOP
  }

  // Expands a MethodBinding containing C primitive pointer types into
  // multiple variants taking Java primitive arrays and NIO buffers, subject
  // to the per-function "NIO only" rule in the configuration file
  protected List<MethodBinding> expandMethodBinding(final MethodBinding binding) {

    final List<MethodBinding> result = new ArrayList<MethodBinding>();
    // Indicates whether it is possible to produce an array variant
    // Prevents e.g. char* -> String conversions from emitting two entry points
    final boolean[] canProduceArrayVariant = new boolean[1];

    if (binding.signatureUsesCPrimitivePointers() ||
        binding.signatureUsesCVoidPointers() ||
        binding.signatureUsesCArrays()) {

      result.add(lowerMethodBindingPointerTypes(binding, false, canProduceArrayVariant));

      // FIXME: should add new configuration flag for this
      if (canProduceArrayVariant[0] && (binding.signatureUsesCPrimitivePointers() || binding.signatureUsesCArrays()) &&
          !cfg.useNIOOnly(binding.getName()) ) {
        result.add(lowerMethodBindingPointerTypes(binding, true, null));
      }
    } else {
      result.add(binding);
    }

    return result;
  }

  private Type canonicalize(final Type t) {
    final Type res = canonMap.get(t);
    if (res != null) {
        return res;
    } else {
        canonMap.put(t, t);
        return t;
    }
  }

  /**
   * Converts first letter to upper case.
   */
  private final String capitalizeString(final String string) {
      return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }
  /**
   * Converts first letter to lower case.
   */
  private final String decapitalizeString(final String string) {
      return Character.toLowerCase(string.charAt(0)) + string.substring(1);
  }

  private static final String optStringCharsetCode =
          "  private static Charset _charset = StandardCharsets.UTF_8;\n" +
          "\n"+
          "  /** Returns the Charset for this class's String mapping, default is StandardCharsets.UTF_8. */\n"+
          "  public static Charset getCharset() { return _charset; };\n"+
          "\n"+
          "  /** Sets the Charset for this class's String mapping, default is StandardCharsets.UTF_8. */\n"+
          "  public static void setCharset(Charset cs) { _charset = cs; }\n";

  private static final String optStringMaxStrnlenCode =
          "  private static int _max_strnlen = 8192;\n"+
          "\n"+
          "  /** Returns the maximum number of bytes to read to determine native string length using `strnlen(..)`, default is 8192. */\n"+
          "  public static int getMaxStrnlen() { return _max_strnlen; };\n"+
          "\n"+
          "  /** Sets the maximum number of bytes to read to determine native string length using `strnlen(..)`, default is 8192. */\n"+
          "  public static void setMaxStrnlen(int v) { _max_strnlen = v; }\n";
}
