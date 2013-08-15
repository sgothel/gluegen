
#include <jni.h>

#include <assert.h>

#include <gluegen_stdint.h>

#include "com_jogamp_common_os_Platform.h"

#include <sys/time.h>

JNIEXPORT jlong JNICALL 
Java_com_jogamp_common_os_Platform_currentTimeMillis(JNIEnv *env, jclass _unused) {
    struct timeval tv;
    gettimeofday(&tv,NULL);
    return (int64_t)tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

JNIEXPORT jlong JNICALL 
Java_com_jogamp_common_os_Platform_currentTimeMicros(JNIEnv *env, jclass _unused) {
    struct timeval tv;
    gettimeofday(&tv,NULL);
    return (int64_t)tv.tv_sec * 1000000 + tv.tv_usec;
}

