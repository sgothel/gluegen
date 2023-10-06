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

    /** Initial audio queue size in milliseconds. {@value} ms, i.e. 16 {@link AudioFrame}s per 32 ms. See {@link #init(AudioFormat, float, int)}.*/
    public static final int DefaultQueueSize = 16 * 32; // 512 ms
    /** Audio queue size w/ video in milliseconds. {@value} ms, i.e. 24 {@link AudioFrame}s per 32 ms. See {@link #init(AudioFormat, float, int)}.*/
    public static final int DefaultQueueSizeWithVideo =  24 * 32; // 768 ms

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

        /**
         * Ctor w/ zero duration, {@link #INVALID_PTS} and zero byte size
         */
        public AudioFrame() {
            this.byteSize = 0;
        }
        /**
         * Create a new instance
         * @param pts frame pts in milliseconds
         * @param duration frame duration in milliseconds
         * @param byteCount size in bytes
         */
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

        /**
         * Create a new instance
         * @param pts frame pts in milliseconds
         * @param duration frame duration in milliseconds
         * @param bytes audio data
         * @param byteCount size in bytes
         */
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
     * Makes the audio context current on the calling thread, if implementation utilizes context locking.
     * <p>
     * If implementation doesn't utilizes context locking, method always returns true.
     * </p>
     * <p>
     * Recursive call to {@link #makeCurrent()} and hence {@link #release()} are supported.
     * </p>
     * <p>
     * At any point in time one context can only be current by one thread,
     * and one thread can only have one context current.
     * </p>
     * @param throwException if true, throws ALException if context is null, current thread holds another context or failed to natively make current
     * @return true if current thread holds no other context and context successfully made current, otherwise false
     * @see #release()
     */
    public boolean makeCurrent(final boolean throwException);

    /**
     * Releases control of this audio context from the current thread, if implementation utilizes context locking.
     * <p>
     * If implementation doesn't utilizes context locking, method always returns true.
     * </p>
     * <p>
     * Recursive call to {@link #makeCurrent()} and hence {@link #release()} are supported.
     * </p>
     * @param throwException if true, throws ALException if context has not been previously made current on current thread
     *                       or native release failed.
     * @return true if context has previously been made current on the current thread and successfully released, otherwise false
     * @see #makeCurrent()
     */
    public boolean release(final boolean throwException);

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
     * Returns the native {@link AudioFormat} by this sink.
     * <p>
     * The native format is guaranteed to be supported
     * and shall reflect this sinks most native format,
     * i.e. best performance w/o data conversion.
     * </p>
     * <p>
     * The native format is not impacted by {@link #setChannelLimit(int)}.
     * </p>
     * <p>
     * May return {@link AudioSink#DefaultFormat} if undefined.
     * </p>
     * @see #init(AudioFormat, float, int)
     */
    public AudioFormat getNativeFormat();

    /**
     * Returns the preferred {@link AudioFormat} by this sink.
     * <p>
     * The preferred format is a subset of {@link #getNativeFormat()},
     * impacted by {@link #setChannelLimit(int)}.
     * </p>
     * <p>
     * Known {@link #AudioFormat} attributes considered by implementations:
     * <ul>
     *   <li>ALAudioSink: {@link AudioFormat#sampleRate}.
     *   <li>ALAudioSink: {@link AudioFormat#channelCount}
     * </ul>
     * </p>
     * @see #getNativeFormat()
     * @see #init(AudioFormat, float, int)
     * @see #setChannelLimit(int)
     * @see #isSupported(AudioFormat)
     */
    public AudioFormat getPreferredFormat();

    /**
     * Limit maximum supported audio channels by user.
     * <p>
     * Must be set before {@link #getPreferredFormat()}, {@link #isSupported(AudioFormat)} and naturally {@link #init(AudioFormat, int, int)}.
     * </p>
     * <p>
     * May be utilized to enforce 1 channel (mono) downsampling
     * in combination with JOAL/OpenAL to experience spatial 3D position effects.
     * </p>
     * @param cc maximum supported audio channels, will be clipped [1..{@link #getNativeFormat()}.{@link AudioFormat#channelCount channelCount}]
     * @see #getNativeFormat()
     * @see #getPreferredFormat()
     * @see #isSupported(AudioFormat)
     * @see #init(AudioFormat, int, int)
     */
    public void setChannelLimit(final int cc);

    /**
     * Returns true if the given format is supported by the sink, otherwise false.
     * <p>
     * The {@link #getPreferredFormat()} is used to validate compatibility with the given format.
     * </p>
     * @see #init(AudioFormat, float, int)
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
     * {@link #getPreferredFormat()} may help.
     * </p>
     * @param requestedFormat the requested {@link AudioFormat}.
     * @param frameDurationHint average {@link AudioFrame} duration hint in milliseconds.
     *                      May assist to adjust latency of the backend, as currently used for JOAL's ALAudioSink.
     *                      A value below 30ms or {@link #DefaultFrameDuration} may increase the audio processing load.
     *                      Assumed as {@link #DefaultFrameDuration}, if <code>frameDuration < 1 ms</code>.
     * @param queueSize queue size in milliseconds, see {@link #DefaultQueueSize}.
     * @return true if successful, otherwise false
     * @see #enqueueData(int, ByteBuffer, int)
     * @see #getAvgFrameDuration()
     */
    public boolean init(AudioFormat requestedFormat, int frameDurationHint, int queueSize);

    /**
     * Returns the {@link AudioFormat} as chosen by {@link #init(AudioFormat, float, int)},
     * i.e. it shall match the <i>requestedFormat</i>.
     */
    public AudioFormat getChosenFormat();

    /**
     * Returns the (minimum) latency in seconds of this sink as set by {@link #init(AudioFormat, float, int)}, see {@link #getDefaultLatency()}.
     * <p>
     * Latency might be the reciprocal mixer-refresh-interval [Hz], e.g. 50 Hz refresh-rate = 20ms minimum latency.
     * </p>
     * @see #init(AudioFormat, float, int)
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
     * {@link #init(AudioFormat, float, int)} must be called first.
     * </p>
     * @see #play()
     * @see #pause()
     * @see #enqueueData(AudioFrame)
     * @see #init(AudioFormat, float, int)
     */
    public void flush();

    /** Destroys this instance, i.e. closes all streams and devices allocated. */
    public void destroy();

    /**
     * Returns the number of allocated buffers as requested by
     * {@link #init(AudioFormat, float, int)}.
     * @see #init(AudioFormat, float, int)
     */
    public int getFrameCount();

    /**
     * Returns the current enqueued frames count since {@link #init(AudioFormat, float, int)}.
     * @see #init(AudioFormat, float, int)
     */
    public int getEnqueuedFrameCount();

    /**
     * Returns the current number of frames queued for playing.
     * <p>
     * {@link #init(AudioFormat, float, int)} must be called first.
     * </p>
     * @see #init(AudioFormat, float, int)
     */
    public int getQueuedFrameCount();

    /**
     * Returns the current number of bytes queued for playing.
     * <p>
     * {@link #init(AudioFormat, float, int)} must be called first.
     * </p>
     * @see #init(AudioFormat, float, int)
     */
    public int getQueuedByteCount();

    /**
     * Returns the current queued frame time in seconds for playing.
     * <p>
     * {@link #init(AudioFormat, float, int)} must be called first.
     * </p>
     * @see #init(AudioFormat, float, int)
     */
    public float getQueuedTime();

    /**
     * Returns average frame duration last assessed @ {@link #enqueueData(int, ByteBuffer, int)} when queue was full.
     * <pre>
     *   avgFrameDuration = {@link #getQueuedTime()} / {@link #getQueuedFrameCount()}
     * </pre>
     */
    public float getAvgFrameDuration();

    /**
     * Return the current audio presentation timestamp (PTS) in milliseconds.
     * <p>
     * In case implementation updates the audio buffer passively, consider using {@link #updateQueue()}.
     * </p>
     * <p>
     * The relative millisecond PTS since start of the presentation stored in integer
     * covers a time span of 2'147'483'647 ms (see {@link Integer#MAX_VALUE}
     * or 2'147'483 seconds or 24.855 days.
     * </p>
     * @see #updateQueue()
     * @see #enqueueData(int, ByteBuffer, int)
     */
    public int getPTS();

    /**
     * Return the last buffered audio presentation timestamp (PTS) in milliseconds.
     * @see #getPTS()
     */
    public int getLastBufferedPTS();

    /**
     * Returns the current number of frames in the sink available for writing.
     * <p>
     * {@link #init(AudioFormat, float, int)} must be called first.
     * </p>
     * @see #init(AudioFormat, float, int)
     */
    public int getFreeFrameCount();

    /**
     * Enqueue <code>byteCount</code> bytes as a new {@link AudioFrame} to this sink.
     * <p>
     * The data must comply with the chosen {@link AudioFormat} as set via {@link #init(AudioFormat, float, int)}.
     * </p>
     * <p>
     * {@link #init(AudioFormat, float, int)} must be called first.
     * </p>
     * @param pts presentation time stamp in milliseconds for the newly enqueued {@link AudioFrame}
     * @param bytes audio data for the newly enqueued {@link AudioFrame}
     * @returns the enqueued internal {@link AudioFrame}.
     * @see #init(AudioFormat, float, int)
     */
    public AudioFrame enqueueData(int pts, ByteBuffer bytes, int byteCount);
}
