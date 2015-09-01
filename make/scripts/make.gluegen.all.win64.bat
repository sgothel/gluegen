set THISDIR="C:\JOGL"

set J2RE_HOME=c:\jre1.8.0_60_x64
set JAVA_HOME=c:\jdk1.8.0_60_x64
set ANT_PATH=C:\apache-ant-1.9.4

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;c:\mingw64\bin;c:\mingw\bin;%PATH%

set LIB_GEN=%THISDIR%\lib
set CLASSPATH=.;%THISDIR%\build-win64\classes
REM    -Dc.compiler.debug=true 
REM    -DuseOpenMAX=true 
REM    -DuseKD=true
REM    -Djogl.cg=1 -D-Dwindows.cg.lib=C:\Cg-2.2

set SOURCE_LEVEL=1.6
set TARGET_LEVEL=1.6
set TARGET_RT_JAR=C:\jre1.6.0_30\lib\rt.jar

REM set JOGAMP_JAR_CODEBASE=Codebase: *.jogamp.org
set JOGAMP_JAR_CODEBASE=Codebase: *.goethel.localnet

ant -Drootrel.build=build-win64 %1 %2 %3 %4 %5 %6 %7 %8 %9 > make.gluegen.all.win64.log 2>&1

