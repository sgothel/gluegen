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
import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.SingletonJunitCase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import jogamp.common.os.MachineDataInfoRuntime;

import org.junit.Assert;


/**
 * @author Michael Bien
 * @author Sven Gothel
 */
public class BaseClass extends SingletonJunitCase {

    /**
     * Verifies the existence and creation of the generated class.
     */
    public void testClassExist(final String name) throws Exception {
        final String ifName = "com.jogamp.gluegen.test.junit.generation.Binding"+name;
        final String implName = "com.jogamp.gluegen.test.junit.generation.impl.Binding"+name+"Impl";

        final Class<?> clazzIf   = Class.forName(ifName);
        final Class<?> clazzImpl = Class.forName(implName);

        Assert.assertNotNull(ifName+" does not exist", clazzIf);
        Assert.assertNotNull(implName+" does not exist", clazzImpl);

        Assert.assertNotNull(clazzImpl.getDeclaredMethod("nopTest"));

        final Object obj = clazzImpl.newInstance();
        Assert.assertTrue("Not of type "+ifName, clazzIf.isAssignableFrom(obj.getClass()));
        Assert.assertTrue("Not of type com.jogamp.gluegen.test.junit.generation.Bindingtest1",
                (obj instanceof com.jogamp.gluegen.test.junit.generation.Bindingtest1));
    }

    public static final float EPSILON = 1.1920929E-7f; // Float.MIN_VALUE == 1.4e-45f ; double EPSILON 2.220446049250313E-16d

