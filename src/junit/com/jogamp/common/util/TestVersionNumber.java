/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestVersionNumber extends SingletonJunitCase {

    @Test
    public void test01() {
        final String vs00 = "1.0.16";
        final String vs01 = "OpenGL ES GLSL ES 1.0.16";
        final String vs02 = "1.0.16 OpenGL ES GLSL ES";
        final VersionNumber vn0 = new VersionNumber(1, 0, 16);
        Assert.assertTrue(vn0.hasMajor());
        Assert.assertTrue(vn0.hasMinor());
        Assert.assertTrue(vn0.hasSub());

        VersionNumber vn;

        vn = new VersionNumber(vs00);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs01);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs02);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);
    }
    @Test
    public void test01b() {
        final String delim = ",";

        final String vs00 = "1,0,16";
        final String vs01 = "OpenGL ES GLSL ES 1,0,16";
        final String vs02 = "1,0,16 OpenGL ES GLSL ES";
        final VersionNumber vn0 = new VersionNumber(1, 0, 16);
        Assert.assertTrue(vn0.hasMajor());
        Assert.assertTrue(vn0.hasMinor());
        Assert.assertTrue(vn0.hasSub());

        VersionNumber vn;

        vn = new VersionNumber(vs00, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs01, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs02, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);
    }

    @Test
    public void test02() {
        final String vs00 = "4.20";
        final String vs01 = "COMPANY via Stupid tool 4.20";
        final String vs02 = "4.20 COMPANY via Stupid tool";
        final VersionNumber vn0 = new VersionNumber(4, 20, 0);
        Assert.assertTrue(vn0.hasMajor());
        Assert.assertTrue(vn0.hasMinor());
        Assert.assertTrue(vn0.hasSub());

        VersionNumber vn;

        vn = new VersionNumber(vs00);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(!vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs01);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(!vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs02);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(!vn.hasSub());
        Assert.assertEquals(vn0, vn);
    }

    @Test
    public void test02b() {
        final String delim = ",";

        final String vs00 = "4,20";
        final String vs01 = "COMPANY via Stupid tool 4,20";
        final String vs02 = "4,20 COMPANY via Stupid tool";
        final VersionNumber vn0 = new VersionNumber(4, 20, 0);
        Assert.assertTrue(vn0.hasMajor());
        Assert.assertTrue(vn0.hasMinor());
        Assert.assertTrue(vn0.hasSub());

        VersionNumber vn;

        vn = new VersionNumber(vs00, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(!vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs01, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(!vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs02, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(!vn.hasSub());
        Assert.assertEquals(vn0, vn);
    }

    @Test
    public void test03() {
        final String vs00 = "A10.11.12b";
        final String vs01 = "Prelim Text 10.Funny11.Weird12 Something is odd";
        final String vs02 = "Prelim Text 10.Funny11l1.Weird12 2 Something is odd";
        final VersionNumber vn0 = new VersionNumber(10, 11, 12);
        Assert.assertTrue(vn0.hasMajor());
        Assert.assertTrue(vn0.hasMinor());
        Assert.assertTrue(vn0.hasSub());

        VersionNumber vn;

        vn = new VersionNumber(vs00);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs01);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs02);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);
    }

    @Test
    public void test03b() {
        final String delim = ",";

        final String vs00 = "A10,11,12b";
        final String vs01 = "Prelim Text 10,Funny11,Weird12 Something is odd";
        final String vs02 = "Prelim Text 10,Funny11l1,Weird12 2 Something is odd";
        final VersionNumber vn0 = new VersionNumber(10, 11, 12);
        Assert.assertTrue(vn0.hasMajor());
        Assert.assertTrue(vn0.hasMinor());
        Assert.assertTrue(vn0.hasSub());

        VersionNumber vn;

        vn = new VersionNumber(vs00, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs01, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs02, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);
    }

    @Test
    public void test04() {
        final String vs00 = "A10.11.12b (git-d6c318e)";
        final String vs01 = "Prelim Text 10.Funny11.Weird12 Something is odd (git-d6c318e)";
        final String vs02 = "Prelim Text 10.Funny11l1.Weird12 2 Something is odd (git-d6c318e)";
        final VersionNumber vn0 = new VersionNumber(10, 11, 12);
        Assert.assertTrue(vn0.hasMajor());
        Assert.assertTrue(vn0.hasMinor());
        Assert.assertTrue(vn0.hasSub());

        VersionNumber vn;

        vn = new VersionNumber(vs00);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs01);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs02);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);
    }
    @Test
    public void test04b() {
        final String delim = ",";

        final String vs00 = "A10,11,12b (git-d6c318e)";
        final String vs01 = "Prelim Text 10,Funny11,Weird12 Something is odd (git-d6c318e)";
        final String vs02 = "Prelim Text 10,Funny11l1,Weird12 2 Something is odd (git-d6c318e)";
        final VersionNumber vn0 = new VersionNumber(10, 11, 12);
        Assert.assertTrue(vn0.hasMajor());
        Assert.assertTrue(vn0.hasMinor());
        Assert.assertTrue(vn0.hasSub());

        VersionNumber vn;

        vn = new VersionNumber(vs00, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs01, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);

        vn = new VersionNumber(vs02, delim);
        Assert.assertTrue(vn.hasMajor());
        Assert.assertTrue(vn.hasMinor());
        Assert.assertTrue(vn.hasSub());
        Assert.assertEquals(vn0, vn);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestVersionNumber.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
