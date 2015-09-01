
set SMB_ROOT=\\risa.goethel.localnet\deployment\test\jogamp

set BLD_SUB=build-win32
set J2RE_HOME=c:\jre1.8.0_60_x32
set JAVA_HOME=c:\jdk1.8.0_60_x32
set ANT_PATH=C:\apache-ant-1.9.4

set PROJECT_ROOT=%SMB_ROOT%\gluegen
set BLD_DIR=%PROJECT_ROOT%\%BLD_SUB%

set PATH=%JAVA_HOME%\bin;%ANT_PATH%\bin;%PATH%

set D_ARGS="-Djogamp.debug=all"

set LIB_DIR=

set CP_ALL=.;%BLD_DIR%\gluegen-rt.jar;%PROJECT_ROOT%\make\lib\junit.jar;%ANT_PATH%\lib\ant.jar;%ANT_PATH%\lib\ant-junit.jar

echo CP_ALL %CP_ALL%

%J2RE_HOME%\bin\java -classpath %CP_ALL% %D_ARGS% %X_ARGS% com.jogamp.common.GlueGenVersion > java-win.log 2>&1
tail java-win.log

