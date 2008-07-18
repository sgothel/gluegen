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

package com.sun.gluegen.opengl;

import java.util.*;
import com.sun.gluegen.runtime.*;
import com.sun.gluegen.runtime.opengl.GLExtensionNames;

public class GLUnifiedName implements Cloneable {

    public GLUnifiedName(String name) {
        isGLFunc = GLExtensionNames.isGLFunction(name);
        isGLEnum = GLExtensionNames.isGLEnumeration(name);
        if(!isGLFunc && !isGLEnum) {
            nameUni=name;
        } else {
            nameUni=GLExtensionNames.normalizeARB(name, isGLFunc);
        }
        this.nameOrig=new ArrayList();
        this.nameOrig.add(name);
    }

    protected GLUnifiedName(List origs, String uni, boolean isGLFunc, boolean isGLEnum) {
        this.nameOrig=new ArrayList();
        this.nameOrig.addAll(origs);
        this.nameUni=uni;
        this.isGLFunc=isGLFunc;
        this.isGLEnum=isGLEnum;
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

    /**
     * unique in case this name reflects only one
     * original entry (no extension unification)
     */
    public boolean isUnique() {
        return nameOrig.size()==1;
    }

    public boolean isExtensionARB() {
        boolean res = false;
        if(isGLFunc||isGLEnum) {
            for (Iterator iter = nameOrig.iterator(); !res && iter.hasNext(); ) {
                res = GLExtensionNames.isExtensionARB((String)iter.next(), isGLFunc);
            }
        }
        return res;
    }

    public void normalizeVEN() {
        if(isGLFunc||isGLEnum) {
            nameUni=GLExtensionNames.normalizeVEN(nameUni, isGLFunc);
        }
    }

    public boolean isExtensionVEN() {
        boolean res = false;
        if(isGLFunc||isGLEnum) {
            for (Iterator iter = nameOrig.iterator(); !res && iter.hasNext(); ) {
                res = GLExtensionNames.isExtensionVEN((String)iter.next(), isGLFunc);
            }
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
        return new GLUnifiedName(nameOrig, nameUni, isGLFunc, isGLEnum);
    }

    public String getUni() { return nameUni; }
    public List   getOrig() { return nameOrig; }
    public String getOrig(int i) { return (String)nameOrig.get(i); }

    private List   nameOrig;
    private String nameUni;
    private boolean isGLFunc, isGLEnum;
}

