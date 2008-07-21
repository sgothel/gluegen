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

package com.sun.gluegen;

import java.util.*;

public class UnifiedName implements Cloneable {

    public UnifiedName(String name) {
        nameUni=name;
        nameList=new ArrayList();
        nameSet=new HashSet();
        add(name);
    }

    protected UnifiedName(UnifiedName un) {
        nameUni=un.nameUni;
        nameList=new ArrayList(un.nameList);
        nameSet=new HashSet(un.nameSet);
    }

    public void reset() {
        resetUni();
        resetOriginal();
    }

    public void resetUni() {
        nameSet.remove(nameUni);
        nameUni=(String)nameList.get(0);
    }

    public void resetOriginal() {
        nameList.clear();
        nameSet.clear();
        add(nameUni);
    }

    public void setUni(String name) {
        if(!nameUni.equals(name)) {
            nameUni=name;
            add(name);
        }
    }

    /**
     * unique in case this name reflects only one
     * original entry (no extension unification)
     */
    public boolean isUnique() {
        return nameSet.size()==1;
    }

    public void add(String name) {
        if (nameSet.add(name)) {
            nameList.add(name);
        }
    }
    public void addAll(Collection col) {
        for (Iterator iter = col.iterator(); iter.hasNext(); ) {
            Object obj = iter.next();
            if( obj instanceof String ) {
                add((String)obj);
            } else {
                throw new ClassCastException("not a String: "+obj);
            }
        }
        
    }

    public boolean contains(UnifiedName un) {
        boolean res = contains(un.nameUni);
        for (Iterator iter = un.nameList.iterator(); !res && iter.hasNext(); ) {
            res = contains((String)iter.next());
        }
        return res;
    }

    public boolean contains(String name) {
        return nameSet.contains(name);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
          return true;
        }
        if(obj instanceof UnifiedName) {
            UnifiedName un = (UnifiedName)obj;
            return nameUni.equals(un.nameUni) && nameSet.equals(un.nameSet);
        }
        return false;
    }

    public Object clone() {
        return new UnifiedName(this);
    }

    public int hashCode() {
        return nameSet.hashCode();
    }

    public String getUni() { return nameUni; }
    public List   getNameList() { return nameList; }
    public Set    getNameSet() { return nameSet; }

    public int size() { return nameList.size(); }
    public String get(int i) { return (String)nameList.get(i); }

    public void remapAllNames(Map map) {
        List allNames = new ArrayList();
        // 1st pass: collect all other x-mappings to this one
        for (Iterator iter = nameList.iterator(); iter.hasNext(); ) {
            UnifiedName un = (UnifiedName) map.get((String)iter.next());
            if(null!=un && this!=un) {
                allNames.addAll(un.getNameList());
            }
        }
        addAll(allNames);

        // 2nd pass: map all containing names
        for (Iterator iter = nameList.iterator(); iter.hasNext(); ) {
            map.put((String)iter.next(), this);
        }
    }

    public static UnifiedName getOrPut(Map map, String name) {
        UnifiedName un = (UnifiedName)map.get(name);
        if(null==un) {
            un = new UnifiedName(name);
            un.remapAllNames(map);
        }
        return un;
    }

    public String getCommentString() {
        return getCommentString(true, " ");
    }
    public String getCommentString(boolean encloseCommentStartEnd, String seperator) {
        if(nameList.size()==1 && ((String)nameList.get(0)).equals(nameUni)) {
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
        for (Iterator iter = nameList.iterator(); iter.hasNext(); ) {
            res = res.concat((String)iter.next());
            if(iter.hasNext()) {
                res = res.concat(seperator);
            }
        }
        return res;
    }

    public String toString() {
        if(nameList.size()==1 && ((String)nameList.get(0)).equals(nameUni)) {
            return nameUni;
        }
        String res = nameUni + " /* " ;
        for (Iterator iter = nameList.iterator(); iter.hasNext(); ) {
            res = res.concat((String)iter.next()+", ");
        }
        res = res.concat(" */");
        return res;
    }

    protected String nameUni;
    protected List   nameList;
    protected HashSet nameSet;

}

