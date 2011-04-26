/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */
package com.jogamp.common.nio;

import java.nio.*;

/**
 * Utility methods allowing easy {@link java.nio.Buffer} manipulations.
 * 
 * @author Kenneth Russel
 * @author Sven Gothel
 * @author Michael Bien
 */
public class Buffers {

    public static final int SIZEOF_BYTE     = 1;
    public static final int SIZEOF_SHORT    = 2;
    public static final int SIZEOF_CHAR     = 2;
    public static final int SIZEOF_INT      = 4;
    public static final int SIZEOF_FLOAT    = 4;
    public static final int SIZEOF_LONG     = 8;
    public static final int SIZEOF_DOUBLE   = 8;

    protected Buffers() {}

    /**
     * Allocates a new direct ByteBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static ByteBuffer newDirectByteBuffer(int numElements) {
        return nativeOrder(ByteBuffer.allocateDirect(numElements));
    }

    public static ByteBuffer newDirectByteBuffer(byte[] values, int offset, int lenght) {
        return (ByteBuffer)newDirectByteBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public static ByteBuffer newDirectByteBuffer(byte[] values, int offset) {
        return newDirectByteBuffer(values, offset, values.length-offset);
    }

    public static ByteBuffer newDirectByteBuffer(byte[] values) {
        return newDirectByteBuffer(values, 0);
    }

    /**
     * Allocates a new direct DoubleBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static DoubleBuffer newDirectDoubleBuffer(int numElements) {
        return newDirectByteBuffer(numElements * SIZEOF_DOUBLE).asDoubleBuffer();
    }

    public static DoubleBuffer newDirectDoubleBuffer(double[] values, int offset, int lenght) {
        return (DoubleBuffer)newDirectDoubleBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public static DoubleBuffer newDirectDoubleBuffer(double[] values, int offset) {
        return newDirectDoubleBuffer(values, offset, values.length - offset);
    }

    public static DoubleBuffer newDirectDoubleBuffer(double[] values) {
        return newDirectDoubleBuffer(values, 0);
    }

    /**
     * Allocates a new direct FloatBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static FloatBuffer newDirectFloatBuffer(int numElements) {
        return newDirectByteBuffer(numElements * SIZEOF_FLOAT).asFloatBuffer();
    }

    public static FloatBuffer newDirectFloatBuffer(float[] values, int offset, int lenght) {
        return (FloatBuffer)newDirectFloatBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public static FloatBuffer newDirectFloatBuffer(float[] values, int offset) {
        return newDirectFloatBuffer(values, offset, values.length - offset);
    }

    public static FloatBuffer newDirectFloatBuffer(float[] values) {
        return newDirectFloatBuffer(values, 0);
    }

    /**
     * Allocates a new direct IntBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static IntBuffer newDirectIntBuffer(int numElements) {
        return newDirectByteBuffer(numElements * SIZEOF_INT).asIntBuffer();
    }

    public static IntBuffer newDirectIntBuffer(int[] values, int offset, int lenght) {
        return (IntBuffer)newDirectIntBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public static IntBuffer newDirectIntBuffer(int[] values, int offset) {
        return newDirectIntBuffer(values, offset, values.length - offset);
    }

    public static IntBuffer newDirectIntBuffer(int[] values) {
        return newDirectIntBuffer(values, 0);
    }

    /**
     * Allocates a new direct LongBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static LongBuffer newDirectLongBuffer(int numElements) {
        return newDirectByteBuffer(numElements * SIZEOF_LONG).asLongBuffer();
    }

    public static LongBuffer newDirectLongBuffer(long[] values, int offset, int lenght) {
        return (LongBuffer)newDirectLongBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public static LongBuffer newDirectLongBuffer(long[] values, int offset) {
        return newDirectLongBuffer(values, offset, values.length - offset);
    }

    public static LongBuffer newDirectLongBuffer(long[] values) {
        return newDirectLongBuffer(values, 0);
    }

    /**
     * Allocates a new direct ShortBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static ShortBuffer newDirectShortBuffer(int numElements) {
        return newDirectByteBuffer(numElements * SIZEOF_SHORT).asShortBuffer();
    }

    public static ShortBuffer newDirectShortBuffer(short[] values, int offset, int lenght) {
        return (ShortBuffer)newDirectShortBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public static ShortBuffer newDirectShortBuffer(short[] values, int offset) {
        return newDirectShortBuffer(values, offset, values.length - offset);
    }

    public static ShortBuffer newDirectShortBuffer(short[] values) {
        return newDirectShortBuffer(values, 0);
    }

    /**
     * Allocates a new direct CharBuffer with the specified number of
     * elements. The returned buffer will have its byte order set to
     * the host platform's native byte order.
     */
    public static CharBuffer newDirectCharBuffer(int numElements) {
        return newDirectByteBuffer(numElements * SIZEOF_SHORT).asCharBuffer();
    }

