#! /bin/sh

# set -x

# Aligned with Android SDK build-tools 36 and NDK 29 as of 2025-06-25
#
# As it is no more easily achievable to download the complete SDK 
# separately, I used Android-Studio to fetch all parts incl. the NDK.
# Thereafter I copied ~/Android/Sdk -> /opt-linux-x86_64/android-sdk-linux_x86_64
# which I also use for the official crosscompilation.
#
# Variable names borrowed from ~/Android/Sdk/ndk/29.0.13599879/build/cmake/android.toolchain.cmake
# We only use ANDROID_API_LEVEL instead of ANDROID_PLATFORM_LEVEL, as it describes the API level.
#
#
# User should set environment variables:
# ==========================================
#
# - ANDROID_HOME - defaults to one of
#        ~/Android/Sdk
#        /opt-linux-x86_64/android-sdk-linux_x86_64
#        /opt/android-sdk-linux_x86_64
#        /usr/local/android-sdk-linux_x86_64
#
# - ANDROID_API_LEVEL - defaults to 24
#
# - ANDROID_HOST_TAG - defaults to linux-x86_64
#
# - ANDROID_ABI - defaults to x86_64, one of
#        armeabi-v7a (with NEON by default since NDK r21)
#        arm64-v8a 
#        x86_64
#        x86
#
# Following environment variables will be set
# ============================================
#
# - ANDROID_SYSROOT_ABI
# - ANDROID_TOOLCHAIN_NAME
# - ANDROID_LLVM_TRIPLE
# - ANDROID_BUILD_TOOLS_VERSION
# - ANDROID_NDK
# - ANDROID_BUILDTOOLS_ROOT
# - ANDROID_TOOLCHAIN_ROOT
# - ANDROID_TOOLCHAIN_SYSROOT1
# - ANDROID_TOOLCHAIN_SYSROOT1_INC
# - ANDROID_TOOLCHAIN_SYSROOT1_INC_ARCH
# - ANDROID_TOOLCHAIN_SYSROOT1_INC_STL
# - ANDROID_TOOLCHAIN_SYSROOT1_LIB1
# - ANDROID_TOOLCHAIN_SYSROOT1_LIB2
#
# Android Studio SDK + NDK Filesystem Layout (official)
#
# ~/Android/Sdk/
# ~/Android/Sdk/build-tools/36.0.0/
# ~/Android/Sdk/build-tools/36.0.0/zipalign (*)
# ~/Android/Sdk/ndk/
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/bin/lld (*)
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/bin/clang (*)
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot (2)
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/aarch64-linux-android/asm/types.h (*) (2,3)
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc.a (*)
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/24/libc++.so (*) (2)
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/24/libc.so (*) (2)
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/c++/v1/complex (*) (4)
#
# (*) tested by this script
#
# (2) ANDROID_TOOLCHAIN_SYSROOT1 exposes all libs with standard FS layout usr/lib and usr/include
#     -> ANDROID_TOOLCHAIN_SYSROOT1_INC
#     -> ANDROID_TOOLCHAIN_SYSROOT1_LIB1
#     -> ANDROID_TOOLCHAIN_SYSROOT1_LIB2
#
# (3) ANDROID_TOOLCHAIN_SYSROOT1 also exposes the arch dependent include files, i.e. asm/types.h etc
#     -> ANDROID_TOOLCHAIN_SYSROOT1_INC_ARCH
#
# (4) ANDROID_TOOLCHAIN_SYSROOT1_INC_STL for LLVM's C++ STL lib (default since NDK r18)
#     Using LLVM's c++_shared as of NDK r18: https://developer.android.com/ndk/guides/cpp-support.html
#     LLVM's c++ headers must come before other system header!
#     Also see https://github.com/android/ndk/issues/452 and https://gitlab.kitware.com/cmake/cmake/issues/17059
#
# Having
#   ANDROID_HOME=~/Android/Sdk
#   ANDROID_API_LEVEL 24
#   ANDROID_HOST_TAG linux-x86_64
#   ANDROID_ABI arm64-v8a
# Using derived values of
#   ANDROID_BUILD_TOOLS_VERSION=36.0.0
#   ANDROID_NDK_VERSION=29.0.13599879
#   ANDROID_TOOLCHAIN_NAME aarch64-linux-android
#   ANDROID_TOOLCHAIN_PLATFORM_NAME=arm64
#   ANDROID_SYSROOT_ABI=arm64

