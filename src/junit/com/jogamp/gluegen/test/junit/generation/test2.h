
#include <gluegen_stdint.h>
#include <gluegen_stddef.h>

// Opaque long T2_UndefStruct*
// struct T2_UndefStruct;  // undefined struct forward declaration, implementation secret
typedef struct T2_UndefStruct* T2_UndefStructPtr;

typedef int32_t ( * T2_CustomFuncA)(T2_UndefStructPtr aptr);

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

//
// T2_CallbackFunc01
//
typedef void ( * T2_CallbackFunc01)(size_t id, const char* msg, void* usrParam);

void MessageCallback01(T2_CallbackFunc01 cbFunc, void* usrParam);
void InjectMessageCallback01(size_t id, const char* msg);

//
// ALBUFFERCALLBACKTYPESOFT (similar to OpenAL's AL_SOFT_callback_buffer)
//
// typedef void ( * ALBUFFERCALLBACKTYPESOFT)(int buffer, void *userptr, void *sampledata, int numbytes);
typedef void ( * ALBUFFERCALLBACKTYPESOFT)(int buffer /* key */, void *userptr, int sampledata, int numbytes);

void alBufferCallback0(int buffer /* key */, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, void *userptr);
// void alBufferCallback0Inject(int buffer, void *sampledata, int numbytes);
void alBufferCallback0Inject(int buffer, int sampledata, int numbytes);

void alBufferCallback1(void* user_ptr, int buffer_key /* key */, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback);
// void alBufferCallback1Inject(int buffer, void *sampledata, int numbytes);
void alBufferCallback1Inject(int buffer, int sampledata, int numbytes);

//
// ALEVENTPROCSOFT (similar to OpenAL's AL_SOFT_events)
//
typedef void ( * ALEVENTPROCSOFT)(int eventType, int object, int param, int length, const char *message, void *userParam /* key */);

void alEventCallback0(ALEVENTPROCSOFT callback, void *userParam /* key */);

void alEventCallback1(int object /* key */, ALEVENTPROCSOFT callback, void *userParam /* key */);

//
// T2_CallbackFunc11[ab]
//
typedef struct {
    int32_t ApiVersion;
    void* Data;
    long i;
    long r;
    size_t id;
} T2_Callback11UserType;

typedef void ( * T2_CallbackFunc11)(size_t id /* key */, const T2_Callback11UserType* usrParam, long val);

void MessageCallback11a(size_t id /* key */, T2_CallbackFunc11 cbFunc, const T2_Callback11UserType* usrParam);
void MessageCallback11aInject(size_t id, long val);

void MessageCallback11b(size_t id /* key */, T2_CallbackFunc11 cbFunc, void* Data);
void MessageCallback11bInject(size_t id, long val);

//
// T2_CallbackFunc12
//

typedef enum {
	LOG_Off = 0,
	LOG_Fatal = 100,
	LOG_Error = 200,
	LOG_Warning = 300,
	LOG_Info = 400,
	LOG_Verbose = 500,
	LOG_VeryVerbose = 600
} T2_Callback12LogLevel;

typedef struct {
    const char* Category;
    const char* Message;
    T2_Callback12LogLevel Level;
} T2_Callback12LogMessage;

typedef void ( * T2_CallbackFunc12a)(const T2_Callback12LogMessage* usrParam);

void SetLogCallBack12a(T2_CallbackFunc12a cbFunc);
void LogCallBack12aInject(const T2_Callback12LogMessage* message);

typedef void ( * T2_CallbackFunc12b)(int param0, const T2_Callback12LogMessage* usrParam1);

void SetLogCallBack12b(T2_CallbackFunc12b cbFunc);
void LogCallBack12bInject(const T2_Callback12LogMessage* message, int param0);

//
// T2_CallbackFunc13
//
typedef struct {
    int aNumber;
} T2_Callback13UserType;

typedef struct {
    int keyValue1;
} T2_Callback13UserKey1;

typedef void ( * T2_CallbackFunc13)(const char* msg1, const T2_Callback13UserType* info, const char* msg2, const T2_Callback13UserKey1* usrParamKey1 /* key */, size_t usrKey2 /* key */);

void MessageCallback13(const char* debugMsg, T2_CallbackFunc13 cbFunc, const T2_Callback13UserKey1* usrParamKey1 /* key */, size_t usrKey2 /* key */);
void InjectMessageCallback13(const char* msg1, const T2_Callback13UserType* info, const char* msg2, const T2_Callback13UserKey1* usrParamKey1 /* key */, size_t usrKey2 /* key */);

