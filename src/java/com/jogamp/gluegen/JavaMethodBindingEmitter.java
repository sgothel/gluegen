/**
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

import com.jogamp.gluegen.JavaConfiguration.JavaCallbackInfo;
import com.jogamp.gluegen.cgram.HeaderParser;
import com.jogamp.gluegen.cgram.types.AliasedSymbol;
import com.jogamp.gluegen.cgram.types.ArrayType;
import com.jogamp.gluegen.cgram.types.EnumType;
import com.jogamp.gluegen.cgram.types.FunctionSymbol;
import com.jogamp.gluegen.cgram.types.Type;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Emits the Java-side component (interface and.or implementation) of the Java<->C JNI binding to its {@link CodeUnit}, see {@link FunctionEmitter}. */
public class JavaMethodBindingEmitter extends FunctionEmitter {

  public static final EmissionModifier PUBLIC = new EmissionModifier("public");
  public static final EmissionModifier PROTECTED = new EmissionModifier("protected");
  public static final EmissionModifier PRIVATE = new EmissionModifier("private");
  public static final EmissionModifier ABSTRACT = new EmissionModifier("abstract");
  public static final EmissionModifier FINAL = new EmissionModifier("final");
  public static final EmissionModifier NATIVE = new EmissionModifier("native");
  public static final EmissionModifier SYNCHRONIZED = new EmissionModifier("synchronized");

  protected final CommentEmitter defaultJavaCommentEmitter = new DefaultCommentEmitter();
  protected final CommentEmitter defaultInterfaceCommentEmitter = new InterfaceCommentEmitter();
  protected final boolean tagNativeBinding;
  protected final boolean useNIODirectOnly;

  // Exception type raised in the generated code if runtime checks fail
  private final String runtimeExceptionType;
  private final String unsupportedExceptionType;
  private final boolean useNIOOnly;
  private final boolean isNativeMethod;
  private final boolean isUnimplemented;

  private boolean emitBody;
  private boolean eraseBufferAndArrayTypes;
  private boolean isPrivateNativeMethod;
  private boolean forDirectBufferImplementation;
  private boolean forIndirectBufferAndArrayImplementation;

  // Manually-specified prologue and epilogue code
  protected List<String> prologue;
  protected List<String> epilogue;

  // A non-null value indicates that rather than returning a compound
  // type accessor we are returning an array of such accessors; this
  // expression is a MessageFormat string taking the names of the
  // incoming Java arguments as parameters and computing as an int the
  // number of elements of the returned array.
  private String returnedArrayLengthExpression;
  private boolean returnedArrayLengthExpressionOnlyForComments = false;

  private final JavaCallbackInfo javaCallback;

  // A suffix used to create a temporary outgoing array of Buffers to
  // represent an array of compound type wrappers
  private static final String COMPOUND_ARRAY_SUFFIX = "_buf_array_copy";

  public JavaMethodBindingEmitter(final MethodBinding binding,
                                  final CodeUnit unit,
                                  final String runtimeExceptionType,
                                  final String unsupportedExceptionType,
                                  final boolean emitBody,
                                  final boolean tagNativeBinding,
                                  final boolean eraseBufferAndArrayTypes,
                                  final boolean useNIOOnly,
                                  final boolean useNIODirectOnly,
                                  final boolean forDirectBufferImplementation,
                                  final boolean forIndirectBufferAndArrayImplementation,
                                  final boolean isUnimplemented,
                                  final boolean isInterface,
                                  final boolean isNativeMethod,
                                  final boolean isPrivateNativeMethod, final JavaConfiguration configuration) {
    super(binding, unit, isInterface, configuration);
    this.runtimeExceptionType = runtimeExceptionType;
    this.unsupportedExceptionType = unsupportedExceptionType;
    this.emitBody = emitBody;
    this.tagNativeBinding = tagNativeBinding;
    this.eraseBufferAndArrayTypes = eraseBufferAndArrayTypes;
    this.useNIOOnly = useNIOOnly;
    this.useNIODirectOnly = useNIODirectOnly;
    this.forDirectBufferImplementation = forDirectBufferImplementation;
    this.forIndirectBufferAndArrayImplementation = forIndirectBufferAndArrayImplementation;
    this.isUnimplemented = isUnimplemented;
    this.isNativeMethod = isNativeMethod;
    this.isPrivateNativeMethod = isPrivateNativeMethod;
    if (isPrivateNativeMethod) {
      setCommentEmitter(defaultJavaCommentEmitter);
    } else {
      setCommentEmitter(defaultInterfaceCommentEmitter);
    }
    javaCallback = cfg.setFuncToJavaCallbackMap.get(binding.getName());
    // !forImplementingMethodCall && !isInterface
  }

  public JavaMethodBindingEmitter(final JavaMethodBindingEmitter arg) {
    super(arg);
    runtimeExceptionType          = arg.runtimeExceptionType;
    unsupportedExceptionType      = arg.unsupportedExceptionType;
    emitBody                      = arg.emitBody;
    tagNativeBinding              = arg.tagNativeBinding;
    eraseBufferAndArrayTypes      = arg.eraseBufferAndArrayTypes;
    useNIOOnly                    = arg.useNIOOnly;
    useNIODirectOnly              = arg.useNIODirectOnly;
    isNativeMethod                = arg.isNativeMethod;
    isPrivateNativeMethod         = arg.isPrivateNativeMethod;
    forDirectBufferImplementation = arg.forDirectBufferImplementation;
    forIndirectBufferAndArrayImplementation = arg.forIndirectBufferAndArrayImplementation;
    isUnimplemented               = arg.isUnimplemented;
    prologue                      = arg.prologue;
    epilogue                      = arg.epilogue;
    returnedArrayLengthExpression = arg.returnedArrayLengthExpression;
    returnedArrayLengthExpressionOnlyForComments = arg.returnedArrayLengthExpressionOnlyForComments;
    javaCallback                  = arg.javaCallback;
  }

  public boolean isNativeMethod() { return isNativeMethod; }
  public boolean isPrivateNativeMethod() { return isPrivateNativeMethod; }
  public boolean isForDirectBufferImplementation() { return forDirectBufferImplementation; }
  public boolean isForIndirectBufferAndArrayImplementation() { return forIndirectBufferAndArrayImplementation; }

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

  protected String getArgumentName(final int i) {
    return binding.getArgumentName(i);
  }

  /** The type of exception (must subclass
      <code>java.lang.RuntimeException</code>) raised if runtime
      checks fail in the generated code. */
  public String getRuntimeExceptionType() {
    return runtimeExceptionType;
  }

  public String getUnsupportedExceptionType() {
    return unsupportedExceptionType;
  }

