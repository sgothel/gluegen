/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.common.os;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import jogamp.common.os.BionicDynamicLinker32bitImpl;
import jogamp.common.os.BionicDynamicLinker64BitImpl;
import jogamp.common.os.MacOSXDynamicLinkerImpl;
import jogamp.common.os.PlatformPropsImpl;
import jogamp.common.os.PosixDynamicLinkerImpl;
import jogamp.common.os.WindowsDynamicLinkerImpl;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.SecurityUtil;
import com.jogamp.common.util.cache.TempJarCache;

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

public final class NativeLibrary implements DynamicLookupHelper {
  private static final String[] prefixes;
  private static final String[] suffixes;
  private static final boolean isOSX;
  private static String sys_env_lib_path_varname;

  static {
    // Instantiate dynamic linker implementation
    switch (PlatformPropsImpl.OS_TYPE) {
      case WINDOWS:
        prefixes = new String[] { "" };
        suffixes = new String[] { ".dll" };
        sys_env_lib_path_varname = "PATH";
        isOSX = false;
        break;

      case MACOS:
      case IOS:
        prefixes = new String[] { "lib" };
        suffixes = new String[] { ".dylib" };
        sys_env_lib_path_varname = "DYLD_LIBRARY_PATH";
        isOSX = true;
        break;

      /*
      case ANDROID:
      case FREEBSD:
      case SUNOS:
      case HPUX:
      case OPENKODE:
      case LINUX: */
      default:
        prefixes = new String[] { "lib" };
        suffixes = new String[] { ".so" };
        sys_env_lib_path_varname = "LD_LIBRARY_PATH";
        isOSX = false;
        break;
    }
  }

  private final DynamicLinker dynLink;

  // Platform-specific representation for the handle to the open
  // library. This is an HMODULE on Windows and a void* (the result of
  // a dlopen() call) on Unix and Mac OS X platforms.
  private long libraryHandle;

  // May as well keep around the path to the library we opened
  private final String libraryPath;

  // Native library path of the opened native libraryHandle, maybe null
  private final String nativeLibraryPath;

  private final boolean global;

  // Private constructor to prevent arbitrary instances from floating around
  private NativeLibrary(final DynamicLinker dynLink, final long libraryHandle, final String libraryPath, final boolean global, final String symbolName) {
    this.dynLink = dynLink;
    this.libraryHandle = libraryHandle;
    this.libraryPath   = libraryPath;
    this.nativeLibraryPath = dynLink.lookupLibraryPathname(libraryHandle, symbolName);
    this.global        = global;
    if (DEBUG) {
      System.err.println("NativeLibrary.open(): Successfully loaded: " + this);
    }
  }

  @Override
  public final String toString() {
    final String nlp_s = null != nativeLibraryPath ? ", native '"+nativeLibraryPath+"'" : "";
    return "NativeLibrary[" + dynLink.getClass().getSimpleName() + ", path[given '" + libraryPath + "'"+nlp_s+"], 0x" +
           Long.toHexString(libraryHandle) + ", global " + global + "]";
  }

  /**
   * Returns the system's environment variable name used for the dynamic linker to resolve library locations, e.g.
   * - Windows: PATH
   * - MacOS: DYLD_LIBRARY_PATH
   * - Unix: LD_LIBRARY_PATH
   */
  public static final String getSystemEnvLibraryPathVarname() { return sys_env_lib_path_varname; }

  /**
   * Returns a list of system paths, from the {@link #getSystemEnvLibraryPathVarname()} variable.
   */
  public static final List<String> getSystemEnvLibraryPaths() {
      final String paths =
              SecurityUtil.doPrivileged(new PrivilegedAction<String>() {
                  @Override
                  public String run() {
                      return System.getenv(getSystemEnvLibraryPathVarname());
                  }
              });
      final List<String> res = new ArrayList<String>();
      if( null != paths && paths.length() > 0 ) {
          final StringTokenizer st = new StringTokenizer(paths, File.pathSeparator);
          while (st.hasMoreTokens()) {
              res.add(st.nextToken());
          }
      }
      return res;
  }

