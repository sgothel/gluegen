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

package com.jogamp.common.util.locks;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRecursiveLock01 extends SingletonJunitCase {

    public enum YieldMode {
        NONE(0), YIELD(1), SLEEP(2);

        public final int id;

        YieldMode(final int id){
            this.id = id;
        }
    }

    static void yield(final YieldMode mode) {
        switch(mode) {
            case YIELD:
                Thread.yield();
                break;
            case SLEEP:
                try {
                    Thread.sleep(10);
                } catch (final InterruptedException ie) {
                    ie.printStackTrace();
                }
                break;
            default:
                break;
        }

    }

    static class LockedObject {
        static final boolean DEBUG = false;

        static class ThreadStat {
            ThreadStat() {
                total = 0;
                counter = 0;
            }
            long total; // ns
            int counter;
        }

        private final RecursiveLock locker; // post
        private int deferredThreadCount = 0; // synced
        private final Map<String, ThreadStat> threadWaitMap = Collections.synchronizedMap(new HashMap<String, ThreadStat>()); // locked

        long avrg; // ns, post
        long max_deviation; // ns, post
        long min_deviation; // ns, post

        public LockedObject(final LockFactory.ImplType implType, final boolean fair) {
            locker = LockFactory.createRecursiveLock(implType, fair);
        }

        private synchronized void incrDeferredThreadCount() {
            deferredThreadCount++;
        }
        private synchronized void decrDeferredThreadCount() {
            deferredThreadCount--;
        }
        public synchronized int getDeferredThreadCount() {
            return deferredThreadCount;
        }

        public final void action1Direct(int l, final YieldMode yieldMode) {
            if(DEBUG) {
                System.err.print("<a1");
            }
            lock();
            try {
                if(DEBUG) {
                    System.err.print("+");
                }
                while(l>0) l--;
                yield(yieldMode);
            } finally {
                if(DEBUG) {
                    System.err.print("-");
                }
                unlock();
                if(DEBUG) {
                    System.err.println(">");
                }
            }
        }

        class Action2 implements Runnable {
            int l;
            YieldMode yieldMode;

            Action2(final int l, final YieldMode yieldMode) {
                this.l=l;
                this.yieldMode=yieldMode;
                incrDeferredThreadCount();
            }

            public void run() {
                if(DEBUG) {
                    System.err.print("[a2");
                }
                lock();
                try {
                    if(DEBUG) {
                        System.err.print("+");
                    }
                    while(l>0) l--;
                     yield(yieldMode);
                } finally {
                    if(DEBUG) {
                        System.err.print("-");
                    }
                    unlock();
                    if(DEBUG) {
                        System.err.println("]");
                    }
                }
                decrDeferredThreadCount();
                final int dc = getDeferredThreadCount();
                if(0>dc) {
                    throw new InternalError("deferredThreads: "+dc);
                }
            }
        }

        public final void action2Deferred(final int l, final YieldMode yieldMode) {
            final Action2 action2 = new Action2(l, yieldMode);
            new InterruptSource.Thread(null, action2, Thread.currentThread().getName()+"-deferred").start();
        }

        public final void lock() {
            long td = System.nanoTime();
            locker.lock();
            td = System.nanoTime() - td;

            final String cur = Thread.currentThread().getName();
            ThreadStat ts = threadWaitMap.get(cur);
            if(null == ts) {
                ts = new ThreadStat();
            }
            ts.total += td;
            ts.counter++;
            threadWaitMap.put(cur, ts);
        }

        public final void unlock() {
            locker.unlock();
        }

        public final boolean isLocked() {
            return locker.isLocked();
        }

        public void stats(final boolean dump) {
            long timeAllLocks=0;
            int numAllLocks=0;
            for(final Iterator<String> i = threadWaitMap.keySet().iterator(); i.hasNext(); ) {
                final String name = i.next();
                final ThreadStat ts = threadWaitMap.get(name);
                timeAllLocks += ts.total;
                numAllLocks += ts.counter;
            }
            max_deviation = Long.MIN_VALUE;
            min_deviation = Long.MAX_VALUE;
            avrg = timeAllLocks/numAllLocks;
            if(dump) {
                System.err.printf("Average: %6d ms / %6d times = %8d ns",
                        timeAllLocks/1000000, numAllLocks, avrg);
                System.err.println();
            }
            for(final Iterator<String> i = threadWaitMap.keySet().iterator(); i.hasNext(); numAllLocks++) {
                final String name = i.next();
                final ThreadStat ts = threadWaitMap.get(name);
                final long a = ts.total/ts.counter;
                final long d = a - avrg;
                max_deviation = Math.max(max_deviation, d);
                min_deviation = Math.min(min_deviation, d);
                if(dump) {
                    System.err.printf("%-35s %12d ns / %6d times, a %8d ns, d %8d ns",
                            name, ts.total, ts.counter, a, d);
                    System.err.println();
                }
            }
            if(dump) {
                System.err.printf("Deviation (min/max): [%8d ns - %8d ns]", min_deviation, max_deviation);
                System.err.println();
            }
        }

    }

    interface LockedObjectRunner extends Runnable {
        void stop();
        boolean isStopped();
        void waitUntilStopped();
    }

    class LockedObjectRunner1 implements LockedObjectRunner {
        volatile boolean shouldStop;
        volatile boolean stopped;
        LockedObject lo;
        int loops;
        int iloops;
        YieldMode yieldMode;

        public LockedObjectRunner1(final LockedObject lo, final int loops, final int iloops, final YieldMode yieldMode) {
            this.lo = lo;
            this.loops = loops;
            this.iloops = iloops;
            this.shouldStop = false;
            this.stopped = false;
            this.yieldMode = yieldMode;
        }

        public final void stop() {
            shouldStop = true;
        }

        public final boolean isStopped() {
            return stopped;
        }

        public void waitUntilStopped() {
            synchronized(this) {
                while(!stopped) {
                    try {
                        this.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        public void run() {
            synchronized(this) {
                while(!shouldStop && loops>0) {
                    lo.action1Direct(iloops, yieldMode);
                    lo.action2Deferred(iloops, yieldMode);
                    loops--;
                }
                stopped = true;
                this.notifyAll();
            }
        }
    }

    protected long testLockedObjectImpl(final LockFactory.ImplType implType, final boolean fair,
                                        final int threadNum, final int loops, final int iloops, final YieldMode yieldMode) throws InterruptedException {
        final long t0 = System.currentTimeMillis();
        final LockedObject lo = new LockedObject(implType, fair);
        final LockedObjectRunner[] runners = new LockedObjectRunner[threadNum];
        final InterruptSource.Thread[] threads = new InterruptSource.Thread[threadNum];
        int i;

        for(i=0; i<threadNum; i++) {
            runners[i] = new LockedObjectRunner1(lo, loops, iloops, yieldMode);
            // String name = Thread.currentThread().getName()+"-ActionThread-"+i+"_of_"+threadNum;
            final String name = "ActionThread-"+i+"_of_"+threadNum;
            threads[i] = new InterruptSource.Thread( null, runners[i], name );
            threads[i].start();
        }

        for( i=0; i<threadNum; i++ ) {
            runners[i].waitUntilStopped();
        }
        while( 0 < lo.getDeferredThreadCount() ) {
            Thread.sleep(100);
        }
        Assert.assertEquals(0, lo.locker.getHoldCount());
        Assert.assertEquals(false, lo.locker.isLocked());
        Assert.assertEquals(0, lo.getDeferredThreadCount());

        final long dt = System.currentTimeMillis()-t0;
        lo.stats(false);

        System.err.println();
        final String fair_S = fair ? "fair  " : "unfair" ;
        System.err.printf("---- TestRecursiveLock01.testLockedObjectThreading: i %5s, %s, threads %2d, loops-outter %6d, loops-inner %6d, yield %5s - dt %6d ms, avrg %8d ns, deviation [ %8d .. %8d ] ns",
                implType, fair_S, threadNum, loops, iloops, yieldMode, dt, lo.avrg, lo.min_deviation, lo.max_deviation);
        System.err.println();
        return dt;
    }

    @Test
    public void testLockedObjectThreading5x1000x10000N_Int01_Fair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Int01;
        final boolean fair=true;
        int threadNum=5;
        int loops=1000;
        int iloops=10000;
        final YieldMode yieldMode=YieldMode.NONE;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading5x1000x10000N_Java5_Fair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Java5;
        final boolean fair=true;
        int threadNum=5;
        int loops=1000;
        int iloops=10000;
        final YieldMode yieldMode=YieldMode.NONE;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading5x1000x10000N_Int01_Unfair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Int01;
        final boolean fair=false;
        int threadNum=5;
        int loops=1000;
        int iloops=10000;
        final YieldMode yieldMode=YieldMode.NONE;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading5x1000x10000N_Java5_Unfair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Java5;
        final boolean fair=false;
        int threadNum=5;
        int loops=1000;
        int iloops=10000;
        final YieldMode yieldMode=YieldMode.NONE;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading25x100x100Y_Int01_Fair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Int01;
        final boolean fair=true;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.YIELD;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading25x100x100Y_Java5_Fair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Java5;
        final boolean fair=true;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.YIELD;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading25x100x100Y_Int01_Unair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Int01;
        final boolean fair=false;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.YIELD;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading25x100x100Y_Java5_Unfair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Java5;
        final boolean fair=false;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.YIELD;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    // @Test
    public void testLockedObjectThreading25x100x100S_Int01_Fair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Int01;
        final boolean fair=true;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.SLEEP;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    // @Test
    public void testLockedObjectThreading25x100x100S_Java5() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Java5;
        final boolean fair=true;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.SLEEP;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading25x100x100N_Int01_Fair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Int01;
        final boolean fair=true;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.NONE;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading25x100x100N_Java5_Fair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Java5;
        final boolean fair=true;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.NONE;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading25x100x100N_Int01_Unfair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Int01;
        final boolean fair=false;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.NONE;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    @Test
    public void testLockedObjectThreading25x100x100N_Java5_Unfair() throws InterruptedException {
        final LockFactory.ImplType t = LockFactory.ImplType.Java5;
        final boolean fair=false;
        int threadNum=25;
        int loops=100;
        int iloops=100;
        final YieldMode yieldMode=YieldMode.NONE;

        if( Platform.getCPUFamily() == Platform.CPUFamily.ARM ) {
            threadNum=5; loops=5; iloops=10;
        }

        testLockedObjectImpl(t, fair, threadNum, loops, iloops, yieldMode);
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException, InterruptedException {
        final String tstname = TestRecursiveLock01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);

        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine());
        TestRecursiveLock01 t = new TestRecursiveLock01();
        t.testLockedObjectThreading5x1000x10000N_Int01_Unfair();

        t.testLockedObjectThreading5x1000x10000N_Int01_Fair();
        t.testLockedObjectThreading5x1000x10000N_Java5_Fair();
        t.testLockedObjectThreading5x1000x10000N_Int01_Unfair();
        t.testLockedObjectThreading5x1000x10000N_Java5_Unfair();
        */
    }

}
