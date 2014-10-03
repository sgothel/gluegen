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
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.AccessController;
import java.security.PrivilegedAction;

import jogamp.common.Debug;

import com.jogamp.common.os.Platform;

/**
 * An {@link InputStream} implementation based on an underlying {@link FileChannel}'s memory mapped {@link ByteBuffer},
 * {@link #markSupported() supporting} {@link #mark(int) mark} and {@link #reset()}.
 * <p>
 * Implementation allows full memory mapped {@link ByteBuffer} coverage via {@link FileChannel#map(MapMode, long, long) FileChannel}
 * beyond its size limitation of {@link Integer#MAX_VALUE} utilizing an array of {@link ByteBuffer} slices.<br>
 * </p>
 * <p>
 * Implementation further allows full random access via {@link #position()} and {@link #position(long)}
 * and accessing the memory mapped {@link ByteBuffer} slices directly via {@link #currentSlice()} and {@link #nextSlice()}.
 * </p>
 * @since 2.3.0
 */
public class MappedByteBufferInputStream extends InputStream {
    public static enum CacheMode {
        /**
         * Keep all previous lazily cached buffer slices alive, useful for hopping readers,
         * i.e. random access via {@link MappedByteBufferInputStream#position(long) position(p)}
         * or {@link MappedByteBufferInputStream#reset() reset()}.
         * <p>
         * Note that without flushing, the platform may fail memory mapping
         * due to virtual address space exhaustion.<br>
         * In such case an {@link OutOfMemoryError} may be thrown directly,
         * or encapsulated as the {@link IOException#getCause() the cause}
         * of a thrown {@link IOException}.
         * </p>
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
         * <p>
         * This is the default.
         * </p>
         */
        FLUSH_PRE_HARD
    };