    public static CharBuffer newDirectCharBuffer(char[] values, int offset, int lenght) {
        return (CharBuffer)newDirectCharBuffer(lenght).put(values, offset, lenght).rewind();
    }

    public static CharBuffer newDirectCharBuffer(char[] values, int offset) {
        return newDirectCharBuffer(values, offset, values.length - offset);
    }

    public static CharBuffer newDirectCharBuffer(char[] values) {
        return newDirectCharBuffer(values, 0);
    }

    /**
     * Calls slice on the specified buffer while maintaining the byteorder.
     * @see #slice(java.nio.Buffer, int, int) 
     */
    public static <B extends Buffer> B slice(B buffer) {
        if (buffer instanceof ByteBuffer) {
            final ByteBuffer bb = (ByteBuffer) buffer;
            return (B) bb.slice().order(bb.order()); // slice and duplicate may change byte order
        } else if (buffer instanceof IntBuffer) {
            return (B) ((IntBuffer) buffer).slice();
        } else if (buffer instanceof ShortBuffer) {
            return (B) ((ShortBuffer) buffer).slice();
        } else if (buffer instanceof FloatBuffer) {
            return (B) ((FloatBuffer) buffer).slice();
        } else if (buffer instanceof DoubleBuffer) {
            return (B) ((DoubleBuffer) buffer).slice();
        } else if (buffer instanceof LongBuffer) {
            return (B) ((LongBuffer) buffer).slice();
        } else if (buffer instanceof CharBuffer) {
            return (B) ((CharBuffer) buffer).slice();
        }
        throw new IllegalArgumentException("unexpected buffer type: " + buffer.getClass());
    }

    /**
     * Slices the specified buffer with offset as position and offset+size as limit
     * while maintaining the byteorder.
     * Concurrency warning: this method changes the buffers position and limit but
     * will restore it before return.
     */
    public static <B extends Buffer> B slice(B buffer, int offset, int size) {
        int pos = buffer.position();
        int limit = buffer.limit();

        B slice = null;
        try{
            buffer.position(offset).limit(offset+size);
            slice = slice(buffer);
        }finally{
            buffer.position(pos).limit(limit);
        }

        return slice;
    }

    /**
     * Helper routine to set a ByteBuffer to the native byte order, if
     * that operation is supported by the underlying NIO
     * implementation.
     */
    public static ByteBuffer nativeOrder(ByteBuffer buf) {
        return buf.order(ByteOrder.nativeOrder());
    }

    /**
     * Returns the size of a single element of this buffer in bytes.
     */
    public static int sizeOfBufferElem(Buffer buffer) {
        if (buffer == null) {
            return 0;
        }
        if (buffer instanceof ByteBuffer) {
            return SIZEOF_BYTE;
        } else if (buffer instanceof IntBuffer) {
            return SIZEOF_INT;
        } else if (buffer instanceof ShortBuffer) {
            return SIZEOF_SHORT;
        } else if (buffer instanceof FloatBuffer) {
            return SIZEOF_FLOAT;
        } else if (buffer instanceof DoubleBuffer) {
            return SIZEOF_DOUBLE;
        } else if (buffer instanceof LongBuffer) {
            return SIZEOF_LONG;
        } else if (buffer instanceof CharBuffer) {
            return SIZEOF_CHAR;
        }
        throw new RuntimeException("Unexpected buffer type " + buffer.getClass().getName());
    }

