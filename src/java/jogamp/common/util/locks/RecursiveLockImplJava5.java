package jogamp.common.util.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.jogamp.common.util.locks.RecursiveLock;

public class RecursiveLockImplJava5 implements RecursiveLock {

    volatile Thread owner = null;
    ReentrantLock lock;
    
    public RecursiveLockImplJava5(boolean fair) {
        lock = new ReentrantLock(fair);
    }
    
    public void lock() {
        try {
            if(!tryLock(TIMEOUT)) {
                throw new RuntimeException("Waited "+TIMEOUT+"ms for: "+threadName(owner)+" - "+threadName(Thread.currentThread())+", with count "+getHoldCount()+", lock: "+this);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
        owner = Thread.currentThread();
    }

    public boolean tryLock(long timeout) throws InterruptedException {
        if(lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            owner = Thread.currentThread();
            return true;
        }
        return false;
    }

    public void unlock() throws RuntimeException {
        unlock(null);
    }

    public void unlock(Runnable taskAfterUnlockBeforeNotify) {
        validateLocked();
        owner = null;
        if(null!=taskAfterUnlockBeforeNotify) {
            taskAfterUnlockBeforeNotify.run();
        }
        lock.unlock();
    }
    
    public boolean isLocked() {
        return lock.isLocked();
    }

    public Thread getOwner() {
        return owner;
    }

    public boolean isLockedByOtherThread() {
        return lock.isLocked() && !lock.isHeldByCurrentThread();
    }

    public boolean isOwner() {
        return lock.isHeldByCurrentThread();
    }

    public boolean isOwner(Thread thread) {
        return lock.isLocked() && owner == thread;
    }

    public void validateLocked() throws RuntimeException {
        if ( !lock.isHeldByCurrentThread() ) {
            if ( !lock.isLocked() ) {
                throw new RuntimeException(Thread.currentThread()+": Not locked");
            } else {
                throw new RuntimeException(Thread.currentThread()+": Not owner, owner is "+owner);
            }
        }
    }

    public int getHoldCount() {
        return lock.getHoldCount();
    }

    public int getQueueLength() {
        return lock.getQueueLength();
    }

    private String threadName(Thread t) { return null!=t ? "<"+t.getName()+">" : "<NULL>" ; }
}