echo $0

NDK_TOOLCHAIN_VERSION=clang
echo "Setting NDK_TOOLCHAIN_VERSION to ${NDK_TOOLCHAIN_VERSION} default!"

if [ -z "${ANDROID_API_LEVEL}" ] ; then
    ANDROID_API_LEVEL=24
    echo "Setting undefined ANDROID_API_LEVEL to ${ANDROID_API_LEVEL} default!"
fi
if [ -z "${ANDROID_HOST_TAG}" ] ; then
    ANDROID_HOST_TAG=linux-x86_64
    echo "Setting undefined ANDROID_HOST_TAG to ${ANDROID_HOST_TAG} default!"
fi

if [ -z "${ANDROID_ABI}" ] ; then
    ANDROID_ABI=x86_64
    echo "Setting undefined ANDROID_ABI to ${ANDROID_ABI} default!"
fi
if [ "${ANDROID_ABI}" = "armeabi-v7a" ] ; then
    # with NEON by default since NDK r21
    ANDROID_SYSROOT_ABI=arm
    #CMAKE_SYSTEM_PROCESSOR=armv7-a
    ANDROID_TOOLCHAIN_NAME=arm-linux-androideabi
    ANDROID_LLVM_TRIPLE=armv7-none-linux-androideabi
elif [ "${ANDROID_ABI}" = "arm64-v8a" ] ; then
    ANDROID_SYSROOT_ABI=arm64
    #CMAKE_SYSTEM_PROCESSOR=aarch64
    ANDROID_TOOLCHAIN_NAME=aarch64-linux-android
    ANDROID_LLVM_TRIPLE=aarch64-none-linux-android
elif [ "${ANDROID_ABI}" = "x86_64" ] ; then
    ANDROID_SYSROOT_ABI=x86_64
    #CMAKE_SYSTEM_PROCESSOR=x86_64
    ANDROID_TOOLCHAIN_NAME=x86_64-linux-android
    ANDROID_LLVM_TRIPLE=x86_64-none-linux-android
elif [ "${ANDROID_ABI}" = "x86" ] ; then
    ANDROID_SYSROOT_ABI=x86
    #CMAKE_SYSTEM_PROCESSOR=i686
    ANDROID_TOOLCHAIN_NAME=i686-linux-android
    ANDROID_LLVM_TRIPLE=i686-none-linux-android
else
    echo "ANDROID_ABI is ${ANDROID_ABI} and not supported!"
    exit 1
fi

