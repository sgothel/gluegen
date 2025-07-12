/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package jogamp.common.os;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.jogamp.common.os.NativeLibrary.LibPath;

public final class WindowsDynamicLinkerImpl extends DynamicLinkerImpl {
  public static final int DONT_RESOLVE_DLL_REFERENCES           = 0x00000001;
  public static final int LOAD_LIBRARY_AS_DATAFILE              = 0x00000002;
  /**
   * Same as the standard search order except that in step 7
   * the system searches the folder that the specified module was loaded from (the top-loading module's folder)
   * instead of the executable's folder.
   *
   * Requires path to be absolute.
   *
   * @see https://learn.microsoft.com/en-us/windows/win32/dlls/dynamic-link-library-search-order?redirectedfrom=MSDN
   * @see https://learn.microsoft.com/en-us/windows/win32/api/libloaderapi/nf-libloaderapi-loadlibraryexw
   */
  public static final int LOAD_WITH_ALTERED_SEARCH_PATH         = 0x00000008;
  public static final int LOAD_IGNORE_CODE_AUTHZ_LEVEL          = 0x00000010;
  public static final int LOAD_LIBRARY_AS_IMAGE_RESOURCE        = 0x00000020;
  public static final int LOAD_LIBRARY_AS_DATAFILE_EXCLUSIVE    = 0x00000040;
  public static final int LOAD_LIBRARY_REQUIRE_SIGNED_TARGET    = 0x00000080;
  public static final int LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR      = 0x00000100;
  public static final int LOAD_LIBRARY_SEARCH_APPLICATION_DIR   = 0x00000200;
  public static final int LOAD_LIBRARY_SEARCH_USER_DIRS         = 0x00000400;
  public static final int LOAD_LIBRARY_SEARCH_SYSTEM32          = 0x00000800;
  public static final int LOAD_LIBRARY_SEARCH_DEFAULT_DIRS      = 0x00001000; //< LOAD_LIBRARY_SEARCH_APPLICATION_DIR | LOAD_LIBRARY_SEARCH_SYSTEM32 | LOAD_LIBRARY_SEARCH_USER_DIRS
  public static final int LOAD_LIBRARY_SAFE_CURRENT_DIRS        = 0x00002000;

  /** Interface to C language function: <br> <code> BOOL FreeLibrary(HANDLE hLibModule); </code>    */
  private static native int FreeLibrary(long hLibModule);

  /** Interface to C language function: <br> <code> DWORD GetLastError(void); </code>    */
  private static native int GetLastError();

  /** Interface to C language function: <br> <code> PROC GetProcAddressA(HANDLE hModule, LPCSTR lpProcName); </code>    */
  private static native long GetProcAddressA(long hModule, java.lang.String lpProcName);

  /** Interface to C language function: <br> <code> HANDLE LoadLibraryW(LPCWSTR lpLibFileName); </code>    */
  private static native long LoadLibraryW(java.lang.String lpLibFileName);

  /**
   * Interface to C language function: <br> <code> HANDLE LoadLibraryExW(LPCWSTR lpLibFileName, HANDLE hFile, DWORD dwFlags); </code>
   * @see https://learn.microsoft.com/en-us/windows/win32/api/libloaderapi/nf-libloaderapi-loadlibraryexw
   * @see https://learn.microsoft.com/en-us/windows/win32/dlls/dynamic-link-library-search-order?redirectedfrom=MSDN
   */
  private static native long LoadLibraryExW(java.lang.String lpLibFileName, int dwFlags);

  private static native long AddDllDirectory(java.lang.String lpLibFileName);
  private static native boolean RemoveDllDirectory(long dllDir);

  /** Interface to C language function: <br> <code> PROC GetModuleFileNameA(HANDLE hModule, LPSTR lpFilename, DWORD nSize); </code>    */
  private static native java.lang.String GetModuleFileNameA(long hModule);

  @Override
  protected final long openLibraryLocalImpl(final LibPath libpath) throws SecurityException {
    // How does that work under Windows ?
    // Don't know .. so it's an alias to global, for the time being
    return openLibraryImpl(libpath);
  }
  @Override
  protected final long openLibraryGlobalImpl(final LibPath libpath) throws SecurityException {
    return openLibraryImpl(libpath);
  }
  private static final boolean TRACE = false;
  private final long openLibraryImpl(final LibPath libpath) throws SecurityException {
    if( TRACE ) {
        System.err.println("openLibraryImpl: "+libpath);
    }
    int dwFlags = 0; // defaults to LoadLibraryW
    final List<Long> addedDllDirs = new ArrayList<Long>();
    if( libpath.addToSearchPath ) {
        if (!libpath.searchPathPrepend.isEmpty()) {
            final StringTokenizer st = new StringTokenizer(libpath.searchPathPrepend, File.pathSeparator);
            while (st.hasMoreTokens()) {
                final String dir = st.nextToken();
                final long dllDir = AddDllDirectory(dir);
                if( TRACE ) {
                    System.err.println("- AddDllDirectory: '"+dir+"' -> 0x"+Long.toHexString(dllDir));
                }
                if( 0 != dllDir ) {
                    addedDllDirs.add( dllDir );
                }
            }
        }
        if (libpath.isAbsolute) {
            dwFlags |= LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR | LOAD_LIBRARY_SEARCH_DEFAULT_DIRS;
        }
    }
    final long handle = LoadLibraryExW(libpath.path, dwFlags);
    if( TRACE ) {
        System.err.println("- LoadLibraryExW: '"+libpath.path+"', flags 0x"+Long.toHexString(dwFlags)+" -> handle 0x"+Long.toHexString(handle));
    }
    for(final Long dllDir : addedDllDirs) {
        final boolean r = RemoveDllDirectory(dllDir);
        if( TRACE ) {
            System.err.println("- RemoveDllDirectory: 0x"+Long.toHexString(dllDir)+" -> "+r);
        }
    }
    return handle;
  }

  @Override
  protected final String lookupLibraryPathnameImpl(final long libraryHandle, final String symbolName) throws SecurityException {
      // symbolName is not required
      return 0 != libraryHandle ? GetModuleFileNameA(libraryHandle) : null;
  }

  @Override
  protected final long lookupSymbolGlobalImpl(final String symbolName) throws SecurityException {
    if(DEBUG_LOOKUP) {
        System.err.println("lookupSymbolGlobal: Not supported on Windows");
    }
    // allow DynamicLibraryBundle to continue w/ local libs
    return 0;
  }

  private static final int symbolArgAlignment=4;  // 4 byte alignment of each argument
  private static final int symbolMaxArguments=12; // experience ..

  @Override
  protected final long lookupSymbolLocalImpl(final long libraryHandle, final String symbolName) throws IllegalArgumentException {
    String _symbolName = symbolName;
    long addr = GetProcAddressA(libraryHandle, _symbolName);
    if( 0 == addr ) {
        // __stdcall hack: try some @nn decorations,
        //                 the leading '_' must not be added (same with cdecl)
        for(int arg=0; 0==addr && arg<=symbolMaxArguments; arg++) {
            _symbolName = symbolName+"@"+(arg*symbolArgAlignment);
            addr = GetProcAddressA(libraryHandle, _symbolName);
        }
    }
    return addr;
  }

  @Override
  protected final void closeLibraryImpl(final long libraryHandle) throws IllegalArgumentException {
    FreeLibrary(libraryHandle);
  }

  @Override
  public final String getLastError() {
      final int err = GetLastError();
      return "Last error: 0x"+Integer.toHexString(err)+" ("+err+")";
  }

}
