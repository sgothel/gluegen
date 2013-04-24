#! /bin/bash

SDIR=`dirname $0`
XDIR=$SDIR/../../make/lib/toolchain
RDIR=$SDIR/../../..

function check_glibc() {
  OBJDUMP=$1/bin/objdump

  echo "------------------------------------------------------------"
  echo $2 via $OBJDUMP
  echo
  $OBJDUMP -T $2 | grep GLIBC | awk ' { print $5 " " $6 } ' | sort
  echo "------------------------------------------------------------"
}

check_glibc $XDIR/armsf-linux-gnueabi $SDIR/../ $RDIR/gluegen/build-linux-armv6/obj/libgluegen-rt.so 
check_glibc $XDIR/armhf-linux-gnueabi $RDIR/gluegen/build-linux-armv6hf/obj/libgluegen-rt.so 
check_glibc $XDIR/armsf-linux-gnueabi $RDIR/joal/build-linux-armv6/obj/libopenal.so 
check_glibc $XDIR/armsf-linux-gnueabi $RDIR/joal/build-linux-armv6/obj/libjoal.so 
check_glibc $XDIR/armhf-linux-gnueabi $RDIR/joal/build-linux-armv6hf/obj/libopenal.so 
check_glibc $XDIR/armhf-linux-gnueabi $RDIR/joal/build-linux-armv6hf/obj/libjoal.so 
