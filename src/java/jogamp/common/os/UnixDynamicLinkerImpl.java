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

/* pp */ abstract class UnixDynamicLinkerImpl extends DynamicLinkerImpl {

  //
  // Package private scope of class w/ protected native code access
  // and sealed jogamp.common.* package definition
  // ensuring no abuse via subclassing.
  //

  /** Interface to C language function: <br> <code> int dlclose(void * ); </code>    */
  protected static native int dlclose(long arg0);

  /** Interface to C language function: <br> <code> char *  dlerror(void); </code>    */
  protected static native java.lang.String dlerror();

  /** Interface to C language function: <br> <code> void *  dlopen(const char * , int); </code>    */
  protected static native long dlopen(java.lang.String arg0, int arg1);

  /** Interface to C language function: <br> <code> void *  dlsym(void * , const char * ); </code>    */
  protected static native long dlsym(long arg0, java.lang.String arg1);

  /** Interface to C language function: <br> <code> int dladdr(void * , Dl_info *); </code>, returning the <code>Dl_info.dli_fname</code> */
  protected static native java.lang.String dladdr_fname(long arg0);

  @Override
  protected final String lookupLibraryPathnameImpl(final long libraryHandle, final String symbolName) throws SecurityException {
      if( 0 != libraryHandle && null != symbolName && symbolName.length() > 0 ) {
          final long addr = dlsym(libraryHandle, symbolName);
          if( 0 != addr ) {
              return dladdr_fname(addr);
          }
      }
      return null;
  }

  @Override
  protected final long lookupSymbolLocalImpl(final long libraryHandle, final String symbolName) throws SecurityException {
      return 0 != libraryHandle ? dlsym(libraryHandle, symbolName) : 0;
  }

  @Override
  protected final void closeLibraryImpl(final long libraryHandle) throws SecurityException {
      if( 0 != libraryHandle ) {
          dlclose(libraryHandle);
      }
  }

  @Override
  public final String getLastError() {
      return dlerror();
  }
}
