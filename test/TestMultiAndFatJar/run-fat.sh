
D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache"
#D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup"
#D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup -Djogl.debug=all"

#T_CLASS="com.jogamp.opengl.JoglVersion"
T_CLASS="com.jogamp.newt.opengl.GLWindow"

java -cp jogl-fat.jar $D_ARGS $T_CLASS 2>&1 | tee run-fat.log
