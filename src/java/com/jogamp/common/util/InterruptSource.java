/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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
 * Interface exposing {@link java.lang.Thread#interrupt()} source,
 * intended for {@link java.lang.Thread} specializations.
 * @since 2.3.2
 */
public interface InterruptSource {
    /**
     * Returns the source of the last {@link #interrupt()} call.
     * @param clear if true, issues {@link #clearInterruptSource()}
     */
    Throwable getInterruptSource(final boolean clear);

    /**
     * Returns the count of {@link java.lang.Thread#interrupt()} calls.
     * @param clear if true, issues {@link #clearInterruptSource()}
     */
    int getInterruptCounter(final boolean clear);

    /**
     * Clears source and count of {@link java.lang.Thread#interrupt()} calls, if any.
     */
    void clearInterruptSource();

    public static class Util {
        /**
         * Casts given {@link java.lang.Thread} to {@link InterruptSource}
         * if applicable, otherwise returns {@code null}.
         */
        public static InterruptSource get(final java.lang.Thread t) {
            if(t instanceof InterruptSource) {
                return (InterruptSource)t;
            } else {
                return null;
            }
        }
        /**
         * Casts current {@link java.lang.Thread} to {@link InterruptSource}
         * if applicable, otherwise returns {@code null}.
         */
        public static InterruptSource currentThread() {
            return get(java.lang.Thread.currentThread());
        }
    }

    /**
     * {@link java.lang.Thread} specialization implementing {@link InterruptSource}
     * to track {@link java.lang.Thread#interrupt()} calls.
     * @since 2.3.2
     */
    public static class Thread extends java.lang.Thread implements InterruptSource {
        volatile Throwable interruptSource = null;
        volatile int interruptCounter = 0;
        final Object sync = new Object();

        /**
         * See {@link Thread#Thread(} for details.
         */
        public Thread() {
            super();
        }
        /**
         * See {@link Thread#Thread(ThreadGroup, Runnable)} for details.
         * @param tg explicit {@link ThreadGroup}, may be {@code null}
         * @param target explicit {@link Runnable}, may be {@code null}
         */
        public Thread(final ThreadGroup tg, final Runnable target) {
            super(tg, target);
        }
        /**
         * See {@link Thread#Thread(ThreadGroup, Runnable, String)} for details.
         * @param tg explicit {@link ThreadGroup}, may be {@code null}
         * @param target explicit {@link Runnable}, may be {@code null}
         * @param name explicit name of thread, must not be {@code null}
         */
        public Thread(final ThreadGroup tg, final Runnable target, final String name) {
            super(tg, target, name);
        }

        /**
         * Depending on whether {@code name} is null, either
         * {@link #Thread(ThreadGroup, Runnable, String)} or
         * {@link #Thread(ThreadGroup, Runnable)} is being utilized.
         * @param tg explicit {@link ThreadGroup}, may be {@code null}
         * @param target explicit {@link Runnable}, may be {@code null}
         * @param name explicit name of thread, may be {@code null}
         */
        public static Thread create(final ThreadGroup tg, final Runnable target, final String name) {
            return null != name ? new Thread(tg, target, name) : new Thread(tg, target);
        }

        @Override
        public final Throwable getInterruptSource(final boolean clear) {
            synchronized(sync) {
                final Throwable r = interruptSource;
                if( clear ) {
                    clearInterruptSource();
                }
                return r;
            }
        }
        @Override
        public final int getInterruptCounter(final boolean clear) {
            synchronized(sync) {
                final int r = interruptCounter;
                if( clear ) {
                    clearInterruptSource();
                }
                return r;
            }
        }
        @Override
        public final void clearInterruptSource() {
            synchronized(sync) {
                interruptCounter = 0;
                interruptSource = null;
            }
        }
        @Override
        public final void interrupt() {
            synchronized(sync) {
                interruptCounter++;
                interruptSource = new Throwable(getName()+".interrupt() #"+interruptCounter);
            }
            super.interrupt();
        }
    }
}
