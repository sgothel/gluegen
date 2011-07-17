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
import java.net.URL;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import com.jogamp.common.os.MachineDescription;
import com.jogamp.common.os.Platform;

public class TestIOUtil01 {

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
        URL url = IOUtil.getResource(this.getClass(), tfilename);
        Assert.assertNotNull(url);
        final byte[] bb = IOUtil.copyStream2ByteArray( new BufferedInputStream( url.openStream() ) );        
        Assert.assertEquals("Byte number not equal orig vs array", orig.length, bb.length);
        Assert.assertTrue("Bytes not equal orig vs array", Arrays.equals(orig, bb));
        
    }

    @Test
    public void testCopyStream02Buffer() throws IOException {
        URL url = IOUtil.getResource(this.getClass(), tfilename);
        Assert.assertNotNull(url);
        final ByteBuffer bb = IOUtil.copyStream2ByteBuffer( new BufferedInputStream( url.openStream() ) );        
        Assert.assertEquals("Byte number not equal orig vs buffer", orig.length, bb.limit());
        int i;
        for(i=tsz-1; i>=0 && orig[i]==bb.get(i); i--) ;        
        Assert.assertTrue("Bytes not equal orig vs array", 0>i);
    }
    
    @Test
    public void testCopyStream03Buffer() throws IOException {
        final String tfilename2 = "./test2.bin" ;
        URL url1 = IOUtil.getResource(this.getClass(), tfilename);
        Assert.assertNotNull(url1);
        
        File file2 = new File(tfilename2);
        IOUtil.copyURL2File(url1, file2);
        URL url2 = IOUtil.getResource(this.getClass(), tfilename2);
        Assert.assertNotNull(url2);
        
        final ByteBuffer bb = IOUtil.copyStream2ByteBuffer( new BufferedInputStream( url2.openStream() ) );        
        Assert.assertEquals("Byte number not equal orig vs buffer", orig.length, bb.limit());
        int i;
        for(i=tsz-1; i>=0 && orig[i]==bb.get(i); i--) ;        
        Assert.assertTrue("Bytes not equal orig vs array", 0>i);
    }
    
    public static void main(String args[]) throws IOException {
        String tstname = TestIOUtil01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
