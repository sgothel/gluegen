/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.common.util;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio./*value2*/FloatBuffer/*value2*/;

/**
 * Simple primitive-type stack.
 * <p>
 * Implemented operations:
 * <ul>
 *  <li>FILO - First In, Last Out</li>
 * </ul>
 * </p>
 */
public class /*name*/FloatStack/*name*/ implements PrimitiveStack {
    private int position;
    private /*value*/float/*value*/[] buffer;
    private int growSize;

    /**
     * @param initialSize initial size, must be &gt; zero
     * @param growSize grow size if {@link #position()} is reached, maybe <code>0</code>
     *        in which case an {@link IndexOutOfBoundsException} is thrown.
     */
    public /*name*/FloatStack/*name*/(final int initialSize, final int growSize) {
        this.position = 0;
        this.growSize = growSize;
        this.buffer = new /*value*/float/*value*/[initialSize];
    }

    @Override
    public final int capacity() { return buffer.length; }

    @Override
    public final int position() { return position; }

    @Override
    public final void position(final int newPosition) throws IndexOutOfBoundsException {
        if( 0 > position || position >= buffer.length ) {
            throw new IndexOutOfBoundsException("Invalid new position "+newPosition+", "+this.toString());
        }
        position = newPosition;
    }

    @Override
    public final int remaining() { return buffer.length - position; }

    @Override
    public final int getGrowSize() { return growSize; }

    @Override
    public final void setGrowSize(final int newGrowSize) { growSize = newGrowSize; }

    @Override
    public final String toString() {
        return "FloatStack[0..(pos "+position+").."+buffer.length+", remaining "+remaining()+"]";
    }

    public final /*value*/float/*value*/[] buffer() { return buffer; }

    private final void growIfNecessary(final int length) throws IndexOutOfBoundsException {
        if( position + length > buffer.length ) {
            if( 0 >= growSize ) {
                throw new IndexOutOfBoundsException("Out of fixed stack size: "+this);
            }
            final /*value*/float/*value*/[] newBuffer =
                    new /*value*/float/*value*/[buffer.length + growSize];
            System.arraycopy(buffer, 0, newBuffer, 0, position);
            buffer = newBuffer;
        }
    }

    /**
     * FILO put operation
     *
     * @param src source buffer
     * @param srcOffset offset of src
     * @param length number of float elements to put from <code>src</code> on-top this stack
     * @return the src float[]
     * @throws IndexOutOfBoundsException if stack cannot grow due to zero grow-size or offset+length exceeds src.
     */
    public final /*value*/float/*value*/[]
                    putOnTop(final /*value*/float/*value*/[] src, final int srcOffset, final int length) throws IndexOutOfBoundsException {
        growIfNecessary(length);
        System.arraycopy(src, srcOffset, buffer, position, length);
        position += length;
        return src;
    }

    /**
     * FILO put operation
     *
     * @param src source buffer, it's position is incremented by <code>length</code>
     * @param length number of float elements to put from <code>src</code> on-top this stack
     * @return the src FloatBuffer
     * @throws IndexOutOfBoundsException if stack cannot grow due to zero grow-size
     * @throws BufferUnderflowException if <code>src</code> FloatBuffer has less remaining elements than <code>length</code>.
     */
    public final /*value2*/FloatBuffer/*value2*/
                        putOnTop(final /*value2*/FloatBuffer/*value2*/ src, final int length) throws IndexOutOfBoundsException, BufferUnderflowException  {
        growIfNecessary(length);
        src.get(buffer, position, length);
        position += length;
        return src;
    }

    /**
     * FILO get operation
     *
     * @param dest destination buffer
     * @param destOffset offset of dest
     * @param length number of float elements to get from-top this stack to <code>dest</code>.
     * @return the dest float[]
     * @throws IndexOutOfBoundsException if stack or <code>dest</code> has less elements than <code>length</code>.
     */
    public final /*value*/float/*value*/[]
                        getFromTop(final /*value*/float/*value*/[] dest, final int destOffset, final int length) throws IndexOutOfBoundsException {
        System.arraycopy(buffer, position-length, dest, destOffset, length);
        position -= length;
        return dest;
    }

    /**
     * FILO get operation
     *
     * @param dest destination buffer, it's position is incremented by <code>length</code>.
     * @param length number of float elements to get from-top this stack to <code>dest</code>.
     * @return the dest FloatBuffer
     * @throws IndexOutOfBoundsException if stack has less elements than length
     * @throws BufferOverflowException if <code>src</code> FloatBuffer has less remaining elements than <code>length</code>.
     */
    public final /*value2*/FloatBuffer/*value2*/
                        getFromTop(final /*value2*/FloatBuffer/*value2*/ dest, final int length) throws IndexOutOfBoundsException, BufferOverflowException  {
        dest.put(buffer, position-length, length);
        position -= length;
        return dest;
    }
}