echo "Preset-0 (user)"
echo   ANDROID_HOME ${ANDROID_HOME}
echo   ANDROID_API_LEVEL ${ANDROID_API_LEVEL}
echo   ANDROID_HOST_TAG ${ANDROID_HOST_TAG}
echo   ANDROID_ABI ${ANDROID_ABI}
echo
echo Preset-1
echo   NDK_TOOLCHAIN_VERSION ${NDK_TOOLCHAIN_VERSION}
echo   ANDROID_SYSROOT_ABI ${ANDROID_SYSROOT_ABI}
echo   ANDROID_TOOLCHAIN_NAME ${ANDROID_TOOLCHAIN_NAME}
echo   "ANDROID_LLVM_TRIPLE ${ANDROID_LLVM_TRIPLE} (compiler target)"
echo   ANDROID_BUILD_TOOLS_VERSION ${ANDROID_BUILD_TOOLS_VERSION}
echo   ANDROID_NDK ${ANDROID_NDK}
echo
echo Preset-2
echo   ANDROID_BUILDTOOLS_ROOT ${ANDROID_BUILDTOOLS_ROOT}
echo   ANDROID_TOOLCHAIN_ROOT ${ANDROID_TOOLCHAIN_ROOT}
echo   ANDROID_TOOLCHAIN_SYSROOT1 ${ANDROID_TOOLCHAIN_SYSROOT1}
echo   ANDROID_TOOLCHAIN_SYSROOT1_INC ${ANDROID_TOOLCHAIN_SYSROOT1_INC}
echo   ANDROID_TOOLCHAIN_SYSROOT1_INC_ARCH ${ANDROID_TOOLCHAIN_SYSROOT1_INC_ARCH}
echo   ANDROID_TOOLCHAIN_SYSROOT1_INC_STL ${ANDROID_TOOLCHAIN_SYSROOT1_INC_STL}
echo   ANDROID_TOOLCHAIN_SYSROOT1_LIB1 ${ANDROID_TOOLCHAIN_SYSROOT1_LIB1}
echo   ANDROID_TOOLCHAIN_SYSROOT1_LIB2 ${ANDROID_TOOLCHAIN_SYSROOT1_LIB2}
echo

check_exists() {
    if [ ! -e "$1" ] ; then
        echo "$1" does not exist
        exit 1
    fi
    return 0
}

if [ -z "${ANDROID_HOME}" ] ; then
    if [ -e /opt-linux-x86_64/android-sdk-linux_x86_64 ] ; then
        ANDROID_HOME=/opt-linux-x86_64/android-sdk-linux_x86_64
    elif [ -e /opt/android-sdk-linux_x86_64 ] ; then
        ANDROID_HOME=/opt/android-sdk-linux_x86_64
    elif [ -e /usr/local/android-sdk-linux_x86_64 ] ; then
        ANDROID_HOME=/usr/local/android-sdk-linux_x86_64
    elif [ -e ${HOME}/Android/Sdk ] ; then
        ANDROID_HOME=${HOME}/Android/Sdk
    else 
        echo ANDROID_HOME is not specified and does not exist in default locations
        exit 1
    fi
elif [ ! -e ${ANDROID_HOME} ] ; then
    echo ANDROID_HOME ${ANDROID_HOME} does not exist
    exit 1
fi

unset ANDROID_BUILD_TOOLS_VERSION
if [ -z "${ANDROID_BUILD_TOOLS_VERSION}" ] ; then
    # basename $(dirname `find -L /home/sven/Android/Sdk/build-tools -name zipalign | sort -u | tail -n1`)
    fzipalign=`find -L ${ANDROID_HOME}/build-tools -name zipalign | sort -u | tail -n1`
    if [ ! -z "${fzipalign}" ] ; then
        dzipalign=`dirname ${fzipalign}`
        vzipalign=`basename ${dzipalign}`
        if [ -e ${ANDROID_HOME}/build-tools/${vzipalign}/zipalign ] ; then
            ANDROID_BUILD_TOOLS_VERSION=${vzipalign}
        fi
    fi
    if [ -z "${ANDROID_BUILD_TOOLS_VERSION}" ] ; then
        echo ANDROID_BUILD_TOOLS_VERSION ${ANDROID_HOME}/build-tools/ANDROID_BUILD_TOOLS_VERSION/zipalign does not exist
        exit 1
    fi
fi

