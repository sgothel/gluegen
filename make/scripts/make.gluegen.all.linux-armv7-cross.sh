#! /bin/sh

PATH=`pwd`/lib/linux-x86_64/arm-linux-gnueabi/bin:$PATH
export PATH

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxARMv7=true \
#    -DisX11=false \

ant \
    -Drootrel.build=build-linux-armv7 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxARMv7=true \
    -DisX11=true \
    \
    -DuseKD=true \
    -DuseOpenMAX=true \
    -DuseBroadcomEGL=true \
    $* 2>&1 | tee make.gluegen.all.linux-armv7-cross.log


