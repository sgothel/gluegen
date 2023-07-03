/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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
package com.jogamp.gluegen.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.jogamp.common.util.HashUtil;

public class NativeObjectMap {
    private static class UserPtr {
        final long aptr;

        public UserPtr(final long userPtr) {
            this.aptr = userPtr;
        }
        @Override
        public boolean equals(final Object o) {
            if( this == o ) {
                return true;
            }
            if( !(o instanceof UserPtr) ) {
                return false;
            }
            final UserPtr o2 = (UserPtr)o;
            return aptr == o2.aptr;
        }
        @Override
        public int hashCode() {
            return HashUtil.getAddrHash32_EqualDist(aptr);
        }
        @Override
        public String toString() {
            return "UserPtr[aptr 0x "+Long.toHexString(aptr)+"]";
        }
    }
    private final Map<UserPtr, Object> nativeToObj = new HashMap<UserPtr, Object>();
    final AtomicLong NEXT_ID = new AtomicLong(0);

    public final long getNextId() {
         return NEXT_ID.getAndIncrement();
    }

    public synchronized Object getObj(final long aptr) {
        return nativeToObj.get(new UserPtr(aptr));
    }
    public synchronized Object putObj(final long aptr, final Object obj) {
        return nativeToObj.put(new UserPtr(aptr), obj);
    }
    public synchronized Object removeObj(final long aptr) {
        return nativeToObj.remove(new UserPtr(aptr));
    }
}