  /** Opens the given native library, assuming it has the same base
      name on all platforms.
      <p>
      The {@code searchSystemPath} argument changes the behavior to
      either use the default system path or not at all.
      </p>
      <p>
      Assuming {@code searchSystemPath} is {@code true},
      the {@code searchSystemPathFirst} argument changes the behavior to first
      search the default system path rather than searching it last.
      </p>
   * @param libName library name, with or without prefix and suffix
   * @param searchSystemPath if {@code true} library shall be searched in the system path <i>(default)</i>, otherwise {@code false}.
   * @param searchSystemPathFirst if {@code true} system path shall be searched <i>first</i> <i>(default)</i>, rather than searching it last.
   *                              if {@code searchSystemPath} is {@code false} this argument is ignored.
   * @param loader {@link ClassLoader} to locate the library
   * @param global if {@code true} allows system wide access of the loaded library, otherwise access is restricted to the process.
   * @return {@link NativeLibrary} instance or {@code null} if library could not be loaded.
   * @throws SecurityException if user is not granted access for the named library.
   * @since 2.4.0
   */
  public static final NativeLibrary open(final String libName,
                                         final boolean searchSystemPath,
                                         final boolean searchSystemPathFirst,
                                         final ClassLoader loader, final boolean global) throws SecurityException {
    return open(libName, libName, libName, searchSystemPath, searchSystemPathFirst, loader, global, null);
  }

  /** Opens the given native library, assuming it has the same base
      name on all platforms.
      <p>
      The {@code searchSystemPath} argument changes the behavior to
      either use the default system path or not at all.
      </p>
      <p>
      Assuming {@code searchSystemPath} is {@code true},
      the {@code searchSystemPathFirst} argument changes the behavior to first
      search the default system path rather than searching it last.
      </p>
   * @param libName library name, with or without prefix and suffix
   * @param searchSystemPath if {@code true} library shall be searched in the system path <i>(default)</i>, otherwise {@code false}.
   * @param searchSystemPathFirst if {@code true} system path shall be searched <i>first</i> <i>(default)</i>, rather than searching it last.
   *                              if {@code searchSystemPath} is {@code false} this argument is ignored.
   * @param loader {@link ClassLoader} to locate the library
   * @param global if {@code true} allows system wide access of the loaded library, otherwise access is restricted to the process.
   * @param symbolName optional symbol name for an OS which requires the symbol's address to retrieve the path of the containing library
   * @return {@link NativeLibrary} instance or {@code null} if library could not be loaded.
   * @throws SecurityException if user is not granted access for the named library.
   * @since 2.4.0
   */
  public static final NativeLibrary open(final String libName,
                                         final boolean searchSystemPath,
                                         final boolean searchSystemPathFirst,
                                         final ClassLoader loader, final boolean global, final String symbolName) throws SecurityException {
    return open(libName, libName, libName, searchSystemPath, searchSystemPathFirst, loader, global, symbolName);
  }

