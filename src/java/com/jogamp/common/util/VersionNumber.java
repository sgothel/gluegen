/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import java.util.regex.Matcher;

/**
 * Simple version number class containing a version number
 * either being {@link #VersionNumber(int, int, int) defined explicit}
 * or {@link #VersionNumber(String, String) derived from a string}.
 * <p>
 * For the latter case, you can query whether a component has been defined explicitly by the given <code>versionString</code>,
 * via {@link #hasMajor()}, {@link #hasMinor()} and {@link #hasSub()}.
 * </p>
 * <p>
 * The state whether a component is defined explicitly <i>is not considered</i>
 * in the {@link #hashCode()}, {@link #equals(Object)} or {@link #compareTo(Object)} methods,
 * since the version number itself is treated regardless.
 * </p>
 */
public class VersionNumber implements Comparable<Object> {

    /**
     * A {@link #isZero() zero} version instance, w/o any component defined explicitly.
     * @see #hasMajor()
     * @see #hasMinor()
     * @see #hasSub()
     */
    public static final VersionNumber zeroVersion = new VersionNumber(0, 0, 0, -1, (short)0);

    /**
     * Returns the {@link java.util.regex.Pattern pattern}
     * with Perl regular expression:
     * <pre>
     *   "\\D*(\\d+)[^\\"+delim+"\\s]*(?:\\"+delim+"\\D*(\\d+)[^\\"+delim+"\\s]*(?:\\"+delim+"\\D*(\\d+))?)?"
     * </pre>
     * </p>
     * <p>
     * A whitespace within the version number will end the parser.
     * </p>
     * <p>
     * Capture groups represent the major (1), optional minor (2) and optional sub version number (3) component in this order.
     * </p>
     * <p>
     * Each capture group ignores any leading non-digit and uses only contiguous digits, i.e. ignores pending non-digits.
     * </p>
     * @param delim the delimiter, e.g. "."
     */
    public static java.util.regex.Pattern getVersionNumberPattern(final String delim) {
        return java.util.regex.Pattern.compile("\\D*(\\d+)[^\\"+delim+"\\s]*(?:\\"+delim+"\\D*(\\d+)[^\\"+delim+"\\s]*(?:\\"+delim+"\\D*(\\d+))?)?");
    }

    /**
     * Returns the default {@link java.util.regex.Pattern pattern} using {@link #getVersionNumberPattern(String)}
     * with delimiter "<b>.</b>".
     * <p>
     * Instance is cached.
     * </p>
     */
    public static java.util.regex.Pattern getDefaultVersionNumberPattern() {
        if( null == defPattern ) { // volatile dbl-checked-locking OK
            synchronized( VersionNumber.class ) {
                if( null == defPattern ) {
                    defPattern = getVersionNumberPattern(".");
                }
            }
        }
        return defPattern;
    }
    private static volatile java.util.regex.Pattern defPattern = null;

    protected final int major, minor, sub, strEnd;

    protected final short state;
    protected final static short HAS_MAJOR = 1 << 0 ;
    protected final static short HAS_MINOR = 1 << 1 ;
    protected final static short HAS_SUB   = 1 << 2 ;

    protected VersionNumber(final int majorRev, final int minorRev, final int subMinorRev, final int _strEnd, final short _state) {
        major = majorRev;
        minor = minorRev;
        sub   = subMinorRev;
        strEnd = _strEnd;
        state = _state;
    }

    /**
     * Explicit version number instantiation, with all components defined explicitly.
     * @see #hasMajor()
     * @see #hasMinor()
     * @see #hasSub()
     */
    public VersionNumber(final int majorRev, final int minorRev, final int subMinorRev) {
        this(majorRev, minorRev, subMinorRev, -1, (short)(HAS_MAJOR | HAS_MINOR | HAS_SUB));
    }

    /**
     * String derived version number instantiation.
     * <p>
     * Utilizing the default {@link java.util.regex.Pattern pattern} parser with delimiter "<b>.</b>", see {@link #getDefaultVersionNumberPattern()}.
     * </p>
     * <p>
     * You can query whether a component has been defined explicitly by the given <code>versionString</code>,
     * via {@link #hasMajor()}, {@link #hasMinor()} and {@link #hasSub()}.
     * </p>
     * @param versionString should be given as [MAJOR[.MINOR[.SUB]]]
     *
     * @see #hasMajor()
     * @see #hasMinor()
     * @see #hasSub()
     */
    public VersionNumber(final String versionString) {
        this(versionString, getDefaultVersionNumberPattern());
    }

