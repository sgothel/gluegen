
#include <jni.h>

#include <assert.h>

JNIEXPORT jlong JNICALL 
Java_com_jogamp_common_nio_PointerBuffer_getDirectBufferAddressImpl(JNIEnv *env, jclass _unused, jobject directBuffer) {
    return ( NULL != directBuffer ) ? ( jlong) (*env)->GetDirectBufferAddress(env, directBuffer) : 0L ; 
}

