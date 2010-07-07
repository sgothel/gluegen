/*
 * Created on Sunday, July 04 2010 20:00
 */
package com.jogamp.common.nio;

import java.nio.IntBuffer;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Michael Bien
 */
public class BuffersTest {

    @Test
    public void slice() {
        IntBuffer buffer = Buffers.newDirectIntBuffer(6);
        buffer.put(new int[]{1,2,3,4,5,6}).rewind();

        IntBuffer threefour = (IntBuffer)Buffers.slice(buffer, 2, 2);

        assertEquals(3, threefour.get(0));
        assertEquals(4, threefour.get(1));
        assertEquals(2, threefour.capacity());

    }

}
