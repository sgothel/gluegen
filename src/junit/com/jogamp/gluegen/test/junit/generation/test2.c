#include "test2.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int32_t CustomFuncA1(void* aptr) {
    (void)aptr;
    return 0xa001;
}
static int32_t CustomFuncA2(void* aptr) {
    (void)aptr;
    return 0xa002;
}

static int32_t CustomFuncB1(T2_UserData* pUserData) {
    return pUserData->balance;
}

static int32_t CustomFuncB2(T2_UserData* pUserData) {
    return -pUserData->balance;
}

int Initialize(T2_InitializeOptions* Options) {
    Options->ProductName = calloc(100, sizeof(char));
    Options->ProductVersion = calloc(100, sizeof(char));
    strncpy(Options->ProductName, "Product Name", 100);
    strncpy(Options->ProductVersion, "Product Version", 100);
    Options->ApiVersion = 1;

    Options->Reserved1 = NULL;
    Options->CustomFuncA1 = CustomFuncA1;
    Options->CustomFuncA2 = CustomFuncA2;
    Options->CustomFuncB1 = CustomFuncB1;
    Options->CustomFuncB2 = CustomFuncB2;
    
    Options->OverrideThreadAffinity = NULL;
}

int Release(T2_InitializeOptions* Options) {
    if( NULL != Options->ProductName ) {
        free( Options->ProductName );
        Options->ProductName = NULL;
    }
    if( NULL != Options->ProductVersion ) {
        free( Options->ProductVersion );
        Options->ProductVersion = NULL;
    }
    Options->CustomFuncA1 = NULL;
    Options->CustomFuncA2 = NULL;
    Options->CustomFuncB1 = NULL;
    Options->CustomFuncB2 = NULL;
}


