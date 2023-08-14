# Loading a MacOS Native Library's Dependencies 
Assume we have `libBindingtest1p1.dylib`, which links to `libtest1.dylib`,
i.e. requires the OS native library to load `libtest1.dylib` to resolve symbols.

Usually we just se `DYLD_LIBRARY_PATH` including the path where `libtest1.dylib`
is located and we are good to go.

## Just use dynamic loading via GlueGen's ProcAddressTable
Note, the above problem does not occur when using GlueGen's ProcAddressTable,
i.e. loading the underlying tool library `libtest2.dylib` w/ dlopen
and passing all symbols to the JNI library `libBindingtest1p2.dylib`.

## Can't pass `DYLD_LIBRARY_PATH` to `/usr/bin/java`
This is supposed to be related to MacOS's `System Integrity Protect (SIP)`.

## Workaround inability to pass `DYLD_LIBRARY_PATH` to `/usr/bin/java`

### Use explicit OpenJDK/Temurin JDK `java` executable path

Interestingly passing `DYLD_LIBRARY_PATH` works using the 
explicit `java` executable path, set by default as follows:

```
# MacOS: Select JVM path to allow DYLD_LIBRARY_PATH
JAVA_HOME=`/usr/libexec/java_home -version 17`
PATH=$JAVA_HOME/bin:$PATH
export JAVA_HOME PATH
```        


### Using ``@loader_path` within dependent library
Set location of referenced library `libtest1.dylib` to same path of dependent library `libBindingtest1p1.dylib`
using `@loader_path`.
```
cd build-macosx/test/build/natives/
otool -L libBindingtest1p1.dylib 
install_name_tool -change libtest1.dylib @loader_path/libtest1.dylib libBindingtest1p1.dylib
otool -L libBindingtest1p1.dylib 
```

Further we could try `@executable_path` and `@rpath`.

See [An alternative to macOS's DYLD_LIBRARY_PATH](https://www.joyfulbikeshedding.com/blog/2021-01-13-alternative-to-macos-dyld-library-path.html).
