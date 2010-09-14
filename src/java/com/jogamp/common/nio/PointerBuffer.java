/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

/*
 * Created on Saturday, March 27 2010 11:55
 */
package com.jogamp.common.nio;

import com.jogamp.common.os.*;
import java.nio.ByteBuffer;
import java.nio.Buffer;
import java.util.HashMap;

/**
 * Hardware independent container for native pointer arrays.
 *
 * The native values (NIO direct ByteBuffer) might be 32bit or 64bit wide,
 * depending of the CPU pointer width.
 *
 * @author Michael Bien
 * @author Sven Gothel
 */
public abstract class PointerBuffer extends AbstractLongBuffer {

    protected HashMap/*<aptr, buffer>*/ dataMap = new HashMap();

    static {
        NativeLibrary.ensureNativeLibLoaded();
    }

    protected PointerBuffer(ByteBuffer bb) {
        super(bb, elementSize());
    }

    public static PointerBuffer allocate(int size) {
        if (Platform.isJavaSE()) {
            return new PointerBufferSE(ByteBuffer.wrap(new byte[elementSize() * size]));
        } else {
            return new PointerBufferME_CDC_FP(ByteBuffer.wrap(new byte[elementSize() * size]));
        }
    }

    public static PointerBuffer allocateDirect(int size) {
        if (Platform.isJavaSE()) {
            return new PointerBufferSE(Buffers.newDirectByteBuffer(elementSize() * size));
        } else {
            return new PointerBufferME_CDC_FP(Buffers.newDirectByteBuffer(elementSize() * size));
        }
    }

    public static PointerBuffer wrap(ByteBuffer src) {
        PointerBuffer res;
        if (Platform.isJavaSE()) {
            res = new PointerBufferSE(src);
        } else {
            res = new PointerBufferME_CDC_FP(src);
        }
        res.updateBackup();
        return res;

    }

    public static int elementSize() {
        return Platform.is32Bit() ? Buffers.SIZEOF_INT : Buffers.SIZEOF_LONG;
    }

    public final PointerBuffer put(PointerBuffer src) {
        if (remaining() < src.remaining()) {
            throw new IndexOutOfBoundsException();
        }
        long addr;
        while (src.hasRemaining()) {
             addr = src.get();
             put(addr);
             Long addrL = new Long(addr);
             Buffer bb = (Buffer) dataMap.get(addrL);
             if(null!=bb) {
                 dataMap.put(addrL, bb);
             } else {
                 dataMap.remove(addrL);
             }
        }
        return this;
    }

    /** Put the address of the given direct Buffer at the given position
        of this pointer array.
        Adding a reference of the given direct Buffer to this object. */
    public final PointerBuffer referenceBuffer(int index, Buffer bb) {
        if(null==bb) {
            throw new RuntimeException("Buffer is null");
        }
        if(!Buffers.isDirect(bb)) {
            throw new RuntimeException("Buffer is not direct");
        }
        long mask = Platform.is32Bit() ?  0x00000000FFFFFFFFL : 0xFFFFFFFFFFFFFFFFL ;
        long bbAddr = getDirectBufferAddressImpl(bb) & mask;
        if(0==bbAddr) {
            throw new RuntimeException("Couldn't determine native address of given Buffer: "+bb);
        }

        put(index, bbAddr);
        dataMap.put(new Long(bbAddr), bb);
        return this;
    }

    /** Put the address of the given direct Buffer at the end
        of this pointer array.
        Adding a reference of the given direct Buffer to this object. */
    public final PointerBuffer referenceBuffer(Buffer bb) {
        referenceBuffer(position, bb);
        position++;
        return this;
    }

    public final Buffer getReferencedBuffer(int index) {
        long addr = get(index);
        return (Buffer) dataMap.get(new Long(addr));
    }

    public final Buffer getReferencedBuffer() {
        Buffer bb = getReferencedBuffer(position);
        position++;
        return bb;
    }

    private native long getDirectBufferAddressImpl(Object directBuffer);

    public String toString() {
        return "PointerBuffer:"+super.toString();
    }

}
