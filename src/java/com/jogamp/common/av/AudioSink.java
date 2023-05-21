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

import java.nio.ByteBuffer;

import jogamp.common.Debug;

public interface AudioSink {
    public static final boolean DEBUG = Debug.debug("AudioSink");

    /** Default frame duration in millisecond, i.e. 1 {@link AudioFrame} per {@value} ms. */
    public static final int DefaultFrameDuration = 32;

    /** Initial audio queue size in milliseconds. {@value} ms, i.e. 16 {@link AudioFrame}s per 32 ms. See {@link #init(AudioFormat, float, int, int, int)}.*/
    public static final int DefaultInitialQueueSize = 16 * 32; // 512 ms
    /** Audio queue grow size in milliseconds. {@value} ms, i.e. 16 {@link AudioFrame}s per 32 ms. See {@link #init(AudioFormat, float, int, int, int)}.*/
    public static final int DefaultQueueGrowAmount = 16 * 32; // 512 ms
    /** Audio queue limit w/ video in milliseconds. {@value} ms, i.e. 96 {@link AudioFrame}s per 32 ms. See {@link #init(AudioFormat, float, int, int, int)}.*/
    public static final int DefaultQueueLimitWithVideo =  96 * 32; // 3072 ms
    /** Audio queue limit w/o video in milliseconds. {@value} ms, i.e. 32 {@link AudioFrame}s per 32 ms. See {@link #init(AudioFormat, float, int, int, int)}.*/
    public static final int DefaultQueueLimitAudioOnly =  32 * 32; // 1024 ms

    /** Default {@link AudioFormat}, [type PCM, sampleRate 44100, sampleSize 16, channelCount 2, signed, fixedP, !planar, littleEndian]. */
    public static final AudioFormat DefaultFormat = new AudioFormat(44100, 16, 2, true /* signed */,
                                          true /* fixed point */, false /* planar */, true /* littleEndian */);

    /**
     * Abstract audio frame containing multiple audio samples per channel, tracking {@link TimeFrameI} pts and size in bytes.
     * <p>
     * One {@link AudioFrame} may contain multiple pairs of samples per channel,
     * i.e. this {@link AudioFrame} does not limit a frame to be one sample per channel.
     * See its application in {@link AudioSink#enqueueData(int, ByteBuffer, int)}.
     * </p>
     * <p>
     * Implementations may assign actual data to queue frames from streaming, see {@link AudioDataFrame}.
     * </p>
     * @see AudioSink#enqueueData(int, ByteBuffer, int)
     */
    public static abstract class AudioFrame extends TimeFrameI {
        protected int byteSize;

        public AudioFrame() {
            this.byteSize = 0;
        }
        public AudioFrame(final int pts, final int duration, final int byteCount) {
            super(pts, duration);
            this.byteSize=byteCount;
        }

        /** Get this frame's size in bytes. */
        public final int getByteSize() { return byteSize; }
        /** Set this frame's size in bytes. */
        public final void setByteSize(final int size) { this.byteSize=size; }

        @Override
        public String toString() {
            return "AudioFrame[pts " + pts + " ms, l " + duration + " ms, "+byteSize + " bytes]";
        }
    }
    /**
     * Audio data frame example of {@link AudioFrame} with actual audio data being attached.
     */
    public static class AudioDataFrame extends AudioFrame {
        protected final ByteBuffer data;

        public AudioDataFrame(final int pts, final int duration, final ByteBuffer bytes, final int byteCount) {
            super(pts, duration, byteCount);
            if( byteCount > bytes.remaining() ) {
                throw new IllegalArgumentException("Give size "+byteCount+" exceeds remaining bytes in ls "+bytes+". "+this);
            }
            this.data=bytes;
        }

        /** Get this frame's data. */
        public final ByteBuffer getData() { return data; }

        @Override
        public String toString() {
            return "AudioDataFrame[pts " + pts + " ms, l " + duration + " ms, "+byteSize + " bytes, " + data + "]";
        }
    }

    /**
     * Exclusively locks this instance for the calling thread, if implementation utilizes locking.
     * @see #unlockExclusive()
     */
    public void lockExclusive();

    /**
     * Releases the exclusive lock for the calling thread, if implementation utilizes locking.
     * @see #lockExclusive()
     */
    public void unlockExclusive();

    /**
     * Returns the <code>available state</code> of this instance.
     * <p>
     * The <code>available state</code> is affected by this instance
     * overall availability, i.e. after instantiation,
     * as well as by {@link #destroy()}.
     * </p>
     */
    public boolean isAvailable();

    /** Returns the playback speed. */
    public float getPlaySpeed();

