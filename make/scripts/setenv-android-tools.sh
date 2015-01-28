#! /bin/sh

echo $0

echo Presets
echo   NDK_ROOT $NDK_ROOT
echo   ANDROID_HOME $ANDROID_HOME
echo   ANDROID_BUILD_TOOLS_VERSION $ANDROID_BUILD_TOOLS_VERSION

if [ -z "$NDK_ROOT" ] ; then
    #
    # Generic android-ndk
    #
    if [ -e /usr/local/android-ndk ] ; then
        NDK_ROOT=/usr/local/android-ndk
    elif [ -e /opt-linux-x86_64/android-ndk ] ; then
        NDK_ROOT=/opt-linux-x86_64/android-ndk
    elif [ -e /opt-linux-x86/android-ndk ] ; then
        NDK_ROOT=/opt-linux-x86/android-ndk
    elif [ -e /opt/android-ndk ] ; then
        NDK_ROOT=/opt/android-ndk
    #
    # Specific android-ndk-r10d
    #
    elif [ -e /usr/local/android-ndk-r10d ] ; then
        NDK_ROOT=/usr/local/android-ndk-r10d
    elif [ -e /opt-linux-x86_64/android-ndk-r10d ] ; then
        NDK_ROOT=/opt-linux-x86_64/android-ndk-r10d
    elif [ -e /opt-linux-x86/android-ndk-r10d ] ; then
        NDK_ROOT=/opt-linux-x86/android-ndk-r10d
    elif [ -e /opt/android-ndk-r10d ] ; then
        NDK_ROOT=/opt/android-ndk-r10d
    else 
        echo NDK_ROOT is not specified and does not exist in default locations
        exit 1
    fi
elif [ ! -e $NDK_ROOT ] ; then
    echo NDK_ROOT $NDK_ROOT does not exist
    exit 1
fi
export NDK_ROOT

if [ -z "$ANDROID_HOME" ] ; then
    if [ -e /usr/local/android-sdk-linux_x86 ] ; then
        ANDROID_HOME=/usr/local/android-sdk-linux_x86
    elif [ -e /opt-linux-x86/android-sdk-linux_x86 ] ; then
        ANDROID_HOME=/opt-linux-x86/android-sdk-linux_x86
    elif [ -e /opt/android-sdk-linux_x86 ] ; then
        ANDROID_HOME=/opt/android-sdk-linux_x86
    else 
        echo ANDROID_HOME is not specified and does not exist in default locations
        exit 1
    fi
elif [ ! -e $ANDROID_HOME ] ; then
    echo ANDROID_HOME $ANDROID_HOME does not exist
    exit 1
fi
export ANDROID_HOME

if [ -z "$ANDROID_BUILD_TOOLS_VERSION" ] ; then
    if [ -e $ANDROID_HOME/build-tools/21.1.2/zipalign ] ; then
        ANDROID_BUILD_TOOLS_VERSION=21.1.2
    elif [ -e $ANDROID_HOME/build-tools/20.0.0/zipalign ] ; then
        ANDROID_BUILD_TOOLS_VERSION=20.0.0
    else 
        echo ANDROID_BUILD_TOOLS_VERSION $ANDROID_HOME/build-tools/ANDROID_BUILD_TOOLS_VERSION/zipalign does not exist in default locations
        exit 1
    fi
elif [ ! -e $ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION/zipalign ] ; then
    echo ANDROID_BUILD_TOOLS_VERSION $ANDROID_HOME/build-tools/$ANDROID_BUILD_TOOLS_VERSION/zipalign does not exist
    exit 1
fi
export ANDROID_BUILD_TOOLS_VERSION

echo Set
echo   NDK_ROOT $NDK_ROOT
echo   ANDROID_HOME $ANDROID_HOME
echo   ANDROID_BUILD_TOOLS_VERSION $ANDROID_BUILD_TOOLS_VERSION

