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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.cache.TempJarCache;

import jogamp.common.Debug;
import jogamp.common.os.PlatformPropsImpl;

public class JNILibLoaderBase {
  public static final boolean DEBUG = Debug.debug("JNILibLoader");

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
    public boolean loadLibrary(String libname, boolean ignoreError, ClassLoader cl) {
      boolean res = true;
      if(!isLoaded(libname)) {
          try {
            loadLibraryInternal(libname, cl);
            addLoaded(libname);
            if(DEBUG) {
                System.err.println("JNILibLoaderBase: loaded "+libname);
            }
          } catch (UnsatisfiedLinkError e) {
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
    public void loadLibrary(String libname, String[] preload, boolean preloadIgnoreError, ClassLoader cl) {
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

  public static boolean isLoaded(String libName) {
    return loaded.contains(libName);
  }

  public static void addLoaded(String libName) {
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

  public static synchronized void setLoadingAction(LoaderAction action) {
    loaderAction = action;
  }

  /**
   *
   * @param classFromJavaJar
   * @param classJarURI
   * @param jarBasename jar basename w/ suffix
   * @param nativeJarBasename native jar basename w/ suffix
   * @param msg
   * @return
   * @throws IOException
   * @throws SecurityException
   * @throws URISyntaxException
   */
  /* pp */ static final boolean addNativeJarLibsImpl(Class<?> classFromJavaJar, URI classJarURI, String jarBasename, String nativeJarBasename, StringBuilder msg)
    throws IOException, SecurityException, URISyntaxException
  {
    msg.setLength(0); // reset
    msg.append("addNativeJarLibsImpl(classFromJavaJar ").append(classFromJavaJar).append(", classJarURI ").append(classJarURI)
       .append(", nativeJarBaseName ").append(nativeJarBasename).append("): ");
    boolean ok = false;
    if(TempJarCache.isInitialized()) {
        final URI jarSubURI = JarUtil.getJarSubURI( classJarURI );
        if(null == jarSubURI) {
            throw new IllegalArgumentException("JarSubURI is null of: "+classJarURI);
        }
        final String jarUriRoot_s = IOUtil.getURIDirname( jarSubURI.toString() );
        msg.append("[ ").append(jarSubURI.toString()).append(" -> ").append(jarUriRoot_s).append(" ] + ");

        final String nativeLibraryPath = "natives/"+PlatformPropsImpl.os_and_arch+"/";
        final ClassLoader cl = classFromJavaJar.getClassLoader();
        final URL nativeLibraryURI = cl.getResource(nativeLibraryPath);
        if( null != nativeLibraryURI ) {
            // We probably have one big-fat jar file, containing java classes
            // and all native platform libraries under 'natives/os.and.arch'!
            final URI nativeJarURI = JarUtil.getJarFileURI(jarUriRoot_s+jarBasename);
            if( TempJarCache.addNativeLibs(classFromJavaJar, nativeJarURI, nativeLibraryPath) ) {
                ok = true;
                msg.append(jarBasename).append(" -> fat: ").append(nativeJarURI);
            }
        }
        if( !ok ) {
            // We assume one slim native jar file per 'os.and.arch'!
            final URI nativeJarURI = JarUtil.getJarFileURI(jarUriRoot_s+nativeJarBasename);
            msg.append(nativeJarBasename).append(" -> slim: ").append(nativeJarURI);
            ok = TempJarCache.addNativeLibs(classFromJavaJar, nativeJarURI, null /* nativeLibraryPath */);
        }
    } else {
        msg.append("TempJarCache n/a");
    }
    if(DEBUG) {
        System.err.println(msg.toString()+" - OK "+ok);
    }
    return ok;
  }

  /**
   * Loads and adds a JAR file's native library to the TempJarCache.<br>
   * The native library JAR file's URI is derived as follows:
   * <ul>
   *   <li> [1] <code>GLProfile.class</code> -> </li>
   *   <li> [2] <code>http://lala/</code> -> </li>
   *   <li> [4] <code>http://lala/'nativeJarBaseName'-'os.and.arch'.jar</code> </li>
   * </ul>
   * Where:
   * <ul>
   *   <li> [1] is the <code>classFromJavaJar</code></li>
   *   <li> [2] is it's <i>URI path</i></li>
   *   <li> [4] is the derived native JAR filename</li>
   * </ul>
   *
   * @param classFromJavaJar GLProfile
   * @param nativeJarBasename jogl-all
   * @return true if the native JAR file loaded successful or were loaded already, false in case of an error
   */
  public static final boolean addNativeJarLibs(Class<?> classFromJavaJar, String nativeJarBasename) {
    if(TempJarCache.isInitialized()) {
        final StringBuilder msg = new StringBuilder();
        try {
            final URI classJarURI = JarUtil.getJarURI(classFromJavaJar.getName(), classFromJavaJar.getClassLoader());
            final String jarName = JarUtil.getJarBasename(classJarURI);
            return addNativeJarLibsImpl(classFromJavaJar, classJarURI, jarName, nativeJarBasename+"-natives-"+PlatformPropsImpl.os_and_arch+".jar", msg);
        } catch (Exception e0) {
            // IllegalArgumentException, IOException
            System.err.println("Catched "+e0.getClass().getSimpleName()+": "+e0.getMessage()+", while "+msg.toString());
            if(DEBUG) {
                e0.printStackTrace();
            }
        }
    } else if(DEBUG) {
        System.err.println("JNILibLoaderBase: addNativeJarLibs1: disabled due to uninitialized TempJarCache");
    }
    return false;
  }

  /**
   * Loads and adds a JAR file's native library to the TempJarCache,
   * calling {@link JNILibLoaderBase#addNativeJarLibs(Class[], String, String[])}
   * with default JOGL deployment configuration:
   * <pre>
        return JNILibLoaderBase.addNativeJarLibs(classesFromJavaJars, "-all", new String[] { "-noawt", "-mobile", "-core", "-android" } );
   * </pre>
   * If <code>Class1.class</code> is contained in a JAR file which name includes <code>singleJarMarker</code> <i>-all</i>,
   * implementation will attempt to resolve the native JAR file as follows:
   * <ul>
   *   <li><i>ClassJar-all</i>[-noawt,-mobile,-android]?.jar to <i>ClassJar-all</i>-natives-<i>os.and.arch</i>.jar</li>
   * </ul>
   * Otherwise the native JAR files will be resolved for each class's JAR file:
   * <ul>
   *   <li><i>ClassJar1</i>[-noawt,-mobile,-core,-android]?.jar to <i>ClassJar1</i>-natives-<i>os.and.arch</i>.jar</li>
   *   <li><i>ClassJar2</i>[-noawt,-mobile,-core,-android]?.jar to <i>ClassJar2</i>-natives-<i>os.and.arch</i>.jar</li>
   *   <li>..</li>
   * </ul>
   */
  public static final boolean addNativeJarLibsJoglCfg(final Class<?>[] classesFromJavaJars) {
      return addNativeJarLibs(classesFromJavaJars, "-all", joglDeployCfg);
  }
  private static final String[] joglDeployCfg = new String[] { "-noawt", "-mobile", "-core", "-android" };

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
       final ClassLoader cl = GLProfile.class.getClassLoader();
       final String newtFactoryClassName = "com.jogamp.newt.NewtFactory";
       final Class<?>[] classesFromJavaJars = new Class<?>[] { Class1.class, Class2.class };
       JNILibLoaderBase.addNativeJarLibs(classesFromJavaJars, "-all", new String[] { "-suff1", "-suff2" } );
   * </pre>
   * If <code>Class1.class</code> is contained in a JAR file which name includes <code>singleJarMarker</code>, here <i>-all</i>,
   * implementation will attempt to resolve the native JAR file as follows:
   * <ul>
   *   <li><i>ClassJar-all</i>[-suff1,-suff2]?.jar to <i>ClassJar-all</i>-natives-<i>os.and.arch</i>.jar</li>
   * </ul>
   * Otherwise the native JAR files will be resolved for each class's JAR file:
   * <ul>
   *   <li><i>Class1Jar</i>[-suff1,-suff2]?.jar to <i>Class1Jar</i>-natives-<i>os.and.arch</i>.jar</li>
   *   <li><i>Class2Jar</i>[-suff1,-suff2]?.jar to <i>Class2Jar</i>-natives-<i>os.and.arch</i>.jar</li>
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
   *
   * Newt Only:
   * <pre>
       // either: [jogl-all.jar, jogl-all-noawt.jar, jogl-all-mobile.jar, jogl-all-android.jar] -> jogl-all-natives-<i>os.and.arch</i>.jar
       // or:     nativewindow-core.jar                                                         -> nativewindow-natives-<i>os.and.arch</i>.jar,
       //         newt-core.jar                                                                 -> newt-natives-<i>os.and.arch</i>.jar
       JNILibLoaderBase.addNativeJarLibs(new Class<?>[] { NWJNILibLoader.class, NEWTJNILibLoader.class }, "-all", new String[] { "-noawt", "-mobile", "-core", "-android" } );
   * </pre>
   * </p>
   * <p>
   * JOGL:
   * <pre>
       final ClassLoader cl = GLProfile.class.getClassLoader();
       // either: [jogl-all.jar, jogl-all-noawt.jar, jogl-all-mobile.jar, jogl-all-android.jar] -> jogl-all-natives-<i>os.and.arch</i>.jar
       // or:     nativewindow-core.jar                                                         -> nativewindow-natives-<i>os.and.arch</i>.jar,
       //         jogl-core.jar                                                                 -> jogl-natives-<i>os.and.arch</i>.jar,
       //        (newt-core.jar                                                                 -> newt-natives-<i>os.and.arch</i>.jar)? (if available)
       final String newtFactoryClassName = "com.jogamp.newt.NewtFactory";
       final Class<?>[] classesFromJavaJars = new Class<?>[] { NWJNILibLoader.class, GLProfile.class, null };
       if( ReflectionUtil.isClassAvailable(newtFactoryClassName, cl) ) {
           classesFromJavaJars[2] = ReflectionUtil.getClass(newtFactoryClassName, false, cl);
       }
       JNILibLoaderBase.addNativeJarLibs(classesFromJavaJars, "-all", new String[] { "-noawt", "-mobile", "-core", "-android" } );
   * </pre>
   * </p>
   *
   * @param classesFromJavaJars For each given Class, load the native library JAR.
   * @param singleJarMarker Optional string marker like "-all" to identify the single 'all-in-one' JAR file
   *                        after which processing of the class array shall stop.
   * @param stripBasenameSuffixes Optional substrings to be stripped of the <i>base URI</i>
   *
   * @return true if either the 'all-in-one' native JAR or all native JARs loaded successful or were loaded already,
   *         false in case of an error
   */
  public static boolean addNativeJarLibs(Class<?>[] classesFromJavaJars, String singleJarMarker, String[] stripBasenameSuffixes) {
    if(DEBUG) {
        System.err.println("JNILibLoaderBase: addNativeJarLibs0(classesFromJavaJars "+Arrays.asList(classesFromJavaJars)+", singleJarMarker "+singleJarMarker+", stripBasenameSuffixes "+(null!=stripBasenameSuffixes?Arrays.asList(stripBasenameSuffixes):"none"));
    }
    boolean ok = false;
    if(TempJarCache.isInitialized()) {
        final StringBuilder msg = new StringBuilder();
        int count = 0;
        try {
            boolean done = false;
            ok = true;
            for(int i=0; !done && ok && i<classesFromJavaJars.length && null!=classesFromJavaJars[i]; i++) {
                final ClassLoader cl = classesFromJavaJars[i].getClassLoader();
                final URI classJarURI = JarUtil.getJarURI(classesFromJavaJars[i].getName(), cl);
                final String jarName = JarUtil.getJarBasename(classJarURI);
                ok = null != jarName;
                if(ok) {
                    final String jarBasename = jarName.substring(0, jarName.indexOf(".jar")); // ".jar" already validated w/ JarUtil.getJarBasename(..)
                    final String nativeJarBasename = stripName(jarBasename, stripBasenameSuffixes)+"-natives-"+PlatformPropsImpl.os_and_arch+".jar";
                    done = null != singleJarMarker && jarBasename.indexOf(singleJarMarker) >= 0; // done if single-jar ('all' variant)
                    ok = JNILibLoaderBase.addNativeJarLibsImpl(classesFromJavaJars[i], classJarURI, jarName, nativeJarBasename, msg);
                    if(ok) { count++; }
                    if(DEBUG && done) {
                        System.err.println("JNILibLoaderBase: addNativeJarLibs0: end after all-in-one JAR: "+jarBasename);
                    }
                }
            }
        } catch (Exception e0) {
            // IllegalArgumentException, IOException
            System.err.println("Catched "+e0.getClass().getSimpleName()+": "+e0.getMessage()+", while "+msg.toString());
            if(DEBUG) {
                e0.printStackTrace();
            }
            ok = false;
        }
        if(DEBUG) {
            System.err.println("JNILibLoaderBase: addNativeJarLibs0(..) done, count "+count+", ok "+ok);
        }
    } else if(DEBUG) {
        System.err.println("JNILibLoaderBase: addNativeJarLibs0: disabled due to uninitialized TempJarCache");
    }
    return ok;
  }

  private static final String stripName(String name, String[] suffixes) {
      if(null != suffixes) {
          for(int i=0; i<suffixes.length && null != suffixes[i]; i++) {
              int idx = name.indexOf(suffixes[i]);
              if(0 < idx) {
                  return name.substring(0, idx);
              }
          }
      }
      return name;
  }

  /**
   * Loads the library specified by libname, using the {@link LoaderAction} set by {@link #setLoadingAction(LoaderAction)}.<br>
   * The implementation should ignore, if the library has been loaded already.<br>
   * @param libname the library to load
   * @param ignoreError if true, errors during loading the library should be ignored
   * @param cl optional ClassLoader, used to locate the library
   * @return true if library loaded successful
   */
  protected static synchronized boolean loadLibrary(String libname, boolean ignoreError, ClassLoader cl) {
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
  protected static synchronized void loadLibrary(String libname, String[] preload, boolean preloadIgnoreError, ClassLoader cl) {
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
            final boolean usingJNLPAppletLauncher = Debug.getBooleanProperty(sunAppletLauncherProperty, true);

            Class<?> launcherClass = null;
            Method loadLibraryMethod = null;

            if (usingJNLPAppletLauncher) {
                try {
                  launcherClass = Class.forName(sunAppletLauncherClassName);
                } catch (ClassNotFoundException cnfe) {
                  // oops .. look like JNLPAppletLauncher doesn't exist, despite property
                  // this may happen if a previous applet was using JNLPAppletLauncher in the same JVM
                  System.err.println("JNILibLoaderBase: <"+sunAppletLauncherClassName+"> not found, despite enabled property <"+sunAppletLauncherProperty+">, JNLPAppletLauncher was probably used before");
                  System.setProperty(sunAppletLauncherProperty, Boolean.FALSE.toString());
                } catch (LinkageError le) {
                    throw le;
                }
                if(null != launcherClass) {
                   try {
                      loadLibraryMethod = launcherClass.getDeclaredMethod("loadLibrary", new Class[] { String.class });
                   } catch (NoSuchMethodException ex) {
                        if(DEBUG) {
                            ex.printStackTrace();
                        }
                        launcherClass = null;
                   }
                }
            }
            if(null==launcherClass) {
                String launcherClassName = PropertyAccess.getProperty("jnlp.launcher.class", false);
                if(null!=launcherClassName) {
                    try {
                        launcherClass = Class.forName(launcherClassName);
                        loadLibraryMethod = launcherClass.getDeclaredMethod("loadLibrary", new Class[] { String.class });
                    } catch (ClassNotFoundException ex) {
                        if(DEBUG) {
                            ex.printStackTrace();
                        }
                    } catch (NoSuchMethodException ex) {
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

  private static void loadLibraryInternal(String libraryName, ClassLoader cl) {
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
              } catch (UnsatisfiedLinkError ex1) {
                  if(DEBUG) {
                      System.err.println("ERROR (retry w/ enumLibPath) - "+ex1.getMessage());
                  }
                  List<String> possiblePaths = NativeLibrary.enumerateLibraryPaths(libraryName, libraryName, libraryName, true, cl);
                  // Iterate down these and see which one if any we can actually find.
                  for (Iterator<String> iter = possiblePaths.iterator(); 0 == mode && iter.hasNext(); ) {
                      String path = iter.next();
                      if (DEBUG) {
                          System.err.println("JNILibLoaderBase: System.load("+path+") - mode 4");
                      }
                      try {
                          System.load(path);
                          mode = 4;
                      } catch (UnsatisfiedLinkError ex2) {
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
