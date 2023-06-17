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
    public static ElementBuffer allocate(final int elementSize, final int elementCount) {
        return new ElementBuffer(elementSize, ByteBuffer.allocate(elementCount * elementSize));
    }

    /** Returns a direct PointerBuffer in native order, w/o backup array */
    public static ElementBuffer allocateDirect(final int elementSize, final int elementCount) {
        return new ElementBuffer(elementSize, Buffers.newDirectByteBuffer(elementCount * elementSize));
    }

    public static ElementBuffer wrap(final int elementSize, final ByteBuffer src) {
        return new ElementBuffer(elementSize, src);
    }
    public static ElementBuffer wrap(final int elementSize, final int elementCount, final ByteBuffer src, final int srcByteOffset) {
        final int oldPos = src.position();
        final int oldLimit = src.limit();
        src.position(srcByteOffset);
        src.limit(srcByteOffset + ( elementSize * elementCount ));
        final ElementBuffer r = new ElementBuffer(elementSize, src.slice().order(src.order())); // slice and duplicate may change byte order
        src.position(oldPos);
        src.limit(oldLimit);
        return r;
    }
    public static ElementBuffer derefPointer(final int elementSize, final int elementCount, final long aptr) {
        if( 0 == aptr ) {
            throw new NullPointerException("aptr is null");
        }
        final ByteBuffer bb = Buffers.getDirectByteBuffer(aptr, elementCount * elementSize);
        if( null == bb ) {
            throw new InternalError("Couldn't dereference aptr 0x"+Long.toHexString(aptr)+", size "+elementCount+" * "+elementSize);
        }
        return new ElementBuffer(elementSize, bb);
    }
    public static ElementBuffer derefPointer(final int elementSize, final int elementCount, final ByteBuffer ptrSrc, final int ptrSrcByteOffset) {
        return derefPointer(elementSize, elementCount, PointerBuffer.wrap(ptrSrc, ptrSrcByteOffset, 1).get(0));
    }

    @Override
    public final ElementBuffer put(final ElementBuffer src) {
        if (remaining() < src.remaining()) {
            throw new IndexOutOfBoundsException("remaining[this "+remaining()+" < src "+src.remaining()+"], this "+this+", src "+src);
        }
        if( this.elementSize() != src.elementSize() ) {
            throw new IllegalArgumentException("Element-Size mismatch source "+src+", dest "+this);
        }
        final ByteBuffer tmp = ByteBuffer.allocate(elementSize);
        while (src.hasRemaining()) {
            put( src.get(tmp) );
        }
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
        if (0 > offset || offset + length > capacity) {
            throw new IndexOutOfBoundsException("idx "+offset+" + elementCount "+length+" not within [0.."+capacity+"), "+this);
        }
        final ByteBuffer src = getByteBuffer();
        final int oldPos = src.position();
        final int oldLimit = src.limit();
        src.position( elementSize * offset ).limit(elementSize * (offset + length));
        final ByteBuffer ref = src.slice().order(src.order()); // slice and duplicate may change byte order
        src.position( oldPos ).limit( oldLimit );
        return ref;
    }

    /** Absolute get method. Get element-bytes for `elementCount` elements from this buffer at `srcElemPos` into `destElemBytes` at the given element-index `destElemPos` */
    public final ByteBuffer get(final int srcElemPos, final ByteBuffer destElemBytes, final int destElemPos, final int elementCount) {
        if (0 > srcElemPos || srcElemPos + elementCount > capacity || 0 > elementCount ||
            0 > destElemPos || elementSize * ( destElemPos + elementCount ) > destElemBytes.limit() )
        {
            throw new IndexOutOfBoundsException("destElemPos "+destElemPos+", srcElemPos "+srcElemPos+", elementCount "+elementCount+
                                                ", srcCapacity "+capacity+", destLimit "+(destElemBytes.limit()/elementSize)+", "+this);
        }
        final ByteBuffer srcElemBytes = getByteBuffer();
        final int oldSrcLim = srcElemBytes.limit();
        srcElemBytes.position( srcElemPos * elementSize ).limit( ( srcElemPos + elementCount ) * elementSize ); // remaining = elementCount * elementSize
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
        final ByteBuffer r = get(position, destElemBytes);
        position++;
        return r;
    }
    /**
     * Relative bulk get method. Copy the element-bytes <code> [ position .. position+length [</code>
     * to the destination array <code> [ destElements[offset] .. destElements[offset+length] [ </code>
     * and increment the position by <code>length</code>. */
    public final ElementBuffer get(final ByteBuffer[] destElements, int offset, int length) {
        if (destElements.length<offset+length) {
            throw new IndexOutOfBoundsException("dest.length "+destElements.length+" < (offset "+offset+" + length "+length+")");
        }
        if (remaining() < length) {
            throw new IndexOutOfBoundsException("remaining "+remaining()+" < length "+length+", this "+this);
        }
        while(length>0) {
            get(position++, destElements[offset++]);
            length--;
        }
        return this;
    }

    /** Absolute put method. Put element-bytes for `elementCount` elements from `srcElemBytes` at `srcElemPos` into this buffer at the given element-index `destElemPos` */
    public final ElementBuffer put(final ByteBuffer srcElemBytes, final int srcElemPos, final int destElemPos, final int elementCount) {
        if (0 > destElemPos || destElemPos + elementCount > capacity || 0 > elementCount ||
            0 > srcElemPos || elementSize * ( srcElemPos + elementCount ) > srcElemBytes.limit() )
        {
            throw new IndexOutOfBoundsException("srcElemPos "+srcElemPos+", destElemPos "+destElemPos+", elementCount "+elementCount+
                                                ", destCapacity "+capacity+", srcLimit "+(srcElemBytes.limit()/elementSize)+", "+this);
        }
        final ByteBuffer destElemBytes = getByteBuffer();
        final int oldSrcPos = srcElemBytes.position();
        final int oldSrcLim = srcElemBytes.limit();
        srcElemBytes.position( srcElemPos * elementSize ).limit( ( srcElemPos + elementCount ) * elementSize ); // remaining = elementCount * elementSize
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

    @Override
    public String toString() {
        return "ElementBuffer"+toSubString();
    }
}
