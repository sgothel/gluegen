#! /bin/bash

RDIR=`pwd`

rm -rf jar
rm -f gluegen-test.jar

mkdir jar
cp ../../build/*jar jar/
cp ../../build/test/build/gluegen-test.jar .

/opt-linux-x86_64/jdk1.6.0_35/bin/java \
    -Djogamp.debug.IOUtil -Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache \
    -Djava.security.policy=$RDIR/java.policy.applet \
    -Dfile.encoding=UTF-8 \
    sun.applet.AppletViewer $RDIR/applet01.html

