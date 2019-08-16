#! /bin/sh

#    -Dc.compiler.debug=true \
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxX86=true \
#    -DisX11=true \

MACHINE=ppc64le
ARCH=ppc64el
TRIPLET=powerpc64le-linux-gnu

export TARGET_PLATFORM_USRLIBS=/usr/lib/$TRIPLET
export TARGET_JAVA_LIBS=/usr/lib/jvm/java-7-openjdk-$ARCH/jre/lib/$MACHINE

export GLUEGEN_CPPTASKS_FILE="lib/gluegen-cpptasks-linux-$MACHINE.xml"

export SOURCE_LEVEL=1.8
export TARGET_LEVEL=1.8
export TARGET_RT_JAR=/opt-share/jre1.8.0_212/lib/rt.jar

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-linux-$MACHINE \
    $* 2>&1 | tee make.gluegen.all.linux-$MACHINE.log
