/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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
package com.jogamp.gluegen.cgram.types;

import java.util.HashSet;
import java.util.Set;

/**
 * Supports symbol aliasing, i.e. renaming,
 * while preserving all its original names, i.e. aliases.
 */
public interface AliasedSymbol {
    /**
     * Rename this symbol with the given {@code newName} if not equal {@link #getName() current-name}.
     * <p>
     * Before renaming the {@link #getName() current-name} will be added
     * to the list of {@link #getAliasedNames() aliases}.
     * while the given {@code newName} will be removed.
     * </p>
     * <p>
     * Operation will be ignored if {@code newName} is {@code null}.
     * </p>
     * @param newName the new {@link #getName() current-name}, maybe {@code null}
     */
    void rename(final String newName);
    /**
     * Add the given {@code origName} to the list of {@link #getAliasedNames() aliases}
     * if not equal {@link #getName() current-name}.
     * <p>
     * Operation will be ignored if {@code newName} is {@code null}.
     * </p>
     * @param origName the new alias to be added, maybe {@code null}
     */
    void addAliasedName(final String origName);
    /**
     *
     * Returns {@code true} if this symbol has aliases, i.e. either being {@link #rename(String) renamed}
     * or {@link #addAliasedName(String) aliases-added}.
     * <p>
     * Otherwise {@code false} is being returned.
     * </p>
     */
    boolean hasAliases();
    /**
     * Return all aliases for this symbol, i.e. original names, for this symbol.
     * <p>
     * Inclusive {@link #getOrigName() original-name}, if {@link #rename(String) renamed},
     * </p>
     * <p>
     * Exclusive {@link #getName() current-name}.
     * </p>
     * <p>
     * May return {@code null} or a zero sized {@link Set} for no aliases.
     * </p>
     */
    Set<String> getAliasedNames();
    /**
     * Return the original-name as set at creation.
     */
    String getOrigName();
    /**
     * Return the current-name, which is the last {@link #rename(String) renamed-name} if issued,
     * or the {@link #getOrigName() original-name}.
     */
    String getName();
    /**
     * Return this object's {@link #toString()} wrapped w/ the {@link #getName() current-name}
     * and all {@link #getAliasedNames() aliases}.
     */
    String getAliasedString();

    public static class AliasedSymbolImpl implements AliasedSymbol {
        private final String origName;
        private final HashSet<String> aliasedNames;
        private String name;

        public AliasedSymbolImpl(final String origName) {
            if( null == origName ) {
                throw new IllegalArgumentException("Null origName not allowed");
            }
            this.origName = origName;
            this.aliasedNames=new HashSet<String>();
            this.name = origName;
        }
        public AliasedSymbolImpl(final AliasedSymbolImpl o) {
            this.origName = o.origName;
            this.aliasedNames = new HashSet<String>(o.aliasedNames);
            this.name = o.name;
        }
        @Override
        public void rename(final String newName) {
            if( null != newName && !name.equals(newName) ) {
                aliasedNames.add(name);
                aliasedNames.remove(newName);
                name = newName;
            }
        }
        @Override
        public void addAliasedName(final String origName) {
            if( null != origName && !name.equals(origName) ) {
                aliasedNames.add(origName);
            }
        }
        @Override
        public boolean hasAliases() {
            return aliasedNames.size() > 0;
        }
        @Override
        public Set<String> getAliasedNames() {
            return aliasedNames;
        }
        @Override
        public String getOrigName() {
            return origName;
        }
        @Override
        public String getName() {
            return name;
        }
        @Override
        public String getAliasedString() {
            return "["+name+", aliases "+aliasedNames.toString()+", "+toString()+"]";
        }
    }
    public static class NoneAliasedSymbol implements AliasedSymbol {
        private final String name;

        public NoneAliasedSymbol(final String origName) {
            this.name = origName;
        }
        @Override
        public void rename(final String newName) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void addAliasedName(final String origName) {
            throw new UnsupportedOperationException();
        }
        @Override
        public boolean hasAliases() {
            return false;
        }
        @Override
        public Set<String> getAliasedNames() {
            return null;
        }
        @Override
        public String getOrigName() {
            return name;
        }
        @Override
        public String getName() {
            return name;
        }
        @Override
        public String getAliasedString() {
            return toString();
        }
    }
}
