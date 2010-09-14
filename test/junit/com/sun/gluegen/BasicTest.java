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
 
package com.sun.gluegen;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static java.lang.System.*;
import static com.sun.gluegen.BuildUtil.*;

/**
 *
 * @author Michael Bien
 */
public class BasicTest {

    @Test
    public void generateBindingTest() {
        generate("test");
    }

    /**
     * fails if ant script fails (which is a good thing).
     * executeTarget throws RuntimeException on failure
     */
    @Test
    public void compileJavaTest() {
        compileJava();
    }

    /*
     * fails if ant script fails (which is a good thing)
     * executeTarget throws RuntimeException on failure
     */
    @Test
    public void compileNativesTest() {
        compileNatives();
    }

    @Test
    public void bindingTest() throws Exception {

        // String nativesPath = testOutput + "/build/natives";
        // System.load(nativesPath + "/librofl.so");
        System.loadLibrary("rofl");

        Class<?> clazz = Class.forName("test.BindingTest");

        assertEquals((long)0xFFFFFFFF,  clazz.getDeclaredField("GL_INVALID_INDEX").get(null));
        assertEquals(-0.5f,             clazz.getDeclaredField("AL_FLANGER_DEFAULT_FEEDBACK").get(null));

        // TODO fix Exception: ...Caused by: java.lang.UnsatisfiedLinkError: test.BindingTest.arrayTest0(JLjava/lang/Object;I)I
        /*
        // test values
        ByteBuffer dbb = Buffers.newDirectByteBuffer(32);
        ByteBuffer bb  = ByteBuffer.allocate(32).order(ByteOrder.nativeOrder());

        PointerBuffer dpb = PointerBuffer.allocateDirect(32);
        PointerBuffer pb  = PointerBuffer.allocate(32);

        long[] array = new long[] {1,2,3,4,5,6,7,8,9};
        int offset = 0;
        long id = 42;


        // invoke everything public
        Object bindingTest = clazz.newInstance();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {

            // prepare method parameters
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] paramInstances = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> paramType = paramTypes[i];
                if(paramType.isInstance(dbb)) {
                    paramInstances[i] = dbb;
                }else if(paramType.isInstance(bb)) {
                    paramInstances[i] = bb;
                }else if(paramType.isInstance(dpb)) {
                    paramInstances[i] = dpb;
                }else if(paramType.isInstance(pb)) {
                    paramInstances[i] = pb;
                }else if(paramType.isPrimitive()) { // TODO primitive types
                    paramInstances[i] = offset;
                }else if(paramType.isArray()) {     // TODO array types
                    paramInstances[i] = array;
                }
            }

            out.println("invoking: "+method);
            out.println("with params: ");
            for (Object param : paramInstances)
                out.print(param+", ");
            out.println();

            Object result = method.invoke(bindingTest, paramInstances);
            out.println("result: "+result);
            out.println("success");
        }
        */
    }

    @AfterClass
    public static void tearDown() {
//        cleanGeneratedFiles();
    }

}
