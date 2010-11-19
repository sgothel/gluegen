#! /bin/bash

builddir=$1
shift

if [ -z "$builddir" ] ; then 
    echo Usage $0 build-dir
    exit 1
fi

if [ -z "$ANT_PATH" ] ; then
    TMP_ANT_PATH=$(dirname `which ant`)/..
    if [ -e $TMP_ANT_PATH/lib/ant.jar ] ; then
        ANT_PATH=$TMP_ANT_PATH
        export ANT_PATH
        echo autosetting ANT_PATH to $ANT_PATH
    fi
fi
if [ -z "$ANT_PATH" ] ; then
    echo ANT_PATH does not exist, set it
    print_usage
    exit
fi

ANT_JARS=$ANT_PATH/lib/ant.jar:$ANT_PATH/lib/ant-junit.jar:$ANT_PATH/lib/ant-launcher.jar

LOG=runtest.log
rm -f $LOG

#D_ARGS="-Djogamp.debug.ProcAddressHelper=true -Djogamp.debug.NativeLibrary=true"
D_ARGS="-Djogamp.debug.TraceLock"

function onetest() {
    clazz=$1
    shift
    echo $clazz
    java $D_ARGS -Djava.library.path=$builddir/obj:$builddir/test/build/natives -classpath lib/junit.jar:$ANT_JARS:$builddir/gluegen-rt.jar:$builddir/gluegen.jar:$builddir/test/build/classes $clazz
    echo
}

#onetest com.jogamp.common.GlueGenVersion 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestVersionInfo 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestIteratorIndexCORE 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestRecursiveLock01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestArrayHashSet01 2>&1 | tee -a $LOG
onetest com.jogamp.common.nio.TestBuffersFloatDoubleConversion 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.PCPPTest 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.TestPointerBufferEndian 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.TestStructAccessorEndian 2>&1 | tee -a $LOG
