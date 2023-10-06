/**
 * Author: Sven Gothel <sgothel@jausoft.com>
 * Copyright (c) 2022-2023 Gothel Software e.K.
 * Copyright (c) 2022-2023 JogAmp Community.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.jogamp.common.util;

import java.io.PrintStream;

import com.jogamp.common.os.Clock;

/** A simple millisecond timestamp prepending `print*()` wrapper for a {@link PrintStream}.  */
public class TSPrinter {
    private static TSPrinter err;
    private static TSPrinter out;

    public static synchronized TSPrinter stderr() {
        if( null == err ) {
            err = new TSPrinter(System.err);
        }
        return err;
    }
    public static synchronized TSPrinter stdout() {
        if( null == out ) {
            out = new TSPrinter(System.out);
        }
        return out;
    }

    private final PrintStream parent;

    public TSPrinter(final PrintStream parent) {
        this.parent = parent;
    }

    public static void printf(final PrintStream out, final long millis, final String format, final Object ... args) {
        out.printf("[%,9d] ", millis);
        out.printf(format, args);
    }
    public static void printf(final PrintStream out, final String format, final Object ... args) {
        printf(out, Clock.currentMillis(), format, args);
    }
    public void printf(final long millis, final String format, final Object ... args) {
        printf(parent, millis, format, args);
    }
    public void printf(final String format, final Object ... args) {
        printf(parent, Clock.currentMillis(), format, args);
    }

    public static void print(final PrintStream out, final long millis, final String msg) {
        out.printf("[%,9d] %s", millis, msg);
    }
    public static void print(final PrintStream out, final String msg) {
        print(out, Clock.currentMillis(), msg);
    }
    public void print(final long millis, final String msg) {
        print(parent, millis, msg);
    }
    public void print(final String msg) {
        print(parent, Clock.currentMillis(), msg);
    }

    public static void println(final PrintStream out, final long millis, final String msg) {
        out.printf("[%,9d] %s%n", millis, msg);
    }
    public static void println(final PrintStream out, final String msg) {
        println(out, Clock.currentMillis(), msg);
    }
    public void println(final long millis, final String msg) {
        println(parent, millis, msg);
    }
    public void println(final String msg) {
        println(parent, Clock.currentMillis(), msg);
    }

}
