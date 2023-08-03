/**
 * Copyright 2023 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.gluegen;

import com.jogamp.gluegen.JavaConfiguration.JavaCallbackInfo;
import com.jogamp.gluegen.cgram.types.Type;

import java.nio.ByteBuffer;

public final class JavaCallbackEmitter {
    final JavaConfiguration cfg;
    final MethodBinding binding;
    final String setFuncSignature;
    final JavaCallbackInfo info;
    final String capIfaceName;
    final String lowIfaceName;
    final String lockInstanceName;
    final String dataMapInstanceName;
    final String dataInstanceName;
    final String DataClassName;
    final String fqUsrParamClassName;
    final JavaType cbFuncJavaReturnType;
    final String jcbNextIDVarName;

    final boolean userParamDefined;

    final String setFuncCBArgName;
    final Type setFuncUserParamCType;
    final JavaType setFuncUserParamJType;
    final String setFuncUserParamTypeName;
    final String setFuncUserParamArgName;
    final boolean userParamIsKey;
    final boolean userParamIsCompound;
    final boolean userParamIsMappedToID;
    final String userParamIDMapInstanceName;

    final String userParamClassName;
    final boolean customKeyClass;
    final String KeyClassName;
    final boolean useDataMap;

    public JavaCallbackEmitter(final JavaConfiguration cfg, final MethodBinding mb, final JavaCallbackInfo javaCallback, final String setFuncSignature) {
        this.cfg = cfg;
        this.binding = mb;
        this.setFuncSignature = setFuncSignature;
        this.info = javaCallback;

        capIfaceName = CodeGenUtils.capitalizeString( mb.getInterfaceName() );
        lowIfaceName = CodeGenUtils.decapitalizeString( mb.getInterfaceName() );
        lockInstanceName = lowIfaceName+"Lock";
        dataMapInstanceName = lowIfaceName+"DataMap";
        dataInstanceName = lowIfaceName+"Data";
        DataClassName = capIfaceName+"Data";
        fqUsrParamClassName = cfg.packageName()+"."+cfg.className()+"."+DataClassName;
        cbFuncJavaReturnType = javaCallback.cbFuncBinding.getJavaReturnType();
        jcbNextIDVarName = "NEXT_"+capIfaceName+"_ID";

        setFuncCBArgName = binding.getArgumentName(javaCallback.setFuncCBParamIdx);

        userParamDefined = javaCallback.setFuncUserParamIdx >= 0;
        if( !userParamDefined ) {
            setFuncUserParamCType = null;
            setFuncUserParamJType = null;
            setFuncUserParamTypeName = null;
            setFuncUserParamArgName = null;

            userParamIsKey = false;
        } else {
            setFuncUserParamCType = mb.getCArgumentType(javaCallback.setFuncUserParamIdx);
            setFuncUserParamJType = mb.getJavaArgumentType(javaCallback.setFuncUserParamIdx);
            setFuncUserParamTypeName = setFuncUserParamJType.getName();
            setFuncUserParamArgName = binding.getArgumentName(javaCallback.setFuncUserParamIdx);

            userParamIsKey = info.setFuncKeyIndices.contains(info.setFuncUserParamIdx);
        }
        if( null != setFuncUserParamJType && !setFuncUserParamJType.isLong() ) {
            if( setFuncUserParamJType.isCompoundTypeWrapper() ) {
                userParamIsCompound = true;
                userParamIsMappedToID = false;
                userParamIDMapInstanceName = null;
            } else {
                userParamIsCompound = false;
                userParamIsMappedToID = true;
                userParamIDMapInstanceName = userParamIsKey ? lowIfaceName+"UserObjIDMap" : null;
            }
        } else {
            userParamIsCompound = false;
            userParamIsMappedToID = false;
            userParamIDMapInstanceName = null;
        }

        if( userParamDefined ) {
            if( userParamIsCompound ) {
                userParamClassName = setFuncUserParamTypeName;
            } else if( null != javaCallback.userParamClassName ) {
                userParamClassName = javaCallback.userParamClassName;
            } else {
                userParamClassName = "Object";
            }
        } else {
            userParamClassName = null;
        }

        if( null != javaCallback.customKeyClassName ) {
            customKeyClass = true;
            KeyClassName = javaCallback.customKeyClassName;
            useDataMap = true;
        } else {
            customKeyClass = false;
            KeyClassName = capIfaceName+"Key";
            useDataMap = javaCallback.setFuncKeyIndices.size() > 0;
        }
    }

    public void emitJavaAdditionalCode(final CodeUnit unit, final boolean isInterface) {
        if( isInterface ) {
            if( useDataMap ) {
                if( !customKeyClass && !info.keyClassEmitted ) {
                    emitJavaKeyClass(unit);
                    unit.emitln();
                    info.keyClassEmitted = true;
                }
                emitJavaBriefAPIDoc(unit, "Returns ", "set of ", "", "for ");
                unit.emitln("  public Set<"+KeyClassName+"> get"+capIfaceName+"Keys();");
                unit.emitln();
                emitJavaBriefAPIDoc(unit, "Returns ", "whether callback ", "if callback ", "is mapped for ");
                unit.emitln("  public boolean is"+capIfaceName+"Mapped("+KeyClassName+" key);");
                unit.emitln();
                emitJavaBriefAPIDoc(unit, "Returns "+info.cbFuncTypeName+" callback ", "mapped to ", "", "for ");
                unit.emitln("  public "+info.cbFuncTypeName+" get"+capIfaceName+"("+KeyClassName+" key);");
                unit.emitln();
                if( userParamDefined ) {
                    emitJavaBriefAPIDoc(unit, "Returns user-param ", "mapped to ", "", "for ");
                    unit.emitln("  public "+userParamClassName+" get"+capIfaceName+"UserParam("+KeyClassName+" key);");
                    unit.emitln();
                }
                emitJavaBriefAPIDoc(unit, "Releases all callback data ", "mapped via ", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public int releaseAll"+capIfaceName+"();");
                unit.emitln();
                emitJavaBriefAPIDoc(unit, "Releases callback data ", "mapped to ", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public void release"+capIfaceName+"("+KeyClassName+" key);");
                unit.emitln();
            } else {
                emitJavaBriefAPIDoc(unit, "Returns ", "whether callback ", "if callback ", "is mapped for ");
                unit.emitln("  public boolean is"+capIfaceName+"Mapped();");
                unit.emitln();
                emitJavaBriefAPIDoc(unit, "Returns "+info.cbFuncTypeName+" callback ", "mapped to ", "", "for ");
                unit.emitln("  public "+info.cbFuncTypeName+" get"+capIfaceName+"();");
                unit.emitln();
                if( userParamDefined ) {
                    emitJavaBriefAPIDoc(unit, "Returns user-param ", "mapped to ", "", "for ");
                    unit.emitln("  public "+userParamClassName+" get"+capIfaceName+"UserParam();");
                    unit.emitln();
                }
                emitJavaBriefAPIDoc(unit, "Releases callback data ", "", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public void release"+capIfaceName+"();");
                unit.emitln();
            }
        } else {
            if( useDataMap ) {
                if( !customKeyClass && !info.keyClassEmitted ) {
                    emitJavaKeyClass(unit);
                    unit.emitln();
                    info.keyClassEmitted = true;
                }
                emitJavaBriefAPIDoc(unit, "Returns ", "set of ", "", "for ");
                unit.emitln("  public final Set<"+KeyClassName+"> get"+capIfaceName+"Keys() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      return "+dataMapInstanceName+".keySet();");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();
                emitJavaBriefAPIDoc(unit, "Returns ", "whether callback ", "if callback ", "is mapped for ");
                unit.emitln("  public final boolean is"+capIfaceName+"Mapped("+KeyClassName+" key) {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      return null != "+dataMapInstanceName+".get(key);");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();

                emitJavaBriefAPIDoc(unit, "Returns "+info.cbFuncTypeName+" callback ", "mapped to ", "", "for ");
                unit.emitln("  public final "+info.cbFuncTypeName+" get"+capIfaceName+"("+KeyClassName+" key) {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      final "+DataClassName+" value = "+dataMapInstanceName+".get(key);");
                unit.emitln("      return null != value ? value.func : null;");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();

                if( userParamDefined ) {
                    emitJavaBriefAPIDoc(unit, "Returns user-param ", "mapped to ", "", "for ");
                    unit.emitln("  public final "+userParamClassName+" get"+capIfaceName+"UserParam("+KeyClassName+" key) {");
                    unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                    unit.emitln("      final "+DataClassName+" value = "+dataMapInstanceName+".get(key);");
                    unit.emitln("      return null != value ? value.param : null;");
                    unit.emitln("    }");
                    unit.emitln("  }");
                    unit.emitln();
                }
                emitJavaBriefAPIDoc(unit, "Releases all callback data ", "mapped via ", "", "skipping toolkit API. Favor passing `null` callback ref to ");
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
                emitJavaBriefAPIDoc(unit, "Releases callback data ", "mapped to ", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public final void release"+capIfaceName+"("+KeyClassName+" key) {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                if( userParamIsMappedToID && userParamIsKey ) {
                    unit.emitln("      "+DataClassName+" value = "+dataMapInstanceName+".remove(key);");
                    unit.emitln("      if( null != value ) {");
                    unit.emitln("        "+userParamIDMapInstanceName+".remove(value.paramID);");
                    unit.emitln("      }");
                } else {
                    unit.emitln("      /* "+DataClassName+" value = */ "+dataMapInstanceName+".remove(key);");
                }
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();
            } else {
                emitJavaBriefAPIDoc(unit, "Returns ", "whether callback ", "if callback ", "is mapped for ");
                unit.emitln("  public final boolean is"+capIfaceName+"Mapped() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      return null != "+dataInstanceName+";");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();

                emitJavaBriefAPIDoc(unit, "Returns "+info.cbFuncTypeName+" callback ", "mapped to ", "", "for ");
                unit.emitln("  public final "+info.cbFuncTypeName+" get"+capIfaceName+"() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      final "+DataClassName+" value = "+dataInstanceName+";");
                unit.emitln("      return null != value ? value.func : null;");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();

                if( userParamDefined ) {
                    emitJavaBriefAPIDoc(unit, "Returns user-param ", "mapped to ", "", "for ");
                    unit.emitln("  public final "+userParamClassName+" get"+capIfaceName+"UserParam() {");
                    unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                    unit.emitln("      final "+DataClassName+" value = "+dataInstanceName+";");
                    unit.emitln("      return null != value ? value.param : null;");
                    unit.emitln("    }");
                    unit.emitln("  }");
                    unit.emitln();
                }

                emitJavaBriefAPIDoc(unit, "Releases callback data ", "", "", "skipping toolkit API. Favor passing `null` callback ref to ");
                unit.emitln("  public final void release"+capIfaceName+"() {");
                unit.emitln("    synchronized( "+lockInstanceName+" ) {");
                unit.emitln("      // final "+DataClassName+" value = "+dataInstanceName+";");
                if( userParamIsMappedToID && userParamIsKey ) {
                    unit.emitln("      "+userParamIDMapInstanceName+".remove(dataInstanceName.paramID);");
                }
                unit.emitln("      "+dataInstanceName+" = null;");
                unit.emitln("    }");
                unit.emitln("  }");
                unit.emitln();
            }
            unit.emitln("  private final void add"+capIfaceName+"("+binding.getJavaSelectParameter(new StringBuilder(), info.setFuncKeyIndices, true).toString()+DataClassName+" value) {");
            if( useDataMap ) {
                unit.emitln("    final "+KeyClassName+" key = new "+KeyClassName+"("+binding.getJavaCallSelectArguments(new StringBuilder(), info.setFuncKeyIndices, false).toString()+");");
                if( userParamIsMappedToID && userParamIsKey ) {
                    unit.emitln("    final "+DataClassName+" old = "+dataMapInstanceName+".put(key, value);");
                } else {
                    unit.emitln("    /* final "+DataClassName+" old = */ "+dataMapInstanceName+".put(key, value);");
                }
            } else {
                if( userParamIsMappedToID && userParamIsKey ) {
                    unit.emitln("    final "+DataClassName+" old = "+dataInstanceName+";");
                } else {
                    unit.emitln("    // final "+DataClassName+" old = "+dataInstanceName+";");
                }
                unit.emitln("    "+dataInstanceName+" = value;");
            }
            if( userParamIsMappedToID && userParamIsKey ) {
                unit.emitln("    if( null != old ) {");
                unit.emitln("      "+userParamIDMapInstanceName+".remove(old.paramID);");
                unit.emitln("    }");
                unit.emitln("    if( null != value.param ) {");
                unit.emitln("      "+userParamIDMapInstanceName+".put(value.paramID, value.param);");
                unit.emitln("    }");
            }
            unit.emitln("  }");
            unit.emitln();
            if( !cfg.emittedJavaCallbackUserParamClasses.contains(fqUsrParamClassName) ) {
                emitJavaDataClass(unit);
                cfg.emittedJavaCallbackUserParamClasses.add(fqUsrParamClassName);
            }
            if( useDataMap ) {
                unit.emitln("  private static final Map<"+KeyClassName+", "+DataClassName+"> "+dataMapInstanceName+" = new HashMap<"+KeyClassName+", "+DataClassName+">();");
            } else {
                unit.emitln("  private static "+DataClassName+" "+dataInstanceName+" = null;");
            }
            if( userParamIsMappedToID && userParamIsKey ) {
                unit.emitln("  private static final LongObjectHashMap "+userParamIDMapInstanceName+" = new LongObjectHashMap();");
            }
            unit.emitln("  private static long "+jcbNextIDVarName+" = 1;");
            unit.emitln("  private static final Object "+lockInstanceName+" = new Object();");
            unit.emitln();
            emitJavaStaticCallback(unit);
        }
    }

    private final void emitJavaBriefAPIDoc(final CodeUnit unit, final String actionText, final String relationToKey, final String noKeyText, final String relationToFunc) {
        unit.emit("  /** "+actionText);
        if( info.setFuncKeyIndices.size() > 0 ) {
            unit.emit(relationToKey);
            unit.emit("Key { "+binding.getJavaSelectParameter(new StringBuilder(), info.setFuncKeyIndices, false).toString()+" } ");
        } else {
            unit.emit(noKeyText);
        }
        unit.emit(relationToFunc);
        unit.emitln("<br> <code>"+setFuncSignature+"</code> */");
    }

    private final void emitJavaKeyClass(final CodeUnit unit) {
        emitJavaBriefAPIDoc(unit, "", "", "", "for ");
        unit.emitln("  public static class "+KeyClassName+" {");
        binding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
            if( !cType.isVoid() && info.setFuncKeyIndices.contains(idx) ) {
                unit.emitln("    public final "+jType+" "+name+";");
                return true;
            } else {
                return false;
            }
        } );
        unit.emitln("    public "+KeyClassName+"("+binding.getJavaSelectParameter(new StringBuilder(), info.setFuncKeyIndices, false).toString()+") {");
        binding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
            if( !cType.isVoid() && info.setFuncKeyIndices.contains(idx) ) {
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
        unit.emitln("      if( !(o instanceof "+KeyClassName+") ) {");
        unit.emitln("        return false;");
        unit.emitln("      }");
        {
            final int count = binding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
                if( !cType.isVoid() && info.setFuncKeyIndices.contains(idx) ) {
                    if( 0 == consumedCount ) {
                        unit.emitln("      final "+KeyClassName+" o2 = ("+KeyClassName+")o;");
                        unit.emit  ("      return ");
                    } else {
                        unit.emitln(" &&");
                        unit.emit  ("             ");
                    }
                    if( jType.isPrimitive() || idx == info.setFuncUserParamIdx ) {
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
                if( !cType.isVoid() && info.setFuncKeyIndices.contains(idx) ) {
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
                        if( idx == info.setFuncUserParamIdx ) {
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

    private final void emitJavaDataClass(final CodeUnit unit) {
        unit.emitln("  private static class "+DataClassName+" {");
        unit.emitln("    final " + info.cbFuncTypeName + " func;");
        if( userParamDefined ) {
            unit.emitln("    // userParamArgCType "+setFuncUserParamCType);
            unit.emitln("    // userParamArgJType "+setFuncUserParamJType);
            unit.emitln("    final "+setFuncUserParamTypeName+" param;");
            if( userParamIsMappedToID ) {
                unit.emitln("    final long paramID;");
            }
        } else {
            unit.emitln("    // No user param defined.");
        }
        unit.emit  ("    "+DataClassName+"(final "+info.cbFuncTypeName+" func");
        if( userParamDefined ) {
            if( userParamIsMappedToID ) {
                unit.emit(", final long paramID");
            }
            unit.emit(", final "+setFuncUserParamTypeName+" param");
        }
        unit.emitln(") {");
        unit.emitln("      this.func = func;");
        if( userParamDefined ) {
            if( userParamIsMappedToID ) {
                unit.emitln("      this.paramID = paramID;");
            }
            unit.emitln("      this.param = param;");
        }
        unit.emitln("    }");
        unit.emitln("  }");
    }

    public final String getJavaStaticCallbackSignature() {
        final StringBuilder buf = new StringBuilder();
        buf.append("(");
        info.cbFuncBinding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
            if( !cType.isVoid() ) {
                if( idx == info.cbFuncUserParamIdx ) {
                    buf.append("J");
                } else {
                    buf.append(jType.getDescriptor(cfg));
                }
                return true;
            } else {
                return false;
            }
        } );
        buf.append(")");
        buf.append(cbFuncJavaReturnType.getDescriptor(cfg));
        return buf.toString();
    }

    public final int appendJavaAdditionalJNIParameter(final StringBuilder buf) {
        buf.append("Class<?> staticCBClazz, String callbackSignature");
        if( !userParamDefined ) {
            return 2;
        }
        buf.append(", long nativeUserParam");
        return 3;
    }
    public final int appendJavaAdditionalJNIArguments(final StringBuilder buf) {
        buf.append("this.getClass(), \"" + getJavaStaticCallbackSignature() + "\"");
        if( !userParamDefined ) {
            return 2;
        }
        buf.append(", nativeUserParam");
        return 3;
    }

    public void emitJavaSetFuncPreCall(final CodeUnit unit) {
        if( userParamDefined ) {
            unit.emitln("    final long nativeUserParam;");
        }
        unit.emitln("    synchronized( "+lockInstanceName+" ) {");
        if( userParamDefined ) {
            if( setFuncUserParamJType.isLong() ) {
                unit.emitln("      nativeUserParam = "+setFuncUserParamArgName+";");
            } else {
                unit.emitln("      if( null != "+setFuncUserParamArgName+" ) {");
                if( setFuncUserParamJType.isCompoundTypeWrapper() ) {
                    // userParamIsCompound == true
                    unit.emitln("        nativeUserParam = "+setFuncUserParamArgName+".getDirectBufferAddress();");
                } else {
                    // userParamIsMappedToID == true
                    unit.emitln("        nativeUserParam = "+jcbNextIDVarName+"++;");
                    unit.emitln("        if( 0 >= "+jcbNextIDVarName+" ) { "+jcbNextIDVarName+" = 1; }");
                }
                unit.emitln("      } else {");
                unit.emitln("        nativeUserParam = 0;");
                unit.emitln("      }");
            }
        }
        unit.emitln("      if( null != "+setFuncCBArgName+" ) {");
        unit.emit  ("        add"+capIfaceName+"("+binding.getJavaCallSelectArguments(new StringBuilder(), info.setFuncKeyIndices, true).toString()+
                "new "+DataClassName+"("+setFuncCBArgName);
        if( userParamDefined ) {
            if( userParamIsMappedToID ) {
                unit.emit(", nativeUserParam");
            }
            unit.emit(", "+setFuncUserParamArgName);
        }
        unit.emitln("));");
        unit.emitln("      } else { ");
        unit.emitln("        // release a previously mapped instance ");
        if( useDataMap ) {
            unit.emitln("        release"+capIfaceName+"( new "+KeyClassName+"( "+binding.getJavaCallSelectArguments(new StringBuilder(), info.setFuncKeyIndices, false).toString()+" ) );");
        } else {
            unit.emitln("        release"+capIfaceName+"();");
        }
        unit.emitln("      }");
        unit.emitln("    } // synchronized ");
        unit.emitln();
    }

    private final void emitJavaStaticCallback(final CodeUnit unit) {
        unit.emitln("  /** Static callback invocation, dispatching to "+info.cbSimpleClazzName+" for callback <br> <code>"+
                info.cbFuncType.toString(info.cbFuncTypeName, false, true)+"</code> */");
        unit.emit  ("  /* pp */ static "+cbFuncJavaReturnType.getName()+" invoke"+capIfaceName+"(");
        final boolean[] mapNativePtrToCompound = { false };
        final JavaType[] origUserParamJType = { null };
        info.cbFuncBinding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
            if( !cType.isVoid() ) {
                if( 0 < consumedCount ) { unit.emit(", "); }
                if( idx == info.cbFuncUserParamIdx ) {
                    unit.emit("long nativeUserParam");
                    if( jType.isCompoundTypeWrapper() ) {
                        mapNativePtrToCompound[0] = true;
                        origUserParamJType[0] = jType;
                    }
                } else {
                    unit.emit(jType+" "+name);
                }
                return true;
            } else {
                return false;
            }
        } );
        unit.emitln(") {");
        final boolean useParamLocal[] = { false };
        if( userParamDefined ) {
            if( mapNativePtrToCompound[0] ) {
                unit.emitln("    final "+origUserParamJType[0]+" "+info.cbFuncUserParamName+" = "+origUserParamJType[0]+".derefPointer(nativeUserParam);");
                useParamLocal[0] = true;
            } else if( userParamIsMappedToID && userParamIsKey ) {
                unit.emitln("    final "+userParamClassName+" "+info.cbFuncUserParamName+";");
            }
        }
        unit.emitln("    final "+DataClassName+" value;");
        unit.emitln("    synchronized( "+lockInstanceName+" ) {");
        if( userParamDefined ) {
            if( userParamIsMappedToID && userParamIsKey && !mapNativePtrToCompound[0] ) {
                unit.emitln("      "+info.cbFuncUserParamName+" = ("+userParamClassName+") "+userParamIDMapInstanceName+".get(nativeUserParam);");
                useParamLocal[0] = true;
            }
        }
        if( useDataMap ) {
            unit.emitln("      final "+KeyClassName+" key = new "+KeyClassName+"("+info.cbFuncBinding.getJavaCallSelectArguments(new StringBuilder(), info.cbFuncKeyIndices, false).toString()+");");
            unit.emitln("      value = "+dataMapInstanceName+".get(key);");
        } else {
            unit.emitln("      value = "+dataInstanceName+";");
        }
        unit.emitln("    }");
        unit.emitln("    if( null == value ) {");
        if( !cbFuncJavaReturnType.isVoid() ) {
            unit.emitln("      return 0;");
        } else {
            unit.emitln("      return;");
        }
        unit.emitln("    }");
        if( !cbFuncJavaReturnType.isVoid() ) {
            unit.emit("    return ");
        } else {
            unit.emit("    ");
        }
        unit.emit("value.func.callback(");
        info.cbFuncBinding.forEachParameter( ( final int idx, final int consumedCount, final Type cType, final JavaType jType, final String name ) -> {
            if( !cType.isVoid() ) {
                if( 0 < consumedCount ) { unit.emit(", "); }
                if( idx == info.cbFuncUserParamIdx && !useParamLocal[0] ) {
                    unit.emit("value.param");
                } else {
                    unit.emit(name);
                }
                return true;
            } else {
                return false;
            }
        } );
        unit.emitln(");");
        unit.emitln("  }");
        unit.emitln();
    }

    //
    // C JNI Code ..
    //

    public int appendCAdditionalParameter(final StringBuilder buf) {
        buf.append(", jclass staticCBClazz, jstring jcallbackSignature");
        if( !userParamDefined ) {
            return 2;
        }
        buf.append(", jlong jnativeUserParam");
        return 3;
    }

    public void emitCOptArgumentSuffix(final CodeUnit unit, final int argIdx) {
      if( ( argIdx == info.setFuncCBParamIdx || argIdx == info.setFuncUserParamIdx ) ) {
          unit.emit("_native");
      }
    }

    public void appendCAdditionalJNIDescriptor(final StringBuilder buf) {
        JavaType.appendJNIDescriptor(buf, Class.class, false);  // to account for the additional 'jclass staticCBClazz' parameter
        JavaType.appendJNIDescriptor(buf, String.class, false);  // to account for the additional 'jstring jcallbackSignature' parameter
        if( userParamDefined ) {
            JavaType.appendJNIDescriptor(buf, long.class, false);  // to account for the additional 'long nativeUserParam' parameter
        }
    }

    public void emitCSetFuncPreCall(final CodeUnit unit, final CMethodBindingEmitter jcbFuncCMethodEmitter) {
        final String jcbNativeBasename = CodeGenUtils.capitalizeString( info.setFuncName );
        final String jcbFriendlyBasename = info.setFuncName+"("+info.cbSimpleClazzName+")";
        final String staticBindingMethodName = "invoke"+jcbNativeBasename;
        final String staticBindingClazzVarName = "staticCBClazz"+jcbNativeBasename;
        final String staticBindingMethodIDVarName = "staticCBMethod"+jcbNativeBasename;
        final String cbFuncArgName = binding.getArgumentName(info.setFuncCBParamIdx);
        final String nativeCBFuncVarName = cbFuncArgName+"_native";
        final String userParamTypeName, nativeUserParamVarName;
        if( userParamDefined ) {
            userParamTypeName = info.cbFuncUserParamType.getCName();
            nativeUserParamVarName = binding.getArgumentName(info.setFuncUserParamIdx)+"_native";
        } else {
            userParamTypeName = null;
            nativeUserParamVarName = null;
        }
        unit.emitln();
        unit.emitln("  // JavaCallback handling");
        unit.emitln("  if( NULL == staticCBClazz ) { (*env)->FatalError(env, \"NULL staticCBClazz passed to '"+jcbFriendlyBasename+"'\"); }");
        unit.emitln("  "+info.cbFuncTypeName+" "+nativeCBFuncVarName+";");
        if( userParamDefined ) {
            unit.emitln("  "+userParamTypeName+"* "+nativeUserParamVarName+";");
        }
        unit.emitln("  if( NULL != "+cbFuncArgName+" ) {");
        unit.emitln("    if( NULL == "+staticBindingClazzVarName+" || NULL == "+staticBindingMethodIDVarName+" ) {");
        unit.emitln("      jclass staticCBClazz2 = (*env)->NewGlobalRef(env, staticCBClazz);");
        unit.emitln("      if( NULL == staticCBClazz2 ) { (*env)->FatalError(env, \"Failed NewGlobalRef(staticCBClazz) in '"+jcbFriendlyBasename+"'\"); }");
        unit.emitln("      const char* callbackSignature = (*env)->GetStringUTFChars(env, jcallbackSignature, (jboolean*)NULL);");
        unit.emitln("      if( NULL == callbackSignature ) { (*env)->FatalError(env, \"Failed callbackSignature in '"+jcbFriendlyBasename+"'\"); }");
        unit.emitln("      jmethodID cbMethodID = (*env)->GetStaticMethodID(env, staticCBClazz2, \""+staticBindingMethodName+"\", callbackSignature);");
        unit.emitln("      if( NULL == cbMethodID ) {");
        unit.emitln("        char cmsg[400];");
        unit.emitln("        snprintf(cmsg, 400, \"Failed GetStaticMethodID of '"+staticBindingMethodName+"(%s)' in '"+jcbFriendlyBasename+"'\", callbackSignature);");
        unit.emitln("        (*env)->FatalError(env, cmsg);");
        unit.emitln("      }");
        unit.emitln("      (*env)->ReleaseStringUTFChars(env, jcallbackSignature, callbackSignature);");
        unit.emitln("      "+staticBindingClazzVarName+" = staticCBClazz2;");
        unit.emitln("      "+staticBindingMethodIDVarName+" = cbMethodID;");
        unit.emitln("    }");
        final JavaType bbjt = JavaType.createForClass(ByteBuffer.class);
        for (int i = 0; i < jcbFuncCMethodEmitter.binding.getNumArguments(); i++) {
            final String baseArgName = CodeGenUtils.capitalizeString( jcbFuncCMethodEmitter.binding.getArgumentName(i) );
            final JavaType currentJavaType = jcbFuncCMethodEmitter.binding.getJavaArgumentType(i);
            if( i != info.cbFuncUserParamIdx && currentJavaType.isCompoundTypeWrapper() ) {
                final String staticBindingClazzArgVarName = "staticCBArg" + baseArgName + "Clazz"+jcbNativeBasename;
                final String staticBindingMethodIDArgVarName = "staticCBArg" + baseArgName + "Method"+jcbNativeBasename;
                unit.emitln("    if( NULL == "+staticBindingClazzArgVarName+" || NULL == "+staticBindingMethodIDArgVarName+" ) {");
                final String argClassDescriptor = currentJavaType.getDescriptor(cfg);
                final String argClassRef = currentJavaType.getName(cfg);
                final String argClassLocation = argClassRef.replace(".", "/");
                unit.emitln("      jclass "+staticBindingClazzArgVarName+"2 = (*env)->FindClass(env, \""+argClassLocation+"\");");
                unit.emitln("      if( NULL == "+staticBindingClazzArgVarName+"2 ) { (*env)->FatalError(env, \"Failed FindClass("+argClassLocation+") in '"+jcbFriendlyBasename+"'\"); }");
                unit.emitln("      jmethodID "+staticBindingMethodIDArgVarName+"2 = (*env)->GetStaticMethodID(env, "+staticBindingClazzArgVarName+"2, \"create\", \"("+bbjt.getDescriptor()+")"+argClassDescriptor+"\");");
                unit.emitln("      if( NULL == "+staticBindingMethodIDArgVarName+"2 ) {");
                unit.emitln("        char cmsg[400];");
                unit.emitln("        snprintf(cmsg, 400, \"Failed GetStaticMethodID of '"+argClassRef+".create("+bbjt.getDescriptor()+")"+argClassDescriptor+" in "+jcbFriendlyBasename+"'\");");
                unit.emitln("        (*env)->FatalError(env, cmsg);");
                unit.emitln("      }");
                unit.emitln("      "+staticBindingClazzArgVarName+" = "+staticBindingClazzArgVarName+"2;");
                unit.emitln("      "+staticBindingMethodIDArgVarName+" = "+staticBindingMethodIDArgVarName+"2;");
                unit.emitln("    }");
            }
        }
        unit.emitln("    "+nativeCBFuncVarName+" = func"+jcbNativeBasename+";");
        if( userParamDefined ) {
            unit.emitln("    "+nativeUserParamVarName+" = ("+userParamTypeName+"*) jnativeUserParam;");
        }
        unit.emitln("  } else {");
        unit.emitln("    "+nativeCBFuncVarName+" = NULL;");
        if( userParamDefined ) {
            unit.emitln("    "+nativeUserParamVarName+" = NULL;");
        }
        unit.emitln("  }");
        unit.emitln();

    }

    public void emitCAdditionalCode(final CodeUnit unit, final CMethodBindingEmitter jcbFuncCMethodEmitter) {
        final String jcbNativeBasename = CodeGenUtils.capitalizeString( info.setFuncName );
        final String jcbFriendlyBasename = info.setFuncName+"("+info.cbSimpleClazzName+")";
        final String staticBindingClazzVarName = "staticCBClazz"+jcbNativeBasename;
        final String staticBindingMethodIDVarName = "staticCBMethod"+jcbNativeBasename;
        final String staticCallbackName = "func"+jcbNativeBasename;
        final String userParamTypeName, userParamArgName;
        if( userParamDefined ) {
            // final Type userParamType = javaCallback.cbFuncBinding.getCArgumentType(javaCallback.cbFuncUserParamIdx);
            userParamTypeName = info.cbFuncUserParamType.getCName();
            userParamArgName = info.cbFuncBinding.getArgumentName(info.cbFuncUserParamIdx);
        } else {
            userParamTypeName = null;
            userParamArgName = null;
        }
        final Type cReturnType = info.cbFuncBinding.getCReturnType();
        final JavaType jretType = info.cbFuncBinding.getJavaReturnType();
        unit.emitln();
        unit.emitln("static jclass "+staticBindingClazzVarName+" = NULL;");
        unit.emitln("static jmethodID "+staticBindingMethodIDVarName+" = NULL;");
        for (int i = 0; i < jcbFuncCMethodEmitter.binding.getNumArguments(); i++) {
            final String baseArgName = CodeGenUtils.capitalizeString( jcbFuncCMethodEmitter.binding.getArgumentName(i) );
            final JavaType currentJavaType = jcbFuncCMethodEmitter.binding.getJavaArgumentType(i);
            if( i != info.cbFuncUserParamIdx && currentJavaType.isCompoundTypeWrapper() ) {
                final String staticBindingClazzArgVarName = "staticCBArg" + baseArgName + "Clazz"+jcbNativeBasename;
                final String staticBindingMethodIDArgVarName = "staticCBArg" + baseArgName + "Method"+jcbNativeBasename;
                unit.emitln("static jclass "+staticBindingClazzArgVarName+" = NULL;");
                unit.emitln("static jmethodID "+staticBindingMethodIDArgVarName+" = NULL;");
            }
        }
        unit.emitln();
        // javaCallback.cbFuncCEmitter.emitSignature();
        unit.emit("static "+cReturnType.getCName()+" "+staticCallbackName+"(");
        // javaCallback.cbFuncCEmitter.emitArguments();
        unit.emit(info.cbFuncBinding.getCParameterList(new StringBuilder(), false, null).toString());
        unit.emitln(") {");
        // javaCallback.cbFuncCEmitter.emitBody();
        {
            unit.emitln("  int detachJVM = 0;");
            unit.emitln("  JNIEnv* env = JVMUtil_GetJNIEnv(1 /* daemon */, &detachJVM);");
            unit.emitln("  jclass cbClazz = "+staticBindingClazzVarName+";");
            unit.emitln("  jmethodID cbMethod = "+staticBindingMethodIDVarName+";");
            for (int i = 0; i < jcbFuncCMethodEmitter.binding.getNumArguments(); i++) {
                final String baseArgName = CodeGenUtils.capitalizeString( jcbFuncCMethodEmitter.binding.getArgumentName(i) );
                final JavaType currentJavaType = jcbFuncCMethodEmitter.binding.getJavaArgumentType(i);
                if( i != info.cbFuncUserParamIdx && currentJavaType.isCompoundTypeWrapper() ) {
                    final String staticBindingClazzArgVarName = "staticCBArg" + baseArgName + "Clazz"+jcbNativeBasename;
                    final String staticBindingMethodIDArgVarName = "staticCBArg" + baseArgName + "Method"+jcbNativeBasename;
                    unit.emitln("  jclass cbClazzArg" + baseArgName+" = "+staticBindingClazzArgVarName+";");
                    unit.emitln("  jmethodID cbMethodArg" + baseArgName+" = "+staticBindingMethodIDArgVarName+";");
                }
            }
            unit.emitln("  if( NULL == env || NULL == cbClazz || NULL == cbMethod ) {");
            if( !cReturnType.isVoid() ) {
                unit.emitln("    return 0;");
            } else {
                unit.emitln("    return;");
            }
            unit.emitln("  }");
            // javaCallback.cbFuncCEmitter.emitBodyVariableDeclarations();
            // javaCallback.cbFuncCEmitter.emitBodyUserVariableDeclarations();
            // javaCallback.cbFuncCEmitter.emitBodyVariablePreCallSetup();
            emitJavaCallbackBodyCToJavaPreCall(jcbFuncCMethodEmitter);

            // javaCallback.cbFuncCEmitter.emitBodyCallCFunction();
            if( userParamDefined ) {
                unit.emitln("  "+userParamTypeName+"* "+userParamArgName+"_jni = ("+userParamTypeName+"*) "+userParamArgName+";");
            }
            unit.emitln("  // C Params: "+info.cbFuncBinding.getCParameterList(new StringBuilder(), false, null).toString());
            unit.emitln("  // J Params: "+info.cbFuncBinding.getJavaParameterList(new StringBuilder()).toString());

            final String returnStatement;
            if( !cReturnType.isVoid() ) {
                unit.emit("  "+cReturnType.getCName()+" _res = 0;");
                returnStatement = "return _res;";
            } else {
                returnStatement = "return;";
            }
            if( !cReturnType.isVoid() ) {
                unit.emit("  _res = ("+cReturnType.getCName()+") ");
            } else {
                unit.emit("  ");
            }
            unit.emit("(*env)->CallStatic" + CodeGenUtils.capitalizeString( jretType.getName() ) +"Method(env, cbClazz, cbMethod, ");
            // javaCallback.cbFuncCEmitter.emitBodyPassCArguments();
            emitJavaCallbackBodyPassJavaArguments(unit, jcbFuncCMethodEmitter.binding);
            unit.emitln(");");
            unit.emitln("  if( (*env)->ExceptionCheck(env) ) {");
            unit.emitln("    fprintf(stderr, \"Info: Callback '"+jcbFriendlyBasename+"': Exception in Java Callback caught:\\n\");");
            unit.emitln("    (*env)->ExceptionDescribe(env);");
            unit.emitln("    (*env)->ExceptionClear(env);");
            unit.emitln("  }");

            // javaCallback.cbFuncCEmitter.emitBodyUserVariableAssignments();
            // javaCallback.cbFuncCEmitter.emitBodyVariablePostCallCleanup();
            // javaCallback.cbFuncCEmitter.emitBodyMapCToJNIType(-1 /* return value */, true /* addLocalVar */)
            unit.emitln("  JVMUtil_ReleaseJNIEnv(env, detachJVM);");
            unit.emitln("  "+returnStatement);
        }
        unit.emitln("}");
        unit.emitln();
    }

    private int emitJavaCallbackBodyCToJavaPreCall(final CMethodBindingEmitter ce)  {
        int count = 0;
        for (int i = 0; i < ce.binding.getNumArguments(); i++) {
            if( i == info.cbFuncUserParamIdx ) {
                continue;
            }
            if( ce.emitBodyMapCToJNIType(i, true /* addLocalVar */) ) {
                ++count;
            }
        }
        return count;
    }

    private int emitJavaCallbackBodyPassJavaArguments(final CodeUnit unit, final MethodBinding jcbFuncCMethodBinding) {
        int count = 0;
        boolean needsComma = false;
        for (int i = 0; i < jcbFuncCMethodBinding.getNumArguments(); i++) {
            if (needsComma) {
                unit.emit(", ");
                needsComma = false;
            }
            final String baseArgName = jcbFuncCMethodBinding.getArgumentName(i);
            final JavaType currentJavaType = jcbFuncCMethodBinding.getJavaArgumentType(i);
            if( i != info.cbFuncUserParamIdx && currentJavaType.isCompoundTypeWrapper() ) {
                final String cBaseArgName = CodeGenUtils.capitalizeString( baseArgName );
                unit.emit( "(*env)->CallStaticObjectMethod(env, cbClazzArg" + cBaseArgName + ", cbMethodArg" + cBaseArgName + ", " + baseArgName + "_jni)" );
            } else {
                unit.emit( baseArgName + "_jni" );
            }
            needsComma = true;
            ++count;
        }
        return count;
    }


}
