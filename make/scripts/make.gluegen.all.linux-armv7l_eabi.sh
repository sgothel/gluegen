#! /bin/sh

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxX86=true \
#    -DisX11=true \

ant \
    -Drootrel.build=build-armv7l_eabi \
    $* 2>&1 | tee make.gluegen.all.linux-armv7l_eabi.log
