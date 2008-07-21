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
import com.sun.gluegen.UnifiedName;
import com.sun.gluegen.runtime.opengl.GLExtensionNames;

public class GLUnifiedName extends UnifiedName implements Cloneable {

    public GLUnifiedName(String name) {
        super(name);
        isGLFunc = GLExtensionNames.isGLFunction(name);
        isGLEnum = GLExtensionNames.isGLEnumeration(name);
        if(isGLFunc || isGLEnum) {
            setUni(GLExtensionNames.normalizeARB(name, isGLFunc));
        }
    }

    protected GLUnifiedName(GLUnifiedName un) {
        super(un);
        this.isGLFunc=un.isGLFunc;
        this.isGLEnum=un.isGLEnum;
    }

    public boolean isExtensionARB() {
        boolean res = false;
        if(isGLFunc||isGLEnum) {
            res = GLExtensionNames.isExtensionARB(nameUni, isGLFunc);
            for (Iterator iter = nameList.iterator(); !res && iter.hasNext(); ) {
                res = GLExtensionNames.isExtensionARB((String)iter.next(), isGLFunc);
            }
        }
        return res;
    }

    public void normalizeVEN() {
        if(isGLFunc||isGLEnum) {
            setUni(GLExtensionNames.normalizeVEN(nameUni, isGLFunc));
        }
    }

    public boolean isExtensionVEN() {
        boolean res = false;
        if(isGLFunc||isGLEnum) {
            res = GLExtensionNames.isExtensionVEN(nameUni, isGLFunc);
            for (Iterator iter = nameList.iterator(); !res && iter.hasNext(); ) {
                res = GLExtensionNames.isExtensionVEN((String)iter.next(), isGLFunc);
            }
        }
        return res;
    }

    public boolean isExtension() {
        boolean res = false;
        if(isGLFunc||isGLEnum) {
            res = GLExtensionNames.isExtension(nameUni, isGLFunc);
            for (Iterator iter = nameList.iterator(); !res && iter.hasNext(); ) {
                res = GLExtensionNames.isExtension((String)iter.next(), isGLFunc);
            }
        }
        return res;
    }

    public static UnifiedName getOrPut(Map map, String name) {
        GLUnifiedName un = (GLUnifiedName)map.get(name);
        if(null==un) {
            un = new GLUnifiedName(name);
            un.remapAllNames(map);
        }
        return un;
    }

    public Object clone() {
        return new GLUnifiedName(this);
    }

    protected boolean isGLFunc, isGLEnum;
}

