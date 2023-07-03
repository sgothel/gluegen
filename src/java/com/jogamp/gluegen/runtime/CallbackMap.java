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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.HashUtil;
import com.jogamp.common.util.ReflectionUtil;

public class CallbackMap {
    public static class NativeUserObj {
        final long aptr;

        public NativeUserObj(final long userPtr) {
            this.aptr = userPtr;
        }
        @Override
        public boolean equals(final Object o) {
            if( this == o ) {
                return true;
            }
            if( !(o instanceof NativeUserObj) ) {
                return false;
            }
            final NativeUserObj o2 = (NativeUserObj)o;
            return aptr == o2.aptr;
        }
        @Override
        public int hashCode() {
            return HashUtil.getAddrHash32_EqualDist(aptr);
        }
        @Override
        public String toString() {
            return "JavaCallback.NativeUserObj[aptr 0x "+Long.toHexString(aptr)+"]";
        }
    }
    public static class Glue {
        final long userPtr;
        final Object userObj;
        final Object callbackObj;
        final Method callbackMethod;

        public Glue(final long userPtr, final Object userObj, final Object callbackObj, final String methodName, final String[] argTypeNames) {
            this.userPtr = userPtr;
            this.userObj = userObj;
            this.callbackObj = callbackObj;
            final Class<?>[] argTypes = new Class<?>[argTypeNames.length];
            for(int i=0; i<argTypeNames.length; ++i) {
                argTypes[i] = ReflectionUtil.getClass(argTypeNames[i], true, callbackObj.getClass().getClassLoader());
            }
            callbackMethod = ReflectionUtil.getMethod(callbackObj.getClass(), methodName, argTypes);
        }
        public Object invoke(final Object[] args) {
            return ReflectionUtil.callMethod(callbackObj, callbackMethod, args);
        }
        @Override
        public boolean equals(final Object o) {
            if( this == o ) {
                return true;
            }
            if( !(o instanceof Glue) ) {
                return false;
            }
            final Glue o2 = (Glue)o;
            return userPtr == o2.userPtr;
        }
        @Override
        public int hashCode() {
            return HashUtil.getAddrHash32_EqualDist(userPtr);
        }
        @Override
        public String toString() {
            return "JavaCallbackGlue[this 0x"+Long.toHexString(System.identityHashCode(this))+
                   ", userPtr 0x "+Long.toHexString(userPtr)+", userObj 0x"+Long.toHexString(System.identityHashCode(userObj))+"]";
        }
    }
    private final Map<NativeUserObj, Glue> userToGlue = new HashMap<NativeUserObj, Glue>();
    final AtomicLong NEXT_ID = new AtomicLong(0);

    public long getNextId() {
         return NEXT_ID.getAndIncrement();
    }

    public synchronized Glue getGlue(final NativeUserObj usrObj) {
        return userToGlue.get(usrObj);
    }
    public synchronized Glue putGlue(final NativeUserObj usrObj, final Glue glue) {
        return userToGlue.put(usrObj, glue);
    }
    public synchronized Glue removeGlue(final NativeUserObj usrObj) {
        return userToGlue.remove(usrObj);
    }

    public synchronized Object invoke(final long usrPtr, final Object[] args) {
        final Glue g = getGlue(new NativeUserObj(usrPtr));
        if( null == g ) {
            throw new JogampRuntimeException("Could not resolve usrPtr 0x"+Long.toHexString(usrPtr));
        }
        return g.invoke(args);
    }
}
