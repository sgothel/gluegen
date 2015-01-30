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

package com.jogamp.common.os;

/** Low level secure dynamic linker access. */
public interface DynamicLinker {
  public static final boolean DEBUG = NativeLibrary.DEBUG;
  public static final boolean DEBUG_LOOKUP = NativeLibrary.DEBUG_LOOKUP;

  /**
   * @throws SecurityException if user is not granted global access
   */
  public void claimAllLinkPermission() throws SecurityException;

  /**
   * @throws SecurityException if user is not granted global access
   */
  public void releaseAllLinkPermission() throws SecurityException;

  /**
   * If a {@link SecurityManager} is installed, user needs link permissions
   * for the named library.
   * <p>
   * Opens the named library, allowing system wide access for other <i>users</i>.
   * </p>
   *
   * @param pathname the full pathname for the library to open
   * @param debug set to true to enable debugging
   * @return the library handle, maybe 0 if not found.
   * @throws SecurityException if user is not granted access for the named library.
   */
  public long openLibraryGlobal(String pathname, boolean debug) throws SecurityException;

  /**
   * If a {@link SecurityManager} is installed, user needs link permissions
   * for the named library.
   * <p>
   * Opens the named library, restricting access to this process.
   * </p>
   *
   * @param pathname the full pathname for the library to open
   * @param debug set to true to enable debugging
   * @return the library handle, maybe 0 if not found.
   * @throws SecurityException if user is not granted access for the named library.
   */
  public long openLibraryLocal(String pathname, boolean debug) throws SecurityException;

  /**
   * If a {@link SecurityManager} is installed, user needs link permissions
   * for <b>all</b> libraries, i.e. for <code>new RuntimePermission("loadLibrary.*");</code>!
   *
   * @param symbolName global symbol name to lookup up system wide.
   * @return the library handle, maybe 0 if not found.
   * @throws SecurityException if user is not granted access for all libraries.
   */
  public long lookupSymbolGlobal(String symbolName) throws SecurityException;

  /**
   * Security checks are implicit by previous call of
   * {@link #openLibraryLocal(String, boolean)} or {@link #openLibraryGlobal(String, boolean)}
   * retrieving the <code>librarHandle</code>.
   *
   * @param libraryHandle a library handle previously retrieved via {@link #openLibraryLocal(String, boolean)} or {@link #openLibraryGlobal(String, boolean)}.
   * @param symbolName global symbol name to lookup up system wide.
   * @return the library handle, maybe 0 if not found.
   * @throws IllegalArgumentException in case case <code>libraryHandle</code> is unknown.
   * @throws SecurityException if user is not granted access for the given library handle
   */
  public long lookupSymbol(long libraryHandle, String symbolName) throws SecurityException, IllegalArgumentException;

  /**
   * Security checks are implicit by previous call of
   * {@link #openLibraryLocal(String, boolean)} or {@link #openLibraryGlobal(String, boolean)}
   * retrieving the <code>librarHandle</code>.
   *
   * @param libraryHandle a library handle previously retrieved via {@link #openLibraryLocal(String, boolean)} or {@link #openLibraryGlobal(String, boolean)}.
   * @param debug set to true to enable debugging
   * @throws IllegalArgumentException in case case <code>libraryHandle</code> is unknown.
   * @throws SecurityException if user is not granted access for the given library handle
   */
  public void closeLibrary(long libraryHandle, boolean debug) throws SecurityException, IllegalArgumentException;

  /**
   * Returns a string containing the last error.
   * Maybe called for debuging purposed if any method fails.
   * @return error string, maybe null. A null or non-null value has no semantics.
   */
  public String getLastError();
}
