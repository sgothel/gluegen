#! /bin/sh

SDIR=`dirname $0` 

$SDIR/make.gluegen.all.macosx.sh && \
$SDIR/make.gluegen.all.ios.amd64.sh && \
$SDIR/make.gluegen.all.ios.arm64.sh

# $SDIR/make.gluegen.all.macosx.sh
# $SDIR/make.gluegen.all.ios.amd64.sh
# $SDIR/make.gluegen.all.ios.arm64.sh
# $SDIR/make.gluegen.all.win32.bat
# $SDIR/make.gluegen.all.win64.bat
# $SDIR/make.gluegen.all.linux-ppc64le.sh
# $SDIR/make.gluegen.all.linux-armv6hf.sh
# $SDIR/make.gluegen.all.linux-aarch64.sh
