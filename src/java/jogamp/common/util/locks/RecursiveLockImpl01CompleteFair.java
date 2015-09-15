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

package jogamp.common.util.locks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;

import com.jogamp.common.util.SourcedInterruptedException;
import com.jogamp.common.util.locks.RecursiveLock;

/**
 * Reentrance locking toolkit, impl a complete fair FIFO scheduler
 *
 * <p>
 * Sync object extends {@link AbstractOwnableSynchronizer}, hence monitoring is possible.</p>
 */
public class RecursiveLockImpl01CompleteFair implements RecursiveLock {

    private static class WaitingThread {
        WaitingThread(final Thread t) {
            thread = t;
            signaledByUnlock = false;
        }
        final Thread thread;
        boolean signaledByUnlock; // if true, it's also removed from queue
    }

    @SuppressWarnings("serial")
    private static class Sync extends AbstractOwnableSynchronizer {
        private Sync() {
            super();
        }
        private final Thread getOwner() {
            return getExclusiveOwnerThread();
        }
        private final void setOwner(final Thread t) {
            setExclusiveOwnerThread(t);
        }
        private final void setLockedStack(final Throwable s) {
            final List<Throwable> ls = LockDebugUtil.getRecursiveLockTrace();
            if(s==null) {
                ls.remove(lockedStack);
            } else {
                ls.add(s);
            }
            lockedStack = s;
        }
        /** lock count by same thread */
        private int holdCount = 0;
        /** waiting thread queue */
        final ArrayList<WaitingThread> queue = new ArrayList<WaitingThread>();
        /** stack trace of the lock, only used if DEBUG */
        private Throwable lockedStack = null;
    }
    private final Sync sync = new Sync();

    public RecursiveLockImpl01CompleteFair() {
    }

    /**
     * Returns the Throwable instance generated when this lock was taken the 1st time
     * and if {@link com.jogamp.common.util.locks.Lock#DEBUG} is turned on, otherwise it returns always <code>null</code>.
     * @see com.jogamp.common.util.locks.Lock#DEBUG
     */
    public final Throwable getLockedStack() {
        synchronized(sync) {
            return sync.lockedStack;
        }
    }

    @Override
    public final Thread getOwner() {
        synchronized(sync) {
            return sync.getOwner();
        }
    }

    @Override
    public final boolean isOwner(final Thread thread) {
        synchronized(sync) {
            return sync.getOwner() == thread ;
        }
    }

    @Override
    public final boolean isLocked() {
        synchronized(sync) {
            return null != sync.getOwner();
        }
    }

    @Override
    public final boolean isLockedByOtherThread() {
        synchronized(sync) {
            final Thread o = sync.getOwner();
            return null != o && Thread.currentThread() != o ;
        }
    }

    @Override
    public final int getHoldCount() {
        synchronized(sync) {
            return sync.holdCount;
        }
    }

    @Override
    public final void validateLocked() throws RuntimeException {
        synchronized(sync) {
            if ( Thread.currentThread() != sync.getOwner() ) {
                if ( null == sync.getOwner() ) {
                    throw new RuntimeException(threadName(Thread.currentThread())+": Not locked: "+toString());
                }
                if(null!=sync.lockedStack) {
                    sync.lockedStack.printStackTrace();
                }
                throw new RuntimeException(Thread.currentThread()+": Not owner: "+toString());
            }
        }
    }

    @Override
    public final void lock() {
        synchronized(sync) {
            try {
                if(!tryLock(TIMEOUT)) {
                    if(null!=sync.lockedStack) {
                        sync.lockedStack.printStackTrace();
                    }
                    throw new RuntimeException("Waited "+TIMEOUT+"ms for: "+toString()+" - "+threadName(Thread.currentThread()));
                }
            } catch (final InterruptedException e) {
                throw new RuntimeException("Interrupted", e);
            }
        }
    }