    /**
     * Sets the playback speed.
     * <p>
     * To simplify test, play speed is  <i>normalized</i>, i.e.
     * <ul>
     *   <li><code>1.0f</code>: if <code> Math.abs(1.0f - rate) < 0.01f </code></li>
     * </ul>
     * </p>
     * @return true if successful, otherwise false, i.e. due to unsupported value range of implementation.
     */
    public boolean setPlaySpeed(float s);

    /** Returns the volume. */
    public float getVolume();

    /**
     * Sets the volume [0f..1f].
     * <p>
     * To simplify test, volume is <i>normalized</i>, i.e.
     * <ul>
     *   <li><code>0.0f</code>: if <code> Math.abs(v) < 0.01f </code></li>
     *   <li><code>1.0f</code>: if <code> Math.abs(1.0f - v) < 0.01f </code></li>
     * </ul>
     * </p>
     * @return true if successful, otherwise false, i.e. due to unsupported value range of implementation.
     */
    public boolean setVolume(float v);

    /**
     * Returns the preferred sample-rate of this sink, i.e. mixer frequency in Hz, e.g. 41000 or 48000.
     * <p>
     * The preferred sample-rate is guaranteed to be supported
     * and shall reflect this sinks most native format,
     * i.e. best performance w/o data conversion.
     * </p>
     * <p>
     * May return {@link AudioSink#DefaultFormat}'s  44100 default if undefined.
     * </p>
     * @see #init(AudioFormat, float, int, int, int)
     * @see #isSupported(AudioFormat)
     */
    public int getPreferredSampleRate();

    /**
     * Returns the number of sources the used device is capable to mix.
     * <p>
     * This device attribute is only formally exposed and not used,
     * since an audio sink is only utilizing one source.
     * </p>
     * <p>
     * May return <code>-1</code> if undefined.
     * </p>
     * @return
     */
    public int getSourceCount();

    /**
     * Returns the default (minimum) latency in seconds
     * <p>
     * Latency might be the reciprocal mixer-refresh-interval [Hz], e.g. 50 Hz refresh-rate = 20ms minimum latency.
     * </p>
     * <p>
     * May return 20ms for a 50 Hz refresh rate if undefined.
     * </p>
     */
    public float getDefaultLatency();

    /**
     * Returns the preferred {@link AudioFormat} by this sink.
     * <p>
     * The preferred format is guaranteed to be supported
     * and shall reflect this sinks most native format,
     * i.e. best performance w/o data conversion.
     * </p>
     * <p>
     * Known {@link #AudioFormat} attributes considered by implementations:
     * <ul>
     *   <li>ALAudioSink: {@link AudioFormat#sampleRate}.
     * </ul>
     * </p>
     * <p>
     * May return {@link AudioSink#DefaultFormat} if undefined.
     * </p>
     * @see #init(AudioFormat, float, int, int, int)
     * @see #isSupported(AudioFormat)
     * @see #getPreferredSampleRate()
     */
    public AudioFormat getPreferredFormat();

    /** Return the maximum number of supported channels, e.g. 1 for mono, 2 for stereo, etc. */
    public int getMaxSupportedChannels();

    /**
     * Returns true if the given format is supported by the sink, otherwise false.
     * @see #init(AudioFormat, float, int, int, int)
     * @see #getPreferredFormat()
     */
    public boolean isSupported(AudioFormat format);

    /**
     * Initializes the sink.
     * <p>
     * Implementation must match the given <code>requestedFormat</code> {@link AudioFormat}.
     * </p>
     * <p>
     * Caller shall validate <code>requestedFormat</code> via {@link #isSupported(AudioFormat)}
     * beforehand and try to find a suitable supported one.
     * {@link #getPreferredFormat()} and {@link #getMaxSupportedChannels()} may help.
     * </p>
     * @param requestedFormat the requested {@link AudioFormat}.
     * @param frameDurationHint average {@link AudioFrame} duration hint in milliseconds.
     *                      May assist to shape the {@link AudioFrame} initial queue size using `initialQueueSize`.
     *                      May assist to adjust latency of the backend, as currently used for JOAL's ALAudioSink.
     *                      A value below 30ms or {@link #DefaultFrameDuration} may increase the audio processing load.
     *                      Assumed as {@link #DefaultFrameDuration}, if <code>frameDuration < 1 ms</code>.
     * @param initialQueueSize initial queue size in milliseconds, see {@link #DefaultInitialQueueSize}.
     *                        May use `frameDurationHint` to determine initial {@link AudioFrame} queue size.
     * @param queueGrowAmount queue grow size in milliseconds if queue is full, see {@link #DefaultQueueGrowAmount}.
     *                        May use {@link #getAvgFrameDuration()} to determine {@link AudioFrame} queue growth amount.
     * @param queueLimit maximum time in milliseconds the queue can hold (and grow), see {@link #DefaultQueueLimitWithVideo} and {@link #DefaultQueueLimitAudioOnly}.
     *                        May use {@link #getAvgFrameDuration()} to determine {@link AudioFrame} queue limit.
     * @return true if successful, otherwise false
     * @see #enqueueData(int, ByteBuffer, int)
     * @see #getAvgFrameDuration()
     */
    public boolean init(AudioFormat requestedFormat, int frameDurationHint,
                        int initialQueueSize, int queueGrowAmount, int queueLimit);

