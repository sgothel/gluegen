/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.common.nio;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import jogamp.common.Debug;

import com.jogamp.common.os.Platform;

/**
 * An {@link InputStream} implementation based on an underlying {@link MappedByteBuffer}
 * supporting {@link #markSupported() mark}.
 * <p>
 * Intended to be utilized with a {@link MappedByteBuffer memory-mapped} {@link FileChannel#map(MapMode, long, long) FileChannel}
 * beyond its size limitation of {@link Integer#MAX_VALUE}.<br>
 * </p>
 * @since 2.3.0
 */
public class MappedByteBufferInputStream extends InputStream {
    public static enum CacheMode {
        /**
         * Keep all previous lazily cached buffer slices alive, useful for hopping readers,
         * i.e. random access via {@link MappedByteBufferInputStream#position(long) position(p)}
         * or {@link MappedByteBufferInputStream#reset() reset()}.
         */
        FLUSH_NONE,
        /**
         * Soft flush the previous lazily cached buffer slice when caching the next buffer slice,
         * useful for sequential forward readers, as well as for hopping readers like {@link #FLUSH_NONE}
         * in case of relatively short periods between hopping across slices.
         * <p>
         * Implementation clears the buffer slice reference
         * while preserving a {@link WeakReference} to allow its resurrection if not yet
         * {@link System#gc() garbage collected}.
         * </p>
         * <p>
         * This is the default.
         * </p>
         */
        FLUSH_PRE_SOFT,
        /**
         * Hard flush the previous lazily cached buffer slice when caching the next buffer slice,
         * useful for sequential forward readers.
         * <p>
         * Besides clearing the buffer slice reference,
         * implementation attempts to hard flush the mapped buffer
         * using a {@code sun.misc.Cleaner} by reflection.
         * In case such method does not exist nor works, implementation falls back to {@link #FLUSH_PRE_SOFT}.
         * </p>
         */
        FLUSH_PRE_HARD
    };

    /**
     * Default slice shift, i.e. 1L << shift, denoting slice size in MiB:
     * <ul>
     *   <li>{@link Platform#is64Bit() 64bit machines} -> 30 = 1024 MiB</li>
     *   <li>{@link Platform#is32Bit() 32bit machines} -> 29 = 512 MiB</li>
     * </ul>
     * <p>
     * In case the default is too much of-used up address-space, one may choose other values:
     * <ul>
     *   <li>29 ->  512 MiB</li>
     *   <li>28 ->  256 MiB</li>
     *   <li>27 ->  128 MiB</li>
     *   <li>26 ->   64 MiB</li>
     * </ul>
     * </p>
     */
    public static final int DEFAULT_SLICE_SHIFT;

    private static final boolean DEBUG;

    static {
        Platform.initSingleton();
        if( Platform.is32Bit() ) {
            DEFAULT_SLICE_SHIFT = 29;
        } else {
            DEFAULT_SLICE_SHIFT = 30;
        }

        DEBUG = Debug.debug("ByteBufferInputStream");
    }

    private final int sliceShift;
    private final FileChannel fc;
    private final FileChannel.MapMode mmode;
    private final MappedByteBuffer[] slices;
    private final WeakReference<MappedByteBuffer>[] slices2GC;
    private final int sliceCount;
    private final long totalSize;

    private Method mbbCleaner;
    private Method cClean;
    private boolean cleanerInit;
    private boolean hasCleaner;
    private CacheMode cmode;

    private int currSlice;
    private long mark;