    @Override
    public final boolean tryLock(long timeout) throws InterruptedException {
        synchronized(sync) {
            final Thread cur = Thread.currentThread();
            if(TRACE_LOCK) {
                System.err.println("+++ LOCK 0 "+toString()+", cur "+threadName(cur));
            }
            if (sync.getOwner() == cur) {
                ++sync.holdCount;
                if(TRACE_LOCK) {
                    System.err.println("+++ LOCK XR "+toString()+", cur "+threadName(cur));
                }
                return true;
            }

            if ( sync.getOwner() != null || ( 0<timeout && 0<sync.queue.size() ) ) {

                if ( 0 >= timeout ) {
                    // locked by other thread and no waiting requested
                    if(TRACE_LOCK) {
                        System.err.println("+++ LOCK XY "+toString()+", cur "+threadName(cur)+", left "+timeout+" ms");
                    }
                    return false;
                }

                // enqueue at the start
                final WaitingThread wCur = new WaitingThread(cur);
                sync.queue.add(0, wCur);
                do {
                    final long t0 = System.currentTimeMillis();
                    try {
                        sync.wait(timeout);
                        timeout -= System.currentTimeMillis() - t0;
                    } catch (final InterruptedException e) {
                        if( !wCur.signaledByUnlock ) {
                            sync.queue.remove(wCur); // O(n)
                            throw SourcedInterruptedException.wrap(e); // propagate interruption not send by unlock
                        } else if( cur != sync.getOwner() ) {
                            // Issued by unlock, but still locked by other thread
                            //
                            timeout -= System.currentTimeMillis() - t0;

                            if(TRACE_LOCK) {
                                System.err.println("+++ LOCK 1 "+toString()+", cur "+threadName(cur)+", left "+timeout+" ms, signaled: "+wCur.signaledByUnlock);
                            }

                            if(0 < timeout) {
                                // not timed out, re-enque - lock was 'stolen'
                                wCur.signaledByUnlock = false;
                                sync.queue.add(sync.queue.size(), wCur);
                            }
                        } // else: Issued by unlock, owning lock .. expected!
                    }
                } while ( cur != sync.getOwner() && 0 < timeout ) ;
                Thread.interrupted(); // clear slipped interrupt

                if( 0 >= timeout && cur != sync.getOwner() ) {
                    // timed out
                    if(!wCur.signaledByUnlock) {
                        sync.queue.remove(wCur); // O(n)
                    }
                    if(TRACE_LOCK) {
                        System.err.println("+++ LOCK XX "+toString()+", cur "+threadName(cur)+", left "+timeout+" ms");
                    }
                    return false;
                }

                ++sync.holdCount;
                if(TRACE_LOCK) {
                    System.err.println("+++ LOCK X1 "+toString()+", cur "+threadName(cur)+", left "+timeout+" ms");
                }
            } else {
                ++sync.holdCount;
                if(TRACE_LOCK) {
                    System.err.println("+++ LOCK X0 "+toString()+", cur "+threadName(cur));
                }
            }

            sync.setOwner(cur);
            if(DEBUG) {
                sync.setLockedStack(new Throwable("Previously locked by "+toString()));
            }
            return true;
        }
    }


    @Override
    public final void unlock() {
        synchronized(sync) {
            unlock(null);
        }
    }

    @Override
    public final void unlock(final Runnable taskAfterUnlockBeforeNotify) {
        synchronized(sync) {
            validateLocked();
            final Thread cur = Thread.currentThread();

            --sync.holdCount;

            if (sync.holdCount > 0) {
                if(TRACE_LOCK) {
                    System.err.println("--- LOCK XR "+toString()+", cur "+threadName(cur));
                }
                return;
            }

            if(DEBUG) {
                sync.setLockedStack(null);
            }
            if(null!=taskAfterUnlockBeforeNotify) {
                taskAfterUnlockBeforeNotify.run();
            }

            if(sync.queue.size() > 0) {
                // fair, wakeup the oldest one ..
                // final WaitingThread oldest = queue.removeLast();
                final WaitingThread oldest = sync.queue.remove(sync.queue.size()-1);
                sync.setOwner(oldest.thread);

                if(TRACE_LOCK) {
                    System.err.println("--- LOCK X1 "+toString()+", cur "+threadName(cur)+", signal: "+threadName(oldest.thread));
                }

                oldest.signaledByUnlock = true;
                oldest.thread.interrupt(); // Propagate SecurityException if it happens
            } else {
                sync.setOwner(null);
                if(TRACE_LOCK) {
                    System.err.println("--- LOCK X0 "+toString()+", cur "+threadName(cur)+", signal any");
                }
                sync.notify();
            }
        }
    }

    @Override
    public final int getQueueLength() {
        synchronized(sync) {
            return sync.queue.size();
        }
    }

    @Override
    public String toString() {
        return syncName()+"[count "+sync.holdCount+
                           ", qsz "+sync.queue.size()+", owner "+threadName(sync.getOwner())+"]";
    }

    private final String syncName() {
        return "<"+Integer.toHexString(this.hashCode())+", "+Integer.toHexString(sync.hashCode())+">";
    }
    private final String threadName(final Thread t) { return null!=t ? "<"+t.getName()+">" : "<NULL>" ; }
}

