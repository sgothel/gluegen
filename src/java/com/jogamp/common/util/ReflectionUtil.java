/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package com.jogamp.common.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jogamp.common.Debug;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.JogampRuntimeException;

public final class ReflectionUtil {

    public static final boolean DEBUG;
    public static final boolean DEBUG_STATS_FORNAME;

    private static final Object forNameLock;
    private static final Map<String, ClassNameLookup> forNameStats;
    private static int forNameCount = 0;
    private static long forNameNanoCosts = 0;

    static {
        Debug.initSingleton();
        DEBUG = Debug.debug("ReflectionUtil");
        DEBUG_STATS_FORNAME = PropertyAccess.isPropertyDefined("jogamp.debug.ReflectionUtil.forNameStats", true);
        if(DEBUG_STATS_FORNAME) {
            forNameLock = new Object();
            forNameStats = new HashMap<String, ClassNameLookup>();
        } else {
            forNameLock = null;
            forNameStats = null;
        }
    }

    public static class AWTNames {
        public static final String ComponentClass = "java.awt.Component" ;
        public static final String GraphicsEnvironmentClass = "java.awt.GraphicsEnvironment";
        public static final String isHeadlessMethod = "isHeadless";
    }
    private static final Class<?>[] zeroTypes = new Class[0];

    private static class ClassNameLookup {
        public ClassNameLookup(final String name) {
            this.name = name;
            this.nanoCosts = 0;
            this.count = 0;
        }
        public final String name;
        public long nanoCosts;
        public int count;
        @Override
        public String toString() {
            return String.format("%8.3f ms, %03d invoc, %s", nanoCosts/1e6, count, name);
        }
    }
    public static void resetForNameCount() {
        if(DEBUG_STATS_FORNAME) {
            synchronized(forNameLock) {
                forNameCount=0;
                forNameNanoCosts=0;
                forNameStats.clear();
            }
        }
    }
    public static StringBuilder getForNameStats(StringBuilder sb) {
        if( null == sb ) {
            sb = new StringBuilder();
        }
        if(DEBUG_STATS_FORNAME) {
            synchronized(forNameLock) {
                sb.append(String.format("ReflectionUtil.forName: %8.3f ms, %03d invoc%n", forNameNanoCosts/1e6, forNameCount));
                final Set<Entry<String, ClassNameLookup>> entries = forNameStats.entrySet();
                int entryNum = 0;
                for(final Iterator<Entry<String, ClassNameLookup>> iter = entries.iterator(); iter.hasNext(); entryNum++) {
                    final Entry<String, ClassNameLookup> entry = iter.next();
                    sb.append(String.format("ReflectionUtil.forName[%03d]: %s%n", entryNum, entry.getValue()));
                }
            }
        }
        return sb;
    }

    private static Class<?> getClassImpl(final String clazzName, final boolean initializeClazz, final ClassLoader cl) throws ClassNotFoundException {
        if(DEBUG_STATS_FORNAME) {
            final long t0 = System.nanoTime();
            final Class<?> res = Class.forName(clazzName, initializeClazz, cl);
            final long t1 = System.nanoTime();
            final long nanoCosts = t1 - t0;
            synchronized(forNameLock) {
                forNameCount++;
                forNameNanoCosts += nanoCosts;
                ClassNameLookup cnl = forNameStats.get(clazzName);
                if( null == cnl ) {
                    cnl = new ClassNameLookup(clazzName);
                    forNameStats.put(clazzName, cnl);
                }
                cnl.count++;
                cnl.nanoCosts += nanoCosts;
                System.err.printf("ReflectionUtil.getClassImpl.%03d: %8.3f ms, init %b, [%s]@ Thread %s%n",
                        forNameCount, nanoCosts/1e6, initializeClazz, cnl.toString(), Thread.currentThread().getName());
                if(DEBUG) {
                    ExceptionUtils.dumpStack(System.err);
                }
            }
            return res;
        } else {
            return Class.forName(clazzName, initializeClazz, cl);
        }
    }

