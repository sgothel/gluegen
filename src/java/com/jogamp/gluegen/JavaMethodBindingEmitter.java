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

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

import com.jogamp.gluegen.cgram.types.*;
import com.jogamp.gluegen.cgram.*;

/**
 * An emitter that emits only the interface for a Java<->C JNI binding.
 */
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

  // Exception type raised in the generated code if runtime checks fail
  private String runtimeExceptionType;
  private String unsupportedExceptionType;

  protected boolean emitBody;
  protected boolean eraseBufferAndArrayTypes;
  protected boolean directNIOOnly;
  protected boolean forImplementingMethodCall;
  protected boolean forDirectBufferImplementation;
  protected boolean forIndirectBufferAndArrayImplementation;
  protected boolean isUnimplemented;
  protected boolean tagNativeBinding;

  protected MethodBinding binding;

  // Manually-specified prologue and epilogue code
  protected List<String> prologue;
  protected List<String> epilogue;

  // A non-null value indicates that rather than returning a compound
  // type accessor we are returning an array of such accessors; this
  // expression is a MessageFormat string taking the names of the
  // incoming Java arguments as parameters and computing as an int the
  // number of elements of the returned array.
  private String returnedArrayLengthExpression;

  // A suffix used to create a temporary outgoing array of Buffers to
  // represent an array of compound type wrappers
  private static final String COMPOUND_ARRAY_SUFFIX = "_buf_array_copy";

  // Only present to provide more clear comments
  private JavaConfiguration cfg;

  public JavaMethodBindingEmitter(MethodBinding binding,
                                  PrintWriter output,
                                  String runtimeExceptionType,
                                  String unsupportedExceptionType,
                                  boolean emitBody,
                                  boolean tagNativeBinding,
                                  boolean eraseBufferAndArrayTypes,
                                  boolean directNIOOnly,
                                  boolean forImplementingMethodCall,
                                  boolean forDirectBufferImplementation,
                                  boolean forIndirectBufferAndArrayImplementation,
                                  boolean isUnimplemented,
                                  boolean isInterface,
                                  JavaConfiguration configuration) {
    super(output, isInterface);
    this.binding = binding;
    this.runtimeExceptionType = runtimeExceptionType;
    this.unsupportedExceptionType = unsupportedExceptionType;
    this.emitBody = emitBody;
    this.tagNativeBinding = tagNativeBinding;
    this.eraseBufferAndArrayTypes = eraseBufferAndArrayTypes;
    this.directNIOOnly = directNIOOnly;
    this.forImplementingMethodCall = forImplementingMethodCall;
    this.forDirectBufferImplementation = forDirectBufferImplementation;
    this.forIndirectBufferAndArrayImplementation = forIndirectBufferAndArrayImplementation;
    this.isUnimplemented = isUnimplemented;
    if (forImplementingMethodCall) {
      setCommentEmitter(defaultJavaCommentEmitter);
    } else {
      setCommentEmitter(defaultInterfaceCommentEmitter);
    }
    cfg = configuration;
  }

  public JavaMethodBindingEmitter(JavaMethodBindingEmitter arg) {
    super(arg);
    binding                       = arg.binding;
    runtimeExceptionType          = arg.runtimeExceptionType;
    unsupportedExceptionType      = arg.unsupportedExceptionType;
    emitBody                      = arg.emitBody;
    tagNativeBinding              = arg.tagNativeBinding;
    eraseBufferAndArrayTypes      = arg.eraseBufferAndArrayTypes;
    directNIOOnly                 = arg.directNIOOnly;
    forImplementingMethodCall     = arg.forImplementingMethodCall;
    forDirectBufferImplementation = arg.forDirectBufferImplementation;
    forIndirectBufferAndArrayImplementation = arg.forIndirectBufferAndArrayImplementation;
    isUnimplemented               = arg.isUnimplemented;
    prologue                      = arg.prologue;
    epilogue                      = arg.epilogue;
    returnedArrayLengthExpression = arg.returnedArrayLengthExpression;
    cfg                           = arg.cfg;
  }

  public final MethodBinding getBinding() { return binding; }

  public boolean isForImplementingMethodCall() { return forImplementingMethodCall; }
  public boolean isForDirectBufferImplementation() { return forDirectBufferImplementation; }
  public boolean isForIndirectBufferAndArrayImplementation() { return forIndirectBufferAndArrayImplementation; }

  public String getName() {
    return binding.getName();
  }

  protected String getArgumentName(int i) {
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
  public void setReturnedArrayLengthExpression(String expr) {
    returnedArrayLengthExpression = expr;
  }

  /** Sets the manually-generated prologue code for this emitter. */
  public void setPrologue(List<String> prologue) {
    this.prologue = prologue;
  }

  /** Sets the manually-generated epilogue code for this emitter. */
  public void setEpilogue(List<String> epilogue) {
    this.epilogue = epilogue;
  }

  /** Indicates whether this emitter will print only a signature, or
      whether it will emit Java code for the body of the method as
      well. */
  public boolean signatureOnly() {
    return !emitBody;
  }

  /** Accessor for subclasses. */
  public void setEmitBody(boolean emitBody) {
    this.emitBody = emitBody;
  }

  /** Accessor for subclasses. */
  public void setEraseBufferAndArrayTypes(boolean erase) {
    this.eraseBufferAndArrayTypes = erase;
  }

  /** Accessor for subclasses. */
  public void setForImplementingMethodCall(boolean impl) {
    this.forImplementingMethodCall = impl;
  }

  /** Accessor for subclasses. */
  public void setForDirectBufferImplementation(boolean direct) {
    this.forDirectBufferImplementation = direct;
  }

  /** Accessor for subclasses. */
  public void setForIndirectBufferAndArrayImplementation(boolean indirect) {
    this.forIndirectBufferAndArrayImplementation = indirect;
  }

  protected void emitReturnType(PrintWriter writer)  {
    writer.print(getReturnTypeString(false));
  }

  protected String erasedTypeString(JavaType type, boolean skipBuffers) {
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
    int index = name.lastIndexOf('.')+1; // always >= 0
    name = name.substring(index);

    if (type.isArrayOfCompoundTypeWrappers()) {
      // We don't want to bake the array specification into the type name
      return name + "[]";
    }
    return name;
  }

  protected String getReturnTypeString(boolean skipArray) {
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

  protected void emitName(PrintWriter writer)  {
    if (forImplementingMethodCall) {
      writer.print(getImplMethodName());
    } else {
      writer.print(getName());
    }
  }

  protected int emitArguments(PrintWriter writer) {
    boolean needComma = false;
    int numEmitted = 0;

    if (forImplementingMethodCall  && binding.hasContainingType()) {
      // Always emit outgoing "this" argument
      writer.print("ByteBuffer ");
      writer.print(javaThisArgumentName());
      ++numEmitted;
      needComma = true;
    }

    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
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
        writer.print(", ");
      }

      writer.print(erasedTypeString(type, false));
      writer.print(" ");
      writer.print(getArgumentName(i));

      ++numEmitted;
      needComma = true;

      // Add Buffer and array index offset arguments after each associated argument
      if (forDirectBufferImplementation || forIndirectBufferAndArrayImplementation) {
        if (type.isNIOBuffer()) {
          writer.print(", int " + byteOffsetArgName(i));
          if(!directNIOOnly) {
              writer.print(", boolean " + isNIOArgName(i));
          }
        } else if (type.isNIOBufferArray()) {
          writer.print(", int[] " +  byteOffsetArrayArgName(i));
        }
      }

      // Add offset argument after each primitive array
      if (type.isPrimitiveArray()) {
        if(directNIOOnly) {
            throw new RuntimeException("NIODirectOnly "+binding+" is set, but "+getArgumentName(i)+" is a primitive array");
        }
        writer.print(", int " + offsetArgName(i));
      }
    }
    return numEmitted;
  }


  protected String getImplMethodName() {
    return binding.getName() + ( directNIOOnly ? "0" : "1" );
  }

  protected String byteOffsetArgName(int i) {
    return byteOffsetArgName(getArgumentName(i));
  }

  protected String byteOffsetArgName(String s) {
    return s + "_byte_offset";
  }

  protected String isNIOArgName(int i) {
    return isNIOArgName(binding.getArgumentName(i));
  }

  protected String isNIOArgName(String s) {
    return s + "_is_direct";
  }

  protected String byteOffsetArrayArgName(int i) {
    return getArgumentName(i) + "_byte_offset_array";
  }

  protected String offsetArgName(int i) {
    return getArgumentName(i) + "_offset";
  }

  protected void emitBody(PrintWriter writer)  {
    if (!emitBody) {
      writer.println(';');
    } else {
      MethodBinding mBinding = getBinding();
      writer.println("  {");
      writer.println();
      if (isUnimplemented) {
        writer.println("    throw new " + getUnsupportedExceptionType() + "(\"Unimplemented\");");
      } else {
        emitPrologueOrEpilogue(prologue, writer);
        emitPreCallSetup(mBinding, writer);
        //emitReturnVariableSetup(binding, writer);
        emitReturnVariableSetupAndCall(mBinding, writer);
      }
      writer.println("  }");
    }
  }

  protected void emitPrologueOrEpilogue(List<String> code, PrintWriter writer) {
    if (code != null) {
      String[] argumentNames = argumentNameArray();
      for (String str : code) {
        try {
            MessageFormat fmt = new MessageFormat(str);
            writer.println("    " + fmt.format(argumentNames));
        } catch (IllegalArgumentException e) {
            // (Poorly) handle case where prologue / epilogue contains blocks of code with braces
            writer.println("    " + str);
        }
      }
    }
  }

  protected void emitPreCallSetup(MethodBinding binding, PrintWriter writer) {
    emitArrayLengthAndNIOBufferChecks(binding, writer);
    emitCompoundArrayCopies(binding, writer);
  }

  protected void emitArrayLengthAndNIOBufferChecks(MethodBinding binding, PrintWriter writer) {

    // Check lengths of any incoming arrays if necessary
    for (int i = 0; i < binding.getNumArguments(); i++) {
      Type type = binding.getCArgumentType(i);
      if (type.isArray()) {
        ArrayType arrayType = type.asArray();
        writer.println("    if (" + getArgumentName(i) + ".length < " +
                       arrayType.getLength() + ")");
        writer.println("      throw new " + getRuntimeExceptionType() +
                       "(\"Length of array \\\"" + getArgumentName(i) +
                       "\\\" was less than the required " + arrayType.getLength() + "\");");
        // FIXME: What is this ??? Until resolved - throw an exception !
        throw new RuntimeException("????? "+binding+": binding.getCArgumentType("+i+").isArray(): "+type);
      } else {
        JavaType javaType = binding.getJavaArgumentType(i);
        if (javaType.isNIOBuffer()) {
          if (directNIOOnly) {
            writer.println("    if (!Buffers.isDirect(" + getArgumentName(i) + "))");
            writer.println("      throw new " + getRuntimeExceptionType() + "(\"Argument \\\"" +
                           getArgumentName(i) + "\\\" was not a direct buffer\");");
          } else {
            writer.print("    boolean " + isNIOArgName(i) + " = ");
            writer.println(getArgumentName(i) + " != null && Buffers.isDirect(" + getArgumentName(i) + ");");
          }
        } else if (javaType.isNIOBufferArray()) {
          // All buffers passed down in an array of NIO buffers must be direct
          String argName = getArgumentName(i);
          String arrayName = byteOffsetArrayArgName(i);
          writer.println("    int[] " + arrayName + " = new int[" + argName + ".length];");
          // Check direct buffer properties of all buffers within
          writer.println("    if (" + argName + " != null) {");
          writer.println("      for (int _ctr = 0; _ctr < " + argName + ".length; _ctr++) {");
          writer.println("        if (!Buffers.isDirect(" + argName + "[_ctr])) {");
          writer.println("          throw new " + getRuntimeExceptionType() +
                         "(\"Element \" + _ctr + \" of argument \\\"" +
                         getArgumentName(i) + "\\\" was not a direct buffer\");");
          writer.println("        }");
          // get the Buffer Array offset values and save them into another array to send down to JNI
          writer.print  ("        " + arrayName + "[_ctr] = Buffers.getDirectBufferByteOffset(");
          writer.println(argName + "[_ctr]);");
          writer.println("      }");
          writer.println("    }");
        } else if (javaType.isPrimitiveArray()) {
          String argName = getArgumentName(i);
          String offsetArg = offsetArgName(i);
          writer.println("    if(" + argName + " != null && " + argName + ".length <= " + offsetArg + ")");
          writer.print  ("      throw new " + getRuntimeExceptionType());
          writer.println("(\"array offset argument \\\"" + offsetArg + "\\\" (\" + " + offsetArg +
                         " + \") equals or exceeds array length (\" + " + argName + ".length + \")\");");
        }
      }
    }
  }

  protected void emitCompoundArrayCopies(MethodBinding binding, PrintWriter writer) {
    // If the method binding uses outgoing arrays of compound type
    // wrappers, we need to generate a temporary copy of this array
    // into a ByteBuffer[] for processing by the native code
    if (binding.signatureUsesArraysOfCompoundTypeWrappers()) {
      for (int i = 0; i < binding.getNumArguments(); i++) {
        JavaType javaType = binding.getJavaArgumentType(i);
        if (javaType.isArrayOfCompoundTypeWrappers()) {
          String argName = getArgumentName(i);
          String tempArrayName = argName + COMPOUND_ARRAY_SUFFIX;
          writer.println("    ByteBuffer[] " + tempArrayName + " = new ByteBuffer[" + argName + ".length];");
          writer.println("    for (int _ctr = 0; _ctr < + " + argName + ".length; _ctr++) {");
          writer.println("      " + javaType.getName() + " _tmp = " + argName + "[_ctr];");
          writer.println("      " + tempArrayName + "[_ctr] = ((_tmp == null) ? null : _tmp.getBuffer());");
          writer.println("    }");
        }
      }
    }
  }

  protected void emitCall(MethodBinding binding, PrintWriter writer) {
    writer.print(getImplMethodName());
    writer.print("(");
    emitCallArguments(binding, writer);
    writer.print(");");
  }


  protected void emitReturnVariableSetupAndCall(MethodBinding binding, PrintWriter writer) {
    writer.print("    ");
    JavaType returnType = binding.getJavaReturnType();
    boolean needsResultAssignment = false;

    if (!returnType.isVoid()) {
      if (returnType.isCompoundTypeWrapper() ||
          returnType.isNIOBuffer()) {
        writer.println("ByteBuffer _res;");
        needsResultAssignment = true;
      } else if (returnType.isArrayOfCompoundTypeWrappers()) {
        writer.println("ByteBuffer[] _res;");
        needsResultAssignment = true;
      } else if (((epilogue != null) && (epilogue.size() > 0)) ||
                 binding.signatureUsesArraysOfCompoundTypeWrappers()) {
        emitReturnType(writer);
        writer.println(" _res;");
        needsResultAssignment = true;
      }
    }

    if (needsResultAssignment) {
      writer.print("    _res = ");
    } else {
      writer.print("    ");
      if (!returnType.isVoid()) {
        writer.print("return ");
      }
    }

    emitCall(binding, writer);
    writer.println();

    emitPostCallCleanup(binding, writer);
    emitPrologueOrEpilogue(epilogue, writer);
    if (needsResultAssignment) {
      emitCallResultReturn(binding, writer);
    }
  }

  protected int emitCallArguments(MethodBinding binding, PrintWriter writer) {
    boolean needComma = false;
    int numArgsEmitted = 0;

    if (binding.hasContainingType()) {
      // Emit this pointer
      assert(binding.getContainingType().isCompoundTypeWrapper());
      writer.print("getBuffer()");
      needComma = true;
      ++numArgsEmitted;
    }
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
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
        writer.print(", ");
      }

      if (type.isCompoundTypeWrapper()) {
        writer.print("((");
      }

      if (type.isNIOBuffer()) {
          if(type.isNIOInt64Buffer() || type.isNIOPointerBuffer()) {
              if (directNIOOnly) {
                  writer.print( getArgumentName(i)+ " != null ? " + getArgumentName(i) + ".getBuffer() : null");
              } else {
                  writer.print( isNIOArgName(i) + " ? ( " + getArgumentName(i)+ " != null ? " + getArgumentName(i) + ".getBuffer() : null )");
                  writer.print( " : Buffers.getArray(" + getArgumentName(i) + ")" );
              }
          } else {
              if (directNIOOnly) {
                  writer.print( getArgumentName(i) );
              } else {
                  writer.print( isNIOArgName(i) + " ? " + getArgumentName(i) + " : Buffers.getArray(" + getArgumentName(i) + ")" );
              }
          }
      } else if (type.isArrayOfCompoundTypeWrappers()) {
          writer.print(getArgumentName(i) + COMPOUND_ARRAY_SUFFIX);
      } else {
          writer.print(getArgumentName(i));
      }

      if (type.isCompoundTypeWrapper()) {
        writer.print(" == null) ? null : ");
        writer.print(getArgumentName(i));
        writer.print(".getBuffer())");
      }

      if (type.isNIOBuffer()) {
        if (directNIOOnly) {
          writer.print( ", Buffers.getDirectBufferByteOffset(" + getArgumentName(i) + ")");
        } else {
          writer.print( ", " + isNIOArgName(i) + " ? Buffers.getDirectBufferByteOffset(" + getArgumentName(i) + ")");
          writer.print(        " : Buffers.getIndirectBufferByteOffset(" + getArgumentName(i) + ")");
        }
      } else if (type.isNIOBufferArray()) {
        writer.print(", " + byteOffsetArrayArgName(i));
      } else if (type.isPrimitiveArray()) {
        if(type.isFloatArray()) {
          writer.print(", Buffers.SIZEOF_FLOAT * ");
        } else if(type.isDoubleArray()) {
          writer.print(", Buffers.SIZEOF_DOUBLE * ");
        } else if(type.isByteArray()) {
          writer.print(", ");
        } else if(type.isLongArray()) {
          writer.print(", Buffers.SIZEOF_LONG * ");
        } else if(type.isShortArray()) {
          writer.print(", Buffers.SIZEOF_SHORT * ");
        } else if(type.isIntArray()) {
          writer.print(", Buffers.SIZEOF_INT * ");
        } else {
          throw new RuntimeException("Unsupported type for calculating array offset argument for " +
                                     getArgumentName(i) +
                                     " -- error occurred while processing Java glue code for " + getName());
        }
        writer.print(offsetArgName(i));
      }

      if (type.isNIOBuffer()) {
        if (!directNIOOnly) {
            writer.print( ", " + isNIOArgName(i) );
        }
      } else if (type.isPrimitiveArray()) {
        if (directNIOOnly) {
            throw new RuntimeException("NIODirectOnly "+binding+" is set, but "+getArgumentName(i)+" is a primitive array");
        }
        writer.print( ", false");
      }

      needComma = true;
      ++numArgsEmitted;
    }
    return numArgsEmitted;
  }

  protected void emitPostCallCleanup(MethodBinding binding, PrintWriter writer) {
    if (binding.signatureUsesArraysOfCompoundTypeWrappers()) {
      // For each such array, we need to take the ByteBuffer[] that
      // came back from the C method invocation and wrap the
      // ByteBuffers back into the wrapper types
      for (int i = 0; i < binding.getNumArguments(); i++) {
        JavaType javaArgType = binding.getJavaArgumentType(i);
        if (javaArgType.isArrayOfCompoundTypeWrappers()) {
          String argName = binding.getArgumentName(i);
          writer.println("    for (int _ctr = 0; _ctr < " + argName + ".length; _ctr++) {");
          writer.println("      if ((" + argName + "[_ctr] == null && " + argName + COMPOUND_ARRAY_SUFFIX + "[_ctr] == null) ||");
          writer.println("          (" + argName + "[_ctr] != null && " + argName + "[_ctr].getBuffer() == " + argName + COMPOUND_ARRAY_SUFFIX + "[_ctr])) {");
          writer.println("        // No copy back needed");
          writer.println("      } else {");
          writer.println("        if (" + argName + COMPOUND_ARRAY_SUFFIX + "[_ctr] == null) {");
          writer.println("          " + argName + "[_ctr] = null;");
          writer.println("        } else {");
          writer.println("          " + argName + "[_ctr] = " + javaArgType.getName() + ".create(" + argName + COMPOUND_ARRAY_SUFFIX + "[_ctr]);");
          writer.println("        }");
          writer.println("      }");
          writer.println("    }");
        }
      }
    }
  }

  protected void emitCallResultReturn(MethodBinding binding, PrintWriter writer) {
    JavaType returnType = binding.getJavaReturnType();

    if (returnType.isCompoundTypeWrapper()) {
      String fmt = getReturnedArrayLengthExpression();
      writer.println("    if (_res == null) return null;");
      if (fmt == null) {
        writer.print("    return " + returnType.getName() + ".create(Buffers.nativeOrder(_res))");
      } else {
        writer.println("    Buffers.nativeOrder(_res);");
        String expr = new MessageFormat(fmt).format(argumentNameArray());
        PointerType cReturnTypePointer = binding.getCReturnType().asPointer();
        CompoundType cReturnType = null;
        if (cReturnTypePointer != null) {
          cReturnType = cReturnTypePointer.getTargetType().asCompound();
        }
        if (cReturnType == null) {
          throw new RuntimeException("ReturnedArrayLength directive currently only supported for pointers to compound types " +
                                     "(error occurred while generating Java glue code for " + getName() + ")");
        }
        writer.println("    " + getReturnTypeString(false) + " _retarray = new " + getReturnTypeString(true) + "[" + expr + "];");
        writer.println("    for (int _count = 0; _count < " + expr + "; _count++) {");
        // Create temporary ByteBuffer slice
        // FIXME: probably need Type.getAlignedSize() for arrays of
        // compound types (rounding up to machine-dependent alignment)
        writer.println("      _res.position(_count * " + getReturnTypeString(true) + ".size());");
        writer.println("      _res.limit   ((1 + _count) * " + getReturnTypeString(true) + ".size());");
        writer.println("      ByteBuffer _tmp = _res.slice();");
        writer.println("      Buffers.nativeOrder(_tmp);");
        writer.println("      _res.position(0);");
        writer.println("      _res.limit(_res.capacity());");
        writer.println("      _retarray[_count] = " + getReturnTypeString(true) + ".create(_tmp);");
        writer.println("    }");
        writer.print  ("    return _retarray");
      }
      writer.println(";");
    } else if (returnType.isNIOBuffer()) {
      writer.println("    if (_res == null) return null;");
      writer.println("    Buffers.nativeOrder(_res);");
      if (!returnType.isNIOByteBuffer()) {
        // See whether we have to expand pointers to longs
        if (getBinding().getCReturnType().pointerDepth() >= 2) {
          if (returnType.isNIOPointerBuffer()) {
              writer.println("    return PointerBuffer.wrap(_res);");
          } else if (returnType.isNIOInt64Buffer()) {
              writer.println("    return Int64Buffer.wrap(_res);");
          } else {
            throw new RuntimeException("While emitting glue code for " + getName() +
                                       ": can not legally make pointers opaque to anything but PointerBuffer or Int64Buffer/long");
          }
        } else if (getBinding().getCReturnType().pointerDepth() == 1 &&
          returnType.isNIOInt64Buffer()) {
          writer.println("    return Int64Buffer.wrap(_res);");
        } else {
          String returnTypeName = returnType.getName().substring("java.nio.".length());
          writer.println("    return _res.as" + returnTypeName + "();");
        }
      } else {
        writer.println("    return _res;");
      }
    } else if (returnType.isArrayOfCompoundTypeWrappers()) {
      writer.println("    if (_res == null) return null;");
      writer.println("    " + getReturnTypeString(false) + " _retarray = new " + getReturnTypeString(true) + "[_res.length];");
      writer.println("    for (int _count = 0; _count < _res.length; _count++) {");
      writer.println("      _retarray[_count] = " + getReturnTypeString(true) + ".create(_res[_count]);");
      writer.println("    }");
      writer.println("    return _retarray;");
    } else {
      // Assume it's a primitive type or other type we don't have to
      // do any conversion on
      writer.println("    return _res;");
    }
  }

  protected String[] argumentNameArray() {
    String[] argumentNames = new String[binding.getNumArguments()];
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
  protected String getBaseIndentString() { return "  "; }

  protected String getReturnedArrayLengthExpression() {
    return returnedArrayLengthExpression;
  }

  /**
   * Class that emits a generic comment for JavaMethodBindingEmitters; the comment
   * includes the C signature of the native method that is being bound by the
   * emitter java method.
   */
  protected class DefaultCommentEmitter implements CommentEmitter {
    public void emit(FunctionEmitter emitter, PrintWriter writer) {
      emitBeginning(emitter, writer);
      emitBindingCSignature(((JavaMethodBindingEmitter)emitter).getBinding(), writer);
      emitEnding(emitter, writer);
    }
    protected void emitBeginning(FunctionEmitter emitter, PrintWriter writer) {
      writer.print("Entry point to C language function: ");
    }
    protected void emitBindingCSignature(MethodBinding binding, PrintWriter writer) {
      writer.print("<code> ");
      writer.print(binding.getCSymbol().toString(tagNativeBinding));
      writer.print(" </code> ");
    }
    protected void emitEnding(FunctionEmitter emitter, PrintWriter writer) {
      // If argument type is a named enum, then emit a comment detailing the
      // acceptable values of that enum.
      // If we're emitting a direct buffer variant only, then declare
      // that the NIO buffer arguments must be direct.
      MethodBinding binding = ((JavaMethodBindingEmitter)emitter).getBinding();
      for (int i = 0; i < binding.getNumArguments(); i++) {
        Type type = binding.getCArgumentType(i);
        JavaType javaType = binding.getJavaArgumentType(i);
        // don't emit param comments for anonymous enums, since we can't
        // distinguish between the values found within multiple anonymous
        // enums in the same C translation unit.
        if (type.isEnum() && !HeaderParser.ANONYMOUS_ENUM_NAME.equals(type.getName())) {
          EnumType enumType = (EnumType)type;
          writer.println();
          writer.print(emitter.getBaseIndentString());
          writer.print("    ");
          writer.print("@param ");
          writer.print(getArgumentName(i));
          writer.print(" valid values are: <code>");
          for (int j = 0; j < enumType.getNumEnumerates(); ++j) {
            if (j>0) writer.print(", ");
            writer.print(enumType.getEnumName(j));
          }
          writer.println("</code>");
        } else if (directNIOOnly && javaType.isNIOBuffer()) {
          writer.println();
          writer.print(emitter.getBaseIndentString());
          writer.print("    ");
          writer.print("@param ");
          writer.print(getArgumentName(i));
          writer.print(" a direct {@link " + javaType.getName() + "}");
        }
      }
    }
  }

    protected class InterfaceCommentEmitter extends JavaMethodBindingEmitter.DefaultCommentEmitter {

        @Override
        protected void emitBeginning(FunctionEmitter emitter,
                PrintWriter writer) {
            writer.print("Interface to C language function: <br> ");
        }
    }
}

