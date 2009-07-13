#! /bin/sh

. ../../setenv-build-jogl-x86.sh

#    -Dc.compiler.debug=true 

ant -v \
    -DisCDCFP=true \
    -Drootrel.build=build-cdcfp-x86 \
    -Dgluegen.cpptasks.detected.os=true \
    -DisUnix=true \
    -DisLinux=true \
    -DisLinuxX86=true \
    -DisX11=true \
    $* 2>&1 | tee make.gluegen.cdcfp.linux-x86.log
