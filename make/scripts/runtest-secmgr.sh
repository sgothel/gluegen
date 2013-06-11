#! /bin/bash

TDIR=`pwd`
SDIR=`dirname $0` #scripts
cd $SDIR
SDIR=`pwd` #scripts
cd $TDIR
MDIR=`dirname $SDIR` #make
RDIR=`dirname $MDIR` #gluegen

builddir=$1
shift

if [ -z "$builddir" ] ; then 
    echo Usage $0 build-dir
    exit 1
fi

BDIR=$RDIR/`basename $builddir`

if [ -e /opt-share/apache-ant ] ; then
    ANT_PATH=/opt-share/apache-ant
    PATH=$ANT_PATH/bin:$PATH
    export ANT_PATH
fi
if [ -z "$ANT_PATH" ] ; then
    TMP_ANT_PATH=$(dirname `which ant`)/..
    if [ -e $TMP_ANT_PATH/lib/ant.jar ] ; then
        ANT_PATH=$TMP_ANT_PATH
        export ANT_PATH
        echo autosetting ANT_PATH to $ANT_PATH
    fi
fi
if [ -z "$ANT_PATH" ] ; then
    if [ -e /usr/share/ant/bin/ant -a -e /usr/share/ant/lib/ant.jar ] ; then
        ANT_PATH=/usr/share/ant
        export ANT_PATH
        echo autosetting ANT_PATH to $ANT_PATH
    fi
fi
if [ -z "$ANT_PATH" ] ; then
    echo ANT_PATH does not exist, set it
    print_usage
    exit
fi

ANT_JARS=$ANT_PATH/lib/ant.jar:$ANT_PATH/lib/ant-junit.jar:$ANT_PATH/lib/ant-launcher.jar

LOG=runtest.log
rm -f $LOG

#D_ARGS="-Djogamp.debug.ProcAddressHelper=true -Djogamp.debug.NativeLibrary=true"
#D_ARGS="-Djogamp.debug.TraceLock"
#D_ARGS="-Djogamp.debug.Platform -Djogamp.debug.NativeLibrary"
#D_ARGS="-Djogamp.debug.JarUtil"
#D_ARGS="-Djogamp.debug.TempJarCache"
#D_ARGS="-Djogamp.debug.TempFileCache"
#D_ARGS="-Djogamp.debug.IOUtil -Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djava.io.tmpdir=/run/tmp"
#D_ARGS="-Djogamp.debug.IOUtil -Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache"
#D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.gluegen.UseTempJarCache=false"
#D_ARGS="-Djogamp.debug.JNILibLoader"
#D_ARGS="-Djogamp.debug.Lock"
#D_ARGS="-Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock"
#D_ARGS="-Djogamp.debug.Lock.TraceLock"
#D_ARGS="-Djogamp.debug.IOUtil"
D_ARGS="-Djogamp.debug=all"

SPFILE=$BDIR/java.policy.secure

echo "grant codeBase \"file:$BDIR/*\" {" > $SPFILE
echo "   permission java.security.AllPermission;" >> $SPFILE
echo "};" >> $SPFILE

function onetest() {
    CLASSPATH=lib/junit.jar:$ANT_JARS:$RDIR/make/lib/TestJarsInJar.jar:$BDIR/gluegen-rt.jar:$BDIR/test/build/gluegen-test.jar
    libspath=$BDIR/test/build/natives
    echo LD_LIBRARY_PATH $LD_LIBRARY_PATH
    echo CLASSPATH $CLASSPATH
    which java
    echo java -Djava.security.policy=$SPFILE -Dfile.encoding=UTF-8 -cp $CLASSPATH $D_ARGS $*
    java -Djava.security.manager -Djava.security.policy=$SPFILE -Dfile.encoding=UTF-8 -cp $CLASSPATH $D_ARGS $*
    echo
}
#
#onetest com.jogamp.common.GlueGenVersion 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestSystemPropsAndEnvs 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestVersionInfo 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestVersionNumber 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestIteratorIndexCORE 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestRecursiveLock01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestRecursiveThreadGroupLock01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestSingletonServerSocket00 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestSingletonServerSocket01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestSingletonServerSocket02 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestFloatStack01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestIntegerStack01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestArrayHashSet01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.IntIntHashMapTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.IntObjectHashMapTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.LongIntHashMapTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.TestBuffersFloatDoubleConversion 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.PCPPTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.TestPointerBufferEndian 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.TestStructAccessorEndian 2>&1 | tee -a $LOG
#onetest com.jogamp.common.os.TestElfReader01 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.junit.generation.Test1p1JavaEmitter 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.junit.generation.Test1p2ProcAddressEmitter 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestPlatform01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestRunnableTask01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestIOUtil01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestTempJarCache 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestJarUtil 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestValueConversion 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.AssetURLConnectionUnregisteredTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.AssetURLConnectionRegisteredTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.URLCompositionTest 2>&1 | tee -a $LOG
onetest com.jogamp.junit.sec.TestSecIOUtil01 2>&1 | tee -a $LOG
