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

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class JogampVersion {

    public static Attributes.Name IMPLEMENTATION_BRANCH = new Attributes.Name("Implementation-Branch");
    public static Attributes.Name IMPLEMENTATION_COMMIT = new Attributes.Name("Implementation-Commit");
    
    protected static final String SEPERATOR = "-----------------------------------------------------------------------------------------------------";

    private String packageName;
    private Manifest mf;
    private int hash;
    private Attributes mainAttributes;
    private Set/*<Attributes.Name>*/ mainAttributeNames;

    protected JogampVersion(String packageName, Manifest mf) {
        this.packageName = packageName;
        this.mf = mf;
        this.hash = mf.hashCode();
        mainAttributes = mf.getMainAttributes();
        mainAttributeNames = mainAttributes.keySet();
    }

    public final int hashCode() {
        return hash;
    }

    public final boolean equals(Object o) {
        if (o instanceof JogampVersion) {
            return mf.equals(((JogampVersion) o).getManifest());
        }
        return false;
    }

    public final Manifest getManifest() {
        return mf;
    }

    public final String getPackageName() {
        return packageName;
    }

    public final String getAttribute(Attributes.Name attributeName) {
        return (null != attributeName) ? (String) mainAttributes.get(attributeName) : null;
    }

    public final String getAttribute(String attributeName) {
        return getAttribute(getAttributeName(attributeName));
    }

    public final Attributes.Name getAttributeName(String attributeName) {
        for (Iterator iter = mainAttributeNames.iterator(); iter.hasNext();) {
            Attributes.Name an = (Attributes.Name) iter.next();
            if (an.toString().equals(attributeName)) {
                return an;
            }
        }
        return null;
    }

    public final Set getAttributeNames() {
        return mainAttributeNames;
    }

    public final String getExtensionName() {
        return this.getAttribute(Attributes.Name.EXTENSION_NAME);
    }

    public final String getImplementationBranch() {
        return this.getAttribute(GlueGenVersion.IMPLEMENTATION_BRANCH);
    }

    public final String getImplementationCommit() {
        return this.getAttribute(GlueGenVersion.IMPLEMENTATION_COMMIT);
    }

    public final String getImplementationTitle() {
        return this.getAttribute(Attributes.Name.IMPLEMENTATION_TITLE);
    }

    public final String getImplementationVendor() {
        return this.getAttribute(Attributes.Name.IMPLEMENTATION_VENDOR);
    }

    public final String getImplementationVendorID() {
        return this.getAttribute(Attributes.Name.IMPLEMENTATION_VENDOR_ID);
    }

    public final String getImplementationURL() {
        return this.getAttribute(Attributes.Name.IMPLEMENTATION_URL);
    }

    public final String getImplementationVersion() {
        return this.getAttribute(Attributes.Name.IMPLEMENTATION_VERSION);
    }

    public final String getSpecificationTitle() {
        return this.getAttribute(Attributes.Name.SPECIFICATION_TITLE);
    }

    public final String getSpecificationVendor() {
        return this.getAttribute(Attributes.Name.SPECIFICATION_VENDOR);
    }

    public final String getSpecificationVersion() {
        return this.getAttribute(Attributes.Name.SPECIFICATION_VERSION);
    }

    public final StringBuffer getFullManifestInfo(StringBuffer sb) {
        return VersionUtil.getFullManifestInfo(getManifest(), sb);
    }

    public StringBuffer getPlatformInfo(StringBuffer sb) {
        return VersionUtil.getPlatformInfo(sb);
    }

    public StringBuffer getManifestInfo(StringBuffer sb) {
        if(null==sb) {
            sb = new StringBuffer();
        }
        String nl = Platform.getNewline();
        sb.append("Package: ").append(getPackageName()).append(nl);
        sb.append("Extension Name: ").append(getExtensionName()).append(nl);
        sb.append("Specification Title: ").append(getSpecificationTitle()).append(nl);
        sb.append("Specification Vendor: ").append(getSpecificationVendor()).append(nl);
        sb.append("Specification Version: ").append(getSpecificationVersion()).append(nl);
        sb.append("Implementation Title: ").append(getImplementationTitle()).append(nl);
        sb.append("Implementation Vendor: ").append(getImplementationVendor()).append(nl);
        sb.append("Implementation Vendor ID: ").append(getImplementationVendorID()).append(nl);
        sb.append("Implementation URL: ").append(getImplementationURL()).append(nl);
        sb.append("Implementation Version: ").append(getImplementationVersion()).append(nl);
        sb.append("Implementation Branch: ").append(getImplementationBranch()).append(nl);
        sb.append("Implementation Commit: ").append(getImplementationCommit()).append(nl);
        return sb;
    }

    public StringBuffer toString(StringBuffer sb) {
        if(null==sb) {
            sb = new StringBuffer();
        }

        String nl = Platform.getNewline();
        sb.append(SEPERATOR).append(nl);
        getManifestInfo(sb);
        sb.append(SEPERATOR).append(nl);

        return sb;
    }
    
    public String toString() {
        return toString(null).toString();
    }
}