if [ -z "${ANDROID_NDK}" ] ; then
    #
    # Generic android-ndk
    #
    if [ -e ${ANDROID_HOME}/ndk ] ; then
        # basename $(dirname `find -L ndk -name toolchains -a -type d | sort -u | tail -n1`)
        d2toolchains=`find -L ${ANDROID_HOME}/ndk -name toolchains -a -type d | sort -u | tail -n1`
        if [ ! -z "${d2toolchains}" ] ; then
            dtoolchains=`dirname ${d2toolchains}`
            vtoolchains=`basename ${dtoolchains}`
            # ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc.a (*)
            if [ -e ${ANDROID_HOME}/ndk/${vtoolchains}/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc.a ] ; then
                ANDROID_NDK_VERSION=${vtoolchains}
            fi
        fi
        if [ -z "${ANDROID_NDK_VERSION}" ] ; then
            echo ANDROID_NDK_VERSION ${ANDROID_HOME}/ndk/ANDROID_NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc.a does not exist
        else
            ANDROID_NDK=${ANDROID_HOME}/ndk/${vtoolchains}
        fi
    fi
    if [ -z "${ANDROID_NDK}" ] ; then
        if [ -e /usr/local/android-ndk ] ; then
            ANDROID_NDK=/usr/local/android-ndk
        elif [ -e /opt-linux-x86_64/android-ndk ] ; then
            ANDROID_NDK=/opt-linux-x86_64/android-ndk
        elif [ -e /opt/android-ndk ] ; then
            ANDROID_NDK=/opt/android-ndk
        fi
    fi
    if [ -z "${ANDROID_NDK}" ] ; then
        echo ANDROID_NDK is not specified and does not exist in default locations
        exit 1
    fi
fi
if [ ! -e "${ANDROID_NDK}" ] ; then
    echo ANDROID_NDK ${ANDROID_NDK} does not exist
    exit 1
fi

ANDROID_BUILDTOOLS_ROOT=${ANDROID_HOME}/build-tools/${ANDROID_BUILD_TOOLS_VERSION}
ANDROID_TOOLCHAIN_ROOT=${ANDROID_NDK}/toolchains/llvm/prebuilt/${ANDROID_HOST_TAG}

ANDROID_TOOLCHAIN_SYSROOT1=${ANDROID_TOOLCHAIN_ROOT}/sysroot
ANDROID_TOOLCHAIN_SYSROOT1_INC=${ANDROID_TOOLCHAIN_SYSROOT1}/usr/include
ANDROID_TOOLCHAIN_SYSROOT1_INC_ARCH=${ANDROID_TOOLCHAIN_SYSROOT1_INC}/${ANDROID_TOOLCHAIN_NAME}
ANDROID_TOOLCHAIN_SYSROOT1_INC_STL=${ANDROID_TOOLCHAIN_SYSROOT1_INC}/c++/v1
ANDROID_TOOLCHAIN_SYSROOT1_LIB1=${ANDROID_TOOLCHAIN_SYSROOT1}/usr/lib/${ANDROID_TOOLCHAIN_NAME}/${ANDROID_API_LEVEL}
ANDROID_TOOLCHAIN_SYSROOT1_LIB2=${ANDROID_TOOLCHAIN_SYSROOT1}/usr/lib/${ANDROID_TOOLCHAIN_NAME}

# ~/Android/Sdk/build-tools/36.0.0/zipalign (*)
check_exists ${ANDROID_BUILDTOOLS_ROOT}/zipalign

# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/bin/lld (*)
check_exists ${ANDROID_TOOLCHAIN_ROOT}/bin/lld

# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/bin/clang (*)
check_exists ${ANDROID_TOOLCHAIN_ROOT}/bin/clang

# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/aarch64-linux-android/asm/types.h (*) (2)
check_exists ${ANDROID_TOOLCHAIN_SYSROOT1_INC_ARCH}/asm/types.h

# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/c++/v1/complex (*) (4)
check_exists ${ANDROID_TOOLCHAIN_SYSROOT1_INC_STL}/complex

# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/libc.a (*)
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/24/libc++.so (*) (2)
# ~/Android/Sdk/ndk/29.0.13599879/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/24/libc.so (*) (2)
check_exists ${ANDROID_TOOLCHAIN_SYSROOT1_LIB2}/libc.a
check_exists ${ANDROID_TOOLCHAIN_SYSROOT1_LIB1}/libc++.a
check_exists ${ANDROID_TOOLCHAIN_SYSROOT1_LIB1}/libc.so

export ANDROID_HOME
export ANDROID_API_LEVEL
export ANDROID_HOST_TAG
export ANDROID_ABI

