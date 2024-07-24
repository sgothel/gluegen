
#include <jni.h>

#include <assert.h>
#include <string.h>

#include <gluegen_stdint.h>

#include "com_jogamp_common_nio_Buffers.h"

JNIEXPORT jlong JNICALL 
Java_com_jogamp_common_nio_Buffers_getDirectBufferAddressImpl(JNIEnv *env, jclass _unused, jobject directBuffer) {
    return ( NULL != directBuffer ) ? (jlong) (intptr_t)  (*env)->GetDirectBufferAddress(env, directBuffer) : 0L ; 
}

JNIEXPORT jobject JNICALL 
Java_com_jogamp_common_nio_Buffers_getDirectByteBufferImpl(JNIEnv *env, jclass _unused, jlong japtr, jint jbyteCount) {
    return ( 0 != japtr && 0 < jbyteCount ) ? (*env)->NewDirectByteBuffer(env, (void *)(intptr_t)japtr, jbyteCount) : NULL;
}

JNIEXPORT jint JNICALL 
Java_com_jogamp_common_nio_Buffers_strnlenImpl(JNIEnv *env, jclass _unused, jlong jcstrptr, jint jmaxlen) {
    return ( 0 != jcstrptr && 0 < jmaxlen ) ? strnlen((const char *)(void *)(intptr_t)jcstrptr, jmaxlen) : 0;
}

JNIEXPORT jlong JNICALL 
Java_com_jogamp_common_nio_Buffers_memcpyImpl(JNIEnv *env, jclass _unused, jlong jdest, jlong jsrc, jlong jlen) {
    return ( 0 != jdest && 0 != jsrc && 0 < jlen ) ? (jlong) (intptr_t) memcpy((void *)(intptr_t)jdest, (void *)(intptr_t)jsrc, (size_t)jlen) : jdest;
}

