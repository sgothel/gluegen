/*
 * Author: Sven Gothel <sgothel@jausoft.com>
 * Copyright (c) 2020-2023 Gothel Software e.K.
 * Copyright (c) 2020-2023 JogAmp Community.
 * Copyright (c) 2020 ZAFENA AB
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <assert.h>

#include <gluegen_stdint.h>

#include "com_jogamp_common_os_Clock.h"

#include <time.h>

#if defined(_WIN32)
    #include <windows.h>
    #ifndef IN_WINPTHREAD
        #define IN_WINPTHREAD 1
    #endif
    #include "pthread.h"
    #include "pthread_time.h"
#endif

static const int64_t NanoPerMilli =    1000000L;
static const int64_t MilliPerOne  =       1000L;
static const int64_t NanoPerSec   = 1000000000L;

static struct timespec startup_t = { .tv_sec = 0, .tv_nsec = 0 };

static void throwNewRuntimeException(JNIEnv *env, const char* msg, ...) {
    char buffer[512];
    va_list ap;

    if( NULL != msg ) {
        va_start(ap, msg);
        vsnprintf(buffer, sizeof(buffer), msg, ap);
        va_end(ap);

        fprintf(stderr, "RuntimeException: %s\n", buffer);
        if(NULL != env) {
            (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"), buffer);
        }
    }
}

static jboolean throwNewRuntimeExceptionOnException(JNIEnv *env) {
    if( (*env)->ExceptionCheck(env) ) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        throwNewRuntimeException(env, "An exception occured from JNI as shown.");
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}


JNIEXPORT jboolean JNICALL
Java_com_jogamp_common_os_Clock_getMonotonicTimeImpl(JNIEnv *env, jclass clazz, jlongArray jval) {
    (void)clazz;

    // Avoid GetPrimitiveArrayCritical(), which occasionally hangs on system call ::clock_gettime()
    struct timespec t = { .tv_sec = 0, .tv_nsec = 0 };
    int res = clock_gettime(CLOCK_MONOTONIC, &t);
    const jlong val[] = { (jlong)t.tv_sec, (jlong)t.tv_nsec };
    (*env)->SetLongArrayRegion(env, jval, 0, 2, val);
    return 0 == res ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_jogamp_common_os_Clock_getWallClockTimeImpl(JNIEnv *env, jclass clazz, jlongArray jval) {
    (void)clazz;

    // Avoid GetPrimitiveArrayCritical(), which occasionally hangs on system call ::clock_gettime()
    struct timespec t = { .tv_sec = 0, .tv_nsec = 0 };
    int res = clock_gettime(CLOCK_REALTIME, &t);
    const jlong val[] = { (jlong)t.tv_sec, (jlong)t.tv_nsec };
    (*env)->SetLongArrayRegion(env, jval, 0, 2, val);
    return 0 == res ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_jogamp_common_os_Clock_getMonotonicStartupTimeImpl(JNIEnv *env, jclass clazz, jlongArray jval) {
    (void)clazz;

    // Avoid GetPrimitiveArrayCritical(), which occasionally hangs on system call ::clock_gettime()
    int res = clock_gettime(CLOCK_MONOTONIC, &startup_t);
    const jlong val[] = { (jlong)startup_t.tv_sec, (jlong)startup_t.tv_nsec };
    (*env)->SetLongArrayRegion(env, jval, 0, 2, val);
    if( throwNewRuntimeExceptionOnException(env) ) {
        return JNI_FALSE;
    }
    return 0 == res ? JNI_TRUE : JNI_FALSE;
}

/**
 * See <http://man7.org/linux/man-pages/man2/clock_gettime.2.html>
 * <p>
 * Regarding avoiding kernel via VDSO,
 * see <http://man7.org/linux/man-pages/man7/vdso.7.html>,
 * clock_gettime seems to be well supported at least on kernel >= 4.4.
 * Only bfin and sh are missing, while ia64 seems to be complicated.
 */
JNIEXPORT jlong JNICALL
Java_com_jogamp_common_os_Clock_currentNanos(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;

    struct timespec t = { .tv_sec = 0, .tv_nsec = 0 };
    if( 0 == clock_gettime(CLOCK_MONOTONIC, &t) ) {
        struct timespec d = { .tv_sec = t.tv_sec - startup_t.tv_sec,
                              .tv_nsec = t.tv_nsec - startup_t.tv_nsec };
        if ( 0 > d.tv_nsec ) {
            d.tv_nsec += NanoPerSec;
            d.tv_sec  -= 1;
        }
        return (jlong) ( (int64_t)d.tv_sec * NanoPerSec + (int64_t)d.tv_nsec );
    } else {
        return 0;
    }
}

/**
 * See <http://man7.org/linux/man-pages/man2/clock_gettime.2.html>
 * <p>
 * Regarding avoiding kernel via VDSO,
 * see <http://man7.org/linux/man-pages/man7/vdso.7.html>,
 * clock_gettime seems to be well supported at least on kernel >= 4.4.
 * Only bfin and sh are missing, while ia64 seems to be complicated.
 */
