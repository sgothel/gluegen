/**
 * Copyright 2014 JogAmp Community. All rights reserved.
 * Copyright 2006, 2010 The Apache Software Foundation.
 *
 * This code is derived from the Apache Harmony project's {@code class java.net.URI.Helper},
 * and has been heavily modified for GlueGen/JogAmp.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the LICENSE.txt file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jogamp.common.net;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jogamp.common.Debug;

import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.PropertyAccess;

/**
 * This class implements an immutable Uri as defined by <a href="https://tools.ietf.org/html/rfc2396">RFC 2396</a>.
 * <p>
 * Character encoding is employed as defined by <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>,
 * see <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 section 2.1</a>,
 * while multibyte unicode characters are preserved in encoded parts.
 * </p>
 *
 * <pre>
     1 [scheme:]scheme-specific-part[#fragment]
     2 [scheme:][//authority]path[?query][#fragment]
     3 [scheme:][//[user-info@]host[:port]]path[?query][#fragment]

        scheme-specific-part: [//authority]path[?query]
        authority:            [user-info@]host[:port]
 * </pre>
 * <p>
 * <a href="https://tools.ietf.org/html/rfc3986#section-2.2">RFC 3986 section 2.2</a> <i>Reserved Characters</i> (January 2005)
 * <table border="1">
    <tr>
    <td><code>!</code></td>
    <td><code>*</code></td>
    <td><code>'</code></td>
    <td><code>(</code></td>
    <td><code>)</code></td>
    <td><code>;</code></td>
    <td><code>:</code></td>
    <td><code>@</code></td>
    <td><code>&amp;</code></td>
    <td><code>=</code></td>
    <td><code>+</code></td>
    <td><code>$</code></td>
    <td><code>,</code></td>
    <td><code>/</code></td>
    <td><code>?</code></td>
    <td><code>#</code></td>
    <td><code>[</code></td>
    <td><code>]</code></td>
    </tr>
 * </table>
 * </p>
 * <p>
 * <a href="https://tools.ietf.org/html/rfc3986#section-2.3">RFC 3986 section 2.3</a> <i>Unreserved Characters</i> (January 2005)
 * <table border="1">
    <tr>
    <td><code>A</code></td>
    <td><code>B</code></td>
    <td><code>C</code></td>
    <td><code>D</code></td>
    <td><code>E</code></td>
    <td><code>F</code></td>
    <td><code>G</code></td>
    <td><code>H</code></td>
    <td><code>I</code></td>
    <td><code>J</code></td>
    <td><code>K</code></td>
    <td><code>L</code></td>
    <td><code>M</code></td>
    <td><code>N</code></td>
    <td><code>O</code></td>
    <td><code>P</code></td>
    <td><code>Q</code></td>
    <td><code>R</code></td>
    <td><code>S</code></td>
    <td><code>T</code></td>
    <td><code>U</code></td>
    <td><code>V</code></td>
    <td><code>W</code></td>
    <td><code>X</code></td>
    <td><code>Y</code></td>
    <td><code>Z</code></td>
    </tr>
    <tr>
    <td><code>a</code></td>
    <td><code>b</code></td>
    <td><code>c</code></td>
    <td><code>d</code></td>
    <td><code>e</code></td>
    <td><code>f</code></td>
    <td><code>g</code></td>
    <td><code>h</code></td>
    <td><code>i</code></td>
    <td><code>j</code></td>
    <td><code>k</code></td>
    <td><code>l</code></td>
    <td><code>m</code></td>
    <td><code>n</code></td>
    <td><code>o</code></td>
    <td><code>p</code></td>
    <td><code>q</code></td>
    <td><code>r</code></td>
    <td><code>s</code></td>
    <td><code>t</code></td>
    <td><code>u</code></td>
    <td><code>v</code></td>
    <td><code>w</code></td>
    <td><code>x</code></td>
    <td><code>y</code></td>
    <td><code>z</code></td>
    </tr>
    <tr>
    <td><code>0</code></td>
    <td><code>1</code></td>
    <td><code>2</code></td>
    <td><code>3</code></td>
    <td><code>4</code></td>
    <td><code>5</code></td>
    <td><code>6</code></td>
    <td><code>7</code></td>
    <td><code>8</code></td>
    <td><code>9</code></td>
    <td><code>-</code></td>
    <td><code>_</code></td>
    <td><code>.</code></td>
    <td><code>~</code></td>
    </tr>
 * </table>
 * </p>
 * <p>
 * Other characters in a Uri must be percent encoded.
 * </p>
 * @since 2.2.1
 */
public class Uri {
    private static final boolean DEBUG;
    private static final boolean DEBUG_SHOWFIX;

    static {
        Debug.initSingleton();
        DEBUG = IOUtil.DEBUG || Debug.debug("Uri");
        DEBUG_SHOWFIX = PropertyAccess.isPropertyDefined("jogamp.debug.Uri.ShowFix", true);
    }

    /**
     * Usually used to fix a path from a previously contained and opaque Uri,
     * i.e. {@link #getContainedUri()}.
     * <p>
     * Such an opaque Uri w/ erroneous encoding may have been injected via
     * {@link #valueOf(URI)} and {@link #valueOf(URL)} where the given URL or URI was opaque!
     * </p>
     * <p>
     * This remedies issues when dealing w/ java URI/URL opaque sources,
     * which do not comply to the spec, i.e. containe un-encoded chars, e.g. ':', '$', ..
     * </p>
     */
    private static final int PARSE_HINT_FIX_PATH = 1 << 0;

    private static final String DIGITS = "0123456789ABCDEF";

    private static final String ENCODING = "UTF8";
    private static final String MSG_ENCODING_NA = "Charset UTF8 not available";
    private static final Pattern patternSingleFS = Pattern.compile("/{1}");

    /**
     * RFC 3986 section 2.3 Unreserved Characters (January 2005)
     * <p>
     * {@value} + {@code alphanum}
     * </p>
     */
    public static final String UNRESERVED = "_-.~";
    // Harmony: _ - ! . ~ ' ( ) *

    private static final String punct = ",;:$&+=";
    // Harmony: , ; : $ & + =

    /**
     * RFC 3986 section 2.2 Reserved Characters (January 2005)
     * <p>
     * {@value} + {@code alphanum}
     * </p>
     */
    public static final String RESERVED = punct + "!*\'()@/?#[]";
    // Harmony: , ; : $ & + = ? / [ ] @

    public static final String RESERVED_2 = punct + "!*\'()@/?[]";
    // Harmony: , ; : $ & + = ? / [ ] @

    // Bug 908, issues w/ windows file path char: $ ^ ~ # [ ]
    // Windows invalid File characters: * ? " < > |

    /**
     * Valid charset for RFC 2396 {@code authority}'s {@code user-info},
     * additional to legal {@code alphanum} characters.
     * <p>
     * {@value} + {@code alphanum}
     * </p>
     */
    public static final String USERINFO_LEGAL = UNRESERVED + punct;
    // Harmony: someLegal = unreserved + punct -> _ - ! . ~ ' ( ) * , ; : $ & + =

    /**
     * Valid charset for RFC 2396 {@code authority},
     * additional to legal {@code alphanum} characters.
     * <p>
     * {@value} + {@code alphanum}
     * </p>
     */
    public static final String AUTHORITY_LEGAL = "@[]" + USERINFO_LEGAL;

    /**
     * Valid charset for RFC 2396 {@code path},
     * additional to legal {@code alphanum} characters.
     * <p>
     * {@value} + {@code alphanum}
     * </p>
     */
    public static final String PATH_LEGAL = "/!" + UNRESERVED; // no RESERVED chars but '!',  to allow JAR Uris;
    // Harmony: "/@" + unreserved + punct -> / @ _ - ! . ~ \ ' ( ) * , ; : $ & + =

    /**
     * Valid charset for RFC 2396 {@code query},
     * additional to legal {@code alphanum} characters.
     * <p>
     * {@value} + {@code alphanum}
     * </p>
     */
    public static final String QUERY_LEGAL = UNRESERVED + RESERVED_2 + "\\\"";
    // Harmony: unreserved + reserved + "\\\""

    /**
     * Valid charset for RFC 2396 {@code scheme-specific-part},
     * additional to legal {@code alphanum} characters.
     * <p>
     * {@value} + {@code alphanum}
     * </p>
     */
    public static final String SSP_LEGAL = QUERY_LEGAL;
    // Harmony: unreserved + reserved

    /**
     * Valid charset for RFC 2396 {@code fragment},
     * additional to legal {@code alphanum} characters.
     * <p>
     * {@value} + {@code alphanum}
     * </p>
     */
    public static final String FRAG_LEGAL = UNRESERVED + RESERVED;
    // Harmony: unreserved + reserved

    /** {@value} */
    public static final char SCHEME_SEPARATOR = ':';
    /** {@value} */
    public static final char QUERY_SEPARATOR = '?';
    /** {@value} */
    public static final char FRAGMENT_SEPARATOR = '#';
    /** {@value} */
    public static final String FILE_SCHEME = "file";
    /** {@value} */
    public static final String HTTP_SCHEME = "http";
    /** {@value} */
    public static final String HTTPS_SCHEME = "https";
    /** {@value} */
    public static final String JAR_SCHEME = "jar";
    /** A JAR sub-protocol is separated from the JAR entry w/ this separator {@value}. Even if no class is specified '!/' must follow!. */
    public static final char JAR_SCHEME_SEPARATOR = '!';

    /**
     * Immutable RFC3986 encoded string.
     */
    public static class Encoded implements Comparable<Encoded>, CharSequence {
        private final String s;

        /**
         * Casts the given encoded String by creating a new Encoded instance.
         * <p>
         * No encoding will be performed, use with care.
         * </p>
         */
        public static Encoded cast(final String encoded) {
            return new Encoded(encoded);
        }

        Encoded(final String encodedString) {
            this.s = encodedString;
        }

        /**
         * Encodes all characters into their hexadecimal value prepended by '%', except:
         * <ol>
         *   <li>letters ('a'..'z', 'A'..'Z')</li>
         *   <li>numbers ('0'..'9')</li>
         *   <li>characters in the legal-set parameter</li>
         *   <li> others (unicode characters that are not in
         *        US-ASCII set, and are not ISO Control or are not ISO Space characters)</li>
         * </ol>
         * <p>
         * Uses {@link Uri#encode(String, String)} for implementation..
         * </p>
         *
         * @param vanilla the string to be encoded
         * @param legal extended character set, allowed to be preserved in the vanilla string
         */
        public Encoded(final String vanilla, final String legal) {
            this.s = encode(vanilla, legal);
        }

