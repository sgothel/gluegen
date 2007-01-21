/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.gluegen.runtime;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

/** Provides low-level, relatively platform-independent access to
    shared ("native") libraries. The core library routines
    <code>System.load()</code> and <code>System.loadLibrary()</code>
    in general provide suitable functionality for applications using
    native code, but are not flexible enough to support certain kinds
    of glue code generation and deployment strategies. This class
    supports direct linking of native libraries to other shared
    objects not necessarily installed on the system (in particular,
    via the use of dlopen(RTLD_GLOBAL) on Unix platforms) as well as
    manual lookup of function names to support e.g. GlueGen's
    ProcAddressTable glue code generation style without additional
    supporting code needed in the generated library. */

public class NativeLibrary {
  private static final int WINDOWS = 1;
  private static final int UNIX    = 2;
  private static final int MACOSX  = 3;
  private static boolean DEBUG;
  private static int platform;
  private static DynamicLinker dynLink;
  private static String[] prefixes;
  private static String[] suffixes;

  static {
    // Determine platform we're running on
    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          String osName = System.getProperty("os.name").toLowerCase();
          if (osName.startsWith("wind")) {
            platform = WINDOWS;
          } else if (osName.startsWith("mac os x")) {
            platform = MACOSX;
          } else {
            platform = UNIX;
          }

          DEBUG = (System.getProperty("gluegen.debug.NativeLibrary") != null);

          return null;
        }
      });
    // Instantiate dynamic linker implementation
    switch (platform) {
      case WINDOWS:
        dynLink = new WindowsDynamicLinkerImpl();
        prefixes = new String[] { "" };
        suffixes = new String[] { ".dll" };
        break;
      case UNIX:
        dynLink = new UnixDynamicLinkerImpl();
        prefixes = new String[] { "lib" };
        suffixes = new String[] { ".so" };
        break;
      case MACOSX:
        dynLink = new MacOSXDynamicLinkerImpl();
        prefixes = new String[] { "lib", "" };
        suffixes = new String[] { ".dylib", ".jnilib", "" };
        break;
      default:
        throw new InternalError("Platform not initialized properly");
    }
  }

  // Platform-specific representation for the handle to the open
  // library. This is an HMODULE on Windows and a void* (the result of
  // a dlopen() call) on Unix and Mac OS X platforms.
  private long libraryHandle;

  // May as well keep around the path to the library we opened
  private String libraryPath;

  // Private constructor to prevent arbitrary instances from floating around
  private NativeLibrary(long libraryHandle, String libraryPath) {
    this.libraryHandle = libraryHandle;
    this.libraryPath   = libraryPath;
  }

  /** Opens the given native library, assuming it has the same base
      name on all platforms, looking first in the system's search
      path, and in the context of the specified ClassLoader, which is
      used to help find the library in the case of e.g. Java Web Start. */
  public static NativeLibrary open(String libName, ClassLoader loader) {
    return open(libName, libName, libName, true, loader);
  }

  /** Opens the given native library, assuming it has the given base
      names (no "lib" prefix or ".dll/.so/.dylib" suffix) on the
      Windows, Unix and Mac OS X platforms, respectively, and in the
      context of the specified ClassLoader, which is used to help find
      the library in the case of e.g. Java Web Start. The
      searchSystemPathFirst argument changes the behavior to first
      search the default system path rather than searching it last.
      Note that we do not currently handle DSO versioning on Unix.
      Experience with JOAL and OpenAL has shown that it is extremely
      problematic to rely on a specific .so version (for one thing,
      ClassLoader.findLibrary on Unix doesn't work with files not
      ending in .so, for example .so.0), and in general if this
      dynamic loading facility is used correctly the version number
      will be irrelevant.
  */
  public static NativeLibrary open(String windowsLibName,
                                   String unixLibName,
                                   String macOSXLibName,
                                   boolean searchSystemPathFirst,
                                   ClassLoader loader) {
    List possiblePaths = enumerateLibraryPaths(windowsLibName,
                                               unixLibName,
                                               macOSXLibName,
                                               searchSystemPathFirst,
                                               loader);
    // Iterate down these and see which one if any we can actually find.
    for (Iterator iter = possiblePaths.iterator(); iter.hasNext(); ) {
      String path = (String) iter.next();
      if (DEBUG) {
        System.out.println("Trying to load " + path);
      }
      ensureNativeLibLoaded();
      long res = dynLink.openLibrary(path);
      if (res != 0) {
        if (DEBUG) {
          System.out.println("Successfully loaded " + path + ": res = 0x" + Long.toHexString(res));
        }
        return new NativeLibrary(res, path);
      }
    }

    if (DEBUG) {
      System.out.println("Did not succeed in loading (" + windowsLibName + ", " + unixLibName + ", " + macOSXLibName + ")");
    }

    // For now, just return null to indicate the open operation didn't
    // succeed (could also throw an exception if we could tell which
    // of the openLibrary operations actually failed)
    return null;
  }

  /** Looks up the given function name in this native library. */
  public long lookupFunction(String functionName) {
    if (libraryHandle == 0)
      throw new RuntimeException("Library is not open");
    return dynLink.lookupSymbol(libraryHandle, functionName);
  }

  /** Retrieves the low-level library handle from this NativeLibrary
      object. On the Windows platform this is an HMODULE, and on Unix
      and Mac OS X platforms the void* result of calling dlopen(). */
  public long getLibraryHandle() {
    return libraryHandle;
  }

  /** Retrieves the path under which this library was opened. */
  public String getLibraryPath() {
    return libraryPath;
  }

  /** Closes this native library. Further lookup operations are not
      allowed after calling this method. */
  public void close() {
    if (libraryHandle == 0)
      throw new RuntimeException("Library already closed");
    long handle = libraryHandle;
    libraryHandle = 0;
    dynLink.closeLibrary(handle);
  }

  /** Given the base library names (no prefixes/suffixes) for the
      various platforms, enumerate the possible locations and names of
      the indicated native library on the system. */
  private static List enumerateLibraryPaths(String windowsLibName,
                                            String unixLibName,
                                            String macOSXLibName,
                                            boolean searchSystemPathFirst,
                                            ClassLoader loader) {
    List paths = new ArrayList();
    String libName = selectName(windowsLibName, unixLibName, macOSXLibName);
    if (libName == null)
      return paths;

    // Allow user's full path specification to override our building of paths
    File file = new File(libName);
    if (file.isAbsolute()) {
	paths.add(libName);
	return paths;
    }

    String[] baseNames = buildNames(libName);

    if (searchSystemPathFirst) {
      // Add just the library names to use the OS's search algorithm
      for (int i = 0; i < baseNames.length; i++) {
        paths.add(baseNames[i]);
      }
    }

    // The idea to ask the ClassLoader to find the library is borrowed
    // from the LWJGL library
    String clPath = getPathFromClassLoader(libName, loader);
    if (DEBUG) {
      System.out.println("Class loader path to " + libName + ": " + clPath);
    }
    if (clPath != null) {
      paths.add(clPath);
    }

    // Add entries from java.library.path
    String javaLibraryPath =
      (String) AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            return System.getProperty("java.library.path");
          }
        });
    if (javaLibraryPath != null) {
      StringTokenizer tokenizer = new StringTokenizer(javaLibraryPath, File.pathSeparator);
      while (tokenizer.hasMoreTokens()) {
        addPaths(tokenizer.nextToken(), baseNames, paths);
      }
    }

    // Add current working directory
    String userDir =
      (String) AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            return System.getProperty("user.dir");
          }
        });
    addPaths(userDir, baseNames, paths);

    // Add probable Mac OS X-specific paths
    if (platform == MACOSX) {
      // Add historical location
      addPaths("/Library/Frameworks/" + libName + ".Framework", baseNames, paths);
      // Add current location
      addPaths("/System/Library/Frameworks/" + libName + ".Framework", baseNames, paths);
    }

    if (!searchSystemPathFirst) {
      // Add just the library names to use the OS's search algorithm
      for (int i = 0; i < baseNames.length; i++) {
        paths.add(baseNames[i]);
      }
    }

    return paths;
  }


  private static String selectName(String windowsLibName,
                                   String unixLibName,
                                   String macOSXLibName) {
    switch (platform) {
      case WINDOWS:
        return windowsLibName;
      case UNIX:
        return unixLibName;
      case MACOSX:
        return macOSXLibName;
      default:
        throw new InternalError();
    }
  }

  private static String[] buildNames(String libName) {
    // If the library name already has the prefix / suffix added
    // (principally because we want to force a version number on Unix
    // operating systems) then just return the library name.
    if (libName.startsWith(prefixes[0])) {
      if (libName.endsWith(suffixes[0])) {
        return new String[] { libName };
      }

      int idx = libName.indexOf(suffixes[0]);
      boolean ok = true;
      if (idx >= 0) {
        // Check to see if everything after it is a Unix version number
        for (int i = idx + suffixes[0].length();
             i < libName.length();
             i++) {
          char c = libName.charAt(i);
          if (!(c == '.' || (c >= '0' && c <= '9'))) {
            ok = false;
            break;
          }
        }
        if (ok) {
          return new String[] { libName };
        }
      }
    }

    String[] res = new String[prefixes.length * suffixes.length];
    int idx = 0;
    for (int i = 0; i < prefixes.length; i++) {
      for (int j = 0; j < suffixes.length; j++) {
        res[idx++] = prefixes[i] + libName + suffixes[j];
      }
    }
    return res;
  }

  private static void addPaths(String path, String[] baseNames, List paths) {
    for (int j = 0; j < baseNames.length; j++) {
      paths.add(path + File.separator + baseNames[j]);
    }
  }

  private static boolean initializedFindLibraryMethod = false;
  private static Method  findLibraryMethod = null;
  private static String getPathFromClassLoader(final String libName, final ClassLoader loader) {
    if (loader == null)
      return null;
    if (!initializedFindLibraryMethod) {
      AccessController.doPrivileged(new PrivilegedAction() {
          public Object run() {
            try {
              findLibraryMethod = ClassLoader.class.getDeclaredMethod("findLibrary",
                                                                      new Class[] { String.class });
              findLibraryMethod.setAccessible(true);
            } catch (Exception e) {
              // Fail silently disabling this functionality
            }
            initializedFindLibraryMethod = true;
            return null;
          }
        });
    }
    if (findLibraryMethod != null) {
      try {
        return (String) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
              try {
                return findLibraryMethod.invoke(loader, new Object[] { libName });
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
          });
      } catch (Exception e) {
        if (DEBUG) {
          e.printStackTrace();
        }
        // Fail silently and continue with other search algorithms
      }
    }
    return null;
  }

  private static volatile boolean loadedDynLinkNativeLib;
  private static void ensureNativeLibLoaded() {
    if (!loadedDynLinkNativeLib) {
      synchronized (NativeLibrary.class) {
        if (!loadedDynLinkNativeLib) {
          loadedDynLinkNativeLib = true;
          NativeLibLoader.loadGlueGenRT();
        }
      }
    }
  }
}
