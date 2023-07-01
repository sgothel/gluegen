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

import static java.util.logging.Level.INFO;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.List;

import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.gluegen.JavaConfiguration.JavaCallbackInfo;
import com.jogamp.gluegen.Logging.LoggerIf;
import com.jogamp.gluegen.cgram.types.ArrayType;
import com.jogamp.gluegen.cgram.types.FunctionSymbol;
import com.jogamp.gluegen.cgram.types.PointerType;
import com.jogamp.gluegen.cgram.types.Type;

/** Emits the C-side component of the Java<->C JNI binding to its {@link CodeUnit}, see {@link FunctionEmitter}. */
public class CMethodBindingEmitter extends FunctionEmitter {

  protected static final CommentEmitter defaultCommentEmitter = new DefaultCommentEmitter();

  protected static final String arrayResLength = "_array_res_length";
  protected static final String arrayRes       = "_array_res";
  protected static final String arrayIdx       = "_array_idx";

  protected final LoggerIf LOG;

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

  private final JavaCallbackInfo javaCallback;
  private final String jcbNativeBasename;
  private final CMethodBindingEmitter jcbCMethodEmitter;

  /**
   * Constructs an emitter for the specified binding, and sets a default
   * comment emitter that will emit the signature of the C function that is
   * being bound.
   */
  public CMethodBindingEmitter(final MethodBinding binding,
                               final CodeUnit unit,
                               final String javaPackageName,
                               final String javaClassName,
                               final boolean isOverloadedBinding,
                               final boolean isJavaMethodStatic,
                               final boolean forImplementingMethodCall,
                               final boolean forIndirectBufferAndArrayImplementation,
                               final MachineDataInfo machDesc,
                               final JavaConfiguration configuration)
  {
    super(binding, unit, false, configuration);
    LOG = Logging.getLogger(CMethodBindingEmitter.class.getPackage().getName(), CMethodBindingEmitter.class.getSimpleName());

    assert(binding != null);
    assert(javaClassName != null);
    assert(javaPackageName != null);

    this.packageName = javaPackageName;
    this.className = javaClassName;
    this.isOverloadedBinding = isOverloadedBinding;
    this.isJavaMethodStatic = isJavaMethodStatic;

    this.forImplementingMethodCall = forImplementingMethodCall;
    this.forIndirectBufferAndArrayImplementation = forIndirectBufferAndArrayImplementation;
    this.machDesc = machDesc;

    javaCallback = cfg.setFuncToJavaCallbackMap.get(binding.getName());
    if( null != javaCallback ) {
        jcbNativeBasename = CodeGenUtils.capitalizeString( javaCallback.setFuncName+javaCallback.cbSimpleClazzName.replace("_", "") );
        jcbCMethodEmitter = new CMethodBindingEmitter(javaCallback.cbFuncBinding,
                                                      unit, javaPackageName, javaClassName, isOverloadedBinding,
                                                      isJavaMethodStatic, forImplementingMethodCall,
                                                      forIndirectBufferAndArrayImplementation, machDesc, configuration);
    } else {
        jcbNativeBasename = null;
        jcbCMethodEmitter = null;
    }

    setCommentEmitter(defaultCommentEmitter);
  }

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

  private static final boolean DEBUG_JAVACALLBACK = false;

