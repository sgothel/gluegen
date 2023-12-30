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
package com.jogamp.common.av;

import java.util.concurrent.TimeUnit;

import com.jogamp.common.os.Clock;

/**
 * Presentation Timestamp (PTS) with added System Clock Reference (SCR) via
 * {@link #set(long, int)} and its interpolation via {@link #get(long)}, as well as giving raw access via {@link #getLast()}.
 * <p>
 * The relative millisecond PTS since start of the presentation stored in integer
 * covers a time span of 2'147'483'647 ms (see {@link Integer#MAX_VALUE}
 * or 2'147'483 seconds or 24.855 days.
 * </p>
 */
public final class PTS {
    /** An external float value getter */
    public static interface FloatValue {
        float get();
    }
    private final FloatValue speed;
    /** System Clock Reference (SCR) of last PTS update. */
    private volatile long scr;
    /** Last updated PTS value */
    private volatile int pts;

    /**
     * Create new instance, initializing pts with {@link TimeFrameI#INVALID_PTS} and system-clock timestamp with zero.
     * @param speed external {@link FloatValue} getter for playback speed.
     * @see #set(long, int)
     */
    public PTS(final FloatValue speed) {
        this.speed = speed;
        this.scr = 0;
        this.pts = TimeFrameI.INVALID_PTS;
    }
    /**
     * Create new instance.
     * @param speed external {@link FloatValue} getter for playback speed.
     * @param scr System Clock Reference (SCR) in milliseconds of taken pts value, i.e. {@link Clock#currentMillis()}.
     * @param pts the presentation timestamp (PTS) in milliseconds
     * @see #set(long, int)
     */
    public PTS(final FloatValue speed, final long scr, final int pts) {
        this.speed = speed;
        set(scr, pts);
    }
    /** Copy constructor */
    public PTS(final PTS other) {
        this.speed = other.speed;
        set(other);
    }

    /** Returns true if {@link #getLast()} is unequal to {@link TimeFrameI#INVALID_PTS}. */
    public boolean isValid() { return TimeFrameI.INVALID_PTS != pts; }

    /** Returns true if {@link #getLast()} equals to {@link TimeFrameI#END_OF_STREAM_PTS}, indicating end of stream (EOS). */
    public boolean isEOS() { return TimeFrameI.END_OF_STREAM_PTS == pts; }

    /** Returns the System Clock Reference (SCR) in milliseconds of last PTS update via {@link #set(long, int)}. */
    public long getSCR() { return scr; }
    /** Returns {@link #getSCR()} as time string representation via {@link #millisToTimeStr(long, boolean)}. */
    public String getSCRTimeStr(final boolean addFractions) {
        return millisToTimeStr(getSCR(), addFractions);
    }
    /** Returns the last updated PTS value via {@link #set(long, int)} w/o System Clock Reference (SCR) interpolation. */
    public int getLast() { return pts; }
    /** Returns {@link #getLast()} as time string representation via {@link #millisToTimeStr(long, boolean)}. */
    public String getLastTimeStr(final boolean addFractions) {
        return millisToTimeStr(getLast(), addFractions);
    }

    /** Returns the external playback speed. */
    public float getSpeed() { return speed.get(); }

    /**
     * Updates the PTS value with given System Clock Reference (SCR) in milliseconds.
     * @param scr System Clock Reference (SCR) in milliseconds of taken PTS value, i.e. {@link Clock#currentMillis()}.
     * @param pts the presentation timestamp (PTS) in milliseconds
     */
    public void set(final long scr, final int pts) {
        this.scr = scr;
        this.pts = pts;
    }
    /** Sets the PTS value, see {@link #set(long, int)}. */
    public void setPTS(final int pts) { this.pts = pts; }
    /** Sets the System Clock Reference (SCR) in milliseconds of last PTS update, see {@link #set(long, int)}. */
    public void setSCR(final long currentMillis) { scr = currentMillis; }

    /**
     * Updates the PTS value with values from other {@link PTS} instance.
     * @param other source {@link PTS} values
     * @see #get(long)
     */
    public void set(final PTS other) {
        this.scr = other.getSCR();
        this.pts = other.getLast();
    }

    /**
     * Returns the {@link #getLast() last updated PTS}, interpolated by {@link #getSCR() System Clock Reference (SCR)} delta to given {@code currentMillis} and playback {@link #getSpeed() speed}.
     * <pre>
     *      last_pts + (int) ( ( currentMillis - SCR ) * speed + 0.5f )
     * </pre>
     * @param currentMillis current system clock in milliseconds, i.e. {@link Clock#currentMillis()}.
     * @see #set(long, int)
     */
    public int get(final long currentMillis) {
        return pts + (int) ( ( currentMillis - scr ) * speed.get() + 0.5f );
    }

    /** Returns {@link #get(long)} as time string representation via {@link #millisToTimeStr(long, boolean)}. */
    public String getTimeStr(final long currentMillis, final boolean addFractions) {
        return millisToTimeStr(get(currentMillis), addFractions);
    }

    /** Returns {@link #getLast()} - rhs.{@link #getLast()}. */
    public int diffLast(final PTS rhs) {
        return this.pts - rhs.getLast();
    }

    /** Returns {@link #get(long)} - rhs.{@link #get(long)}. */
    public int diff(final long currentMillis, final PTS rhs) {
        return get(currentMillis) - rhs.get(currentMillis);
    }

    @Override
    public String toString() { return String.valueOf(pts); }

    public String toString(final long currentMillis) { return "last "+pts+" ms, current "+get(currentMillis)+" ms"; }

    /**
     * Returns a time string representation '[hh:]mm:ss[.sss]', dropping unused hour quantities and fractions of seconds optionally.
     * @param millis complete time in milliseconds
     * @param addFractions toggle for fractions of seconds
     * @see #millisToTimeStr(long)
     */
    public static String millisToTimeStr(final long millis, final boolean addFractions) {
        final long h = TimeUnit.MILLISECONDS.toHours(millis);
        final long m = TimeUnit.MILLISECONDS.toMinutes(millis);
        if( addFractions ) {
            if( 0 < h ) {
                return String.format("%02d:%02d:%02d.%03d",
                    h,
                    m - TimeUnit.HOURS.toMinutes(h),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(m),
                    millis%1000);
            } else {
                return String.format("%02d:%02d.%03d",
                    m,
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(m),
                    millis%1000);
            }
        } else {
            if( 0 < h ) {
                return String.format("%02d:%02d:%02d",
                    h,
                    m - TimeUnit.HOURS.toMinutes(h),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(m));
            } else {
                return String.format("%02d:%02d",
                    m,
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(m));
            }
        }
    }

    /**
     * Returns a full time string representation 'hh:mm:ss.sss'.
     * @param millis complete time in milliseconds
     * @see #millisToTimeStr(long, boolean)
     */
    public static String millisToTimeStr(final long millis) {
        final long h = TimeUnit.MILLISECONDS.toHours(millis);
        final long m = TimeUnit.MILLISECONDS.toMinutes(millis);
        return String.format("%02d:%02d:%02d.%03d",
            h,
            m - TimeUnit.HOURS.toMinutes(h),
            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(m),
            millis%1000);
    }
}
