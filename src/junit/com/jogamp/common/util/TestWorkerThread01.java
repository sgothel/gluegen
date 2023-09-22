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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWorkerThread01 extends SingletonJunitCase {

    static class Action implements WorkerThread.Callback {
        final Duration sleep;
        final AtomicInteger counter = new AtomicInteger(0);
        Instant tlast = Instant.now();
        volatile Duration td = Duration.ZERO;

        Action(final Duration sleep) {
            this.sleep = sleep;
        }

        @Override
        public void run(final WorkerThread self) throws InterruptedException {
            {
                java.lang.Thread.sleep(sleep.toMillis());
                // java.util.concurrent.locks.LockSupport.parkNanos(sleep.toNanos());
            }
            final Instant t1 = Instant.now();
            td = Duration.between(tlast, t1);
            final int v = counter.incrementAndGet();
            // System.err.println("action period "+td.toMillis()+"ms, counter "+v+": "+self);
            tlast = t1;
        }
    }
    static class StateCB implements WorkerThread.StateCallback {
        final AtomicInteger initCounter = new AtomicInteger(0);
        final AtomicInteger pausedCounter = new AtomicInteger(0);
        final AtomicInteger resumedCounter = new AtomicInteger(0);
        final AtomicInteger endCounter = new AtomicInteger(0);

        @Override
        public void run(final WorkerThread self, final State cause) throws InterruptedException {
            // System.err.println("WT-"+cause+": "+self);
            switch( cause ) {
                case END:
                    endCounter.incrementAndGet();
                    break;
                case INIT:
                    initCounter.incrementAndGet();
                    break;
                case PAUSED:
                    pausedCounter.incrementAndGet();
                    break;
                case RESUMED:
                    resumedCounter.incrementAndGet();
                    break;
                default:
                    break;
            }
        }
    }

    static void checkStarted(final WorkerThread wt, final boolean isPaused) {
        Assert.assertTrue(wt.toString(), wt.isRunning());
        Assert.assertEquals("isPaused "+isPaused+", "+wt.toString(), !isPaused, wt.isActive());
        Assert.assertNotNull(wt.toString(), wt.getThread());
    }
    static void checkStopped(final WorkerThread wt) {
        Assert.assertFalse(wt.toString(), wt.isRunning());
        Assert.assertFalse(wt.toString(), wt.isActive());
        Assert.assertNull(wt.toString(), wt.getThread());
    }
    static void start(final WorkerThread wt, final boolean paused) {
        // System.err.println("WT Start.0: paused "+paused+", "+wt);
        wt.start(paused);
        // System.err.println("WT Start.X: "+wt);
    }
    static void stop(final WorkerThread wt, final boolean wait) {
        // System.err.println("WT Stop.0: wait "+wait+", "+wt);
        wt.stop(wait);
        // System.err.println("WT Stop.X: wait "+wait+", "+wt);
    }
    static void pause(final WorkerThread wt, final boolean wait) {
        // System.err.println("WT Pause.0: wait "+wait+", "+wt);
        wt.pause(wait);
        // System.err.println("WT Pause.X: wait "+wait+", "+wt);
    }
    static void resume(final WorkerThread wt) {
        // System.err.println("WT Resume.0: "+wt);
        wt.resume();
        // System.err.println("WT Resume.X: "+wt);
    }

    public void testAction(final boolean startPaused, final long periodMS, final long minDelayMS, final long actionMS) throws IOException, InterruptedException, InvocationTargetException {
        final Action action = new Action( 0 < actionMS ? Duration.of(actionMS, ChronoUnit.MILLIS) : Duration.ZERO);
        final StateCB stateCB = new StateCB();
        final WorkerThread wt =new WorkerThread(Duration.of(periodMS, ChronoUnit.MILLIS),
                                                Duration.of(minDelayMS, ChronoUnit.MILLIS), true /* daemonThread */, action, stateCB);

        final long maxPeriodMS = Math.max(minDelayMS+actionMS, Math.max(periodMS, actionMS));
        System.err.println("testAction: startPaused "+startPaused+", maxPeriodMS "+maxPeriodMS+", actionMS "+actionMS+", "+wt);

        int counterA = action.counter.get();
        checkStopped(wt);
        Assert.assertEquals(0, stateCB.initCounter.get());
        Assert.assertEquals(0, action.counter.get());
        Assert.assertEquals(0, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());
        start(wt, startPaused);
        checkStarted(wt, startPaused /* isPaused */);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertEquals(startPaused?1:0, stateCB.pausedCounter.get());
        Assert.assertEquals(startPaused?0:0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());
        if( startPaused ) {
            wt.resume();
            checkStarted(wt, false /* isPaused */);
            Assert.assertEquals(1, stateCB.initCounter.get());
            Assert.assertEquals(1, stateCB.pausedCounter.get());
            Assert.assertEquals(1, stateCB.resumedCounter.get());
            Assert.assertEquals(0, stateCB.endCounter.get());
        }
        Thread.sleep(maxPeriodMS*3);
        {
            final Duration td = action.td;
            final Duration wt_slept = wt.getSleptDuration();
            final long minEps = 4;
            final long actionMS_d = td.minus( wt_slept ).toMillis() - actionMS;
            System.err.println("actionMS_d "+actionMS_d+" = td "+td.toMillis()+"ms - wt_slept "+wt_slept.toMillis()+"ms - actionMS "+actionMS+"ms < minEps "+minEps+"ms");
            Assert.assertTrue(Math.abs(actionMS_d) < minEps);
        }

        checkStarted(wt, false /* isPaused */);
        stop(wt, true); // running -> stop
        checkStopped(wt);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertEquals(startPaused?1:0, stateCB.pausedCounter.get());
        Assert.assertEquals(startPaused?1:0, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());
        int counterB = action.counter.get();
        Assert.assertTrue(counterB > counterA);

        counterA = action.counter.get();
        checkStopped(wt);
        start(wt, startPaused); // stop -> running
        checkStarted(wt, startPaused /* isPaused */);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertEquals(startPaused?2:0, stateCB.pausedCounter.get());
        Assert.assertEquals(startPaused?1:0, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());
        if( startPaused ) {
            wt.resume();
            checkStarted(wt, false /* isPaused */);
            Assert.assertEquals(2, stateCB.initCounter.get());
            Assert.assertEquals(startPaused?2:0, stateCB.pausedCounter.get());
            Assert.assertEquals(startPaused?2:0, stateCB.resumedCounter.get());
            Assert.assertEquals(1, stateCB.endCounter.get());
        }
        Thread.sleep(maxPeriodMS*3);

        checkStarted(wt, false /* isPaused */);
        pause(wt, true /* wait */); // running -> pause
        checkStarted(wt, true /* isPaused */);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertEquals(startPaused?3:1, stateCB.pausedCounter.get());
        Assert.assertEquals(startPaused?2:0, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());
        counterB = action.counter.get();
        Assert.assertTrue(counterB > counterA);

        Thread.sleep(maxPeriodMS);
        counterA = action.counter.get();
        Assert.assertTrue(counterB == counterA);
        resume(wt); // pause -> running
        checkStarted(wt, false /* isPaused */);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertEquals(startPaused?3:1, stateCB.pausedCounter.get());
        Assert.assertEquals(startPaused?3:1, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());
        Thread.sleep(maxPeriodMS*3);

        checkStarted(wt, false /* isPaused */);
        pause(wt, true /* wait */); // running -> pause
        checkStarted(wt, true /* isPaused */);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertEquals(startPaused?4:2, stateCB.pausedCounter.get());
        Assert.assertEquals(startPaused?3:1, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());
        counterB = action.counter.get();
        Assert.assertTrue(counterB > counterA);
        counterA = counterB;

        checkStarted(wt, true /* isPaused */);
        stop(wt, true); // pause -> stop
        checkStopped(wt);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertEquals(startPaused?4:2, stateCB.pausedCounter.get());
        Assert.assertEquals(startPaused?4:2, stateCB.resumedCounter.get());
        Assert.assertEquals(2, stateCB.endCounter.get());
        counterB = action.counter.get();
        Assert.assertTrue(counterB == counterA);

        resume(wt); // stop -> stop
        checkStopped(wt);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertEquals(startPaused?4:2, stateCB.pausedCounter.get());
        Assert.assertEquals(startPaused?4:2, stateCB.resumedCounter.get());
        Assert.assertEquals(2, stateCB.endCounter.get());

        pause(wt, true /* wait */); // stop -> stop
        checkStopped(wt);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertEquals(startPaused?4:2, stateCB.pausedCounter.get());
        Assert.assertEquals(startPaused?4:2, stateCB.resumedCounter.get());
        Assert.assertEquals(2, stateCB.endCounter.get());
    }

    @Test
    public void test01ZeroAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(false, 16 /* periodMS */, 0 /* minDelayMS */, 0 /* actionMS*/);
        testAction(true, 16 /* periodMS */, 0 /* minDelayMS */, 0 /* actionMS*/);
    }

    @Test
    public void test02MidAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(false, 16 /* periodMS */, 0 /* minDelayMS */, 8 /* actionMS*/);
        testAction(true, 16 /* periodMS */, 0 /* minDelayMS */, 8 /* actionMS*/);
    }

    @Test
    public void test03HeavyAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(false, 16 /* periodMS */, 0 /* minDelayMS */, 20 /* actionMS*/);
        testAction(true, 16 /* periodMS */, 0 /* minDelayMS */, 20 /* actionMS*/);
    }

    @Test
    public void test03ZeroMidAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(false, 0 /* periodMS */, 0 /* minDelayMS */, 8 /* actionMS*/);
        testAction(true, 0 /* periodMS */, 0 /* minDelayMS */, 8 /* actionMS*/);
    }

    @Test
    public void test04ZeroMinDelayMidAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(false, 0 /* periodMS */, 4 /* minDelayMS */, 8 /* actionMS*/);
        testAction(true, 0 /* periodMS */, 4 /* minDelayMS */, 8 /* actionMS*/);
    }

    @Test
    public void test05MinDelayMidAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(false, 8 /* periodMS */, 8 /* minDelayMS */, 8 /* actionMS*/);
        testAction(true, 8 /* periodMS */, 8 /* minDelayMS */, 8 /* actionMS*/);
    }

    @Test
    public void test10InitEnd01() throws IOException, InterruptedException, InvocationTargetException {
        // Issuing stop not in the worker-thread
        final AtomicInteger actionLatch = new AtomicInteger(0);
        final WorkerThread.Callback action = (final WorkerThread self) -> {
            java.lang.Thread.sleep(1);
            final boolean v = actionLatch.compareAndSet(0, 1);
            // System.err.println("action set "+v+": "+self);
        };
        final StateCB stateCB = new StateCB();

        Assert.assertEquals(0, stateCB.initCounter.get());
        Assert.assertEquals(0, actionLatch.get());
        Assert.assertEquals(0, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        final long minPeriodMS = 2;
        final long maxPeriodMS = 4;
        final WorkerThread wt =new WorkerThread(Duration.of(minPeriodMS, ChronoUnit.MILLIS),
                                                Duration.of(0, ChronoUnit.MILLIS), true /* daemonThread */,
                                                action, stateCB);
        Assert.assertEquals(0, stateCB.initCounter.get());
        Assert.assertEquals(0, actionLatch.get());
        Assert.assertEquals(0, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());
        checkStopped(wt);

        start(wt, true);
        checkStarted(wt, true /* isPaused */);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertEquals(0, actionLatch.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        wt.resume();
        checkStarted(wt, false /* isPaused */);
        Assert.assertEquals(1, stateCB.initCounter.get());
        // maybe: Assert.assertEquals(1, actionLatch.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        Thread.sleep(maxPeriodMS);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertEquals(1, actionLatch.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        checkStarted(wt, false /* isPaused */);
        stop(wt, true);
        checkStopped(wt);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertEquals(1, actionLatch.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());

        actionLatch.set(0);
        Assert.assertEquals(0, actionLatch.get());
        start(wt, false);
        checkStarted(wt, false/* isPaused */);
        Assert.assertEquals(2, stateCB.initCounter.get());
        // maybe: Assert.assertEquals(1, actionLatch.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());

        Thread.sleep(maxPeriodMS);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertEquals(1, actionLatch.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());

        checkStarted(wt, false /* isPaused */);
        stop(wt, true);
        checkStopped(wt);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertEquals(1, actionLatch.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(2, stateCB.endCounter.get());
    }

    @Test
    public void test11InitEnd02() throws IOException, InterruptedException, InvocationTargetException {
        // Issuing stop on the worker-thread
        final AtomicInteger actionCounter = new AtomicInteger(0);
        final WorkerThread.Callback action = (final WorkerThread self) -> {
            java.lang.Thread.sleep(1);
            final int v = actionCounter.incrementAndGet();
            // System.err.println("action cntr "+v+": "+self);
            if( 8 == v ) {
                stop(self, true);
            }
        };
        final StateCB stateCB = new StateCB();

        Assert.assertEquals(0, stateCB.initCounter.get());
        Assert.assertEquals(0, actionCounter.get());
        Assert.assertEquals(0, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        final long minPeriodMS = 2;
        final long maxPeriodMS = 16;
        final WorkerThread wt =new WorkerThread(Duration.of(minPeriodMS, ChronoUnit.MILLIS),
                                                Duration.of(0, ChronoUnit.MILLIS), true /* daemonThread */,
                                                action, stateCB);
        Assert.assertEquals(0, stateCB.initCounter.get());
        Assert.assertEquals(0, actionCounter.get());
        Assert.assertEquals(0, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());
        checkStopped(wt);

        start(wt, true);
        checkStarted(wt, true /* isPaused */);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertEquals(0, actionCounter.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        wt.resume();
        checkStarted(wt, false /* isPaused */);
        Assert.assertEquals(1, stateCB.initCounter.get());
        // maybe Assert.assertEquals(1, actionCounter.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        Thread.sleep(maxPeriodMS);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertTrue(0 < actionCounter.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());
        checkStopped(wt);

        actionCounter.set(0);
        Assert.assertEquals(0, actionCounter.get());
        start(wt, false);
        checkStarted(wt, false/* isPaused */);
        Assert.assertEquals(2, stateCB.initCounter.get());
        // maybe: Assert.assertEquals(1, actionLatch.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());

        Thread.sleep(maxPeriodMS);
        Assert.assertEquals(2, stateCB.initCounter.get());
        Assert.assertTrue(0 < actionCounter.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(2, stateCB.endCounter.get());
        checkStopped(wt);
    }

    @Test
    public void test20ExceptionAtWork() throws IOException, InterruptedException, InvocationTargetException {
        final AtomicInteger actionCounter = new AtomicInteger(0);
        final WorkerThread.Callback action = (final WorkerThread self) -> {
            java.lang.Thread.sleep(1);
            final int v = actionCounter.incrementAndGet();
            // System.err.println("action cntr "+v+": "+self);
            if( 8 == v ) {
                throw new RuntimeException("Test exception from worker action: "+self);
            }
        };
        final StateCB stateCB = new StateCB();

        Assert.assertEquals(0, stateCB.initCounter.get());
        Assert.assertEquals(0, actionCounter.get());
        Assert.assertEquals(0, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        final long minPeriodMS = 2;
        final long maxPeriodMS = 16;
        final WorkerThread wt =new WorkerThread(Duration.of(minPeriodMS, ChronoUnit.MILLIS),
                                                Duration.of(0, ChronoUnit.MILLIS), true /* daemonThread */,
                                                action, stateCB);
        Assert.assertEquals(0, stateCB.initCounter.get());
        Assert.assertEquals(0, actionCounter.get());
        Assert.assertEquals(0, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());
        checkStopped(wt);

        start(wt, true);
        checkStarted(wt, true /* isPaused */);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertEquals(0, actionCounter.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(0, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        wt.resume();
        checkStarted(wt, false /* isPaused */);
        Assert.assertEquals(1, stateCB.initCounter.get());
        // maybe: Assert.assertEquals(1, actionLatch.get());
        Assert.assertEquals(1, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());

        Thread.sleep(maxPeriodMS);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertTrue(0 < actionCounter.get());
        Assert.assertEquals(2, stateCB.pausedCounter.get());
        Assert.assertEquals(1, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());
        checkStarted(wt, true /* isPaused */);
        Assert.assertTrue(wt.hasError());
        Assert.assertNotNull(wt.getError(true));
        final int counterA = actionCounter.get();

        wt.resume();
        checkStarted(wt, false /* isPaused */);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertTrue(0 < actionCounter.get());
        Assert.assertEquals(2, stateCB.pausedCounter.get());
        Assert.assertEquals(2, stateCB.resumedCounter.get());
        Assert.assertEquals(0, stateCB.endCounter.get());
        Thread.sleep(maxPeriodMS);

        stop(wt, true);
        checkStopped(wt);
        final int counterB = actionCounter.get();
        Assert.assertTrue(counterB > counterA);
        Assert.assertEquals(1, stateCB.initCounter.get());
        Assert.assertTrue(0 < actionCounter.get());
        Assert.assertEquals(2, stateCB.pausedCounter.get());
        Assert.assertEquals(2, stateCB.resumedCounter.get());
        Assert.assertEquals(1, stateCB.endCounter.get());
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestWorkerThread01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
