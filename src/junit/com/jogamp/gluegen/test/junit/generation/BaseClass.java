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
 
package com.jogamp.gluegen.test.junit.generation;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.nio.Int64Buffer;
import com.jogamp.common.os.Platform;
import java.nio.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.System.*;
import static com.jogamp.gluegen.test.junit.generation.BuildEnvironment.*;

/**
 * @author Michael Bien
 * @author Sven Gothel
 */
public class BaseClass {

    /**
     * Verifies the existence and creation of the generated class.
     */
    public void testClassExist(String name) throws Exception {
        String ifName = "com.jogamp.gluegen.test.junit.generation.Binding"+name;
        String implName = "com.jogamp.gluegen.test.junit.generation.impl.Binding"+name+"Impl";

        Class<?> clazzIf   = Class.forName(ifName);
        Class<?> clazzImpl = Class.forName(implName);

        Assert.assertNotNull(ifName+" does not exist", clazzIf);
        Assert.assertNotNull(implName+" does not exist", clazzImpl);

        Assert.assertEquals((int)1,  clazzIf.getDeclaredField("CONSTANT_ONE").get(null));

        Object obj = clazzImpl.newInstance();
        Assert.assertTrue("Not of type "+ifName, clazzIf.isAssignableFrom(obj.getClass()));
        Assert.assertTrue("Not of type com.jogamp.gluegen.test.junit.generation.Bindingtest1", (obj instanceof com.jogamp.gluegen.test.junit.generation.Bindingtest1));
    }

    /**
     * Verifies if all generated method signatures are completed,
     * ie a compilation only coverage test without functional tests.
     */
    public void chapter__TestCoverageSignature(Bindingtest1 binding) throws Exception {
          int i;
          long result;
          long context = 0;
          ByteBuffer bb=null;
          Int64Buffer lb=null;
          PointerBuffer pb=null;
          IntBuffer ib=null;
          long[] larray = null;
          int larray_offset = 0;
          String str=null;
          String[] strings = null;
          int[] iarray = null;
          int iarray_offset = 0;
          long l = 0;

          result = binding.arrayTestInt32(context, ib);
          result = binding.arrayTestInt32(context, iarray, iarray_offset);

          result = binding.arrayTestInt64(context, lb);
          result = binding.arrayTestInt64(context, larray, larray_offset);

          result = binding.arrayTestFoo1(context, lb);
          result = binding.arrayTestFoo1(context, larray, larray_offset);
          result = binding.arrayTestFooNioOnly(context, lb);

          lb = binding.arrayTestFoo2(lb);
          lb = binding.arrayTestFoo2(larray, larray_offset);

          pb = binding.arrayTestFoo3ArrayToPtrPtr(lb);
          pb = binding.arrayTestFoo3PtrPtr(pb);

          result = binding.bufferTest(bb);
          result = binding.bufferTestNioOnly(bb);

          result = binding.doubleTest(context, bb, lb, bb, lb);
          result = binding.doubleTest(context, bb, larray, larray_offset, bb, larray, larray_offset);
          result = binding.doubleTestNioOnly(context, bb, lb, bb, lb);

          result = binding.mixedTest(context, bb, lb);
          result = binding.mixedTest(context, bb, larray, larray_offset);
          result = binding.mixedTestNioOnly(context, bb, lb);

          result = binding.nopTest();

          i = binding.strToInt(str);
          str = binding.intToStr(i);

          i = binding.stringArrayRead(strings, i);

          i = binding.intArrayRead(ib, i);
          i = binding.intArrayRead(iarray, iarray_offset, i);

          long cfg=0;
          cfg = binding.typeTestAnonSingle(cfg);
          pb = binding.typeTestAnonPointer(pb);

          i = binding.typeTestInt32T(i, i);
          i = binding.typeTestUInt32T(i, i);
          l = binding.typeTestInt64T(l, l);
          l = binding.typeTestUInt64T(l, l);

          i = binding.typeTestWCharT(i, i);
          l = binding.typeTestSizeT(l, l);
          l = binding.typeTestPtrDiffT(l, l);
          l = binding.typeTestIntPtrT(l, l);
          l = binding.typeTestUIntPtrT(l, l);
    }

