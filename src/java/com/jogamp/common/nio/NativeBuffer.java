/*
 * Created on Tuesday, March 30 2010 18:22
 */
package com.jogamp.common.nio;

import java.nio.ByteBuffer;

/**
 * Hardware independent container for various kinds of buffers.
 *
 * @author Michael Bien
 * @author Sven Gothel
 */
public interface NativeBuffer/*<B extends NativeBuffer>*/ {

    public int limit();

    public int capacity();

    public int position();

    public NativeBuffer position(int newPos);

    public int remaining();

    public boolean hasRemaining();

    public NativeBuffer rewind();

    public boolean hasArray();

    public int arrayOffset();

    public ByteBuffer getBuffer();

    public boolean isDirect();

/*
    public long[] array();

    public B rewind();

    public B put(int index, long value);

    public B put(long value);

    public B put(B src);

    public long get();

    public long get(int idx);
*/

}
