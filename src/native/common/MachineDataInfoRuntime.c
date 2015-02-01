
#include <jni.h>

#include <assert.h>

#include "jogamp_common_os_MachineDataInfoRuntime.h"

#if defined(_WIN32)
    #include <windows.h>
#else /* assume POSIX sysconf() availability */
    #include <unistd.h>
#endif

#include <gluegen_stdint.h>

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getPointerSizeInBytesImpl(JNIEnv *env, jclass _unused) {
    return sizeof(void *);
}

JNIEXPORT jlong JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getPageSizeInBytesImpl(JNIEnv *env, jclass _unused) {
#if defined(_WIN32)
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    return (jlong) si.dwPageSize;
#else
    return (jlong) sysconf(_SC_PAGESIZE);
#endif
}

typedef struct { 
    int8_t      c1;
    int8_t      v;
} struct_alignment_int8;

typedef struct { 
    int8_t      c1;
    int16_t     v;
} struct_alignment_int16;

typedef struct { 
    int8_t      c1;
    int32_t      v;
} struct_alignment_int32;

typedef struct { 
    int8_t      c1;
    int64_t      v;
} struct_alignment_int64;

typedef struct { 
    int8_t      c1;
    int         v;
} struct_alignment_int;

typedef struct { 
    int8_t      c1;
    long        v;
} struct_alignment_long;

typedef struct { 
    int8_t      c1;
    void *     v;
} struct_alignment_pointer;

typedef struct { 
    int8_t      c1;
    float     v;
} struct_alignment_float;

typedef struct { 
    int8_t      c1;
    double     v;
} struct_alignment_double;

typedef struct { 
    int8_t      c1;
    long double     v;
} struct_alignment_ldouble;

// size_t padding(size_t totalsize, size_t typesize)   { return totalsize - typesize - sizeof(char); }
// static size_t alignment(size_t totalsize, size_t typesize) { return totalsize - typesize; }
#define ALIGNMENT(a, b) ( (a) - (b) )

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentInt8Impl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_int8 ), sizeof(int8_t));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentInt16Impl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_int16 ), sizeof(int16_t));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentInt32Impl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_int32 ), sizeof(int32_t));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentInt64Impl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_int64 ), sizeof(int64_t));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentIntImpl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_int ), sizeof(int));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentLongImpl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_long ), sizeof(long));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentPointerImpl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_pointer ), sizeof(void *));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentFloatImpl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_float ), sizeof(float));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentDoubleImpl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_double ), sizeof(double));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getAlignmentLongDoubleImpl(JNIEnv *env, jclass _unused) {
    return ALIGNMENT(sizeof( struct_alignment_ldouble ), sizeof(long double));
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getSizeOfIntImpl(JNIEnv *env, jclass _unused) {
    return sizeof(int);
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getSizeOfLongImpl(JNIEnv *env, jclass _unused) {
    return sizeof(long);
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getSizeOfFloatImpl(JNIEnv *env, jclass _unused) {
    return sizeof(float);
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getSizeOfDoubleImpl(JNIEnv *env, jclass _unused) {
    return sizeof(double);
}

JNIEXPORT jint JNICALL 
Java_jogamp_common_os_MachineDataInfoRuntime_getSizeOfLongDoubleImpl(JNIEnv *env, jclass _unused) {
    return sizeof(long double);
}

