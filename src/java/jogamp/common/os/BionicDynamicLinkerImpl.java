package jogamp.common.os;

import com.jogamp.common.util.SecurityUtil;

/**
 * Bionic specialization of {@link UnixDynamicLinkerImpl}
 * utilizing Bionic's non POSIX flags and mode values.
 * <p>
 * Bionic is used on Android.
 * </p>
 */
public class BionicDynamicLinkerImpl extends UnixDynamicLinkerImpl {
  private static final long RTLD_DEFAULT = 0xffffffffL;
  //      static final long RTLD_NEXT    = 0xfffffffeL;
  
  private static final int RTLD_LAZY     = 0x00001;
  //      static final int RTLD_NOW      = 0x00000;
  private static final int RTLD_LOCAL    = 0x00000;
  private static final int RTLD_GLOBAL   = 0x00002;

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
  
  public long lookupSymbolGlobal(String symbolName) {
    final long addr = dlsym(RTLD_DEFAULT, symbolName);
    if(DEBUG_LOOKUP) {
        System.err.println("DynamicLinkerImpl.lookupSymbolGlobal("+symbolName+") -> 0x"+Long.toHexString(addr));
    }
    return addr;    
  }
  
}
