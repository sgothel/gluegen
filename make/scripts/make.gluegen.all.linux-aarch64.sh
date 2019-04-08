#! /bin/sh

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxX86=true \
#    -DisX11=true \

# aarch64-linux-gnue == aarch64 triplet
export TARGET_PLATFORM_USRROOT=
export TARGET_PLATFORM_USRLIBS=$TARGET_PLATFORM_USRROOT/usr/lib/aarch64-linux-gnu
export TARGET_JAVA_LIBS=$TARGET_PLATFORM_USRROOT/usr/lib/jvm/java-8-openjdk-aarch64/jre/lib/aarch64

export GLUEGEN_CPPTASKS_FILE="lib/gluegen-cpptasks-linux-aarch64-ontarget.xml"

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-linux-aarch64 \
    $* 2>&1 | tee make.gluegen.all.linux-aarch64.log
