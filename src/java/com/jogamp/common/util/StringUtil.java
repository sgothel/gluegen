/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.common.util;

import java.util.ArrayList;
import java.util.List;

/** Basic utility functions for {@link String} and {@link CharSequence} in general. */
public class StringUtil {
    /** Linefeed character unicode {@code '\n'}, 0x000A. */
    public static final char LF = '\n';
    /** CR character unicode {@code '\r'}, 0x000D. */
    public static final char CR = '\r';
    /** Space character unicode {@code ' '}, 0x0020. */
    public static final char SPACE = ' ';

    /**
     * List of ASCII & Unicode space separator, aka {@code Whitespace}.
     * @see https://www.compart.com/en/unicode/category/Zs
     * @see https://en.wikipedia.org/wiki/Whitespace_character
     * @see https://www.unicode.org/reports/tr44/#General_Category_Values
     * @see ftp://ftp.unicode.org/Public/UNIDATA/Scripts.txt
     * @see https://www.w3schools.com/charsets/ref_utf_punctuation.asp
     */
    public static final String WHITESPACE =
                                    String.valueOf("\t")+                      // char tabulator
                                    String.valueOf(LF)+                        // LF 0x000A
                                    String.valueOf(Character.toChars(0x000B))+ // line tab
                                    String.valueOf(Character.toChars(0x000C))+ // FF
                                    String.valueOf(CR)+                        // CR 0x000D
                                    String.valueOf(SPACE)+                     // SPACE 0x0020
                                    String.valueOf(Character.toChars(0x0085))+ // Next Line
                                    String.valueOf(Character.toChars(0x00A0))+ // No-break space (NBSP)
                                    String.valueOf(Character.toChars(0x1680))+ // Ogham Space Mark
                                    String.valueOf(Character.toChars(0x2000))+ // En Quad
                                    String.valueOf(Character.toChars(0x2001))+ // Em Quad
                                    String.valueOf(Character.toChars(0x2002))+ // En Space
                                    String.valueOf(Character.toChars(0x2003))+ // Em Space
                                    String.valueOf(Character.toChars(0x2004))+ // Three-Per Em-Space
                                    String.valueOf(Character.toChars(0x2005))+ // Four-Per Em-Space
                                    String.valueOf(Character.toChars(0x2006))+ // Six-Per Em-Space
                                    String.valueOf(Character.toChars(0x2007))+ // Figure-Space
                                    String.valueOf(Character.toChars(0x2008))+ // Punctuation-Space
                                    String.valueOf(Character.toChars(0x2009))+ // Thin-Space
                                    String.valueOf(Character.toChars(0x200A))+ // Hair-Space
                                    String.valueOf(Character.toChars(0x202F))+ // Narrow No-break space (NNBSP)
                                    String.valueOf(Character.toChars(0x205F))+ // Medium Mathematical space (MMSP)
                                    String.valueOf(Character.toChars(0x3000)); // Ideographic Space


    /** Return true if given codepoint in included within {@link #WHITESPACE}. */
    public static boolean isWhitespace(final int cp) {
        return 0 <= WHITESPACE.indexOf(cp);
    }

    /**
     * Returns true if given codepoint is a fullwidth unicode character.
     * @see https://www.compart.com/en/unicode/block/U+FF00
     */
    public static boolean isFullwidth(final int cp) {
        return 0xff01 <= cp && cp <= 0xff60;
    }
    /**
     * Returns true if given codepoint is a halfwidth unicode character.
     * @see https://www.compart.com/en/unicode/block/U+FF00
     */
    public static boolean isHalfwidth(final int cp) {
        return 0xff61 == cp && cp <= 0xffee;
    }

    /** Returns true if given codepoint is either {@link #isFullwidth(int)}, {@link #isHalfwidth(int)} or {@link #isWhitespace(int)}. */
    public static boolean hasSpace(final int cp) {
        return isFullwidth(cp) || isHalfwidth(cp) || 0 <= WHITESPACE.indexOf(cp);
    }

    /** Returns number of lines, i.e. number of non-empty lines, separated by {@link #LF}. */
    public static int getLineCount(final CharSequence s) {
        if( null == s ) {
            return 0;
        }
        final int len = s.length();
        if( 0 == len ) {
            return 0;
        }
        int lc = 0;
        for (int i=0; len > i; ) {
            ++lc;
            final int j = indexOf(s, LF, i);
            if ( 0 > j ) {
                break;
            }
            i = j + 1;
        }
        return lc;
    }

