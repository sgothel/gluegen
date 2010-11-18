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
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class VersionUtil {

    /**
     * Appends environment information like OS, JVM and CPU architecture properties to the StringBuffer.
     */
    public static StringBuffer getPlatformInfo(StringBuffer sb) {
        if(null == sb) {
            sb = new StringBuffer();
        }

        // environment
        sb.append("Platform: ").append(Platform.getOS()).append(' ').append(Platform.getOSVersion()).append(" (os), ");
        sb.append(Platform.getArch()).append(" (arch) ").append(Runtime.getRuntime().availableProcessors()).append(" cores");
        sb.append(Platform.getNewline());

        // arch
        sb.append("Platform: littleEndian ").append(Platform.isLittleEndian()).append(", 32Bit ");
        sb.append(Platform.is32Bit()).append(", a-ptr bit-size ").append(Platform.getPointerSizeInBits());
        sb.append(Platform.getNewline());

        // JVM/JRE
        sb.append("Platform: Java ").append(Platform.getJavaVersion()).append(", ").append(System.getProperty("java.vm.name")).append(", ");
        sb.append(Platform.getJavaVendor()).append(", ").append(Platform.getJavaVendorURL()).append(", is JavaSE: ").append(Platform.isJavaSE());
        sb.append(Platform.getNewline());

        return sb;
    }

    /**
     * Prints platfomr info.
     * @see #getPlatformInfo(java.lang.StringBuffer) 
     */
    public static String getPlatformInfo() {
        return getPlatformInfo(null).toString();
    }

    /**
     * Returns the manifest of the jar which contains the specified extension.
     * The provided ClassLoader is used for resource loading.
     * @param cl A ClassLoader which should find the manifest.
     * @param extension The value of the 'Extension-Name' jar-manifest attribute; used to identify the manifest.
     * @return the requested manifest or null when not found.
     */
    public static Manifest getManifest(ClassLoader cl, String extension) {
        try {
            Enumeration resources = cl.getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                Manifest manifest = new Manifest(((URL)resources.nextElement()).openStream());
                Attributes attributes = manifest.getMainAttributes();
                if(attributes != null && extension.equals(attributes.getValue(Attributes.Name.EXTENSION_NAME))) {
                    return manifest;
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read manifest.", ex);
        }
        return null;
    }

    public static StringBuffer getFullManifestInfo(Manifest mf, StringBuffer sb) {
        if(null==mf) {
            return sb;
        }

        if(null==sb) {
            sb = new StringBuffer();
        }

        Attributes attr = mf.getMainAttributes();
        Set keys = attr.keySet();
        for(Iterator iter=keys.iterator(); iter.hasNext(); ) {
            Attributes.Name key = (Attributes.Name) iter.next();
            String val = attr.getValue(key);
            sb.append(" ");
            sb.append(key);
            sb.append(" = ");
            sb.append(val);
            sb.append(Platform.getNewline());
        }
        return sb;
    }
}

