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

import java.util.StringTokenizer;
import java.util.regex.Matcher;

public class VersionNumber implements Comparable<Object> {

    /** int[3] { major, minor, sub } */
    protected int[] val = new int[] { 0, 0, 0 };

    public VersionNumber(int majorRev, int minorRev, int subMinorRev) {
        val[0] = majorRev;
        val[1] = minorRev;
        val[2] = subMinorRev;
    }

    /**
     * Parser first tokenizes the input versionString w/ given delimiter.
     * <p>
     * Tokens represent the major, minor and sub version number component in this order.
     * </p>
     * <p>
     * For each token it ignores any leading non-digit and uses only contiguous digits, i.e. ignores pending non-digits. 
     * </p>
     * @param versionString should be given as [MAJOR[.MINOR[.SUB]]]
     * @param delim the delimiter, e.g. "."
     */
    public VersionNumber(String versionString, String delim) {
        // group1: \D* == leading non digits, optional
        // group2: \d* == digits
        // group3: .*  == any pending chars, optional
        final java.util.regex.Pattern nonDigitsCutOff = java.util.regex.Pattern.compile("(\\D*)(\\d*)(.*)");
        final StringTokenizer tok = new StringTokenizer(versionString, delim);
        for(int n=0; tok.hasMoreTokens() && n<3; n++) {
            try {
                final Matcher matcher = nonDigitsCutOff.matcher( tok.nextToken() );
                if(matcher.matches()) {
                    val[n] = Integer.parseInt(matcher.group(2));
                }
            } catch (Exception e) { }
        }
    }
    
    protected VersionNumber() { }

    @Override
    public final int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + val[0];
        hash = ((hash << 5) - hash) + val[1];
        return ((hash << 5) - hash) + val[2];
    }

    @Override
    public final boolean equals(Object o) {
        if ( o instanceof VersionNumber ) {
            return 0 == compareTo( (VersionNumber) o );
        }
        return false;
    }
    
    @Override
    public final int compareTo(Object o) {
        if ( ! ( o instanceof VersionNumber ) ) {
            Class<?> c = (null != o) ? o.getClass() : null ;
            throw new ClassCastException("Not a VersionNumber object: " + c);
        }
        return compareTo( (VersionNumber) o );
    }

    public final int compareTo(VersionNumber vo) {
        if (val[0] > vo.val[0]) {        // major
            return 1;
        } else if (val[0] < vo.val[0]) { // major
            return -1;
        } else if (val[1] > vo.val[1]) { // minor
            return 1;
        } else if (val[1] < vo.val[1]) { // minor
            return -1;
        } else if (val[2] > vo.val[2]) { // sub
            return 1;
        } else if (val[2] < vo.val[2]) { // sub
            return -1;
        }
        return 0; // they are equal
    }
    
    public final int getMajor() {
        return val[0];
    }

    public final int getMinor() {
        return val[1];
    }

    public final int getSub() {
        return val[2];
    }

    @Override
    public String toString() {
        return getMajor() + "." + getMinor() + "." + getSub() ;
    }
}
