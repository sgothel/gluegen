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

import com.jogamp.common.os.NativeLibrary;
import com.jogamp.gluegen.test.junit.generation.impl.Bindingtest2p1Impl;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;

/**
 * Test {@link Bindingtest2p1} with {@link T2_PointerStorage} instance and pointer pointer..
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Test4p1JavaCallback extends BaseTest4JavaCallback {

    /**
     * Verifies loading of the new library.
     */
    @BeforeClass
    public static void chapter__TestLoadLibrary() throws Exception {
        BindingJNILibLoader.loadBindingtest2p1();
    }

    /**
     * Verifies unloading of the new library.
     */
    @AfterClass
    public static void chapter0XTestUnloadLibrary() throws Exception {
    }

    @Test
    public void chapter01() throws Exception {
        chapter01(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter02() throws Exception {
        chapter02(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter03() throws Exception {
        chapter03(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter04() throws Exception {
        chapter04(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter05a() throws Exception {
        chapter05a(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter05b() throws Exception {
        chapter05b(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter11a() throws Exception {
        chapter11a(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter11b() throws Exception {
        chapter11b(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter12a() throws Exception {
        chapter12a(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter12b() throws Exception {
        chapter12b(new Bindingtest2p1Impl());
    }

    @Test
    public void chapter13() throws Exception {
        chapter13(new Bindingtest2p1Impl());
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = Test4p1JavaCallback.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