  @Override
  protected void emitReturnType()  {
    if( null != javaCallback ) {
        LOG.log(INFO, "BindCFunc.R.JavaCallback: {0}: {1}", binding.getName(), javaCallback);
        final String staticCallbackName = "func"+jcbNativeBasename;
        final Type userParamType = javaCallback.cbFuncBinding.getCArgumentType(javaCallback.cbFuncUserParamIdx);
        final String userParamArgName = javaCallback.cbFuncBinding.getArgumentName(javaCallback.cbFuncUserParamIdx);
        final Type cReturnType = javaCallback.cbFuncBinding.getCReturnType();
        final JavaType jretType = javaCallback.cbFuncBinding.getJavaReturnType();
        unit.emitln("typedef struct {");
        unit.emitln("  jobject lockObj;");
        unit.emitln("  jobject cbFunc;");
        unit.emitln("  jmethodID cbMethodID;");
        unit.emitln("  jobject userParam;");
        unit.emitln("} T_"+jcbNativeBasename+";");
        unit.emitln();
        // javaCallback.cbFuncCEmitter.emitSignature();
        unit.emit("static "+cReturnType.getCName()+" "+staticCallbackName+"(");
        // javaCallback.cbFuncCEmitter.emitArguments();
        unit.emit(javaCallback.cbFuncBinding.getCParameterList(new StringBuilder(), false, null).toString());
        unit.emitln(") {");
        // javaCallback.cbFuncCEmitter.emitBody();
        {
            unit.emitln("  JNIEnv* env = JVMUtil_GetJNIEnv();");
            unit.emitln("  if( NULL == env ) {");
            if( !cReturnType.isVoid() ) {
                unit.emitln("    return 0;");
            } else {
                unit.emitln("    return;");
            }
            unit.emitln("  }");
            // javaCallback.cbFuncCEmitter.emitBodyVariableDeclarations();
            // javaCallback.cbFuncCEmitter.emitBodyUserVariableDeclarations();
            // javaCallback.cbFuncCEmitter.emitBodyVariablePreCallSetup();
            jcbCMethodEmitter.emitJavaCallbackBodyCToJavaPreCall(javaCallback);

            // javaCallback.cbFuncCEmitter.emitBodyCallCFunction();
            unit.emitln("  T_"+jcbNativeBasename+"* cb = (T_"+jcbNativeBasename+"*) "+userParamArgName+";");
            unit.emitln("  // C Params: "+javaCallback.cbFuncBinding.getCParameterList(new StringBuilder(), false, null).toString());
            unit.emitln("  // J Params: "+javaCallback.cbFuncBinding.getJavaParameterList(new StringBuilder()).toString());

            final String returnStatement;
            if( !cReturnType.isVoid() ) {
                unit.emit("  "+cReturnType.getCName()+" _res = 0;");
                returnStatement = "return _res;";
            } else {
                returnStatement = "return;";
            }
            unit.emitln("  if( NULL == cb ) { fprintf(stderr, \"Info: Callback '"+staticCallbackName+"(..)': NULL "+userParamArgName+", skipping!\\n\"); "+returnStatement+" }");
            unit.emitln("  T_"+jcbNativeBasename+" cb2 = *cb; // use a copy to avoid data-race between GetObjectRefType() and MonitorEnter()");
            unit.emitln();
            unit.emitln("  jobjectRefType refType = (*env)->GetObjectRefType(env, cb2.lockObj);");
            unit.emitln("  if( 0 == refType ) { fprintf(stderr, \"Info: Callback '"+staticCallbackName+"(..)': User after free(lock), skipping!\\n\"); "+returnStatement+" }");
            unit.emitln("  jint lockRes = (*env)->MonitorEnter(env, cb2.lockObj);");
            unit.emitln("  if( 0 != lockRes ) { fprintf(stderr, \"Info: Callback '"+staticCallbackName+"(..)': MonitorEnter failed %d, skipping!\\n\", lockRes); "+returnStatement+" }");
            unit.emitln("  // synchronized block");
            /**
             * Since we have acquired the lock, in-sync w/ our Java code, cb2.cbFunc and cb2.userParam could not have been changed!
             *
            unit.emitln("  refType = (*env)->GetObjectRefType(env, cb2.userParam);");
            unit.emitln("  if( 0 == refType ) {");
            unit.emitln("    fprintf(stderr, \"Info: Callback '"+staticCallbackName+"(..)': User after free(userParam), skipping!\\n\");");
            unit.emitln("    lockRes = (*env)->MonitorExit(env, cb2.lockObj);");
            unit.emitln("    if( 0 != lockRes ) { fprintf(stderr, \"Info: Callback '"+staticCallbackName+"(..)': MonitorExit failed %d\\n\", lockRes); }");
            unit.emitln("    "+returnStatement);
            unit.emitln("  }");
            */
            if( !cReturnType.isVoid() ) {
                unit.emit("  _res = ("+cReturnType.getCName()+") ");
            } else {
                unit.emit("  ");
            }
            unit.emit("(*env)->Call" + CodeGenUtils.capitalizeString( jretType.getName() ) +"Method(env, cb2.cbFunc, cb2.cbMethodID, ");
            // javaCallback.cbFuncCEmitter.emitBodyPassCArguments();
            jcbCMethodEmitter.emitJavaCallbackBodyPassJavaArguments(javaCallback, "cb2.userParam");
            unit.emitln(");");

            // javaCallback.cbFuncCEmitter.emitBodyUserVariableAssignments();
            // javaCallback.cbFuncCEmitter.emitBodyVariablePostCallCleanup();
            // javaCallback.cbFuncCEmitter.emitBodyMapCToJNIType(-1 /* return value */, true /* addLocalVar */)

            unit.emitln("  lockRes = (*env)->MonitorExit(env, cb2.lockObj);");
            unit.emitln("  if( 0 != lockRes ) { fprintf(stderr, \"Info: Callback '"+staticCallbackName+"(..)': MonitorExit failed %d\\n\", lockRes); }");
            unit.emitln("  "+returnStatement);
        }
        unit.emitln("}");
        unit.emitln();
    }
    unit.emit("JNIEXPORT ");
    unit.emit(binding.getJavaReturnType().jniTypeName());
    unit.emit(" JNICALL");
  }
  /* pp */ int emitJavaCallbackBodyCToJavaPreCall(final JavaCallbackInfo jcbi)  {
    int count = 0;
    for (int i = 0; i < binding.getNumArguments(); i++) {
      if( i == jcbi.cbFuncUserParamIdx ) {
        continue;
      }
      if( emitBodyMapCToJNIType(i, true /* addLocalVar */) ) {
          ++count;
      }
    }
    return count;
  }
  /* pp */ int emitJavaCallbackBodyPassJavaArguments(final JavaCallbackInfo jcbi, final String userParamVarName) {
    int count = 0;
    boolean needsComma = false;
    for (int i = 0; i < binding.getNumArguments(); i++) {
      if (needsComma) {
        unit.emit(", ");
        needsComma = false;
      }
      if( i == jcbi.cbFuncUserParamIdx ) {
          unit.emit( userParamVarName );
      } else {
          unit.emit( binding.getArgumentName(i) + "_jni" );
      }
      needsComma = true;
      ++count;
    }
    return count;
  }

