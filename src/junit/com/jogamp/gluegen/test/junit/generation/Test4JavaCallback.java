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
import java.util.Set;

import com.jogamp.common.os.NativeLibrary;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.ALBUFFERCALLBACKTYPESOFT;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.ALEVENTPROCSOFT;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.AlBufferCallback0Key;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.AlEventCallback0Key;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.AlEventCallback1Key;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.MessageCallback11aKey;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.MessageCallback11bKey;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.T2_CallbackFunc01;
import com.jogamp.gluegen.test.junit.generation.Bindingtest2.T2_CallbackFunc11;
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

    private static class MyUserParam01 {
        final long i;
        long j;
        public MyUserParam01(final long i) { this.i = i; j=0; }

        @Override
        public boolean equals(final Object o) {
            if( this == o ) {
                return true;
            }
            if( !(o instanceof MyUserParam01) ) {
                return false;
            }
            return false; // we require identity!
        }
        @Override
        public int hashCode() {
            return System.identityHashCode(this); // we require identity!
        }
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
                final MyUserParam01 myUserParam = (MyUserParam01)userParam;
                id_res[0] = id + myUserParam.i;
                msg_res[0] = msg;
                myUserParam.j += id_res[0];
                System.err.println("chapter01.myCallback01: "+id+", '"+msg+"'");
            }
        };
        final T2_CallbackFunc01 myCallback02 = new T2_CallbackFunc01() {
            @Override
            public void callback(final long id, final String msg, final Object userParam) {
                final MyUserParam01 myUserParam = (MyUserParam01)userParam;
                id_res[0] = id;
                msg_res[0] = msg;
                myUserParam.j += id_res[0];
                System.err.println("chapter01.myCallback02: "+id+", '"+msg+"'");
            }
        };
        final MyUserParam01 myUserParam01 = new MyUserParam01(10);
        Assert.assertEquals(10, myUserParam01.i);
        Assert.assertEquals( 0, myUserParam01.j);
        Assert.assertEquals(false, bt2.isMessageCallback01Mapped());

        bt2.MessageCallback01(myCallback01, myUserParam01);
        Assert.assertEquals(true, bt2.isMessageCallback01Mapped());
        Assert.assertEquals(-1, id_res[0]);
        Assert.assertEquals(null, msg_res[0]);
        Assert.assertEquals(10, myUserParam01.i);
        Assert.assertEquals( 0, myUserParam01.j);

        {
            final String msgNo1 = "My First JavaCallback message";
            bt2.InjectMessageCallback01(404, msgNo1);
            Assert.assertEquals(404+10, id_res[0]);
            Assert.assertEquals(msgNo1, msg_res[0]);
            Assert.assertEquals(    10, myUserParam01.i);
            Assert.assertEquals(404+10, myUserParam01.j);
        }
        final String msgNo2 = "My Second JavaCallback message";
        {
            bt2.InjectMessageCallback01( 42, msgNo2);
            Assert.assertEquals(       42+10, id_res[0]);
            Assert.assertEquals(      msgNo2, msg_res[0]);
            Assert.assertEquals(          10, myUserParam01.i);
            Assert.assertEquals(42+10+404+10, myUserParam01.j);
        }

        // Switch the callback function
        // The previously mapped myUserParam01 gets released and remapped to new callback
        bt2.MessageCallback01(myCallback02, myUserParam01);
        Assert.assertEquals(true, bt2.isMessageCallback01Mapped());
        Assert.assertEquals(       42+10, id_res[0]);
        Assert.assertEquals(      msgNo2, msg_res[0]);
        Assert.assertEquals(          10, myUserParam01.i);
        Assert.assertEquals(42+10+404+10, myUserParam01.j);

        final String msgNo3 = "My Third JavaCallback message";
        {
            bt2.InjectMessageCallback01(     1, msgNo3);
            Assert.assertEquals(             1, id_res[0]);
            Assert.assertEquals(        msgNo3, msg_res[0]);
            Assert.assertEquals(            10, myUserParam01.i);
            Assert.assertEquals(1+42+10+404+10, myUserParam01.j);
        }

        // Just release the callback and mapped myUserParam01
        bt2.MessageCallback01(null, myUserParam01);
        Assert.assertEquals(false, bt2.isMessageCallback01Mapped());
        {
            final String msgNo4 = "My Fourth JavaCallback message";
            bt2.InjectMessageCallback01( 21, msgNo4);
            // No callback shall be received, hence old values
            Assert.assertEquals(             1, id_res[0]);
            Assert.assertEquals(        msgNo3, msg_res[0]);
            Assert.assertEquals(            10, myUserParam01.i);
            Assert.assertEquals(1+42+10+404+10, myUserParam01.j);
        }
    }

    public static class ALCcontext {
        final long i;
        long j;
        int buffer;
        boolean throwPreAction, throwPostAction;
        public ALCcontext(final long i) { this.i = i; j=0; buffer=0; throwPreAction=false; throwPostAction=false; }
        public ALCcontext() { this.i = 0; j=0; buffer=0; throwPreAction=false; throwPostAction=false; }

        @Override
        public boolean equals(final Object o) {
            if( this == o ) {
                return true;
            }
            if( !(o instanceof ALCcontext) ) {
                return false;
            }
            return false; // we require identity!
        }
        @Override
        public int hashCode() {
            return System.identityHashCode(this); // we require identity!
        }
    }


    /**
     * Test Bindingtest2 with ALBUFFERCALLBACKTYPESOFT JavaCallback via alBufferCallback1()
     * using the default AlBufferCallback1Key class.
     */
    @Test
    public void chapter02() throws Exception {
        final Bindingtest2 bt2 = new Bindingtest2Impl();

        final long[] id_res = { -1 };
        final ALBUFFERCALLBACKTYPESOFT myCallback01 = new ALBUFFERCALLBACKTYPESOFT() {
            @Override
            public void callback(final int buffer, final ALCcontext context, final int sampledata, final int numbytes) {
                final long res = sampledata + numbytes + context.i;
                id_res[0] = res;
                context.j = res;
                context.buffer = buffer;
                System.err.println("chapter02.myCallback01: buffer "+buffer+", sampledata "+sampledata+", numbytes "+numbytes);
            }
        };
        final ALBUFFERCALLBACKTYPESOFT myCallback02 = new ALBUFFERCALLBACKTYPESOFT() {
            @Override
            public void callback(final int buffer, final ALCcontext context, final int sampledata, final int numbytes) {
                final long res = sampledata * numbytes + context.i;
                id_res[0] = res;
                context.j = res;
                context.buffer = buffer;
                System.err.println("chapter02.myCallback02: buffer "+buffer+", sampledata "+sampledata+", numbytes "+numbytes);
            }
        };
        final int buffer1 = 1;
        final int buffer2 = 2;
        final int buffer3 = 3;
        final AlBufferCallback0Key buffer1Key = new AlBufferCallback0Key(buffer1);
        final AlBufferCallback0Key buffer2Key = new AlBufferCallback0Key(buffer2);
        final AlBufferCallback0Key buffer3Key = new AlBufferCallback0Key(buffer3);
        final ALCcontext context01 = new ALCcontext( 1);
        final ALCcontext context02 = new ALCcontext( 2);
        Assert.assertEquals( 1, context01.i);
        Assert.assertEquals( 0, context01.j);
        Assert.assertEquals( 0, context01.buffer);
        Assert.assertEquals( 2, context02.i);
        Assert.assertEquals( 0, context02.j);
        Assert.assertEquals( 0, context02.buffer);

        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(0,     bt2.getAlBufferCallback0Keys().size());

        // 1st mapping: buffer1 -> myCallback01, context01
        bt2.alBufferCallback0(buffer1, 0, 0, myCallback01, context01);
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback0UserParam(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback0(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
        Assert.assertEquals(1,             bt2.getAlBufferCallback0Keys().size());
        {
            final Set<AlBufferCallback0Key> keys = bt2.getAlBufferCallback0Keys();
            Assert.assertEquals(true,  keys.contains(buffer1Key));
            Assert.assertEquals(false, keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        // 2nd mapping: buffer2 -> myCallback02, context02
        bt2.alBufferCallback0(buffer2, 0, 0, myCallback02, context02);
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback0UserParam(buffer1Key));
        Assert.assertEquals(context02, bt2.getAlBufferCallback0UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback0(buffer1Key));
        Assert.assertEquals(myCallback02,  bt2.getAlBufferCallback0(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
        Assert.assertEquals(2,             bt2.getAlBufferCallback0Keys().size());
        {
            final Set<AlBufferCallback0Key> keys = bt2.getAlBufferCallback0Keys();
            Assert.assertEquals(true,  keys.contains(buffer1Key));
            Assert.assertEquals(true,  keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.alBufferCallback0Inject(buffer1, 10, 100); // buffer1 -> myCallback01, context01
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals(10+100+1, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(10+100+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(       0, context02.j);
            Assert.assertEquals(       0, context02.buffer);
        }
        {
            bt2.alBufferCallback0Inject(buffer2, 10, 100); // buffer2 -> myCallback02, context02
            Assert.assertEquals(10*100+2, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(10+100+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(10*100+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }

        // Switch the callback function for buffer2 -> myCallback01, context02
        bt2.alBufferCallback0(buffer2, 0, 0, myCallback01, context02);
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback0UserParam(buffer1Key));
        Assert.assertEquals(context02, bt2.getAlBufferCallback0UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback0(buffer1Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback0(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));

        {
            bt2.alBufferCallback0Inject(buffer1, 11, 101); // buffer1 -> myCallback01, context01
            Assert.assertEquals(11+101+1, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(11+101+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(10*100+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }
        {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.alBufferCallback0Inject(buffer2,  1,  10); // buffer2 -> myCallback01, context02
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals( 1+ 10+2, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(11+101+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals( 1+ 10+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }

        // Just release the buffer2 callback and mapped resources
        bt2.alBufferCallback0(buffer2, 0, 0, null, context02); // usrptr is not key, only buffer is key!
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback0UserParam(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback0(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
        Assert.assertEquals(1,             bt2.getAlBufferCallback0Keys().size());
        {
            final Set<AlBufferCallback0Key> keys = bt2.getAlBufferCallback0Keys();
            Assert.assertEquals(true,  keys.contains(buffer1Key));
            Assert.assertEquals(false, keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        // Switch the callback function for buffer2 -> myCallback01, context02
        {
            // pre-condition
            Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer1Key));
            Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
            Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
            Assert.assertEquals(context01, bt2.getAlBufferCallback0UserParam(buffer1Key));
            Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer2Key));
            Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
            Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback0(buffer1Key));
            Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer2Key));
            Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
            Assert.assertEquals(1,             bt2.getAlBufferCallback0Keys().size());
        }
        bt2.alBufferCallback0(buffer1, 0, 0, myCallback02, context02);
        {
            // post-state
            Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer1Key));
            Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
            Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
            Assert.assertEquals(context02, bt2.getAlBufferCallback0UserParam(buffer1Key));
            Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer2Key));
            Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
            Assert.assertEquals(myCallback02,  bt2.getAlBufferCallback0(buffer1Key));
            Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer2Key));
            Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
        }
        {
            context01.j = 0;
            context01.buffer = 0;
            context02.j = 0;
            context02.buffer = 0;
            bt2.alBufferCallback0Inject(buffer1, 2, 10); // buffer1 -> myCallback01, context01
            Assert.assertEquals(  2*10+2, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(       0, context01.j);
            Assert.assertEquals(       0, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(  2*10+2, context02.j);
            Assert.assertEquals(       1, context02.buffer);
        }

        // Just release the buffer1 callback and mapped resources
        bt2.alBufferCallback0(buffer1, 0, 0, null, null); // usrptr is not key, only buffer is key!
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
        Assert.assertEquals(0,             bt2.getAlBufferCallback0Keys().size());
        {
            final Set<AlBufferCallback0Key> keys = bt2.getAlBufferCallback0Keys();
            Assert.assertEquals(false, keys.contains(buffer1Key));
            Assert.assertEquals(false, keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        {
            id_res[0] = 0;
            context01.j = 0;
            context01.buffer = 0;
            context02.j = 0;
            context02.buffer = 0;
            bt2.alBufferCallback0Inject(buffer2,  1,  10); // unmapped, no change in data
            Assert.assertEquals(       0, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(       0, context01.j);
            Assert.assertEquals(       0, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(       0, context02.j);
            Assert.assertEquals(       0, context02.buffer);
        }
    }

    /**
     * Test Bindingtest2 with ALBUFFERCALLBACKTYPESOFT JavaCallback via alBufferCallback1()
     * using our custom CustomAlBufferCallback1Key class.
     */
    @Test
    public void chapter03() throws Exception {
        final Bindingtest2 bt2 = new Bindingtest2Impl();

        final long[] id_res = { -1 };
        final ALBUFFERCALLBACKTYPESOFT myCallback01 = new ALBUFFERCALLBACKTYPESOFT() {
            @Override
            public void callback(final int buffer, final ALCcontext context, final int sampledata, final int numbytes) {
                final long res = sampledata + numbytes + context.i;
                id_res[0] = res;
                context.j = res;;
                context.buffer = buffer;
                System.err.println("chapter03.myCallback01: buffer "+buffer+", sampledata "+sampledata+", numbytes "+numbytes);
            }
        };
        final ALBUFFERCALLBACKTYPESOFT myCallback02 = new ALBUFFERCALLBACKTYPESOFT() {
            @Override
            public void callback(final int buffer, final ALCcontext context, final int sampledata, final int numbytes) {
                final long res = sampledata * numbytes + context.i;
                id_res[0] = res;
                context.j = res;
                context.buffer = buffer;
                System.err.println("chapter03.myCallback02: buffer "+buffer+", sampledata "+sampledata+", numbytes "+numbytes);
            }
        };
        final int buffer1 = 1;
        final int buffer2 = 2;
        final int buffer3 = 3;
        final CustomAlBufferCallback1Key buffer1Key = new CustomAlBufferCallback1Key(buffer1);
        final CustomAlBufferCallback1Key buffer2Key = new CustomAlBufferCallback1Key(buffer2);
        final CustomAlBufferCallback1Key buffer3Key = new CustomAlBufferCallback1Key(buffer3);
        final ALCcontext context01 = new ALCcontext( 1);
        final ALCcontext context02 = new ALCcontext( 2);
        Assert.assertEquals( 1, context01.i);
        Assert.assertEquals( 0, context01.j);
        Assert.assertEquals( 0, context01.buffer);
        Assert.assertEquals( 2, context02.i);
        Assert.assertEquals( 0, context02.j);
        Assert.assertEquals( 0, context02.buffer);

        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer3Key));
        Assert.assertEquals(0,     bt2.getAlBufferCallback1Keys().size());

        // 1st mapping: buffer1 -> myCallback01, context01
        bt2.alBufferCallback1(context01, buffer1, 0, 0, myCallback01);
        Assert.assertEquals(true,  bt2.isAlBufferCallback1Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback1UserParam(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback1(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1(buffer3Key));
        Assert.assertEquals(1,             bt2.getAlBufferCallback1Keys().size());
        {
            final Set<CustomAlBufferCallback1Key> keys = bt2.getAlBufferCallback1Keys();
            Assert.assertEquals(true,  keys.contains(buffer1Key));
            Assert.assertEquals(false, keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        // 2nd mapping: buffer2 -> myCallback02, context02
        bt2.alBufferCallback1(context02, buffer2, 0, 0, myCallback02);
        Assert.assertEquals(true,  bt2.isAlBufferCallback1Mapped(buffer1Key));
        Assert.assertEquals(true,  bt2.isAlBufferCallback1Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback1UserParam(buffer1Key));
        Assert.assertEquals(context02, bt2.getAlBufferCallback1UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback1(buffer1Key));
        Assert.assertEquals(myCallback02,  bt2.getAlBufferCallback1(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1(buffer3Key));
        Assert.assertEquals(2,             bt2.getAlBufferCallback1Keys().size());
        {
            final Set<CustomAlBufferCallback1Key> keys = bt2.getAlBufferCallback1Keys();
            Assert.assertEquals(true,  keys.contains(buffer1Key));
            Assert.assertEquals(true,  keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        {
            bt2.alBufferCallback1Inject(buffer1, 10, 100); // buffer1 -> myCallback01, context01
            Assert.assertEquals(10+100+1, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(10+100+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(       0, context02.j);
            Assert.assertEquals(       0, context02.buffer);
        }
        {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.alBufferCallback1Inject(buffer2, 10, 100); // buffer2 -> myCallback02, context02
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals(10*100+2, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(10+100+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(10*100+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }

        // Switch the callback function for buffer2 -> myCallback01, context02
        bt2.alBufferCallback1(context02, buffer2, 0, 0, myCallback01);
        Assert.assertEquals(true,  bt2.isAlBufferCallback1Mapped(buffer1Key));
        Assert.assertEquals(true,  bt2.isAlBufferCallback1Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback1UserParam(buffer1Key));
        Assert.assertEquals(context02, bt2.getAlBufferCallback1UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback1(buffer1Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback1(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1(buffer3Key));

        {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.alBufferCallback1Inject(buffer1, 11, 101); // buffer1 -> myCallback01, context01
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals(11+101+1, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(11+101+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(10*100+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }
        {
            bt2.alBufferCallback1Inject(buffer2,  1,  10); // buffer2 -> myCallback01, context02
            Assert.assertEquals( 1+ 10+2, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(11+101+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals( 1+ 10+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }

        // Just release the buffer2 callback and mapped resources
        bt2.alBufferCallback1(context02, buffer2, 0, 0, null); // usrptr is not key, only buffer is key!
        Assert.assertEquals(true,  bt2.isAlBufferCallback1Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback1UserParam(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback1(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1(buffer3Key));
        Assert.assertEquals(1,             bt2.getAlBufferCallback1Keys().size());
        {
            final Set<CustomAlBufferCallback1Key> keys = bt2.getAlBufferCallback1Keys();
            Assert.assertEquals(true,  keys.contains(buffer1Key));
            Assert.assertEquals(false, keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        // Just release the buffer1 callback and mapped resources
        bt2.alBufferCallback1(null, buffer1, 0, 0, null); // usrptr is not key, only buffer is key!
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback1Mapped(buffer3Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1UserParam(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1UserParam(buffer3Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback1(buffer3Key));
        Assert.assertEquals(0,             bt2.getAlBufferCallback1Keys().size());
        {
            final Set<CustomAlBufferCallback1Key> keys = bt2.getAlBufferCallback1Keys();
            Assert.assertEquals(false, keys.contains(buffer1Key));
            Assert.assertEquals(false, keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        {
            bt2.alBufferCallback1Inject(buffer2,  1,  10); // unmapped, no change in data
            Assert.assertEquals( 1+ 10+2, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(11+101+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals( 1+ 10+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }
    }

    /**
     * Test in depth lifecycle of Bindingtest2 with ALBUFFERCALLBACKTYPESOFT JavaCallback via alBufferCallback1()
     * using the default AlBufferCallback1Key class.
     */
    @Test
    public void chapter04() throws Exception {
        final Bindingtest2 bt2 = new Bindingtest2Impl();

        final long[] id_res = { -1 };
        final ALBUFFERCALLBACKTYPESOFT myCallback01 = new ALBUFFERCALLBACKTYPESOFT() {
            @Override
            public void callback(final int buffer, final ALCcontext context, final int sampledata, final int numbytes) {
                if( context.throwPreAction ) {
                    context.throwPreAction = false;
                    throw new RuntimeException("Exception test.pre: chapter04.myCallback01");
                }
                final long res = sampledata + numbytes + context.i;
                id_res[0] = res;
                context.j = res;
                context.buffer = buffer;
                System.err.println("chapter04.myCallback01: buffer "+buffer+", sampledata "+sampledata+", numbytes "+numbytes);
                if( context.throwPostAction ) {
                    context.throwPostAction = false;
                    throw new RuntimeException("Exception test.post: chapter04.myCallback01");
                }
            }
        };
        final ALBUFFERCALLBACKTYPESOFT myCallback02 = new ALBUFFERCALLBACKTYPESOFT() {
            @Override
            public void callback(final int buffer, final ALCcontext context, final int sampledata, final int numbytes) {
                if( context.throwPreAction ) {
                    context.throwPreAction = false;
                    throw new RuntimeException("Exception test.pre: chapter04.myCallback02");
                }
                final long res = sampledata * numbytes + context.i;
                id_res[0] = res;
                context.j = res;
                context.buffer = buffer;
                System.err.println("chapter04.myCallback02: buffer "+buffer+", sampledata "+sampledata+", numbytes "+numbytes);
                if( context.throwPostAction ) {
                    context.throwPostAction = false;
                    throw new RuntimeException("Exception test.post: chapter04.myCallback02");
                }
            }
        };
        final int buffer1 = 1;
        final int buffer2 = 2;
        final int buffer3 = 3;
        final AlBufferCallback0Key buffer1Key = new AlBufferCallback0Key(buffer1);
        final AlBufferCallback0Key buffer2Key = new AlBufferCallback0Key(buffer2);
        final AlBufferCallback0Key buffer3Key = new AlBufferCallback0Key(buffer3);
        final ALCcontext context01 = new ALCcontext( 1);
        final ALCcontext context02 = new ALCcontext( 2);
        Assert.assertEquals( 1, context01.i);
        Assert.assertEquals( 0, context01.j);
        Assert.assertEquals( 0, context01.buffer);
        Assert.assertEquals( 2, context02.i);
        Assert.assertEquals( 0, context02.j);
        Assert.assertEquals( 0, context02.buffer);

        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(0,     bt2.getAlBufferCallback0Keys().size());

        // 1st mapping: buffer1 -> myCallback01, context01
        bt2.alBufferCallback0(buffer1, 0, 0, myCallback01, context01);
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback0UserParam(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback0(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
        Assert.assertEquals(1,             bt2.getAlBufferCallback0Keys().size());
        {
            final Set<AlBufferCallback0Key> keys = bt2.getAlBufferCallback0Keys();
            Assert.assertEquals(true,  keys.contains(buffer1Key));
            Assert.assertEquals(false, keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        // 2nd mapping: buffer2 -> myCallback02, context02
        bt2.alBufferCallback0(buffer2, 0, 0, myCallback02, context02);
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback0UserParam(buffer1Key));
        Assert.assertEquals(context02, bt2.getAlBufferCallback0UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback0(buffer1Key));
        Assert.assertEquals(myCallback02,  bt2.getAlBufferCallback0(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
        Assert.assertEquals(2,             bt2.getAlBufferCallback0Keys().size());
        {
            final Set<AlBufferCallback0Key> keys = bt2.getAlBufferCallback0Keys();
            Assert.assertEquals(true,  keys.contains(buffer1Key));
            Assert.assertEquals(true,  keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        // Exception text post action, i.e. result as expected
        // Continuous program flow
        id_res[0] = -1;
        {
            context01.throwPostAction = true;
            bt2.alBufferCallback0Inject(buffer1, 10, 100); // buffer1 -> myCallback01, context01
            Assert.assertEquals(10+100+1, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(10+100+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(       0, context02.j);
            Assert.assertEquals(       0, context02.buffer);
        }
        {
            context02.throwPostAction = true;
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.alBufferCallback0Inject(buffer2, 10, 100); // buffer2 -> myCallback02, context02
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals(10*100+2, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(10+100+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(10*100+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }

        // Exception text pre action, i.e. result NOT as expected (unchanged)
        // Continuous program flow
        id_res[0] = -1;
        {
            context01.throwPreAction = true;
            bt2.alBufferCallback0Inject(buffer1, 20, 200); // buffer1 -> myCallback01, context01
            Assert.assertEquals(      -1, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(10+100+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(10*100+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }
        {
            context02.throwPreAction = true;
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.alBufferCallback0Inject(buffer2, 20, 200); // buffer2 -> myCallback02, context02
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals(      -1, id_res[0]);
            Assert.assertEquals(       1, context01.i);
            Assert.assertEquals(10+100+1, context01.j);
            Assert.assertEquals(       1, context01.buffer);
            Assert.assertEquals(       2, context02.i);
            Assert.assertEquals(10*100+2, context02.j);
            Assert.assertEquals(       2, context02.buffer);
        }

        // Just release the buffer2 callback and mapped resources
        bt2.releaseAlBufferCallback0(buffer2Key);
        // bt2.alBufferCallback0(buffer2, 0, 0, null, context02); // usrptr is not key, only buffer is key!
        Assert.assertEquals(true,  bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(context01, bt2.getAlBufferCallback0UserParam(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
        Assert.assertEquals(myCallback01,  bt2.getAlBufferCallback0(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
        Assert.assertEquals(1,             bt2.getAlBufferCallback0Keys().size());
        {
            final Set<AlBufferCallback0Key> keys = bt2.getAlBufferCallback0Keys();
            Assert.assertEquals(true,  keys.contains(buffer1Key));
            Assert.assertEquals(false, keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        // Just release the buffer1 callback and mapped resources
        bt2.releaseAlBufferCallback0(buffer1Key);
        // bt2.alBufferCallback0(buffer1, 0, 0, null, null); // usrptr is not key, only buffer is key!
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer1Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer2Key));
        Assert.assertEquals(false, bt2.isAlBufferCallback0Mapped(buffer3Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0UserParam(buffer3Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer1Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer2Key));
        Assert.assertEquals(null,          bt2.getAlBufferCallback0(buffer3Key));
        Assert.assertEquals(0,             bt2.getAlBufferCallback0Keys().size());
        {
            final Set<AlBufferCallback0Key> keys = bt2.getAlBufferCallback0Keys();
            Assert.assertEquals(false, keys.contains(buffer1Key));
            Assert.assertEquals(false, keys.contains(buffer2Key));
            Assert.assertEquals(false, keys.contains(buffer3Key));
        }

        // The native callback is still registered,
        // we 'just' pulled the resource via release*()!
        //
        // So we no only test no-change in data
        // but also whether the native callback handles this case well,
        // i.e. detects the released data-resource and *NOT* issuing the java callback.
        // The latter would end up in a SIGSEGV otherwise!
        //
        // Note: After successfully checking a correct jobject reference,
        // the native callback also enters and leaves the monitor (Object sync/lock).
        id_res[0] = -1;
        bt2.alBufferCallback0Inject(buffer2,  1,  10);
        Assert.assertEquals(      -1, id_res[0]);
        Assert.assertEquals(       1, context01.i);
        Assert.assertEquals(10+100+1, context01.j);
        Assert.assertEquals(       1, context01.buffer);
        Assert.assertEquals(       2, context02.i);
        Assert.assertEquals(10*100+2, context02.j);
        Assert.assertEquals(       2, context02.buffer);
    }

    public static class CustomAlBufferCallback1Key {
        private final int buffer;
        public CustomAlBufferCallback1Key(final int buffer) {
            this.buffer = buffer;
        }
        @Override
        public boolean equals(final Object o) {
            if( this == o ) {
                return true;
            }
            if( !(o instanceof CustomAlBufferCallback1Key) ) {
                return false;
            }
            final CustomAlBufferCallback1Key o2 = (CustomAlBufferCallback1Key)o;
            return buffer == o2.buffer;
        }
        @Override
        public int hashCode() {
            return buffer;
        }
        @Override
        public String toString() {
            return "CustomALKey[this "+toHexString(System.identityHashCode(this))+", buffer "+buffer+"]";
        }
    }

    /**
     * Test Bindingtest2 with ALEVENTPROCSOFT JavaCallback
     * on alEventCallback0(..) having the 'Object userParam` as single key.
     */
    @Test
    public void chapter05a() throws Exception {
        final Bindingtest2 bt2 = new Bindingtest2Impl();

        final int[] id_res = { -1 };
        final String[] msg_res = { null };
        final ALEVENTPROCSOFT myCallback01 = new ALEVENTPROCSOFT() {
            @Override
            public void callback(final int eventType, final int object, final int param, final int length, final String message, final ALCcontext context) {
                id_res[0] = object;
                msg_res[0] = message;
                System.err.println("chapter05a.myCallback01: type "+eventType+", obj "+object+", param "+param+", '"+message+"', userParam 0x"+
                        Integer.toHexString(System.identityHashCode(context)));
            }
        };
        final ALEVENTPROCSOFT myCallback02 = new ALEVENTPROCSOFT() {
            @Override
            public void callback(final int eventType, final int object, final int param, final int length, final String message, final ALCcontext context) {
                id_res[0] = 1000 * object;
                msg_res[0] = message;
                System.err.println("chapter05a.myCallback02: type "+eventType+", obj "+object+", param "+param+", '"+message+"', userParam 0x"+
                        Integer.toHexString(System.identityHashCode(context)));
            }
        };
        final ALCcontext context01 = new ALCcontext();
        final ALCcontext context02 = new ALCcontext();
        final AlEventCallback0Key myKey01 = new AlEventCallback0Key(context01);
        final AlEventCallback0Key myKey02 = new AlEventCallback0Key(context02);
        Assert.assertEquals(false, bt2.isAlEventCallback0Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback0Mapped(myKey02));

        bt2.alEventCallback0(myCallback01, context01);
        Assert.assertEquals(true,  bt2.isAlEventCallback0Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback0Mapped(myKey02));
        {
            final Set<AlEventCallback0Key> keys = bt2.getAlEventCallback0Keys();
            Assert.assertEquals(1, keys.size());
            Assert.assertEquals(true,  keys.contains(myKey01));
            Assert.assertEquals(false, keys.contains(myKey02));
        }
        Assert.assertEquals(myCallback01, bt2.getAlEventCallback0(myKey01));
        Assert.assertEquals(null, bt2.getAlEventCallback0(myKey02));
        Assert.assertEquals(-1, id_res[0]);
        Assert.assertEquals(null, msg_res[0]);

        {
            final String msgNo1 = "First message";
            id_res[0] = -1;
            msg_res[0] = null;
            bt2.alEventCallback0Inject(context01, 0, 1, 0, msgNo1);
            Assert.assertEquals(     1, id_res[0]);
            Assert.assertEquals(msgNo1, msg_res[0]);
        }
        {
            final String msgNo2 = "Second message";
            id_res[0] = -1;
            msg_res[0] = null;
            bt2.alEventCallback0Inject(context02, 0, 2, 0, msgNo2);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);

            bt2.alEventCallback0Inject(context01, 0, 2, 0, msgNo2);
            Assert.assertEquals(     2, id_res[0]);
            Assert.assertEquals(  msgNo2, msg_res[0]);
        }

        // Switch the callback function
        // The previously mapped myCallback01 (context01) gets released
        // and remapped to myCallback02 + ( context01 )(key)
        bt2.alEventCallback0(myCallback02, context01);
        Assert.assertEquals(true,  bt2.isAlEventCallback0Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback0Mapped(myKey02));
        {
            final Set<AlEventCallback0Key> keys = bt2.getAlEventCallback0Keys();
            Assert.assertEquals(1, keys.size());
            Assert.assertEquals(true,  keys.contains(myKey01));
            Assert.assertEquals(false, keys.contains(myKey02));
        }
        Assert.assertEquals(myCallback02, bt2.getAlEventCallback0(myKey01));
        Assert.assertEquals(null,         bt2.getAlEventCallback0(myKey02));

        {
            final String msgNo3 = "Third message";
            id_res[0] = -1;
            msg_res[0] = null;
            bt2.alEventCallback0Inject(context02, 0, 3, 0, msgNo3);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);

            bt2.alEventCallback0Inject(context01, 0, 3, 0, msgNo3);
            Assert.assertEquals(    3000, id_res[0]);
            Assert.assertEquals(  msgNo3, msg_res[0]);
        }

        // Fake release (wrong key)
        bt2.alEventCallback0(null, context02);
        Assert.assertEquals(true,  bt2.isAlEventCallback0Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback0Mapped(myKey02));

        // Just release the callback and mapped context01
        bt2.alEventCallback0(null, context01);
        Assert.assertEquals(false, bt2.isAlEventCallback0Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback0Mapped(myKey02));
        Assert.assertEquals(0, bt2.getAlEventCallback0Keys().size());

        {
            final String msgNo4 = "Forth message";
            id_res[0] = -1;
            msg_res[0] = null;
            bt2.alEventCallback0Inject(context01, 0, 4, 0, msgNo4);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);

            bt2.alEventCallback0Inject(context02, 0, 4, 0, msgNo4);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);
        }
    }

    /**
     * Test Bindingtest2 with ALEVENTPROCSOFT JavaCallback
     * on alEventCallback0(..) having the 'Object userParam` and `int object` as keys.
     */
    @Test
    public void chapter05b() throws Exception {
        final Bindingtest2 bt2 = new Bindingtest2Impl();

        final int[] id_res = { -1 };
        final String[] msg_res = { null };
        final ALEVENTPROCSOFT myCallback01 = new ALEVENTPROCSOFT() {
            @Override
            public void callback(final int eventType, final int object, final int param, final int length, final String message, final ALCcontext context) {
                id_res[0] = object;
                msg_res[0] = message;
                System.err.println("chapter05.myCallback01: type "+eventType+", obj "+object+", param "+param+", '"+message+"', userParam 0x"+
                        Integer.toHexString(System.identityHashCode(context)));
            }
        };
        final ALEVENTPROCSOFT myCallback02 = new ALEVENTPROCSOFT() {
            @Override
            public void callback(final int eventType, final int object, final int param, final int length, final String message, final ALCcontext context) {
                id_res[0] = 1000 * object;
                msg_res[0] = message;
                System.err.println("chapter05.myCallback02: type "+eventType+", obj "+object+", param "+param+", '"+message+"', userParam 0x"+
                        Integer.toHexString(System.identityHashCode(context)));
            }
        };
        final ALCcontext context01 = new ALCcontext();
        final ALCcontext context02 = new ALCcontext();
        final AlEventCallback1Key myKey01 = new AlEventCallback1Key(1, context01);
        final AlEventCallback1Key myKey02 = new AlEventCallback1Key(2, context02);
        Assert.assertEquals(false, bt2.isAlEventCallback1Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback1Mapped(myKey02));

        bt2.alEventCallback1(1, myCallback01, context01);
        Assert.assertEquals(true,  bt2.isAlEventCallback1Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback1Mapped(myKey02));
        {
            final Set<AlEventCallback1Key> keys = bt2.getAlEventCallback1Keys();
            Assert.assertEquals(1, keys.size());
            Assert.assertEquals(true,  keys.contains(myKey01));
            Assert.assertEquals(false, keys.contains(myKey02));
        }
        Assert.assertEquals(myCallback01, bt2.getAlEventCallback1(myKey01));
        Assert.assertEquals(null, bt2.getAlEventCallback1(myKey02));
        Assert.assertEquals(-1, id_res[0]);
        Assert.assertEquals(null, msg_res[0]);

        {
            final String msgNo1 = "First message";
            id_res[0] = -1;
            msg_res[0] = null;
            bt2.alEventCallback1Inject(context01, 0, 1, 0, msgNo1);
            Assert.assertEquals(     1, id_res[0]);
            Assert.assertEquals(msgNo1, msg_res[0]);
        }
        {
            final String msgNo2 = "Second message";
            id_res[0] = -1;
            msg_res[0] = null;
            bt2.alEventCallback1Inject(context02, 0, 2, 0, msgNo2);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);

            bt2.alEventCallback1Inject(context01, 0, 2, 0, msgNo2);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);

            bt2.alEventCallback1Inject(context01, 0, 1, 0, msgNo2);
            Assert.assertEquals(     1, id_res[0]);
            Assert.assertEquals(  msgNo2, msg_res[0]);
        }

        // Switch the callback function
        // The previously mapped myCallback01 (1, context01) gets released
        // and remapped to myCallback02 + ( 1, context01 )(key)
        bt2.alEventCallback1(1, myCallback02, context01);
        Assert.assertEquals(true,  bt2.isAlEventCallback1Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback1Mapped(myKey02));
        {
            final Set<AlEventCallback1Key> keys = bt2.getAlEventCallback1Keys();
            Assert.assertEquals(1, keys.size());
            Assert.assertEquals(true,  keys.contains(myKey01));
            Assert.assertEquals(false, keys.contains(myKey02));
        }
        Assert.assertEquals(myCallback02, bt2.getAlEventCallback1(myKey01));
        Assert.assertEquals(null,         bt2.getAlEventCallback1(myKey02));

        {
            final String msgNo3 = "Third message";
            id_res[0] = -1;
            msg_res[0] = null;
            bt2.alEventCallback1Inject(context02, 0, 2, 0, msgNo3);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);

            bt2.alEventCallback1Inject(context01, 0, 2, 0, msgNo3);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);

            bt2.alEventCallback1Inject(context01, 0, 1, 0, msgNo3);
            Assert.assertEquals(    1000, id_res[0]);
            Assert.assertEquals(  msgNo3, msg_res[0]);
        }

        // Fake release (wrong key)
        bt2.alEventCallback1(2, null, context02);
        Assert.assertEquals(true,  bt2.isAlEventCallback1Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback1Mapped(myKey02));

        bt2.alEventCallback1(2, null, context01);
        Assert.assertEquals(true,  bt2.isAlEventCallback1Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback1Mapped(myKey02));

        // Just release the callback and mapped context01
        bt2.alEventCallback1(1, null, context01);
        Assert.assertEquals(false, bt2.isAlEventCallback1Mapped(myKey01));
        Assert.assertEquals(false, bt2.isAlEventCallback1Mapped(myKey02));
        Assert.assertEquals(0, bt2.getAlEventCallback1Keys().size());

        {
            final String msgNo4 = "Forth message";
            id_res[0] = -1;
            msg_res[0] = null;
            bt2.alEventCallback1Inject(context01, 0, 4, 0, msgNo4);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);

            bt2.alEventCallback1Inject(context02, 0, 4, 0, msgNo4);
            Assert.assertEquals(    -1, id_res[0]);
            Assert.assertEquals(  null, msg_res[0]);
        }
    }

    /**
     * Test Bindingtest2 with T2_CallbackFunc11 JavaCallback via MessageCallback11a()
     * using the default MessageCallback11aKey class.
     */
    @Test
    public void chapter11a() throws Exception {
        final Bindingtest2 bt2 = new Bindingtest2Impl();

        final long userParam01Ptr = 0xAFFEBEAFC0FFEEL;
        final long userParam02Ptr = 0xC0FFEEDEADBEAFL;

        final long[] id_res = { -1 };
        final T2_CallbackFunc11 myCallback01 = new T2_CallbackFunc11() {
            @Override
            public void callback(final long id, final T2_Callback11UserType usrParam, final long val) {
                Assert.assertEquals(42, usrParam.getApiVersion()); // native toolkit should have set API version
                if( 1 == id ) {
                    BaseClass.assertAPTR(userParam01Ptr, usrParam.getData());
                } else if( 2 == id ) {
                    BaseClass.assertAPTR(userParam02Ptr, usrParam.getData());
                }
                final long res = val + usrParam.getI();
                id_res[0] = res;
                usrParam.setR(res);
                usrParam.setId(id);
                System.err.println("chapter11a.myCallback01: id "+id+", val "+val);
            }
        };
        final T2_CallbackFunc11 myCallback02 = new T2_CallbackFunc11() {
            @Override
            public void callback(final long id, final T2_Callback11UserType usrParam, final long val) {
                Assert.assertEquals(42, usrParam.getApiVersion()); // native toolkit should have set API version
                if( 1 == id ) {
                    BaseClass.assertAPTR(userParam01Ptr, usrParam.getData());
                } else if( 2 == id ) {
                    BaseClass.assertAPTR(userParam02Ptr, usrParam.getData());
                }
                final long res = val * usrParam.getI();
                id_res[0] = res;
                usrParam.setR(res);
                usrParam.setId(id);
                System.err.println("chapter11a.myCallback02: id "+id+", val "+val);
            }
        };
        final int id1 = 1;
        final int id2 = 2;
        final int id3 = 3;
        final MessageCallback11aKey id1Key = new MessageCallback11aKey(id1);
        final MessageCallback11aKey id2Key = new MessageCallback11aKey(id2);
        final MessageCallback11aKey id3Key = new MessageCallback11aKey(id3);
        final T2_Callback11UserType userParam01 = T2_Callback11UserType.create(); // native toolkit should set API version
        userParam01.setData(userParam01Ptr);
        userParam01.setI(1);
        final T2_Callback11UserType userParam02 = T2_Callback11UserType.create(); // native toolkit should set API version
        userParam02.setData(userParam02Ptr);
        userParam02.setI(2);
        Assert.assertEquals( 1, userParam01.getI());
        Assert.assertEquals( 0, userParam01.getR());
        Assert.assertEquals( 0, userParam01.getId());
        Assert.assertEquals( 2, userParam02.getI());
        Assert.assertEquals( 0, userParam02.getR());
        Assert.assertEquals( 0, userParam02.getId());

        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id1Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id3Key));
        Assert.assertEquals(0,     bt2.getMessageCallback11aKeys().size());

        // 1st mapping: buffer1 -> myCallback01, userParam01
        bt2.MessageCallback11a(id1, myCallback01, userParam01);
        Assert.assertEquals(true,  bt2.isMessageCallback11aMapped(id1Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id3Key));
        Assert.assertEquals(userParam01, bt2.getMessageCallback11aUserParam(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11aUserParam(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11aUserParam(id3Key));
        Assert.assertEquals(myCallback01,  bt2.getMessageCallback11a(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11a(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11a(id3Key));
        Assert.assertEquals(1,             bt2.getMessageCallback11aKeys().size());
        {
            final Set<MessageCallback11aKey> keys = bt2.getMessageCallback11aKeys();
            Assert.assertEquals(true,  keys.contains(id1Key));
            Assert.assertEquals(false, keys.contains(id2Key));
            Assert.assertEquals(false, keys.contains(id3Key));
        }

        // 2nd mapping: buffer2 -> myCallback02, userParam02
        bt2.MessageCallback11a(id2, myCallback02, userParam02);
        Assert.assertEquals(true,  bt2.isMessageCallback11aMapped(id1Key));
        Assert.assertEquals(true,  bt2.isMessageCallback11aMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id3Key));
        Assert.assertEquals(userParam01, bt2.getMessageCallback11aUserParam(id1Key));
        Assert.assertEquals(userParam02, bt2.getMessageCallback11aUserParam(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11aUserParam(id3Key));
        Assert.assertEquals(myCallback01,  bt2.getMessageCallback11a(id1Key));
        Assert.assertEquals(myCallback02,  bt2.getMessageCallback11a(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11a(id3Key));
        Assert.assertEquals(2,             bt2.getMessageCallback11aKeys().size());
        {
            final Set<MessageCallback11aKey> keys = bt2.getMessageCallback11aKeys();
            Assert.assertEquals(true,  keys.contains(id1Key));
            Assert.assertEquals(true,  keys.contains(id2Key));
            Assert.assertEquals(false, keys.contains(id3Key));
        }

        {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.MessageCallback11aInject(id1, 10); // buffer1 -> myCallback01, userParam01
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals(    10+1, id_res[0]);
            Assert.assertEquals(       1, userParam01.getI());
            Assert.assertEquals(    10+1, userParam01.getR());
            Assert.assertEquals(       1, userParam01.getId());
            Assert.assertEquals(       2, userParam02.getI());
            Assert.assertEquals(       0, userParam02.getR());
            Assert.assertEquals(       0, userParam02.getId());
        }
        {
            bt2.MessageCallback11aInject(id2, 10); // buffer2 -> myCallback02, userParam02
            Assert.assertEquals(    10*2, id_res[0]);
            Assert.assertEquals(       1, userParam01.getI());
            Assert.assertEquals(    10+1, userParam01.getR());
            Assert.assertEquals(       1, userParam01.getId());
            Assert.assertEquals(       2, userParam02.getI());
            Assert.assertEquals(    10*2, userParam02.getR());
            Assert.assertEquals(       2, userParam02.getId());
        }

        // Switch the callback function for buffer2 -> myCallback01, userParam02
        bt2.MessageCallback11a(id2, myCallback01, userParam02);
        Assert.assertEquals(true,  bt2.isMessageCallback11aMapped(id1Key));
        Assert.assertEquals(true,  bt2.isMessageCallback11aMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id3Key));
        Assert.assertEquals(userParam01, bt2.getMessageCallback11aUserParam(id1Key));
        Assert.assertEquals(userParam02, bt2.getMessageCallback11aUserParam(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11aUserParam(id3Key));
        Assert.assertEquals(myCallback01,  bt2.getMessageCallback11a(id1Key));
        Assert.assertEquals(myCallback01,  bt2.getMessageCallback11a(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11a(id3Key));

        {
            bt2.MessageCallback11aInject(id1, 11); // buffer1 -> myCallback01, userParam01
            Assert.assertEquals(    11+1, id_res[0]);
            Assert.assertEquals(       1, userParam01.getI());
            Assert.assertEquals(    11+1, userParam01.getR());
            Assert.assertEquals(       1, userParam01.getId());
            Assert.assertEquals(       2, userParam02.getI());
            Assert.assertEquals(    10*2, userParam02.getR());
            Assert.assertEquals(       2, userParam02.getId());
        }
        {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.MessageCallback11aInject(id2, 22); // buffer2 -> myCallback01, userParam02
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals(    22+2, id_res[0]);
            Assert.assertEquals(       1, userParam01.getI());
            Assert.assertEquals(    11+1, userParam01.getR());
            Assert.assertEquals(       1, userParam01.getId());
            Assert.assertEquals(       2, userParam02.getI());
            Assert.assertEquals(    22+2, userParam02.getR());
            Assert.assertEquals(       2, userParam02.getId());
        }

        // Just release the buffer2 callback and mapped resources
        bt2.MessageCallback11a(id2, null, userParam02); // usrptr is not key, only id is key!
        Assert.assertEquals(true,  bt2.isMessageCallback11aMapped(id1Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id3Key));
        Assert.assertEquals(userParam01, bt2.getMessageCallback11aUserParam(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11aUserParam(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11aUserParam(id3Key));
        Assert.assertEquals(myCallback01,  bt2.getMessageCallback11a(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11a(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11a(id3Key));
        Assert.assertEquals(1,             bt2.getMessageCallback11aKeys().size());
        {
            final Set<MessageCallback11aKey> keys = bt2.getMessageCallback11aKeys();
            Assert.assertEquals(true,  keys.contains(id1Key));
            Assert.assertEquals(false, keys.contains(id2Key));
            Assert.assertEquals(false, keys.contains(id3Key));
        }

        // Just release the buffer1 callback and mapped resources
        bt2.MessageCallback11a(id1, null, null); // usrptr is not key, only id is key!
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id1Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11aMapped(id3Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11aUserParam(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11aUserParam(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11aUserParam(id3Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11a(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11a(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11a(id3Key));
        Assert.assertEquals(0,             bt2.getMessageCallback11aKeys().size());
        {
            final Set<MessageCallback11aKey> keys = bt2.getMessageCallback11aKeys();
            Assert.assertEquals(false, keys.contains(id1Key));
            Assert.assertEquals(false, keys.contains(id2Key));
            Assert.assertEquals(false, keys.contains(id3Key));
        }

        {
            bt2.MessageCallback11aInject(id2, 5); // unmapped, no change in data
            Assert.assertEquals(    22+2, id_res[0]);
            Assert.assertEquals(       1, userParam01.getI());
            Assert.assertEquals(    11+1, userParam01.getR());
            Assert.assertEquals(       1, userParam01.getId());
            Assert.assertEquals(       2, userParam02.getI());
            Assert.assertEquals(    22+2, userParam02.getR());
            Assert.assertEquals(       2, userParam02.getId());
        }
    }

    /**
     * Test Bindingtest2 with T2_CallbackFunc11 JavaCallback via MessageCallback11b()
     * using the default MessageCallback11bKey class.
     */
    @Test
    public void chapter11b() throws Exception {
        final Bindingtest2 bt2 = new Bindingtest2Impl();

        final long userParam01Ptr = 0xAFFEBEAFC0FFEEL;
        final long userParam02Ptr = 0xC0FFEEDEADBEAFL;

        final long[] id_res = { -1 };
        final T2_CallbackFunc11 myCallback01 = new T2_CallbackFunc11() {
            @Override
            public void callback(final long id, final T2_Callback11UserType usrParam, final long val) {
                Assert.assertEquals(42, usrParam.getApiVersion()); // native toolkit should have set API version
                if( 1 == id ) {
                    BaseClass.assertAPTR(userParam01Ptr, usrParam.getData());
                } else if( 2 == id ) {
                    BaseClass.assertAPTR(userParam02Ptr, usrParam.getData());
                }
                final long res = val + id;
                id_res[0] = res;
                usrParam.setR(res);
                usrParam.setId(id);
                System.err.println("chapter11b.myCallback01: id "+id+", val "+val);
            }
        };
        final T2_CallbackFunc11 myCallback02 = new T2_CallbackFunc11() {
            @Override
            public void callback(final long id, final T2_Callback11UserType usrParam, final long val) {
                Assert.assertEquals(42, usrParam.getApiVersion()); // native toolkit should have set API version
                if( 1 == id ) {
                    BaseClass.assertAPTR(userParam01Ptr, usrParam.getData());
                } else if( 2 == id ) {
                    BaseClass.assertAPTR(userParam02Ptr, usrParam.getData());
                }
                final long res = val * id;
                id_res[0] = res;
                usrParam.setR(res);
                usrParam.setId(id);
                System.err.println("chapter11b.myCallback02: id "+id+", val "+val);
            }
        };
        final int id1 = 1;
        final int id2 = 2;
        final int id3 = 3;
        final MessageCallback11bKey id1Key = new MessageCallback11bKey(id1);
        final MessageCallback11bKey id2Key = new MessageCallback11bKey(id2);
        final MessageCallback11bKey id3Key = new MessageCallback11bKey(id3);

        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id1Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id3Key));
        Assert.assertEquals(0,     bt2.getMessageCallback11bKeys().size());

        // 1st mapping: buffer1 -> myCallback01, userParam01Ptr
        bt2.MessageCallback11b(id1, myCallback01, userParam01Ptr);
        Assert.assertEquals(true,  bt2.isMessageCallback11bMapped(id1Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id3Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11bUserParam(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11bUserParam(id3Key));
        Assert.assertEquals(myCallback01,  bt2.getMessageCallback11b(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11b(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11b(id3Key));
        Assert.assertEquals(1,             bt2.getMessageCallback11bKeys().size());
        {
            final Set<MessageCallback11bKey> keys = bt2.getMessageCallback11bKeys();
            Assert.assertEquals(true,  keys.contains(id1Key));
            Assert.assertEquals(false, keys.contains(id2Key));
            Assert.assertEquals(false, keys.contains(id3Key));
        }

        // 2nd mapping: buffer2 -> myCallback02, userParam02Ptr
        bt2.MessageCallback11b(id2, myCallback02, userParam02Ptr);
        Assert.assertEquals(true,  bt2.isMessageCallback11bMapped(id1Key));
        Assert.assertEquals(true,  bt2.isMessageCallback11bMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id3Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11bUserParam(id3Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11b(id3Key));
        Assert.assertEquals(2,             bt2.getMessageCallback11bKeys().size());
        {
            final Set<MessageCallback11bKey> keys = bt2.getMessageCallback11bKeys();
            Assert.assertEquals(true,  keys.contains(id1Key));
            Assert.assertEquals(true,  keys.contains(id2Key));
            Assert.assertEquals(false, keys.contains(id3Key));
        }

        {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.MessageCallback11bInject(id1, 10); // buffer1 -> myCallback01, userParam01
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals(    10+1, id_res[0]);
        }
        {
            bt2.MessageCallback11bInject(id2, 10); // buffer2 -> myCallback02, userParam02
            Assert.assertEquals(    10*2, id_res[0]);
        }

        // Switch the callback function for buffer2 -> myCallback01, userParam02Ptr
        bt2.MessageCallback11b(id2, myCallback01, userParam02Ptr);
        Assert.assertEquals(true,  bt2.isMessageCallback11bMapped(id1Key));
        Assert.assertEquals(true,  bt2.isMessageCallback11bMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id3Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11bUserParam(id3Key));
        Assert.assertEquals(myCallback01,  bt2.getMessageCallback11b(id1Key));
        Assert.assertEquals(myCallback01,  bt2.getMessageCallback11b(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11b(id3Key));

        {
            bt2.MessageCallback11bInject(id1, 11); // buffer1 -> myCallback01, userParam01
            Assert.assertEquals(    11+1, id_res[0]);
        }
        {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bt2.MessageCallback11bInject(id2, 22); // buffer2 -> myCallback01, userParam02
                }
            });
            thread.start();
            thread.join();
            Assert.assertEquals(    22+2, id_res[0]);
        }

        // Just release the buffer2 callback and mapped resources
        bt2.MessageCallback11b(id2, null, userParam02Ptr); // usrptr is not key, only id is key!
        Assert.assertEquals(true,  bt2.isMessageCallback11bMapped(id1Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id3Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11bUserParam(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11bUserParam(id3Key));
        Assert.assertEquals(myCallback01,  bt2.getMessageCallback11b(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11b(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11b(id3Key));
        Assert.assertEquals(1,             bt2.getMessageCallback11bKeys().size());
        {
            final Set<MessageCallback11bKey> keys = bt2.getMessageCallback11bKeys();
            Assert.assertEquals(true,  keys.contains(id1Key));
            Assert.assertEquals(false, keys.contains(id2Key));
            Assert.assertEquals(false, keys.contains(id3Key));
        }

        // Just release the buffer1 callback and mapped resources
        bt2.MessageCallback11b(id1, null, 0); // usrptr is not key, only id is key!
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id1Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id2Key));
        Assert.assertEquals(false, bt2.isMessageCallback11bMapped(id3Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11bUserParam(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11bUserParam(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11bUserParam(id3Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11b(id1Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11b(id2Key));
        Assert.assertEquals(null,          bt2.getMessageCallback11b(id3Key));
        Assert.assertEquals(0,             bt2.getMessageCallback11bKeys().size());
        {
            final Set<MessageCallback11bKey> keys = bt2.getMessageCallback11bKeys();
            Assert.assertEquals(false, keys.contains(id1Key));
            Assert.assertEquals(false, keys.contains(id2Key));
            Assert.assertEquals(false, keys.contains(id3Key));
        }

        {
            bt2.MessageCallback11bInject(id2, 5); // unmapped, no change in data
            Assert.assertEquals(    22+2, id_res[0]);
        }
    }

    public static class CustomMessageCallback11Key {
        public CustomMessageCallback11Key() {
        }
        @Override
        public boolean equals(final Object o) {
            if( this == o ) {
                return true;
            }
            if( !(o instanceof CustomAlBufferCallback1Key) ) {
                return false;
            }
            final CustomAlBufferCallback1Key o2 = (CustomAlBufferCallback1Key)o;
            return true;
        }
        @Override
        public int hashCode() {
            return 0;
        }
        @Override
        public String toString() {
            return "CustomMessageCallback11Key[this "+toHexString(System.identityHashCode(this))+"]";
        }
    }

    static private String toHexString(final int v) { return "0x"+Integer.toHexString(v); }

    public static void main(final String args[]) throws IOException {
        final String tstname = Test4JavaCallback.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}