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

import com.jogamp.common.util.SecurityUtil;

/**
 * Bionic specialization of {@link UnixDynamicLinkerImpl}
 * utilizing Bionic's non POSIX flags and mode values.
 * <p>
 * Bionic is used on Android.
 * </p>
 */
public final class BionicDynamicLinkerImpl extends UnixDynamicLinkerImpl {
  private static final long RTLD_DEFAULT = 0xffffffffL;
  //      static final long RTLD_NEXT    = 0xfffffffeL;
  
  private static final int RTLD_LAZY     = 0x00001;
  //      static final int RTLD_NOW      = 0x00000;
  private static final int RTLD_LOCAL    = 0x00000;
  private static final int RTLD_GLOBAL   = 0x00002;

  @Override
  public final long openLibraryLocal(String pathname, boolean debug) throws SecurityException {
    // Note we use RTLD_GLOBAL visibility to _NOT_ allow this functionality to
    // be used to pre-resolve dependent libraries of JNI code without
    // requiring that all references to symbols in those libraries be
    // looked up dynamically via the ProcAddressTable mechanism; in
    // other words, one can actually link against the library instead of
    // having to dlsym all entry points. System.loadLibrary() uses
    // RTLD_LOCAL visibility so can't be used for this purpose.
    SecurityUtil.checkLinkPermission(pathname);
    final long handle = dlopen(pathname, RTLD_LAZY | RTLD_LOCAL);
    if( 0 != handle ) {
        incrLibRefCount(handle, pathname);
    } else if ( DEBUG || debug ) {
        System.err.println("dlopen \""+pathname+"\" local failed, error: "+dlerror());
    }
    return handle;
  }

  @Override
  public final long openLibraryGlobal(String pathname, boolean debug) throws SecurityException {
    // Note we use RTLD_GLOBAL visibility to allow this functionality to
    // be used to pre-resolve dependent libraries of JNI code without
    // requiring that all references to symbols in those libraries be
    // looked up dynamically via the ProcAddressTable mechanism; in
    // other words, one can actually link against the library instead of
    // having to dlsym all entry points. System.loadLibrary() uses
    // RTLD_LOCAL visibility so can't be used for this purpose.
    SecurityUtil.checkLinkPermission(pathname);
    final long handle = dlopen(pathname, RTLD_LAZY | RTLD_GLOBAL);
    if( 0 != handle ) {
        incrLibRefCount(handle, pathname);
    } else if ( DEBUG || debug ) {
        System.err.println("dlopen \""+pathname+"\" global failed, error: "+dlerror());
    }
    return handle;
  }
  
  @Override
  public final long lookupSymbolGlobal(String symbolName) throws SecurityException {
    SecurityUtil.checkAllLinkPermission();
    final long addr = dlsym(RTLD_DEFAULT, symbolName);
    if(DEBUG_LOOKUP) {
        System.err.println("DynamicLinkerImpl.lookupSymbolGlobal("+symbolName+") -> 0x"+Long.toHexString(addr));
    }
    return addr;    
  }
  
}
