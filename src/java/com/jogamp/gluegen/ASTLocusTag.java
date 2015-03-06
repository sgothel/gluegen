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
package com.jogamp.gluegen;

/**
 * An AST location tag.
 */
public class ASTLocusTag {
    /** Source object, might be {@link String}. */
    public final Object source;
    /** Line number, {@code -1} if undefined */
    public final int line;
    /** Column number, {@code -1} if undefined */
    public final int column;
    /** Source text reflecting current location, {@code null} if undefined */
    public final String text;

    public ASTLocusTag(final Object source, final int line, final int column, final String text) {
        this.source = source;
        this.line = line;
        this.column = column;
        this.text = text;
    }

    public String toString() {
        final StringBuffer buf = new StringBuffer();
        if (source != null) {
            buf.append(source).append(": ");
        }
        if (line != -1) {
            buf.append("line ").append(line);
            if (column != -1) {
                buf.append(":" + column);
            }
            buf.append(": ");
        }
        if( null != text && text.length()>0 ) {
            buf.append("text '").append(text).append("'");
        }
        return buf.toString();
    }

    /**
     * Interface tag for {@link ASTLocusTag} provider.
     */
    public static interface ASTLocusTagProvider {
        /**
         * Returns this instance's {@link ASTLocusTag}, if available,
         * otherwise returns {@code null}.
         */
        ASTLocusTag getASTLocusTag();
    }
}
