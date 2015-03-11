/*
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

import java.util.*;
import java.io.*;
import java.text.MessageFormat;

import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.gluegen.Logging.LoggerIf;
import com.jogamp.gluegen.cgram.types.*;

/** Emits the C-side component of the Java<->C JNI binding. */
public class CMethodBindingEmitter extends FunctionEmitter {

  protected static final CommentEmitter defaultCommentEmitter = new DefaultCommentEmitter();

  protected static final String arrayResLength = "_array_res_length";
  protected static final String arrayRes       = "_array_res";
  protected static final String arrayIdx       = "_array_idx";

  protected final LoggerIf LOG;

  protected MethodBinding binding;

  /** Name of the package in which the corresponding Java method resides.*/
  private final String packageName;

  /** Name of the class in which the corresponding Java method resides.*/
  private final String className;

  /**
   * Whether or not the Java<->C JNI binding for this emitter's MethodBinding
   * is overloaded.
   */
  private final boolean isOverloadedBinding;

  /**
   * Whether or not the Java-side of the Java<->C JNI binding for this
   * emitter's MethodBinding is static.
   */
  private final boolean isJavaMethodStatic;

  // Flags which change various aspects of glue code generation
  protected boolean forImplementingMethodCall;
  protected boolean forIndirectBufferAndArrayImplementation;

  /**
   * Optional List of Strings containing temporary C variables to declare.
   */
  private List<String> temporaryCVariableDeclarations;

  /**
   * Optional List of Strings containing assignments to temporary C variables
   * to make after the call is completed.
   */
  private List<String> temporaryCVariableAssignments;

  /**
   * Capacity of the return value in the event that it is encapsulated in a
   * java.nio.Buffer. Is ignored if binding.getJavaReturnType().isNIOBuffer()
   * == false;
   */
  private MessageFormat returnValueCapacityExpression = null;

  /**
   * Length of the returned array. Is ignored if
   * binding.getJavaReturnType().isArray() is false.
   */
  private MessageFormat returnValueLengthExpression = null;

  protected static final String STRING_CHARS_PREFIX = "_strchars_";

  // We need this in order to compute sizes of certain types
  protected MachineDataInfo machDesc;

  /**
   * Constructs an emitter for the specified binding, and sets a default
   * comment emitter that will emit the signature of the C function that is
   * being bound.
   */
  public CMethodBindingEmitter(final MethodBinding binding,
                               final PrintWriter output,
                               final String javaPackageName,
                               final String javaClassName,
                               final boolean isOverloadedBinding,
                               final boolean isJavaMethodStatic,
                               final boolean forImplementingMethodCall,
                               final boolean forIndirectBufferAndArrayImplementation,
                               final MachineDataInfo machDesc,
                               final JavaConfiguration configuration)
  {
    super(output, false, configuration);
    LOG = Logging.getLogger(CMethodBindingEmitter.class.getPackage().getName(), CMethodBindingEmitter.class.getSimpleName());

    assert(binding != null);
    assert(javaClassName != null);
    assert(javaPackageName != null);

    this.binding = binding;
    this.packageName = javaPackageName;
    this.className = javaClassName;
    this.isOverloadedBinding = isOverloadedBinding;
    this.isJavaMethodStatic = isJavaMethodStatic;

    this.forImplementingMethodCall = forImplementingMethodCall;
    this.forIndirectBufferAndArrayImplementation = forIndirectBufferAndArrayImplementation;
    this.machDesc = machDesc;

    setCommentEmitter(defaultCommentEmitter);
  }

  public final MethodBinding getBinding() { return binding; }

  @Override
  public String getInterfaceName() {
    return binding.getInterfaceName();
  }
  @Override
  public String getImplName() {
    return binding.getImplName();
  }
  @Override
  public String getNativeName() {
    return binding.getNativeName();
  }

  @Override
  public FunctionSymbol getCSymbol() {
      return binding.getCSymbol();
  }

  /**
   * Get the expression for the capacity of the returned java.nio.Buffer.
   */
  public final MessageFormat getReturnValueCapacityExpression()  {
    return returnValueCapacityExpression;
  }

  /**
   * If this function returns a void* encapsulated in a
   * java.nio.Buffer (or compound type wrapper), sets the expression
   * for the capacity of the returned Buffer.
   *
   * @param expression a MessageFormat which, when applied to an array
   * of type String[] that contains each of the arguments names of the
   * Java-side binding, returns an expression that will (when compiled
   * by a C compiler) evaluate to an integer-valued expression. The
   * value of this expression is the capacity of the java.nio.Buffer
   * returned from this method.
   *
   * @throws IllegalArgumentException if the <code>
   * binding.getJavaReturnType().isNIOBuffer() == false and
   * binding.getJavaReturnType().isCompoundTypeWrapper() == false
   * </code>
   */
  public final void setReturnValueCapacityExpression(final MessageFormat expression)  {
    returnValueCapacityExpression = expression;

    if (!binding.getJavaReturnType().isNIOBuffer() &&
        !binding.getJavaReturnType().isCompoundTypeWrapper())    {
      throw new IllegalArgumentException(
        "Cannot specify return value capacity for a method that does not " +
        "return java.nio.Buffer or a compound type wrapper: \"" + binding + "\"");
    }
  }

  /**
   * Get the expression for the length of the returned array
   */
  public final MessageFormat getReturnValueLengthExpression()  {
    return returnValueLengthExpression;
  }

  /**
   * If this function returns an array, sets the expression for the
   * length of the returned array.
   *
   * @param expression a MessageFormat which, when applied to an array
   * of type String[] that contains each of the arguments names of the
   * Java-side binding, returns an expression that will (when compiled
   * by a C compiler) evaluate to an integer-valued expression. The
   * value of this expression is the length of the array returned from
   * this method.
   *
   * @throws IllegalArgumentException if the <code>
   * binding.getJavaReturnType().isNIOBuffer() == false
   * </code>
   */
  public final void setReturnValueLengthExpression(final MessageFormat expression)  {
    returnValueLengthExpression = expression;

    if (!binding.getJavaReturnType().isArray() &&
        !binding.getJavaReturnType().isArrayOfCompoundTypeWrappers())    {
      throw new IllegalArgumentException(
        "Cannot specify return value length for a method that does not " +
        "return an array: \"" + binding + "\"");
    }
  }

  /**
   * Returns the List of Strings containing declarations for temporary
   * C variables to be assigned to after the underlying function call.
   */
  public final List<String> getTemporaryCVariableDeclarations() {
    return temporaryCVariableDeclarations;
  }

  /**
   * Sets up a List of Strings containing declarations for temporary C
   * variables to be assigned to after the underlying function call. A
   * null argument indicates that no manual declarations are to be made.
   */
  public final void setTemporaryCVariableDeclarations(final List<String> arg) {
    temporaryCVariableDeclarations = arg;
  }

  /**
   * Returns the List of Strings containing assignments for temporary
   * C variables which are made after the underlying function call. A
   * null argument indicates that no manual assignments are to be
   * made.
   */
  public final List<String> getTemporaryCVariableAssignments() {
    return temporaryCVariableAssignments;
  }

  /**
   * Sets up a List of Strings containing assignments for temporary C
   * variables which are made after the underlying function call. A
   * null argument indicates that no manual assignments are to be made.
   */
  public final void setTemporaryCVariableAssignments(final List<String> arg) {
    temporaryCVariableAssignments = arg;
  }

