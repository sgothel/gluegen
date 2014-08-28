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
import org.osjava.jardiff.DiffCriteria;
import org.osjava.jardiff.SimpleDiffCriteria;
import org.semver.Delta;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionNumberString;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.junit.util.VersionSemanticsUtil;

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
public class TestVersionSemantics extends JunitTracer {
    static final String jarFile = "gluegen-rt.jar";
    static final VersionNumberString preVersionNumber = new VersionNumberString("2.2.0");
    // static final Delta.CompatibilityType expectedCompatibilityType = Delta.CompatibilityType.NON_BACKWARD_COMPATIBLE;
    static final Delta.CompatibilityType expectedCompatibilityType = Delta.CompatibilityType.BACKWARD_COMPATIBLE_USER;

    static final DiffCriteria diffCriteria = new SimpleDiffCriteria();
    // static final DiffCriteria diffCriteria = new PublicDiffCriteria();

    static final JogampVersion curVersion = GlueGenVersion.getInstance();
    static final VersionNumberString curVersionNumber = new VersionNumberString(curVersion.getImplementationVersion());

    static final Set<String> excludes;
    static {
        excludes = new HashSet<String>();
        excludes.add("^\\Qjogamp/\\E.*$");
    }

    @Test
    public void testVersionLatest() throws IllegalArgumentException, IOException, URISyntaxException {

        final File previousJar = new File("lib/v"+preVersionNumber.getVersionString()+"/"+jarFile);

        final ClassLoader currentCL = TestVersionSemantics.class.getClassLoader();

        VersionSemanticsUtil.testVersion(diffCriteria, expectedCompatibilityType,
                                         previousJar, preVersionNumber,
                                         curVersion.getClass(), currentCL, curVersionNumber, excludes);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestVersionSemantics.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }
}
