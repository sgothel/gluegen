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
import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import com.jogamp.common.av.AudioFormat;
import com.jogamp.common.av.AudioSink;

/***
 * JavaSound Audio Sink
 * <p>
 * FIXME: Parameterize .. all configs .. best via an init-method, passing requested
 * audio capabilities
 * </p>
 */
public class JavaSoundAudioSink implements AudioSink {

    // Chunk of audio processed at one time
    public static final int BUFFER_SIZE = 1000;
    public static final int SAMPLES_PER_BUFFER = BUFFER_SIZE / 2;
    private static final boolean staticAvailable;

    // Sample time values
    // public static final double SAMPLE_TIME_IN_SECS = 1.0 / DEFAULT_SAMPLE_RATE;
    // public static final double BUFFER_TIME_IN_SECS = SAMPLE_TIME_IN_SECS * SAMPLES_PER_BUFFER;

    private javax.sound.sampled.AudioFormat format;
    private DataLine.Info info;
    private SourceDataLine auline;
    private int bufferCount;
    private final byte [] sampleData = new byte[BUFFER_SIZE];
    private boolean available = false;
    private AudioFormat chosenFormat = null;

    private volatile boolean playRequested = false;
    private float volume = 1.0f;

    static {
        boolean ok = false;
        try {
            ok = AudioSystem.getAudioFileTypes().length > 0;
        } catch (final Throwable t) {

        }
        staticAvailable=ok;
    }

    public JavaSoundAudioSink() {
        available = false;
        if( !staticAvailable ) {
            return;
        }
        available = true;
    }

    @Override
    public final void lockExclusive() { }

    @Override
    public final void unlockExclusive() { }

    @Override
    public String toString() {
        return "JavaSoundSink[avail "+available+", dataLine "+info+", source "+auline+", bufferCount "+bufferCount+
               ", chosen "+chosenFormat+", jsFormat "+format;
    }

    @Override
    public final float getPlaySpeed() { return 1.0f; } // FIXME

    @Override
    public final boolean setPlaySpeed(final float rate) {
        return false; // FIXME
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
    public int getPreferredSampleRate() {
        return DefaultFormat.sampleRate;
    }

    @Override
    public AudioFormat getPreferredFormat() {
        return DefaultFormat;
    }

    @Override
    public final int getMaxSupportedChannels() {
        return 2;
    }

    @Override
    public final boolean isSupported(final AudioFormat format) {
        return true;
    }

    @Override
    public boolean init(final AudioFormat requestedFormat, final float frameDuration, final int initialQueueSize, final int queueGrowAmount, final int queueLimit) {
        if( !staticAvailable ) {
            return false;
        }
        // Create the audio format we wish to use
        format = new javax.sound.sampled.AudioFormat(requestedFormat.sampleRate, requestedFormat.sampleSize, requestedFormat.channelCount, requestedFormat.signed, !requestedFormat.littleEndian);

        // Create dataline info object describing line format
        info = new DataLine.Info(SourceDataLine.class, format);

        // Clear buffer initially
        Arrays.fill(sampleData, (byte) 0);
        try{
            // Get line to write data to
            auline = (SourceDataLine) AudioSystem.getLine(info);
            auline.open(format);
            auline.start();
            System.out.println("JavaSound audio sink");
            available=true;
            chosenFormat = requestedFormat;
        } catch (final Exception e) {
            available=false;
        }
        return true;
    }

    @Override
    public final AudioFormat getChosenFormat() {
        return chosenFormat;
    }

    @Override
    public boolean isPlaying() {
        return playRequested && auline.isRunning();
    }

    @Override
    public void play() {
        if( null != auline ) {
            playRequested = true;
            playImpl();
        }
    }
    private void playImpl() {
        if( playRequested && !auline.isRunning() ) {
            auline.start();
        }
    }

    @Override
    public void pause() {
        if( null != auline ) {
            playRequested = false;
            auline.stop();
        }
    }

    @Override
    public void flush() {
        if( null != auline ) {
            playRequested = false;
            auline.stop();
            auline.flush();
        }
    }

    @Override
    public final int getEnqueuedFrameCount() {
        return 0; // FIXME
    }

    @Override
    public int getFrameCount() {
        return 1;
    }

    @Override
    public int getQueuedFrameCount() {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void destroy() {
        available = false;
        chosenFormat = null;
        // FIXEM: complete code!
    }

    @Override
    public AudioFrame enqueueData(final int pts, final ByteBuffer byteBuffer, final int byteCount) {
        final byte[] bytes = new byte[byteCount];
        final int p = byteBuffer.position();
        byteBuffer.get(bytes, 0, byteCount);
        byteBuffer.position(p);

        int written = 0;
        int len;
        int bytesLeft = byteCount;
        while (bytesLeft > 0) {
            len = auline.write(bytes, written, byteCount);
            bytesLeft -= len;
            written += len;
        }
        playImpl();
        return new AudioDataFrame(pts, chosenFormat.getBytesDuration(byteCount), byteBuffer, byteCount);
    }

    @Override
    public int getQueuedByteCount() {
        return auline.getBufferSize() - auline.available();
    }

    @Override
    public int getFreeFrameCount() {
        return auline.available();
    }

    @Override
    public int getQueuedTime() {
        return getQueuedTimeImpl( getQueuedByteCount() );
    }
    private final int getQueuedTimeImpl(final int byteCount) {
        final int bytesPerSample = chosenFormat.sampleSize >>> 3; // /8
        return byteCount / ( chosenFormat.channelCount * bytesPerSample * ( chosenFormat.sampleRate / 1000 ) );
    }

    @Override
    public final int getPTS() { return 0; } // FIXME
}
