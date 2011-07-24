export HOST_UID=sven
export HOST_IP=192.168.0.52
export HOST_RSYNC_ROOT=PROJECTS/JOGL

export TARGET_UID=jogamp
export TARGET_IP=beagle02
export TARGET_ROOT=/projects

export BUILD_DIR=../build-android-armv7

if [ -e /opt-linux-x86/android-sdk-linux_x86 ] ; then
    export ANDROID_SDK_HOME=/opt-linux-x86/android-sdk-linux_x86
    export PATH=$ANDROID_SDK_HOME/platform-tools:$PATH
fi 

#
# orig android:
#   export LD_LIBRARY_PATH /system/lib
#   export BOOTCLASSPATH /system/framework/core.jar:/system/framework/bouncycastle.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar:/system/framework/core-junit.jar
#

#TSTCLASS=com.jogamp.gluegen.test.junit.generation.Test1p2ProcAddressEmitter
TSTCLASS=com.jogamp.common.GlueGenVersion

LOGFILE=`basename $0 .sh`.log

#  -Djava.class.path=lib/junit.jar:/usr/share/ant/lib/ant.jar:/usr/share/ant/lib/ant-junit.jar:$BUILD_DIR/gluegen.jar:$BUILD_DIR/test/build/gluegen-test.jar \
#  -Djava.library.path=/system/lib:$TARGET_ROOT/gluegen/make/$BUILD_DIR/obj:$BUILD_DIR/test/build/natives \

adb shell "\
rsync -av --delete --delete-after --exclude 'build-x86*/' $HOST_UID@$HOST_IP::$HOST_RSYNC_ROOT/gluegen $TARGET_ROOT ; \
cd $TARGET_ROOT/gluegen/make ; \
export LD_LIBRARY_PATH=/system/lib:$TARGET_ROOT/gluegen/make/$BUILD_DIR/obj:$TARGET_ROOT/gluegen/make/$BUILD_DIR/test/build/natives ; \
export BOOTCLASSPATH=/system/framework/core.jar:/system/framework/bouncycastle.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar ; \
dalvikvm \
  -Xjnigreflimit:2000 \
  -Djava.class.path=lib/ant-junit-all.apk:$BUILD_DIR/gluegen-rt.apk \
  -Djogamp.debug.JNILibLoader=true \
  -Djogamp.debug.NativeLibrary=true \
  -Djogamp.debug.NativeLibrary.Lookup=true \
  -Djogamp.debug.ProcAddressHelper=true \
  com.android.internal.util.WithFramework \
  $TSTCLASS \
" 2>&1 | tee $LOGFILE

adb pull $TARGET_ROOT/gluegen/make/$LOGFILE .