    /**
     * Verifies if all methods / signatures are properly generated,
     * can be invoked and functions.
     * This is a compilation (coverage) and runtime time (semantic) test.
     * This covers indirect primitive arrays and direct NIO buffers.
     */
    public void chapter03TestCoverageFunctionalityDirectNIOAndPrimitiveArray(Bindingtest1 binding) throws Exception {
          int i;
          long result;

          long context = 1;
          Int64Buffer lb = Int64Buffer.allocateDirect(1);
          lb.put(0,  10);

          ByteBuffer bb2 = Buffers.newDirectByteBuffer(Buffers.SIZEOF_LONG);
          Int64Buffer bb2L = Int64Buffer.wrap(bb2);
          bb2L.put(0, 100);

          IntBuffer ib1 = Buffers.newDirectByteBuffer(Buffers.SIZEOF_INT * Bindingtest1.ARRAY_SIZE).asIntBuffer();
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            ib1.put(i,  1000);
          }

          Int64Buffer lb1 = Int64Buffer.allocateDirect(Bindingtest1.ARRAY_SIZE);
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            lb1.put(i,  1000);
          }
          Int64Buffer lb2 = Int64Buffer.allocateDirect(Bindingtest1.ARRAY_SIZE);
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            lb2.put(i, 10000);
          }

          int[] iarray1 = new int[Bindingtest1.ARRAY_SIZE];
          int iarray1_offset = 0;
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            iarray1[i]=  1000;
          }

          long[] larray1 = new long[Bindingtest1.ARRAY_SIZE];
          int larray1_offset = 0;
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            larray1[i]=  1000;
          }

          long[] larray2 = new long[Bindingtest1.ARRAY_SIZE];
          int larray2_offset = 0;
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            larray2[i]= 10000;
          }

          result = binding.arrayTestInt32(context, ib1);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          result = binding.arrayTestInt32(context, iarray1, iarray1_offset);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          result = binding.arrayTestInt64(context, lb1);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          result = binding.arrayTestInt64(context, larray1, larray1_offset);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          result = binding.arrayTestFoo1(context, lb1);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          result = binding.arrayTestFoo1(context, larray1, larray1_offset);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          result = binding.arrayTestFooNioOnly(context, lb1);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          // Int64Buffer arrayTestFoo2 ( Int64Buffer )
          {
              lb2.rewind();
              Int64Buffer lb3 = Int64Buffer.allocateDirect(Bindingtest1.ARRAY_SIZE);
              lb3.put(lb2);
              lb3.rewind();
              lb2.rewind();

              // System.out.println("lb3: "+lb3);
              Assert.assertTrue("Wrong result: "+lb3.capacity(), Bindingtest1.ARRAY_SIZE == lb3.capacity());
              Assert.assertTrue("Wrong result: "+lb3.remaining(), Bindingtest1.ARRAY_SIZE == lb3.remaining());

              Int64Buffer lbR = binding.arrayTestFoo2(lb3);
              // System.out.println("lbR: "+lbR);

              Assert.assertNotNull(lbR);
              Assert.assertTrue("Wrong result: "+lb3.capacity(), Bindingtest1.ARRAY_SIZE == lb3.capacity());
              Assert.assertTrue("Wrong result: "+lb3.remaining(), Bindingtest1.ARRAY_SIZE == lb3.remaining());
              Assert.assertTrue("Wrong result: "+lbR.capacity(), Bindingtest1.ARRAY_SIZE == lbR.capacity());
              Assert.assertTrue("Wrong result: "+lbR.remaining(), Bindingtest1.ARRAY_SIZE == lbR.remaining());
              int j=0;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                Assert.assertTrue("Wrong result: s:"+lb3.get(j)+" d: "+lbR.get(j), 1+lb3.get(j)==lbR.get(j));
              }
          }

          // Int64Buffer arrayTestFoo2 ( long[], int )
          {
              long[] larray3 = new long[Bindingtest1.ARRAY_SIZE];
              for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                larray3[i]=  larray2[i];
              }

              Int64Buffer lbR = binding.arrayTestFoo2(larray3, 0);

              Assert.assertNotNull(lbR);
              Assert.assertTrue("Wrong result: "+lbR.capacity(), Bindingtest1.ARRAY_SIZE == lbR.capacity());
              Assert.assertTrue("Wrong result: "+lbR.remaining(), Bindingtest1.ARRAY_SIZE == lbR.remaining());
              int j=0;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                Assert.assertTrue("Wrong result: s:"+larray3[j]+" d: "+lbR.get(j), 1+larray3[j]==lbR.get(j));
              }
          }

          // PointerBuffer arrayTestFoo3ArrayToPtrPtr(Int64Buffer)
          // PointerBuffer arrayTestFoo3PtrPtr(PointerBuffer)
          {
              lb2.rewind();
              Int64Buffer lb3 = Int64Buffer.allocateDirect(Bindingtest1.ARRAY_SIZE*Bindingtest1.ARRAY_SIZE);
              int j;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  lb3.put(lb2);
                  lb2.rewind();
              }
              lb3.rewind();

              // System.out.println("lb3: "+lb3);
              Assert.assertTrue("Wrong result: "+lb3.capacity(), Bindingtest1.ARRAY_SIZE*Bindingtest1.ARRAY_SIZE == lb3.capacity());
              Assert.assertTrue("Wrong result: "+lb3.remaining(), Bindingtest1.ARRAY_SIZE*Bindingtest1.ARRAY_SIZE == lb3.remaining());

              PointerBuffer pb = binding.arrayTestFoo3ArrayToPtrPtr(lb3);
              // System.out.println("pb: "+pb);
              Assert.assertTrue("Wrong result: "+pb.capacity(), Bindingtest1.ARRAY_SIZE == pb.capacity());
              Assert.assertTrue("Wrong result: "+pb.remaining(), Bindingtest1.ARRAY_SIZE == pb.remaining());

              PointerBuffer pb2 = binding.arrayTestFoo3PtrPtr(pb);

              Assert.assertNotNull(pb2);
              Assert.assertTrue("Wrong result: "+pb2.capacity(), Bindingtest1.ARRAY_SIZE == pb2.capacity());
              Assert.assertTrue("Wrong result: "+pb2.remaining(), Bindingtest1.ARRAY_SIZE == pb2.remaining());
              for(j=0; j<Bindingtest1.ARRAY_SIZE*Bindingtest1.ARRAY_SIZE; j++) {
                Assert.assertTrue("Wrong result: s:"+lb2.get(j%Bindingtest1.ARRAY_SIZE)+" d: "+lb3.get(j), 
                                  1+lb2.get(j%Bindingtest1.ARRAY_SIZE)==lb3.get(j));
              }
          }

          // PointerBuffer.referenceBuffer(Int64Buffer.getBuffer)
          //  " "
          // PointerBuffer arrayTestFoo3PtrPtr(PointerBuffer)
          {
              PointerBuffer pb = PointerBuffer.allocateDirect(Bindingtest1.ARRAY_SIZE);
              int j;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  Int64Buffer lb3 = Int64Buffer.allocateDirect(Bindingtest1.ARRAY_SIZE);
                  lb3.put(lb2);
                  lb2.rewind();
                  lb3.rewind();

                  pb.referenceBuffer(lb3.getBuffer());
              }
              pb.rewind();

              // System.out.println("lb3: "+lb3);
              Assert.assertTrue("Wrong result: "+pb.capacity(), Bindingtest1.ARRAY_SIZE == pb.capacity());
              Assert.assertTrue("Wrong result: "+pb.remaining(), Bindingtest1.ARRAY_SIZE == pb.remaining());
              Assert.assertNotNull(pb.getReferencedBuffer(0));
              Assert.assertTrue("Wrong result: "+pb.getReferencedBuffer(0)+" != "+lb2.getBuffer(), pb.getReferencedBuffer(0).equals(lb2.getBuffer()));

              PointerBuffer pb2 = binding.arrayTestFoo3PtrPtr(pb);

              Assert.assertNotNull(pb2);
              Assert.assertTrue("Wrong result: "+pb2.capacity(), Bindingtest1.ARRAY_SIZE == pb2.capacity());
              Assert.assertTrue("Wrong result: "+pb2.remaining(), Bindingtest1.ARRAY_SIZE == pb2.remaining());
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  ByteBuffer bb = (ByteBuffer) pb.getReferencedBuffer(j);
                  Int64Buffer i64b = Int64Buffer.wrap(bb);
                  for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                    Assert.assertTrue("Wrong result: ["+j+"]["+i+"] s:"+lb2.get(i)+" d: "+i64b.get(i), 1+lb2.get(i)==i64b.get(i));
                  }
              }
          }

          result = binding.bufferTest(lb.getBuffer());
          Assert.assertTrue("Wrong result: "+result, 10==result);

          result = binding.bufferTestNioOnly(lb.getBuffer());
          Assert.assertTrue("Wrong result: "+result, 10==result);

          result = binding.doubleTest(context, lb.getBuffer(), lb1, bb2, lb2);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000+100+80000==result);

          result = binding.doubleTest(context, lb.getBuffer(), larray1, larray1_offset, bb2, larray2, larray2_offset);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000+100+80000==result);

          result = binding.doubleTestNioOnly(context, lb.getBuffer(), lb1, bb2, lb2);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000+100+80000==result);

          result = binding.mixedTest(context, lb.getBuffer(), lb1);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000==result);

          result = binding.mixedTest(context, lb.getBuffer(), larray1, larray1_offset);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000==result);

          result = binding.mixedTestNioOnly(context, lb.getBuffer(), lb1);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000==result);

          result = binding.nopTest();
          Assert.assertTrue("Wrong result: "+result, 42==result);

          i = binding.strToInt("42");
          Assert.assertTrue("Wrong result: "+i, 42==i);

          String str = binding.intToStr(42);
          Assert.assertTrue("Wrong result: "+str, str.equals("42"));

          i = binding.stringArrayRead(new String[] { "1234", "5678", "9a" }, 3);
          Assert.assertTrue("Wrong result: "+i, 10==i);

          ByteBuffer bb3 = Buffers.newDirectByteBuffer(Buffers.SIZEOF_INT * 3);
          IntBuffer ib = bb3.asIntBuffer();
          ib.put(0, 1);
          ib.put(1, 2);
          ib.put(2, 3);

          int[] iarray = new int[] { 1, 2, 3 };
          int iarray_offset = 0;

          i = binding.intArrayRead(ib, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);

          i = binding.intArrayRead(iarray, 0, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);

          {
              long cfg_base = 0xAABBCCDD11223344L;
              
              PointerBuffer pb = PointerBuffer.allocateDirect(Bindingtest1.ARRAY_SIZE);
              for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                long cfg_native;
                if(Platform.is32Bit()) {
                    cfg_native = (cfg_base+i) & 0x00000000FFFFFFFFL; // umask 1st 32bit
                } else {
                    cfg_native = (cfg_base+i);
                }
                long cfg = binding.typeTestAnonSingle(cfg_base + i);
                Assert.assertTrue("Wrong result: 0x"+Long.toHexString(cfg_native)+"+1 != 0x"+Long.toHexString(cfg), (cfg_native+1)==cfg);
                pb.put(i, cfg_base+i);

                long t = pb.get(i);
                Assert.assertTrue("Wrong result: 0x"+Long.toHexString(cfg_native)+" != 0x"+Long.toHexString(t), cfg_native==t);
              }
              pb.rewind();
              PointerBuffer pb2 = binding.typeTestAnonPointer(pb);
              Assert.assertTrue("Wrong result: "+pb2.capacity(), Bindingtest1.ARRAY_SIZE == pb2.capacity());
              Assert.assertTrue("Wrong result: "+pb2.remaining(), Bindingtest1.ARRAY_SIZE == pb2.remaining());
              for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                  Assert.assertTrue("Wrong result: 0x"+Long.toHexString(pb.get(i))+"+1 != 0x"+Long.toHexString(pb2.get(i)), (pb.get(i)+1)==pb2.get(i));
              }
          }
    }

    /**
     * This covers indirect primitive arrays and indirect NIO buffers.
     */
    public void chapter04TestSomeFunctionsAllIndirect(Bindingtest1 binding) throws Exception {
          int i;
          long result;

          IntBuffer ib = IntBuffer.allocate(3);
          ib.put(0, 1);
          ib.put(1, 2);
          ib.put(2, 3);

          int[] iarray = new int[] { 1, 2, 3 };
          int iarray_offset = 0;

          i = binding.intArrayRead(ib, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);

          i = binding.intArrayRead(iarray, 0, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);
    }

}
