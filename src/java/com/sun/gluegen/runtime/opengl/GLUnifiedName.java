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
 */

package com.sun.gluegen.runtime.opengl;

import java.util.*;
import com.sun.gluegen.runtime.*;

public class GLUnifiedName implements Cloneable {
    //GL_XYZ : GL_XYZ, GL_XYZ_GL2, GL_XYZ_ARB, GL_XYZ_OES, GL_XYZ_OML
    //GL_XYZ : GL_XYZ, GL_GL2_XYZ, GL_ARB_XYZ, GL_OES_XYZ, GL_OML_XYZ
    //
    // Pass-1 Unify ARB extensions with the same value
    // Pass-2 Unify vendor extensions, 
    //        if exist as an ARB extension with the same value.
    // Pass-3 Emit

    public static final String[] extensionsARB = { "GL2", "ARB", "OES",  "OML" };
    public static final String[] extensionsVEN = { "EXT", "SGI", "SGIS", "SGIX", "NV", "AMD", "ATI", "HP", "IBM", "WIN" };

    public static final boolean isExtension(String[] extensions, String str) {
        for(int i = extensions.length - 1 ; i>=0 ; i--) {
            if(str.endsWith(extensions[i])) {
                return true;
            }
        }
        return false;
    }

    public static final String normalize(String[] extensions, String str) {
        boolean touched = false;
        for(int i = extensions.length - 1 ; !touched && i>=0 ; i--) {
            if(str.endsWith("_"+extensions[i])) {
                // enums
                str = str.substring(0, str.length()-1-extensions[i].length());
                touched=true;
            } else if(str.endsWith(extensions[i])) {
                // functions
                str = str.substring(0, str.length()-extensions[i].length());
                touched=true;
            }
        }
        return str;
    }
    public static final String normalizeARB(String str) {
        return normalize(extensionsARB, str);
    }
    public static final boolean isExtensionARB(String str) {
        return isExtension(extensionsARB, str);
    }
    public static final String normalizeVEN(String str) {
        return normalize(extensionsVEN, str);
    }
    public static final boolean isExtensionVEN(String str) {
        return isExtension(extensionsVEN, str);
    }

    public static final int getNamePermutationNumber(String name) {
        if(isExtensionARB(name) || isExtensionVEN(name)) {
            // no name permutation, if it's already a known extension
            return 1;
        }
        return 1 + extensionsARB.length + extensionsVEN.length;
    }

    public static final String getNamePermutation(String name, int i) {
        // identity
        if(i==0) {
            return name;
        }
        if(0>i || i>=(1+extensionsARB.length + extensionsVEN.length)) {
            throw new RuntimeException("Index out of range [0.."+(1+extensionsARB.length+extensionsVEN.length-1)+"]: "+i);
        }
        // ARB
        i-=1;
        if(i<extensionsARB.length) {
            return name+extensionsARB[i];
        }
        // VEN
        i-=extensionsARB.length;
        return name+extensionsVEN[i];
    }

    /** 
     */

    public GLUnifiedName(String name) {
        this(name, normalizeARB(name));
    }

    protected GLUnifiedName(String orig, String uni) {
        this.nameOrig=new ArrayList();
        this.nameOrig.add(orig);
        this.nameUni=uni;
    }

    protected GLUnifiedName(List origs, String uni) {
        this.nameOrig=new ArrayList();
        this.nameOrig.addAll(origs);
        this.nameUni=uni;
    }

    public void resetUni() {
        nameUni=(String)nameOrig.get(0);
    }
    public void resetOriginal() {
        nameOrig.clear();
        nameOrig.add(nameUni);
    }

    public void addOrig(String name) {
        if(!nameOrig.contains(name)) {
            nameOrig.add(name);
        }
    }

    public boolean isExtensionARB() {
        boolean res = false;
        for (Iterator iter = nameOrig.iterator(); !res && iter.hasNext(); ) {
            res = isExtensionARB((String)iter.next());
        }
        return res;
    }

    public void normalizeVEN() {
        nameUni=normalizeVEN(nameUni);
    }

    public boolean isExtensionVEN() {
        boolean res = false;
        for (Iterator iter = nameOrig.iterator(); !res && iter.hasNext(); ) {
            res = isExtensionVEN((String)iter.next());
        }
        return res;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
          return true;
        }
        if(null==obj || !(obj instanceof GLUnifiedName)) return false;
        GLUnifiedName uen = (GLUnifiedName) obj;
        return nameUni.equals(uen.nameUni);
    }

    public String getCommentString() {
        return getCommentString(true, " ");
    }
    public String getCommentString(boolean encloseCommentStartEnd, String seperator) {
        if(nameOrig.size()==1 && ((String)nameOrig.get(0)).equals(nameUni)) {
            return new String();
        }
        String res = new String();
        if(encloseCommentStartEnd) {
            res = res.concat(" /** ");
        }
        res = res.concat("Alias for: <code>");
        res = res.concat(getOrigStringList(seperator));
        res = res.concat("</code> ");
        if(encloseCommentStartEnd) {
            res = res.concat("*/");
        }
        return res;
    }
    public String getOrigStringList(String seperator) {
        String res = new String();
        for (Iterator iter = nameOrig.iterator(); iter.hasNext(); ) {
            res = res.concat((String)iter.next());
            if(iter.hasNext()) {
                res = res.concat(seperator);
            }
        }
        return res;
    }

    public String toString() {
        if(nameOrig.size()==1 && ((String)nameOrig.get(0)).equals(nameUni)) {
            return nameUni;
        }
        String res = nameUni + " /* " ;
        for (Iterator iter = nameOrig.iterator(); iter.hasNext(); ) {
            res = res.concat((String)iter.next()+", ");
        }
        res = res.concat(" */");
        return res;
    }

    public Object clone() {
        return new GLUnifiedName(nameOrig, nameUni);
    }

    public String getUni() { return nameUni; }
    public List   getOrig() { return nameOrig; }
    public String getOrig(int i) { return (String)nameOrig.get(i); }

    private List   nameOrig;
    private String nameUni;
}

