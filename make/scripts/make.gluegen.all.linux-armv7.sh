#! /bin/sh

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxX86=true \
#    -DisX11=true \

export TARGET_PLATFORM_LIBS=/usr/lib/arm-linux-gnueabi
export TARGET_JAVA_LIBS=/usr/lib/jvm/default-java/jre/lib/arm

ant \
    -Drootrel.build=build-linux-armv7 \
    $* 2>&1 | tee make.gluegen.all.linux-armv7.log
