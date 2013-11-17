#! /bin/bash

BDIR=$1
shift

SDIR=`dirname $0`
RDIR=$SDIR/../../..

function check_glibc() {

  echo "------------------------------------------------------------"
  echo $1 via objdump
  echo
  objdump -T $1 | grep GLIBC | awk ' { print $5 " " $6 } ' | sort
  echo "------------------------------------------------------------"
}

check_glibc $RDIR/gluegen/$BDIR/obj/libgluegen-rt.so 
check_glibc $RDIR/joal/$BDIR/obj/libopenal.so 
check_glibc $RDIR/joal/$BDIR/obj/libjoal.so 
check_glibc $RDIR/jogl/$BDIR/nativewindow/obj/libnativewindow_awt.so
check_glibc $RDIR/jogl/$BDIR/nativewindow/obj/libnativewindow_x11.so
check_glibc $RDIR/jogl/$BDIR/jogl/obj/libjogl_desktop.so
check_glibc $RDIR/jogl/$BDIR/jogl/obj/libjogl_mobile.so
check_glibc $RDIR/jogl/$BDIR/jogl/obj/libjogl_cg.so
check_glibc $RDIR/jogl/$BDIR/newt/obj/libnewt.so
