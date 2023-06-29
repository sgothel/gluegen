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

import com.jogamp.common.os.NativeLibrary;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.T2_CallbackFunc01;
import com.jogamp.gluegen.test.junit.generation.impl.Bindingtest2Impl;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test {@link Bindingtest2} with {@link T2_PointerStorage} instance and pointer pointer..
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Test4JavaCallback extends BaseClass {
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
     * Test Bindingtest2 with T2_CallbackFunc JavaCallback
     */
    @Test
    public void chapter01() throws Exception {
        final Bindingtest2 bt2 = new Bindingtest2Impl();

        final long[] id_res = { -1 };
        final String[] msg_res = { null };
        final T2_CallbackFunc01 myCallback01 = new T2_CallbackFunc01() {
            @Override
            public void callback(final long id, final String msg, final Object userParam) {
                final MyUserParam myUserParam = (MyUserParam)userParam;
                id_res[0] = id + myUserParam.i;
                msg_res[0] = msg;
                myUserParam.j += id_res[0];
                System.err.println("chapter10.myCallback01: "+id+", '"+msg+"'");
            }
        };
        final T2_CallbackFunc01 myCallback02 = new T2_CallbackFunc01() {
            @Override
            public void callback(final long id, final String msg, final Object userParam) {
                final MyUserParam myUserParam = (MyUserParam)userParam;
                id_res[0] = id;
                msg_res[0] = msg;
                myUserParam.j += id_res[0];
                System.err.println("chapter10.myCallback02: "+id+", '"+msg+"'");
            }
        };
        final MyUserParam myUserParam = new MyUserParam(10);
        Assert.assertEquals(10, myUserParam.i);
        Assert.assertEquals( 0, myUserParam.j);
        Assert.assertEquals(false, bt2.isMessageCallback01Mapped(myUserParam));

        bt2.MessageCallback01(myCallback01, myUserParam);
        Assert.assertEquals(true, bt2.isMessageCallback01Mapped(myUserParam));
        Assert.assertEquals(-1, id_res[0]);
        Assert.assertEquals(null, msg_res[0]);
        Assert.assertEquals(10, myUserParam.i);
        Assert.assertEquals( 0, myUserParam.j);

        {
            final String msgNo1 = "My First JavaCallback message";
            bt2.InjectMessageCallback01(404, msgNo1);
            Assert.assertEquals(404+10, id_res[0]);
            Assert.assertEquals(msgNo1, msg_res[0]);
            Assert.assertEquals(    10, myUserParam.i);
            Assert.assertEquals(404+10, myUserParam.j);
        }
        final String msgNo2 = "My Second JavaCallback message";
        {
            bt2.InjectMessageCallback01( 42, msgNo2);
            Assert.assertEquals(       42+10, id_res[0]);
            Assert.assertEquals(      msgNo2, msg_res[0]);
            Assert.assertEquals(          10, myUserParam.i);
            Assert.assertEquals(42+10+404+10, myUserParam.j);
        }

        // Switch the callback function
        // The previously mapped myUserParam01 gets released and remapped to new callback
        bt2.MessageCallback01(myCallback02, myUserParam);
        Assert.assertEquals(true, bt2.isMessageCallback01Mapped(myUserParam));
        Assert.assertEquals(       42+10, id_res[0]);
        Assert.assertEquals(      msgNo2, msg_res[0]);
        Assert.assertEquals(          10, myUserParam.i);
        Assert.assertEquals(42+10+404+10, myUserParam.j);

        final String msgNo3 = "My Third JavaCallback message";
        {
            bt2.InjectMessageCallback01(     1, msgNo3);
            Assert.assertEquals(             1, id_res[0]);
            Assert.assertEquals(        msgNo3, msg_res[0]);
            Assert.assertEquals(            10, myUserParam.i);
            Assert.assertEquals(1+42+10+404+10, myUserParam.j);
        }

        // Just release the callback and mapped myUserParam01
        bt2.MessageCallback01(null, myUserParam);
        Assert.assertEquals(false, bt2.isMessageCallback01Mapped(myUserParam));
        {
            final String msgNo4 = "My Fourth JavaCallback message";
            bt2.InjectMessageCallback01( 21, msgNo4);
            // No callback shall be received, hence old values
            Assert.assertEquals(             1, id_res[0]);
            Assert.assertEquals(        msgNo3, msg_res[0]);
            Assert.assertEquals(            10, myUserParam.i);
            Assert.assertEquals(1+42+10+404+10, myUserParam.j);
        }
    }
    private static class MyUserParam {
        final long i;
        long j;
        public MyUserParam(final long i) { this.i = i; j=0; }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = Test4JavaCallback.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
