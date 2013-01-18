#! /bin/bash

export HOST_UID=jogamp
export HOST_IP=10.1.0.122
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
export TARGET_IP=jautab03
export TARGET_ADB_PORT=5555
export TARGET_ROOT=/data/projects

export BUILD_DIR=../build-android-armv6

if [ -e /opt-linux-x86/android-sdk-linux_x86 ] ; then
    export ANDROID_HOME=/opt-linux-x86/android-sdk-linux_x86
    export PATH=$ANDROID_HOME/platform-tools:$PATH
fi 

#
# orig android:
#   export LD_LIBRARY_PATH /system/lib
#   export BOOTCLASSPATH /system/framework/core.jar:/system/framework/bouncycastle.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar:/system/framework/core-junit.jar
#

#TSTCLASS=com.jogamp.gluegen.test.junit.generation.Test1p1JavaEmitter
#TSTCLASS=com.jogamp.gluegen.test.junit.generation.Test1p2ProcAddressEmitter
#TSTCLASS=com.jogamp.common.GlueGenVersion
TSTCLASS=jogamp.android.launcher.LauncherUtil
# am start -a android.intent.action.MAIN -n com.jogamp.common/jogamp.common.os.android.GluegenVersionActivity

LOGFILE=`basename $0 .sh`.log

#  -Djava.class.path=lib/junit.jar:/usr/share/ant/lib/ant.jar:/usr/share/ant/lib/ant-junit.jar:$BUILD_DIR/gluegen.jar:$BUILD_DIR/test/build/gluegen-test.jar \
#  -Djava.class.path=lib/ant-junit-all.apk:$BUILD_DIR/gluegen-rt.apk \
#  -Djava.library.path=/system/lib:$TARGET_ROOT/gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/test/build/natives \

RSYNC_EXCLUDES="--delete-excluded \
                --exclude 'build-x86*/' --exclude 'build-linux*/' --exclude 'build-win*/' --exclude 'build-mac*/' \
                --exclude 'classes/' --exclude 'src/' --exclude '.git/' --exclude '*-java-src.zip' \
                --exclude 'gensrc/' --exclude 'doc/' --exclude 'jnlp-files' --exclude 'archive/' \
                --exclude 'android-sdk/' --exclude 'resources/' --exclude 'scripts/' \
                --exclude 'stub_includes/' --exclude 'nbproject/' --exclude '*.log' --exclude '*.zip' --exclude '*.7z' \
                --exclude 'make/lib/external/'"

echo "#! /system/bin/sh" > $BUILD_DIR/gluegen-targetcommand.sh

# export BOOTCLASSPATH=/system/framework/core.jar:/system/framework/bouncycastle.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar ; \

echo "\
rsync -av --delete --delete-after $RSYNC_EXCLUDES \
   $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/gluegen \
   $TARGET_ROOT ; \
cd $TARGET_ROOT/gluegen/make ; \
export LD_LIBRARY_PATH=/system/lib:$TARGET_ROOT/gluegen/make/$BUILD_DIR/obj:$TARGET_ROOT/gluegen/make/$BUILD_DIR/test/build/natives ; \
dalvikvm \
  -Xjnigreflimit:2000 \
  -cp ../make/lib/ant-junit-all.apk:$BUILD_DIR/jogamp-android-launcher.apk:$BUILD_DIR/gluegen.apk:$BUILD_DIR/test/build/gluegen-test.apk \
  -Djogamp.debug.JNILibLoader=true \
  -Djogamp.debug.NativeLibrary=true \
  -Djogamp.debug.NativeLibrary.Lookup=true \
  -Djogamp.debug.ProcAddressHelper=true \
  com.android.internal.util.WithFramework \
  $TSTCLASS \
" >> $BUILD_DIR/gluegen-targetcommand.sh

chmod ugo+x $BUILD_DIR/gluegen-targetcommand.sh
adb connect $TARGET_IP:$TARGET_ADB_PORT
adb -s $TARGET_IP:$TARGET_ADB_PORT push $BUILD_DIR/gluegen-targetcommand.sh $TARGET_ROOT/gluegen-targetcommand.sh
adb -s $TARGET_IP:$TARGET_ADB_PORT shell su -c $TARGET_ROOT/gluegen-targetcommand.sh 2>&1 | tee $LOGFILE


