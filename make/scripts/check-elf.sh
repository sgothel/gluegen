#! /bin/bash

SDIR=`dirname $0`
RDIR=$SDIR/../../..

function check_arm_elf_sub() {

  echo $1
  echo
  readelf -A $1
}
function check_arm_elf() {

  echo "============================================================"
  check_arm_elf_sub $1
  echo "============================================================"
}
function check_jogl_arm_elf() {
  echo "============================================================"
  echo JOGL $1
  echo "------------------------------------------------------------"
  check_arm_elf_sub $1/libjogl_desktop.so
  echo "------------------------------------------------------------"
  check_arm_elf_sub $1/libjogl_mobile.so
  echo "------------------------------------------------------------"
  check_arm_elf_sub $1/libnativewindow_awt.so
  echo "------------------------------------------------------------"
  check_arm_elf_sub $1/libnativewindow_x11.so
  echo "------------------------------------------------------------"
  check_arm_elf_sub $1/libnewt.so
  echo "============================================================"
}

echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
echo "Test Android ARMv6 -> ARMv6"
check_arm_elf $RDIR/gluegen/build-android-armv6/obj/libgluegen-rt.so 
check_arm_elf $RDIR/joal/build-android-armv6/obj/libopenal.so 
check_jogl_arm_elf $RDIR/jogl/build-android-armv6/lib
echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"

echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
echo "Test Linux ARMv6 -> ARMv5te"
check_arm_elf $RDIR/gluegen/build-linux-armv6/obj/libgluegen-rt.so 
check_arm_elf $RDIR/joal/build-linux-armv6/obj/libopenal.so 
check_arm_elf $RDIR/joal/build-linux-armv6/obj/libjoal.so 
check_jogl_arm_elf $RDIR/jogl/build-linux-armv6/lib
echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"

echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
echo "Test Android ARMv6 Hard-Float -> ARMv6 Hard-Float"
check_arm_elf $RDIR/gluegen/build-linux-armv6hf/obj/libgluegen-rt.so 
check_arm_elf $RDIR/joal/build-linux-armv6hf/obj/libopenal.so 
check_arm_elf $RDIR/joal/build-linux-armv6hf/obj/libjoal.so 
check_jogl_arm_elf $RDIR/jogl/build-linux-armv6hf/lib
echo "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++"
