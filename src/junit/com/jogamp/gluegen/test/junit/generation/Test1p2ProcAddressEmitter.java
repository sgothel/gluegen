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

package com.jogamp.gluegen.test.junit.generation;

import com.jogamp.gluegen.test.junit.generation.impl.Bindingtest1p2Impl;

import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.DynamicLookupHelper;

import org.junit.Assert;
import org.junit.Test;

import static com.jogamp.gluegen.test.junit.generation.BuildEnvironment.*;

/**
 * @author Michael Bien
 * @author Sven Gothel
 */
public class Test1p2ProcAddressEmitter extends BaseClass {

    DynamicLookupHelper dynamicLookupHelper;

    /**
     * Verifies loading of the new library.
     */
    @Test
    public void chapter01TestLoadLibrary() throws Exception {
        BindingJNILibLoader.loadBindingtest1p2();
        dynamicLookupHelper = NativeLibrary.open("test1", getClass().getClassLoader(), true);
        Assert.assertNotNull("NativeLibrary.open(test1) failed", dynamicLookupHelper);

        Bindingtest1p2Impl.resetProcAddressTable(dynamicLookupHelper);
    }

    /**
     * Verifies the existence and creation of the generated class.
     */
    @Test
    public void chapter02TestClassExist() throws Exception {
        testClassExist("test1p2");
    }

    /**
     * Verifies if all generated method signatures are completed,
     * ie a compilation only coverage test without functional tests.
     */
    public void chapter__TestCoverageSignature() throws Exception {
        chapter__TestCoverageSignature(new Bindingtest1p2Impl());
    }

    /**
     * Verifies if all methods / signatures are properly generated,
     * can be invoked and functions.
     * This is a compilation (coverage) and runtime time (semantic) test.
     * This covers indirect primitive arrays and direct NIO buffers.
     */
    @Test
    public void chapter03TestCoverageFunctionalityDirectNIOAndPrimitiveArray() throws Exception {
        chapter03TestCoverageFunctionalityDirectNIOAndPrimitiveArray(new Bindingtest1p2Impl());
    }

    /**
     * This covers indirect primitive arrays and indirect NIO buffers.
     */
    @Test
    public void chapter04TestSomeFunctionsAllIndirect() throws Exception {
        chapter04TestSomeFunctionsAllIndirect(new Bindingtest1p2Impl());
    }

    public static void main(String[] args) {
        Test1p2ProcAddressEmitter test = new Test1p2ProcAddressEmitter();
        try {
            test.chapter01TestLoadLibrary();
            test.chapter02TestClassExist();
            test.chapter03TestCoverageFunctionalityDirectNIOAndPrimitiveArray();
            test.chapter04TestSomeFunctionsAllIndirect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