    /**
     * Helper routine to tell whether a buffer is direct or not. Null
     * pointers are considered NOT direct. isDirect() should really be
     * public in Buffer and not replicated in all subclasses.
     */
    public static boolean isDirect(Object buf) {
        if (buf == null) {
            return true;
        }
        if (buf instanceof Buffer) {
            return ((Buffer) buf).isDirect();
        } else if (buf instanceof PointerBuffer) {
            return ((PointerBuffer) buf).isDirect();
        }
        throw new IllegalArgumentException("Unexpected buffer type " + buf.getClass().getName());
    }

    /**
     * Helper routine to get the Buffer byte offset by taking into
     * account the Buffer position and the underlying type.  This is
     * the total offset for Direct Buffers.
     */
    public static int getDirectBufferByteOffset(Object buf) {
        if (buf == null) {
            return 0;
        }
        if (buf instanceof Buffer) {
            int pos = ((Buffer) buf).position();
            if (buf instanceof ByteBuffer) {
                return pos;
            } else if (buf instanceof FloatBuffer) {
                return pos * SIZEOF_FLOAT;
            } else if (buf instanceof IntBuffer) {
                return pos * SIZEOF_INT;
            } else if (buf instanceof ShortBuffer) {
                return pos * SIZEOF_SHORT;
            } else if (buf instanceof DoubleBuffer) {
                return pos * SIZEOF_DOUBLE;
            } else if (buf instanceof LongBuffer) {
                return pos * SIZEOF_LONG;
            } else if (buf instanceof CharBuffer) {
                return pos * SIZEOF_CHAR;
            }
        } else if (buf instanceof PointerBuffer) {
            PointerBuffer pointerBuffer = (PointerBuffer) buf;
            return pointerBuffer.position() * PointerBuffer.elementSize();
        }

        throw new IllegalArgumentException("Disallowed array backing store type in buffer " + buf.getClass().getName());
    }

    /**
     * Helper routine to return the array backing store reference from
     * a Buffer object.
     */
    public static Object getArray(Object buf) {
        if (buf == null) {
            return null;
        }
        if (buf instanceof Buffer) {
            return ((Buffer) buf).array();
        } else if (buf instanceof PointerBuffer) {
            return ((PointerBuffer) buf).array();
        }

        throw new IllegalArgumentException("Disallowed array backing store type in buffer " + buf.getClass().getName());
    }

    /**
     * Helper routine to get the full byte offset from the beginning of
     * the array that is the storage for the indirect Buffer
     * object.  The array offset also includes the position offset
     * within the buffer, in addition to any array offset.
     */
    public static int getIndirectBufferByteOffset(Object buf) {
        if (buf == null) {
            return 0;
        }
        if (buf instanceof Buffer) {
            int pos = ((Buffer) buf).position();
            if (buf instanceof ByteBuffer) {
                return (((ByteBuffer) buf).arrayOffset() + pos);
            } else if (buf instanceof FloatBuffer) {
                return (SIZEOF_FLOAT * (((FloatBuffer) buf).arrayOffset() + pos));
            } else if (buf instanceof IntBuffer) {
                return (SIZEOF_INT * (((IntBuffer) buf).arrayOffset() + pos));
            } else if (buf instanceof ShortBuffer) {
                return (SIZEOF_SHORT * (((ShortBuffer) buf).arrayOffset() + pos));
            } else if (buf instanceof DoubleBuffer) {
                return (SIZEOF_DOUBLE * (((DoubleBuffer) buf).arrayOffset() + pos));
            } else if (buf instanceof LongBuffer) {
                return (SIZEOF_LONG * (((LongBuffer) buf).arrayOffset() + pos));
            } else if (buf instanceof CharBuffer) {
                return (SIZEOF_CHAR * (((CharBuffer) buf).arrayOffset() + pos));
            }
        } else if (buf instanceof PointerBuffer) {
            PointerBuffer pointerBuffer = (PointerBuffer) buf;
            return PointerBuffer.elementSize() * (pointerBuffer.arrayOffset() + pointerBuffer.position());
        }

        throw new IllegalArgumentException("Unknown buffer type " + buf.getClass().getName());
    }