    @SuppressWarnings("unchecked")
    MappedByteBufferInputStream(final FileChannel fc, final FileChannel.MapMode mmode, final CacheMode cmode,
                                final int sliceShift, final MappedByteBuffer[] bufs, final long totalSize,
                                final int currSlice) throws IOException {
        this.sliceShift = sliceShift;
        this.fc = fc;
        this.mmode = mmode;
        this.slices = bufs;
        this.sliceCount = bufs.length;
        this.slices2GC = new WeakReference[sliceCount];
        this.totalSize = totalSize;
        if( 0 >= totalSize || 0 >= sliceCount ) {
            throw new IllegalArgumentException("Zero sized: total "+totalSize+", slices "+sliceCount);
        }

        this.cleanerInit = false;
        this.hasCleaner = false;
        this.cmode = cmode;

        this.currSlice = currSlice;
        this.mark = -1;

        slice(currSlice).position(0);
    }

    /**
     * Creates a new instance using the given {@link FileChannel},
     * {@link FileChannel.MapMode#READ_ONLY read-only} mapping mode, {@link CacheMode#FLUSH_PRE_SOFT}
     * and the {@link #DEFAULT_SLICE_SHIFT}.
     * <p>
     * The {@link MappedByteBuffer} slices will be mapped {@link FileChannel.MapMode#READ_ONLY} lazily at first usage.
     * </p>
     * @param fileChannel the file channel to be used.
     * @throws IOException
     */
    public static MappedByteBufferInputStream create(final FileChannel fileChannel) throws IOException {
        return create(fileChannel, FileChannel.MapMode.READ_ONLY, CacheMode.FLUSH_PRE_SOFT, DEFAULT_SLICE_SHIFT);
    }

    /**
     * Creates a new instance using the given {@link FileChannel},
     * {@link FileChannel.MapMode#READ_ONLY read-only} mapping mode and the {@link #DEFAULT_SLICE_SHIFT}.
     * <p>
     * The {@link MappedByteBuffer} slices will be mapped {@link FileChannel.MapMode#READ_ONLY} lazily at first usage.
     * </p>
     * @param fileChannel the file channel to be used.
     * @param cmode the caching mode, default is {@link CacheMode#FLUSH_PRE_SOFT}.
     * @throws IOException
     */
    public static MappedByteBufferInputStream create(final FileChannel fileChannel, final CacheMode cmode) throws IOException {
        return create(fileChannel, FileChannel.MapMode.READ_ONLY, cmode, DEFAULT_SLICE_SHIFT);
    }

    /**
     * Creates a new instance using the given {@link FileChannel}.
     * <p>
     * The {@link MappedByteBuffer} slices will be mapped lazily at first usage.
     * </p>
     * @param fileChannel the file channel to be mapped lazily.
     * @param mmode the map mode, default is {@link FileChannel.MapMode#READ_ONLY}.
     * @param cmode the caching mode, default is {@link CacheMode#FLUSH_PRE_SOFT}.
     * @param sliceShift the pow2 slice size, default is {@link #DEFAULT_SLICE_SHIFT}.
     * @throws IOException
     */
    public static MappedByteBufferInputStream create(final FileChannel fileChannel,
                                                     final FileChannel.MapMode mmode,
                                                     final CacheMode cmode,
                                                     final int sliceShift) throws IOException {
        final long sliceSize = 1L << sliceShift;
        final long totalSize = fileChannel.size();
        final int sliceCount = (int)( ( totalSize + ( sliceSize - 1 ) ) / sliceSize );
        final MappedByteBuffer[] bufs = new MappedByteBuffer[ sliceCount ];
        return new MappedByteBufferInputStream(fileChannel, mmode, cmode, sliceShift, bufs, totalSize, 0);
    }

    @Override
    public final synchronized void close() throws IOException {
        for(int i=0; i<sliceCount; i++) {
            final MappedByteBuffer s = slices[i];
            if( null != s ) {
                slices[i] = null;
                cleanBuffer(s);
            }
            slices2GC[i] = null;
        }
        if( mmode != FileChannel.MapMode.READ_ONLY ) {
            fc.force(true);
        }
        fc.close();
        mark = -1;
        currSlice = -1;
        super.close();
    }

