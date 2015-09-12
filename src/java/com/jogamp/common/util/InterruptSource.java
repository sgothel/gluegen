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
         * Casts given {@link java.lang.Thread} to {@link InterruptSource},
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
         * Casts current {@link java.lang.Thread} to {@link InterruptSource},
         * if applicable, otherwise returns {@code null}.
         */
        public static InterruptSource currentThread() {
            return get(java.lang.Thread.currentThread());
        }
    }

    /**
     * {@link java.lang.Thread} specialization implementing {@link InterruptSource}
     * to track {@link java.lang.Thread#interrupt()} calls.
     */
    public static class Thread extends java.lang.Thread implements InterruptSource {
        volatile Throwable interruptSource = null;
        volatile int interruptCounter = 0;
        final Object sync = new Object();

        public Thread(final String name) {
            super(name);
        }
        public Thread(final Runnable target) {
            super(target);
        }
        public Thread(final Runnable target, final String name) {
            super(target, name);
        }
        public Thread(final ThreadGroup tg, final Runnable target, final String name) {
            super(tg, target, name);
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
