# /cygdrive/c/mingw/bin/gcc -nodefaultlibs -nostdlib -s -Os -mconsole -o tiny-conso-i386.exe tiny.c

jardir=../../../build-win64

/cygdrive/c/mingw/bin/gcc -nodefaultlibs -nostdlib -s -Os -mwindows -o tiny2-win32-i386.exe tiny2.c -lUser32
java -cp "$jardir/test/build/gluegen-test.jar;$jardir/gluegen-rt.jar" com.jogamp.common.util.CustomDeflate tiny2-win32-i386.exe exe2-windows-i386.defl

/cygdrive/c/mingw64/bin/gcc -nodefaultlibs -nostdlib -s -Os -mwindows -o tiny2-win32-x86_64.exe tiny2.c -lUser32
java -cp "$jardir/test/build/gluegen-test.jar;$jardir/gluegen-rt.jar" com.jogamp.common.util.CustomDeflate tiny2-win32-x86_64.exe exe2-windows-x86_64.defl

/cygdrive/c/mingw/bin/gcc -nodefaultlibs -nostdlib -s -Os -mwindows -o tiny-win32-i386.exe tiny.c
java -cp "$jardir/test/build/gluegen-test.jar;$jardir/gluegen-rt.jar" com.jogamp.common.util.CustomDeflate tiny-win32-i386.exe exe-windows-i386.defl

/cygdrive/c/mingw64/bin/gcc -nodefaultlibs -nostdlib -s -Os -mwindows -o tiny-win32-x86_64.exe tiny.c
java -cp "$jardir/test/build/gluegen-test.jar;$jardir/gluegen-rt.jar" com.jogamp.common.util.CustomDeflate tiny-win32-x86_64.exe exe-windows-x86_64.defl
