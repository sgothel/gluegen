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

public final class WindowsDynamicLinkerImpl extends DynamicLinkerImpl {

  /** Interface to C language function: <br> <code> BOOL FreeLibrary(HANDLE hLibModule); </code>    */
  private static native int FreeLibrary(long hLibModule);

  /** Interface to C language function: <br> <code> DWORD GetLastError(void); </code>    */
  private static native int GetLastError();

  /** Interface to C language function: <br> <code> PROC GetProcAddressA(HANDLE hModule, LPCSTR lpProcName); </code>    */
  private static native long GetProcAddressA(long hModule, java.lang.String lpProcName);

  /** Interface to C language function: <br> <code> HANDLE LoadLibraryW(LPCWSTR lpLibFileName); </code>    */
  private static native long LoadLibraryW(java.lang.String lpLibFileName);

  @Override
  protected final long openLibraryLocalImpl(final String libraryName) throws SecurityException {
    // How does that work under Windows ?
    // Don't know .. so it's an alias to global, for the time being
    return LoadLibraryW(libraryName);
  }

  @Override
  protected final long openLibraryGlobalImpl(final String libraryName) throws SecurityException {
    return LoadLibraryW(libraryName);
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