    //----------------------------------------------------------------------
    // Copy routines (type-to-type)
    //
    /**
     * Copies the <i>remaining</i> elements (as defined by
     * <code>limit() - position()</code>) in the passed ByteBuffer into
     * a newly-allocated direct ByteBuffer. The returned buffer will
     * have its byte order set to the host platform's native byte
     * order. The position of the newly-allocated buffer will be zero,
     * and the position of the passed buffer is unchanged (though its
     * mark is changed).
     */
    public static ByteBuffer copyByteBuffer(ByteBuffer orig) {
        ByteBuffer dest = newDirectByteBuffer(orig.remaining());
        dest.put(orig);
        dest.rewind();
        return dest;
    }

    /**
     * Copies the <i>remaining</i> elements (as defined by
     * <code>limit() - position()</code>) in the passed FloatBuffer
     * into a newly-allocated direct FloatBuffer. The returned buffer
     * will have its byte order set to the host platform's native byte
     * order. The position of the newly-allocated buffer will be zero,
     * and the position of the passed buffer is unchanged (though its
     * mark is changed).
     */
    public static FloatBuffer copyFloatBuffer(FloatBuffer orig) {
        return copyFloatBufferAsByteBuffer(orig).asFloatBuffer();
    }

    /**
     * Copies the <i>remaining</i> elements (as defined by
     * <code>limit() - position()</code>) in the passed IntBuffer
     * into a newly-allocated direct IntBuffer. The returned buffer
     * will have its byte order set to the host platform's native byte
     * order. The position of the newly-allocated buffer will be zero,
     * and the position of the passed buffer is unchanged (though its
     * mark is changed).
     */
    public static IntBuffer copyIntBuffer(IntBuffer orig) {
        return copyIntBufferAsByteBuffer(orig).asIntBuffer();
    }

    /**
     * Copies the <i>remaining</i> elements (as defined by
     * <code>limit() - position()</code>) in the passed ShortBuffer
     * into a newly-allocated direct ShortBuffer. The returned buffer
     * will have its byte order set to the host platform's native byte
     * order. The position of the newly-allocated buffer will be zero,
     * and the position of the passed buffer is unchanged (though its
     * mark is changed).
     */
    public static ShortBuffer copyShortBuffer(ShortBuffer orig) {
        return copyShortBufferAsByteBuffer(orig).asShortBuffer();
    }

    //----------------------------------------------------------------------
    // Copy routines (type-to-ByteBuffer)
    //
    /**
     * Copies the <i>remaining</i> elements (as defined by
     * <code>limit() - position()</code>) in the passed FloatBuffer
     * into a newly-allocated direct ByteBuffer. The returned buffer
     * will have its byte order set to the host platform's native byte
     * order. The position of the newly-allocated buffer will be zero,
     * and the position of the passed buffer is unchanged (though its
     * mark is changed).
     */
    public static ByteBuffer copyFloatBufferAsByteBuffer(FloatBuffer orig) {
        ByteBuffer dest = newDirectByteBuffer(orig.remaining() * SIZEOF_FLOAT);
        dest.asFloatBuffer().put(orig);
        dest.rewind();
        return dest;
    }