  @Override
  protected void emitName()  {
    unit.emitln(); // start name on new line
    unit.emit(JavaEmitter.getJNIMethodNamePrefix(getJavaPackageName(), getJavaClassName()));
    unit.emit("_");
    if (isOverloadedBinding)    {
      unit.emit(jniMangle(binding));
    } else {
      unit.emit(JavaEmitter.jniMangle(getImplName()));
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
  protected int emitArguments()  {
    unit.emit("JNIEnv *env, ");
    int numEmitted = 1; // initially just the JNIEnv
    if (isJavaMethodStatic && !binding.hasContainingType())    {
      unit.emit("jclass");
    } else {
      unit.emit("jobject");
    }
    unit.emit(" _unused");
    ++numEmitted;

    if( binding.isReturnCompoundByValue() ) {
        unit.emit(", jclass _clazzBuffers");
        ++numEmitted;
    }
    if (binding.hasContainingType())   {
      // "this" argument always comes down in argument 0 as direct buffer
      unit.emit(", jobject " + JavaMethodBindingEmitter.javaThisArgumentName());
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
      unit.emit(", ");
      unit.emit(javaArgType.jniTypeName());
      unit.emit(" ");
      unit.emit(binding.getArgumentName(i));
      ++numEmitted;

      if (javaArgType.isPrimitiveArray() ||
          javaArgType.isNIOBuffer()) {
        unit.emit(", jint " + byteOffsetArgName(i));
        if(forIndirectBufferAndArrayImplementation) {
            unit.emit(", jboolean " + isNIOArgName(i));
        }
      } else if (javaArgType.isNIOBufferArray()) {
        unit.emit(", jintArray " +
                     byteOffsetArrayArgName(i));
      }
    }

    final JavaCallbackInfo jcb = this.javaCallback;
    if( null != jcb ) {
        LOG.log(INFO, "BindCFunc.A.JavaCallback: {0}: {1}", binding.getName(), jcb);
        unit.emit(", jstring jcallbackSignature, jobject jlockObj, jlongArray jnativeUserParam");
        numEmitted+=2;
    } else {
        LOG.log(INFO, "BindCFunc.JavaCallback: {0}: NONE", binding.getName());
    }
    return numEmitted;
  }


  @Override
  protected void emitBody()  {
    unit.emitln(" {");
//    unit().emitln("printf(\" - - - - "+ getName() + getImplSuffix() +" - - - -\\n\");");
    emitBodyVariableDeclarations();
    emitBodyUserVariableDeclarations();
    emitBodyVariablePreCallSetup();
    final JavaCallbackInfo jcb = this.javaCallback;
    if( null != jcb ) {
        LOG.log(INFO, "BindCFunc.B.JavaCallback: {0}: {1}", binding.getName(), jcb);
        final String cbFuncArgName = binding.getArgumentName(jcb.setFuncCBParamIdx);
        final String userParamArgName = binding.getArgumentName(jcb.setFuncUserParamIdx);
        final String nativeCBFuncVarName = cbFuncArgName+"_native";
        final String nativeUserParamVarName = userParamArgName+"_native";
        unit.emitln();
        unit.emitln("  // JavaCallback handling");
        unit.emitln("  "+jcb.cbFuncTypeName+" "+nativeCBFuncVarName+";");
        unit.emitln("  T_"+jcbNativeBasename+"* "+nativeUserParamVarName+";");
        // unit.emit(", jstring jcallbackSignature, jobject jlockObj, jlongArray jnativeUserParam");
        unit.emitln("  if( NULL == jlockObj ) { (*env)->FatalError(env, \"Null jlockObj in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("  if( NULL == jnativeUserParam ) { (*env)->FatalError(env, \"Null jnativeUserParam in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("  const size_t jnativeUserParam_size = (*env)->GetArrayLength(env, jnativeUserParam);");
        unit.emitln("  if( 1 > jnativeUserParam_size ) { (*env)->FatalError(env, \"nativeUserParam size < 1 in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("  if( NULL != "+cbFuncArgName+" ) {");
        unit.emitln("    if( NULL == "+userParamArgName+" ) { (*env)->FatalError(env, \"Null "+userParamArgName+" in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("    "+nativeUserParamVarName+" = (T_"+jcbNativeBasename+"*) calloc(1, sizeof(T_"+jcbNativeBasename+"));");
        unit.emitln("    if( NULL == "+nativeUserParamVarName+" ) { (*env)->FatalError(env, \"Can't alloc "+nativeUserParamVarName+" in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("    "+nativeUserParamVarName+"->lockObj = (*env)->NewGlobalRef(env, jlockObj);");
        unit.emitln("    if( NULL == "+nativeUserParamVarName+"->lockObj ) { (*env)->FatalError(env, \"Failed NewGlobalRef(lock) in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("    "+nativeUserParamVarName+"->cbFunc = (*env)->NewGlobalRef(env, "+cbFuncArgName+");");
        unit.emitln("    if( NULL == "+nativeUserParamVarName+"->cbFunc ) { (*env)->FatalError(env, \"Failed NewGlobalRef(func) in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("    "+nativeUserParamVarName+"->userParam = (*env)->NewGlobalRef(env, "+userParamArgName+");");
        unit.emitln("    if( NULL == "+nativeUserParamVarName+"->userParam ) { (*env)->FatalError(env, \"Failed NewGlobalRef(userParam) in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("    {");
        unit.emitln("      jclass cbClazz = (*env)->GetObjectClass(env, "+nativeUserParamVarName+"->cbFunc);");
        unit.emitln("      if( NULL == cbClazz ) { (*env)->FatalError(env, \"Failed GetObjectClass in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("      const char* callbackSignature = (*env)->GetStringUTFChars(env, jcallbackSignature, (jboolean*)NULL);");
        unit.emitln("      if( NULL == callbackSignature ) { (*env)->FatalError(env, \"Failed callbackSignature in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("      "+nativeUserParamVarName+"->cbMethodID = (*env)->GetMethodID(env, cbClazz, \"callback\", callbackSignature);");
        unit.emitln("      (*env)->ReleaseStringUTFChars(env, jcallbackSignature, callbackSignature);");
        unit.emitln("      if( NULL == "+nativeUserParamVarName+"->cbMethodID ) { (*env)->FatalError(env, \"Failed GetMethodID in '"+jcbNativeBasename+"'\"); }");
        unit.emitln("    }");
        unit.emitln("    "+nativeCBFuncVarName+" = func"+jcbNativeBasename+";");
        unit.emitln("  } else {");
        unit.emitln("    "+nativeCBFuncVarName+" = NULL;");
        unit.emitln("    "+nativeUserParamVarName+" = NULL;");
        unit.emitln("  }");
        unit.emitln("  {");
        unit.emitln("    jlong v = (jlong) (intptr_t) "+nativeUserParamVarName+";");
        unit.emitln("    (*env)->SetLongArrayRegion(env, jnativeUserParam, 0, (jsize)1, &v);");
        if( DEBUG_JAVACALLBACK ) {
            unit.emitln("    fprintf(stderr, \"YYY user %p -> native %p\\n\", "+userParamArgName+", "+nativeUserParamVarName+");");
        }
        unit.emitln("  }");
        unit.emitln();
    }
    emitBodyCallCFunction();
    emitBodyUserVariableAssignments();
    emitBodyVariablePostCallCleanup();
    if( emitBodyMapCToJNIType(-1 /* return value */, true /* addLocalVar */) ) {
        unit.emitln("  return _res_jni;");
    }
    unit.emitln("}");
    unit.emitln();
    if( null != jcb ) {
        final String capIfaceName = CodeGenUtils.capitalizeString( getInterfaceName() );
        unit.emitln("JNIEXPORT void JNICALL");
        unit.emit(JavaEmitter.getJNIMethodNamePrefix(getJavaPackageName(), getJavaClassName()));
        unit.emitln("_release"+capIfaceName+"Impl(JNIEnv *env, jobject _unused, jlong jnativeUserParam) {");
        unit.emitln("  // already locked");
        unit.emitln("  T_"+jcbNativeBasename+"* nativeUserParam = (T_"+jcbNativeBasename+"*) (intptr_t) jnativeUserParam;");
        unit.emitln("  if( NULL != nativeUserParam ) {");
        unit.emitln("    (*env)->DeleteGlobalRef(env, nativeUserParam->lockObj);");
        unit.emitln("    (*env)->DeleteGlobalRef(env, nativeUserParam->cbFunc);");
        unit.emitln("    (*env)->DeleteGlobalRef(env, nativeUserParam->userParam);");
        unit.emitln("    free(nativeUserParam);");
        unit.emitln("  }");
        unit.emitln("}");
        unit.emitln();
    }
  }

  protected void emitBodyVariableDeclarations()  {
    // Emit declarations for all pointer and String conversion variables
    if (binding.hasContainingType()) {
      emitPointerDeclaration(binding.getContainingType(),
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
          emitPointerDeclaration(type,
                                 binding.getCArgumentType(i),
                                 convName, javaArgName);
        if (needsDataCopy && !emittedDataCopyTemps) {
          // emit loop counter and array length variables used during data
          // copy
          unit.emitln("  jobject _tmpObj;");
          unit.emitln("  int _copyIndex;");
          unit.emitln("  jsize _tmpArrayLen;");

          // Pointer to the data in the Buffer, taking the offset into account
          if(type.isNIOBufferArray()) {
            unit.emitln("  int * _offsetHandle = NULL;");
          }

          emittedDataCopyTemps = true;
        }
      } else if (type.isString()) {
        final Type cType = binding.getCArgumentType(i);
        if (isUTF8Type(cType)) {
          unit.emit("  const char* ");
        } else {
          unit.emit("  jchar* ");
        }
        unit.emit(STRING_CHARS_PREFIX);
        unit.emit(binding.getArgumentName(i));
        unit.emitln(" = NULL;");
      }

    }

    // Emit declaration for return value if necessary
    final Type cReturnType = binding.getCReturnType();

    final JavaType javaReturnType = binding.getJavaReturnType();
    if (!cReturnType.isVoid()) {
      unit.emit("  ");
      // Note we respect const/volatile in the function return type.
      // However, we cannot have it 'const' for our local variable.
      // See cast in emitBodyCallCFunction(..)!
      unit.emit(binding.getCSymbol().getReturnType().getCName(false));
      unit.emitln(" _res;");
      if (javaReturnType.isNIOByteBufferArray() ||
          javaReturnType.isArrayOfCompoundTypeWrappers()) {
        unit.emit("  int ");
        unit.emit(arrayResLength);
        unit.emitln(";");
        unit.emit("  int ");
        unit.emit(arrayIdx);
        unit.emitln(";");
        unit.emit("  jobjectArray ");
        unit.emit(arrayRes);
        unit.emitln(";");
      } else if (javaReturnType.isArray()) {
        unit.emit("  int ");
        unit.emit(arrayResLength);
        unit.emitln(";");

        final Class<?> componentType = javaReturnType.getJavaClass().getComponentType();
        if (componentType.isArray()) {
          throw new RuntimeException("Multi-dimensional arrays not supported yet");
        }

        final String javaTypeName = componentType.getName();
        final String javaArrayTypeName = "j" + javaTypeName + "Array";
        unit.emit("  ");
        unit.emit(javaArrayTypeName);
        unit.emit(" ");
        unit.emit(arrayRes);
        unit.emitln(";");
      }
    }
  }

  /** Emits the user-defined C variable declarations from the
      TemporaryCVariableDeclarations directive in the .cfg file. */
  protected void emitBodyUserVariableDeclarations() {
    if (temporaryCVariableDeclarations != null) {
      for (final String val : temporaryCVariableDeclarations) {
        unit.emit("  ");
        unit.emitln(val);
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
        type = arrt.getTargetType();
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
  protected void emitBodyVariablePreCallSetup()  {

    // Convert all Buffers to pointers first so we don't have to
    // call ReleasePrimitiveArrayCritical for any arrays if any
    // incoming buffers aren't direct
    if (binding.hasContainingType()) {
        emitPointerConversion(binding,
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
        emitPointerConversion(binding, javaArgType,
                              binding.getCArgumentType(i), javaArgName,
                              pointerConversionArgumentName(javaArgName),
                              byteOffsetArgName(i));
      } else if (javaArgType.isArray() ||
                 javaArgType.isArrayOfCompoundTypeWrappers() ||
                 ( javaArgType.isNIOBuffer() && forIndirectBufferAndArrayImplementation ) ) {
        final boolean needsDataCopy = javaArgTypeNeedsDataCopy(javaArgType);

        unit.emitln("  if ( NULL != " + javaArgName + " ) {");

        final Type cArgType = binding.getCArgumentType(i);
        String cArgTypeName = cArgType.getCName();

        final String convName = pointerConversionArgumentName(javaArgName);

        if (!needsDataCopy) {
          unit.emit("    ");
          unit.emit(convName);
          unit.emit(" = (");
          if (javaArgType.isStringArray()) {
            // java-side type is String[]
            cArgTypeName = "jstring *";
          }
          unit.emit(cArgTypeName);
          unit.emit(") ( JNI_TRUE == " + isNIOArgName(i) + " ? ");
          unit.emit(" (*env)->GetDirectBufferAddress(env, " + javaArgName + ") : ");
          unit.emit(" (*env)->GetPrimitiveArrayCritical(env, " + javaArgName + ", NULL) );");
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

          unit.emitln();
          unit.emitln("    /* Copy contents of " + javaArgName + " into " + convName + "_copy */");

          // get length of array being copied
          final String arrayLenName = "_tmpArrayLen";
          unit.emit("    ");
          unit.emit(arrayLenName);
          unit.emit(" = (*env)->GetArrayLength(env, ");
          unit.emit(javaArgName);
          unit.emitln(");");

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
                  cArgElementType = cArgType.getBaseType();
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
            unit.emitln
              ("    _offsetHandle = (int *) (*env)->GetPrimitiveArrayCritical(env, " +
               byteOffsetArrayArgName(i) +
               ", NULL);");
          }

          // process each element in the array
          unit.emitln("    for (_copyIndex = 0; _copyIndex < "+arrayLenName+"; ++_copyIndex) {");

          // get each array element
          unit.emitln("      /* get each element of the array argument \"" + javaArgName + "\" */");
          unit.emit("      _tmpObj = (*env)->GetObjectArrayElement(env, ");
          unit.emit(javaArgName);
          unit.emitln(", _copyIndex);");

          if (javaArgType.isStringArray()) {
            unit.emit("  ");
            emitGetStringChars("(jstring) _tmpObj",
                               convName+"_copy[_copyIndex]",
                               isUTF8Type(cArgType),
                               true);
          } else if (javaArgType.isNIOBufferArray()) {
            /* We always assume an integer "byte offset" argument follows any Buffer
               in the method binding. */
            emitGetDirectBufferAddress("_tmpObj",
                                       cArgElementType.getCName(),
                                       convName + "_copy[_copyIndex]",
                                       true,
                                       "_offsetHandle[_copyIndex]", true);
          } else if (javaArgType.isArrayOfCompoundTypeWrappers()) {
            // These come down in similar fashion to an array of NIO
            // Buffers only we do not pass down any integer byte
            // offset argument
            emitGetDirectBufferAddress("_tmpObj",
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
            unit.emit("      ");
            emitMalloc(convName+"_copy[_copyIndex]",
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
          unit.emitln("    }");

          if (javaArgType.isNIOBufferArray()) {
            unit.emitln
              ("    (*env)->ReleasePrimitiveArrayCritical(env, " +
               byteOffsetArrayArgName(i) +
               ", _offsetHandle, JNI_ABORT);");
          }

          unit.emitln();
        } // end of data copy

        unit.emitln("  }");

      } else if (javaArgType.isString()) {
        emitGetStringChars(javaArgName,
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
  protected void emitBodyVariablePostCallCleanup() {

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
          unit.emitln("  if ( JNI_FALSE == " + isNIOArgName(i) + " && NULL != " + javaArgName + " ) {");

          // Release array
          final String modeFlag = cArgType.isBaseTypeConst() ? "JNI_ABORT" : "0" ;
          unit.emit("    (*env)->ReleasePrimitiveArrayCritical(env, " + javaArgName + ", " + convName + ", "+modeFlag+");");
        } else {
          unit.emitln("  if ( NULL != " + javaArgName + " ) {");

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
              unit.emitln("    _tmpArrayLen = (*env)->GetArrayLength(env, " + javaArgName + ");");
              unit.emitln("    for (_copyIndex = 0; _copyIndex < _tmpArrayLen; ++_copyIndex) {");
              unit.emitln("      _tmpObj = (*env)->GetObjectArrayElement(env, " + javaArgName + ", _copyIndex);");
              emitReturnDirectBufferAddress("_tmpObj",
                                            cArgType.getBaseType().getCName(),
                                            "("+convName + "_copy + _copyIndex)",
                                            false /* receivingIsPtrPtr */,
                                            null);
              unit.emitln("    }");
            } else {
              throw new GlueGenException(
                "Cannot clean up copied data for ptr-to-ptr arg type \"" + cArgType.getDebugString() +
                "\": support for cleaning up most non-const ptr-to-ptr types not implemented.",
                binding.getCSymbol().getASTLocusTag());
            }
          }

          unit.emitln("    /* Clean up " + convName + "_copy */");

          // Only need to perform cleanup for individual array
          // elements if they are not direct buffers
          if (!javaArgType.isNIOBufferArray() &&
              !javaArgType.isArrayOfCompoundTypeWrappers()) {
            // Re-fetch length of array that was copied
            final String arrayLenName = "_tmpArrayLen";
            unit.emit("    ");
            unit.emit(arrayLenName);
            unit.emit(" = (*env)->GetArrayLength(env, ");
            unit.emit(javaArgName);
            unit.emitln(");");

            // free each element
            final PointerType cArgPtrType = cArgType.asPointer();
            if (cArgPtrType == null) {
              throw new GlueGenException(
                "Could not copy data for type \"" + cArgType.getDebugString() +
                "\"; currently only pointer types supported.",
                binding.getCSymbol().getASTLocusTag());
            }

            // process each element in the array
            unit.emitln("    for (_copyIndex = 0; _copyIndex < " + arrayLenName +"; ++_copyIndex) {");

            // get each array element
            unit.emitln("      /* free each element of " +convName +"_copy */");
            unit.emit("      _tmpObj = (*env)->GetObjectArrayElement(env, ");
            unit.emit(javaArgName);
            unit.emitln(", _copyIndex);");

            if (javaArgType.isStringArray()) {
              unit.emit("     (*env)->ReleaseStringUTFChars(env, ");
              unit.emit("(jstring) _tmpObj");
              unit.emit(", ");
              unit.emit(convName+"_copy[_copyIndex]");
              unit.emitln(");");
            } else {
              throw new GlueGenException(
                "Cannot yet handle type \"" + cArgType.getDebugString() +
                "\"; need to add support for cleaning up copied ptr-to-ptr-to-primitiveType subarrays",
                binding.getCSymbol().getASTLocusTag());
            }
            unit.emitln("    }");
          }

          // free the main array
          unit.emit("    free((void*) ");
          unit.emit(convName+"_copy");
          unit.emitln(");");
        } // end of cleaning up copied data

        unit.emitln("  }");

      } else if (javaArgType.isString()) {
        unit.emitln("  if ( NULL != " + javaArgName + " ) {");

        if (isUTF8Type(cArgType)) {
          unit.emit("    (*env)->ReleaseStringUTFChars(env, ");
          unit.emit(javaArgName);
          unit.emit(", " + STRING_CHARS_PREFIX);
          unit.emit(javaArgName);
          unit.emitln(");");
        } else {
          unit.emitln("    free((void*) " + STRING_CHARS_PREFIX + javaArgName + ");");
        }

        unit.emitln("  }");
      }
    }
  }

  /** Returns the number of arguments passed so calling code knows
      whether to print a comma */
  protected int emitBodyPassCArguments() {
    for (int i = 0; i < binding.getNumArguments(); i++) {
      if (i != 0) {
        unit.emit(", ");
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
        unit.emit("env");
      } else if (binding.isArgumentThisPointer(i)) {
        unit.emit(CMethodBindingEmitter.cThisArgumentName());
      } else {
        unit.emit("(");
        final Type cArgType = binding.getCArgumentType(i);
        final boolean needsDataCopy = javaArgTypeNeedsDataCopy(javaArgType);
        final boolean needsArrayOffset = !needsDataCopy && (
                                     javaArgType.isArray() ||
                                     javaArgType.isArrayOfCompoundTypeWrappers() ||
                                     ( javaArgType.isNIOBuffer() && forIndirectBufferAndArrayImplementation ) );
        unit.emit(cArgType.getCName(true));
        unit.emit(") ");

        if (cArgType.isPointer() && javaArgType.isPrimitive()) {
          unit.emit("(intptr_t) ");
        }
        if (javaArgType.isArray() || javaArgType.isNIOBuffer() ||
            javaArgType.isCompoundTypeWrapper() || javaArgType.isArrayOfCompoundTypeWrappers()) {
          if( needsArrayOffset ) {
              unit.emit("(((char *) ");
          } else if( !cArgType.isPointer() && javaArgType.isCompoundTypeWrapper() ) { // FIXME: Compound call-by-value
              unit.emit("*");
          }
          unit.emit(pointerConversionArgumentName(binding.getArgumentName(i)));
          if ( needsDataCopy ) {
            unit.emit("_copy");
          }
          if( needsArrayOffset ) {
              unit.emit(") + " + byteOffsetArgName(i) + ")");
          }
        } else {
          if (javaArgType.isString()) { unit.emit(STRING_CHARS_PREFIX); }
          unit.emit(binding.getArgumentName(i));
          if( null != this.javaCallback &&
              ( i == this.javaCallback.setFuncCBParamIdx || i == this.javaCallback.setFuncUserParamIdx ) ) {
              unit.emit("_native");
          }
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

  protected void emitBodyCallCFunction() {
    // Make the call to the actual C function
    unit.emit("  ");

    // WARNING: this code assumes that the return type has already been
    // typedef-resolved.
    final Type cReturnType = binding.getCReturnType();

    if (!cReturnType.isVoid()) {
      // Note we respect const/volatile in the function return type.
      // However, we cannot have it 'const' for our local variable.
      // See return type in emitBodyVariableDeclarations(..)!
      unit.emit("_res = (");
      unit.emit(cReturnType.getCName(false));
      unit.emit(") ");
    }
    if ( isCStructFunctionPointer && binding.hasContainingType() ) {
      // Call through function pointer
      unit.emit(CMethodBindingEmitter.cThisArgumentName() + "->");
    }
    unit.emit(getNativeName());
    unit.emit("(");
    emitBodyPassCArguments();
    unit.emitln(");");
  }

  /** Emits the user-defined C variable assignments from the
      TemporaryCVariableAssignments directive in the .cfg file. */
  protected void emitBodyUserVariableAssignments() {
    if (temporaryCVariableAssignments != null) {
      for (final String val : temporaryCVariableAssignments) {
        unit.emit("  ");
        unit.emitln(val);
      }
    }
  }

  /**
   * Emit code, converting a C type into a java JNI-type.
   * <p>
   * The resulting JNI value is assigned to a local JNI variable named cArgName+"_jni"
   * with `cArgName = binding.getArgumentName(argIdx)` or `cArgName = "_res"`.
   * </p>
   * @param argIdx -1 is return value, [0..n] is argument index
   * @param addLocalVar if true, emit instantiating the local JNI variable.
   * @return true if a non-void result has been produced, otherwise false
   */
  protected boolean emitBodyMapCToJNIType(final int argIdx, final boolean addLocalVar)
  {
    // WARNING: this code assumes that the return type has already been
    // typedef-resolved.
    final boolean isReturnVal;
    final Type cType;
    final JavaType javaType;
    final String cArgName;
    if( 0 > argIdx ) {
        isReturnVal = true;
        cType = binding.getCReturnType();
        javaType = binding.getJavaReturnType();
        cArgName = "_res";
    } else {
        isReturnVal = false;
        cType = binding.getCArgumentType(argIdx);
        javaType = binding.getJavaArgumentType(argIdx);
        cArgName = binding.getArgumentName(argIdx);
    }
    final String javaArgName = cArgName + "_jni";

    if ( cType.isVoid() ) {
        // No result to produce
        return false;
    }
    if (javaType.isPrimitive()) {
        if( addLocalVar ) {
            unit.emit("  "+javaType.jniTypeName()+" "+javaArgName+" = ");
        } else {
            unit.emit("  "+javaArgName+" = ");
        }
        if (cType.isPointer()) {
            // Pointer being converted to int or long: cast this result
            // (through intptr_t to avoid compiler warnings with gcc)
            unit.emit("(" + javaType.jniTypeName() + ") (intptr_t) ");
        }
        unit.emit(cArgName);
        unit.emitln(";");
    } else if ( !cType.isPointer() && javaType.isCompoundTypeWrapper() ) { // isReturnCompoundByValue()
        if( addLocalVar ) {
            unit.emit("  "+javaType.jniTypeName()+" "+javaArgName+" = ");
        } else {
            unit.emit("  "+javaArgName+" = ");
        }
        final String returnSizeOf;
        if ( isReturnVal && returnValueCapacityExpression != null ) {
            returnSizeOf = returnValueCapacityExpression.format(argumentNameArray());
        } else {
            returnSizeOf = "sizeof(" + cType.getCName() + ")";
        }
        unit.emitln("JVMUtil_NewDirectByteBufferCopy(env, _clazzBuffers, &"+cArgName+", "+returnSizeOf+");");
        unit.addTailCode(CCodeUnit.NewDirectByteBufferCopyUnitCode);
    } else if (javaType.isNIOBuffer() || javaType.isCompoundTypeWrapper()) {
        if( addLocalVar ) {
            unit.emit("  "+javaType.jniTypeName()+" "+javaArgName+" = ");
        } else {
            unit.emit("  "+javaArgName+" = ");
        }
        unit.emit("(NULL == "+cArgName+") ? NULL : (*env)->NewDirectByteBuffer(env, (void *)"+cArgName+", ");

        // See whether capacity has been specified
        if ( isReturnVal && returnValueCapacityExpression != null) {
            unit.emitln( returnValueCapacityExpression.format( argumentNameArray() ) + ");");
        } else {
            final Type cTargetType = cType.isPointer() ? cType.getTargetType() : null;
            int mode = 0;
            if ( 1 == cType.pointerDepth() && null != cTargetType ) {
                if( cTargetType.isCompound() ) {
                    if( !cTargetType.isAnon() &&
                        cTargetType.asCompound().getNumFields() > 0 )
                    {
                        // fully declared non-anonymous struct pointer: pass content
                        if ( cTargetType.getSize() == null ) {
                            throw new GlueGenException(
                                    "Error emitting code for compound type "+
                                    "for function \"" + binding + "\": " +
                                    "Structs to be emitted should have been laid out by this point " +
                                    "(type " + cTargetType.getCName() + " / " +
                                    cTargetType.getDebugString() + " was not) for "+binding.getCSymbol(),
                                    binding.getCSymbol().getASTLocusTag() );
                        }
                        unit.emitln("sizeof(" + cTargetType.getCName() + ") );");
                        mode = 10;
                    } else if( cTargetType.asCompound().getNumFields() == 0 ) {
                        // anonymous struct pointer: pass pointer
                        unit.emitln("sizeof(" + cType.getCName() + ") );");
                        mode = 11;
                    }
                }
                if( 0 == mode ) {
                    if( cTargetType.isPrimitive() ) {
                        // primitive pointer: pass primitive
                        unit.emitln("sizeof(" + cTargetType.getCName() + ") );");
                        mode = 20;
                    } else if( cTargetType.isVoid() ) {
                        // void pointer: pass pointer
                        unit.emitln("sizeof(" + cType.getCName() + ") );");
                        mode = 21;
                    }
                }
            }
            if( 0 == mode ) {
                if( null != cfg.typeInfo(cType) ) { // javaReturnType.isOpaqued() covered above via isPrimitive()
                    // Opaque
                    unit.emitln("sizeof(" + cType.getCName()  + ") );");
                    mode = 88;
                } else {
                    final String wmsg = "Assumed return size of equivalent C return type";
                    unit.emitln("sizeof(" + cType.getCName()  + ") ); // WARNING: "+wmsg);
                    mode = 99;
                    LOG.warning(binding.getCSymbol().getASTLocusTag(),
                            "No capacity specified for java.nio.Buffer return " +
                            "value for function \"" + binding.getName() + "\". " + wmsg + " (sizeof(" + cType.getCName() + ")): " + binding);
                }
            }
            unit.emitln("  /** ");
            unit.emitln("   * mode: "+mode+", arg #"+argIdx);
            unit.emitln("   * cType: "+cType.getDebugString());
            unit.emitln("   * cTargetType: "+cTargetType.getDebugString());
            unit.emitln("   * javaType: "+javaType.getDebugString());
            unit.emitln("   */");
        }
    } else if (javaType.isString()) {
        if( addLocalVar ) {
            unit.emit("  "+javaType.jniTypeName()+" "+javaArgName+" = ");
        } else {
            unit.emit("  "+javaArgName+" = ");
        }
        unit.emitln("(NULL == "+cArgName+") ? NULL : (*env)->NewStringUTF(env, (const char *)"+cArgName+");");
    } else if (javaType.isArrayOfCompoundTypeWrappers() ||
              ( javaType.isArray() && javaType.isNIOByteBufferArray() ) )
    {
        if( addLocalVar ) {
            unit.emitln("  "+javaType.jniTypeName()+" "+javaArgName+";");
        }
        unit.emitln("  if (NULL == "+cArgName+") { "+javaArgName+" = NULL; } else {");
        if ( !isReturnVal || returnValueLengthExpression == null ) {
            throw new GlueGenException("Error while generating C code: No length specified for array returned from function for arg #" +
                                       argIdx + ", "+cType.getDebugString()+", for "+
                                       binding, binding.getCSymbol().getASTLocusTag());
        } // TODO: Perhaps allow usage for non-return types, i.e. configure 'returnValueLengthExpression' for all arguments.
        unit.emitln("    " + arrayResLength + " = " + returnValueLengthExpression.format(argumentNameArray()) + ";");
        unit.emitln("    " + arrayRes + " = (*env)->NewObjectArray(env, " + arrayResLength + ", (*env)->FindClass(env, \"java/nio/ByteBuffer\"), NULL);");
        unit.emitln("    for (" + arrayIdx + " = 0; " + arrayIdx + " < " + arrayResLength + "; " + arrayIdx + "++) {");
        final Type retType = binding.getCSymbol().getReturnType();
        final Type pointerType = retType.getArrayBaseOrPointerTargetType();
        unit.emitln("      (*env)->SetObjectArrayElement(env, " + arrayRes + ", " + arrayIdx +
                    ", (*env)->NewDirectByteBuffer(env, (void *)"+cArgName+"[" + arrayIdx + "], sizeof(" + pointerType.getCName() + ")));");
        unit.emitln("    }");
        unit.emitln("  "+javaArgName+" = " + arrayRes + ";");
        unit.emitln("  }");
    } else if (javaType.isArray()) {
        // FIXME: must have user provide length of array in .cfg file
        // by providing a constant value, input parameter, or
        // expression which computes the array size (already present
        // as ReturnValueCapacity, not yet implemented / tested here)
        throw new GlueGenException(
                "Could not emit native code for arg #"+argIdx+", "+cType.getDebugString()+", for " + binding +
                ": array return values for non-char types not implemented yet.",
                binding.getCSymbol().getASTLocusTag());

        // FIXME: This is approximately what will be required here
        //
        //unit().emit("  ");
        //unit().emit(arrayRes);
        //unit().emit(" = (*env)->New");
        //unit().emit(capitalizedComponentType);
        //unit().emit("Array(env, ");
        //unit().emit(arrayResLength);
        //unit().emitln(");");
        //unit().emit("  (*env)->Set");
        //unit().emit(capitalizedComponentType);
        //unit().emit("ArrayRegion(env, ");
        //unit().emit(arrayRes);
        //unit().emit(", 0, ");
        //unit().emit(arrayResLength);
        //unit().emitln(", "+cArgName+");");
        //unit().emit("  return ");
        //unit().emit(arrayRes);
        //unit().emitln(";");
    } else {
        throw new GlueGenException("Unhandled return type: arg #"+argIdx+", C "+cType.getDebugString()+", java "+javaType.getDebugString()+" for "+binding,
                                   binding.getCSymbol().getReturnType().getASTLocusTag());
    }
    return true;
  }

  protected static String cThisArgumentName() {
    return "this0";
  }

  protected String jniMangle(final MethodBinding binding) {
    final StringBuilder buf = new StringBuilder();
    buf.append(JavaEmitter.jniMangle(getImplName()));
    buf.append(getImplSuffix());
    if( null == this.javaCallback ) {
        buf.append("__");
        getJNIMangledArgs(binding, forIndirectBufferAndArrayImplementation, buf);
        if( null != this.javaCallback ) {
            getJNIMangledArg(String.class, buf, false);  // to account for the additional 'jstring jcallbackSignature' parameter
            getJNIMangledArg(long[].class, buf, false);  // to account for the additional 'long[] nativeUserParam' parameter
        }
    }
    return buf.toString();
  }

  /**
   * Return the mangled JNI argument names of given binding.
   * @param binding
   * @param forIndirectBufferAndArrayImplementation If true, this CMethodBindingEmitter implements the case of an indirect buffer or array being passed down to C code, otherwise false.
   * @param buf
   * @return
   */
  public static StringBuilder getJNIMangledArgs(final MethodBinding binding, final boolean forIndirectBufferAndArrayImplementation, final StringBuilder buf) {
    if (binding.isReturnCompoundByValue()) {
        getJNIMangledArg(Class.class, buf, true);
    }
    if (binding.hasContainingType()) {
      // "this" argument always comes down in argument 0 as direct buffer
      getJNIMangledArg(java.nio.ByteBuffer.class, buf, true);
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
          getJNIMangledArg(c, buf, false);
          // If Buffer offset arguments were added, we need to mangle the JNI for the
          // extra arguments
          if (type.isNIOBuffer()) {
            getJNIMangledArg(Integer.TYPE, buf, false);
            if(forIndirectBufferAndArrayImplementation) {
                getJNIMangledArg(Boolean.TYPE, buf, false);
            }
          } else if (type.isNIOBufferArray())   {
            final int[] intArrayType = new int[0];
            c = intArrayType.getClass();
            getJNIMangledArg(c , buf, true);
          }
          if (type.isPrimitiveArray()) {
            getJNIMangledArg(Integer.TYPE, buf, false);
          }
        } else if (type.isNamedClass()) {
          buf.append(type.getJNIMethodDesciptor());
        } else if (type.isCompoundTypeWrapper()) {
          // Mangle wrappers for C structs as ByteBuffer
          getJNIMangledArg(java.nio.ByteBuffer.class, buf, true);
        } else if (type.isArrayOfCompoundTypeWrappers()) {
          // Mangle arrays of C structs as ByteBuffer[]
          final java.nio.ByteBuffer[] tmp = new java.nio.ByteBuffer[0];
          getJNIMangledArg(tmp.getClass(), buf, true);
        } else if (type.isJNIEnv()) {
          // These are not exposed at the Java level
        } else {
          // FIXME: add support for char* -> String conversion
          throw new GlueGenException("Unknown kind of JavaType: arg "+i+", name="+type.getName()+" of "+binding,
                                     binding.getCArgumentType(i).getASTLocusTag());
        }
      }
    }
    return buf;
  }

  public static void getJNIMangledArg(final Class<?> c, final StringBuilder res, final boolean syntheticArgument) {
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
          getJNIMangledArg(componentType, res,
                    (componentType == java.nio.ByteBuffer.class));
        } else {
          res.append("L");
          res.append(c.getName().replace('.', '_'));
          res.append("_2");
        }
      } else {
        if (c.isArray()) {
          res.append("_3");
          getJNIMangledArg(c.getComponentType(), res, false);
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

  private void emitOutOfMemoryCheck(final String varName, final String errorMessage)  {
    unit.emitln("  if ( NULL == " + varName + " ) {");
    unit.emitln("      (*env)->ThrowNew(env, (*env)->FindClass(env, \"java/lang/OutOfMemoryError\"),");
    unit.emit("                       \"" + errorMessage);
    unit.emit(" in native dispatcher for \\\"");
    unit.emit(getInterfaceName());
    unit.emitln("\\\"\");");
    unit.emit("      return");
    if (!binding.getJavaReturnType().isVoid()) {
      unit.emit(" 0");
    }
    unit.emitln(";");
    unit.emitln("    }");
  }

  private void emitMalloc(final String targetVarName,
                          final String elementTypeString,
                          final boolean elementTypeIsConst,
                          final String numElementsExpression,
                          final String mallocFailureErrorString)  {
    unit.emit("    ");
    unit.emit(targetVarName);
    unit.emit(" = (");
    if(elementTypeIsConst) {
        unit.emit("const ");
    }
    unit.emit(elementTypeString);
    unit.emit(" *) malloc(");
    unit.emit(numElementsExpression);
    unit.emit(" * sizeof(");
    unit.emit(elementTypeString);
    unit.emitln("));");
    // Catch memory allocation failure
    emitOutOfMemoryCheck( targetVarName, mallocFailureErrorString);
  }

  private void emitCalloc(final String targetVarName,
                          final String elementTypeString,
                          final String numElementsExpression,
                          final String mallocFailureErrorString)  {
    unit.emit("    ");
    unit.emit(targetVarName);
    unit.emit(" = (");
    unit.emit(elementTypeString);
    unit.emit(" *) calloc(");
    unit.emit(numElementsExpression);
    unit.emit(", sizeof(");
    unit.emit(elementTypeString);
    unit.emitln("));");
    // Catch memory allocation failure
    emitOutOfMemoryCheck( targetVarName, mallocFailureErrorString);
  }

  private void emitGetStringChars(final String sourceVarName,
                                  final String receivingVarName,
                                  final boolean isUTF8,
                                  final boolean emitElseClause)  {
    unit.emitln("  if ( NULL != " + sourceVarName + " ) {");

    if (isUTF8) {
      unit.emit("    ");
      unit.emit(receivingVarName);
      unit.emit(" = (*env)->GetStringUTFChars(env, ");
      unit.emit(sourceVarName);
      unit.emitln(", (jboolean*)NULL);");
      // Catch memory allocation failure in the event that the VM didn't pin
      // the String and failed to allocate a copy
      emitOutOfMemoryCheck( receivingVarName, "Failed to get UTF-8 chars for argument \\\""+sourceVarName+"\\\"");
    } else {
      // The UTF-16 case is basically Windows specific. Unix platforms
      // tend to use only the UTF-8 encoding. On Windows the problem
      // is that wide character strings are expected to be null
      // terminated, but the JNI GetStringChars doesn't return a
      // null-terminated Unicode string. For this reason we explicitly
      // calloc our buffer, including the null terminator, and use
      // GetStringRegion to fetch the string's characters.
      emitCalloc(receivingVarName,
                 "jchar",
                 "(*env)->GetStringLength(env, " + sourceVarName + ") + 1",
                 "Could not allocate temporary buffer for copying string argument \\\""+sourceVarName+"\\\"");
      unit.emitln("    (*env)->GetStringRegion(env, " + sourceVarName + ", 0, (*env)->GetStringLength(env, " + sourceVarName + "), " + receivingVarName + ");");
    }
    unit.emit("  }");
    if (emitElseClause) {
      unit.emit(" else {");
      unit.emit("      ");
      unit.emit(receivingVarName);
      unit.emitln(" = NULL;");
      unit.emitln("  }");
    } else {
      unit.emitln();
    }
  }

  private void emitGetDirectBufferAddress(final String sourceVarName,
                                          final String receivingVarTypeString,
                                          final String receivingVarName,
                                          final boolean receivingIsPtrPtr,
                                          final String byteOffsetVarName, final boolean emitElseClause) {
    unit.emitln("    if ( NULL != " + sourceVarName + " ) {");
    unit.emit("    ");
    unit.emit("    ");
    if( receivingIsPtrPtr ) {
        unit.emit(receivingVarName+" = ("+receivingVarTypeString+") (((char*) (*env)->GetDirectBufferAddress(env, "+sourceVarName+"))");
        unit.emitln(" + " + ((byteOffsetVarName != null) ? byteOffsetVarName : "0") + ");");
    } else {
        // linear layout -> use memcpy
        unit.emitln("memcpy((void *)"+receivingVarName+", (*env)->GetDirectBufferAddress(env, "+sourceVarName+"), sizeof("+receivingVarTypeString+"));");
    }

    if (emitElseClause) {
      unit.emitln("    } else {");
      unit.emit("    ");
      unit.emit("    ");
      if( receivingIsPtrPtr ) {
          unit.emit(receivingVarName);
          unit.emitln(" = NULL;");
      } else {
          unit.emitln("memset((void *)"+receivingVarName+", 0, sizeof("+receivingVarTypeString+"));");
      }
    }
    unit.emitln("    }");
    unit.emitln();
  }

  private void emitReturnDirectBufferAddress(final String sourceVarName,
                                             final String receivingVarTypeString,
                                             final String receivingVarName,
                                             final boolean receivingIsPtrPtr,
                                             final String byteOffsetVarName) {
    unit.emit("    ");
    unit.emit("    ");
    if( receivingIsPtrPtr ) {
        unit.emit("(((char*) (*env)->GetDirectBufferAddress(env, "+sourceVarName+"))");
        unit.emitln(" + " + ((byteOffsetVarName != null) ? byteOffsetVarName : "0") + ") = "+receivingVarName+";");
        throw new RuntimeException("incomplete implementation"); // FIXME doesn't work, currently unused
    } else {
        // linear layout -> use memcpy
        unit.emitln("memcpy((*env)->GetDirectBufferAddress(env, "+sourceVarName+"), "+receivingVarName+", sizeof("+receivingVarTypeString+"));");
    }
    unit.emitln();
  }

  // Note: if the data in the Type needs to be converted from the Java memory
  // model to the C memory model prior to calling any C-side functions, then
  // an extra variable named XXX_copy (where XXX is the value of the
  // cVariableName argument) will be emitted and TRUE will be returned.
  private boolean emitPointerDeclaration(final JavaType javaType,
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

    unit.emit("  ");
    if (!needsDataCopy) {
      // declare the pointer variable
      unit.emit(ptrTypeString);
      if( !cType.isPointer() && javaType.isCompoundTypeWrapper() ) { // FIXME: Compound call-by-value
          unit.emit(" * ");
      } else {
          unit.emit(" ");
      }
      unit.emit(cVariableName);
      unit.emitln(" = NULL;");
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
            unit.emit("const ");
        }
        unit.emit(cElementTypeName+" *");
      } else {
        if (cType.isBaseTypeConst()) {
            unit.emit("const ");
        }
        unit.emit(ptrTypeString);
      }
      unit.emit(" ");
      unit.emit(cVariableName);
      unit.emit("_copy = NULL; /* copy of data in ");
      unit.emit(javaArgumentName);
      unit.emitln(", laid out according to C memory model */");
    }

    return needsDataCopy;
  }

  private void emitPointerConversion(final MethodBinding binding,
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
    emitGetDirectBufferAddress(incomingArgumentName,
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