  /** If the underlying function returns an array (currently only
      arrays of compound types are supported) as opposed to a pointer
      to an object, this method should be called to provide a
      MessageFormat string containing an expression that computes the
      number of elements of the returned array. The parameters to the
      MessageFormat expression are the names of the incoming Java
      arguments. */
  public void setReturnedArrayLengthExpression(final String expr) {
    returnedArrayLengthExpression = expr;
    returnedArrayLengthExpressionOnlyForComments = false;
  }
  protected void setReturnedArrayLengthExpression(final String expr, final boolean onlyForComments) {
    returnedArrayLengthExpression = expr;
    returnedArrayLengthExpressionOnlyForComments = onlyForComments;
  }
  protected String getReturnedArrayLengthExpression() {
    return returnedArrayLengthExpressionOnlyForComments ? null : returnedArrayLengthExpression;
  }
  protected String getReturnedArrayLengthComment() {
    return returnedArrayLengthExpression;
  }

  /** Sets the manually-generated prologue code for this emitter. */
  public void setPrologue(final List<String> prologue) {
    this.prologue = prologue;
  }

  /** Sets the manually-generated epilogue code for this emitter. */
  public void setEpilogue(final List<String> epilogue) {
    this.epilogue = epilogue;
  }

  /** Indicates whether this emitter will print only a signature, or
      whether it will emit Java code for the body of the method as
      well. */
  public boolean signatureOnly() {
    return !emitBody;
  }

  /** Accessor for subclasses. */
  public void setEmitBody(final boolean emitBody) {
    this.emitBody = emitBody;
  }

  /** Accessor for subclasses. */
  public void setEraseBufferAndArrayTypes(final boolean erase) {
    this.eraseBufferAndArrayTypes = erase;
  }

  /** Accessor for subclasses. */
  public void setPrivateNativeMethod(final boolean v) {
    this.isPrivateNativeMethod = v;
  }

  /** Accessor for subclasses. */
  public void setForDirectBufferImplementation(final boolean direct) {
    this.forDirectBufferImplementation = direct;
  }

  /** Accessor for subclasses. */
  public void setForIndirectBufferAndArrayImplementation(final boolean indirect) {
    this.forIndirectBufferAndArrayImplementation = indirect;
  }

  @Override
  protected void emitReturnType()  {
    unit.emit(getReturnTypeString(false));
  }

  protected String erasedTypeString(final JavaType type, final boolean skipBuffers) {
    if (eraseBufferAndArrayTypes) {
      if (type.isNIOBuffer()) {
        if (!skipBuffers) {
          // Direct buffers and arrays sent down as Object (but
          // returned as e.g. ByteBuffer)
          return "Object";
        }
        if (!type.isNIOByteBuffer()) {
          // Return buffer requiring change of view from ByteBuffer to e.g. LongBuffer
          return "ByteBuffer";
        }
      } else if (type.isPrimitiveArray()) {
        if (!skipBuffers) {
          // Direct buffers and arrays sent down as Object (but
          // returned as e.g. ByteBuffer)
          return "Object";
        }
      } else if (type.isNIOBufferArray()) {
        // Arrays of direct Buffers sent down as Object[]
        // (Note we don't yet support returning void**)
        return "Object[]";
      } else if (type.isCompoundTypeWrapper()) {
        // Compound type wrappers are unwrapped to ByteBuffer
        return "ByteBuffer";
      } else if (type.isArrayOfCompoundTypeWrappers()) {
        if (skipBuffers) {
          return "ByteBuffer";
        } else {
          // In the case where this is called with a false skipBuffers
          // argument we want to erase the array of compound type
          // wrappers to ByteBuffer[]
          return "ByteBuffer[]";
        }
      }
    }
    String name = type.getName();
    if( null == name ) {
        throw new IllegalArgumentException("null type name: "+type.getDebugString());
    }
    final int index = name.lastIndexOf('.')+1; // always >= 0
    name = name.substring(index);

    if (type.isArrayOfCompoundTypeWrappers()) {
      // We don't want to bake the array specification into the type name
      return name + "[]";
    }
    return name;
  }

  protected String getReturnTypeString(final boolean skipArray) {
    // The first arm of the "if" clause is used by the glue code
    // generation for arrays of compound type wrappers
    if (skipArray ||
    // The following arm is used by most other kinds of return types
        (getReturnedArrayLengthExpression() == null &&
         !binding.getJavaReturnType().isArrayOfCompoundTypeWrappers()) ||
    // The following arm is used specifically to get the splitting up
    // of one returned ByteBuffer into an array of compound type
    // wrappers to work (e.g., XGetVisualInfo)
        (eraseBufferAndArrayTypes &&
         binding.getJavaReturnType().isCompoundTypeWrapper() &&
         (getReturnedArrayLengthExpression() != null))) {
      return erasedTypeString(binding.getJavaReturnType(), true);
    }
    return erasedTypeString(binding.getJavaReturnType(), true) + "[]";
  }

  @Override
  protected void emitName()  {
    if (isPrivateNativeMethod) {
      unit.emit(getNativeImplMethodName());
    } else if( isInterface()) {
      unit.emit(getInterfaceName());
    } else {
      unit.emit(getImplName());
    }
  }

