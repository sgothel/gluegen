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

import com.jogamp.common.os.Platform;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class VersionUtil {

    public static StringBuffer getInfo(ClassLoader cl, String prefix, String pkgName, String clazzName, StringBuffer sb) {

        if(null==sb) {
            sb = new StringBuffer();
        }

        sb.append(Platform.getNewline());
        sb.append("-----------------------------------------------------------------------------------------------------");
        sb.append(Platform.getNewline());
        getPlatformInfo(sb, prefix);
        sb.append(Platform.getNewline());
        getManifestInfo(cl, prefix, pkgName, clazzName, sb);
        sb.append("-----------------------------------------------------------------------------------------------------");
        sb.append(Platform.getNewline());

        return sb;
    }

    public static StringBuffer getPlatformInfo(StringBuffer sb, String prefix) {
        if(null==sb) {
            sb = new StringBuffer();
        }

        sb.append(prefix+" Platform: " + Platform.getOS() + " " + Platform.getOSVersion() + " (os), " + Platform.getArch() + " (arch)");
        sb.append(Platform.getNewline());
        sb.append(prefix+" Platform: littleEndian " + Platform.isLittleEndian() + ", 32Bit "+Platform.is32Bit() + ", a-ptr bit-size "+Platform.getPointerSizeInBits());
        sb.append(Platform.getNewline());
        sb.append(prefix+" Platform: Java " + Platform.getJavaVersion()+", "
            +Platform.getJavaVendor()+", "+Platform.getJavaVendorURL()+", is JavaSE: "+Platform.isJavaSE());
        sb.append(Platform.getNewline());

        return sb;
    }

    public static Manifest getManifest(ClassLoader cl, String fullClazzName) {
        String fullClazzFileName = "/" + fullClazzName.replace('.', '/') + ".class" ;
        URL url = cl.getClass().getResource(fullClazzFileName);
        Manifest mf = null;
        try {
            URLConnection urlConn = url.openConnection();
            if(urlConn instanceof JarURLConnection) {
                mf = ((JarURLConnection)urlConn).getManifest();
            }
        } catch (IOException ex) { }
        return mf;
    }

    public static StringBuffer getManifestInfo(ClassLoader cl, String prefix,
                                               String pkgName, String className, StringBuffer sb) {
        if(null==sb) {
            sb = new StringBuffer();
        }

        Manifest mf = getManifest(cl, pkgName + "." + className);
        
        if(null==mf) { 
            sb.append("Manifest for <");
            sb.append(pkgName);
            sb.append("> not available");
            sb.append(Platform.getNewline());
            return sb;
        }

        sb.append(prefix);
        sb.append(" package ");
        sb.append(pkgName);
        sb.append(Platform.getNewline());

        Attributes attr = mf.getMainAttributes();
        Set keys = attr.keySet();
        for(Iterator iter=keys.iterator(); iter.hasNext(); ) {
            Attributes.Name key = (Attributes.Name) iter.next();
            String val = attr.getValue(key);
            sb.append(prefix);
            sb.append(" ");
            sb.append(key);
            sb.append(" = ");
            sb.append(val);
            sb.append(Platform.getNewline());
        }
        return sb;
    }
}

