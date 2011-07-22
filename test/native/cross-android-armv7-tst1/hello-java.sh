
TEST_APP=hello-java

SPEC=androideabi.armv7-a

. /opt-linux-x86/etc/profile.android
. ./android.ndk.env-4.4.3-${SPEC}.sh

JAVA_INCLUDE="-I/opt-linux-x86/j2se6/include"

mkdir -p build/native
mkdir -p build/classes
mkdir -p build/jar

javac -d build/classes HelloJava.java
dx --dex --output=build/jar/HelloJava.jar ./build/classes
adb push build/jar/HelloJava.jar /projects/native-tst
adb shell 'cd /projects/native-tst ; dalvikvm -cp HelloJava.jar HelloJava'

$NDK_GCC $NDK_INCLUDE $JAVA_INCLUDE $NDK_CFLAGS $NDK_LDFLAGS -o build/native/${TEST_APP}-${SPEC} ${TEST_APP}.c
$NDK_READELF -a build/native/${TEST_APP}-${SPEC} > build/native/${TEST_APP}-${SPEC}.txt
adb push build/native/${TEST_APP}-${SPEC} /projects/native-tst
adb shell "cd /projects/native-tst ; ./${TEST_APP}-${SPEC}"
