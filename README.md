# GlueGen, Native Binding Generator for Java™

[Original document location](https://jogamp.org/cgit/gluegen.git/about/)

## Git Repository
This project's canonical repositories is hosted on [JogAmp](https://jogamp.org/cgit/gluegen.git/).

## Overview
[GlueGen](https://jogamp.org/gluegen/www/) is a compiler for function and data-structure declarations, 
generating Java™ and JNI C code offline at compile time 
and allows using native libraries within your Java™ application.

GlueGen also provides a comprehensive [runtime library](https://jogamp.org/deployment/jogamp-next/javadoc/gluegen/javadoc/) offering 
- Support for multi-arch and java code fat-jar deployment
  - Native library including JNI bundle handling and Jar file cache
  - Platform architecture information retrieval, ELF parser, alignment etc
- Enhanced NIO buffer handling for pointer, arrays, DMA mapping etc</li>
- Network Uri RFC 2396, connection and resource handler to simplify asset loading
- Bitstream, hash maps, ringbuffer, sha cumulator, reflection and threading utils
- Abstract AudioFormat and AudioSink interfaces, concurrent locks .. and more

GlueGen's compiler reads ANSI C header files
and separate configuration files which provide control over many
aspects of the glue code generation. GlueGen uses a complete ANSI C
parser and an internal representation (IR) capable of representing all
C types to represent the APIs for which it generates interfaces. It
has the ability to perform significant transformations on the IR
before glue code emission. 

GlueGen can produce native foreign function bindings to Java™ as well as
[map native data structures](doc/GlueGen_Mapping.md#struct-mapping) to be fully accessible from Java™ including 
potential calls to [embedded function pointer](doc/GlueGen_Mapping.md#struct-function-pointer-support).

GlueGen supports [registering Java™ callback methods](doc/GlueGen_Mapping.md#java-callback)
to receive asynchronous and off-thread native toolkit events,
where a generated native callback function dispatches the events to Java™.

GlueGen also supports [producing an OO-Style API mapping](doc/GlueGen_Mapping.md#oo-style-api-interface-mapping) like [JOGL's incremental OpenGL Profile API levels](https://jogamp.org/jogl/doc/uml/html/index.html).

GlueGen is capable to bind low-level APIs such as the Java™ Native Interface (JNI) and
the AWT Native Interface (JAWT) back up to the Java programming language.

Further, GlueGen supports [generating `JNI_OnLoad*(..)` for dynamic and static libraries](doc/GlueGen_Mapping.md#libraryonload-librarybasename-for-jni_onload-), also resolving off-thread `JNIEnv*` lookup.

GlueGen utilizes [JCPP](https://jogamp.org/cgit/jcpp.git/about/), migrated C preprocessor written in Java™.

GlueGen is used for the [JogAmp](https://jogamp.org) projects
[JOAL](https://jogamp.org/cgit/joal.git/about/),
[JOGL](https://jogamp.org/cgit/jogl.git/about/) and
[JOCL](https://jogamp.org/cgit/jocl.git/).

GlueGen is part of [the JogAmp project](https://jogamp.org).

**The JogAmp project needs funding and we offer [commercial support](https://jogamp.org/wiki/index.php?title=Maintainer_and_Contacts#Commercial_Support)!**<br/>
Please contact [Göthel Software (Jausoft)](https://jausoft.com/).

### License
See [LICENSE.txt](LICENSE.txt).

## Documentation

* [GlueGen Java™ API-Doc](https://jogamp.org/deployment/jogamp-next/javadoc/gluegen/javadoc/)
* [GlueGen Native Data & Function Mapping for Java™](doc/GlueGen_Mapping.md)
* [GlueGen Manual](doc/manual/index.html)
* [JogAmp's MacOS Version Support](doc/JogAmpMacOSVersions.md)
* [How To Build](https://jogamp.org/gluegen/doc/HowToBuild.html)

## Build Requirements
Check [GlueGen's HowToBuild](https://jogamp.org/gluegen/doc/HowToBuild.html).

## Directory Organization:
```
make/           Build-related files and the main build.xml
doc/            Documentation
jcpp/           JCPP (git sub-module), migrated C preprocessor written in Java
src/            The actual source for the JOAL APIs.
src/junit/      Unit tests
test/           Standalone manual tests
build/          (generated directory) Where the Jar and DLL files get built to
www/            JOAL project webpage files
```

## Contact Us
- JogAmp             [http://jogamp.org/](https://jogamp.org/)
- JOAL Web           [http://jogamp.org/](https://jogamp.org/joal/)
- Forum/Mailinglist  [http://forum.jogamp.org/](https://forum.jogamp.org/)
- Repository         [http://jogamp.org/git/](https://jogamp.org/git/)
- Wiki               [https://jogamp.org/wiki/](https://jogamp.org/wiki/)
- Maintainer         [https://jogamp.org/wiki/index.php/Maintainer_and_Contacts](https://jogamp.org/wiki/index.php/Maintainer_and_Contacts)
- Sven's Blog        [https://jausoft.com/blog/tag/jogamp/](https://jausoft.com/blog/tag/jogamp/)
- Email              sgothel _at_ jausoft _dot_ com

## History
Since roughly 2010, GlueGen development has been continued
by individuals of the JogAmp community, see git log for details.

