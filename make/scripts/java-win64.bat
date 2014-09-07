
set BLD_SUB=build-win64
set J2RE_HOME=c:\jre1.7.0_67_x64
set JAVA_HOME=c:\jdk1.7.0_67_x64
set ANT_PATH=C:\apache-ant-1.9.4

set BLD_DIR=..\%BLD_SUB%
REM set LIB_DIR=..\%BLD_SUB%\obj;..\%BLD_SUB%\test\build\natives
set LIB_DIR=..\%BLD_SUB%\test\build\natives

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%LIB_DIR%;%PATH%

set CP_ALL=.;lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;lib/semantic-versioning/semver.jar;%BLD_DIR%\gluegen-rt.jar;%BLD_DIR%\gluegen.jar;%BLD_DIR%\gluegen-test-util.jar;%BLD_DIR%\test\build\gluegen-test.jar

echo CP_ALL %CP_ALL%

set D_ARGS="-Djogamp.debug.IOUtil" "-Djogamp.debug.JNILibLoader" "-Djogamp.debug.TempFileCache" "-Djogamp.debug.JarUtil" "-Djogamp.debug.TempJarCache"
REM set D_ARGS="-Djogamp.debug.Platform" "-Djogamp.debug.NativeLibrary" "-Djogamp.debug.IOUtil"
REM set D_ARGS="-Djogamp.debug.IOUtil"
REM set D_ARGS="-Djogamp.debug=all"

%J2RE_HOME%\bin\java -classpath %CP_ALL% %D_ARGS% "-Djava.library.path=%LIB_DIR%" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win64.log 2>&1

