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
package com.jogamp.junit.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Assert;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.VersionNumberString;

import japicmp.config.Options;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.output.markdown.MarkdownOutputGenerator;
import japicmp.output.markdown.config.MarkdownOptions;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.cmp.JApiCmpArchive;

public class VersionSemanticsUtil {

    /**
     * Library compatibility type. From most compatible to less compatible.
     */
    public enum CompatibilityType {

        /**
         * No (public) changes.
         */
        BACKWARD_COMPATIBLE_IMPLEMENTER,

        /**
         * Only <i>added</i> and <i>deprecated</i> changes,
         * i.e. source compatible changes.
         */
        BACKWARD_COMPATIBLE_SOURCE,

        /**
         * Contains binary compatible changes,
         * but may not be fully source compatible
         * and may contain changed values of fields.
         */
        BACKWARD_COMPATIBLE_BINARY,

        /**
         * Contains non binary compatible changes.
         */
        NON_BACKWARD_COMPATIBLE
    }

    public static void testVersion2(final CompatibilityType expectedCompatibilityType,
                                    final File previousJar, final VersionNumberString preVersionNumber,
                                    final Class<?> currentJarClazz, final ClassLoader currentJarCL, final VersionNumberString curVersionNumber,
                                    final Set<String> excludesRegExp) throws IllegalArgumentException, URISyntaxException, IOException
    {
        // Get containing JAR file "TestJarsInJar.jar" and add it to the TempJarCache
        final Uri currentJarUri = JarUtil.getJarUri(currentJarClazz.getName(), currentJarCL).getContainedUri();
        testVersion2(expectedCompatibilityType,
                    previousJar, preVersionNumber,
                    currentJarUri.toFile(), curVersionNumber,
                    excludesRegExp);
    }

    public static void testVersion2(final CompatibilityType expectedCompatibilityType,
                                    final File previousJar, final VersionNumberString preVersionNumber,
                                    final File currentJar, final VersionNumberString curVersionNumber,
                                    final Set<String> excludesRegExp) throws IllegalArgumentException, IOException, URISyntaxException
    {
        final JApiCmpArchive previous = new JApiCmpArchive(previousJar, preVersionNumber.getVersionString());
        final JApiCmpArchive current = new JApiCmpArchive(currentJar, curVersionNumber.getVersionString());
        final JarArchiveComparatorOptions comparatorOptions = new JarArchiveComparatorOptions();
        final JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

        CompatibilityType detectedCompatibilityType = CompatibilityType.BACKWARD_COMPATIBLE_IMPLEMENTER;
        final List<JApiClass> jApiClasses1 = jarArchiveComparator.compare(previous, current);
        final List<JApiClass> jApiClasses2 = new ArrayList<JApiClass>(jApiClasses1.size());
        for(final JApiClass jApiClass : jApiClasses1) {
            jApiClass.isBinaryCompatible();
            jApiClass.isSourceCompatible();
            final boolean unchanged = jApiClass.getChangeStatus() == JApiChangeStatus.UNCHANGED && jApiClass.getChangeStatus() != JApiChangeStatus.MODIFIED;
            if( !unchanged ) {
                jApiClasses2.add(jApiClass);
            }
            switch( detectedCompatibilityType ) {
                case BACKWARD_COMPATIBLE_IMPLEMENTER:
                    if( !unchanged ) {
                        if( !jApiClass.isBinaryCompatible() ) {
                            detectedCompatibilityType = CompatibilityType.NON_BACKWARD_COMPATIBLE;
                        } else if( !jApiClass.isSourceCompatible() ) {
                            detectedCompatibilityType = CompatibilityType.BACKWARD_COMPATIBLE_BINARY;
                        } else {
                            detectedCompatibilityType = CompatibilityType.BACKWARD_COMPATIBLE_SOURCE;
                        }
                    }
                    break;
                case BACKWARD_COMPATIBLE_SOURCE:
                    if( !unchanged ) {
                        if( !jApiClass.isBinaryCompatible() ) {
                            detectedCompatibilityType = CompatibilityType.NON_BACKWARD_COMPATIBLE;
                        } else if( !jApiClass.isSourceCompatible() ) {
                            detectedCompatibilityType = CompatibilityType.BACKWARD_COMPATIBLE_BINARY;
                        }
                    }
                    break;
                case BACKWARD_COMPATIBLE_BINARY:
                    if( !unchanged ) {
                        if( !jApiClass.isBinaryCompatible() ) {
                            detectedCompatibilityType = CompatibilityType.NON_BACKWARD_COMPATIBLE;
                        }
                    }
                    break;
                case NON_BACKWARD_COMPATIBLE:
                    // NOP
                    break;
            }
        }

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

        System.err.println("Semantic Version Test (japicmp)");
        System.err.println(" Previous version: "+preVersionNumber+" - "+previousJar.toString());
        System.err.println(" Current  version: "+curVersionNumber+" - "+currentJar.toString());
        System.err.println(" Compat. expected: "+expectedCompatibilityType);
        System.err.println(" Compat. detected: "+detectedCompatibilityType);
        System.err.println(" Compat. result:   detected "+compS+" expected -> "+(compOK ? "OK" : "ERROR"));
        final String resS;
        if( compOK ) {
            resS = " Current version "+curVersionNumber+" is "+expectedCompatibilityType+" to previous version "+preVersionNumber+", actually "+detectedCompatibilityType;
        } else {
            resS = " Current version "+curVersionNumber+" is not "+expectedCompatibilityType+" to previous version "+preVersionNumber+", but "+detectedCompatibilityType;
        }
        System.err.println(resS);
        System.err.printf("%n%n");

        final Options opts = Options.newDefault();
        opts.setReportOnlySummary(true);
        final MarkdownOptions mdOpts = MarkdownOptions.newDefault(opts);
        final MarkdownOutputGenerator mdGen = new MarkdownOutputGenerator(mdOpts, jApiClasses2);
        System.err.println(mdGen.generate());

        Assert.assertTrue(resS, compOK);

        /***
        //Provide version number for previous and current Jar files.
        final Version previous = Version.parse(...);
        final Version current = Version.parse(...);

        //Validates that current version number is valid based on semantic versioning principles.
        final boolean compatible = delta.validate(previous, current);
        */
    }
}
