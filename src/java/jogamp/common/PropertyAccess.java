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

package jogamp.common;

import java.security.*;

/** Helper routines for accessing properties. */
public class PropertyAccess {
  public static final String jnlp_prefix = "jnlp." ;
  
  /** @see #getProperty(String, boolean, AccessControlContext) */
  public static final int getIntProperty(final String property, final boolean jnlpAlias, final AccessControlContext acc, int defaultValue) {
    int i=defaultValue;
    try {
        final String sv = PropertyAccess.getProperty(property, jnlpAlias, acc);
        if(null!=sv) {
            i = Integer.valueOf(sv).intValue();
        }
    } catch (NumberFormatException nfe) {}
    return i;
  }

  /** @see #getProperty(String, boolean, AccessControlContext) */
  public static final long getLongProperty(final String property, final boolean jnlpAlias, final AccessControlContext acc, long defaultValue) {
    long l=defaultValue;
    try {
        final String sv = PropertyAccess.getProperty(property, jnlpAlias, acc);
        if(null!=sv) {
            l = Long.valueOf(sv).longValue();
        }
    } catch (NumberFormatException nfe) {}
    return l;
  }

  /** @see #getProperty(String, boolean, AccessControlContext) */
  public static final boolean getBooleanProperty(final String property, final boolean jnlpAlias, final AccessControlContext acc) {
    return Boolean.valueOf(PropertyAccess.getProperty(property, jnlpAlias, acc)).booleanValue();
  }

  /** @see #getProperty(String, boolean, AccessControlContext) */
  public static final boolean getBooleanProperty(final String property, final boolean jnlpAlias, final AccessControlContext acc, boolean defaultValue) {
    final String valueS = PropertyAccess.getProperty(property, jnlpAlias, acc);
    if(null != valueS) {
        return Boolean.valueOf(valueS).booleanValue();
    }
    return defaultValue;
  }

  /** @see #getProperty(String, boolean, AccessControlContext) */
  public static final boolean isPropertyDefined(final String property, final boolean jnlpAlias, final AccessControlContext acc) {
    return (PropertyAccess.getProperty(property, jnlpAlias, acc) != null) ? true : false;
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
   * @param acc the AccessControlerContext to be used for privileged access to the system property, or null.
   * 
   * @return the property value if exists, or null
   * 
   * @throws NullPointerException if the property name is null
   * @throws IllegalArgumentException if the property name is of length 0
   * @throws SecurityException if no access to the JNLP aliased <i>trusted property</i> is allowed.
   *                           This is actually a bug in the JRE implementation, since the JNLP aliased <i>trusted properties</i>
   *                           shall be allowed without extended priviledges.  
   * 
   * @see System#getProperty(String)
   */
  public static final String getProperty(final String propertyKey, final boolean jnlpAlias, final AccessControlContext acc) 
      throws SecurityException, NullPointerException, IllegalArgumentException {
    if(null == propertyKey) {
        throw new NullPointerException("propertyKey is NULL");
    }
    if(0 == propertyKey.length()) {
        throw new IllegalArgumentException("propertyKey is empty");
    }
    String s=null;
    if( null!=acc ) {
        s = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
              String val=null;
              try {
                  val = System.getProperty(propertyKey);
              } catch (SecurityException se) {}
              return val;
            }
          }, acc);
    } else {
        try {
          s = System.getProperty(propertyKey);
        } catch (SecurityException se) {}
    }
    if(null==s && jnlpAlias && !propertyKey.startsWith(jnlp_prefix)) {
        // Properties within the namespace "jnlp." or "javaws." should be considered trusted,
        // i.e. always granted w/o special priviledges.
        // FIXME: Nevertheless we use this class AccessControlContext to ensure access
        //        on all supported implementations.
        s = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
              final String propertyKeyAliased = jnlp_prefix + propertyKey;
              String val = null;
              try {
                  val = System.getProperty(propertyKeyAliased);
              } catch (SecurityException se) {
                  throw new SecurityException("Could not access trusted property 'propertyKeyAliased'", se);
              }
              return val;
            }
          });
    }
    return s;
  }
}