export NDK_TOOLCHAIN_VERSION
export ANDROID_SYSROOT_ABI
export ANDROID_TOOLCHAIN_NAME
export ANDROID_LLVM_TRIPLE
export ANDROID_BUILD_TOOLS_VERSION
export ANDROID_NDK

export ANDROID_BUILDTOOLS_ROOT
export ANDROID_TOOLCHAIN_ROOT
export ANDROID_TOOLCHAIN_SYSROOT1
export ANDROID_TOOLCHAIN_SYSROOT1_INC
export ANDROID_TOOLCHAIN_SYSROOT1_INC_ARCH
export ANDROID_TOOLCHAIN_SYSROOT1_INC_STL
export ANDROID_TOOLCHAIN_SYSROOT1_LIB1
export ANDROID_TOOLCHAIN_SYSROOT1_LIB2

echo "Postset-0 (user)"
echo   ANDROID_HOME ${ANDROID_HOME}
echo   ANDROID_API_LEVEL ${ANDROID_API_LEVEL}
echo   ANDROID_HOST_TAG ${ANDROID_HOST_TAG}
echo   ANDROID_ABI ${ANDROID_ABI}
echo
echo Postset-1
echo   NDK_TOOLCHAIN_VERSION ${NDK_TOOLCHAIN_VERSION}
echo   ANDROID_SYSROOT_ABI ${ANDROID_SYSROOT_ABI}
echo   ANDROID_TOOLCHAIN_NAME ${ANDROID_TOOLCHAIN_NAME}
echo   "ANDROID_LLVM_TRIPLE ${ANDROID_LLVM_TRIPLE} (compiler target)"
echo   ANDROID_BUILD_TOOLS_VERSION ${ANDROID_BUILD_TOOLS_VERSION}
echo   ANDROID_NDK ${ANDROID_NDK}
echo
echo Postset-2
echo   ANDROID_BUILDTOOLS_ROOT ${ANDROID_BUILDTOOLS_ROOT}
echo   ANDROID_TOOLCHAIN_ROOT ${ANDROID_TOOLCHAIN_ROOT}
echo   ANDROID_TOOLCHAIN_SYSROOT1 ${ANDROID_TOOLCHAIN_SYSROOT1}
echo   ANDROID_TOOLCHAIN_SYSROOT1_INC ${ANDROID_TOOLCHAIN_SYSROOT1_INC}
echo   ANDROID_TOOLCHAIN_SYSROOT1_INC_ARCH ${ANDROID_TOOLCHAIN_SYSROOT1_INC_ARCH}
echo   ANDROID_TOOLCHAIN_SYSROOT1_INC_STL ${ANDROID_TOOLCHAIN_SYSROOT1_INC_STL}
echo   ANDROID_TOOLCHAIN_SYSROOT1_LIB1 ${ANDROID_TOOLCHAIN_SYSROOT1_LIB1}
echo   ANDROID_TOOLCHAIN_SYSROOT1_LIB2 ${ANDROID_TOOLCHAIN_SYSROOT1_LIB2}
echo

export -p | grep ANDROID

#
# CC="$ANDROID_NDK/toolchains/llvm/prebuilt/$ANDROID_HOST_TAG/bin/clang -target $ANDROID_LLVM_TRIPLE"
#
## Generic flags.
##list(APPEND ANDROID_COMPILER_FLAGS
#  -g
#  -DANDROID
#  -fdata-sections
#  -ffunction-sections
#  -funwind-tables
#  -fstack-protector-strong
#  -no-canonical-prefixes)
#list(APPEND ANDROID_LINKER_FLAGS
#  -Wl,--build-id
#  -Wl,--warn-shared-textrel
#  -Wl,--fatal-warnings)
#list(APPEND ANDROID_LINKER_FLAGS_EXE -Wl,--gc-sections)
#
#list(APPEND ANDROID_COMPILER_FLAGS_RELEASE -O2)
#