    /**
     * Verifies if all generated method signatures are completed,
     * ie a compilation only coverage test without functional tests.
     */
    public void chapter__TestCoverageSignature(final Bindingtest1 binding) throws Exception {
          int i = 0;
          final long context = 0;
          LongBuffer lb=null;
          ByteBuffer bb=null;
          final IntBuffer ib=null;
          final long[] larray = null;
          final int larray_offset = 0;
          String str=null;
          final String[] strings = null;
          final int[] iarray = null;
          final int iarray_offset = 0;
          long result = 0;
          long l = result;
          ShortBlob sb = null;
          Int32Struct i32s = null;
          AnonBlob ab = null;
          PointerBuffer pb=null;

          {
              l = binding.testXID(l);
              l = binding.testXID_2(l);

              bb = binding.testAnonBuffer(bb);

              sb = binding.testShortBlob(sb);
              sb = binding.testLPShortBlob0(sb);
              sb = binding.testLPShortBlob1(sb);
              sb = binding.testLPShortBlob2(sb);
              sb = binding.testLPShortBlob3(sb);
              sb = binding.testShortBlobL1(sb);
              sb = binding.testShortBlobL2(sb);

              i32s = binding.testInt32Struct(i32s);

              ab = binding.testCreateAnonBlob();
              binding.testDestroyAnonBlob(ab);

              l = binding.testCreateAnonBlob2();
              binding.testDestroyAnonBlob2(l);

              lb = binding.testFooPtr(larray, 0);
              lb = binding.testFooPtr(lb);

              i = binding.testDelegate(i);
          }

          {
              bb = binding.createAPtrBlob();
              pb = safeByteBuffer2PointerBuffer(bb, 1);
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

    /**
     * Verifies if all generated static constant values are completed,
     * and whether their value is as expected!
     * <p>
     * Covers all enumerates and defines.
     * </p>
     */
    public void chapter01TestStaticConstants(final Bindingtest1 binding) throws Exception {
        // Plain vanilla CPP constants
        Assert.assertEquals(   1, Bindingtest1.CONSTANT_ONE);
        Assert.assertEquals(   8, Bindingtest1.ARRAY_SIZE);
        Assert.assertEquals(1234, Bindingtest1.DEFINE_01);

        // Enums
        Assert.assertEquals(   1, Bindingtest1.LI);
        Assert.assertEquals(   3, Bindingtest1.LO);
        Assert.assertEquals(   2, Bindingtest1.LU);
        Assert.assertEquals(   1, Bindingtest1.MI);
        Assert.assertEquals(   3, Bindingtest1.MO);
        Assert.assertEquals(   2, Bindingtest1.MU);
        Assert.assertEquals(   0, Bindingtest1.ZERO);
        Assert.assertEquals(   1, Bindingtest1.ONE);
        Assert.assertEquals(   2, Bindingtest1.TWO);
        Assert.assertEquals(   3, Bindingtest1.THREE);

        // CPP Macro Expansion!
        Assert.assertEquals(   1, Bindingtest1.NUMBER_ONE);
        Assert.assertEquals(   2, Bindingtest1.NUMBER_TWO);
        Assert.assertEquals(   4, Bindingtest1.NUMBER_FOUR);
        Assert.assertEquals(   5, Bindingtest1.NUMBER_FIVE);
        Assert.assertEquals(   8, Bindingtest1.NUMBER_EIGHT);
        Assert.assertEquals(   9, Bindingtest1.NUMBER_NINE);
        Assert.assertEquals(  10, Bindingtest1.NUMBER_TEN);

        // Enum Constant Expressions!
        Assert.assertEquals(   1, Bindingtest1.ENUM_NUM_ONE);
        Assert.assertEquals(   2, Bindingtest1.ENUM_NUM_TWO);
        Assert.assertEquals(   3, Bindingtest1.ENUM_NUM_THREE);
        Assert.assertEquals(   4, Bindingtest1.ENUM_NUM_FOUR);
        Assert.assertEquals(   5, Bindingtest1.ENUM_NUM_FIVE);
        Assert.assertEquals(   8, Bindingtest1.ENUM_NUM_EIGHT);
        Assert.assertEquals(   9, Bindingtest1.ENUM_NUM_NINE);
        Assert.assertEquals(  10, Bindingtest1.ENUM_NUM_TEN);

        // Integer 32bit (int / enum)
        final int ENUM_I0 =  Bindingtest1.ENUM_I0;
        final int ENUM_I1 =  Bindingtest1.ENUM_I1;
        final int ENUM_I2 =  Bindingtest1.ENUM_I2;
        final int ENUM_I3 =  Bindingtest1.ENUM_I3;
        final int ENUM_I4 = -Bindingtest1.ENUM_I4;
        final int ENUM_I5 = -Bindingtest1.ENUM_I5;
        final int ENUM_I6 = -Bindingtest1.ENUM_I6;
        final int ENUM_I7 =  Bindingtest1.ENUM_I7;
        final int ENUM_I8 =  Bindingtest1.ENUM_I8;
        final int ENUM_I9 =  Bindingtest1.ENUM_I9;
        final int ENUM_IA =  Bindingtest1.ENUM_IA;
        final int ENUM_IB =  Bindingtest1.ENUM_IB;
        final int ENUM_IX =  Bindingtest1.ENUM_IX;
        int iexp = 10;
        Assert.assertEquals(iexp++, ENUM_I0);
        Assert.assertEquals(iexp++, ENUM_I1);
        Assert.assertEquals(iexp++, ENUM_I2);
        Assert.assertEquals(iexp++, ENUM_I3);
        Assert.assertEquals(iexp++, ENUM_I4);
        Assert.assertEquals(iexp++, ENUM_I5);
        Assert.assertEquals(iexp++, ENUM_I6);
        Assert.assertEquals(iexp++, ENUM_I7);
        Assert.assertEquals(iexp++, ENUM_I8);
        Assert.assertEquals(iexp++, ENUM_I9);
        Assert.assertEquals(iexp++, ENUM_IA);
        Assert.assertEquals(iexp++, ENUM_IB);
        Assert.assertEquals(0xffffffff, ENUM_IX);

        // Integer 32bit (int / define)
        final int CL_INT_I0 =  Bindingtest1.CL_INT_I0;
        final int CL_INT_I1 =  Bindingtest1.CL_INT_I1;
        final int CL_INT_I2 =  Bindingtest1.CL_INT_I2;
        final int CL_INT_I3 =  Bindingtest1.CL_INT_I3;
        final int CL_INT_I4 = -Bindingtest1.CL_INT_I4;
        final int CL_INT_I5 = -Bindingtest1.CL_INT_I5;
        final int CL_INT_I6 = -Bindingtest1.CL_INT_I6;
        final int CL_INT_I7 = -Bindingtest1.CL_INT_I7;
        final int CL_INT_I8 =  Bindingtest1.CL_INT_I8;
        final int CL_INT_I9 =  Bindingtest1.CL_INT_I9;
        final int CL_INT_IA =  Bindingtest1.CL_INT_IA;
        final int CL_INT_IB =  Bindingtest1.CL_INT_IB;
        final int CL_INT_IX =  Bindingtest1.CL_INT_IX;
        iexp = 10;
        Assert.assertEquals(iexp++, CL_INT_I0);
        Assert.assertEquals(iexp++, CL_INT_I1);
        Assert.assertEquals(iexp++, CL_INT_I2);
        Assert.assertEquals(iexp++, CL_INT_I3);
        Assert.assertEquals(iexp++, CL_INT_I4);
        Assert.assertEquals(iexp++, CL_INT_I5);
        Assert.assertEquals(iexp++, CL_INT_I6);
        Assert.assertEquals(iexp++, CL_INT_I7);
        Assert.assertEquals(iexp++, CL_INT_I8);
        Assert.assertEquals(iexp++, CL_INT_I9);
        Assert.assertEquals(iexp++, CL_INT_IA);
        Assert.assertEquals(iexp++, CL_INT_IB);
        Assert.assertEquals(0xffffffff, CL_INT_IX);

        // Integer 64bit (long / define )
        final long CL_LNG_L0 =  Bindingtest1.CL_LNG_L0;
        final long CL_LNG_L1 =  Bindingtest1.CL_LNG_L1;
        final long CL_LNG_L2 =  Bindingtest1.CL_LNG_L2;
        final long CL_LNG_L3 =  Bindingtest1.CL_LNG_L3;
        final long CL_LNG_L4 = -Bindingtest1.CL_LNG_L4;
        final long CL_LNG_L5 = -Bindingtest1.CL_LNG_L5;
        final long CL_LNG_L6 = -Bindingtest1.CL_LNG_L6;
        final long CL_LNG_L7 = -Bindingtest1.CL_LNG_L7;
        final long CL_LNG_L8 =  Bindingtest1.CL_LNG_L8;
        final long CL_LNG_L9 =  Bindingtest1.CL_LNG_L9;
        final long CL_LNG_LA =  Bindingtest1.CL_LNG_LA;
        final long CL_LNG_LB =  Bindingtest1.CL_LNG_LB;
        final long CL_LNG_LX =  Bindingtest1.CL_LNG_LX;
        long lexp = 2147483648L;
        Assert.assertEquals(lexp++, CL_LNG_L0);
        Assert.assertEquals(lexp++, CL_LNG_L1);
        Assert.assertEquals(lexp++, CL_LNG_L2);
        Assert.assertEquals(lexp++, CL_LNG_L3);
        Assert.assertEquals(lexp++, CL_LNG_L4);
        Assert.assertEquals(lexp++, CL_LNG_L5);
        Assert.assertEquals(lexp++, CL_LNG_L6);
        Assert.assertEquals(lexp++, CL_LNG_L7);
        Assert.assertEquals(lexp++, CL_LNG_L8);
        Assert.assertEquals(lexp++, CL_LNG_L9);
        Assert.assertEquals(lexp++, CL_LNG_LA);
        Assert.assertEquals(lexp++, CL_LNG_LB);
        Assert.assertEquals(0xffffffffffffffffL, CL_LNG_LX);

        // Floating point hexadecimals
        final float CL_FLT_A0 = Bindingtest1.CL_FLT_A0;
        final float CL_FLT_A1 = Bindingtest1.CL_FLT_A1;
        final float CL_FLT_A2 = Bindingtest1.CL_FLT_A2;
        final float CL_FLT_A3 = Bindingtest1.CL_FLT_A3;
        final float CL_FLT_A4 = Bindingtest1.CL_FLT_A4;
        final float CL_FLT_A5 = Bindingtest1.CL_FLT_A5;
        final float CL_FLT_A6 = Bindingtest1.CL_FLT_A6;
        final float CL_FLT_A7 = Bindingtest1.CL_FLT_A7;
        Assert.assertEquals(  0x1.p127f,  CL_FLT_A0, EPSILON);
        Assert.assertEquals(  0x1.p+127F, CL_FLT_A1, EPSILON);
        Assert.assertEquals(  0x1.p-127f, CL_FLT_A2, EPSILON);
        Assert.assertEquals( -0.1f, CL_FLT_A3, EPSILON);
        Assert.assertEquals(  0.2f, CL_FLT_A4, EPSILON);
        Assert.assertEquals(  0.3f, CL_FLT_A5, EPSILON);
        Assert.assertEquals(  0.4f, CL_FLT_A6, EPSILON);
        Assert.assertEquals(  1.0f, CL_FLT_A7, EPSILON);

        final float CL_FLT_EPSILON = Bindingtest1.CL_FLT_EPSILON;
        final double CL_FLT_MAX= Bindingtest1.CL_FLT_MAX;
        final double CL_FLT_MIN = Bindingtest1.CL_FLT_MIN;
        Assert.assertEquals(  0x1.0p-23f, CL_FLT_EPSILON, EPSILON);
        Assert.assertEquals(  0x1.fffffep127f, CL_FLT_MAX, EPSILON);
        Assert.assertEquals(  0x1.0p-126f, CL_FLT_MIN, EPSILON);

        final double CL_DBL_B0 = Bindingtest1.CL_DBL_B0;
        final double CL_DBL_B1 = Bindingtest1.CL_DBL_B1;
        final double CL_DBL_B2 = Bindingtest1.CL_DBL_B2;
        final double CL_DBL_B3 = Bindingtest1.CL_DBL_B3;
        final double CL_DBL_B4 = Bindingtest1.CL_DBL_B4;
        final double CL_DBL_B5 = Bindingtest1.CL_DBL_B5;
        final double CL_DBL_B6 = Bindingtest1.CL_DBL_B6;
        Assert.assertEquals(  0x1.p127d,  CL_DBL_B0, EPSILON);
        Assert.assertEquals(  0x1.p+127D, CL_DBL_B1, EPSILON);
        Assert.assertEquals(  0x1.p-127d, CL_DBL_B2, EPSILON);
        Assert.assertEquals( -0.1, CL_DBL_B3, EPSILON);
        Assert.assertEquals(  0.2, CL_DBL_B4, EPSILON);
        Assert.assertEquals(  0.3, CL_DBL_B5, EPSILON);
        Assert.assertEquals(  3.5e+38, CL_DBL_B6, EPSILON);

        final float CL_DBL_EPSILON = Bindingtest1.CL_DBL_EPSILON;
        final double CL_DBL_MAX= Bindingtest1.CL_DBL_MAX;
        final double CL_DBL_MIN = Bindingtest1.CL_DBL_MIN;
        Assert.assertEquals(  0x1.0p-52f, CL_DBL_EPSILON, EPSILON);
        Assert.assertEquals(  0x1.fffffffffffffp1023, CL_DBL_MAX, EPSILON);
        Assert.assertEquals(  0x1.0p-1022, CL_DBL_MIN, EPSILON);
    }

    ByteBuffer newByteBuffer(final int size, final boolean direct) {
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

    IntBuffer newIntBuffer(final int size, final boolean direct) {
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

    LongBuffer newLongBuffer(final int size, final boolean direct) {
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

    PointerBuffer newPointerBuffer(final int size, final boolean direct) {
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

    long cleanAddress(final long a) {
        if (Platform.is32Bit()) {
            return a & 0x00000000FFFFFFFFL;
        } else {
            return a;
        }
    }

    PointerBuffer validatePointerBuffer(final PointerBuffer pb, final int elements) {
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
    PointerBuffer safeByteBuffer2PointerBuffer(final ByteBuffer bb, final int elements) {
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
    public void chapter03TestCoverageFunctionalityNIOAndPrimitiveArray(final Bindingtest1 binding, final boolean direct) throws Exception {
          int i;
          long result;

          final long context = 1;
          final LongBuffer lb = newLongBuffer(1, direct);
          lb.put(0,  10);

          final ByteBuffer bb2 = newByteBuffer(Buffers.SIZEOF_LONG, direct);
          final LongBuffer bb2L = bb2.asLongBuffer();
          bb2L.put(0, 100);

          final IntBuffer ib1 = newIntBuffer(Bindingtest1.ARRAY_SIZE, direct);
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            ib1.put(i,  1000);
          }

          final LongBuffer lb1 = newLongBuffer(Bindingtest1.ARRAY_SIZE, direct);
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            lb1.put(i,  1000);
          }
          final LongBuffer lb2 = newLongBuffer(Bindingtest1.ARRAY_SIZE, direct);
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            lb2.put(i, 10000);
          }

          final int[] iarray1 = new int[Bindingtest1.ARRAY_SIZE];
          final int iarray1_offset = 0;
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            iarray1[i]=  1000;
          }

          final long[] larray1 = new long[Bindingtest1.ARRAY_SIZE];
          final int larray1_offset = 0;
          for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
            larray1[i]=  1000;
          }

          final long[] larray2 = new long[Bindingtest1.ARRAY_SIZE];
          final int larray2_offset = 0;
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
              final LongBuffer lb3 = newLongBuffer(Bindingtest1.ARRAY_SIZE, direct);
              lb3.put(lb2);
              lb3.rewind();
              lb2.rewind();

              // System.out.println("lb3: "+lb3);
              Assert.assertTrue("Wrong result: "+lb3.capacity(), Bindingtest1.ARRAY_SIZE == lb3.capacity());
              Assert.assertTrue("Wrong result: "+lb3.remaining(), Bindingtest1.ARRAY_SIZE == lb3.remaining());

              final LongBuffer lbR = binding.arrayTestFoo2(lb3);
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
              final long[] larray3 = new long[Bindingtest1.ARRAY_SIZE];
              for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                larray3[i]=  larray2[i];
              }

              final LongBuffer lbR = binding.arrayTestFoo2(larray3, 0);

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
              final LongBuffer lb3 = newLongBuffer(Bindingtest1.ARRAY_SIZE, direct);
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
              final long[] larray3 = new long[Bindingtest1.ARRAY_SIZE];
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
              final LongBuffer lb3 = newLongBuffer(Bindingtest1.ARRAY_SIZE*Bindingtest1.ARRAY_SIZE, direct);
              int j;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  lb3.put(lb2);
                  lb2.rewind();
              }
              lb3.rewind();

              // System.out.println("lb3: "+lb3);
              Assert.assertTrue("Wrong result: "+lb3.capacity(), Bindingtest1.ARRAY_SIZE*Bindingtest1.ARRAY_SIZE == lb3.capacity());
              Assert.assertTrue("Wrong result: "+lb3.remaining(), Bindingtest1.ARRAY_SIZE*Bindingtest1.ARRAY_SIZE == lb3.remaining());

              final PointerBuffer pb = binding.arrayTestFoo3ArrayToPtrPtr(lb3);
              validatePointerBuffer(pb, Bindingtest1.ARRAY_SIZE);

              final PointerBuffer pb2 = binding.arrayTestFoo3PtrPtr(pb);
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
              final PointerBuffer pb = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
              int j;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  // the referenced buffer must be direct, non direct is not supported
                  final LongBuffer lb3 = Buffers.newDirectLongBuffer(Bindingtest1.ARRAY_SIZE);
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

              final PointerBuffer pb2 = binding.arrayTestFoo3PtrPtr(pb); // pb2 is shallow
              validatePointerBuffer(pb2, Bindingtest1.ARRAY_SIZE);
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  final LongBuffer i64b = (LongBuffer) pb.getReferencedBuffer(j);
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
              final PointerBuffer pbS = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
              int j;
              for(j=0; j<Bindingtest1.ARRAY_SIZE; j++) {
                  // the referenced buffer must be direct, non direct is not supported
                  final LongBuffer lb3 = Buffers.newDirectLongBuffer(Bindingtest1.ARRAY_SIZE);
                  lb3.put(lb2);
                  lb2.rewind();
                  lb3.rewind();

                  pbS.referenceBuffer(lb3);
              }
              pbS.rewind();
              validatePointerBuffer(pbS, Bindingtest1.ARRAY_SIZE);
              Assert.assertNotNull(pbS.getReferencedBuffer(0));
              Assert.assertTrue("Wrong result: "+pbS.getReferencedBuffer(0)+" != "+lb2, pbS.getReferencedBuffer(0).equals(lb2));

              final PointerBuffer pbD = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);

              // System.err.println("\n***pbS "+pbS); System.err.println("***pbD "+pbD);
              binding.arrayTestFoo3CopyPtrPtrA(pbD, pbS); // pbD is shallow
              validatePointerBuffer(pbD, Bindingtest1.ARRAY_SIZE);

              final PointerBuffer pbD2 = binding.arrayTestFoo3PtrPtr(pbD); // pbD2 is shallow
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
              } catch (final RuntimeException re) {
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

          final String str = binding.intToStr(42);
          Assert.assertTrue("Wrong result: "+str, str.equals("42"));

          i = binding.stringArrayRead(new String[] { "1234", "5678", "9a" }, 3);
          Assert.assertTrue("Wrong result: "+i, 10==i);

          i = binding.stringArrayRead(null, 0);
          Assert.assertTrue("Wrong result: "+i, 0==i);

          {
              // one 0xff in each byte array
              // the referenced buffer must be direct, non direct is not supported
              final ByteBuffer bbB = Buffers.newDirectByteBuffer(new byte [] {(byte)0xaa, (byte)0xff, (byte)0xba, (byte)0xbe});
              bbB.rewind();
              final PointerBuffer pbB = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
              final PointerBuffer pbL = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
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
              final long temp = pbL.get();
              Assert.assertTrue("Wrong result: "+temp, temp==bbB.capacity());
              pbL.rewind();
              i = binding.binaryArrayRead(pbL, pbB, Bindingtest1.ARRAY_SIZE);
              Assert.assertTrue("Wrong result: "+i, Bindingtest1.ARRAY_SIZE==i);
          }

          final IntBuffer ib = newIntBuffer(3, direct);
          ib.put(0, 1);
          ib.put(1, 2);
          ib.put(2, 3);

          final int[] iarray = new int[] { 1, 2, 3 };

          i = binding.intArrayRead(ib, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);

          i = binding.intArrayRead(null, 0);
          Assert.assertTrue("Wrong result: "+i, 0==i);

          i = binding.intArrayRead(iarray, 0, 3);
          Assert.assertTrue("Wrong result: "+i, 6==i);

          i = binding.intArrayRead(null, 0, 0);
          Assert.assertTrue("Wrong result: "+i, 0==i);

          {
              final long cfg_base = 0xAABBCCDD11223344L;

              final PointerBuffer pb = newPointerBuffer(Bindingtest1.ARRAY_SIZE, direct);
              for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                long cfg_native;
                if(Platform.is32Bit()) {
                    cfg_native = (cfg_base+i) & 0x00000000FFFFFFFFL; // umask 1st 32bit
                } else {
                    cfg_native = (cfg_base+i);
                }
                final long cfg = binding.typeTestAnonSingle(cfg_base + i);
                Assert.assertTrue("Wrong result: 0x"+Long.toHexString(cfg_native)+"+1 != 0x"+Long.toHexString(cfg), (cfg_native+1)==cfg);
                pb.put(i, cfg_base+i);

                final long t = pb.get(i);
                Assert.assertTrue("Wrong result: 0x"+Long.toHexString(cfg_native)+" != 0x"+Long.toHexString(t), cfg_native==t);
              }
              pb.rewind();
              final PointerBuffer pb2 = binding.typeTestAnonPointer(pb);
              Assert.assertTrue("Wrong result: "+pb2.capacity(), Bindingtest1.ARRAY_SIZE == pb2.capacity());
              Assert.assertTrue("Wrong result: "+pb2.remaining(), Bindingtest1.ARRAY_SIZE == pb2.remaining());
              for(i=0; i<Bindingtest1.ARRAY_SIZE; i++) {
                  Assert.assertTrue("Wrong result: 0x"+Long.toHexString(pb.get(i))+"+1 != 0x"+Long.toHexString(pb2.get(i)), (pb.get(i)+1)==pb2.get(i));
              }
          }

