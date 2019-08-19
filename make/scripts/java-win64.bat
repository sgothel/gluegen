
set BLD_SUB=build-win64

set J2RE_HOME=c:\jre-11.0.4+11_x64
set JAVA_HOME=c:\jdk-11.0.4+11_x64
set ANT_PATH=C:\apache-ant-1.10.5

set BLD_DIR=..\%BLD_SUB%
REM set LIB_DIR=..\%BLD_SUB%\obj;..\%BLD_SUB%\test\build\natives
set LIB_DIR=..\%BLD_SUB%\test\build\natives

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw64\bin;%LIB_DIR%;%PATH%
REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw64\bin;%PATH%

set CP_ALL=.;lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;lib/semantic-versioning/semver.jar;lib\TestJarsInJar.jar;%BLD_DIR%\gluegen-rt.jar;%BLD_DIR%\gluegen.jar;%BLD_DIR%\gluegen-test-util.jar;%BLD_DIR%\test\build\gluegen-test.jar

echo CP_ALL %CP_ALL%

set X_ARGS="-Drootrel.build=%BLD_SUB%" "-Dgluegen.root=.."
REM set D_ARGS="-Djogamp.debug.IOUtil" "-Djogamp.debug.JNILibLoader" "-Djogamp.debug.TempFileCache" "-Djogamp.debug.JarUtil" "-Djogamp.debug.TempJarCache"
REM set D_ARGS="-Djogamp.debug.Platform" "-Djogamp.debug.NativeLibrary" "-Djogamp.debug.IOUtil"
REM set D_ARGS="-Djogamp.debug.IOUtil" "-Djogamp.debug.IOUtil.Exe" "-Djogamp.debug.IOUtil.Exe.NoStream"
REM set D_ARGS="-Djogamp.debug.IOUtil" "-Djogamp.debug.TempFileCache" "-Djogamp.debug.TempJarCache" "-Djogamp.debug.IOUtil.Exe" "-Djogamp.gluegen.UseNativeExeFile=true" "-Djava.io.tmpdir=c:\temp_noexec"
REM set D_ARGS="-Djogamp.debug.IOUtil" "-Djogamp.debug.TempFileCache" "-Djogamp.debug.TempJarCache" "-Djava.io.tmpdir=c:\temp_noexec"
REM set D_ARGS="-Djogamp.debug=all"

%J2RE_HOME%\bin\java -classpath %CP_ALL% %X_ARGS% %D_ARGS% "-Djava.library.path=%LIB_DIR%" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win64.log 2>&1
REM %J2RE_HOME%\bin\java -classpath %CP_ALL% %X_ARGS% %D_ARGS% "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win64.log 2>&1