  /** Opens the given native library, assuming it has the given base
      names (no "lib" prefix or ".dll/.so/.dylib" suffix) on the
      Windows, Unix and Mac OS X platforms, respectively, and in the
      context of the specified ClassLoader, which is used to help find
      the library in the case of e.g. Java Web Start.
      <p>
      The {@code searchSystemPath} argument changes the behavior to
      either use the default system path or not at all.
      </p>
      <p>
      Assuming {@code searchSystemPath} is {@code true},
      the {@code searchSystemPathFirst} argument changes the behavior to first
      search the default system path rather than searching it last.
      </p>
      Note that we do not currently handle DSO versioning on Unix.
      Experience with JOAL and OpenAL has shown that it is extremely
      problematic to rely on a specific .so version (for one thing,
      ClassLoader.findLibrary on Unix doesn't work with files not
      ending in .so, for example .so.0), and in general if this
      dynamic loading facility is used correctly the version number
      will be irrelevant.
   * @param windowsLibName windows library name, with or without prefix and suffix
   * @param unixLibName unix library name, with or without prefix and suffix
   * @param macOSXLibName mac-osx library name, with or without prefix and suffix
   * @param searchSystemPath if {@code true} library shall be searched in the system path <i>(default)</i>, otherwise {@code false}.
   * @param searchSystemPathFirst if {@code true} system path shall be searched <i>first</i> <i>(default)</i>, rather than searching it last.
   *                              if {@code searchSystemPath} is {@code false} this argument is ignored.
   * @param loader {@link ClassLoader} to locate the library
   * @param global if {@code true} allows system wide access of the loaded library, otherwise access is restricted to the process.
   * @param symbolName optional symbol name for an OS which requires the symbol's address to retrieve the path of the containing library
   * @return {@link NativeLibrary} instance or {@code null} if library could not be loaded.
   * @throws SecurityException if user is not granted access for the named library.
   */
  public static final NativeLibrary open(final String windowsLibName,
                                         final String unixLibName,
                                         final String macOSXLibName,
                                         final boolean searchSystemPath,
                                         final boolean searchSystemPathFirst,
                                         final ClassLoader loader, final boolean global, final String symbolName) throws SecurityException {
    final List<String> possiblePaths = enumerateLibraryPaths(windowsLibName,
                                                       unixLibName,
                                                       macOSXLibName,
                                                       searchSystemPath, searchSystemPathFirst,
                                                       loader);
    Platform.initSingleton(); // loads native gluegen_rt library

    final DynamicLinker dynLink = getDynamicLinker();

    // Iterate down these and see which one if any we can actually find.
    for (final Iterator<String> iter = possiblePaths.iterator(); iter.hasNext(); ) {
        final String path = iter.next();
        if (DEBUG) {
            System.err.println("NativeLibrary.open(global "+global+"): Trying to load " + path);
        }
        long res;
        Throwable t = null;
        try {
            if(global) {
                res = dynLink.openLibraryGlobal(path, DEBUG);
            } else {
                res = dynLink.openLibraryLocal(path, DEBUG);
            }
        } catch (final Throwable t1) {
            t = t1;
            res = 0;
        }
        if ( 0 != res ) {
            return new NativeLibrary(dynLink, res, path, global, symbolName);
        } else if( DEBUG ) {
            if( null != t ) {
                System.err.println("NativeLibrary.open: Caught "+t.getClass().getSimpleName()+": "+t.getMessage());
            }
            String errstr;
            try {
                errstr = dynLink.getLastError();
            } catch (final Throwable t2) { errstr=null; }
            System.err.println("NativeLibrary.open: Last error "+errstr);
            if( null != t ) {
                t.printStackTrace();
            }
        }
    }

    if (DEBUG) {
      System.err.println("NativeLibrary.open(global "+global+"): Did not succeed in loading (" + windowsLibName + ", " + unixLibName + ", " + macOSXLibName + ")");
    }

    // For now, just return null to indicate the open operation didn't
    // succeed (could also throw an exception if we could tell which
    // of the openLibrary operations actually failed)
    return null;
  }

  @Override
  public final void claimAllLinkPermission() throws SecurityException {
      dynLink.claimAllLinkPermission();
  }
  @Override
  public final void releaseAllLinkPermission() throws SecurityException {
      dynLink.releaseAllLinkPermission();
  }

  @Override
  public final long dynamicLookupFunction(final String funcName) throws SecurityException {
    if ( 0 == libraryHandle ) {
      throw new RuntimeException("Library is not open");
    }
    return dynLink.lookupSymbol(libraryHandle, funcName);
  }

  @Override
  public final boolean isFunctionAvailable(final String funcName) throws SecurityException {
    if ( 0 == libraryHandle ) {
      throw new RuntimeException("Library is not open");
    }
    return 0 != dynLink.lookupSymbol(libraryHandle, funcName);
  }

  /** Looks up the given function name in all loaded libraries.
   * @throws SecurityException if user is not granted access for the named library.
   */
  public final long dynamicLookupFunctionGlobal(final String funcName) throws SecurityException {
    return dynLink.lookupSymbolGlobal(funcName);
  }

  /* pp */ final DynamicLinker dynamicLinker() { return dynLink; }

  /* pp */ static DynamicLinker getDynamicLinker() {
      final DynamicLinker dynLink;
      switch (PlatformPropsImpl.OS_TYPE) {
          case WINDOWS:
              dynLink = new WindowsDynamicLinkerImpl();
              break;

          case MACOS:
          case IOS:
              dynLink = new MacOSXDynamicLinkerImpl();
              break;

          case ANDROID:
              if( PlatformPropsImpl.CPU_ARCH.is32Bit ) {
                  dynLink = new BionicDynamicLinker32bitImpl();
              } else {
                  dynLink = new BionicDynamicLinker64BitImpl();
              }
              break;

          default:
              dynLink = new PosixDynamicLinkerImpl();
              break;
      }
      return dynLink;
  }

  /** Retrieves the low-level library handle from this NativeLibrary
      object. On the Windows platform this is an HMODULE, and on Unix
      and Mac OS X platforms the void* result of calling dlopen(). */
  public final long getLibraryHandle() {
    return libraryHandle;
  }