    /**
     * Copies the <i>remaining</i> elements (as defined by
     * <code>limit() - position()</code>) in the passed IntBuffer into
     * a newly-allocated direct ByteBuffer. The returned buffer will
     * have its byte order set to the host platform's native byte
     * order. The position of the newly-allocated buffer will be zero,
     * and the position of the passed buffer is unchanged (though its
     * mark is changed).
     */
    public static ByteBuffer copyIntBufferAsByteBuffer(IntBuffer orig) {
        ByteBuffer dest = newDirectByteBuffer(orig.remaining() * SIZEOF_INT);
        dest.asIntBuffer().put(orig);
        dest.rewind();
        return dest;
    }

    /**
     * Copies the <i>remaining</i> elements (as defined by
     * <code>limit() - position()</code>) in the passed ShortBuffer
     * into a newly-allocated direct ByteBuffer. The returned buffer
     * will have its byte order set to the host platform's native byte
     * order. The position of the newly-allocated buffer will be zero,
     * and the position of the passed buffer is unchanged (though its
     * mark is changed).
     */
    public static ByteBuffer copyShortBufferAsByteBuffer(ShortBuffer orig) {
        ByteBuffer dest = newDirectByteBuffer(orig.remaining() * SIZEOF_SHORT);
        dest.asShortBuffer().put(orig);
        dest.rewind();
        return dest;
    }

    //----------------------------------------------------------------------
    // Conversion routines
    //

    /**
     * @param source the source array
     * @param soffset the offset
     * @param dest the target array, if null, a new array is being created with size len.
     * @param doffset the offset in the dest array
     * @param len the payload of elements to be copied, if <code>len < 0</code> then <code>len = source.length - soffset</code>
     * @return the passed or newly created target array
     */
    public static float[] getFloatArray(double[] source, int soffset, float[] dest, int doffset, int len) {
        if(0>len) {
            len = source.length - soffset;
        }
        if(len > source.length - soffset) {
            throw new IllegalArgumentException("payload ("+len+") greater than remaining source bytes [len "+source.length+", offset "+soffset+"]");
        }
        if(null==dest) {
            dest = new float[len];
            doffset = 0;
        }
        if(len > dest.length - doffset) {
            throw new IllegalArgumentException("payload ("+len+") greater than remaining dest bytes [len "+dest.length+", offset "+doffset+"]");
        }
        for(int i=0; i<len; i++) {
            dest[doffset+i] = (float) source[soffset+i];
        }
        return dest;
    }

    /**
     * No rewind or repositioning is performed.
     * @param source the source buffer, which elements from it's current position and it's limit are being copied
     * @param dest the target buffer, if null, a new buffer is being created with size </code>source.remaining()</code>
     * @return the passed or newly created target buffer
     */
    public static FloatBuffer getFloatBuffer(DoubleBuffer source, FloatBuffer dest) {
        if(null == dest) {
            dest = newDirectFloatBuffer(source.remaining());
        }
        if( dest.remaining() < source.remaining() ) {
            throw new IllegalArgumentException("payload ("+source.remaining()+") is greater than remaining dest bytes: "+dest.remaining());
        }
        while (source.hasRemaining()) {
            dest.put((float) source.get());
        }
        return dest;
    }

    /**
     * @param source the source array
     * @param soffset the offset
     * @param dest the target array, if null, a new array is being created with size len.
     * @param doffset the offset in the dest array
     * @param len the payload of elements to be copied, if <code>len < 0</code> then <code>len = source.length - soffset</code>
     * @return the passed or newly created target array
     */
    public static double[] getDoubleArray(float[] source, int soffset, double[] dest, int doffset, int len) {
        if(0>len) {
            len = source.length - soffset;
        }
        if(len > source.length - soffset) {
            throw new IllegalArgumentException("payload ("+len+") greater than remaining source bytes [len "+source.length+", offset "+soffset+"]");
        }
        if(null==dest) {
            dest = new double[len];
            doffset = 0;
        }
        if(len > dest.length - doffset) {
            throw new IllegalArgumentException("payload ("+len+") greater than remaining dest bytes [len "+dest.length+", offset "+doffset+"]");
        }
        for(int i=0; i<len; i++) {
            dest[doffset+i] = (double) source[soffset+i];
        }
        return dest;
    }

