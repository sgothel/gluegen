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
    private static final Instant t0;
    static {
        Platform.initSingleton(); // loads native gluegen_rt library
        {
            final long[/*2*/] val = { 0, 0 };
            if( getMonotonicStartupTimeImpl(val) ) {
                t0 = Instant.ofEpochSecond(val[0], val[1]);
            } else {
                t0 = Instant.EPOCH;
            }
        }
    }

    /**
     * Returns current monotonic time since Unix Epoch `00:00:00 UTC on 1970-01-01`.
     * <p>
     * Returned timespec is passing machine precision and range of the underlying native API.
     * </p>
     * <p>
     * Monotonic time shall be used for high-performance measurements of durations,
     * since the underlying OS shall support fast calls.
     * </p>
     * <p>
     * Note that {@link #currentNanos()} and {@link #getMonotonicNanos()}
     * perform much better than this method, since they only return one long nanosecond value
     * since module startup. <br/>
     * The implementation of this method needs to write two long values into an array.
     * </p>
     * @see #getMonotonicStartupTime()
     * @see #currentNanos()
     * @see #getMonotonicNanos()
     * @see #getWallClockTime()
     */
    public static Instant getMonotonicTime() {
        final long[/*2*/] val = { 0, 0 };
        if( getMonotonicTimeImpl(val) ) {
            return Instant.ofEpochSecond(val[0], val[1]);
        } else {
            return Instant.EPOCH;
        }
    }
    private static native boolean getMonotonicTimeImpl(final long[/*2*/] val);

    /**
     * Returns current wall-clock real-time since Unix Epoch `00:00:00 UTC on 1970-01-01`.
     * <p>
     * Returned Instant is passing machine precision and range of the underlying native API.
     * </p>
     * <p>
     * Wall-Clock time shall be used for accurate measurements of the actual time only,
     * since the underlying OS unlikely supports fast calls.
     * </p>
     * @see #getMonotonicStartupTime()
     * @see #currentNanos()
     * @see #getMonotonicNanos()
     * @see #getMonotonicTime()
     */
    public static Instant getWallClockTime() {
        final long[/*2*/] val = { 0, 0 };
        if( getWallClockTimeImpl(val) ) {
            return Instant.ofEpochSecond(val[0], val[1]);
        } else {
            return Instant.EPOCH;
        }
    }
    private static native boolean getWallClockTimeImpl(final long[/*2*/] val);

    /**
     * Returns the monotonic startup time since module startup as used in {@link #currentNanos()} and {@link #getMonotonicNanos()}.
     * @see #currentNanos()
     * @see #getMonotonicNanos()
     */
    public static Instant getMonotonicStartupTime() { return t0; }
    private static native boolean getMonotonicStartupTimeImpl(final long[/*2*/] val);

    /**
     * Returns current monotonic nanoseconds since start of this application.
     * <p>
     * Monotonic time shall be used for high-performance measurements of durations,
     * since the underlying OS shall support fast calls.
     * </p>
     * <p>
     * The returned nanoseconds are counted not from Unix Epoch but start of this module,
     * hence it lasts for 9'223'372'036 seconds or 292 years using the 64-bit type `long`.
     * </p>
     * <p>
     * Method name doesn't include the term `Time` intentionally,
     * since the returned value represent the nanoseconds duration since module start.
     * </p>
     * @see #getMonotonicStartupTime()
     * @see #getMonotonicNanos()
     */
    public static native long currentNanos();

    /**
     * Returns the Instant presentation of monotonic {@link #currentNanos()}.
     * <p>
     * Monotonic time shall be used for high-performance measurements of durations,
     * since the underlying OS shall support fast calls.
     * </p>
     * <p>
     * The returned nanoseconds are counted not from Unix Epoch but start of this module,
     * hence it lasts for 9'223'372'036 seconds or 292 years using the 64-bit type `long`.
     * </p>
     * <p>
     * Method name doesn't include the term `Time` intentionally,
     * since the returned value represent the nanoseconds duration since module start.
     * </p>
     * @see #getMonotonicStartupTime()
     * @see #currentNanos()
     */
    public static Instant getMonotonicNanos() {
        final long nanos = currentNanos();
        return Instant.ofEpochSecond(nanos/1000000000L, nanos%1000000000L);
    }

    /**
     * Returns current monotonic time in milliseconds.
     *
     * @see #getMonotonicStartupTime()
     * @see #currentNanos()
     * @see #getMonotonicNanos()
     */
    public static native long currentTimeMillis();

    /**
     * Returns current wall-clock system `time of day` in seconds since Unix Epoch
     * `00:00:00 UTC on 1 January 1970`.
     *
     * @see #getWallClockTime()
     * @see #getMonotonicTime()
     * @see #currentNanos()
     * @see #getMonotonicNanos()
     */
    public static native long wallClockSeconds();
}
