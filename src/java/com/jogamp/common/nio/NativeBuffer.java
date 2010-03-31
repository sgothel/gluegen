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
/*public*/ interface NativeBuffer/*<B extends NativeBuffer>*/ { // make public as soon we support generics

    public boolean hasRemaining();

    public boolean isDirect();

    public int limit();

    public int position();

    public int remaining();

    public long[] array();

    public int capacity();
/*
    public B rewind();

    public B position(int newPos);

    public B put(int index, long value);

    public B put(long value);

    public B put(B src);
*/
    public long get();

    public long get(int idx);

    public ByteBuffer getBuffer();


}
