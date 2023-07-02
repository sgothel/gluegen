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

import java.io.IOException;

import com.jogamp.gluegen.test.junit.generation.impl.Bindingtest2Impl;
import com.jogamp.common.os.NativeLibrary;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test {@link Bindingtest2} with {@link T2_InitializeOptions} instance and function pointer...
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Test2FuncPtr extends BaseClass {

    static NativeLibrary dynamicLookupHelper;

    /**
     * Verifies loading of the new library.
     */
    @BeforeClass
    public static void chapter__TestLoadLibrary() throws Exception {
        BindingJNILibLoader.loadBindingtest2();
        dynamicLookupHelper = NativeLibrary.open("test2", false, false, Test2FuncPtr.class.getClassLoader(), true);
        Assert.assertNotNull("NativeLibrary.open(test2) failed", dynamicLookupHelper);

        Bindingtest2Impl.resetProcAddressTable(dynamicLookupHelper);
    }

    /**
     * Verifies unloading of the new library.
     */
    @AfterClass
    public static void chapter0XTestUnloadLibrary() throws Exception {
        Assert.assertNotNull(dynamicLookupHelper);
        dynamicLookupHelper.close();
        dynamicLookupHelper = null;
    }

    /**
     * Test Bindingtest2 with T2_InitializeOptions instance and function pointer
     */
    @Test
    public void chapter01() throws Exception {
        final Bindingtest2 bt2 = new Bindingtest2Impl();

        final T2_InitializeOptions options = T2_InitializeOptions.create();
        Assert.assertEquals(true, options.isOverrideThreadAffinityNull());
        Assert.assertEquals(true, options.isProductNameNull());
        Assert.assertEquals(true, options.isProductVersionNull());
        Assert.assertEquals(0, options.getCustomFuncA1());
        Assert.assertEquals(0, options.getCustomFuncA2());
        Assert.assertEquals(0, options.getCustomFuncB1());
        Assert.assertEquals(0, options.getCustomFuncB2());

        bt2.Initialize(options);
        Assert.assertEquals(true, options.isOverrideThreadAffinityNull());
        Assert.assertEquals(false, options.isProductNameNull());
        Assert.assertEquals(false, options.isProductVersionNull());
        Assert.assertNotEquals(0, options.getCustomFuncA1());
        Assert.assertNotEquals(0, options.getCustomFuncA2());
        Assert.assertNotEquals(0, options.getCustomFuncB1());
        Assert.assertNotEquals(0, options.getCustomFuncB2());
        Assert.assertEquals(1, options.getApiVersion());
        // dropped: Assert.assertEquals(1, T2_InitializeOptions.getReserved1ElemCount());
        BaseClass.assertAPTR(0x0000CAFFEEBEEFL, options.getReserved1());
        {
            options.setReserved1(0xBEEFCAFFEE0DADL);
            BaseClass.assertAPTR(0xBEEFCAFFEE0DADL, options.getReserved1());
        }
        Assert.assertEquals("Product Name", options.getProductName());
        Assert.assertEquals("Product Version", options.getProductVersion());
        Assert.assertEquals(0xa001, options.CustomFuncA1(0));
        Assert.assertEquals(0xa002, options.CustomFuncA2(0));
        final T2_UserData ud1 = T2_UserData.create();
        {
            ud1.setBalance(101);
            ud1.setName("John Doe");
            Assert.assertEquals(101, ud1.getBalance());
            Assert.assertEquals("John Doe", ud1.getName());
        }
        final T2_UserData ud2 = T2_UserData.create();
        {
            ud2.setBalance(404);
            ud2.setName("Jane Doe");
            Assert.assertEquals(404, ud2.getBalance());
            Assert.assertEquals("Jane Doe", ud2.getName());
        }
        // Check func-ptr are original
        {
            final long[] funcBOrigs = options.getCustomFuncBVariants(0, new long[2], 0, 2);
            final long funcB1 = options.getCustomFuncB1();
            final long funcB2 = options.getCustomFuncB2();
            Assert.assertEquals(funcBOrigs[0], funcB1);
            Assert.assertEquals(funcBOrigs[1], funcB2);
        }
        Assert.assertEquals(101, options.CustomFuncB1(ud1));
        Assert.assertEquals(404, options.CustomFuncB1(ud2));
        Assert.assertEquals(-101, options.CustomFuncB2(ud1));
        Assert.assertEquals(-404, options.CustomFuncB2(ud2));
        // switch functions
        {
            final long funcB1 = options.getCustomFuncB1();
            final long funcB2 = options.getCustomFuncB2();
            options.setCustomFuncB1(funcB2);
            options.setCustomFuncB2(funcB1);
        }
        // Check func-ptr are switched
        {
            final long[] funcBOrigs = options.getCustomFuncBVariants(0, new long[2], 0, 2);
            final long funcB1 = options.getCustomFuncB1();
            final long funcB2 = options.getCustomFuncB2();
            Assert.assertEquals(funcBOrigs[1], funcB1);
            Assert.assertEquals(funcBOrigs[0], funcB2);
        }
        Assert.assertEquals(-101, options.CustomFuncB1(ud1));
        Assert.assertEquals(-404, options.CustomFuncB1(ud2));
        Assert.assertEquals(101, options.CustomFuncB2(ud1));
        Assert.assertEquals(404, options.CustomFuncB2(ud2));

        bt2.Release(options);
        Assert.assertEquals(true, options.isOverrideThreadAffinityNull());
        Assert.assertEquals(true, options.isProductNameNull());
        Assert.assertEquals(true, options.isProductVersionNull());
        Assert.assertEquals(0, options.getCustomFuncA1());
        // const Assert.assertEquals(0, options.getCustomFuncA2());
        Assert.assertEquals(0, options.getCustomFuncB1());
        Assert.assertEquals(0, options.getCustomFuncB2());
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = Test2FuncPtr.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
