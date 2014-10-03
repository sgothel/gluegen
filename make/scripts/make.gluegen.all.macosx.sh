#! /bin/sh

if [ -e /opt-share/etc/profile.ant ] ; then
    . /opt-share/etc/profile.ant
fi

#    -Dc.compiler.debug=true 
#
#    -Dtarget.sourcelevel=1.6 \
#    -Dtarget.targetlevel=1.6 \
#    -Dtarget.rt.jar=/opt-share/jre1.6.0_30/lib/rt.jar \

# Force OSX SDK 10.6, if desired
# export SDKROOT=macosx10.6

JAVA_HOME=`/usr/libexec/java_home -version 1.8`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH

export SOURCE_LEVEL=1.6
export TARGET_LEVEL=1.6
export TARGET_RT_JAR=/opt-share/jre1.6.0_30/lib/rt.jar

#export JOGAMP_JAR_CODEBASE="Codebase: *.jogamp.org"
export JOGAMP_JAR_CODEBASE="Codebase: *.goethel.localnet"

ant \
    -Drootrel.build=build-macosx \
    $* 2>&1 | tee make.gluegen.all.macosx.log