  /**
   * Get the name of the class in which the corresponding Java method
   * resides.
   */
  public String getJavaPackageName() { return packageName; }

  /**
   * Get the name of the package in which the corresponding Java method
   * resides.
   */
  public String getJavaClassName() { return className; }

  /**
   * Is the Java<->C JNI binding for this emitter's MethodBinding one of
   * several overloaded methods with the same name?
   */
  public final boolean getIsOverloadedBinding() { return isOverloadedBinding; }

  /**
   * Is the Java side of the Java<->C JNI binding for this emitter's
   * MethodBinding a static method?.
   */
  public final boolean getIsJavaMethodStatic() { return isJavaMethodStatic; }

  /**
   * Is this CMethodBindingEmitter implementing the case of an
   * indirect buffer or array being passed down to C code?
   */
  public final boolean forIndirectBufferAndArrayImplementation() { return forIndirectBufferAndArrayImplementation; }

  /**
   * Used for certain internal type size computations
   */
  public final MachineDataInfo getMachineDataInfo() { return machDesc; }


  @Override
  protected void emitReturnType(final PrintWriter writer)  {
    writer.print("JNIEXPORT ");
    writer.print(binding.getJavaReturnType().jniTypeName());
    writer.print(" JNICALL");
  }

  @Override
  protected void emitName(final PrintWriter writer)  {
    writer.println(); // start name on new line
    writer.print(JavaEmitter.getJNIMethodNamePrefix(getJavaPackageName(), getJavaClassName()));
    writer.print("_");
    if (isOverloadedBinding)    {
      writer.print(jniMangle(binding));
    } else {
      writer.print(JavaEmitter.jniMangle(getImplName()));
    }
  }

  protected String getImplSuffix() {
    if (forImplementingMethodCall) {
      if (forIndirectBufferAndArrayImplementation) {
        return "1";
      } else {
        return "0";
      }
    }
    return "";
  }

