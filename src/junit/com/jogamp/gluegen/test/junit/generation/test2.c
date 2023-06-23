#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    int value;
} T2_UndefStruct;

#include "test2.h"

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
    strncpy((char*)Options->ProductName, "Product Name", 100); // yuck: nonsense-warning
    strncpy((char*)Options->ProductVersion, "Product Version", 100); // yuck: nonsense-warning
    Options->ApiVersion = 1;

    Options->Reserved1 = NULL;
    Options->CustomFuncA1 = CustomFuncA1;
    *( (T2_CustomFuncA*) &Options->CustomFuncA2 ) = CustomFuncA2; // yuck: real yuck
    Options->CustomFuncB1 = CustomFuncB1;
    Options->CustomFuncB2 = CustomFuncB2;
    Options->customFuncBVariants[0] = CustomFuncB1;
    Options->customFuncBVariants[1] = CustomFuncB2;
    
    Options->OverrideThreadAffinity = NULL;
}

int Release(T2_InitializeOptions* Options) {
    if( NULL != Options->ProductName ) {
        free( (void*) Options->ProductName ); // yuck: nonsense-warning
        Options->ProductName = NULL;
    }
    if( NULL != Options->ProductVersion ) {
        free( (void*) Options->ProductVersion ); // yuck: nonsense-warning
        Options->ProductVersion = NULL;
    }
    Options->CustomFuncA1 = NULL;
    // Options->CustomFuncA2 = NULL; // keep const
    Options->CustomFuncB1 = NULL;
    Options->CustomFuncB2 = NULL;
}

static int32_t StaticInt32Array[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
static T2_UndefStruct StaticUndefStructArray[] = { { 0 }, { 1 }, { 2 }, { 3 }, { 4 }, { 5 }, { 6 }, { 7 }, { 8 }, { 9 } };

T2_PointerStorage * createT2PointerStorage() {
    T2_PointerStorage * s = calloc(1, sizeof(T2_PointerStorage));
    for(int i=0; i<10; ++i) {
        s->int32PtrArray[i] = &StaticInt32Array[i];
    }
    s->undefStructPtr = &StaticUndefStructArray[0];
    for(int i=0; i<10; ++i) {
        s->undefStructPtrArray[i] = &StaticUndefStructArray[i];
    }

    for(int i=0; i<10; ++i) {
        s->customFuncAVariantsArray[i] = ( i %2 == 0 ) ? CustomFuncA1 : CustomFuncA2;
    }
    for(int i=0; i<10; ++i) {
        s->customFuncBVariantsArray[i] = ( i %2 == 0 ) ? CustomFuncB1 : CustomFuncB2;
    }
    return s;
}

void destroyT2PointerStorage(T2_PointerStorage * s) {
    assert(NULL!=s);
    memset(s, 0, sizeof(T2_PointerStorage));
    free(s);
}

