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
import com.jogamp.common.util.VersionUtil;
import java.util.jar.Manifest;

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

    public static void main(final String args[]) {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
    }
}
