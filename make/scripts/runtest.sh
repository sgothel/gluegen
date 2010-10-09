#! /bin/bash

builddir=$1
shift

if [ -z "$builddir" ] ; then 
    echo Usage $0 build-dir
    exit 1
fi

function onetest() {
    clazz=$1
    shift
    echo $clazz
    java -Djava.library.path=$builddir/obj:$builddir/test/build/natives -classpath lib/junit.jar:$builddir/classes:$builddir/test/build/classes $clazz
    echo
}

onetest com.jogamp.common.util.TestRecursiveToolkitLock
#onetest com.jogamp.gluegen.test.TestPointerBufferEndian
#onetest com.jogamp.gluegen.test.TestStructAccessorEndian
