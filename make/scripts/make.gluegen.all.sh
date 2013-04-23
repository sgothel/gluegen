#! /bin/sh

SDIR=`dirname $0` 

$SDIR/make.gluegen.all.linux-armv6-cross.sh \
&& $SDIR/make.gluegen.all.linux-armv6hf-cross.sh \
&& $SDIR/make.gluegen.all.linux-x86_64.sh \
&& $SDIR/make.gluegen.all.linux-x86.sh \
&& $SDIR/make.gluegen.all.android-armv6-cross.sh \
