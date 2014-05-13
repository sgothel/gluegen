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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osjava.jardiff.DiffCriteria;
import org.osjava.jardiff.SimpleDiffCriteria;
import org.semver.Comparer;
import org.semver.Delta;
import org.semver.Dumper;
import org.semver.Delta.Difference;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.VersionNumberString;

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
public class TestVersionSemantics {
    public static final String jarFile = "gluegen-rt.jar";
    public static final VersionNumberString preVersion = new VersionNumberString("2.1.5");
    public static final Delta.CompatibilityType expectedCompatibilityType = Delta.CompatibilityType.BACKWARD_COMPATIBLE_USER;

    @Test
    public void testVersion01() throws IllegalArgumentException, IOException, URISyntaxException {

        final GlueGenVersion glueGenVersion = GlueGenVersion.getInstance();
        final VersionNumberString curVersion = new VersionNumberString(glueGenVersion.getImplementationVersion());

        final File previousJar = new File("lib/v"+preVersion.getVersionString()+"/"+jarFile);

        final ClassLoader rootCL = TestVersionSemantics.class.getClassLoader();

        // Get containing JAR file "TestJarsInJar.jar" and add it to the TempJarCache
        final URI currentJarURI = JarUtil.getJarSubURI(GlueGenVersion.class.getName(), rootCL);
        final String currentJarLocS = IOUtil.decodeURIIfFilePath(currentJarURI);
        final File currentJar = new File(currentJarLocS);

        final DiffCriteria diffCriteria = new SimpleDiffCriteria();

        final Set<String> includes = new HashSet<String>();

        final Set<String> excludes = new HashSet<String>();
        excludes.add("jogamp/**");

        final Comparer comparer = new Comparer(diffCriteria, previousJar, currentJar, includes, excludes);
        final Delta delta = comparer.diff();

        //Validates that computed and provided compatibility type are compatible.
        final Delta.CompatibilityType detectedCompatibilityType = delta.computeCompatibilityType();
        final int comp = detectedCompatibilityType.compareTo(expectedCompatibilityType);
        final boolean compOK = 0 >= comp;
        final String compS;
        if( 0 > comp ) {
            compS = "< ";
        } else if ( 0 == comp ) {
            compS = "==";
        } else {
            compS = "> ";
        }

        System.err.println("GlueGen Semantic Versioning Test");
        System.err.println("Previous version:   "+preVersion+" - "+previousJar.toString());
        System.err.println("Current  version:   "+curVersion+" - "+currentJar.toString());
        System.err.println("Compat. expected:   "+expectedCompatibilityType);
        System.err.println("Compat. pre -> cur: "+detectedCompatibilityType);
        System.err.println("Compat. result:     detected "+compS+" expected -> "+(compOK ? "OK" : "ERROR"));

        final Set<Difference> diffs = delta.getDifferences();
        System.err.println(diffs.size()+" differences!");
        int diffI = 0;
        for(final Iterator<Difference> iter = diffs.iterator(); iter.hasNext(); diffI++) {
            final Difference diff = iter.next();
            System.err.printf("Diff %4d: %-11s in class %s%n", diffI, diff.getClass().getSimpleName(), diff.getClassName());
        }
        Dumper.dump(delta);

        Assert.assertTrue("Current version "+curVersion+" is "+compS+" of previous version "+preVersion, compOK);

        /***
        //Provide version number for previous and current Jar files.
        final Version previous = Version.parse(...);
        final Version current = Version.parse(...);

        //Validates that current version number is valid based on semantic versioning principles.
        final boolean compatible = delta.validate(previous, current);
        */
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestVersionSemantics.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
