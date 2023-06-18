
#include <gluegen_stdint.h>
#include <gluegen_stddef.h>

typedef void* ( * T2_AllocateMemoryFunc)(size_t SizeInBytes, size_t Alignment);

typedef void* ( * T2_ReallocateMemoryFunc)(void* Pointer, size_t SizeInBytes, size_t Alignment);

typedef void ( * T2_ReleaseMemoryFunc)(void* Pointer);

typedef void ( * T2_CustomFunc)(void* Pointer);

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
    int32_t ApiVersion;
    T2_AllocateMemoryFunc AllocateMemoryFunction;
    T2_ReallocateMemoryFunc ReallocateMemoryFunction;
    T2_ReleaseMemoryFunc ReleaseMemoryFunction;
    
    const char* ProductName;
    
    const char* ProductVersion;
    
    void* Reserved1;
    void* Reserved2;
    T2_CustomFunc CustomFunc2;
    
    T2_ThreadAffinity* OverrideThreadAffinity;
} T2_InitializeOptions;

extern int Initialize(const T2_InitializeOptions* Options);
extern int Shutdown();
