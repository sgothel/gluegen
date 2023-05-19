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

    /** Default frame duration in millisecond, i.e. 1 frame per {@value} ms. */
    public static final int DefaultFrameDuration = 32;

    /** Initial audio queue size in milliseconds. {@value} ms, i.e. 16 frames per 32 ms. See {@link #init(AudioFormat, float, int, int, int)}.*/
    public static final int DefaultInitialQueueSize = 16 * 32; // 512 ms
    /** Audio queue grow size in milliseconds. {@value} ms, i.e. 16 frames per 32 ms. See {@link #init(AudioFormat, float, int, int, int)}.*/
    public static final int DefaultQueueGrowAmount = 16 * 32; // 512 ms
    /** Audio queue limit w/ video in milliseconds. {@value} ms, i.e. 96 frames per 32 ms. See {@link #init(AudioFormat, float, int, int, int)}.*/
    public static final int DefaultQueueLimitWithVideo =  96 * 32; // 3072 ms
    /** Audio queue limit w/o video in milliseconds. {@value} ms, i.e. 32 frames per 32 ms. See {@link #init(AudioFormat, float, int, int, int)}.*/
    public static final int DefaultQueueLimitAudioOnly =  32 * 32; // 1024 ms

    /** Default {@link AudioFormat}, [type PCM, sampleRate 44100, sampleSize 16, channelCount 2, signed, fixedP, !planar, littleEndian]. */
    public static final AudioFormat DefaultFormat = new AudioFormat(44100, 16, 2, true /* signed */,
                                          true /* fixed point */, false /* planar */, true /* littleEndian */);

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
     * @see #init(AudioFormat, float, int, int, int)
     * @see #isSupported(AudioFormat)
     */
    public int getPreferredSampleRate();

    /**
     * Returns the default (minimum) latency in seconds
     * <p>
     * Latency might be the reciprocal mixer-refresh-interval [Hz], e.g. 50 Hz refresh-rate = 20ms minimum latency.
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
     * @see #init(AudioFormat, float, int, int, int)
     * @see #isSupported(AudioFormat)
     * @see #getPreferredSampleRate()
     */
    public AudioFormat getPreferredFormat();

    /** Return the maximum number of supported channels. */
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
     * @param frameDuration average or fixed frame duration in milliseconds
     *                      helping a caching {@link AudioFrame} based implementation to determine the frame count in the queue.
     *                      See {@link #DefaultFrameDuration}.
     * @param initialQueueSize initial time in milliseconds to queue in this sink, see {@link #DefaultInitialQueueSize}.
     * @param queueGrowAmount time in milliseconds to grow queue if full, see {@link #DefaultQueueGrowAmount}.
     * @param queueLimit maximum time in milliseconds the queue can hold (and grow), see {@link #DefaultQueueLimitWithVideo} and {@link #DefaultQueueLimitAudioOnly}.
     * @return true if successful, otherwise false
     */
    public boolean init(AudioFormat requestedFormat, int frameDuration,
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
     * Enqueue <code>byteCount</code> bytes of the remaining bytes of the given NIO {@link ByteBuffer} to this sink.
     * <p>
     * The data must comply with the chosen {@link AudioFormat} as set via {@link #init(AudioFormat, float, int, int, int)}.
     * </p>
     * <p>
     * {@link #init(AudioFormat, float, int, int, int)} must be called first.
     * </p>
     * @returns the enqueued internal {@link AudioFrame}.
     * @see #init(AudioFormat, float, int, int, int)
     */
    public AudioFrame enqueueData(int pts, ByteBuffer bytes, int byteCount);
}
