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

import java.util.List;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;

import com.jogamp.common.util.locks.RecursiveLock;

/**
 * Reentrance locking toolkit, impl a non-complete fair FIFO scheduler.
 * <p>
 * Fair scheduling is not guaranteed due to the usage of {@link Object#notify()},
 * however new lock-applicants will wait if queue is not empty for {@link #lock()}
 * and {@link #tryLock(long) tryLock}(timeout>0).</p>
 *
 * <p>
 * Sync object extends {@link AbstractOwnableSynchronizer}, hence monitoring is possible.</p>
 */
public class RecursiveLockImpl01Unfairish implements RecursiveLock {

    /* package */ static interface Sync {
        Thread getOwner();
        boolean isOwner(Thread t);
        void setOwner(Thread t);

        Throwable getLockedStack();
        void setLockedStack(Throwable s);

        int getHoldCount();
        void incrHoldCount(Thread t);
        void decrHoldCount(Thread t);

        int getQSz();
        void incrQSz();
        void decrQSz();
    }

    @SuppressWarnings("serial")
    /* package */ static class SingleThreadSync extends AbstractOwnableSynchronizer implements Sync {
        /* package */ SingleThreadSync() {
            super();
        }
        @Override
        public final Thread getOwner() {
            return getExclusiveOwnerThread();
        }
        @Override
        public boolean isOwner(final Thread t) {
            return getExclusiveOwnerThread()==t;
        }
        @Override
        public final void setOwner(final Thread t) {
            setExclusiveOwnerThread(t);
        }
        @Override
        public final Throwable getLockedStack() {
            return lockedStack;
        }
        @Override
        public final void setLockedStack(final Throwable s) {
            final List<Throwable> ls = LockDebugUtil.getRecursiveLockTrace();
            if(s==null) {
                ls.remove(lockedStack);
            } else {
                ls.add(s);
            }
            lockedStack = s;
        }
        @Override
        public final int getHoldCount() { return holdCount; }
        @Override
        public void incrHoldCount(final Thread t) { holdCount++; }
        @Override
        public void decrHoldCount(final Thread t) { holdCount--; }

        @Override
        public final int getQSz() { return qsz; }
        @Override
        public final void incrQSz() { qsz++; }
        @Override
        public final void decrQSz() { qsz--; }

        /** lock count by same thread */
        private int holdCount = 0;
        /** queue size of waiting threads */
        private int qsz = 0;
        /** stack trace of the lock, only used if DEBUG */
        private Throwable lockedStack = null;
    }

    protected final Sync sync;

    public RecursiveLockImpl01Unfairish(final Sync sync) {
        this.sync = sync;
    }

    public RecursiveLockImpl01Unfairish() {
        this(new SingleThreadSync());
    }

    /**
     * Returns the Throwable instance generated when this lock was taken the 1st time
     * and if {@link com.jogamp.common.util.locks.Lock#DEBUG} is turned on, otherwise it returns always <code>null</code>.
     * @see com.jogamp.common.util.locks.Lock#DEBUG
     */
    public final Throwable getLockedStack() {
        synchronized(sync) {
            return sync.getLockedStack();
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
            return sync.isOwner(thread);
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
            return sync.getHoldCount();
        }
    }

    @Override
    public final void validateLocked() throws RuntimeException {
        synchronized(sync) {
            if ( !sync.isOwner(Thread.currentThread()) ) {
                if ( null == sync.getOwner() ) {
                    throw new RuntimeException(threadName(Thread.currentThread())+": Not locked: "+toString());
                }
                if(null!=sync.getLockedStack()) {
                    sync.getLockedStack().printStackTrace();
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
                    if(null!=sync.getLockedStack()) {
                        sync.getLockedStack().printStackTrace();
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
                System.err.println("+++ LOCK 0 "+toString()+", timeout "+timeout+" ms, cur "+threadName(cur));
            }
            if (sync.isOwner(cur)) {
                sync.incrHoldCount(cur);
                if(TRACE_LOCK) {
                    System.err.println("+++ LOCK XR "+toString()+", cur "+threadName(cur));
                }
                return true;
            }

            if ( sync.getOwner() != null || ( 0<timeout && 0<sync.getQSz() ) ) {

                if ( 0 >= timeout ) {
                    // locked by other thread and no waiting requested
                    if(TRACE_LOCK) {
                        System.err.println("+++ LOCK XY "+toString()+", cur "+threadName(cur)+", left "+timeout+" ms");
                    }
                    return false;
                }

                sync.incrQSz();
                do {
                    final long t0 = System.currentTimeMillis();
                    sync.wait(timeout);
                    timeout -= System.currentTimeMillis() - t0;
                } while (null != sync.getOwner() && 0 < timeout) ;
                sync.decrQSz();

                if( 0 >= timeout && sync.getOwner() != null ) {
                    // timed out
                    if(TRACE_LOCK) {
                        System.err.println("+++ LOCK XX "+toString()+", cur "+threadName(cur)+", left "+timeout+" ms");
                    }
                    return false;
                }

                if(TRACE_LOCK) {
                    System.err.println("+++ LOCK X1 "+toString()+", cur "+threadName(cur)+", left "+timeout+" ms");
                }
            } else if(TRACE_LOCK) {
                System.err.println("+++ LOCK X0 "+toString()+", cur "+threadName(cur));
            }

            sync.setOwner(cur);
            sync.incrHoldCount(cur);

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
    public void unlock(final Runnable taskAfterUnlockBeforeNotify) {
        synchronized(sync) {
            validateLocked();
            final Thread cur = Thread.currentThread();

            sync.decrHoldCount(cur);

            if (sync.getHoldCount() > 0) {
                if(TRACE_LOCK) {
                    System.err.println("--- LOCK XR "+toString()+", cur "+threadName(cur));
                }
                return;
            }

            sync.setOwner(null);
            if(DEBUG) {
                sync.setLockedStack(null);
            }
            if(null!=taskAfterUnlockBeforeNotify) {
                taskAfterUnlockBeforeNotify.run();
            }

            if(TRACE_LOCK) {
                System.err.println("--- LOCK X0 "+toString()+", cur "+threadName(cur)+", signal any");
            }
            sync.notify();
        }
    }

    @Override
    public final int getQueueLength() {
        synchronized(sync) {
            return sync.getQSz();
        }
    }

    @Override
    public String toString() {
        return syncName()+"[count "+sync.getHoldCount()+
                           ", qsz "+sync.getQSz()+", owner "+threadName(sync.getOwner())+"]";
    }

    /* package */ final String syncName() {
        return "<"+Integer.toHexString(this.hashCode())+", "+Integer.toHexString(sync.hashCode())+">";
    }
    /* package */ final String threadName(final Thread t) { return null!=t ? "<"+t.getName()+">" : "<NULL>" ; }
}

