#! /bin/sh

builddir=$1
shift

if [ -z "$builddir" ] ; then 
    echo Usage $0 build-dir
    exit 1
fi

echo com.sun.gluegen.test.TestPointerBufferEndian
java -Djava.library.path=$builddir/obj -classpath $builddir/classes  com.sun.gluegen.test.TestPointerBufferEndian
echo
echo com.sun.gluegen.test.TestStructAccessorEndian
java -Djava.library.path=$builddir/obj -classpath $builddir/classes  com.sun.gluegen.test.TestStructAccessorEndian
echo
