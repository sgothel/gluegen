REM scripts\java-win64.bat com.jogamp.common.GlueGenVersion 
REM scripts\java-win64.bat com.jogamp.common.util.TestVersionInfo
REM scripts\java-win64.bat com.jogamp.gluegen.test.junit.generation.Test1p1JavaEmitter
REM scripts\java-win64.bat com.jogamp.common.os.TestElfReader01

REM scripts\java-win64.bat com.jogamp.common.util.TestIOUtil01 2>&1 | tee -a $LOG
REM scripts\java-win64.bat com.jogamp.common.util.TestIOUtilURICompose 2>&1 | tee -a $LOG
scripts\java-win64.bat com.jogamp.common.util.TestIOUtilURIHandling 2>&1 | tee -a $LOG
REM scripts\java-win64.bat com.jogamp.common.net.TestUrisWithAssetHandler 2>&1 | tee -a $LOG
REM scripts\java-win64.bat com.jogamp.common.net.TestURIQueryProps 2>&1 | tee -a $LOG

