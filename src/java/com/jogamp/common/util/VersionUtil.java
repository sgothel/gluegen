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

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.os.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import jogamp.common.Debug;
import jogamp.common.os.PlatformPropsImpl;

public class VersionUtil {
    private static final boolean DEBUG = Debug.debug("VersionUtil");

    public static final String SEPERATOR = "-----------------------------------------------------------------------------------------------------";

    /**
     * Appends environment information like OS, JVM and CPU architecture properties to the StringBuilder.
     */
    public static StringBuilder getPlatformInfo(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }

        sb.append(SEPERATOR).append(Platform.getNewline());

        // environment
        sb.append("Platform: ").append(Platform.getOSType()).append(" / ").append(Platform.getOSName()).append(' ').append(Platform.getOSVersion()).append(" (").append(Platform.getOSVersionNumber()).append("), ");
        sb.append(Platform.getArchName()).append(" (").append(Platform.getCPUType()).append(", ").append(Platform.getABIType()).append("), ");
        sb.append(Runtime.getRuntime().availableProcessors()).append(" cores, ").append("littleEndian ").append(PlatformPropsImpl.LITTLE_ENDIAN);
        sb.append(Platform.getNewline());
        if( Platform.OSType.ANDROID == PlatformPropsImpl.OS_TYPE ) {
            sb.append("Platform: Android Version: ").append(AndroidVersion.CODENAME).append(", ");
            sb.append(AndroidVersion.RELEASE).append(" [").append(AndroidVersion.RELEASE).append("], SDK: ").append(AndroidVersion.SDK_INT).append(", ").append(AndroidVersion.SDK_NAME);
            sb.append(Platform.getNewline());
        }

        Platform.getMachineDataInfo().toString(sb).append(Platform.getNewline());

        // JVM/JRE
        sb.append("Platform: Java Version: ").append(Platform.getJavaVersion()).append(" (").append(Platform.getJavaVersionNumber()).append("u").append(PlatformPropsImpl.JAVA_VERSION_UPDATE).append("), VM: ").append(Platform.getJavaVMName());
        sb.append(", Runtime: ").append(Platform.getJavaRuntimeName()).append(Platform.getNewline());
        sb.append("Platform: Java Vendor: ").append(Platform.getJavaVendor()).append(", ").append(Platform.getJavaVendorURL());
        if( PlatformPropsImpl.JAVA_21 ) {
            sb.append(", Java21");
        } else if( PlatformPropsImpl.JAVA_17 ) {
            sb.append(", Java17");
        } else if( PlatformPropsImpl.JAVA_9 ) {
            sb.append(", Java9");
        } else if( PlatformPropsImpl.JAVA_6 ) {
            sb.append(", Java6");
        } else if( PlatformPropsImpl.JAVA_SE ) {
            sb.append(", JavaSE");
        }
        sb.append(", dynamicLib: ").append(PlatformPropsImpl.useDynamicLibraries);
        sb.append(", AWT enabled: ").append(Platform.AWT_AVAILABLE);
        sb.append(Platform.getNewline()).append(SEPERATOR);

