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

package com.jogamp.common;

import com.jogamp.common.util.JogampVersion;
import com.jogamp.common.util.SHASum;
import com.jogamp.common.util.VersionUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public class GlueGenVersion extends JogampVersion {

    protected static volatile GlueGenVersion jogampCommonVersionInfo;

    protected GlueGenVersion(final String packageName, final Manifest mf) {
        super(packageName, mf);
    }

    public static GlueGenVersion getInstance() {
        if(null == jogampCommonVersionInfo) { // volatile: ok
            synchronized(GlueGenVersion.class) {
                if( null == jogampCommonVersionInfo ) {
                    final String packageNameCompileTime = "com.jogamp.gluegen";
                    final String packageNameRuntime = "com.jogamp.common";
                    Manifest mf = VersionUtil.getManifest(GlueGenVersion.class.getClassLoader(), packageNameRuntime);
                    if(null != mf) {
                        jogampCommonVersionInfo = new GlueGenVersion(packageNameRuntime, mf);
                    } else {
                        mf = VersionUtil.getManifest(GlueGenVersion.class.getClassLoader(), packageNameCompileTime);
                        jogampCommonVersionInfo = new GlueGenVersion(packageNameCompileTime, mf);
                    }
                }
            }
        }
        return jogampCommonVersionInfo;
    }

    /**
     * {@code gluegen-rt.jar} definition of {@link SHASum.TempJarSHASum}'s specialization of {@link SHASum}.
     * <p>
     * Implementation uses {@link com.jogamp.common.util.cache.TempJarCache}.
     * </p>
     * <p>
     * Constructor defines the includes and excludes as used for {@code gluegen-rt.jar} {@link SHASum} computation.
     * </p>
     */
    public static class GluGenRTJarSHASum extends SHASum.TempJarSHASum {
        /**
         * See {@link GluGenRTJarSHASum}
         * @throws SecurityException
         * @throws IllegalArgumentException
         * @throws NoSuchAlgorithmException
         * @throws IOException
         * @throws URISyntaxException
         */
        public GluGenRTJarSHASum()
                throws SecurityException, IllegalArgumentException, NoSuchAlgorithmException, IOException, URISyntaxException
        {
            super(MessageDigest.getInstance("SHA-256"), GlueGenVersion.class, new ArrayList<Pattern>(), new ArrayList<Pattern>());
            final List<Pattern> excludes = getExcludes();
            final List<Pattern> includes = getIncludes();
            final String origin = getOrigin();
            excludes.add(Pattern.compile(origin+"/jogamp/android/launcher"));
            excludes.add(Pattern.compile(origin+"/jogamp/common/os/android"));
            excludes.add(Pattern.compile(origin+"/com/jogamp/gluegen/jcpp"));
            includes.add(Pattern.compile(origin+"/com/jogamp/gluegen/runtime/.*\\.class"));
            includes.add(Pattern.compile(origin+"/com/jogamp/common/.*"));
            includes.add(Pattern.compile(origin+"/jogamp/common/.*"));
        }
    }

    public static void main(final String args[]) {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
    }
}