    /**
     * String derived version number instantiation.
     * <p>
     * Utilizing {@link java.util.regex.Pattern pattern} parser created via {@link #getVersionNumberPattern(String)}.
     * </p>
     * <p>
     * You can query whether a component has been defined explicitly by the given <code>versionString</code>,
     * via {@link #hasMajor()}, {@link #hasMinor()} and {@link #hasSub()}.
     * </p>
     * @param versionString should be given as [MAJOR[.MINOR[.SUB]]]
     * @param delim the delimiter, e.g. "."
     *
     * @see #hasMajor()
     * @see #hasMinor()
     * @see #hasSub()
     */
    public VersionNumber(final String versionString, final String delim) {
        this(versionString, getVersionNumberPattern(delim));
    }

    /**
     * String derived version number instantiation.
     * <p>
     * You can query whether a component has been defined explicitly by the given <code>versionString</code>,
     * via {@link #hasMajor()}, {@link #hasMinor()} and {@link #hasSub()}.
     * </p>
     * @param versionString should be given as [MAJOR[.MINOR[.SUB]]]
     * @param versionPattern the {@link java.util.regex.Pattern pattern} parser, must be compatible w/ {@link #getVersionNumberPattern(String)}
     *
     * @see #hasMajor()
     * @see #hasMinor()
     * @see #hasSub()
     */
    public VersionNumber(final String versionString, final java.util.regex.Pattern versionPattern) {
        // group1: \d* == digits major
        // group2: \d* == digits minor
        // group3: \d* == digits sub
        final int[] val = new int[3];
        int _strEnd = 0;
        short _state = 0;
        try {
            final Matcher matcher = versionPattern.matcher( versionString );
            if( matcher.lookingAt() ) {
                _strEnd = matcher.end();
                final int groupCount = matcher.groupCount();
                if( 1 <= groupCount ) {
                    val[0] = Integer.parseInt(matcher.group(1));
                    _state = HAS_MAJOR;
                    if( 2 <= groupCount ) {
                        val[1] = Integer.parseInt(matcher.group(2));
                        _state |= HAS_MINOR;
                        if( 3 <= groupCount ) {
                            val[2] = Integer.parseInt(matcher.group(3));
                            _state |= HAS_SUB;
                        }
                    }
                }
            }
        } catch (final Exception e) { }

        major = val[0];
        minor = val[1];
        sub   = val[2];
        strEnd = _strEnd;
        state = _state;
    }

    /** Returns <code>true</code>, if all version components are zero, otherwise <code>false</code>. */
    public final boolean isZero() {
        return major == 0 && minor == 0 && sub == 0;
    }

    /** Returns <code>true</code>, if the major component is defined explicitly, otherwise <code>false</code>. Undefined components has the value <code>0</code>. */
    public final boolean hasMajor() { return 0 != ( HAS_MAJOR & state ); }
    /** Returns <code>true</code>, if the optional minor component is defined explicitly, otherwise <code>false</code>. Undefined components has the value <code>0</code>. */
    public final boolean hasMinor() { return 0 != ( HAS_MINOR & state ); }
    /** Returns <code>true</code>, if the optional sub component is defined explicitly, otherwise <code>false</code>. Undefined components has the value <code>0</code>. */
    public final boolean hasSub()   { return 0 != ( HAS_SUB & state ); }

    /**
     * If constructed with <code>version-string</code>, returns the string offset <i>after</i> the last matching character,
     * or <code>0</code> if none matched, or <code>-1</code> if not constructed with a string.
     */
    public final int endOfStringMatch() { return strEnd; }

    @Override
    public final int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + major;
        hash = ((hash << 5) - hash) + minor;
        return ((hash << 5) - hash) + sub;
    }

    @Override
    public final boolean equals(final Object o) {
        if ( o instanceof VersionNumber ) {
            return 0 == compareTo( (VersionNumber) o );
        }
        return false;
    }

    @Override
    public final int compareTo(final Object o) {
        if ( ! ( o instanceof VersionNumber ) ) {
            final Class<?> c = (null != o) ? o.getClass() : null ;
            throw new ClassCastException("Not a VersionNumber object: " + c);
        }
        return compareTo( (VersionNumber) o );
    }

    public final int compareTo(final VersionNumber vo) {
        if (major > vo.major) {
            return 1;
        } else if (major < vo.major) {
            return -1;
        } else if (minor > vo.minor) {
            return 1;
        } else if (minor < vo.minor) {
            return -1;
        } else if (sub > vo.sub) {
            return 1;
        } else if (sub < vo.sub) {
            return -1;
        }
        return 0;
    }

    public final int getMajor() {
        return major;
    }

    public final int getMinor() {
        return minor;
    }

    public final int getSub() {
        return sub;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + sub ;
    }
}
