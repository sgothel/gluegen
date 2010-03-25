#! /bin/sh

. ../../setenv-build-jogl-x86.sh

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxX86=true \
#    -DisX11=true \

ant \
    -Dgluegen.user.compiler.file=`pwd`/lib/gluegen.compiler.linux-32bit.xml \
    -Drootrel.build=build-x86 \
    -Dos.arch=x86 \
    $* 2>&1 | tee make.gluegen.all.linux-x86.log
