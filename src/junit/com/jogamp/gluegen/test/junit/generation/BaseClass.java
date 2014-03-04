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
import com.jogamp.common.os.MachineDescription;
import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.JunitTracer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import jogamp.common.os.MachineDescriptionRuntime;

import org.junit.Assert;


/**
 * @author Michael Bien
 * @author Sven Gothel
 */
public class BaseClass extends JunitTracer {

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
          long context = 0;
          LongBuffer lb=null;
          IntBuffer ib=null;
          long[] larray = null;
          int larray_offset = 0;
          String str=null;
          String[] strings = null;
          int[] iarray = null;
          int iarray_offset = 0;
          long result = 0;
          long l = result;

          {              
              ByteBuffer bb = binding.createAPtrBlob();
              PointerBuffer pb = safeByteBuffer2PointerBuffer(bb, 1);
              long bb2A = binding.getAPtrAddress(bb);
              bb2A = bb2A - 0; // avoid warning
          
              binding.arrayTestAVoidPtrTypeDim1Mutable(pb);
              pb = PointerBuffer.wrap( binding.arrayTestAVoidPtrTypeDim1Immutable(pb) );
              pb = PointerBuffer.wrap( binding.arrayTestAVoidPtrTypeDim0(pb.getBuffer()) );
              binding.releaseAPtrBlob( binding.getAPtrMemory( pb.get(0) ) );
              
              binding.arrayTestAIntPtrTypeDim1Mutable(pb);
              result = binding.arrayTestAIntPtrTypeDim1Immutable(pb);
              result = binding.arrayTestAIntPtrTypeDim0(pb.get(0));
              binding.releaseAPtrBlob( binding.getAPtrMemory( pb.get(0) ) );
              
              binding.arrayTestAPtr1TypeDim1Mutable(pb);
              pb = PointerBuffer.wrap( binding.arrayTestAPtr1TypeDim1Immutable(pb) );
              pb = PointerBuffer.wrap( binding.arrayTestAPtr1TypeDim0(pb.getBuffer()) );
              binding.releaseAPtrBlob( binding.getAPtrMemory( pb.get(0) ) );
              
              binding.arrayTestAPtr2TypeDim1Mutable(pb);
              result = binding.arrayTestAPtr2TypeDim1Immutable(pb);
              result = binding.arrayTestAPtr2TypeDim0(pb.get(0));
              binding.releaseAPtrBlob( binding.getAPtrMemory( pb.get(0) ) );

              binding.releaseAPtrBlob(bb);
          }
          
          ByteBuffer bb=null;
          PointerBuffer pb=null;

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

          i = binding.binaryArrayRead(pb, pb, 0);

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

    ByteBuffer newByteBuffer(int size, boolean direct) {
        if(direct) {
            final ByteBuffer bb = Buffers.newDirectByteBuffer(size);
            Assert.assertTrue(bb.isDirect());
            return bb;
        } else {
            final ByteBuffer bb = ByteBuffer.wrap(new byte[size]);
            Assert.assertTrue(bb.hasArray());
            Assert.assertTrue(!bb.isDirect());
            bb.order(ByteOrder.nativeOrder());
            Assert.assertTrue(bb.hasArray());
            Assert.assertTrue(!bb.isDirect());
            return bb;
        }
    }
    
    IntBuffer newIntBuffer(int size, boolean direct) {
        if(direct) {
            final IntBuffer ib = Buffers.newDirectIntBuffer(size);
            Assert.assertTrue(ib.isDirect());
            return ib;
        } else {
            final IntBuffer ib = IntBuffer.wrap(new int[size]);
            Assert.assertTrue(ib.hasArray());
            Assert.assertTrue(!ib.isDirect());
            return ib;
        }
    }

    LongBuffer newLongBuffer(int size, boolean direct) {
        if(direct) {
            final LongBuffer lb = Buffers.newDirectLongBuffer(size);
            Assert.assertTrue(lb.isDirect());
            return lb;            
        } else {
            final LongBuffer lb = LongBuffer.wrap(new long[size]);
            Assert.assertTrue(!lb.isDirect());
            Assert.assertTrue(lb.hasArray());
            return lb;
        }        
    }

    PointerBuffer newPointerBuffer(int size, boolean direct) {
        if(direct) {
          final PointerBuffer pb = PointerBuffer.allocateDirect(size);
          Assert.assertTrue(pb.isDirect());
          Assert.assertTrue(pb.getBuffer().isDirect());
          return pb;
        } else {
          final PointerBuffer pb = PointerBuffer.allocate(size);
          Assert.assertTrue(pb.hasArray());
          Assert.assertTrue(!pb.isDirect());
          return pb;
        }
    }
    
