/*
 * Copyright (c) 2010, Michael Bien
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Michael Bien nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Michael Bien BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jogamp.common.nio;

import com.jogamp.common.os.*;
import java.nio.ByteBuffer;

/**
 * Hardware independent container for native int64_t arrays.
 *
 * The native values (NIO direct ByteBuffer) are always 64bit wide.
 *
 * @author Michael Bien
 * @author Sven Gothel
 */
public abstract class Int64Buffer extends AbstractLongBuffer {

    protected Int64Buffer(ByteBuffer bb) {
        super(bb, elementSize());
    }

    public static Int64Buffer allocate(int size) {
        if (Platform.isJavaSE()) {
            return new Int64BufferSE(ByteBuffer.wrap(new byte[elementSize() * size]));
        } else {
            return new Int64BufferME_CDC_FP(ByteBuffer.wrap(new byte[elementSize() * size]));
        }
    }

    public static Int64Buffer allocateDirect(int size) {
        if (Platform.isJavaSE()) {
            return new Int64BufferSE(Buffers.newDirectByteBuffer(elementSize() * size));
        } else {
            return new Int64BufferME_CDC_FP(Buffers.newDirectByteBuffer(elementSize() * size));
        }
    }

    public static Int64Buffer wrap(ByteBuffer src) {
        Int64Buffer res;
        if (Platform.isJavaSE()) {
            res = new Int64BufferSE(src);
        } else {
            res = new Int64BufferME_CDC_FP(src);
        }
        res.updateBackup();
        return res;

    }

    public static int elementSize() {
        return Buffers.SIZEOF_LONG;
    }

    public String toString() {
        return "Int64Buffer:"+super.toString();
    }

}
