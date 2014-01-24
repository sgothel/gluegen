#! /bin/bash

#D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache -Djogamp.debug.IOUtil"
#D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup"
#D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup -Djogl.debug=all"

#T_CLASS="com.jogamp.opengl.JoglVersion"
#T_CLASS="com.jogamp.newt.opengl.GLWindow"
T_CLASS="com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT -time 2000"

#
# Just to run some tests: jogl-test.jar and junit.jar
#
java -cp output/jogl-fat.jar:output/jogl-test.jar:output/junit.jar $D_ARGS $T_CLASS 2>&1 | tee run-fat.log
