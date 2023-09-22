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
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A re-{@link #start()}'able, {@link #pause(boolean)}'able and interrupt'able worker {@link #getThread() thread}
 * with an optional minimum execution duration, see {@link #getSleptDuration()}
 * executing a {@link Callback#run(WorkerThread) task} periodically.
 * <p>
 * Optionally a {@link WorkerThread.StateCallback.State state} {@link StateCallback#run(WorkerThread, WorkerThread.StateCallback.State) task}
 * can be given for fine grained control.
 * </p>
 * <p>
 * If an exception occurs during execution of the work {@link Callback}, the worker {@link #getThread() thread} is {@link #pause(boolean)}'ed
 * and {@link #hasError()} as well as {@link #getError(boolean)} can be used to query and clear the state.
 * User may {@link #resume()} or {@link #stop()} the thread.
 * </p>
 * <p>
 * If an exception occurs during execution of the optional
 * {@link WorkerThread.StateCallback.State state} {@link StateCallback#run(WorkerThread, WorkerThread.StateCallback.State) task},
 * the worker {@link #getThread() thread} is {@link #stop()}'ed
 * and {@link #hasError()} as well as {@link #getError(boolean)} can be used to query and clear the state.
 * </p>
 */
public class WorkerThread {
    /**
     * An interruptible {@link #run() task} periodically executed on the {@link WorkerThread} {@link WorkerThread#getThread() thread}.
     */
    public interface Callback {
        /**
         * Task to be periodically executed on the {@link WorkerThread} {@link WorkerThread#getThread() thread}.
         * @param self The {@link WorkerThread} manager
         * @throws InterruptedException
         */
        void run(WorkerThread self) throws InterruptedException;
    }

    /**
     * An interruptible {@link State} {@link #run() task} on the {@link WorkerThread} {@link WorkerThread#getThread() thread}.
     */
    public interface StateCallback {
        /** State change cause. */
        public static enum State {
            INIT, PAUSED, RESUMED, END
        }
        /**
         * Task to be executed on {@link State} change on the {@link WorkerThread} {@link WorkerThread#getThread() thread}.
         * @param self The {@link WorkerThread} manager
         * @param cause the {@link State} change cause
         * @throws InterruptedException
         */
        void run(WorkerThread self, State cause) throws InterruptedException;
    }

    private static final int RUNNING     = 1 << 0;
    private static final int ACTIVE      = 1 << 1;
    private static final int BLOCKED     = 1 << 2;
    private static final int SHALL_PAUSE = 1 << 3;
    private static final int SHALL_STOP  = 1 << 4;
    private static final int USE_MINIMUM = 1 << 5;
    private static final int DAEMON      = 1 << 6;
    private static AtomicInteger instanceId = new AtomicInteger(0);

    private volatile int state;
    private final static boolean isSet(final int state, final int mask) { return mask == ( state & mask ); }
    private final boolean isSet(final int mask) { return mask == ( state & mask ); }
    private final void set(final int mask) { state |= mask; }
    private final void clear(final int mask) { state &= ~mask; }

    private final Duration minPeriod;
    private final Duration minDelay;
    private final Callback cbWork;
    private final StateCallback cbState;
    private Thread thread;
    private volatile Duration sleptDuration = Duration.ZERO;
    private volatile Exception workErr = null;

    /**
     * Instantiates a new {@link WorkerThread}.
     * @param minPeriod minimum work-loop-period to throttle execution or {@code null} if unthrottled, see {@link #getSleptDuration()}
     * @param minDelay minimum work-loop-delay to throttle execution or {@code null} if unthrottled, see {@link #getSleptDuration()}
     * @param daemonThread argument for {@link Thread#setDaemon(boolean)}
     * @param work the actual work {@link Callback} to perform.
     */
    public WorkerThread(final Duration minPeriod, final Duration minDelay, final boolean daemonThread, final Callback work) {
        this(minPeriod, minDelay, daemonThread, work, null);
    }

    /**
     * Instantiates a new {@link WorkerThread}.
     * @param minPeriod minimum work-loop-period to throttle execution or {@code null} if unthrottled, see {@link #getSleptDuration()}
     * @param minDelay minimum work-loop-delay to throttle execution or {@code null} if unthrottled, see {@link #getSleptDuration()}
     * @param daemonThread argument for {@link Thread#setDaemon(boolean)}
     * @param work the actual work {@link Callback} to perform.
     * @param stateChangeCB optional {@link StateCallback} called at different {@link StateCallback.State} changes while locked
     */
    public WorkerThread(final Duration minPeriod, final Duration minDelay, final boolean daemonThread, final Callback work, final StateCallback stateChangeCB) {
        this.state = 0;
        this.minPeriod = null != minPeriod ? minPeriod : Duration.ZERO;
        this.minDelay = null != minDelay ? minDelay : Duration.ZERO;
        if( this.minPeriod.toMillis() > 0 || this.minDelay.toMillis() > 0 ) {
            set(USE_MINIMUM);
        }
        this.cbWork = work;
        this.cbState = stateChangeCB;
        if( daemonThread ) {
            set(DAEMON);
        }
        thread = null;
    }

    /**
     * Starts execution of a new worker thread if not {@link #isRunning}, i.e. never {@link #start()}'ed or {@link #stop()}'ed.
     * <p>
     * Method blocks until the new worker thread has been started and {@link #isRunning()} and {@link #isActive()} if {@code paused == false}.
     * </p>
     * @param paused if {@code true}, keeps the new worker thread paused, otherwise {@link #resume()} it.
     */
    public final synchronized void start(final boolean paused) {
        if( isSet(RUNNING) || null != thread || isSet(SHALL_STOP) || isSet(SHALL_PAUSE) ) {
            // definite start condition: !isRunning
            // subsequent conditions only for consistency/doc: null == thread && !shallStop && !shallPause
            return;
        }
        if( paused ) {
            set(SHALL_PAUSE);
        }
        thread = new Thread(threadRunnable);
        thread.setDaemon(isSet(DAEMON));
        thread.start();
        try {
            this.notifyAll();  // wake-up startup-block
            if( !paused ) {
                while( !isSet(RUNNING) && !isSet(ACTIVE) && null != thread && !isSet(SHALL_STOP) ) {
                    this.wait();  // wait until started and active (not-paused)
                }
            } else {
                while( !isSet(RUNNING) && null != thread && !isSet(SHALL_STOP) ) {
                    this.wait();  // wait until started
                }
                while( isSet(RUNNING) && isSet(ACTIVE) && null != thread && !isSet(SHALL_STOP) ) {
                    this.wait();  // wait until paused
                }
            }
        } catch (final InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        }
    }

    /**
     * Stops execution of the {@link #start()}'ed worker thread.
     * <p>
     * Method blocks until worker thread has been {@link #isRunning() stopped} if {@code waitUntilDone} is {@code true}.
     * </p>
     */
    public final synchronized void stop(final boolean waitUntilDone) {
        if( isSet(RUNNING) ) {
            set(SHALL_STOP);
            this.notifyAll();  // wake-up pause-block (opt)
            if( java.lang.Thread.currentThread() != thread ) {
                if( isSet(BLOCKED | RUNNING) ) {
                    thread.interrupt();
                }
                if( waitUntilDone ) {
                    try {
                        while( isSet(RUNNING) ) {
                            this.wait();  // wait until stopped
                        }
                    } catch (final InterruptedException e) {
                        throw new InterruptedRuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Pauses execution of the {@link #start()}'ed worker thread.
     * <p>
     * Method blocks until worker thread has been {@link #isActive()}'ated if {@code waitUntilDone} is {@code true}.
     * </p>
     */
    public final synchronized void pause(final boolean waitUntilDone) {
        if( isSet(RUNNING | ACTIVE) && !isSet(SHALL_STOP) ) {
            set(SHALL_PAUSE);
            if( java.lang.Thread.currentThread() != thread ) {
                if( isSet(BLOCKED | ACTIVE) ) {
                    thread.interrupt();
                }
                if( waitUntilDone ) {
                    try {
                        while( isSet(RUNNING | ACTIVE) ) {
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
        if( isSet(RUNNING) && !isSet(ACTIVE) && !isSet(SHALL_STOP) ) {
            clear(SHALL_PAUSE);
            this.notifyAll();  // wake-up pause-block
            if( java.lang.Thread.currentThread() != thread ) {
                try {
                    while( !isSet(ACTIVE) && !isSet(SHALL_PAUSE) && isSet(RUNNING) ) {
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
    public final boolean isRunning() { return isSet(RUNNING); }
    /** Returns true if the worker thread {@link #isRunning()} and is not {@link #pause(boolean) paused}. */
    public final boolean isActive() { return isSet(ACTIVE); }
    /** Returns true if the worker thread {@link #isRunning()} and is {@link #pause(boolean) paused}. */
    public final boolean isPaused() { return isSet(RUNNING) && !isSet(ACTIVE); }
    /** Returns true if an exception occured during {@link Callable} work execution. */
    public final boolean hasError() { return null != workErr; }
    /** Returns the worker thread if {@link #isRunning()}, otherwise {@code null}. */
    public final Thread getThread() { return thread; }

    /**
     * Returns the exception is {@link #hasError()}.
     * @param clear if true, clear the exception
     */
    public final Exception getError(final boolean clear ) { final Exception e = workErr; if( clear) { workErr = null; } return e; }

    /**
     * Returns enforced minimum work-loop-period or {@link Duration#ZERO} for none.
     * @see #getSleptDuration()
     */
    public final Duration getMinPeriod() { return minPeriod; }

    /**
     * Returns enforced minimum work-loop-delay or {@link Duration#ZERO} for none.
     * @see #getSleptDuration()
     */
    public final Duration getMinDelay() { return minDelay; }

    /**
     * Returns the slept {@link Duration} delta of {@link #getMinPeriod()} and consumed {@link Callback#run()} duration,
     * which minimum is {@link #getMinDelay()}.
     * <p>
     * Returns {@link Duration#ZERO zero} for {@link Duration#ZERO zero} {@link #getMinPeriod()} and {@link #getMinDelay()} or exceeding {@link Callback#run()} duration
     * without {@link #getMinDelay()}.
     * </p>
     */
    public final Duration getSleptDuration() { return sleptDuration; }

    @Override
    public String toString() {
        synchronized(this) {
            final int _state = state;
            return "Worker[running "+isSet(_state, RUNNING)+", active "+isSet(_state, ACTIVE)+", blocked "+isSet(_state, BLOCKED)+
                    ", shall[pause "+isSet(_state, SHALL_PAUSE)+", stop "+isSet(_state, SHALL_STOP)+
                    "], min[period "+minPeriod.toMillis()+"ms, delay "+minDelay.toMillis()+"], slept "+sleptDuration.toMillis()+
                    "ms, daemon "+isSet(_state, DAEMON)+", thread "+thread+"]";
        }
    }

    private final Runnable threadRunnable = new Runnable() {
        @Override
        public final void run() {
            final Thread ct = Thread.currentThread();
            ct.setName(ct.getName()+"-Worker_"+instanceId.getAndIncrement());

            synchronized ( WorkerThread.this ) {
                Exception err = null;
                if( null != cbState ) {
                    try {
                        cbState.run(WorkerThread.this, StateCallback.State.INIT);
                    } catch (final InterruptedException e) {
                        // OK
                    } catch (final Throwable t) {
                        err = new Exception(t.getClass().getSimpleName()+" while processing init-state "+cbState, t);
                    }
                    if( null != err ) {
                        workErr = err;
                        clear(RUNNING | ACTIVE | SHALL_STOP | SHALL_PAUSE);
                        thread = null;
                        workErr.printStackTrace();
                        WorkerThread.this.notifyAll(); // wake-up ctor()
                        return; // bail out
                    }
                }
                set(RUNNING | ACTIVE);
                WorkerThread.this.notifyAll(); // wake-up ctor()
            }

            while( !isSet(SHALL_STOP) ) {
                Exception err = null;
                try {
                    if( isSet(SHALL_PAUSE) ) {
                        synchronized ( WorkerThread.this ) {
                            if( null != cbState ) {
                                try {
                                    cbState.run(WorkerThread.this, StateCallback.State.PAUSED);
                                } catch (final InterruptedException e) {
                                    // OK
                                } catch (final Throwable t) {
                                    err = new Exception(t.getClass().getSimpleName()+" while processing pause-state "+cbState, t);
                                }
                                if( null != err ) {
                                    workErr = err;
                                    clear(RUNNING | ACTIVE | SHALL_STOP | SHALL_PAUSE);
                                    thread = null;
                                    workErr.printStackTrace();
                                    WorkerThread.this.notifyAll(); // wake-up ctor()
                                    return; // bail out
                                }
                            }
                            while( isSet(SHALL_PAUSE) && !isSet(SHALL_STOP) ) {
                                clear(ACTIVE);
                                WorkerThread.this.notifyAll(); // wake-up doPause()
                                try {
                                    WorkerThread.this.wait();  // wait until resumed
                                } catch (final InterruptedException e) {
                                    if( !isSet(SHALL_PAUSE) ) {
                                        throw new InterruptedRuntimeException(e);
                                    }
                                }
                            }
                            if( null != cbState ) {
                                try {
                                    cbState.run(WorkerThread.this, StateCallback.State.RESUMED);
                                } catch (final InterruptedException e) {
                                    err = new InterruptedRuntimeException(e.getClass().getSimpleName()+" while processing resume-state"+cbState, e);
                                } catch (final Throwable t) {
                                    err = new Exception(t.getClass().getSimpleName()+" while processing resume-state "+cbState, t);
                                }
                                if( null != err ) {
                                    workErr = err;
                                    clear(RUNNING | ACTIVE | SHALL_STOP | SHALL_PAUSE);
                                    thread = null;
                                    workErr.printStackTrace();
                                    WorkerThread.this.notifyAll(); // wake-up ctor()
                                    return; // bail out
                                }
                            }
                            set(ACTIVE);
                            WorkerThread.this.notifyAll(); // wake-up doResume()
                        }
                    }
                    if( !isSet(SHALL_STOP) ) {
                        final Instant t0 = Instant.now();
                        set(BLOCKED);
                        {
                            cbWork.run(WorkerThread.this);
                        }
                        clear(BLOCKED);
                        if( isSet(USE_MINIMUM) ) {
                            final long minDelayMS = minDelay.toMillis();
                            final Instant t1 = Instant.now();
                            final Duration td = Duration.between(t0, t1);
                            if( minPeriod.compareTo(td) > 0 ) {
                                final Duration minPeriodDelta = minPeriod.minus(td);
                                final long minPeriodDeltaMS = minPeriodDelta.toMillis();
                                if( minPeriodDeltaMS > 0 ) {
                                    final long minSleepMS = Math.max(minDelayMS, minPeriodDeltaMS);
                                    sleptDuration = Duration.of(minSleepMS, ChronoUnit.MILLIS);
                                    java.lang.Thread.sleep( minSleepMS );
                                } else if( minDelayMS > 0 ) {
                                    sleptDuration = minDelay;
                                    java.lang.Thread.sleep( minDelayMS );
                                } else {
                                    sleptDuration = Duration.ZERO;
                                }
                                // java.util.concurrent.locks.LockSupport.parkNanos(tdMin.toNanos());
                            } else if( minDelayMS > 0 ) {
                                sleptDuration = minDelay;
                                java.lang.Thread.sleep( minDelayMS );
                            } else {
                                sleptDuration = Duration.ZERO;
                            }
                        }
                    }
                } catch (final InterruptedException e) {
                    if( !isSet(BLOCKED) ) { // !shallStop && !shallPause
                        err = new InterruptedRuntimeException(e.getClass().getSimpleName()+" while processing work-callback "+cbWork, e);
                    }
                    clear(BLOCKED);
                    sleptDuration = Duration.ZERO;
                } catch (final Throwable t) {
                    err = new Exception(t.getClass().getSimpleName()+" while processing work-callback "+cbWork, t);
                    sleptDuration = Duration.ZERO;
                } finally {
                    if( null != err ) {
                        // state transition incl. notification
                        synchronized ( WorkerThread.this ) {
                            workErr = err;
                            err = null;
                            set(SHALL_PAUSE);
                            clear(ACTIVE);
                            WorkerThread.this.notifyAll(); // wake-up potential do*()
                        }
                    }
                }
            }
            synchronized ( WorkerThread.this ) {
                if( null != cbState ) {
                    try {
                        cbState.run(WorkerThread.this, StateCallback.State.END);
                    } catch (final InterruptedException e) {
                        // OK
                    } catch (final Throwable t) {
                        workErr = new Exception(t.getClass().getSimpleName()+" while processing end-state "+cbState, t);
                        workErr.printStackTrace();
                    }
                }
                thread = null;
                clear(RUNNING | ACTIVE | SHALL_STOP | SHALL_PAUSE);
                WorkerThread.this.notifyAll(); // wake-up doStop()
            }
        } };
}
