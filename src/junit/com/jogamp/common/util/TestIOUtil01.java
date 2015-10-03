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

package com.jogamp.common.util;

import java.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.net.URIDumpUtil;
import com.jogamp.common.net.Uri;
import com.jogamp.common.os.MachineDataInfo;
import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIOUtil01 extends SingletonJunitCase {

    static final MachineDataInfo machine = Platform.getMachineDataInfo();
    static final int tsz = machine.pageSizeInBytes() + machine.pageSizeInBytes() / 2 ;
    static final byte[] orig = new byte[tsz];
    static final String tfilename = "./test.bin" ;

    @BeforeClass
    public static void setup() throws IOException {
        final File tfile = new File(tfilename);
        tfile.deleteOnExit();
        final OutputStream tout = new BufferedOutputStream(new FileOutputStream(tfile));
        for(int i=0; i<tsz; i++) {
            final byte b = (byte) (i%256);
            orig[i] = b;
            tout.write(b);
        }
        tout.close();
    }

    @AfterClass
    public static void cleanup() {
        final File tfile = new File(tfilename);
        tfile.delete();
    }

    @Test
    public void test01CleanPathString() throws IOException, URISyntaxException {
        {
            final String input    = "./dummy/nop/../a.txt";
            final String expected = "dummy/a.txt";
            Assert.assertEquals(expected, IOUtil.cleanPathString(input));
        }
        {
            final String input    = "../dummy/nop/../a.txt";
            final String expected = "../dummy/a.txt";
            Assert.assertEquals(expected, IOUtil.cleanPathString(input));
        }
        {
            final String input    = ".././dummy/nop/../a.txt";
            final String expected = "../dummy/a.txt";
            Assert.assertEquals(expected, IOUtil.cleanPathString(input));
        }
        {
            final String input    = "./../dummy/nop/../a.txt";
            final String expected = "../dummy/a.txt";
            Assert.assertEquals(expected, IOUtil.cleanPathString(input));
        }
        {
            final String input    = "../dummy/./nop/../a.txt";
            final String expected = "../dummy/a.txt";
            Assert.assertEquals(expected, IOUtil.cleanPathString(input));
        }
        {
            final String input    = "/dummy/nop/./../a.txt";
            final String expected = "/dummy/a.txt";
            Assert.assertEquals(expected, IOUtil.cleanPathString(input));
        }
        {
            final String input    = "dummy/../nop/./.././aaa/bbb/../../a.txt";
            final String expected = "a.txt";
            Assert.assertEquals(expected, IOUtil.cleanPathString(input));
        }
        {
            final String input    = "/dummy/../nop/./.././aaa/bbb/././ccc/../../../a.txt";
            final String expected = "/a.txt";
            Assert.assertEquals(expected, IOUtil.cleanPathString(input));
        }
        {
            URISyntaxException use = null;
            try {
                // Error case!
                final String input    = "../../error.txt";
                final String expected = "error.txt";
                final String result = IOUtil.cleanPathString(input); // URISyntaxException
                System.err.println("input   : "+input);
                System.err.println("expected: "+expected);
                System.err.println("result  : "+result);
                Assert.assertEquals(expected, result);
            } catch (final URISyntaxException _use) {
                use = _use;
                ExceptionUtils.dumpThrowable("", _use, 0, 3);
            }
            Assert.assertNotNull("URISyntaxException expected", use);
        }
        {
            URISyntaxException use = null;
            try {
                // Error case!
                final String input    = ".././a/../../error.txt";
                final String expected = "error.txt";
                final String result = IOUtil.cleanPathString(input); // URISyntaxException
                System.err.println("input   : "+input);
                System.err.println("expected: "+expected);
                System.err.println("result  : "+result);
                Assert.assertEquals(expected, result);
            } catch (final URISyntaxException _use) {
                use = _use;
                ExceptionUtils.dumpThrowable("", _use, 0, 3);
            }
            Assert.assertNotNull("URISyntaxException expected", use);
        }
    }

    @Test
    public void test11CopyStream01Array() throws IOException {
        final URLConnection urlConn = IOUtil.getResource(tfilename, this.getClass().getClassLoader(), this.getClass());
        Assert.assertNotNull(urlConn);
        final BufferedInputStream bis = new BufferedInputStream( urlConn.getInputStream() );
        final byte[] bb;
        try {
            bb = IOUtil.copyStream2ByteArray( bis );
        } finally {
            IOUtil.close(bis, false);
        }
        Assert.assertEquals("Byte number not equal orig vs array", orig.length, bb.length);
        Assert.assertTrue("Bytes not equal orig vs array", Arrays.equals(orig, bb));

    }

    @Test
    public void test12CopyStream02Buffer() throws IOException {
        final URLConnection urlConn = IOUtil.getResource(tfilename, this.getClass().getClassLoader(), this.getClass());
        Assert.assertNotNull(urlConn);
        final BufferedInputStream bis = new BufferedInputStream( urlConn.getInputStream() );
        final ByteBuffer bb;
        try {
            bb = IOUtil.copyStream2ByteBuffer( bis );
        } finally {
            IOUtil.close(bis, false);
        }
        Assert.assertEquals("Byte number not equal orig vs buffer", orig.length, bb.limit());
        int i;
        for(i=tsz-1; i>=0 && orig[i]==bb.get(i); i--) ;
        Assert.assertTrue("Bytes not equal orig vs array", 0>i);
    }

    @Test
    public void test13CopyStream03Buffer() throws IOException {
        final String tfilename2 = "./test2.bin" ;
        final URLConnection urlConn1 = IOUtil.getResource(tfilename, this.getClass().getClassLoader(), this.getClass());
        Assert.assertNotNull(urlConn1);

        final File file2 = new File(tfilename2);
        file2.deleteOnExit();
        try {
            IOUtil.copyURLConn2File(urlConn1, file2);
            final URLConnection urlConn2 = IOUtil.getResource(tfilename2, this.getClass().getClassLoader(), this.getClass());
            Assert.assertNotNull(urlConn2);

            final BufferedInputStream bis = new BufferedInputStream( urlConn2.getInputStream() );
            final ByteBuffer bb;
            try {
                bb = IOUtil.copyStream2ByteBuffer( bis );
            } finally {
                IOUtil.close(bis, false);
            }
            Assert.assertEquals("Byte number not equal orig vs buffer", orig.length, bb.limit());
            int i;
            for(i=tsz-1; i>=0 && orig[i]==bb.get(i); i--) ;
            Assert.assertTrue("Bytes not equal orig vs array", 0>i);
        } finally {
            file2.delete();
        }
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestIOUtil01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
