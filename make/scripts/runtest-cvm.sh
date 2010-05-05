#! /bin/sh

CVM=$1
shift

builddir=$1
shift

if [ ! -x "$CVM" -o -z "$builddir" ] ; then 
    echo Usage $0 CVM-Binary build-dir
    exit 1
fi

echo com.jogamp.gluegen.test.TestPointerBufferEndian
$CVM -Dsun.boot.library.path=$builddir/obj -Xbootclasspath/a:$builddir/classes-cdc  com.jogamp.gluegen.test.TestPointerBufferEndian
echo
echo com.jogamp.gluegen.test.TestStructAccessorEndian
$CVM -Dsun.boot.library.path=$builddir/obj -Xbootclasspath/a:$builddir/classes-cdc  com.jogamp.gluegen.test.TestStructAccessorEndian
echo
