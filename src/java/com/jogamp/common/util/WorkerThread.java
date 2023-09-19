/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A re-{@link #start()}'able, {@link #pause(boolean)}'able and interrupt'able worker {@link Thread}
 * with an optional minimum execution duration, see {@link #getSleptDuration()}.
 */
public class WorkerThread {
    /**
     * An interruptible {@link #run()} task.
     */
    public interface Callback {
        void run() throws InterruptedException;
    }

    private static AtomicInteger instanceId = new AtomicInteger(0);
    private volatile boolean isRunning = false;
    private volatile boolean isActive = false;
    private volatile boolean isBlocked = false;

    private volatile boolean shallPause = true;
    private volatile boolean shallStop = false;
    private Exception streamErr = null;
    private final Duration minPeriod;
    private final boolean useMinPeriod;
    private final Callback cbWork;
    private final Runnable cbInitLocked;
    private final Runnable cbEndLocked;
    private final boolean isDaemonThread;
    private Thread thread;
    private volatile Duration sleptDuration = Duration.ZERO;

    /**
     * Instantiates a new {@link WorkerThread}.
     * @param minPeriod minimum work-loop-period to throttle execution or {@code null} if unthrottled, see {@link #getSleptDuration()}
     * @param daemonThread argument for {@link Thread#setDaemon(boolean)}
     * @param work the actual work {@link Callback} to perform.
     */
    public WorkerThread(final Duration minPeriod, final boolean daemonThread, final Callback work) {
        this(minPeriod, daemonThread, work, null, null);
    }

    /**
     * Instantiates a new {@link WorkerThread}.
     * @param minPeriod minimum work-loop-period to throttle execution or {@code null} if unthrottled, see {@link #getSleptDuration()}
     * @param daemonThread argument for {@link Thread#setDaemon(boolean)}
     * @param work the actual work {@link Callback} to perform.
     * @param init optional initialization {@link Runnable} called at {@link #start()} while locked
     * @param end optional release {@link Runnable} called at {@link #stop()} while locked
     */
    public WorkerThread(final Duration minPeriod, final boolean daemonThread, final Callback work, final Runnable init, final Runnable end) {
        this.minPeriod = null != minPeriod ? minPeriod : Duration.ZERO;
        this.useMinPeriod = this.minPeriod.toMillis() > 0;
        this.cbWork = work;
        this.cbInitLocked = init;
        this.cbEndLocked = end;
        this.isDaemonThread = daemonThread;
        thread = null;
    }

    /**
     * Starts execution of a new worker thread if not {@link #isRunning}, i.e. never {@link #start()}'ed or {@link #stop()}'ed.
     * <p>
     * Method blocks until the new worker thread has started, {@link #isRunning()} and also {@link #isActive()}
     * </p>
     */
    public final synchronized void start() {
        start(false);
    }

    /**
     * Starts execution of a new worker thread if not {@link #isRunning}, i.e. never {@link #start()}'ed or {@link #stop()}'ed.
     * <p>
     * Method blocks until the new worker thread has been started and {@link #isRunning()} and {@link #isActive()} if {@code paused == false}.
     * </p>
     * @param paused if {@code true}, keeps the new worker thread paused, otherwise {@link #resume()} it.
     */
    public final synchronized void start(final boolean paused) {
        if( isRunning ) {
            return;
        }
        shallStop = false;
        shallPause = true;
        thread = new Thread(threadRunnable);
        thread.setDaemon(isDaemonThread);
        thread.start();
        try {
            this.notifyAll();  // wake-up startup-block
            while( !isRunning && !shallStop ) {
                this.wait();  // wait until started
            }
        } catch (final InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        }
        if( !paused ) {
            resume();
        }
    }

    /**
     * Stops execution of the {@link #start()}'ed worker thread.
     * <p>
     * Method blocks until worker thread has stopped.
     * </p>
     */
    public final synchronized void stop() {
        if( isRunning ) {
            shallStop = true;
            if( java.lang.Thread.currentThread() != thread ) {
                if( isBlocked && isRunning ) {
                    thread.interrupt();
                }
                try {
                    this.notifyAll();  // wake-up pause-block (opt)
                    while( isRunning ) {
                        this.wait();  // wait until stopped
                    }
                } catch (final InterruptedException e) {
                    throw new InterruptedRuntimeException(e);
                }
            }
            thread = null;
            shallStop = false;
            shallPause = true;
        }
    }

    /** Pauses execution of the {@link #start()}'ed worker thread. */
    public final synchronized void pause(final boolean waitUntilDone) {
        if( isActive ) {
            shallPause = true;
            if( java.lang.Thread.currentThread() != thread ) {
                if( isBlocked && isActive ) {
                    thread.interrupt();
                }
                if( waitUntilDone ) {
                    try {
                        while( isActive && isRunning ) {
                            this.wait(); // wait until paused
                        }
                    } catch (final InterruptedException e) {
                        throw new InterruptedRuntimeException(e);
                    }
                }
            }
        }
    }

    /** Resumes execution of the {@link #pause(boolean)}'ed worker thread. */
    public final synchronized void resume() {
        if( isRunning && !isActive ) {
            shallPause = false;
            if( java.lang.Thread.currentThread() != thread ) {
                try {
                    this.notifyAll();  // wake-up pause-block
                    while( !isActive && !shallPause && isRunning ) {
                        this.wait(); // wait until resumed
                    }
                } catch (final InterruptedException e) {
                    pause(false);
                    throw new InterruptedRuntimeException(e);
                }
            }
        }
    }

    /** Returns true if the worker thread has started via {@link #start()} and has not ended, e.g. via {@link #stop()}. It might be {@link #pause(boolean) paused}. */
    public final boolean isRunning() { return isRunning; }
    /** Returns true if the worker thread has started via {@link #start()} and has not ended, e.g. via {@link #stop()} and is not {@link #pause(boolean) paused}. */
    public final boolean isActive() { return isActive; }

    /**
     * Returns enforced minimum work-loop-period or {@link Duration#ZERO} for none.
     * @see #getSleptDuration()
     */
    public final Duration getMinPeriod() { return minPeriod; }

    /**
     * Returns the slept {@link Duration} delta of {@link #getMinPeriod()} and consumed {@link Callback#run()} duration.
     * <p>
     * Returns {@link Duration#ZERO zero} for {@link Duration#ZERO zero} {@link #getMinPeriod()} or exceeding {@link Callback#run()} duration.
     * </p>
     */
    public final Duration getSleptDuration() { return sleptDuration; }

    @Override
    public String toString() {
        synchronized(this) {
            return "Worker[running "+isRunning+", active "+isActive+", blocked "+isBlocked+
                    ", shall[pause "+shallPause+", stop "+shallStop+
                    "], minPeriod[set "+minPeriod.toMillis()+"ms, sleptDelta "+sleptDuration.toMillis()+
                    "ms], daemon "+isDaemonThread+", thread "+thread+"]";
        }
    }

    private final Runnable threadRunnable = new Runnable() {
        @Override
        public final void run() {
            final Thread ct = Thread.currentThread();
            ct.setName(ct.getName()+"-Worker_"+instanceId.getAndIncrement());

            synchronized ( WorkerThread.this ) {
                if( null != cbInitLocked ) {
                    cbInitLocked.run();
                }
                isRunning = true;
                WorkerThread.this.notifyAll(); // wake-up ctor()
            }

            while( !shallStop ) {
                try {
                    if( shallPause ) {
                        synchronized ( WorkerThread.this ) {
                            while( shallPause && !shallStop ) {
                                isActive = false;
                                WorkerThread.this.notifyAll(); // wake-up doPause()
                                try {
                                    WorkerThread.this.wait();  // wait until resumed
                                } catch (final InterruptedException e) {
                                    if( !shallPause ) {
                                        throw new InterruptedRuntimeException(e);
                                    }
                                }
                            }
                            isActive = true;
                            WorkerThread.this.notifyAll(); // wake-up doResume()
                        }
                    }
                    if( !shallStop ) {
                        final Instant t0 = Instant.now();
                        isBlocked = true;
                        {
                            cbWork.run();
                        }
                        isBlocked = false;
                        if( useMinPeriod ) {
                            final Instant t1 = Instant.now();
                            final Duration td = Duration.between(t0, t1);
                            if( minPeriod.compareTo(td) > 0 ) {
                                final Duration sleepMinPeriodDelta = minPeriod.minus(td);
                                final long tdMinMS = sleepMinPeriodDelta.toMillis();
                                if( tdMinMS > 0 ) {
                                    sleptDuration = sleepMinPeriodDelta;
                                    java.lang.Thread.sleep( tdMinMS );
                                } else {
                                    sleptDuration = Duration.ZERO;
                                }
                                // java.util.concurrent.locks.LockSupport.parkNanos(tdMin.toNanos());
                            } else {
                                sleptDuration = Duration.ZERO;
                            }
                        }
                    }
                } catch (final InterruptedException e) {
                    if( !isBlocked ) { // !shallStop && !shallPause
                        streamErr = new InterruptedRuntimeException(e);
                    }
                    isBlocked = false;
                    sleptDuration = Duration.ZERO;
                } catch (final Throwable t) {
                    streamErr = new Exception(t.getClass().getSimpleName()+" while processing", t);
                    sleptDuration = Duration.ZERO;
                } finally {
                    if( null != streamErr ) {
                        // state transition incl. notification
                        synchronized ( WorkerThread.this ) {
                            shallPause = true;
                            isActive = false;
                            WorkerThread.this.notifyAll(); // wake-up potential do*()
                        }
                        pause(false);
                    }
                }
            }
            synchronized ( WorkerThread.this ) {
                if( null != cbEndLocked ) {
                    cbEndLocked.run();
                }
                isRunning = false;
                isActive = false;
                WorkerThread.this.notifyAll(); // wake-up doStop()
            }
        } };

}
