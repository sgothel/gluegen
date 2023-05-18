/**
 * Copyright 2013-2023 JogAmp Community. All rights reserved.
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

/**
 * Specifies the linear audio PCM format.
 */
public class AudioFormat {
    /**
     * @param sampleRate sample rate in Hz (1/s)
     * @param sampleSize sample size in bits
     * @param channelCount number of channels
     * @param signed true if signed number, false for unsigned
     * @param fixedP true for fixed point value, false for unsigned floating point value with a sampleSize of 32 (float) or 64 (double)
     * @param planar true for planar data package (each channel in own data buffer), false for packed data channels interleaved in one buffer.
     * @param littleEndian true for little-endian, false for big endian
     */
    public AudioFormat(final int sampleRate, final int sampleSize, final int channelCount, final boolean signed, final boolean fixedP, final boolean planar, final boolean littleEndian) {
        this.sampleRate = sampleRate;
        this.sampleSize = sampleSize;
        this.channelCount = channelCount;
        this.signed = signed;
        this.fixedP = fixedP;
        this.planar = planar;
        this.littleEndian = littleEndian;
        if( !fixedP ) {
            if( sampleSize != 32 && sampleSize != 64 ) {
                throw new IllegalArgumentException("Floating point: sampleSize "+sampleSize+" bits");
            }
            if( !signed ) {
                throw new IllegalArgumentException("Floating point: unsigned");
            }
        }
    }

    /** Sample rate in Hz (1/s). */
    public final int sampleRate;
    /** Sample size in bits. */
    public final int sampleSize;
    /** Number of channels. */
    public final int channelCount;
    public final boolean signed;
    /** Fixed or floating point values. Floating point 'float' has {@link #sampleSize} 32, 'double' has {@link #sampleSize} 64. */
    public final boolean fixedP;
    /** Planar or packed samples. If planar, each channel has their own data buffer. If packed, channel data is interleaved in one buffer. */
    public final boolean planar;
    public final boolean littleEndian;


    //
    // Time <-> Bytes
    //

    /**
     * Returns the byte size of the given milliseconds
     * according to {@link #sampleSize}, {@link #channelCount} and {@link #sampleRate}.
     * <p>
     * Time -> Byte Count
     * </p>
     */
    public final int getDurationsByteSize(final int millisecs) {
        final int bytesPerSample = sampleSize >>> 3; // /8
        return millisecs * ( channelCount * bytesPerSample * ( sampleRate / 1000 ) );
    }

    /**
     * Returns the duration in milliseconds of the given byte count
     * according to {@link #sampleSize}, {@link #channelCount} and {@link #sampleRate}.
     * <p>
     * Byte Count -> Time
     * </p>
     */
    public final int getBytesDuration(final int byteCount) {
        final int bytesPerSample = sampleSize >>> 3; // /8
        return byteCount / ( channelCount * bytesPerSample * ( sampleRate / 1000 ) );
    }

    /**
     * Returns the duration in milliseconds of the given sample count per frame and channel
     * according to the {@link #sampleRate}, i.e.
     * <pre>
     *    ( 1000f * sampleCount ) / sampleRate
     * </pre>
     * <p>
     * Sample Count -> Time
     * </p>
     * @param sampleCount sample count per frame and channel
     */
    public final float getSamplesDuration(final int sampleCount) {
        return ( 1000f * sampleCount ) / sampleRate;
    }

    /**
     * Returns the rounded frame count of the given milliseconds and frame duration.
     * <pre>
     *     Math.max( 1, millisecs / frameDuration + 0.5f )
     * </pre>
     * <p>
     * Note: <code>frameDuration</code> can be derived by <i>sample count per frame and channel</i>
     * via {@link #getSamplesDuration(int)}.
     * </p>
     * <p>
     * Frame Time -> Frame Count
     * </p>
     * @param millisecs time in milliseconds
     * @param frameDuration duration per frame in milliseconds.
     */
    public final int getFrameCount(final int millisecs, final float frameDuration) {
        return Math.max(1, (int) ( millisecs / frameDuration + 0.5f ));
    }

    /**
     * Returns the byte size of given sample count
     * according to the {@link #sampleSize}, i.e.:
     * <pre>
     *  sampleCount * ( sampleSize / 8 )
     * </pre>
     * <p>
     * Note: To retrieve the byte size for all channels,
     * you need to pre-multiply <code>sampleCount</code> with {@link #channelCount}.
     * </p>
     * <p>
     * Sample Count -> Byte Count
     * </p>
     * @param sampleCount sample count
     */
    public final int getSamplesByteCount(final int sampleCount) {
        return sampleCount * ( sampleSize >>> 3 );
    }

    /**
     * Returns the sample count of given byte count
     * according to the {@link #sampleSize}, i.e.:
     * <pre>
     *  ( byteCount * 8 ) / sampleSize
     * </pre>
     * <p>
     * Note: If <code>byteCount</code> covers all channels and you request the sample size per channel,
     * you need to divide the result by <code>sampleCount</code> by {@link #channelCount}.
     * </p>
     * <p>
     * Byte Count -> Sample Count
     * </p>
     * @param byteCount number of bytes
     */
    public final int getBytesSampleCount(final int byteCount) {
        return ( byteCount << 3 ) / sampleSize;
    }

    @Override
    public String toString() {
        return "AudioFormat[sampleRate "+sampleRate+", sampleSize "+sampleSize+", channelCount "+channelCount+
               ", signed "+signed+", fixedP "+fixedP+", "+(planar?"planar":"packed")+", "+(littleEndian?"little":"big")+"-endian]"; }
}