    /**
     * Returns true only if the class could be loaded.
     */
    public static final boolean isClassAvailable(final String clazzName, final ClassLoader cl) {
        try {
            return null != getClassImpl(clazzName, false, cl);
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Loads and returns the class or null.
     * @see Class#forName(java.lang.String, boolean, java.lang.ClassLoader)
     */
    public static final Class<?> getClass(final String clazzName, final boolean initializeClazz, final ClassLoader cl)
        throws JogampRuntimeException {
        try {
            return getClassImpl(clazzName, initializeClazz, cl);
        } catch (final ClassNotFoundException e) {
            throw new JogampRuntimeException(clazzName + " not available", e);
        }
    }

    /**
     * @param initializeClazz TODO
     * @throws JogampRuntimeException if the constructor can not be delivered.
     */
    public static final Constructor<?> getConstructor(final String clazzName, final Class<?>[] cstrArgTypes, final boolean initializeClazz, final ClassLoader cl)
        throws JogampRuntimeException {
        try {
            return getConstructor(getClassImpl(clazzName, initializeClazz, cl), cstrArgTypes);
        } catch (final ClassNotFoundException ex) {
            throw new JogampRuntimeException(clazzName + " not available", ex);
        }
    }

    static final String asString(final Class<?>[] argTypes) {
        final StringBuilder args = new StringBuilder();
        boolean coma = false;
        if(null != argTypes) {
            for (int i = 0; i < argTypes.length; i++) {
                if(coma) {
                     args.append(", ");
                }
                args.append(argTypes[i].getName());
                coma = true;
            }
        }
        return args.toString();
    }

    /**
     * Returns a compatible constructor
     * if available, otherwise throws an exception.
     * <p>
     * It first attempts to get the specific Constructor
     * using the given <code>cstrArgTypes</code>.
     * If this fails w/ <code>NoSuchMethodException</code>, a compatible
     * Constructor is being looked-up w/ with parameter types assignable
     * from the given <code>cstrArgs</code>.
     * </p>
     *
     * @throws JogampRuntimeException if the constructor can not be delivered.
     */
    public static final Constructor<?> getConstructor(final Class<?> clazz, Class<?> ... cstrArgTypes)
        throws JogampRuntimeException {
        if(null == cstrArgTypes) {
            cstrArgTypes = zeroTypes;
        }
        Constructor<?> cstr = null;
        try {
            cstr = clazz.getDeclaredConstructor(cstrArgTypes);
        } catch (final NoSuchMethodException ex) {
            // ok, cont. w/ 'isAssignableFrom()' validation
        }
        if(null == cstr) {
            final Constructor<?>[] cstrs = clazz.getConstructors();
            for(int i=0; null==cstr && i<cstrs.length; i++) {
                final Constructor<?> c = cstrs[i];
                final Class<?>[] types = c.getParameterTypes();
                if(types.length == cstrArgTypes.length) {
                    int j;
                    for(j=0; j<types.length; j++) {
                        if(!types[j].isAssignableFrom(cstrArgTypes[j])) {
                            break; // cont w/ next cstr
                        }
                    }
                    if(types.length == j) {
                        cstr = c; // gotcha
                    }
                }
            }
        }
        if(null == cstr) {
            throw new JogampRuntimeException("Constructor: '" + clazz.getName() + "(" + asString(cstrArgTypes) + ")' not found");
        }
        return cstr;
    }

  public static final Constructor<?> getConstructor(final String clazzName, final ClassLoader cl)
        throws JogampRuntimeException {
    return getConstructor(clazzName, null, true, cl);
  }

  /**
   * @throws JogampRuntimeException if the instance can not be created.
   */
  public static final Object createInstance(final Constructor<?> cstr, final Object ... cstrArgs)
      throws JogampRuntimeException, RuntimeException
  {
    try {
        return cstr.newInstance(cstrArgs);
    } catch (final Exception e) {
      Throwable t = e;
      if (t instanceof InvocationTargetException) {
        t = ((InvocationTargetException) t).getTargetException();
      }
      if (t instanceof Error) {
        throw (Error) t;
      }
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      }
      throw new JogampRuntimeException("can not create instance of "+cstr.getName(), t);
    }
  }

  /**
   * @throws JogampRuntimeException if the instance can not be created.
   */
  public static final Object createInstance(final Class<?> clazz, final Class<?>[] cstrArgTypes, final Object ... cstrArgs)
      throws JogampRuntimeException, RuntimeException
  {
    return createInstance(getConstructor(clazz, cstrArgTypes), cstrArgs);
  }

  public static final Object createInstance(final Class<?> clazz, final Object ... cstrArgs)
      throws JogampRuntimeException, RuntimeException
  {
    Class<?>[] cstrArgTypes = null;
    if(null!=cstrArgs) {
        cstrArgTypes = new Class[cstrArgs.length];
        for(int i=0; i<cstrArgs.length; i++) {
            cstrArgTypes[i] = cstrArgs[i].getClass();
        }
    }
    return createInstance(clazz, cstrArgTypes, cstrArgs);
  }

  public static final Object createInstance(final String clazzName, final Class<?>[] cstrArgTypes, final Object[] cstrArgs, final ClassLoader cl)
      throws JogampRuntimeException, RuntimeException
  {
    try {
        return createInstance(getClassImpl(clazzName, true, cl), cstrArgTypes, cstrArgs);
    } catch (final ClassNotFoundException ex) {
        throw new JogampRuntimeException(clazzName + " not available", ex);
    }
  }

  public static final Object createInstance(final String clazzName, final Object[] cstrArgs, final ClassLoader cl)
      throws JogampRuntimeException, RuntimeException
  {
    Class<?>[] cstrArgTypes = null;
    if(null!=cstrArgs) {
        cstrArgTypes = new Class[cstrArgs.length];
        for(int i=0; i<cstrArgs.length; i++) {
            cstrArgTypes[i] = cstrArgs[i].getClass();
        }
    }
    return createInstance(clazzName, cstrArgTypes, cstrArgs, cl);
  }

  public static final Object createInstance(final String clazzName, final ClassLoader cl)
      throws JogampRuntimeException, RuntimeException
  {
    return createInstance(clazzName, null, null, cl);
  }

  public static final boolean instanceOf(final Object obj, final String clazzName) {
    return instanceOf(obj.getClass(), clazzName);
  }
  public static final boolean instanceOf(Class<?> clazz, final String clazzName) {
    do {
        if(clazz.getName().equals(clazzName)) {
            return true;
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static final boolean implementationOf(final Object obj, final String faceName) {
    return implementationOf(obj.getClass(), faceName);
  }
  public static final boolean implementationOf(Class<?> clazz, final String faceName) {
    do {
        final Class<?>[] clazzes = clazz.getInterfaces();
        for(int i=clazzes.length-1; i>=0; i--) {
            final Class<?> face = clazzes[i];
            if(face.getName().equals(faceName)) {
                return true;
            }
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static boolean isAWTComponent(final Object target) {
      return instanceOf(target, AWTNames.ComponentClass);
  }

  public static boolean isAWTComponent(final Class<?> clazz) {
      return instanceOf(clazz, AWTNames.ComponentClass);
  }

  /**
   * @throws JogampRuntimeException if the Method can not be found.
   */
  public static final Method getMethod(final Class<?> clazz, final String methodName, final Class<?> ... argTypes)
      throws JogampRuntimeException, RuntimeException
  {
    Throwable t = null;
    Method m = null;
    try {
        m = clazz.getDeclaredMethod(methodName, argTypes);
    } catch (final NoClassDefFoundError ex0) {
        t = ex0;
    } catch (final NoSuchMethodException ex1) {
        t = ex1;
    }
    if(null != t) {
        throw new JogampRuntimeException("Method: '" + clazz + "." + methodName + "(" + asString(argTypes) + ")' not found", t);
    }
    return m;
  }

  /**
   * @throws JogampRuntimeException if the Method can not be found.
   */
  public static final Method getMethod(final String clazzName, final String methodName, final Class<?>[] argTypes, final ClassLoader cl)
      throws JogampRuntimeException, RuntimeException
  {
    try {
        return getMethod(getClassImpl(clazzName, true, cl), methodName, argTypes);
    } catch (final ClassNotFoundException ex) {
        throw new JogampRuntimeException(clazzName + " not available", ex);
    }
  }

  /**
   * @param instance may be null in case of a static method
   * @param method the method to be called
   * @param args the method arguments
   * @return the methods result, maybe null if void
   * @throws JogampRuntimeException if call fails
   * @throws RuntimeException if call fails
   */
  public static final Object callMethod(final Object instance, final Method method, final Object ... args)
      throws JogampRuntimeException, RuntimeException
  {
    try {
        return method.invoke(instance, args);
    } catch (final Exception e) {
      Throwable t = e;
      if (t instanceof InvocationTargetException) {
        t = ((InvocationTargetException) t).getTargetException();
      }
      if (t instanceof Error) {
        throw (Error) t;
      }
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      }
      throw new JogampRuntimeException("calling "+method+" failed", t);
    }
  }

  /**
   * @throws JogampRuntimeException if the instance can not be created.
   */
  public static final Object callStaticMethod(final String clazzName, final String methodName, final Class<?>[] argTypes, final Object[] args, final ClassLoader cl)
      throws JogampRuntimeException, RuntimeException
  {
    return callMethod(null, getMethod(clazzName, methodName, argTypes, cl), args);
  }

  /** Convenient Method access class */
  public static class MethodAccessor {
    Method m = null;

    /** Check {@link #available()} before using instance. */
    public MethodAccessor(final Class<?> clazz, final String methodName, final Class<?> ... argTypes) {
        try {
            m = ReflectionUtil.getMethod(clazz, methodName, argTypes);
        } catch (final JogampRuntimeException jre) { /* method n/a */ }
    }

    /** Returns true if method is available, otherwise false. */
    public boolean available() {
        return null != m;
    }

    /**
     * Check {@link #available()} before calling to avoid throwing a JogampRuntimeException.
     * @throws JogampRuntimeException if method is not available
     */
    public Object callMethod(final Object instance, final Object ... args) {
        if(null == m) {
            throw new JogampRuntimeException("Method not available. Instance: "+instance);
        }
        return ReflectionUtil.callMethod(instance, m, args);
    }
  }

}

