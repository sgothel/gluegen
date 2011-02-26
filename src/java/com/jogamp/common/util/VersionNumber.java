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

public class VersionNumber implements Comparable {

    protected int major;
    protected int minor;
    protected int sub;

    public VersionNumber(int majorRev, int minorRev, int subMinorRev) {
        major = majorRev;
        minor = minorRev;
        sub = subMinorRev;
    }

    protected VersionNumber() { }

    @Override
    public final int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + major;
        hash = ((hash << 5) - hash) + minor;
        return ((hash << 5) - hash) + sub;
    }

    @Override
    public final boolean equals(Object o) {
        return 0 == compareTo(o);
    }
    
    public final int compareTo(Object o) {
        if ( ! ( o instanceof VersionNumber ) ) {
            Class c = (null != o) ? o.getClass() : null ;
            throw new ClassCastException("Not a Capabilities object: " + c);
        }

        VersionNumber vo = (VersionNumber) o;
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
        return 0; // they are equal
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
