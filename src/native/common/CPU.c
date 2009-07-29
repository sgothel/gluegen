
#include <jni.h>

#include <assert.h>

JNIEXPORT jint JNICALL 
Java_com_sun_gluegen_runtime_CPU_getPointerSizeInBits(JNIEnv *env, jclass _unused) {
    return sizeof(void *) * 8;
}

