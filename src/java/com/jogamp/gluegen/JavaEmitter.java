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

  /**
   * Style of code emission. Can emit everything into one class
   * (AllStatic), separate interface and implementing classes
   * (InterfaceAndImpl), only the interface (InterfaceOnly), or only
   * the implementation (ImplOnly).
   */
  public enum EmissionStyle {AllStatic, InterfaceAndImpl, InterfaceOnly, ImplOnly};

  /**
   * Access control for emitted Java methods.
   */
  public enum MethodAccess {
      PUBLIC("public"), PROTECTED("protected"), PRIVATE("private"), PACKAGE_PRIVATE("/* pp */"), PUBLIC_ABSTRACT("abstract");

      public final String getJavaName() { return javaName; }

      MethodAccess(String javaName) {
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
  public void readConfigurationFile(String filename) throws Exception {
    cfg = createConfig();
    cfg.read(filename);
  }

  class ConstantRenamer implements SymbolFilter {

    private List<ConstantDefinition> constants;

    @Override
    public void filterSymbols(List<ConstantDefinition> constants, List<FunctionSymbol> functions) {
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
      List<ConstantDefinition> newConstants = new ArrayList<ConstantDefinition>();
      JavaConfiguration cfg = getConfig();
      for (ConstantDefinition def : constants) {
        def.rename(cfg.getJavaSymbolRename(def.getName()));
        newConstants.add(def);
      }
      constants = newConstants;
    }
  }

    @Override
    public void beginEmission(GlueEmitterControls controls) throws IOException {

        // Request emission of any structs requested
        for (String structs : cfg.forcedStructs()) {
            controls.forceStructEmission(structs);
        }

        if (!cfg.structsOnly()) {
            try {
                openWriters();
            } catch (Exception e) {
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
            } catch (Exception e) {
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

  protected static int getJavaRadix(String name, String value)  {
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
    } catch (NumberFormatException e) {
      try {
        // see if it's a double or float
        Double.parseDouble(value);
        return 10;
      } catch (NumberFormatException e2) {
        throw new RuntimeException(
          "Cannot emit define \""+name+"\": value \""+value+
          "\" cannot be assigned to a int, long, float, or double", e2);
      }
    }
  }

  protected static Object getJavaValue(String name, String value) {

    // "calculates" the result type of a simple expression
    // example: (2+3)-(2.0f-3.0) -> Double
    // example: (1 << 2) -> Integer

    Scanner scanner = new Scanner(value).useDelimiter("[+-/*/></(/)]");

    Object resultType = null;

    while (scanner.hasNext()) {

        String t = scanner.next().trim();

        if(0<t.length()) {
            Object type = getJavaValue2(name, t);

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

  private static Object getJavaValue2(String name, String value) {
    // FIXME: need to handle when type specifier is in last char (e.g.,
    // "1.0d or 2759L", because parseXXX() methods don't allow the type
    // specifier character in the string.
    //
    char lastChar = value.charAt(value.length()-1);

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
      long longVal = Long.parseLong(parseValue, radix);
      // if constant is small enough, store it as an int instead of a long
      if (longVal > Integer.MIN_VALUE && longVal < Integer.MAX_VALUE) {
        return (int)longVal;
      }
      return longVal;

    } catch (NumberFormatException e) {
      try {
        // see if it's a double or float
        double dVal = Double.parseDouble(value);
        double absVal = Math.abs(dVal);
        // if constant is small enough, store it as a float instead of a double
        if (absVal < Float.MIN_VALUE || absVal > Float.MAX_VALUE) {
            return new Double(dVal);
        }
        return new Float((float) dVal);
      } catch (NumberFormatException e2) {
        throw new RuntimeException(
          "Cannot emit define \""+name+"\": value \""+value+
          "\" cannot be assigned to a int, long, float, or double", e2);
      }
    }
  }


  protected static String getJavaType(String name, String value) {
    Object oval = getJavaValue(name, value);
    return getJavaType(name, oval);
  }

  protected static String getJavaType(String name, Object oval) {
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

  @Override
  public void emitDefine(ConstantDefinition def, String optionalComment) throws Exception  {

    if (cfg.allStatic() || cfg.emitInterface()) {
      // TODO: Some defines (e.g., GL_DOUBLE_EXT in gl.h) are defined in terms
      // of other defines -- should we emit them as references to the original
      // define (not even sure if the lexer supports this)? Right now they're
      // emitted as the numeric value of the original definition. If we decide
      // emit them as references we'll also have to emit them in the correct
      // order. It's probably not an issue right now because the emitter
      // currently only emits only numeric defines -- if it handled #define'd
      // objects it would make a bigger difference.

      String name = def.getName();
      String value = def.getValue();

      if (!cfg.shouldIgnoreInInterface(name)) {
        String type = getJavaType(name, value);
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
  public void beginFunctions(TypeDictionary typedefDictionary,
                             TypeDictionary structDictionary,
                             Map<Type, Type> canonMap) throws Exception {

    this.typedefDictionary = typedefDictionary;
    this.canonMap          = canonMap;

    if ((cfg.allStatic() || cfg.emitInterface()) && !cfg.structsOnly()) {
      javaWriter().println();
    }
  }

  @Override
  public Iterator<FunctionSymbol> emitFunctions(List<FunctionSymbol> originalCFunctions) throws Exception {

    // Sometimes headers will have the same function prototype twice, once
    // with the argument names and once without. We'll remember the signatures
    // we've already processed we don't generate duplicate bindings.
    //
    // Note: this code assumes that on the equals() method in FunctionSymbol
    // only considers function name and argument types (i.e., it does not
    // consider argument *names*) when comparing FunctionSymbols for equality
    Set<FunctionSymbol> funcsToBindSet = new HashSet<FunctionSymbol>(100);
    for (FunctionSymbol cFunc : originalCFunctions) {
      if (!funcsToBindSet.contains(cFunc)) {
        funcsToBindSet.add(cFunc);
      }
    }

    //    validateFunctionsToBind(funcsToBindSet);

    ArrayList<FunctionSymbol> funcsToBind = new ArrayList<FunctionSymbol>(funcsToBindSet);
    // sort functions to make them easier to find in native code
    Collections.sort(funcsToBind, new Comparator<FunctionSymbol>() {
            @Override
            public int compare(FunctionSymbol o1, FunctionSymbol o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

    // Bind all the C funcs to Java methods
    HashSet<MethodBinding> methodBindingSet = new HashSet<MethodBinding>();
    ArrayList<FunctionEmitter> methodBindingEmitters = new ArrayList<FunctionEmitter>(2*funcsToBind.size());
    for (FunctionSymbol cFunc : funcsToBind) {
      // Check to see whether this function should be ignored
      if (!cfg.shouldIgnoreInImpl(cFunc.getName())) {
          methodBindingEmitters.addAll(generateMethodBindingEmitters(methodBindingSet, cFunc));
      }

    }

    // Emit all the methods
    for (FunctionEmitter emitter : methodBindingEmitters) {
      try {
        if (!emitter.isInterface() || !cfg.shouldIgnoreInInterface(emitter.getName())) {
            emitter.emit();
            emitter.getDefaultOutput().println(); // put newline after method body
        }
      } catch (Exception e) {
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
   * Generates the public emitters for this MethodBinding which will
   * produce either simply signatures (for the interface class, if
   * any) or function definitions with or without a body (depending on
   * whether or not the implementing function can go directly to
   * native code because it doesn't need any processing of the
   * outgoing arguments).
   */
  protected void generatePublicEmitters(MethodBinding binding,
                                        List<FunctionEmitter> allEmitters,
                                        boolean signatureOnly) {
    if (cfg.manuallyImplement(binding.getName()) && !signatureOnly) {
      // We only generate signatures for manually-implemented methods;
      // user provides the implementation
      return;
    }

    MethodAccess accessControl = cfg.accessControl(binding.getName());
    // We should not emit anything except public APIs into interfaces
    if (signatureOnly && (accessControl != PUBLIC)) {
      return;
    }

    final PrintWriter writer = ((signatureOnly || cfg.allStatic()) ? javaWriter() : javaImplWriter());

    // It's possible we may not need a body even if signatureOnly is
    // set to false; for example, if the routine doesn't take any
    // arrays or buffers as arguments
    boolean isUnimplemented = cfg.isUnimplemented(binding.getName());
    List<String> prologue = cfg.javaPrologueForMethod(binding, false, false);
    List<String> epilogue = cfg.javaEpilogueForMethod(binding, false, false);
    boolean needsBody = (isUnimplemented ||
                         (binding.needsNIOWrappingOrUnwrapping() ||
                          binding.signatureUsesJavaPrimitiveArrays()) ||
                         (prologue != null) ||
                         (epilogue != null));

    JavaMethodBindingEmitter emitter =
      new JavaMethodBindingEmitter(binding,
                                   writer,
                                   cfg.runtimeExceptionType(),
                                   cfg.unsupportedExceptionType(),
                                   !signatureOnly && needsBody,
                                   cfg.tagNativeBinding(),
                                   false,
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
      emitter.addModifier(JavaMethodBindingEmitter.STATIC);
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
  protected void generatePrivateEmitters(MethodBinding binding,
                                         List<FunctionEmitter> allEmitters) {
    if (cfg.manuallyImplement(binding.getName())) {
      // Don't produce emitters for the implementation class
      return;
    }

    boolean hasPrologueOrEpilogue =
        ((cfg.javaPrologueForMethod(binding, false, false) != null) ||
         (cfg.javaEpilogueForMethod(binding, false, false) != null));

    // If we already generated a public native entry point for this
    // method, don't emit another one
    if (!cfg.isUnimplemented(binding.getName()) &&
        (binding.needsNIOWrappingOrUnwrapping() ||
         binding.signatureUsesJavaPrimitiveArrays() ||
         hasPrologueOrEpilogue)) {
      PrintWriter writer = (cfg.allStatic() ? javaWriter() : javaImplWriter());

      // If the binding uses primitive arrays, we are going to emit
      // the private native entry point for it along with the version
      // taking only NIO buffers
      if (!binding.signatureUsesJavaPrimitiveArrays()) {
        // (Always) emit the entry point taking only direct buffers
        JavaMethodBindingEmitter emitter =
          new JavaMethodBindingEmitter(binding,
                                       writer,
                                       cfg.runtimeExceptionType(),
                                       cfg.unsupportedExceptionType(),
                                       false,
                                       cfg.tagNativeBinding(),
                                       true,
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
          emitter.addModifier(JavaMethodBindingEmitter.STATIC);
        }
        emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
        emitter.setReturnedArrayLengthExpression(cfg.returnedArrayLength(binding.getName()));
        allEmitters.add(emitter);
      }
    }

    // Now generate the C emitter(s). We need to produce one for every
    // Java native entry point (public or private). The only
    // situations where we don't produce one are (a) when the method
    // is unimplemented, and (b) when the signature contains primitive
    // arrays, since the latter is handled by the method binding
    // variant taking only NIO Buffers.
    if (!cfg.isUnimplemented(binding.getName()) &&
        !binding.signatureUsesJavaPrimitiveArrays()) {
      CMethodBindingEmitter cEmitter;
      // Generate a binding without mixed access (NIO-direct, -indirect, array)
      cEmitter =
          new CMethodBindingEmitter(binding,
                                    cWriter(),
                                    cfg.implPackageName(),
                                    cfg.implClassName(),
                                    true, // NOTE: we always disambiguate with a suffix now, so this is optional
                                    cfg.allStatic(),
                                    (binding.needsNIOWrappingOrUnwrapping() || hasPrologueOrEpilogue),
                                    !cfg.useNIODirectOnly(binding.getName()),
                                    machDescJava);
      prepCEmitter(binding, cEmitter);
      allEmitters.add(cEmitter);
    }
  }

  protected void prepCEmitter(MethodBinding binding, CMethodBindingEmitter cEmitter)
  {
      // See whether we need an expression to help calculate the
      // length of any return type
      JavaType javaReturnType = binding.getJavaReturnType();
      if (javaReturnType.isNIOBuffer() ||
          javaReturnType.isCompoundTypeWrapper()) {
        // See whether capacity has been specified
        String capacity = cfg.returnValueCapacity(binding.getName());
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
        String len = cfg.returnValueLength(binding.getName());
        if (len != null) {
          cEmitter.setReturnValueLengthExpression( new MessageFormat(len) );
        }
      }
      cEmitter.setTemporaryCVariableDeclarations(cfg.temporaryCVariableDeclarations(binding.getName()));
      cEmitter.setTemporaryCVariableAssignments(cfg.temporaryCVariableAssignments(binding.getName()));
  }

  /**
   * Generate all appropriate Java bindings for the specified C function
   * symbols.
   */
  protected List<? extends FunctionEmitter> generateMethodBindingEmitters(Set<MethodBinding> methodBindingSet, FunctionSymbol sym) throws Exception {

    ArrayList<FunctionEmitter> allEmitters = new ArrayList<FunctionEmitter>();

    try {
      // Get Java binding for the function
      MethodBinding mb = bindFunction(sym, null, null, machDescJava);

      // JavaTypes representing C pointers in the initial
      // MethodBinding have not been lowered yet to concrete types
      List<MethodBinding> bindings = expandMethodBinding(mb);

      for (MethodBinding binding : bindings) {

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
    } catch (Exception e) {
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
    }
  }

  @Override
  public void beginStructLayout() throws Exception {}
  @Override
  public void layoutStruct(CompoundType t) throws Exception {
    getLayout().layout(t);
  }
  @Override
  public void endStructLayout() throws Exception {}

  @Override
  public void beginStructs(TypeDictionary typedefDictionary,
                           TypeDictionary structDictionary,
                           Map<Type, Type> canonMap) throws Exception {
    this.typedefDictionary = typedefDictionary;
    this.canonMap          = canonMap;
  }

  @Override
  public void emitStruct(CompoundType structType, String alternateName) throws Exception {
    String name = structType.getName();
    if (name == null && alternateName != null) {
      name = alternateName;
    }

    if (name == null) {
        if ((structType.getStructName() != null) && cfg.shouldIgnoreInInterface(structType.getStructName()))
            return;

        LOG.log(WARNING, "skipping emission of unnamed struct \"{0}\"", structType);
        return;
    }

    if (cfg.shouldIgnoreInInterface(name)) {
      return;
    }

    Type containingCType = canonicalize(new PointerType(SizeThunk.POINTER, structType, 0));
    JavaType containingType = typeToJavaType(containingCType, false, null);
    if (!containingType.isCompoundTypeWrapper()) {
      return;
    }
    String containingTypeName = containingType.getName();

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
    for (int i = 0; i < structType.getNumFields(); i++) {
      if (structType.getField(i).getType().isFunctionPointer()) {
        needsNativeCode = true;
        break;
      }
    }

    String structClassPkg = cfg.packageForStruct(name);
    PrintWriter writer = null;
    PrintWriter newWriter = null;
    try  {
      writer = openFile(
        cfg.javaOutputDir() + File.separator +
        CodeGenUtils.packageAsPath(structClassPkg) +
        File.separator + containingTypeName + ".java", containingTypeName);
      CodeGenUtils.emitAutogeneratedWarning(writer, this);
      if (needsNativeCode) {
        String nRoot = cfg.nativeOutputDir();
        if (cfg.nativeOutputUsesJavaHierarchy()) {
          nRoot += File.separator + CodeGenUtils.packageAsPath(cfg.packageName());
        }
        newWriter = openFile(nRoot + File.separator + containingTypeName + "_JNI.c", containingTypeName);
        CodeGenUtils.emitAutogeneratedWarning(newWriter, this);
        emitCHeader(newWriter, containingTypeName);
      }
    } catch(Exception e)   {
      throw new RuntimeException("Unable to open files for emission of struct class", e);
    }

    writer.println();
    writer.println("package " + structClassPkg + ";");
    writer.println();
    writer.println("import java.nio.*;");
    writer.println();

    writer.println("import " + cfg.gluegenRuntimePackage() + ".*;");
    writer.println("import " + DynamicLookupHelper.class.getPackage().getName() + ".*;");
    writer.println("import " + Buffers.class.getPackage().getName() + ".*;");
    writer.println("import " + MachineDescriptionRuntime.class.getName() + ";");
    writer.println();
    List<String> imports = cfg.imports();
    for (String str : imports) {
      writer.print("import ");
      writer.print(str);
      writer.println(";");
    }
    writer.println();
    List<String> javadoc = cfg.javadocForClass(containingTypeName);
    for (String doc : javadoc) {
      writer.println(doc);
    }
    writer.print("public class " + containingTypeName + " ");
    boolean firstIteration = true;
    List<String> userSpecifiedInterfaces = cfg.implementedInterfaces(containingTypeName);
    for (String userInterface : userSpecifiedInterfaces) {
      if (firstIteration) {
        writer.print("implements ");
      }
      firstIteration = false;
      writer.print(userInterface);
      writer.print(" ");
    }
    writer.println("{");
    writer.println();
    writer.println("  StructAccessor accessor;");
    writer.println();
    writer.println("  private static final int mdIdx = MachineDescriptionRuntime.getStatic().ordinal();");
    writer.println();
    // generate all offset and size arrays
    generateOffsetAndSizeArrays(writer, containingTypeName, structType, null); /* w/o offset */
    for (int i = 0; i < structType.getNumFields(); i++) {
      final Field field = structType.getField(i);
      final Type fieldType = field.getType();

      if (!cfg.shouldIgnoreInInterface(name + " " + field.getName())) {
        final String renamed = cfg.getJavaSymbolRename(field.getName());
        final String fieldName = renamed==null ? field.getName() : renamed;
        if (fieldType.isFunctionPointer()) {
           // no offset/size for function pointer ..
        } else if (fieldType.isCompound()) {
          // FIXME: will need to support this at least in order to
          // handle the union in jawt_Win32DrawingSurfaceInfo (fabricate
          // a name?)
          if (fieldType.getName() == null) {
            throw new RuntimeException("Anonymous structs as fields not supported yet (field \"" +
                                       field + "\" in type \"" + name + "\")");
          }

          generateOffsetAndSizeArrays(writer, fieldName, fieldType, field);
        } else if (fieldType.isArray()) {
            Type baseElementType = field.getType().asArray().getBaseElementType();

            if(!baseElementType.isPrimitive())
                break;

            generateOffsetAndSizeArrays(writer, fieldName, null, field); /* w/o size */
        } else {
          JavaType externalJavaType = null;
          try {
            externalJavaType = typeToJavaType(fieldType, false, machDescJava);
          } catch (Exception e) {
            System.err.println("Error occurred while creating accessor for field \"" +
                               field.getName() + "\" in type \"" + name + "\"");
            throw(e);
          }
          if (externalJavaType.isPrimitive()) {
            // Primitive type
            generateOffsetAndSizeArrays(writer, fieldName, null, field); /* w/o size */
          } else {
            // FIXME
            LOG.log(WARNING, "Complicated fields (field \"{0}\" of type \"{1}\") not implemented yet", new Object[]{field, name});
            //          throw new RuntimeException("Complicated fields (field \"" + field + "\" of type \"" + t +
            //                                     "\") not implemented yet");
          }
        }
      }
    }
    writer.println();

    writer.println("  public static int size() {");
    writer.println("    return "+containingTypeName+"_size[mdIdx];");
    writer.println("  }");
    writer.println();
    writer.println("  public static " + containingTypeName + " create() {");
    writer.println("    return create(Buffers.newDirectByteBuffer(size()));");
    writer.println("  }");
    writer.println();
    writer.println("  public static " + containingTypeName + " create(java.nio.ByteBuffer buf) {");
    writer.println("      return new " + containingTypeName + "(buf);");
    writer.println("  }");
    writer.println();
    writer.println("  " + containingTypeName + "(java.nio.ByteBuffer buf) {");
    writer.println("    accessor = new StructAccessor(buf);");
    writer.println("  }");
    writer.println();
    writer.println("  public java.nio.ByteBuffer getBuffer() {");
    writer.println("    return accessor.getBuffer();");
    writer.println("  }");

    for (int i = 0; i < structType.getNumFields(); i++) {
      final Field field = structType.getField(i);
      final Type fieldType = field.getType();

      if (!cfg.shouldIgnoreInInterface(name + " " + field.getName())) {
        final String renamed = cfg.getJavaSymbolRename(field.getName());
        final String fieldName = renamed==null ? field.getName() : renamed;

        if (fieldType.isFunctionPointer()) {
            try {
              // Emit method call and associated native code
              FunctionType   funcType     = fieldType.asPointer().getTargetType().asFunction();
              FunctionSymbol funcSym      = new FunctionSymbol(fieldName, funcType);
              MethodBinding  binding      = bindFunction(funcSym, containingType, containingCType, machDescJava);
              binding.findThisPointer(); // FIXME: need to provide option to disable this on per-function basis
              writer.println();

              // Emit public Java entry point for calling this function pointer
              JavaMethodBindingEmitter emitter =
                new JavaMethodBindingEmitter(binding,
                                             writer,
                                             cfg.runtimeExceptionType(),
                                             cfg.unsupportedExceptionType(),
                                             true,
                                             cfg.tagNativeBinding(),
                                             false,
                                             true, // FIXME: should unify this with the general emission code
                                             true, // FIXME: should unify this with the general emission code
                                             false,
                                             false, // FIXME: should unify this with the general emission code
                                             false, // FIXME: should unify this with the general emission code
                                             false, // FIXME: should unify this with the general emission code
                                             false,
                                             cfg);
              emitter.addModifier(JavaMethodBindingEmitter.PUBLIC);
              emitter.emit();

              // Emit private native Java entry point for calling this function pointer
              emitter =
                new JavaMethodBindingEmitter(binding,
                                             writer,
                                             cfg.runtimeExceptionType(),
                                             cfg.unsupportedExceptionType(),
                                             false,
                                             cfg.tagNativeBinding(),
                                             true,
                                             true, // FIXME: should unify this with the general emission code
                                             true, // FIXME: should unify this with the general emission code
                                             true,
                                             true, // FIXME: should unify this with the general emission code
                                             false, // FIXME: should unify this with the general emission code
                                             false, // FIXME: should unify this with the general emission code
                                             false,
                                             cfg);
              emitter.addModifier(JavaMethodBindingEmitter.PRIVATE);
              emitter.addModifier(JavaMethodBindingEmitter.NATIVE);
              emitter.emit();

              // Emit (private) C entry point for calling this function pointer
              CMethodBindingEmitter cEmitter =
                new CMethodBindingEmitter(binding,
                                          newWriter,
                                          structClassPkg,
                                          containingTypeName,
                                          true, // FIXME: this is optional at this point
                                          false,
                                          true,
                                          false, // FIXME: should unify this with the general emission code
                                          machDescJava);
              prepCEmitter(binding, cEmitter);
              cEmitter.emit();
            } catch (Exception e) {
              System.err.println("While processing field " + field + " of type " + name + ":");
              throw(e);
            }
        } else if (fieldType.isCompound()) {
          // FIXME: will need to support this at least in order to
          // handle the union in jawt_Win32DrawingSurfaceInfo (fabricate
          // a name?)
          if (fieldType.getName() == null) {
            throw new RuntimeException("Anonymous structs as fields not supported yet (field \"" +
                                       field + "\" in type \"" + name + "\")");
          }

          writer.println();
          generateGetterSignature(writer, false, fieldType.getName(), capitalizeString(fieldName));
          writer.println(" {");
          writer.println("    return " + fieldType.getName() + ".create( accessor.slice( " +
                           fieldName+"_offset[mdIdx], "+fieldName+"_size[mdIdx] ) );");
          writer.println(" }");

        } else if (fieldType.isArray()) {

            Type baseElementType = field.getType().asArray().getBaseElementType();

            if(!baseElementType.isPrimitive())
                break;

            String paramType = typeToJavaType(baseElementType, false, machDescJava).getName();
            String capitalized = capitalizeString(fieldName);

            // Setter
            writer.println();
            generateSetterSignature(writer, false, containingTypeName, capitalized, paramType+"[]");
            writer.println(" {");
            writer.print  ("    accessor.set" + capitalizeString(paramType) + "sAt(" + fieldName+"_offset[mdIdx], val);");
            writer.println("    return this;");
            writer.println("  }");
            writer.println();
            // Getter
            generateGetterSignature(writer, false, paramType+"[]", capitalized);
            writer.println(" {");
            writer.print  ("    return accessor.get" + capitalizeString(paramType) + "sAt(" + fieldName+"_offset[mdIdx], new " +paramType+"["+fieldType.asArray().getLength()+"]);");
            writer.println(" }");
        } else {
          JavaType javaType = null;

          try {
            javaType = typeToJavaType(fieldType, false, machDescJava);
          } catch (Exception e) {
            System.err.println("Error occurred while creating accessor for field \"" +
                               field.getName() + "\" in type \"" + name + "\"");
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
                System.err.println("Java.StructEmitter.Primitive: "+field.getName()+", "+fieldType.getName(true)+", "+javaTypeName+", "+
                                   ", fixedSize "+fieldTypeNativeSizeFixed+", opaque "+isOpaque(fieldType)+", isPointer "+fieldType.isPointer()+", isCompound "+fieldType.isCompound()+
                                   ", sizeDenominator "+sizeDenominator);
            }

            writer.println();
            // Setter
            generateSetterSignature(writer, false, containingTypeName, capFieldName, javaTypeName);
            writer.println(" {");
            if( fieldTypeNativeSizeFixed ) {
                writer.println("    accessor.set" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx], val);");
            } else {
                writer.println("    accessor.set" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx], val, MachineDescriptionRuntime.getStatic().md."+sizeDenominator+"SizeInBytes());");
            }
            writer.println("    return this;");
            writer.println("  }");
            writer.println();

            // Getter
            generateGetterSignature(writer, false, javaTypeName, capFieldName);
            writer.println(" {");
            writer.print  ("    return ");
            if( fieldTypeNativeSizeFixed ) {
                writer.println("accessor.get" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx]);");
            } else {
                writer.println("accessor.get" + capJavaTypeName + "At(" + fieldName+"_offset[mdIdx], MachineDescriptionRuntime.getStatic().md."+sizeDenominator+"SizeInBytes());");
            }
            writer.println("  }");
          }
        }
      }
    }
    emitCustomJavaCode(writer, containingTypeName);
    writer.println("}");
    writer.flush();
    writer.close();
    if (needsNativeCode) {
      newWriter.flush();
      newWriter.close();
    }
  }
  @Override
  public void endStructs() throws Exception {}

  public static int addStrings2Buffer(StringBuilder buf, String sep, String first, Collection<String> col) {
    int num = 0;
    if(null==buf) {
        buf = new StringBuilder();
    }

    Iterator<String> iter = col.iterator();
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

  private void generateGetterSignature(PrintWriter writer, boolean abstractMethod, String returnTypeName, String capitalizedFieldName) {
      writer.print("  public " + (abstractMethod ? "abstract " : "") + returnTypeName + " get" + capitalizedFieldName + "()");
  }

  private void generateSetterSignature(PrintWriter writer, boolean abstractMethod, String returnTypeName, String capitalizedFieldName, String paramTypeName) {
      writer.print("  public " + (abstractMethod ? "abstract " : "") + returnTypeName + " set" + capitalizedFieldName + "(" + paramTypeName + " val)");
  }

  private void generateOffsetAndSizeArrays(PrintWriter writer, String fieldName, Type fieldType, Field field) {
      if(null != field) {
          writer.print("  private static final int[] "+fieldName+"_offset = new int[] { ");
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
          writer.print("  private static final int[] "+fieldName+"_size = new int[] { ");
          for( int i=0; i < machDescTargetConfigs.length; i++ ) {
              if(0<i) {
                  writer.print(", ");
              }
              writer.print(fieldType.getSize(machDescTargetConfigs[i].md) +
                           " /* " + machDescTargetConfigs[i].name() + " */");
          }
          writer.println("  };");
      }
  }

  private JavaType typeToJavaType(Type cType, boolean outgoingArgument, MachineDescription curMachDesc) {
    // Recognize JNIEnv* case up front
    PointerType opt = cType.asPointer();
    if ((opt != null) &&
        (opt.getTargetType().getName() != null) &&
        (opt.getTargetType().getName().equals("JNIEnv"))) {
      return JavaType.createForJNIEnv();
    }
    Type t = cType;

    // Opaque specifications override automatic conversions
    // in case the identity is being used .. not if ptr-ptr
    TypeInfo info = cfg.typeInfo(t, typedefDictionary);
    if (info != null) {
      boolean isPointerPointer = false;
      if (t.pointerDepth() > 0 || t.arrayDimension() > 0) {
        Type targetType; // target type
        if (t.isPointer()) {
          // t is <type>*, we need to get <type>
          targetType = t.asPointer().getTargetType();
        } else {
          // t is <type>[], we need to get <type>
          targetType = t.asArray().getElementType();
        }
        if (t.pointerDepth() == 2 || t.arrayDimension() == 2) {
          // Get the target type of the target type (targetType was computer earlier
          // as to be a pointer to the target type, so now we need to get its
          // target type)
          if (targetType.isPointer()) {
            isPointerPointer = true;

            // t is<type>**, targetType is <type>*, we need to get <type>
            Type bottomType = targetType.asPointer().getTargetType();
            LOG.log(INFO, "Opaque Type: {0}, targetType: {1}, bottomType: {2} is ptr-ptr", new Object[]{t, targetType, bottomType});
          }
        }
      }
      if(!isPointerPointer) {
          return info.javaType();
      }
    }

    if (t.isInt() || t.isEnum()) {
      switch ((int) t.getSize(curMachDesc)) {
       case 1:  return javaType(Byte.TYPE);
       case 2:  return javaType(Short.TYPE);
       case 4:  return javaType(Integer.TYPE);
       case 8:  return javaType(Long.TYPE);
       default: throw new RuntimeException("Unknown integer type of size " +
                                           t.getSize(curMachDesc) + " and name " + t.getName());
      }
    } else if (t.isFloat()) {
      return javaType(Float.TYPE);
    } else if (t.isDouble()) {
      return javaType(Double.TYPE);
    } else if (t.isVoid()) {
      return javaType(Void.TYPE);
    } else {
      if (t.pointerDepth() > 0 || t.arrayDimension() > 0) {
        Type targetType; // target type
        if (t.isPointer()) {
          // t is <type>*, we need to get <type>
          targetType = t.asPointer().getTargetType();
        } else {
          // t is <type>[], we need to get <type>
          targetType = t.asArray().getElementType();
        }

        // Handle Types of form pointer-to-type or array-of-type, like
        // char* or int[]; these are expanded out into Java primitive
        // arrays, NIO buffers, or both in expandMethodBinding
        if (t.pointerDepth() == 1 || t.arrayDimension() == 1) {
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
                                                  t.getSize(curMachDesc) + " and name " + t.getName());
            }
          } else if (targetType.isFloat()) {
            return JavaType.createForCFloatPointer();
          } else if (targetType.isDouble()) {
            return JavaType.createForCDoublePointer();
          } else if (targetType.isCompound()) {
            if (t.isArray()) {
              throw new RuntimeException("Arrays of compound types not handled yet");
            }
            // Special cases for known JNI types (in particular for converting jawt.h)
            if (t.getName() != null &&
                t.getName().equals("jobject")) {
              return javaType(java.lang.Object.class);
            }

            String name = targetType.getName();
            if (name == null) {
              // Try containing pointer type for any typedefs
              name = t.getName();
              if (name == null) {
                throw new RuntimeException("Couldn't find a proper type name for pointer type " + t);
              }
            }

            return JavaType.createForCStruct(cfg.renameJavaType(name));
          } else {
            throw new RuntimeException("Don't know how to convert pointer/array type \"" +
                                       t + "\"");
          }
        }
        // Handle Types of form pointer-to-pointer-to-type or
        // array-of-arrays-of-type, like char** or int[][]
        else if (t.pointerDepth() == 2 || t.arrayDimension() == 2) {
          // Get the target type of the target type (targetType was computer earlier
          // as to be a pointer to the target type, so now we need to get its
          // target type)
          Type bottomType;
          if (targetType.isPointer()) {
            // t is<type>**, targetType is <type>*, we need to get <type>
            bottomType = targetType.asPointer().getTargetType();
            return JavaType.forNIOPointerBufferClass();
          } else {
            // t is<type>[][], targetType is <type>[], we need to get <type>
            bottomType = targetType.asArray().getElementType();
            LOG.log(WARNING, "typeToJavaType(ptr-ptr): {0}, targetType: {1}, bottomType: {2} -> Unhandled!", new Object[]{t, targetType, bottomType});
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
                                                    bottomType.getSize(curMachDesc) + " and name " + bottomType.getName());
              }
            } else if (bottomType.isFloat()) {
              return javaType(ArrayTypes.floatBufferArrayClass);
            } else if (bottomType.isDouble()) {
              return javaType(ArrayTypes.doubleBufferArrayClass);
            } else {
              throw new RuntimeException("Unexpected primitive type " + bottomType.getName() +
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
              "Could not convert C type \"" + t + "\" " +
              "to appropriate Java type; need to add more support for " +
              "depth=2 pointer/array types [debug info: targetType=\"" +
              targetType + "\"]");
          }
        } else {
          // can't handle this type of pointer/array argument
          throw new RuntimeException(
            "Could not convert C pointer/array \"" + t + "\" to " +
            "appropriate Java type; types with pointer/array depth " +
            "greater than 2 are not yet supported [debug info: " +
            "pointerDepth=" + t.pointerDepth() + " arrayDimension=" +
            t.arrayDimension() + " targetType=\"" + targetType + "\"]");
        }

      } else {
        throw new RuntimeException(
          "Could not convert C type \"" + t + "\" (class " +
          t.getClass().getName() + ") to appropriate Java type");
      }
    }
  }

  private static boolean isIntegerType(Class<?> c) {
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
   * @return a {@link PrintWriter} instance to write the class source file
   * @throws IOException
   */
  protected PrintWriter openFile(String filename, String simpleClassName) throws IOException {
    //System.out.println("Trying to open: " + filename);
    File file = new File(filename);
    String parentDir = file.getParent();
    if (parentDir != null)
    {
      File pDirFile = new File(parentDir);
      pDirFile.mkdirs();
    }
    return new PrintWriter(new BufferedWriter(new FileWriter(file)));
  }

  private boolean isOpaque(Type type) {
    return (cfg.typeInfo(type, typedefDictionary) != null);
  }

  private String compatiblePrimitiveJavaTypeName(Type fieldType,
                                                 JavaType javaType,
                                                 MachineDescription curMachDesc) {
    Class<?> c = javaType.getJavaClass();
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

  private void closeWriter(PrintWriter writer) throws IOException {
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
  protected void emitCustomJavaCode(PrintWriter writer, String className) throws Exception  {
    List<String> code = cfg.customJavaCodeForClass(className);
    if (code.isEmpty())
      return;

    writer.println();
    writer.println("  // --- Begin CustomJavaCode .cfg declarations");
    for (String line : code) {
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
        List<String> imports = new ArrayList<String>(cfg.imports());
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
        CodeGenUtils.EmissionCallback docEmitter =
          new CodeGenUtils.EmissionCallback() {
            @Override
            public void emit(PrintWriter w) {
              for (Iterator<String> iter = intfDocs.iterator(); iter.hasNext(); ) {
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
        CodeGenUtils.EmissionCallback docEmitter =
          new CodeGenUtils.EmissionCallback() {
            @Override
            public void emit(PrintWriter w) {
              for (Iterator<String> iter = implDocs.iterator(); iter.hasNext(); ) {
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
        emitCHeader(cWriter(), cfg.implClassName());
      }
    } catch (Exception e) {
      throw new RuntimeException(
        "Error emitting all file headers: cfg.allStatic()=" + cfg.allStatic() +
        " cfg.emitImpl()=" + cfg.emitImpl() + " cfg.emitInterface()=" + cfg.emitInterface(),
        e);
    }

  }

  protected void emitCHeader(PrintWriter cWriter, String className) {
    cWriter.println("#include <jni.h>");
    cWriter.println("#include <stdlib.h>");
    cWriter.println();

    if (getConfig().emitImpl()) {
      cWriter.println("#include <assert.h>");
      cWriter.println();
    }

    for (String code : cfg.customCCode()) {
      cWriter.println(code);
    }
    cWriter.println();
  }

  /**
   * Write out any footer information for the output files (closing brace of
   * class definition, etc).
   */
  protected void emitAllFileFooters(){
    if (cfg.allStatic() || cfg.emitInterface()) {
      javaWriter().println();
      javaWriter().println("} // end of class " + cfg.className());
    }
    if (!cfg.allStatic() && cfg.emitImpl())  {
      javaImplWriter().println();
      javaImplWriter().println("} // end of class " + cfg.implClassName());
    }
  }

  private JavaType javaType(Class<?> c) {
    return JavaType.createForClass(c);
  }

  /** Maps the C types in the specified function to Java types through
      the MethodBinding interface. Note that the JavaTypes in the
      returned MethodBinding are "intermediate" JavaTypes (some
      potentially representing C pointers rather than true Java types)
      and must be lowered to concrete Java types before creating
      emitters for them. */
  private MethodBinding bindFunction(FunctionSymbol sym,
                                     JavaType containingType,
                                     Type containingCType,
                                     MachineDescription curMachDesc) {

    MethodBinding binding = new MethodBinding(sym, containingType, containingCType);

    binding.renameMethodName(cfg.getJavaSymbolRename(sym.getName()));

    // System.out.println("bindFunction(0) "+sym.getReturnType());

    if (cfg.returnsString(binding.getName())) {
      PointerType prt = sym.getReturnType().asPointer();
      if (prt == null ||
          prt.getTargetType().asInt() == null ||
          prt.getTargetType().getSize(curMachDesc) != 1) {
        throw new RuntimeException(
          "Cannot apply ReturnsString configuration directive to \"" + sym +
          "\". ReturnsString requires native method to have return type \"char *\"");
      }
      binding.setJavaReturnType(javaType(java.lang.String.class));
    } else {
      binding.setJavaReturnType(typeToJavaType(sym.getReturnType(), false, curMachDesc));
    }

    // System.out.println("bindFunction(1) "+binding.getJavaReturnType());

    // List of the indices of the arguments in this function that should be
    // converted from byte[] or short[] to String
    List<Integer> stringArgIndices = cfg.stringArguments(binding.getName());

    for (int i = 0; i < sym.getNumArguments(); i++) {
      Type cArgType = sym.getArgumentType(i);
      JavaType mappedType = typeToJavaType(cArgType, true, curMachDesc);
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

  private MethodBinding lowerMethodBindingPointerTypes(MethodBinding inputBinding,
                                                       boolean convertToArrays,
                                                       boolean[] canProduceArrayVariant) {
    MethodBinding result = inputBinding;
    boolean arrayPossible = false;

    // System.out.println("lowerMethodBindingPointerTypes(0): "+result);

    for (int i = 0; i < inputBinding.getNumArguments(); i++) {
      JavaType t = inputBinding.getJavaArgumentType(i);
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
    JavaType t = result.getJavaReturnType();
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
  protected List<MethodBinding> expandMethodBinding(MethodBinding binding) {

    List<MethodBinding> result = new ArrayList<MethodBinding>();
    // Indicates whether it is possible to produce an array variant
    // Prevents e.g. char* -> String conversions from emitting two entry points
    boolean[] canProduceArrayVariant = new boolean[1];

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

  private Type canonicalize(Type t) {
    Type res = canonMap.get(t);
    if (res != null) {
      return res;
    }
    canonMap.put(t, t);
    return t;
  }

  /**
   * Converts first letter to upper case.
   */
  private final String capitalizeString(String string) {
      return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }

}
