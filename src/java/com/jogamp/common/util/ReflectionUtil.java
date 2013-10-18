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

import java.lang.reflect.*;

import com.jogamp.common.JogampRuntimeException;
import jogamp.common.Debug;

public final class ReflectionUtil {

    public static final boolean DEBUG = Debug.debug("ReflectionUtil");

    public static class AWTNames {
        public static final String ComponentClass = "java.awt.Component" ;
        public static final String GraphicsEnvironmentClass = "java.awt.GraphicsEnvironment";
        public static final String isHeadlessMethod = "isHeadless";
    }
    private static final Class<?>[] zeroTypes = new Class[0];

    /**
     * Returns true only if the class could be loaded.
     */
    public static final boolean isClassAvailable(String clazzName, ClassLoader cl) {
        try {
            return null != Class.forName(clazzName, false, cl);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Loads and returns the class or null.
     * @see Class#forName(java.lang.String, boolean, java.lang.ClassLoader)
     */
    public static final Class<?> getClass(String clazzName, boolean initialize, ClassLoader cl)
        throws JogampRuntimeException {
        try {
            return getClassImpl(clazzName, initialize, cl);
        } catch (ClassNotFoundException e) {
            throw new JogampRuntimeException(clazzName + " not available", e);
        }
    }

    private static Class<?> getClassImpl(String clazzName, boolean initialize, ClassLoader cl) throws ClassNotFoundException {
        return Class.forName(clazzName, initialize, cl);
    }

    /**
     * @throws JogampRuntimeException if the constructor can not be delivered.
     */
    public static final Constructor<?> getConstructor(String clazzName, Class<?>[] cstrArgTypes, ClassLoader cl)
        throws JogampRuntimeException {
        try {
            return getConstructor(getClassImpl(clazzName, true, cl), cstrArgTypes);
        } catch (ClassNotFoundException ex) {
            throw new JogampRuntimeException(clazzName + " not available", ex);
        }
    }

    static final String asString(Class<?>[] argTypes) {
        StringBuilder args = new StringBuilder();
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
    public static final Constructor<?> getConstructor(Class<?> clazz, Class<?> ... cstrArgTypes)
        throws JogampRuntimeException {
        if(null == cstrArgTypes) {
            cstrArgTypes = zeroTypes;
        }
        Constructor<?> cstr = null;
        try {
            cstr = clazz.getDeclaredConstructor(cstrArgTypes);
        } catch (NoSuchMethodException ex) {
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

  public static final Constructor<?> getConstructor(String clazzName, ClassLoader cl)
        throws JogampRuntimeException {
    return getConstructor(clazzName, null, cl);
  }

  /**
   * @throws JogampRuntimeException if the instance can not be created.
   */
  public static final Object createInstance(Constructor<?> cstr, Object ... cstrArgs)
      throws JogampRuntimeException, RuntimeException
  {
    try {
        return cstr.newInstance(cstrArgs);
    } catch (Exception e) {
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
  public static final Object createInstance(Class<?> clazz, Class<?>[] cstrArgTypes, Object ... cstrArgs)
      throws JogampRuntimeException, RuntimeException
  {
    return createInstance(getConstructor(clazz, cstrArgTypes), cstrArgs);
  }

  public static final Object createInstance(Class<?> clazz, Object ... cstrArgs)
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

  public static final Object createInstance(String clazzName, Class<?>[] cstrArgTypes, Object[] cstrArgs, ClassLoader cl)
      throws JogampRuntimeException, RuntimeException
  {
    try {
        return createInstance(getClassImpl(clazzName, true, cl), cstrArgTypes, cstrArgs);
    } catch (ClassNotFoundException ex) {
        throw new JogampRuntimeException(clazzName + " not available", ex);
    }
  }

  public static final Object createInstance(String clazzName, Object[] cstrArgs, ClassLoader cl)
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

  public static final Object createInstance(String clazzName, ClassLoader cl)
      throws JogampRuntimeException, RuntimeException
  {
    return createInstance(clazzName, null, null, cl);
  }

  public static final boolean instanceOf(Object obj, String clazzName) {
    return instanceOf(obj.getClass(), clazzName);
  }
  public static final boolean instanceOf(Class<?> clazz, String clazzName) {
    do {
        if(clazz.getName().equals(clazzName)) {
            return true;
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static final boolean implementationOf(Object obj, String faceName) {
    return implementationOf(obj.getClass(), faceName);
  }
  public static final boolean implementationOf(Class<?> clazz, String faceName) {
    do {
        Class<?>[] clazzes = clazz.getInterfaces();
        for(int i=clazzes.length-1; i>=0; i--) {
            Class<?> face = clazzes[i];
            if(face.getName().equals(faceName)) {
                return true;
            }
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static boolean isAWTComponent(Object target) {
      return instanceOf(target, AWTNames.ComponentClass);
  }

  public static boolean isAWTComponent(Class<?> clazz) {
      return instanceOf(clazz, AWTNames.ComponentClass);
  }

  /**
   * @throws JogampRuntimeException if the Method can not be found.
   */
  public static final Method getMethod(Class<?> clazz, String methodName, Class<?> ... argTypes)
      throws JogampRuntimeException, RuntimeException
  {
    Throwable t = null;
    Method m = null;
    try {
        m = clazz.getDeclaredMethod(methodName, argTypes);
    } catch (NoClassDefFoundError ex0) {
        t = ex0;
    } catch (NoSuchMethodException ex1) {
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
  public static final Method getMethod(String clazzName, String methodName, Class<?>[] argTypes, ClassLoader cl)
      throws JogampRuntimeException, RuntimeException
  {
    try {
        return getMethod(getClassImpl(clazzName, true, cl), methodName, argTypes);
    } catch (ClassNotFoundException ex) {
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
  public static final Object callMethod(Object instance, Method method, Object ... args)
      throws JogampRuntimeException, RuntimeException
  {
    try {
        return method.invoke(instance, args);
    } catch (Exception e) {
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
  public static final Object callStaticMethod(String clazzName, String methodName, Class<?>[] argTypes, Object[] args, ClassLoader cl)
      throws JogampRuntimeException, RuntimeException
  {
    return callMethod(null, getMethod(clazzName, methodName, argTypes, cl), args);
  }

  /** Convenient Method access class */
  public static class MethodAccessor {
    Method m = null;

    /** Check {@link #available()} before using instance. */
    public MethodAccessor(Class<?> clazz, String methodName, Class<?> ... argTypes) {
        try {
            m = ReflectionUtil.getMethod(clazz, methodName, argTypes);
        } catch (JogampRuntimeException jre) { /* method n/a */ }
    }

    /** Returns true if method is available, otherwise false. */
    public boolean available() {
        return null != m;
    }

    /**
     * Check {@link #available()} before calling to avoid throwing a JogampRuntimeException.
     * @throws JogampRuntimeException if method is not available
     */
    public Object callMethod(Object instance, Object ... args) {
        if(null == m) {
            throw new JogampRuntimeException("Method not available. Instance: "+instance);
        }
        return ReflectionUtil.callMethod(instance, m, args);
    }
  }

}

