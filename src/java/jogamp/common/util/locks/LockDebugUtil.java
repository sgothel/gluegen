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

package jogamp.common.util.locks;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jogamp.common.util.locks.Lock;

/**
 * Functionality enabled if {@link Lock#DEBUG} is <code>true</code>.
 */
public class LockDebugUtil {
    private static final ThreadLocal<ArrayList<Throwable>> tlsLockedStacks;
    private static final List<Throwable> dummy;
    static {
        if(Lock.DEBUG) {
            tlsLockedStacks = new ThreadLocal<ArrayList<Throwable>>();
            dummy = null;
        } else {
            tlsLockedStacks = null;
            dummy = new ArrayList<Throwable>(0);
        }
    }

    public static List<Throwable> getRecursiveLockTrace() {
        if(Lock.DEBUG) {
            ArrayList<Throwable> ls = tlsLockedStacks.get();
            if(null == ls) {
                ls = new ArrayList<Throwable>();
                tlsLockedStacks.set(ls);
            }
            return ls;
        } else {
            return dummy;
        }
    }

    public static void dumpRecursiveLockTrace(final PrintStream out) {
        if(Lock.DEBUG) {
            final List<Throwable> ls = getRecursiveLockTrace();
            if(null!=ls && ls.size()>0) {
                int j=0;
                out.println("TLSLockedStacks: locks "+ls.size());
                for(final Iterator<Throwable> i=ls.iterator(); i.hasNext(); j++) {
                    out.print(j+": ");
                    i.next().printStackTrace(out);
                }
            }
        }
    }
}