    /**
     * Calls {@link String#indexOf(int, int)}
     * @param hay the unicode character string to search in from {@code fromIdx}
     * @param needle the unicode code point character to search
     * @param start index to start searching
     * @return {@code -1} if not found, otherwise [0..{@link String#length()}-1].
     * @see #indexOf(CharSequence, int, int)
     * @see String#indexOf(int, int)
     */
    public static int indexOf(final String hay, final int needle, final int start) {
        if( null != hay ) {
            return hay.indexOf(needle, start);
        }
        return -1;
    }
    /**
     * Naive implementation of {@link String#indexOf(int, int)} for type {@link CharSequence}.
     * <p>
     * Uses {@link String#indexOf(int, int)} if {@code hay} is of type {@link String},
     * otherwise
     * </p>
     * @param hay the unicode character string to search in from {@code fromIdx}
     * @param needle the unicode code point character to search
     * @param start index to start searching
     * @return {@code -1} if not found, otherwise [0..{@link String#length()}-1].
     * @see #indexOf(String, char, int)
     * @see String#indexOf(int, int)
     */
    public static int indexOf(final CharSequence hay, final int needle, final int start) {
        if( null != hay ) {
            if (hay instanceof String) {
                return ((String) hay).indexOf(needle, start);
            }
            final int l = hay.length();
            final int s = Math.max(0,  start);
            if ( l > s ) {
                if (needle < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                    for (int i = s; i < l; i++) {
                        if (hay.charAt(i) == needle) {
                            return i;
                        }
                    }
                } else if (Character.isValidCodePoint(needle)) {
                    final char[] chars = Character.toChars(needle);
                    for (int i = s; i < l - 1; i++) {
                        final char hi = hay.charAt(i); // Character.toCodePoint(hi, lo);
                        final char lo = hay.charAt(i + 1);
                        if (hi == chars[0] && lo == chars[1]) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Remove all given separator unicode characters from given text,
     * i.e. all leading, all ending as well as duplicate consecutive separator within.
     * The latter reduces the separator to just a single one.
     * @param text the source text
     * @param separators separator unicode characters, pass {@code null} for {@link Character#isWhitespace(int) whitespace}.
     *                   Consider using {@link #WHITESPACE} to cover all unicode space character.
     * @param replacement optional replacement string for matched separator within sequence removing duplicated.
     *                    If {@code null}, the first found separator is used.
     * @return stripped text
     */
    public static String strip(final String text, final String separators, final String replacement) {
        if (text == null ) {
            return "";
        }
        final int len = text.length();
        if (len == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        boolean lastMatched = false;
        if (separators == null) {
            for(int i=0; i < len; ++i) {
                final int cp = text.codePointAt(i);
                final boolean match = Character.isWhitespace(cp);
                if ( !match || !lastMatched && 0 < i ) {
                    if( match && null != replacement ) {
                        sb.append(replacement);
                    } else {
                        sb.append(String.valueOf(Character.toChars(cp)));
                    }
                }
                lastMatched = match;
            }
            if(lastMatched) {
                sb.deleteCharAt(sb.length()-1);
            }
        } else {
            for(int i=0; i < len; ++i) {
                final int cp = text.codePointAt(i);
                final boolean match = separators.indexOf(cp) >= 0;
                if ( !match || !lastMatched && 0 < i ) {
                    if( match && null != replacement ) {
                        sb.append(replacement);
                    } else {
                        sb.append(String.valueOf(Character.toChars(cp)));
                    }
                }
                lastMatched = match;
            }
            if(lastMatched) {
                sb.deleteCharAt(sb.length()-1);
            }
        }
        return sb.toString();
    }

    /**
     * Returns an array of split {@code text} at {@code separators} or {@link Character#isWhitespace(int) whitespace}.
     * <p>
     * Each line's cutting point is the first {@code separator} or {@link Character#isWhitespace(int) whitespace}
     * occurrence starting at {@code text.length() / lineCount * 0.9}.
     * </p>
     * <p>
     * The separator or {@link Character#isWhitespace(int) whitespace} character
     * at the cutting point is skipped in the resulting array of the split parts, i.e. lines.
     * </p>
     * @param text  the text to be split, {@code null} results in an empty list
     * @param lineCount number of resulting lines
     * @param separators separator unicode characters, pass {@code null} for {@link Character#isWhitespace(int) whitespace}.
     *                   Consider using {@link #WHITESPACE} to cover all unicode space character.
     * @see #split(String, int, String, String)
     */
    public static List<String> split(final String text, final int lineCount, final String separators) {
        final List<String> list = new ArrayList<>();
        if (text == null || 0 == lineCount) {
            return list;
        }
        final int len = text.length();
        if (len == 0) {
            return list;
        }
        if( 1 == lineCount ) {
            list.add(text);
            return list;
        }
        final int segLen = (int)Math.ceil((float)len / (float)lineCount * 0.9f);

        int i = segLen;
        int start = 0;
        if (separators == null) {
            while (i < len && list.size() < lineCount - 1) {
                if (Character.isWhitespace(text.codePointAt(i))) {
                    list.add(text.substring(start, i));
                    start = i+1; // skip separator
                    i += segLen;
                } else {
                    i++;
                }
            }
        } else {
            while (i < len && list.size() < lineCount - 1) {
                if (separators.indexOf(text.codePointAt(i)) >= 0) {
                    list.add(text.substring(start, i));
                    start = i+1; // skip separator
                    i += segLen;
                } else {
                    i++;
                }
            }
        }
        if( start < len ) {
            list.add(text.substring(start, len));
        }
        return list;
    }
    /**
     * Returns a multi-line string of split {@code text} at {@code separators} or {@link Character#isWhitespace(int) whitespace}
     * glued with given {@code lineSeparator}.
     * <p>
     * Each line's cutting point is the first {@code separator} or {@link Character#isWhitespace(int) whitespace}
     * occurrence starting at {@code text.length() / lineCount * 0.9}.
     * </p>
     * <p>
     * The separator character or {@link Character#isWhitespace(int) whitespace}
     * at the cutting point is skipped in the string of glued split parts, i.e. lines.
     * </p>
     * @param text  the text to be split, {@code null} results in an empty list
     * @param lineCount number of resulting lines
     * @param separators separator unicode characters, pass {@code null} for {@link Character#isWhitespace(int) whitespace}.
     *                   Consider using {@link #WHITESPACE} to cover all unicode space character.
     * @param lineSeparator the glue placed between the split lines in the concatenated result
     * @see #split(String, int, String)
     */
    public static String split(final String text, final int lineCount, final String separators, final String lineSeparator) {
        final List<String> lines = split(text, lineCount, separators);
        final StringBuilder sb = new StringBuilder();
        boolean addGlue = false;
        for(final String l : lines) {
            if( addGlue ) {
                sb.append(lineSeparator);
            }
            sb.append(l);
            addGlue = true;
        }
        return sb.toString();
    }

}
