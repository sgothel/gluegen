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

import java.util.List;

public class TypeComparator {
    /**
     * Supports semantic equality and hash functions for types.
     */
    public static interface SemanticEqualityOp {
        /**
         * Semantic hashcode for Types exclusive its given {@link #getName() name}.
         * @see #equalSemantics(SemanticEqualityOp)
         */
        int hashCodeSemantics();

        /**
         * Semantic equality test for Types exclusive its given {@link #getName() name}.
         * @see #hashCodeSemantics()
         */
        boolean equalSemantics(final SemanticEqualityOp arg);
    }
    /**
     * Supports common interface for {@link SemanticEqualityOp} and {@link AliasedSymbol}.
     */
    public static interface AliasedSemanticSymbol extends AliasedSymbol, SemanticEqualityOp { };

    /** Helper routine for list equality comparison*/
    static <C> boolean listsEqual(final List<C> a, final List<C> b) {
        if( a == null ) {
            if( null != b ) {
                return false;
            } else {
                return true; // elements equal, i.e. both null
            }
        }
        if( b != null && a.size() == b.size() ) {
            final int count = a.size();
            for(int i=0; i<count; i++) {
                final C ac = a.get(i);
                final C bc = b.get(i);
                if( null == ac ) {
                    if( null != bc ) {
                        return false;
                    } else {
                        continue; // elements equal, i.e. both null
                    }
                }
                if( !ac.equals(bc) ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** Helper routine for list hashCode */
    static <C extends SemanticEqualityOp> int listsHashCode(final List<C> a) {
        if( a == null ) {
            return 0;
        } else {
            final int count = a.size();
            int hash = 31;
            for(int i=0; i<count; i++) {
                final C ac = a.get(i);
                hash = ((hash << 5) - hash) + ( null != ac ? ac.hashCode() : 0 );
            }
            return hash;
        }
    }

    /** Helper routine for list semantic equality comparison*/
    static <C extends SemanticEqualityOp> boolean listsEqualSemantics(final List<C> a, final List<C> b) {
        if( a == null ) {
            if( null != b ) {
                return false;
            } else {
                return true; // elements equal, i.e. both null
            }
        }
        if( b != null && a.size() == b.size() ) {
            final int count = a.size();
            for(int i=0; i<count; i++) {
                final C ac = a.get(i);
                final C bc = b.get(i);
                if( null == ac ) {
                    if( null != bc ) {
                        return false;
                    } else {
                        continue; // elements equal, i.e. both null
                    }
                }
                if( !ac.equalSemantics(bc) ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** Helper routine for list hashCode */
    static <C extends SemanticEqualityOp> int listsHashCodeSemantics(final List<C> a) {
        if( a == null ) {
            return 0;
        } else {
            final int count = a.size();
            int hash = 31;
            for(int i=0; i<count; i++) {
                final C ac = a.get(i);
                hash = ((hash << 5) - hash) + ( null != ac ? ac.hashCodeSemantics() : 0 );
            }
            return hash;
        }
    }
}
