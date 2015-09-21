/cygdrive/c/mingw/bin/gcc -nodefaultlibs -nostdlib -s -Os -mwindows -o tiny2-win32-i386.exe tiny2.c

/cygdrive/c/mingw/bin/gcc -nodefaultlibs -nostdlib -s -Os -mwindows -o tiny-win32-i386.exe tiny.c
gzip -9 -c tiny-win32-i386.exe > tiny-win32-i386.exe.gz

#/cygdrive/c/mingw64/bin/gcc -nodefaultlibs -nostdlib -s -Os -mwindows -o tiny-win32-x86_64.exe tiny.c
#gzip -9 -c tiny-win32-x86_64.exe > tiny-win32-x86_64.exe.gz
