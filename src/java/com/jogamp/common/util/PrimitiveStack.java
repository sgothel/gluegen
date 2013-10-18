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

/**
 * Simple primitive-type stack.
 */
public interface PrimitiveStack {

    /**
     * Returns this stack's current capacity.
     * <p>
     * The capacity may grow with a put operation w/ insufficient {@link #remaining()} elements left, if {@link #getGrowSize()} &gt; 0.
     * </p>
     */
    int capacity();

    /**
     * Returns the current position of this stack.
     * <p>
     * Position is in the range: 0 &le; position &lt; {@link #capacity()}.
     * </p>
     * <p>
     * The position equals to the number of elements stored.
     * </p>
     **/
    int position();

    /**
     * Sets the position of this stack.
     *
     * @param newPosition the new position
     * @throws IndexOutOfBoundsException if <code>newPosition</code> is outside of range: 0 &le; position &lt; {@link #capacity()}.
     */
    void position(int newPosition) throws IndexOutOfBoundsException;

    /**
     * Returns the remaining elements left before stack will grow about {@link #getGrowSize()}.
     * <pre>
     *   remaining := capacity() - position();
     * </pre>
     * <p>
     * 0 denotes a full stack.
     * </p>
     * @see #capacity()
     * @see #position()
     **/
    int remaining();

    /** Returns the grow size. A stack grows by this size in case a put operation exceeds it's {@link #capacity()}. */
    int getGrowSize();

    /** Set new {@link #growSize(). */
    void setGrowSize(int newGrowSize);
}
