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
package jogamp.common.av;

import java.nio.ByteBuffer;

import com.jogamp.common.av.AudioFormat;
import com.jogamp.common.av.AudioSink;
import com.jogamp.common.av.PTS;
import com.jogamp.common.av.TimeFrameI;
import com.jogamp.common.os.Clock;

public final class NullAudioSink implements AudioSink {

    private volatile float playSpeed = 1.0f;
    private volatile boolean playRequested = false;
    private final PTS pts = new PTS( () -> { return playRequested ? 1f : 0f; } );
    private float volume = 1.0f;

    private AudioFormat chosenFormat;
    private boolean available;

    public NullAudioSink() {
        available = true;
        chosenFormat = null;
    }

    @Override
    public final boolean makeCurrent(final boolean throwException) { return true; }

    @Override
    public final boolean release(final boolean throwException) { return true; }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public final float getPlaySpeed() { return playSpeed; }

    @Override
    public final boolean setPlaySpeed(float rate) {
        if( Math.abs(1.0f - rate) < 0.01f ) {
            rate = 1.0f;
        }
        playSpeed = rate;
        return true;
    }

    @Override
    public final float getVolume() {
        // FIXME
        return volume;
    }

    @Override
    public final boolean setVolume(final float v) {
        // FIXME
        volume = v;
        return true;
    }

    @Override
    public int getSourceCount() {
        return -1;
    }

    @Override
    public float getDefaultLatency() {
        return 0;
    }

    @Override
    public float getLatency() {
        return 0;
    }

    @Override
    public AudioFormat getNativeFormat() {
        return DefaultFormat;
    }

    @Override
    public AudioFormat getPreferredFormat() {
        return DefaultFormat;
    }

    @Override
    public void setChannelLimit(final int cc) { }

    @Override
    public final boolean isSupported(final AudioFormat format) {
        /**
         * If we like to emulate constraints ..
         *
        if( format.planar || !format.littleEndian ) {
            return false;
        }
        if( format.sampleRate != DefaultFormat.sampleRate )  {
            return false;
        }
        */
        return true;
    }

    @Override
    public boolean init(final AudioFormat requestedFormat, final int frameDuration, final int queueSize) {
        chosenFormat = requestedFormat;
        return true;
    }

    @Override
    public final AudioFormat getChosenFormat() {
        return chosenFormat;
    }

    @Override
    public boolean isPlaying() {
        return playRequested;
    }

    @Override
    public void play() {
        playRequested = true;
    }

    @Override
    public void pause() {
        playRequested = false;
    }

    @Override
    public void flush() { pts.set(0, TimeFrameI.INVALID_PTS); }

    @Override
    public void destroy() {
        available = false;
        chosenFormat = null;
    }

    @Override
    public final int getEnqueuedFrameCount() { return 0; }

    @Override
    public int getFrameCount() { return 0; }

    @Override
    public int getQueuedFrameCount() { return 0; }

    @Override
    public int getQueuedByteCount() { return 0; }

    @Override
    public float getQueuedDuration() { return 0f; }

    @Override
    public float getAvgFrameDuration() { return 0f; }

    @Override
    public final PTS getPTS() { return pts; }

    @Override
    public final PTS updateQueue() { return pts; }

    @Override
    public int getLastBufferedPTS() { return 0; }

    @Override
    public int getFreeFrameCount() { return 1; }

    @Override
    public AudioFrame enqueueData(final int pts, final ByteBuffer bytes, final int byteCount) {
        if( !available || null == chosenFormat ) {
            return null;
        }
        this.pts.set(Clock.currentMillis(), pts);
        return null;
    }
}
