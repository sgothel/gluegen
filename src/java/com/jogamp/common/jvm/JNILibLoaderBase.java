/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.common.jvm;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.jogamp.common.net.Uri;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.cache.TempJarCache;

import jogamp.common.Debug;
import jogamp.common.os.PlatformPropsImpl;

public class JNILibLoaderBase {
  public static final boolean DEBUG;
  protected static final boolean PERF;

  static {
      Debug.initSingleton();
      DEBUG = Debug.debug("JNILibLoader");
      PERF = DEBUG || PropertyAccess.isPropertyDefined("jogamp.debug.JNILibLoader.Perf", true);
  }

  private static final Object perfSync = new Object();
  private static long perfTotal = 0;
  private static long perfCount = 0;

  public interface LoaderAction {
    /**
     * Loads the library specified by libname.<br>
     * The implementation should ignore, if the library has been loaded already.<br>
     * @param libname the library to load
     * @param ignoreError if true, errors during loading the library should be ignored
     * @param cl optional ClassLoader, used to locate the library
     * @return true if library loaded successful
     */
    boolean loadLibrary(String libname, boolean ignoreError, ClassLoader cl);

    /**
     * Loads the library specified by libname.<br>
     * Optionally preloads the libraries specified by preload.<br>
     * The implementation should ignore, if any library has been loaded already.<br>
     * @param libname the library to load
     * @param preload the libraries to load before loading the main library if not null
     * @param preloadIgnoreError if true, errors during loading the preload-libraries should be ignored
     * @param cl optional ClassLoader, used to locate the library
     */
    void loadLibrary(String libname, String[] preload, boolean preloadIgnoreError, ClassLoader cl);
  }

  private static class DefaultAction implements LoaderAction {
    @Override
    public boolean loadLibrary(final String libname, final boolean ignoreError, final ClassLoader cl) {
      boolean res = true;
      if(!isLoaded(libname)) {
          try {
            loadLibraryInternal(libname, cl);
            addLoaded(libname);
            if(DEBUG) {
                System.err.println("JNILibLoaderBase: loaded "+libname);
            }
          } catch (final UnsatisfiedLinkError e) {
            res = false;
            if(DEBUG) {
                e.printStackTrace();
            }
            if (!ignoreError && e.getMessage().indexOf("already loaded") < 0) {
              throw e;
            }
          }
      }
      return res;
    }

    @Override
    public void loadLibrary(final String libname, final String[] preload, final boolean preloadIgnoreError, final ClassLoader cl) {
      if(!isLoaded(libname)) {
          if (null!=preload) {
            for (int i=0; i<preload.length; i++) {
              loadLibrary(preload[i], preloadIgnoreError, cl);
            }
          }
          loadLibrary(libname, false, cl);
      }
    }
  }

  private static final HashSet<String> loaded = new HashSet<String>();
  private static LoaderAction loaderAction = new DefaultAction();

  public static boolean isLoaded(final String libName) {
    return loaded.contains(libName);
  }

  public static void addLoaded(final String libName) {
    loaded.add(libName);
    if(DEBUG) {
        System.err.println("JNILibLoaderBase: Loaded Native Library: "+libName);
    }
  }

  public static void disableLoading() {
    setLoadingAction(null);
  }

  public static void enableLoading() {
    setLoadingAction(new DefaultAction());
  }

  public static synchronized void setLoadingAction(final LoaderAction action) {
    loaderAction = action;
  }

  private static final String nativeJarTagPackage = "jogamp.nativetag"; // TODO: sync with gluegen-cpptasks-base.xml

