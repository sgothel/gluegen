#! /bin/sh

if [ -e ../../setenv-build-jogl-x86.sh ] ; then
  . ../../setenv-build-jogl-x86.sh
fi


#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxX86=true \
#    -DisX11=true \

ant \
    -Dgluegen-cpptasks.file=`pwd`/lib/gluegen-cpptasks-linux-32bit.xml \
    -Drootrel.build=build-x86 \
    -Dos.arch=x86 \
    $* 2>&1 | tee make.gluegen.all.linux-x86.log