    /**
     * Returns the {@link AudioFormat} as chosen by {@link #init(AudioFormat, float, int, int, int)},
     * i.e. it shall match the <i>requestedFormat</i>.
     */
    public AudioFormat getChosenFormat();

    /**
     * Returns the (minimum) latency in seconds of this sink as set by {@link #init(AudioFormat, float, int, int, int)}, see {@link #getDefaultLatency()}.
     * <p>
     * Latency might be the reciprocal mixer-refresh-interval [Hz], e.g. 50 Hz refresh-rate = 20ms minimum latency.
     * </p>
     * @see #init(AudioFormat, float, int, int, int)
     */
    public float getLatency();

    /**
     * Returns true, if {@link #play()} has been requested <i>and</i> the sink is still playing,
     * otherwise false.
     */
    public boolean isPlaying();

    /**
     * Play buffers queued via {@link #enqueueData(AudioFrame)} from current internal position.
     * If no buffers are yet queued or the queue runs empty, playback is being continued when buffers are enqueued later on.
     * @see #enqueueData(AudioFrame)
     * @see #pause()
     */
    public void play();

    /**
     * Pause playing buffers while keeping enqueued data incl. it's internal position.
     * @see #play()
     * @see #flush()
     * @see #enqueueData(AudioFrame)
     */
    public void pause();

    /**
     * Flush all queued buffers, implies {@link #pause()}.
     * <p>
     * {@link #init(AudioFormat, float, int, int, int)} must be called first.
     * </p>
     * @see #play()
     * @see #pause()
     * @see #enqueueData(AudioFrame)
     * @see #init(AudioFormat, float, int, int, int)
     */
    public void flush();

    /** Destroys this instance, i.e. closes all streams and devices allocated. */
    public void destroy();

    /**
     * Returns the number of allocated buffers as requested by
     * {@link #init(AudioFormat, float, int, int, int)}.
     * @see #init(AudioFormat, float, int, int, int)
     */
    public int getFrameCount();

    /**
     * Returns the current enqueued frames count since {@link #init(AudioFormat, float, int, int, int)}.
     * @see #init(AudioFormat, float, int, int, int)
     */
    public int getEnqueuedFrameCount();

    /**
     * Returns the current number of frames queued for playing.
     * <p>
     * {@link #init(AudioFormat, float, int, int, int)} must be called first.
     * </p>
     * @see #init(AudioFormat, float, int, int, int)
     */
    public int getQueuedFrameCount();

    /**
     * Returns the current number of bytes queued for playing.
     * <p>
     * {@link #init(AudioFormat, float, int, int, int)} must be called first.
     * </p>
     * @see #init(AudioFormat, float, int, int, int)
     */
    public int getQueuedByteCount();

    /**
     * Returns the current queued frame time in milliseconds for playing.
     * <p>
     * {@link #init(AudioFormat, float, int, int, int)} must be called first.
     * </p>
     * @see #init(AudioFormat, float, int, int, int)
     */
    public int getQueuedTime();

    /**
     * Returns average frame duration last assessed @ {@link #enqueueData(int, ByteBuffer, int)} when queue was full.
     * <pre>
     *   avgFrameDuration = {@link #getQueuedTime()} / {@link #getQueuedFrameCount()}
     * </pre>
     */
    public int getAvgFrameDuration();

    /**
     * Return the current audio presentation timestamp (PTS) in milliseconds.
     */
    public int getPTS();

    /**
     * Returns the current number of frames in the sink available for writing.
     * <p>
     * {@link #init(AudioFormat, float, int, int, int)} must be called first.
     * </p>
     * @see #init(AudioFormat, float, int, int, int)
     */
    public int getFreeFrameCount();

    /**
     * Enqueue <code>byteCount</code> bytes as a new {@link AudioFrame} to this sink.
     * <p>
     * The data must comply with the chosen {@link AudioFormat} as set via {@link #init(AudioFormat, float, int, int, int)}.
     * </p>
     * <p>
     * {@link #init(AudioFormat, float, int, int, int)} must be called first.
     * </p>
     * @param pts presentation time stamp for the newly enqueued {@link AudioFrame}
     * @param bytes audio data for the newly enqueued {@link AudioFrame}
     * @returns the enqueued internal {@link AudioFrame}.
     * @see #init(AudioFormat, float, int, int, int)
     */
    public AudioFrame enqueueData(int pts, ByteBuffer bytes, int byteCount);
}
