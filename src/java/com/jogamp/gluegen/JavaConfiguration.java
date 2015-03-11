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

import com.jogamp.gluegen.ASTLocusTag.ASTLocusTagProvider;
import com.jogamp.gluegen.JavaEmitter.EmissionStyle;
import com.jogamp.gluegen.JavaEmitter.MethodAccess;
import com.jogamp.gluegen.Logging.LoggerIf;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.*;

import com.jogamp.gluegen.jgram.*;
import com.jogamp.gluegen.cgram.types.*;

import static java.util.logging.Level.*;
import static com.jogamp.gluegen.JavaEmitter.MethodAccess.*;
import static com.jogamp.gluegen.JavaEmitter.EmissionStyle.*;

/** Parses and provides access to the contents of .cfg files for the
    JavaEmitter. */

public class JavaConfiguration {
    private int nestedReads;
    private String packageName;
    private String implPackageName;
    private String className;
    private String implClassName;

    protected final LoggerIf LOG;

    public static String NEWLINE = System.getProperty("line.separator");

    /**
     * Root directory for the hierarchy of generated java classes. Default is
     * working directory.
     */
    private String javaOutputDir = ".";

    /**
     * Top output root directory for all generated files. Default is null, ie not to use it.
     */
    private String outputRootDir = null;

    /**
     * Directory into which generated native JNI code will be written. Default
     * is current working directory.
     */
    private String nativeOutputDir = ".";

    /**
     * If true, then each native *.c and *.h file will be generated in the
     * directory nativeOutputDir/packageAsPath(packageName). Default is false.
     */
    private boolean nativeOutputUsesJavaHierarchy;

    /**
     * If true, then the comment of a native method binding will include a @native tag
     * to allow taglets to augment the javadoc with additional information regarding
     * the mapped C function. Defaults to false.
     */
    private boolean tagNativeBinding;

    /**
     * If true, {@link TypeConfig.SemanticEqualityOp#equalSemantics(TypeConfig.SemanticEqualityOp)}
     * will attempt to perform a relaxed semantic equality test, e.g. skip the {@code const} and {@code volatile} qualifiers.
     * Otherwise a full semantic equality test will be performed.
     */
    private boolean relaxedEqualSemanticsTest;

    /**
     * Style of code emission. Can emit everything into one class
     * (AllStatic), separate interface and implementing classes
     * (InterfaceAndImpl), only the interface (InterfaceOnly), or only
     * the implementation (ImplOnly).
     */
    private EmissionStyle emissionStyle = AllStatic;

    /**
     * List of imports to emit at the head of the output files.
     */
    private final List<String> imports = new ArrayList<String>();

    /**
     * The package in which the generated glue code expects to find its
     * run-time helper classes (Buffers, Platform,
     * StructAccessor). Defaults to "com.jogamp.gluegen.runtime".
     */
    private String gluegenRuntimePackage = "com.jogamp.gluegen.runtime";

    /**
     * The kind of exception raised by the generated code if run-time
     * checks fail. Defaults to RuntimeException.
     */
    private String runtimeExceptionType = "RuntimeException";
    private String unsupportedExceptionType = "UnsupportedOperationException";

    private final Map<String, MethodAccess> accessControl = new HashMap<String, MethodAccess>();
    private final Map<String, TypeInfo> typeInfoMap = new HashMap<String, TypeInfo>();
    private final Set<String> returnsString = new HashSet<String>();
    private final Map<String, JavaType> returnsOpaqueJType = new HashMap<String, JavaType>();
    private final Map<String, String> returnedArrayLengths = new HashMap<String, String>();

    /**
     * Key is function that has some byte[] or short[] arguments that should be
     * converted to String args; value is List of Integer argument indices
     */
    private final Map<String, List<Integer>> argumentsAreString = new HashMap<String, List<Integer>>();
    private final Set<String> extendedIntfSymbolsIgnore = new HashSet<String>();
    private final Set<String> extendedIntfSymbolsOnly = new HashSet<String>();
    private final Set<String> extendedImplSymbolsIgnore = new HashSet<String>();
    private final Set<String> extendedImplSymbolsOnly = new HashSet<String>();
    private final Set<Pattern> ignores = new HashSet<Pattern>();
    private final Map<String, Pattern> ignoreMap = new HashMap<String, Pattern>();
    private final Set<Pattern> ignoreNots = new HashSet<Pattern>();
    private final Set<Pattern> unignores = new HashSet<Pattern>();
    private final Set<Pattern> unimplemented = new HashSet<Pattern>();
    private boolean forceUseNIOOnly4All = false;
    private final Set<String> useNIOOnly = new HashSet<String>();
    private boolean forceUseNIODirectOnly4All = false;
    private final Set<String> useNIODirectOnly = new HashSet<String>();
    private final Set<String> manuallyImplement = new HashSet<String>();
    private final Map<String, String> delegatedImplementation = new HashMap<String, String>();
    private final Set<String> manualStaticInitCall = new HashSet<String>();
    private final Set<String> forceStaticInitCode = new HashSet<String>();
    private final Map<String, List<String>> customJavaCode = new HashMap<String, List<String>>();
    private final Map<String, List<String>> classJavadoc = new HashMap<String, List<String>>();
    private final Map<String, List<String>> methodJavadoc = new HashMap<String, List<String>>();
    private final Map<String, String> structPackages = new HashMap<String, String>();
    private final List<String> customCCode = new ArrayList<String>();
    private final List<String> forcedStructs = new ArrayList<String>();
    private final Map<String, String> structMachineDataInfoIndex = new HashMap<String, String>();
    private final Map<String, String> returnValueCapacities = new HashMap<String, String>();
    private final Map<String, String> returnValueLengths = new HashMap<String, String>();
    private final Map<String, List<String>> temporaryCVariableDeclarations = new HashMap<String, List<String>>();
    private final Map<String, List<String>> temporaryCVariableAssignments = new HashMap<String, List<String>>();
    private final Map<String, List<String>> extendedInterfaces = new HashMap<String, List<String>>();
    private final Map<String, List<String>> implementedInterfaces = new HashMap<String, List<String>>();
    private final Map<String, String> parentClass = new HashMap<String, String>();
    private final Map<String, String> javaTypeRenames = new HashMap<String, String>();
    private final Map<String, String> javaSymbolRenames = new HashMap<String, String>();
    private final Map<String, Set<String>> javaRenamedSymbols = new HashMap<String, Set<String>>();
    private final Map<String, List<String>> javaPrologues = new HashMap<String, List<String>>();
    private final Map<String, List<String>> javaEpilogues = new HashMap<String, List<String>>();

    public JavaConfiguration() {
        LOG = Logging.getLogger(JavaConfiguration.class.getPackage().getName(), JavaConfiguration.class.getSimpleName());
    }

  /** Reads the configuration file.
      @param filename path to file that should be read
  */
  public final void read(final String filename) throws  IOException {
    read(filename, null);
  }

  /** Reads the specified file, treating each line as if it started with the
      specified string.
      @param filename path to file that should be read
      @param linePrefix if not null, treat each line read as if it were
      prefixed with the specified string.
  */
  protected final void read(final String filename, final String linePrefix) throws IOException {
    final File file = new File(filename);
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
    }
    catch (final FileNotFoundException fnfe) {
      throw new RuntimeException("Could not read file \"" + file + "\"", fnfe);
    }
    int lineNo = 0;
    String line = null;
    final boolean hasPrefix = linePrefix != null && linePrefix.length() > 0;
    try {
      ++nestedReads;
      while ((line = reader.readLine()) != null) {
        ++lineNo;
        if (hasPrefix)  {
          line = linePrefix + " " + line;
        }

        if (line.trim().startsWith("#")) {
          // comment line
          continue;
        }

        final StringTokenizer tok = new StringTokenizer(line);
        if (tok.hasMoreTokens()) {
          // always reset delimiters in case of CustomJavaCode, etc.
          final String cmd = tok.nextToken(" \t\n\r\f");

          dispatch(cmd, tok, file, filename, lineNo);
        }
      }
      reader.close();
    } finally {
      --nestedReads;
    }

