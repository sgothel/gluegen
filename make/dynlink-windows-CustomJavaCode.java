public long openLibrary(String libraryName) {
  return LoadLibraryW(libraryName);
}

public long lookupSymbol(long libraryHandle, String symbolName) {
  return GetProcAddressA(libraryHandle, symbolName);
}

public void closeLibrary(long libraryHandle) {
  FreeLibrary(libraryHandle);
}
