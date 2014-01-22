
set BLD_SUB=build-win32
set J2RE_HOME=c:\jre1.7.0_45_x32
set JAVA_HOME=c:\jdk1.7.0_45_x32
set ANT_PATH=C:\apache-ant-1.8.2

set BLD_DIR=..\%BLD_SUB%
set LIB_DIR=..\%BLD_SUB%\obj;..\%BLD_SUB%\test\build\natives

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw\bin;%LIB_DIR%;%PATH%

set CP_ALL=.;%BLD_DIR%\gluegen-rt.jar;%BLD_DIR%\test\build\gluegen-test.jar;lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar

echo CP_ALL %CP_ALL%

set D_ARGS="-Djogamp.debug.IOUtil"
REM set D_ARGS="-Djogamp.debug=all"

%J2RE_HOME%\bin\java -classpath %CP_ALL% %D_ARGS% "-Djava.library.path=%LIB_DIR%" "-Dsun.java2d.noddraw=true" "-Dsun.awt.noerasebackground=true" %1 %2 %3 %4 %5 %6 %7 %8 %9 > java-win32.log 2>&1
