
#include <jni.h>

#include <assert.h>

#include "com_jogamp_common_os_Platform.h"

#if defined(_WIN32)
    #include <windows.h>
#else /* assume POSIX sysconf() availability */
    #include <unistd.h>
#endif

JNIEXPORT jint JNICALL 
Java_com_jogamp_common_os_Platform_getPointerSizeInBitsImpl(JNIEnv *env, jclass _unused) {
    return sizeof(void *) * 8;
}

JNIEXPORT jlong JNICALL 
Java_com_jogamp_common_os_Platform_getPageSizeImpl(JNIEnv *env, jclass _unused) {
#if defined(_WIN32)
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    return (jlong) si.dwPageSize;
#else
    return (jlong) sysconf(_SC_PAGESIZE);
#endif
}

