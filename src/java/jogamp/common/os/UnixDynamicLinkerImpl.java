package jogamp.common.os;

import com.jogamp.common.os.DynamicLinker;
import com.jogamp.common.util.SecurityUtil;


public class UnixDynamicLinkerImpl implements DynamicLinker {

  private static final long RTLD_DEFAULT = 0;
  //      static final long RTLD_NEXT    = -1L;
  
  private static final int RTLD_LAZY     = 0x00001;
  //      static final int RTLD_NOW      = 0x00002;
  private static final int RTLD_LOCAL    = 0x00000;
  private static final int RTLD_GLOBAL   = 0x00100;

  /** Interface to C language function: <br> <code> int dlclose(void * ); </code>    */
  /* pp */ static native int dlclose(long arg0);

  /** Interface to C language function: <br> <code> char *  dlerror(void); </code>    */
  /* pp */ static native java.lang.String dlerror();

  /** Interface to C language function: <br> <code> void *  dlopen(const char * , int); </code>    */
  /* pp */ static native long dlopen(java.lang.String arg0, int arg1);

  /** Interface to C language function: <br> <code> void *  dlsym(void * , const char * ); </code>    */
  /* pp */ static native long dlsym(long arg0, java.lang.String arg1);


  // --- Begin CustomJavaCode .cfg declarations
  public long openLibraryLocal(String pathname, boolean debug) throws SecurityException {
    // Note we use RTLD_GLOBAL visibility to _NOT_ allow this functionality to
    // be used to pre-resolve dependent libraries of JNI code without
    // requiring that all references to symbols in those libraries be
    // looked up dynamically via the ProcAddressTable mechanism; in
    // other words, one can actually link against the library instead of
    // having to dlsym all entry points. System.loadLibrary() uses
    // RTLD_LOCAL visibility so can't be used for this purpose.
    SecurityUtil.checkLinkPermission(pathname);
    return dlopen(pathname, RTLD_LAZY | RTLD_LOCAL);
  }

  public long openLibraryGlobal(String pathname, boolean debug) throws SecurityException {
    // Note we use RTLD_GLOBAL visibility to allow this functionality to
    // be used to pre-resolve dependent libraries of JNI code without
    // requiring that all references to symbols in those libraries be
    // looked up dynamically via the ProcAddressTable mechanism; in
    // other words, one can actually link against the library instead of
    // having to dlsym all entry points. System.loadLibrary() uses
    // RTLD_LOCAL visibility so can't be used for this purpose.
    SecurityUtil.checkLinkPermission(pathname);
    return dlopen(pathname, RTLD_LAZY | RTLD_GLOBAL);
  }
  
  public long lookupSymbol(long libraryHandle, String symbolName) {
    final long addr = dlsym(libraryHandle, symbolName);
    if(DEBUG_LOOKUP) {
        System.err.println("DynamicLinkerImpl.lookupSymbol(0x"+Long.toHexString(libraryHandle)+", "+symbolName+") -> 0x"+Long.toHexString(addr));
    }
    return addr;    
  }

  public long lookupSymbolGlobal(String symbolName) {
    final long addr = dlsym(RTLD_DEFAULT, symbolName);
    if(DEBUG_LOOKUP) {
        System.err.println("DynamicLinkerImpl.lookupSymbolGlobal("+symbolName+") -> 0x"+Long.toHexString(addr));
    }
    return addr;    
  }
  
  public void closeLibrary(long libraryHandle) {
    dlclose(libraryHandle);
  }

}
