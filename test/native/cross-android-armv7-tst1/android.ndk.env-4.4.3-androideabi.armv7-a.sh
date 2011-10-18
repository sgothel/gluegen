#! /bin/sh

# http://forum.xda-developers.com/showthread.php?t=625277
#

#
# Android NDK tool chain setup
# run cd ~/android/ndk; make APP-xxx -V1 to obtain toolchain values
#
# Old NDK <= 4
# ./build/prebuilt/linux-x86/arm-eabi-4.2.1/bin/
# ./build/prebuilt/linux-x86/arm-eabi-4.4.0/bin/
#
# New NDK >= 5
# ./toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/arm-linux-androideabi-gcc
# ./toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/arm-linux-androideabi/bin/gcc
#
# ./toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/lib/gcc/arm-linux-androideabi/4.4.3/thumb/libgcc.a
# ./toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/lib/gcc/arm-linux-androideabi/4.4.3/libgcc.a
# ./toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/lib/gcc/arm-linux-androideabi/4.4.3/armv7-a/thumb/libgcc.a
# ./toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/lib/gcc/arm-linux-androideabi/4.4.3/armv7-a/libgcc.a
# ./toolchains/x86-4.4.3/prebuilt/linux-x86/lib/gcc/i686-android-linux/4.4.3/libgcc.a




export NDK_ROOT=/opt-linux-x86/android-ndk

ANDROID_VERSION=9
GCC_VERSION=4.4.3
HOST_ARCH=linux-x86
TARGET_ARCH=arm-linux-androideabi
# mcpu: cortex-a8', `cortex-a9', `cortex-r4', `cortex-r4f', `cortex-m3', `cortex-m1', `xscale', `iwmmxt', `iwmmxt2', `ep9312'. 
TARGET_CPU_ARCH=
TARGET_CPU_NAME=armv7-a
TARGET_CPU_TUNE=armv7-a
# mfpu: `vfp', `vfpv3', `vfpv3-d16' and `neon'
TARGET_FPU_NAME=vfpv3
TARGET_FPU_ABI=softfp

TARGET_TOOL_PATH=${NDK_ROOT}/toolchains/${TARGET_ARCH}-${GCC_VERSION}/prebuilt/${HOST_ARCH}

#TARGET_OS_PATH=/usr/local/projects/android/gt-i9000xxjf3/system
TARGET_OS_PATH=${NDK_ROOT}/platforms/android-${ANDROID_VERSION}/arch-arm/usr

export NDK_XBIN_PATH=${TARGET_TOOL_PATH}/bin
export NDK_BIN_PATH=${TARGET_TOOL_PATH}/${TARGET_ARCH}/bin

export NDK_GCC=${NDK_XBIN_PATH}/${TARGET_ARCH}-gcc
export NDK_AR=${NDK_XBIN_PATH}/${TARGET_ARCH}-ar
export NDK_STRIP=${NDK_XBIN_PATH}/${TARGET_ARCH}-strip
export NDK_READELF=${NDK_XBIN_PATH}/${TARGET_ARCH}-readelf

export PATH=${NDK_XBIN_PATH}:$PATH

export NDK_INCLUDE="-I${TARGET_OS_PATH}/include"

# -mfloat-abi=${TARGET_FPU_ABI} -mfpu=${TARGET_FPU_NAME} \
# -D__ARM_ARCH_7__ -D__ARM_ARCH_7A__ \
# -ffunction-sections -funwind-tables -fstack-protector -fno-short-enums \
# -O3 -fomit-frame-pointer -fno-strict-aliasing -finline-limit=64 \
# -mcpu=${TARGET_CPU_NAME} -mtune=${TARGET_CPU_TUNE} \

export NDK_CFLAGS="\
-march=${TARGET_CPU_NAME} \
-mfloat-abi=${TARGET_FPU_ABI} -mfpu=${TARGET_FPU_NAME} \
-fpic \
-DANDROID \
"

#
# THIS WORKS
#

export NDK_LDFLAGS="\
-Wl,--demangle \
-nostdlib -Bdynamic -Wl,-dynamic-linker,/system/bin/linker -Wl,--gc-sections -Wl,-z,nocopyreloc \
${TARGET_OS_PATH}/lib/libc.so \
${TARGET_OS_PATH}/lib/libstdc++.so \
${TARGET_OS_PATH}/lib/libm.so \
${TARGET_OS_PATH}/lib/crtbegin_dynamic.o \
-Wl,--no-undefined -Wl,-rpath-link=${TARGET_OS_PATH}/lib \
${TARGET_TOOL_PATH}/lib/gcc/${TARGET_ARCH}/${GCC_VERSION}/${TARGET_CPU_NAME}/libgcc.a \
${TARGET_OS_PATH}/lib/crtend_android.o \
"

