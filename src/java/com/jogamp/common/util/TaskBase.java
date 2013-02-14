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
 
package com.jogamp.common.util;

/**
 * Helper class to provide a Runnable queue implementation with a Runnable wrapper
 * which notifies after execution for the <code>invokeAndWait()</code> semantics.
 */
public abstract class TaskBase implements Runnable {
    protected final Object syncObject;
    protected final boolean catchExceptions;
    
    protected Object attachment;
    protected Throwable runnableException;
    protected long tCreated, tStarted;
    protected volatile long tExecuted;
    protected volatile boolean isFlushed;

    protected TaskBase(Object syncObject, boolean catchExceptions) {
        this.syncObject = syncObject ;
        this.catchExceptions = catchExceptions ;
        tCreated = System.currentTimeMillis();
        tStarted = 0;
        tExecuted = 0;
        isFlushed = false;
    }

    /** 
     * Return the synchronization object if any.
     * @see #RunnableTask(Runnable, Object, boolean) 
     */
    public final Object getSyncObject() {
        return syncObject;
    }
    
    /** 
     * Attach a custom object to this task. 
     * Useful to piggybag further information, ie tag a task final. 
     */
    public final void setAttachment(Object o) {
        attachment = o;
    }

    /** 
     * Return the attachment object if any.
     * @see #setAttachment(Object) 
     */
    public final Object getAttachment() {
        return attachment;
    }

    @Override
    public abstract void run();
    
    /** 
     * Simply flush this task and notify a waiting executor.
     * The executor which might have been blocked until notified
     * will be unblocked and the task removed from the queue.
     * 
     * @see #isFlushed()
     * @see #isInQueue()
     */ 
    public final void flush() {
        if(!isExecuted() && hasWaiter()) {
            synchronized (syncObject) {
                isFlushed = true;
                syncObject.notifyAll();                
            }
        }
    }

    /**
     * @return !{@link #isExecuted()} && !{@link #isFlushed()}
     */
    public final boolean isInQueue() { return 0 != tExecuted && !isFlushed; }
    
    /**
     * @return True if executed, otherwise false;
     */
    public final boolean isExecuted() { return 0 != tExecuted ; }

    /**
     * @return True if flushed, otherwise false;
     */
    public final boolean isFlushed() { return isFlushed; }

    /**
     * @return True if invoking thread waits until done, 
     *         ie a <code>notifyObject</code> was passed, otherwise false;
     */
    public final boolean hasWaiter() { return null != syncObject; }

    /**
     * @return A thrown exception while execution of the user action, if any and if catched
     * @see #RunnableTask(Runnable, Object, boolean)
     */
    public final Throwable getThrowable() { return runnableException; }

    public final long getTimestampCreate() { return tCreated; }
    public final long getTimestampBeforeExec() { return tStarted; }
    public final long getTimestampAfterExec() { return tExecuted; }
    public final long getDurationInQueue() { return tStarted - tCreated; }
    public final long getDurationInExec() { return tExecuted - tStarted; }
    public final long getDurationTotal() { return tExecuted - tCreated; }

    @Override
    public String toString() {
        return "RunnableTask[executed "+isExecuted()+", t2-t0 "+getDurationTotal()+", t2-t1 "+getDurationInExec()+", t1-t0 "+getDurationInQueue()+", throwable "+getThrowable()+", Attachment "+attachment+"]";
    }
}

