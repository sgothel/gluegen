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

    @Test
    public void bindingTest1() throws Exception {

        String nativesPath = testOutput + "/build/natives";
        System.load(nativesPath + "/libtest1.so");

        Class<?> clazz = Class.forName("com.jogamp.gluegen.test.junit.BindingTest1");

        Assert.assertNotNull("com.jogamp.gluegen.test.junit.BindingTest1 does not exist", clazz);
        Assert.assertEquals((int)1,  clazz.getDeclaredField("CONSTANT_ONE").get(null));

        Object obj = clazz.newInstance();
        Assert.assertTrue("Not of type com.jogamp.gluegen.test.junit.BindingTest1", (obj instanceof com.jogamp.gluegen.test.junit.BindingTest1));

        com.jogamp.gluegen.test.junit.BindingTest1 bindingTest1 = (com.jogamp.gluegen.test.junit.BindingTest1) obj;
        Assert.assertTrue("nopTest1 failed", 42==bindingTest1.nopTest());

        // assertEquals((long)0xFFFFFFFF,  clazz.getDeclaredField("GL_INVALID_INDEX").get(null));
        // assertEquals(-0.5f,             clazz.getDeclaredField("AL_FLANGER_DEFAULT_FEEDBACK").get(null));

        // TODO fix Exception: ...Caused by: java.lang.UnsatisfiedLinkError: test.BindingTest.arrayTest0(JLjava/lang/Object;I)I
        /*
        // test values
        ByteBuffer dbb = BufferFactory.newDirectByteBuffer(32);
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

}
