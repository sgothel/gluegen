/**
 * Copyright 2013-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.common.os.NativeLibrary.LibPath;

/** Low level secure dynamic linker access. */
public interface DynamicLinker {
  public static final boolean DEBUG = NativeLibrary.DEBUG;
  public static final boolean DEBUG_LOOKUP = NativeLibrary.DEBUG_LOOKUP;

  public void claimAllLinkPermission();

  public void releaseAllLinkPermission();

  /**
   * Opens the named library, allowing system wide access for other <i>users</i>.
   *
   * @param libpath the LibPath for the library to open
   * @param debug set to true to enable debugging
   * @return the library handle, maybe 0 if not found.
   */
  public long openLibraryGlobal(LibPath libpath, boolean debug);

  /**
   * Opens the named library, restricting access to this process.
   *
   * @param libpath the LibPath for the library to open
   * @param debug set to true to enable debugging
   * @return the library handle, maybe 0 if not found.
   */
  public long openLibraryLocal(LibPath libpath, boolean debug);

  /**
   * @param libraryHandle a library handle previously retrieved via {@link #openLibraryLocal(String, boolean)} or {@link #openLibraryGlobal(String, boolean)}.
   * @param symbolName optional symbol name for an OS which requires the symbol's address to retrieve the path of the containing library
   * @return the library pathname if found and supported by OS or {@code null}.
   * @throws IllegalArgumentException in case case <code>libraryHandle</code> is unknown.
   */
  public String lookupLibraryPathname(long libraryHandle, String symbolName) throws IllegalArgumentException;

  /**
   * @param symbolName global symbol name to lookup up system wide.
   * @return the library handle, maybe 0 if not found.
   */
  public long lookupSymbolGlobal(String symbolName);

  /**
   * Security checks are implicit by previous call of
   * {@link #openLibraryLocal(String, boolean)} or {@link #openLibraryGlobal(String, boolean)}
   * retrieving the <code>librarHandle</code>.
   *
   * @param libraryHandle a library handle previously retrieved via {@link #openLibraryLocal(String, boolean)} or {@link #openLibraryGlobal(String, boolean)}.
   * @param symbolName global symbol name to lookup up system wide.
   * @return the library handle, maybe 0 if not found.
   * @throws IllegalArgumentException in case case <code>libraryHandle</code> is unknown.
   */
  public long lookupSymbol(long libraryHandle, String symbolName) throws IllegalArgumentException;

  /**
   * Security checks are implicit by previous call of
   * {@link #openLibraryLocal(String, boolean)} or {@link #openLibraryGlobal(String, boolean)}
   * retrieving the <code>librarHandle</code>.
   *
   * @param libraryHandle a library handle previously retrieved via {@link #openLibraryLocal(String, boolean)} or {@link #openLibraryGlobal(String, boolean)}.
   * @param debug set to true to enable debugging
   * @throws IllegalArgumentException in case case <code>libraryHandle</code> is unknown.
   */
  public void closeLibrary(long libraryHandle, boolean debug) throws IllegalArgumentException;

  /**
   * Returns a string containing the last error.
   * Maybe called for debugging purposed if any method fails.
   * @return error string, maybe null. A null or non-null value has no semantics.
   */
  public String getLastError();
}
