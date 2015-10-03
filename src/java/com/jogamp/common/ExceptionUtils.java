/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.common;

import java.io.PrintStream;

/**
 * @since 2.3.0
 */
public class ExceptionUtils {
    public static void dumpStack(final PrintStream out) {
        dumpStack(out, 1, -1);
    }
    public static void dumpStack(final PrintStream out, final int skip, final int depth) {
        dumpStack(out, new Exception(""), skip+1, depth);
    }
    public static void dumpStack(final PrintStream out, final Throwable t, final int skip, final int depth) {
        dumpStack(out, t.getStackTrace(), skip, depth);
    }
    public static void dumpStack(final PrintStream out, final StackTraceElement[] stack, final int skip, final int depth) {
        if( null == stack ) {
            return;
        }
        final int maxDepth;
        if( 0 > depth ) {
            maxDepth = stack.length;
        } else {
            maxDepth = Math.min(depth+skip, stack.length);
        }
        for(int i=skip; i<maxDepth; i++) {
            out.println("    ["+i+"]: "+stack[i]);
        }
    }

    /**
     * Interface allowing {@link Throwable} specializations to provide their custom stack trace presentation.
     * @since 2.3.2
     */
    public static interface CustomStackTrace {
        /**
         * Prints this {@link Throwable} as a cause to the output {@link PrintStream} {@code s},
         * not iterating over all inner causes!
         * @param s output stream
         * @param causeStr the cause title
         * @param causeIdx the cause index over all causes known by caller
         * @param stackDepth the maximum depth for stack entries, or {@code -1} for all
         * @since 2.3.2
         */
        void printCauseStack(final PrintStream s, final String causeStr, final int causeIdx, final int stackDepth);
        /**
         * Custom {@code printStackTrace} method, similar to {@link Throwable#printStackTrace(PrintStream, int, int)}.
         * @param s output stream
         * @param causeDepth the maximum depth for causes, or {@code -1} for all
         * @param stackDepth the maximum depth for stack entries, or {@code -1} for all
         */
        void printStackTrace(final PrintStream s, final int causeDepth, final int stackDepth);
    }

    /**
     * Prints the given {@link Throwable} cause to the output {@link PrintStream} {@code s}.
     * @param s output stream
     * @param causeStr the cause title
     * @param cause the {@link Throwable} cause for output
     * @param causeIdx the cause index over all causes known by caller
     * @param causeDepth the maximum depth for causes, or {@code -1} for all
     * @param stackDepth the maximum depth for stack entries, or {@code -1} for all
     * @since 2.3.2
     */
    public static int printCause(final PrintStream s, final String causeStr, Throwable cause, final int causeIdx, final int causeDepth, final int stackDepth) {
        int i=causeIdx;
        for(; null != cause && ( -1 == causeDepth || i < causeDepth ); cause = cause.getCause()) {
            if( cause instanceof CustomStackTrace ) {
                ((CustomStackTrace)cause).printCauseStack(s, causeStr, i, stackDepth);
            } else {
                s.println(causeStr+"["+i+"] by "+cause.getClass().getSimpleName()+": "+cause.getMessage()+" on thread "+Thread.currentThread().getName());
                dumpStack(s, cause.getStackTrace(), 0, stackDepth);
            }
            i++;
        }
        return i;
    }

    /**
     * Prints the given {@link Throwable} to the output {@link PrintStream} {@code s}.
     * @param s output stream
     * @param t the {@link Throwable} for output
     * @param causeDepth the maximum depth for causes, or {@code -1} for all
     * @param stackDepth the maximum depth for stack entries, or {@code -1} for all
     * @since 2.3.2
     */
    public static void printStackTrace(final PrintStream s, final Throwable t, final int causeDepth, final int stackDepth) {
        if( t instanceof CustomStackTrace ) {
            ((CustomStackTrace)t).printStackTrace(s, causeDepth, stackDepth);
        } else {
            s.println(t.getClass().getSimpleName()+": "+t.getMessage()+" on thread "+Thread.currentThread().getName());
            dumpStack(s, t.getStackTrace(), 0, stackDepth);
            printCause(s, "Caused", t.getCause(), 0, causeDepth, stackDepth);
        }
    }

    /**
     * Dumps a {@link Throwable} to {@link System.err} in a decorating message including the current thread name,
     * and its {@link #dumpStack(PrintStream, StackTraceElement[], int, int) stack trace}.
     * <p>
     * Implementation will iterate through all {@link Throwable#getCause() causes}.
     * </p>
     * @param additionalDescr additional text placed before the {@link Throwable} details.
     * @param t the {@link Throwable} for output
     */
    public static void dumpThrowable(final String additionalDescr, final Throwable t) {
        dumpThrowable(additionalDescr, t, -1, -1);
    }
    /**
     * Dumps a {@link Throwable} to {@link System.err} in a decorating message including the current thread name,
     * and its {@link #dumpStack(PrintStream, StackTraceElement[], int, int) stack trace}.
     * <p>
     * Implementation will iterate through all {@link Throwable#getCause() causes}.
     * </p>
     * @param additionalDescr additional text placed before the {@link Throwable} details.
     * @param t the {@link Throwable} for output
     * @param causeDepth the maximum depth for causes, or {@code -1} for all
     * @param stackDepth the maximum depth for stack entries, or {@code -1} for all
     * @since 2.3.2
     */
    public static void dumpThrowable(final String additionalDescr, final Throwable t, final int causeDepth, final int stackDepth) {
        System.err.print("Caught "+additionalDescr+" ");
        printStackTrace(System.err, t, causeDepth, stackDepth);
    }
}