  @Override
  protected int emitArguments(final PrintWriter writer)  {
    writer.print("JNIEnv *env, ");
    int numEmitted = 1; // initially just the JNIEnv
    if (isJavaMethodStatic && !binding.hasContainingType())    {
      writer.print("jclass");
    } else {
      writer.print("jobject");
    }
    writer.print(" _unused");
    ++numEmitted;

    if (binding.hasContainingType())   {
      // "this" argument always comes down in argument 0 as direct buffer
      writer.print(", jobject " + JavaMethodBindingEmitter.javaThisArgumentName());
    }
    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType javaArgType = binding.getJavaArgumentType(i);
      // Handle case where only param is void
      if (javaArgType.isVoid()) {
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        assert(binding.getNumArguments() == 1);
        continue;
      }
      if (javaArgType.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        continue;
      }
      writer.print(", ");
      writer.print(javaArgType.jniTypeName());
      writer.print(" ");
      writer.print(binding.getArgumentName(i));
      ++numEmitted;

      if (javaArgType.isPrimitiveArray() ||
          javaArgType.isNIOBuffer()) {
        writer.print(", jint " + byteOffsetArgName(i));
        if(forIndirectBufferAndArrayImplementation) {
            writer.print(", jboolean " + isNIOArgName(i));
        }
      } else if (javaArgType.isNIOBufferArray()) {
        writer.print(", jintArray " +
                     byteOffsetArrayArgName(i));
      }
    }
    return numEmitted;
  }


  @Override
  protected void emitBody(final PrintWriter writer)  {
    writer.println(" {");
//    writer.println("printf(\" - - - - "+ getName() + getImplSuffix() +" - - - -\\n\");");
    emitBodyVariableDeclarations(writer);
    emitBodyUserVariableDeclarations(writer);
    emitBodyVariablePreCallSetup(writer);
    emitBodyCallCFunction(writer);
    emitBodyUserVariableAssignments(writer);
    emitBodyVariablePostCallCleanup(writer);
    emitBodyReturnResult(writer);
    writer.println("}");
    writer.println();
  }

  protected void emitBodyVariableDeclarations(final PrintWriter writer)  {
    // Emit declarations for all pointer and String conversion variables
    if (binding.hasContainingType()) {
      emitPointerDeclaration(writer,
                             binding.getContainingType(),
                             binding.getContainingCType(),
                             CMethodBindingEmitter.cThisArgumentName(),
                             null);
    }

    boolean emittedDataCopyTemps = false;
    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType type = binding.getJavaArgumentType(i);
      if (type.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        continue;
      }

      if (type.isArray() || type.isNIOBuffer() || type.isCompoundTypeWrapper() || type.isArrayOfCompoundTypeWrappers()) {
        final String javaArgName = binding.getArgumentName(i);
        final String convName = pointerConversionArgumentName(javaArgName);
        // handle array/buffer argument types
        final boolean needsDataCopy =
          emitPointerDeclaration(writer,
                                 type,
                                 binding.getCArgumentType(i),
                                 convName, javaArgName);
        if (needsDataCopy && !emittedDataCopyTemps) {
          // emit loop counter and array length variables used during data
          // copy
          writer.println("  jobject _tmpObj;");
          writer.println("  int _copyIndex;");
          writer.println("  jsize _tmpArrayLen;");

          // Pointer to the data in the Buffer, taking the offset into account
          if(type.isNIOBufferArray()) {
            writer.println("  int * _offsetHandle = NULL;");
          }

          emittedDataCopyTemps = true;
        }
      } else if (type.isString()) {
        final Type cType = binding.getCArgumentType(i);
        if (isUTF8Type(cType)) {
          writer.print("  const char* ");
        } else {
          writer.print("  jchar* ");
        }
        writer.print(STRING_CHARS_PREFIX);
        writer.print(binding.getArgumentName(i));
        writer.println(" = NULL;");
      }

    }

    // Emit declaration for return value if necessary
    final Type cReturnType = binding.getCReturnType();

    final JavaType javaReturnType = binding.getJavaReturnType();
    if (!cReturnType.isVoid()) {
      writer.print("  ");
      // Note we respect const/volatile in the function return type.
      // However, we cannot have it 'const' for our local variable.
      // See cast in emitBodyCallCFunction(..)!
      writer.print(binding.getCSymbol().getReturnType().getCName(false));
      writer.println(" _res;");
      if (javaReturnType.isNIOByteBufferArray() ||
          javaReturnType.isArrayOfCompoundTypeWrappers()) {
        writer.print("  int ");
        writer.print(arrayResLength);
        writer.println(";");
        writer.print("  int ");
        writer.print(arrayIdx);
        writer.println(";");
        writer.print("  jobjectArray ");
        writer.print(arrayRes);
        writer.println(";");
      } else if (javaReturnType.isArray()) {
        writer.print("  int ");
        writer.print(arrayResLength);
        writer.println(";");

        final Class<?> componentType = javaReturnType.getJavaClass().getComponentType();
        if (componentType.isArray()) {
          throw new RuntimeException("Multi-dimensional arrays not supported yet");
        }

        final String javaTypeName = componentType.getName();
        final String javaArrayTypeName = "j" + javaTypeName + "Array";
        writer.print("  ");
        writer.print(javaArrayTypeName);
        writer.print(" ");
        writer.print(arrayRes);
        writer.println(";");
      }
    }
  }

  /** Emits the user-defined C variable declarations from the
      TemporaryCVariableDeclarations directive in the .cfg file. */
  protected void emitBodyUserVariableDeclarations(final PrintWriter writer) {
    if (temporaryCVariableDeclarations != null) {
      for (final String val : temporaryCVariableDeclarations) {
        writer.print("  ");
        writer.println(val);
      }
    }
  }

  /** Checks a type to see whether it is for a UTF-8 pointer type
      (i.e., "const char *", "const char **"). False implies that this
      type is for a Unicode pointer type ("jchar *", "jchar **"). */
  protected boolean isUTF8Type(Type type) {
    final int i = 0;
    // Try to dereference the type at most two levels
    while (!type.isInt() && !type.isVoid() && (i < 2)) {
      final PointerType pt = type.asPointer();
      if (pt != null) {
        type = pt.getTargetType();
      } else {
        final ArrayType arrt = type.asArray();
        if (arrt == null) {
          throw new IllegalArgumentException("Type " + type + " should have been a pointer or array type");
        }
        type = arrt.getElementType();
      }
    }
    if (type.isVoid()) {
      // Assume UTF-8 since UTF-16 is rare
      return true;
    }
    if (!type.isInt()) {
      throw new IllegalArgumentException("Type " + type + " should have been a one- or two-dimensional integer pointer or array type");
    }
    if (type.getSize(machDesc) != 1 && type.getSize(machDesc) != 2) {
      throw new IllegalArgumentException("Type " + type + " should have been a one- or two-dimensional pointer to char or short");
    }
    return (type.getSize(machDesc) == 1);
  }

  /**
   * Code to init the variables that were declared in
   * emitBodyVariableDeclarations(), PRIOR TO calling the actual C
   * function.
   */
  protected void emitBodyVariablePreCallSetup(final PrintWriter writer)  {

    // Convert all Buffers to pointers first so we don't have to
    // call ReleasePrimitiveArrayCritical for any arrays if any
    // incoming buffers aren't direct
    if (binding.hasContainingType()) {
        emitPointerConversion(writer, binding,
                              binding.getContainingType(),
                              binding.getContainingCType(),
                              JavaMethodBindingEmitter.javaThisArgumentName(),
                              CMethodBindingEmitter.cThisArgumentName(),
                              null);
    }

    // Convert all arrays to pointers, and get UTF-8 versions of jstring args
    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType javaArgType = binding.getJavaArgumentType(i);

      if (javaArgType.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        continue;
      }
      final String javaArgName = binding.getArgumentName(i);

      if (javaArgType.isCompoundTypeWrapper() ||
          (javaArgType.isNIOBuffer() && !forIndirectBufferAndArrayImplementation ) ) {
        emitPointerConversion(writer, binding, javaArgType,
                              binding.getCArgumentType(i), javaArgName,
                              pointerConversionArgumentName(javaArgName),
                              byteOffsetArgName(i));
      } else if (javaArgType.isArray() ||
                 javaArgType.isArrayOfCompoundTypeWrappers() ||
                 ( javaArgType.isNIOBuffer() && forIndirectBufferAndArrayImplementation ) ) {
        final boolean needsDataCopy = javaArgTypeNeedsDataCopy(javaArgType);

        writer.println("  if ( NULL != " + javaArgName + " ) {");

        final Type cArgType = binding.getCArgumentType(i);
        String cArgTypeName = cArgType.getCName();

        final String convName = pointerConversionArgumentName(javaArgName);

        if (!needsDataCopy) {
          writer.print("    ");
          writer.print(convName);
          writer.print(" = (");
          if (javaArgType.isStringArray()) {
            // java-side type is String[]
            cArgTypeName = "jstring *";
          }
          writer.print(cArgTypeName);
          writer.print(") ( JNI_TRUE == " + isNIOArgName(i) + " ? ");
          writer.print(" (*env)->GetDirectBufferAddress(env, " + javaArgName + ") : ");
          writer.print(" (*env)->GetPrimitiveArrayCritical(env, " + javaArgName + ", NULL) );");
        } else {
          // Handle the case where the array elements are of a type that needs a
          // data copy operation to convert from the java memory model to the C
          // memory model (e.g., int[][], String[], etc)
          //
          // FIXME: should factor out this whole block of code into a separate
          // method for clarity and maintenance purposes
          //
          // Note that we properly handle only the case of an array of
          // compound type wrappers in emitBodyVariablePostCallCleanup below
          if (!cArgType.isBaseTypeConst() &&
              !javaArgType.isArrayOfCompoundTypeWrappers()) {
            // FIXME: if the arg type is non-const, the sematics might be that
            // the function modifies the argument -- we don't yet support
            // this.
            throw new GlueGenException(
              "Cannot copy data for ptr-to-ptr arg type \"" + cArgType.getDebugString() +
              "\": support for non-const ptr-to-ptr types not implemented: "+binding, binding.getCSymbol().getASTLocusTag());
          }

          writer.println();
          writer.println("    /* Copy contents of " + javaArgName + " into " + convName + "_copy */");

          // get length of array being copied
          final String arrayLenName = "_tmpArrayLen";
          writer.print("    ");
          writer.print(arrayLenName);
          writer.print(" = (*env)->GetArrayLength(env, ");
          writer.print(javaArgName);
          writer.println(");");

          // allocate an array to hold each element
          final Type cArgElementType, cArgElementType2;
          {
              int error = 0;
              if( cArgType.isPointer() ) {
                  cArgElementType = cArgType.asPointer().getTargetType();
                  if( cArgElementType.isPointer() ) {
                      // pointer-to-pointer
                      cArgElementType2 = cArgElementType.asPointer().getTargetType();
                      if( cArgElementType2.isPointer() ) {
                          error = 1;
                      }
                      if(cArgType.pointerDepth() != 2) {
                          error = 2;
                      }
                  } else {
                      cArgElementType2 = null;
                      if(cArgType.pointerDepth() != 1) {
                          error = 10;
                      }
                  }
              } else if( cArgType.isArray() ) {
                  cArgElementType = cArgType.asArray().getBaseElementType();
                  cArgElementType2 = null;
              } else {
                  cArgElementType = null;
                  cArgElementType2 = null;
                  error = 100;
              }
              if( 0 < error ) {
                throw new GlueGenException(
                  "Could not copy data for type \"" + cArgType.getDebugString() +
                  "\"; currently only pointer- and array-types are supported. (error "+error+"): "+binding,
                  binding.getCSymbol().getASTLocusTag());
              }
          }
          emitMalloc(
            writer,
            convName+"_copy",
            cArgElementType.getCName(),
            cArgType.isBaseTypeConst(),
            arrayLenName,
            "Could not allocate buffer for copying data in argument \\\""+javaArgName+"\\\"");

          // Get the handle for the byte offset array sent down for Buffers
          // FIXME: not 100% sure this is correct with respect to the
          // JNI spec because it may be illegal to call
          // GetObjectArrayElement while in a critical section. May
          // need to do another loop and add in the offsets.
          if (javaArgType.isNIOBufferArray()) {
            writer.println
              ("    _offsetHandle = (int *) (*env)->GetPrimitiveArrayCritical(env, " +
               byteOffsetArrayArgName(i) +
               ", NULL);");
          }

          // process each element in the array
          writer.println("    for (_copyIndex = 0; _copyIndex < "+arrayLenName+"; ++_copyIndex) {");

          // get each array element
          writer.println("      /* get each element of the array argument \"" + javaArgName + "\" */");
          writer.print("      _tmpObj = (*env)->GetObjectArrayElement(env, ");
          writer.print(javaArgName);
          writer.println(", _copyIndex);");

          if (javaArgType.isStringArray()) {
            writer.print("  ");
            emitGetStringChars(writer,
                               "(jstring) _tmpObj",
                               convName+"_copy[_copyIndex]",
                               isUTF8Type(cArgType),
                               true);
          } else if (javaArgType.isNIOBufferArray()) {
            /* We always assume an integer "byte offset" argument follows any Buffer
               in the method binding. */
            emitGetDirectBufferAddress(writer,
                                       "_tmpObj",
                                       cArgElementType.getCName(),
                                       convName + "_copy[_copyIndex]",
                                       true,
                                       "_offsetHandle[_copyIndex]", true);
          } else if (javaArgType.isArrayOfCompoundTypeWrappers()) {
            // These come down in similar fashion to an array of NIO
            // Buffers only we do not pass down any integer byte
            // offset argument
            emitGetDirectBufferAddress(writer,
                                       "_tmpObj",
                                       cArgElementType.getCName(),
                                       "("+convName + "_copy + _copyIndex)",
                                       false /* !receivingIsPtrPtr -> linear layout -> use memcpy */,
                                       null, true);
          } else {
            if( null == cArgElementType2 ) {
                throw new GlueGenException("XXX: Type "+cArgType.getDebugString()+" not properly handled as ptr-to-ptr: "+binding,
                                           binding.getCSymbol().getASTLocusTag());
            }
            // Question: do we always need to copy the sub-arrays, or just
            // GetPrimitiveArrayCritical on each jobjectarray element and
            // assign it to the appropriate elements at pointer depth 1?
            // Probably depends on const-ness of the argument.
            // Malloc enough space to hold a copy of each sub-array
            writer.print("      ");
            emitMalloc(
                       writer,
                       convName+"_copy[_copyIndex]",
                       cArgElementType2.getCName(), // assumes cArgPtrType is ptr-to-ptr-to-primitive !!
                       cArgType.isBaseTypeConst(),
                       "(*env)->GetArrayLength(env, _tmpObj)",
                       "Could not allocate buffer during copying of data in argument \\\""+javaArgName+"\\\"");
            // FIXME: copy the data (use matched Get/ReleasePrimitiveArrayCritical() calls)
            if (true) {
                throw new GlueGenException("Cannot yet handle type \"" + cArgType.getDebugString() +
                                           "\"; need to add support for copying ptr-to-ptr-to-primitiveType subarrays: "+binding,
                                           binding.getCSymbol().getASTLocusTag());
            }

          }
          writer.println("    }");

          if (javaArgType.isNIOBufferArray()) {
            writer.println
              ("    (*env)->ReleasePrimitiveArrayCritical(env, " +
               byteOffsetArrayArgName(i) +
               ", _offsetHandle, JNI_ABORT);");
          }

          writer.println();
        } // end of data copy

        writer.println("  }");

      } else if (javaArgType.isString()) {
        emitGetStringChars(writer, javaArgName,
                           STRING_CHARS_PREFIX + javaArgName,
                           isUTF8Type(binding.getCArgumentType(i)),
                           false);
      }
    }
  }


  /**
   * Code to clean up any variables that were declared in
   * emitBodyVariableDeclarations(), AFTER calling the actual C function.
   */
  protected void emitBodyVariablePostCallCleanup(final PrintWriter writer) {

    // Release primitive arrays and temporary UTF8 strings if necessary
    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType javaArgType = binding.getJavaArgumentType(i);
      if (javaArgType.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        continue;
      }

      final Type cArgType = binding.getCArgumentType(i);
      final String javaArgName = binding.getArgumentName(i);

      if (javaArgType.isArray() ||
          (javaArgType.isNIOBuffer() && forIndirectBufferAndArrayImplementation) ||
          javaArgType.isArrayOfCompoundTypeWrappers()) {
        final boolean needsDataCopy = javaArgTypeNeedsDataCopy(javaArgType);

        final String convName = pointerConversionArgumentName(javaArgName);

        if (!needsDataCopy) {
          writer.println("  if ( JNI_FALSE == " + isNIOArgName(i) + " && NULL != " + javaArgName + " ) {");

          // Release array
          final String modeFlag = cArgType.isBaseTypeConst() ? "JNI_ABORT" : "0" ;
          writer.print("    (*env)->ReleasePrimitiveArrayCritical(env, " + javaArgName + ", " + convName + ", "+modeFlag+");");
        } else {
          writer.println("  if ( NULL != " + javaArgName + " ) {");

          // clean up the case where the array elements are of a type that needed
          // a data copy operation to convert from the java memory model to the
          // C memory model (e.g., int[][], String[], etc)
          //
          // FIXME: should factor out this whole block of code into a separate
          // method for clarity and maintenance purposes
          if (!cArgType.isBaseTypeConst()) {
            // FIXME: handle any cleanup from treatment of non-const args,
            // assuming they were treated differently in
            // emitBodyVariablePreCallSetup() (see the similar section in that
            // method for details).
            if (javaArgType.isArrayOfCompoundTypeWrappers()) {
              // This is the only form of cleanup we handle right now
              writer.println("    _tmpArrayLen = (*env)->GetArrayLength(env, " + javaArgName + ");");
              writer.println("    for (_copyIndex = 0; _copyIndex < _tmpArrayLen; ++_copyIndex) {");
              writer.println("      _tmpObj = (*env)->GetObjectArrayElement(env, " + javaArgName + ", _copyIndex);");
              emitReturnDirectBufferAddress(writer,
                                          "_tmpObj",
                                          cArgType.asArray().getBaseElementType().getCName(),
                                          "("+convName + "_copy + _copyIndex)",
                                          false /* receivingIsPtrPtr */,
                                          null);
              writer.println("    }");
            } else {
              throw new GlueGenException(
                "Cannot clean up copied data for ptr-to-ptr arg type \"" + cArgType.getDebugString() +
                "\": support for cleaning up most non-const ptr-to-ptr types not implemented.",
                binding.getCSymbol().getASTLocusTag());
            }
          }

          writer.println("    /* Clean up " + convName + "_copy */");

          // Only need to perform cleanup for individual array
          // elements if they are not direct buffers
          if (!javaArgType.isNIOBufferArray() &&
              !javaArgType.isArrayOfCompoundTypeWrappers()) {
            // Re-fetch length of array that was copied
            final String arrayLenName = "_tmpArrayLen";
            writer.print("    ");
            writer.print(arrayLenName);
            writer.print(" = (*env)->GetArrayLength(env, ");
            writer.print(javaArgName);
            writer.println(");");

            // free each element
            final PointerType cArgPtrType = cArgType.asPointer();
            if (cArgPtrType == null) {
              throw new GlueGenException(
                "Could not copy data for type \"" + cArgType.getDebugString() +
                "\"; currently only pointer types supported.",
                binding.getCSymbol().getASTLocusTag());
            }

            // process each element in the array
            writer.println("    for (_copyIndex = 0; _copyIndex < " + arrayLenName +"; ++_copyIndex) {");

            // get each array element
            writer.println("      /* free each element of " +convName +"_copy */");
            writer.print("      _tmpObj = (*env)->GetObjectArrayElement(env, ");
            writer.print(javaArgName);
            writer.println(", _copyIndex);");

            if (javaArgType.isStringArray()) {
              writer.print("     (*env)->ReleaseStringUTFChars(env, ");
              writer.print("(jstring) _tmpObj");
              writer.print(", ");
              writer.print(convName+"_copy[_copyIndex]");
              writer.println(");");
            } else {
              throw new GlueGenException(
                "Cannot yet handle type \"" + cArgType.getDebugString() +
                "\"; need to add support for cleaning up copied ptr-to-ptr-to-primitiveType subarrays",
                binding.getCSymbol().getASTLocusTag());
            }
            writer.println("    }");
          }

          // free the main array
          writer.print("    free((void*) ");
          writer.print(convName+"_copy");
          writer.println(");");
        } // end of cleaning up copied data

        writer.println("  }");

      } else if (javaArgType.isString()) {
        writer.println("  if ( NULL != " + javaArgName + " ) {");

        if (isUTF8Type(cArgType)) {
          writer.print("    (*env)->ReleaseStringUTFChars(env, ");
          writer.print(javaArgName);
          writer.print(", " + STRING_CHARS_PREFIX);
          writer.print(javaArgName);
          writer.println(");");
        } else {
          writer.println("    free((void*) " + STRING_CHARS_PREFIX + javaArgName + ");");
        }

        writer.println("  }");
      }
    }
  }

  /** Returns the number of arguments passed so calling code knows
      whether to print a comma */
  protected int emitBodyPassCArguments(final PrintWriter writer) {
    for (int i = 0; i < binding.getNumArguments(); i++) {
      if (i != 0) {
        writer.print(", ");
      }
      final JavaType javaArgType = binding.getJavaArgumentType(i);
      // Handle case where only param is void.
      if (javaArgType.isVoid()) {
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        assert(binding.getNumArguments() == 1);
        continue;
      }

      if (javaArgType.isJNIEnv()) {
        writer.print("env");
      } else if (binding.isArgumentThisPointer(i)) {
        writer.print(CMethodBindingEmitter.cThisArgumentName());
      } else {
        writer.print("(");
        final Type cArgType = binding.getCArgumentType(i);
        final boolean needsDataCopy = javaArgTypeNeedsDataCopy(javaArgType);
        final boolean needsArrayOffset = !needsDataCopy && (
                                     javaArgType.isArray() ||
                                     javaArgType.isArrayOfCompoundTypeWrappers() ||
                                     ( javaArgType.isNIOBuffer() && forIndirectBufferAndArrayImplementation ) );
        writer.print(cArgType.getCName(true));
        writer.print(") ");

        if (cArgType.isPointer() && javaArgType.isPrimitive()) {
          writer.print("(intptr_t) ");
        }
        if (javaArgType.isArray() || javaArgType.isNIOBuffer() ||
            javaArgType.isCompoundTypeWrapper() || javaArgType.isArrayOfCompoundTypeWrappers()) {
          if( needsArrayOffset ) {
              writer.print("(((char *) ");
          } else if( !cArgType.isPointer() && javaArgType.isCompoundTypeWrapper() ) { // FIXME: Compound call-by-value
              writer.print("*");
          }
          writer.print(pointerConversionArgumentName(binding.getArgumentName(i)));
          if ( needsDataCopy ) {
            writer.print("_copy");
          }
          if( needsArrayOffset ) {
              writer.print(") + " + byteOffsetArgName(i) + ")");
          }
        } else {
          if (javaArgType.isString()) { writer.print(STRING_CHARS_PREFIX); }
          writer.print(binding.getArgumentName(i));
        }
      }
    }
    return binding.getNumArguments();
  }

  private boolean isCStructFunctionPointer = false;

  /**
   * If method originates from a struct, see {@link MethodBinding#hasContainingType()},
   * it can either purposed to call a native static function (default)
   * or a struct's function pointer.
   */
  protected void setIsCStructFunctionPointer(final boolean v) {
      isCStructFunctionPointer = v;
  }

  protected void emitBodyCallCFunction(final PrintWriter writer) {
    // Make the call to the actual C function
    writer.print("  ");

    // WARNING: this code assumes that the return type has already been
    // typedef-resolved.
    final Type cReturnType = binding.getCReturnType();

    if (!cReturnType.isVoid()) {
      // Note we respect const/volatile in the function return type.
      // However, we cannot have it 'const' for our local variable.
      // See return type in emitBodyVariableDeclarations(..)!
      writer.print("_res = (");
      writer.print(cReturnType.getCName(false));
      writer.print(") ");
    }
    if ( isCStructFunctionPointer && binding.hasContainingType() ) {
      // Call through function pointer
      writer.print(CMethodBindingEmitter.cThisArgumentName() + "->");
    }
    writer.print(getNativeName());
    writer.print("(");
    emitBodyPassCArguments(writer);
    writer.println(");");
  }

  /** Emits the user-defined C variable assignments from the
      TemporaryCVariableAssignments directive in the .cfg file. */
  protected void emitBodyUserVariableAssignments(final PrintWriter writer) {
    if (temporaryCVariableAssignments != null) {
      for (final String val : temporaryCVariableAssignments) {
        writer.print("  ");
        writer.println(val);
      }
    }
  }

  protected void emitBodyReturnResult(final PrintWriter writer)
  {
    // WARNING: this code assumes that the return type has already been
    // typedef-resolved.
    final Type cReturnType = binding.getCReturnType();

    // Return result if necessary
    if (!cReturnType.isVoid()) {
      final JavaType javaReturnType = binding.getJavaReturnType();
      if (javaReturnType.isPrimitive()) {
        writer.print("  return ");
        if (cReturnType.isPointer()) {
          // Pointer being converted to int or long: cast this result
          // (through intptr_t to avoid compiler warnings with gcc)
          writer.print("(" + javaReturnType.jniTypeName() + ") (intptr_t) ");
        }
        writer.println("_res;");
      } else if ( !cReturnType.isPointer() && javaReturnType.isCompoundTypeWrapper() ) { // FIXME: Compound call-by-value
        final String returnSizeOf;
        if (returnValueCapacityExpression != null) {
            returnSizeOf = returnValueCapacityExpression.format(argumentNameArray());
        } else {
            returnSizeOf = "sizeof(" + cReturnType.getCName() + ")";
        }
        writer.println("  return JVMUtil_NewDirectByteBufferCopy(env, &_res, "+returnSizeOf+");");
      } else if (javaReturnType.isNIOBuffer() || javaReturnType.isCompoundTypeWrapper()) {
        writer.println("  if (NULL == _res) return NULL;");
        writer.print("  return (*env)->NewDirectByteBuffer(env, (void *)_res, ");

        // See whether capacity has been specified
        if (returnValueCapacityExpression != null) {
          writer.println( returnValueCapacityExpression.format( argumentNameArray() ) + ");");
        } else {
          final Type cReturnTargetType = cReturnType.isPointer() ? cReturnType.getTargetType() : null;
          int mode = 0;
          if ( 1 == cReturnType.pointerDepth() && null != cReturnTargetType ) {
            if( cReturnTargetType.isCompound() ) {
                if( !cReturnTargetType.isAnon() &&
                    cReturnTargetType.asCompound().getNumFields() > 0 )
                {
                    // fully declared non-anonymous struct pointer: pass content
                    if ( cReturnTargetType.getSize() == null ) {
                      throw new GlueGenException(
                        "Error emitting code for compound return type "+
                        "for function \"" + binding + "\": " +
                        "Structs to be emitted should have been laid out by this point " +
                        "(type " + cReturnTargetType.getCName() + " / " +
                        cReturnTargetType.getDebugString() + " was not) for "+binding.getCSymbol(),
                        binding.getCSymbol().getASTLocusTag()
                      );
                    }
                    writer.println("sizeof(" + cReturnTargetType.getCName() + ") );");
                    mode = 10;
                } else if( cReturnTargetType.asCompound().getNumFields() == 0 ) {
                    // anonymous struct pointer: pass pointer
                    writer.println("sizeof(" + cReturnType.getCName() + ") );");
                    mode = 11;
                }
            }
            if( 0 == mode ) {
                if( cReturnTargetType.isPrimitive() ) {
                    // primitive pointer: pass primitive
                    writer.println("sizeof(" + cReturnTargetType.getCName() + ") );");
                    mode = 20;
                } else if( cReturnTargetType.isVoid() ) {
                    // void pointer: pass pointer
                    writer.println("sizeof(" + cReturnType.getCName() + ") );");
                    mode = 21;
                }
            }
          }
          if( 0 == mode ) {
            if( null != cfg.typeInfo(cReturnType) ) { // javaReturnType.isOpaqued() covered above via isPrimitive()
                // Opaque
                writer.println("sizeof(" + cReturnType.getCName()  + ") );");
                mode = 88;
            } else {
                final String wmsg = "Assumed return size of equivalent C return type";
                writer.println("sizeof(" + cReturnType.getCName()  + ") ); // WARNING: "+wmsg);
                mode = 99;
                LOG.warning(binding.getCSymbol().getASTLocusTag(),
                  "No capacity specified for java.nio.Buffer return " +
                  "value for function \"" + binding.getName() + "\". " + wmsg + " (sizeof(" + cReturnType.getCName() + ")): " + binding);
            }
          }
          writer.println("  /** ");
          writer.println("   * mode: "+mode);
          writer.println("   * cReturnType: "+cReturnType.getDebugString());
          writer.println("   * cReturnTargetType: "+cReturnTargetType.getDebugString());
          writer.println("   * javaReturnType: "+javaReturnType.getDebugString());
          writer.println("   */");
        }
      } else if (javaReturnType.isString()) {
        writer.println("  if (NULL == _res) return NULL;");
        writer.println("  return (*env)->NewStringUTF(env, (const char *)_res);");
      } else if (javaReturnType.isArrayOfCompoundTypeWrappers() ||
                 (javaReturnType.isArray() && javaReturnType.isNIOByteBufferArray())) {
        writer.println("  if (NULL == _res) return NULL;");
        if (returnValueLengthExpression == null) {
          throw new GlueGenException("Error while generating C code: no length specified for array returned from function " +
                                      binding, binding.getCSymbol().getASTLocusTag());
        }
        writer.println("  " + arrayResLength + " = " + returnValueLengthExpression.format(argumentNameArray()) + ";");
        writer.println("  " + arrayRes + " = (*env)->NewObjectArray(env, " + arrayResLength + ", (*env)->FindClass(env, \"java/nio/ByteBuffer\"), NULL);");
        writer.println("  for (" + arrayIdx + " = 0; " + arrayIdx + " < " + arrayResLength + "; " + arrayIdx + "++) {");
        final Type retType = binding.getCSymbol().getReturnType();
        Type pointerType;
        if (retType.isPointer()) {
          pointerType = retType.asPointer().getTargetType();
        } else {
          pointerType = retType.asArray().getBaseElementType();
        }
        writer.println("    (*env)->SetObjectArrayElement(env, " + arrayRes + ", " + arrayIdx +
                       ", (*env)->NewDirectByteBuffer(env, (void *)_res[" + arrayIdx + "], sizeof(" + pointerType.getCName() + ")));");
        writer.println("  }");
        writer.println("  return " + arrayRes + ";");
      } else if (javaReturnType.isArray()) {
        // FIXME: must have user provide length of array in .cfg file
        // by providing a constant value, input parameter, or
        // expression which computes the array size (already present
        // as ReturnValueCapacity, not yet implemented / tested here)

        throw new GlueGenException(
                                   "Could not emit native code for function \"" + binding +
                                   "\": array return values for non-char types not implemented yet, for "+binding,
                                   binding.getCSymbol().getASTLocusTag());

        // FIXME: This is approximately what will be required here
        //
        //writer.print("  ");
        //writer.print(arrayRes);
        //writer.print(" = (*env)->New");
        //writer.print(capitalizedComponentType);
        //writer.print("Array(env, ");
        //writer.print(arrayResLength);
        //writer.println(");");
        //writer.print("  (*env)->Set");
        //writer.print(capitalizedComponentType);
        //writer.print("ArrayRegion(env, ");
        //writer.print(arrayRes);
        //writer.print(", 0, ");
        //writer.print(arrayResLength);
        //writer.println(", _res);");
        //writer.print("  return ");
        //writer.print(arrayRes);
        //writer.println(";");
      } else {
        throw new GlueGenException("Unhandled return type: "+javaReturnType.getDebugString()+" for "+binding,
                                   binding.getCSymbol().getReturnType().getASTLocusTag());
      }
    }
  }

  protected static String cThisArgumentName() {
    return "this0";
  }

  protected String jniMangle(final MethodBinding binding) {
    final StringBuilder buf = new StringBuilder();
    buf.append(JavaEmitter.jniMangle(getImplName()));
    buf.append(getImplSuffix());
    buf.append("__");
    if (binding.hasContainingType()) {
      // "this" argument always comes down in argument 0 as direct buffer
      jniMangle(java.nio.ByteBuffer.class, buf, true);
    }
    for (int i = 0; i < binding.getNumArguments(); i++) {
      if (binding.isArgumentThisPointer(i)) {
        continue;
      }
      final JavaType type = binding.getJavaArgumentType(i);
      if (type.isVoid()) {
        // We should only see "void" as the first argument of a 1-argument function
        // FIXME: should normalize this in the parser
        if ((i != 0) || (binding.getNumArguments() > 1)) {
          throw new GlueGenException("Saw illegal \"void\" argument while emitting arg "+i+" of "+binding,
                                     binding.getCArgumentType(i).getASTLocusTag());
        }
      } else {
        Class<?> c = type.getJavaClass();
        if (c != null) {
          jniMangle(c, buf, false);
          // If Buffer offset arguments were added, we need to mangle the JNI for the
          // extra arguments
          if (type.isNIOBuffer()) {
            jniMangle(Integer.TYPE, buf, false);
            if(forIndirectBufferAndArrayImplementation) {
                jniMangle(Boolean.TYPE, buf, false);
            }
          } else if (type.isNIOBufferArray())   {
            final int[] intArrayType = new int[0];
            c = intArrayType.getClass();
            jniMangle(c , buf, true);
          }
          if (type.isPrimitiveArray()) {
            jniMangle(Integer.TYPE, buf, false);
          }
        } else if (type.isCompoundTypeWrapper()) {
          // Mangle wrappers for C structs as ByteBuffer
          jniMangle(java.nio.ByteBuffer.class, buf, true);
        } else if (type.isArrayOfCompoundTypeWrappers()) {
          // Mangle arrays of C structs as ByteBuffer[]
          final java.nio.ByteBuffer[] tmp = new java.nio.ByteBuffer[0];
          jniMangle(tmp.getClass(), buf, true);
        } else if (type.isJNIEnv()) {
          // These are not exposed at the Java level
        } else {
          // FIXME: add support for char* -> String conversion
          throw new GlueGenException("Unknown kind of JavaType: arg "+i+", name="+type.getName()+" of "+binding,
                                     binding.getCArgumentType(i).getASTLocusTag());
        }
      }
    }

    return buf.toString();
  }

  protected void jniMangle(final Class<?> c, final StringBuilder res, final boolean syntheticArgument) {
    if (c.isPrimitive()) {
           if (c == Boolean.TYPE)   res.append("Z");
      else if (c == Byte.TYPE)      res.append("B");
      else if (c == Character.TYPE) res.append("C");
      else if (c == Short.TYPE)     res.append("S");
      else if (c == Integer.TYPE)   res.append("I");
      else if (c == Long.TYPE)      res.append("J");
      else if (c == Float.TYPE)     res.append("F");
      else if (c == Double.TYPE)    res.append("D");
      else throw new RuntimeException("Illegal primitive type \"" + c.getName() + "\"");
    } else {
      // Arrays and NIO Buffers are always passed down as java.lang.Object.
      // The only arrays that show up as true arrays in the signature
      // are the synthetic byte offset arrays created when passing
      // down arrays of direct Buffers. Compound type wrappers are
      // passed down as ByteBuffers (no good reason, just to avoid
      // accidental conflation) so we mangle them differently.
      if (syntheticArgument) {
        if (c.isArray()) {
          res.append("_3");
          final Class<?> componentType = c.getComponentType();
          // Handle arrays of compound type wrappers differently for
          // convenience of the Java-level glue code generation
          jniMangle(componentType, res,
                    (componentType == java.nio.ByteBuffer.class));
        } else {
          res.append("L");
          res.append(c.getName().replace('.', '_'));
          res.append("_2");
        }
      } else {
        if (c.isArray()) {
          res.append("_3");
          jniMangle(c.getComponentType(), res, false);
        } else if (c == java.lang.String.class) {
          res.append("L");
          res.append(c.getName().replace('.', '_'));
          res.append("_2");
        } else {
          res.append("L");
          res.append("java_lang_Object");
          res.append("_2");
        }
      }
    }
  }

  private void emitOutOfMemoryCheck(final PrintWriter writer, final String varName, final String errorMessage)  {
    writer.println("  if ( NULL == " + varName + " ) {");
    writer.println("      (*env)->ThrowNew(env, (*env)->FindClass(env, \"java/lang/OutOfMemoryError\"),");
    writer.print("                       \"" + errorMessage);
    writer.print(" in native dispatcher for \\\"");
    writer.print(getInterfaceName());
    writer.println("\\\"\");");
    writer.print("      return");
    if (!binding.getJavaReturnType().isVoid()) {
      writer.print(" 0");
    }
    writer.println(";");
    writer.println("    }");
  }

  private void emitMalloc(final PrintWriter writer,
                          final String targetVarName,
                          final String elementTypeString,
                          final boolean elementTypeIsConst,
                          final String numElementsExpression,
                          final String mallocFailureErrorString)  {
    writer.print("    ");
    writer.print(targetVarName);
    writer.print(" = (");
    if(elementTypeIsConst) {
        writer.print("const ");
    }
    writer.print(elementTypeString);
    writer.print(" *) malloc(");
    writer.print(numElementsExpression);
    writer.print(" * sizeof(");
    writer.print(elementTypeString);
    writer.println("));");
    // Catch memory allocation failure
    emitOutOfMemoryCheck( writer, targetVarName, mallocFailureErrorString);
  }

  private void emitCalloc(final PrintWriter writer,
                          final String targetVarName,
                          final String elementTypeString,
                          final String numElementsExpression,
                          final String mallocFailureErrorString)  {
    writer.print("    ");
    writer.print(targetVarName);
    writer.print(" = (");
    writer.print(elementTypeString);
    writer.print(" *) calloc(");
    writer.print(numElementsExpression);
    writer.print(", sizeof(");
    writer.print(elementTypeString);
    writer.println("));");
    // Catch memory allocation failure
    emitOutOfMemoryCheck( writer, targetVarName, mallocFailureErrorString);
  }

  private void emitGetStringChars(final PrintWriter writer,
                                  final String sourceVarName,
                                  final String receivingVarName,
                                  final boolean isUTF8,
                                  final boolean emitElseClause)  {
    writer.println("  if ( NULL != " + sourceVarName + " ) {");

    if (isUTF8) {
      writer.print("    ");
      writer.print(receivingVarName);
      writer.print(" = (*env)->GetStringUTFChars(env, ");
      writer.print(sourceVarName);
      writer.println(", (jboolean*)NULL);");
      // Catch memory allocation failure in the event that the VM didn't pin
      // the String and failed to allocate a copy
      emitOutOfMemoryCheck( writer, receivingVarName, "Failed to get UTF-8 chars for argument \\\""+sourceVarName+"\\\"");
    } else {
      // The UTF-16 case is basically Windows specific. Unix platforms
      // tend to use only the UTF-8 encoding. On Windows the problem
      // is that wide character strings are expected to be null
      // terminated, but the JNI GetStringChars doesn't return a
      // null-terminated Unicode string. For this reason we explicitly
      // calloc our buffer, including the null terminator, and use
      // GetStringRegion to fetch the string's characters.
      emitCalloc(writer,
                 receivingVarName,
                 "jchar",
                 "(*env)->GetStringLength(env, " + sourceVarName + ") + 1",
                 "Could not allocate temporary buffer for copying string argument \\\""+sourceVarName+"\\\"");
      writer.println("    (*env)->GetStringRegion(env, " + sourceVarName + ", 0, (*env)->GetStringLength(env, " + sourceVarName + "), " + receivingVarName + ");");
    }
    writer.print("  }");
    if (emitElseClause) {
      writer.print(" else {");
      writer.print("      ");
      writer.print(receivingVarName);
      writer.println(" = NULL;");
      writer.println("  }");
    } else {
      writer.println();
    }
  }

  private void emitGetDirectBufferAddress(final PrintWriter writer,
                                          final String sourceVarName,
                                          final String receivingVarTypeString,
                                          final String receivingVarName,
                                          final boolean receivingIsPtrPtr,
                                          final String byteOffsetVarName, final boolean emitElseClause) {
    writer.println("    if ( NULL != " + sourceVarName + " ) {");
    writer.print("    ");
    writer.print("    ");
    if( receivingIsPtrPtr ) {
        writer.print(receivingVarName+" = ("+receivingVarTypeString+") (((char*) (*env)->GetDirectBufferAddress(env, "+sourceVarName+"))");
        writer.println(" + " + ((byteOffsetVarName != null) ? byteOffsetVarName : "0") + ");");
    } else {
        // linear layout -> use memcpy
        writer.println("memcpy((void *)"+receivingVarName+", (*env)->GetDirectBufferAddress(env, "+sourceVarName+"), sizeof("+receivingVarTypeString+"));");
    }

    if (emitElseClause) {
      writer.println("    } else {");
      writer.print("    ");
      writer.print("    ");
      if( receivingIsPtrPtr ) {
          writer.print(receivingVarName);
          writer.println(" = NULL;");
      } else {
          writer.println("memset((void *)"+receivingVarName+", 0, sizeof("+receivingVarTypeString+"));");
      }
    }
    writer.println("    }");
    writer.println();
  }

  private void emitReturnDirectBufferAddress(final PrintWriter writer,
                                          final String sourceVarName,
                                          final String receivingVarTypeString,
                                          final String receivingVarName,
                                          final boolean receivingIsPtrPtr,
                                          final String byteOffsetVarName) {
    writer.print("    ");
    writer.print("    ");
    if( receivingIsPtrPtr ) {
        writer.print("(((char*) (*env)->GetDirectBufferAddress(env, "+sourceVarName+"))");
        writer.println(" + " + ((byteOffsetVarName != null) ? byteOffsetVarName : "0") + ") = "+receivingVarName+";");
        throw new RuntimeException("incomplete implementation"); // FIXME doesn't work, currently unused
    } else {
        // linear layout -> use memcpy
        writer.println("memcpy((*env)->GetDirectBufferAddress(env, "+sourceVarName+"), "+receivingVarName+", sizeof("+receivingVarTypeString+"));");
    }
    writer.println();
  }

  // Note: if the data in the Type needs to be converted from the Java memory
  // model to the C memory model prior to calling any C-side functions, then
  // an extra variable named XXX_copy (where XXX is the value of the
  // cVariableName argument) will be emitted and TRUE will be returned.
  private boolean emitPointerDeclaration(final PrintWriter writer,
                                         final JavaType javaType,
                                         final Type cType,
                                         final String cVariableName,
                                         final String javaArgumentName) {
    String ptrTypeString = null;
    boolean needsDataCopy = false;

    // Emit declaration for the pointer variable.
    //
    // Note that we don't need to obey const/volatile for outgoing arguments
    //
    if (javaType.isNIOBuffer()) {
      // primitive NIO object
      ptrTypeString = cType.getCName();
    } else if (javaType.isArray() || javaType.isArrayOfCompoundTypeWrappers()) {
      needsDataCopy = javaArgTypeNeedsDataCopy(javaType);
      if (javaType.isPrimitiveArray() ||
          javaType.isNIOBufferArray() ||
          javaType.isArrayOfCompoundTypeWrappers()) {
        ptrTypeString = cType.getCName();
      } else if (!javaType.isStringArray()) {
        final Class<?> elementType = javaType.getJavaClass().getComponentType();
        if (elementType.isArray()) {
          final Class<?> subElementType = elementType.getComponentType();
          if (subElementType.isPrimitive()) {
            // type is pointer to pointer to primitive
            ptrTypeString = cType.getCName();
          } else {
            // type is pointer to pointer of some type we don't support (maybe
            // it's an array of pointers to structs?)
            throw new GlueGenException("Unsupported pointer type: \"" + cType.getDebugString() + "\"", cType.getASTLocusTag());
          }
        } else {
          // type is pointer to pointer of some type we don't support (maybe
          // it's an array of pointers to structs?)
          throw new GlueGenException("Unsupported pointer type: \"" + cType.getDebugString() + "\"", cType.getASTLocusTag());
        }
      }
    } else {
      ptrTypeString = cType.getCName();
    }

    writer.print("  ");
    if (!needsDataCopy) {
      // declare the pointer variable
      writer.print(ptrTypeString);
      if( !cType.isPointer() && javaType.isCompoundTypeWrapper() ) { // FIXME: Compound call-by-value
          writer.print(" * ");
      } else {
          writer.print(" ");
      }
      writer.print(cVariableName);
      writer.println(" = NULL;");
    } else {
      // Declare a variable to hold a copy of the argument data in which the
      // incoming data has been properly laid out in memory to match the C
      // memory model
      if (javaType.isStringArray()) {
        String cElementTypeName = "char *";
        final PointerType cPtrType = cType.asPointer();
        if (cPtrType != null) {
            cElementTypeName = cPtrType.getTargetType().asPointer().getCName();
        }
        if (cType.isBaseTypeConst()) {
            writer.print("const ");
        }
        writer.print(cElementTypeName+" *");
      } else {
        if (cType.isBaseTypeConst()) {
            writer.print("const ");
        }
        writer.print(ptrTypeString);
      }
      writer.print(" ");
      writer.print(cVariableName);
      writer.print("_copy = NULL; /* copy of data in ");
      writer.print(javaArgumentName);
      writer.println(", laid out according to C memory model */");
    }

    return needsDataCopy;
  }

  private void emitPointerConversion(final PrintWriter writer,
                                     final MethodBinding binding,
                                     final JavaType type,
                                     final Type cType,
                                     final String incomingArgumentName,
                                     final String cVariableName,
                                     String byteOffsetVarName) {
    // Compound type wrappers do not get byte offsets added on
    if (type.isCompoundTypeWrapper()) {
      byteOffsetVarName = null;
    }

    final String cVariableType;
    if( !cType.isPointer() && type.isCompoundTypeWrapper() ) { // FIXME: Compound call-by-value
        cVariableType = cType.getCName()+" *";
    } else {
        cVariableType = cType.getCName();
    }
    emitGetDirectBufferAddress(writer,
                               incomingArgumentName,
                               cVariableType,
                               cVariableName,
                               true,
                               byteOffsetVarName, false);
  }

  protected String byteOffsetArgName(final int i) {
    return JavaMethodBindingEmitter.byteOffsetArgName(binding.getArgumentName(i));
  }

  protected String isNIOArgName(final int i) {
    return isNIOArgName(binding.getArgumentName(i));
  }

  protected String isNIOArgName(final String s) {
    return s + "_is_nio";
  }

  protected String byteOffsetArrayArgName(final int i) {
    return binding.getArgumentName(i) + "_byte_offset_array";
  }

  protected String[] argumentNameArray() {
    final String[] argumentNames = new String[binding.getNumArguments()];
    for (int i = 0; i < binding.getNumArguments(); i++) {
      argumentNames[i] = binding.getArgumentName(i);
      if (binding.getJavaArgumentType(i).isPrimitiveArray()) {
        // Add on _offset argument in comma-separated expression
        argumentNames[i] = argumentNames[i] + ", " + byteOffsetArgName(i);
      }
    }
    return argumentNames;
  }

  protected String pointerConversionArgumentName(final String argName) {
    return "_" + argName + "_ptr";
  }

  /**
   * Class that emits a generic comment for CMethodBindingEmitters; the comment
   * includes the C signature of the native method that is being bound by the
   * emitter java method.
   */
  protected static class DefaultCommentEmitter implements CommentEmitter {
    @Override
    public void emit(final FunctionEmitter emitter, final PrintWriter writer) {
      emitBeginning((CMethodBindingEmitter)emitter, writer);
      emitEnding((CMethodBindingEmitter)emitter, writer);
    }
    protected void emitBeginning(final CMethodBindingEmitter emitter, final PrintWriter writer) {
      writer.println("  Java->C glue code:");
      writer.print(" *   Java package: ");
      writer.print(emitter.getJavaPackageName());
      writer.print(".");
      writer.println(emitter.getJavaClassName());
      writer.print(" *    Java method: ");
      final MethodBinding binding = emitter.getBinding();
      writer.println(binding);
      writer.println(" *     C function: " + binding.getCSymbol());
    }
    protected void emitEnding(final CMethodBindingEmitter emitter, final PrintWriter writer) {
    }
  }

  protected boolean javaArgTypeNeedsDataCopy(final JavaType javaArgType) {
    if (javaArgType.isArray()) {
      return (javaArgType.isNIOBufferArray() ||
              javaArgType.isStringArray() ||
              javaArgType.getJavaClass().getComponentType().isArray());
    }
    if (javaArgType.isArrayOfCompoundTypeWrappers()) {
      return true;
    }
    return false;
  }
}
