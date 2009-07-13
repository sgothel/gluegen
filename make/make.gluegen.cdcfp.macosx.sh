#! /bin/sh

if [ -e /devtools/etc/profile.ant ] ; then
    . /devtools/etc/profile.ant
fi

#    -Dc.compiler.debug=true 

ant -v \
    -DisCDCFP=true \
    -Drootrel.build=build-cdcfp-macosx \
    $* 2>&1 | tee make.gluegen.cdcfp.macosx-x86.log
