/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Hardware independent container holding an array of linearly aligned elements,
 * while its {@link #getDirectBufferAddress()} is-a pointer-type value, i.e. the element-array address.
 * <p>
 * An instance maps an array of linearly aligned elements, represented as bytes.
 * </p>
 */
public class ElementBuffer extends AbstractBuffer<ElementBuffer> {
    /** supports backup array */
    ElementBuffer(final int elementSize, final ByteBuffer b) {
        super(b, elementSize, b.capacity()/elementSize);
    }

    /** Returns a non direct PointerBuffer in native order, having a backup array */
    public static ElementBuffer allocate(final int elementSize, final int elemCount) {
        return new ElementBuffer(elementSize, ByteBuffer.allocate(elemCount * elementSize));
    }

    /** Returns a direct PointerBuffer in native order, w/o backup array */
    public static ElementBuffer allocateDirect(final int elementSize, final int elemCount) {
        return new ElementBuffer(elementSize, Buffers.newDirectByteBuffer(elemCount * elementSize));
    }

    public static ElementBuffer wrap(final int elementSize, final ByteBuffer src) {
        return new ElementBuffer(elementSize, src);
    }
    public static ElementBuffer wrap(final int elementSize, final ByteBuffer src, final int srcByteOffset, final int elemCount) {
        final int oldPos = src.position();
        final int oldLimit = src.limit();
        src.position(srcByteOffset);
        src.limit(srcByteOffset + ( elementSize * elemCount ));
        final ElementBuffer r = new ElementBuffer(elementSize, src.slice().order(src.order())); // slice and duplicate may change byte order
        src.position(oldPos);
        src.limit(oldLimit);
        return r;
    }
    public static ElementBuffer derefPointer(final int elementSize, final long aptr, final int elemCount) {
        if( 0 == aptr ) {
            throw new NullPointerException("aptr is null");
        }
        final ByteBuffer bb = Buffers.getDirectByteBuffer(aptr, elemCount * elementSize);
        if( null == bb ) {
            throw new InternalError("Couldn't dereference aptr 0x"+Long.toHexString(aptr)+", size "+elemCount+" * "+elementSize);
        }
        return new ElementBuffer(elementSize, bb);
    }
    public static ElementBuffer derefPointer(final int elementSize, final ByteBuffer ptrSrc, final int ptrSrcByteOffset, final int elemCount) {
        return derefPointer(elementSize, PointerBuffer.wrap(ptrSrc, ptrSrcByteOffset, 1).get(0), elemCount);
    }

    @Override
    public final ElementBuffer put(final ElementBuffer src) {
        final int elemCount = src.remaining();
        if (remaining() < elemCount) {
            throw new IndexOutOfBoundsException("remaining[this "+remaining()+" < src "+elemCount+"], this "+this+", src "+src);
        }
        if( this.elementSize() != src.elementSize() ) {
            throw new IllegalArgumentException("Element-Size mismatch source "+src+", dest "+this);
        }
        final int srcPos = src.position();
        put(src.getByteBuffer(), srcPos, position, elemCount);
        src.position(srcPos + elemCount);
        position += elemCount;
        return this;
    }

    /** Returns the ByteBuffer, i.e. {@link #getBuffer()} w/o casting. */
    public final ByteBuffer getByteBuffer() {
        return (ByteBuffer) buffer;
    }

    /**
     * Returns a slice of this instance's ByteBuffer `[offset..offset+length)`, i.e. referencing the underlying bytes.
     * @param offset starting element-index within this buffer
     * @param length element count
     * @return slice of this instance's ByteBuffer
     */
    public final ByteBuffer slice(final int offset, final int length) {
        if (0 > offset || offset + length > limit()) {
            throw new IndexOutOfBoundsException("idx "+offset+" + elemCount "+length+" not within [0.."+limit()+"), "+this);
        }
        final ByteBuffer src = getByteBuffer();
        final int oldPos = src.position();
        final int oldLimit = src.limit();
        src.position( elementSize * offset ).limit(elementSize * (offset + length));
        final ByteBuffer ref = src.slice().order(src.order()); // slice and duplicate may change byte order
        src.position( oldPos ).limit( oldLimit );
        return ref;
    }

    /** Absolute get method. Get element-bytes for `elemCount` elements from this buffer at `srcElemPos` into `destElemBytes` at the given element-index `destElemPos` */
    public final ByteBuffer get(final int srcElemPos, final ByteBuffer destElemBytes, final int destElemPos, final int elemCount) {
        if (0 > srcElemPos || srcElemPos + elemCount > limit() || 0 > elemCount ||
            0 > destElemPos || elementSize * ( destElemPos + elemCount ) > destElemBytes.limit() ) {
            throw new IndexOutOfBoundsException("destElemPos "+destElemPos+", srcElemPos "+srcElemPos+", elemCount "+elemCount+
                                                ", srcLimit "+limit()+", destLimit "+(destElemBytes.limit()/elementSize)+", "+this);
        }
        final ByteBuffer srcElemBytes = getByteBuffer();
        final int oldSrcLim = srcElemBytes.limit();
        srcElemBytes.position( srcElemPos * elementSize ).limit( ( srcElemPos + elemCount ) * elementSize ); // remaining = elemCount * elementSize
        final int oldDestPos = destElemBytes.position();
        destElemBytes.position( destElemPos * elementSize );
        destElemBytes.put(srcElemBytes).position(oldDestPos);
        srcElemBytes.limit(oldSrcLim).rewind();
        return destElemBytes;
    }
    /** Absolute get method. Copy the element-bytes from this buffer at the given element-index `srcElemPos`, storing them into `destElemBytes`. */
    public final ByteBuffer get(final int srcElemPos, final ByteBuffer destElemBytes) {
        return get(srcElemPos, destElemBytes, 0, 1);
    }
    /** Relative get method. Copy the element-bytes at the current position and increment the position by one, storing the element-bytes into `destElemBytes`. */
    public final ByteBuffer get(final ByteBuffer destElemBytes) {
        final ByteBuffer r = get(position, destElemBytes, 0, 1);
        position++;
        return r;
    }
    /**
     * Relative bulk get method. Copy the element-bytes <code> [ position .. position+elemCount [</code>
     * to the destination array <code> [ destElements[destElemPos] .. destElements[destElemPos+elemCount] [ </code>
     * and increment the position by <code>elemCount</code>. */
    public final ElementBuffer get(final ByteBuffer[] destElements, int destElemPos, int elemCount) {
        if (destElements.length<destElemPos+elemCount) {
            throw new IndexOutOfBoundsException("dest.length "+destElements.length+" < (offset "+destElemPos+" + length "+elemCount+")");
        }
        if (remaining() < elemCount) {
            throw new IndexOutOfBoundsException("remaining "+remaining()+" < length "+elemCount+", this "+this);
        }
        while(elemCount>0) {
            get(position++, destElements[destElemPos++]);
            elemCount--;
        }
        return this;
    }

    /** Absolute get method. Get byte-elements for `elemCount` elements from this buffer at `srcElemPos` into `dest` at the given element-index `destElemPos` */
    public final ElementBuffer get(final int srcElemPos, final byte[] dest, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_BYTE != elementSize ) { throw new UnsupportedOperationException("'byte' type byte-size "+Buffers.SIZEOF_BYTE+" != elementSize "+elementSize+", "+this); }
        if (0 > srcElemPos || srcElemPos + elemCount > limit() || 0 > elemCount ||
            0 > destElemPos || destElemPos + elemCount > dest.length ) {
            throw new IndexOutOfBoundsException("destElemPos "+destElemPos+", srcElemPos "+srcElemPos+", elemCount "+elemCount+
                                                ", srcLimit "+limit()+", destLimit "+dest.length+", "+this);
        }
        final ByteBuffer src = getByteBuffer();
        final int oldPos = src.position();
        final int oldLim = src.limit();
        src.position( srcElemPos ).limit( srcElemPos + elemCount ); // remaining = elemCount
        src.get(dest, destElemPos, elemCount);
        src.position( oldPos ).limit( oldLim );
        return this;
    }
    /** Absolute get method. Get short-elements for `elemCount` elements from this buffer at `srcElemPos` into `dest` at the given element-index `destElemPos` */
    public final ElementBuffer get(final int srcElemPos, final short[] dest, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_SHORT != elementSize ) { throw new UnsupportedOperationException("'short' type byte-size "+Buffers.SIZEOF_SHORT+" != elementSize "+elementSize+", "+this); }
        if (0 > srcElemPos || srcElemPos + elemCount > limit() || 0 > elemCount ||
            0 > destElemPos || destElemPos + elemCount > dest.length ) {
            throw new IndexOutOfBoundsException("destElemPos "+destElemPos+", srcElemPos "+srcElemPos+", elemCount "+elemCount+
                                                ", srcLimit "+limit()+", destLimit "+dest.length+", "+this);
        }
        final ShortBuffer src = getByteBuffer().asShortBuffer();
        src.position( srcElemPos ).limit( srcElemPos + elemCount ); // remaining = elemCount
        src.get(dest, destElemPos, elemCount);
        return this;
    }
    /** Absolute get method. Get char-elements for `elemCount` elements from this buffer at `srcElemPos` into `dest` at the given element-index `destElemPos` */
    public final ElementBuffer get(final int srcElemPos, final char[] dest, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_CHAR != elementSize ) { throw new UnsupportedOperationException("'char' type byte-size "+Buffers.SIZEOF_CHAR+" != elementSize "+elementSize+", "+this); }
        if (0 > srcElemPos || srcElemPos + elemCount > limit() || 0 > elemCount ||
            0 > destElemPos || destElemPos + elemCount > dest.length ) {
            throw new IndexOutOfBoundsException("destElemPos "+destElemPos+", srcElemPos "+srcElemPos+", elemCount "+elemCount+
                                                ", srcLimit "+limit()+", destLimit "+dest.length+", "+this);
        }
        final CharBuffer src = getByteBuffer().asCharBuffer();
        src.position( srcElemPos ).limit( srcElemPos + elemCount ); // remaining = elemCount
        src.get(dest, destElemPos, elemCount);
        return this;
    }
    /** Absolute get method. Get int-elements for `elemCount` elements from this buffer at `srcElemPos` into `dest` at the given element-index `destElemPos` */
    public final ElementBuffer get(final int srcElemPos, final int[] dest, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_INT != elementSize ) { throw new UnsupportedOperationException("'int' type byte-size "+Buffers.SIZEOF_INT+" != elementSize "+elementSize+", "+this); }
        if (0 > srcElemPos || srcElemPos + elemCount > limit() || 0 > elemCount ||
            0 > destElemPos || destElemPos + elemCount > dest.length ) {
            throw new IndexOutOfBoundsException("destElemPos "+destElemPos+", srcElemPos "+srcElemPos+", elemCount "+elemCount+
                                                ", srcLimit "+limit()+", destLimit "+dest.length+", "+this);
        }
        final IntBuffer src = getByteBuffer().asIntBuffer();
        src.position( srcElemPos ).limit( srcElemPos + elemCount ); // remaining = elemCount
        src.get(dest, destElemPos, elemCount);
        return this;
    }
    /** Absolute get method. Get float-elements for `elemCount` elements from this buffer at `srcElemPos` into `dest` at the given element-index `destElemPos` */
    public final ElementBuffer get(final int srcElemPos, final float[] dest, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_FLOAT != elementSize ) { throw new UnsupportedOperationException("'float' type byte-size "+Buffers.SIZEOF_FLOAT+" != elementSize "+elementSize+", "+this); }
        if (0 > srcElemPos || srcElemPos + elemCount > limit() || 0 > elemCount ||
            0 > destElemPos || destElemPos + elemCount > dest.length ) {
            throw new IndexOutOfBoundsException("destElemPos "+destElemPos+", srcElemPos "+srcElemPos+", elemCount "+elemCount+
                                                ", srcLimit "+limit()+", destLimit "+dest.length+", "+this);
        }
        final FloatBuffer src = getByteBuffer().asFloatBuffer();
        src.position( srcElemPos ).limit( srcElemPos + elemCount ); // remaining = elemCount
        src.get(dest, destElemPos, elemCount);
        return this;
    }
    /** Absolute get method. Get long-elements for `elemCount` elements from this buffer at `srcElemPos` into `dest` at the given element-index `destElemPos` */
    public final ElementBuffer get(final int srcElemPos, final long[] dest, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_LONG != elementSize ) { throw new UnsupportedOperationException("'long' type byte-size "+Buffers.SIZEOF_LONG+" != elementSize "+elementSize+", "+this); }
        if (0 > srcElemPos || srcElemPos + elemCount > limit() || 0 > elemCount ||
            0 > destElemPos || destElemPos + elemCount > dest.length ) {
            throw new IndexOutOfBoundsException("destElemPos "+destElemPos+", srcElemPos "+srcElemPos+", elemCount "+elemCount+
                                                ", srcLimit "+limit()+", destLimit "+dest.length+", "+this);
        }
        final LongBuffer src = getByteBuffer().asLongBuffer();
        src.position( srcElemPos ).limit( srcElemPos + elemCount ); // remaining = elemCount
        src.get(dest, destElemPos, elemCount);
        return this;
    }
    /** Absolute get method. Get double-elements for `elemCount` elements from this buffer at `srcElemPos` into `dest` at the given element-index `destElemPos` */
    public final ElementBuffer get(final int srcElemPos, final double[] dest, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_DOUBLE != elementSize ) { throw new UnsupportedOperationException("'double' type byte-size "+Buffers.SIZEOF_DOUBLE+" != elementSize "+elementSize+", "+this); }
        if (0 > srcElemPos || srcElemPos + elemCount > limit() || 0 > elemCount ||
            0 > destElemPos || destElemPos + elemCount > dest.length ) {
            throw new IndexOutOfBoundsException("destElemPos "+destElemPos+", srcElemPos "+srcElemPos+", elemCount "+elemCount+
                                                ", srcLimit "+limit()+", destLimit "+dest.length+", "+this);
        }
        final DoubleBuffer src = getByteBuffer().asDoubleBuffer();
        src.position( srcElemPos ).limit( srcElemPos + elemCount ); // remaining = elemCount
        src.get(dest, destElemPos, elemCount);
        return this;
    }


    /** Absolute put method. Put element-bytes for `elemCount` elements from `srcElemBytes` at `srcElemPos` into this buffer at the given element-index `destElemPos` */
    public final ElementBuffer put(final ByteBuffer srcElemBytes, final int srcElemPos, final int destElemPos, final int elemCount) {
        if (0 > destElemPos || destElemPos + elemCount > limit() || 0 > elemCount ||
            0 > srcElemPos || elementSize * ( srcElemPos + elemCount ) > srcElemBytes.limit() ) {
            throw new IndexOutOfBoundsException("srcElemPos "+srcElemPos+", destElemPos "+destElemPos+", elemCount "+elemCount+
                                                ", destLimit "+limit()+", srcLimit "+(srcElemBytes.limit()/elementSize)+", "+this);
        }
        final ByteBuffer destElemBytes = getByteBuffer();
        final int oldSrcPos = srcElemBytes.position();
        final int oldSrcLim = srcElemBytes.limit();
        srcElemBytes.position( srcElemPos * elementSize ).limit( ( srcElemPos + elemCount ) * elementSize ); // remaining = elemCount * elementSize
        destElemBytes.position( elementSize * destElemPos );
        destElemBytes.put(srcElemBytes).rewind();
        srcElemBytes.limit(oldSrcLim).position(oldSrcPos);
        return this;
    }
    /** Absolute put method. Put element-bytes from `srcElemBytes` into the given element-index `destElemPos` */
    public final ElementBuffer put(final int destElemPos, final ByteBuffer srcElemBytes) {
        return put(srcElemBytes, 0, destElemPos, 1);
    }
    /** Relative put method. Put the element-bytes at the current position and increment the position by one. */
    public final ElementBuffer put(final ByteBuffer srcElemBytes) {
        put(position, srcElemBytes);
        position++;
        return this;
    }
    /**
     * Relative bulk put method. Put the element-bytes <code> [ srcElements[offset] .. srcElements[offset+length] [</code>
     * at the current position and increment the position by <code>length</code>. */
    public final ElementBuffer put(final ByteBuffer[] srcElements, int offset, int length) {
        if (srcElements.length<offset+length) {
            throw new IndexOutOfBoundsException("src.length "+srcElements.length+" < (offset "+offset+" + length "+length+")");
        }
        if (remaining() < length) {
            throw new IndexOutOfBoundsException("remaining "+remaining()+" < length "+length+", this "+this);
        }
        while(length>0) {
            put(position++, srcElements[offset++]);
            length--;
        }
        return this;
    }

    /** Absolute put method. Put byte-elements for `elemCount` elements from `src` at `srcElemPos` into this buffer at the given element-index `destElemPos` */
    public final ElementBuffer put(final byte[] src, final int srcElemPos, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_BYTE != elementSize ) { throw new UnsupportedOperationException("'byte' type byte-size "+Buffers.SIZEOF_BYTE+" != elementSize "+elementSize+", "+this); }
        if (0 > destElemPos || destElemPos + elemCount > limit() || 0 > elemCount ||
            0 > srcElemPos || srcElemPos + elemCount > src.length ) {
            throw new IndexOutOfBoundsException("srcElemPos "+srcElemPos+", destElemPos "+destElemPos+", elemCount "+elemCount+
                                                ", destLimit "+limit()+", srcLimit "+src.length+", "+this);
        }
        final ByteBuffer dest = getByteBuffer();
        final int oldPos = dest.position();
        final int oldLim = dest.limit();
        dest.position( destElemPos ).limit( destElemPos + elemCount ); // remaining = elemCount
        dest.put(src, srcElemPos, elemCount);
        dest.position( oldPos ).limit( oldLim );
        return this;
    }
    /** Absolute put method. Put short-elements for `elemCount` elements from `src` at `srcElemPos` into this buffer at the given element-index `destElemPos` */
    public final ElementBuffer put(final short[] src, final int srcElemPos, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_SHORT != elementSize ) { throw new UnsupportedOperationException("'short' type byte-size "+Buffers.SIZEOF_SHORT+" != elementSize "+elementSize+", "+this); }
        if (0 > destElemPos || destElemPos + elemCount > limit() || 0 > elemCount ||
            0 > srcElemPos || srcElemPos + elemCount > src.length ) {
            throw new IndexOutOfBoundsException("srcElemPos "+srcElemPos+", destElemPos "+destElemPos+", elemCount "+elemCount+
                                                ", destLimit "+limit()+", srcLimit "+src.length+", "+this);
        }
        final ShortBuffer dest = getByteBuffer().asShortBuffer();
        dest.position( destElemPos ).limit( destElemPos + elemCount ); // remaining = elemCount
        dest.put(src, srcElemPos, elemCount);
        return this;
    }
    /** Absolute put method. Put char-elements for `elemCount` elements from `src` at `srcElemPos` into this buffer at the given element-index `destElemPos` */
    public final ElementBuffer put(final char[] src, final int srcElemPos, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_CHAR != elementSize ) { throw new UnsupportedOperationException("'char' type byte-size "+Buffers.SIZEOF_CHAR+" != elementSize "+elementSize+", "+this); }
        if (0 > destElemPos || destElemPos + elemCount > limit() || 0 > elemCount ||
            0 > srcElemPos || srcElemPos + elemCount > src.length ) {
            throw new IndexOutOfBoundsException("srcElemPos "+srcElemPos+", destElemPos "+destElemPos+", elemCount "+elemCount+
                                                ", destLimit "+limit()+", srcLimit "+src.length+", "+this);
        }
        final CharBuffer dest = getByteBuffer().asCharBuffer();
        dest.position( destElemPos ).limit( destElemPos + elemCount ); // remaining = elemCount
        dest.put(src, srcElemPos, elemCount);
        return this;
    }
    /** Absolute put method. Put int-elements for `elemCount` elements from `src` at `srcElemPos` into this buffer at the given element-index `destElemPos` */
    public final ElementBuffer put(final int[] src, final int srcElemPos, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_INT != elementSize ) { throw new UnsupportedOperationException("'int' type byte-size "+Buffers.SIZEOF_INT+" != elementSize "+elementSize+", "+this); }
        if (0 > destElemPos || destElemPos + elemCount > limit() || 0 > elemCount ||
            0 > srcElemPos || srcElemPos + elemCount > src.length ) {
            throw new IndexOutOfBoundsException("srcElemPos "+srcElemPos+", destElemPos "+destElemPos+", elemCount "+elemCount+
                                                ", destLimit "+limit()+", srcLimit "+src.length+", "+this);
        }
        final IntBuffer dest = getByteBuffer().asIntBuffer();
        dest.position( destElemPos ).limit( destElemPos + elemCount ); // remaining = elemCount
        dest.put(src, srcElemPos, elemCount);
        return this;
    }
    /** Absolute put method. Put float-elements for `elemCount` elements from `src` at `srcElemPos` into this buffer at the given element-index `destElemPos` */
    public final ElementBuffer put(final float[] src, final int srcElemPos, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_FLOAT != elementSize ) { throw new UnsupportedOperationException("'float' type byte-size "+Buffers.SIZEOF_FLOAT+" != elementSize "+elementSize+", "+this); }
        if (0 > destElemPos || destElemPos + elemCount > limit() || 0 > elemCount ||
            0 > srcElemPos || srcElemPos + elemCount > src.length ) {
            throw new IndexOutOfBoundsException("srcElemPos "+srcElemPos+", destElemPos "+destElemPos+", elemCount "+elemCount+
                                                ", destLimit "+limit()+", srcLimit "+src.length+", "+this);
        }
        final FloatBuffer dest = getByteBuffer().asFloatBuffer();
        dest.position( destElemPos ).limit( destElemPos + elemCount ); // remaining = elemCount
        dest.put(src, srcElemPos, elemCount);
        return this;
    }
    /** Absolute put method. Put long-elements for `elemCount` elements from `src` at `srcElemPos` into this buffer at the given element-index `destElemPos` */
    public final ElementBuffer put(final long[] src, final int srcElemPos, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_LONG != elementSize ) { throw new UnsupportedOperationException("'long' type byte-size "+Buffers.SIZEOF_LONG+" != elementSize "+elementSize+", "+this); }
        if (0 > destElemPos || destElemPos + elemCount > limit() || 0 > elemCount ||
            0 > srcElemPos || srcElemPos + elemCount > src.length ) {
            throw new IndexOutOfBoundsException("srcElemPos "+srcElemPos+", destElemPos "+destElemPos+", elemCount "+elemCount+
                                                ", destLimit "+limit()+", srcLimit "+src.length+", "+this);
        }
        final LongBuffer dest = getByteBuffer().asLongBuffer();
        dest.position( destElemPos ).limit( destElemPos + elemCount ); // remaining = elemCount
        dest.put(src, srcElemPos, elemCount);
        return this;
    }
    /** Absolute put method. Put double-elements for `elemCount` elements from `src` at `srcElemPos` into this buffer at the given element-index `destElemPos` */
    public final ElementBuffer put(final double[] src, final int srcElemPos, final int destElemPos, final int elemCount) {
        if( Buffers.SIZEOF_DOUBLE != elementSize ) { throw new UnsupportedOperationException("'double' type byte-size "+Buffers.SIZEOF_DOUBLE+" != elementSize "+elementSize+", "+this); }
        if (0 > destElemPos || destElemPos + elemCount > limit() || 0 > elemCount ||
            0 > srcElemPos || srcElemPos + elemCount > src.length ) {
            throw new IndexOutOfBoundsException("srcElemPos "+srcElemPos+", destElemPos "+destElemPos+", elemCount "+elemCount+
                                                ", destLimit "+limit()+", srcLimit "+src.length+", "+this);
        }
        final DoubleBuffer dest = getByteBuffer().asDoubleBuffer();
        dest.position( destElemPos ).limit( destElemPos + elemCount ); // remaining = elemCount
        dest.put(src, srcElemPos, elemCount);
        return this;
    }

    @Override
    public String toString() {
        return "ElementBuffer"+toSubString();
    }
}
