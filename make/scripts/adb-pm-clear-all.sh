#! /bin/sh

echo jogamp process running
adb $* shell ps | grep jogamp

adb $* shell pm clear jogamp.android.launcher
adb $* shell pm clear com.jogamp.common
adb $* shell pm clear com.jogamp.openal
adb $* shell pm clear com.jogamp.opengl
adb $* shell pm clear com.jogamp.opencl
adb $* shell pm clear com.jogamp.openal.test
adb $* shell pm clear com.jogamp.opengl.test
adb $* shell pm clear com.jogamp.opencl.test

