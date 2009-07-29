#! /bin/sh

CVM=$1
shift

builddir=$1
shift

if [ ! -x "$CVM" -o -z "$builddir" ] ; then 
    echo Usage $0 CVM-Binary build-dir
    exit 1
fi

echo com.sun.gluegen.test.TestPointerBufferEndian
$CVM -Dsun.boot.library.path=$builddir/obj -Xbootclasspath/a:$builddir/classes-cdc  com.sun.gluegen.test.TestPointerBufferEndian
echo
echo com.sun.gluegen.test.TestStructAccessorEndian
$CVM -Dsun.boot.library.path=$builddir/obj -Xbootclasspath/a:$builddir/classes-cdc  com.sun.gluegen.test.TestStructAccessorEndian
echo
