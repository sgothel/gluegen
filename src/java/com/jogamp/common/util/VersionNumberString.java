/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

/**
 * {@link VersionNumber} specialization, holding the <code>versionString</code>
 * this instance is derived from.
 */
public class VersionNumberString extends VersionNumber {

    /**
     * A {@link #isZero() zero} version instance, w/o any component defined explicitly.
     * @see #hasMajor()
     * @see #hasMinor()
     * @see #hasSub()
     */
    public static final VersionNumberString zeroVersion = new VersionNumberString(0, 0, 0, -1, (short)0, "n/a");

    protected final String strVal;

    protected VersionNumberString(final int majorRev, final int minorRev, final int subMinorRev, final int strEnd, final short _state, final String versionString) {
        super(majorRev, minorRev, subMinorRev, strEnd, _state);
        strVal = versionString;
    }

    /**
     * See {@link VersionNumber#VersionNumber(int, int, int)}.
     */
    public VersionNumberString(final int majorRev, final int minorRev, final int subMinorRev, final String versionString) {
        this(majorRev, minorRev, subMinorRev, -1, (short)(HAS_MAJOR | HAS_MINOR | HAS_SUB), versionString);
    }

    /**
     * See {@link VersionNumber#VersionNumber(String)}.
     */
    public VersionNumberString(final String versionString) {
        super( versionString);
        strVal = versionString;
    }

    /**
     * See {@link VersionNumber#VersionNumber(String, String)}.
     */
    public VersionNumberString(final String versionString, final String delim) {
        super( versionString, delim);
        strVal = versionString;
    }

    /**
     * See {@link VersionNumber#VersionNumber(String, java.util.regex.Pattern)}.
     */
    public VersionNumberString(final String versionString, final java.util.regex.Pattern versionPattern) {
        super( versionString, versionPattern);
        strVal = versionString;
    }

    /** Returns the version string this version number is derived from. */
    public final String getVersionString() { return strVal; }

    @Override
    public String toString() {
        return super.toString() + " ("+strVal+")" ;
    }
}
