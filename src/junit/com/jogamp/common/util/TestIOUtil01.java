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
import java.net.URLConnection;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.os.MachineDescription;
import com.jogamp.common.os.Platform;
import com.jogamp.junit.util.JunitTracer;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIOUtil01 extends JunitTracer {

    static final MachineDescription machine = Platform.getMachineDescription();
    static final int tsz = machine.pageSizeInBytes() + machine.pageSizeInBytes() / 2 ;
    static final byte[] orig = new byte[tsz];
    static final String tfilename = "./test.bin" ;

    @BeforeClass
    public static void setup() throws IOException {
        final File tfile = new File(tfilename);
        final OutputStream tout = new BufferedOutputStream(new FileOutputStream(tfile));
        for(int i=0; i<tsz; i++) {
            final byte b = (byte) (i%256);
            orig[i] = b;
            tout.write(b);
        }
        tout.close();
    }

    @Test
    public void testCopyStream01Array() throws IOException {
        final URLConnection urlConn = IOUtil.getResource(this.getClass(), tfilename);
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
    public void testCopyStream02Buffer() throws IOException {
        final URLConnection urlConn = IOUtil.getResource(this.getClass(), tfilename);
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
    public void testCopyStream03Buffer() throws IOException {
        final String tfilename2 = "./test2.bin" ;
        final URLConnection urlConn1 = IOUtil.getResource(this.getClass(), tfilename);
        Assert.assertNotNull(urlConn1);

        final File file2 = new File(tfilename2);
        IOUtil.copyURLConn2File(urlConn1, file2);
        final URLConnection urlConn2 = IOUtil.getResource(this.getClass(), tfilename2);
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
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestIOUtil01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
