#! /bin/sh

SDIR=`dirname $0` 

if [ -e $SDIR/setenv-build-jogl-x86_64.java7.sh ] ; then
    . $SDIR/setenv-build-jogl-x86_64.java7.sh
fi

#    -Dc.compiler.debug=true 
#    -Dgluegen.cpptasks.detected.os=true \
#    -DisUnix=true \
#    -DisLinux=true \
#    -DisLinuxAMD64=true \
#    -DisX11=true \
#
#    -Dtarget.sourcelevel=1.6 \
#    -Dtarget.targetlevel=1.6 \
#    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \

# BUILD_ARCHIVE=true \
ant \
    -Dc.compiler.debug=true  \
    -Djavacdebuglevel="source,lines,vars" \
    -Drootrel.build=build-x86_64.java7 \
    $* 2>&1 | tee make.gluegen.all.linux-x86_64.java7.log
