/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Sven Gothel or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.gluegen.test.junit;

import com.jogamp.gluegen.runtime.BufferFactory;
import com.jogamp.gluegen.runtime.PointerBuffer;
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
import static com.jogamp.gluegen.test.junit.BuildEnvironment.*;

/**
 * @author Michael Bien
 * @author Sven Gothel
 */
public class BaseTest1 {

    /**
     * Verifies the existence and creation of the generated class.
     */
    public void testClassExist(String name) throws Exception {
        String ifName = "com.jogamp.gluegen.test.junit.Binding"+name;
        String implName = "com.jogamp.gluegen.test.junit.impl.Binding"+name+"Impl";

        Class<?> clazzIf   = Class.forName(ifName);
        Class<?> clazzImpl = Class.forName(implName);

        Assert.assertNotNull(ifName+" does not exist", clazzIf);
        Assert.assertNotNull(implName+" does not exist", clazzImpl);

        Assert.assertEquals((int)1,  clazzIf.getDeclaredField("CONSTANT_ONE").get(null));

        Object obj = clazzImpl.newInstance();
        Assert.assertTrue("Not of type "+ifName, clazzIf.isAssignableFrom(obj.getClass()));
        Assert.assertTrue("Not of type com.jogamp.gluegen.test.junit.BindingTest1", (obj instanceof com.jogamp.gluegen.test.junit.BindingTest1));
    }

    /**
     * Verifies if all generated method signatures are completed,
     * ie a compilation only coverage test without functional tests.
     */
    public void chapter__TestCoverageSignature(BindingTest1 binding) throws Exception {
          int i;
          long result;
          long context = 0;
          ByteBuffer bb=null;
          PointerBuffer pb=null;
          LongBuffer lb=null;
          long[] larray = null;
          int array_offset = 0;
          String str=null;
          String[] strings = null;
          IntBuffer ib = null;
          int[] iarray = null;
          int iarray_offset = 0;

          result = binding.arrayTest(context, pb);
          result = binding.arrayTest(context, larray, array_offset);
          result = binding.arrayTestNioOnly(context, pb);

          result = binding.bufferTest(bb);
          result = binding.bufferTestNioOnly(bb);

          result = binding.doubleTest(context, bb, pb, bb, pb);
          result = binding.doubleTest(context, bb, larray, array_offset, bb, larray, array_offset);
          result = binding.doubleTestNioOnly(context, bb, pb, bb, pb);

          result = binding.mixedTest(context, bb, pb);
          result = binding.mixedTest(context, bb, larray, array_offset);
          result = binding.mixedTestNioOnly(context, bb, pb);

          result = binding.nopTest();

          i = binding.strToInt(str);
          str = binding.intToStr(i);

          i = binding.stringArrayRead(strings, i);

          i = binding.intArrayRead(ib, i);
          i = binding.intArrayRead(iarray, iarray_offset, i);

    }

    /**
     * Verifies if all methods / signatures are properly generated,
     * can be invoked and functions.
     * This is a compilation (coverage) and runtime time (semantic) test.
     * This covers indirect primitive arrays and direct NIO buffers.
     */
    public void chapter03TestCoverageFunctionalityDirectNIOAndPrimitiveArray(BindingTest1 binding) throws Exception {
          int i;
          long result;

          long context = 1;
          ByteBuffer bb1 = BufferFactory.newDirectByteBuffer(BufferFactory.SIZEOF_LONG);
          LongBuffer bb1L = bb1.asLongBuffer();
          bb1L.put(0,  10);

          ByteBuffer bb2 = BufferFactory.newDirectByteBuffer(BufferFactory.SIZEOF_LONG);
          LongBuffer bb2L = bb2.asLongBuffer();
          bb2L.put(0, 100);

          PointerBuffer pb1 = PointerBuffer.allocateDirect(BindingTest1.ARRAY_SIZE);
          for(i=0; i<BindingTest1.ARRAY_SIZE; i++) {
            pb1.put(i,  1000);
          }
          PointerBuffer pb2 = PointerBuffer.allocateDirect(BindingTest1.ARRAY_SIZE);
          for(i=0; i<BindingTest1.ARRAY_SIZE; i++) {
            pb2.put(i, 10000);
          }

          long[] larray1 = new long[BindingTest1.ARRAY_SIZE];
          int array1_offset = 0;
          for(i=0; i<BindingTest1.ARRAY_SIZE; i++) {
            larray1[i]=  1000;
          }

          long[] larray2 = new long[BindingTest1.ARRAY_SIZE];
          int array2_offset = 0;
          for(i=0; i<BindingTest1.ARRAY_SIZE; i++) {
            larray2[i]= 10000;
          }

          result = binding.arrayTest(context, pb1);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          result = binding.arrayTest(context, larray1, array1_offset);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          result = binding.arrayTestNioOnly(context, pb1);
          Assert.assertTrue("Wrong result: "+result, 1+8000==result);

          result = binding.bufferTest(bb1);
          Assert.assertTrue("Wrong result: "+result, 10==result);

          result = binding.bufferTestNioOnly(bb1);
          Assert.assertTrue("Wrong result: "+result, 10==result);

          result = binding.doubleTest(context, bb1, pb1, bb2, pb2);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000+100+80000==result);

          result = binding.doubleTest(context, bb1, larray1, array1_offset, bb2, larray2, array2_offset);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000+100+80000==result);

          result = binding.doubleTestNioOnly(context, bb1, pb1, bb2, pb2);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000+100+80000==result);

          result = binding.mixedTest(context, bb1, pb1);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000==result);

          result = binding.mixedTest(context, bb1, larray1, array1_offset);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000==result);

          result = binding.mixedTestNioOnly(context, bb1, pb1);
          Assert.assertTrue("Wrong result: "+result, 1+10+8000==result);

          result = binding.nopTest();
          Assert.assertTrue("Wrong result: "+result, 42==result);

          i = binding.strToInt("42");
          Assert.assertTrue("Wrong result: "+i, 42==i);

          String str = binding.intToStr(42);
          Assert.assertTrue("Wrong result: "+str, str.equals("42"));

          i = binding.stringArrayRead(new String[] { "1234", "5678", "9a" }, 3);
          Assert.assertTrue("Wrong result: "+i, 10==i);

          ByteBuffer bb3 = BufferFactory.newDirectByteBuffer(BufferFactory.SIZEOF_INT * 3);
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
    }

    /**
     * This covers indirect primitive arrays and indirect NIO buffers.
     */
    public void chapter04TestSomeFunctionsAllIndirect(BindingTest1 binding) throws Exception {
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