    /**
     * File resize interface allowing a file to change its size,
     * e.g. via {@link RandomAccessFile#setLength(long)}.
     */
    public static interface FileResizeOp {
        /**
         * @param newSize the new file size
         * @throws IOException if file size change is not supported or any other I/O error occurs
         */
        void setLength(final long newSize) throws IOException;
    }
    private static final FileResizeOp NoFileResize = new FileResizeOp() {
        @Override
        public void setLength(final long newSize) throws IOException {
            throw new IOException("file size change not supported");
        }
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

    static final boolean DEBUG;

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
    private FileResizeOp fileResizeOp = NoFileResize;

    private int sliceCount;
    private ByteBuffer[] slices;
    private WeakReference<ByteBuffer>[] slices2GC;
    private long totalSize;
    private int slicesEntries, slices2GCEntries;
    private boolean synchronous;

    private int refCount;

    private Method mbbCleaner;
    private Method cClean;
    private boolean cleanerInit;
    private boolean hasCleaner;
    private CacheMode cmode;

    private int sliceIdx;
    private long mark;

    final void dbgDump(final String prefix, final PrintStream out) {
        int _slicesEntries = 0;
        for(int i=0; i<sliceCount; i++) {
            if( null != slices[i] ) {
                _slicesEntries++;
            }
        }
        int _slices2GCEntries = 0;
        int _slices2GCAliveEntries = 0;
        for(int i=0; i<sliceCount; i++) {
            final WeakReference<ByteBuffer> ref = slices2GC[i];
            if( null != ref ) {
                _slices2GCEntries++;
                if( null != ref.get() ) {
                    _slices2GCAliveEntries++;
                }
            }
        }
        long fcSz = 0, pos = 0, rem = 0;
        try {
            fcSz = fc.size();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        if( 0 < refCount ) {
            try {
                pos = position();
                rem = totalSize - pos;
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        final int sliceCount2 = null != slices ? slices.length : 0;
        out.println(prefix+" refCount "+refCount+", fcSize "+fcSz+", totalSize "+totalSize);
        out.println(prefix+" position "+pos+", remaining "+rem);
        out.println(prefix+" mmode "+mmode+", cmode "+cmode+", fileResizeOp "+fileResizeOp);
        out.println(prefix+" slice "+sliceIdx+" / "+sliceCount+" ("+sliceCount2+"), synchronous "+synchronous);
        out.println(prefix+"   mapped   "+slicesEntries+" / "+_slicesEntries);
        out.println(prefix+"   GC-queue "+slices2GCEntries+" / "+_slices2GCEntries+" (alive "+_slices2GCAliveEntries+")");
        out.println(prefix+" sliceShift "+sliceShift+" -> "+(1L << sliceShift));
    }

    MappedByteBufferInputStream(final FileChannel fc, final FileChannel.MapMode mmode, final CacheMode cmode,
                                final int sliceShift, final long totalSize, final int currSliceIdx) throws IOException {
        this.sliceShift = sliceShift;
        this.fc = fc;
        this.mmode = mmode;

        if( 0 > totalSize ) {
            throw new IllegalArgumentException("Negative size "+totalSize);
        }
        // trigger notifyLengthChange
        this.totalSize = -1;
        this.sliceCount = 0;
        notifyLengthChange( totalSize );

        this.refCount = 1;
        this.cleanerInit = false;
        this.hasCleaner = false;
        this.cmode = cmode;

        this.sliceIdx = currSliceIdx;
        this.mark = -1;

        currentSlice().position(0);
    }

    /**
     * Creates a new instance using the given {@link FileChannel}.
     * <p>
     * The {@link ByteBuffer} slices will be mapped lazily at first usage.
     * </p>
     * @param fileChannel the file channel to be mapped lazily.
     * @param mmode the map mode, default is {@link FileChannel.MapMode#READ_ONLY}.
     * @param cmode the caching mode, default is {@link CacheMode#FLUSH_PRE_HARD}.
     * @param sliceShift the pow2 slice size, default is {@link #DEFAULT_SLICE_SHIFT}.
     * @throws IOException
     */
    public MappedByteBufferInputStream(final FileChannel fileChannel,
                                       final FileChannel.MapMode mmode,
                                       final CacheMode cmode,
                                       final int sliceShift) throws IOException {
        this(fileChannel, mmode, cmode, sliceShift, fileChannel.size(), 0);
    }

    /**
     * Creates a new instance using the given {@link FileChannel},
     * given mapping-mode, given cache-mode and the {@link #DEFAULT_SLICE_SHIFT}.
     * <p>
     * The {@link ByteBuffer} slices will be mapped lazily at first usage.
     * </p>
     * @param fileChannel the file channel to be used.
     * @param mmode the map mode, default is {@link FileChannel.MapMode#READ_ONLY}.
     * @param cmode the caching mode, default is {@link CacheMode#FLUSH_PRE_HARD}.
     * @throws IOException
     */
    public MappedByteBufferInputStream(final FileChannel fileChannel, final FileChannel.MapMode mmode, final CacheMode cmode) throws IOException {
        this(fileChannel, mmode, cmode, DEFAULT_SLICE_SHIFT);
    }

    /**
     * Creates a new instance using the given {@link FileChannel},
     * {@link FileChannel.MapMode#READ_ONLY read-only} mapping mode, {@link CacheMode#FLUSH_PRE_HARD}
     * and the {@link #DEFAULT_SLICE_SHIFT}.
     * <p>
     * The {@link ByteBuffer} slices will be mapped {@link FileChannel.MapMode#READ_ONLY} lazily at first usage.
     * </p>
     * @param fileChannel the file channel to be used.
     * @throws IOException
     */
    public MappedByteBufferInputStream(final FileChannel fileChannel) throws IOException {
        this(fileChannel, FileChannel.MapMode.READ_ONLY, CacheMode.FLUSH_PRE_HARD, DEFAULT_SLICE_SHIFT);
    }

    /**
     * Enable or disable synchronous mode.
     * <p>
     * If synchronous mode is enabled, mapped buffers will be {@link #flush(boolean) flushed}
     * if {@link #notifyLengthChange(long) resized}, <i>written to</i> or {@link #close() closing}  in {@link FileChannel.MapMode#READ_WRITE read-write} mapping mode.
     * </p>
     * <p>
     * If synchronous mode is enabled, {@link FileChannel#force(boolean)} is issued
     * if {@link #setLength(long) resizing} or {@link #close() closing} and not in {@link FileChannel.MapMode#READ_ONLY read-only} mapping mode.
     * </p>
     * @param s {@code true} to enable synchronous mode
     */
    public final synchronized void setSynchronous(final boolean s) {
        synchronous = s;
    }
    /**
     * Return {@link #setSynchronous(boolean) synchronous mode}.
     */
    public final synchronized boolean getSynchronous() {
        return synchronous ;
    }

    final synchronized void checkOpen() throws IOException {
        if( 0 == refCount ) {
            throw new IOException("stream closed");
        }
    }

    @Override
    public final synchronized void close() throws IOException {
        if( 0 < refCount ) {
            refCount--;
            if( 0 == refCount ) {
                try {
                    cleanAllSlices( true /* syncBuffer */ );
                } finally {
                    flushImpl(true /* metaData */, false /* syncBuffer */);
                    fc.close();
                    mark = -1;
                    sliceIdx = -1;
                    super.close();
                }
            }
        }
    }

    final FileChannel.MapMode getMapMode() { return mmode; }

    /**
     * @param fileResizeOp the new {@link FileResizeOp}.
     * @throws IllegalStateException if attempting to set the {@link FileResizeOp} to a different value than before
     */
    public final synchronized void setFileResizeOp(final FileResizeOp fileResizeOp) throws IllegalStateException {
        if( NoFileResize != this.fileResizeOp && this.fileResizeOp != fileResizeOp ) {
            throw new IllegalStateException("FileResizeOp already set, this value differs");
        }
        this.fileResizeOp = null != fileResizeOp ? fileResizeOp : NoFileResize;
    }

    /**
     * Resize the underlying {@link FileChannel}'s size and adjusting this instance
     * via {@link #notifyLengthChange(long) accordingly}.
     * <p>
     * User must have a {@link FileResizeOp} {@link #setFileResizeOp(FileResizeOp) registered} before.
     * </p>
     * <p>
     * Implementation calls {@link #notifyLengthChange(long)} after {@link FileResizeOp#setLength(long)}.
     * </p>
     * @param newTotalSize the new total size
     * @throws IOException if no {@link FileResizeOp} has been {@link #setFileResizeOp(FileResizeOp) registered}
     *                     or if a buffer slice operation failed
     */
    public final synchronized void setLength(final long newTotalSize) throws IOException {
        final long currentPosition;
        if( 0 != newTotalSize &&  totalSize != newTotalSize ) {
            currentPosition = position();
        } else {
            currentPosition = -1L;
        }
        if( fc.size() != newTotalSize ) {
            if( Platform.OSType.WINDOWS == Platform.getOSType() ) {
                // On Windows, we have to close all mapped slices.
                // Otherwise we will receive:
                // java.io.IOException: The requested operation cannot be performed on a file with a user-mapped section open
                //      at java.io.RandomAccessFile.setLength(Native Method)
                cleanAllSlices( synchronous );
            }
            fileResizeOp.setLength(newTotalSize);
            if( synchronous ) {
                // buffers will be synchronized in notifyLengthChangeImpl(..)
                flushImpl( true /* metaData */, false /* syncBuffer */);
            }
        }
        notifyLengthChangeImpl(newTotalSize, currentPosition);
    }

    /**
     * Notify this instance that the underlying {@link FileChannel}'s size has been changed
     * and adjusting this instances buffer slices and states accordingly.
     * <p>
     * Should be called by user API when aware of such event.
     * </p>
     * @param newTotalSize the new total size
     * @throws IOException if a buffer slice operation failed
     */
    public final synchronized void notifyLengthChange(final long newTotalSize) throws IOException {
        notifyLengthChangeImpl(newTotalSize, -1L);
    }
    private final synchronized void notifyLengthChangeImpl(final long newTotalSize, final long currentPosition) throws IOException {
        /* if( DEBUG ) {
            System.err.println("notifyLengthChange.0: "+totalSize+" -> "+newTotalSize);
            dbgDump("notifyLengthChange.0:", System.err);
        } */
        if( totalSize == newTotalSize ) {
            // NOP
            return;
        } else if( 0 == newTotalSize ) {
            // ZERO - ensure one entry avoiding NULL checks
            cleanAllSlices( synchronous );
            @SuppressWarnings("unchecked")
            final WeakReference<ByteBuffer>[] newSlices2GC = new WeakReference[ 1 ];
            slices2GC = newSlices2GC;
            slices = new ByteBuffer[1];
            slices[0] = ByteBuffer.allocate(0);
            sliceCount = 0;
            totalSize = 0;
            mark = -1;
            sliceIdx = 0;
        } else {
            final long prePosition = 0 <= currentPosition ? currentPosition : position();

            final long sliceSize = 1L << sliceShift;
            final int newSliceCount = (int)( ( newTotalSize + ( sliceSize - 1 ) ) / sliceSize );
            @SuppressWarnings("unchecked")
            final WeakReference<ByteBuffer>[] newSlices2GC = new WeakReference[ newSliceCount ];
            final ByteBuffer[] newSlices = new ByteBuffer[ newSliceCount ];
            final int copySliceCount = Math.min(newSliceCount, sliceCount-1); // drop last (resize)
            if( 0 <= copySliceCount ) {
                if( 0 < copySliceCount ) {
                    System.arraycopy(slices2GC, 0, newSlices2GC, 0, copySliceCount);
                    System.arraycopy(slices,    0, newSlices,    0, copySliceCount);
                }
                for(int i=copySliceCount; i<sliceCount; i++) { // clip shrunken slices + 1 (last), incl. slices2GC!
                    cleanSlice(i, synchronous);
                }
            }
            slices2GC = newSlices2GC;
            slices = newSlices;
            sliceCount = newSliceCount;
            totalSize = newTotalSize;
            if( newTotalSize < mark ) {
                mark = -1;
            }
            position2( Math.min(prePosition, newTotalSize) ); // -> clipped position (set currSlice and re-map/-pos buffer)
        }
        /* if( DEBUG ) {
            System.err.println("notifyLengthChange.X: "+slices[currSlice]);
            dbgDump("notifyLengthChange.X:", System.err);
        } */
    }

    /**
     * Similar to {@link OutputStream#flush()}, synchronizes all mapped buffers
     * from local storage via {@link MappedByteBuffer#force()}
     * as well as the {@link FileChannel#force(boolean)} w/o {@code metaData}.
     * @param metaData TODO
     * @throws IOException if this stream has been {@link #close() closed}.
     */
    public final synchronized void flush(final boolean metaData) throws IOException {
        checkOpen();
        flushImpl(metaData, true);
    }
    private final synchronized void flushImpl(final boolean metaData, final boolean syncBuffer) throws IOException {
        if( FileChannel.MapMode.READ_ONLY != mmode ) {
            if( syncBuffer && FileChannel.MapMode.READ_WRITE == mmode ) {
                for(int i=0; i<sliceCount; i++) {
                    syncSlice(slices[i], true);
                }
                for(int i=0; i<sliceCount; i++) {
                    final WeakReference<ByteBuffer> ref = slices2GC[i];
                    if( null != ref ) {
                        syncSlice(ref.get(), true);
                    }
                }
            }
            fc.force(metaData);
        }
    }


    /**
     * Returns a new MappedByteBufferOutputStream instance sharing
     * all resources of this input stream, including all buffer slices.
     *
     * @throws IllegalStateException if attempting to set the {@link FileResizeOp} to a different value than before
     * @throws IOException if this instance was opened w/ {@link FileChannel.MapMode#READ_ONLY}
     *                     or if this stream has been {@link #close() closed}.
     */
    public final synchronized MappedByteBufferOutputStream getOutputStream(final FileResizeOp fileResizeOp)
            throws IllegalStateException, IOException
    {
        checkOpen();
        final MappedByteBufferOutputStream res = new MappedByteBufferOutputStream(this, fileResizeOp);
        refCount++;
        return res;
    }

    /**
     * Return the mapped {@link ByteBuffer} slice at the current {@link #position()}.
     * <p>
     * Due to the nature of using sliced buffers mapping the whole region,
     * user has to determine whether the returned buffer covers the desired region
     * and may fetch the {@link #nextSlice()} until satisfied.<br>
     * It is also possible to repeat this operation after reposition the stream via {@link #position(long)}
     * or {@link #skip(long)} to a position within the next block, similar to {@link #nextSlice()}.
     * </p>
     * @throws IOException if a buffer slice operation failed.
     */
    public final synchronized ByteBuffer currentSlice() throws IOException {
        final ByteBuffer s0 = slices[sliceIdx];
        if ( null != s0 ) {
            return s0;
        } else {
            if( CacheMode.FLUSH_PRE_SOFT == cmode ) {
                final WeakReference<ByteBuffer> ref = slices2GC[sliceIdx];
                if( null != ref ) {
                    final ByteBuffer mbb = ref.get();
                    slices2GC[sliceIdx] = null;
                    slices2GCEntries--;
                    if( null != mbb ) {
                        slices[sliceIdx] = mbb;
                        slicesEntries++;
                        return mbb;
                    }
                }
            }
            final long pos = (long)sliceIdx << sliceShift;
            final MappedByteBuffer s1 = fc.map(mmode, pos, Math.min(1L << sliceShift, totalSize - pos));
            slices[sliceIdx] = s1;
            slicesEntries++;
            return s1;
        }
    }

    /**
     * Return the <i>next</i> mapped {@link ByteBuffer} slice from the current {@link #position()},
     * implicitly setting {@link #position(long)} to the start of the returned <i>next</i> slice,
     * see {@link #currentSlice()}.
     * <p>
     * If no subsequent slice is available, {@code null} is being returned.
     * </p>
     * @throws IOException if a buffer slice operation failed.
     */
    public final synchronized ByteBuffer nextSlice() throws IOException {
        if ( sliceIdx < sliceCount - 1 ) {
            flushSlice(sliceIdx, synchronous);
            sliceIdx++;
            final ByteBuffer slice = currentSlice();
            slice.position( 0 );
            return slice;
        } else {
            return null;
        }
    }

    synchronized void syncSlice(final ByteBuffer s) throws IOException {
        syncSlice(s, synchronous);
    }
    synchronized void syncSlice(final ByteBuffer s, final boolean syncBuffer) throws IOException {
        if( syncBuffer && null != s && FileChannel.MapMode.READ_WRITE == mmode ) {
            try {
                ((MappedByteBuffer)s).force();
            } catch( final Throwable t ) {
                // On Windows .. this may happen, like:
                // java.io.IOException: The process cannot access the file because another process has locked a portion of the file
                //   at java.nio.MappedByteBuffer.force0(Native Method)
                //   at java.nio.MappedByteBuffer.force(MappedByteBuffer.java:203)
                if( DEBUG ) {
                    System.err.println("Caught "+t.getMessage());
                    t.printStackTrace();
                }
            }
        }
    }
    private synchronized void flushSlice(final int i, final boolean syncBuffer) throws IOException {
        final ByteBuffer s = slices[i];
        if ( null != s ) {
            if( CacheMode.FLUSH_NONE != cmode ) {
                slices[i] = null; // trigger slice GC
                slicesEntries--;
                if( CacheMode.FLUSH_PRE_HARD == cmode ) {
                    if( !cleanBuffer(s, syncBuffer) ) {
                        // buffer already synced in cleanBuffer(..) if requested
                        slices2GC[i] = new WeakReference<ByteBuffer>(s);
                        slices2GCEntries++;
                    }
                } else {
                    syncSlice(s, syncBuffer);
                    slices2GC[i] = new WeakReference<ByteBuffer>(s);
                    slices2GCEntries++;
                }
            } else {
                syncSlice(s, syncBuffer);
            }
        }
    }
    private synchronized void cleanAllSlices(final boolean syncBuffers) throws IOException {
        if( null != slices ) {
            for(int i=0; i<sliceCount; i++) {
                cleanSlice(i, syncBuffers);
            }
            if( 0 != slicesEntries || 0 != slices2GCEntries ) { // FIXME
                final String err = "mappedSliceCount "+slicesEntries+", slices2GCEntries "+slices2GCEntries;
                dbgDump(err+": ", System.err);
                throw new InternalError(err);
            }
        }
    }

    private synchronized void cleanSlice(final int i, final boolean syncBuffer) throws IOException {
        final ByteBuffer s1 = slices[i];
        final ByteBuffer s2;
        {
            final WeakReference<ByteBuffer> ref = slices2GC[i];
            slices2GC[i] = null;
            if( null != ref ) {
                slices2GCEntries--;
                s2 = ref.get();
            } else {
                s2 = null;
            }
        }
        if( null != s1 ) {
            slices[i] = null;
            slicesEntries--;
            cleanBuffer(s1, syncBuffer);
            if( null != s2 ) {
                throw new InternalError("XXX");
            }
        } else if( null != s2 ) {
            cleanBuffer(s2, syncBuffer);
        }
    }
    private synchronized boolean cleanBuffer(final ByteBuffer mbb, final boolean syncBuffer) throws IOException {
        if( !cleanerInit ) {
            initCleaner(mbb);
        }
        syncSlice(mbb, syncBuffer);
        if( !mbb.isDirect() ) {
            return false;
        }
        boolean res = false;
        if ( hasCleaner ) {
            try {
                cClean.invoke(mbbCleaner.invoke(mbb));
                res = true;
            } catch(final Throwable t) {
                hasCleaner = false;
                if( DEBUG ) {
                    System.err.println("Caught "+t.getMessage());
                    t.printStackTrace();
                }
            }
        }
        if( !res && CacheMode.FLUSH_PRE_HARD == cmode ) {
            cmode = CacheMode.FLUSH_PRE_SOFT;
        }
        return res;
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
    public final synchronized long length() {
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
     * @throws IOException if a buffer slice operation failed.
     */
    public final synchronized long remaining() throws IOException {
        return 0 < refCount ? totalSize - position() : 0;
    }

    /**
     * <i>See {@link #remaining()} for an accurate variant.</i>
     * <p>
     * {@inheritDoc}
     * </p>
     * @throws IOException if a buffer slice operation failed.
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
     * @throws IOException if a buffer slice operation failed.
     */
    // @Override
    public final synchronized long position() throws IOException {
        if( 0 < refCount ) {
            return ( (long)sliceIdx << sliceShift ) + currentSlice().position();
        } else {
            return 0;
        }
    }

    /**
     * Sets the absolute position of the {@link InputStream} to {@code newPosition}.
     * <pre>
     *   <code>0 <= {@link #position()} <= {@link #length()}</code>
     * </pre>
     * @param newPosition The new position, which must be non-negative and &le; {@link #length()}.
     * @return this instance
     * @throws IOException if a buffer slice operation failed or stream is {@link #close() closed}.
     */
    // @Override
    public final synchronized MappedByteBufferInputStream position( final long newPosition ) throws IOException {
        checkOpen();
        if ( totalSize < newPosition || 0 > newPosition ) {
            throw new IllegalArgumentException("new position "+newPosition+" not within [0.."+totalSize+"]");
        }
        final int preSlice = sliceIdx;

        if ( totalSize == newPosition ) {
            // EOF, pos == maxPos + 1
            sliceIdx = Math.max(0, sliceCount - 1); // handle zero size
            if( preSlice != sliceIdx ) {
                flushSlice(preSlice, synchronous);
            }
            final ByteBuffer s = currentSlice();
            s.position( s.capacity() );
        } else {
            sliceIdx = (int)( newPosition >>> sliceShift );
            if( preSlice != sliceIdx ) {
                flushSlice(preSlice, synchronous);
            }
            currentSlice().position( (int)( newPosition - ( (long)sliceIdx << sliceShift ) ) );
        }
        return this;
    }
    private final synchronized void position2( final long newPosition ) throws IOException {
        if ( totalSize == newPosition ) {
            // EOF, pos == maxPos + 1
            sliceIdx = Math.max(0, sliceCount - 1); // handle zero size
            final ByteBuffer s = currentSlice();
            s.position( s.capacity() );
        } else {
            sliceIdx = (int)( newPosition >>> sliceShift );
            currentSlice().position( (int)( newPosition - ( (long)sliceIdx << sliceShift ) ) );
        }
    }

    @Override
    public final boolean markSupported() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <i>Parameter {@code readLimit} is not used in this implementation,
     * since the whole file is memory mapped and no read limitation occurs.</i>
     * </p>
     */
    @Override
    public final synchronized void mark( final int readlimit ) {
        if( 0 < refCount ) {
            try {
                mark = position();
            } catch (final IOException e) {
                throw new RuntimeException(e); // FIXME: oops
            }
        }
    }

    /**
     * {@inheritDoc}
     * @throws IOException if this stream has not been marked,
     *                     a buffer slice operation failed or stream has been {@link #close() closed}.
     */
    @Override
    public final synchronized void reset() throws IOException {
        checkOpen();
        if ( mark == -1 ) {
            throw new IOException("mark not set");
        }
        position( mark );
    }

    /**
     * {@inheritDoc}
     * @throws IOException if a buffer slice operation failed or stream is {@link #close() closed}.
     */
    @Override
    public final synchronized long skip( final long n ) throws IOException {
        checkOpen();
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
        checkOpen();
        ByteBuffer slice = currentSlice();
        if ( !slice.hasRemaining() ) {
            if ( null == ( slice = nextSlice() ) ) {
                return -1;
            }
        }
        return slice.get() & 0xFF;
    }

    @Override
    public final synchronized int read( final byte[] b, final int off, final int len ) throws IOException {
        checkOpen();
        if (b == null) {
            throw new NullPointerException();
        } else if( off < 0 ||
                   len < 0 ||
                   off > b.length ||
                   off + len > b.length ||
                   off + len < 0
                 ) {
            throw new IndexOutOfBoundsException("offset "+off+", length "+len+", b.length "+b.length);
        } else if ( 0 == len ) {
            return 0;
        }
        final long totalRem = remaining();
        if ( 0 == totalRem ) {
            return -1;
        }
        final int maxLen = (int)Math.min( totalRem, len );
        int read = 0;
        while( read < maxLen ) {
            ByteBuffer slice = currentSlice();
            int currRem = slice.remaining();
            if ( 0 == currRem ) {
                if ( null == ( slice = nextSlice() ) ) {
                    throw new InternalError("Unexpected EOT");
                }
                currRem = slice.remaining();
            }
            final int currLen = Math.min( maxLen - read, currRem );
            slice.get( b, off + read, currLen );
            read += currLen;
        }
        return maxLen;
    }

    /**
     * Perform similar to {@link #read(byte[], int, int)}
     * with {@link ByteBuffer} instead of byte array.
     * @param b the {@link ByteBuffer} sink, data is written at current {@link ByteBuffer#position()}
     * @param len the number of bytes to read
     * @return the number of bytes read, -1 for EOS
     * @throws IOException if a buffer slice operation failed or stream has been {@link #close() closed}.
     */
    // @Override
    public final synchronized int read(final ByteBuffer b, final int len) throws IOException {
        checkOpen();
        if (b == null) {
            throw new NullPointerException();
        } else if (len < 0 || len > b.remaining()) {
            throw new IndexOutOfBoundsException("length "+len+", b "+b);
        } else if ( 0 == len ) {
            return 0;
        }
        final long totalRem = remaining();
        if ( 0 == totalRem ) {
            return -1;
        }
        final int maxLen = (int)Math.min( totalRem, len );
        int read = 0;
        while( read < maxLen ) {
            ByteBuffer slice = currentSlice();
            int currRem = slice.remaining();
            if ( 0 == currRem ) {
                if ( null == ( slice = nextSlice() ) ) {
                    throw new InternalError("Unexpected EOT");
                }
                currRem = slice.remaining();
            }
            final int currLen = Math.min( maxLen - read, currRem );
            if( slice.hasArray() && b.hasArray() ) {
                System.arraycopy(slice.array(), slice.arrayOffset() + slice.position(),
                                 b.array(), b.arrayOffset() + b.position(),
                                 currLen);
                slice.position( slice.position() + currLen );
                b.position( b.position() + currLen );
            } else if( currLen == currRem ) {
                b.put(slice);
            } else {
                final int _limit = slice.limit();
                slice.limit(currLen);
                try {
                    b.put(slice);
                } finally {
                    slice.limit(_limit);
                }
            }
            read += currLen;
        }
        return maxLen;
    }
}