  @Override
  protected int emitArguments() {
    boolean needComma = false;
    int numEmitted = 0;

    if( hasModifier(JavaMethodBindingEmitter.NATIVE) && binding.isReturnCompoundByValue() ) {
      unit.emit("final Class<?> _clazzBuffers");
      ++numEmitted;
      needComma = true;
    }
    if (isPrivateNativeMethod  && binding.hasContainingType()) {
      // Always emit outgoing "this" argument
      if (needComma) {
        unit.emit(", ");
      }
      unit.emit("ByteBuffer ");
      unit.emit(javaThisArgumentName());
      ++numEmitted;
      needComma = true;
    }

    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType type = binding.getJavaArgumentType(i);
      if (type.isVoid()) {
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        if (binding.getNumArguments() != 1) {
          throw new InternalError(
            "\"void\" argument type found in " +
            "multi-argument function \"" + binding + "\"");
        }
        continue;
      }

      if (type.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        // Don't need to expose these at the Java level
        continue;
      }

      if (needComma) {
        unit.emit(", ");
      }

      unit.emit(erasedTypeString(type, false));
      unit.emit(" ");
      unit.emit(getArgumentName(i));

      ++numEmitted;
      needComma = true;

      // Add Buffer and array index offset arguments after each associated argument
      if (forDirectBufferImplementation || forIndirectBufferAndArrayImplementation) {
        if (type.isNIOBuffer()) {
          unit.emit(", int " + byteOffsetArgName(i));
          if(!useNIODirectOnly) {
              unit.emit(", boolean " + isNIOArgName(i));
          }
        } else if (type.isNIOBufferArray()) {
          unit.emit(", int[] " +  byteOffsetArrayArgName(i));
        }
      }

      // Add offset argument after each primitive array
      if (type.isPrimitiveArray()) {
        if(useNIOOnly) {
            throw new RuntimeException("NIO[Direct]Only "+binding+" is set, but "+getArgumentName(i)+" is a primitive array");
        }
        unit.emit(", int " + offsetArgName(i));
      }
    }
    if( hasModifier(JavaMethodBindingEmitter.NATIVE) &&
        null != javaCallback )
    {
        if (needComma) {
            unit.emit(", ");
        }
        unit.emit("String callbackSignature, Object lockObj, long[/*1*/] nativeUserParam");
        ++numEmitted;
    }
    return numEmitted;
  }

  protected String getNativeImplMethodName() {
    return binding.getImplName() + ( useNIODirectOnly ? "0" : "1" );
  }

  protected String byteOffsetArgName(final int i) {
    return byteOffsetArgName(getArgumentName(i));
  }

  protected static String byteOffsetArgName(final String s) {
    return s + "_byte_offset";
  }

  protected String isNIOArgName(final int i) {
    return isNIOArgName(binding.getArgumentName(i));
  }

  protected String isNIOArgName(final String s) {
    return s + "_is_direct";
  }

  protected String byteOffsetArrayArgName(final int i) {
    return getArgumentName(i) + "_byte_offset_array";
  }

  protected String offsetArgName(final int i) {
    return getArgumentName(i) + "_offset";
  }

  private static final boolean DEBUG_JAVACALLBACK = false;

  private final void emitJavaCallbackBirefAPIDoc(final String actionText, final String relationToKey, final String noKeyText, final String relationToFunc) {
      unit.emit("  /** "+actionText);
      if( javaCallback.setFuncKeyIndices.size() > 0 ) {
          unit.emit(relationToKey);
          unit.emit("Key { "+binding.getJavaSelectParameter(new StringBuilder(), javaCallback.setFuncKeyIndices, false).toString()+" } ");
      } else {
          unit.emit(noKeyText);
      }
      unit.emit(relationToFunc);
      unit.emit("<br> <code>");
      emitSignature();
      unit.emitln("</code> **/");
  }
  private final void emitJavaCallbackKeyClass(final String capIfaceName, final String keyClassName) {
      emitJavaCallbackBirefAPIDoc("", "", "", "for ");
      unit.emitln("  public static class "+keyClassName+" {");
      binding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
          if( !cType.isVoid() && javaCallback.setFuncKeyIndices.contains(idx) ) {
              unit.emitln("    public final "+jType+" "+name+";");
              return true;
          } else {
              return false;
          }
      } );
      unit.emitln("    public "+keyClassName+"("+binding.getJavaSelectParameter(new StringBuilder(), javaCallback.setFuncKeyIndices, false).toString()+") {");
      binding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
          if( !cType.isVoid() && javaCallback.setFuncKeyIndices.contains(idx) ) {
              unit.emitln("      this."+name+" = "+name+";");
              return true;
          } else {
              return false;
          }
      } );
      unit.emitln("    }");
      unit.emitln("    @Override");
      unit.emitln("    public boolean equals(final Object o) {");
      unit.emitln("      if( this == o ) {");
      unit.emitln("        return true;");
      unit.emitln("      }");
      unit.emitln("      if( !(o instanceof "+keyClassName+") ) {");
      unit.emitln("        return false;");
      unit.emitln("      }");
      {
          final int count = binding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
              if( !cType.isVoid() && javaCallback.setFuncKeyIndices.contains(idx) ) {
                  if( 0 == consumedCount ) {
                      unit.emitln("      final "+keyClassName+" o2 = ("+keyClassName+")o;");
                      unit.emit  ("      return ");
                  } else {
                      unit.emitln(" &&");
                      unit.emit  ("             ");
                  }
                  if( jType.isPrimitive() || idx == javaCallback.setFuncUserParamIdx ) {
                      unit.emit(name+" == o2."+name);
                  } else {
                      unit.emit(name+".equals( o2."+name+" )");
                  }
                  return true;
              } else {
                  return false;
              }
          } );
          if( 0 == count ) {
              unit.emit("      return true");
          }
          unit.emitln(";");
      }
      unit.emitln("    }");
      unit.emitln("    @Override");
      unit.emitln("    public int hashCode() {");
      {
          final int count = binding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
              if( !cType.isVoid() && javaCallback.setFuncKeyIndices.contains(idx) ) {
                  if( 0 == consumedCount ) {
                      unit.emitln("      // 31 * x == (x << 5) - x");
                      unit.emit  ("      int hash = ");
                  } else {
                      unit.emit  ("      hash = ((hash << 5) - hash) + ");
                  }
                  if( jType.isPrimitive() ) {
                      if( jType.isLong() ) {
                          unit.emitln("HashUtil.getAddrHash32_EqualDist( "+name+" );");
                      } else {
                          unit.emitln(name+";");
                      }
                  } else {
                      if( idx == javaCallback.setFuncUserParamIdx ) {
                          unit.emitln("System.identityHashCode( "+name+" );");
                      } else {
                          unit.emitln(name+".hashCode();");
                      }
                  }
                  return true;
              } else {
                  return false;
              }
          } );
          if( 0 == count ) {
              unit.emitln("      return 0;");
          } else {
              unit.emitln("      return hash;");
          }
      }
      unit.emitln("    }");
      unit.emitln("  }");
  }
  private final void emitJavaCallbackDataClass(final String capIfaceName, final String dataClassName) {
      unit.emitln("  private static class "+dataClassName+" {");
      unit.emitln("    final "+javaCallback.cbFuncTypeName+" func;");
      unit.emitln("    final Object param;");
      unit.emitln("    final long nativeParam;");
      unit.emitln("    "+dataClassName+"("+javaCallback.cbFuncTypeName+" func, Object param, long nativeParam) {");
      unit.emitln("      this.func = func;");
      unit.emitln("      this.param = param;");
      unit.emitln("      this.nativeParam = nativeParam;");
      unit.emitln("    }");
      unit.emitln("  }");
  }

  @Override
  protected void emitBody()  {
    if (!emitBody) {
      unit.emitln(";");
    } else {
      final MethodBinding mBinding = getBinding();
      unit.emitln("  {");
      unit.emitln();
      if (isUnimplemented) {
        unit.emitln("    throw new " + getUnsupportedExceptionType() + "(\"Unimplemented\");");
      } else {
        emitPrologueOrEpilogue(prologue);
        emitPreCallSetup(mBinding);
        //emitReturnVariableSetup(binding, writer);
        emitReturnVariableSetupAndCall(mBinding);
      }
      unit.emitln("  }");
    }
    if( null != javaCallback && !isPrivateNativeMethod ) {
        final String capIfaceName = CodeGenUtils.capitalizeString( getInterfaceName() );
        final String lowIfaceName = CodeGenUtils.decapitalizeString( getInterfaceName() );
        final String lockInstanceName = lowIfaceName+"Lock";
        final String dataMapInstanceName = lowIfaceName+"DataMap";
        final String dataInstanceName = lowIfaceName+"Data";
        final boolean customKeyClass;
        final String KeyClassName;
        final boolean useDataMap;
        if( null != javaCallback.setFuncKeyClassName ) {
            customKeyClass = true;;
            KeyClassName = javaCallback.setFuncKeyClassName;
            useDataMap = true;
        } else {
            customKeyClass = false;
            KeyClassName = CodeGenUtils.capitalizeString(capIfaceName+"Key");
            useDataMap = javaCallback.setFuncKeyIndices.size() > 0;
        }
        final String DataClassName = CodeGenUtils.capitalizeString( javaCallback.cbFuncTypeName+"Data" );
        final String fqUsrParamClassName = cfg.packageName()+"."+cfg.className()+"."+DataClassName;
        unit.emitln();
        if( isInterface() ) {
            if( useDataMap ) {
                if( !customKeyClass && !javaCallback.keyClassEmitted ) {
                    emitJavaCallbackKeyClass(capIfaceName, KeyClassName);
                    unit.emitln();
                    javaCallback.keyClassEmitted = true;
                }
                emitJavaCallbackBirefAPIDoc("Returns ", "set of ", "", "for ");
                unit.emitln("  public Set<"+KeyClassName+"> get"+capIfaceName+"Keys();");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Returns ", "whether callback ", "if callback ", "is mapped for ");
                unit.emitln("  public boolean is"+capIfaceName+"Mapped("+KeyClassName+" key);");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Returns "+javaCallback.cbFuncTypeName+" callback ", "mapped to ", "", "for ");
                unit.emitln("  public "+javaCallback.cbFuncTypeName+" get"+capIfaceName+"("+KeyClassName+" key);");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Returns user-param ", "mapped to ", "", "for ");
                unit.emitln("  public Object get"+capIfaceName+"UserParam("+KeyClassName+" key);");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Releases all callback data ", "mapped via ", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public int releaseAll"+capIfaceName+"();");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Releases callback data ", "mapped to ", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public void release"+capIfaceName+"("+KeyClassName+" key);");
                unit.emitln();
            } else {
                emitJavaCallbackBirefAPIDoc("Returns ", "whether callback ", "if callback ", "is mapped for ");
                unit.emitln("  public boolean is"+capIfaceName+"Mapped();");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Returns "+javaCallback.cbFuncTypeName+" callback ", "mapped to ", "", "for ");
                unit.emitln("  public "+javaCallback.cbFuncTypeName+" get"+capIfaceName+"();");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Returns user-param ", "mapped to ", "", "for ");
                unit.emitln("  public Object get"+capIfaceName+"UserParam();");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Releases callback data ", "", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public void release"+capIfaceName+"();");
                unit.emitln();
            }
        } else {
            if( useDataMap ) {
                if( !customKeyClass && !javaCallback.keyClassEmitted ) {
                    emitJavaCallbackKeyClass(capIfaceName, KeyClassName);
                    unit.emitln();
                    javaCallback.keyClassEmitted = true;
                }
                emitJavaCallbackBirefAPIDoc("Returns ", "set of ", "", "for ");
                unit.emitln("  public final Set<"+KeyClassName+"> get"+capIfaceName+"Keys() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      return "+dataMapInstanceName+".keySet();");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Returns ", "whether callback ", "if callback ", "is mapped for ");
                unit.emitln("  public final boolean is"+capIfaceName+"Mapped("+KeyClassName+" key) {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      return null != "+dataMapInstanceName+".get(key);");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();

                emitJavaCallbackBirefAPIDoc("Returns "+javaCallback.cbFuncTypeName+" callback ", "mapped to ", "", "for ");
                unit.emitln("  public final "+javaCallback.cbFuncTypeName+" get"+capIfaceName+"("+KeyClassName+" key) {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      final "+DataClassName+" value = "+dataMapInstanceName+".get(key);");
                unit.emitln("      return null != value ? value.func : null;");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();

                emitJavaCallbackBirefAPIDoc("Returns user-param ", "mapped to ", "", "for ");
                unit.emitln("  public final Object get"+capIfaceName+"UserParam("+KeyClassName+" key) {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      final "+DataClassName+" value = "+dataMapInstanceName+".get(key);");
                unit.emitln("      return null != value ? value.param : null;");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Releases all callback data ", "mapped via ", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public final int releaseAll"+capIfaceName+"() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      final Set<"+KeyClassName+"> keySet = "+dataMapInstanceName+".keySet();");
                unit.emitln("      final "+KeyClassName+"[] keys = keySet.toArray(new "+KeyClassName+"[keySet.size()]);");
                unit.emitln("      for(int i=0; i<keys.length; ++i) {");
                unit.emitln("        final "+KeyClassName+" key = keys[i];");
                unit.emitln("          release"+capIfaceName+"(key);");
                unit.emitln("      }");
                unit.emitln("      return keys.length;");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();
                emitJavaCallbackBirefAPIDoc("Releases callback data ", "mapped to ", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public final void release"+capIfaceName+"("+KeyClassName+" key) {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      final "+DataClassName+" value = "+dataMapInstanceName+".remove(key);");
                if( DEBUG_JAVACALLBACK ) {
                    unit.emitln("      System.err.println(\"ZZZ Release \"+key+\" -> value.nativeParam 0x\"+Long.toHexString(null!=value?value.nativeParam:0));");
                }
                unit.emitln("      if( null != value ) {");
                unit.emitln("        release"+capIfaceName+"Impl(value.nativeParam);");
                unit.emitln("      }");
                unit.emitln("    }");
                unit.emitln("  }");
            } else {
                emitJavaCallbackBirefAPIDoc("Returns ", "whether callback ", "if callback ", "is mapped for ");
                unit.emitln("  public final boolean is"+capIfaceName+"Mapped() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      return null != "+dataInstanceName+";");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();

                emitJavaCallbackBirefAPIDoc("Returns "+javaCallback.cbFuncTypeName+" callback ", "mapped to ", "", "for ");
                unit.emitln("  public final "+javaCallback.cbFuncTypeName+" get"+capIfaceName+"() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      final "+DataClassName+" value = "+dataInstanceName+";");
                unit.emitln("      return null != value ? value.func : null;");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();

                emitJavaCallbackBirefAPIDoc("Returns user-param ", "mapped to ", "", "for ");
                unit.emitln("  public final Object get"+capIfaceName+"UserParam() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      final "+DataClassName+" value = "+dataInstanceName+";");
                unit.emitln("      return null != value ? value.param : null;");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();

                emitJavaCallbackBirefAPIDoc("Releases callback data ", "", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public final void release"+capIfaceName+"() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      final "+DataClassName+" value = "+dataInstanceName+";");
                unit.emitln("      "+dataInstanceName+" = null;");
                if( DEBUG_JAVACALLBACK ) {
                    unit.emitln("      System.err.println(\"ZZZ Release \"+key+\" -> value.nativeParam 0x\"+Long.toHexString(null!=value?value.nativeParam:0));");
                }
                unit.emitln("      if( null != value ) {");
                unit.emitln("        release"+capIfaceName+"Impl(value.nativeParam);");
                unit.emitln("      }");
                unit.emitln("    }");
                unit.emitln("  }");
            }
            unit.emitln("  private native void release"+capIfaceName+"Impl(long nativeUserParam);");
            unit.emitln();
            unit.emitln("  private final void add"+capIfaceName+"("+binding.getJavaSelectParameter(new StringBuilder(), javaCallback.setFuncKeyIndices, true).toString()+DataClassName+" value) {");
            if( useDataMap ) {
                unit.emitln("    final "+KeyClassName+" key = new "+KeyClassName+"("+binding.getJavaCallSelectArguments(new StringBuilder(), javaCallback.setFuncKeyIndices, false).toString()+");");
                unit.emitln("    final "+DataClassName+" old = "+dataMapInstanceName+".put(key, value);");
            } else {
                unit.emitln("    final "+DataClassName+" old = "+dataInstanceName+";");
                unit.emitln("    "+dataInstanceName+" = value;");
            }
            if( DEBUG_JAVACALLBACK ) {
                unit.emitln("    System.err.println(\"ZZZ Map \"+key+\" -> value.nativeParam 0x\"+Long.toHexString(null!=value?value.nativeParam:0));");
            }
            unit.emitln("    if( null != old ) {");
            unit.emitln("      release"+capIfaceName+"Impl(old.nativeParam);");
            unit.emitln("    }");
            unit.emitln("  }");
            if( !cfg.emittedJavaCallbackUserParamClasses.contains(fqUsrParamClassName) ) {
                emitJavaCallbackDataClass(capIfaceName, DataClassName);
                cfg.emittedJavaCallbackUserParamClasses.add(fqUsrParamClassName);
            }
            if( useDataMap ) {
                unit.emitln("  private final Map<"+KeyClassName+", "+DataClassName+"> "+dataMapInstanceName+" = new HashMap<"+KeyClassName+", "+DataClassName+">();");
            } else {
                unit.emitln("  private "+DataClassName+" "+dataInstanceName+" = null;");
            }
            unit.emitln("  private final Object "+lockInstanceName+" = new Object();");
            unit.emitln();
        }
    }
  }

  protected void emitPrologueOrEpilogue(final List<String> code) {
    if (code != null) {
      final String[] argumentNames = argumentNameArray();
      for (final String str : code) {
        try {
            final MessageFormat fmt = new MessageFormat(str);
            unit.emitln("    " + fmt.format(argumentNames));
        } catch (final IllegalArgumentException e) {
            // (Poorly) handle case where prologue / epilogue contains blocks of code with braces
            unit.emitln("    " + str);
        }
      }
    }
  }

  protected void emitPreCallSetup(final MethodBinding binding) {
    emitArrayLengthAndNIOBufferChecks(binding);
    emitCompoundArrayCopies(binding);
  }

  protected void emitArrayLengthAndNIOBufferChecks(final MethodBinding binding) {
      // Check lengths of any incoming arrays if necessary
      for (int i = 0; i < binding.getNumArguments(); i++) {
          final Type type = binding.getCArgumentType(i);
          final JavaType javaType = binding.getJavaArgumentType(i);
          if ( type.isArray() ) { // FIXME: Compound and Compound-Arrays
              // Simply add a range check upfront
              final ArrayType arrayType = type.asArray();
              if (javaType.isNIOBuffer()) {
                  unit.emitln("    if ( Buffers.remainingElem("+getArgumentName(i)+") < " + arrayType.getLength() + ")");
              } else {
                  unit.emitln("    if ( "+getArgumentName(i)+".length < " + arrayType.getLength() + ")");
              }
              unit.emit("      throw new " + getRuntimeExceptionType() +
                      "(\"Array \\\"" + getArgumentName(i) +
                      "\\\" length (\" + ");
              if (javaType.isNIOBuffer()) {
                  unit.emit("Buffers.remainingElem("+getArgumentName(i)+")");
              } else {
                  unit.emit(getArgumentName(i)+".length");
              }
              unit.emitln("+ \") was less than the required (" + arrayType.getLength() + ")\");");
          }
          if (javaType.isNIOBuffer()) {
              if (useNIODirectOnly) {
                  unit.emitln("    if (!Buffers.isDirect(" + getArgumentName(i) + "))");
                  unit.emitln("      throw new " + getRuntimeExceptionType() + "(\"Argument \\\"" +
                          getArgumentName(i) + "\\\" is not a direct buffer\");");
              } else {
                  unit.emitln("    final boolean " + isNIOArgName(i) + " = Buffers.isDirect(" + getArgumentName(i) + ");");
              }
          } else if (javaType.isNIOBufferArray()) {
              // All buffers passed down in an array of NIO buffers must be direct
              final String argName = getArgumentName(i);
              final String arrayName = byteOffsetArrayArgName(i);
              unit.emitln("    final int[] " + arrayName + " = new int[" + argName + ".length];");
              // Check direct buffer properties of all buffers within
              unit.emitln("    if (" + argName + " != null) {");
              unit.emitln("      for (int _ctr = 0; _ctr < " + argName + ".length; _ctr++) {");
              unit.emitln("        if (!Buffers.isDirect(" + argName + "[_ctr])) {");
              unit.emitln("          throw new " + getRuntimeExceptionType() +
                      "(\"Element \" + _ctr + \" of argument \\\"" +
                      getArgumentName(i) + "\\\" was not a direct buffer\");");
              unit.emitln("        }");
              // get the Buffer Array offset values and save them into another array to send down to JNI
              unit.emit  ("        " + arrayName + "[_ctr] = Buffers.getDirectBufferByteOffset(");
              unit.emitln(argName + "[_ctr]);");
              unit.emitln("      }");
              unit.emitln("    }");
          } else if (javaType.isPrimitiveArray()) {
              final String argName = getArgumentName(i);
              final String offsetArg = offsetArgName(i);
              unit.emitln("    if(" + argName + " != null && " + argName + ".length <= " + offsetArg + ")");
              unit.emit  ("      throw new " + getRuntimeExceptionType());
              unit.emitln("(\"array offset argument \\\"" + offsetArg + "\\\" (\" + " + offsetArg +
                      " + \") equals or exceeds array length (\" + " + argName + ".length + \")\");");
          }
      }
  }

  protected void emitCompoundArrayCopies(final MethodBinding binding) {
    // If the method binding uses outgoing arrays of compound type
    // wrappers, we need to generate a temporary copy of this array
    // into a ByteBuffer[] for processing by the native code
    if (binding.signatureUsesArraysOfCompoundTypeWrappers()) {
      for (int i = 0; i < binding.getNumArguments(); i++) {
        final JavaType javaType = binding.getJavaArgumentType(i);
        if (javaType.isArrayOfCompoundTypeWrappers()) {
          final String argName = getArgumentName(i);
          final String tempArrayName = argName + COMPOUND_ARRAY_SUFFIX;
          unit.emitln("    final ByteBuffer[] " + tempArrayName + " = new ByteBuffer[" + argName + ".length];");
          unit.emitln("    for (int _ctr = 0; _ctr < + " + argName + ".length; _ctr++) {");
          unit.emitln("      " + javaType.getName() + " _tmp = " + argName + "[_ctr];");
          unit.emitln("      " + tempArrayName + "[_ctr] = ((_tmp == null) ? null : _tmp.getBuffer());");
          unit.emitln("    }");
        }
      }
    }
  }

  protected void emitCall(final MethodBinding binding) {
    unit.emit(getNativeImplMethodName());
    unit.emit("(");
    emitCallArguments(binding);
    unit.emit(");");
  }


  protected void emitReturnVariableSetupAndCall(final MethodBinding binding) {
    final JavaType returnType = binding.getJavaReturnType();
    boolean needsResultAssignment = false;
    final String capIfaceName = CodeGenUtils.capitalizeString( getInterfaceName() );

    if( null != javaCallback ) {
        final String lowIfaceName = CodeGenUtils.decapitalizeString( getInterfaceName() );
        final String lockInstanceName = lowIfaceName+"Lock";
        unit.emitln("    synchronized( "+lockInstanceName+" ) {");
        unit.emitln("      final long[] nativeUserParam = { 0 };");
        unit.emitln();
    }
    if (!returnType.isVoid()) {
      unit.emit("      ");
      if (returnType.isCompoundTypeWrapper() ||
          returnType.isNIOBuffer()) {
        unit.emitln("final ByteBuffer _res;");
        needsResultAssignment = true;
      } else if (returnType.isArrayOfCompoundTypeWrappers()) {
        unit.emitln("final ByteBuffer[] _res;");
        needsResultAssignment = true;
      } else if (((epilogue != null) && (epilogue.size() > 0)) ||
                 binding.signatureUsesArraysOfCompoundTypeWrappers()) {
        unit.emit("final ");
        emitReturnType();
        unit.emitln(" _res;");
        needsResultAssignment = true;
      }
    }

    if (needsResultAssignment) {
      unit.emit("      _res = ");
    } else {
      unit.emit("      ");
      if (!returnType.isVoid()) {
        unit.emit("return ");
      }
    }

    emitCall(binding);
    unit.emitln();

    if( null != javaCallback ) {
        final String funcArgName = binding.getArgumentName(javaCallback.setFuncCBParamIdx);
        final String userParamArgName = binding.getArgumentName(javaCallback.setFuncUserParamIdx);
        final String DataClassName = CodeGenUtils.capitalizeString( javaCallback.cbFuncTypeName+"Data" );
        final String KeyClassName;
        final boolean useDataMap;
        if( null != javaCallback.setFuncKeyClassName ) {
            KeyClassName = javaCallback.setFuncKeyClassName;
            useDataMap = true;
        } else {
            KeyClassName = CodeGenUtils.capitalizeString(capIfaceName+"Key");
            useDataMap = javaCallback.setFuncKeyIndices.size() > 0;
        }
        if( DEBUG_JAVACALLBACK ) {
            unit.emitln("    System.err.println(\"ZZZ returned nativeUserParam 0x\"+Long.toHexString(nativeUserParam[0]));");
        }
        unit.emitln();
        unit.emitln("      if( 0 != nativeUserParam[0] ) {");
        unit.emitln("          // callback registrated -> add will release a previously mapped instance ");
        unit.emitln("          add"+capIfaceName+"("+binding.getJavaCallSelectArguments(new StringBuilder(), javaCallback.setFuncKeyIndices, true).toString()+
                                                   "new "+DataClassName+"("+funcArgName+", "+userParamArgName+", nativeUserParam[0]));");
        unit.emitln("      } else {");
        unit.emitln("          // callback released (null func) -> release a previously mapped instance ");
        if( useDataMap ) {
            unit.emitln("          release"+capIfaceName+"( new "+KeyClassName+"( "+binding.getJavaCallSelectArguments(new StringBuilder(), javaCallback.setFuncKeyIndices, false).toString()+" ) );");
        } else {
            unit.emitln("          release"+capIfaceName+"();");
        }
        unit.emitln("      }");
        unit.emitln("    } // synchronized ");
    }

    emitPostCallCleanup(binding);
    emitPrologueOrEpilogue(epilogue);
    if (needsResultAssignment) {
      emitCallResultReturn(binding);
    }
  }

  protected int emitCallArguments(final MethodBinding binding) {
    boolean needComma = false;
    int numArgsEmitted = 0;

    if( binding.isReturnCompoundByValue() ) {
      unit.emit("com.jogamp.common.nio.Buffers.class");
      needComma = true;
      ++numArgsEmitted;
    }
    if (binding.hasContainingType()) {
      // Emit this pointer
      assert(binding.getContainingType().isCompoundTypeWrapper());
      if (needComma) {
        unit.emit(", ");
      }
      unit.emit("getBuffer()");
      needComma = true;
      ++numArgsEmitted;
    }
    for (int i = 0; i < binding.getNumArguments(); i++) {
      final JavaType type = binding.getJavaArgumentType(i);
      if (type.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        // Don't need to expose these at the Java level
        continue;
      }

      if (type.isVoid()) {
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        assert(binding.getNumArguments() == 1);
        continue;
      }

      if (needComma) {
        unit.emit(", ");
      }

      if (type.isCompoundTypeWrapper()) {
        unit.emit("((");
      }

      if (type.isNIOBuffer()) {
          if(type.isNIOPointerBuffer()) {
              if (useNIODirectOnly) {
                  unit.emit( getArgumentName(i)+ " != null ? " + getArgumentName(i) + ".getBuffer() : null");
              } else {
                  unit.emit( isNIOArgName(i) + " ? ( " + getArgumentName(i)+ " != null ? " + getArgumentName(i) + ".getBuffer() : null )");
                  unit.emit( " : Buffers.getArray(" + getArgumentName(i) + ")" );
              }
          } else {
              if (useNIODirectOnly) {
                  unit.emit( getArgumentName(i) );
              } else {
                  unit.emit( isNIOArgName(i) + " ? " + getArgumentName(i) + " : Buffers.getArray(" + getArgumentName(i) + ")" );
              }
          }
      } else if (type.isArrayOfCompoundTypeWrappers()) {
          unit.emit(getArgumentName(i) + COMPOUND_ARRAY_SUFFIX);
      } else {
          unit.emit(getArgumentName(i));
      }

      if (type.isCompoundTypeWrapper()) {
        unit.emit(" == null) ? null : ");
        unit.emit(getArgumentName(i));
        unit.emit(".getBuffer())");
      }

      if (type.isNIOBuffer()) {
        if (useNIODirectOnly) {
          unit.emit( ", Buffers.getDirectBufferByteOffset(" + getArgumentName(i) + ")");
        } else {
          unit.emit( ", " + isNIOArgName(i) + " ? Buffers.getDirectBufferByteOffset(" + getArgumentName(i) + ")");
          unit.emit(        " : Buffers.getIndirectBufferByteOffset(" + getArgumentName(i) + ")");
        }
      } else if (type.isNIOBufferArray()) {
        unit.emit(", " + byteOffsetArrayArgName(i));
      } else if (type.isPrimitiveArray()) {
        if(type.isFloatArray()) {
          unit.emit(", Buffers.SIZEOF_FLOAT * ");
        } else if(type.isDoubleArray()) {
          unit.emit(", Buffers.SIZEOF_DOUBLE * ");
        } else if(type.isByteArray()) {
          unit.emit(", ");
        } else if(type.isLongArray()) {
          unit.emit(", Buffers.SIZEOF_LONG * ");
        } else if(type.isShortArray()) {
          unit.emit(", Buffers.SIZEOF_SHORT * ");
        } else if(type.isIntArray()) {
          unit.emit(", Buffers.SIZEOF_INT * ");
        } else {
          throw new GlueGenException("Unsupported type for calculating array offset argument for " +
                                     getArgumentName(i) +
                                     " -- error occurred while processing Java glue code for " + getCSymbol().getAliasedString(),
                                     getCSymbol().getASTLocusTag());
        }
        unit.emit(offsetArgName(i));
      }

      if (type.isNIOBuffer()) {
        if (!useNIODirectOnly) {
            unit.emit( ", " + isNIOArgName(i) );
        }
      } else if (type.isPrimitiveArray()) {
        if (useNIOOnly) {
            throw new GlueGenException("NIO[Direct]Only "+binding+" is set, but "+getArgumentName(i)+" is a primitive array",
                                       getCSymbol().getASTLocusTag());
        }
        unit.emit( ", false");
      }

      needComma = true;
      ++numArgsEmitted;
    }
    if( null != javaCallback ) {
        final String lowIfaceName = CodeGenUtils.decapitalizeString( getInterfaceName() );
        final String lockInstanceName = lowIfaceName+"Lock";
        if (needComma) {
            unit.emit(", ");
        }
        unit.emit("\"" + javaCallback.cbMethodSignature + "\", "+lockInstanceName+", nativeUserParam");
        ++numArgsEmitted;
    }
    return numArgsEmitted;
  }

  protected void emitPostCallCleanup(final MethodBinding binding) {
    if (binding.signatureUsesArraysOfCompoundTypeWrappers()) {
      // For each such array, we need to take the ByteBuffer[] that
      // came back from the C method invocation and wrap the
      // ByteBuffers back into the wrapper types
      for (int i = 0; i < binding.getNumArguments(); i++) {
        final JavaType javaArgType = binding.getJavaArgumentType(i);
        if ( javaArgType.isArrayOfCompoundTypeWrappers() && !javaArgType.getElementCType().isBaseTypeConst() ) {
          final String argName = binding.getArgumentName(i);
          unit.emitln("    for (int _ctr = 0; _ctr < " + argName + ".length; _ctr++) {");
          unit.emitln("      if ((" + argName + "[_ctr] == null && " + argName + COMPOUND_ARRAY_SUFFIX + "[_ctr] == null) ||");
          unit.emitln("          (" + argName + "[_ctr] != null && " + argName + "[_ctr].getBuffer() == " + argName + COMPOUND_ARRAY_SUFFIX + "[_ctr])) {");
          unit.emitln("        // No copy back needed");
          unit.emitln("      } else {");
          unit.emitln("        if (" + argName + COMPOUND_ARRAY_SUFFIX + "[_ctr] == null) {");
          unit.emitln("          " + argName + "[_ctr] = null;");
          unit.emitln("        } else {");
          unit.emitln("          " + argName + "[_ctr] = " + javaArgType.getName() + ".create(" + argName + COMPOUND_ARRAY_SUFFIX + "[_ctr]);");
          unit.emitln("        }");
          unit.emitln("      }");
          unit.emitln("    }");
        }
      }
    }
  }

  protected void emitCallResultReturn(final MethodBinding binding) {
    final JavaType returnType = binding.getJavaReturnType();

    if (returnType.isCompoundTypeWrapper()) {
      // Details are handled in JavaEmitter's struct handling!
      unit.emitln("    if (_res == null) return null;");
      unit.emitln("    return " + returnType.getName() + ".create(Buffers.nativeOrder(_res));");
    } else if (returnType.isNIOBuffer()) {
      unit.emitln("    if (_res == null) return null;");
      unit.emitln("    Buffers.nativeOrder(_res);");
      if (!returnType.isNIOByteBuffer()) {
        // See whether we have to expand pointers to longs
        if (getBinding().getCReturnType().pointerDepth() >= 2) {
          if (returnType.isNIOPointerBuffer()) {
              unit.emitln("    return PointerBuffer.wrap(_res);");
          } else if (returnType.isNIOLongBuffer()) {
              unit.emitln("    return _res.asLongBuffer();");
          } else {
            throw new GlueGenException("While emitting glue code for " + getCSymbol().getAliasedString() +
                                       ": can not legally make pointers opaque to anything but PointerBuffer or LongBuffer/long",
                                       getCSymbol().getASTLocusTag());
          }
        } else if (getBinding().getCReturnType().pointerDepth() == 1 && returnType.isNIOLongBuffer()) {
          unit.emitln("    return _res.asLongBuffer();");
        } else {
          final String returnTypeName = returnType.getName().substring("java.nio.".length());
          unit.emitln("    return _res.as" + returnTypeName + "();");
        }
      } else {
        unit.emitln("    return _res;");
      }
    } else if (returnType.isArrayOfCompoundTypeWrappers()) {
      unit.emitln("    if (_res == null) return null;");
      unit.emitln("    final " + getReturnTypeString(false) + " _retarray = new " + getReturnTypeString(true) + "[_res.length];");
      unit.emitln("    for (int _count = 0; _count < _res.length; _count++) {");
      unit.emitln("      _retarray[_count] = " + getReturnTypeString(true) + ".create(_res[_count]);");
      unit.emitln("    }");
      unit.emitln("    return _retarray;");
    } else {
      // Assume it's a primitive type or other type we don't have to
      // do any conversion on
      unit.emitln("    return _res;");
    }
  }

  protected String[] argumentNameArray() {
    final String[] argumentNames = new String[binding.getNumArguments()];
    for (int i = 0; i < binding.getNumArguments(); i++) {
      argumentNames[i] = getArgumentName(i);
      if (binding.getJavaArgumentType(i).isPrimitiveArray()) {
        // Add on _offset argument in comma-separated expression
        argumentNames[i] = argumentNames[i] + ", " + offsetArgName(i);
      }
    }
    return argumentNames;
  }

  public static String javaThisArgumentName() {
    return "jthis0";
  }

  @Override
  protected String getCommentStartString() { return "/** "; }

  @Override
  protected String getCommentEndString() {
      final StringBuilder sb = new StringBuilder();
      final String methodName = binding.getName();
      final List<String> methodDocs = cfg.javadocForMethod(methodName);
      for (final Iterator<String> iter = methodDocs.iterator(); iter.hasNext(); ) {
        sb.append(JavaConfiguration.NEWLINE).append(getBaseIndentString()).append(iter.next());
      }
      if( methodDocs.size() > 0 ) {
          sb.append(JavaConfiguration.NEWLINE).append(getBaseIndentString());
      }
      sb.append(" */");
      return sb.toString();
  }

  @Override
  protected String getBaseIndentString() { return "  "; }

  /**
   * Class that emits a generic comment for JavaMethodBindingEmitters; the comment
   * includes the C signature of the native method that is being bound by the
   * emitter java method.
   */
  protected class DefaultCommentEmitter implements CommentEmitter {
    protected void emitAliasedDocNamesComment(final AliasedSymbol sym, final PrintWriter writer) {
        writer.print(emitAliasedDocNamesComment(sym, new StringBuilder()).toString());
    }
    protected StringBuilder emitAliasedDocNamesComment(final AliasedSymbol sym, final StringBuilder sb) {
      final Set<String> aliases = cfg.getAliasedDocNames(sym);
      if (aliases != null && aliases.size() > 0 ) {
          int i=0;
          sb.append("Alias for: <code>");
          for (final String alias : aliases) {
              if(0 < i) {
                  sb.append("</code>, <code>");
              }
              sb.append(alias);
              i++;
          }
          sb.append("</code>");
      }
      return sb;
    }

    @Override
    public void emit(final FunctionEmitter emitter, final PrintWriter writer) {
      emitBeginning(emitter, writer);
      emitBindingCSignature(((JavaMethodBindingEmitter)emitter).getBinding(), writer);
      final String arrayLengthExpr = getReturnedArrayLengthComment();
      if( null != arrayLengthExpr ) {
          writer.print(", covering an array of length <code>"+arrayLengthExpr+"</code>");
      }
      emitEnding(emitter, writer);
    }
    protected void emitBeginning(final FunctionEmitter emitter, final PrintWriter writer) {
      writer.print("Entry point to C language function: ");
    }
    protected void emitBindingCSignature(final MethodBinding binding, final PrintWriter writer) {
      final FunctionSymbol funcSym = binding.getCSymbol();
      writer.print("<code>");
      writer.print(funcSym.toString(tagNativeBinding));
      writer.print("</code><br>");
      emitAliasedDocNamesComment(funcSym, writer);
    }
    protected void emitEnding(final FunctionEmitter emitter, final PrintWriter writer) {
      // If argument type is a named enum, then emit a comment detailing the
      // acceptable values of that enum.
      // If we're emitting a direct buffer variant only, then declare
      // that the NIO buffer arguments must be direct.
      final MethodBinding binding = ((JavaMethodBindingEmitter)emitter).getBinding();
      for (int i = 0; i < binding.getNumArguments(); i++) {
        final Type type = binding.getCArgumentType(i);
        final JavaType javaType = binding.getJavaArgumentType(i);
        // don't emit param comments for anonymous enums, since we can't
        // distinguish between the values found within multiple anonymous
        // enums in the same C translation unit.
        if (type.isEnum() && !HeaderParser.ANONYMOUS_ENUM_NAME.equals(type.getName())) {
          final EnumType enumType = (EnumType)type;
          writer.println();
          writer.print(emitter.getBaseIndentString());
          writer.print("    ");
          writer.print("@param ");
          writer.print(getArgumentName(i));
          writer.print(" valid values are: <code>");
          for (int j = 0; j < enumType.getNumEnumerates(); ++j) {
            if (j>0) writer.print(", ");
            writer.print(enumType.getEnum(j).getName());
          }
          writer.println("</code>");
        } else if (javaType.isNIOBuffer()) {
          writer.println();
          writer.print(emitter.getBaseIndentString());
          writer.print("    ");
          writer.print("@param ");
          writer.print(getArgumentName(i));
          if (useNIODirectOnly) {
              writer.print(" a direct only {@link " + javaType.getName() + "}");
          } else {
              writer.print(" a direct or array-backed {@link " + javaType.getName() + "}");
          }
        }
      }
    }
  }

    protected class InterfaceCommentEmitter extends JavaMethodBindingEmitter.DefaultCommentEmitter {

        @Override
        protected void emitBeginning(final FunctionEmitter emitter, final PrintWriter writer) {
            writer.print("Interface to C language function: <br> ");
        }
    }
}

