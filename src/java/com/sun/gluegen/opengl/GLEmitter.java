/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.gluegen.opengl;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import com.sun.gluegen.*;
import com.sun.gluegen.cgram.types.*;
import com.sun.gluegen.procaddress.*;
import com.sun.gluegen.runtime.*;
import com.sun.gluegen.runtime.opengl.GLExtensionNames;

/**
 * A subclass of ProcAddressEmitter with special OpenGL-specific
 * configuration abilities.
 */
public class GLEmitter extends ProcAddressEmitter
{
  // Keeps track of which MethodBindings were created for handling
  // Buffer Object variants. Used as a Set rather than a Map.
  private Map/*<MethodBinding>*/ bufferObjectMethodBindings = new IdentityHashMap();

  static class BufferObjectKind {
    private BufferObjectKind() {}

    static final BufferObjectKind UNPACK_PIXEL = new BufferObjectKind();
    static final BufferObjectKind PACK_PIXEL   = new BufferObjectKind();
    static final BufferObjectKind ARRAY        = new BufferObjectKind();
    static final BufferObjectKind ELEMENT      = new BufferObjectKind();
  }
  
  public void beginEmission(GlueEmitterControls controls) throws IOException
  {
    getGLConfig().parseGLHeaders(controls);
    super.beginEmission(controls);
  }

  static class DefineEntry implements Cloneable {
    public DefineEntry(String namestr, String valuestr, String optionalComment) {
        this.name=new GLUnifiedName(namestr);
        this.value=getJavaValue(namestr, valuestr);
        this.type=getJavaType(namestr, this.value);
        this.radix=getJavaRadix(namestr, valuestr);
        this.optionalComment=optionalComment;
    }

    protected DefineEntry(GLUnifiedName name, String type, Object value, int radix, String optionalComment) {
        this.name=name;
        this.value=value;
        this.type=type;
        this.radix=radix;
        this.optionalComment=optionalComment;
    }

    public Object clone() {
        return new DefineEntry((GLUnifiedName)name.clone(), type, value, radix, optionalComment);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
          return true;
        }
        if(null==obj || !(obj instanceof DefineEntry)) return false;
        DefineEntry de = (DefineEntry) obj;
        return name.equals(de.name) &&
               type.equals(de.type) &&
               value.equals(de.value);
    }

    public String toString() {
        String res = "  public static final " + type + " " + name + " = ";
        if(16!=radix) {
            res = res + value;
        } else {
            res = res.concat("0x");
            if(value instanceof Integer) {
                res = res.concat( Integer.toString( ((Integer)value).intValue(), radix ).toUpperCase() );
            } else if(value instanceof Long) {
                res = res.concat( Long.toString( ((Long)value).longValue(), radix ).toUpperCase() );
            } else {
                res = res + value;
            }
        }

        return res.concat(";");
    }

    public String getOptCommentString() {
        if (optionalComment != null && optionalComment.length() > 0) {
          return "  /** " + optionalComment + " */";
        }
        return new String();
    }

    public void addOrigName(String name) {
        this.name.addOrig(name);
    }
    public boolean isExtensionVEN() {
        return name.isExtensionVEN();
    }
    public void normalizeVEN() {
        name.normalizeVEN();
    }
    public boolean shouldIgnoreInInterface(GLConfiguration cfg) {
        return GLEmitter.shouldIgnoreInInterface(name, cfg);
    }

