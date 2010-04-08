#! /bin/sh

if [ -e /devtools/etc/profile.ant ] ; then
    . /devtools/etc/profile.ant
fi

#    -Dc.compiler.debug=true 

ant -v \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.gluegen.all.macosx.log