    if (nestedReads == 0) {
      if (allStatic() && implClassName != null) {
        throw new IllegalStateException("Error in configuration file \"" + filename + "\": Cannot use " +
                                        "directive \"ImplJavaClass\" in conjunction with " +
                                        "\"Style AllStatic\"");
      }

      if (className == null && (emissionStyle() != ImplOnly)) {
//        throw new RuntimeException("Output class name was not specified in configuration file \"" + filename + "\"");
      }
      if (packageName == null && (emissionStyle() != ImplOnly)) {
        throw new RuntimeException("Output package name was not specified in configuration file \"" + filename + "\"");
      }

      if (allStatic()) {
        implClassName = className;
        // If we're using the "Style AllStatic" directive, then the
        // implPackageName is the same as the regular package name
        implPackageName = packageName;
      } else {
        if (implClassName == null) {
          // implClassName defaults to "<className>Impl" if ImplJavaClass
          // directive is not used
          if (className == null) {
            throw new RuntimeException("If ImplJavaClass is not specified, must specify JavaClass");
          }
          implClassName = className + "Impl";
        }
        if (implPackageName == null) {
          // implPackageName defaults to "<packageName>.impl" if ImplPackage
          // directive is not used
          if (packageName == null) {
            throw new RuntimeException("If ImplPackageName is not specified, must specify PackageName");
          }
          implPackageName = packageName + ".impl";
        }
      }
    }
  }

  public void setOutputRootDir(final String s) { outputRootDir=s; }

    /** Returns the package name parsed from the configuration file. */
    public String packageName() {
        return packageName;
    }

    /** Returns the implementation package name parsed from the configuration file. */
    public String implPackageName() {
        return implPackageName;
    }

    /** Returns the class name parsed from the configuration file. */
    public String className() {
        return className;
    }

    /** Returns the implementation class name parsed from the configuration file. */
    public String implClassName() {
        return implClassName;
    }

    public boolean structsOnly() {
        return className == null && implClassName == null;
    }

    /** Returns the Java code output directory parsed from the configuration file. */
    public String javaOutputDir() {
        return (null != outputRootDir) ? (outputRootDir + "/" + javaOutputDir) : javaOutputDir;
    }

    /** Returns the native code output directory parsed from the configuration file. */
    public String nativeOutputDir() {
        return (null != outputRootDir) ? (outputRootDir + "/" + nativeOutputDir) : nativeOutputDir;
    }

    /** Returns whether the native code directory structure mirrors the Java hierarchy. */
    public boolean nativeOutputUsesJavaHierarchy() {
        return nativeOutputUsesJavaHierarchy;
    }

    /** Returns whether the comment of a native method binding should include a @native tag. */
    public boolean tagNativeBinding() {
        return tagNativeBinding;
    }

    /**
     * Returns whether {@link TypeConfig.SemanticEqualityOp#equalSemantics(TypeConfig.SemanticEqualityOp)}
     * shall attempt to perform a relaxed semantic equality test, e.g. skip the {@code const} and {@code volatile} qualifier
     * - or not.
     */
    public boolean relaxedEqualSemanticsTest() {
        return relaxedEqualSemanticsTest;
    }

    /** Returns the code emission style (constants in JavaEmitter) parsed from the configuration file. */
    public EmissionStyle emissionStyle() {
        return emissionStyle;
    }

    /**
     * Returns the access control for the given method-name
     * or fully qualified class-name.
     */
    public MethodAccess accessControl(final String name) {
        final MethodAccess ret = accessControl.get(name);
        if (ret != null) {
            return ret;
        }
        // Default access control is public
        return PUBLIC;
    }

    /** Returns the package in which the generated glue code expects to
    find its run-time helper classes (Buffers, Platform,
    StructAccessor). Defaults to "com.jogamp.gluegen.runtime". */
    public String gluegenRuntimePackage() {
        return gluegenRuntimePackage;
    }

    /** Returns the kind of exception to raise if run-time checks fail in the generated code. */
    public String runtimeExceptionType() {
        return runtimeExceptionType;
    }

    /** Returns the kind of exception to raise if run-time checks fail in the generated code. */
    public String unsupportedExceptionType() {
        return unsupportedExceptionType;
    }

    /** Returns the list of imports that should be emitted at the top of each .java file. */
    public List<String> imports() {
        return imports;
    }

  private static final boolean DEBUG_TYPE_INFO = false;

  /**
   * If the given {@code canonicalName} should be considered opaque,
   * returns the TypeInfo describing the replacement type.
   * <p>
   * Returns null if this type should not be considered opaque.
   * </p>
   * <p>
   * If symbol references a struct fields, see {@link #canonicalStructFieldSymbol(String, String)},
   * it describes field's array-length or element-count referenced by a pointer.
   * </p>
   */
  public TypeInfo canonicalNameOpaque(final String canonicalName) {
    return typeInfoMap.get(canonicalName);
  }

  /** If this type should be considered opaque, returns the TypeInfo
      describing the replacement type. Returns null if this type
      should not be considered opaque. */
  public TypeInfo typeInfo(Type type) {
    // Because typedefs of pointer types can show up at any point,
    // walk the pointer chain looking for a typedef name that is in
    // the TypeInfo map.
    if (DEBUG_TYPE_INFO)
      System.err.println("Incoming type = " + type + ", " + type.getDebugString());
    final int pointerDepth = type.pointerDepth();
    for (int i = 0; i <= pointerDepth; i++) {
      String name = type.getName();
      if (DEBUG_TYPE_INFO) {
        System.err.println(" Type = " + type);
        System.err.println(" Name = " + name);
      }
      if (name != null) {
        final TypeInfo info = closestTypeInfo(name, i + type.pointerDepth());
        if (info != null) {
          final TypeInfo res = promoteTypeInfo(info, i);
          if (DEBUG_TYPE_INFO) {
            System.err.println(" [1] info.name=" + info.name() + ", name=" + name +
                               ", info.pointerDepth=" + info.pointerDepth() +
                               ", type.pointerDepth=" + type.pointerDepth() + " -> "+res);
          }
          return res;
        }
      }

      if (type.isCompound()) {
        // Try struct name as well
        name = type.asCompound().getStructName();
        if (name != null) {
          final TypeInfo info = closestTypeInfo(name, i + type.pointerDepth());
          if (info != null) {
            final TypeInfo res = promoteTypeInfo(info, i);
            if (DEBUG_TYPE_INFO) {
              System.err.println(" [2] info.name=" + info.name() + ", name=" + name +
                                 ", info.pointerDepth=" + info.pointerDepth() +
                                 ", type.pointerDepth=" + type.pointerDepth() + " -> "+res);
            }
            return res;
          }
        }
      }

      if (type.isPointer()) {
        type = type.asPointer().getTargetType();
      }
    }
    if (DEBUG_TYPE_INFO) {
      System.err.println(" [X] NULL");
    }
    return null;
  }

  // Helper functions for above
  private TypeInfo closestTypeInfo(final String name, final int pointerDepth) {
    TypeInfo info = typeInfoMap.get(name);
    TypeInfo closest = null;
    while (info != null) {
      if (DEBUG_TYPE_INFO)
        System.err.println("  Checking TypeInfo for " + name + " at pointerDepth " + pointerDepth);
      if (info.pointerDepth() <= pointerDepth && (closest == null || info.pointerDepth() > closest.pointerDepth())) {
        if (DEBUG_TYPE_INFO)
          System.err.println("   Accepted");
        closest = info;
      }
      info = info.next();
    }
    return closest;
  }

  // Promotes a TypeInfo to a higher pointer type (if necessary)
  private TypeInfo promoteTypeInfo(final TypeInfo info, final int numPointersStripped) {
    int diff = numPointersStripped - info.pointerDepth();
    if (diff == 0) {
      return info;
    }

    if (diff < 0) {
      throw new RuntimeException("TypeInfo for " + info.name() + " and pointerDepth " +
                                 info.pointerDepth() + " should not have matched for depth " +
                                 numPointersStripped);
    }

    Class<?> c = info.javaType().getJavaClass();
    final int pd = info.pointerDepth();

    // Handle single-pointer stripping for types compatible with C
    // integral and floating-point types specially so we end up
    // generating NIO variants for these
    if (diff == 1) {
      JavaType jt = null;
      if      (c == Boolean.TYPE) jt = JavaType.createForCCharPointer();
      else if (c == Byte.TYPE)    jt = JavaType.createForCCharPointer();
      else if (c == Short.TYPE)   jt = JavaType.createForCShortPointer();
      else if (c == Integer.TYPE) jt = JavaType.createForCInt32Pointer();
      else if (c == Long.TYPE)    jt = JavaType.createForCInt64Pointer();
      else if (c == Float.TYPE)   jt = JavaType.createForCFloatPointer();
      else if (c == Double.TYPE)  jt = JavaType.createForCDoublePointer();

      if (jt != null)
        return new TypeInfo(info.name(), pd + numPointersStripped, jt);
    }

    while (diff > 0) {
      c = Array.newInstance(c, 0).getClass();
      --diff;
    }

    return new TypeInfo(info.name(),
                        numPointersStripped,
                        JavaType.createForClass(c));
  }

  /** Indicates whether the given function (which returns a
      <code>char*</code> in C) should be translated as returning a
      <code>java.lang.String</code>. */
  public boolean returnsString(final String functionName) {
    return returnsString.contains(functionName);
  }
  /** Indicates whether the given function (which returns a
      <code>char*</code> in C) should be translated as returning a
      <code>java.lang.String</code>. */
  public boolean returnsString(final AliasedSymbol symbol) {
      return returnsString.contains( symbol.getName() ) ||
             oneInSet(returnsString, symbol.getAliasedNames());
  }

  /**
   * Returns a MessageFormat string of the Java expression calculating
   * the number of elements in the returned array from the specified function
   * name. The literal <code>1</code> indicates a single object.
   * <p>
   * If symbol references a struct fields, see {@link #canonicalStructFieldSymbol(String, String)},
   * it describes field's array-length or element-count referenced by a pointer.
   * </p>
   * <p>
   * In case of struct fields, this array length will also be used
   * for the native C function, i.e. multiplied w/ <code>sizeof(C-Type)</code>
   * and passed down to native code, <b>if</b> not overriden by
   * either {@link #returnValueCapacity(String)} or {@link #returnValueLength(String)}!
   * </p>
   */
  public String returnedArrayLength(final String functionName) {
    return returnedArrayLengths.get(functionName);
  }

  /** Returns a list of <code>Integer</code>s which are the indices of <code>const char*</code>
      arguments that should be converted to <code>String</code>s. Returns null if there are no
      such hints for the given function name. */

  public List<Integer> stringArguments(final String functionName) {
    return argumentsAreString.get(functionName);
  }

  public boolean isForceUsingNIOOnly4All() { return forceUseNIOOnly4All; }

  public void addUseNIOOnly(final String fname ) {
      useNIOOnly.add(fname);
  }
  /** Returns true if the given function should only create a java.nio
      variant, and no array variants, for <code>void*</code> and other
      C primitive pointers. NIO only still allows usage of array backed not direct Buffers. */
  public boolean useNIOOnly(final String functionName) {
    return useNIODirectOnly(functionName) || forceUseNIOOnly4All || useNIOOnly.contains(functionName);
  }

  public void addUseNIODirectOnly(final String fname ) {
      useNIODirectOnly.add(fname);
  }
  /** Returns true if the given function should only create a java.nio
      variant, and no array variants, for <code>void*</code> and other
      C primitive pointers. NIO direct only does only allow direct Buffers.
      Implies useNIOOnly !
   */
  public boolean useNIODirectOnly(final String functionName) {
    return forceUseNIODirectOnly4All || useNIODirectOnly.contains(functionName);
  }

  /**
   * Returns true if the static initialization java code calling <code>initializeImpl()</code>
   * for the given class will be manually implemented by the end user
   * as requested via configuration directive <code>ManualStaticInitCall 'class-name'</code>.
   */
  public boolean manualStaticInitCall(final String clazzName) {
    return manualStaticInitCall.contains(clazzName);
  }

  /**
   * Returns true if the static initialization java code implementing <code>initializeImpl()</code>
   * and the native code implementing:
   * <pre>
   *   static jobject JVMUtil_NewDirectByteBufferCopy(JNIEnv *env, void * source_address, jlong capacity);
   * </pre>
   * for the given class will be included in the generated code, always,
   * as requested via configuration directive <code>ForceStaticInitCode 'class-name'</code>.
   * <p>
   * If case above code has been generated, static class initialization is generated
   * to call <code>initializeImpl()</code>, see {@link #manualStaticInitCall(String)}.
   * </p>
   */
  public boolean forceStaticInitCode(final String clazzName) {
    return forceStaticInitCode.contains(clazzName);
  }

  /** Returns a list of Strings containing user-implemented code for
      the given Java type name (not fully-qualified, only the class
      name); returns either null or an empty list if there is no
      custom code for the class. */
  public List<String> customJavaCodeForClass(final String className) {
    List<String> res = customJavaCode.get(className);
    if (res == null) {
      res = new ArrayList<String>();
      customJavaCode.put(className, res);
    }
    return res;
  }

  public List<String> javadocForMethod(final String methodName) {
    List<String> res = methodJavadoc.get(methodName);
    if (res == null) {
      res = new ArrayList<String>();
      methodJavadoc.put(methodName, res);
    }
    return res;
  }

  /** Returns a list of Strings containing Javadoc documentation for
      the given Java type name (not fully-qualified, only the class
      name); returns either null or an empty list if there is no
      Javadoc documentation for the class. */
  public List<String> javadocForClass(final String className) {
    List<String> res = classJavadoc.get(className);
    if (res == null) {
      res = new ArrayList<String>();
      classJavadoc.put(className, res);
    }
    return res;
  }

  /** Returns the package into which to place the glue code for
      accessing the specified struct. Defaults to emitting into the
      regular package (i.e., the result of {@link #packageName}). */
  public String packageForStruct(final String structName) {
    String res = structPackages.get(structName);
    if (res == null) {
      res = packageName;
    }
    return res;
  }

  /** Returns, as a List of Strings, the custom C code to be emitted
      along with the glue code for the main class. */
  public List<String> customCCode() {
    return customCCode;
  }

  /** Returns, as a List of Strings, the structs for which glue code
      emission should be forced. */
  public List<String> forcedStructs() {
    return forcedStructs;
  }

  /**
   * Returns a MessageFormat string of the Java code defining {@code mdIdx},
   * i.e. the index of the static MachineDescriptor index for structs.
   * <p>
   * If undefined, code generation uses the default expression:
   * <pre>
   *     private static final int mdIdx = MachineDataInfoRuntime.getStatic().ordinal();
   * </pre>
   * </p>
   */
  public String returnStructMachineDataInfoIndex(final String structName) {
    return structMachineDataInfoIndex.get(structName);
  }

  /**
   * Returns a MessageFormat string of the C expression calculating
   * the capacity of the java.nio.ByteBuffer being returned from a
   * native method, or null if no expression has been specified.
   * <p>
   * If symbol references a struct fields, see {@link #canonicalStructFieldSymbol(String, String)},
   * it describes field's array-length or element-count referenced by a pointer.
   * </p>
   */
  public String returnValueCapacity(final String functionName) {
    return returnValueCapacities.get(functionName);
  }

  /**
   * Returns a MessageFormat string of the C expression calculating
   * the length of the array being returned from a native method.
   * <p>
   * If symbol references a struct fields, see {@link #canonicalStructFieldSymbol(String, String)},
   * it describes field's array-length or element-count referenced by a pointer.
   * </p>
   */
  public String returnValueLength(final String symbol) {
    return returnValueLengths.get(symbol);
  }

  /** Returns a List of Strings of expressions declaring temporary C
      variables in the glue code for the specified function. */
  public List<String> temporaryCVariableDeclarations(final String functionName) {
    return temporaryCVariableDeclarations.get(functionName);
  }

  /** Returns a List of Strings of expressions containing assignments
      to temporary C variables in the glue code for the specified
      function. */
  public List<String> temporaryCVariableAssignments(final String functionName) {
    return temporaryCVariableAssignments.get(functionName);
  }

  /** Returns a List of Strings indicating the interfaces the passed
      interface should declare it extends. May return null or a list
      of zero length if there are none. */
  public List<String> extendedInterfaces(final String interfaceName) {
    List<String> res = extendedInterfaces.get(interfaceName);
    if (res == null) {
      res = new ArrayList<String>();
      extendedInterfaces.put(interfaceName, res);
    }
    return res;
  }

  /** Returns a List of Strings indicating the interfaces the passed
      class should declare it implements. May return null or a list
      of zero length if there are none. */
  public List<String> implementedInterfaces(final String className) {
    List<String> res = implementedInterfaces.get(className);
    if (res == null) {
      res = new ArrayList<String>();
      implementedInterfaces.put(className, res);
    }
    return res;
  }

  /** Returns a List of Strings indicating the interfaces the passed
      class should declare it implements. May return null or a list
      of zero length if there are none. */
  public String extendedParentClass(final String className) {
    return parentClass.get(className);
  }

  public void logIgnoresOnce() {
    if(!loggedIgnores) {
        loggedIgnores = true;
        logIgnores();
    }
  }
  private static boolean loggedIgnores = false;

  public void logIgnores() {
    LOG.log(INFO, "Extended Intf: {0}", extendedIntfSymbolsIgnore.size());
    for (final String str : extendedIntfSymbolsIgnore) {
        LOG.log(INFO, "\t{0}", str);
    }
    LOG.log(INFO, "Extended Impl: {0}", extendedImplSymbolsIgnore.size());
    for (final String str : extendedImplSymbolsIgnore) {
        LOG.log(INFO, "\t{0}", str);
    }
    LOG.log(INFO, "Ignores (All): {0}", ignores.size());
    for (final Pattern pattern : ignores) {
        LOG.log(INFO, "\t{0}", pattern);
    }
  }

  public void logRenamesOnce() {
    if(!loggedRenames) {
        loggedRenames = true;
        logRenames();
    }
  }
  private static boolean loggedRenames = false;

  public void logRenames() {
    LOG.log(INFO, "Symbol Renames: {0}", javaSymbolRenames.size());
    for (final String key : javaSymbolRenames.keySet()) {
        LOG.log(INFO, "\t{0} -> {1}", key, javaSymbolRenames.get(key));
    }

    LOG.log(INFO, "Symbol Aliasing (through renaming): {0}", javaSymbolRenames.size());
    for(final String newName : javaSymbolRenames.values()) {
        final Set<String> origNames = javaRenamedSymbols.get(newName);
        if(null!=origNames) {
            LOG.log(INFO, "\t{0} <- {1}", newName, origNames);
        }
    }
  }

  public static <K,V> V oneInMap(final Map<K, V> map, final Set<K> symbols) {
      if( null != map && map.size() > 0 &&
          null != symbols && symbols.size() > 0 ) {
          for(final K sym : symbols) {
              final V v = map.get(sym);
              if( null != v ) {
                  return v;
              }
          }
      }
      return null;
  }
  public static <K> boolean oneInSet(final Set<K> set1, final Set<K> set2) {
      if( null != set1 && set1.size() > 0 &&
          null != set2 && set2.size() > 0 ) {
          for(final K sym : set2) {
              if( set1.contains( sym ) ) {
                  return true;
              }
          }
      }
      return false;
  }
  private static boolean onePatternMatch(final Pattern ignoreRegexp, final Set<String> set) {
      if( null != ignoreRegexp && null != set && set.size() > 0 ) {
          for(final String sym : set) {
              final Matcher matcher = ignoreRegexp.matcher(sym);
              if (matcher.matches()) {
                  return true;
              }
          }
      }
      return false;
  }
  protected static ASTLocusTag getASTLocusTag(final AliasedSymbol s) {
      if( s instanceof ASTLocusTagProvider ) {
          return ((ASTLocusTagProvider)s).getASTLocusTag();
      } else {
          return null;
      }
  }

  /**
   * Returns the canonical configuration name for a struct field name,
   * i.e. 'struct-name'.'field-name'
   */
  public static String canonicalStructFieldSymbol(final String structName, final String fieldName) {
      return structName+"."+fieldName;
  }

  /**
   * Variant of {@link #manuallyImplement(AliasedSymbol)},
   * where this method only considers the {@link AliasedSymbol#getName() current-name}
   * of the given symbol, not the {@link #getJavaSymbolRename(String) renamed-symbol}.
   */
  public boolean manuallyImplement(final String functionName) {
      if( manuallyImplement.contains(functionName) ) {
          LOG.log(INFO, "ManuallyImplement: \"{0}\"", functionName);
          return true;
      } else {
          return false;
      }
  }

  /**
   * Returns true if the glue code for the given aliased function will be
   * manually implemented by the end user.
   * <p>
   * Both, the {@link AliasedSymbol#getName() current-name}
   * and all {@link AliasedSymbol#getAliasedNames() aliases} shall be considered.
   * </p>
   * <p>
   * If symbol references a struct field or method, see {@link #canonicalStructFieldSymbol(String, String)},
   * it describes field's array-length or element-count referenced by a pointer.
   * </p>
   * @see #manuallyImplement(String)
   */
  public boolean manuallyImplement(final AliasedSymbol symbol) {
      final String name = symbol.getName();
      final Set<String> aliases = symbol.getAliasedNames();

      if ( manuallyImplement.contains( name ) ||
           oneInSet(manuallyImplement, aliases)
         )
      {
          LOG.log(INFO, getASTLocusTag(symbol), "ManuallyImplement: {0}", symbol);
          return true;
      } else {
          return false;
      }
  }

  /**
   * Variant of {@link #getDelegatedImplementation(AliasedSymbol)},
   * where this method only considers the {@link AliasedSymbol#getName() current-name}
   * of the given symbol, not the {@link #getJavaSymbolRename(String) renamed-symbol}.
   */
  public String getDelegatedImplementation(final String functionName) {
      final String res = delegatedImplementation.get(functionName);
      if( null == res ) {
          return null;
      }
      LOG.log(INFO, "DelegatedImplementation: {0} -> {1}", functionName, res);
      return res;
  }

  /**
   * Returns the {@code RENAMED-IMPL-SYMBOL} if the implementation of the glue code
   * of the given function shall be manually delegated by the end user.
   * <p>
   * {@code DelegateImplementation <ORIG-SYMBOL> <RENAMED-IMPL-SYMBOL>}
   * </p>
   * <p>
   * The interface is emitted unchanged.
   * </p>
   * <p>
   * The Java and native-code implementation is renamed to {@code RENAMED-IMPL-SYMBOL}.
   * The user's manual implementation of {@code ORIG-SYMBOL}
   * may delegate to {@code RENAMED-IMPL-SYMBOL}.
   * </p>
   * <p>
   * If symbol references a struct field or method, see {@link #canonicalStructFieldSymbol(String, String)},
   * it describes field's array-length or element-count referenced by a pointer.
   * </p>
   */
  public String getDelegatedImplementation(final AliasedSymbol symbol) {
      final String name = symbol.getName();
      final Set<String> aliases = symbol.getAliasedNames();

      String res = delegatedImplementation.get(name);
      if( null == res ) {
          res = oneInMap(delegatedImplementation, aliases);
          if( null == res ) {
              return null;
          }
      }
      LOG.log(INFO, getASTLocusTag(symbol), "DelegatedImplementation: {0} -> {1}", symbol, res);
      return res;
  }

  /**
   * Variant of {@link #getOpaqueReturnType(AliasedSymbol)},
   * where this method only considers the {@link AliasedSymbol#getName() current-name}
   * of the given symbol, not the {@link #getJavaSymbolRename(String) renamed-symbol}.
   */
  public JavaType getOpaqueReturnType(final String functionName) {
      final JavaType res = returnsOpaqueJType.get(functionName);
      if( null == res ) {
          return null;
      }
      LOG.log(INFO, "ReturnsOpaque: {0} -> {1}", functionName, res);
      return res;
  }

  /**
   * Returns the opaque {@link JavaType} for the given function {@link AliasedSymbol}
   * or {@code null} if not opaque.
   * <p>
   * {@code ReturnsOpaque <Primitive Java Type> <Function Name>}
   * </p>
   */
  public JavaType getOpaqueReturnType(final AliasedSymbol symbol) {
      final String name = symbol.getName();
      final Set<String> aliases = symbol.getAliasedNames();
      JavaType res = returnsOpaqueJType.get(name);
      if( null == res ) {
          res = oneInMap(returnsOpaqueJType, aliases);
          if( null == res ) {
              return null;
          }
      }
      LOG.log(INFO, getASTLocusTag(symbol), "ReturnsOpaque: {0} -> {1}", symbol, res);
      return res;
  }

  /**
   * Variant of {@link #shouldIgnoreInInterface(AliasedSymbol)},
   * where this method only considers the {@link AliasedSymbol#getName() current-name}
   * of the given symbol, not the {@link #getJavaSymbolRename(String) renamed-symbol}.
   */
  public final boolean shouldIgnoreInInterface(final String symbol) {
      return shouldIgnoreInInterface( new AliasedSymbol.NoneAliasedSymbol(symbol) );
  }
  /**
   * Returns true if this aliased symbol should be ignored
   * during glue code generation of interfaces and implementation.
   * <p>
   * Both, the {@link AliasedSymbol#getName() current-name}
   * and all {@link AliasedSymbol#getAliasedNames() aliases} shall be considered.
   * </p>
   * <p>
   * Implementation calls {@link #shouldIgnoreInInterface_Int(AliasedSymbol)}
   * and overriding implementations shall ensure its being called as well!
   * </p>
   * @param symbol the symbolic aliased name to check for exclusion
   */
  public boolean shouldIgnoreInInterface(final AliasedSymbol symbol) {
      return shouldIgnoreInInterface_Int(symbol);
  }

  protected final boolean shouldIgnoreInInterface_Int(final AliasedSymbol symbol) {
      if( GlueGen.debug() ) {
          logIgnoresOnce();
      }
      final String name = symbol.getName();
      final Set<String> aliases = symbol.getAliasedNames();

      // Simple case-1; the symbol (orig or renamed) is in the interface ignore table
      if ( extendedIntfSymbolsIgnore.contains( name ) ||
           oneInSet(extendedIntfSymbolsIgnore, aliases)
         )
      {
          LOG.log(INFO, getASTLocusTag(symbol), "Ignore Intf ignore (one): {0}", symbol);
          return true;
      }
      // Simple case-2; the entire symbol (orig and renamed) is _not_ in the not-empty interface only table
      if ( !extendedIntfSymbolsOnly.isEmpty() &&
           !extendedIntfSymbolsOnly.contains( name ) &&
           !oneInSet(extendedIntfSymbolsOnly, aliases) ) {
          LOG.log(INFO, getASTLocusTag(symbol), "Ignore Intf !extended (all): {0}", symbol);
          return true;
      }
      return shouldIgnoreInImpl_Int(symbol);
  }

  /**
   * Returns true if this aliased symbol should be ignored
   * during glue code generation of implementation only.
   * <p>
   * Both, the {@link AliasedSymbol#getName() current-name}
   * and all {@link AliasedSymbol#getAliasedNames() aliases} shall be considered.
   * </p>
   * <p>
   * Implementation calls {@link #shouldIgnoreInImpl_Int(AliasedSymbol)}
   * and overriding implementations shall ensure its being called as well!
   * </p>
   * @param symbol the symbolic aliased name to check for exclusion
   */
  public boolean shouldIgnoreInImpl(final AliasedSymbol symbol) {
    return shouldIgnoreInImpl_Int(symbol);
  }

  protected final boolean shouldIgnoreInImpl_Int(final AliasedSymbol symbol) {
      final String name = symbol.getName();
      final Set<String> aliases = symbol.getAliasedNames();

      // Simple case-1; the symbol (orig or renamed) is in the interface ignore table
      if ( extendedImplSymbolsIgnore.contains( name ) ||
           oneInSet(extendedImplSymbolsIgnore, aliases)
         )
      {
          LOG.log(INFO, getASTLocusTag(symbol), "Ignore Impl ignore (one): {0}", symbol);
          return true;
      }
      // Simple case-2; the entire symbol (orig and renamed) is _not_ in the not-empty interface only table
      if ( !extendedImplSymbolsOnly.isEmpty() &&
           !extendedImplSymbolsOnly.contains( name ) &&
           !oneInSet(extendedImplSymbolsOnly, aliases) ) {
          LOG.log(INFO, getASTLocusTag(symbol), "Ignore Impl !extended (all): {0}", symbol);
          return true;
      }

      // Ok, the slow case. We need to check the entire table, in case the table
      // contains an regular expression that matches the symbol.
      for (final Pattern ignoreRegexp : ignores) {
          final Matcher matcher = ignoreRegexp.matcher(name);
          if ( matcher.matches() || onePatternMatch(ignoreRegexp, aliases) ) {
              LOG.log(INFO, getASTLocusTag(symbol), "Ignore Impl RegEx: {0}", symbol);
              return true;
          }
      }

      // Check negated ignore table if not empty
      if (ignoreNots.size() > 0) {
          // Ok, the slow case. We need to check the entire table, in case the table
          // contains an regular expression that matches the symbol.
          for (final Pattern ignoreNotRegexp : ignoreNots) {
              final Matcher matcher = ignoreNotRegexp.matcher(name);
              if ( !matcher.matches() && !onePatternMatch(ignoreNotRegexp, aliases) ) {
                  // Special case as this is most often likely to be the case.
                  // Unignores are not used very often.
                  if(unignores.isEmpty()) {
                      LOG.log(INFO, getASTLocusTag(symbol), "Ignore Impl unignores==0: {0} -> {1}", symbol, name);
                      return true;
                  }
                  boolean unignoreFound = false;
                  for (final Pattern unignoreRegexp : unignores) {
                      final Matcher unignoreMatcher = unignoreRegexp.matcher(name);
                      if ( unignoreMatcher.matches() || onePatternMatch(unignoreRegexp, aliases) ) {
                          unignoreFound = true;
                          break;
                      }
                  }

                  if (!unignoreFound) {
                      LOG.log(INFO, getASTLocusTag(symbol), "Ignore Impl !unignore: {0} -> {1}", symbol, name);
                      return true;
                  }
              }
          }
      }
      return false;
  }

  /** Returns true if this function should be given a body which
      throws a run-time exception with an "unimplemented" message
      during glue code generation. */
  public boolean isUnimplemented(final AliasedSymbol symbol) {
      // Ok, the slow case. We need to check the entire table, in case the table
      // contains an regular expression that matches the symbol.
      for (final Pattern unimplRegexp : unimplemented) {
          final Matcher matcher = unimplRegexp.matcher(symbol.getName());
          if ( matcher.matches() || onePatternMatch(unimplRegexp, symbol.getAliasedNames()) ) {
              return true;
          }
      }
      return false;
  }


  /**
   * Return a set of aliased-name for comment in docs.
   * <p>
   * This is usually {@link AliasedSymbol#addAliasedName(String)},
   * however an implementation may choose otherwise.
   * </p>
   * @param symbol the aliased symbol to retrieve the aliases
   * @return set of aliased-names or {@code null}.
   */
  public Set<String> getAliasedDocNames(final AliasedSymbol symbol) {
      return symbol.getAliasedNames();
  }

  /** Returns a replacement name for this type, which should be the
      name of a Java wrapper class for a C struct, or the name
      unchanged if no RenameJavaType directive was specified for this
      type. */
  public String renameJavaType(final String javaTypeName) {
    final String rename = javaTypeRenames.get(javaTypeName);
    if (rename != null) {
      return rename;
    }
    return javaTypeName;
  }

  /** Returns a replacement name for this function or definition which
      should be used as the Java name for the bound method or
      constant. If a function, it still calls the originally-named C
      function under the hood. Returns null if this symbol has not
      been explicitly renamed. */
  public String getJavaSymbolRename(final String origName) {
    if( LOG.isLoggable(INFO) ) {
        logRenamesOnce();
    }
    return javaSymbolRenames.get(origName);
  }

  /** Returns a set of replaced names to the given <code>aliasedName</code>. */
  public Set<String> getRenamedJavaSymbols(final String aliasedName) {
    return javaRenamedSymbols.get(aliasedName);
  }

  /** Programmatically adds a rename directive for the given symbol. */
  public void addJavaSymbolRename(final String origName, final String newName) {
    LOG.log(INFO, "\tRename {0} -> {1}", origName, newName);
    final String prevValue = javaSymbolRenames.put(origName, newName);
    if(null != prevValue && !prevValue.equals(newName)) {
        throw new RuntimeException("Rename-Override Attampt: "+origName+" -> "+newName+
                                   ", but "+origName+" -> "+prevValue+" already exist. Run in 'debug' mode to analyze!");
    }
    Set<String> origNames = javaRenamedSymbols.get(newName);
    if(null == origNames) {
        origNames = new HashSet<String>();
        javaRenamedSymbols.put(newName, origNames);
    }
    origNames.add(origName);
  }

  /** Programmatically adds a delegate implementation directive for the given symbol. */
  public void addDelegateImplementation(final String origName, final String renamedImpl) {
    LOG.log(INFO, "\tDelegateImplementation {0} -> {1}", origName, renamedImpl);
    final String prevValue = delegatedImplementation.put(origName, renamedImpl);
    if(null != prevValue && !prevValue.equals(renamedImpl)) {
        throw new RuntimeException("Rename-Override Attampt: "+origName+" -> "+renamedImpl+
                                   ", but "+origName+" -> "+prevValue+" already exist. Run in 'debug' mode to analyze!");
    }
  }

  /** Returns true if the emission style is AllStatic. */
  public boolean allStatic() {
    return emissionStyle == AllStatic;
  }

  /** Returns true if an interface should be emitted during glue code generation. */
  public boolean emitInterface() {
    return emissionStyle() == InterfaceAndImpl || emissionStyle() == InterfaceOnly;
  }

  /** Returns true if an implementing class should be emitted during glue code generation. */
  public boolean emitImpl() {
    return emissionStyle() == AllStatic || emissionStyle() == InterfaceAndImpl || emissionStyle() == ImplOnly;
  }

  /** Returns a list of Strings which should be emitted as a prologue
      to the body for the Java-side glue code for the given method.
      Returns null if no prologue was specified. */
  public List<String> javaPrologueForMethod(final MethodBinding binding,
                                                final boolean forImplementingMethodCall,
                                                final boolean eraseBufferAndArrayTypes) {
    List<String> res = javaPrologues.get(binding.getName());
    if (res == null) {
      // Try again with method name and descriptor
      res = javaPrologues.get(binding.getName() + binding.getDescriptor(forImplementingMethodCall, eraseBufferAndArrayTypes));
    }
    return res;
  }

  /** Returns a list of Strings which should be emitted as an epilogue
      to the body for the Java-side glue code for the given method.
      Returns null if no epilogue was specified. */
  public List<String> javaEpilogueForMethod(final MethodBinding binding,
                                                final boolean forImplementingMethodCall,
                                                final boolean eraseBufferAndArrayTypes) {
    List<String> res = javaEpilogues.get(binding.getName());
    if (res == null) {
      // Try again with method name and descriptor
      res = javaEpilogues.get(binding.getName() + binding.getDescriptor(forImplementingMethodCall, eraseBufferAndArrayTypes));
    }
    return res;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  protected void dispatch(final String cmd, final StringTokenizer tok, final File file, final String filename, final int lineNo) throws IOException {
    //System.err.println("read cmd = [" + cmd + "]");
    if (cmd.equalsIgnoreCase("Package")) {
      packageName = readString("package", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("GlueGenRuntimePackage")) {
      gluegenRuntimePackage = readString("GlueGenRuntimePackage", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ImplPackage")) {
      implPackageName = readString("ImplPackage", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("JavaClass")) {
      className = readString("JavaClass", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ImplJavaClass")) {
      implClassName = readString("ImplJavaClass", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("JavaOutputDir")) {
      javaOutputDir = readString("JavaOutputDir", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("NativeOutputDir")) {
      nativeOutputDir = readString("NativeOutputDir", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("HierarchicalNativeOutput")) {
      final String tmp = readString("HierarchicalNativeOutput", tok, filename, lineNo);
      nativeOutputUsesJavaHierarchy = Boolean.valueOf(tmp).booleanValue();
    } else if (cmd.equalsIgnoreCase("TagNativeBinding")) {
      tagNativeBinding = readBoolean("TagNativeBinding", tok, filename, lineNo).booleanValue();
    } else if (cmd.equalsIgnoreCase("RelaxedEqualSemanticsTest")) {
      relaxedEqualSemanticsTest = readBoolean("RelaxedEqualSemanticsTest", tok, filename, lineNo).booleanValue();
      TypeConfig.setRelaxedEqualSemanticsTest(relaxedEqualSemanticsTest); // propagate ..
    } else if (cmd.equalsIgnoreCase("Style")) {
        try{
          emissionStyle = EmissionStyle.valueOf(readString("Style", tok, filename, lineNo));
        }catch(final IllegalArgumentException ex) {
            LOG.log(WARNING, "Error parsing \"style\" command at line {0} in file \"{1}\"", lineNo, filename);
        }
    } else if (cmd.equalsIgnoreCase("AccessControl")) {
      readAccessControl(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("Import")) {
      imports.add(readString("Import", tok, filename, lineNo));
    } else if (cmd.equalsIgnoreCase("Opaque")) {
      readOpaque(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ReturnsString")) {
      readReturnsString(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ReturnsOpaque")) {
      readReturnsOpaque(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ReturnedArrayLength")) {
      readReturnedArrayLength(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because ReturnedArrayLength changes them.
    } else if (cmd.equalsIgnoreCase("ArgumentIsString")) {
      readArgumentIsString(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ExtendedInterfaceSymbolsIgnore")) {
      readExtendedIntfImplSymbols(tok, filename, lineNo, true, false, false);
    } else if (cmd.equalsIgnoreCase("ExtendedInterfaceSymbolsOnly")) {
      readExtendedIntfImplSymbols(tok, filename, lineNo, true, false, true);
    } else if (cmd.equalsIgnoreCase("ExtendedImplementationSymbolsIgnore")) {
      readExtendedIntfImplSymbols(tok, filename, lineNo, false, true, false);
    } else if (cmd.equalsIgnoreCase("ExtendedImplementationSymbolsOnly")) {
      readExtendedIntfImplSymbols(tok, filename, lineNo, false, true, true);
    } else if (cmd.equalsIgnoreCase("ExtendedIntfAndImplSymbolsIgnore")) {
      readExtendedIntfImplSymbols(tok, filename, lineNo, true, true, false);
    } else if (cmd.equalsIgnoreCase("ExtendedIntfAndImplSymbolsOnly")) {
      readExtendedIntfImplSymbols(tok, filename, lineNo, true, true, true);
    } else if (cmd.equalsIgnoreCase("Ignore")) {
      readIgnore(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("Unignore")) {
      readUnignore(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("IgnoreNot")) {
      readIgnoreNot(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("Unimplemented")) {
      readUnimplemented(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("IgnoreField")) {
      readIgnoreField(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ManuallyImplement")) {
      readManuallyImplement(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ManualStaticInitCall")) {
      readManualStaticInitCall(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ForceStaticInitCode")) {
      readForceStaticInitCode(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("CustomJavaCode")) {
      readCustomJavaCode(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because readCustomJavaCode changes them.
    } else if (cmd.equalsIgnoreCase("CustomCCode")) {
      readCustomCCode(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because readCustomCCode changes them.
    } else if (cmd.equalsIgnoreCase("MethodJavadoc")) {
      readMethodJavadoc(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because readMethodJavadoc changes them.
    } else if (cmd.equalsIgnoreCase("ClassJavadoc")) {
      readClassJavadoc(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because readClassJavadoc changes them.
    } else if (cmd.equalsIgnoreCase("NIOOnly")) {
      final String funcName = readString("NIOOnly", tok, filename, lineNo);
      if(funcName.equals("__ALL__")) {
          forceUseNIOOnly4All=true;
      } else {
          addUseNIOOnly( funcName );
      }
    } else if (cmd.equalsIgnoreCase("NIODirectOnly")) {
      final String funcName = readString("NIODirectOnly", tok, filename, lineNo);
      if(funcName.equals("__ALL__")) {
          forceUseNIODirectOnly4All=true;
      } else {
          addUseNIODirectOnly( funcName );
      }
    } else if (cmd.equalsIgnoreCase("EmitStruct")) {
      forcedStructs.add(readString("EmitStruct", tok, filename, lineNo));
    } else if (cmd.equalsIgnoreCase("StructPackage")) {
      readStructPackage(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("TemporaryCVariableDeclaration")) {
      readTemporaryCVariableDeclaration(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because TemporaryCVariableDeclaration changes them.
    } else if (cmd.equalsIgnoreCase("TemporaryCVariableAssignment")) {
      readTemporaryCVariableAssignment(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because TemporaryCVariableAssignment changes them.
    } else if (cmd.equalsIgnoreCase("StructMachineDataInfoIndex")) {
      readStructMachineDataInfoIndex(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because StructMachineDescriptorIndex changes them.
    } else if (cmd.equalsIgnoreCase("ReturnValueCapacity")) {
      readReturnValueCapacity(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because ReturnValueCapacity changes them.
    } else if (cmd.equalsIgnoreCase("ReturnValueLength")) {
      readReturnValueLength(tok, filename, lineNo);
      // Warning: make sure delimiters are reset at the top of this loop
      // because ReturnValueLength changes them.
    } else if (cmd.equalsIgnoreCase("Include")) {
      doInclude(tok, file, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("IncludeAs")) {
      doIncludeAs(tok, file, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("Extends")) {
      readExtend(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("Implements")) {
      readImplements(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("ParentClass")) {
      readParentClass(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("RenameJavaType")) {
      readRenameJavaType(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("RenameJavaSymbol")) {
      readRenameJavaSymbol(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("DelegateImplementation")) {
      readDelegateImplementation(tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("RuntimeExceptionType")) {
      runtimeExceptionType = readString("RuntimeExceptionType", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("UnsupportedExceptionType")) {
      unsupportedExceptionType = readString("UnsupportedExceptionType", tok, filename, lineNo);
    } else if (cmd.equalsIgnoreCase("JavaPrologue")) {
      readJavaPrologueOrEpilogue(tok, filename, lineNo, true);
      // Warning: make sure delimiters are reset at the top of this loop
      // because readJavaPrologueOrEpilogue changes them.
    } else if (cmd.equalsIgnoreCase("JavaEpilogue")) {
      readJavaPrologueOrEpilogue(tok, filename, lineNo, false);
      // Warning: make sure delimiters are reset at the top of this loop
      // because readJavaPrologueOrEpilogue changes them.
    } else if (cmd.equalsIgnoreCase("RangeCheck")) {
      readRangeCheck(tok, filename, lineNo, false);
      // Warning: make sure delimiters are reset at the top of this loop
      // because RangeCheck changes them.
    } else if (cmd.equalsIgnoreCase("RangeCheckBytes")) {
      readRangeCheck(tok, filename, lineNo, true);
      // Warning: make sure delimiters are reset at the top of this loop
      // because RangeCheckBytes changes them.
    } else {
      throw new RuntimeException("Unknown command \"" + cmd +
                                 "\" in command file " + filename +
                                 " at line number " + lineNo);
    }
  }

  protected String readString(final String cmd, final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      return tok.nextToken();
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"" + cmd + "\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected Boolean readBoolean(final String cmd, final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      return Boolean.valueOf(tok.nextToken());
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"" + cmd + "\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected boolean value", e);
    }
  }

  protected Class<?> stringToPrimitiveType(final String type) throws ClassNotFoundException {
    if (type.equals("boolean")) return Boolean.TYPE;
    if (type.equals("byte"))    return Byte.TYPE;
    if (type.equals("char"))    return Character.TYPE;
    if (type.equals("short"))   return Short.TYPE;
    if (type.equals("int"))     return Integer.TYPE;
    if (type.equals("long"))    return Long.TYPE;
    if (type.equals("float"))   return Float.TYPE;
    if (type.equals("double"))  return Double.TYPE;
    throw new RuntimeException("Only primitive types are supported here");
  }

    protected void readAccessControl(final StringTokenizer tok, final String filename, final int lineNo) {
        try {
            final String methodName = tok.nextToken();
            final String style = tok.nextToken();
            final MethodAccess access = MethodAccess.valueOf(style.toUpperCase());
            accessControl.put(methodName, access);
        } catch (final Exception e) {
            throw new RuntimeException("Error parsing \"AccessControl\" command at line " + lineNo
                    + " in file \"" + filename + "\"", e);
        }
    }

  protected void readOpaque(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final JavaType javaType = JavaType.createForOpaqueClass(stringToPrimitiveType(tok.nextToken()));
      String cType = null;
      while (tok.hasMoreTokens()) {
        if (cType == null) {
          cType = tok.nextToken();
        } else {
          cType = cType + " " + tok.nextToken();
        }
      }
      if (cType == null) {
        throw new RuntimeException("No C type for \"Opaque\" command at line " + lineNo +
          " in file \"" + filename + "\"");
      }
      final TypeInfo info = parseTypeInfo(cType, javaType);
      addTypeInfo(info);
    } catch (final Exception e) {
      throw new RuntimeException("Error parsing \"Opaque\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readReturnsOpaque(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final JavaType javaType = JavaType.createForOpaqueClass(stringToPrimitiveType(tok.nextToken()));
      final String funcName = tok.nextToken();
      returnsOpaqueJType.put(funcName, javaType);
    } catch (final Exception e) {
      throw new RuntimeException("Error parsing \"ReturnsOpaque\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readReturnsString(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String name = tok.nextToken();
      returnsString.add(name);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ReturnsString\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readReturnedArrayLength(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      returnedArrayLengths.put(functionName, restOfLine);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ReturnedArrayLength\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readExtendedIntfImplSymbols(final StringTokenizer tok, final String filename, final int lineNo, final boolean forInterface, final boolean forImplementation, final boolean onlyList) {
    File javaFile;
    BufferedReader javaReader;
    try {
      javaFile  = new File(tok.nextToken());
      javaReader = new BufferedReader(new FileReader(javaFile));
    } catch (final FileNotFoundException e) {
      throw new RuntimeException(e);
    }

    final JavaLexer lexer = new JavaLexer(javaReader);
    lexer.setFilename(javaFile.getName());

    final JavaParser parser = new JavaParser(lexer);
    parser.setFilename(javaFile.getName());

    try {
        parser.compilationUnit();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    final Set<String> parsedEnumNames = parser.getParsedEnumNames();
    final Set<String> parsedFuncNames = parser.getParsedFunctionNames();

    if(forInterface) {
        if(onlyList) {
            extendedIntfSymbolsOnly.addAll(parsedEnumNames);
            extendedIntfSymbolsOnly.addAll(parsedFuncNames);
        } else {
            extendedIntfSymbolsIgnore.addAll(parsedEnumNames);
            extendedIntfSymbolsIgnore.addAll(parsedFuncNames);
        }
    }
    if(forImplementation) {
        if(onlyList) {
            extendedImplSymbolsOnly.addAll(parsedEnumNames);
            extendedImplSymbolsOnly.addAll(parsedFuncNames);
        } else {
            extendedImplSymbolsIgnore.addAll(parsedEnumNames);
            extendedImplSymbolsIgnore.addAll(parsedFuncNames);
        }
    }
  }

  protected void readIgnore(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String regex = tok.nextToken();
      final Pattern pattern = Pattern.compile(regex);
      ignores.add(pattern);
      ignoreMap.put(regex, pattern);
      //System.err.println("IGNORING " + regex + " / " + ignores.get(regex));
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Ignore\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readUnignore(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String regex = tok.nextToken();
      Pattern pattern = ignoreMap.get(regex);
      ignoreMap.remove(regex);
      ignores.remove(pattern);

      // If the pattern wasn't registered before, then make sure we have a
      // valid pattern instance to put into the unignores set.
      if(pattern == null)
        pattern = Pattern.compile(regex);
      unignores.add(pattern);

      //System.err.println("UN-IGNORING " + regex + " / " + ignores.get(regex));
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Unignore\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readIgnoreNot(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String regex = tok.nextToken();
      ignoreNots.add(Pattern.compile(regex));
      //System.err.println("IGNORING NEGATION OF " + regex + " / " + ignores.get(regex));
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"IgnoreNot\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readUnimplemented(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String regex = tok.nextToken();
      unimplemented.add(Pattern.compile(regex));
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Unimplemented\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readIgnoreField(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String containingStruct = tok.nextToken();
      final String name = tok.nextToken();
      ignores.add(Pattern.compile(containingStruct + "\\." + name));
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"IgnoreField\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readManuallyImplement(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String name = tok.nextToken();
      manuallyImplement.add(name);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ManuallyImplement\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }
  protected void readManualStaticInitCall(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String name = tok.nextToken();
      manualStaticInitCall.add(name);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ManualStaticInitCall\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }
  protected void readForceStaticInitCode(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String name = tok.nextToken();
      forceStaticInitCode.add(name);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ForceStaticInitCode\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readCustomJavaCode(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String tokenClassName = tok.nextToken();
      try {
          final String restOfLine = tok.nextToken("\n\r\f");
          addCustomJavaCode(tokenClassName, restOfLine);
      } catch (final NoSuchElementException e) {
          addCustomJavaCode(tokenClassName, "");
      }
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"CustomJavaCode\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void addCustomJavaCode(final String className, final String code) {
    final List<String> codeList = customJavaCodeForClass(className);
    codeList.add(code);
  }

  protected void readCustomCCode(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String restOfLine = tok.nextToken("\n\r\f");
      customCCode.add(restOfLine);
    } catch (final NoSuchElementException e) {
      customCCode.add("");
    }
  }

  protected void readMethodJavadoc(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String tokenClassName = tok.nextToken();
      final String restOfLine = tok.nextToken("\n\r\f");
      addMethodJavadoc(tokenClassName, restOfLine);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"MethodJavadoc\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }
  protected void addMethodJavadoc(final String methodName, final String code) {
    final List<String> codeList = javadocForMethod(methodName);
    codeList.add(code);
  }

  protected void readClassJavadoc(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String tokenClassName = tok.nextToken();
      final String restOfLine = tok.nextToken("\n\r\f");
      addClassJavadoc(tokenClassName, restOfLine);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ClassJavadoc\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void addClassJavadoc(final String className, final String code) {
    final List<String> codeList = javadocForClass(className);
    codeList.add(code);
  }

 /**
   * When const char* arguments in the C function prototypes are encountered,
   * the emitter will normally convert them to <code>byte[]</code>
   * arguments. This directive lets you specify which of those arguments
   * should be converted to <code>String</code> arguments instead of <code>
   * byte[] </code>. <p>
   *
   * For example, given the C prototype:
   * <pre>
   * void FuncName(const char* ugh, int bar, const char *foo, const char* goop);
   * </pre>
   *
   * The emitter will normally emit:
   * <pre>
   * public abstract void FuncName(byte[] ugh, int bar, byte[] foo, byte[] goop);
   * </pre>
   *
   * However, if you supplied the following directive:
   *
   * <pre>
   * ArgumentIsString FuncName 0 2
   * </pre>
   *
   * The emitter will instead emit:
   * <pre>
   * public abstract void FuncName(String ugh, int bar, String foo, byte[] goop);
   * </pre>
   *
   */
  protected void readArgumentIsString(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String methodName = tok.nextToken();
      final ArrayList<Integer> argIndices = new ArrayList<Integer>(2);
      while (tok.hasMoreTokens()) {
        final Integer idx = Integer.valueOf(tok.nextToken());
        argIndices.add(idx);
      }

      if (argIndices.size() > 0) {
        argumentsAreString.put(methodName, argIndices);
      } else {
        throw new RuntimeException("ERROR: Error parsing \"ArgumentIsString\" command at line " + lineNo +
          " in file \"" + filename + "\": directive requires specification of at least 1 index");
      }
    } catch (final NoSuchElementException e) {
      throw new RuntimeException(
        "Error parsing \"ArgumentIsString\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readStructPackage(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String struct = tok.nextToken();
      final String pkg = tok.nextToken();
      structPackages.put(struct, pkg);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"StructPackage\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readStructMachineDataInfoIndex(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String structName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      structMachineDataInfoIndex.put(structName, restOfLine);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"StructMachineDataInfoIndex\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readReturnValueCapacity(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      returnValueCapacities.put(functionName, restOfLine);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ReturnValueCapacity\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readReturnValueLength(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      returnValueLengths.put(functionName, restOfLine);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ReturnValueLength\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readTemporaryCVariableDeclaration(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      List<String> list = temporaryCVariableDeclarations.get(functionName);
      if (list == null) {
        list = new ArrayList<String>();
        temporaryCVariableDeclarations.put(functionName, list);
      }
      list.add(restOfLine);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"TemporaryCVariableDeclaration\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readTemporaryCVariableAssignment(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String functionName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      List<String> list = temporaryCVariableAssignments.get(functionName);
      if (list == null) {
        list = new ArrayList<String>();
        temporaryCVariableAssignments.put(functionName, list);
      }
      list.add(restOfLine);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"TemporaryCVariableAssignment\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void doInclude(final StringTokenizer tok, final File file, final String filename, final int lineNo) throws IOException {
    try {
      final String includedFilename = tok.nextToken();
      File includedFile = new File(includedFilename);
      if (!includedFile.isAbsolute()) {
        includedFile = new File(file.getParentFile(), includedFilename);
      }
      read(includedFile.getAbsolutePath());
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Include\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void doIncludeAs(final StringTokenizer tok, final File file, final String filename, final int lineNo) throws IOException {
    try {
      final StringBuilder linePrefix = new StringBuilder(128);
      while (tok.countTokens() > 1)
      {
        linePrefix.append(tok.nextToken());
        linePrefix.append(" ");
      }
      // last token is filename
      final String includedFilename = tok.nextToken();
      File includedFile = new File(includedFilename);
      if (!includedFile.isAbsolute()) {
        includedFile = new File(file.getParentFile(), includedFilename);
      }
      read(includedFile.getAbsolutePath(), linePrefix.toString());
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"IncludeAs\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected void readExtend(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String interfaceName = tok.nextToken();
      final List<String> intfs = extendedInterfaces(interfaceName);
      intfs.add(tok.nextToken());
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Extends\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected void readImplements(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String tokenClassName = tok.nextToken();
      final List<String> intfs = implementedInterfaces(tokenClassName);
      intfs.add(tok.nextToken());
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"Implements\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected void readParentClass(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String tokenClassName = tok.nextToken();
      parentClass.put(tokenClassName, tok.nextToken());
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"ParentClass\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected void readRenameJavaType(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String fromName = tok.nextToken();
      final String toName   = tok.nextToken();
      javaTypeRenames.put(fromName, toName);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"RenameJavaType\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected void readRenameJavaSymbol(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String fromName = tok.nextToken();
      final String toName   = tok.nextToken();
      addJavaSymbolRename(fromName, toName);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"RenameJavaSymbol\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  public void readDelegateImplementation(final StringTokenizer tok, final String filename, final int lineNo) {
    try {
      final String fromName = tok.nextToken();
      final String toName   = tok.nextToken();
      addDelegateImplementation(fromName, toName);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"DelegateImplementation\" command at line " + lineNo +
        " in file \"" + filename + "\": missing expected parameter", e);
    }
  }

  protected void readJavaPrologueOrEpilogue(final StringTokenizer tok, final String filename, final int lineNo, final boolean prologue) {
    try {
      String methodName = tok.nextToken();
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      if (startsWithDescriptor(restOfLine)) {
        // Assume it starts with signature for disambiguation
        final int spaceIdx = restOfLine.indexOf(' ');
        if (spaceIdx > 0) {
          final String descriptor = restOfLine.substring(0, spaceIdx);
          restOfLine = restOfLine.substring(spaceIdx + 1, restOfLine.length());
          methodName = methodName + descriptor;
        }
      }
      addJavaPrologueOrEpilogue(methodName, restOfLine, prologue);
    } catch (final NoSuchElementException e) {
      throw new RuntimeException("Error parsing \"" +
                                 (prologue ? "JavaPrologue" : "JavaEpilogue") +
                                 "\" command at line " + lineNo +
                                 " in file \"" + filename + "\"", e);
    }
  }

  protected void addJavaPrologueOrEpilogue(final String methodName, final String code, final boolean prologue) {
    final Map<String, List<String>> codes = (prologue ? javaPrologues : javaEpilogues);
    List<String> data = codes.get(methodName);
    if (data == null) {
      data = new ArrayList<String>();
      codes.put(methodName, data);
    }
    data.add(code);
  }

  protected void readRangeCheck(final StringTokenizer tok, final String filename, final int lineNo, final boolean inBytes) {
    try {
      final String functionName = tok.nextToken();
      final int argNum = Integer.parseInt(tok.nextToken());
      String restOfLine = tok.nextToken("\n\r\f");
      restOfLine = restOfLine.trim();
      // Construct a JavaPrologue for this
      addJavaPrologueOrEpilogue(functionName,
                                "Buffers.rangeCheck" +
                                (inBytes ? "Bytes" : "") +
                                "({" + argNum + "}, " + restOfLine + ");",
                                true);
    } catch (final Exception e) {
      throw new RuntimeException("Error parsing \"RangeCheck" + (inBytes ? "Bytes" : "") + "\" command at line " + lineNo +
        " in file \"" + filename + "\"", e);
    }
  }

  protected static TypeInfo parseTypeInfo(final String cType, final JavaType javaType) {
    String typeName = null;
    int pointerDepth = 0;
    int idx = 0;
    while (idx < cType.length() &&
           (cType.charAt(idx) != ' ') &&
           (cType.charAt(idx) != '*')) {
      ++idx;
    }
    typeName = cType.substring(0, idx);
    // Count pointer depth
    while (idx < cType.length()) {
      if (cType.charAt(idx) == '*') {
        ++pointerDepth;
      }
      ++idx;
    }
    return new TypeInfo(typeName, pointerDepth, javaType);
  }

  public TypeInfo addTypeInfo(final String alias, final Type superType) {
      final TypeInfo superInfo = typeInfo(superType);
      if( null != superInfo ) {
          final TypeInfo res = new TypeInfo(alias, superInfo.pointerDepth(), superInfo.javaType());
          addTypeInfo(res);
          return res;
      } else {
          return null;
      }
  }
  protected void addTypeInfo(final TypeInfo info) {
    TypeInfo tmp = typeInfoMap.get(info.name());
    if (tmp == null) {
      typeInfoMap.put(info.name(), info);
      return;
    }
    while (tmp.next() != null) {
      tmp = tmp.next();
    }
    tmp.setNext(info);
  }

  private static int nextIndexAfterType(final String s, int idx) {
    final int len = s.length();
    while (idx < len) {
      final char c = s.charAt(idx);

      if (Character.isJavaIdentifierStart(c) ||
          Character.isJavaIdentifierPart(c) ||
          (c == '/')) {
        idx++;
      } else if (c == ';') {
        return (idx + 1);
      } else {
        return -1;
      }
    }
    return -1;
  }

  private static int nextIndexAfterDescriptor(final String s, final int idx) {
    final char c = s.charAt(idx);
    switch (c) {
      case 'B':
      case 'C':
      case 'D':
      case 'F':
      case 'I':
      case 'J':
      case 'S':
      case 'Z':
      case 'V': return (1 + idx);
      case 'L': return nextIndexAfterType(s, idx + 1);
      case ')': return idx;
      default: break;
    }
    return -1;
  }

  protected static boolean startsWithDescriptor(final String s) {
    // Try to see whether the String s starts with a valid Java
    // descriptor.

    int idx = 0;
    final int len = s.length();
    while ((idx < len) && s.charAt(idx) == ' ') {
      ++idx;
    }

    if (idx >= len) return false;
    if (s.charAt(idx++) != '(')  return false;
    while (idx < len) {
      final int nextIdx = nextIndexAfterDescriptor(s, idx);
      if (nextIdx < 0) {
        return false;
      }
      if (nextIdx == idx) {
        // ')'
        break;
      }
      idx = nextIdx;
    }
    final int nextIdx = nextIndexAfterDescriptor(s, idx + 1);
    if (nextIdx < 0) {
      return false;
    }
    return true;
  }
}
