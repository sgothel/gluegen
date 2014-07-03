/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package com.jogamp.common.util.locks;

import jogamp.common.util.locks.RecursiveLockImpl01CompleteFair;
import jogamp.common.util.locks.RecursiveLockImpl01Unfairish;
import jogamp.common.util.locks.RecursiveLockImplJava5;
import jogamp.common.util.locks.RecursiveThreadGroupLockImpl01Unfairish;

public class LockFactory {

    public enum ImplType {
        Int01(0), Java5(1), Int02ThreadGroup(2);

        public final int id;

        ImplType(final int id){
            this.id = id;
        }
    }

    /** default is ImplType.Int01, unfair'ish (fastest w/ least deviation) */
    public static RecursiveLock createRecursiveLock() {
        return new RecursiveLockImpl01Unfairish();
    }

    /** default is ImplType.Int02ThreadGroup, unfair'ish (fastest w/ least deviation) */
    public static RecursiveThreadGroupLock createRecursiveThreadGroupLock() {
        return new RecursiveThreadGroupLockImpl01Unfairish();
    }

    public static RecursiveLock createRecursiveLock(final ImplType t, final boolean fair) {
        switch(t) {
            case Int01:
                return fair ? new RecursiveLockImpl01CompleteFair() : new RecursiveLockImpl01Unfairish();
            case Java5:
                return new RecursiveLockImplJava5(fair);
            case Int02ThreadGroup:
                return new RecursiveThreadGroupLockImpl01Unfairish();
        }
        throw new InternalError("XXX");
    }

}
