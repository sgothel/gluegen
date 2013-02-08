#! /bin/bash

export HOST_UID=jogamp
# jogamp02 - 10.1.0.122
export HOST_IP=10.1.0.122
#export HOST_IP=10.1.0.52
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
#export TARGET_IP=panda02
#export TARGET_IP=jautab03
#export TARGET_IP=C5OKCT139647
export TARGET_IP=D025A0A025040L5L
#export TARGET_ADB_PORT=5555
export TARGET_ADB_PORT=
export TARGET_ROOT=jogamp-test

if [ -z "$TARGET_ADB_PORT" ] ; then
  export TARGET_IP_PORT=$TARGET_IP
else
  export TARGET_IP_PORT=$TARGET_IP:$TARGET_ADB_PORT
fi

export BUILD_DIR=../build-android-armv6

if [ -e /opt-linux-x86/android-sdk-linux_x86 ] ; then
    export ANDROID_HOME=/opt-linux-x86/android-sdk-linux_x86
    export PATH=$ANDROID_HOME/platform-tools:$PATH
fi 

TSTCLASS=com.jogamp.common.GlueGenVersion
#TSTCLASS=jogamp.android.launcher.LauncherUtil
#TSTSCLASS=com.jogamp.common.os.TestElfReader01

LOGFILE=`basename $0 .sh`.log

#adb -s $TARGET_IP_PORT uninstall jogamp.android.launcher
#adb -s $TARGET_IP_PORT install $BUILD_DIR/jogamp-android-launcher.apk

#adb -s $TARGET_IP_PORT uninstall com.jogamp.common
#adb -s $TARGET_IP_PORT install $BUILD_DIR/gluegen-rt-android-armeabi.apk

SHELL_CMD="\
cd /sdcard ; \
if [ -e $TARGET_ROOT ] ; then rm -r $TARGET_ROOT ; fi ; \
mkdir $TARGET_ROOT ; cd $TARGET_ROOT ; \
setprop log.redirect-stdio true ; setprop log.redirect-stderr true ; \
am kill-all ; \
am start -W -S -a android.intent.action.MAIN -n jogamp.android.launcher/jogamp.android.launcher.MainLauncher -d launch://jogamp.org/$TSTCLASS/?sys=com.jogamp.common \
"

adb connect $TARGET_IP_PORT
adb -s $TARGET_IP_PORT logcat -c
adb -s $TARGET_IP_PORT shell $SHELL_CMD 2>&1 | tee $LOGFILE
adb -s $TARGET_IP_PORT logcat -d 2>&1 | tee -a $LOGFILE

