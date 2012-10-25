/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2011 JogAmp Community. All rights reserved.
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

package com.jogamp.common.os;

import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.cache.TempJarCache;

import jogamp.common.os.MacOSXDynamicLinkerImpl;
import jogamp.common.os.PlatformPropsImpl;
import jogamp.common.os.UnixDynamicLinkerImpl;
import jogamp.common.os.WindowsDynamicLinkerImpl;

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

public class NativeLibrary implements DynamicLookupHelper {  
  private static DynamicLinker dynLink;
  private static String[] prefixes;
  private static String[] suffixes;

  static {
    // Instantiate dynamic linker implementation
    switch (PlatformPropsImpl.OS_TYPE) {
      case WINDOWS:
        dynLink = new WindowsDynamicLinkerImpl();
        prefixes = new String[] { "" };
        suffixes = new String[] { ".dll" };
        break;
        
      case MACOS:
        dynLink = new MacOSXDynamicLinkerImpl();
        prefixes = new String[] { "lib" };
        suffixes = new String[] { ".dylib", ".jnilib" };
        break;

      /*
      case FREEBSD:
      case DALVIK:
      case SUNOS:
      case HPUX: 
      case OPENKODE:         
      case LINUX: */
      default:
        dynLink = new UnixDynamicLinkerImpl();
        prefixes = new String[] { "lib" };
        suffixes = new String[] { ".so" };
        break;
    }
  }

  // Platform-specific representation for the handle to the open
  // library. This is an HMODULE on Windows and a void* (the result of
  // a dlopen() call) on Unix and Mac OS X platforms.
  private long libraryHandle;

  // May as well keep around the path to the library we opened
  private String libraryPath;
  
  private boolean global;

  // Private constructor to prevent arbitrary instances from floating around
  private NativeLibrary(long libraryHandle, String libraryPath, boolean global) {
    this.libraryHandle = libraryHandle;
    this.libraryPath   = libraryPath;
    this.global        = global;
    if (DEBUG) {
      System.err.println("NativeLibrary.open(): Successfully loaded: " + this);
    }
  }

  public String toString() {
    return "NativeLibrary[" + libraryPath + ", 0x" + Long.toHexString(libraryHandle) + ", global " + global + "]";
  }

  /** Opens the given native library, assuming it has the same base
      name on all platforms, looking first in the system's search
      path, and in the context of the specified ClassLoader, which is
      used to help find the library in the case of e.g. Java Web Start. */
  public static NativeLibrary open(String libName, ClassLoader loader) {
    return open(libName, libName, libName, true, loader, true);
  }

  /** Opens the given native library, assuming it has the same base
      name on all platforms, looking first in the system's search
      path, and in the context of the specified ClassLoader, which is
      used to help find the library in the case of e.g. Java Web Start. */
  public static NativeLibrary open(String libName, ClassLoader loader, boolean global) {
    return open(libName, libName, libName, true, loader, global);
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
    return open(windowsLibName, unixLibName, macOSXLibName, searchSystemPathFirst, loader, true);
  }

  public static NativeLibrary open(String windowsLibName,
                                   String unixLibName,
                                   String macOSXLibName,
                                   boolean searchSystemPathFirst,
                                   ClassLoader loader, boolean global) {
    List<String> possiblePaths = enumerateLibraryPaths(windowsLibName,
                                                       unixLibName,
                                                       macOSXLibName,
                                                       searchSystemPathFirst,
                                                       loader);
    // Iterate down these and see which one if any we can actually find.
    for (Iterator<String> iter = possiblePaths.iterator(); iter.hasNext(); ) {
      String path = iter.next();
      if (DEBUG) {
        System.err.println("NativeLibrary.open(): Trying to load " + path);
      }
      Platform.initSingleton(); // loads native gluegen-rt library
      long res;
      if(global) {
          res = dynLink.openLibraryGlobal(path, DEBUG);
      } else {
          res = dynLink.openLibraryLocal(path, DEBUG);
      }
      if (res != 0) {
        return new NativeLibrary(res, path, global);
      }
    }

    if (DEBUG) {
      System.err.println("NativeLibrary.open(): Did not succeed in loading (" + windowsLibName + ", " + unixLibName + ", " + macOSXLibName + ")");
    }

    // For now, just return null to indicate the open operation didn't
    // succeed (could also throw an exception if we could tell which
    // of the openLibrary operations actually failed)
    return null;
  }

  /** Looks up the given function name in this native library. */
  public long dynamicLookupFunction(String funcName) {
    if (libraryHandle == 0)
      throw new RuntimeException("Library is not open");
    return dynLink.lookupSymbol(libraryHandle, funcName);
  }

