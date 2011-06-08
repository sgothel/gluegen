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

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.os.Platform;

public class TestPlatform01 {

    @Test
    public void testPageSize01()  {
        final int ps = Platform.getPageSize();
        System.err.println("PageSize: "+ps);
        Assert.assertTrue("PageSize is 0", 0 < ps );
        
        final int ps_pages = Platform.getPageNumber(ps);
        Assert.assertTrue("PageNumber of PageSize is not 1, but "+ps_pages, 1 == ps_pages);
        
        final int sz0 = ps - 10;
        final int sz0_pages = Platform.getPageNumber(sz0);
        Assert.assertTrue("PageNumber of PageSize-10 is not 1, but "+sz0_pages, 1 == sz0_pages);
        
        final int sz1 = ps + 10;
        final int sz1_pages = Platform.getPageNumber(sz1);
        Assert.assertTrue("PageNumber of PageSize+10 is not 2, but "+sz1_pages, 2 == sz1_pages);
        
        final int ps_psa = Platform.getPageAlignedSize(ps);
        Assert.assertTrue("PageAlignedSize of PageSize is not PageSize, but "+ps_psa, ps == ps_psa);
        
        final int sz0_psa = Platform.getPageAlignedSize(sz0);
        Assert.assertTrue("PageAlignedSize of PageSize-10 is not PageSize, but "+sz0_psa, ps == sz0_psa);
        
        final int sz1_psa = Platform.getPageAlignedSize(sz1);
        Assert.assertTrue("PageAlignedSize of PageSize+10 is not 2*PageSize, but "+sz1_psa, ps*2 == sz1_psa);
    }

    public static void main(String args[]) {
        String tstname = TestPlatform01.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
