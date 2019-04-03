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

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.junit.util.SingletonJunitCase;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestVersionInfo extends SingletonJunitCase {
    static boolean VERBOSE = false;

    @Test
    public void test01Info() {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println("Version Info:");
        System.err.println(GlueGenVersion.getInstance());
        System.err.println("");
        System.err.println("Full Manifest:");
        System.err.println(GlueGenVersion.getInstance().getFullManifestInfo(null));
    }

    @Test
    public void test02ValidateSHA256()
            throws IllegalArgumentException, IOException, URISyntaxException, SecurityException, NoSuchAlgorithmException
    {
        final GlueGenVersion info = GlueGenVersion.getInstance();
        final String sha256ClassesThis = info.getImplementationSHA256ClassesThis();
        System.err.println("SHA256 CLASSES.this (build-time): "+sha256ClassesThis);

        final GlueGenVersion.GluGenRTJarSHASum shaSum = new GlueGenVersion.GluGenRTJarSHASum();
        final byte[] shasum = shaSum.compute(VERBOSE);
        final String sha256Classes = SHASum.toHexString(shasum, null).toString();
        System.err.println("SHA256 CLASSES.this (now): "+sha256Classes);
        Assert.assertEquals("SHA256 not equal", sha256ClassesThis, sha256Classes);
    }

    public static void main(final String args[]) throws IOException {
        // VERBOSE = true;
        final String tstname = TestVersionInfo.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