    long cleanAddress(long a) {
        if (Platform.is32Bit()) {
            return a & 0x00000000FFFFFFFFL;
        } else {
            return a;
        }        
    }
    
    PointerBuffer validatePointerBuffer(PointerBuffer pb, int elements) {
        Assert.assertNotNull(pb);
        Assert.assertEquals("PointerBuffer capacity not "+elements, elements, pb.capacity());
        Assert.assertEquals("PointerBuffer remaining not "+elements, elements, pb.remaining());
        System.err.println("Testing accessing PointerBuffer values [0.."+(elements-1)+"]");
        for(int i=0; i<elements; i++) {
            final long v = pb.get(i);
            System.err.println("  "+i+"/"+elements+": 0x"+Long.toHexString(v));
        }
        return pb;
    }
    PointerBuffer safeByteBuffer2PointerBuffer(ByteBuffer bb, int elements) {
        Assert.assertEquals("ByteBuffer capacity not PointerBuffer ELEMENT_SIZE * "+elements, elements * PointerBuffer.ELEMENT_SIZE, bb.capacity());
        Assert.assertEquals("ByteBuffer remaining not PointerBuffer ELEMENT_SIZE * "+elements, elements * PointerBuffer.ELEMENT_SIZE, bb.remaining());
        return validatePointerBuffer(PointerBuffer.wrap(bb), elements);
    }
    
    /**
     * Verifies if all methods / signatures are properly generated,
     * can be invoked and functions.
     * This is a compilation (coverage) and runtime time (semantic) test.
     * This covers indirect primitive arrays and direct NIO buffers.
     */
    public void chapter03TestCoverageFunctionalityNIOAndPrimitiveArray(Bindingtest1 binding, boolean direct) throws Exception {
          int i;
          long result;

          final long context = 1;
          LongBuffer lb = newLongBuffer(1, direct);
          lb.put(0,  10);

          ByteBuffer bb2 = newByteBuffer(Buffers.SIZEOF_LONG, direct);
          LongBuffer bb2L = bb2.asLongBuffer();
          bb2L.put(0, 100);

          IntBuffer ib1 = newIntBuffer(Bindingtest1.ARRAY_SIZE, direct);
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            ib1.put(i,  1000);
          }

