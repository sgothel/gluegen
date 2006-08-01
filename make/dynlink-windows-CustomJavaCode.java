public long openLibrary(String libraryName) {
  return LoadLibraryA(libraryName);
}

public long lookupSymbol(long libraryHandle, String symbolName) {
  return GetProcAddress(libraryHandle, symbolName);
}

public void closeLibrary(long libraryHandle) {
  FreeLibrary(libraryHandle);
}