    private synchronized MappedByteBuffer slice(final int i) throws IOException {
        if ( null != slices[i] ) {
            return slices[i];
        } else {
            if( CacheMode.FLUSH_PRE_SOFT == cmode ) {
                final WeakReference<MappedByteBuffer> ref = slices2GC[i];
                if( null != ref ) {
                    final MappedByteBuffer mbb = ref.get();
                    slices2GC[i] = null;
                    if( null != mbb ) {
                        slices[i] = mbb;
                        return mbb;
                    }
                }
            }
            final long pos = (long)i << sliceShift;
            slices[i] = fc.map(mmode, pos, Math.min(1L << sliceShift, totalSize - pos));
            return slices[i];
        }
    }

    private synchronized void flushSlice(final int i) throws IOException {
        final MappedByteBuffer s = slices[i];
        if ( null != s ) {
            slices[i] = null; // GC a slice is enough
            if( CacheMode.FLUSH_PRE_HARD == cmode ) {
                if( !cleanBuffer(s) ) {
                    cmode = CacheMode.FLUSH_PRE_SOFT;
                    slices2GC[i] = new WeakReference<MappedByteBuffer>(s);
                }
            } else {
                slices2GC[i] = new WeakReference<MappedByteBuffer>(s);
            }
        }
    }
    private synchronized boolean cleanBuffer(final MappedByteBuffer mbb) {
        if( !cleanerInit ) {
            initCleaner(mbb);
        }
        if ( !hasCleaner || !mbb.isDirect() ) {
            return false;
        }
        try {
            cClean.invoke(mbbCleaner.invoke(mbb));
            return true;
        } catch(final Throwable t) {
            hasCleaner = false;
            if( DEBUG ) {
                System.err.println("Caught "+t.getMessage());
                t.printStackTrace();
            }
            return false;
        }
    }
    private synchronized void initCleaner(final ByteBuffer bb) {
        final Method[] _mbbCleaner = { null };
        final Method[] _cClean = { null };
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    _mbbCleaner[0] = bb.getClass().getMethod("cleaner");
                    _mbbCleaner[0].setAccessible(true);
                    _cClean[0] = Class.forName("sun.misc.Cleaner").getMethod("clean");
                    _cClean[0].setAccessible(true);
                } catch(final Throwable t) {
                    if( DEBUG ) {
                        System.err.println("Caught "+t.getMessage());
                        t.printStackTrace();
                    }
                }
                return null;
            } } );
        mbbCleaner = _mbbCleaner[0];
        cClean = _cClean[0];
        final boolean res = null != mbbCleaner && null != cClean;
        if( DEBUG ) {
            System.err.println("initCleaner: Has cleaner: "+res+", mbbCleaner "+mbbCleaner+", cClean "+cClean);
        }
        hasCleaner = res;
        cleanerInit = true;
    }

    /**
     * Return the used {@link CacheMode}.
     * <p>
     * If a desired {@link CacheMode} is not available, it may fall back to an available one at runtime,
     * see {@link CacheMode#FLUSH_PRE_HARD}.<br>
     * This evaluation only happens if the {@link CacheMode} != {@link CacheMode#FLUSH_NONE}
     * and while attempting to flush an unused buffer slice.
     * </p>
     */
    public final synchronized CacheMode getCacheMode() { return cmode; }

    /**
     * Returns the total size in bytes of the {@link InputStream}
     * <pre>
     *   <code>0 <= {@link #position()} <= {@link #length()}</code>
     * </pre>
     */
    // @Override
    public final long length() {
        return totalSize;
    }

    /**
     * Returns the number of remaining available bytes of the {@link InputStream},
     * i.e. <code>{@link #length()} - {@link #position()}</code>.
     * <pre>
     *   <code>0 <= {@link #position()} <= {@link #length()}</code>
     * </pre>
     * <p>
     * In contrast to {@link InputStream}'s {@link #available()} method,
     * this method returns the proper return type {@code long}.
     * </p>
     * @throws IOException
     */
    public final synchronized long remaining() throws IOException {
        return totalSize - position();
    }

    /**
     * <i>See {@link #remaining()} for an accurate variant.</i>
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public final synchronized int available() throws IOException {
        final long available = remaining();
        return available <= Integer.MAX_VALUE ? (int)available : Integer.MAX_VALUE;
    }

    /**
     * Returns the absolute position of the {@link InputStream}.
     * <pre>
     *   <code>0 <= {@link #position()} <= {@link #length()}</code>
     * </pre>
     * @throws IOException
     */
    // @Override
    public final synchronized long position() throws IOException {
        return ( (long)currSlice << sliceShift ) + slice( currSlice ).position();
    }

    /**
     * Sets the absolute position of the {@link InputStream} to {@code newPosition}.
     * <pre>
     *   <code>0 <= {@link #position()} <= {@link #length()}</code>
     * </pre>
     * @param newPosition The new position, which must be non-negative and &le; {@link #length()}.
     * @return this instance
     * @throws IOException
     */
    // @Override
    public final synchronized MappedByteBufferInputStream position( final long newPosition ) throws IOException {
        if ( totalSize < newPosition || 0 > newPosition ) {
            throw new IllegalArgumentException("new position "+newPosition+" not within [0.."+totalSize+"]");
        }
        final int preSlice = currSlice;
        if ( totalSize == newPosition ) {
            currSlice = sliceCount - 1;
            final MappedByteBuffer s = slice( currSlice );
            s.position( s.capacity() );
        } else {
            currSlice = (int)( newPosition >>> sliceShift );
            slice( currSlice ).position( (int)( newPosition - ( (long)currSlice << sliceShift ) ) );
        }
        if( CacheMode.FLUSH_NONE != cmode && preSlice != currSlice) {
            flushSlice(preSlice);
        }
        return this;
    }

    @Override
    public final boolean markSupported() {
        return true;
    }

    @Override
    public final synchronized void mark( final int unused ) {
        try {
            mark = position();
        } catch (final IOException e) {
            throw new RuntimeException(e); // FIXME: oops
        }
    }

    @Override
    public final synchronized void reset() throws IOException {
        if ( mark == -1 ) {
            throw new IOException("mark not set");
        }
        position( mark );
    }

    @Override
    public final synchronized long skip( final long n ) throws IOException {
        if( 0 > n ) {
            return 0;
        }
        final long pos = position();
        final long rem = totalSize - pos; // remaining
        final long s = Math.min( rem, n );
        position( pos + s );
        return s;
    }

    @Override
    public final synchronized int read() throws IOException {
        if ( ! slice( currSlice ).hasRemaining() ) {
            if ( currSlice < sliceCount - 1 ) {
                final int preSlice = currSlice;
                currSlice++;
                slice( currSlice ).position( 0 );
                if( CacheMode.FLUSH_NONE != cmode ) {
                    flushSlice(preSlice);
                }
            } else {
                return -1;
            }
        }
        return slices[ currSlice ].get() & 0xFF;
    }

    @Override
    public final synchronized int read( final byte[] b, final int off, final int len ) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException("offset "+off+", length "+len+", b.length "+b.length);
        }
        if ( 0 == len ) {
            return 0;
        }
        final long totalRem = remaining();
        if ( 0 == totalRem ) {
            return -1;
        }
        final int maxLen = (int)Math.min( totalRem, len );
        int read = 0;
        while( read < maxLen ) {
            int currRem = slice( currSlice ).remaining();
            if ( 0 == currRem ) {
                final int preSlice = currSlice;
                currSlice++;
                slice( currSlice ).position( 0 );
                currRem = slice( currSlice ).remaining();
                if( CacheMode.FLUSH_NONE != cmode ) {
                    flushSlice(preSlice);
                }
            }
            slices[ currSlice ].get( b, off + read, Math.min( maxLen - read, currRem ) );
            read += Math.min( maxLen - read, currRem );
        }
        return maxLen;
    }
}