        return sb;
    }

    /**
     * Prints platform info.
     * @see #getPlatformInfo(java.lang.StringBuilder)
     */
    public static String getPlatformInfo() {
        return getPlatformInfo(null).toString();
    }

    /** Returns the manifest's Attributes.Name.EXTENSION_NAME, i.e. package-name. */
    public static String getExtensionName(final Manifest mf) {
        return getExtensionName(mf.getMainAttributes());
    }
    /** Returns the attributes' Attributes.Name.EXTENSION_NAME, i.e. package-name. */
    public static String getExtensionName(final Attributes attributes) {
        if(null != attributes) {
            return attributes.getValue( Attributes.Name.EXTENSION_NAME );
        }
        return null;
    }

    /**
     * Returns the manifest of the jar which contains the specified extension.
     * The provided ClassLoader is used for resource loading, while excluding its parent-classloader resources.
     * @param cl ClassLoader used to locate the manifest.
     * @param extension The value of the 'Extension-Name' jar-manifest attribute; used to identify the manifest.
     * @return the requested manifest, or null if not matching or none found.
     */
    public static Manifest getManifest(final ClassLoader cl, final String extension) {
        return getManifest(cl, new String[] { extension } );
    }

    /**
     * Returns the manifest of the jar which contains one of the specified extensions.
     * The provided ClassLoader is used for resource loading, while excluding its parent-classloader resources.
     * @param cl ClassLoader used to locate the manifest.
     * @param extensions The values of many 'Extension-Name's jar-manifest attribute; used to identify the manifest.
     *                   Matching is applied in decreasing order, i.e. first element is favored over the second, etc.
     * @return the requested manifest, or null if not matching or none found.
     */
    public static Manifest getManifest(final ClassLoader cl, final String[] extensions) {
        return getManifest(cl, extensions, false);
    }

    /**
     * Returns the manifest of the jar which contains one of the specified extensions.
     * The provided ClassLoader is used for resource loading, while excluding its parent-classloader resources.
     * @param cl ClassLoader used to locate the manifest.
     * @param extensions The values of many 'Extension-Name's jar-manifest attribute; used to identify the manifest.
     *                   Matching is applied in decreasing order, i.e. first element is favored over the second, etc.
     * @param acceptFirst pass true to accept the first Manifest w/ an extension-name if non matching extension is found
     * @return the requested manifest, otherwise the first found manifest w/ an extension-name or null when no manifest found.
     */
    public static Manifest getManifest(final ClassLoader cl, final String[] extensions, final boolean acceptFirst) {
        final Manifest[] extManifests = new Manifest[extensions.length];
        Manifest firstManifest = null;
        try {
            if( DEBUG ) {
                System.err.println();
                System.err.println("XXXX: getManifest: acceptFirst "+acceptFirst+", extensions "+Arrays.asList(extensions));
            }
            final List<URL> resources = getResources(cl, "META-INF/MANIFEST.MF");
            final List<URL> parentResources = getResources(cl.getParent(), "META-INF/MANIFEST.MF");
            if( DEBUG ) {
                for(final URL r : parentResources) {
                    System.err.println("XXXX: drop parent "+r);
                }
            }
            resources.removeAll(parentResources);
            if( DEBUG ) {
                for(final URL r : resources) {
                    System.err.println("XXXX: uniq "+r);
                }
            }
            for(final URL resource : resources) {
                final InputStream is = resource.openStream();
                final Manifest manifest;
                try {
                    manifest = new Manifest(is);
                } finally {
                    IOUtil.close(is, false);
                }
                final Attributes attributes = manifest.getMainAttributes();
                final String extensionName = getExtensionName(attributes);
                if( DEBUG ) { System.err.println("XXXX: ext-name "+extensionName+", resource "+resource); }
                if( null != extensionName && null != attributes) {
                    if( null == firstManifest ) {
                        firstManifest = manifest;
                    }
                    for(int i=0; i < extensions.length && null == extManifests[i]; i++) {
                        final String extension = extensions[i];
                        if( extension.equals( extensionName ) ) {
                            if( 0 == i ) {
                                return manifest; // 1st one has highest prio - done
                            }
                            extManifests[i] = manifest;
                        }
                    }
                }
            }
        } catch (final IOException ex) {
            throw new RuntimeException("Unable to read manifest.", ex);
        }
        for(int i=1; i<extManifests.length; i++) {
            if( null != extManifests[i] ) {
                final Manifest mf = extManifests[i];
                return mf;
            }
        }
        if( acceptFirst && null != firstManifest ) {
            return firstManifest;
        }
        return null;
    }
    private static List<URL> getResources(final ClassLoader cl, final String name) throws IOException {
        final List<URL> res = new ArrayList<URL>();
        if( null != cl ) {
            final Enumeration<URL> resources = cl.getResources(name);
            while (resources.hasMoreElements()) {
                final URL resource = resources.nextElement();
                res.add(resource);
            }
        }
        return res;
    }

    public static StringBuilder getFullManifestInfo(final Manifest mf, StringBuilder sb) {
        if(null==mf) {
            return sb;
        }

        if(null==sb) {
            sb = new StringBuilder();
        }

        final Attributes attr = mf.getMainAttributes();
        final Set<Object> keys = attr.keySet();
        for(final Iterator<Object> iter=keys.iterator(); iter.hasNext(); ) {
            final Attributes.Name key = (Attributes.Name) iter.next();
            final String val = attr.getValue(key);
            sb.append(" ");
            sb.append(key);
            sb.append(" = ");
            sb.append(val);
            sb.append(Platform.getNewline());
        }
        return sb;
    }
}

