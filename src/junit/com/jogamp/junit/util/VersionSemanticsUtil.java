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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.osjava.jardiff.DiffCriteria;
import org.semver.Comparer;
import org.semver.Delta;
import org.semver.Delta.Difference;
import org.semver.Dumper;

import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.VersionNumberString;

public class VersionSemanticsUtil {

    public static void testVersion(final DiffCriteria diffCriteria,
                                   final Delta.CompatibilityType expectedCompatibilityType,
                                   final File previousJar, final VersionNumberString preVersionNumber,
                                   final Class<?> currentJarClazz, final ClassLoader currentJarCL, final VersionNumberString curVersionNumber,
                                   final Set<String> excludesRegExp)
                                       throws IllegalArgumentException, IOException, URISyntaxException
    {
        // Get containing JAR file "TestJarsInJar.jar" and add it to the TempJarCache
        final URI currentJarURI = JarUtil.getJarSubURI(currentJarClazz.getName(), currentJarCL);
        final String currentJarLocS = IOUtil.decodeURIIfFilePath(currentJarURI);
        final File currentJar = new File(currentJarLocS);
        testVersion(diffCriteria, expectedCompatibilityType,
                    previousJar, preVersionNumber,
                    currentJar, curVersionNumber,
                    excludesRegExp);
    }

    public static void testVersion(final DiffCriteria diffCriteria,
                                   final Delta.CompatibilityType expectedCompatibilityType,
                                   final File previousJar, final VersionNumberString preVersionNumber,
                                   final File currentJar, final VersionNumberString curVersionNumber,
                                   final Set<String> excludesRegExp)
                                       throws IllegalArgumentException, IOException, URISyntaxException
    {
        final Set<String> includesRegExp = new HashSet<String>();

        final Comparer comparer = new Comparer(diffCriteria, previousJar, currentJar, includesRegExp, true, excludesRegExp, true);
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

        System.err.println("Semantic Version Test");
        System.err.println("Previous version: "+preVersionNumber+" - "+previousJar.toString());
        System.err.println("Current  version: "+curVersionNumber+" - "+currentJar.toString());
        System.err.println("Compat. expected: "+expectedCompatibilityType);
        System.err.println("Compat. detected: "+detectedCompatibilityType);
        System.err.println("Compat. result:   detected "+compS+" expected -> "+(compOK ? "OK" : "ERROR"));
        final String resS;
        if( compOK ) {
            resS = "Current version "+curVersionNumber+" is "+expectedCompatibilityType+" to previous version "+preVersionNumber+", actually "+detectedCompatibilityType;
        } else {
            resS = "Current version "+curVersionNumber+" is not "+expectedCompatibilityType+" to previous version "+preVersionNumber+", but "+detectedCompatibilityType;
        }
        System.err.println(resS);
        System.err.println("--------------------------------------------------------------------------------------------------------------");

        final Set<Difference> diffs = delta.getDifferences();

        final List<Difference> diffsAdd = new ArrayList<Difference>();
        final List<Difference> diffsChange = new ArrayList<Difference>();
        final List<Difference> diffsDeprecate = new ArrayList<Difference>();
        final List<Difference> diffsRemove = new ArrayList<Difference>();
        final Map<String, DiffCount> className2DiffCount = new HashMap<String, DiffCount>();

        int maxClassNameLen = 0;

        for(final Iterator<Difference> iter = diffs.iterator(); iter.hasNext(); ) {
            final Difference diff = iter.next();
            final String className = diff.getClassName();
            maxClassNameLen = Math.max(maxClassNameLen, className.length());

            DiffCount dc = className2DiffCount.get(className);
            if( null == dc ) {
                dc = new DiffCount(className);
                className2DiffCount.put(className, dc);
            }

            if( diff instanceof Delta.Add ) {
                diffsAdd.add(diff);
                dc.additions++;
            } else if( diff instanceof Delta.Change ) {
                diffsChange.add(diff);
                dc.changes++;
            } else if( diff instanceof Delta.Deprecate ) {
                diffsDeprecate.add(diff);
                dc.deprecates++;
            } else if( diff instanceof Delta.Remove ) {
                diffsRemove.add(diff);
                dc.removes++;
            }
        }
        Collections.sort(diffsAdd);
        Collections.sort(diffsChange);
        Collections.sort(diffsDeprecate);
        Collections.sort(diffsRemove);

        final List<String> classNames = new ArrayList<String>(className2DiffCount.keySet());
        Collections.sort(classNames);

        System.err.println("Summary: "+diffs.size()+" differences in "+classNames.size()+" classes:");
        System.err.println("  Remove "+diffsRemove.size()+
                           ", Change "+diffsChange.size()+
                           ", Deprecate "+diffsDeprecate.size()+
                           ", Add "+diffsAdd.size());
        System.err.println("--------------------------------------------------------------------------------------------------------------");

        int iterI = 0;
        for(final Iterator<String> iter = classNames.iterator(); iter.hasNext(); iterI++) {
            final String className = iter.next();
            final DiffCount dc = className2DiffCount.get(className);
            System.err.printf("%4d/%4d: %-"+maxClassNameLen+"s: %s%n", iterI, classNames.size(), className, dc.format(4));
        }

        System.err.println("--------------------------------------------------------------------------------------------------------------");
        System.err.println("Removes");
        System.err.println("--------------------------------------------------------------------------------------------------------------");
        Dumper.dump(diffsRemove, System.err);
        System.err.println("--------------------------------------------------------------------------------------------------------------");
        System.err.println("Changes");
        System.err.println("--------------------------------------------------------------------------------------------------------------");
        Dumper.dump(diffsChange, System.err);
        System.err.println("--------------------------------------------------------------------------------------------------------------");
        System.err.println("Deprecates");
        System.err.println("--------------------------------------------------------------------------------------------------------------");
        Dumper.dump(diffsDeprecate, System.err);
        System.err.println("--------------------------------------------------------------------------------------------------------------");
        System.err.println("Additions");
        System.err.println("--------------------------------------------------------------------------------------------------------------");
        Dumper.dump(diffsAdd, System.err);
        System.err.println("==============================================================================================================");

        Assert.assertTrue(resS, compOK);

        /***
        //Provide version number for previous and current Jar files.
        final Version previous = Version.parse(...);
        final Version current = Version.parse(...);

        //Validates that current version number is valid based on semantic versioning principles.
        final boolean compatible = delta.validate(previous, current);
        */
    }
    static class DiffCount {
        public DiffCount(String name) { this.name = name; }
        public final String name;
        public int removes;
        public int changes;
        public int deprecates;
        public int additions;
        public String toString() { return name+": Remove "+removes+", Change "+changes+", Deprecate "+deprecates+", Add "+additions; }
        public String format(final int digits) {
            return String.format("Remove %"+digits+"d, Change %"+digits+"d, Deprecate %"+digits+"d, Add %"+digits+"d",
                                    removes, changes, deprecates, additions);
        }
    }
}
