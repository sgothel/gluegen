public long openLibrary(String pathname) {
  // Note we use RTLD_GLOBAL visibility to allow this functionality to
  // be used to pre-resolve dependent libraries of JNI code without
  // requiring that all references to symbols in those libraries be
  // looked up dynamically via the ProcAddressTable mechanism; in
  // other words, one can actually link against the library instead of
  // having to dlsym all entry points. System.loadLibrary() uses
  // RTLD_LOCAL visibility so can't be used for this purpose.
  return dlopen(pathname, RTLD_GLOBAL);
}

public long lookupSymbol(long libraryHandle, String symbolName) {
  return dlsym(libraryHandle, symbolName);
}

public void closeLibrary(long libraryHandle) {
  dlclose(libraryHandle);
}
