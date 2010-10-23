#! /bin/bash

builddir=$1
shift

if [ -z "$builddir" ] ; then 
    echo Usage $0 build-dir
    exit 1
fi

LOG=runtest.log
rm -f $LOG

#D_ARGS="-Djogamp.debug.ProcAddressHelper=true -Djogamp.debug.NativeLibrary=true"
D_ARGS="-Djogamp.debug.TraceLock"

function onetest() {
    clazz=$1
    shift
    echo $clazz
    java $D_ARGS -Djava.library.path=$builddir/obj:$builddir/test/build/natives -classpath lib/junit.jar:$builddir/classes:$builddir/test/build/classes $clazz
    echo
}

#onetest com.jogamp.common.util.TestIteratorIndexCORE 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestRecursiveLock01 2>&1 | tee -a $LOG
onetest com.jogamp.common.util.TestArrayHashSet01 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.TestPointerBufferEndian 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.TestStructAccessorEndian 2>&1 | tee -a $LOG
