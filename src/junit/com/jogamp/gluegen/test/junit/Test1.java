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

import com.jogamp.gluegen.test.junit.impl.BindingTest1Impl;

import com.sun.gluegen.runtime.BufferFactory;
import com.sun.gluegen.runtime.PointerBuffer;
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
public class Test1 {

    /**
     * Verifies loading of the new library.
     */
    @Test
    public void chapter01TestLoadLibrary() throws Exception {
        String nativesPath = testOutput + "/build/natives";
        System.load(nativesPath + "/libtest1.so");
    }

    /**
     * Verifies the existence and creation of the generated class.
     */
    @Test
    public void chapter01TestClassExist() throws Exception {
        Class<?> clazzIf   = Class.forName("com.jogamp.gluegen.test.junit.BindingTest1");
        Class<?> clazzImpl = Class.forName("com.jogamp.gluegen.test.junit.impl.BindingTest1Impl");

        Assert.assertNotNull("com.jogamp.gluegen.test.junit.BindingTest1 does not exist", clazzIf);
        Assert.assertNotNull("com.jogamp.gluegen.test.junit.impl.BindingTest1Impl does not exist", clazzImpl);

        Assert.assertEquals((int)1,  clazzIf.getDeclaredField("CONSTANT_ONE").get(null));

        Object obj = clazzImpl.newInstance();
        Assert.assertTrue("Not of type com.jogamp.gluegen.test.junit.BindingTest1", (obj instanceof com.jogamp.gluegen.test.junit.BindingTest1));

        BindingTest1 bindingTest1 = (BindingTest1) obj;
        Assert.assertTrue("nopTest1 failed", 42==bindingTest1.nopTest());
    }

    /**
     * Verifies if all methods / signatures are properly generated,
     * and can be invoked.
     * Gluegen Coverage test.
     * This is a compilation and runtime time test.
     */
    @Test
    public void chapter01TestCoverage() throws Exception {
          int dummy;

          int array_size = 10;
          long context = 0;
          ByteBuffer bb1 = BufferFactory.newDirectByteBuffer(PointerBuffer.elementSize() * array_size);
          ByteBuffer bb2 = BufferFactory.newDirectByteBuffer(PointerBuffer.elementSize() * array_size);

          PointerBuffer pb1 = PointerBuffer.allocateDirect(array_size);
          PointerBuffer pb2 = PointerBuffer.allocateDirect(array_size);

          long[] larray1 = new long[array_size];
          int array1_offset = 0;

          long[] larray2 = new long[array_size];
          int array2_offset = 0;

          BindingTest1 binding = new BindingTest1Impl();

          /** Interface to C language function: <br> <code> int arrayTest(long context, foo *  array); </code>    */
          dummy = binding.arrayTest(context, pb1);
          Assert.assertTrue(42==dummy);

          /** Interface to C language function: <br> <code> int arrayTest(long context, foo *  array); </code>    */
          dummy = binding.arrayTest(context, larray1, array1_offset);
          Assert.assertTrue(42==dummy);

          /** Interface to C language function: <br> <code> int bufferTest(void *  object); </code>    */
          dummy = binding.bufferTest(bb1);
          Assert.assertTrue(42==dummy);

          /** Interface to C language function: <br> <code> int doubleTest(long context, void *  object1, foo *  array1, void *  object2, foo *  array2); </code>    */
          dummy = binding.doubleTest(context, bb1, pb1, bb2, pb2);
          Assert.assertTrue(42==dummy);

          /** Interface to C language function: <br> <code> int doubleTest(long context, void *  object1, foo *  array1, void *  object2, foo *  array2); </code>    */
          dummy = binding.doubleTest(context, bb1, larray1, array1_offset, bb2, larray2, array2_offset);
          Assert.assertTrue(42==dummy);

          /** Interface to C language function: <br> <code> int mixedTest(long context, void *  object, foo *  array); </code>    */
          dummy = binding.mixedTest(context, bb1, pb1);
          Assert.assertTrue(42==dummy);

          /** Interface to C language function: <br> <code> int mixedTest(long context, void *  object, foo *  array); </code>    */
          dummy = binding.mixedTest(context, bb1, larray1, array1_offset);
          Assert.assertTrue(42==dummy);

          /** Interface to C language function: <br> <code> int nopTest(); </code>    */
          dummy = binding.nopTest();
          Assert.assertTrue(42==dummy);
    }

}