          LongBuffer lb1 = newLongBuffer(Bindingtest1.ARRAY_SIZE, direct);
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            lb1.put(i,  1000);
          }
          LongBuffer lb2 = newLongBuffer(Bindingtest1.ARRAY_SIZE, direct);
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

          // LongBuffer arrayTestFoo2 ( LongBuffer ) - don't write-back array-arg
          {
              lb2.rewind();
              LongBuffer lb3 = newLongBuffer(Bindingtest1.ARRAY_SIZE, direct);
              lb3.put(lb2);
              lb3.rewind();
              lb2.rewind();

              // System.out.println("lb3: "+lb3);
              Assert.assertTrue("Wrong result: "+lb3.capacity(), Bindingtest1.ARRAY_SIZE == lb3.capacity());
              Assert.assertTrue("Wrong result: "+lb3.remaining(), Bindingtest1.ARRAY_SIZE == lb3.remaining());

              LongBuffer lbR = binding.arrayTestFoo2(lb3);
              // System.out.println("lbR: "+lbR);

              Assert.assertNotNull(lbR);
              Assert.assertTrue("Wrong result: "+lb3.capacity(), Bindingtest1.ARRAY_SIZE == lb3.capacity());
              Assert.assertTrue("Wrong result: "+lb3.remaining(), Bindingtest1.ARRAY_SIZE == lb3.remaining());
              Assert.assertTrue("Wrong result: "+lbR.capacity(), Bindingtest1.ARRAY_SIZE == lbR.capacity());
              Assert.assertTrue("Wrong result: "+lbR.remaining(), Bindingtest1.ARRAY_SIZE == lbR.remaining());
              int j=0;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                Assert.assertTrue("Wrong result: s:"+lb2.get(j)+" c: "+lb3.get(j), lb2.get(j)==lb3.get(j));
                Assert.assertTrue("Wrong result: s:"+lb3.get(j)+" d: "+lbR.get(j), 1+lb3.get(j)==lbR.get(j));
              }
          }

          // LongBuffer arrayTestFoo2 ( long[], int ) - don't write-back array-arg
          {
              long[] larray3 = new long[Bindingtest1.ARRAY_SIZE];
              for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                larray3[i]=  larray2[i];
              }

              LongBuffer lbR = binding.arrayTestFoo2(larray3, 0);

              Assert.assertNotNull(lbR);
              Assert.assertTrue("Wrong result: "+lbR.capacity(), Bindingtest1.ARRAY_SIZE == lbR.capacity());
              Assert.assertTrue("Wrong result: "+lbR.remaining(), Bindingtest1.ARRAY_SIZE == lbR.remaining());
              int j=0;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                Assert.assertTrue("Wrong result: s:"+larray2[j]+" c: "+larray3[j], larray2[j]==larray3[j]);
                Assert.assertTrue("Wrong result: s:"+larray3[j]+" d: "+lbR.get(j), 1+larray3[j]==lbR.get(j));
              }
          }

          // void arrayTestFoo3 ( LongBuffer ) - write-back array-arg
          {
              lb2.rewind();
              LongBuffer lb3 = newLongBuffer(Bindingtest1.ARRAY_SIZE, direct);
              lb3.put(lb2);
              lb3.rewind();
              lb2.rewind();

              // System.out.println("lb3: "+lb3);
              Assert.assertTrue("Wrong result: "+lb3.capacity(), Bindingtest1.ARRAY_SIZE == lb3.capacity());
              Assert.assertTrue("Wrong result: "+lb3.remaining(), Bindingtest1.ARRAY_SIZE == lb3.remaining());

              binding.arrayTestFoo3(lb3);

              Assert.assertTrue("Wrong result: "+lb3.capacity(), Bindingtest1.ARRAY_SIZE == lb3.capacity());
              Assert.assertTrue("Wrong result: "+lb3.remaining(), Bindingtest1.ARRAY_SIZE == lb3.remaining());
              int j=0;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                Assert.assertTrue("Wrong result: s:"+lb2.get(j)+" d: "+lb3.get(j), 1+lb2.get(j)==lb3.get(j));
              }
          }

          // void arrayTestFoo3 ( long[], int ) - write-back array-arg
          {
              long[] larray3 = new long[Bindingtest1.ARRAY_SIZE];
              for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                larray3[i]=  larray2[i];
              }

              binding.arrayTestFoo3(larray3, 0);

              int j=0;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                Assert.assertTrue("Wrong result: s:"+larray2[j]+" d: "+larray3[j], 1+larray2[j]==larray3[j]);
              }
          }

          // PointerBuffer arrayTestFoo3ArrayToPtrPtr(LongBuffer)
          // PointerBuffer arrayTestFoo3PtrPtr(PointerBuffer)
          {
              lb2.rewind();
              LongBuffer lb3 = newLongBuffer(Bindingtest1.ARRAY_SIZE*Bindingtest1.ARRAY_SIZE, direct);
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
              validatePointerBuffer(pb, Bindingtest1.ARRAY_SIZE);

              PointerBuffer pb2 = binding.arrayTestFoo3PtrPtr(pb);
              validatePointerBuffer(pb2, Bindingtest1.ARRAY_SIZE);
              for(j=0; j<Bindingtest1.ARRAY_SIZE*Bindingtest1.ARRAY_SIZE; j++) {
                Assert.assertEquals("Wrong result: s:"+lb2.get(j%Bindingtest1.ARRAY_SIZE)+" d: "+lb3.get(j), 
                                  1+lb2.get(j%Bindingtest1.ARRAY_SIZE), lb3.get(j));
              }
              Assert.assertEquals(0, binding.arrayTestFoo3PtrPtrValidation(pb2, 10000));
          }

          // PointerBuffer.alloc*(ARRAY_SIZE)
          // PointerBuffer.referenceBuffer(LongBuffer.getBuffer)
          //  " "
          // PointerBuffer arrayTestFoo3PtrPtr(PointerBuffer)
          {
              PointerBuffer pb = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
              int j;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  // the referenced buffer must be direct, non direct is not supported
                  LongBuffer lb3 = Buffers.newDirectLongBuffer(Bindingtest1.ARRAY_SIZE);
                  lb3.put(lb2);
                  lb2.rewind();
                  lb3.rewind();

                  pb.referenceBuffer(lb3);
              }
              pb.rewind();

              // System.out.println("lb3: "+lb3);
              validatePointerBuffer(pb, Bindingtest1.ARRAY_SIZE);
              Assert.assertNotNull(pb.getReferencedBuffer(0));
              Assert.assertTrue("Wrong result: "+pb.getReferencedBuffer(0)+" != "+lb2, pb.getReferencedBuffer(0).equals(lb2));              

              PointerBuffer pb2 = binding.arrayTestFoo3PtrPtr(pb); // pb2 is shallow
              validatePointerBuffer(pb2, Bindingtest1.ARRAY_SIZE);
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  LongBuffer i64b = (LongBuffer) pb.getReferencedBuffer(j);
                  for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                    Assert.assertEquals("Wrong result: ["+j+"]["+i+"] s:"+lb2.get(i)+" d: "+i64b.get(i), 
                                        1+lb2.get(i), i64b.get(i));
                  }
              }
              Assert.assertEquals(0, binding.arrayTestFoo3PtrPtrValidation(pb, 10000));
          }

          // pb = PointerBuffer.alloc*(ARRAY_SIZE)
          // arrayTestFoo3CopyPtrPtrA(PointerBuffer dst, PointerBuffer src) (Native deep copy w/ alloc)  
          //  " "
          // PointerBuffer arrayTestFoo3PtrPtr(PointerBuffer)
          {
              PointerBuffer pbS = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
              int j;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  // the referenced buffer must be direct, non direct is not supported
                  LongBuffer lb3 = Buffers.newDirectLongBuffer(Bindingtest1.ARRAY_SIZE);
                  lb3.put(lb2);
                  lb2.rewind();
                  lb3.rewind();

                  pbS.referenceBuffer(lb3);
              }
              pbS.rewind();
              validatePointerBuffer(pbS, Bindingtest1.ARRAY_SIZE);
              Assert.assertNotNull(pbS.getReferencedBuffer(0));
              Assert.assertTrue("Wrong result: "+pbS.getReferencedBuffer(0)+" != "+lb2, pbS.getReferencedBuffer(0).equals(lb2));
              
              PointerBuffer pbD = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
              
              // System.err.println("\n***pbS "+pbS); System.err.println("***pbD "+pbD);
              binding.arrayTestFoo3CopyPtrPtrA(pbD, pbS); // pbD is shallow              
              validatePointerBuffer(pbD, Bindingtest1.ARRAY_SIZE);              
              
              PointerBuffer pbD2 = binding.arrayTestFoo3PtrPtr(pbD); // pbD2 is shallow
              Assert.assertEquals(0, binding.arrayTestFoo3PtrPtrValidation(pbD, 10000));
              validatePointerBuffer(pbD2, Bindingtest1.ARRAY_SIZE);
              Assert.assertEquals(0, binding.arrayTestFoo3PtrPtrValidation(pbD2, 10000));
          }

          result = binding.bufferTest(lb);
          Assert.assertTrue("Wrong result: "+result, 10==result);

          result = binding.bufferTestNioOnly(lb);
          Assert.assertTrue("Wrong result: "+result, 10==result);

          if(direct) {
              result = binding.bufferTestNioDirectOnly(lb);
              Assert.assertTrue("Wrong result: "+result, 10==result);
          } else {
              Exception e = null;
              try {
                  binding.bufferTestNioDirectOnly(lb);
              } catch (RuntimeException re) {
                  e = re;
              }
              Assert.assertNotNull(e);
          }
          
          result = binding.doubleTest(context, lb, lb1, bb2, lb2);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000+100+80000==result);

          result = binding.doubleTest(context, lb, larray1, larray1_offset, bb2, larray2, larray2_offset);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000+100+80000==result);

          result = binding.doubleTestNioOnly(context, lb, lb1, bb2, lb2);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000+100+80000==result);

          result = binding.mixedTest(context, lb, lb1);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000==result);

          result = binding.mixedTest(context, lb, larray1, larray1_offset);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000==result);

          result = binding.mixedTestNioOnly(context, lb, lb1);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000==result);

          result = binding.nopTest();
          Assert.assertTrue("Wrong result: "+result, 42==result);

          i = binding.strToInt("42");
          Assert.assertTrue("Wrong result: "+i, 42==i);

          String str = binding.intToStr(42);
          Assert.assertTrue("Wrong result: "+str, str.equals("42"));

          i = binding.stringArrayRead(new String[] { "1234", "5678", "9a" }, 3);
          Assert.assertTrue("Wrong result: "+i, 10==i);

          i = binding.stringArrayRead(null, 0);
          Assert.assertTrue("Wrong result: "+i, 0==i);

          {
        	  // one 0xff in each byte array
              // the referenced buffer must be direct, non direct is not supported
        	  ByteBuffer bbB = Buffers.newDirectByteBuffer(new byte [] {(byte)0xaa, (byte)0xff, (byte)0xba, (byte)0xbe});
        	  bbB.rewind();
              PointerBuffer pbB = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
              PointerBuffer pbL = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
              for(int j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  pbB.referenceBuffer(bbB);
                  pbL.put(bbB.capacity());
              }
              pbB.rewind();
              pbL.rewind();
              validatePointerBuffer(pbB, Bindingtest1.ARRAY_SIZE);
              Assert.assertNotNull(pbB.getReferencedBuffer(0));
              Assert.assertTrue("Wrong result: "+pbB.getReferencedBuffer(0)+" != "+bbB, pbB.getReferencedBuffer(0).equals(bbB));
              validatePointerBuffer(pbL, Bindingtest1.ARRAY_SIZE);
              long temp = pbL.get();
              Assert.assertTrue("Wrong result: "+temp, temp==bbB.capacity());
              pbL.rewind();
	          i = binding.binaryArrayRead(pbL, pbB, Bindingtest1.ARRAY_SIZE);
	          Assert.assertTrue("Wrong result: "+i, Bindingtest1.ARRAY_SIZE==i);
          }

          IntBuffer ib = newIntBuffer(3, direct);
          ib.put(0, 1);
          ib.put(1, 2);
          ib.put(2, 3);

          int[] iarray = new int[] { 1, 2, 3 };

          i = binding.intArrayRead(ib, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);

          i = binding.intArrayRead(null, 0);
          Assert.assertTrue("Wrong result: "+i, 0==i);

          i = binding.intArrayRead(iarray, 0, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);

          i = binding.intArrayRead(null, 0, 0);
          Assert.assertTrue("Wrong result: "+i, 0==i);
          
          {
              long cfg_base = 0xAABBCCDD11223344L;
              
              PointerBuffer pb = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
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

    public void chapter04TestPointerBuffer(Bindingtest1 binding) throws Exception {
          final long DEADBEEF = 0x00000000DEADBEEFL;

          {
              long bbA, bbA2;
              ByteBuffer bb, bb2;
              PointerBuffer bbPb; 
              
              final ByteBuffer blob = binding.createAPtrBlob();
              final PointerBuffer blobPb = safeByteBuffer2PointerBuffer(blob, 1);
              Assert.assertEquals(DEADBEEF, 0xFFFFFFFF & blobPb.get(0));
              
              binding.arrayTestAVoidPtrTypeDim1Mutable(blobPb); // new memory in [0]
              Assert.assertTrue(DEADBEEF != ( 0xFFFFFFFF & blobPb.get(0) ) );
              bb = binding.arrayTestAVoidPtrTypeDim1Immutable(blobPb); // returns memory address of [0], returned as bb (blob)
              bbA = cleanAddress( binding.getAPtrAddress(bb) ); // address of new memory in [0]
              Assert.assertEquals(blobPb.get(0), bbA);
              
              bbPb = safeByteBuffer2PointerBuffer(bb, 1);
              Assert.assertEquals(DEADBEEF, 0xFFFFFFFF & bbPb.get(0));
              Assert.assertEquals( blobPb.get(0), cleanAddress( binding.getAPtrAddress(bbPb.getBuffer()) ) );
              
              bb2 = binding.arrayTestAVoidPtrTypeDim0(bb);
              bbA2 = cleanAddress( binding.getAPtrAddress(bb2) );
              Assert.assertEquals(bbA, bbA2);          
              binding.releaseAPtrBlob(bb);
              binding.releaseAPtrBlob(blob);
          }
          
          {
              long bbA, bbA2;
              ByteBuffer bb;
              PointerBuffer bbPb;
              
              final ByteBuffer blob = binding.createAPtrBlob();
              final PointerBuffer blobPb = safeByteBuffer2PointerBuffer(blob, 1);
              Assert.assertEquals(DEADBEEF, 0xFFFFFFFF & blobPb.get(0));

              binding.arrayTestAIntPtrTypeDim1Mutable(blobPb);  // new memory in [0]
              Assert.assertTrue(DEADBEEF != ( 0xFFFFFFFF & blobPb.get(0) ) );
              bbA = cleanAddress( binding.arrayTestAIntPtrTypeDim1Immutable(blobPb) );  // returns memory address of [0], returned as intptr_t              
              Assert.assertEquals(blobPb.get(0), bbA);
              bb = binding.getAPtrMemory(bbA);
              bbPb = safeByteBuffer2PointerBuffer(bb, 1);
              Assert.assertEquals(DEADBEEF, 0xFFFFFFFF & bbPb.get(0));
              
              bbA2 = cleanAddress( binding.arrayTestAIntPtrTypeDim0(bbA) );
              Assert.assertEquals(bbA, bbA2);                        
              binding.releaseAPtrBlob(bb);
              binding.releaseAPtrBlob(blob);
          }

          {
              long bbA, bbA2;
              ByteBuffer bb, bb2;
              PointerBuffer bbPb; 
              
              final ByteBuffer blob = binding.createAPtrBlob();
              final PointerBuffer blobPb = safeByteBuffer2PointerBuffer(blob, 1);
              Assert.assertEquals(DEADBEEF, 0xFFFFFFFF & blobPb.get(0));
              
              binding.arrayTestAPtr1TypeDim1Mutable(blobPb); // new memory in [0]
              Assert.assertTrue(DEADBEEF != ( 0xFFFFFFFF & blobPb.get(0) ) );
              bb = binding.arrayTestAPtr1TypeDim1Immutable(blobPb); // returns memory address of [0], returned as bb (blob)
              bbA = cleanAddress( binding.getAPtrAddress(bb) ); // address of new memory in [0]
              Assert.assertEquals(blobPb.get(0), bbA);
              
              bbPb = safeByteBuffer2PointerBuffer(bb, 1);
              Assert.assertEquals(DEADBEEF, 0xFFFFFFFF & bbPb.get(0));
              Assert.assertEquals(blobPb.get(0), cleanAddress( binding.getAPtrAddress(bbPb.getBuffer()) ) );
              
              bb2 = binding.arrayTestAPtr1TypeDim0(bb);
              bbA2 = cleanAddress( binding.getAPtrAddress(bb2) );
              Assert.assertEquals(bbA, bbA2);          
              binding.releaseAPtrBlob(bb);
              binding.releaseAPtrBlob(blob);
              
          }
          
          {
              long bbA, bbA2;
              ByteBuffer bb;
              PointerBuffer bbPb;
              
              final ByteBuffer blob = binding.createAPtrBlob();
              final PointerBuffer blobPb = safeByteBuffer2PointerBuffer(blob, 1);
              Assert.assertEquals(DEADBEEF, 0xFFFFFFFF & blobPb.get(0));

              binding.arrayTestAPtr2TypeDim1Mutable(blobPb);  // new memory in [0]
              Assert.assertTrue(DEADBEEF != ( 0xFFFFFFFF & blobPb.get(0) ) );
              bbA = cleanAddress( binding.arrayTestAPtr2TypeDim1Immutable(blobPb) );  // returns memory address of [0], returned as intptr_t              
              Assert.assertEquals(blobPb.get(0), bbA);
              bb = binding.getAPtrMemory(bbA);
              bbPb = safeByteBuffer2PointerBuffer(bb, 1);
              Assert.assertEquals(DEADBEEF, 0xFFFFFFFF & bbPb.get(0));
              
              bbA2 = cleanAddress( binding.arrayTestAPtr2TypeDim0(bbA) );
              Assert.assertEquals(bbA, bbA2);                        
              binding.releaseAPtrBlob(bb);
              binding.releaseAPtrBlob(blob);
          }
          
    }

    /**
     * This covers indirect primitive arrays and indirect NIO buffers.
     */
    public void chapter05TestSomeFunctionsAllIndirect(Bindingtest1 binding) throws Exception {
          int i;

          IntBuffer ib = IntBuffer.allocate(3);
          ib.put(0, 1);
          ib.put(1, 2);
          ib.put(2, 3);

          int[] iarray = new int[] { 1, 2, 3 };

          i = binding.intArrayRead(ib, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);

          i = binding.intArrayRead(iarray, 0, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);
          
          final int[] src = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
          final IntBuffer srcB = IntBuffer.wrap(src);
          {
              final int[] dst = new int[src.length];
              i = binding.intArrayCopy(dst, 0, src, 0, src.length);
              System.err.println("ArrayCopy.01: "+Arrays.toString(dst));
              Assert.assertTrue("Wrong result: "+i, src.length==i);
              Assert.assertTrue(Arrays.equals(src, dst));              
          }
          {
              IntBuffer dstB = IntBuffer.allocate(src.length);
              i = binding.intArrayCopy(dstB, srcB, src.length);
              System.err.println("ArrayCopy.02: "+Arrays.toString(dstB.array())+", "+dstB);
              Assert.assertTrue("Wrong result: "+i, src.length==i);
              Assert.assertTrue(Arrays.equals(src, dstB.array()));
          }
          
          {
              final int[] src36 = new int[] { 4, 5, 6, 7 };
              final int[] dst = new int[src36.length];
              i = binding.intArrayCopy(dst, 0, src, 3, src36.length);
              System.err.println("ArrayCopy.03: "+Arrays.toString(dst));
              Assert.assertTrue("Wrong result: "+i, src36.length==i);
              Assert.assertTrue(Arrays.equals(src36, dst));
          }
          
          final int[] src2 = new int[] { 0, 0, 0, 4, 5, 6, 7, 0, 0, 0 };
          {
              final int[] dst = new int[src2.length];
              i = binding.intArrayCopy(dst, 3, src, 3, 4);
              System.err.println("ArrayCopy.04: "+Arrays.toString(dst));
              Assert.assertTrue("Wrong result: "+i, 4==i);
              Assert.assertTrue(Arrays.equals(src2, dst));
          }
          {
              IntBuffer dstB = IntBuffer.allocate(src2.length);
              {
                  dstB.position(3);
                  srcB.position(3);
                  i = binding.intArrayCopy(dstB, srcB, 4);
                  dstB.position(0);
                  srcB.position(0);
              }
              System.err.println("ArrayCopy.05: "+Arrays.toString(dstB.array())+", "+dstB);
              Assert.assertTrue("Wrong result: "+i, 4==i);
              Assert.assertTrue(Arrays.equals(src2, dstB.array()));
          }
    }
    
    void assertAPTR(final long expected, final long actual) {
        System.err.println("0x"+Long.toHexString(expected)+" == 0x"+Long.toHexString(actual));
        if (Platform.is32Bit()) {
            int exp32;
            int act32;
            // if(Platform.isLittleEndian()) {
                exp32 = (int) ( expected ) ;
                act32 = (int) ( actual ) ;                
            /* } else {
                exp32 = (int) ( expected >> 32 ) ;
                act32 = (int) ( actual >> 32 ) ;
            } */
            System.err.println("0x"+Integer.toHexString(exp32)+" == 0x"+Integer.toHexString(act32));
            Assert.assertEquals(exp32, act32);
        } else {
            Assert.assertEquals(expected, actual);
        }        
    }
    
    public void chapter09TestCompoundAndAlignment(Bindingtest1 binding) throws Exception {
        
        MachineDescription.StaticConfig smd = MachineDescriptionRuntime.getStatic();
        MachineDescription md = MachineDescriptionRuntime.getRuntime();
        
        System.err.println("static  md: "+smd);
        System.err.println("runtime md: "+md);
        System.err.println("compatible static/runtime: "+md.compatible(smd.md));
        
        {
            TK_ComplicatedSuperSet cs =  binding.createComplicatedSuperSet();
            Assert.assertEquals((byte)0xA0, cs.getBits1());
            
            TK_ComplicatedSubSet sub1 =  cs.getSub1();
            Assert.assertEquals((byte)0xA1, sub1.getBits1());
            Assert.assertEquals(0x12345678, sub1.getId());
            Assert.assertEquals((byte)0xA2, sub1.getBits2());
            Assert.assertEquals(0x123456789abcdef0L, sub1.getLong0());
            Assert.assertEquals((byte)0xA3, sub1.getBits3());
            Assert.assertEquals(3.1415926535897932384626433832795, sub1.getReal0(), 0.0);
            Assert.assertEquals((byte)0xA4, sub1.getBits4());
            Assert.assertEquals(256.12345f, sub1.getReal1(), 0.0);
            Assert.assertEquals((byte)0xA5, sub1.getBits5());
            Assert.assertEquals((long)0xdeadbeefL, sub1.getLongX());
            Assert.assertEquals((byte)0xA6, sub1.getBits6());
    
            Assert.assertEquals((byte)0xB0, cs.getBits2());
            
            TK_ComplicatedSubSet sub2 =  cs.getSub2();
            Assert.assertEquals((byte)0xB1, sub2.getBits1());
            Assert.assertEquals(0x12345678, sub2.getId());
            Assert.assertEquals((byte)0xB2, sub2.getBits2());
            Assert.assertEquals(0x123456789abcdef0L, sub2.getLong0());
            Assert.assertEquals((byte)0xB3, sub2.getBits3());
            Assert.assertEquals(3.1415926535897932384626433832795, sub2.getReal0(), 0.0);
            Assert.assertEquals((byte)0xB4, sub2.getBits4());
            Assert.assertEquals(256.12345f, sub2.getReal1(), 0.0);
            Assert.assertEquals((byte)0xB5, sub2.getBits5());
            Assert.assertEquals((long)0xdeadbeefL, sub2.getLongX());
            Assert.assertEquals((byte)0xB6, sub2.getBits6());
            
            Assert.assertEquals((byte)0xC0, cs.getBits3());
            
            binding.destroyComplicatedSuperSet(cs);
        }

        /********************************************************************************/

        {
            TK_ComplicatedSuperSet cs =  TK_ComplicatedSuperSet.create();
            cs.setBits1((byte)0xA0);
            
            TK_ComplicatedSubSet sub1 =  cs.getSub1();
            sub1.setBits1((byte)0xA1);
            sub1.setId(0x12345678);
            sub1.setBits2((byte)0xA2);
            sub1.setLong0(0x123456789abcdef0L);
            sub1.setBits3((byte)0xA3);
            sub1.setReal0(3.1415926535897932384626433832795);
            sub1.setBits4((byte)0xA4);
            sub1.setReal1(256.12345f);
            sub1.setBits5((byte)0xA5);            
            sub1.setLongX((long)0xdeadbeefL);
            sub1.setBits6((byte)0xA6);
    
            cs.setBits2((byte)0xB0);
            
            TK_ComplicatedSubSet sub2 =  cs.getSub2();
            sub2.setBits1((byte)0xB1);
            sub2.setId(0x12345678);
            sub2.setBits2((byte)0xB2);
            sub2.setLong0(0x123456789abcdef0L);
            sub2.setBits3((byte)0xB3);
            sub2.setReal0(3.1415926535897932384626433832795);
            sub2.setBits4((byte)0xB4);
            sub2.setReal1(256.12345f);
            sub2.setBits5((byte)0xB5);            
            sub2.setLongX((long)0xdeadbeefL);
            sub2.setBits6((byte)0xB6);
            
            cs.setBits3((byte)0xC0);
            
            Assert.assertTrue(binding.hasInitValues(cs));
        }

        /********************************************************************************/
        
        TK_Surface surface = binding.createSurface();
        
        final long surfaceContext = surface.getCtx(); 
        assertAPTR(0x123456789abcdef0L, surfaceContext);
        
        TK_ContextWrapper ctxWrapper = surface.getCtxWrapper();
        final long wrapperContext = ctxWrapper.getCtx();
        assertAPTR(0xA23456781abcdef0L, wrapperContext);
        
        TK_Engine engine = surface.getEngine();
        final long engineContext = engine.getCtx();
        assertAPTR(0xB23456782abcdef0L, engineContext);
        Assert.assertEquals(0x0111, engine.render(0x0100, 0x0010, 0x0001));
        
        surface.setCtx(surfaceContext);
        assertAPTR(surfaceContext, surface.getCtx());
        assertAPTR(wrapperContext, ctxWrapper.getCtx());
        assertAPTR(engineContext, engine.getCtx());
        Assert.assertEquals(0x0111, engine.render(0x0100, 0x0010, 0x0001));
        
        ctxWrapper.setCtx(wrapperContext);
        assertAPTR(surfaceContext, surface.getCtx());
        assertAPTR(wrapperContext, ctxWrapper.getCtx());
        assertAPTR(engineContext, engine.getCtx());
        Assert.assertEquals(0x0111, engine.render(0x0100, 0x0010, 0x0001));
        
        engine.setCtx(engineContext);
        assertAPTR(surfaceContext, surface.getCtx());
        assertAPTR(wrapperContext, ctxWrapper.getCtx());
        assertAPTR(engineContext, engine.getCtx());
        Assert.assertEquals(0x0111, engine.render(0x0100, 0x0010, 0x0001));
        
        TK_Dimension dimension = surface.getBounds();
        Assert.assertEquals(0x11111111, dimension.getX());
        Assert.assertEquals(0x22222222, dimension.getY());
        Assert.assertEquals(0x33333333, dimension.getWidth());
        Assert.assertEquals(0x44444444, dimension.getHeight());

        Assert.assertEquals(2, surface.getClipSize());
        
        for(int i=0; i<surface.getClipSize(); i++) {
            TK_Dimension clip = surface.getClip(i);
            Assert.assertEquals(0x44444444 * (i+1) + 0x11111111, clip.getX());
            Assert.assertEquals(0x44444444 * (i+1) + 0x22222222, clip.getY());
            Assert.assertEquals(0x44444444 * (i+1) + 0x33333333, clip.getWidth());
            Assert.assertEquals(0x44444444 * (i+1) + 0x44444444, clip.getHeight());
        }
        binding.destroySurface(surface);
    }

}
