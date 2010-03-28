set CVM_HOME=c:\cvm

set JAR_DIR=jogl\lib
set LIB_DIR=jogl\lib

%CVM_HOME%\bin\cvm "-Djava.awt.headless=true" "-Dsun.boot.library.path=%LIB_DIR%" "-Xbootclasspath/a:gluegen\classes-cdc" com.jogamp.gluegen.test.TestPointerBufferEndian
%CVM_HOME%\bin\cvm "-Djava.awt.headless=true" "-Dsun.boot.library.path=%LIB_DIR%" "-Xbootclasspath/a:gluegen\classes-cdc" com.jogamp.gluegen.test.TestStructAccessorEndian

