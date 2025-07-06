/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.junit.util.SingletonJunitCase;
import com.jogamp.junit.util.VersionSemanticsUtil;
import com.jogamp.junit.util.VersionSemanticsUtil.CompatibilityType;

/**
 * Compares a defined previous version with the current version.
 * <p>
 * {@link SimpleDiffCriteria} is chosen as it's {@link DiffCriteria},
 * i.e. considering:
 * <ul>
 *   <li>not synthetic classes, methods and fields which are either
 *   <ul>
 *     <li>public</li>
 *     <li>protected</li>
 *   </ul></li>
 * </ul>
 * </p>
 *
 * @throws IllegalArgumentException
 * @throws IOException
 * @throws URISyntaxException
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestVersionSemantics extends SingletonJunitCase {
    static final String jarFile = "gluegen-rt.jar";

    static final JogampVersion curVersion = GlueGenVersion.getInstance();
    static final VersionNumberString curVersionNumber = new VersionNumberString(curVersion.getImplementationVersion());

    static final String excludesDefault;
    static {
        excludesDefault = "jogamp";
    }

    @Test
    public void testVersionV220V221() throws IllegalArgumentException, IOException, URISyntaxException {
        testVersions(CompatibilityType.BACKWARD_COMPATIBLE_SOURCE, "2.2.0", "2.2.1", excludesDefault);
    }

    @Test
    public void testVersionV221V230() throws IllegalArgumentException, IOException, URISyntaxException {
        testVersions(CompatibilityType.NON_BACKWARD_COMPATIBLE, "2.2.1", "2.3.0", excludesDefault);
    }

    @Test
    public void testVersionV230V232() throws IllegalArgumentException, IOException, URISyntaxException {
        testVersions(CompatibilityType.BACKWARD_COMPATIBLE_BINARY, "2.3.0", "2.3.2", excludesDefault);
    }

    void testVersions(final CompatibilityType expectedCompatibilityType,
                      final String v1, final String v2, final String excludes)
                              throws IllegalArgumentException, IOException, URISyntaxException {
        final VersionNumberString preVersionNumber = new VersionNumberString(v1);
        final File previousJar = new File("lib/v"+v1+"/"+jarFile);

        final VersionNumberString curVersionNumber = new VersionNumberString(v2);
        final File currentJar = new File("lib/v"+v2+"/"+jarFile);

        VersionSemanticsUtil.testVersion2(expectedCompatibilityType,
                                          previousJar, preVersionNumber,
                                          currentJar, curVersionNumber, excludes, true);
    }

    @Test
    public void testVersionV232V24x() throws IllegalArgumentException, IOException, URISyntaxException {
        final CompatibilityType expectedCompatibilityType = CompatibilityType.NON_BACKWARD_COMPATIBLE;
        // final CompatibilityType expectedCompatibilityType = CompatibilityType.BACKWARD_COMPATIBLE_SOURCE;
        // final CompatibilityType expectedCompatibilityType = CompatibilityType.BACKWARD_COMPATIBLE_BINARY;

        final VersionNumberString preVersionNumber = new VersionNumberString("2.3.2");
        final File previousJar = new File("lib/v"+preVersionNumber.getVersionString()+"/"+jarFile);

        final ClassLoader currentCL = TestVersionSemantics.class.getClassLoader();

        VersionSemanticsUtil.testVersion2(expectedCompatibilityType,
                                          previousJar, preVersionNumber,
                                          curVersion.getClass(), currentCL, curVersionNumber, excludesDefault, true);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestVersionSemantics.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
