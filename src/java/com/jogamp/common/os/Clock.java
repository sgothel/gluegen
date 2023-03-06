/**
 * Author: Sven Gothel <sgothel@jausoft.com>
 * Copyright (c) 2020-2023 Gothel Software e.K.
 * Copyright (c) 2020-2023 JogAmp Community.
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
package com.jogamp.common.os;

import java.time.Instant;

public class Clock {
    private static long t0;
    static {
        Platform.initSingleton(); // loads native gluegen_rt library
        t0 = currentTimeMillis();
    }

    /**
     * Returns current monotonic time since Unix Epoch `00:00:00 UTC on 1970-01-01`.
     *
     * Returned fraction_timespec is passing machine precision and range of the underlying native API.
     *
     * Monotonic time shall be used for high-performance measurements of durations,
     * since the underlying OS shall support fast calls.
     *
     * @see getWallClockTime()
     */
    public static Instant getMonotonicTime() {
        final long[/*2*/] val = { 0, 0 };
        getMonotonicTimeImpl(val);
        return Instant.ofEpochSecond(val[0], val[1]);
    }
    private static native void getMonotonicTimeImpl(final long[/*2*/] val);

    /**
     * Returns current wall-clock real-time since Unix Epoch `00:00:00 UTC on 1970-01-01`.
     *
     * Returned Instant is passing machine precision and range of the underlying native API.
     *
     * Wall-Clock time shall be used for accurate measurements of the actual time only,
     * since the underlying OS unlikely supports fast calls.
     *
     * @see getMonotonicTime()
     */
    public static Instant getWallClockTime() {
        final long[/*2*/] val = { 0, 0 };
        getWallClockTimeImpl(val);
        return Instant.ofEpochSecond(val[0], val[1]);
    }
    private static native void getWallClockTimeImpl(final long[/*2*/] val);

    /**
     * Returns current monotonic time in milliseconds.
     */
    public static native long currentTimeMillis();

    /**
     * Returns current wall-clock system `time of day` in seconds since Unix Epoch
     * `00:00:00 UTC on 1 January 1970`.
     */
    public static native long wallClockSeconds();

    /**
     * Returns the startup time in monotonic time in milliseconds of the native module.
     */
    public static long startupTimeMillis() { return t0; }

    /**
     * Returns current elapsed monotonic time in milliseconds since module startup, see {@link #startupTimeMillis()}.
     */
    public static long elapsedTimeMillis() { return currentTimeMillis() - t0; }

    /**
     * Returns elapsed monotonic time in milliseconds since module startup comparing against the given timestamp, see {@link #startupTimeMillis()}.
     */
    public static long elapsedTimeMillis(final long current_ts) { return current_ts - t0; }

}
