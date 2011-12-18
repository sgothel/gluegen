#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/setenv-build-jogl-x86.sh ] ; then
    . $SDIR/setenv-build-jogl-x86.sh
fi

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxAMD64=true \
#    -DisX11=true \

# BUILD_ARCHIVE=true \
ant \
    -Djavacdebuglevel="source,lines,vars" \
    -Drootrel.build=build-solaris-x86 \
    $* 2>&1 | tee make.gluegen.all.solaris-x86.log
