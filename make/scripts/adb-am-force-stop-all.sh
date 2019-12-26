#! /bin/sh

echo jogamp process running
adb $* shell ps | grep jogamp

adb $* shell am force-stop jogamp.android.launcher
adb $* shell am force-stop com.jogamp.common
adb $* shell am force-stop com.jogamp.openal
adb $* shell am force-stop com.jogamp.opengl
adb $* shell am force-stop com.jogamp.opencl
adb $* shell am force-stop com.jogamp.openal.test
adb $* shell am force-stop com.jogamp.opengl.test
adb $* shell am force-stop com.jogamp.opencl.test