    /**
     * No rewind or repositioning is performed.
     * @param source the source buffer, which elements from it's current position and it's limit are being copied
     * @param dest the target buffer, if null, a new buffer is being created with size </code>source.remaining()</code>
     * @return the passed or newly created target buffer
     */
    public static DoubleBuffer getDoubleBuffer(FloatBuffer source, DoubleBuffer dest) {
        if(null == dest) {
            dest = newDirectDoubleBuffer(source.remaining());
        }
        if( dest.remaining() < source.remaining() ) {
            throw new IllegalArgumentException("payload ("+source.remaining()+") is greater than remaining dest bytes: "+dest.remaining());
        }
        while (source.hasRemaining()) {
            dest.put((double) source.get());
        }
        return dest;
    }


    //----------------------------------------------------------------------
    // Convenient put methods with generic target Buffer
    //
    public static <B extends Buffer> B put(B dest, Buffer src) {
        if ((dest instanceof ByteBuffer) && (src instanceof ByteBuffer)) {
            return (B) ((ByteBuffer) dest).put((ByteBuffer) src);
        } else if ((dest instanceof ShortBuffer) && (src instanceof ShortBuffer)) {
            return (B) ((ShortBuffer) dest).put((ShortBuffer) src);
        } else if ((dest instanceof IntBuffer) && (src instanceof IntBuffer)) {
            return (B) ((IntBuffer) dest).put((IntBuffer) src);
        } else if ((dest instanceof FloatBuffer) && (src instanceof FloatBuffer)) {
            return (B) ((FloatBuffer) dest).put((FloatBuffer) src);
        } else if ((dest instanceof LongBuffer) && (src instanceof LongBuffer)) {
            return (B) ((LongBuffer) dest).put((LongBuffer) src);
        } else if ((dest instanceof DoubleBuffer) && (src instanceof DoubleBuffer)) {
            return (B) ((DoubleBuffer) dest).put((DoubleBuffer) src);
        } else if ((dest instanceof CharBuffer) && (src instanceof CharBuffer)) {
            return (B) ((CharBuffer) dest).put((CharBuffer) src);
        }
        throw new IllegalArgumentException("Incompatible Buffer classes: dest = " + dest.getClass().getName() + ", src = " + src.getClass().getName());
    }

    public static <B extends Buffer> B putb(B dest, byte v) {
        if (dest instanceof ByteBuffer) {
            return (B) ((ByteBuffer) dest).put(v);
        } else if (dest instanceof ShortBuffer) {
            return (B) ((ShortBuffer) dest).put((short) v);
        } else if (dest instanceof IntBuffer) {
            return (B) ((IntBuffer) dest).put((int) v);
        } else {
            throw new IllegalArgumentException("Byte doesn't match Buffer Class: " + dest);
        }
    }

    public static <B extends Buffer> B puts(B dest, short v) {
        if (dest instanceof ShortBuffer) {
            return (B) ((ShortBuffer) dest).put(v);
        } else if (dest instanceof IntBuffer) {
            return (B) ((IntBuffer) dest).put((int) v);
        } else {
            throw new IllegalArgumentException("Short doesn't match Buffer Class: " + dest);
        }
    }

    public static <B extends Buffer> B puti(B dest, int v) {
        if (dest instanceof IntBuffer) {
            return (B) ((IntBuffer) dest).put(v);
        } else {
            throw new IllegalArgumentException("Integer doesn't match Buffer Class: " + dest);
        }
    }

