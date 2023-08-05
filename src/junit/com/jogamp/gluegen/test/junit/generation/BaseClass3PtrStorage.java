/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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

package com.jogamp.gluegen.test.junit.generation;

import java.nio.IntBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.ElementBuffer;

import org.junit.Assert;

/**
 * Test {@link Bindingtest2} with {@link T2_PointerStorage} instance and pointer pointer..
 */
public class BaseTest3PtrStorage extends BaseClass {

    /**
     * Test {@link Bindingtest2} with {@link T2_PointerStorage} instance and pointer pointer
     */
    public void chapter01(final Bindingtest2 bt2) throws Exception {
        Assert.assertEquals(false, T2_PointerStorage.usesNativeCode());

        final T2_PointerStorage store = bt2.createT2PointerStorage();
        // final T2_PointerStorage store = T2_PointerStorage.create();
        final long[] int32PtrArray = store.getInt32PtrArray(0, new long[10], 0, 10); // 0, 1, 2, 3, 4, 5, 6, 7, 8, 9
        {
            Assert.assertEquals(10, int32PtrArray.length);
            System.err.print("int32PtrArray[10] = { ");
            for(int i=0; i<int32PtrArray.length; ++i) {
                Assert.assertNotEquals(0, int32PtrArray[i]);
                final ElementBuffer eb = ElementBuffer.derefPointer(Buffers.SIZEOF_INT, int32PtrArray[i], 1);
                final IntBuffer ib = eb.getByteBuffer().asIntBuffer();
                Assert.assertEquals(1, ib.limit());
                final int value = ib.get(0);
                Assert.assertEquals(i, value);
                System.err.print(value+", ");
            }
            System.err.println("}");
        }
        Assert.assertEquals(0, store.getInt32PtrPtrElemCount());
        store.setInt32PtrPtr(false, int32PtrArray, 3, 0,  7); // -> 3, 4, 5, 6, 7, 8, 9
        store.setInt32PtrPtr(true,  int32PtrArray, 8, 3,  2); // -> 3, 4, 5, 8, 9, 8, 9
        store.setInt32PtrPtr(true,  int32PtrArray, 0, 5,  2); // -> 3, 4, 5, 8, 9, 0, 1
        final long[] int32PtrPtr = store.getInt32PtrPtr(0, new long[7], 0, 7); // 3, 4, 5, 8, 9, 0, 1
        {
            System.err.print("int32PtrPtr[7] = { ");
            for(int i=0; i<int32PtrPtr.length; ++i) {
                Assert.assertNotEquals(0, int32PtrPtr[i]);
                final ElementBuffer eb = ElementBuffer.derefPointer(Buffers.SIZEOF_INT, int32PtrPtr[i], 1);
                final IntBuffer ib = eb.getByteBuffer().asIntBuffer();
                Assert.assertEquals(1, ib.limit());
                final int value = ib.get(0);
                final int exp;
                switch( i ) {
                    case 0: exp = 3; break;
                    case 1: exp = 4; break;
                    case 2: exp = 5; break;
                    case 3: exp = 8; break;
                    case 4: exp = 9; break;
                    case 5: exp = 0; break;
                    case 6: exp = 1; break;
                    default: exp = 99;
                }
                Assert.assertEquals(exp, value);
                System.err.print(value+", ");
            }
            System.err.println("}");
        }
        bt2.destroyT2PointerStorage(store);
    }

}
