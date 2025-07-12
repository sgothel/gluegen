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

import com.jogamp.common.os.DynamicLinker;
import com.jogamp.common.os.NativeLibrary.LibPath;
import com.jogamp.common.util.LongObjectHashMap;
import com.jogamp.common.util.SecurityUtil;

/* pp */ abstract class DynamicLinkerImpl implements DynamicLinker {

  //
  // Package private scope of class w/ protected native code access
  // and sealed jogamp.common.* package definition
  // ensuring no abuse via subclassing.
  //

  private final Object secSync = new Object();
  private boolean allLinkPermissionGranted = false;

  /**
   * @throws SecurityException if user is not granted global access
   */
  @Override
  public final void claimAllLinkPermission() throws SecurityException {
      synchronized( secSync ) {
          allLinkPermissionGranted = true;
      }
  }

  /**
   * @throws SecurityException if user is not granted global access
   */
  @Override
  public final void releaseAllLinkPermission() throws SecurityException {
      synchronized( secSync ) {
          allLinkPermissionGranted = false;
      }
  }

  private final void checkLinkPermission(final String pathname) throws SecurityException {
      synchronized( secSync ) {
          if( !allLinkPermissionGranted ) {
              SecurityUtil.checkLinkPermission(pathname);
          }
      }
  }
  private final void checkLinkPermission(final long libraryHandle) throws SecurityException {
      synchronized( secSync ) {
          if( !allLinkPermissionGranted ) {
              final LibRef libRef = getLibRef( libraryHandle );
              if( null == libRef ) {
                  throw new IllegalArgumentException("Library handle 0x"+Long.toHexString(libraryHandle)+" unknown.");
              }
              SecurityUtil.checkLinkPermission(libRef.getLibPath().path);
          }
      }
  }

  private final void checkAllLinkPermission() throws SecurityException {
      synchronized( secSync ) {
          if( !allLinkPermissionGranted ) {
              SecurityUtil.checkAllLinkPermission();
          }
      }
  }

  @Override
  public long openLibraryGlobal(final LibPath libpath, final boolean debug) throws SecurityException {
    checkLinkPermission(libpath.path);
    final long handle = openLibraryGlobalImpl(libpath);
    if( 0 != handle ) {
        final LibRef libRef = incrLibRefCount(handle, libpath);
        if( DEBUG || debug ) {
            System.err.println("DynamicLinkerImpl.openLibraryGlobal \""+libpath+"\": 0x"+Long.toHexString(handle)+" -> "+libRef+")");
        }
    } else if ( DEBUG || debug ) {
        System.err.println("DynamicLinkerImpl.openLibraryGlobal \""+libpath+"\" failed, error: "+getLastError());
    }
    return handle;
  }
  protected abstract long openLibraryGlobalImpl(final LibPath libpath) throws SecurityException;

  @Override
  public final long openLibraryLocal(final LibPath libpath, final boolean debug) throws SecurityException {
    checkLinkPermission(libpath.path);
    final long handle = openLibraryLocalImpl(libpath);
    if( 0 != handle ) {
        final LibRef libRef = incrLibRefCount(handle, libpath);
        if( DEBUG || debug ) {
            System.err.println("DynamicLinkerImpl.openLibraryLocal \""+libpath+"\": 0x"+Long.toHexString(handle)+" -> "+libRef+")");
        }
    } else if ( DEBUG || debug ) {
        System.err.println("DynamicLinkerImpl.openLibraryLocal \""+libpath+"\" failed, error: "+getLastError());
    }
    return handle;
  }
  protected abstract long openLibraryLocalImpl(final LibPath libpath) throws SecurityException;

  @Override
  public final String lookupLibraryPathname(final long libraryHandle, final String symbolName) throws SecurityException {
    checkLinkPermission(libraryHandle);
    final String fname = lookupLibraryPathnameImpl(libraryHandle, symbolName);
    if(DEBUG_LOOKUP) {
        System.err.println("DynamicLinkerImpl.lookupLibraryPathname(0x"+Long.toHexString(libraryHandle)+", "+symbolName+") -> '"+fname+"'");
    }
    return fname;
  }
  protected abstract String lookupLibraryPathnameImpl(final long libraryHandle, String symbolName) throws SecurityException;

  @Override
  public final long lookupSymbolGlobal(final String symbolName) throws SecurityException {
    checkAllLinkPermission();
    final long addr = lookupSymbolGlobalImpl(symbolName);
    if(DEBUG_LOOKUP) {
        System.err.println("DynamicLinkerImpl.lookupSymbolGlobal("+symbolName+") -> 0x"+Long.toHexString(addr));
    }
    return addr;
  }
  protected abstract long lookupSymbolGlobalImpl(final String symbolName) throws SecurityException;

  @Override
  public final long lookupSymbol(final long libraryHandle, final String symbolName) throws SecurityException, IllegalArgumentException {
    checkLinkPermission(libraryHandle);
    final long addr = lookupSymbolLocalImpl(libraryHandle, symbolName);
    if(DEBUG_LOOKUP) {
        System.err.println("DynamicLinkerImpl.lookupSymbol(0x"+Long.toHexString(libraryHandle)+", "+symbolName+") -> 0x"+Long.toHexString(addr));
    }
    return addr;
  }
  protected abstract long lookupSymbolLocalImpl(final long libraryHandle, final String symbolName) throws SecurityException;

  @Override
  public final void closeLibrary(final long libraryHandle, final boolean debug) throws SecurityException, IllegalArgumentException {
    final LibRef libRef = decrLibRefCount( libraryHandle );
    if( null != libRef ) {
        checkLinkPermission(libRef.getLibPath().path);
    } // else null libRef is OK for global lookup
    if( DEBUG || debug ) {
        System.err.println("DynamicLinkerImpl.closeLibrary(0x"+Long.toHexString(libraryHandle)+" -> "+libRef+")");
    }
    if( 0 != libraryHandle ) {
        closeLibraryImpl(libraryHandle);
    }
  }
  protected abstract void closeLibraryImpl(final long libraryHandle) throws SecurityException;

  private static final LongObjectHashMap libHandle2Name = new LongObjectHashMap( 16 /* initialCapacity */ );

  static final class LibRef {
      LibRef(final LibPath libPath) {
          this.libPath = libPath;
          this.refCount = 1;
      }
      final int incrRefCount() { return ++refCount; }
      final int decrRefCount() { return --refCount; }
      final int getRefCount() { return refCount; }

      final LibPath getLibPath() { return libPath; }
      @Override
      public final String toString() { return "LibRef["+libPath+", refCount "+refCount+"]"; }

      private final LibPath libPath;
      private int refCount;
  }

  private final LibRef getLibRef(final long handle) {
      synchronized( libHandle2Name ) {
          return (LibRef) libHandle2Name.get(handle);
      }
  }

  private final LibRef incrLibRefCount(final long handle, final LibPath libPath) {
      synchronized( libHandle2Name ) {
          LibRef libRef = getLibRef(handle);
          if( null == libRef ) {
              libRef = new LibRef(libPath);
              libHandle2Name.put(handle, libRef);
          } else {
              libRef.incrRefCount();
          }
          if(DEBUG) {
              System.err.println("DynamicLinkerImpl.incrLibRefCount 0x"+Long.toHexString(handle)+ " -> "+libRef+", libs loaded "+libHandle2Name.size());
          }
          return libRef;
      }
  }

  private final LibRef decrLibRefCount(final long handle) {
      synchronized( libHandle2Name ) {
          final LibRef libRef = getLibRef(handle);
          if( null != libRef ) {
              if( 0 == libRef.decrRefCount() ) {
                  libHandle2Name.remove(handle);
              }
          }
          if(DEBUG) {
              System.err.println("DynamicLinkerImpl.decrLibRefCount 0x"+Long.toHexString(handle)+ " -> "+libRef+", libs loaded "+libHandle2Name.size());
          }
          return libRef;
      }
  }
}