          {
              final long l0 = 0xAAFFEE;
              final long l1 = binding.testXID(l0);
              final long l2 = binding.testXID_2(l0);
              Assert.assertEquals(l0, l1);
              Assert.assertEquals(l0, l2);

              final ByteBuffer bb = Buffers.newDirectByteBuffer(PointerBuffer.ELEMENT_SIZE);
              for(int j=0; j<bb.limit(); j++) {
                  bb.put(j, (byte)(0xAA+j));
              }
              final ByteBuffer bbOut = binding.testAnonBuffer(bb);
              Assert.assertEquals(bb, bbOut);

              final ShortBlob sb = ShortBlob.create();
              sb.setB1((byte)0xAA);
              sb.setB2((byte)0xEE);
              final ShortBlob sb_ = binding.testShortBlob(sb);
              final ShortBlob sb0 = binding.testLPShortBlob0(sb);
              final ShortBlob sb1 = binding.testLPShortBlob1(sb);
              final ShortBlob sb2 = binding.testLPShortBlob2(sb);
              final ShortBlob sb3 = binding.testLPShortBlob3(sb);
              final ShortBlob sb4 = binding.testShortBlobL1(sb);
              final ShortBlob sb5 = binding.testShortBlobL2(sb);
              Assert.assertEquals(sb.getBuffer(), sb_.getBuffer());
              Assert.assertEquals(sb.getBuffer(), sb0.getBuffer());
              Assert.assertEquals(sb.getBuffer(), sb1.getBuffer());
              Assert.assertEquals(sb.getBuffer(), sb2.getBuffer());
              Assert.assertEquals(sb.getBuffer(), sb3.getBuffer());
              Assert.assertEquals(sb.getBuffer(), sb4.getBuffer());
              Assert.assertEquals(sb.getBuffer(), sb5.getBuffer());

              final Int32Struct i32s = Int32Struct.create();
              i32s.setB1((byte)0x02);
              i32s.setB2((byte)0x12);
              i32s.setB3((byte)0x22);
              i32s.setB4((byte)0x32);
              final Int32Struct i32s0 = binding.testInt32Struct(i32s);
              Assert.assertEquals(i32s.getBuffer(), i32s0.getBuffer());

              final AnonBlob ab = binding.testCreateAnonBlob();
              binding.testDestroyAnonBlob(ab);

              final long ab2 = binding.testCreateAnonBlob2();
              binding.testDestroyAnonBlob2(ab2);

              final long[] foo = new long[] { 0x1122334455667788L };
              final LongBuffer fooLB = Buffers.newDirectLongBuffer(foo);
              final LongBuffer foo1Out = binding.testFooPtr(fooLB);
              Assert.assertEquals(fooLB, foo1Out);
              final LongBuffer foo2Out = binding.testFooPtr(foo, 0);
              Assert.assertEquals(fooLB, foo2Out);
          }