    protected GLUnifiedName name;
    protected Object value;
    protected String type;
    protected int radix;
    protected String optionalComment;
  }

  protected static boolean shouldIgnoreInInterface(GLUnifiedName name, GLConfiguration cfg) {
        boolean res = cfg.shouldIgnoreInInterface(name.getUni(), name.isUnique());
        for (Iterator iter = name.getOrig().iterator(); !res && iter.hasNext(); ) {
            res = cfg.shouldIgnoreInInterface((String)iter.next(), false);
        }
        return res;
  }

  protected static boolean shouldIgnoreInImpl(GLUnifiedName name, GLConfiguration cfg) {
        boolean res = cfg.shouldIgnoreInImpl(name.getUni(), name.isUnique());
        for (Iterator iter = name.getOrig().iterator(); !res && iter.hasNext(); ) {
            res = cfg.shouldIgnoreInImpl((String)iter.next(), false);
        }
        return res;
  }

  protected LinkedHashMap/*<String name, DefineEntry entry>*/ defineMap = new LinkedHashMap();

  public void beginDefines() throws Exception
  {
    super.beginDefines();
  }

  /**
   * Pass-1 Unify ARB extensions with the same value
   */
  public void emitDefine(String name, String value, String optionalComment) throws Exception {
    if (cfg.allStatic() || cfg.emitInterface()) {
      DefineEntry deNew = new DefineEntry(name, value, optionalComment);
      DefineEntry deExist = (DefineEntry) defineMap.get(deNew.name.getUni());
      if(deExist!=null) {
        if(deNew.equals(deExist)) {
            if(deNew.getOptCommentString().length()>deExist.getOptCommentString().length()) {
                deExist.optionalComment=deNew.optionalComment;
            }
            deExist.addOrigName(name);
            return; // done ..
        }
        deNew.name.resetUni();
        System.err.println("WARNING: Normalized ARB entry with different value exists (keep ARB orig):"+
                           "\n\tDef: "+deExist+
                           "\n\tNew: "+deNew);
      }
      defineMap.put(deNew.name.getUni(), deNew);
    }
  }

  /**
   * Pass-2 Unify vendor extensions, 
   *        if exist as an ARB extension with the same value.
   * Pass-3 Emit all ..
   */
  public void endDefines() throws Exception
  {
    if (cfg.allStatic() || cfg.emitInterface()) {
        Iterator/*<DefineEntry>*/ deIter = null; 

        deIter = defineMap.values().iterator();
        while( deIter.hasNext() ) {
            DefineEntry de = (DefineEntry) deIter.next();
            if(de.isExtensionVEN()) {
                String extSuffix = GLExtensionNames.getExtensionSuffix(de.name.getUni(), false);
                DefineEntry deUni = (DefineEntry) de.clone();
                deUni.normalizeVEN();
                DefineEntry deExist = (DefineEntry) defineMap.get(deUni.name.getUni());
                if(null!=deExist) {
                    if(deUni.equals(deExist)) {
                        if(deUni.getOptCommentString().length()>deExist.getOptCommentString().length()) {
                            deExist.optionalComment=deUni.optionalComment;
                        }
                        deIter.remove();
                        deExist.addOrigName(de.name.getUni());
                    } else {
                        if( ((GLConfiguration)cfg).getDropUniqVendorExtensions(extSuffix) ) {
                            deIter.remove(); // remove non unified (uniq) vendor extension
                            System.err.println("INFO: Drop uniq VEN entry: "+de.name.getUni());
                        } else {
                            System.err.println("INFO: Normalized VEN entry with different value exists (keep VEN orig):"+
                                               "\n\tDef: "+deExist+
                                               "\n\tNew: "+de);
                       }
                   }
                } else if( ((GLConfiguration)cfg).getDropUniqVendorExtensions(extSuffix) ) {
                    deIter.remove(); // remove non unified (uniq) vendor extension
                    System.err.println("INFO: Drop uniq VEN entry: "+de.name.getUni());
                }
            }
        }

        deIter = defineMap.values().iterator();
        while( deIter.hasNext() ) {
            DefineEntry de = (DefineEntry) deIter.next();
            if (de.shouldIgnoreInInterface((GLConfiguration)cfg)) {
                continue;
            }
            String comment = de.getOptCommentString();
            if (comment.length() != 0) {
              javaWriter().println(comment);
            } else {
                comment = de.name.getCommentString();
                if (comment.length() != 0) {
                  de.name.resetOriginal(); // just shorten the comment space
                  javaWriter().println(comment);
                }
            }
            javaWriter().println(de.toString());
        }
    }
    defineMap.clear();

    super.endDefines();
  }

  protected JavaConfiguration createConfig() {
    return new GLConfiguration(this);
  }

  /** In order to implement Buffer Object variants of certain
      functions we generate another MethodBinding which maps the void*
      argument to a Java long. The generation of emitters then takes
      place as usual. We do however need to keep track of the modified
      MethodBinding object so that we can also modify the emitters
      later to inform them that their argument has changed. We might
      want to push this functionality down into the MethodBinding
      (i.e., mutators for argument names). We also would need to
      inform the CMethodBindingEmitter that it is overloaded in this
      case (though we default to true currently). */
  protected List/*<MethodBinding>*/ expandMethodBinding(MethodBinding binding) {
    List/*<MethodBinding>*/ bindings = super.expandMethodBinding(binding);
    
    if (!getGLConfig().isBufferObjectFunction(binding.getName())) {
      return bindings;
    }

    List/*<MethodBinding>*/ newBindings = new ArrayList();
    newBindings.addAll(bindings);

    // Need to expand each one of the generated bindings to take a
    // Java long instead of a Buffer for each void* argument
    for (Iterator iter = bindings.iterator(); iter.hasNext(); ) {
      MethodBinding cur = (MethodBinding) iter.next();
      
      // Some of these routines (glBitmap) take strongly-typed
      // primitive pointers as arguments which are expanded into
      // non-void* arguments
      // This test (rather than !signatureUsesNIO) is used to catch
      // more unexpected situations
      if (cur.signatureUsesJavaPrimitiveArrays()) {
        continue;
      }

      MethodBinding result = cur;
      for (int i = 0; i < cur.getNumArguments(); i++) {
        if (cur.getJavaArgumentType(i).isNIOBuffer()) {
          result = result.replaceJavaArgumentType(i, JavaType.createForClass(Long.TYPE));
        }
      }

      if (result == cur) {
        throw new RuntimeException("Error: didn't find any void* arguments for BufferObject function " +
                                   binding.getName());
      }

      newBindings.add(result);
      // Now need to flag this MethodBinding so that we generate the
      // correct flags in the emitters later
      bufferObjectMethodBindings.put(result, result);
    }

    return newBindings;
  }

  protected boolean needsModifiedEmitters(FunctionSymbol sym) {
    if ((!needsProcAddressWrapper(sym) && !needsBufferObjectVariant(sym)) ||
        getConfig().isUnimplemented(sym.getName())) {
      return false;
    }

    return true;
  }

  public boolean isBufferObjectMethodBinding(MethodBinding binding) {
    return bufferObjectMethodBindings.containsKey(binding);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  // map the uniName to the GLUnifiedName type
  protected HashMap/*<String uniNameStr, GLUnifiedName uniName>*/ funcNameMap = new HashMap();
  protected Map/*<String uniNameStr, GLUnifiedName uniName>*/ getFuncNameMap() {
    return funcNameMap;
  }

  protected void validateFunctionsToBind(Set/*FunctionSymbol*/ funcsSet) {
    super.validateFunctionsToBind(funcsSet);

    String localCallingConvention = ((GLConfiguration)cfg).getLocalProcAddressCallingConvention4All();
    if(null==localCallingConvention) {
        localCallingConvention="GL_APIENTRY";
    }
    ArrayList newUniFuncs = new ArrayList();
    HashSet   origFuncNames = new HashSet();
    for (Iterator iter = funcsSet.iterator(); iter.hasNext(); ) {
      FunctionSymbol fsOrig = (FunctionSymbol) iter.next();
      origFuncNames.add(fsOrig.getName());
      GLUnifiedName uniName = new GLUnifiedName(fsOrig.getName());
      if (GLEmitter.shouldIgnoreInImpl(uniName, (GLConfiguration)cfg)) {
          iter.remove(); // remove ignored function 
      } else {
          if( uniName.isExtensionARB() && 
              !((GLConfiguration)cfg).skipProcAddressGen(fsOrig.getName()) ) {
              FunctionSymbol fsUni = new FunctionSymbol(uniName.getUni(), fsOrig.getType());
              if(!funcsSet.contains(fsUni)) {
                newUniFuncs.add(fsUni); // add new uni name
                System.err.println("INFO: New ARB Normalized Function:"+
                                   "\n\tARB: "+fsOrig+
                                   "\n\tUNI: "+fsUni);
              } else {
                uniName.addOrig(uniName.getUni()); // the original name w/o extension
                System.err.println("INFO: Dub ARB Normalized Function:"+
                                   "\n\tARB: "+fsOrig+
                                   "\n\tDUB: "+fsUni);
              }
              if(!funcNameMap.containsKey(uniName.getUni())) {
                  funcNameMap.put(uniName.getUni(), uniName);
              }
              iter.remove(); // remove ARB function
              // make the function being dynamical fetched, due to it's dynamic naming scheme
              ((GLConfiguration)cfg).addForceProcAddressGen(uniName.getUni());
              // Make sure we produce the right calling convention for
              // the typedefed function pointers on Windows
              ((GLConfiguration)cfg).addLocalProcAddressCallingConvention(uniName.getUni(), localCallingConvention);
          }
      }
    }
    funcsSet.addAll(newUniFuncs);

    for (Iterator iter = funcsSet.iterator(); iter.hasNext(); ) {
      FunctionSymbol fsOrig = (FunctionSymbol) iter.next();
      GLUnifiedName uniName = new GLUnifiedName(fsOrig.getName());
      if(uniName.isExtensionVEN()) {
          uniName.normalizeVEN();
          if (GLEmitter.shouldIgnoreInImpl(uniName, (GLConfiguration)cfg)) {
              System.err.println("INFO: Ignored: Remove Function:"+ uniName);
              iter.remove(); // remove ignored function 
          } else {
              String extSuffix = GLExtensionNames.getExtensionSuffix(fsOrig.getName(), true);
              FunctionSymbol fsUni = new FunctionSymbol(uniName.getUni(), fsOrig.getType());
              if(funcsSet.contains(fsUni)) {
                  GLUnifiedName uniNameMap = (GLUnifiedName) funcNameMap.get(uniName.getUni());
                  if(null==uniNameMap) {
                    funcNameMap.put(uniName.getUni(), uniName);
                    uniNameMap=uniName;
                  } else {
                    uniNameMap.addOrig(fsOrig.getName()); // add the VEN extension name
                  }
                  if(origFuncNames.contains(uniName.getUni())) {
                      uniNameMap.addOrig(uniName.getUni()); // the original name w/o extension
                  }
                  iter.remove(); // remove VEN function (already incl. as ARB)
                  System.err.println("INFO: Dub VEN Function:"+
                                     "\n\tVEN: "+fsOrig+
                                     "\n\tDUB: "+fsUni);
              } else if( ((GLConfiguration)cfg).getDropUniqVendorExtensions(extSuffix) ) {
                iter.remove(); // remove non unified (uniq) vendor extension
                System.err.println("INFO: Drop uniq VEN Function: "+fsOrig.getName());
              }
          }
      }
    }
  }

  
  protected void generateModifiedEmitters(JavaMethodBindingEmitter baseJavaEmitter, List emitters) {
    List superEmitters = new ArrayList();
    super.generateModifiedEmitters(baseJavaEmitter, superEmitters);

    // See whether this is one of the Buffer Object variants
    boolean bufferObjectVariant = bufferObjectMethodBindings.containsKey(baseJavaEmitter.getBinding());

    for (Iterator iter = superEmitters.iterator(); iter.hasNext(); ) {
        JavaMethodBindingEmitter emitter = (JavaMethodBindingEmitter) iter.next();
        if (emitter instanceof ProcAddressJavaMethodBindingEmitter) {
          emitters.add(new GLJavaMethodBindingEmitter((ProcAddressJavaMethodBindingEmitter) emitter, this, bufferObjectVariant));
        } else {
          emitters.add(emitter);
        }
    }
  }

  protected boolean needsBufferObjectVariant(FunctionSymbol sym) {
    return getGLConfig().isBufferObjectFunction(sym.getName());
  }
  
  protected GLConfiguration getGLConfig() {
    return (GLConfiguration) getConfig();
  }

  protected void endProcAddressTable() throws Exception
  {
    PrintWriter w = tableWriter;

    w.println("  /**");
    w.println("   * This is a convenience method to get (by name) the native function");
    w.println("   * pointer for a given function. It lets you avoid having to");
    w.println("   * manually compute the &quot;" + PROCADDRESS_VAR_PREFIX + " + ");
    w.println("   * &lt;functionName&gt;&quot; member variable name and look it up via");
    w.println("   * reflection; it also will throw an exception if you try to get the");
    w.println("   * address of an unknown function, or one that is statically linked");
    w.println("   * and therefore does not have a function pointer in this table.");
    w.println("   *");
    w.println("   * @throws RuntimeException if the function pointer was not found in");
    w.println("   *   this table, either because the function was unknown or because");
    w.println("   *   it was statically linked.");
    w.println("   */");
    w.println("  public long getAddressFor(String functionNameUsr) {");
    w.println("    String functionNameBase = com.sun.gluegen.runtime.opengl.GLExtensionNames.normalizeVEN(com.sun.gluegen.runtime.opengl.GLExtensionNames.normalizeARB(functionNameUsr, true), true);");
    w.println("    String addressFieldNameBase = " + getProcAddressConfig().gluegenRuntimePackage() + ".ProcAddressHelper.PROCADDRESS_VAR_PREFIX + functionNameBase;");
    w.println("    java.lang.reflect.Field addressField = null;");
    w.println("    int  funcNamePermNum = com.sun.gluegen.runtime.opengl.GLExtensionNames.getFuncNamePermutationNumber(functionNameBase);");
    w.println("    for(int i = 0; null==addressField && i < funcNamePermNum; i++) {");
    w.println("        String addressFieldName = com.sun.gluegen.runtime.opengl.GLExtensionNames.getFuncNamePermutation(addressFieldNameBase, i);");
    w.println("        try {");
    w.println("          addressField = getClass().getField(addressFieldName);");
    w.println("        } catch (Exception e) { }");
    w.println("    }");
    w.println("");
    w.println("    if(null==addressField) {");
    w.println("      // The user is calling a bogus function or one which is not");
    w.println("      // runtime linked");
    w.println("      throw new RuntimeException(");
    w.println("          \"WARNING: Address field query failed for \\\"\" + functionNameBase + \"\\\"/\\\"\" + functionNameUsr +");
    w.println("          \"\\\"; it's either statically linked or address field is not a known \" +");
    w.println("          \"function\");");
    w.println("    } ");
    w.println("    try {");
    w.println("      return addressField.getLong(this);");
    w.println("    } catch (Exception e) {");
    w.println("      throw new RuntimeException(");
    w.println("          \"WARNING: Address query failed for \\\"\" + functionNameBase + \"\\\"/\\\"\" + functionNameUsr +");
    w.println("          \"\\\"; it's either statically linked or is not a known \" +");
    w.println("          \"function\", e);");
    w.println("    }");
    w.println("  }");

    w.println("} // end of class " + tableClassName);
    w.flush();
    w.close();
  }

  protected void emitProcAddressTableEntryForSymbol(FunctionSymbol cFunc)
  {
    emitProcAddressTableEntryForString(cFunc.getName());
  }

  protected void emitProcAddressTableEntryForString(String str)
  {
    // Deal gracefully with forced proc address generation in the face
    // of having the function pointer typedef in the header file too
    if (emittedTableEntries.contains(str))
      return;
    emittedTableEntries.add(str);
    tableWriter.print("  public long ");
    tableWriter.print(PROCADDRESS_VAR_PREFIX);
    tableWriter.print(str);
    tableWriter.println(";");
  }

}
