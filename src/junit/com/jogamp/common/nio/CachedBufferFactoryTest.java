/*
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package com.jogamp.common.nio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;

import static java.lang.System.*;
import static org.junit.Assert.*;

/**
 *
 * @author Michael Bien
 */
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CachedBufferFactoryTest extends SingletonJunitCase {

    private final int BUFFERCOUNT = 120;

    private static int[] sizes;
    private static int[] values;
    private static IntBuffer[] buffers;

    @Before
    public void setup() {

        sizes = new int[BUFFERCOUNT];
        values = new int[sizes.length];
        buffers = new IntBuffer[sizes.length];

        final Random rnd = new Random(7);

        // setup
        for (int i = 0; i < sizes.length; i++) {
            sizes[i] = rnd.nextInt(80)+1;
            values[i] = rnd.nextInt();
        }

    }

    @After
    public void teardown() {
        sizes = null;
        values = null;
        buffers = null;
    }

    @Test
    public void dynamicTest() {

        final CachedBufferFactory factory = CachedBufferFactory.create(64);

        // create
        for (int i = 0; i < sizes.length; i++) {
            buffers[i] = factory.newDirectIntBuffer(sizes[i]);
            assertEquals(ByteOrder.nativeOrder(), buffers[i].order());
            fill(buffers[i], values[i]);
        }

        // check
        checkBuffers(buffers, sizes, values);

    }

    @Test
    public void dynamicConcurrentTest() throws InterruptedException, ExecutionException {

        final CachedBufferFactory factory = CachedBufferFactory.createSynchronized(24);

        final List<Callable<Object>> callables = new ArrayList<Callable<Object>>();

        final CountDownLatch latch = new CountDownLatch(10);

        // create
        for (int i = 0; i < sizes.length; i++) {
            final int n = i;
            final Callable<Object> c = new Callable<Object>() {
                public Object call() throws Exception {
                    latch.countDown();
                    latch.await();
                    buffers[n] = factory.newDirectIntBuffer(sizes[n]);
                    fill(buffers[n], values[n]);
                    return null;
                }
            };
            callables.add(c);
        }

        final ExecutorService dathVader = Executors.newFixedThreadPool(10);
        dathVader.invokeAll(callables);

        dathVader.shutdown();

        // check
        checkBuffers(buffers, sizes, values);

    }

    private void checkBuffers(final IntBuffer[] buffers, final int[] sizes, final int[] values) {
        for (int i = 0; i < buffers.length; i++) {
            final IntBuffer buffer = buffers[i];
            assertEquals(sizes[i], buffer.capacity());
            assertEquals(0, buffer.position());
            assertTrue(equals(buffer, values[i]));
        }
    }

    @Test
    public void staticTest() {

        final CachedBufferFactory factory = CachedBufferFactory.create(10, true);

        for (int i = 0; i < 5; i++) {
            factory.newDirectByteBuffer(2);
        }

        try{
            factory.newDirectByteBuffer(1);
            fail();
        }catch (final RuntimeException ex) {
            // expected
        }

    }

    private void fill(final IntBuffer buffer, final int value) {
        while(buffer.remaining() != 0)
            buffer.put(value);

        buffer.rewind();
    }

    private boolean equals(final IntBuffer buffer, final int value) {
        while(buffer.remaining() != 0) {
            if(value != buffer.get())
                return false;
        }

        buffer.rewind();
        return true;
    }


    /* load testing */

    private int size = 4;
    private final int iterations = 10000;

//    @Test
    public Object loadTest() {
        final CachedBufferFactory factory = CachedBufferFactory.create();
        final ByteBuffer[] buffer = new ByteBuffer[iterations];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = factory.newDirectByteBuffer(size);
        }
        return buffer;
    }

//    @Test
    public Object referenceTest() {
        final ByteBuffer[] buffer = new ByteBuffer[iterations];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Buffers.newDirectByteBuffer(size);
        }
        return buffer;
    }


    public static void main(final String[] args) {

        CachedBufferFactoryTest test = new CachedBufferFactoryTest();

        out.print("warmup...");
        Object obj = null;
        for (int i = 0; i < 100; i++) {
            obj = test.referenceTest();
            obj = test.loadTest();
            gc();
        }
        out.println("done");

        test = new CachedBufferFactoryTest();
        gc();

        for (int i = 0; i < 10; i++) {

            out.println("allocation size: "+test.size);

            long time = System.currentTimeMillis();
            obj = test.referenceTest();
            if(obj == null) return; // ref lock

            out.println("reference: "+ (System.currentTimeMillis()-time));

            gc();

            time = currentTimeMillis();
            obj = test.loadTest();
            if(obj == null) return; // ref lock

            out.println("factory:   "+ (System.currentTimeMillis()-time));

            gc();

            test.size*=2;
        }

    }

}
