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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

public class SecurityUtil {
    /* package private */ static final AccessControlContext localACC;
    /* package private */ static final Certificate[] localCerts;
    
    static {
        localACC = AccessController.doPrivileged(new PrivilegedAction<AccessControlContext>() {
                                public AccessControlContext run() { 
                                    return AccessController.getContext(); 
                                } } );        
        localCerts = getCerts(SecurityUtil.class);
    }
    
    public static final Certificate[] getCerts(final Class<?> clz) {
        final ProtectionDomain pd = AccessController.doPrivileged(new PrivilegedAction<ProtectionDomain>() {
                                        public ProtectionDomain run() {
                                            return clz.getProtectionDomain();
                                        } } );                
        final CodeSource cs = (null != pd) ? pd.getCodeSource() : null;
        final Certificate[] certs = (null != cs) ? cs.getCertificates() : null;
        return (null != certs && certs.length>0) ? certs : null;
    }
    
    public static final boolean equals(Certificate[] a, Certificate[] b) {
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
    
    public static final boolean equalsLocalCert(Certificate[] b) {
        return equals(localCerts, b);
    }
    
    public static final boolean equalsLocalCert(Class<?> clz) {
        return equalsLocalCert(getCerts(clz));
    }
    
    public static final AccessControlContext getCommonAccessControlContext(Class<?> clz) {
        if(equalsLocalCert(clz)) {
            return localACC;
        } else {
            return null;
        }
    }
}
