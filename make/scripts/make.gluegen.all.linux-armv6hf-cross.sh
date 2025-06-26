#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/setenv-build-jogamp-x86_64.sh ] ; then
    . $SDIR/setenv-build-jogamp-x86_64.sh
fi

# arm-linux-gnueabihf == armhf triplet
PATH=`pwd`/lib/toolchain/armhf-linux-gnueabi/bin:$PATH
export PATH

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARMv6=true \
#    -DisX11=false \

export NODE_LABEL=.

export HOST_UID=jogamp
export HOST_IP=jogamp02
export HOST_RSYNC_ROOT=PROJECTS/JogAmp

export TARGET_UID=jogamp
export TARGET_IP=panda02
export TARGET_ROOT=/home/jogamp/projects-cross
export TARGET_ANT_HOME=/usr/share/ant

export TARGET_PLATFORM_SYSROOT=`gcc --print-sysroot`
export TARGET_PLATFORM_USRROOT=/opt-linux-armv6-armhf
export TARGET_PLATFORM_USRLIBS=$TARGET_PLATFORM_USRROOT/usr/lib
export TARGET_JAVA_LIBS=$TARGET_PLATFORM_USRROOT/jre/lib/arm

export GLUEGEN_CPPTASKS_FILE="lib/gluegen-cpptasks-linux-armv6hf.xml"

#export JUNIT_DISABLED="true"
export JUNIT_RUN_ARG0="-Dnewt.test.Screen.disableScreenMode"

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-linux-armv6hf \
    $* 2>&1 | tee make.gluegen.all.linux-armv6hf-cross.log


