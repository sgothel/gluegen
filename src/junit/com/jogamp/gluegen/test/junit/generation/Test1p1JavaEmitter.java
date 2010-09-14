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

import com.jogamp.gluegen.test.junit.generation.impl.Bindingtest1p1Impl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
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
public class Test1p1JavaEmitter extends BaseClass {

    /**
     * Verifies loading of the new library.
     */
    @Test
    public void chapter01TestLoadLibrary() throws Exception {
        BindingJNILibLoader.loadBindingtest1p1();
    }

    /**
     * Verifies the existence and creation of the generated class.
     */
    @Test
    public void chapter02TestClassExist() throws Exception {
        testClassExist("test1p1");
    }

    /**
     * Verifies if all generated method signatures are completed,
     * ie a compilation only coverage test without functional tests.
     */
    public void chapter__TestCoverageSignature() throws Exception {
        chapter__TestCoverageSignature(new Bindingtest1p1Impl());
    }

    /**
     * Verifies if all methods / signatures are properly generated,
     * can be invoked and functions.
     * This is a compilation (coverage) and runtime time (semantic) test.
     * This covers indirect primitive arrays and direct NIO buffers.
     */
    @Test
    public void chapter03TestCoverageFunctionalityDirectNIOAndPrimitiveArray() throws Exception {
        chapter03TestCoverageFunctionalityDirectNIOAndPrimitiveArray(new Bindingtest1p1Impl());
    }

    /**
     * This covers indirect primitive arrays and indirect NIO buffers.
     */
    @Test
    public void chapter04TestSomeFunctionsAllIndirect() throws Exception {
        chapter04TestSomeFunctionsAllIndirect(new Bindingtest1p1Impl());
    }

}
