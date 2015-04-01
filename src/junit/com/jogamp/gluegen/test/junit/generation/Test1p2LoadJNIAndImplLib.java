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

import com.jogamp.gluegen.test.junit.generation.impl.Bindingtest1p2Impl;
import com.jogamp.common.os.NativeLibrary;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Test1p2LoadJNIAndImplLib extends BaseClass {

    static NativeLibrary dynamicLookupHelper;

    /**
     * Verifies loading of the new library.
     */
    @BeforeClass
    public static void chapter__TestLoadLibrary() throws Exception {
        BindingJNILibLoader.loadBindingtest1p2();
        dynamicLookupHelper = NativeLibrary.open("test1", Test1p2LoadJNIAndImplLib.class.getClassLoader(), true);
        Assert.assertNotNull("NativeLibrary.open(test1) failed", dynamicLookupHelper);

        Bindingtest1p2Impl.resetProcAddressTable(dynamicLookupHelper);
    }

    /**
     * Verifies the existence and creation of the generated class.
     */
    @Test
    public void chapter00TestClassExist() throws Exception {
        testClassExist("test1p2");
    }

    @SuppressWarnings("unused")
    public static void main(final String args[]) throws Exception {
        if( true ) {
            chapter__TestLoadLibrary();
            final Test1p2LoadJNIAndImplLib tst = new Test1p2LoadJNIAndImplLib();
            tst.chapter00TestClassExist();
        } else {
            final String tstname = Test1p2LoadJNIAndImplLib.class.getName();
            org.junit.runner.JUnitCore.main(tstname);
        }
    }
}
