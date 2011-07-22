
TEST_APP=arm-gcc-test

SPEC=androideabi.armv7-a

. /opt-linux-x86/etc/profile.android
. ./android.ndk.env-4.4.3-${SPEC}.sh

mkdir -p build/native

$NDK_GCC $NDK_INCLUDE $NDK_CFLAGS $NDK_LDFLAGS -o build/native/${TEST_APP}-${SPEC} ${TEST_APP}.c
$NDK_READELF -a build/native/${TEST_APP}-${SPEC} > build/native/${TEST_APP}-${SPEC}.txt
adb push build/native/${TEST_APP}-${SPEC} /projects/native-tst
adb shell /projects/native-tst/${TEST_APP}-${SPEC}

