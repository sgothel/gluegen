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

public class HashUtil {
    /**
     * Generates a 32bit equally distributed identity hash value
     * from <code>addr</code> avoiding XOR collision.
     */
    public static int getAddrHash32_EqualDist(final long addr) {
        // avoid xor collisions of low/high parts
        // 31 * x == (x << 5) - x
        final int  hash = 31 +              (int)   addr          ; // lo addr
        return ((hash << 5) - hash) + (int) ( addr >>> 32 ) ; // hi addr
    }

    /**
     * Generates a 32bit equally distributed identity hash value
     * from <code>addr</code> and <code>size</code> avoiding XOR collision.
     */
    public static int getAddrSizeHash32_EqualDist(final long addr, final long size) {
        // avoid xor collisions of low/high parts
        // 31 * x == (x << 5) - x
        int  hash = 31 +              (int)   addr          ; // lo addr
        hash = ((hash << 5) - hash) + (int) ( addr >>> 32 ) ; // hi addr
        hash = ((hash << 5) - hash) + (int)   size          ; // lo size
        return ((hash << 5) - hash) + (int) ( size >>> 32 ) ; // hi size
    }

    /**
     * Generates a 64bit equally distributed hash value
     * from <code>addr</code> and <code>size</code> avoiding XOR collisions.
     */
    public static long getHash64(final long addr, final long size) {
        // 31 * x == (x << 5) - x
        final long hash = 31 + addr;
        return ((hash << 5) - hash) + size;
    }


}
