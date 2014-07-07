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

package com.jogamp.gluegen;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.MachineDescription;

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

import com.jogamp.gluegen.cgram.types.*;

import java.nio.Buffer;
import java.util.logging.Logger;

import jogamp.common.os.MachineDescriptionRuntime;
import static java.util.logging.Level.*;
import static com.jogamp.gluegen.JavaEmitter.MethodAccess.*;

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
  private TypeDictionary typedefDictionary;
  private Map<Type, Type> canonMap;
  protected JavaConfiguration cfg;
  private boolean requiresStaticInitialization = false;

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

  private PrintWriter javaWriter; // Emits either interface or, in AllStatic mode, everything
  private PrintWriter javaImplWriter; // Only used in non-AllStatic modes for impl class
  private PrintWriter cWriter;
  private final MachineDescription machDescJava = MachineDescription.StaticConfig.X86_64_UNIX.md;
  private final MachineDescription.StaticConfig[] machDescTargetConfigs = MachineDescription.StaticConfig.values();

  protected final static Logger LOG = Logger.getLogger(JavaEmitter.class.getPackage().getName());

  @Override
  public void readConfigurationFile(final String filename) throws Exception {
    cfg = createConfig();
    cfg.read(filename);
  }

  class ConstantRenamer implements SymbolFilter {

    private List<ConstantDefinition> constants;

    @Override
    public void filterSymbols(final List<ConstantDefinition> constants, final List<FunctionSymbol> functions) {
      this.constants = constants;
      doWork();
    }

    @Override
    public List<ConstantDefinition> getConstants() {
      return constants;
    }

    @Override
    public List<FunctionSymbol> getFunctions() {
      return null;
    }

    private void doWork() {
      final List<ConstantDefinition> newConstants = new ArrayList<ConstantDefinition>();
      final JavaConfiguration cfg = getConfig();
      for (final ConstantDefinition def : constants) {
        def.rename(cfg.getJavaSymbolRename(def.getName()));
        newConstants.add(def);
      }
      constants = newConstants;
    }
  }

    @Override
    public void beginEmission(final GlueEmitterControls controls) throws IOException {

        // Request emission of any structs requested
        for (final String structs : cfg.forcedStructs()) {
            controls.forceStructEmission(structs);
        }

        if (!cfg.structsOnly()) {
            try {
                openWriters();
            } catch (final Exception e) {
                throw new RuntimeException("Unable to open files for writing", e);
            }
            emitAllFileHeaders();

            // Handle renaming of constants
            controls.runSymbolFilter(new ConstantRenamer());
        }
    }

    @Override
    public void endEmission() {
        if (!cfg.structsOnly()) {
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
    if ((cfg.allStatic() || cfg.emitInterface()) && !cfg.structsOnly()) {
      javaWriter().println();
    }
  }

  protected static int getJavaRadix(final String name, final String value)  {
    // FIXME: need to handle when type specifier is in last char (e.g.,
    // "1.0d or 2759L", because parseXXX() methods don't allow the type
    // specifier character in the string.
    //
    //char lastChar = value.charAt(value.length()-1);

    try {
      // see if it's a long or int
      int radix;
      String parseValue;
      // FIXME: are you allowed to specify hex/octal constants with
      // negation, e.g. "-0xFF" or "-056"? If so, need to modify the
      // following "if(..)" checks and parseValue computation
      if (value.startsWith("0x") || value.startsWith("0X")) {
        radix = 16;
        parseValue = value.substring(2);
      }
      else if (value.startsWith("0") && value.length() > 1) {
        // TODO: is "0" the prefix in C to indicate octal???
        radix = 8;
        parseValue = value.substring(1);
      }
      else {
        radix = 10;
        parseValue = value;
      }
      //System.err.println("parsing " + value + " as long w/ radix " + radix);
      Long.parseLong(parseValue, radix);
      return radix;
    } catch (final NumberFormatException e) {
      try {
        // see if it's a double or float
        Double.parseDouble(value);
        return 10;
      } catch (final NumberFormatException e2) {
        throw new RuntimeException(
          "Cannot emit define \""+name+"\": value \""+value+
          "\" cannot be assigned to a int, long, float, or double", e2);
      }
    }
  }

  protected static Object getJavaValue(final String name, final String value) {

    // "calculates" the result type of a simple expression
    // example: (2+3)-(2.0f-3.0) -> Double
    // example: (1 << 2) -> Integer

    final Scanner scanner = new Scanner(value).useDelimiter("[+-/*/></(/)]");

    Object resultType = null;

    while (scanner.hasNext()) {

        final String t = scanner.next().trim();

        if(0<t.length()) {
            final Object type = getJavaValue2(name, t);

            //fast path
            if(type instanceof Double)
                return type;

            if(resultType != null) {

                if(resultType instanceof Integer) {
                    if(type instanceof Long || type instanceof Float || type instanceof Double)
                        resultType = type;
                }else if(resultType instanceof Long) {
                    if(type instanceof Float || type instanceof Double)
                        resultType = type;
                }else if(resultType instanceof Float) {
                    if(type instanceof Float)
                        resultType = type;
                }
            }else{
                resultType = type;
            }

            //fast path
            if(resultType instanceof Double)
                return type;
        }
    }

    return resultType;
  }

  private static Object getJavaValue2(final String name, final String value) {
    // FIXME: need to handle when type specifier is in last char (e.g.,
    // "1.0d or 2759L", because parseXXX() methods don't allow the type
    // specifier character in the string.
    //
    final char lastChar = value.charAt(value.length()-1);

    try {
      // see if it's a long or int
      int radix;
      String parseValue;
      // FIXME: are you allowed to specify hex/octal constants with
      // negation, e.g. "-0xFF" or "-056"? If so, need to modify the
      // following "if(..)" checks and parseValue computation
      if (value.startsWith("0x") || value.startsWith("0X")) {
        radix = 16;
        parseValue = value.substring(2);
      } else if (value.startsWith("0") && value.length() > 1) {
        // TODO: is "0" the prefix in C to indicate octal???
        radix = 8;
        parseValue = value.substring(1);
      } else {
        radix = 10;
        parseValue = value;
      }
      if(lastChar == 'u' || lastChar == 'U') {
          parseValue = parseValue.substring(0, parseValue.length()-1);
      }

      //System.err.println("parsing " + value + " as long w/ radix " + radix);
      final long longVal = Long.parseLong(parseValue, radix);
      // if constant is small enough, store it as an int instead of a long
      if (longVal > Integer.MIN_VALUE && longVal < Integer.MAX_VALUE) {
        return (int)longVal;
      }
      return longVal;

    } catch (final NumberFormatException e) {
      try {
        // see if it's a double or float
        final double dVal = Double.parseDouble(value);
        final double absVal = Math.abs(dVal);
        // if constant is small enough, store it as a float instead of a double
        if (absVal < Float.MIN_VALUE || absVal > Float.MAX_VALUE) {
            return new Double(dVal);
        }
        return new Float((float) dVal);
      } catch (final NumberFormatException e2) {
        throw new RuntimeException(
          "Cannot emit define \""+name+"\": value \""+value+
          "\" cannot be assigned to a int, long, float, or double", e2);
      }
    }
  }


  protected static String getJavaType(final String name, final String value) {
    final Object oval = getJavaValue(name, value);
    return getJavaType(name, oval);
  }

  protected static String getJavaType(final String name, final Object oval) {
    if(oval instanceof Integer) {
        return "int";
    } else if(oval instanceof Long) {
        return "long";
    } else if(oval instanceof Float) {
        return "float";
    } else if(oval instanceof Double) {
        return "double";
    }

    throw new RuntimeException(
      "Cannot emit define (2) \""+name+"\": value \""+oval+
      "\" cannot be assigned to a int, long, float, or double");
  }

  /** Mangle a class, package or function name for JNI usage, i.e. replace all '.' w/ '_' */
  protected static String jniMangle(final String name) {
    return name.replaceAll("_", "_1").replace('.', '_');
  }
  /** Returns the JNI method prefix consisting our of mangled package- and class-name */
  protected static String getJNIMethodNamePrefix(final String javaPackageName, final String javaClassName) {
      return "Java_"+jniMangle(javaPackageName)+"_"+jniMangle(javaClassName);
  }

  @Override
  public void emitDefine(final ConstantDefinition def, final String optionalComment) throws Exception  {

    if (cfg.allStatic() || cfg.emitInterface()) {
      // TODO: Some defines (e.g., GL_DOUBLE_EXT in gl.h) are defined in terms
      // of other defines -- should we emit them as references to the original
      // define (not even sure if the lexer supports this)? Right now they're
      // emitted as the numeric value of the original definition. If we decide
      // emit them as references we'll also have to emit them in the correct
      // order. It's probably not an issue right now because the emitter
      // currently only emits only numeric defines -- if it handled #define'd
      // objects it would make a bigger difference.

      final String name = def.getName();
      String value = def.getValue();

      if (!cfg.shouldIgnoreInInterface(name)) {
        final String type = getJavaType(name, value);
        if (optionalComment != null && optionalComment.length() != 0) {
          javaWriter().println("  /** " + optionalComment + " */");
        }
        String suffix = "";
        if(!value.endsWith(")")) {
            if (type.equals("float") && !value.endsWith("f")) {
                suffix = "f";
            }else if(value.endsWith("u") || value.endsWith("U")) {
                value = value.substring(0, value.length()-1);
            }
        }

        javaWriter().println("  public static final " + type + " " + name + " = " + value + suffix + ";");
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

    this.typedefDictionary = typedefDictionary;
    this.canonMap          = canonMap;
    this.requiresStaticInitialization = false; // reset

    if ((cfg.allStatic() || cfg.emitInterface()) && !cfg.structsOnly()) {
      javaWriter().println();
    }
  }

  @Override
  public Iterator<FunctionSymbol> emitFunctions(final List<FunctionSymbol> originalCFunctions) throws Exception {

    // Sometimes headers will have the same function prototype twice, once
    // with the argument names and once without. We'll remember the signatures
    // we've already processed we don't generate duplicate bindings.
    //
    // Note: this code assumes that on the equals() method in FunctionSymbol
    // only considers function name and argument types (i.e., it does not
    // consider argument *names*) when comparing FunctionSymbols for equality
    final Set<FunctionSymbol> funcsToBindSet = new HashSet<FunctionSymbol>(100);
    for (final FunctionSymbol cFunc : originalCFunctions) {
      if (!funcsToBindSet.contains(cFunc)) {
        funcsToBindSet.add(cFunc);
      }
    }

    //    validateFunctionsToBind(funcsToBindSet);

    final ArrayList<FunctionSymbol> funcsToBind = new ArrayList<FunctionSymbol>(funcsToBindSet);
    // sort functions to make them easier to find in native code
    Collections.sort(funcsToBind, new Comparator<FunctionSymbol>() {
            @Override
            public int compare(final FunctionSymbol o1, final FunctionSymbol o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

    // Bind all the C funcs to Java methods
    final HashSet<MethodBinding> methodBindingSet = new HashSet<MethodBinding>();
    final ArrayList<FunctionEmitter> methodBindingEmitters = new ArrayList<FunctionEmitter>(2*funcsToBind.size());
    for (final FunctionSymbol cFunc : funcsToBind) {
      // Check to see whether this function should be ignored
      if (!cfg.shouldIgnoreInImpl(cFunc.getName())) {
          methodBindingEmitters.addAll(generateMethodBindingEmitters(methodBindingSet, cFunc));
      }

    }

    // Emit all the methods
    for (final FunctionEmitter emitter : methodBindingEmitters) {
      try {
        if (!emitter.isInterface() || !cfg.shouldIgnoreInInterface(emitter.getName())) {
            emitter.emit();
            emitter.getDefaultOutput().println(); // put newline after method body
        }
      } catch (final Exception e) {
        throw new RuntimeException(
            "Error while emitting binding for \"" + emitter.getName() + "\"", e);
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
   * Returns <code>true</code> if implementation (java and native-code)
   * requires {@link #staticClassInitCodeCCode} and {@link #staticClassInitCallJavaCode}
   * and have <code>initializeImpl()</code> being called at static class initialization.
   * <p>
   * This is currently true, if one of the following method returns <code>true</code>
   * <ul>
   *   <li>{@link MethodBinding#signatureRequiresStaticInitialization() one of the binding's signature requires it}</li>
   *   <li>{@link JavaConfiguration#forceStaticInitCode(String)}</li>
   * </ul>
   * </p>
   */
  protected final boolean requiresStaticInitialization(final String clazzName) {
      return requiresStaticInitialization || cfg.forceStaticInitCode(clazzName);
  }

  /**
   * Generates the public emitters for this MethodBinding which will
   * produce either simply signatures (for the interface class, if
   * any) or function definitions with or without a body (depending on
   * whether or not the implementing function can go directly to
   * native code because it doesn't need any processing of the
   * outgoing arguments).
   */
  protected void generatePublicEmitters(final MethodBinding binding, final List<FunctionEmitter> allEmitters, final boolean signatureOnly) {
      if (cfg.manuallyImplement(binding.getName()) && !signatureOnly) {
          // We only generate signatures for manually-implemented methods;
          // user provides the implementation
          return;
      }

      final MethodAccess accessControl = cfg.accessControl(binding.getName());
      // We should not emit anything except public APIs into interfaces
      if (signatureOnly && (accessControl != PUBLIC)) {
          return;
      }

      final PrintWriter writer = ((signatureOnly || cfg.allStatic()) ? javaWriter() : javaImplWriter());

      // It's possible we may not need a body even if signatureOnly is
      // set to false; for example, if the routine doesn't take any
      // arrays or buffers as arguments
      final boolean isUnimplemented = cfg.isUnimplemented(binding.getName());
      final List<String> prologue = cfg.javaPrologueForMethod(binding, false, false);
      final List<String> epilogue = cfg.javaEpilogueForMethod(binding, false, false);
      final boolean needsBody = isUnimplemented ||
                                binding.needsNIOWrappingOrUnwrapping() ||
                                binding.signatureUsesJavaPrimitiveArrays() ||
                                null != prologue  ||
                                null != epilogue;

      if( !requiresStaticInitialization ) {
          requiresStaticInitialization = binding.signatureRequiresStaticInitialization();
          if( requiresStaticInitialization ) {
              LOG.log(INFO, "StaticInit Trigger.1 \"{0}\"", binding);
          }
      }

      final JavaMethodBindingEmitter emitter =
              new JavaMethodBindingEmitter(binding,
                      writer,
                      cfg.runtimeExceptionType(),
                      cfg.unsupportedExceptionType(),
                      !signatureOnly && needsBody,
                      cfg.tagNativeBinding(),
                      false, // eraseBufferAndArrayTypes
                      cfg.useNIOOnly(binding.getName()),
                      cfg.useNIODirectOnly(binding.getName()),
                      false,
                      false,
                      false,
                      isUnimplemented,
                      signatureOnly,
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
      if (!isUnimplemented && !needsBody && !signatureOnly) {
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
      if (cfg.manuallyImplement(binding.getName())) {
          // Don't produce emitters for the implementation class
          return;
      }

      final boolean hasPrologueOrEpilogue =
              cfg.javaPrologueForMethod(binding, false, false) != null ||
              cfg.javaEpilogueForMethod(binding, false, false) != null ;

      if ( !cfg.isUnimplemented( binding.getName() ) ) {
          if( !requiresStaticInitialization ) {
              requiresStaticInitialization = binding.signatureRequiresStaticInitialization();
              if( requiresStaticInitialization ) {
                  LOG.log(INFO, "StaticInit Trigger.2 \"{0}\"", binding);
              }
          }

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
              final PrintWriter writer = (cfg.allStatic() ? javaWriter() : javaImplWriter());

              // (Always) emit the entry point taking only direct buffers
              final JavaMethodBindingEmitter emitter =
                      new JavaMethodBindingEmitter(binding,
                              writer,
                              cfg.runtimeExceptionType(),
                              cfg.unsupportedExceptionType(),
                              false,
                              cfg.tagNativeBinding(),
                              true, // eraseBufferAndArrayTypes
                              cfg.useNIOOnly(binding.getName()),
                              cfg.useNIODirectOnly(binding.getName()),
                              true,
                              true,
                              false,
                              false,
                              false,
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
                              cWriter(),
                              cfg.implPackageName(),
                              cfg.implClassName(),
                              true, // NOTE: we always disambiguate with a suffix now, so this is optional
                              cfg.allStatic(),
                              (binding.needsNIOWrappingOrUnwrapping() || hasPrologueOrEpilogue),
                              !cfg.useNIODirectOnly(binding.getName()),
                              machDescJava);
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
  protected List<? extends FunctionEmitter> generateMethodBindingEmitters(final Set<MethodBinding> methodBindingSet, final FunctionSymbol sym) throws Exception {

    final ArrayList<FunctionEmitter> allEmitters = new ArrayList<FunctionEmitter>();

    try {
      // Get Java binding for the function
      final MethodBinding mb = bindFunction(sym, null, null, machDescJava);

      // JavaTypes representing C pointers in the initial
      // MethodBinding have not been lowered yet to concrete types
      final List<MethodBinding> bindings = expandMethodBinding(mb);

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

        if (cfg.emitInterface()) {
          generatePublicEmitters(binding, allEmitters, true);
        }
        if (cfg.emitImpl()) {
          generatePublicEmitters(binding, allEmitters, false);
          generatePrivateEmitters(binding, allEmitters);
        }
      } // end iteration over expanded bindings
    } catch (final Exception e) {
      throw new RuntimeException("Error while generating bindings for \"" + sym + "\"", e);
    }

    return allEmitters;
  }


  @Override
  public void endFunctions() throws Exception {
    if (!cfg.structsOnly()) {
        if (cfg.allStatic() || cfg.emitInterface()) {
            emitCustomJavaCode(javaWriter(), cfg.className());
        }
        if (!cfg.allStatic() && cfg.emitImpl()) {
            emitCustomJavaCode(javaImplWriter(), cfg.implClassName());
        }
        if ( cfg.allStatic() ) {
            emitJavaInitCode(javaWriter(), cfg.className());
        } else if ( cfg.emitImpl() ) {
            emitJavaInitCode(javaImplWriter(), cfg.implClassName());
        }
        if ( cfg.emitImpl() ) {
            emitCInitCode(cWriter(), getImplPackageName(), cfg.implClassName());
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
    this.typedefDictionary = typedefDictionary;
    this.canonMap          = canonMap;
  }

  @Override
  public void emitStruct(final CompoundType structCType, final String alternateName) throws Exception {
    final String structCTypeName;
    {
        String _name = structCType.getName();
        if (_name == null && alternateName != null) {
          _name = alternateName;
        }
        structCTypeName = _name;
    }

    if (structCTypeName == null) {
        final String structName = structCType.getStructName();
        if ( null != structName && cfg.shouldIgnoreInInterface(structName) ) {
            return;
        }
        LOG.log(WARNING, "skipping emission of unnamed struct \"{0}\"", structCType);
        return;
    }

    if (cfg.shouldIgnoreInInterface(structCTypeName)) {
      return;
    }

    final Type containingCType = canonicalize(new PointerType(SizeThunk.POINTER, structCType, 0));
    final JavaType containingJType = typeToJavaType(containingCType, null);
    if (!containingJType.isCompoundTypeWrapper()) {
      return;
    }
    final String containingJTypeName = containingJType.getName();

    this.requiresStaticInitialization = false; // reset

    // machDescJava global MachineDescription is the one used to determine
    // the sizes of the primitive types seen in the public API in Java.
    // For example, if a C long is an element of a struct, it is the size
    // of a Java int on a 32-bit machine but the size of a Java long
    // on a 64-bit machine. To support both of these sizes with the
    // same API, the abstract base class must take and return a Java
    // long from the setter and getter for this field. However the
    // implementation on a 32-bit platform must downcast this to an
    // int and set only an int's worth of data in the struct.
    //
    // The machDescTarget MachineDescription is the one used to determine how
    // much data to set in or get from the struct and exactly from
    // where it comes.
    //
    // Note that machDescJava MachineDescription is always 64bit unix,
    // which complies w/ Java types.

    boolean needsNativeCode = false;

    // Native code for calls through function pointers gets emitted
    // into the abstract base class; Java code which accesses fields
    // gets emitted into the concrete classes
    for (int i = 0; i < structCType.getNumFields(); i++) {
      final Field field = structCType.getField(i);
      final Type fieldType = field.getType();

      final String cfgFieldName0 = JavaConfiguration.canonicalStructFieldSymbol(structCTypeName, field.getName());

      if (!cfg.shouldIgnoreInInterface(cfgFieldName0)) {

        final String renamed = cfg.getJavaSymbolRename(cfgFieldName0);
        final String fieldName = renamed==null ? field.getName() : renamed;
        final String cfgFieldName1 = JavaConfiguration.canonicalStructFieldSymbol(structCTypeName, fieldName);

        if ( fieldType.isFunctionPointer() || fieldType.isPointer() || requiresGetCStringLength(fieldType, cfgFieldName1) ) {
            needsNativeCode = true;
            break;
        }
      }
    }

    final String structClassPkgName = cfg.packageForStruct(structCTypeName);
    final PrintWriter javaWriter;
    final PrintWriter jniWriter;
    try  {
        javaWriter = openFile(cfg.javaOutputDir() + File.separator +
                              CodeGenUtils.packageAsPath(structClassPkgName) +
                              File.separator + containingJTypeName + ".java", containingJTypeName);
        if( null == javaWriter ) {
            // suppress output if openFile deliberately returns null.
            return;
        }
        CodeGenUtils.emitAutogeneratedWarning(javaWriter, this);
        if (needsNativeCode) {
            String nRoot = cfg.nativeOutputDir();
            if (cfg.nativeOutputUsesJavaHierarchy()) {
                nRoot += File.separator + CodeGenUtils.packageAsPath(cfg.packageName());
            }
            jniWriter = openFile(nRoot + File.separator + containingJTypeName + "_JNI.c", containingJTypeName);
            CodeGenUtils.emitAutogeneratedWarning(jniWriter, this);
            emitCHeader(jniWriter, structClassPkgName, containingJTypeName);
        } else {
            jniWriter = null;
        }
    } catch(final Exception e)   {
        throw new RuntimeException("Unable to open files for emission of struct class", e);
    }

    javaWriter.println();
    javaWriter.println("package " + structClassPkgName + ";");
    javaWriter.println();
    javaWriter.println("import java.nio.*;");
    javaWriter.println();

    javaWriter.println("import " + cfg.gluegenRuntimePackage() + ".*;");
    javaWriter.println("import " + DynamicLookupHelper.class.getPackage().getName() + ".*;");
    javaWriter.println("import " + Buffers.class.getPackage().getName() + ".*;");
    javaWriter.println("import " + MachineDescriptionRuntime.class.getName() + ";");
    javaWriter.println();
    final List<String> imports = cfg.imports();
    for (final String str : imports) {
      javaWriter.print("import ");
      javaWriter.print(str);
      javaWriter.println(";");
    }
    javaWriter.println();
    final List<String> javadoc = cfg.javadocForClass(containingJTypeName);
    for (final String doc : javadoc) {
      javaWriter.println(doc);
    }
    javaWriter.print("public class " + containingJTypeName + " ");
    boolean firstIteration = true;
    final List<String> userSpecifiedInterfaces = cfg.implementedInterfaces(containingJTypeName);
    for (final String userInterface : userSpecifiedInterfaces) {
      if (firstIteration) {
        javaWriter.print("implements ");
      }
      firstIteration = false;
      javaWriter.print(userInterface);
      javaWriter.print(" ");
    }
    javaWriter.println("{");
    javaWriter.println();
    javaWriter.println("  StructAccessor accessor;");
    javaWriter.println();
    javaWriter.println("  private static final int mdIdx = MachineDescriptionRuntime.getStatic().ordinal();");
    javaWriter.println();
    // generate all offset and size arrays
    generateOffsetAndSizeArrays(javaWriter, "  ", containingJTypeName, structCType, null, null); /* w/o offset */
    if( GlueGen.debug() ) {
        System.err.printf("SE.__: structCType %s%n", structCType.getDebugString());
        System.err.printf("SE.__: contCTypeName %s%n", containingCType.getDebugString());
        System.err.printf("SE.__: contJTypeName %s%n", containingJType.getDebugString());
    }
    for (int i = 0; i < structCType.getNumFields(); i++) {
      final Field field = structCType.getField(i);
      final Type fieldType = field.getType();
      final String cfgFieldName0 = JavaConfiguration.canonicalStructFieldSymbol(structCTypeName, field.getName());
      if ( !cfg.shouldIgnoreInInterface(cfgFieldName0) ) {
        final String renamed = cfg.getJavaSymbolRename(cfgFieldName0);
        final String fieldName = null==renamed ? field.getName() : renamed;
        final String cfgFieldName1 = JavaConfiguration.canonicalStructFieldSymbol(structCTypeName, fieldName);
        if (fieldType.isFunctionPointer()) {
          // no offset/size for function pointer ..
          if( GlueGen.debug() ) {
            System.err.printf("SE.os.%02d: %s / %s, %s (%s)%n", (i+1), field, cfgFieldName1, fieldType.getDebugString(), "SKIP FuncPtr");
          }
        } else if (fieldType.isCompound()) {
          // FIXME: will need to support this at least in order to
          // handle the union in jawt_Win32DrawingSurfaceInfo (fabricate
          // a name?)
          if (fieldType.getName() == null) {
            throw new RuntimeException("Anonymous structs as fields not supported yet, field \"" +
                                       cfgFieldName1 + "\", "+fieldType.getDebugString());
          }
          if( GlueGen.debug() ) {
            System.err.printf("SE.os.%02d: %s / %s, %s (%s)%n", (i+1), field, cfgFieldName1, fieldType.getDebugString(), "compound");
          }
          generateOffsetAndSizeArrays(javaWriter, "  ", fieldName, fieldType, field, null);
        } else if (fieldType.isArray()) {
            final Type baseElementType = field.getType().asArray().getBaseElementType();
            if( GlueGen.debug() ) {
                System.err.printf("SE.os.%02d: %s / %s, %s (%s)%n", (i+1), field, cfgFieldName1, fieldType.getDebugString(), "array");
                System.err.printf("SE.os.%02d: baseType %s%n", (i+1), baseElementType.getDebugString());
            }
            generateOffsetAndSizeArrays(javaWriter, "  ", fieldName, fieldType, field, null);
        } else {
          final JavaType externalJavaType;
          try {
            externalJavaType = typeToJavaType(fieldType, machDescJava);
          } catch (final Exception e) {
            throw new RuntimeException("Error occurred while creating accessor for field \"" +
                                       cfgFieldName1 + "\", "+fieldType.getDebugString(), e);
          }
          if( GlueGen.debug() ) {
              System.err.printf("SE.os.%02d: %s / %s, %s (%s)%n", (i+1), field, fieldName, fieldType.getDebugString(), "MISC");
              System.err.printf("SE.os.%02d: javaType %s%n", (i+1), externalJavaType.getDebugString());
          }
          if (externalJavaType.isPrimitive()) {
            // Primitive type
            generateOffsetAndSizeArrays(javaWriter, "  ", fieldName, null, field, null); /* w/o size */
            generateOffsetAndSizeArrays(javaWriter, "//", fieldName, fieldType, null, null);
          } else if (externalJavaType.isCPrimitivePointerType()) {
            if( requiresGetCStringLength(fieldType, cfgFieldName1) ) {
                generateOffsetAndSizeArrays(javaWriter, "  ", fieldName, null, field, null); /* w/o size */
                generateOffsetAndSizeArrays(javaWriter, "//", fieldName, fieldType, null, "// "+externalJavaType.getDebugString());
            } else {
                generateOffsetAndSizeArrays(javaWriter, "//", fieldName, fieldType, field, "// "+externalJavaType.getDebugString());
            }
          } else {
            generateOffsetAndSizeArrays(javaWriter, "  ", fieldName, null, field, null); /* w/o size */
            generateOffsetAndSizeArrays(javaWriter, "//", fieldName, fieldType, null, "// "+externalJavaType.getDebugString());
          }
        }
      } else if( GlueGen.debug() ) {
        System.err.printf("SE.os.%02d: %s, %s (IGNORED)%n", (i+1), field, fieldType.getDebugString());
      }
    }
    javaWriter.println();
    javaWriter.println("  public static int size() {");
    javaWriter.println("    return "+containingJTypeName+"_size[mdIdx];");
    javaWriter.println("  }");
    javaWriter.println();
    javaWriter.println("  public static " + containingJTypeName + " create() {");
    javaWriter.println("    return create(Buffers.newDirectByteBuffer(size()));");
    javaWriter.println("  }");
    javaWriter.println();
    javaWriter.println("  public static " + containingJTypeName + " create(java.nio.ByteBuffer buf) {");
    javaWriter.println("      return new " + containingJTypeName + "(buf);");
    javaWriter.println("  }");
    javaWriter.println();
    javaWriter.println("  " + containingJTypeName + "(java.nio.ByteBuffer buf) {");
    javaWriter.println("    accessor = new StructAccessor(buf);");
    javaWriter.println("  }");
    javaWriter.println();
    javaWriter.println("  public java.nio.ByteBuffer getBuffer() {");
    javaWriter.println("    return accessor.getBuffer();");
    javaWriter.println("  }");

    final Set<MethodBinding> methodBindingSet = new HashSet<MethodBinding>();

    for (int i = 0; i < structCType.getNumFields(); i++) {
      final Field field = structCType.getField(i);
      final Type fieldType = field.getType();

      final String cfgFieldName0 = JavaConfiguration.canonicalStructFieldSymbol(structCTypeName, field.getName());
      if (!cfg.shouldIgnoreInInterface(cfgFieldName0)) {
        final String renamed = cfg.getJavaSymbolRename(cfgFieldName0);
        final String fieldName = renamed==null ? field.getName() : renamed;
        final String cfgFieldName1 = JavaConfiguration.canonicalStructFieldSymbol(structCTypeName, fieldName);

        if( GlueGen.debug() ) {
          System.err.printf("SE.ac.%02d: %s / %s, %s%n", (i+1), field, cfgFieldName1, fieldType.getDebugString());
        }
        if (fieldType.isFunctionPointer()) {
            generateFunctionPointerCode(methodBindingSet, javaWriter, jniWriter, structCTypeName, structClassPkgName,
                                        containingCType, containingJType, i,
                                        new FunctionSymbol(fieldName, fieldType.asPointer().getTargetType().asFunction()), cfgFieldName1);
        } else if (fieldType.isCompound()) {
          // FIXME: will need to support this at least in order to
          // handle the union in jawt_Win32DrawingSurfaceInfo (fabricate a name?)
          if (fieldType.getName() == null) {
            throw new RuntimeException("Anonymous structs as fields not supported yet (field \"" +
                                       field + "\" in type \"" + structCTypeName + "\")");
          }
          javaWriter.println();
          generateGetterSignature(javaWriter, fieldType, false, fieldType.getName(), capitalizeString(fieldName), null, null);
          javaWriter.println(" {");
          javaWriter.println("    return " + fieldType.getName() + ".create( accessor.slice( " +
                           fieldName+"_offset[mdIdx], "+fieldName+"_size[mdIdx] ) );");
          javaWriter.println(" }");

        } else if ( fieldType.isArray() || fieldType.isPointer() ) {
            generateArrayGetterSetterCode(methodBindingSet, javaWriter, jniWriter, structCTypeName, structClassPkgName,
                                          containingCType, containingJType,
                                          i, field, fieldName, cfgFieldName1);
        } else {
          final JavaType javaType;
          try {
            javaType = typeToJavaType(fieldType, machDescJava);
          } catch (final Exception e) {
            System.err.println("Error occurred while creating accessor for field \"" +
                               field.getName() + "\", "+fieldType.getDebugString());
            throw(e);
          }
          if (javaType.isPrimitive()) {
            // Primitive type
            final boolean fieldTypeNativeSizeFixed = fieldType.getSize().hasFixedNativeSize();
            final String javaTypeName;
            if ( isOpaque(fieldType) ) {
              javaTypeName = compatiblePrimitiveJavaTypeName(fieldType, javaType, machDescJava);
            } else {
              javaTypeName = javaType.getName();
            }
            final String capJavaTypeName = capitalizeString(javaTypeName);
            final String capFieldName = capitalizeString(fieldName);
            final String sizeDenominator = fieldType.isPointer() ? "pointer" : javaTypeName ;

            if(GlueGen.debug()) {
                System.err.println("Java.StructEmitter.Primitive: "+field.getName()+", "+fieldType.getDebugString()+", "+javaTypeName+", "+
                                   ", fixedSize "+fieldTypeNativeSizeFixed+", opaque "+isOpaque(fieldType)+", sizeDenominator "+sizeDenominator);
            }

            if( !fieldType.isConst() ) {
                // Setter
                javaWriter.println();
                generateSetterSignature(javaWriter, fieldType, false, containingJTypeName, capFieldName, null, javaTypeName, null, null);
                javaWriter.println(" {");
                if( fieldTypeNativeSizeFixed ) {
                    javaWriter.println("    accessor.set" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx], val);");
                } else {
                    javaWriter.println("    accessor.set" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx], val, MachineDescriptionRuntime.getStatic().md."+sizeDenominator+"SizeInBytes());");
                }
                javaWriter.println("    return this;");
                javaWriter.println("  }");
            }

            // Getter
            javaWriter.println();
            generateGetterSignature(javaWriter, fieldType, false, javaTypeName, capFieldName, null, null);
            javaWriter.println(" {");
            javaWriter.print  ("    return ");
            if( fieldTypeNativeSizeFixed ) {
                javaWriter.println("accessor.get" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx]);");
            } else {
                javaWriter.println("accessor.get" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx], MachineDescriptionRuntime.getStatic().md."+sizeDenominator+"SizeInBytes());");
            }
            javaWriter.println("  }");
          } else {
            javaWriter.println();
            javaWriter.println("  /** UNKNOWN: "+cfgFieldName1 +": "+fieldType.getDebugString()+", "+javaType.getDebugString()+" */");
          }
        }
      }
    }
    emitCustomJavaCode(javaWriter, containingJTypeName);
    if (needsNativeCode) {
        javaWriter.println();
        emitJavaInitCode(javaWriter, containingJTypeName);
        javaWriter.println();
    }
    javaWriter.println("}");
    javaWriter.flush();
    javaWriter.close();
    if (needsNativeCode) {
      emitCInitCode(jniWriter, structClassPkgName, containingJTypeName);
      jniWriter.flush();
      jniWriter.close();
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

  private void generateGetterSignature(final PrintWriter writer, final Type origFieldType, final boolean abstractMethod,
                                       final String returnTypeName, final String capitalizedFieldName,
                                       final String customArgs, final String arrayLengthExpr) {
      writer.print("  /** Getter for native field: "+origFieldType.getDebugString());
      if( null != arrayLengthExpr ) {
          writer.print(", with array length of <code>"+arrayLengthExpr+"</code>");
      }
      writer.println(" */");
      writer.print("  public " + (abstractMethod ? "abstract " : "") + returnTypeName + " get" + capitalizedFieldName + "(");
      if( null != customArgs ) {
          writer.print(customArgs);
      }
      writer.print(")");
  }

  private void generateSetterSignature(final PrintWriter writer, final Type origFieldType, final boolean abstractMethod,
                                       final String returnTypeName, final String capitalizedFieldName,
                                       final String customArgsPre, final String paramTypeName, final String customArgsPost,
                                       final String arrayLengthExpr) {
      writer.print("  /** Setter for native field: "+origFieldType.getDebugString());
      if( null != arrayLengthExpr ) {
          writer.print(", with array length of <code>"+arrayLengthExpr+"</code>");
      }
      writer.println(" */");
      writer.print("  public " + (abstractMethod ? "abstract " : "") + returnTypeName + " set" + capitalizedFieldName + "(");
      if( null != customArgsPre ) {
          writer.print(customArgsPre+", ");
      }
      writer.print(paramTypeName + " val");
      if( null != customArgsPost ) {
          writer.print(", "+customArgsPost);
      }
      writer.print(")");
  }

  private void generateOffsetAndSizeArrays(final PrintWriter writer, final String prefix, final String fieldName, final Type fieldType, final Field field, final String postfix) {
      if(null != field) {
          writer.print(prefix+"private static final int[] "+fieldName+"_offset = new int[] { ");
          for( int i=0; i < machDescTargetConfigs.length; i++ ) {
              if(0<i) {
                  writer.print(", ");
              }
              writer.print(field.getOffset(machDescTargetConfigs[i].md) +
                           " /* " + machDescTargetConfigs[i].name() + " */");
          }
          writer.println(" };");
      }
      if(null!=fieldType) {
          writer.print(prefix+"private static final int[] "+fieldName+"_size = new int[] { ");
          for( int i=0; i < machDescTargetConfigs.length; i++ ) {
              if(0<i) {
                  writer.print(", ");
              }
              writer.print(fieldType.getSize(machDescTargetConfigs[i].md) +
                           " /* " + machDescTargetConfigs[i].name() + " */");
          }
          writer.print("  };");
          if( null != postfix ) {
              writer.println(postfix);
          } else {
              writer.println();
          }
      }
  }

  private void generateFunctionPointerCode(final Set<MethodBinding> methodBindingSet,
          final PrintWriter javaWriter, final PrintWriter jniWriter,
          final String structCTypeName, final String structClassPkgName,
          final Type containingCType, final JavaType containingJType,
          final int i, final FunctionSymbol funcSym, final String returnSizeLookupName) {
      // Emit method call and associated native code
      final MethodBinding  mb = bindFunction(funcSym, containingJType, containingCType, machDescJava);
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
          javaWriter.println();
          // Emit public Java entry point for calling this function pointer
          JavaMethodBindingEmitter emitter =
                  new JavaMethodBindingEmitter(binding,
                          javaWriter,
                          cfg.runtimeExceptionType(),
                          cfg.unsupportedExceptionType(),
                          true,
                          cfg.tagNativeBinding(),
                          false, // eraseBufferAndArrayTypes
                          useNIOOnly,
                          useNIODirectOnly,
                          false,
                          false, // FIXME: should unify this with the general emission code
                          false, // forIndirectBufferAndArrayImplementation
                          false, // FIXME: should unify this with the general emission code
                          false,
                          cfg);
          emitter.addModifier(JavaMethodBindingEmitter.PUBLIC);
          emitter.emit();

          // Emit private native Java entry point for calling this function pointer
          emitter =
                  new JavaMethodBindingEmitter(binding,
                          javaWriter,
                          cfg.runtimeExceptionType(),
                          cfg.unsupportedExceptionType(),
                          false,
                          cfg.tagNativeBinding(),
                          true, // eraseBufferAndArrayTypes
                          useNIOOnly,
                          useNIODirectOnly,
                          true,
                          true, // FIXME: should unify this with the general emission code
                          false, // forIndirectBufferAndArrayImplementation
                          false, // FIXME: should unify this with the general emission code
                          false,
                          cfg);
          emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
          emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
          emitter.emit();

          // Emit (private) C entry point for calling this function pointer
          final CMethodBindingEmitter cEmitter =
                  new CMethodBindingEmitter(binding,
                          jniWriter,
                          structClassPkgName,
                          containingJType.getName(),
                          true, // FIXME: this is optional at this point
                          false,
                          true,
                          false, // forIndirectBufferAndArrayImplementation
                          machDescJava);
          cEmitter.setIsCStructFunctionPointer(true);
          prepCEmitter(returnSizeLookupName, binding.getJavaReturnType(), cEmitter);
          cEmitter.emit();
      }
  }

  private void generateArrayPointerCode(final Set<MethodBinding> methodBindingSet,
          final PrintWriter javaWriter, final PrintWriter jniWriter,
          final String structCTypeName, final String structClassPkgName,
          final Type containingCType, final JavaType containingJType,
          final int i, final FunctionSymbol funcSym,
          final String returnSizeLookupName, final String docArrayLenExpr, final String nativeArrayLenExpr) {
      // Emit method call and associated native code
      final MethodBinding  mb = bindFunction(funcSym, containingJType, containingCType, machDescJava);
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
          JavaMethodBindingEmitter emitter;

          // Emit private native Java entry point for calling this function pointer
          emitter =
                  new JavaMethodBindingEmitter(binding,
                          javaWriter,
                          cfg.runtimeExceptionType(),
                          cfg.unsupportedExceptionType(),
                          false,
                          cfg.tagNativeBinding(),
                          true, // eraseBufferAndArrayTypes
                          useNIOOnly,
                          useNIODirectOnly,
                          true,
                          true, // FIXME: should unify this with the general emission code
                          false, // forIndirectBufferAndArrayImplementation
                          false, // FIXME: should unify this with the general emission code
                          false,
                          cfg);
          if( null != docArrayLenExpr ) {
              emitter.setReturnedArrayLengthExpression(docArrayLenExpr, true);
          }
          emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
          emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
          emitter.emit();

          // Emit (private) C entry point for calling this function pointer
          final CMethodBindingEmitter cEmitter =
                  new CMethodBindingEmitter(binding,
                          jniWriter,
                          structClassPkgName,
                          containingJType.getName(),
                          true, // FIXME: this is optional at this point
                          false,
                          true,
                          false, // forIndirectBufferAndArrayImplementation
                          machDescJava);
          cEmitter.setIsCStructFunctionPointer(false);
          final String lenExprSet;
          if( null != nativeArrayLenExpr ) {
              final JavaType javaReturnType = binding.getJavaReturnType();
              if (javaReturnType.isNIOBuffer() ||
                      javaReturnType.isCompoundTypeWrapper()) {
                  final Type retType = funcSym.getReturnType();
                  final Type baseType = retType.getBaseElementType();
                  lenExprSet = nativeArrayLenExpr+" * sizeof("+baseType.getName()+")";
                  cEmitter.setReturnValueCapacityExpression( new MessageFormat(lenExprSet) );
              } else if (javaReturnType.isArray() ||
                      javaReturnType.isArrayOfCompoundTypeWrappers()) {
                  lenExprSet = nativeArrayLenExpr;
                  cEmitter.setReturnValueLengthExpression( new MessageFormat(lenExprSet) );
              } else {
                  lenExprSet = null;
              }
          } else {
              lenExprSet = null;
          }
          prepCEmitter(returnSizeLookupName, binding.getJavaReturnType(), cEmitter);
          cEmitter.emit();
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
              typeIter = typeIter.getElementType().asArray();
          }
      }
      final String cfgVal = cfg.returnedArrayLength(returnSizeLookupName);
      if( null != cfgVal ) {
          if( hasFixedTypeLen[0] ) {
              System.err.println("WARNING: struct array field '"+returnSizeLookupName+"' of '"+type+"' length '"+Arrays.toString(length)+"' overwritten by cfg-expression: "+cfgVal);
          }
          return cfgVal;
      }
      if( hasFixedTypeLen[0] ) {
          return lengthExpr.toString();
      } else {
          System.err.println("WARNING: struct array field '"+returnSizeLookupName+"' length '"+Arrays.toString(length)+"' without fixed- nor configured-size: "+type.getDebugString());
          return null;
      }
  }
  private String getPointerArrayLengthExpr(final PointerType type, final String returnSizeLookupName) {
      final String cfgVal = cfg.returnedArrayLength(returnSizeLookupName);
      if( null != cfgVal ) {
          return cfgVal;
      }
      return null;
  }

  private static final String dummyFuncTypeName = "null *";
  private static final Type int32Type = new IntType("int32_t", SizeThunk.INT32, false, CVAttributes.CONST);
  // private static final Type int8Type = new IntType("char", SizeThunk.INT8, false, 0);
  // private static final Type int8PtrType = new PointerType(SizeThunk.POINTER, int8Type, 0);
  private static final String nativeArrayLengthArg = "arrayLength";
  private static final String nativeArrayLengthONE = "1";
  private static final String nativeArrayElemOffsetArg = "elem_offset";

  private boolean requiresGetCStringLength(final Type fieldType, final String returnSizeLookupName) {
      if( !cfg.returnsString(returnSizeLookupName) ) {
          return false;
      }
      final PointerType pointerType = fieldType.asPointer();
      if( null != pointerType ) {
          return null == getPointerArrayLengthExpr(pointerType, returnSizeLookupName);
      }
      return false;
  }

  private void generateArrayGetterSetterCode(final Set<MethodBinding> methodBindingSet,
                                             final PrintWriter javaWriter, final PrintWriter jniWriter,
                                             final String structCTypeName, final String structClassPkgName,
                                             final Type containingCType, final JavaType containingJType,
                                             final int i, final Field field, final String fieldName, final String returnSizeLookupName) throws Exception {
      final Type fieldType = field.getType();
      final JavaType javaType;
      try {
        javaType = typeToJavaType(fieldType, machDescJava);
      } catch (final Exception e) {
        throw new RuntimeException("Error occurred while creating array/pointer accessor for field \"" +
                                   returnSizeLookupName + "\", "+fieldType.getDebugString(), e);
      }
      if( GlueGen.debug() ) {
          System.err.printf("SE.ac.%02d: javaType  %s%n", (i+1), javaType.getDebugString());
      }

      //
      // Collect all required information including considering Opaque types
      //
      final String containingJTypeName = containingJType.getName();
      final boolean isOpaque = isOpaque(fieldType);
      final boolean isString = cfg.returnsString(returnSizeLookupName); // FIXME: Allow custom Charset ? US-ASCII, UTF-8 or UTF-16 ?
      final boolean useGetCStringLength;
      final String arrayLengthExpr;
      final int[] arrayLengths;
      final boolean useFixedTypeLen[] = { false };
      final boolean isPointer;
      final boolean isPrimitive;
      final boolean isConst;
      final JavaType baseJElemType;
      final String baseJElemTypeName;
      final boolean hasSingleElement;
      final String capitalFieldName;
      final String baseJElemTypeNameC;
      final String baseJElemTypeNameU;
      final boolean isByteBuffer;
      final boolean baseCElemNativeSizeFixed;
      final String baseCElemSizeDenominator;
      {
          final Type baseCElemType;
          final ArrayType arrayType = fieldType.asArray();
          String _arrayLengthExpr = null;
          if( isOpaque || javaType.isPrimitive() ) {
              // Overridden by JavaConfiguration.typeInfo(..), i.e. Opaque!
              // Emulating array w/ 1 element
              isPrimitive = true;
              _arrayLengthExpr = nativeArrayLengthONE;
              arrayLengths = new int[] { 1 };
              baseCElemType = null;
              isPointer = false;
              isConst = fieldType.isConst();
              baseJElemType = null;
              baseJElemTypeName = compatiblePrimitiveJavaTypeName(fieldType, javaType, machDescJava);
              baseCElemNativeSizeFixed = false;
              baseCElemSizeDenominator = fieldType.isPointer() ? "pointer" : baseJElemTypeName ;
          } else {
              if( null != arrayType ) {
                  final int[][] lengthRes = new int[1][];
                  _arrayLengthExpr = getArrayArrayLengthExpr(arrayType, returnSizeLookupName, useFixedTypeLen, lengthRes);
                  arrayLengths = lengthRes[0];
                  baseCElemType = arrayType.getBaseElementType();
                  isPointer = false;
              } else {
                  final PointerType pointerType = fieldType.asPointer();
                  _arrayLengthExpr = getPointerArrayLengthExpr(pointerType, returnSizeLookupName);
                  arrayLengths = null;
                  baseCElemType = pointerType.getBaseElementType();
                  isPointer = true;
                  if( 1 != pointerType.pointerDepth() ) {
                      javaWriter.println();
                      final String msg = "SKIP ptr-ptr (depth "+pointerType.pointerDepth()+"): "+returnSizeLookupName +": "+fieldType;
                      javaWriter.println("  // "+msg);
                      System.err.println("WARNING: "+msg);
                      return;
                  }
              }
              if( GlueGen.debug() ) {
                  System.err.printf("SE.ac.%02d: baseCType %s%n", (i+1), baseCElemType.getDebugString());
              }
              isPrimitive = baseCElemType.isPrimitive();
              isConst = baseCElemType.isConst();
              try {
                  baseJElemType = typeToJavaType(baseCElemType, machDescJava);
              } catch (final Exception e ) {
                  throw new RuntimeException("Error occurred while creating array/pointer accessor for field \"" +
                                              returnSizeLookupName + "\", baseType "+baseCElemType.getDebugString()+", topType "+fieldType.getDebugString(), e);
              }
              baseJElemTypeName = baseJElemType.getName();
              baseCElemNativeSizeFixed = baseCElemType.isPrimitive() ? baseCElemType.getSize().hasFixedNativeSize() : true;
              baseCElemSizeDenominator = baseCElemType.isPointer() ? "pointer" : baseJElemTypeName ;

              if( !baseCElemNativeSizeFixed ) {
                  javaWriter.println();
                  final String msg = "SKIP primitive w/ platform dependent sized type in struct: "+returnSizeLookupName+": "+fieldType.getDebugString();
                  javaWriter.println("  // "+msg);
                  System.err.println("WARNING: "+msg);
                  return;
              }
          }
          if( GlueGen.debug() ) {
              System.err.printf("SE.ac.%02d: baseJElemType %s%n", (i+1), (null != baseJElemType ? baseJElemType.getDebugString() : null));
          }
          capitalFieldName = capitalizeString(fieldName);
          baseJElemTypeNameC = capitalizeString(baseJElemTypeName);
          baseJElemTypeNameU = baseJElemTypeName.toUpperCase();
          isByteBuffer = "Byte".equals(baseJElemTypeNameC);
          if( null == _arrayLengthExpr && isString && isPointer ) {
              useGetCStringLength = true;
              _arrayLengthExpr = "getCStringLengthImpl(pString)+1";
              this.requiresStaticInitialization = true;
              LOG.log(INFO, "StaticInit Trigger.3 \"{0}\"", returnSizeLookupName);
          } else {
              useGetCStringLength = false;
          }
          arrayLengthExpr = _arrayLengthExpr;
          if( null == arrayLengthExpr ) {
              javaWriter.println();
              final String msg = "SKIP unsized array in struct: "+returnSizeLookupName+": "+fieldType.getDebugString();
              javaWriter.println("  // "+msg);
              System.err.println("WARNING: "+msg);
              return;
          }
          boolean _hasSingleElement=false;
          try {
              _hasSingleElement = 1 ==Integer.parseInt(_arrayLengthExpr);
          } catch (final Exception e ) {}
          hasSingleElement = _hasSingleElement;
      }
      if( GlueGen.debug() ) {
          System.err.printf("SE.ac.%02d: baseJElemTypeName %s, array-lengths %s%n", (i+1), baseJElemTypeName, Arrays.toString(arrayLengths));
          System.err.printf("SE.ac.%02d: arrayLengthExpr: %s, hasSingleElement %b, isByteBuffer %b, isString %b, isPointer %b, isPrimitive %b, isOpaque %b, baseCElemNativeSizeFixed %b, baseCElemSizeDenominator %s, isConst %b, useGetCStringLength %b%n",
                  (i+1), arrayLengthExpr, hasSingleElement, isByteBuffer, isString, isPointer, isPrimitive, isOpaque,
                  baseCElemNativeSizeFixed, baseCElemSizeDenominator,
                  isConst, useGetCStringLength);
      }

      //
      // Emit ..
      //
      if( !hasSingleElement && useFixedTypeLen[0] ) {
          javaWriter.println();
          generateGetterSignature(javaWriter, fieldType, false, "final int", capitalFieldName+"ArrayLength", null, arrayLengthExpr);
          javaWriter.println(" {");
          javaWriter.println("    return "+arrayLengthExpr+";");
          javaWriter.println("  }");
      }
      if( !isConst ) {
          // Setter
          javaWriter.println();
          if( isPrimitive ) {
              // Setter Primitive
              if( isPointer ) {
                  // Setter Primitive Pointer
                  final String msg = "SKIP setter for primitive-pointer type in struct: "+returnSizeLookupName+": "+fieldType.getDebugString();
                  javaWriter.println("  // "+msg);
                  System.err.println("INFO: "+msg);
              } else {
                  // Setter Primitive Array
                  if( hasSingleElement ) {
                      generateSetterSignature(javaWriter, fieldType, false, containingJTypeName, capitalFieldName, null, baseJElemTypeName, null, arrayLengthExpr);
                      javaWriter.println(" {");
                      if( baseCElemNativeSizeFixed ) {
                          javaWriter.println("    accessor.set" + baseJElemTypeNameC + "At(" + fieldName+"_offset[mdIdx], val);");
                      } else {
                          javaWriter.println("    accessor.set" + baseJElemTypeNameC + "At(" + fieldName+"_offset[mdIdx], val, MachineDescriptionRuntime.getStatic().md."+baseCElemSizeDenominator+"SizeInBytes());");
                      }
                      javaWriter.println("    return this;");
                      javaWriter.println("  }");
                  } else {
                      generateSetterSignature(javaWriter, fieldType, false, containingJTypeName, capitalFieldName, "final int offset", baseJElemTypeName+"[]", null, arrayLengthExpr);
                      javaWriter.println(" {");
                      javaWriter.println("    final int arrayLength = "+arrayLengthExpr+";");
                      javaWriter.println("    if( offset + val.length > arrayLength ) { throw new IndexOutOfBoundsException(\"offset \"+offset+\" + val.length \"+val.length+\" > array-length \"+arrayLength); };");
                      javaWriter.println("    final int elemSize = Buffers.SIZEOF_"+baseJElemTypeNameU+";");
                      javaWriter.println("    final ByteBuffer destB = getBuffer();");
                      javaWriter.println("    final int bTotal = arrayLength * elemSize;");
                      javaWriter.println("    if( bTotal > "+fieldName+"_size[mdIdx] ) { throw new IndexOutOfBoundsException(\"bTotal \"+bTotal+\" > size \"+"+fieldName+"_size[mdIdx]+\", elemSize \"+elemSize+\" * \"+arrayLength); };");
                      javaWriter.println("    int bOffset = "+fieldName+"_offset[mdIdx];");
                      javaWriter.println("    final int bLimes = bOffset + bTotal;");
                      javaWriter.println("    if( bLimes > destB.limit() ) { throw new IndexOutOfBoundsException(\"bLimes \"+bLimes+\" > buffer.limit \"+destB.limit()+\", elemOff \"+bOffset+\", elemSize \"+elemSize+\" * \"+arrayLength); };");
                      javaWriter.println("    bOffset += elemSize * offset;");
                      javaWriter.println("    accessor.set" + baseJElemTypeNameC + "sAt(bOffset, val);");
                      javaWriter.println("    return this;");
                      javaWriter.println("  }");
                  }
              }
          } else {
              // Setter Struct
              if( isPointer ) {
                  // Setter Struct Pointer
                  final String msg = "SKIP setter for complex-pointer type in struct: "+returnSizeLookupName+": "+fieldType.getDebugString();
                  javaWriter.println("  // "+msg);
                  System.err.println("INFO: "+msg);
              } else {
                  // Setter Struct Array
                  if( hasSingleElement ) {
                      generateSetterSignature(javaWriter, fieldType, false, containingJTypeName, capitalFieldName, null, baseJElemTypeName, null, arrayLengthExpr);
                      javaWriter.println(" {");
                      javaWriter.println("    final int elemSize = "+baseJElemTypeName+".size();");
                      javaWriter.println("    final ByteBuffer destB = getBuffer();");
                      javaWriter.println("    if( elemSize > "+fieldName+"_size[mdIdx] ) { throw new IndexOutOfBoundsException(\"elemSize \"+elemSize+\" > size \"+"+fieldName+"_size[mdIdx]); };");
                      javaWriter.println("    int bOffset = "+fieldName+"_offset[mdIdx];");
                      javaWriter.println("    final int bLimes = bOffset + elemSize;");
                      javaWriter.println("    if( bLimes > destB.limit() ) { throw new IndexOutOfBoundsException(\"bLimes \"+bLimes+\" > buffer.limit \"+destB.limit()+\", elemOff \"+bOffset+\", elemSize \"+elemSize); };");
                      javaWriter.println("    final ByteBuffer sourceB = val.getBuffer();");
                      javaWriter.println("    for(int f=0; f<elemSize; f++) {");
                      javaWriter.println("      if( bOffset >= bLimes ) { throw new IndexOutOfBoundsException(\"elem-byte[0][\"+f+\"]: bOffset \"+bOffset+\" >= bLimes \"+bLimes+\", elemSize \"+elemSize); };");
                      javaWriter.println("      destB.put(bOffset++, sourceB.get(f));");
                      javaWriter.println("    }");
                      javaWriter.println("    return this;");
                      javaWriter.println("  }");
                  } else {
                      generateSetterSignature(javaWriter, fieldType, false, containingJTypeName, capitalFieldName, "final int offset", baseJElemTypeName+"[]", null, arrayLengthExpr);
                      javaWriter.println(" {");
                      javaWriter.println("    final int arrayLength = "+arrayLengthExpr+";");
                      javaWriter.println("    if( offset + val.length > arrayLength ) { throw new IndexOutOfBoundsException(\"offset \"+offset+\" + val.length \"+val.length+\" > array-length \"+arrayLength); };");
                      javaWriter.println("    final int elemSize = "+baseJElemTypeName+".size();");
                      javaWriter.println("    final ByteBuffer destB = getBuffer();");
                      javaWriter.println("    final int bTotal = arrayLength * elemSize;");
                      javaWriter.println("    if( bTotal > "+fieldName+"_size[mdIdx] ) { throw new IndexOutOfBoundsException(\"bTotal \"+bTotal+\" > size \"+"+fieldName+"_size[mdIdx]+\", elemSize \"+elemSize+\" * \"+arrayLength); };");
                      javaWriter.println("    int bOffset = "+fieldName+"_offset[mdIdx];");
                      javaWriter.println("    final int bLimes = bOffset + bTotal;");
                      javaWriter.println("    if( bLimes > destB.limit() ) { throw new IndexOutOfBoundsException(\"bLimes \"+bLimes+\" > buffer.limit \"+destB.limit()+\", elemOff \"+bOffset+\", elemSize \"+elemSize+\" * \"+arrayLength); };");
                      javaWriter.println("    bOffset += elemSize * offset;");
                      javaWriter.println("    for(int index=0; index<val.length; index++) {");
                      javaWriter.println("      final ByteBuffer sourceB = val[index].getBuffer();");
                      javaWriter.println("      for(int f=0; f<elemSize; f++) {");
                      javaWriter.println("        if( bOffset >= bLimes ) { throw new IndexOutOfBoundsException(\"elem-byte[\"+(offset+index)+\"][\"+f+\"]: bOffset \"+bOffset+\" >= bLimes \"+bLimes+\", elemSize \"+elemSize+\" * \"+arrayLength); };");
                      javaWriter.println("        destB.put(bOffset++, sourceB.get(f));");
                      javaWriter.println("      }");
                      javaWriter.println("    }");
                      javaWriter.println("    return this;");
                      javaWriter.println("  }");
                      javaWriter.println();
                      generateSetterSignature(javaWriter, fieldType, false, containingJTypeName, capitalFieldName, "final int index", baseJElemTypeName, null, arrayLengthExpr);
                      javaWriter.println(" {");
                      javaWriter.println("    final int arrayLength = "+arrayLengthExpr+";");
                      javaWriter.println("    final int elemSize = "+baseJElemTypeName+".size();");
                      javaWriter.println("    final ByteBuffer destB = getBuffer();");
                      javaWriter.println("    final int bTotal = arrayLength * elemSize;");
                      javaWriter.println("    if( bTotal > "+fieldName+"_size[mdIdx] ) { throw new IndexOutOfBoundsException(\"bTotal \"+bTotal+\" > size \"+"+fieldName+"_size[mdIdx]+\", elemSize \"+elemSize+\" * \"+arrayLength); };");
                      javaWriter.println("    int bOffset = "+fieldName+"_offset[mdIdx];");
                      javaWriter.println("    final int bLimes = bOffset + bTotal;");
                      javaWriter.println("    if( bLimes > destB.limit() ) { throw new IndexOutOfBoundsException(\"bLimes \"+bLimes+\" > buffer.limit \"+destB.limit()+\", elemOff \"+bOffset+\", elemSize \"+elemSize+\" * \"+arrayLength); };");
                      javaWriter.println("    bOffset += elemSize * index;");
                      javaWriter.println("    final ByteBuffer sourceB = val.getBuffer();");
                      javaWriter.println("    for(int f=0; f<elemSize; f++) {");
                      javaWriter.println("      if( bOffset >= bLimes ) { throw new IndexOutOfBoundsException(\"elem-byte[\"+index+\"][\"+f+\"]: bOffset \"+bOffset+\" >= bLimes \"+bLimes+\", elemSize \"+elemSize+\" * \"+arrayLength); };");
                      javaWriter.println("      destB.put(bOffset++, sourceB.get(f));");
                      javaWriter.println("    }");
                      javaWriter.println("    return this;");
                      javaWriter.println("  }");
                  }
              }
          }
      }
      // Getter
      javaWriter.println();
      if( isPrimitive ) {
          // Getter Primitive
          if( isPointer ) {
              // Getter Primitive Pointer
              final FunctionType ft = new FunctionType(dummyFuncTypeName, SizeThunk.POINTER, fieldType, 0);
              ft.addArgument(containingCType.getCVVariant(containingCType.getCVAttributes() | CVAttributes.CONST),
                             CMethodBindingEmitter.cThisArgumentName());
              ft.addArgument(int32Type, nativeArrayLengthArg);
              final FunctionSymbol fs = new FunctionSymbol("get"+capitalFieldName, ft);
              jniWriter.println();
              jniWriter.print("static "+fs.toString());
              jniWriter.println("{");
              jniWriter.println("  return "+CMethodBindingEmitter.cThisArgumentName()+"->"+field.getName()+";");
              jniWriter.println("}");
              jniWriter.println();
              generateArrayPointerCode(methodBindingSet, javaWriter, jniWriter, structCTypeName, structClassPkgName,
                                       containingCType, containingJType, i, fs, returnSizeLookupName, arrayLengthExpr, nativeArrayLengthArg);
              javaWriter.println();
              generateGetterSignature(javaWriter, fieldType, false, baseJElemTypeNameC+"Buffer", capitalFieldName, null, arrayLengthExpr);
              javaWriter.println(" {");
              if( useGetCStringLength ) {
                  javaWriter.println("    final int arrayLength = get"+capitalFieldName+"ArrayLength();");
              } else {
                  javaWriter.println("    final int arrayLength = "+arrayLengthExpr+";");
              }
              javaWriter.println("    final ByteBuffer _res = get"+capitalFieldName+"0(getBuffer(), arrayLength);");
              javaWriter.println("    if (_res == null) return null;");
              javaWriter.print("    return Buffers.nativeOrder(_res)");
              if( !isByteBuffer ) {
                  javaWriter.print(".as"+baseJElemTypeNameC+"Buffer()");
              }
              javaWriter.println(";");
              javaWriter.println("  }");
              if( isString && isByteBuffer ) {
                  javaWriter.println();
                  generateGetterSignature(javaWriter, fieldType, false, "String", capitalFieldName+"AsString", null, arrayLengthExpr);
                  javaWriter.println(" {");
                  if( useGetCStringLength ) {
                      javaWriter.println("    final int arrayLength = get"+capitalFieldName+"ArrayLength();");
                  } else {
                      javaWriter.println("    final int arrayLength = "+arrayLengthExpr+";");
                  }
                  javaWriter.println("    final ByteBuffer bb = get"+capitalFieldName+"0(getBuffer(), arrayLength);");
                  javaWriter.println("    if (bb == null) return null;");
                  javaWriter.println("    final byte[] ba = new byte[arrayLength];");
                  javaWriter.println("    int i = -1;");
                  javaWriter.println("    while( ++i < arrayLength ) {");
                  javaWriter.println("      ba[i] = bb.get(i);");
                  javaWriter.println("      if( (byte)0 == ba[i] ) break;");
                  javaWriter.println("    }");
                  javaWriter.println("    return new String(ba, 0, i);");
                  javaWriter.println("  }");
              }
              if( useGetCStringLength ) {
                  javaWriter.println();
                  generateGetterSignature(javaWriter, fieldType, false, "final int", capitalFieldName+"ArrayLength", null, arrayLengthExpr);
                  javaWriter.println(" {");
                  javaWriter.println("    final long pString = PointerBuffer.wrap( accessor.slice(" + fieldName+"_offset[mdIdx],  PointerBuffer.ELEMENT_SIZE) ).get(0);");
                  javaWriter.println("    return "+arrayLengthExpr+";");
                  javaWriter.println("  }");
              }
          } else {
              // Getter Primitive Array
              if( hasSingleElement ) {
                  generateGetterSignature(javaWriter, fieldType, false, baseJElemTypeName, capitalFieldName, null, arrayLengthExpr);
                  javaWriter.println(" {");
                  if( baseCElemNativeSizeFixed ) {
                      javaWriter.println("    return accessor.get" + baseJElemTypeNameC + "At(" + fieldName+"_offset[mdIdx]);");
                  } else {
                      javaWriter.println("    return accessor.get" + baseJElemTypeNameC + "At(" + fieldName+"_offset[mdIdx], MachineDescriptionRuntime.getStatic().md."+baseCElemSizeDenominator+"SizeInBytes());");
                  }
                  javaWriter.println("  }");
                  javaWriter.println();
              } else {
                  generateGetterSignature(javaWriter, fieldType, false, baseJElemTypeNameC+"Buffer", capitalFieldName, null, arrayLengthExpr);
                  javaWriter.println(" {");
                  javaWriter.print("    return accessor.slice(" + fieldName+"_offset[mdIdx],  Buffers.SIZEOF_"+baseJElemTypeNameU+" * "+arrayLengthExpr+")");
                  if( !isByteBuffer ) {
                      javaWriter.print(".as"+baseJElemTypeNameC+"Buffer()");
                  }
                  javaWriter.println(";");
                  javaWriter.println("  }");
                  javaWriter.println();
                  if( isString && isByteBuffer ) {
                      generateGetterSignature(javaWriter, fieldType, false, "String", capitalFieldName+"AsString", null, arrayLengthExpr);
                      javaWriter.println(" {");
                      javaWriter.println("    final int offset = " + fieldName+"_offset[mdIdx];");
                      javaWriter.println("    final int arrayLength = "+arrayLengthExpr+";");
                      javaWriter.println("    final ByteBuffer bb = getBuffer();");
                      javaWriter.println("    final byte[] ba = new byte[arrayLength];");
                      javaWriter.println("    int i = -1;");
                      javaWriter.println("    while( ++i < arrayLength ) {");
                      javaWriter.println("      ba[i] = bb.get(offset+i);");
                      javaWriter.println("      if( (byte)0 == ba[i] ) break;");
                      javaWriter.println("    }");
                      javaWriter.println("    return new String(ba, 0, i);");
                      javaWriter.println("  }");
                  } else {
                      generateGetterSignature(javaWriter, fieldType, false, baseJElemTypeName+"[]", capitalFieldName, "final int offset, "+baseJElemTypeName+" result[]", arrayLengthExpr);
                      javaWriter.println(" {");
                      javaWriter.println("    final int arrayLength = "+arrayLengthExpr+";");
                      javaWriter.println("    if( offset + result.length > arrayLength ) { throw new IndexOutOfBoundsException(\"offset \"+offset+\" + result.length \"+result.length+\" > array-length \"+arrayLength); };");
                      javaWriter.println("    return accessor.get" + baseJElemTypeNameC + "sAt(" + fieldName+"_offset[mdIdx] + (Buffers.SIZEOF_"+baseJElemTypeNameU+" * offset), result);");
                      javaWriter.println("  }");
                      javaWriter.println();
                  }
              }
          }
      } else {
          // Getter Struct
          if( isPointer ) {
              // Getter Struct Pointer
              final FunctionType ft = new FunctionType(dummyFuncTypeName, SizeThunk.POINTER, fieldType, 0);
              ft.addArgument(containingCType.getCVVariant(containingCType.getCVAttributes() | CVAttributes.CONST),
                             CMethodBindingEmitter.cThisArgumentName());
              ft.addArgument(int32Type, nativeArrayElemOffsetArg);
              final FunctionSymbol fs = new FunctionSymbol("get"+capitalFieldName, ft);
              jniWriter.println();
              jniWriter.print("static "+fs.toString());
              jniWriter.println("{");
              jniWriter.println("  return "+CMethodBindingEmitter.cThisArgumentName()+"->"+field.getName()+"+"+nativeArrayElemOffsetArg+";");
              jniWriter.println("}");
              jniWriter.println();
              generateArrayPointerCode(methodBindingSet, javaWriter, jniWriter, structCTypeName, structClassPkgName,
                                       containingCType, containingJType, i, fs, returnSizeLookupName, arrayLengthExpr, nativeArrayLengthONE);
              javaWriter.println();
              if( hasSingleElement ) {
                  generateGetterSignature(javaWriter, fieldType, false, baseJElemTypeName, capitalFieldName, null, arrayLengthExpr);
                  javaWriter.println(" {");
                  javaWriter.println("    final ByteBuffer source = getBuffer();");
                  javaWriter.println("    final ByteBuffer _res = get"+capitalFieldName+"0(source, 0);");
                  javaWriter.println("    if (_res == null) return null;");
                  javaWriter.println("    return "+baseJElemTypeName+".create(_res);");
                  javaWriter.println("  }");
              } else {
                  generateGetterSignature(javaWriter, fieldType, false, baseJElemTypeName+"[]", capitalFieldName, "final int offset, "+baseJElemTypeName+" result[]", arrayLengthExpr);
                  javaWriter.println(" {");
                  javaWriter.println("    final int arrayLength = "+arrayLengthExpr+";");
                  javaWriter.println("    if( offset + result.length > arrayLength ) { throw new IndexOutOfBoundsException(\"offset \"+offset+\" + result.length \"+result.length+\" > array-length \"+arrayLength); };");
                  javaWriter.println("    final ByteBuffer source = getBuffer();");
                  javaWriter.println("    for(int index=0; index<result.length; index++) {");
                  javaWriter.println("      final ByteBuffer _res = get"+capitalFieldName+"0(source, offset+index);");
                  javaWriter.println("      if (_res == null) return null;");
                  javaWriter.println("      result[index] = "+baseJElemTypeName+".create(_res);");
                  javaWriter.println("    }");
                  javaWriter.println("    return result;");
                  javaWriter.println("  }");
              }
          } else {
              // Getter Struct Array
              if( hasSingleElement ) {
                  generateGetterSignature(javaWriter, fieldType, false, baseJElemTypeName, capitalFieldName, null, arrayLengthExpr);
                  javaWriter.println(" {");
                  javaWriter.println("    return "+baseJElemTypeName+".create(accessor.slice("+fieldName+"_offset[mdIdx], "+baseJElemTypeName+".size()));");
                  javaWriter.println("  }");
              } else {
                  generateGetterSignature(javaWriter, fieldType, false, baseJElemTypeName+"[]", capitalFieldName, "final int offset, "+baseJElemTypeName+" result[]", arrayLengthExpr);
                  javaWriter.println(" {");
                  javaWriter.println("    final int arrayLength = "+arrayLengthExpr+";");
                  javaWriter.println("    if( offset + result.length > arrayLength ) { throw new IndexOutOfBoundsException(\"offset \"+offset+\" + result.length \"+result.length+\" > array-length \"+arrayLength); };");
                  javaWriter.println("    final int elemSize = "+baseJElemTypeName+".size();");
                  javaWriter.println("    int bOffset = "+fieldName+"_offset[mdIdx] + ( elemSize * offset );");
                  javaWriter.println("    for(int index=0; index<result.length; index++) {");
                  javaWriter.println("      result[index] = "+baseJElemTypeName+".create(accessor.slice(bOffset, elemSize));");
                  javaWriter.println("      bOffset += elemSize;");
                  javaWriter.println("    }");
                  javaWriter.println("    return result;");
                  javaWriter.println("  }");
              }
          }
      }
  }

  private static final boolean DEBUG_TYPEC2JAVA = false;
  private JavaType typeToJavaType(final Type cType, final MachineDescription curMachDesc) {
      final JavaType jt = typeToJavaTypeImpl(cType, curMachDesc);
      if( DEBUG_TYPEC2JAVA ) {
          System.err.println("typeToJavaType: "+cType.getDebugString()+" -> "+jt.getDebugString());
      }
      return jt;
  }
  private boolean isJNIEnvPointer(final Type cType) {
    final PointerType opt = cType.asPointer();
    return (opt != null) &&
           (opt.getTargetType().getName() != null) &&
           (opt.getTargetType().getName().equals("JNIEnv"));
  }
  private JavaType typeToJavaTypeImpl(final Type cType, final MachineDescription curMachDesc) {
    // Recognize JNIEnv* case up front
    if( isJNIEnvPointer(cType) ) {
        return JavaType.createForJNIEnv();
    }
    // Opaque specifications override automatic conversions
    // in case the identity is being used .. not if ptr-ptr
    final TypeInfo info = cfg.typeInfo(cType, typedefDictionary);
    if (info != null) {
      boolean isPointerPointer = false;
      if (cType.pointerDepth() > 0 || cType.arrayDimension() > 0) {
        Type targetType; // target type
        if (cType.isPointer()) {
          // t is <type>*, we need to get <type>
          targetType = cType.asPointer().getTargetType();
        } else {
          // t is <type>[], we need to get <type>
          targetType = cType.asArray().getBaseElementType();
        }
        if (cType.pointerDepth() == 2 || cType.arrayDimension() == 2) {
          // Get the target type of the target type (targetType was computer earlier
          // as to be a pointer to the target type, so now we need to get its
          // target type)
          if (targetType.isPointer()) {
            isPointerPointer = true;

            // t is<type>**, targetType is <type>*, we need to get <type>
            final Type bottomType = targetType.asPointer().getTargetType();
            if( GlueGen.debug() ) {
                LOG.log(INFO, "Opaque Type: {0}, targetType: {1}, bottomType: {2} is ptr-ptr", new Object[]{cType.getDebugString(), targetType, bottomType});
            }
          }
        }
      }
      if(!isPointerPointer) {
          return info.javaType();
      }
    }

    if (cType.isInt() || cType.isEnum()) {
      switch ((int) cType.getSize(curMachDesc)) {
       case 1:  return javaType(Byte.TYPE);
       case 2:  return javaType(Short.TYPE);
       case 4:  return javaType(Integer.TYPE);
       case 8:  return javaType(Long.TYPE);
       default: throw new RuntimeException("Unknown integer type of size " +
                                           cType.getSize(curMachDesc) + " and name " + cType.getName());
      }
    } else if (cType.isFloat()) {
      return javaType(Float.TYPE);
    } else if (cType.isDouble()) {
      return javaType(Double.TYPE);
    } else if (cType.isVoid()) {
      return javaType(Void.TYPE);
    } else if (cType.pointerDepth() > 0 || cType.arrayDimension() > 0) {
        Type targetType; // target type
        if (cType.isPointer()) {
          // t is <type>*, we need to get <type>
          targetType = cType.asPointer().getTargetType();
        } else {
          // t is <type>[], we need to get <type>
          targetType = cType.asArray().getBaseElementType();
        }

        // Handle Types of form pointer-to-type or array-of-type, like
        // char* or int[]; these are expanded out into Java primitive
        // arrays, NIO buffers, or both in expandMethodBinding
        if (cType.pointerDepth() == 1 || cType.arrayDimension() == 1) {
          if (targetType.isVoid()) {
            return JavaType.createForCVoidPointer();
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
              default: throw new RuntimeException("Unknown integer array type of size " +
                                                  cType.getSize(curMachDesc) + " and name " + cType.getName()+", "+cType.getDebugString());
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
            String name = targetType.getName();
            if (name == null) {
              // Try containing pointer type for any typedefs
              name = cType.getName();
              if (name == null) {
                throw new RuntimeException("Couldn't find a proper type name for pointer type " + cType.getDebugString());
              }
            }

            return JavaType.createForCStruct(cfg.renameJavaType(name));
          } else {
            throw new RuntimeException("Don't know how to convert pointer/array type \"" +
                                       cType.getDebugString() + "\"");
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
                LOG.log(INFO, "typeToJavaType(ptr-ptr): {0}, targetType: {1}, bottomType: {2}", new Object[]{cType.getDebugString(), targetType, bottomType});
            }
            return JavaType.forNIOPointerBufferClass();
          } else if(targetType.isArray()) {
            // t is<type>[][], targetType is <type>[], we need to get <type>
            bottomType = targetType.asArray().getBaseElementType();
            if( GlueGen.debug() ) {
                LOG.log(INFO, "typeToJavaType(ptr-ptr.array): {0}, targetType: {1}, bottomType: {2}", new Object[]{cType.getDebugString(), targetType, bottomType});
            }
          } else {
            bottomType = targetType;
            if( GlueGen.debug() ) {
                LOG.log(INFO, "typeToJavaType(ptr-ptr.primitive): {0}, targetType: {1}, bottomType: {2}", new Object[]{cType.getDebugString(), targetType, bottomType});
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
                default: throw new RuntimeException("Unknown two-dimensional integer array type of element size " +
                                                    bottomType.getSize(curMachDesc) + " and name " + bottomType.getName()+", "+bottomType.getDebugString());
              }
            } else if (bottomType.isFloat()) {
              return javaType(ArrayTypes.floatBufferArrayClass);
            } else if (bottomType.isDouble()) {
              return javaType(ArrayTypes.doubleBufferArrayClass);
            } else {
              throw new RuntimeException("Unexpected primitive type " + bottomType.getDebugString() +
                                         " in two-dimensional array");
            }
          } else if (bottomType.isVoid()) {
            return javaType(ArrayTypes.bufferArrayClass);
          } else if (targetType.isPointer() && (targetType.pointerDepth() == 1) &&
                     targetType.asPointer().getTargetType().isCompound()) {
            // Array of pointers; convert as array of StructAccessors
            return JavaType.createForCArray(bottomType);
          } else {
            throw new RuntimeException(
              "Could not convert C type \"" + cType.getDebugString() + "\" " +
              "to appropriate Java type; need to add more support for " +
              "depth=2 pointer/array types [debug info: targetType=\"" +
              targetType + "\"]");
          }
        } else {
          // can't handle this type of pointer/array argument
          throw new RuntimeException(
            "Could not convert C pointer/array \"" + cType.getDebugString() + "\" to " +
            "appropriate Java type; types with pointer/array depth " +
            "greater than 2 are not yet supported [debug info: " +
            "pointerDepth=" + cType.pointerDepth() + " arrayDimension=" +
            cType.arrayDimension() + " targetType=\"" + targetType + "\"]");
        }

    } else if(cType.isCompound() ) { // FIXME: Compound and Compound-Arrays
        final String name = cType.getName();
        if (name == null) {
          throw new RuntimeException("Couldn't find a proper type name for pointer type " + cType.getDebugString());
        }
        return JavaType.createForCStruct(cfg.renameJavaType(name));
    } else {
        throw new RuntimeException(
          "Could not convert C type \"" + cType.getDebugString() + "\" (class " +
          cType.getClass().getName() + ") to appropriate Java type");
    }
  }

  private static boolean isIntegerType(final Class<?> c) {
    return ((c == Byte.TYPE) ||
            (c == Short.TYPE) ||
            (c == Character.TYPE) ||
            (c == Integer.TYPE) ||
            (c == Long.TYPE));
  }

  private StructLayout getLayout() {
    if (layout == null) {
      layout = StructLayout.create(0);
    }
    return layout;
  }

  /**
   * @param filename the class's full filename to open w/ write access
   * @param simpleClassName the simple class name, i.e. w/o package name
   * @return a {@link PrintWriter} instance to write the class source file or <code>null</code> to suppress output!
   * @throws IOException
   */
  protected PrintWriter openFile(final String filename, final String simpleClassName) throws IOException {
    //System.out.println("Trying to open: " + filename);
    final File file = new File(filename);
    final String parentDir = file.getParent();
    if (parentDir != null) {
        new File(parentDir).mkdirs();
    }
    return new PrintWriter(new BufferedWriter(new FileWriter(file)));
  }

  private boolean isOpaque(final Type type) {
    return (cfg.typeInfo(type, typedefDictionary) != null);
  }

  private String compatiblePrimitiveJavaTypeName(final Type fieldType,
                                                 final JavaType javaType,
                                                 final MachineDescription curMachDesc) {
    final Class<?> c = javaType.getJavaClass();
    if (!isIntegerType(c)) {
      // FIXME
      throw new RuntimeException("Can't yet handle opaque definitions of structs' fields to non-integer types (byte, short, int, long, etc.): type: "+fieldType+", javaType "+javaType+", javaClass "+c);
    }
    switch ((int) fieldType.getSize(curMachDesc)) {
      case 1:  return "byte";
      case 2:  return "short";
      case 4:  return "int";
      case 8:  return "long";
      default: throw new RuntimeException("Can't handle opaque definitions if the starting type isn't compatible with integral types");
    }
  }

  private void openWriters() throws IOException {
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
      javaWriter = openFile(jRoot + File.separator + cfg.className() + ".java", cfg.className());
    }
    if (!cfg.allStatic() && cfg.emitImpl()) {
      javaImplWriter = openFile(jImplRoot + File.separator + cfg.implClassName() + ".java", cfg.implClassName());
    }
    if (cfg.emitImpl()) {
      cWriter = openFile(nRoot + File.separator + cfg.implClassName() + "_JNI.c", cfg.implClassName());
    }

    if (javaWriter != null) {
      CodeGenUtils.emitAutogeneratedWarning(javaWriter, this);
    }
    if (javaImplWriter != null) {
      CodeGenUtils.emitAutogeneratedWarning(javaImplWriter, this);
    }
    if (cWriter != null) {
      CodeGenUtils.emitAutogeneratedWarning(cWriter, this);
    }
  }

  protected PrintWriter javaWriter() {
    if (!cfg.allStatic() && !cfg.emitInterface()) {
      throw new InternalError("Should not call this");
    }
    return javaWriter;
  }

  protected PrintWriter javaImplWriter() {
    if (cfg.allStatic() || !cfg.emitImpl()) {
      throw new InternalError("Should not call this");
    }
    return javaImplWriter;
  }

  protected PrintWriter cWriter() {
    if (!cfg.emitImpl()) {
      throw new InternalError("Should not call this");
    }
    return cWriter;
  }

  private void closeWriter(final PrintWriter writer) throws IOException {
    writer.flush();
    writer.close();
  }

  private void closeWriters() throws IOException {
    if (javaWriter != null) {
      closeWriter(javaWriter);
    }
    if (javaImplWriter != null) {
      closeWriter(javaImplWriter);
    }
    if (cWriter != null) {
      closeWriter(cWriter);
    }
    javaWriter = null;
    javaImplWriter = null;
    cWriter = null;
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
  protected void emitCustomJavaCode(final PrintWriter writer, final String className) throws Exception  {
    final List<String> code = cfg.customJavaCodeForClass(className);
    if (code.isEmpty())
      return;

    writer.println();
    writer.println("  // --- Begin CustomJavaCode .cfg declarations");
    for (final String line : code) {
      writer.println(line);
    }
    writer.println("  // ---- End CustomJavaCode .cfg declarations");
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

        String[] accessModifiers = null;
        if(cfg.accessControl(cfg.className()) == PUBLIC_ABSTRACT) {
            accessModifiers = new String[] { "public", "abstract" };
        } else {
            accessModifiers = new String[] { "public" };
        }

        CodeGenUtils.emitJavaHeaders(
          javaWriter,
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

        String[] accessModifiers = null;
        if(cfg.accessControl(cfg.implClassName()) == PUBLIC_ABSTRACT) {
            accessModifiers = new String[] { "public", "abstract" };
        } else {
            accessModifiers = new String[] { "public" };
        }

        CodeGenUtils.emitJavaHeaders(
          javaImplWriter,
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
        emitCHeader(cWriter(), getImplPackageName(), cfg.implClassName());
      }
    } catch (final Exception e) {
      throw new RuntimeException(
        "Error emitting all file headers: cfg.allStatic()=" + cfg.allStatic() +
        " cfg.emitImpl()=" + cfg.emitImpl() + " cfg.emitInterface()=" + cfg.emitInterface(),
        e);
    }

  }

  protected void emitCHeader(final PrintWriter cWriter, final String packageName, final String className) {
    cWriter.println("#include <jni.h>");
    cWriter.println("#include <stdlib.h>");
    cWriter.println("#include <string.h>");
    cWriter.println();

    if (getConfig().emitImpl()) {
      cWriter.println("#include <assert.h>");
      cWriter.println();
      cWriter.println("static jobject JVMUtil_NewDirectByteBufferCopy(JNIEnv *env, void * source_address, jlong capacity); /* forward decl. */");
      cWriter.println();
    }
    for (final String code : cfg.customCCode()) {
      cWriter.println(code);
    }
    cWriter.println();
  }

  private static final String staticClassInitCodeCCode = "\n"+
         "static const char * clazzNameBuffers = \"com/jogamp/common/nio/Buffers\";\n"+
         "static const char * clazzNameBuffersStaticNewCstrName = \"newDirectByteBuffer\";\n"+
         "static const char * clazzNameBuffersStaticNewCstrSignature = \"(I)Ljava/nio/ByteBuffer;\";\n"+
         "static jclass clazzBuffers = NULL;\n"+
         "static jmethodID cstrBuffersNew = NULL;\n"+
         "static jboolean _initClazzAccessDone = JNI_FALSE;\n"+
         "\n"+
         "static jboolean _initClazzAccess(JNIEnv *env) {\n"+
         "    jclass c;\n"+
         "\n"+
         "    if(NULL!=cstrBuffersNew) return JNI_TRUE;\n"+
         "\n"+
         "    c = (*env)->FindClass(env, clazzNameBuffers);\n"+
         "    if(NULL==c) {\n"+
         "        fprintf(stderr, \"FatalError: Can't find %s\\n\", clazzNameBuffers);\n"+
         "        (*env)->FatalError(env, clazzNameBuffers);\n"+
         "        return JNI_FALSE;\n"+
         "    }\n"+
         "    clazzBuffers = (jclass)(*env)->NewGlobalRef(env, c);\n"+
         "    if(NULL==clazzBuffers) {\n"+
         "        fprintf(stderr, \"FatalError: Can't use %s\\n\", clazzNameBuffers);\n"+
         "        (*env)->FatalError(env, clazzNameBuffers);\n"+
         "        return JNI_FALSE;\n"+
         "    }\n"+
         "\n"+
         "    cstrBuffersNew = (*env)->GetStaticMethodID(env, clazzBuffers,\n"+
         "                            clazzNameBuffersStaticNewCstrName, clazzNameBuffersStaticNewCstrSignature);\n"+
         "    if(NULL==cstrBuffersNew) {\n"+
         "        fprintf(stderr, \"FatalError: can't create %s.%s %s\\n\",\n"+
         "            clazzNameBuffers,\n"+
         "            clazzNameBuffersStaticNewCstrName, clazzNameBuffersStaticNewCstrSignature);\n"+
         "        (*env)->FatalError(env, clazzNameBuffersStaticNewCstrName);\n"+
         "        return JNI_FALSE;\n"+
         "    }\n"+
         "    _initClazzAccessDone = JNI_TRUE;\n"+
         "    return JNI_TRUE;\n"+
         "}\n"+
         "\n"+
         "static jobject JVMUtil_NewDirectByteBufferCopy(JNIEnv *env, void * source_address, jlong capacity) {\n"+
         "    jobject jbyteBuffer;\n"+
         "    void * byteBufferPtr;\n"+
         "\n"+
         "    if( JNI_FALSE == _initClazzAccessDone ) {\n"+
         "        fprintf(stderr, \"FatalError: initializeImpl() not called\\n\");\n"+
         "        (*env)->FatalError(env, \"initializeImpl() not called\");\n"+
         "        return NULL;\n"+
         "    }\n"+
         "    jbyteBuffer  = (*env)->CallStaticObjectMethod(env, clazzBuffers, cstrBuffersNew, capacity);\n"+
         "    byteBufferPtr = (*env)->GetDirectBufferAddress(env, jbyteBuffer);\n"+
         "    memcpy(byteBufferPtr, source_address, capacity);\n"+
         "    return jbyteBuffer;\n"+
         "}\n"+
         "\n";

  private static final String staticClassInitCallJavaCode = "\n"+
         "  static {\n"+
         "    if( !initializeImpl() ) {\n"+
         "      throw new RuntimeException(\"Initialization failure\");\n"+
         "    }\n"+
         "  }\n"+
         "\n";

  protected void emitCInitCode(final PrintWriter cWriter, final String packageName, final String className) {
    if ( requiresStaticInitialization(className) ) {
      cWriter.println(staticClassInitCodeCCode);
      cWriter.println("JNIEXPORT jboolean JNICALL "+JavaEmitter.getJNIMethodNamePrefix(packageName, className)+"_initializeImpl(JNIEnv *env, jclass _unused) {");
      cWriter.println("    return _initClazzAccess(env);");
      cWriter.println("}");
      cWriter.println();
      cWriter.println("JNIEXPORT jint JNICALL "+JavaEmitter.getJNIMethodNamePrefix(packageName, className)+"_getCStringLengthImpl(JNIEnv *env, jclass _unused, jlong pString) {");
      cWriter.println("    return 0 != pString ? strlen((const char*)(intptr_t)pString) : 0;");
      cWriter.println("}");
      cWriter.println();
    }
  }

  protected void emitJavaInitCode(final PrintWriter jWriter, final String className) {
    if( null != jWriter && requiresStaticInitialization(className) ) {
        jWriter.println();
        jWriter.println("  private static native boolean initializeImpl();");
        jWriter.println();
        jWriter.println();
        jWriter.println("  private static native int getCStringLengthImpl(final long pString);");
        jWriter.println();
        if( !cfg.manualStaticInitCall(className) ) {
            jWriter.println(staticClassInitCallJavaCode);
        }
    }
  }

  /**
   * Write out any footer information for the output files (closing brace of
   * class definition, etc).
   */
  protected void emitAllFileFooters() {
    if (cfg.allStatic() || cfg.emitInterface()) {
      javaWriter().println();
      javaWriter().println("} // end of class " + cfg.className());
    }
    if (!cfg.allStatic() && cfg.emitImpl())  {
      javaImplWriter().println();
      javaImplWriter().println("} // end of class " + cfg.implClassName());
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
  private MethodBinding bindFunction(final FunctionSymbol sym,
                                     final JavaType containingType,
                                     final Type containingCType,
                                     final MachineDescription curMachDesc) {

    final MethodBinding binding = new MethodBinding(sym, containingType, containingCType);

    binding.renameMethodName(cfg.getJavaSymbolRename(sym.getName()));

    // System.out.println("bindFunction(0) "+sym.getReturnType());

    if (cfg.returnsString(binding.getName())) {
      final PointerType prt = sym.getReturnType().asPointer();
      if (prt == null ||
          prt.getTargetType().asInt() == null ||
          prt.getTargetType().getSize(curMachDesc) != 1) {
        throw new RuntimeException(
          "Cannot apply ReturnsString configuration directive to \"" + sym +
          "\". ReturnsString requires native method to have return type \"char *\"");
      }
      binding.setJavaReturnType(javaType(java.lang.String.class));
    } else {
      binding.setJavaReturnType(typeToJavaType(sym.getReturnType(), curMachDesc));
    }

    // System.out.println("bindFunction(1) "+binding.getJavaReturnType());

    // List of the indices of the arguments in this function that should be
    // converted from byte[] or short[] to String
    final List<Integer> stringArgIndices = cfg.stringArguments(binding.getName());

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
        throw new RuntimeException(
          "Cannot apply ArgumentIsString configuration directive to " +
          "argument " + i + " of \"" + sym + "\": argument type is not " +
          "a \"void*\", \"char *\", \"short *\", \"char**\", or \"short**\" equivalent");
        }
      }
      binding.addJavaArgumentType(mappedType);
      //System.out.println("During binding of [" + sym + "], added mapping from C type: " + cArgType + " to Java type: " + mappedType);
    }

    // System.out.println("---> " + binding);
    // System.out.println("    ---> " + binding.getCSymbol());
    // System.out.println("bindFunction(3) "+binding);
    return binding;
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
          throw new RuntimeException("Unknown C pointer type " + t);
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
        throw new RuntimeException("Unknown C pointer type " + t);
      }
    }

    // System.out.println("lowerMethodBindingPointerTypes(2): "+result);

    if (canProduceArrayVariant != null) {
      canProduceArrayVariant[0] = arrayPossible;
    }

    return result;
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
    }
    canonMap.put(t, t);
    return t;
  }

  /**
   * Converts first letter to upper case.
   */
  private final String capitalizeString(final String string) {
      return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }
}
