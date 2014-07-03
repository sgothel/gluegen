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

import java.security.*;
import java.util.HashSet;


/** Helper routines for accessing properties. */
public class PropertyAccess {
  /** trusted build-in property prefix 'jnlp.' */
  public static final String jnlp_prefix = "jnlp." ;
  /** trusted build-in property prefix 'javaws.' */
  public static final String javaws_prefix = "javaws.";

  static final HashSet<String> trustedPrefixes;
  static final HashSet<String> trusted;

  static {
      trustedPrefixes = new HashSet<String>();
      trustedPrefixes.add(javaws_prefix);
      trustedPrefixes.add(jnlp_prefix);
      // 'jogamp.' and maybe other trusted prefixes will be added later via 'addTrustedPrefix()'

      trusted = new HashSet<String>();
      trusted.add("sun.java2d.opengl");
      trusted.add("sun.java2d.noddraw");
      trusted.add("sun.java2d.d3d");
      trusted.add("sun.awt.noerasebackground");
  }

  /**
   * @param prefix New prefix to be registered as trusted.
   * @throws AccessControlException as thrown by {@link SecurityUtil#checkAllPermissions()}.
   */
  protected static final void addTrustedPrefix(final String prefix) throws AccessControlException {
      SecurityUtil.checkAllPermissions();
      trustedPrefixes.add(prefix);
  }

  public static final boolean isTrusted(final String propertyKey) {
      final int dot1 = propertyKey.indexOf('.');
      if(0<=dot1) {
          return trustedPrefixes.contains(propertyKey.substring(0,  dot1+1)) || trusted.contains(propertyKey);
      } else {
          return false;
      }
  }

  /** @see #getProperty(String, boolean) */
  public static final int getIntProperty(final String property, final boolean jnlpAlias, final int defaultValue) {
    int i=defaultValue;
    try {
        final String sv = PropertyAccess.getProperty(property, jnlpAlias);
        if(null!=sv) {
            i = Integer.parseInt(sv);
        }
    } catch (final NumberFormatException nfe) {}
    return i;
  }

  /** @see #getProperty(String, boolean) */
  public static final long getLongProperty(final String property, final boolean jnlpAlias, final long defaultValue) {
    long l=defaultValue;
    try {
        final String sv = PropertyAccess.getProperty(property, jnlpAlias);
        if(null!=sv) {
            l = Long.parseLong(sv);
        }
    } catch (final NumberFormatException nfe) {}
    return l;
  }

  /** @see #getProperty(String, boolean) */
  public static final boolean getBooleanProperty(final String property, final boolean jnlpAlias) {
    return Boolean.valueOf(PropertyAccess.getProperty(property, jnlpAlias)).booleanValue();
  }

  /** @see #getProperty(String, boolean) */
  public static final boolean getBooleanProperty(final String property, final boolean jnlpAlias, final boolean defaultValue) {
    final String valueS = PropertyAccess.getProperty(property, jnlpAlias);
    if(null != valueS) {
        return Boolean.valueOf(valueS).booleanValue();
    }
    return defaultValue;
  }

  /** @see #getProperty(String, boolean) */
  public static final boolean isPropertyDefined(final String property, final boolean jnlpAlias) {
    return (PropertyAccess.getProperty(property, jnlpAlias) != null) ? true : false;
  }

  /**
   * Query the property with the name <code>propertyKey</code>.
   * <p>
   * If <code>jnlpAlias</code> is <code>true</code> and the plain <code>propertyKey</code>
   * could not be resolved, an attempt to resolve the JNLP aliased <i>trusted property</i> is made.<br>
   * Example: For the propertyName <code>OneTwo</code>, the jnlp alias name is <code>jnlp.OneTwo</code>, which is considered trusted.<br>
   * </p>
   *
   * @param propertyKey the property name to query.
   * @param jnlpAlias true if a fallback attempt to query the JNLP aliased <i>trusted property</i> shall be made,
   *                  otherwise false.
   * @return the property value if exists, or null
   *
   * @throws NullPointerException if the property name is null
   * @throws IllegalArgumentException if the property name is of length 0
   * @throws SecurityException if access is not allowed to the given <code>propertyKey</code>
   *
   * @see System#getProperty(String)
   */
  public static final String getProperty(final String propertyKey, final boolean jnlpAlias)
      throws SecurityException, NullPointerException, IllegalArgumentException {
    if(null == propertyKey) {
        throw new NullPointerException("propertyKey is NULL");
    }
    if(0 == propertyKey.length()) {
        throw new IllegalArgumentException("propertyKey is empty");
    }
    String s=null;

    if( isTrusted(propertyKey) ) {
        // 'trusted' property (jnlp., javaws., jogamp., ..)
        s = getTrustedPropKey(propertyKey);
    } else {
        // may throw SecurityException, AccessControlerException
        s = System.getProperty(propertyKey);
    }
    if( null == s && jnlpAlias ) {
        // Try 'jnlp.' aliased property ..
        if( !propertyKey.startsWith(jnlp_prefix) ) {
            // Properties within the namespace "jnlp." or "javaws." should be considered trusted,
            // i.e. always granted w/o special privileges.
            s = getTrustedPropKey(jnlp_prefix + propertyKey);
        }
    }
    return s;
  }

  /** See {@link #getProperty(String, boolean)}, additionally allows a <code>defaultValue</code> if property value is <code>null</code>. */
  public static final String getProperty(final String propertyKey, final boolean jnlpAlias, final String defaultValue)
      throws SecurityException, NullPointerException, IllegalArgumentException {
    final String s = PropertyAccess.getProperty(propertyKey, jnlpAlias);
    if( null != s ) {
        return s;
    } else {
        return defaultValue;
    }
  }

  private static final String getTrustedPropKey(final String propertyKey) {
    return AccessController.doPrivileged(new PrivilegedAction<String>() {
        @Override
        public String run() {
          try {
              return System.getProperty(propertyKey);
          } catch (final SecurityException se) {
              throw new SecurityException("Could not access trusted property '"+propertyKey+"'", se);
          }
        }
      });
  }
}