        public boolean isASCII() { return false; }

        /** Returns the encoded String */
        public final String get() { return s; }

        /**
         * Decodes the string argument which is assumed to be encoded in the {@code
         * x-www-form-urlencoded} MIME content type using the UTF-8 encoding scheme.
         * <p>
         *'%' and two following hex digit characters are converted to the
         * equivalent byte value. All other characters are passed through
         * unmodified.
         * </p>
         * <p>
         * e.g. "A%20B%20C %24%25" -> "A B C $%"
         * </p>
         * <p>
         * Uses {@link Uri#decode(String)} for implementation..
         * </p>
         */
        public final String decode() { return Uri.decode(s); }

        //
        // Basic Object / Identity
        //

        /**
         * {@inheritDoc}
         * <p>
         * Returns the encoded String, same as {@link #get()}.
         * </p>
         */
        @Override
        public final String toString() { return s; }

        @Override
        public final int hashCode() { return s.hashCode(); }

        /**
         * {@inheritDoc}
         *
         * @param  o The comparison argument, either a {@link Encoded} or a {@link String}
         *
         * @return {@code true} if the given object is equivalent to this instance,
         *         otherwise {@code false}.
         *
         * @see  #compareTo(Encoded)
         * @see  #equalsIgnoreCase(Encoded)
         */
        @Override
        public final boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof Encoded) {
                return s.equals(((Encoded)o).s);
            }
            return s.equals(o);
        }

        //
        // CharSequence
        //

        @Override
        public final int length() { return s.length(); }

        @Override
        public final char charAt(final int index) { return s.charAt(index); }

        @Override
        public final CharSequence subSequence(final int start, final int end) { return s.subSequence(start, end); }

        @Override
        public final int compareTo(final Encoded o) { return s.compareTo(o.s); }

        //
        // String derived ..
        //
        /** See {@link String#concat(String)}. */
        public Encoded concat(final Encoded encoded) { return new Encoded(s.concat(encoded.s)); }

        /** See {@link String#substring(int)}. */
        public final Encoded substring(final int start) { return new Encoded(s.substring(start)); }
        /** See {@link String#substring(int, int)}. */
        public final Encoded substring(final int start, final int end) { return new Encoded(s.substring(start, end)); }

        /** See {@link String#indexOf(int)}. */
        public final int indexOf(final int ch) { return s.indexOf(ch); }
        /** See {@link String#indexOf(int, int)}. */
        public final int indexOf(final int ch, final int fromIndex) { return s.indexOf(ch, fromIndex); }
        /** See {@link String#indexOf(String)}. */
        public final int indexOf(final String str) { return s.indexOf(str); }
        /** See {@link String#indexOf(String, int)}. */
        public final int indexOf(final String str, final int fromIndex) { return s.indexOf(str, fromIndex); }

        /** See {@link String#lastIndexOf(int)}. */
        public final int lastIndexOf(final int ch) { return s.lastIndexOf(ch); }
        /** See {@link String#lastIndexOf(int, int)}. */
        public int lastIndexOf(final int ch, final int fromIndex) { return s.lastIndexOf(ch, fromIndex); }
        /** See {@link String#lastIndexOf(String)}. */
        public int lastIndexOf(final String str) { return s.lastIndexOf(str); }
        /** See {@link String#lastIndexOf(String, int)}. */
        public int lastIndexOf(final String str, final int fromIndex) { return s.lastIndexOf(str, fromIndex); }

        /** See {@link String#startsWith(String)} */
        public boolean startsWith(final String prefix) { return s.startsWith(prefix); }
        /** See {@link String#startsWith(String, int)} */
        public boolean startsWith(final String prefix, final int toffset) { return s.startsWith(prefix, toffset); }
        /** See {@link String#endsWith(String)} */
        public boolean endsWith(final String suffix) { return s.endsWith(suffix); }

        /** See {@link String#equalsIgnoreCase(String)}. */
        public final boolean equalsIgnoreCase(final Encoded anotherEncoded) { return s.equalsIgnoreCase(anotherEncoded.s); }
    }

    public static class ASCIIEncoded extends Encoded {
        /**
         * Casts the given encoded String by creating a new ASCIIEncoded instance.
         * <p>
         * No encoding will be performed, use with care.
         * </p>
         */
        public static ASCIIEncoded cast(final String encoded) {
            return new ASCIIEncoded(encoded, null);
        }
        private ASCIIEncoded(final String encoded, final Object unused) {
            super(encoded);
        }

        /**
         * Other characters, which are Unicode chars that are not US-ASCII, and are
         * not ISO Control or are not ISO Space chars are not preserved
         * and encoded into their hexidecimal value prepended by '%'.
         * <p>
         * For example: Euro currency symbol -> "%E2%82%AC".
         * </p>
         * <p>
         * Uses {@link Uri#encodeToASCIIString(String)} for implementation.
         * </p>
         * @param unicode unencoded input
         */
        public ASCIIEncoded(final String unicode) {
            super(encodeToASCIIString(unicode));
        }
        public boolean isASCII() { return true; }
    }

    private static void encodeChar2UTF8(final StringBuilder buf, final char ch) {
        final byte[] bytes;
        try {
            bytes = new String(new char[] { ch }).getBytes(ENCODING);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(MSG_ENCODING_NA, e);
        }
        // FIXME: UTF-8 produces more than one byte ? Optimization might be possible.
        for (int j = 0; j < bytes.length; j++) {
            final byte b = bytes[j];
            buf.append('%');
            buf.append(DIGITS.charAt( ( b & 0xf0 ) >> 4 ));
            buf.append(DIGITS.charAt(   b & 0xf         ));
        }
    }

    /**
     * All characters are encoded into their hexadecimal value prepended by '%', except:
     * <ol>
     *   <li>letters ('a'..'z', 'A'..'Z')</li>
     *   <li>numbers ('0'..'9')</li>
     *   <li>characters in the legal-set parameter</li>
     *   <li> others (unicode characters that are not in
     *        US-ASCII set, and are not ISO Control or are not ISO Space characters)</li>
     * </ol>
     * <p>
     * Use {@link #encodeToASCIIString(String)} for US-ASCII encoding.
     * </p>
     * <p>
     * Consider using {@link Encoded#Encoded(String, String)} in APIs
     * to distinguish encoded from unencoded data by type.
     * </p>
     *
     * @param vanilla the string to be encoded
     * @param legal extended character set, allowed to be preserved in the vanilla string
     * @return java.lang.String the converted string
     */
    public static String encode(final String vanilla, final String legal) {
        if( null == vanilla ) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < vanilla.length(); i++) {
            final char ch = vanilla.charAt(i);
            if ( (ch >= 'a' && ch <= 'z') ||
                 (ch >= 'A' && ch <= 'Z')  ||
                 (ch >= '0' && ch <= '9')  ||
                 legal.indexOf(ch) > -1 ||
                 ( ch > 127 && !Character.isSpaceChar(ch) && !Character.isISOControl(ch) )
               ) {
                buf.append(ch);
            } else {
                encodeChar2UTF8(buf, ch);
            }
        }
        return buf.toString();
    }

    /**
     * Other characters, which are Unicode chars that are not US-ASCII, and are
     * not ISO Control or are not ISO Space chars are not preserved
     * and encoded into their hexidecimal value prepended by '%'.
     * <p>
     * For example: Euro currency symbol -> "%E2%82%AC".
     * </p>
     * <p>
     * Consider using {@link ASCIIEncoded#ASCIIEncoded(String)} in APIs
     * to distinguish encoded from unencoded data by type.
     * </p>
     * @param unicode string to be converted
     * @return java.lang.String the converted string
     */
    public static String encodeToASCIIString(final String unicode) {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < unicode.length(); i++) {
            final char ch = unicode.charAt(i);
            if (ch <= 127) {
                buf.append(ch);
            } else {
                encodeChar2UTF8(buf, ch);
            }
        }
        return buf.toString();
    }

    /**
     * Safe {@link Encoded#decode()} call on optional {@code encoded} instance.
     * @param encoded {@link Encoded} instance to be decoded, may be {@code null}.
     * @return the {@link Encoded#decode() decoded} String or {@code null} if {@code encoded} was {@code null}.
     */
    public static String decode(final Encoded encoded) {
        return null != encoded ? encoded.decode() : null;
    }

    /**
     * Decodes the string argument which is assumed to be encoded in the {@code
     * x-www-form-urlencoded} MIME content type using the UTF-8 encoding scheme.
     * <p>
     *'%' and two following hex digit characters are converted to the
     * equivalent byte value. All other characters are passed through
     * unmodified.
     * </p>
     * <p>
     * e.g. "A%20B%20C %24%25" -> "A B C $%"
     * </p>
     *
     * @param encoded The encoded string.
     * @return java.lang.String The decoded version.
     */
    public static String decode(final String encoded) {
        if( null == encoded ) {
            return null;
        }
        final StringBuilder result = new StringBuilder();
        final byte[] buf = new byte[32];
        int bufI = 0;
        for (int i = 0; i < encoded.length();) {
            final char c = encoded.charAt(i);
            if (c == '%') {
                bufI = 0;
                do {
                    if (i + 2 >= encoded.length()) {
                        throw new IllegalArgumentException("missing '%' hex-digits at index "+i);
                    }
                    final int d1 = Character.digit(encoded.charAt(i + 1), 16);
                    final int d2 = Character.digit(encoded.charAt(i + 2), 16);
                    if (d1 == -1 || d2 == -1) {
                        throw new IllegalArgumentException("invalid hex-digits at index "+i+": "+encoded.substring(i, i + 3));
                    }
                    buf[bufI++] = (byte) ((d1 << 4) + d2);
                    if( 32 == bufI ) {
                        appendUTF8(result, buf, bufI);
                        bufI = 0;
                    }
                    i += 3;
                } while (i < encoded.length() && encoded.charAt(i) == '%');
                if( 0 < bufI ) {
                    appendUTF8(result, buf, bufI);
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }
    private static void appendUTF8(final StringBuilder sb, final byte[] buf, final int count) {
        try {
            sb.append(new String(buf, 0, count, ENCODING));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(MSG_ENCODING_NA, e);
        }
    }

    /**
     * Creates a new Uri instance using the given unencoded arguments.
     * <p>
     * This constructor first creates a temporary Uri string from the given unencoded components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * {@code [scheme:]scheme-specific-part[#fragment]}
     * </p>
     * <p>
     * {@code host} and {@code port} <i>may</i> be undefined or invalid within {@code scheme-specific-part}.
     * </p>
     *
     * @param scheme the unencoded scheme part of the Uri.
     * @param ssp the unencoded scheme-specific-part of the Uri.
     * @param fragment the unencoded fragment part of the Uri.
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri create(final String scheme, final String ssp, final String fragment) throws URISyntaxException {
        if ( emptyString(scheme) && emptyString(ssp) && emptyString(fragment) ) {
            throw new URISyntaxException("", "all empty parts");
        }
        final StringBuilder uri = new StringBuilder();
        if ( !emptyString(scheme) ) {
            uri.append(scheme);
            uri.append(SCHEME_SEPARATOR);
        }
        if ( !emptyString(ssp) ) {
            // QUOTE ILLEGAL CHARACTERS
            uri.append(encode(ssp, SSP_LEGAL));
        }
        if ( !emptyString(fragment) ) {
            uri.append(FRAGMENT_SEPARATOR);
            // QUOTE ILLEGAL CHARACTERS
            uri.append(encode(fragment, FRAG_LEGAL));
        }
        return new Uri(new Encoded(uri.toString()), false, 0);
    }

    /**
     * Creates a new Uri instance using the given encoded arguments.
     * <p>
     * This constructor first creates a temporary Uri string from the given encoded components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * The given encoded components are taken as-is, i.e. no re-encoding will be performed!
     * However, Uri parsing will re-evaluate encoding of the resulting components.
     * </p>
     * <p>
     * {@code [scheme:]scheme-specific-part[#fragment]}
     * </p>
     * <p>
     * {@code host} and {@code port} <i>may</i> be undefined or invalid within {@code scheme-specific-part}.
     * </p>
     *
     * @param scheme the encoded scheme part of the Uri.
     * @param ssp the encoded scheme-specific-part of the Uri.
     * @param fragment the encoded fragment part of the Uri.
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri create(final Encoded scheme, final Encoded ssp, final Encoded fragment) throws URISyntaxException {
        if ( emptyString(scheme) && emptyString(ssp) && emptyString(fragment) ) {
            throw new URISyntaxException("", "all empty parts");
        }
        final StringBuilder uri = new StringBuilder();
        if ( !emptyString(scheme) ) {
            uri.append(scheme);
            uri.append(SCHEME_SEPARATOR);
        }
        if ( !emptyString(ssp) ) {
            uri.append(ssp.get());
        }
        if ( !emptyString(fragment) ) {
            uri.append(FRAGMENT_SEPARATOR);
            uri.append(fragment.get());
        }
        return new Uri(new Encoded(uri.toString()), false, 0);
    }

    /**
     * Creates a new Uri instance using the given unencoded arguments.
     * <p>
     * This constructor first creates a temporary Uri string from the given unencoded components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * {@code [scheme:][user-info@]host[:port][path][?query][#fragment]}
     * </p>
     * <p>
     * {@code host} and {@code port} <i>must</i> be defined and valid, if any {@code authority} components are defined,
     * i.e. {@code user-info}, {@code host} or {@code port}.
     * </p>
     *
     * @param scheme the unencoded scheme part of the Uri.
     * @param userinfo the unencoded user information of the Uri for authentication and authorization, {@code null} for undefined.
     * @param host the unencoded host name of the Uri, {@code null} for undefined.
     * @param port the port number of the Uri, -1 for undefined.
     * @param path the unencoded path to the resource on the host.
     * @param query the unencoded query part of the Uri to specify parameters for the resource.
     * @param fragment the unencoded fragment part of the Uri.
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri create (final String scheme, final String userinfo, String host, final int port,
                              final String path, final String query, final String fragment) throws URISyntaxException {
        if ( emptyString(scheme) && emptyString(userinfo) && emptyString(host) && emptyString(path) &&
             emptyString(query)  && emptyString(fragment) ) {
            throw new URISyntaxException("", "all empty parts");
        }

        if ( !emptyString(scheme) && !emptyString(path) && path.length() > 0 && path.charAt(0) != '/') {
            throw new URISyntaxException(path, "path doesn't start with '/'");
        }

        final StringBuilder uri = new StringBuilder();
        if ( !emptyString(scheme) ) {
            uri.append(scheme);
            uri.append(SCHEME_SEPARATOR);
        }

        if ( !emptyString(userinfo) || !emptyString(host) || port != -1) {
            uri.append("//");
        }

        if ( !emptyString(userinfo) ) {
            // QUOTE ILLEGAL CHARACTERS in userinfo
            uri.append(encode(userinfo, USERINFO_LEGAL));
            uri.append('@');
        }

        if ( !emptyString(host) ) {
            // check for ipv6 addresses that hasn't been enclosed
            // in square brackets
            if (host.indexOf(SCHEME_SEPARATOR) != -1 && host.indexOf(']') == -1
                    && host.indexOf('[') == -1) {
                host = "[" + host + "]";
            }
            uri.append(host);
        }

        if ( port != -1 ) {
            uri.append(SCHEME_SEPARATOR);
            uri.append(port);
        }

        if ( !emptyString(path) ) {
            // QUOTE ILLEGAL CHARS
            uri.append(encode(path, PATH_LEGAL));
        }

        if ( !emptyString(query) ) {
            uri.append(QUERY_SEPARATOR);
            // QUOTE ILLEGAL CHARS
            uri.append(encode(query, QUERY_LEGAL));
        }

        if ( !emptyString(fragment) ) {
            // QUOTE ILLEGAL CHARS
            uri.append(FRAGMENT_SEPARATOR);
            uri.append(encode(fragment, FRAG_LEGAL));
        }
        return new Uri(new Encoded(uri.toString()), true, 0);
    }

    /**
     * Creates a new Uri instance using the given encoded arguments.
     * <p>
     * This constructor first creates a temporary Uri string from the given encoded components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * The given encoded components are taken as-is, i.e. no re-encoding will be performed!
     * However, Uri parsing will re-evaluate encoding of the resulting components.
     * </p>
     * <p>
     * {@code [scheme:][user-info@]host[:port][path][?query][#fragment]}
     * </p>
     * <p>
     * {@code host} and {@code port} <i>must</i> be defined and valid, if any {@code authority} components are defined,
     * i.e. {@code user-info}, {@code host} or {@code port}.
     * </p>
     *
     * @param scheme the encoded scheme part of the Uri.
     * @param userinfo the encoded user information of the Uri for authentication and authorization, {@code null} for undefined.
     * @param host the encoded host name of the Uri, {@code null} for undefined.
     * @param port the port number of the Uri, -1 for undefined.
     * @param path the encoded path to the resource on the host.
     * @param query the encoded query part of the Uri to specify parameters for the resource.
     * @param fragment the encoded fragment part of the Uri.
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri create (final Encoded scheme, final Encoded userinfo, final Encoded host, final int port,
                              final Encoded path, final Encoded query, final Encoded fragment) throws URISyntaxException {
        if ( emptyString(scheme) && emptyString(userinfo) && emptyString(host) && emptyString(path) &&
             emptyString(query)  && emptyString(fragment) ) {
            throw new URISyntaxException("", "all empty parts");
        }

        if ( !emptyString(scheme) && !emptyString(path) && path.length() > 0 && path.charAt(0) != '/') {
            throw new URISyntaxException(path.get(), "path doesn't start with '/'");
        }

        final StringBuilder uri = new StringBuilder();
        if ( !emptyString(scheme) ) {
            uri.append(scheme);
            uri.append(SCHEME_SEPARATOR);
        }

        if ( !emptyString(userinfo) || !emptyString(host) || port != -1) {
            uri.append("//");
        }

        if ( !emptyString(userinfo) ) {
            uri.append(userinfo.get());
            uri.append('@');
        }

        if ( !emptyString(host) ) {
            uri.append(host.get());
        }

        if ( port != -1 ) {
            uri.append(SCHEME_SEPARATOR);
            uri.append(port);
        }

        if ( !emptyString(path) ) {
            uri.append(path.get());
        }

        if ( !emptyString(query) ) {
            uri.append(QUERY_SEPARATOR);
            uri.append(query.get());
        }

        if ( !emptyString(fragment) ) {
            uri.append(FRAGMENT_SEPARATOR);
            uri.append(fragment.get());
        }
        return new Uri(new Encoded(uri.toString()), true, 0);
    }

    /**
     * Creates a new Uri instance using the given unencoded arguments.
     * <p>
     * This constructor first creates a temporary Uri string from the given unencoded components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * {@code [scheme:]host[path][#fragment]}
     * </p>
     * <p>
     * {@code host} <i>must</i> be valid, if defined.
     * </p>
     *
     * @param scheme the unencoded scheme part of the Uri.
     * @param host the unencoded host name of the Uri.
     * @param path the unencoded path to the resource on the host.
     * @param fragment the unencoded fragment part of the Uri.
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri create(final String scheme, final String host, final String path, final String fragment) throws URISyntaxException {
        return create(scheme, null, host, -1, path, null, fragment);
    }

    /**
     * Creates a new Uri instance using the given encoded arguments.
     * <p>
     * This constructor first creates a temporary Uri string from the given encoded components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * The given encoded components are taken as-is, i.e. no re-encoding will be performed!
     * However, Uri parsing will re-evaluate encoding of the resulting components.
     * </p>
     * <p>
     * {@code [scheme:]host[path][#fragment]}
     * </p>
     * <p>
     * {@code host} <i>must</i> be valid, if defined.
     * </p>
     *
     * @param scheme the encoded scheme part of the Uri.
     * @param host the encoded host name of the Uri.
     * @param path the encoded path to the resource on the host.
     * @param fragment the encoded fragment part of the Uri.
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri create(final Encoded scheme, final Encoded host, final Encoded path, final Encoded fragment) throws URISyntaxException {
        return create(scheme, null, host, -1, path, null, fragment);
    }

    /**
     * Creates a new Uri instance using the given unencoded arguments.
     * <p>
     * This constructor first creates a temporary Uri string from the given unencoded components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * {@code [scheme:][//authority][path][?query][#fragment]}
     * </p>
     * <p>
     * {@code host} and {@code port} <i>may</i> be undefined or invalid, in the optional {@code authority}.
     * </p>
     *
     * @param scheme the unencoded scheme part of the Uri.
     * @param authority the unencoded authority part of the Uri.
     * @param path the unencoded path to the resource on the host.
     * @param query the unencoded query part of the Uri to specify parameters for the resource.
     * @param fragment the unencoded fragment part of the Uri.
     *
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri create(final String scheme, final String authority, final String path, final String query, final String fragment) throws URISyntaxException {
        if ( emptyString(scheme) && emptyString(authority) && emptyString(path) &&
             emptyString(query)  && emptyString(fragment) ) {
            throw new URISyntaxException("", "all empty parts");
        }
        if ( !emptyString(scheme) && !emptyString(path) && path.length() > 0 && path.charAt(0) != '/') {
            throw new URISyntaxException(path, "path doesn't start with '/'");
        }

        final StringBuilder uri = new StringBuilder();
        if ( !emptyString(scheme) ) {
            uri.append(scheme);
            uri.append(SCHEME_SEPARATOR);
        }
        if ( !emptyString(authority) ) {
            uri.append("//");
            // QUOTE ILLEGAL CHARS
            uri.append(encode(authority, AUTHORITY_LEGAL));
        }

        if ( !emptyString(path) ) {
            // QUOTE ILLEGAL CHARS
            uri.append(encode(path, PATH_LEGAL));
        }
        if ( !emptyString(query) ) {
            // QUOTE ILLEGAL CHARS
            uri.append(QUERY_SEPARATOR);
            uri.append(encode(query, QUERY_LEGAL));
        }
        if ( !emptyString(fragment) ) {
            // QUOTE ILLEGAL CHARS
            uri.append(FRAGMENT_SEPARATOR);
            uri.append(encode(fragment, FRAG_LEGAL));
        }
        return new Uri(new Encoded(uri.toString()), false, 0);
    }

    /**
     * Creates a new Uri instance using the given encoded arguments.
     * <p>
     * This constructor first creates a temporary Uri string from the given encoded encoded components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * The given encoded components are taken as-is, i.e. no re-encoding will be performed!
     * However, Uri parsing will re-evaluate encoding of the resulting components.
     * </p>
     * <p>
     * {@code [scheme:][//authority][path][?query][#fragment]}
     * </p>
     * <p>
     * {@code host} and {@code port} <i>may</i> be undefined or invalid, in the optional {@code authority}.
     * </p>
     *
     * @param scheme the encoded scheme part of the Uri.
     * @param authority the encoded authority part of the Uri.
     * @param path the encoded path to the resource on the host.
     * @param query the encoded query part of the Uri to specify parameters for the resource.
     * @param fragment the encoded fragment part of the Uri.
     *
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri create(final Encoded scheme, final Encoded authority, final Encoded path, final Encoded query, final Encoded fragment) throws URISyntaxException {
        if ( emptyString(scheme) && emptyString(authority) && emptyString(path) &&
             emptyString(query)  && emptyString(fragment) ) {
            throw new URISyntaxException("", "all empty parts");
        }
        if ( !emptyString(scheme) && !emptyString(path) && path.length() > 0 && path.charAt(0) != '/') {
            throw new URISyntaxException(path.get(), "path doesn't start with '/'");
        }

        final StringBuilder uri = new StringBuilder();
        if ( !emptyString(scheme) ) {
            uri.append(scheme);
            uri.append(SCHEME_SEPARATOR);
        }
        if ( !emptyString(authority) ) {
            uri.append("//");
            uri.append(authority.get());
        }

        if ( !emptyString(path) ) {
            uri.append(path.get());
        }
        if ( !emptyString(query) ) {
            uri.append(QUERY_SEPARATOR);
            uri.append(query.get());
        }
        if ( !emptyString(fragment) ) {
            uri.append(FRAGMENT_SEPARATOR);
            uri.append(fragment.get());
        }
        return new Uri(new Encoded(uri.toString()), false, 0);
    }

    /**
     * Casts the given encoded String to a {@link Encoded#cast(String) new Encoded instance}
     * used to create the resulting Uri instance via {@link #Uri(Encoded)}.
     * <p>
     * No encoding will be performed on the given {@code encodedUri}, use with care.
     * </p>
     * @throws URISyntaxException
     */
    public static Uri cast(final String encodedUri) throws URISyntaxException {
        return new Uri(Encoded.cast(encodedUri));
    }

    /**
     * Creates a new Uri instance using the given file-path argument.
     * <p>
     * This constructor first creates a temporary Uri string from the given components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * {@code file:path}
     * </p>
     *
     * @param path the unencoded path of the {@code file} {@code schema}.
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri valueOfFilepath(final String path) throws URISyntaxException {
        if ( emptyString(path) ) {
            throw new URISyntaxException("", "empty path");
        }
        if ( path.charAt(0) != '/' ) {
            throw new URISyntaxException(path, "path doesn't start with '/'");
        }

        final StringBuilder uri = new StringBuilder();
        uri.append(FILE_SCHEME);
        uri.append(SCHEME_SEPARATOR);

        // QUOTE ILLEGAL CHARS
        uri.append(encode(path, PATH_LEGAL));

        return new Uri(new Encoded(uri.toString()), false, 0);
    }

    /**
     * Creates a new Uri instance using the given File instance.
     * <p>
     * This constructor first creates a temporary Uri string from the given components. This
     * string will be parsed later on to create the Uri instance.
     * </p>
     * <p>
     * {@code file:path}
     * </p>
     *
     * @param file using {@link IOUtil#slashify(String, boolean, boolean) slashified} {@link File#getAbsolutePath() absolute-path}
     *             for the path of the {@code file} {@code schema}, utilizing {@link #valueOfFilepath(String)}.
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri valueOf(final File file) throws URISyntaxException {
        return Uri.valueOfFilepath(IOUtil.slashify(file.getAbsolutePath(), true, file.isDirectory()));
    }

    /**
     * Creates a new Uri instance using the given URI instance.
     * <p>
     * Re-encoding will be performed if the given URI is {@link URI#isOpaque() not opaque}.
     * </p>
     * <p>
     * See {@link #PARSE_HINT_FIX_PATH} for issues of injecting opaque URLs.
     * </p>
     *
     * @param uri A given URI instance
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri valueOf(final java.net.URI uri) throws URISyntaxException {
        if( uri.isOpaque()) {
            // opaque, without host validation.
            // Note: This may induce encoding errors of authority and path, see {@link #PARSE_HINT_FIX_PATH}
            return new Uri(new Encoded( uri.toString() ), false, 0);
        } else {
            // with host validation if authority is defined
            return Uri.create(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                              uri.getPath(), uri.getQuery(), uri.getFragment());
        }
    }

    /**
     * Creates a new Uri instance using the given URL instance,
     * convenient wrapper for {@link #valueOf(URI)} and {@link URL#toURI()}.
     * <p>
     * Re-encoding will be performed if the given URL is {@link URI#isOpaque() not opaque}, see {@link #valueOf(URI)}.
     * </p>
     * <p>
     * See {@link #PARSE_HINT_FIX_PATH} for issues of injecting opaque URLs.
     * </p>
     *
     * @param url A given URL instance
     *
     * @throws URISyntaxException
     *             if the temporary created string doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     */
    public static Uri valueOf(final java.net.URL url) throws URISyntaxException {
        return valueOf(url.toURI());
    }

    //
    // All string fields are encoded!
    //

    /** Encoded input string used at construction, never {@code null}. */
    public final Encoded input;

    private final Object lazyLock = new Object();

    /** Encoded input string used at construction, in US-ASCII encoding. */
    private ASCIIEncoded inputASCII;

    private int hash;

    /** Encoded {@code scheme}, {@code null} if undefined. */
    public final Encoded scheme;

    /** Encoded {@code scheme-specific-part}, never {@code null}. */
    public final Encoded schemeSpecificPart;
    /** Encoded {@code path} part of {@code scheme-specific-part}, never {@code null}. */
    public final Encoded path;

    /** Indicating whether {@code authority} part is defined or not. */
    public final boolean hasAuthority;
    /** Encoded {@code authority} part of {@code scheme-specific-part}, {@code null} if undefined. */
    public final Encoded authority;
    /** Encoded {@code userinfo} part of {@code authority} and {@code scheme-specific-part}, {@code null} if undefined. */
    public final Encoded userInfo; // part of authority
    /** Encoded {@code host} part of {@code authority} and {@code scheme-specific-part}, {@code null} if undefined. */
    public final Encoded host;     // part of authority
    /** Encoded {@code port} part of {@code authority} and {@code scheme-specific-part}, {@code -1} if undefined. */
    public final int port;        // part of authority

    /** Encoded {@code query} part of {@code scheme-specific-part}, {@code null} if undefined. */
    public final Encoded query;

    /** Encoded {@code fragment}, {@code null} if undefined. */
    public final Encoded fragment;

    /** Indicating whether this Uri is absolute, i.e. has a {@code scheme} and hence an absolute {@code scheme-specific-part}. */
    public final boolean absolute;

    /**
     * Indicating whether this Uri is opaque, i.e. non-hierarchical {@code scheme-specific-part}.
     * <p>
     * An opaque Uri has no {@code scheme-specific-part} being parsed,
     * i.e. {@code path}, {@code query} and {@code authority} are {@code null}.
     * </p>
     */
    public final boolean opaque;

    /**
     * Creates a new Uri instance according to the given encoded string {@code uri}.
     *
     * @param uri the RFC3986 encoded RFC2396 Uri representation to be parsed into a Uri object
     * @throws URISyntaxException
     *             if the given string {@code uri} doesn't fit to the
     *             specification RFC2396 and RFC3986 or could not be parsed correctly.
     */
    public Uri(final Encoded uri) throws URISyntaxException {
        this(uri, false, 0);
    }

    /** Returns true, if this instance is a {@code file} {@code scheme}, otherwise false. */
    public final boolean isFileScheme() {
        return null != scheme && FILE_SCHEME.equals( scheme.get() );
    }

    /**
     * Returns true, if this instance is a {@code jar} {@code scheme}, otherwise false.
     * @since 2.3.2
     */
    public final boolean isJarScheme() {
        return null != scheme && JAR_SCHEME.equals( scheme.get() );
    }

    /**
     * Returns the encoded {@link #input}, never {@code null}.
     */
    public final Encoded getEncoded() {
        return input;
    }

    /**
     * Returns the encoded {@link #input} as String, never {@code null}, same as {@link #getEncoded()}.
     */
    @Override
    public final String toString() {
        return input.get();
    }

    /**
     * Returns the encoded {@link #input} encoded in US-ASCII.
     */
    public ASCIIEncoded toASCIIString() {
        synchronized( lazyLock ) {
            if( null == inputASCII ) {
                inputASCII = new ASCIIEncoded(input.get());
            }
            return inputASCII;
        }
    }

    /**
     * Returns a new {@link URI} instance using the encoded {@link #input} string, {@code new URI(uri.input)},
     * i.e. no re-encoding will be performed.
     * @see #toURIReencoded(boolean)
     * @see #valueOf(URI)
     */
    public final java.net.URI toURI() {
        try {
            return new java.net.URI(input.get());
        } catch (final URISyntaxException e) {
            throw new Error(e); // Can't happen
        }
    }

    /**
     * Returns a new {@link URI} instance based upon this instance.
     * <p>
     * All Uri parts of this instance will be decoded
     * and encoded by the URI constructor, i.e. re-encoding will be performed.
     * </p>
     *
     * @throws URISyntaxException
     *             if the given string {@code uri} doesn't fit to the
     *             specification RFC2396 or could not be parsed correctly.
     * @see #toURI()
     * @see #valueOf(URI)
     */
    public final java.net.URI toURIReencoded() throws URISyntaxException {
        final java.net.URI recomposedURI;
        if( opaque ) {
            // opaque, without host validation
            recomposedURI = new java.net.URI(decode(scheme), decode(schemeSpecificPart), decode(fragment));
        } else if( null != host ) {
            // with host validation
            recomposedURI = new java.net.URI(decode(scheme), decode(userInfo), decode(host), port,
                                             decode(path), decode(query), decode(fragment));
        } else {
            // without host validation
            recomposedURI = new java.net.URI(decode(scheme), decode(authority),
                                             decode(path), decode(query), decode(fragment));
        }
        return recomposedURI;
    }


    /**
     * Returns a new {@link URL} instance using the encoded {@link #input} string, {@code new URL(uri.input)},
     * i.e. no re-encoding will be performed.
     * @throws MalformedURLException
     *             if an error occurs while creating the URL or no protocol
     *             handler could be found.
     */
    public final java.net.URL toURL() throws MalformedURLException {
        if (!absolute) {
            throw new IllegalArgumentException("Cannot convert relative Uri: "+input);
        }
        return new java.net.URL(input.get());
    }

    /**
     * If this instance {@link #isFileScheme() is a file scheme},
     * implementation decodes <i>[ "//"+{@link #authority} ] + {@link #path}</i>,<br>
     * then it processes the result if {@link File#separatorChar} <code> == '\\'</code>
     * as follows:
     * <ul>
     *   <li>slash -> backslash</li>
     *   <li>drop a starting single backslash, preserving windows UNC</li>
     * </ul>
     * and returns the resulting new {@link File} instance.
     * <p>
     * Otherwise implementation returns {@code null}.
     * </p>
     */
    public final File toFile() {
        if( isFileScheme() && !emptyString(path) ) {
            final String authorityS;
            if( null == authority ) {
                authorityS = "";
            } else {
                authorityS = "//"+authority.decode();
            }
            final String path = authorityS+this.path.decode();
            if( File.separator.equals("\\") ) {
                final String r = patternSingleFS.matcher(path).replaceAll("\\\\");
                if( r.startsWith("\\") && !r.startsWith("\\\\") ) { // '\\\\' denotes UNC hostname, which shall not be cut-off
                    return new File(r.substring(1));
                } else {
                    return new File(r);
                }
            }
            return new File(path);
        }
        return null;
    }

    /**
     * If this instance's {@link #schemeSpecificPart} contains a Uri itself, a sub-Uri,
     * return {@link #schemeSpecificPart} + {@code #} {@link #fragment} via it's own new Uri instance.
     * <p>
     * In case this Uri is a {@code jar-scheme}, the {@code query} is omitted,
     * since it shall be invalid for {@code jar-schemes} anyway.
     * </p>
     * <p>
     * Otherwise method returns {@code null}.
     * </p>
     * <pre>
     * Example 1:
     *     This instance: <code>jar:<i>scheme2</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class</code>
     *     Returned Uri:  <code><i>scheme2</i>:/some/path/gluegen-rt.jar</code>
     *
     * Example 2:
     *     This instance: <code>jar:<i>scheme2</i>:/some/path/gluegen-rt.jar!/com/jogamp/common/GlueGenVersion.class?lala=01#fragment</code>
     *     Returned Uri:  <code><i>scheme2</i>:/some/path/gluegen-rt.jar#fragment</code>
     *
     * Example 3:
     *     This instance: <code>scheme1:<i>scheme2</i>:/some/path/gluegen-rt.jar!/?lala=01#fragment</code>
     *     Returned Uri:  <code><i>scheme2</i>:/some/path/gluegen-rt.jar?lala=01#fragment</code>
     * </pre>
     * @throws URISyntaxException if this Uri is a container Uri and does not comply with the container spec, i.e. a JAR Uri
     */
    public final Uri getContainedUri() throws URISyntaxException {
        if( !emptyString(schemeSpecificPart) ) {
            final StringBuilder sb = new StringBuilder();

            if( isJarScheme() ) {
                final int idx = schemeSpecificPart.lastIndexOf(JAR_SCHEME_SEPARATOR);
                if (0 > idx) {
                    throw new URISyntaxException(input.get(), "missing jar separator");
                }
                sb.append( schemeSpecificPart.get().substring(0, idx) ); // exclude '!/'
            } else {
                sb.append( schemeSpecificPart.get() );
            }
            if ( !emptyString(fragment) ) {
                sb.append(FRAGMENT_SEPARATOR);
                sb.append(fragment);
            }
            try {
                final int parseHints = opaque ? PARSE_HINT_FIX_PATH : 0;
                final Uri res = new Uri(new Encoded(sb.toString()), false, parseHints);
                if( null != res.scheme ) {
                    return res;
                }
            } catch(final URISyntaxException e) {
                // OK, does not contain uri
                if( DEBUG ) {
                    System.err.println("Caught "+e.getClass().getSimpleName()+": "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static final boolean cutoffLastPathSegementImpl(final StringBuilder pathBuf,
                                                            final boolean cutoffFile,
                                                            final boolean cutoffDir,
                                                            final Encoded appendPath) throws URISyntaxException {
        final boolean cleaned;
        {// clean-up existing path
            final String pathS = pathBuf.toString();
            if( 0 > pathS.indexOf("/") && emptyString(appendPath) ) {
                return false; // nothing to cut-off
            }
            pathBuf.setLength(0);
            pathBuf.append( IOUtil.cleanPathString( pathS ) );
            cleaned = pathBuf.length() != pathS.length();
        }

        {// cut-off file or last dir-segment
            final String pathS = pathBuf.toString();
            final int jarSepIdx = pathS.lastIndexOf(JAR_SCHEME_SEPARATOR);
            final int e = pathS.lastIndexOf("/");
            if( 0 > jarSepIdx || e - 1 > jarSepIdx ) { // stop at jar-separator '!/', if exist
                if( cutoffFile && e < pathS.length() - 1 ) {
                    // cut-off file
                    pathBuf.setLength(0);
                    pathBuf.append( pathS.substring(0, e+1) );
                } else if( cutoffDir ) {
                    // cut-off dir-segment
                    final int p = pathS.lastIndexOf("/", e-1);
                    if( p >= 0 ) {
                        pathBuf.setLength(0);
                        pathBuf.append( pathS.substring(0, p+1) );
                    } // else keep
                } // else keep
            }
            final boolean cutoff = pathBuf.length() != pathS.length();
            if( !cutoff && ( cutoffDir || !cleaned ) && emptyString(appendPath) ) {
                return false; // no modifications!
            }
        }
        if( !emptyString(appendPath) ) {
            pathBuf.append(appendPath.get());
            // 2nd round of cleaning!
            final String pathS = pathBuf.toString();
            pathBuf.setLength(0);
            pathBuf.append( IOUtil.cleanPathString( pathS ) );
        }
        return true; // continue processing w/ buffer
    }
    private final Uri cutoffLastPathSegementImpl(final boolean cutoffFile, final boolean cutoffDir, final Encoded appendPath) throws URISyntaxException {
        if( opaque ) {
            if( emptyString(schemeSpecificPart) ) {
                 // nothing to cut-off
                if( !emptyString(appendPath) )  {
                    return Uri.create(scheme, appendPath, fragment);
                } else {
                    return null;
                }
            }
            final StringBuilder sspBuf = new StringBuilder(); // without path!

            // save optional query in scheme-specific-part
            final Encoded queryTemp;
            final int queryI = schemeSpecificPart.lastIndexOf(QUERY_SEPARATOR);
            if( queryI >= 0 ) {
                queryTemp = schemeSpecificPart.substring(queryI+1);
                sspBuf.append( schemeSpecificPart.substring(0, queryI).get() );
            } else {
                queryTemp = null;
                sspBuf.append( schemeSpecificPart.get() );
            }

            if( !cutoffLastPathSegementImpl(sspBuf, cutoffFile, cutoffDir, appendPath) ) {
                return null; // no modifications
            }

            if ( !emptyString(queryTemp)  ) {
                sspBuf.append(QUERY_SEPARATOR);
                sspBuf.append( queryTemp.get() );
            }

            // without host validation if authority is defined
            return Uri.create(scheme, new Encoded(sspBuf.toString()), fragment);
        } else {
            if( emptyString(path) ) {
                return null; // nothing to cut-off
            }
            final StringBuilder pathBuf = new StringBuilder();
            pathBuf.append( path.get() );

            if( !cutoffLastPathSegementImpl(pathBuf, cutoffFile, cutoffDir, appendPath) ) {
                return null; // no modifications
            }

            // with host validation if authority is defined
            return Uri.create(scheme, userInfo, host, port, new Encoded(pathBuf.toString()), query, fragment);
        }
    }

    /**
     * {@link IOUtil#cleanPathString(String) Normalizes} this Uri's path and return the
     * {@link IOUtil#cleanPathString(String) normalized} form if it differs, otherwise {@code this} instance.
     * <p>
     * <pre>
     * Example-1:
     *     This instance  : <code>jar:http://some/path/../gluegen-rt.jar!/com/Test.class?arg=1#frag</code>
     *     Normalized     : <code>jar:http://some/gluegen-rt.jar!/com/Test.class?arg=1#frag</code>
     *
     * Example-2:
     *     This instance  : <code>http://some/path/../gluegen-rt.jar?arg=1#frag</code>
     *     Normalized     : <code>http://some/gluegen-rt.jar?arg=1#frag</code>
     * </pre>
     * </p>
     */
    public final Uri getNormalized() {
        try {
            final Uri res = cutoffLastPathSegementImpl(false, false, null);
            return null != res ? res : this;
        } catch (final URISyntaxException e) {
            if( DEBUG ) {
                System.err.println("Caught "+e.getClass().getSimpleName()+": "+e.getMessage());
                e.printStackTrace();
            }
            return this;
        }
    }

    /**
     * Returns this Uri's directory Uri.
     * <p>
     * This Uri path will be {@link IOUtil#cleanPathString(String) normalized} before returning the directory.
     * </p>
     * <p>
     * If this Uri's directory cannot be found, or already denotes a directory, method returns {@code this} instance.
     * </p>
     * <p>
     * <pre>
     * Example-1:
     *     this-uri: http:/some/path/gluegen-rt.jar?arg=1#frag
     *     result:   http:/some/path/?arg=1#frag
     *
     * Example-2:
     *     this-uri: file:/some/path/
     *     result:   file:/some/path/
     *
     * Example-3:
     *     this-uri: file:/some/path/lala/lili/../../hello.txt
     *     result:   file:/some/path/
     * </pre>
     * </p>
     * @throws URISyntaxException if the new string {@code uri} doesn't fit to the
     *                            specification RFC2396 and RFC3986 or could not be parsed correctly.
     */
    public Uri getDirectory() {
        try {
            final Uri res = cutoffLastPathSegementImpl(true, false, null);
            return null != res ? res : this;
        } catch (final URISyntaxException e) {
            if( DEBUG ) {
                System.err.println("Caught "+e.getClass().getSimpleName()+": "+e.getMessage());
                e.printStackTrace();
            }
            return this;
        }
    }

    /**
     * Returns this Uri's parent directory Uri..
     * <p>
     * This Uri path will be {@link IOUtil#cleanPathString(String) normalized} before traversing up one directory.
     * </p>
     * <p>
     * If a parent folder cannot be found, method returns {@code null}.
     * </p>
     * <p>
     * <pre>
     * Example-1:
     *     This instance  : <code>jar:http://some/path/gluegen-rt.jar!/com/Test.class?arg=1#frag</code>
     *     Returned Uri #1: <code>jar:http://some/path/gluegen-rt.jar!/com/?arg=1#frag</code>
     *     Returned Uri #2: <code>jar:http://some/path/gluegen-rt.jar!/?arg=1#frag</code>
     *     Returned Uri #3: <code>null</code>
     *
     * Example-2:
     *     This instance  : <code>http://some/path/gluegen-rt.jar?arg=1#frag</code>
     *     Returned Uri #1: <code>http://some/path/?arg=1#frag</code>
     *     Returned Uri #2: <code>http://some/?arg=1#frag</code>
     *     Returned Uri #2: <code>null</code>
     *
     * Example-3:
     *     This instance  : <code>http://some/path/../gluegen-rt.jar?arg=1#frag</code>
     *     Returned Uri #1: <code>http://some/?arg=1#frag</code>
     *     Returned Uri #2: <code>null</code>
     * </pre>
     * </p>
     */
    public final Uri getParent() {
        try {
            return cutoffLastPathSegementImpl(true, true, null);
        } catch (final URISyntaxException e) {
            if( DEBUG ) {
                System.err.println("Caught "+e.getClass().getSimpleName()+": "+e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Returns a new Uri appending the given {@code appendPath}
     * to this instance's {@link #getDirectory() directory}.
     * <p>
     * If {@code appendPath} is empty, method behaves like {@link #getNormalized()}.
     * </p>
     * <p>
     * This resulting path will be {@link IOUtil#cleanPathString(String) normalized}.
     * </p>
     * <p>
     * <pre>
     * Example-1:
     *     append: null
     *     this-uri: http:/some/path/gluegen-rt.jar
     *     result:   http:/some/path/gluegen-rt.jar
     *
     * Example-2:
     *     append: test.txt
     *     this-uri: file:/some/path/gluegen-rt.jar
     *     result:   file:/some/path/test.txt
     *
     * Example-3:
     *     append: test.txt
     *     this-uri: file:/some/path/lala/lili/../../hello.txt
     *     result:   file:/some/path/test.txt
     * </pre>
     * </p>
     *
     * @param appendPath denotes a relative path to be appended to this Uri's directory
     * @throws URISyntaxException
     *             if the resulting {@code uri} doesn't fit to the
     *             specification RFC2396 and RFC3986 or could not be parsed correctly.
     */
    public Uri getRelativeOf(final Encoded appendPath) throws URISyntaxException {
        if( emptyString(appendPath) ) {
            return getNormalized();
        } else {
            return cutoffLastPathSegementImpl(true, false, appendPath);
        }
    }

    /**
     * Concatenates the given encoded string to the {@link #getEncoded() encoded uri}
     * of this instance and returns {@link #Uri(Encoded) a new Uri instance} with the result.
     *
     * @throws URISyntaxException
     *             if the concatenated string {@code uri} doesn't fit to the
     *             specification RFC2396 and RFC3986 or could not be parsed correctly.
     */
    public final Uri concat(final Encoded suffix) throws URISyntaxException {
        if( null == suffix ) {
            return this;
        } else {
            return new Uri( input.concat(suffix) );
        }
    }

    /**
     * Returns a new Uri instance w/ the given new query {@code newQuery}.
     *
     * @throws URISyntaxException if this Uri is {@link #opaque}
     *             or if the new string {@code uri} doesn't fit to the
     *             specification RFC2396 and RFC3986 or could not be parsed correctly.
     */
    public final Uri getNewQuery(final Encoded newQuery) throws URISyntaxException {
        if( opaque ) {
            throw new URISyntaxException(input.decode(), "Opaque Uri cannot permute by query");
        } else {
            // with host validation if authority is defined
            return Uri.create(scheme, userInfo, host, port, path, newQuery, fragment);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Compares this Uri instance with the given argument {@code o} and
     * determines if both are equal. Two Uri instances are equal if all single
     * parts are identical in their meaning.
     * </p>
     *
     * @param o
     *            the Uri this instance has to be compared with.
     * @return {@code true} if both Uri instances point to the same resource,
     *         {@code false} otherwise.
     */
    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof Uri)) {
            return false;
        }
        final Uri uri = (Uri) o;

        if (uri.fragment == null && fragment != null || uri.fragment != null && fragment == null) {
            return false;
        } else if (uri.fragment != null && fragment != null) {
            if (!equalsHexCaseInsensitive(uri.fragment, fragment)) {
                return false;
            }
        }

        if (uri.scheme == null && scheme != null || uri.scheme != null && scheme == null) {
            return false;
        } else if (uri.scheme != null && scheme != null) {
            if (!uri.scheme.equalsIgnoreCase(scheme)) {
                return false;
            }
        }

        if (uri.opaque && opaque) {
            return equalsHexCaseInsensitive(uri.schemeSpecificPart, schemeSpecificPart);
        } else if (!uri.opaque && !opaque) {
            if (!equalsHexCaseInsensitive(path, uri.path)) {
                return false;
            }

            if (uri.query != null && query == null || uri.query == null && query != null) {
                return false;
            } else if (uri.query != null && query != null) {
                if (!equalsHexCaseInsensitive(uri.query, query)) {
                    return false;
                }
            }

            if (uri.authority != null && authority == null || uri.authority == null && authority != null) {
                return false;
            } else if (uri.authority != null && authority != null) {
                if (uri.host != null && host == null || uri.host == null && host != null) {
                    return false;
                } else if (uri.host == null && host == null) {
                    // both are registry based, so compare the whole authority
                    return equalsHexCaseInsensitive(uri.authority, authority);
                } else { // uri.host != null && host != null, so server-based
                    if (!host.equalsIgnoreCase(uri.host)) {
                        return false;
                    }

                    if (port != uri.port) {
                        return false;
                    }

                    if ( uri.userInfo != null && userInfo == null ||
                         uri.userInfo == null && userInfo != null
                       ) {
                        return false;
                    } else if (uri.userInfo != null && userInfo != null) {
                        return equalsHexCaseInsensitive(userInfo, uri.userInfo);
                    } else {
                        return true;
                    }
                }
            } else {
                // no authority
                return true;
            }

        } else {
            // one is opaque, the other hierarchical
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Gets the hashcode value of this Uri instance.
     * </p>
     */
    @Override
    public final int hashCode() {
        synchronized( lazyLock ) {
            if (hash == -1) {
                hash = getHashString().hashCode();
            }
            return hash;
        }
    }

    /*
     * Takes a string that may contain hex sequences like %F1 or %2b and
     * converts the hex values following the '%' to lowercase
     */
    private String convertHexToLowerCase(final String s) {
        if (s.indexOf('%') == -1) {
            return s;
        }
        final StringBuilder result = new StringBuilder("");
        int index = 0, previndex = 0;
        while ((index = s.indexOf('%', previndex)) != -1) {
            result.append(s.substring(previndex, index + 1));
            result.append(s.substring(index + 1, index + 3).toLowerCase());
            index += 3;
            previndex = index;
        }
        return result.toString();
    }

    /*
     * Takes two strings that may contain hex sequences like %F1 or %2b and
     * compares them, ignoring case for the hex values. Hex values must always
     * occur in pairs as above
     */
    private boolean equalsHexCaseInsensitive(final Encoded first, final Encoded second) {
        if (first.indexOf('%') != second.indexOf('%')) {
            return first.equals(second);
        }

        int index = 0, previndex = 0;
        while ( ( index = first.indexOf('%', previndex) ) != -1 &&
                second.indexOf('%', previndex) == index
              ) {
            if( !first.get().substring(previndex, index).equals( second.get().substring(previndex, index) ) ) {
                return false;
            }
            if( !first.get().substring(index + 1, index + 3).equalsIgnoreCase( second.get().substring(index + 1, index + 3) ) ) {
                return false;
            }
            index += 3;
            previndex = index;
        }
        return first.get().substring(previndex).equals( second.get().substring(previndex) );
    }

    /*
     * Form a string from the components of this Uri, similarly to the
     * toString() method. But this method converts scheme and host to lowercase,
     * and converts escaped octets to lowercase.
     */
    private String getHashString() {
        final StringBuilder result = new StringBuilder();
        if (scheme != null) {
            result.append(scheme.get().toLowerCase());
            result.append(SCHEME_SEPARATOR);
        }
        if (opaque) {
            result.append(schemeSpecificPart.get());
        } else {
            if (authority != null) {
                result.append("//");
                if (host == null) {
                    result.append(authority.get());
                } else {
                    if (userInfo != null) {
                        result.append(userInfo.get() + "@");
                    }
                    result.append(host.get().toLowerCase());
                    if (port != -1) {
                        result.append(SCHEME_SEPARATOR + port);
                    }
                }
            }

            if (path != null) {
                result.append(path.get());
            }

            if (query != null) {
                result.append(QUERY_SEPARATOR);
                result.append(query.get());
            }
        }

        if (fragment != null) {
            result.append(FRAGMENT_SEPARATOR);
            result.append(fragment.get());
        }
        return convertHexToLowerCase(result.toString());
    }

    /**
     *
     * @param input
     * @param expectServer
     * @param parseHints TODO
     * @throws URISyntaxException
     */
    private Uri(final Encoded input, final boolean expectServer, final int parseHints) throws URISyntaxException {
        if( emptyString(input) ) {
            throw new URISyntaxException(input.get(), "empty input");
        }
        String temp = input.get();
        int index;
        // parse into Fragment, Scheme, and SchemeSpecificPart
        // then parse SchemeSpecificPart if necessary

        // Fragment
        index = temp.indexOf(FRAGMENT_SEPARATOR);
        if (index != -1) {
            // remove the fragment from the end
            fragment = new Encoded( temp.substring(index + 1) );
            validateFragment(input, fragment, index + 1);
            temp = temp.substring(0, index);
        } else {
            fragment = null;
        }

        String inputTemp = input.get(); // may get modified due to error correction

        // Scheme and SchemeSpecificPart
        final int indexSchemeSep = temp.indexOf(SCHEME_SEPARATOR);
        index = indexSchemeSep;
        final int indexSSP = temp.indexOf('/');
        final int indexQuerySep = temp.indexOf(QUERY_SEPARATOR);

        String sspTemp; // may get modified due to error correction

        // if a '/' or '?' occurs before the first ':' the uri has no
        // specified scheme, and is therefore not absolute
        if ( indexSchemeSep != -1 &&
             ( indexSSP >= indexSchemeSep || indexSSP == -1 ) &&
             ( indexQuerySep >= indexSchemeSep || indexQuerySep == -1 )
           ) {
            // the characters up to the first ':' comprise the scheme
            absolute = true;
            scheme = new Encoded( temp.substring(0, indexSchemeSep) );
            if (scheme.length() == 0) {
                failExpecting(input, "scheme", indexSchemeSep);
            }
            validateScheme(input, scheme, 0);
            sspTemp = temp.substring(indexSchemeSep + 1);
            if (sspTemp.length() == 0) {
                failExpecting(input, "scheme-specific-part", indexSchemeSep);
            }
        } else {
            absolute = false;
            scheme = null;
            sspTemp = temp;
        }

        if ( scheme == null ||  sspTemp.length() > 0 && sspTemp.charAt(0) == '/' ) {
            // Uri is hierarchical, not opaque
            opaque = false;

            // Query
            temp = sspTemp;
            index = temp.indexOf(QUERY_SEPARATOR);
            if (index != -1) {
                query = new Encoded( temp.substring(index + 1) );
                temp = temp.substring(0, index);
                validateQuery(input, query, indexSSP + 1 + index);
            } else {
                query = null;
            }

            String pathTemp; // may get modified due to error correction
            final int indexPathInSSP;

            // Authority and Path
            if (temp.startsWith("//")) {
                index = temp.indexOf('/', 2);
                final String authorityS;
                if (index != -1) {
                    authorityS = temp.substring(2, index);
                    pathTemp = temp.substring(index);
                    indexPathInSSP = index;
                } else {
                    authorityS = temp.substring(2);
                    if (authorityS.length() == 0 && query == null && fragment == null) {
                        failExpecting(input, "authority, path [, query, fragment]", index);
                    }
                    pathTemp = "";
                    indexPathInSSP = -1;
                    // nothing left, so path is empty
                    // (not null, path should never be null if hierarchical/non-opaque)
                }
                if ( emptyString(authorityS) ) {
                    authority = null;
                } else {
                    authority = new Encoded( authorityS );
                    validateAuthority(input, authority, indexSchemeSep + 3);
                }
            } else { // no authority specified
                pathTemp = temp;
                indexPathInSSP = 0;
                authority = null;
            }

            int indexPath = 0; // in input
            if (indexSSP > -1) {
                indexPath += indexSSP;
            }
            if (indexPathInSSP > -1) {
                indexPath += indexPathInSSP;
            }

            final int pathErrIdx = validateEncoded(pathTemp, PATH_LEGAL);
            if( 0 <= pathErrIdx ) {
                // Perform error correction on PATH if requested!
                if( 0 != ( parseHints & PARSE_HINT_FIX_PATH ) ) {
                    if( DEBUG_SHOWFIX ) {
                        System.err.println("Uri FIX_FILEPATH: input at index "+(indexPath+pathErrIdx)+": "+inputTemp);
                        System.err.println("Uri FIX_FILEPATH: ssp at index   "+(indexPathInSSP+pathErrIdx)+": "+sspTemp);
                        System.err.println("Uri FIX_FILEPATH: path  at index "+pathErrIdx+": "+pathTemp);
                    }
                    final int pathTempOldLen = pathTemp.length();
                    pathTemp = encode( decode( pathTemp ), PATH_LEGAL); // re-encode, and hope for the best!
                    validatePath(input, pathTemp, indexPath); // re-validate!
                    {
                        // Patch SSP + INPUT !
                        final StringBuilder sb = new StringBuilder();
                        if( indexPathInSSP > 0 ) {
                            sb.append( sspTemp.substring(0, indexPathInSSP) );
                        }
                        sb.append( pathTemp ).append( sspTemp.substring( indexPathInSSP + pathTempOldLen ) );
                        sspTemp = sb.toString(); // update

                        sb.setLength(0);
                        if( indexPath > 0 ) {
                            sb.append( inputTemp.substring(0, indexPath) );
                        }
                        sb.append( pathTemp ).append( inputTemp.substring( indexPath + pathTempOldLen ) );
                        inputTemp = sb.toString(); // update
                    }
                    if( DEBUG_SHOWFIX ) {
                        System.err.println("Uri FIX_FILEPATH: result          : "+pathTemp);
                        System.err.println("Uri FIX_FILEPATH: ssp after       : "+sspTemp);
                        System.err.println("Uri FIX_FILEPATH: input after     : "+inputTemp);
                    }
                } else {
                    fail(input, "invalid path", indexPath+pathErrIdx);
                }
            }
            path = new Encoded( pathTemp );
        } else {
            // Uri is not hierarchical, Uri is opaque
            opaque = true;
            query = null;
            path = null;
            authority = null;
            validateSsp(input, sspTemp, indexSchemeSep + 1);
        }
        schemeSpecificPart = new Encoded( sspTemp );
        this.input = inputTemp == input.get() ? input : new Encoded( inputTemp );

        /**
         * determine the host, port and userinfo if the authority parses
         * successfully to a server based authority
         *
         * Behavior in error cases: if forceServer is true, throw
         * URISyntaxException with the proper diagnostic messages. if
         * forceServer is false assume this is a registry based uri, and just
         * return leaving the host, port and userinfo fields undefined.
         *
         * and there are some error cases where URISyntaxException is thrown
         * regardless of the forceServer parameter e.g. malformed ipv6 address
         */
        Encoded tempUserinfo = null, tempHost = null;
        int tempPort = -1;
        boolean authorityComplete;

        if ( null != authority ) {
            authorityComplete = true; // set to false later
            int hostindex = 0;

            temp = authority.get();
            index = temp.indexOf('@');
            if (index != -1) {
                // remove user info
                tempUserinfo = new Encoded( temp.substring(0, index) );
                validateUserinfo(authority, tempUserinfo, 0);
                temp = temp.substring(index + 1); // host[:port] is left
                hostindex = index + 1;
            }

            index = temp.lastIndexOf(SCHEME_SEPARATOR);
            final int endindex = temp.indexOf(']');

            if (index != -1 && endindex < index) {
                // determine port and host
                tempHost = new Encoded( temp.substring(0, index) );

                if (index < (temp.length() - 1)) { // port part is not empty
                    try {
                        tempPort = Integer.parseInt(temp.substring(index + 1));
                        if (tempPort < 0) {
                            if (expectServer) {
                                fail(authority, "invalid port <"+authority+">", hostindex + index + 1);
                            }
                            authorityComplete = false;
                        }
                    } catch (final NumberFormatException e) {
                        if (expectServer) {
                            fail(authority, "invalid port <"+authority+">, "+e.getMessage(), hostindex + index + 1);
                        }
                        authorityComplete = false;
                    }
                }
            } else {
                tempHost = new Encoded( temp );
            }

            if( authorityComplete ) {
                if ( emptyString(tempHost) ) {
                    if (expectServer) {
                        fail(authority, "empty host <"+authority+">", hostindex);
                    }
                    authorityComplete = false;
                } else if (!isValidHost(expectServer, tempHost)) {
                    if (expectServer) {
                        fail(authority, "invalid host <"+tempHost+">", hostindex);
                    }
                    authorityComplete = false;
                }
            }
        } else {
            authorityComplete = false;
        }

        if( authorityComplete ) {
            // this is a server based uri,
            // fill in the userinfo, host and port fields
            userInfo = tempUserinfo;
            host = tempHost;
            port = tempPort;
            hasAuthority = true;
        } else {
            userInfo = null;
            host = null;
            port = -1;
            hasAuthority = false;
        }
    }

    private static void validateScheme(final Encoded uri, final Encoded scheme, final int index) throws URISyntaxException {
        // first char needs to be an alpha char
        final char ch = scheme.charAt(0);
        if ( !((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) ) {
            fail(uri, "invalid scheme", index);
        }
        final int errIdx = validateAlphaNum(scheme.get(), "+-.");
        if( 0 <= errIdx ) {
            fail(uri, "invalid scheme", index+errIdx);
        }
    }

    private static void validateSsp(final Encoded uri, final String ssp, final int index) throws URISyntaxException {
        final int errIdx = validateEncoded(ssp, SSP_LEGAL);
        if( 0 <= errIdx ) {
            fail(uri, "invalid scheme-specific-part", index+errIdx);
        }
    }

    private static void validateAuthority(final Encoded uri, final Encoded authority, final int index) throws URISyntaxException {
        final int errIdx = validateEncoded(authority.get(), AUTHORITY_LEGAL);
        if( 0 <= errIdx ) {
            fail(uri, "invalid authority", index+errIdx);
        }
    }

    private static void validatePath(final Encoded uri, final String path, final int index) throws URISyntaxException {
        final int errIdx = validateEncoded(path, PATH_LEGAL);
        if( 0 <= errIdx ) {
            fail(uri, "invalid path", index+errIdx);
        }
    }

    private static void validateQuery(final Encoded uri, final Encoded query, final int index) throws URISyntaxException {
        final int errIdx = validateEncoded(query.get(), QUERY_LEGAL);
        if( 0 <= errIdx ) {
            fail(uri, "invalid query", index+errIdx);
        }
    }

    private static void validateFragment(final Encoded uri, final Encoded fragment, final int index) throws URISyntaxException {
        final int errIdx = validateEncoded(fragment.get(), FRAG_LEGAL);
        if( 0 <= errIdx ) {
            fail(uri, "invalid fragment", index+errIdx);
        }
    }

    private static void validateUserinfo(final Encoded uri, final Encoded userinfo, final int index) throws URISyntaxException {
        for (int i = 0; i < userinfo.length(); i++) {
            final char ch = userinfo.charAt(i);
            if (ch == ']' || ch == '[') {
                fail(uri, "invalid userinfo", index+i);
            }
        }
    }

    /**
     * distinguish between IPv4, IPv6, domain name and validate it based on
     * its type
     */
    private boolean isValidHost(final boolean expectServer, final Encoded host) throws URISyntaxException {
        if (host.charAt(0) == '[') {
            // ipv6 address
            if (host.charAt(host.length() - 1) != ']') {
                fail(input, "invalid host, missing closing ipv6: "+host, 0);
            }
            if (!isValidIP6Address(host.get())) {
                fail(input, "invalid ipv6: "+host, 0);
            }
            return true;
        }

        // '[' and ']' can only be the first char and last char
        // of the host name
        if (host.indexOf('[') != -1 || host.indexOf(']') != -1) {
            fail(input, "invalid host: "+host, 0);
        }

        final int index = host.lastIndexOf('.');
        if ( index < 0 || index == host.length() - 1 ||
             !Character.isDigit(host.charAt(index + 1)) )
        {
            // domain name
            if (isValidDomainName(host)) {
                return true;
            }
            if (expectServer) {
                fail(input, "invalid host, invalid domain-name or ipv4: "+host, 0);
            }
            return false;
        }

        // IPv4 address
        if (isValidIPv4Address(host.get())) {
            return true;
        }
        if (expectServer) {
            fail(input, "invalid host, invalid ipv4: "+host, 0);
        }
        return false;
    }

    private static boolean isValidDomainName(final Encoded host) {
        final String hostS = host.get();
        if( 0 <= validateAlphaNum(hostS, "-.") ) {
            return false;
        }
        String label = null;
        final StringTokenizer st = new StringTokenizer(hostS, ".");
        while (st.hasMoreTokens()) {
            label = st.nextToken();
            if (label.startsWith("-") || label.endsWith("-")) {
                return false;
            }
        }

        if (!label.equals(hostS)) {
            final char ch = label.charAt(0);
            if (ch >= '0' && ch <= '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidIPv4Address(final String ipv4Address) {
        int index;
        int index2;
        try {
            int num;
            index = ipv4Address.indexOf('.');
            num = Integer.parseInt(ipv4Address.substring(0, index));
            if (num < 0 || num > 255) {
                return false;
            }
            index2 = ipv4Address.indexOf('.', index + 1);
            num = Integer.parseInt(ipv4Address.substring(index + 1, index2));
            if (num < 0 || num > 255) {
                return false;
            }
            index = ipv4Address.indexOf('.', index2 + 1);
            num = Integer.parseInt(ipv4Address.substring(index2 + 1, index));
            if (num < 0 || num > 255) {
                return false;
            }
            num = Integer.parseInt(ipv4Address.substring(index + 1));
            if (num < 0 || num > 255) {
                return false;
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    private static boolean isValidIP6Address(final String ipv6Address) {
        final int length = ipv6Address.length();
        boolean doubleColon = false;
        int numberOfColons = 0;
        int numberOfPeriods = 0;
        String word = "";
        char c = 0;
        char prevChar = 0;
        int offset = 0; // offset for [] ip addresses

        if (length < 2) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            prevChar = c;
            c = ipv6Address.charAt(i);
            switch (c) {

                // case for an open bracket [x:x:x:...x]
                case '[':
                    if (i != 0) {
                        return false; // must be first character
                    }
                    if (ipv6Address.charAt(length - 1) != ']') {
                        return false; // must have a close ]
                    }
                    if ((ipv6Address.charAt(1) == SCHEME_SEPARATOR)
                            && (ipv6Address.charAt(2) != SCHEME_SEPARATOR)) {
                        return false;
                    }
                    offset = 1;
                    if (length < 4) {
                        return false;
                    }
                    break;

                // case for a closed bracket at end of IP [x:x:x:...x]
                case ']':
                    if (i != length - 1) {
                        return false; // must be last character
                    }
                    if (ipv6Address.charAt(0) != '[') {
                        return false; // must have a open [
                    }
                    break;

                // case for the last 32-bits represented as IPv4
                // x:x:x:x:x:x:d.d.d.d
                case '.':
                    numberOfPeriods++;
                    if (numberOfPeriods > 3) {
                        return false;
                    }
                    if (!isValidIP4Word(word)) {
                        return false;
                    }
                    if (numberOfColons != 6 && !doubleColon) {
                        return false;
                    }
                    // a special case ::1:2:3:4:5:d.d.d.d allows 7 colons
                    // with
                    // an IPv4 ending, otherwise 7 :'s is bad
                    if (numberOfColons == 7
                            && ipv6Address.charAt(0 + offset) != SCHEME_SEPARATOR
                            && ipv6Address.charAt(1 + offset) != SCHEME_SEPARATOR) {
                        return false;
                    }
                    word = "";
                    break;

                case SCHEME_SEPARATOR:
                    numberOfColons++;
                    if (numberOfColons > 7) {
                        return false;
                    }
                    if (numberOfPeriods > 0) {
                        return false;
                    }
                    if (prevChar == SCHEME_SEPARATOR) {
                        if (doubleColon) {
                            return false;
                        }
                        doubleColon = true;
                    }
                    word = "";
                    break;

                default:
                    if (word.length() > 3) {
                        return false;
                    }
                    if (!isValidHexChar(c)) {
                        return false;
                    }
                    word += c;
            }
        }

        // Check if we have an IPv4 ending
        if (numberOfPeriods > 0) {
            if (numberOfPeriods != 3 || !isValidIP4Word(word)) {
                return false;
            }
        } else {
            // If we're at then end and we haven't had 7 colons then there
            // is a problem unless we encountered a doubleColon
            if (numberOfColons != 7 && !doubleColon) {
                return false;
            }

            // If we have an empty word at the end, it means we ended in
            // either a : or a .
            // If we did not end in :: then this is invalid
            if (word == "" && ipv6Address.charAt(length - 1 - offset) != SCHEME_SEPARATOR
                    && ipv6Address.charAt(length - 2 - offset) != SCHEME_SEPARATOR) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidIP4Word(final String word) {
        char c;
        if (word.length() < 1 || word.length() > 3) {
            return false;
        }
        for (int i = 0; i < word.length(); i++) {
            c = word.charAt(i);
            if (!(c >= '0' && c <= '9')) {
                return false;
            }
        }
        if (Integer.parseInt(word) > 255) {
            return false;
        }
        return true;
    }

    /**
     * Validate a string by checking if it contains any characters other than:
     * <ol>
     *   <li>letters ('a'..'z', 'A'..'Z')</li>
     *   <li>numbers ('0'..'9')</li>
     *   <li>characters in the legal-set parameter</li>
     *   <li> others (unicode characters that are not in
     *        US-ASCII set, and are not ISO Control or are not ISO Space characters)</li>
     * </ol>
     *
     * @param encoded
     *            {@code java.lang.String} the string to be validated
     * @param legal
     *            {@code java.lang.String} the characters allowed in the String
     *            s
     */
    private static int validateEncoded(final String encoded, final String legal) {
        for (int i = 0; i < encoded.length();) {
            final char ch = encoded.charAt(i);
            if (ch == '%') {
                do {
                    if (i + 2 >= encoded.length()) {
                        throw new IllegalArgumentException("missing '%' hex-digits at index "+i);
                    }
                    final int d1 = Character.digit(encoded.charAt(i + 1), 16);
                    final int d2 = Character.digit(encoded.charAt(i + 2), 16);
                    if (d1 == -1 || d2 == -1) {
                        throw new IllegalArgumentException("invalid hex-digits at index "+i+": "+encoded.substring(i, i + 3));
                    }
                    i += 3;
                } while (i < encoded.length() && encoded.charAt(i) == '%');
                continue;
            }
            if ( !( (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
                    (ch >= '0' && ch <= '9') || legal.indexOf(ch) > -1 ||
                    (ch > 127 && !Character.isSpaceChar(ch) && !Character.isISOControl(ch))
                  )
               ) {
                return i;
            }
            i++;
        }
        return -1;
    }
    private static int validateAlphaNum(final String s, final String legal) {
        for (int i = 0; i < s.length();) {
            final char ch = s.charAt(i);
            if ( !( (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
                    (ch >= '0' && ch <= '9') || legal.indexOf(ch) > -1
                  )
               ) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static boolean isValidHexChar(final char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }
    private static boolean emptyString(final Encoded s) {
        return null == s || 0 == s.length();
    }
    private static boolean emptyString(final String s) {
        return null == s || 0 == s.length();
    }

    private static void fail(final Encoded input, final String reason, final int p) throws URISyntaxException {
        throw new URISyntaxException(input.get(), reason, p);
    }
    private static void failExpecting(final Encoded input, final String expected, final int p) throws URISyntaxException {
        fail(input, "Expecting " + expected, p);
    }
}