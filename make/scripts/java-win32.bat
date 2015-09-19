
set BLD_SUB=build-win32
set J2RE_HOME=c:\jre1.8.0_60_x32
set JAVA_HOME=c:\jdk1.8.0_60_x32
set ANT_PATH=C:\apache-ant-1.9.4

set BLD_DIR=..\%BLD_SUB%
REM set LIB_DIR=..\%BLD_SUB%\obj;..\%BLD_SUB%\test\build\natives
REM set LIB_DIR=..\%BLD_SUB%\test\build\natives

REM set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%LIB_DIR%;%PATH%
set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%PATH%

set CP_ALL=.;lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar;lib/semantic-versioning/semver.jar;%BLD_DIR%\gluegen-rt.jar;%BLD_DIR%\gluegen.jar;%BLD_DIR%\gluegen-test-util.jar;%BLD_DIR%\test\build\gluegen-test.jar

echo CP_ALL %CP_ALL%

set X_ARGS="-Drootrel.build=%BLD_SUB%" "-Dgluegen.root=.."
REM set D_ARGS="-Djogamp.debug.Platform" "-Djogamp.debug.NativeLibrary"
set D_ARGS="-Djogamp.debug.IOUtil" "-Djogamp.debug.IOUtil.Exe"
REM set D_ARGS="-Djogamp.debug=all"

REM %J2RE_HOME%\bin\java -classpath %CP_ALL% %X_ARGS% %D_ARGS% "-Djava.library.path=%LIB_DIR%" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win32.log 2>&1
%J2RE_HOME%\bin\java -classpath %CP_ALL% %X_ARGS% %D_ARGS% "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win32.log 2>&1
