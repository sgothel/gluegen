#! /bin/sh

builddir=$1
shift

if [ -z "$builddir" ] ; then 
    echo Usage $0 build-dir
    exit 1
fi

echo com.jogamp.gluegen.test.TestPointerBufferEndian
java -Djava.library.path=$builddir/obj -classpath $builddir/classes  com.jogamp.gluegen.test.TestPointerBufferEndian
echo
echo com.jogamp.gluegen.test.TestStructAccessorEndian
java -Djava.library.path=$builddir/obj -classpath $builddir/classes  com.jogamp.gluegen.test.TestStructAccessorEndian
echo
