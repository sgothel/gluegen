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

import java.util.Iterator;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import jogamp.common.os.AndroidUtils;

import com.jogamp.common.os.Platform;

public class JogampVersion {

    /** See {@link #getImplementationBuild()} */
    public static final Attributes.Name IMPLEMENTATION_BUILD = new Attributes.Name("Implementation-Build");
    /** See {@link #getImplementationBranch()} */
    public static final Attributes.Name IMPLEMENTATION_BRANCH = new Attributes.Name("Implementation-Branch");
    /** See {@link #getImplementationCommit()} */
    public static final Attributes.Name IMPLEMENTATION_COMMIT = new Attributes.Name("Implementation-Commit");

    /** For FAT JogAmp jar files */
    private static final String packageNameFAT = "com.jogamp";

    private final String packageName;
    private final Manifest mf;
    private final int hash;
    private final Attributes mainAttributes;
    private final Set<?>/*<Attributes.Name>*/ mainAttributeNames;

    private final String androidPackageVersionName;

    protected JogampVersion(final String packageName, final Manifest mf) {
        if( null != mf ) {
            // use provided valid data
            this.mf = mf;
            this.packageName = packageName;
        } else {
            // try FAT jar file
            final Manifest fatMF = VersionUtil.getManifest(JogampVersion.class.getClassLoader(), packageNameFAT);
            if( null != fatMF ) {
                // use FAT jar file
                this.mf = fatMF;
                this.packageName = packageNameFAT;
            } else {
                // use faulty data, unresolvable ..
                this.mf = new Manifest();
                this.packageName = packageName;
            }
        }
        this.hash = this.mf.hashCode();
        mainAttributes = this.mf.getMainAttributes();
        mainAttributeNames = mainAttributes.keySet();
        androidPackageVersionName = AndroidUtils.getPackageInfoVersionName(this.packageName); // null if !Android
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(final Object o) {
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

    public final String getAttribute(final Attributes.Name attributeName) {
        return (null != attributeName) ? (String) mainAttributes.get(attributeName) : null;
    }

    public final String getAttribute(final String attributeName) {
        return getAttribute(getAttributeName(attributeName));
    }

    public final Attributes.Name getAttributeName(final String attributeName) {
        for (final Iterator<?> iter = mainAttributeNames.iterator(); iter.hasNext();) {
            final Attributes.Name an = (Attributes.Name) iter.next();
            if (an.toString().equals(attributeName)) {
                return an;
            }
        }
        return null;
    }

    /**
     * @return set of type {@link Attributes.Name}, disguised as anonymous
     */
    public final Set<?>/*<Attributes.Name>*/ getAttributeNames() {
        return mainAttributeNames;
    }

    public final String getExtensionName() {
        if(null != androidPackageVersionName) {
            return packageName;
        }
        return this.getAttribute(Attributes.Name.EXTENSION_NAME);
    }

    /**
     * Returns the implementation build number, e.g. <code>2.0-b456-20130328</code>.
     */
    public final String getImplementationBuild() {
        return this.getAttribute(JogampVersion.IMPLEMENTATION_BUILD);
    }

    /**
     * Returns the SCM branch name
     */
    public final String getImplementationBranch() {
        return this.getAttribute(JogampVersion.IMPLEMENTATION_BRANCH);
    }

    /**
     * Returns the SCM version of the last commit, e.g. git's sha1
     */
    public final String getImplementationCommit() {
        return this.getAttribute(JogampVersion.IMPLEMENTATION_COMMIT);
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

    /**
     * Returns the {@link Attributes.Name#IMPLEMENTATION_VERSION IMPLEMENTATION_VERSION}.
     * <p>
     * E.g. <code>2.0.2-rc-20130328</code> for snapshots prior to <code>2.0.2</code> release
     * and <code>2.0.2</code> for the upcoming release.
     * </p>
     */
    public final String getImplementationVersion() {
        return this.getAttribute(Attributes.Name.IMPLEMENTATION_VERSION);
    }

    public final String getAndroidPackageVersionName() {
        return androidPackageVersionName;
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

    public final StringBuilder getFullManifestInfo(final StringBuilder sb) {
        return VersionUtil.getFullManifestInfo(getManifest(), sb);
    }

    public StringBuilder getManifestInfo(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        final String nl = Platform.getNewline();
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
        sb.append("Implementation Build: ").append(getImplementationBuild()).append(nl);
        sb.append("Implementation Branch: ").append(getImplementationBranch()).append(nl);
        sb.append("Implementation Commit: ").append(getImplementationCommit()).append(nl);
        if(null != getAndroidPackageVersionName()) {
            sb.append("Android Package Version: ").append(getAndroidPackageVersionName()).append(nl);
        }
        return sb;
    }

    public StringBuilder toString(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }

        sb.append(VersionUtil.SEPERATOR).append(Platform.getNewline());
        getManifestInfo(sb);
        sb.append(VersionUtil.SEPERATOR);

        return sb;
    }

    @Override
    public String toString() {
        return toString(null).toString();
    }
}
