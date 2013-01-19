#! /bin/bash

export HOST_UID=jogamp
# jogamp02 - 10.1.0.122
export HOST_IP=10.1.0.122
#export HOST_IP=10.1.0.52
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
#export TARGET_IP=panda02
export TARGET_IP=jautab03
export TARGET_ADB_PORT=5555
export TARGET_ROOT=jogamp-test

export BUILD_DIR=../build-android-armv6

if [ -e /opt-linux-x86/android-sdk-linux_x86 ] ; then
    export ANDROID_HOME=/opt-linux-x86/android-sdk-linux_x86
    export PATH=$ANDROID_HOME/platform-tools:$PATH
fi 

TSTCLASS=com.jogamp.common.GlueGenVersion
#TSTCLASS=jogamp.android.launcher.LauncherUtil

LOGFILE=`basename $0 .sh`.log

adb -s $TARGET_IP:$TARGET_ADB_PORT uninstall jogamp.android.launcher
adb -s $TARGET_IP:$TARGET_ADB_PORT uninstall com.jogamp.common
adb -s $TARGET_IP:$TARGET_ADB_PORT install $BUILD_DIR/jogamp-android-launcher.apk
adb -s $TARGET_IP:$TARGET_ADB_PORT install $BUILD_DIR/gluegen-rt-android-armeabi.apk

SHELL_CMD="\
cd /sdcard ; \
if [ -e $TARGET_ROOT ] ; then rm -r $TARGET_ROOT ; fi ; \
mkdir $TARGET_ROOT ; cd $TARGET_ROOT ; \
setprop log.redirect-stdio true ; setprop log.redirect-stderr true ; \
am kill-all ; \
am start -S -a android.intent.action.MAIN -n jogamp.android.launcher/jogamp.android.launcher.MainLauncher -d launch://jogamp.org/$TSTCLASS/?pkg=com.jogamp.common \
"

adb connect $TARGET_IP:$TARGET_ADB_PORT
adb -s $TARGET_IP:$TARGET_ADB_PORT shell $SHELL_CMD 2>&1 | tee $LOGFILE

