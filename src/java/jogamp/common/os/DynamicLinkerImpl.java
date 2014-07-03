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
import com.jogamp.common.util.LongObjectHashMap;

/* pp */ abstract class DynamicLinkerImpl implements DynamicLinker {

  //
  // Package private scope of class w/ protected native code access
  // and sealed jogamp.common.* package definition
  // ensuring no abuse via subclassing.
  //

  private final LongObjectHashMap libHandle2Name = new LongObjectHashMap( 16 /* initialCapacity */ );

  protected static final class LibRef {
      public LibRef(final String name) {
          this.name = name;
          this.refCount = 1;
      }
      public final int incrRefCount() { return ++refCount; }
      public final int decrRefCount() { return --refCount; }
      public final int getRefCount() { return refCount; }

      public final String getName() { return name; }
      @Override
      public final String toString() { return "LibRef["+name+", refCount "+refCount+"]"; }

      private final String name;
      private int refCount;
  }

  protected final synchronized LibRef getLibRef(final long handle) {
      return (LibRef) libHandle2Name.get(handle);
  }

  protected final synchronized LibRef incrLibRefCount(final long handle, final String libName) {
      LibRef libRef = getLibRef(handle);
      if( null == libRef ) {
          libRef = new LibRef(libName);
          libHandle2Name.put(handle, libRef);
      } else {
          libRef.incrRefCount();
      }
      if(DEBUG) {
          System.err.println("DynamicLinkerImpl.incrLibRefCount 0x"+Long.toHexString(handle)+ " -> "+libRef+", libs loaded "+libHandle2Name.size());
      }
      return libRef;
  }

  protected final synchronized LibRef decrLibRefCount(final long handle) {
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
