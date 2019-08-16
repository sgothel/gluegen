set THISDIR="C:\JogAmp"

set J2RE_HOME=c:\jre-11.0.4+11_x64
set JAVA_HOME=c:\jdk-11.0.4+11_x64
set ANT_PATH=C:\apache-ant-1.10.5
set GIT_PATH=C:\cygwin\bin
set SEVENZIP=C:\Program Files\7-Zip

set CMAKE_PATH=C:\cmake-2.8.10.2-win32-x86
set CMAKE_C_COMPILER=c:\mingw64\bin\gcc

set PATH=%J2RE_HOME%\bin;%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw64\bin;%CMAKE_PATH%\bin;%GIT_PATH%;%SEVENZIP%;%PATH%

set LIB_GEN=%THISDIR%\lib
set CLASSPATH=.;%THISDIR%\build-win64\classes
REM    -Dc.compiler.debug=true 
REM    -DuseOpenMAX=true 
REM    -DuseKD=true
REM    -Djogl.cg=1 -D-Dwindows.cg.lib=C:\Cg-2.2

set SOURCE_LEVEL=1.8
set TARGET_LEVEL=1.8
set TARGET_RT_JAR=C:\jre1.8.0_212\lib\rt.jar

REM set JOGAMP_JAR_CODEBASE=Codebase: *.jogamp.org
set JOGAMP_JAR_CODEBASE=Codebase: *.goethel.localnet

ant -Drootrel.build=build-win64 %1 %2 %3 %4 %5 %6 %7 %8 %9 > make.gluegen.all.win64.log 2>&1

