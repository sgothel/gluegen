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
public class RunnableTask implements Runnable {
    Runnable runnable;
    final Object syncObject;
    boolean catchExceptions;
    Object attachment;

    Throwable runnableException;
    long ts0, ts1, ts2;

    /**
     * Create a RunnableTask object w/o synchronization,
     * ie. not suitable for <code>invokeAndWait()</code>. 
     * 
     * @param runnable the user action
     */
    public RunnableTask(Runnable runnable) {
        this(runnable, null, false);
    }

    /**
     * Create a RunnableTask object w/ synchronization,
     * ie. suitable for <code>invokeAndWait()</code>. 
     * 
     * @param runnable the user action
     * @param syncObject the synchronization object the caller shall wait for in case of <code>invokeAndWait()</code> 
     */
    public RunnableTask(Runnable runnable, Object syncObject) {
        this(runnable, syncObject, false);
    }

    /**
     * Create a RunnableTask object w/ synchronization,
     * ie. suitable for <code>invokeAndWait()</code>. 
     * 
     * @param runnable the user action
     * @param syncObject the synchronization object the caller shall wait for in case of <code>invokeAndWai()t</code> 
     * @param catchExceptions if true, exception during <code>runnable</code> execution are catched, otherwise not.
     *                        Use {@link #getThrowable()} to determine whether an exception has been catched. 
     */
    public RunnableTask(Runnable runnable, Object syncObject, boolean catchExceptions) {
        this.runnable = runnable ;
        this.syncObject = syncObject ;
        this.catchExceptions = catchExceptions ;
        ts0 = System.currentTimeMillis();
        ts1 = 0;
        ts2 = 0;
    }

    /** Return the user action */
    public Runnable getRunnable() {
        return runnable;
    }

    /** 
     * Return the synchronization object if any.
     * @see #RunnableTask(Runnable, Object, boolean) 
     */
    public Object getSyncObject() {
        return syncObject;
    }
    
    /** 
     * Attach a custom object to this task. 
     * Useful to piggybag further information, ie tag a task final. 
     */
    public void setAttachment(Object o) {
        attachment = o;
    }

    /** 
     * Return the attachment object if any.
     * @see #setAttachment(Object) 
     */
    public Object getAttachment() {
        return attachment;
    }

    public void run() {
        ts1 = System.currentTimeMillis();
        if(null == syncObject) {
            try {
                runnable.run();
            } catch (Throwable t) {
                runnableException = t;
                if(!catchExceptions) {
                    throw new RuntimeException(runnableException);
                }
            } finally {
                ts2 = System.currentTimeMillis();
            }
        } else {
            synchronized (syncObject) {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    runnableException = t;
                    if(!catchExceptions) {
                        throw new RuntimeException(runnableException);
                    }
                } finally {
                    ts2 = System.currentTimeMillis();
                    syncObject.notifyAll();
                }
            }
        }
    }

    /**
     * @return True if executed, otherwise false;
     */
    public boolean isExecuted() { return 0 != ts2 ; }

    /**
     * @return True if invoking thread waits until done, 
     *         ie a <code>notifyObject</code> was passed, otherwise false;
     */
    public boolean hasWaiter() { return null != syncObject; }

    /**
     * @return A thrown exception while execution of the user action, if any and if catched
     * @see #RunnableTask(Runnable, Object, boolean)
     */
    public Throwable getThrowable() { return runnableException; }

    public long getTimestampCreate() { return ts0; }
    public long getTimestampBeforeExec() { return ts1; }
    public long getTimestampAfterExec() { return ts2; }
    public long getDurationInQueue() { return ts1 - ts0; }
    public long getDurationInExec() { return ts2 - ts1; }
    public long getDurationTotal() { return ts2 - ts0; }

    @Override
    public String toString() {
        return "RunnableTask[executed "+isExecuted()+", t2-t0 "+getDurationTotal()+", t2-t1 "+getDurationInExec()+", t1-t0 "+getDurationInQueue()+", throwable "+getThrowable()+", Runnable "+runnable+", Attachment "+attachment+"]";
    }
}

