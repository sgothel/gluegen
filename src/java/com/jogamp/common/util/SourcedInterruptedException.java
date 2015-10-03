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

import java.io.PrintStream;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.ExceptionUtils.CustomStackTrace;

/**
 * {@link InterruptedException}, which may include the source, see {@link #getInterruptSource()}.
 * <p>
 * This exception may be created directly where {@link #getCause()} returns {@code null},
 * or by propagating an existing {@link InterruptedException} as returned by {@link #getCause()}.
 * </p>
 * @since 2.3.2
 */
@SuppressWarnings("serial")
public class SourcedInterruptedException extends InterruptedException implements CustomStackTrace {
    final Throwable interruptSource;

    /**
     * Wraps the given {@link InterruptedException} into a {@link SourcedInterruptedException}
     * if it is not yet of the desired type and
     * if the current thread if a {@link InterruptSource}, i.e. the source is known.
     * <p>
     * Otherwise the given {@link InterruptedException} instance is returned.
     * </p>
     * <p>
     * In case method is creating a new wrapping instance,
     * {@link InterruptSource#clearInterruptSource()} is being issued.
     * </p>
     *
     * @param ie the to be wrapped {@link InterruptedException}
     */
    public static InterruptedException wrap(final InterruptedException ie) {
        return wrap(ie, InterruptSource.Util.currentThread());
    }

    /**
     * Wraps the given {@link InterruptedException} into a {@link SourcedInterruptedException}
     * if it is not yet of the same type and if {@code source} is not {@code null}.
     * <p>
     * Otherwise the given {@link InterruptedException} instance is returned.
     * </p>
     * <p>
     * In case method is creating a new wrapping instance,
     * {@link InterruptSource#clearInterruptSource()} is being issued.
     * </p>
     *
     * @param ie the to be wrapped {@link InterruptedException}
     * @param source the {@link InterruptSource}
     */
    public static InterruptedException wrap(final InterruptedException ie, final InterruptSource source) {
        if( !(ie instanceof SourcedInterruptedException) && null != source ) {
            return new SourcedInterruptedException(ie, source.getInterruptSource(true));
        } else {
            return ie;
        }
    }

    /**
     * @param message mandatory message of this exception
     * @param cause optional propagated cause
     * @param interruptSource optional propagated source of {@link Thread#interrupt()} call
     */
    public SourcedInterruptedException(final String message, final InterruptedException cause, final Throwable interruptSource) {
        super(message);
        if( null != cause ) {
            initCause(cause);
        }
        this.interruptSource = interruptSource;
    }

    /**
     * @param cause mandatory propagated cause
     * @param interruptSource optional propagated source of {@link Thread#interrupt()} call
     */
    public SourcedInterruptedException(final InterruptedException cause, final Throwable interruptSource) {
        super(cause.getMessage());
        initCause(cause);
        this.interruptSource = interruptSource;
    }

    /**
     * Returns the source of the {@link Thread#interrupt()} call if known,
     * otherwise {@code null} is returned.
     */
    public final Throwable getInterruptSource() {
        return interruptSource;
    }

    /**
     * Returns the propagated {@link InterruptedException}, i.e. the cause of this exception,
     * or {@code null} if not applicable.
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public InterruptedException getCause() {
        return (InterruptedException)super.getCause();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append(getClass().getSimpleName()).append(": ");
        if (null != interruptSource) {
            sb.append("[sourced]");
        } else {
            sb.append("[unknown]");
        }
        final String m = getLocalizedMessage();
        if( null != m ) {
            sb.append(" ").append(m);
        }
        return sb.toString();
    }

    @Override
    public final void printCauseStack(final PrintStream s, final String causeStr, final int causeIdx, final int stackDepth) {
        final String s0 = causeStr+"["+causeIdx+"]";
        s.println(s0+" by "+getClass().getSimpleName()+": "+getMessage()+" on thread "+Thread.currentThread().getName());
        ExceptionUtils.dumpStack(s, getStackTrace(), 0, stackDepth);
        if( null != interruptSource ) {
            ExceptionUtils.printCause(s, s0, interruptSource, 0, 1, stackDepth);
        }
    }

    @Override
    public final void printStackTrace(final PrintStream s, final int causeDepth, final int stackDepth) {
        s.println(getClass().getSimpleName()+": "+getMessage()+" on thread "+Thread.currentThread().getName());
        ExceptionUtils.dumpStack(s, getStackTrace(), 0, stackDepth);
        ExceptionUtils.printCause(s, "Caused", getCause(), 0, causeDepth, stackDepth);
        if( null != interruptSource ) {
            ExceptionUtils.printCause(s, "InterruptSource", interruptSource, 0, causeDepth, stackDepth);
        }
    }
}
