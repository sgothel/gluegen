
#include <gluegen_stdint.h>
#include <gluegen_stddef.h>

typedef struct {
    int32_t ApiVersion;
    uint64_t NetworkWork;
    uint64_t StorageIo;
    uint64_t WebSocketIo;
    uint64_t P2PIo;
    uint64_t HttpRequestIo;
    uint64_t RTCIo;
} T2_ThreadAffinity;

typedef struct {
    int32_t balance;
    const char* name;
} T2_UserData;

typedef int32_t ( * T2_CustomFuncA)(void* aptr);

typedef int32_t ( * T2_CustomFuncB)(T2_UserData* pUserData);

typedef struct {
    const char* ProductName;
    const char* ProductVersion;
    
    int32_t ApiVersion;
    
    void* Reserved1;
    T2_CustomFuncA CustomFuncA1;
    const T2_CustomFuncA CustomFuncA2;
    T2_CustomFuncB CustomFuncB1;
    T2_CustomFuncB CustomFuncB2;
    
    T2_ThreadAffinity* OverrideThreadAffinity;
} T2_InitializeOptions;

extern int Initialize(T2_InitializeOptions* Options);
extern int Release(T2_InitializeOptions* Options);