          {
              i=41;
              final int iRes = binding.testDelegate(i);
              Assert.assertEquals(i+1, iRes);
          }
    }

    public void chapter04TestPointerBuffer(final Bindingtest1 binding) throws Exception {
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
    public void chapter05TestSomeFunctionsAllIndirect(final Bindingtest1 binding) throws Exception {
          int i;

          final IntBuffer ib = IntBuffer.allocate(3);
          ib.put(0, 1);
          ib.put(1, 2);
          ib.put(2, 3);

          final int[] iarray = new int[] { 1, 2, 3 };

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
              final IntBuffer dstB = IntBuffer.allocate(src.length);
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
              final IntBuffer dstB = IntBuffer.allocate(src2.length);
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

    public void chapter09TestCompoundAlignment(final Bindingtest1 binding) throws Exception {

        final MachineDataInfo.StaticConfig smd = MachineDataInfoRuntime.getStatic();
        final MachineDataInfo md = MachineDataInfoRuntime.getRuntime();

        System.err.println("static  md: "+smd);
        System.err.println("runtime md: "+md);
        System.err.println("compatible static/runtime: "+md.compatible(smd.md));

        // Test compound alignment read
        {
            final TK_ComplicatedSuperSet cs =  binding.createComplicatedSuperSet();
            Assert.assertEquals((byte)0xA0, cs.getBits1());

            final TK_ComplicatedSubSet sub1 =  cs.getSub1();
            Assert.assertEquals((byte)0xA1, sub1.getBits1());
            Assert.assertEquals(0x12345678, sub1.getId());
            Assert.assertEquals((byte)0xA2, sub1.getBits2());
            Assert.assertEquals(0x123456789abcdef0L, sub1.getLong0());
            Assert.assertEquals((byte)0xA3, sub1.getBits3());
            Assert.assertEquals(3.1415926535897932384626433832795, sub1.getReal0(), 0.0);
            Assert.assertEquals((byte)0xA4, sub1.getBits4());
            Assert.assertEquals(256.12345f, sub1.getReal1(), 0.0);
            Assert.assertEquals((byte)0xA5, sub1.getBits5());
            Assert.assertEquals(0xdeadbeefL, sub1.getLongX());
            Assert.assertEquals((byte)0xA6, sub1.getBits6());

            Assert.assertEquals((byte)0xB0, cs.getBits2());

            final TK_ComplicatedSubSet sub2 =  cs.getSub2();
            Assert.assertEquals((byte)0xB1, sub2.getBits1());
            Assert.assertEquals(0x12345678, sub2.getId());
            Assert.assertEquals((byte)0xB2, sub2.getBits2());
            Assert.assertEquals(0x123456789abcdef0L, sub2.getLong0());
            Assert.assertEquals((byte)0xB3, sub2.getBits3());
            Assert.assertEquals(3.1415926535897932384626433832795, sub2.getReal0(), 0.0);
            Assert.assertEquals((byte)0xB4, sub2.getBits4());
            Assert.assertEquals(256.12345f, sub2.getReal1(), 0.0);
            Assert.assertEquals((byte)0xB5, sub2.getBits5());
            Assert.assertEquals(0xdeadbeefL, sub2.getLongX());
            Assert.assertEquals((byte)0xB6, sub2.getBits6());

            Assert.assertEquals((byte)0xC0, cs.getBits3());

            binding.destroyComplicatedSuperSet(cs);
        }

        /********************************************************************************/

        // Test compound alignment write
        {
            final TK_ComplicatedSuperSet cs =  TK_ComplicatedSuperSet.create();
            cs.setBits1((byte)0xA0);

            final TK_ComplicatedSubSet sub1 =  cs.getSub1();
            sub1.setBits1((byte)0xA1);
            sub1.setId(0x12345678);
            sub1.setBits2((byte)0xA2);
            sub1.setLong0(0x123456789abcdef0L);
            sub1.setBits3((byte)0xA3);
            sub1.setReal0(3.1415926535897932384626433832795);
            sub1.setBits4((byte)0xA4);
            sub1.setReal1(256.12345f);
            sub1.setBits5((byte)0xA5);
            sub1.setLongX(0xdeadbeefL);
            sub1.setBits6((byte)0xA6);

            cs.setBits2((byte)0xB0);

            final TK_ComplicatedSubSet sub2 =  cs.getSub2();
            sub2.setBits1((byte)0xB1);
            sub2.setId(0x12345678);
            sub2.setBits2((byte)0xB2);
            sub2.setLong0(0x123456789abcdef0L);
            sub2.setBits3((byte)0xB3);
            sub2.setReal0(3.1415926535897932384626433832795);
            sub2.setBits4((byte)0xB4);
            sub2.setReal1(256.12345f);
            sub2.setBits5((byte)0xB5);
            sub2.setLongX(0xdeadbeefL);
            sub2.setBits6((byte)0xB6);

            cs.setBits3((byte)0xC0);

            Assert.assertTrue(binding.hasInitValues(cs));
        }
    }

    private static void dumpDim(final String pre, final TK_Dimension dim) {
        System.err.println(pre+dim.getX()+"/"+dim.getY()+" "+dim.getWidth()+"x"+dim.getHeight());
    }
    private static void assertDim(final String pre,
                                  final int expX, final int expY, final int expWidth, final int expHeight,
                                  final TK_Dimension hasDim) {
        dumpDim(pre, hasDim);
        Assert.assertEquals(expX, hasDim.getX());
        Assert.assertEquals(expY, hasDim.getY());
        Assert.assertEquals(expWidth, hasDim.getWidth());
        Assert.assertEquals(expHeight, hasDim.getHeight());
    }
    private static void dumpDim(final String pre, final int[] pos, final int size[]) {
        System.err.println(pre+pos[0]+"/"+pos[1]+" "+size[0]+"x"+size[1]);
    }
    private static void assertDim(final String pre,
                                  final int expX, final int expY, final int expWidth, final int expHeight,
                                  final int[] pos, final int size[]) {
        dumpDim(pre, pos, size);
        Assert.assertEquals(expX, pos[0]);
        Assert.assertEquals(expY, pos[1]);
        Assert.assertEquals(expWidth, size[0]);
        Assert.assertEquals(expHeight, size[1]);
    }

    /** Test compound access call-by-reference */
    public void chapter10TestCompoundCallByReference(final Bindingtest1 binding) throws Exception {

        final TK_Surface surface = binding.createSurface();

        final long surfaceContext = surface.getCtx();
        assertAPTR(0x123456789abcdef0L, surfaceContext);

        final TK_ContextWrapper ctxWrapper = surface.getCtxWrapper();
        final long wrapperContext = ctxWrapper.getCtx();
        assertAPTR(0xA23456781abcdef0L, wrapperContext);

        final TK_Engine engine = surface.getEngine();
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

        final TK_Dimension dimension = surface.getBounds();
        dumpDim("ch10: ref-dim ", dimension);
        Assert.assertEquals(0x11111111, dimension.getX());
        Assert.assertEquals(0x22222222, dimension.getY());
        Assert.assertEquals(0x33333333, dimension.getWidth());
        Assert.assertEquals(0x44444444, dimension.getHeight());

        Assert.assertEquals(2, surface.getClipSize());

        final TK_Dimension[] allclips = surface.getClips(0, new TK_Dimension[surface.getClipSize()]);

        for(int i=0; i<surface.getClipSize(); i++) {
            final TK_Dimension clip0 = surface.getClip(i);
            Assert.assertEquals(0x44444444 * (i+1) + 0x11111111, clip0.getX());
            Assert.assertEquals(0x44444444 * (i+1) + 0x22222222, clip0.getY());
            Assert.assertEquals(0x44444444 * (i+1) + 0x33333333, clip0.getWidth());
            Assert.assertEquals(0x44444444 * (i+1) + 0x44444444, clip0.getHeight());

            final TK_Dimension[] clip1 = new TK_Dimension[1];
            surface.getClips(i, clip1);
            Assert.assertEquals(0x44444444 * (i+1) + 0x11111111, clip1[0].getX());
            Assert.assertEquals(0x44444444 * (i+1) + 0x22222222, clip1[0].getY());
            Assert.assertEquals(0x44444444 * (i+1) + 0x33333333, clip1[0].getWidth());
            Assert.assertEquals(0x44444444 * (i+1) + 0x44444444, clip1[0].getHeight());

            Assert.assertEquals(0x44444444 * (i+1) + 0x11111111, allclips[i].getX());
            Assert.assertEquals(0x44444444 * (i+1) + 0x22222222, allclips[i].getY());
            Assert.assertEquals(0x44444444 * (i+1) + 0x33333333, allclips[i].getWidth());
            Assert.assertEquals(0x44444444 * (i+1) + 0x44444444, allclips[i].getHeight());
        }

        binding.destroySurface(surface);
    }

    /** Test compound access call-by-value */
    public void chapter11TestCompoundCallByValue(final Bindingtest1 binding) throws Exception {
        int sub = 0;
        {
            final TK_Surface surface = binding.createSurface();
            final TK_Dimension dim0 = surface.getBounds();
            assertDim("ch11."+sub+": ref-dim ", 0x11111111, 0x22222222, 0x33333333, 0x44444444, dim0);

            final TK_Dimension dim1 = binding.getSurfaceBoundsValue(surface);
            assertDim("ch11."+sub+": val-dim ", 0x11111111, 0x22222222, 0x33333333, 0x44444444, dim1);

            binding.destroySurface(surface);
        }
        {
            sub++;
            final TK_Dimension dim0 = binding.getBoundsValue(11, 22, 33, 44);
            assertDim("ch11."+sub+": val-dim ", 11, 22, 33, 44, dim0);
            sub++;
            final TK_Surface surface = binding.getSurfaceValue(dim0);
            final TK_Dimension dim1 = binding.getSurfaceBoundsValue(surface);
            assertDim("ch11."+sub+": val-dim ", 11, 22, 33, 44, dim1);
            sub++;
            final boolean sameInstanceByVal = binding.isSameInstanceByVal(dim0, dim1);
            final boolean sameInstanceByRef = binding.isSameInstanceByRef(dim0, dim1);
            System.err.println("ch11."+sub+": sameInstanceByVal "+sameInstanceByVal);
            System.err.println("ch11."+sub+": sameInstanceByRef "+sameInstanceByRef);
            Assert.assertFalse(sameInstanceByVal);
            Assert.assertFalse(sameInstanceByRef);
        }
        {
            final TK_Dimension dim1 = binding.getBoundsValue(11, 22, 33, 44);
            final TK_Dimension dim2 = binding.getBoundsValue(1, 2, 3, 4);
            final TK_Dimension[] sumands = { dim1, dim2 };
            {
                sub++;
                final TK_Dimension dimSum = binding.addDimensions(sumands);
                assertDim("ch11."+sub+": sum-dimArray ", 11+1, 22+2, 33+3, 44+4, dimSum);
            }

            final TK_DimensionPair dimPair = TK_DimensionPair.create();
            dimPair.setPair(0, sumands);
            {
                sub++;
                final TK_Dimension[] dimsGet = dimPair.getPair(0, new TK_Dimension[2]);
                assertDim("ch11."+sub+": dimsGet[0] ", 11, 22, 33, 44, dimsGet[0]);
                assertDim("ch11."+sub+": dimsGet[1] ",  1,  2,  3,  4, dimsGet[1]);
            }
            {
                sub++;
                final TK_Dimension dimSum = binding.addDimensionPair(dimPair);
                assertDim("ch11."+sub+": sum-dimPair ", 11+1, 22+2, 33+3, 44+4, dimSum);
            }
            {
                sub++;
                binding.zeroDimensions(sumands);
                assertDim("ch11."+sub+": zero-dim[0] ", 0, 0, 0, 0, sumands[0]);
                assertDim("ch11."+sub+": zero-dim[1] ", 0, 0, 0, 0, sumands[1]);
            }
        }
        {
            sub++;
            final TK_Dimension dim0 = binding.getBoundsValue(0, 0, 0, 0);
            final TK_Dimension[] dim0A = { dim0 };
            binding.copyPrimToDimensions(new int[] { 11,  22}, 0, new int[] { 100, 200}, 0, dim0A);
            assertDim("ch11."+sub+": copyPrim2Dim ", 11, 22, 100, 200, dim0);

            sub++;
            final int[] pos = { 0, 0 };
            final int[] size = { 0, 0 };
            binding.copyDimensionsToPrim(dim0, pos, 0, size, 0);
            assertDim("ch11."+sub+": copyDim2Prim ", 11, 22, 100, 200, pos, size);
        }
        {
            sub++;
            final int expRGBAi = 0x112233aa;
            final byte[] expRGBAb = { (byte)0xaa, 0x33, 0x22, 0x11 };
            final int hasRGBAi = binding.rgbaToInt(expRGBAb, 0);
            System.err.println("ch11."+sub+": expRGBAb 0x"+
                    Integer.toHexString(expRGBAb[3])+", 0x"+
                    Integer.toHexString(expRGBAb[2])+", 0x"+
                    Integer.toHexString(expRGBAb[1])+", 0x"+
                    Integer.toHexString(expRGBAb[0]) );
            System.err.println("ch11."+sub+": hasRGBAi 0x"+Integer.toHexString(hasRGBAi));
            Assert.assertEquals(expRGBAi, hasRGBAi);

            sub++;
            final byte[] hasRGBAb = new byte[] { 0, 0, 0, 0 };
            binding.intToRgba(hasRGBAi, hasRGBAb, 0);
            System.err.println("ch11."+sub+": hasRGBAb 0x"+
                    Integer.toHexString(hasRGBAb[3])+", 0x"+
                    Integer.toHexString(hasRGBAb[2])+", 0x"+
                    Integer.toHexString(hasRGBAb[1])+", 0x"+
                    Integer.toHexString(hasRGBAb[0]) );
            Assert.assertArrayEquals(expRGBAb, hasRGBAb);
        }
        {
            sub++;
            final int[] result = { 0 };
            binding.addInt(new int[] { 1,  2}, 0, result, 0);
            System.err.println("ch11."+sub+": addInt "+result[0]);
            Assert.assertEquals(3, result[0]);
        }
        {
            sub++;
            final byte[] result = { 0 };
            binding.addByte(new byte[] { 1,  2}, 0, result, 0);
            System.err.println("ch11."+sub+": addByte "+result[0]);
            Assert.assertEquals(3, result[0]);
        }
    }

    /** Test array and pointer bindings of structs  */
    public void chapter12TestStructArrayModelConst(final Bindingtest1 binding) throws Exception {
        final TK_ModelConst model = binding.createModelConst();

        Assert.assertEquals(3, model.getIntxxPointerCustomLenVal());
        Assert.assertEquals(3, model.getInt32PointerCustomLenVal());
        Assert.assertEquals(3, TK_ModelConst.getInt32ArrayFixedLenArrayLength());
        Assert.assertEquals(3, TK_ModelConst.getStructArrayFixedLenArrayLength());
        Assert.assertEquals(3, model.getStructPointerCustomLenVal());

        // field: int32ArrayFixedLen
        //        CType['int32_t *', size [fixed false, lnx64 12], [array*1]], with array length of 3
        {
            final int size = TK_ModelConst.getInt32ArrayFixedLenArrayLength();
            final int[] all = model.getInt32ArrayFixedLen(0, new int[size]);
            final IntBuffer allB = model.getInt32ArrayFixedLen();
            Assert.assertEquals(size, allB.limit());
            for(int i=0; i<size; i++) {
                Assert.assertEquals(21 + i, all[i]);
                Assert.assertEquals(21 + i, allB.get(i));
                final int[] s = model.getInt32ArrayFixedLen(i, new int[1]);
                Assert.assertEquals(21 + i, s[0]);
            }
        }

        // field: int32ArrayOneElem
        //        CType['int32_t *', size [fixed false, lnx64 4], [array*1]], with array length of 1
        {
            Assert.assertEquals(30, model.getInt32ArrayOneElem());
        }

        // field: int32PointerCustomLen
        //        field: CType['int32_t *', size [fixed false, lnx64 8], [pointer*1]], with array length of getInt32PointerCustomLenVal()
        {
            final int size = model.getInt32PointerCustomLenVal();
            final IntBuffer all = model.getInt32PointerCustomLen();
            Assert.assertEquals(size, all.limit());
            for(int i=0; i<size; i++) {
                Assert.assertEquals(31 + i, all.get(i));
            }
        }

        // field: int32PointerOneElem
        //        CType['int32_t *', size [fixed false, lnx64 8], [pointer*1]], with array length of 1
        {
            final IntBuffer all = model.getInt32PointerOneElem();
            Assert.assertEquals(1, all.limit());
            Assert.assertEquals(41, all.get(0));
        }

        // field: mat4x4
        //        CType['float * *', size [fixed false, lnx64 64], [array*2]], with array length of <code>4*4</code> */
        {
            Assert.assertEquals(4*4, TK_ModelConst.getMat4x4ArrayLength());
            final FloatBuffer mat4x4 = model.getMat4x4();
            Assert.assertEquals(4*4, mat4x4.limit());
            for(int i=0; i<4; i++) {
                final float[] vec4 = model.getMat4x4(i*4, new float[4]);
                for(int j=0; j<4; j++) {
                    Assert.assertEquals(i*4+j, mat4x4.get(i*4+j), EPSILON);
                    Assert.assertEquals(i*4+j, vec4[j], EPSILON);
                }
            }
        }

        // field: structArrayFixedLen
        //        field: CType['TK_Dimension *', size [fixed false, lnx64 48], [array*1]], with array length of 3
        {
            final int size = TK_ModelConst.getStructArrayFixedLenArrayLength();
            final TK_Dimension[] all = model.getStructArrayFixedLen(0, new TK_Dimension[size]);
            for(int i=0; i<size; i++) {
                Assert.assertEquals(51 + i * 10, all[i].getX());
                Assert.assertEquals(52 + i * 10, all[i].getY());
                Assert.assertEquals(53 + i * 10, all[i].getWidth());
                Assert.assertEquals(54 + i * 10, all[i].getHeight());
            }
        }

        // field: structArrayOneElem
        //        CType['TK_Dimension *', size [fixed false, lnx64 16], [array*1]], with array length of 1
        {
            final TK_Dimension all = model.getStructArrayOneElem();
            Assert.assertEquals(81, all.getX());
            Assert.assertEquals(82, all.getY());
            Assert.assertEquals(83, all.getWidth());
            Assert.assertEquals(84, all.getHeight());
        }

        // field: structPointerCustomLen
        //        CType['TK_Dimension *', size [fixed false, lnx64 8], [pointer*1]], with array length of getStructPointerCustomLenVal()
        {
            final int size = model.getStructPointerCustomLenVal();
            final TK_Dimension[] all = model.getStructPointerCustomLen(0, new TK_Dimension[size]);
            for(int i=0; i<size; i++) {
                Assert.assertEquals(91 + i * 10, all[i].getX());
                Assert.assertEquals(92 + i * 10, all[i].getY());
                Assert.assertEquals(93 + i * 10, all[i].getWidth());
                Assert.assertEquals(94 + i * 10, all[i].getHeight());
            }
        }

        // field: structPointerOneElem
        //        CType['TK_Dimension *', size [fixed false, lnx64 8], [pointer*1]], with array length of 1
        {
            final TK_Dimension all = model.getStructPointerOneElem();
            Assert.assertEquals(121, all.getX());
            Assert.assertEquals(122, all.getY());
            Assert.assertEquals(123, all.getWidth());
            Assert.assertEquals(124, all.getHeight());

        }

        final long surfaceContext = model.getCtx();
        assertAPTR(0x123456789abcdef0L, surfaceContext);

        model.setCtx(surfaceContext);
        assertAPTR(surfaceContext, model.getCtx());

        {
            Assert.assertEquals(12, TK_ModelConst.getModelNameArrayFixedLenArrayLength());

            final ByteBuffer bb = model.getModelNameArrayFixedLen();
            Assert.assertEquals(12, bb.limit());

            final String exp = "Hello Array";
            final String has = model.getModelNameArrayFixedLenAsString();
            // System.err.println("exp '"+exp+"'");
            System.err.println("has '"+has+"'");
            // dumpStringChars("exp", exp);
            dumpStringChars("has", has);
            Assert.assertEquals(11, has.length()); // w/o EOS
            Assert.assertEquals(exp, has);
        }
        {
            Assert.assertEquals(14, model.getModelNamePointerCStringArrayLength());

            final ByteBuffer bb = model.getModelNamePointerCString();
            Assert.assertEquals(14, bb.limit());

            final String exp = "Hello CString";
            final String has = model.getModelNamePointerCStringAsString();
            // System.err.println("exp '"+exp+"'");
            System.err.println("has '"+has+"'");
            // dumpStringChars("exp", exp);
            dumpStringChars("has", has);
            Assert.assertEquals(13, has.length()); // w/o EOS
            Assert.assertEquals(exp, has);
        }
        {
            Assert.assertEquals(14, model.getModelNamePointerCustomLenVal());

            final ByteBuffer bb = model.getModelNamePointerCustomLen();
            Assert.assertEquals(14, bb.limit());

            final String exp = "Hello Pointer";
            final String has = model.getModelNamePointerCustomLenAsString();
            // System.err.println("exp '"+exp+"'");
            System.err.println("has '"+has+"'");
            // dumpStringChars("exp", exp);
            dumpStringChars("has", has);
            Assert.assertEquals(13, has.length()); // w/o EOS
            Assert.assertEquals(exp, has);
        }

        binding.destroyModelConst(model);
    }
    private void dumpStringChars(final String prefix, final String s) {
        final int len = s.length();
        for(int i=0; i<len; i++) {
            final char c = s.charAt(i);
            System.err.printf("%s %3d: 0x%X %c%n", prefix, i, (int)c, c);
        }
    }

    public void chapter13TestStructArrayModelMutable(final Bindingtest1 binding) throws Exception {
        final TK_ModelMutable model = binding.createModelMutable();

        Assert.assertEquals(3, model.getIntxxPointerCustomLenVal());
        Assert.assertEquals(3, model.getInt32PointerCustomLenVal());
        Assert.assertEquals(3, TK_ModelMutable.getInt32ArrayFixedLenArrayLength());
        Assert.assertEquals(3, TK_ModelMutable.getStructArrayFixedLenArrayLength());
        Assert.assertEquals(3, model.getStructPointerCustomLenVal());

        // field: int32ArrayFixedLen
        //        CType['int32_t *', size [fixed false, lnx64 12], [array*1]], with array length of 3
        {
            final int size = TK_ModelMutable.getInt32ArrayFixedLenArrayLength();
            {
                final int[] values = new int[] { 1, 2, 3 };
                model.setInt32ArrayFixedLen(0, values);

                final int[] all = model.getInt32ArrayFixedLen(0, new int[size]);
                final IntBuffer allB = model.getInt32ArrayFixedLen();
                Assert.assertEquals(size, allB.limit());
                for(int i=0; i<size; i++) {
                    Assert.assertEquals(1 + i, all[i]);
                    Assert.assertEquals(1 + i, allB.get(i));
                    final int[] s = model.getInt32ArrayFixedLen(i, new int[1]);
                    Assert.assertEquals(1 + i, s[0]);
                }
            }
            {
                for(int i=0; i<size; i++) {
                    final int[] ia = new int[] { 4 + i };
                    model.setInt32ArrayFixedLen(i, ia);
                }

                final int[] all = model.getInt32ArrayFixedLen(0, new int[size]);
                final IntBuffer allB = model.getInt32ArrayFixedLen();
                Assert.assertEquals(size, allB.limit());
                for(int i=0; i<size; i++) {
                    Assert.assertEquals(4 + i, all[i]);
                    Assert.assertEquals(4 + i, allB.get(i));
                    final int[] s = model.getInt32ArrayFixedLen(i, new int[1]);
                    Assert.assertEquals(4 + i, s[0]);
                }
            }
        }

        // field: int32ArrayOneElem
        //        CType['int32_t *', size [fixed false, lnx64 4], [array*1]], with array length of 1
        {
            model.setInt32ArrayOneElem(1);
            Assert.assertEquals(1, model.getInt32ArrayOneElem());
        }

        // field: int32PointerCustomLen
        //        field: CType['int32_t *', size [fixed false, lnx64 8], [pointer*1]], with array length of getInt32PointerCustomLenVal()
        {
            final int size = model.getInt32PointerCustomLenVal();
            {
                final IntBuffer all0 = model.getInt32PointerCustomLen();
                Assert.assertEquals(size, all0.limit());
                for(int i=0; i<size; i++) {
                    all0.put(i, 1+i);
                }

                final IntBuffer all1 = model.getInt32PointerCustomLen();
                Assert.assertEquals(size, all1.limit());
                for(int i=0; i<size; i++) {
                    Assert.assertEquals(1 + i, all1.get(i));
                }
            }
        }

        // field: int32PointerOneElem
        //        CType['int32_t *', size [fixed false, lnx64 8], [pointer*1]], with array length of 1
        {
            {
                final IntBuffer one0 = model.getInt32PointerOneElem();
                Assert.assertEquals(1, one0.limit());
                one0.put(0, 1);

                final IntBuffer one1 = model.getInt32PointerOneElem();
                Assert.assertEquals(1, one1.limit());
                Assert.assertEquals(1, one1.get(0));
            }
        }


        // field: mat4x4
        //        CType['float * *', size [fixed false, lnx64 64], [array*2]], with array length of <code>4*4</code> */
        {
            model.setMat4x4(0*4, new float[] { 11, 12, 13, 14 } );
            model.setMat4x4(1*4, new float[] { 21, 22, 23, 24 } );
            model.setMat4x4(2*4, new float[] { 31, 32, 33, 34 } );
            model.setMat4x4(3*4, new float[] { 41, 42, 43, 44 } );

            Assert.assertEquals(4*4, TK_ModelMutable.getMat4x4ArrayLength());
            final FloatBuffer mat4x4 = model.getMat4x4();
            Assert.assertEquals(4*4, mat4x4.limit());
            for(int i=0; i<4; i++) {
                final float[] vec4 = model.getMat4x4(i*4, new float[4]);
                for(int j=0; j<4; j++) {
                    Assert.assertEquals((i+1)*10+(j+1), mat4x4.get(i*4+j), EPSILON);
                    Assert.assertEquals((i+1)*10+(j+1), vec4[j], EPSILON);
                }
            }
        }

        // field: structArrayFixedLen
        //        field: CType['TK_Dimension *', size [fixed false, lnx64 48], [array*1]], with array length of 3
        {
            final int size = TK_ModelMutable.getStructArrayFixedLenArrayLength();
            {
                for(int i=0; i<size; i++) {
                    final TK_Dimension d = TK_Dimension.create();
                    d.setX(1+i*10);
                    d.setY(2+i*10);
                    d.setWidth(3+i*10);
                    d.setHeight(4+i*10);
                    model.setStructArrayFixedLen(i, d);
                }
                final TK_Dimension[] all = model.getStructArrayFixedLen(0, new TK_Dimension[size]);
                for(int i=0; i<size; i++) {
                    Assert.assertEquals(1 + i * 10, all[i].getX());
                    Assert.assertEquals(2 + i * 10, all[i].getY());
                    Assert.assertEquals(3 + i * 10, all[i].getWidth());
                    Assert.assertEquals(4 + i * 10, all[i].getHeight());
                    final TK_Dimension[] one = model.getStructArrayFixedLen(i, new TK_Dimension[1]);
                    Assert.assertEquals(1 + i * 10, one[0].getX());
                    Assert.assertEquals(2 + i * 10, one[0].getY());
                    Assert.assertEquals(3 + i * 10, one[0].getWidth());
                    Assert.assertEquals(4 + i * 10, one[0].getHeight());
                }
            }
            {
                final TK_Dimension[] da = new TK_Dimension[size];
                for(int i=0; i<size; i++) {
                    final TK_Dimension d = TK_Dimension.create();
                    d.setX(5+i*10);
                    d.setY(6+i*10);
                    d.setWidth(7+i*10);
                    d.setHeight(8+i*10);
                    da[i] = d;
                }
                model.setStructArrayFixedLen(0, da);

                final TK_Dimension[] all = model.getStructArrayFixedLen(0, new TK_Dimension[size]);
                for(int i=0; i<size; i++) {
                    Assert.assertEquals(5 + i * 10, all[i].getX());
                    Assert.assertEquals(6 + i * 10, all[i].getY());
                    Assert.assertEquals(7 + i * 10, all[i].getWidth());
                    Assert.assertEquals(8 + i * 10, all[i].getHeight());
                    final TK_Dimension[] one = model.getStructArrayFixedLen(i, new TK_Dimension[1]);
                    Assert.assertEquals(5 + i * 10, one[0].getX());
                    Assert.assertEquals(6 + i * 10, one[0].getY());
                    Assert.assertEquals(7 + i * 10, one[0].getWidth());
                    Assert.assertEquals(8 + i * 10, one[0].getHeight());
                }
            }
            {
                for(int i=0; i<size; i++) {
                    final TK_Dimension d = TK_Dimension.create();
                    d.setX(1+i*10);
                    d.setY(3+i*10);
                    d.setWidth(5+i*10);
                    d.setHeight(7+i*10);
                    model.setStructArrayFixedLen(i, new TK_Dimension[] { d });
                }

                final TK_Dimension[] all = model.getStructArrayFixedLen(0, new TK_Dimension[size]);
                for(int i=0; i<size; i++) {
                    Assert.assertEquals(1 + i * 10, all[i].getX());
                    Assert.assertEquals(3 + i * 10, all[i].getY());
                    Assert.assertEquals(5 + i * 10, all[i].getWidth());
                    Assert.assertEquals(7 + i * 10, all[i].getHeight());
                    final TK_Dimension[] one = model.getStructArrayFixedLen(i, new TK_Dimension[1]);
                    Assert.assertEquals(1 + i * 10, one[0].getX());
                    Assert.assertEquals(3 + i * 10, one[0].getY());
                    Assert.assertEquals(5 + i * 10, one[0].getWidth());
                    Assert.assertEquals(7 + i * 10, one[0].getHeight());
                }
            }
        }

        // field: structArrayOneElem
        //        CType['TK_Dimension *', size [fixed false, lnx64 16], [array*1]], with array length of 1
        {
            {
                final TK_Dimension d = TK_Dimension.create();
                d.setX(1);
                d.setY(2);
                d.setWidth(3);
                d.setHeight(4);
                model.setStructArrayOneElem(d);
            }
            {
                final TK_Dimension one = model.getStructArrayOneElem();
                Assert.assertEquals(1, one.getX());
                Assert.assertEquals(2, one.getY());
                Assert.assertEquals(3, one.getWidth());
                Assert.assertEquals(4, one.getHeight());
            }
        }

        // field: structPointerCustomLen
        //        CType['TK_Dimension *', size [fixed false, lnx64 8], [pointer*1]], with array length of getStructPointerCustomLenVal()
        {
            final int size = model.getStructPointerCustomLenVal();
            {
                final TK_Dimension[] all = model.getStructPointerCustomLen(0, new TK_Dimension[size]);
                for(int i=0; i<size; i++) {
                    final TK_Dimension d = all[i];
                    d.setX(1+i*10);
                    d.setY(2+i*10);
                    d.setWidth(3+i*10);
                    d.setHeight(4+i*10);
                }
            }
            {
                final TK_Dimension[] all = model.getStructPointerCustomLen(0, new TK_Dimension[size]);
                for(int i=0; i<size; i++) {
                    Assert.assertEquals(1 + i * 10, all[i].getX());
                    Assert.assertEquals(2 + i * 10, all[i].getY());
                    Assert.assertEquals(3 + i * 10, all[i].getWidth());
                    Assert.assertEquals(4 + i * 10, all[i].getHeight());
                    final TK_Dimension[] one = model.getStructPointerCustomLen(i, new TK_Dimension[1]);
                    Assert.assertEquals(1 + i * 10, one[0].getX());
                    Assert.assertEquals(2 + i * 10, one[0].getY());
                    Assert.assertEquals(3 + i * 10, one[0].getWidth());
                    Assert.assertEquals(4 + i * 10, one[0].getHeight());
                }
            }
        }

        // field: structPointerOneElem
        //        CType['TK_Dimension *', size [fixed false, lnx64 8], [pointer*1]], with array length of 1
        {
            {
                final TK_Dimension d = model.getStructPointerOneElem();
                d.setX(1);
                d.setY(2);
                d.setWidth(3);
                d.setHeight(4);
            }
            {
                final TK_Dimension one = model.getStructPointerOneElem();
                Assert.assertEquals(1, one.getX());
                Assert.assertEquals(2, one.getY());
                Assert.assertEquals(3, one.getWidth());
                Assert.assertEquals(4, one.getHeight());
            }

        }

        final long surfaceContext = model.getCtx();
        assertAPTR(0x123456789abcdef0L, surfaceContext);

        model.setCtx(surfaceContext);
        assertAPTR(surfaceContext, model.getCtx());

        binding.destroyModelMutable(model);
    }
}
