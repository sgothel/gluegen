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

  static class UnifiedExtensionName implements Cloneable {
    public UnifiedExtensionName(String name) {
        this(name, normalizeARB(name));
    }

    protected UnifiedExtensionName(String orig, String uni) {
        this.nameOrig=orig;
        this.nameUni=uni;
    }

    public void useOriginal() {
        nameUni=nameOrig;
    }

    public void addOrig(String name) {
        if(nameOrig.indexOf(name)<0) {
            nameOrig = nameOrig.concat(", "+name);
        }
    }

    public void normalizeVEN() {
        nameUni=normalizeVEN(nameUni);
    }

    public boolean isExtensionVEN() {
        return isExtensionVEN(nameUni);
    }

    public boolean equals(Object obj) {
        if(null==obj || !(obj instanceof UnifiedExtensionName)) return false;
        UnifiedExtensionName uen = (UnifiedExtensionName) obj;
        return nameUni.equals(uen.nameUni);
    }

    public String getCommentString() {
        if(nameOrig.equals(nameUni)) {
            return new String();
        }
        return " /** " + nameUni + ": Alias of: " + nameOrig + " */";
    }

    public String toString() {
        if(nameOrig.equals(nameUni)) {
            return nameUni;
        }
        return nameUni + " /* " + nameOrig + " */";
    }

    public Object clone() {
        return new UnifiedExtensionName(nameOrig, nameUni);
    }

    //GL_XYZ : GL_XYZ, GL_XYZ_GL2, GL_XYZ_ARB, GL_XYZ_OES, GL_XYZ_OML
    //GL_XYZ : GL_XYZ, GL_GL2_XYZ, GL_ARB_XYZ, GL_OES_XYZ, GL_OML_XYZ
    //
    // Pass-1 Unify ARB extensions with the same value
    // Pass-2 Unify vendor extensions, 
    //        if exist as an ARB extension with the same value.
    // Pass-3 Emit

    public static String[] extensionsARB = { "GL2", "ARB", "OES", "OML" };
    public static String[] extensionsVEN = { "EXT", "NV", "ATI", "SGI", "SGIS", "SGIX", "HP", "IBM", "WIN" };

    public static boolean isExtension(String[] extensions, String str) {
        for(int i = extensions.length - 1 ; i>=0 ; i--) {
            if(str.endsWith("_"+extensions[i])) {
                return true;
            }
            if(str.startsWith("GL_"+extensions[i]+"_")) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String[] extensions, String str) {
        boolean touched = false;
        for(int i = extensions.length - 1 ; !touched && i>=0 ; i--) {
            if(str.endsWith("_"+extensions[i])) {
                str = str.substring(0, str.length()-1-extensions[i].length());
                touched=true;
            }
            if(str.startsWith("GL_"+extensions[i]+"_")) {
                str = "GL_"+str.substring(4+extensions[i].length());
                touched=true;
            }
        }
        return str;
    }
    public static String normalizeARB(String str) {
        return normalize(extensionsARB, str);
    }
    public static String normalizeVEN(String str) {
        return normalize(extensionsVEN, str);
    }
    public static boolean isExtensionVEN(String str) {
        return isExtension(extensionsVEN, str);
    }

    protected String nameOrig;
    protected String nameUni;
  }

  static class DefineEntry implements Cloneable {
    public DefineEntry(String namestr, String valuestr, String optionalComment) {
        this.name=new UnifiedExtensionName(namestr);
        this.value=getJavaValue(namestr, valuestr);
        this.type=getJavaType(namestr, this.value);
        this.radix=getJavaRadix(namestr, valuestr);
        this.optionalComment=optionalComment;
    }

    protected DefineEntry(UnifiedExtensionName name, String type, Object value, int radix, String optionalComment) {
        this.name=name;
        this.value=value;
        this.type=type;
        this.radix=radix;
        this.optionalComment=optionalComment;
    }

    public Object clone() {
        return new DefineEntry((UnifiedExtensionName)name.clone(), type, value, radix, optionalComment);
    }

    public boolean equals(Object obj) {
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

    protected UnifiedExtensionName name;
    protected Object value;
    protected String type;
    protected int radix;
    protected String optionalComment;
  }

  protected LinkedHashMap/*<String name, DefineEntry entry>*/ defineMap = new LinkedHashMap();

  public void beginDefines() throws Exception
  {
    super.beginDefines();
  }

  /**
   * Pass-1 Unify ARB extensions with the same value
   */
  public void emitDefine(String name, String value, String optionalComment) throws Exception
  {
    if (cfg.allStatic() || cfg.emitInterface()) {
      if (!cfg.shouldIgnore(name)) {
        DefineEntry deNew = new DefineEntry(name, value, optionalComment);
        DefineEntry deExist = (DefineEntry) defineMap.get(deNew.name.nameUni);
        if(deExist!=null) {
            if(deNew.equals(deExist)) {
                if(deNew.getOptCommentString().length()>deExist.getOptCommentString().length()) {
                    deExist.optionalComment=deNew.optionalComment;
                }
                deExist.addOrigName(deNew.name.nameOrig);
                return; // done ..
            }
            deNew.name.useOriginal();
            System.err.println("WARNING: Normalized entry with different value exists:"+
                               "\n\tDef: "+deExist+
                               "\n\tNew: "+deNew+
                               "\n\t using original ARB entry");
        }
        defineMap.put(deNew.name.nameUni, deNew);
      }
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
                DefineEntry deUni = (DefineEntry) de.clone();
                deUni.normalizeVEN();
                DefineEntry deExist = (DefineEntry) defineMap.get(deUni.name.nameUni);
                if(null!=deExist) {
                    if(deUni.equals(deExist)) {
                        if(deUni.getOptCommentString().length()>deExist.getOptCommentString().length()) {
                            deExist.optionalComment=deUni.optionalComment;
                        }
                        deIter.remove();
                        deExist.addOrigName(deUni.name.nameOrig);
                    } else {
                        System.err.println("INFO: Normalized entry with different value exists:"+
                                           "\n\tDef: "+deExist+
                                           "\n\tNew: "+de+
                                           "\n\t using original vendor entry");
                   }
                }
            }
        }

        deIter = defineMap.values().iterator();
        while( deIter.hasNext() ) {
            DefineEntry de = (DefineEntry) deIter.next();
            String comment = de.getOptCommentString();
            if (comment.length() != 0) {
              javaWriter().println(comment);
            } else {
                comment = de.name.getCommentString();
                if (comment.length() != 0) {
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
  
  protected void generateModifiedEmitters(JavaMethodBindingEmitter baseJavaEmitter, List emitters) {
    List superEmitters = new ArrayList();
    super.generateModifiedEmitters(baseJavaEmitter, superEmitters);

    // See whether this is one of the Buffer Object variants
    boolean bufferObjectVariant = bufferObjectMethodBindings.containsKey(baseJavaEmitter.getBinding());

    if (bufferObjectVariant) {
      for (Iterator iter = superEmitters.iterator(); iter.hasNext(); ) {
        JavaMethodBindingEmitter emitter = (JavaMethodBindingEmitter) iter.next();
        if (emitter instanceof ProcAddressJavaMethodBindingEmitter) {
          emitters.add(new GLJavaMethodBindingEmitter((ProcAddressJavaMethodBindingEmitter) emitter, bufferObjectVariant));
        } else {
          emitters.add(emitter);
        }
      }
    } else {
      emitters.addAll(superEmitters);
    }
  }

  protected boolean needsBufferObjectVariant(FunctionSymbol sym) {
    return getGLConfig().isBufferObjectFunction(sym.getName());
  }
  
  protected GLConfiguration getGLConfig() {
    return (GLConfiguration) getConfig();
  }
}
