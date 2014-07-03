package jogamp.common.util.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.jogamp.common.util.locks.RecursiveLock;

public class RecursiveLockImplJava5 implements RecursiveLock {

    volatile Thread owner = null;
    ReentrantLock lock;

    public RecursiveLockImplJava5(final boolean fair) {
        lock = new ReentrantLock(fair);
    }

    @Override
    public void lock() {
        try {
            if(!tryLock(TIMEOUT)) {
                throw new RuntimeException("Waited "+TIMEOUT+"ms for: "+threadName(owner)+" - "+threadName(Thread.currentThread())+", with count "+getHoldCount()+", lock: "+this);
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
        owner = Thread.currentThread();
    }

    @Override
    public boolean tryLock(final long timeout) throws InterruptedException {
        if(lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            owner = Thread.currentThread();
            return true;
        }
        return false;
    }

    @Override
    public void unlock() throws RuntimeException {
        unlock(null);
    }

    @Override
    public void unlock(final Runnable taskAfterUnlockBeforeNotify) {
        validateLocked();
        owner = null;
        if(null!=taskAfterUnlockBeforeNotify) {
            taskAfterUnlockBeforeNotify.run();
        }
        lock.unlock();
    }

    @Override
    public boolean isLocked() {
        return lock.isLocked();
    }

    @Override
    public Thread getOwner() {
        return owner;
    }

    @Override
    public boolean isLockedByOtherThread() {
        return lock.isLocked() && !lock.isHeldByCurrentThread();
    }

    @Override
    public boolean isOwner(final Thread thread) {
        return lock.isLocked() && owner == thread;
    }

    @Override
    public void validateLocked() throws RuntimeException {
        if ( !lock.isHeldByCurrentThread() ) {
            if ( !lock.isLocked() ) {
                throw new RuntimeException(Thread.currentThread()+": Not locked");
            } else {
                throw new RuntimeException(Thread.currentThread()+": Not owner, owner is "+owner);
            }
        }
    }

    @Override
    public int getHoldCount() {
        return lock.getHoldCount();
    }

    @Override
    public int getQueueLength() {
        return lock.getQueueLength();
    }

    private String threadName(final Thread t) { return null!=t ? "<"+t.getName()+">" : "<NULL>" ; }
}
