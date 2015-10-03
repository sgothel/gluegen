#! /bin/bash

builddir="$1"
shift

if [ -z "$builddir" ] ; then 
    echo Usage $0 build-dir
    exit 1
fi

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

GLUEGEN_ROOT=`dirname $builddir`
ROOTREL_BUILD=`basename $builddir`

X_ARGS="-Drootrel.build=$ROOTREL_BUILD -Dgluegen.root=$GLUEGEN_ROOT"
#D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup"
#D_ARGS="-Djogamp.debug.TraceLock"
#D_ARGS="-Djogamp.debug.Platform -Djogamp.debug.NativeLibrary"
#D_ARGS="-Djogamp.debug.JarUtil"
#D_ARGS="-Djogamp.debug.TempJarCache"
#D_ARGS="-Djogamp.debug.TempFileCache"
#D_ARGS="-Djogamp.debug.IOUtil -Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djava.io.tmpdir=/run/tmp"
#D_ARGS="-Djogamp.debug.IOUtil -Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache"
#D_ARGS="-Djogamp.debug.IOUtil -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache -Djogamp.debug.Uri -Djogamp.debug.Uri.ShowFix"
#D_ARGS="-Djogamp.debug.Uri -Djogamp.debug.Uri.ShowFix"
#D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.gluegen.UseTempJarCache=false"
#D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempJarCache"
#D_ARGS="-Djogamp.debug.JNILibLoader"
#D_ARGS="-Djogamp.debug.JNILibLoader.Perf"
#D_ARGS="-Djogamp.debug.Lock"
#D_ARGS="-Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock"
#D_ARGS="-Djogamp.debug.Lock.TraceLock"
#D_ARGS="-Djogamp.debug.IOUtil -Djogamp.debug.IOUtil.Exe -Djogamp.debug.IOUtil.Exe.NoStream"
#D_ARGS="-Djogamp.debug.IOUtil -Djogamp.debug.IOUtil.Exe"
#D_ARGS="-Djogamp.debug.ByteBufferInputStream"
#D_ARGS="-Djogamp.debug.Bitstream"
#D_ARGS="-Djogamp.debug=all"
#D_ARGS="-Djogamp.debug.Logging"

function onetest() {
    #USE_CLASSPATH=lib/junit.jar:$ANT_JARS:lib/semantic-versioning/semver.jar:"$builddir"/../make/lib/TestJarsInJar.jar:"$builddir"/gluegen-rt.jar:"$builddir"/gluegen.jar:"$builddir"/gluegen-test-util.jar:"$builddir"/test/build/gluegen-test.jar
    USE_CLASSPATH=lib/junit.jar:$ANT_JARS:lib/semantic-versioning/semver.jar:"$builddir"/../make/lib/TestJarsInJar.jar:"$builddir"/gluegen-rt.jar:"$builddir"/gluegen.jar:"$builddir"/gluegen-test-util.jar:"$builddir"/test/build/gluegen-test.jar:"$builddir"/gluegen-rt-natives.jar
    #USE_CLASSPATH=lib/junit.jar:$ANT_JARS:lib/semantic-versioning/semver.jar:"$builddir"/../make/lib/TestJarsInJar.jar:"$builddir"/gluegen-rt-alt.jar:"$builddir"/gluegen.jar:"$builddir"/gluegen-test-util.jar:"$builddir"/test/build/gluegen-test.jar
    libspath="$builddir"/test/build/natives
    #USE_CLASSPATH=lib/junit.jar:$ANT_JARS:"$builddir"/../make/lib/TestJarsInJar.jar:"$builddir"/classes:"$builddir"/test/build/classes
    #libspath="$builddir"/obj:"$builddir"/test/build/natives:
    LD_LIBRARY_PATH=$libspath:$LD_LIBRARY_PATH
    DYLD_LIBRARY_PATH=$LD_LIBRARY_PATH
    export LD_LIBRARY_PATH DYLD_LIBRARY_PATH
    echo LD_LIBRARY_PATH $LD_LIBRARY_PATH
    echo USE_CLASSPATH $USE_CLASSPATH
    which java
    #echo java -cp $USE_CLASSPATH $X_ARGS $D_ARGS -Djava.library.path=$libspath $*
    #java -cp $USE_CLASSPATH $X_ARGS $D_ARGS -Djava.library.path="$libspath" $*
    echo java -cp "$USE_CLASSPATH" $X_ARGS $D_ARGS $*
    java -cp "$USE_CLASSPATH" $X_ARGS $D_ARGS $*
    #j3 -cp "$USE_CLASSPATH" $X_ARGS $D_ARGS $*
    echo
}
#
#onetest com.jogamp.common.GlueGenVersion 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestSystemPropsAndEnvs 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestVersionInfo 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestVersionNumber 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestVersionSemantics 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestIteratorIndexCORE 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestRecursiveLock01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestRecursiveThreadGroupLock01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestSingletonServerSocket00 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestSingletonServerSocket01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.locks.TestSingletonServerSocket02 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestFloatStack01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestIntegerStack01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestArrayHashSet01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestArrayHashMap01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.IntIntHashMapTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.IntObjectHashMapTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.LongIntHashMapTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestPlatform01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestRunnableTask01 2>&1 | tee -a $LOG
onetest com.jogamp.common.util.TestIOUtil01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestTempJarCache 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestJarUtil 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestValueConversion 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestSyncRingBuffer01 $*
#onetest com.jogamp.common.util.TestLFRingBuffer01 $*
#onetest com.jogamp.common.util.TestBitfield00 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestBitstream00 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestBitstream01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestBitstream02 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestBitstream03 2>&1 | tee -a $LOG
#onetest com.jogamp.common.util.TestBitstream04 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.TestUrisWithAssetHandler 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.TestUriQueryProps 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.TestUri01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.TestUri02Composing 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.TestUri03Resolving 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.TestUri99LaunchOnReservedCharPathBug908 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.AssetURLConnectionUnregisteredTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.net.AssetURLConnectionRegisteredTest 2>&1 | tee -a $LOG
#onetest com.jogamp.junit.sec.TestSecIOUtil01 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.BuffersTest 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.TestBuffersFloatDoubleConversion 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.TestPointerBufferEndian 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.TestStructAccessorEndian 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.TestByteBufferInputStream 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.TestByteBufferOutputStream 2>&1 | tee -a $LOG
#onetest com.jogamp.common.nio.TestByteBufferCopyStream 2>&1 | tee -a $LOG
#onetest com.jogamp.common.os.TestElfReader01 $* 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.junit.internals.TestType 2>&1 | tee -a $LOG

#onetest com.jogamp.gluegen.test.junit.generation.PCPPTest 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.jcpp.IncludeAbsoluteTest 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.jcpp.CppReaderTest 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.jcpp.TokenPastingWhitespaceTest 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.jcpp.PreprocessorTest 2>&1 | tee -a $LOG

#onetest com.jogamp.gluegen.test.junit.generation.Test1p1JavaEmitter 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.junit.generation.Test1p2ProcAddressEmitter 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.junit.generation.Test1p2LoadJNIAndImplLib 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.junit.structgen.TestStructGen01 2>&1 | tee -a $LOG
#onetest com.jogamp.gluegen.test.junit.structgen.TestStructGen02 2>&1 | tee -a $LOG

