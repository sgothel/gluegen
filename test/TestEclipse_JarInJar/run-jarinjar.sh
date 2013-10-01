
THIS_DIR=`pwd`

D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache"
#D_ARGS="-Djogamp.debug.ProcAddressHelper -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup"
#D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JarUtil -Djogamp.debug.TempJarCache -Djogamp.debug.NativeLibrary -Djogamp.debug.NativeLibrary.Lookup -Djogl.debug=all"

java $D_ARGS -jar lala02.jar 2>&1 | tee run-jarinjar.log
