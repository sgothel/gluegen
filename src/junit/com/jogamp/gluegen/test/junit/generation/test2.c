#include "test2.h"

#include <stdio.h>

int Initialize(const T2_InitializeOptions* Options) {
    fprintf(stderr, "T2 Initialize API 0x%X, product %s, version %s\n", 
        Options->ApiVersion,
        Options->ProductName,
        Options->ProductVersion);
    fprintf(stderr, "- MemFuncs: alloc %p, realloc %p, release %p\n",
        Options->AllocateMemoryFunction,
        Options->ReallocateMemoryFunction,
        Options->ReleaseMemoryFunction);
    return 0;
}

int Shutdown() {
    return 0;
}