  /**
   *
   * @param classFromJavaJar
   * @param classJarUri
   * @param jarBasename jar basename w/ suffix
   * @param nativeJarBasename native jar basename w/ suffix
   * @return
   * @throws IOException
   * @throws SecurityException
   * @throws URISyntaxException
   */
  private static final boolean addNativeJarLibsImpl(final Class<?> classFromJavaJar, final Uri classJarUri,
                                                    final Uri.Encoded jarBasename, final Uri.Encoded nativeJarBasename)
    throws IOException, SecurityException, URISyntaxException
  {
    if (DEBUG) {
        final StringBuilder msg = new StringBuilder();
        msg.append("JNILibLoaderBase: addNativeJarLibsImpl(").append(PlatformPropsImpl.NEWLINE);
        msg.append("  classFromJavaJar  = ").append(classFromJavaJar).append(PlatformPropsImpl.NEWLINE);
        msg.append("  classJarURI       = ").append(classJarUri).append(PlatformPropsImpl.NEWLINE);
        msg.append("  jarBasename       = ").append(jarBasename).append(PlatformPropsImpl.NEWLINE);
        msg.append("  os.and.arch       = ").append(PlatformPropsImpl.os_and_arch).append(PlatformPropsImpl.NEWLINE);
        msg.append("  nativeJarBasename = ").append(nativeJarBasename).append(PlatformPropsImpl.NEWLINE);
        msg.append(")");
        System.err.println(msg.toString());
    }
    final long t0 = PERF ? System.currentTimeMillis() : 0; // 'Platform.currentTimeMillis()' not yet available!

    boolean ok = false;

    final Uri jarSubURI = classJarUri.getContainedUri();
    if (null == jarSubURI) {
        throw new IllegalArgumentException("JarSubURI is null of: "+classJarUri);
    }

    final Uri jarSubUriRoot = jarSubURI.getDirectory();

    if (DEBUG) {
        System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl: initial: %s -> %s%n", jarSubURI, jarSubUriRoot);
    }

    final String nativeLibraryPath = String.format("natives/%s/", PlatformPropsImpl.os_and_arch);
    if (DEBUG) {
        System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl: nativeLibraryPath: %s%n", nativeLibraryPath);
    }
    {
        // Attempt-1 a 'one slim native jar file' per 'os.and.arch' layout
        // with native platform libraries under 'natives/os.and.arch'!
        final Uri nativeJarURI = JarUtil.getJarFileUri( jarSubUriRoot.getEncoded().concat(nativeJarBasename) );

        if (DEBUG) {
            System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl: module: %s -> %s%n", nativeJarBasename, nativeJarURI);
        }

        try {
            ok = TempJarCache.addNativeLibs(classFromJavaJar, nativeJarURI, nativeLibraryPath);
        } catch(final Exception e) {
            if(DEBUG) {
                System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl: Caught %s%n", e.getMessage());
                e.printStackTrace();
            }
        }
    }
    if (!ok) {
        final ClassLoader cl = classFromJavaJar.getClassLoader();
        {
            // Attempt-2 a 'one big-fat jar file' layout, containing java classes
            // and all native platform libraries under 'natives/os.and.arch' per platform!
            final URL nativeLibraryURI = cl.getResource(nativeLibraryPath);
            if (null != nativeLibraryURI) {
                final Uri nativeJarURI = JarUtil.getJarFileUri( jarSubUriRoot.getEncoded().concat(jarBasename) );
                try {
                    if( TempJarCache.addNativeLibs(classFromJavaJar, nativeJarURI, nativeLibraryPath) ) {
                        ok = true;
                        if (DEBUG) {
                            System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl: fat: %s -> %s%n", jarBasename, nativeJarURI);
                        }
                    }
                } catch(final Exception e) {
                    if(DEBUG) {
                        System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl: Caught %s%n", e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        if (!ok) {
            // Attempt-3 to find via ClassLoader and Native-Jar-Tag,
            // assuming one slim native jar file per 'os.and.arch'
            // and native platform libraries under 'natives/os.and.arch'!
            final String moduleName;
            {
                final String packageName = classFromJavaJar.getPackage().getName();
                final int idx = packageName.lastIndexOf('.');
                if( 0 <= idx ) {
                    moduleName = packageName.substring(idx+1);
                } else {
                    moduleName = packageName;
                }
            }
            final String os_and_arch_dot = PlatformPropsImpl.os_and_arch.replace('-', '.');
            final String nativeJarTagClassName = nativeJarTagPackage + "." + moduleName + "." + os_and_arch_dot + ".TAG"; // TODO: sync with gluegen-cpptasks-base.xml
            try {
                if(DEBUG) {
                    System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl: ClassLoader/TAG: Locating module %s, os.and.arch %s: %s%n",
                            moduleName, os_and_arch_dot, nativeJarTagClassName);
                }
                final Uri nativeJarTagClassJarURI = JarUtil.getJarUri(nativeJarTagClassName, cl);
                if (DEBUG) {
                    System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl: ClassLoader/TAG: %s -> %s%n", nativeJarTagClassName, nativeJarTagClassJarURI);
                }
                ok = TempJarCache.addNativeLibs(classFromJavaJar, nativeJarTagClassJarURI, nativeLibraryPath);
            } catch (final Exception e ) {
                if(DEBUG) {
                    System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl: Caught %s%n", e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    if (DEBUG || PERF) {
        final long tNow = System.currentTimeMillis() - t0;
        final long tTotal, tCount;
        synchronized(perfSync) {
            tCount = perfCount+1;
            tTotal = perfTotal + tNow;
            perfTotal = tTotal;
            perfCount = tCount;
        }
        final double tAvrg = tTotal / (double)tCount;
        System.err.printf("JNILibLoaderBase: addNativeJarLibsImpl.X: %s / %s -> ok: %b; duration: now %d ms, total %d ms (count %d, avrg %.3f ms)%n",
                          jarBasename, nativeJarBasename, ok, tNow, tTotal, tCount, tAvrg);
    }
    return ok;
  }

  /**
   * Loads and adds a JAR file's native library to the TempJarCache,
   * calling {@link JNILibLoaderBase#addNativeJarLibs(Class[], String)}
   * with default JOGL deployment configuration:
   * <pre>
        return JNILibLoaderBase.addNativeJarLibs(classesFromJavaJars, "-all");
   * </pre>
   * If <code>Class1.class</code> is contained in a JAR file which name includes <code>singleJarMarker</code> <i>-all</i>,
   * implementation will attempt to resolve the native JAR file as follows:
   * <ul>
   *   <li><i>ClassJar-all</i>.jar to <i>ClassJar-all</i>-natives-<i>os.and.arch</i>.jar</li>
   * </ul>
   * Otherwise the native JAR files will be resolved for each class's JAR file:
   * <ul>
   *   <li><i>ClassJar1</i>.jar to <i>ClassJar1</i>-natives-<i>os.and.arch</i>.jar</li>
   *   <li><i>ClassJar2</i>.jar to <i>ClassJar2</i>-natives-<i>os.and.arch</i>.jar</li>
   *   <li>..</li>
   * </ul>
   */

  public static final boolean addNativeJarLibsJoglCfg(final Class<?>[] classesFromJavaJars) {
      return addNativeJarLibs(classesFromJavaJars, "-all");
  }

  /**
   * Loads and adds a JAR file's native library to the TempJarCache.<br>
   * The native library JAR file's URI is derived as follows:
   * <ul>
   *   <li> [1] <code>GLProfile.class</code> -> </li>
   *   <li> [2] <code>http://lala/gluegen-rt.jar</code> -> </li>
   *   <li> [3] <code>http://lala/gluegen-rt</code> -> </li>
   *   <li> [4] <code>http://lala/gluegen-rt-natives-'os.and.arch'.jar</code> </li>
   * </ul>
   * Where:
   * <ul>
   *   <li> [1] is one of <code>classesFromJavaJars</code></li>
   *   <li> [2] is it's complete URI</li>
   *   <li> [3] is it's <i>base URI</i></li>
   *   <li> [4] is the derived native JAR filename</li>
   * </ul>
   * <p>
   * Generic description:
   * <pre>
       final Class<?>[] classesFromJavaJars = new Class<?>[] { Class1.class, Class2.class };
       JNILibLoaderBase.addNativeJarLibs(classesFromJavaJars, "-all");
   * </pre>
   * If <code>Class1.class</code> is contained in a JAR file which name includes <code>singleJarMarker</code>, here <i>-all</i>,
   * implementation will attempt to resolve the native JAR file as follows:
   * <ul>
   *   <li><i>ClassJar-all</i>.jar to <i>ClassJar-all</i>-natives-<i>os.and.arch</i>.jar</li>
   * </ul>
   * Otherwise the native JAR files will be resolved for each class's JAR file:
   * <ul>
   *   <li><i>Class1Jar</i>.jar to <i>Class1Jar</i>-natives-<i>os.and.arch</i>.jar</li>
   *   <li><i>Class2Jar</i>.jar to <i>Class2Jar</i>-natives-<i>os.and.arch</i>.jar</li>
   * </ul>
   * </p>
   * <p>
   * Examples:
   * </p>
   * <p>
   * JOCL:
   * <pre>
        // only: jocl.jar -> jocl-natives-<i>os.and.arch</i>.jar
        addNativeJarLibs(new Class<?>[] { JOCLJNILibLoader.class }, null, null );
   * </pre>
   * </p>
   * <p>
   * JOGL:
   * <pre>
       final ClassLoader cl = GLProfile.class.getClassLoader();
       // jogl-all.jar         -> jogl-all-natives-<i>os.and.arch</i>.jar
       // jogl-all-noawt.jar   -> jogl-all-noawt-natives-<i>os.and.arch</i>.jar
       // jogl-all-mobile.jar  -> jogl-all-mobile-natives-<i>os.and.arch</i>.jar
       // jogl-all-android.jar -> jogl-all-android-natives-<i>os.and.arch</i>.jar
       // nativewindow.jar     -> nativewindow-natives-<i>os.and.arch</i>.jar
       // jogl.jar             -> jogl-natives-<i>os.and.arch</i>.jar
       // newt.jar             -> newt-natives-<i>os.and.arch</i>.jar (if available)
       final String newtFactoryClassName = "com.jogamp.newt.NewtFactory";
       final Class<?>[] classesFromJavaJars = new Class<?>[] { NWJNILibLoader.class, GLProfile.class, null };
       if( ReflectionUtil.isClassAvailable(newtFactoryClassName, cl) ) {
           classesFromJavaJars[2] = ReflectionUtil.getClass(newtFactoryClassName, false, cl);
       }
       JNILibLoaderBase.addNativeJarLibs(classesFromJavaJars, "-all");
   * </pre>
   * </p>
   *
   * @param classesFromJavaJars For each given Class, load the native library JAR.
   * @param singleJarMarker Optional string marker like "-all" to identify the single 'all-in-one' JAR file
   *                        after which processing of the class array shall stop.
   *
   * @return true if either the 'all-in-one' native JAR or all native JARs loaded successful or were loaded already,
   *         false in case of an error
   */
  public static boolean addNativeJarLibs(final Class<?>[] classesFromJavaJars, final String singleJarMarker) {
    if(DEBUG) {
        final StringBuilder msg = new StringBuilder();
        msg.append("JNILibLoaderBase: addNativeJarLibs(").append(PlatformPropsImpl.NEWLINE);
        msg.append("  classesFromJavaJars   = ").append(Arrays.asList(classesFromJavaJars)).append(PlatformPropsImpl.NEWLINE);
        msg.append("  singleJarMarker       = ").append(singleJarMarker).append(PlatformPropsImpl.NEWLINE);
        msg.append(")");
        System.err.println(msg.toString());
    }

    boolean ok = false;
    if (TempJarCache.isInitialized()) {
        ok = addNativeJarLibsWithTempJarCache(classesFromJavaJars, singleJarMarker);
    } else if(DEBUG) {
        System.err.println("JNILibLoaderBase: addNativeJarLibs0: disabled due to uninitialized TempJarCache");
    }
    return ok;
  }

  private static boolean addNativeJarLibsWithTempJarCache(final Class<?>[] classesFromJavaJars, final String singleJarMarker) {
      boolean ok;
      int count = 0;
      try {
          boolean done = false;
          ok = true;

          for (int i = 0; i < classesFromJavaJars.length; ++i) {
              final Class<?> c = classesFromJavaJars[i];
              if (c == null) {
                  continue;
              }

              final ClassLoader cl = c.getClassLoader();
              final Uri classJarURI = JarUtil.getJarUri(c.getName(), cl);
              final Uri.Encoded jarName = JarUtil.getJarBasename(classJarURI);

              if (jarName == null) {
                  continue;
              }

              final Uri.Encoded jarBasename = jarName.substring(0, jarName.indexOf(".jar"));

              if(DEBUG) {
                  System.err.printf("JNILibLoaderBase: jarBasename: %s%n", jarBasename);
              }

              /**
               * If a jar marker was specified, and the basename contains the
               * marker, we're done.
               */

              if (singleJarMarker != null) {
                  if (jarBasename.indexOf(singleJarMarker) >= 0) {
                      done = true;
                  }
              }

              final Uri.Encoded nativeJarBasename =
                      Uri.Encoded.cast( String.format("%s-natives-%s.jar", jarBasename.get(), PlatformPropsImpl.os_and_arch) );

              ok = JNILibLoaderBase.addNativeJarLibsImpl(c, classJarURI, jarName, nativeJarBasename);
              if (ok) {
                  count++;
              }
              if (DEBUG && done) {
                  System.err.printf("JNILibLoaderBase: addNativeJarLibs0: done: %s%n", jarBasename);
              }
          }
      } catch (final Exception x) {
          System.err.printf("JNILibLoaderBase: Caught %s: %s%n", x.getClass().getSimpleName(), x.getMessage());
          if(DEBUG) {
              x.printStackTrace();
          }
          ok = false;
      }
      if(DEBUG) {
          System.err.printf("JNILibLoaderBase: addNativeJarLibsWhenInitialized: count %d, ok %b%n", count, ok);
      }
      return ok;
  }

  /**
   * Loads the library specified by libname, using the {@link LoaderAction} set by {@link #setLoadingAction(LoaderAction)}.<br>
   * The implementation should ignore, if the library has been loaded already.<br>
   * @param libname the library to load
   * @param ignoreError if true, errors during loading the library should be ignored
   * @param cl optional ClassLoader, used to locate the library
   * @return true if library loaded successful
   */
  protected static synchronized boolean loadLibrary(final String libname, final boolean ignoreError, final ClassLoader cl) {
      if (loaderAction != null) {
          return loaderAction.loadLibrary(libname, ignoreError, cl);
      }
      return false;
  }

  /**
   * Loads the library specified by libname, using the {@link LoaderAction} set by {@link #setLoadingAction(LoaderAction)}.<br>
   * Optionally preloads the libraries specified by preload.<br>
   * The implementation should ignore, if any library has been loaded already.<br>
   * @param libname the library to load
   * @param preload the libraries to load before loading the main library if not null
   * @param preloadIgnoreError if true, errors during loading the preload-libraries should be ignored
   * @param cl optional ClassLoader, used to locate the library
   */
  protected static synchronized void loadLibrary(final String libname, final String[] preload, final boolean preloadIgnoreError, final ClassLoader cl) {
      if (loaderAction != null) {
          loaderAction.loadLibrary(libname, preload, preloadIgnoreError, cl);
      }
  }

  // private static final Class<?> customLauncherClass; // FIXME: remove
  private static final Method customLoadLibraryMethod;

  static {
    final String sunAppletLauncherProperty = "sun.jnlp.applet.launcher";
    final String sunAppletLauncherClassName = "org.jdesktop.applet.util.JNLPAppletLauncher";

    final Method loadLibraryMethod = AccessController.doPrivileged(new PrivilegedAction<Method>() {
        @Override
        public Method run() {
            // FIXME: remove
            final boolean usingJNLPAppletLauncher = PropertyAccess.getBooleanProperty(sunAppletLauncherProperty, true);

            Class<?> launcherClass = null;
            Method loadLibraryMethod = null;

            if (usingJNLPAppletLauncher) {
                try {
                  launcherClass = Class.forName(sunAppletLauncherClassName);
                } catch (final ClassNotFoundException cnfe) {
                  // oops .. look like JNLPAppletLauncher doesn't exist, despite property
                  // this may happen if a previous applet was using JNLPAppletLauncher in the same JVM
                  System.err.println("JNILibLoaderBase: <"+sunAppletLauncherClassName+"> not found, despite enabled property <"+sunAppletLauncherProperty+">, JNLPAppletLauncher was probably used before");
                  System.setProperty(sunAppletLauncherProperty, Boolean.FALSE.toString());
                } catch (final LinkageError le) {
                    throw le;
                }
                if(null != launcherClass) {
                   try {
                      loadLibraryMethod = launcherClass.getDeclaredMethod("loadLibrary", new Class[] { String.class });
                   } catch (final NoSuchMethodException ex) {
                        if(DEBUG) {
                            ex.printStackTrace();
                        }
                        launcherClass = null;
                   }
                }
            }
            if(null==launcherClass) {
                final String launcherClassName = PropertyAccess.getProperty("jnlp.launcher.class", false);
                if(null!=launcherClassName) {
                    try {
                        launcherClass = Class.forName(launcherClassName);
                        loadLibraryMethod = launcherClass.getDeclaredMethod("loadLibrary", new Class[] { String.class });
                    } catch (final ClassNotFoundException ex) {
                        if(DEBUG) {
                            ex.printStackTrace();
                        }
                    } catch (final NoSuchMethodException ex) {
                        if(DEBUG) {
                            ex.printStackTrace();
                        }
                        launcherClass = null;
                    }
                }
            }
            return loadLibraryMethod;
        } } );
    customLoadLibraryMethod = loadLibraryMethod;
  }

  private static void loadLibraryInternal(final String libraryName, final ClassLoader cl) {
      // Note: special-casing JAWT which is built in to the JDK
      int mode = 0; // 1 - custom, 2 - System.load( TempJarCache ), 3 - System.loadLibrary( name ), 4 - System.load( enumLibNames )
      if (null!=customLoadLibraryMethod && !libraryName.equals("jawt")) {
          // FIXME: remove
          if(DEBUG) {
              System.err.println("JNILibLoaderBase: customLoad("+libraryName+") - mode 1");
          }
          try {
              customLoadLibraryMethod.invoke(null, new Object[] { libraryName });
              mode = 1;
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
              // Throw UnsatisfiedLinkError for best compatibility with System.loadLibrary()
              throw (UnsatisfiedLinkError) new UnsatisfiedLinkError("can not load library "+libraryName).initCause(e);
          }
      } else {
          // System.err.println("sun.boot.library.path=" + Debug.getProperty("sun.boot.library.path", false));
          final String libraryPath = NativeLibrary.findLibrary(libraryName, cl); // implicit TempJarCache usage if used/initialized
          if(DEBUG) {
              System.err.println("JNILibLoaderBase: loadLibraryInternal("+libraryName+"), TempJarCache: "+libraryPath);
          }
          if(null != libraryPath) {
              if(DEBUG) {
                  System.err.println("JNILibLoaderBase: System.load("+libraryPath+") - mode 2");
              }
              System.load(libraryPath);
              mode = 2;
          } else {
              if(DEBUG) {
                  System.err.println("JNILibLoaderBase: System.loadLibrary("+libraryName+") - mode 3");
              }
              try {
                  System.loadLibrary(libraryName);
                  mode = 3;
              } catch (final UnsatisfiedLinkError ex1) {
                  if(DEBUG) {
                      System.err.println("ERROR (retry w/ enumLibPath) - "+ex1.getMessage());
                  }
                  final List<String> possiblePaths = NativeLibrary.enumerateLibraryPaths(libraryName, libraryName, libraryName, cl);
                  // Iterate down these and see which one if any we can actually find.
                  for (final Iterator<String> iter = possiblePaths.iterator(); 0 == mode && iter.hasNext(); ) {
                      final String path = iter.next();
                      if (DEBUG) {
                          System.err.println("JNILibLoaderBase: System.load("+path+") - mode 4");
                      }
                      try {
                          System.load(path);
                          mode = 4;
                      } catch (final UnsatisfiedLinkError ex2) {
                          if(DEBUG) {
                              System.err.println("n/a - "+ex2.getMessage());
                          }
                          if(!iter.hasNext()) {
                              throw ex2;
                          }
                      }
                  }
              }
          }
      }
      if(DEBUG) {
          System.err.println("JNILibLoaderBase: loadLibraryInternal("+libraryName+"): OK - mode "+mode);
      }
  }
}
