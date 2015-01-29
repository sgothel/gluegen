#!/bin/sh

TDIR=`pwd`

dump_version() {
    echo -n "$1: "
    javap -v $1 | grep 'major version'
}

dump_versions() {
    cd $1
    #dump_version jogamp.common.Debug
    javap -v `find . -name '*.class'` | grep -e '^Classfile' -e 'major version'
    #for i in `find . -name '*.class'` ; do 
    #  dump_version `echo $i | sed -e 's/\//./g' -e 's/\.class//g'`
    #done
    cd $TDIR
}

do_it() {
    dump_versions $1/classes
    dump_versions $1/test/build/classes
}

do_it $1 2>&1 | tee check-java-major-version.log
echo 
echo VERSIONS found:
echo
grep 'major version' check-java-major-version.log | sort -u

