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

import com.jogamp.gluegen.ASTLocusTag.ASTLocusTagProvider;
import com.jogamp.gluegen.cgram.types.AliasedSymbol.AliasedSymbolImpl;
import com.jogamp.gluegen.cgram.types.TypeComparator.AliasedSemanticSymbol;
import com.jogamp.gluegen.cgram.types.TypeComparator.SemanticEqualityOp;

/** Represents the definition of a constant which was provided either
    via a #define statement or through an enum definition. */
public class ConstantDefinition extends AliasedSymbolImpl implements AliasedSemanticSymbol, ASTLocusTagProvider {
    private final boolean relaxedEqSem;
    private final String sValue;
    private final long iValue;
    private final boolean hasIntValue;
    private final boolean isEnum;
    private final String enumName;
    private final ASTLocusTag astLocus;

    /** Covering enums  */
    public ConstantDefinition(final String name,
                              final long value,
                              final String enumName,
                              final ASTLocusTag astLocus) {
        super(name);
        this.relaxedEqSem = TypeConfig.relaxedEqualSemanticsTest();
        this.sValue = String.valueOf(value);
        this.iValue = value;
        this.hasIntValue = true;
        this.isEnum = true;
        this.enumName = enumName;
        this.astLocus = astLocus;
    }

    /** Covering defines */
    public ConstantDefinition(final String name,
                              final String value,
                              final ASTLocusTag astLocus) {
        super(name);
        this.relaxedEqSem = TypeConfig.relaxedEqualSemanticsTest();
        this.sValue = value;
        {
            // Attempt to parse define string as number
            long v;
            boolean b;
            try {
                v = Long.decode(value).longValue();
                b = true;
            } catch (final NumberFormatException e) {
                v = 0;
                b = false;
            }
            this.iValue = v;
            this.hasIntValue = b;
        }
        this.isEnum = false;
        this.enumName = null;
        this.astLocus = astLocus;
    }

    @Override
    public ASTLocusTag getASTLocusTag() { return astLocus; }

    /**
     * Hash by its given {@link #getName() name}.
     */
    @Override
    public final int hashCode() {
        return getName().hashCode();
    }

    /**
     * Equality test by its given {@link #getName() name}.
     */
    @Override
    public final boolean equals(final Object arg) {
        if (arg == this) {
            return true;
        } else  if ( !(arg instanceof ConstantDefinition) ) {
            return false;
        } else {
            final ConstantDefinition t = (ConstantDefinition)arg;
            return equals(getName(), t.getName());
        }
    }

    @Override
    public final int hashCodeSemantics() {
        // 31 * x == (x << 5) - x
        int hash = 31 + ( null != getName() ? getName().hashCode() : 0 );
        hash = ((hash << 5) - hash) + ( null != sValue ? sValue.hashCode() : 0 );
        return ((hash << 5) - hash) + ( null != enumName ? enumName.hashCode() : 0 );
    }

    @Override
    public final boolean equalSemantics(final SemanticEqualityOp arg) {
        if (arg == this) {
            return true;
        } else  if ( !(arg instanceof ConstantDefinition) ) {
            return false;
        } else {
            final ConstantDefinition t = (ConstantDefinition) arg;
            if( !equals(getName(), t.getName()) ||
                !equals(enumName, t.enumName) ) {
                return false;
            }
            if( hasIntValue ) {
                return iValue == t.iValue;
            } else {
                // define's string value may be semantical equal .. but formatted differently!
                return relaxedEqSem || equals(sValue, t.sValue);
            }
        }
    }

    public String getValue()    { return sValue;    }
    /** Returns null if this definition was not part of an
        enumeration, or if the enum was anonymous. */
    public String getEnumName() { return enumName; }

    public boolean isEnum() { return isEnum; }

    @Override
    public String toString() {
        return "ConstantDefinition [name " + getName()
                + ", value " + sValue + " (isInt " + hasIntValue
                + "), enumName " + enumName + ", isEnum " + isEnum + "]";
    }

    private static boolean equals(final String s1, final String s2) {
        if (s1 == null || s2 == null) {
            if (s1 == null && s2 == null) {
                return true;
            }
            return false;
        }

        return s1.equals(s2);
    }

    public static boolean isConstantExpression(final String value) {
        if( null != value && value.length() > 0 ) {
            // Single numeric value
            if ( isNumber(value) ) {
                return true;
            }
            // Find constant expressions like (1 << 3)
            // if found just pass them through, they will most likely work in java too
            // expressions containing identifiers are currently ignored (casts too)
            final String[] values = value.split("[\\s\\(\\)]"); // [ whitespace '(' ')' ]
            int numberCount = 0;
            for (final String s : values) {
                if( s.length() > 0 ) {
                    if( isCPPOperand(s) ) {
                        // OK
                    } else if ( isNumber(s) ) {
                        // OK
                        numberCount++;
                    } else {
                        return false;
                    }
                }
            }
            final boolean res = numberCount > 0;
            return res;
        }
        return false;
    }
    public static boolean isNumber(final String s) {
        if( isHexNumber(s) ) {
            return true;
        } else {
            return isDecimalNumber(s);
        }
    }
    public static boolean isHexNumber(final String s) {
        return patternHexNumber.matcher(s).matches();
    }
    public static java.util.regex.Pattern patternHexNumber =
        java.util.regex.Pattern.compile("0[xX][0-9a-fA-F]+[lLfFuU]?");

    public static boolean isDecimalNumber(final String s) {
        try {
            Float.valueOf(s);
        } catch (final NumberFormatException e) {
            // not parsable as a number
            return false;
        }
        return true;
    }


    public static boolean isCPPOperand(final String s) {
        return patternCPPOperand.matcher(s).matches();
    }
    /**
     * One of: {@code +} {@code -} {@code *} {@code /} {@code |} {@code &} {@code (} {@code )} {@code <<} {@code >>}
     */
    public static java.util.regex.Pattern patternCPPOperand =
        java.util.regex.Pattern.compile("[\\+\\-\\*\\/\\|\\&\\(\\)]|(\\<\\<)|(\\>\\>)");

    public static boolean isIdentifier(final String value) {
        boolean identifier = false;

        final char[] chars = value.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            if (i == 0) {
                if (Character.isJavaIdentifierStart(c)) {
                    identifier = true;
                }
            } else {
                if (!Character.isJavaIdentifierPart(c)) {
                    identifier = false;
                    break;
                }
            }
        }
        return identifier;
    }
}
