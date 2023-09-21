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
        AtomicInteger counter = new AtomicInteger(0);
        Instant tlast = Instant.now();
        volatile Duration td = Duration.ZERO;

        Action(final Duration sleep) {
            this.sleep = sleep;
        }

        @Override
        public void run() throws InterruptedException {
            {
                java.lang.Thread.sleep(sleep.toMillis());
                // java.util.concurrent.locks.LockSupport.parkNanos(sleep.toNanos());
            }
            final Instant t1 = Instant.now();
            td = Duration.between(tlast, t1);
            System.err.println("action period "+td.toMillis()+"ms, counter "+counter.getAndIncrement());
            tlast = t1;
        }
    }

    static void checkStarted(final WorkerThread wt, final boolean isPaused) {
        Assert.assertTrue(wt.isRunning());
        Assert.assertEquals(!isPaused, wt.isActive());
    }
    static void checkStopped(final WorkerThread wt) {
        Assert.assertFalse(wt.isRunning());
        Assert.assertFalse(wt.isActive());
    }
    static void start(final WorkerThread wt) {
        System.err.println("WT Start.0: "+wt);
        wt.start();
        System.err.println("WT Start.X: "+wt);
    }
    static void stop(final WorkerThread wt) {
        System.err.println("WT Stop.0: "+wt);
        wt.stop();
        System.err.println("WT Stop.X: "+wt);
    }
    static void pause(final WorkerThread wt, final boolean wait) {
        System.err.println("WT Pause.0: wait "+wait+", "+wt);
        wt.pause(wait);
        System.err.println("WT Pause.X: wait "+wait+", "+wt);
    }
    static void resume(final WorkerThread wt) {
        System.err.println("WT Resume.0: "+wt);
        wt.resume();
        System.err.println("WT Resume.X: "+wt);
    }

    public void testAction(final long periodMS, final long minDelayMS, final long actionMS) throws IOException, InterruptedException, InvocationTargetException {
        final Action action = new Action( 0 < actionMS ? Duration.of(actionMS, ChronoUnit.MILLIS) : Duration.ZERO);
        final WorkerThread wt =new WorkerThread(Duration.of(periodMS, ChronoUnit.MILLIS),
                                                Duration.of(minDelayMS, ChronoUnit.MILLIS), true /* daemonThread */, action);
        final long maxPeriodMS = Math.max(minDelayMS+actionMS, Math.max(periodMS, actionMS));
        int counterA = action.counter.get();
        checkStopped(wt);
        start(wt);
        checkStarted(wt, false /* isPaused */);
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
        stop(wt);
        checkStopped(wt);
        int counterB = action.counter.get();
        Assert.assertTrue(counterB > counterA);

        counterA = action.counter.get();
        checkStopped(wt);
        start(wt);
        checkStarted(wt, false /* isPaused */);
        Thread.sleep(maxPeriodMS*3);

        checkStarted(wt, false /* isPaused */);
        pause(wt, true /* wait */);
        checkStarted(wt, true /* isPaused */);
        counterB = action.counter.get();
        Assert.assertTrue(counterB > counterA);

        counterA = action.counter.get();
        Assert.assertTrue(counterB == counterA);
        Thread.sleep(maxPeriodMS);
        resume(wt);
        checkStarted(wt, false /* isPaused */);
        Thread.sleep(maxPeriodMS*3);

        checkStarted(wt, false /* isPaused */);
        stop(wt);
        checkStopped(wt);
        counterB = action.counter.get();
        Assert.assertTrue(counterB > counterA);
    }

    @Test
    public void test01ZeroAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(16 /* periodMS */, 0 /* minDelayMS */, 0 /* actionMS*/);
    }

    @Test
    public void test02MidAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(16 /* periodMS */, 0 /* minDelayMS */, 8 /* actionMS*/);
    }

    @Test
    public void test03HeavyAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(16 /* periodMS */, 0 /* minDelayMS */, 20 /* actionMS*/);
    }

    @Test
    public void test03ZeroMidAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(0 /* periodMS */, 0 /* minDelayMS */, 8 /* actionMS*/);
    }

    @Test
    public void test04ZeroMinDelayMidAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(0 /* periodMS */, 4 /* minDelayMS */, 8 /* actionMS*/);
    }

    @Test
    public void test05MinDelayMidAction() throws IOException, InterruptedException, InvocationTargetException {
        testAction(8 /* periodMS */, 8 /* minDelayMS */, 8 /* actionMS*/);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestWorkerThread01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
