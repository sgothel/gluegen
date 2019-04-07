#! /bin/sh

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxX86=true \
#    -DisX11=true \

# arm-linux-gnueabihf == armhf triplet
export TARGET_PLATFORM_LIBS=/usr/lib/arm-linux-gnueabihf
export TARGET_JAVA_LIBS=/usr/lib/jvm/java-8-openjdk-armhf/jre/lib/arm

export GLUEGEN_CPPTASKS_FILE="lib/gluegen-cpptasks-linux-armv6hf-ontarget.xml"

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-linux-armv6hf \
    $* 2>&1 | tee make.gluegen.all.linux-armv6hf.log
