
#include <gluegen_stdint.h>
#include <gluegen_stddef.h>

// Opaque long T2_UndefStruct*
// struct T2_UndefStruct;  // undefined struct forward declaration, implementation secret
typedef struct T2_UndefStruct* T2_UndefStructPtr;

typedef int32_t ( * T2_CustomFuncA)(void* aptr);

typedef struct {
    int32_t balance;
    const char* name;
} T2_UserData;

typedef int32_t ( * T2_CustomFuncB)(T2_UserData* pUserData);

typedef struct {
    int32_t* int32PtrArray[10];
    int32_t** int32PtrPtr;

    T2_UndefStructPtr undefStructPtr;
    T2_UndefStructPtr undefStructPtrArray[10];
    T2_UndefStructPtr* undefStructPtrPtr;
    const T2_UndefStructPtr* constUndefStructPtrPtr;

    T2_CustomFuncA customFuncAVariantsArray[10];
    T2_CustomFuncA* customFuncAVariantsArrayPtr;

    T2_CustomFuncB customFuncBVariantsArray[10];
    T2_CustomFuncB* customFuncBVariantsArrayPtr;
} T2_PointerStorage;

T2_PointerStorage * createT2PointerStorage();
void destroyT2PointerStorage(T2_PointerStorage * s);

typedef struct {
    int32_t ApiVersion;
    uint64_t NetworkWork;
} T2_ThreadAffinity;

typedef struct {
    const char* ProductName;
    const char* ProductVersion;
    
    int32_t ApiVersion;
    
    void* Reserved1;
    T2_CustomFuncA CustomFuncA1;
    const T2_CustomFuncA CustomFuncA2;
    T2_CustomFuncB CustomFuncB1;
    T2_CustomFuncB CustomFuncB2;
    T2_CustomFuncB customFuncBVariants[2];
    
    T2_ThreadAffinity* OverrideThreadAffinity;
} T2_InitializeOptions;

extern int Initialize(T2_InitializeOptions* Options);
extern int Release(T2_InitializeOptions* Options);

typedef int32_t ( * T2_CallbackFunc)(size_t id, size_t msg_len, const char* msg, void* userParam);

void AddMessageCallback(T2_CallbackFunc func, void* userParam);
void RemoveMessageCallback(T2_CallbackFunc func, void* userParam);
void InjectMessageCallback(size_t id, size_t msg_len, const char* msg);
