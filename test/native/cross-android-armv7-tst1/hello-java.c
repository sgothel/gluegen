#include <stdio.h>
#include <math.h>

#include <jni.h>
#include <dlfcn.h>

typedef jint (*FPTR_JNI_GetDefaultJavaVMInitArgs)(void*);
typedef jint (*FPTR_JNI_CreateJavaVM)(JavaVM**, JNIEnv**, void*);
typedef jint (*FPTR_JNI_GetCreatedJavaVMs)(JavaVM**, jsize, jsize*);

static const char * FN_JVM_LIB_NAME = "libdvm.so";
static const char * FN_JNI_GetDefaultJavaVMInitArgs = "JNI_GetDefaultJavaVMInitArgs";
static const char * FN_JNI_CreateJavaVM = "JNI_CreateJavaVM";
static const char * FN_JNI_GetCreatedJavaVMs = "JNI_GetCreatedJavaVMs";

int main(int argc, char ** argv) {
    void * jvmLibHandle;
    FPTR_JNI_GetDefaultJavaVMInitArgs fptr_JNI_GetDefaultJavaVMInitArgs;
    FPTR_JNI_CreateJavaVM fptr_JNI_CreateJavaVM;
    FPTR_JNI_GetCreatedJavaVMs fptr_JNI_GetCreatedJavaVMs;
    jint jres;

    JavaVM *jvm;       /* denotes a Java VM */
    JNIEnv *env;       /* pointer to native method interface */

#if 0
    const int vm_options_count = 4; 
    JavaVMOption vm_options[vm_options_count];
    {
        int i=0;
        vm_options[i++].optionString = "-Djava.compiler=NONE";           /* disable JIT */
        vm_options[i++].optionString = "-Djava.class.path=c:\myclasses"; /* user classes */
        vm_options[i++].optionString = "-Djava.library.path=c:\mylibs";  /* set native library path */
        vm_options[i++].optionString = "-verbose:jni";                   /* print JNI-related messages */
    }
#else
    const int vm_options_count = 1; 
    JavaVMOption vm_options[vm_options_count];
    {
        int i=0;
        vm_options[i++].optionString = "-Djava.class.path=HelloJava.jar"; /* user classes (OK) */
        // vm_options[i++].optionString = "-cp HelloJava.jar"; /* user classes (NOT OK) */
        // vm_options[i++].optionString = "-cp"; /* user classes (OK) */
        // vm_options[i++].optionString = "HelloJava.jar"; /* user classes (OK) */
    }
#endif
    JavaVMInitArgs vm_args; /* VM initialization arguments */

    jvmLibHandle = dlopen(FN_JVM_LIB_NAME, RTLD_LAZY | RTLD_GLOBAL);
    if(NULL == jvmLibHandle) {
        fprintf(stderr, "Error: Could not open %s, abort\n", FN_JVM_LIB_NAME);
        return -1;
    }
    fptr_JNI_GetDefaultJavaVMInitArgs = (FPTR_JNI_GetDefaultJavaVMInitArgs) dlsym(jvmLibHandle, FN_JNI_GetDefaultJavaVMInitArgs);
    if(NULL == fptr_JNI_GetDefaultJavaVMInitArgs) {
        fprintf(stderr, "Error: Could not resolve %s, abort\n", FN_JNI_GetDefaultJavaVMInitArgs);
        return -1;
    }
    fptr_JNI_CreateJavaVM = (FPTR_JNI_CreateJavaVM) dlsym(jvmLibHandle, FN_JNI_CreateJavaVM);
    if(NULL == fptr_JNI_CreateJavaVM) {
        fprintf(stderr, "Error: Could not resolve %s, abort\n", FN_JNI_CreateJavaVM);
        return -1;
    }
    fptr_JNI_GetCreatedJavaVMs = (FPTR_JNI_GetCreatedJavaVMs) dlsym(jvmLibHandle, FN_JNI_GetCreatedJavaVMs);
    if(NULL == fptr_JNI_GetCreatedJavaVMs) {
        fprintf(stderr, "Error: Could not resolve %s, abort\n", FN_JNI_GetCreatedJavaVMs);
        return -1;
    }

    /* Get the default initialization arguments and set the class 
     * path */
    /**
    jres = fptr_JNI_GetDefaultJavaVMInitArgs(&vm_args);
    if(JNI_OK != jres) {
        fprintf(stderr, "Error: JNI_GetDefaultJavaVMInitArgs failed: %d, abort\n", jres);
        return -1;
    } */
    // vm_args.classpath = ...;

    vm_args.version = JNI_VERSION_1_2;
    vm_args.options = vm_options;
    vm_args.nOptions = vm_options_count;
    vm_args.ignoreUnrecognized = JNI_TRUE;

    /* load and initialize a Java VM, return a JNI interface 
     * pointer in env */
    jres = fptr_JNI_CreateJavaVM(&jvm, &env, &vm_args);
    if(JNI_OK != jres) {
        fprintf(stderr, "Error: JNI_CreateJavaVM failed: %d, abort\n", jres);
        return -1;
    }
    fprintf(stderr, "Info: VM created\n");

    /* invoke the HelloJava.test method using the JNI */
    jclass cls = (*env)->FindClass(env, "HelloJava");
    if(NULL == cls) {
        fprintf(stderr, "Error: Could not resolve Class HelloJava, abort\n");
        (*jvm)->DestroyJavaVM(jvm);
        return -1;
    }
    fprintf(stderr, "Info: Found Class HelloJava\n");

    jmethodID mid = (*env)->GetStaticMethodID(env, cls, "test", "(I)V");
    if(NULL == cls) {
        fprintf(stderr, "Error: Could not resolve Method \"void HelloJava.test(int)\", abort\n");
        (*jvm)->DestroyJavaVM(jvm);
        return -1;
    }
    fprintf(stderr, "Info: Found Method HelloJava.test(int)\n");

    (*env)->CallStaticVoidMethod(env, cls, mid, 100);
    fprintf(stderr, "Info: post invocation\n");

    /* We are done. */
    (*jvm)->DestroyJavaVM(jvm);
    fprintf(stderr, "Info: post VM\n");

    return 0;
}