  /** Looks up the given function name in all loaded libraries. */
  public static long dynamicLookupFunctionGlobal(String funcName) {
    return dynLink.lookupSymbolGlobal(funcName);
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
    if (DEBUG) {
      System.err.println("NativeLibrary.close(): closing " + this);
    }
    if (libraryHandle == 0) {
      throw new RuntimeException("Library already closed");
    }
    long handle = libraryHandle;
    libraryHandle = 0;
    dynLink.closeLibrary(handle);
    if (DEBUG) {
      System.err.println("NativeLibrary.close(): Successfully closed " + this);
      Thread.dumpStack();
    }
  }

  /**
   * Comparison of prefix and suffix of the given libName's basename
   * is performed case insensitive <br>
   *  
   * @param libName the full path library name with prefix and suffix 
   * @param isLowerCaseAlready indicates if libName is already lower-case
   * 
   * @return basename of libName w/o path, ie. /usr/lib/libDrinkBeer.so -> DrinkBeer on Unix systems, but null on Windows.
   */
  public static String isValidNativeLibraryName(String libName, boolean isLowerCaseAlready) {
    libName = IOUtil.getBasename(libName);
    final String libNameLC = isLowerCaseAlready ? libName : libName.toLowerCase();
    for(int i=0; i<prefixes.length; i++) {
        if (libNameLC.startsWith(prefixes[i])) {
            for(int j=0; j<suffixes.length; j++) {
                if (libNameLC.endsWith(suffixes[j])) {
                    final int s = prefixes[i].length();
                    final int e = suffixes[j].length();
                    return libName.substring(s, libName.length()-e);
                }
            }
        }    
    }
    return null;  
  }
  
  /** Given the base library names (no prefixes/suffixes) for the
      various platforms, enumerate the possible locations and names of
      the indicated native library on the system. */
  private static List<String> enumerateLibraryPaths(String windowsLibName,
                                                    String unixLibName,
                                                    String macOSXLibName,
                                                    boolean searchSystemPathFirst,
                                                    ClassLoader loader) {
    List<String> paths = new ArrayList<String>();
    String libName = selectName(windowsLibName, unixLibName, macOSXLibName);
    if (libName == null) {
      return paths;
    }

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
    final String clPath = findLibrary(libName, loader);
    if (clPath != null) {
      paths.add(clPath);
    }

    // Add entries from java.library.path
    String javaLibraryPath =
      AccessController.doPrivileged(new PrivilegedAction<String>() {
          public String run() {
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
      AccessController.doPrivileged(new PrivilegedAction<String>() {
          public String run() {
            return System.getProperty("user.dir");
          }
        });
    addPaths(userDir, baseNames, paths);

    if (!searchSystemPathFirst) {
      // Add just the library names to use the OS's search algorithm
      for (int i = 0; i < baseNames.length; i++) {
        paths.add(baseNames[i]);
      }
    }

    // Add probable Mac OS X-specific paths
    if (PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS) {
      // Add historical location
      addPaths("/Library/Frameworks/" + libName + ".Framework", baseNames, paths);
      // Add current location
      addPaths("/System/Library/Frameworks/" + libName + ".Framework", baseNames, paths);
    }
    
    return paths;
  }


  private static String selectName(String windowsLibName,
                                   String unixLibName,
                                   String macOSXLibName) {
    switch (PlatformPropsImpl.OS_TYPE) {
      case WINDOWS:
        return windowsLibName;
        
      case MACOS:
        return macOSXLibName;

      /*
      case FREEBSD:
      case DALVIK:
      case SUNOS:
      case HPUX: 
      case OPENKODE:         
      case LINUX: */
      default:
        return unixLibName;
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

    String[] res = new String[prefixes.length * suffixes.length + 
                              ( PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS ? 1 : 0 )];
    int idx = 0;
    for (int i = 0; i < prefixes.length; i++) {
      for (int j = 0; j < suffixes.length; j++) {
        res[idx++] = prefixes[i] + libName + suffixes[j];
      }
    }
    if (PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS) {
        // Plain library-base-name in Framework folder
        res[idx++] = libName;
    }
    return res;
  }

  private static void addPaths(String path, String[] baseNames, List<String> paths) {
    for (int j = 0; j < baseNames.length; j++) {
      paths.add(path + File.separator + baseNames[j]);
    }
  }

  private static boolean initializedFindLibraryMethod = false;
  private static Method  findLibraryMethod = null;
  private static String findLibraryImpl(final String libName, final ClassLoader loader) {
    if (loader == null) {
      return null;
    }
    if (!initializedFindLibraryMethod) {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
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
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
              try {
                return (String) findLibraryMethod.invoke(loader, new Object[] { libName });
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
  public static String findLibrary(final String libName, final ClassLoader loader) {
    String res = null;
    if(TempJarCache.isInitialized()) {
        res = TempJarCache.findLibrary(libName);
        if (DEBUG) {
          System.err.println("NativeLibrary.findLibrary(<"+libName+">) (TempJarCache): "+res);
        }
    }
    if(null == res) {
        res = findLibraryImpl(libName, loader);
        if (DEBUG) {
          System.err.println("NativeLibrary.findLibrary(<"+libName+">, "+loader+") (CL): "+res);
        }
    }
    return res;
  }
}
