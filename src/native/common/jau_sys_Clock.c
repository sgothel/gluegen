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

#include <assert.h>

#include <gluegen_stdint.h>

#include "com_jogamp_common_os_Clock.h"

// #include <sys/time.h>
#include <time.h>

static const int64_t NanoPerMilli = 1000000L;
static const int64_t MilliPerOne = 1000L;

JNIEXPORT void JNICALL
Java_com_jogamp_common_os_Clock_getMonotonicTimeImpl(JNIEnv *env, jclass clazz, jlongArray jval) {
    (void)clazz;
    {
        // Avoid GetPrimitiveArrayCritical(), which occasionally hangs on system call ::clock_gettime()
        struct timespec t = { .tv_sec = 0, .tv_nsec = 0 };
        clock_gettime(CLOCK_MONOTONIC, &t);
        const jlong val[] = { (jlong)t.tv_sec, (jlong)t.tv_nsec };
        (*env)->SetLongArrayRegion(env, jval, 0, 2, val);
    }
}

JNIEXPORT void JNICALL
Java_com_jogamp_common_os_Clock_getWallClockTimeImpl(JNIEnv *env, jclass clazz, jlongArray jval) {
    (void)clazz;
    {
        // Avoid GetPrimitiveArrayCritical(), which occasionally hangs on system call ::clock_gettime()
        struct timespec t = { .tv_sec = 0, .tv_nsec = 0 };
        clock_gettime(CLOCK_REALTIME, &t);
        const jlong val[] = { (jlong)t.tv_sec, (jlong)t.tv_nsec };
        (*env)->SetLongArrayRegion(env, jval, 0, 2, val);
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
    clock_gettime(CLOCK_MONOTONIC, &t);
    int64_t res = (int64_t)( t.tv_sec ) * MilliPerOne +
                  (int64_t)( t.tv_nsec ) / NanoPerMilli;
    return (jlong)res;
}

JNIEXPORT jlong JNICALL
Java_com_jogamp_common_os_Clock_wallClockSeconds(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;

    struct timespec t = { .tv_sec = 0, .tv_nsec = 0 };
    clock_gettime(CLOCK_REALTIME, &t);
    return (jlong)( (int64_t)( t.tv_sec ) );
}