  /** Retrieves the path under which this library was opened. */
  public final String getLibraryPath() {
    return libraryPath;
  }

  /** Returns the native library path of the opened native {@link #getLibraryHandle()}, maybe null if not supported by OS. */
  public final String getNativeLibraryPath() {
    return nativeLibraryPath;
  }

  /** Closes this native library. Further lookup operations are not
      allowed after calling this method.
   * @throws SecurityException if user is not granted access for the named library.
   */
  public final void close() throws SecurityException {
    if (DEBUG) {
      System.err.println("NativeLibrary.close(): closing " + this);
    }
    if ( 0 == libraryHandle ) {
      throw new RuntimeException("Library already closed");
    }
    final long handle = libraryHandle;
    libraryHandle = 0;
    dynLink.closeLibrary(handle, DEBUG);
    if (DEBUG) {
      System.err.println("NativeLibrary.close(): Successfully closed " + this);
      ExceptionUtils.dumpStack(System.err);
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
  public static final String isValidNativeLibraryName(final String libName, final boolean isLowerCaseAlready) {
    final String libBaseName;
    try {
        libBaseName = IOUtil.getBasename(libName);
    } catch (final URISyntaxException uriEx) {
        throw new IllegalArgumentException(uriEx);
    }
    final String libBaseNameLC = isLowerCaseAlready ? libBaseName : libBaseName.toLowerCase();
    int prefixIdx = -1;
    for(int i=0; i<prefixes.length && 0 > prefixIdx; i++) {
        if (libBaseNameLC.startsWith(prefixes[i])) {
            prefixIdx = i;
        }
    }
    if( 0 <= prefixIdx ) {
        for(int i=0; i<suffixes.length; i++) {
            if (libBaseNameLC.endsWith(suffixes[i])) {
                final int s = prefixes[prefixIdx].length();
                final int e = suffixes[i].length();
                return libBaseName.substring(s, libBaseName.length()-e);
            }
        }
    }
    return null;
  }

  /** Given the base library names (no prefixes/suffixes) for the
      various platforms, enumerate the possible locations and names of
      the indicated native library on the system not using the system path. */
  public static final List<String> enumerateLibraryPaths(final String windowsLibName,
                                                   final String unixLibName,
                                                   final String macOSXLibName,
                                                   final ClassLoader loader) {
      return enumerateLibraryPaths(windowsLibName, unixLibName, macOSXLibName,
                                  false /* searchSystemPath */, false /* searchSystemPathFirst */,
                                  loader);
  }
  /** Given the base library names (no prefixes/suffixes) for the
      various platforms, enumerate the possible locations and names of
      the indicated native library on the system using the system path. */
  public static final List<String> enumerateLibraryPaths(final String windowsLibName,
                                                   final String unixLibName,
                                                   final String macOSXLibName,
                                                   final boolean searchSystemPathFirst,
                                                   final ClassLoader loader) {
      return enumerateLibraryPaths(windowsLibName, unixLibName, macOSXLibName,
                                  true /* searchSystemPath */, searchSystemPathFirst,
                                  loader);
  }
  private static final List<String> enumerateLibraryPaths(final String windowsLibName,
                                                   final String unixLibName,
                                                   final String macOSXLibName,
                                                   final boolean searchSystemPath,
                                                   final boolean searchSystemPathFirst,
                                                   final ClassLoader loader) {
    final List<String> paths = new ArrayList<String>();
    final String libName = selectName(windowsLibName, unixLibName, macOSXLibName);
    if (libName == null) {
        if (DEBUG) {
            System.err.println("NativeLibrary.enumerateLibraryPaths: empty, no libName selected");
        }
        return paths;
    }
    if (DEBUG) {
        System.err.println("NativeLibrary.enumerateLibraryPaths: libName '"+libName+"'");
    }

    // Allow user's full path specification to override our building of paths
    final File file = new File(libName);
    if (file.isAbsolute()) {
        paths.add(libName);
        if (DEBUG) {
            System.err.println("NativeLibrary.enumerateLibraryPaths: done, absolute path found '"+libName+"'");
        }
        return paths;
    }

    final String[] baseNames = buildNames(libName);
    if (DEBUG) {
        System.err.println("NativeLibrary.enumerateLibraryPaths: baseNames: "+Arrays.toString(baseNames));
    }

    if( searchSystemPath && searchSystemPathFirst ) {
        // Utilize system's library path environment variable first
        {
            final List<String> sysLibPaths = getSystemEnvLibraryPaths();
            int count = 0;
            for(final String sysLibPath : sysLibPaths) {
                addRelPaths("add.ssp_path_"+count, sysLibPath, baseNames, paths);
                ++count;
            }
        }
        // Add just the library names to use the OS's search algorithm
        for (int i = 0; i < baseNames.length; i++) {
            if (DEBUG) {
                System.err.println("NativeLibrary.enumerateLibraryPaths: add.ssp_default: "+baseNames[i]);
            }
            paths.add(baseNames[i]);
        }
        // Add probable Mac OS X-specific paths
        if ( isOSX ) {
            // Add historical location
            addAbsPaths("add.ssp_1st_macos_old", "/Library/Frameworks/" + libName + ".framework", baseNames, paths);
            // Add current location
            addAbsPaths("add.ssp_1st_macos_cur", "/System/Library/Frameworks/" + libName + ".framework", baseNames, paths);
        }
    }

    // The idea to ask the ClassLoader to find the library is borrowed
    // from the LWJGL library
    final String clPath = findLibrary(libName, loader);
    if (clPath != null) {
        if (DEBUG) {
            System.err.println("NativeLibrary.enumerateLibraryPaths: add.clp: "+clPath);
        }
        paths.add(clPath);
    }

    // Add entries from java.library.path
    final String[] javaLibraryPaths =
      SecurityUtil.doPrivileged(new PrivilegedAction<String[]>() {
          @Override
          public String[] run() {
            int count = 0;
            final String usrPath = System.getProperty("java.library.path");
            if(null != usrPath) {
                count++;
            }
            final String sysPath;
            if( searchSystemPath ) {
                sysPath = System.getProperty("sun.boot.library.path");
                if(null != sysPath) {
                    count++;
                }
            } else {
                sysPath = null;
            }
            final String[] res = new String[count];
            int i=0;
            if( null != sysPath && searchSystemPathFirst ) {
                res[i++] = sysPath;
            }
            if(null != usrPath) {
                res[i++] = usrPath;
            }
            if( null != sysPath && !searchSystemPathFirst ) {
                res[i++] = sysPath;
            }
            return res;
          }
        });
    if ( null != javaLibraryPaths ) {
        int count = 0;
        for( int i=0; i < javaLibraryPaths.length; i++ ) {
            final StringTokenizer tokenizer = new StringTokenizer(javaLibraryPaths[i], File.pathSeparator);
            while (tokenizer.hasMoreTokens()) {
                addRelPaths("add.java.library.path_"+count, tokenizer.nextToken(), baseNames, paths);
                ++count;
            }
        }
    }

    // Add current working directory
    final String userDir =
      SecurityUtil.doPrivileged(new PrivilegedAction<String>() {
          @Override
          public String run() {
            return System.getProperty("user.dir");
          }
        });
    addAbsPaths("add.user.dir.std", userDir, baseNames, paths);

    // Add current working directory + natives/os-arch/ + library names
    // to handle Bug 1145 cc1 using an unpacked fat-jar
    addAbsPaths("add.user.dir.fat", userDir+File.separator+"natives"+File.separator+PlatformPropsImpl.os_and_arch, baseNames, paths);

    if( searchSystemPath && !searchSystemPathFirst ) {
        // Utilize system's library path environment variable first
        {
            final List<String> sysLibPaths = getSystemEnvLibraryPaths();
            int count = 0;
            for(final String sysLibPath : sysLibPaths) {
                addRelPaths("add.ssp_path_"+count, sysLibPath, baseNames, paths);
                ++count;
            }
        }
        // Add just the library names to use the OS's search algorithm
        for (int i = 0; i < baseNames.length; i++) {
            if (DEBUG) {
                System.err.println("NativeLibrary.enumerateLibraryPaths: add.ssp_default: "+baseNames[i]);
            }
            paths.add(baseNames[i]);
        }
        // Add probable Mac OS X-specific paths
        if ( isOSX ) {
            // Add historical location
            addAbsPaths("add.ssp_lst_macos_old", "/Library/Frameworks/" + libName + ".Framework", baseNames, paths);
            // Add current location
            addAbsPaths("add.ssp_lst_macos_cur", "/System/Library/Frameworks/" + libName + ".Framework", baseNames, paths);
        }
    }
    if (DEBUG) {
        System.err.println("NativeLibrary.enumerateLibraryPaths: done: "+paths.toString());
    }
    return paths;
  }


  private static final String selectName(final String windowsLibName,
                                   final String unixLibName,
                                   final String macOSXLibName) {
    switch (PlatformPropsImpl.OS_TYPE) {
      case WINDOWS:
        return windowsLibName;

      case MACOS:
      case IOS:
        return macOSXLibName;

      default:
        return unixLibName;
    }
  }

  private static final String[] buildNames(final String libName) {
      // If the library name already has the prefix / suffix added
      // (principally because we want to force a version number on Unix
      // operating systems) then just return the library name.
      final String libBaseNameLC;
      try {
          libBaseNameLC = IOUtil.getBasename(libName).toLowerCase();
      } catch (final URISyntaxException uriEx) {
          throw new IllegalArgumentException(uriEx);
      }

      int prefixIdx = -1;
      for(int i=0; i<prefixes.length && 0 > prefixIdx; i++) {
          if (libBaseNameLC.startsWith(prefixes[i])) {
              prefixIdx = i;
          }
      }
      if( 0 <= prefixIdx ) {
          for(int i=0; i<suffixes.length; i++) {
              if (libBaseNameLC.endsWith(suffixes[i])) {
                  return new String[] { libName };
              }
          }
          int suffixIdx = -1;
          for(int i=0; i<suffixes.length && 0 > suffixIdx; i++) {
              suffixIdx = libBaseNameLC.indexOf(suffixes[i]);
          }
          boolean ok = true;
          if (suffixIdx >= 0) {
              // Check to see if everything after it is a Unix version number
              for (int i = suffixIdx + suffixes[0].length();
                      i < libName.length();
                      i++) {
                  final char c = libName.charAt(i);
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

      final String[] res = new String[prefixes.length * suffixes.length + ( isOSX ? 1 : 0 )];
      int idx = 0;
      for (int i = 0; i < prefixes.length; i++) {
          for (int j = 0; j < suffixes.length; j++) {
              res[idx++] = prefixes[i] + libName + suffixes[j];
          }
      }
      if ( isOSX ) {
          // Plain library-base-name in Framework folder
          res[idx++] = libName;
      }
      return res;
  }

  private static final void addRelPaths(final String cause, final String path, final String[] baseNames, final List<String> paths) {
      final String abs_path;
      try {
          final File fpath = new File(path);
          abs_path = fpath.getCanonicalPath();
      } catch( final IOException ioe ) {
          if (DEBUG) {
              System.err.println("NativeLibrary.enumerateLibraryPaths: "+cause+": Exception "+ioe.getMessage()+", from path "+path);
          }
          return;
      }
      addAbsPaths(cause, abs_path, baseNames, paths);
  }
  private static final void addAbsPaths(final String cause, final String abs_path, final String[] baseNames, final List<String> paths) {
      for (int j = 0; j < baseNames.length; j++) {
          final String p = abs_path + File.separator + baseNames[j];
          if (DEBUG) {
              System.err.println("NativeLibrary.enumerateLibraryPaths: "+cause+": "+p+", from path "+abs_path);
          }
          paths.add(p);
      }
  }

  private static boolean initializedFindLibraryMethod = false;
  private static Method  findLibraryMethod = null;
  private static final String findLibraryImpl(final String libName, final ClassLoader loader) {
    if( PlatformPropsImpl.JAVA_9 ) {
        return null;
    }
    if (loader == null) {
        return null;
    }
    if (!initializedFindLibraryMethod) {
      SecurityUtil.doPrivileged(new PrivilegedAction<Object>() {
          @Override
          public Object run() {
            try {
              findLibraryMethod = ClassLoader.class.getDeclaredMethod("findLibrary",
                                                                      new Class[] { String.class });
              findLibraryMethod.setAccessible(true);
            } catch (final Exception e) {
              // Fail silently disabling this functionality
            }
            initializedFindLibraryMethod = true;
            return null;
          }
        });
    }
    if (findLibraryMethod != null) {
      try {
        return SecurityUtil.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
              try {
                return (String) findLibraryMethod.invoke(loader, new Object[] { libName });
              } catch (final Exception e) {
                throw new RuntimeException(e);
              }
            }
          });
      } catch (final Exception e) {
        if (DEBUG) {
          e.printStackTrace();
        }
        // Fail silently and continue with other search algorithms
      }
    }
    return null;
  }
  public static final String findLibrary(final String libName, final ClassLoader loader) {
    String res = null;
    if( TempJarCache.isInitialized(true) ) {
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
