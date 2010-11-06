/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.jogamp.gluegen;

import java.util.*;

/** Represents the definition of a constant which was provided either
    via a #define statement or through an enum definition. */
public class ConstantDefinition {

    private String origName;
    private HashSet<String> aliasedNames;
    private String name;
    private String value;
    private boolean isEnum;
    private String enumName;
    private Set<String> aliases;

    public ConstantDefinition(String name,
                              String value,
                              boolean isEnum,
                              String enumName) {
        this.origName = name;
        this.name = name;
        this.value = value;
        this.isEnum = isEnum;
        this.enumName = enumName;
        this.aliasedNames=new HashSet<String>();
    }

    public boolean equals(ConstantDefinition other) {
        return (equals(name, other.name) &&
                equals(value, other.value) &&
                equals(enumName, other.enumName));
    }

    private boolean equals(String s1, String s2) {
        if (s1 == null || s2 == null) {
            if (s1 == null && s2 == null) {
                return true;
            }
            return false;
        }

        return s1.equals(s2);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /** Supports renaming in Java binding. */
    public void rename(String name) {
      if(null!=name) {
          this.name = name;
          aliasedNames.add(origName);
      }
    }

    public void           addAliasedName(String name) {
        aliasedNames.add(name);
    }
    public Collection<String> getAliasedNames() {
        return aliasedNames;
    }

    public String getOrigName() { 
        return origName;     
    }

    public String getName() { 
        return name;     
    }

    public String getValue()    { return value;    }
    /** Returns null if this definition was not part of an
        enumeration, or if the enum was anonymous. */
    public String getEnumName() { return enumName; }

    public boolean isEnum() { return isEnum; }

    public Set<String> getAliases() {
        return aliases;
    }

    public void addAlias(String alias) {
        if (aliases == null) {
            aliases = new LinkedHashSet<String>();
        }
        aliases.add(alias);
    }

    @Override
    public String toString() {
        return "ConstantDefinition [name " + name + " origName " + origName + " value " + value
                + " aliasedNames " + aliasedNames + " aliases " + aliases
                + " enumName " + enumName + " isEnum " + isEnum + "]";
    }
    
}
