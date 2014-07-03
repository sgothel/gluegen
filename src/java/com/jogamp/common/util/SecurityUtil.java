/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

public class SecurityUtil {
    private static final SecurityManager securityManager;
    private static final Permission allPermissions;
    private static final boolean DEBUG = false;

    static {
        allPermissions = new AllPermission();
        securityManager = System.getSecurityManager();

        if( DEBUG ) {
            final boolean hasAllPermissions;
            {
                final ProtectionDomain insecPD = AccessController.doPrivileged(new PrivilegedAction<ProtectionDomain>() {
                                                @Override
                                                public ProtectionDomain run() {
                                                    return SecurityUtil.class.getProtectionDomain();
                                                } } );
                boolean _hasAllPermissions;
                try {
                    insecPD.implies(allPermissions);
                    _hasAllPermissions = true;
                } catch( final SecurityException ace ) {
                    _hasAllPermissions = false;
                }
                hasAllPermissions = _hasAllPermissions;
            }

            System.err.println("SecurityUtil: Has SecurityManager: "+ ( null != securityManager ) ) ;
            System.err.println("SecurityUtil: Has AllPermissions: "+hasAllPermissions);
            final Certificate[] certs = AccessController.doPrivileged(new PrivilegedAction<Certificate[]>() {
                                                @Override
                                                public Certificate[] run() {
                                                    return getCerts(SecurityUtil.class);
                                                } } );
            System.err.println("SecurityUtil: Cert count: "+ ( null != certs ? certs.length : 0 ));
            if( null != certs ) {
                for(int i=0; i<certs.length; i++) {
                    System.err.println("\t cert["+i+"]: "+certs[i].toString());
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if no {@link SecurityManager} has been installed
     * or the installed {@link SecurityManager}'s <code>checkPermission(new AllPermission())</code>
     * passes. Otherwise method returns <code>false</code>.
     */
    public static final boolean hasAllPermissions() {
        return hasPermission(allPermissions);
    }

    /**
     * Returns <code>true</code> if no {@link SecurityManager} has been installed
     * or the installed {@link SecurityManager}'s <code>checkPermission(perm)</code>
     * passes. Otherwise method returns <code>false</code>.
     */
    public static final boolean hasPermission(final Permission perm) {
        try {
            checkPermission(perm);
            return true;
        } catch( final SecurityException ace ) {
            return false;
        }
    }

    /**
     * Throws an {@link SecurityException} if an installed {@link SecurityManager}
     * does not permit the requested {@link AllPermission}.
     */
    public static final void checkAllPermissions() throws SecurityException {
        checkPermission(allPermissions);
    }

    /**
     * Throws an {@link SecurityException} if an installed {@link SecurityManager}
     * does not permit the requested {@link Permission}.
     */
    public static final void checkPermission(final Permission perm) throws SecurityException {
        if( null != securityManager ) {
            securityManager.checkPermission(perm);
        }
    }

    /**
     * Returns <code>true</code> if no {@link SecurityManager} has been installed
     * or the installed {@link SecurityManager}'s <code>checkLink(libName)</code>
     * passes. Otherwise method returns <code>false</code>.
     */
    public static final boolean hasLinkPermission(final String libName) {
        try {
            checkLinkPermission(libName);
            return true;
        } catch( final SecurityException ace ) {
            return false;
        }
    }

    /**
     * Throws an {@link SecurityException} if an installed {@link SecurityManager}
     * does not permit to dynamically link the given libName.
     */
    public static final void checkLinkPermission(final String libName) throws SecurityException {
        if( null != securityManager ) {
            securityManager.checkLink(libName);
        }
    }

    /**
     * Throws an {@link SecurityException} if an installed {@link SecurityManager}
     * does not permit to dynamically link to all libraries.
     */
    public static final void checkAllLinkPermission() throws SecurityException {
        if( null != securityManager ) {
            securityManager.checkPermission(allLinkPermission);
        }
    }
    private static final RuntimePermission allLinkPermission = new RuntimePermission("loadLibrary.*");

    /**
     * @param clz
     * @return
     * @throws SecurityException if the caller has no permission to access the ProtectedDomain of the given class.
     */
    public static final Certificate[] getCerts(final Class<?> clz) throws SecurityException {
        final ProtectionDomain pd = clz.getProtectionDomain();
        final CodeSource cs = (null != pd) ? pd.getCodeSource() : null;
        final Certificate[] certs = (null != cs) ? cs.getCertificates() : null;
        return (null != certs && certs.length>0) ? certs : null;
    }

    public static final boolean equals(final Certificate[] a, final Certificate[] b) {
        if(a == b) {
            return true;
        }
        if(a==null || b==null) {
            return false;
        }
        if(a.length != b.length) {
            return false;
        }

        int i = 0;
        while( i < a.length && a[i].equals(b[i]) ) {
            i++;
        }
        return i == a.length;
    }
}