JNIEXPORT jlong JNICALL
Java_com_jogamp_common_os_Clock_currentTimeMillis(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;

    struct timespec t = { .tv_sec = 0, .tv_nsec = 0 };
    if( 0 == clock_gettime(CLOCK_MONOTONIC, &t) ) {
        int64_t res = (int64_t)t.tv_sec  * MilliPerOne +
                      (int64_t)t.tv_nsec / NanoPerMilli;
        return (jlong)res;
    } else {
        return 0;
    }
}

JNIEXPORT jlong JNICALL
Java_com_jogamp_common_os_Clock_wallClockSeconds(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;

    struct timespec t = { .tv_sec = 0, .tv_nsec = 0 };
    if( 0 == clock_gettime(CLOCK_REALTIME, &t) ) {
        return (jlong)( (int64_t)( t.tv_sec ) );
    } else {
        return 0;
    }
}

#if defined(_WIN32)

#define POW10_7                 10000000
#define POW10_9                 1000000000

/* Number of 100ns-seconds between the beginning of the Windows epoch
 * (Jan. 1, 1601) and the Unix epoch (Jan. 1, 1970)
 */
#define DELTA_EPOCH_IN_100NS    INT64_C(116444736000000000)

static WINPTHREADS_INLINE int lc_set_errno(int result)
{
    if (result != 0) {
        errno = result;
        return -1;
    }
    return 0;
}

/**
 * Source: https://github.com/Alexpux/mingw-w64/blob/master/mingw-w64-libraries/winpthreads/src/clock.c
 * Public Domain within mingw-w64, included here to simplify linkage.
 *
 * Get the time of the specified clock clock_id and stores it in the struct
 * timespec pointed to by tp.
 * @param  clock_id The clock_id argument is the identifier of the particular
 *         clock on which to act. The following clocks are supported:
 * <pre>
 *     CLOCK_REALTIME  System-wide real-time clock. Setting this clock
 *                 requires appropriate privileges.
 *     CLOCK_MONOTONIC Clock that cannot be set and represents monotonic
 *                 time since some unspecified starting point.
 *     CLOCK_PROCESS_CPUTIME_ID High-resolution per-process timer from the CPU.
 *     CLOCK_THREAD_CPUTIME_ID  Thread-specific CPU-time clock.
 * </pre>
 * @param  tp The pointer to a timespec structure to receive the time.
 * @return If the function succeeds, the return value is 0.
 *         If the function fails, the return value is -1,
 *         with errno set to indicate the error.
 */
int clock_gettime(clockid_t clock_id, struct timespec *tp)
{
    unsigned __int64 t;
    LARGE_INTEGER pf, pc;
    union {
        unsigned __int64 u64;
        FILETIME ft;
    }  ct, et, kt, ut;

    switch(clock_id) {
    case CLOCK_REALTIME:
        {
            GetSystemTimeAsFileTime(&ct.ft);
            t = ct.u64 - DELTA_EPOCH_IN_100NS;
            tp->tv_sec = t / POW10_7;
            tp->tv_nsec = ((int) (t % POW10_7)) * 100;

            return 0;
        }

    case CLOCK_MONOTONIC:
        {
            if (QueryPerformanceFrequency(&pf) == 0)
                return lc_set_errno(EINVAL);

            if (QueryPerformanceCounter(&pc) == 0)
                return lc_set_errno(EINVAL);

            tp->tv_sec = pc.QuadPart / pf.QuadPart;
            tp->tv_nsec = (int) (((pc.QuadPart % pf.QuadPart) * POW10_9 + (pf.QuadPart >> 1)) / pf.QuadPart);
            if (tp->tv_nsec >= POW10_9) {
                tp->tv_sec ++;
                tp->tv_nsec -= POW10_9;
            }

            return 0;
        }

    case CLOCK_PROCESS_CPUTIME_ID:
        {
        if(0 == GetProcessTimes(GetCurrentProcess(), &ct.ft, &et.ft, &kt.ft, &ut.ft))
            return lc_set_errno(EINVAL);
        t = kt.u64 + ut.u64;
        tp->tv_sec = t / POW10_7;
        tp->tv_nsec = ((int) (t % POW10_7)) * 100;

        return 0;
        }

    case CLOCK_THREAD_CPUTIME_ID:
        {
            if(0 == GetThreadTimes(GetCurrentThread(), &ct.ft, &et.ft, &kt.ft, &ut.ft))
                return lc_set_errno(EINVAL);
            t = kt.u64 + ut.u64;
            tp->tv_sec = t / POW10_7;
            tp->tv_nsec = ((int) (t % POW10_7)) * 100;

            return 0;
        }

    default:
        break;
    }

    return lc_set_errno(EINVAL);
}

#endif

