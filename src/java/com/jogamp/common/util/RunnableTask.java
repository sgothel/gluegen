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
    Object notifyObject;
    boolean catchExceptions;
    Object attachment;

    Throwable runnableException;
    long ts0, ts1, ts2;

    public RunnableTask(Runnable runnable) {
        this(runnable, null, false);
    }

    public RunnableTask(Runnable runnable, Object notifyObject) {
        this(runnable, notifyObject, false);
    }

    public RunnableTask(Runnable runnable, Object notifyObject, boolean catchExceptions) {
        this.runnable = runnable ;
        this.notifyObject = notifyObject ;
        this.catchExceptions = catchExceptions ;
        ts0 = System.currentTimeMillis();
        ts1 = 0;
        ts2 = 0;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    /** 
     * Attach a custom object to this task. 
     * Useful to piggybag further information, ie tag a task final. 
     */
    public void setAttachment(Object o) {
        attachment = o;
    }

    public Object getAttachment() {
        return attachment;
    }

    public void run() {
        ts1 = System.currentTimeMillis();
        if(null == notifyObject) {
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
            synchronized (notifyObject) {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    runnableException = t;
                    if(!catchExceptions) {
                        throw new RuntimeException(runnableException);
                    }
                } finally {
                    ts2 = System.currentTimeMillis();
                    notifyObject.notifyAll();
                }
            }
        }
    }

    /**
     * @return True if executed, otherwise false;
     */
    public boolean isExecuted() { return 0 != ts2 ; }

    /**
     * @return A Throwable thrown while execution if any
     */
    public Throwable getThrowable() { return runnableException; }

    public long getTimestampCreate() { return ts0; }
    public long getTimestampBeforeExec() { return ts1; }
    public long getTimestampAfterExec() { return ts2; }
    public long getDurationInQueue() { return ts1 - ts0; }
    public long getDurationInExec() { return ts2 - ts1; }
    public long getDurationTotal() { return ts2 - ts0; }

    public String toString() {
        return "RunnableTask[executed "+isExecuted()+", t2-t0 "+getDurationTotal()+", t2-t1 "+getDurationInExec()+", t1-t0 "+getDurationInQueue()+", throwable "+getThrowable()+", Runnable "+runnable+", Attachment "+attachment+"]";
    }
}

