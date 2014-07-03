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

import java.io.PrintStream;

/**
 * Helper class to provide a Runnable queue implementation with a Runnable wrapper
 * which notifies after execution for the <code>invokeAndWait()</code> semantics.
 */
public class RunnableTask extends TaskBase {
    protected final Runnable runnable;

    /**
     * Invokes <code>runnable</code> on the current thread.
     * @param waitUntilDone if <code>true</code>, waits until <code>runnable</code> execution is completed, otherwise returns immediately.
     * @param runnable the {@link Runnable} to execute.
     */
    public static void invoke(final boolean waitUntilDone, final Runnable runnable) {
        Throwable throwable = null;
        final Object sync = new Object();
        final RunnableTask rt = new RunnableTask( runnable, waitUntilDone ? sync : null, true, waitUntilDone ? null : System.err );
        synchronized(sync) {
            rt.run();
            if( waitUntilDone ) {
                try {
                    sync.wait();
                } catch (final InterruptedException ie) {
                    throwable = ie;
                }
                if(null==throwable) {
                    throwable = rt.getThrowable();
                }
                if(null!=throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        }
    }

    /**
     * Invokes <code>runnable</code> on a new thread belonging to the given {@link ThreadGroup}.
     * @param tg the {@link ThreadGroup} for the new thread, maybe <code>null</code>
     * @param waitUntilDone if <code>true</code>, waits until <code>runnable</code> execution is completed, otherwise returns immediately.
     * @param runnable the {@link Runnable} to execute on the new thread. If <code>waitUntilDone</code> is <code>true</code>,
     *                 the runnable <b>must exist</b>, i.e. not loop forever.
     * @param threadName the name for the new thread
     * @return the newly created {@link Thread}
     */
    public static Thread invokeOnNewThread(final ThreadGroup tg, final boolean waitUntilDone, final Runnable runnable, final String threadName) {
        final Thread t = new Thread(tg, threadName) {
            @Override
            public void run() {
                Throwable throwable = null;
                final Object sync = new Object();
                final RunnableTask rt = new RunnableTask( runnable, waitUntilDone ? sync : null, true, waitUntilDone ? null : System.err );
                synchronized(sync) {
                    rt.run();
                    if( waitUntilDone ) {
                        try {
                            sync.wait();
                        } catch (final InterruptedException ie) {
                            throwable = ie;
                        }
                        if(null==throwable) {
                            throwable = rt.getThrowable();
                        }
                        if(null!=throwable) {
                            throw new RuntimeException(throwable);
                        }
                    }
                }
            } };
        t.start();
        return t;
    }


    /**
     * Create a RunnableTask object w/ synchronization,
     * ie. suitable for <code>invokeAndWait()</code>, i.e. {@link #invoke(boolean, Runnable) invoke(true, runnable)}.
     *
     * @param runnable The user action
     * @param syncObject The synchronization object if caller wait until <code>runnable</code> execution is completed,
     *                   or <code>null</code> if waiting is not desired.
     * @param catchExceptions Influence an occurring exception during <code>runnable</code> execution.
     *                        If <code>true</code>, the exception is silenced and can be retrieved via {@link #getThrowable()},
     *                        otherwise the exception is thrown.
     * @param exceptionOut If not <code>null</code>, exceptions are written to this {@link PrintStream}.
     */
    public RunnableTask(final Runnable runnable, final Object syncObject, final boolean catchExceptions, final PrintStream exceptionOut) {
        super(syncObject, catchExceptions, exceptionOut);
        this.runnable = runnable ;
    }

    /** Return the user action */
    public final Runnable getRunnable() {
        return runnable;
    }

    @Override
    public final void run() {
        runnableException = null;
        tStarted = System.currentTimeMillis();
        if(null == syncObject) {
            try {
                runnable.run();
            } catch (final Throwable t) {
                runnableException = t;
                if(null != exceptionOut) {
                    exceptionOut.println("RunnableTask.run(): "+getExceptionOutIntro()+" exception occured on thread "+Thread.currentThread().getName()+": "+toString());
                    printSourceTrace();
                    runnableException.printStackTrace(exceptionOut);
                }
                if(!catchExceptions) {
                    throw new RuntimeException(runnableException);
                }
            } finally {
                tExecuted = System.currentTimeMillis();
            }
        } else {
            synchronized (syncObject) {
                try {
                    runnable.run();
                } catch (final Throwable t) {
                    runnableException = t;
                    if(null != exceptionOut) {
                        exceptionOut.println("RunnableTask.run(): "+getExceptionOutIntro()+" exception occured on thread "+Thread.currentThread().getName()+": "+toString());
                        printSourceTrace();
                        t.printStackTrace(exceptionOut);
                    }
                    if(!catchExceptions) {
                        throw new RuntimeException(runnableException);
                    }
                } finally {
                    tExecuted = System.currentTimeMillis();
                    syncObject.notifyAll();
                }
            }
        }
    }
}

