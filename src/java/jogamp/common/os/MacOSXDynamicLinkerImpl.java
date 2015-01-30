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

/**
 * Mac OS X specialization of {@link UnixDynamicLinkerImpl}
 * utilizing OS X 's non POSIX flags and mode values.
 */
public final class MacOSXDynamicLinkerImpl extends UnixDynamicLinkerImpl {

  private static final long RTLD_DEFAULT = -2L;
  //      static final long RTLD_NEXT    = -1L;

  private static final int RTLD_LAZY     = 0x00001;
  //      static final int RTLD_NOW      = 0x00002;
  private static final int RTLD_LOCAL    = 0x00004;
  private static final int RTLD_GLOBAL   = 0x00008;

  @Override
  protected final long openLibraryLocalImpl(final String pathname) throws SecurityException {
    return dlopen(pathname, RTLD_LAZY | RTLD_LOCAL);
  }

  @Override
  protected final long openLibraryGlobalImpl(final String pathname) throws SecurityException {
    return dlopen(pathname, RTLD_LAZY | RTLD_GLOBAL);
  }

  @Override
  protected final long lookupSymbolGlobalImpl(final String symbolName) throws SecurityException {
    return dlsym(RTLD_DEFAULT, symbolName);
  }

}
