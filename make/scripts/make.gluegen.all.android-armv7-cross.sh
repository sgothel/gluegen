#! /bin/sh

if [ -z "$NDK_ROOT" ] ; then
    NDK_ROOT=/usr/local/android-ndk-r6
fi
export NDK_ROOT
NDK_TOOLCHAIN=$NDK_ROOT/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/arm-linux-androideabi

export PATH="$NDK_TOOLCHAIN/bin:$PATH"

ANDROID_VERSION=9
export GCC_VERSION=4.4.3
HOST_ARCH=linux-x86
export TARGET_ARCH=arm-linux-androideabi
# mcpu: cortex-a8', `cortex-a9', `cortex-r4', `cortex-r4f', `cortex-m3', `cortex-m1', `xscale', `iwmmxt', `iwmmxt2', `ep9312'. 
export TARGET_CPU_NAME=armv7-a
TARGET_CPU_TUNE=armv7-a
# mfpu: `vfp', `vfpv3', `vfpv3-d16' and `neon'
TARGET_FPU_NAME=vfpv3
TARGET_FPU_ABI=softfp

export TARGET_TOOL_PATH=${NDK_ROOT}/toolchains/${TARGET_ARCH}-${GCC_VERSION}/prebuilt/${HOST_ARCH}

export TARGET_OS_PATH=${NDK_ROOT}/platforms/android-${ANDROID_VERSION}/arch-arm/usr
export HOST_OS_PATH=${NDK_ROOT}/platforms/android-${ANDROID_VERSION}/arch-x86/usr

export NDK_XBIN_PATH=${TARGET_TOOL_PATH}/bin
export NDK_BIN_PATH=${TARGET_TOOL_PATH}/${TARGET_ARCH}/bin

export NDK_GCC=${NDK_XBIN_PATH}/${TARGET_ARCH}-gcc
export NDK_AR=${NDK_XBIN_PATH}/${TARGET_ARCH}-ar
export NDK_STRIP=${NDK_XBIN_PATH}/${TARGET_ARCH}-strip
export NDK_READELF=${NDK_XBIN_PATH}/${TARGET_ARCH}-readelf

export PATH=${NDK_XBIN_PATH}:$PATH

export NDK_INCLUDE="-I${TARGET_OS_PATH}/include"


export NDK_CFLAGS="\
-march=${TARGET_CPU_NAME} \
-fpic \
-DANDROID \
"

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


# directory for cc1 ${TARGET_TOOL_PATH}/libexec/gcc/${TARGET_ARCH}/${GCC_VERSION} \

#arm-eabi-gcc -o hello hello.c -Wl,-rpath-link=/Users/nirnimesh/NIR/android/mydroid/cupcake/out/target/product/generic/obj/lib 
#-L/Users/nirnimesh/NIR/android/mydroid/cupcake/out/target/product/generic/obj/lib 
#-nostdlib /Users/nirnimesh/NIR/android/mydroid/cupcake/out/target/product/generic/obj/lib/crtbegin_dynamic.o -lc

which gcc 2>&1 | tee make.gluegen.all.android-armv7-cross.log

ant \
    -Dgluegen-cpptasks.file=`pwd`/lib/gluegen-cpptasks-android-armv7.xml \
    -Drootrel.build=build-android-armv7 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisAndroid=true \
    -DisAndroidARMv7=true \
    -DisCrosscompilation=true \
    \
    $* 2>&1 | tee -a make.gluegen.all.android-armv7-cross.log

#$NDK_GCC -march=armv7-a -fpic -DANDROID -I/usr/local/android-ndk-r6/platforms/android-9/arch-arm/usr/include -Wl,--demangle -nostdlib -Bdynamic -Wl,-dynamic-linker,/system/bin/linker -Wl,--gc-sections -Wl,-z,nocopyreloc /usr/local/android-ndk-r6/platforms/android-9/arch-arm/usr/lib/libc.so /usr/local/android-ndk-r6/platforms/android-9/arch-arm/usr/lib/libstdc++.so /usr/local/android-ndk-r6/platforms/android-9/arch-arm/usr/lib/libm.so /usr/local/android-ndk-r6/platforms/android-9/arch-arm/usr/lib/crtbegin_dynamic.o -Wl,--no-undefined -Wl,-rpath-link=/usr/local/android-ndk-r6/platforms/android-9/arch-arm/usr/lib /usr/local/android-ndk-r6/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/lib/gcc/arm-linux-androideabi/4.4.3/armv7-a/libgcc.a /usr/local/android-ndk-r6/platforms/android-9/arch-arm/usr/lib/crtend_android.o -c -fno-rtti -fPIC -DANDROID -I/home/rsantina/projects/jogamp/gluegen/build-android-arm/gensrc/native -I/home/rsantina/projects/jogamp/gluegen/build-android-arm/gensrc/native/Unix -I/opt/x86_64/jdk1.6.0_25/include -I/opt/x86_64/jdk1.6.0_25/include/linux -I/home/rsantina/projects/jogamp/gluegen/make/stub_includes/platform /home/rsantina/projects/jogamp/gluegen/src/native/unix/UnixDynamicLinkerImpl_JNI.c /home/rsantina/projects/jogamp/gluegen/src/native/common/PointerBuffer.c /home/rsantina/projects/jogamp/gluegen/src/native/common/MachineDescriptionRuntime.c /home/rsantina/projects/jogamp/gluegen/src/native/common/JVM_Tool.c

#which gcc
#echo $TARGET_TOOL_PATH

#$NDK_GCC $NDK_INCLUDE $NDK_CFLAGS $NDK_LDFLAGS -o /home/rsantina/projects/jogamp/gluegen/src/native/unix/UnixDynamicLinkerImpl_JNI.c /home/rsantina/projects/jogamp/gluegen/src/native/common/PointerBuffer.c /home/rsantina/projects/jogamp/gluegen/src/native/common/MachineDescriptionRuntime.c /home/rsantina/projects/jogamp/gluegen/src/native/common/JVM_Tool.c