    public static <B extends Buffer> B putf(B dest, float v) {
        if (dest instanceof FloatBuffer) {
            return (B) ((FloatBuffer) dest).put(v);
/* TODO FixedPoint required
        } else if (dest instanceof IntBuffer) {
            return (B) ((IntBuffer) dest).put(FixedPoint.toFixed(v));
*/
        } else {
            throw new IllegalArgumentException("Float doesn't match Buffer Class: " + dest);
        }
    }
    public static <B extends Buffer> B putd(B dest, double v) {
        if (dest instanceof FloatBuffer) {
            return (B) ((FloatBuffer) dest).put((float) v);
        } else {
            throw new IllegalArgumentException("Double doesn't match Buffer Class: " + dest);
        }
    }

    public static void rangeCheck(byte[] array, int offset, int minElementsRemaining) {
        if (array == null) {
            return;
        }

        if (array.length < offset + minElementsRemaining) {
            throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
        }
    }

    public static void rangeCheck(char[] array, int offset, int minElementsRemaining) {
        if (array == null) {
            return;
        }

        if (array.length < offset + minElementsRemaining) {
            throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
        }
    }

    public static void rangeCheck(short[] array, int offset, int minElementsRemaining) {
        if (array == null) {
            return;
        }

        if (array.length < offset + minElementsRemaining) {
            throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
        }
    }

    public static void rangeCheck(int[] array, int offset, int minElementsRemaining) {
        if (array == null) {
            return;
        }

        if (array.length < offset + minElementsRemaining) {
            throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
        }
    }

    public static void rangeCheck(long[] array, int offset, int minElementsRemaining) {
        if (array == null) {
            return;
        }

        if (array.length < offset + minElementsRemaining) {
            throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
        }
    }

    public static void rangeCheck(float[] array, int offset, int minElementsRemaining) {
        if (array == null) {
            return;
        }

        if (array.length < offset + minElementsRemaining) {
            throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
        }
    }

    public static void rangeCheck(double[] array, int offset, int minElementsRemaining) {
        if (array == null) {
            return;
        }

        if (array.length < offset + minElementsRemaining) {
            throw new ArrayIndexOutOfBoundsException("Required " + minElementsRemaining + " elements in array, only had " + (array.length - offset));
        }
    }

    public static void rangeCheck(Buffer buffer, int minElementsRemaining) {
        if (buffer == null) {
            return;
        }

        if (buffer.remaining() < minElementsRemaining) {
            throw new IndexOutOfBoundsException("Required " + minElementsRemaining + " remaining elements in buffer, only had " + buffer.remaining());
        }
    }

    public static void rangeCheckBytes(Object buffer, int minBytesRemaining) {
        if (buffer == null) {
            return;
        }

        int bytesRemaining = 0;
        if (buffer instanceof Buffer) {
            int elementsRemaining = ((Buffer) buffer).remaining();
            if (buffer instanceof ByteBuffer) {
                bytesRemaining = elementsRemaining;
            } else if (buffer instanceof FloatBuffer) {
                bytesRemaining = elementsRemaining * SIZEOF_FLOAT;
            } else if (buffer instanceof IntBuffer) {
                bytesRemaining = elementsRemaining * SIZEOF_INT;
            } else if (buffer instanceof ShortBuffer) {
                bytesRemaining = elementsRemaining * SIZEOF_SHORT;
            }else if (buffer instanceof DoubleBuffer) {
                bytesRemaining = elementsRemaining * SIZEOF_DOUBLE;
            } else if (buffer instanceof LongBuffer) {
                bytesRemaining = elementsRemaining * SIZEOF_LONG;
            } else if (buffer instanceof CharBuffer) {
                bytesRemaining = elementsRemaining * SIZEOF_CHAR;
            }
        } else if (buffer instanceof PointerBuffer) {
            PointerBuffer pointerBuffer = (PointerBuffer) buffer;
            bytesRemaining = pointerBuffer.remaining() * PointerBuffer.elementSize();
        }
        if (bytesRemaining < minBytesRemaining) {
            throw new IndexOutOfBoundsException("Required " + minBytesRemaining + " remaining bytes in buffer, only had " + bytesRemaining);
        }
    }

}
