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

package com.jogamp.common.util.locks;

import com.jogamp.common.impl.Debug;
import java.security.AccessController;

import java.util.LinkedList;

/**
 * Reentrance locking toolkit, impl a complete fair FIFO scheduler
 */
public class RecursiveLock implements LockExt {

    static class SyncData {
        // owner of the lock
        Thread owner = null; 
        // lock recursion
        int recursionCount = 0; 
        // stack trace of the lock, only used if DEBUG
        Throwable lockedStack = null;
        // waiting thread queue
        LinkedList threadQueue = new LinkedList(); 
        // flag signaling unlock has woken up a waiting thread
        boolean signaled = false; 
    }
    private SyncData sdata = new SyncData(); // synchronized (flow/mem)  mutable access

    private static final boolean TRACE_LOCK = Debug.isPropertyDefined("jogamp.debug.Lock.TraceLock", true, AccessController.getContext());

    public RecursiveLock() {
    }

    /**
     * Returns the Throwable instance generated when this lock was taken the 1st time
     * and if {@link com.jogamp.common.util.locks.Lock#DEBUG} is turned on, otherwise it returns always <code>null</code>.
     * @see com.jogamp.common.util.locks.Lock#DEBUG
     */
    public final Throwable getLockedStack() {
        synchronized(sdata) {
            return sdata.lockedStack;
        }
    }

    public final Thread getOwner() {
        synchronized(sdata) {
            return sdata.owner;
        }
    }

    public final boolean isOwner() {
        return isOwner(Thread.currentThread());
    }

    public final boolean isOwner(Thread thread) {
        synchronized(sdata) {
            return sdata.owner == thread ;
        }
    }

    public final boolean isLocked() {
        synchronized(sdata) {
            return null != sdata.owner;
        }
    }

    public final boolean isLockedByOtherThread() {
        synchronized(sdata) {
            return null != sdata.owner && Thread.currentThread() != sdata.owner ;
        }
    }

    public final int getRecursionCount() {
        synchronized(sdata) {
            return sdata.recursionCount;
        }
    }

    public final void validateLocked() {
        synchronized(sdata) {
            if ( null == sdata.owner ) {
                throw new RuntimeException(Thread.currentThread()+": Not locked");
            }
            if ( Thread.currentThread() != sdata.owner ) {
                if(null!=sdata.lockedStack) {
                    sdata.lockedStack.printStackTrace();
                }
                throw new RuntimeException(Thread.currentThread()+": Not owner, owner is "+sdata.owner);
            }
        }
    }

    public final void lock() {
        synchronized(sdata) {
            if(!tryLock(TIMEOUT)) {
                if(null!=sdata.lockedStack) {
                    sdata.lockedStack.printStackTrace();
                }
                throw new RuntimeException("Waited "+TIMEOUT+"ms for: "+sdata.owner+" - "+Thread.currentThread()+", with recursionCount "+sdata.recursionCount+", lock: "+this+", qsz "+sdata.threadQueue.size());
            }
        }
    }

    public boolean tryLock(long maxwait) {
        synchronized(sdata) {
            Thread cur = Thread.currentThread();
            if(TRACE_LOCK) {
                String msg = "LOCK 0 ["+this+"], recursions "+sdata.recursionCount+", cur "+cur+", owner "+sdata.owner;
                System.err.println(msg);
                //Throwable tt = new Throwable(msg);
                //tt.printStackTrace();
            }
            if (sdata.owner == cur) {
                ++sdata.recursionCount;
                if(TRACE_LOCK) {
                    System.err.println("+++ LOCK 2 ["+this+"], recursions "+sdata.recursionCount+", "+cur);
                }
                return true;
            }

            if ( sdata.owner != null || 
                 0 < maxwait && ( sdata.signaled || sdata.threadQueue.size() > 0 ) ) {

                if ( 0 >= maxwait ) {
                    // implies 'sdata.owner != null': locked by other thread
                    // no waiting requested, bail out right away
                    return false;
                }

                boolean timedOut = false;
                do {
                    sdata.threadQueue.addFirst(cur); // should only happen once 
                    try {
                        sdata.wait(maxwait);
                        timedOut = sdata.threadQueue.remove(cur); // TIMEOUT if not already removed by unlock
                    } catch (InterruptedException e) {
                        if(!sdata.signaled) {
                            // theoretically we could stay in the loop,
                            // in case the interrupt wasn't issued by unlock, 
                            // hence the re-enqueue
                            sdata.threadQueue.remove(cur);
                            if(TRACE_LOCK) {
                                System.err.println("XXX LOCK - ["+this+"], recursions "+sdata.recursionCount+", "+cur);
                            }
                        }
                    }
                } while (null != sdata.owner && !timedOut) ;

                sdata.signaled = false;

                if(timedOut || null != sdata.owner) {
                    return false;
                }

                if(TRACE_LOCK) {
                    System.err.println("+++ LOCK 3 ["+this+"], recursions "+sdata.recursionCount+", qsz "+sdata.threadQueue.size()+", "+cur);
                }
            } else if(TRACE_LOCK) {
                System.err.println("+++ LOCK 1 ["+this+"], recursions "+sdata.recursionCount+", qsz "+sdata.threadQueue.size()+", "+cur);
            }

            sdata.owner = cur;
            if(DEBUG) {
                sdata.lockedStack = new Throwable("Previously locked by "+sdata.owner+", lock: "+this);
            }
            return true;
        }
    }
    

    public final void unlock() {
        unlock(null);
    }

    public final void unlock(Runnable taskAfterUnlockBeforeNotify) {
        synchronized(sdata) {
            validateLocked();

            if (sdata.recursionCount > 0) {
                --sdata.recursionCount;
                if(TRACE_LOCK) {
                    System.err.println("--- LOCK 1 ["+this+"], recursions "+sdata.recursionCount+", "+Thread.currentThread());
                }
                return;
            }
            sdata.owner = null;
            sdata.lockedStack = null;
            if(null!=taskAfterUnlockBeforeNotify) {
                taskAfterUnlockBeforeNotify.run();
            }

            int qsz = sdata.threadQueue.size();
            if(qsz > 0) {
                Thread parkedThread = (Thread) sdata.threadQueue.removeLast();
                if(TRACE_LOCK) {
                    System.err.println("--- LOCK X ["+this+"], recursions "+sdata.recursionCount+
                                       ", "+Thread.currentThread()+", irq "+(qsz-1)+": "+parkedThread);
                }
                sdata.signaled = true;
                if(qsz==1) {
                    // fast path, just one waiting thread
                    sdata.notify();
                } else {
                    // signal the oldest one ..
                    parkedThread.interrupt(); // Propagate SecurityException if it happens
                }
            } else if(TRACE_LOCK) {
                System.err.println("--- LOCK X ["+this+"], recursions "+sdata.recursionCount+", "+Thread.currentThread());
            }
        }
    }
}

