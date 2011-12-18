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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.util.HashSet;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.JarUtil;
import com.jogamp.common.util.cache.TempJarCache;

import jogamp.common.Debug;

public class JNILibLoaderBase {
  public static final boolean DEBUG = Debug.debug("JNILibLoader");
  private static final AccessControlContext localACC = AccessController.getContext();

  public interface LoaderAction {
    /**
     * Loads the library specified by libname.<br>
     * The implementation should ignore, if the library has been loaded already.<br>
     * @param libname the library to load
     * @param ignoreError if true, errors during loading the library should be ignored 
     * @return true if library loaded successful
     */
    boolean loadLibrary(String libname, boolean ignoreError);

    /**
     * Loads the library specified by libname.<br>
     * Optionally preloads the libraries specified by preload.<br>
     * The implementation should ignore, if any library has been loaded already.<br>
     * @param libname the library to load
     * @param preload the libraries to load before loading the main library if not null
     * @param preloadIgnoreError if true, errors during loading the preload-libraries should be ignored 
     */
    void loadLibrary(String libname, String[] preload, boolean preloadIgnoreError);
  }
  
  private static class DefaultAction implements LoaderAction {
    public boolean loadLibrary(String libname, boolean ignoreError) {
      boolean res = true;
      if(!isLoaded(libname)) {
          try {
            loadLibraryInternal(libname);
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

    public void loadLibrary(String libname, String[] preload, boolean preloadIgnoreError) {
      if(!isLoaded(libname)) {
          if (null!=preload) {
            for (int i=0; i<preload.length; i++) {
              loadLibrary(preload[i], preloadIgnoreError);
            }
          }
          loadLibrary(libname, false);
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
   * @param classFromJavaJar GLProfile
   * @param nativeJarBaseName jogl-all
   * @return true if the native JAR file loaded successful or were loaded already, false in case of an error
   */
  public static final boolean addNativeJarLibs(Class<?> classFromJavaJar, String nativeJarBaseName) {
    if(TempJarCache.isInitialized()) {
        final String nativeJarName = nativeJarBaseName+"-natives-"+Platform.getOSAndArch()+".jar";
        final ClassLoader cl = classFromJavaJar.getClassLoader();
        try {
            URL jarUrlRoot = JarUtil.getURLDirname( JarUtil.getJarSubURL( classFromJavaJar.getName(), cl ) );
            if(DEBUG) {
                System.err.println("JNILibLoaderBase: addNativeJarLibs: "+nativeJarBaseName+": url-root "+jarUrlRoot);
            }
            URL nativeJarURL = JarUtil.getJarFileURL(jarUrlRoot, nativeJarName);
            if(DEBUG) {
                System.err.println("JNILibLoaderBase: addNativeJarLibs: "+nativeJarBaseName+": nativeJarURL "+nativeJarURL);
            }
            TempJarCache.addNativeLibs(classFromJavaJar, nativeJarURL, cl);
            return true;
        } catch (Exception e0) {
            // IllegalArgumentException, IOException
            System.err.println("Catched: "+e0.getMessage());
            if(DEBUG) {
                e0.printStackTrace();
            }
        }
    }
    return false;
  }
   
  /**
   * @param classFromJavaJar A class file to determine the base URL of the native JAR files, eg.: GLProfile.class
   * @param allNativeJarBaseName Attempt to use the 'all' native JAR variant first, if exists. Eg. "jogl-all"
   * @param atomicNativeJarBaseNames Fallback to use all the atomic native JAR files, eg. [ "nativewindow", "jogl", "newt" ]
   * @return true if either the 'all' native JAR or all of the atomic native JARs loaded successful or were loaded already,
   *         false in case of an error
   */
  public static boolean addNativeJarLibs(Class<?> classFromJavaJar, String allNativeJarBaseName, String[] atomicNativeJarBaseNames) {
    boolean res = false;
    if(TempJarCache.isInitialized()) {
        final ClassLoader cl = classFromJavaJar.getClassLoader();
        try {
            final String jarName = JarUtil.getJarBasename(classFromJavaJar.getName(), cl);
            if(jarName!=null) {
                if(!res && null != allNativeJarBaseName) {
                    // all-in-one variant 1st
                    res = JNILibLoaderBase.addNativeJarLibs(classFromJavaJar, allNativeJarBaseName);
                }
                if(!res && null != atomicNativeJarBaseNames) {
                    // atomic variant
                    res = true;
                    for(int i=0; res && i<atomicNativeJarBaseNames.length; i++) {
                        final String atomicNativeJarBaseName = atomicNativeJarBaseNames[i];
                        if(null != atomicNativeJarBaseName && atomicNativeJarBaseName.length()>0) {
                            res = JNILibLoaderBase.addNativeJarLibs(classFromJavaJar, atomicNativeJarBaseName);
                        }
                    }
                }
            }
        } catch (Exception e0) {
            // IllegalArgumentException, IOException
            System.err.println("Catched: "+e0.getMessage());
            if(DEBUG) {
                e0.printStackTrace();
            }
        }
    }
    return res;
  }
  
  protected static synchronized boolean loadLibrary(String libname, boolean ignoreError) {
    if (loaderAction != null) {
        return loaderAction.loadLibrary(libname, ignoreError);    
    }
    return false;
  }
  
  protected static synchronized void loadLibrary(String libname, String[] preload, boolean preloadIgnoreError) {
    if (loaderAction != null) {
        loaderAction.loadLibrary(libname, preload, preloadIgnoreError);    
    }
  }

  // private static final Class<?> customLauncherClass;
  private static final Method customLoadLibraryMethod;

  static {
    final String sunAppletLauncherProperty = "sun.jnlp.applet.launcher";
    final String sunAppletLauncherClassName = "org.jdesktop.applet.util.JNLPAppletLauncher";
    final boolean usingJNLPAppletLauncher = Boolean.valueOf(System.getProperty(sunAppletLauncherProperty)).booleanValue();

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
        String launcherClassName = Debug.getProperty("jnlp.launcher.class", false, localACC);
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
    // customLauncherClass = launcherClass;
    customLoadLibraryMethod = loadLibraryMethod;
  }

  private static void loadLibraryInternal(String libraryName) {
    // Note: special-casing JAWT which is built in to the JDK
    if (null!=customLoadLibraryMethod && !libraryName.equals("jawt")) {
        try {
          customLoadLibraryMethod.invoke(null, new Object[] { libraryName });
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
      if(TempJarCache.isInitialized()) {
          final String fullLibraryName = TempJarCache.findLibrary(libraryName);
          if(null != fullLibraryName) {
            if(DEBUG) {
              System.err.println("JNILibLoaderBase: loadLibraryInternal("+libraryName+") -> System.load("+fullLibraryName+") (TempJarCache)");
            }
            System.load(fullLibraryName);
            return; // done
          } else if(DEBUG) {
            System.err.println("JNILibLoaderBase: loadLibraryInternal("+libraryName+") -> TempJarCache not mapped");
          }
      }
      // System.err.println("sun.boot.library.path=" + Debug.getProperty("sun.boot.library.path", false));
      if(DEBUG) {
          System.err.println("JNILibLoaderBase: loadLibraryInternal("+libraryName+") -> System.loadLibrary("+libraryName+")");
      }
      System.loadLibrary(libraryName);
    }
  }
}
