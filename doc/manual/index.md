<!---
We convert markdown using pandoc using `markdown+lists_without_preceding_blankline` as source format
and `html5+smart` with a custom template as the target.

Recipe:
```
  ~/pandoc-buttondown-cgit/pandoc_md2html_local.sh index.md > index.html
```  

Git repos:
- https://jausoft.com/cgit/users/sgothel/pandoc-buttondown-cgit.git/about/
- https://github.com/sgothel/pandoc-buttondown-cgit
-->

<style>
table, th, td {
   border: 1px solid black;
}
</style>

# GlueGen Manual {#gluegen-manual}

*Disclaimer: This documented shall be synchronized with source code,
especially the configuration options.*

Please also consider reading [GlueGen Native Data & Function
Mapping](../GlueGen_Mapping.html) for details on native data and
function mappings.

## References

- [GlueGen Git Repo](https://jogamp.org/cgit/gluegen.git/about/)
- [GlueGen Java™ API-Doc](https://jogamp.org/deployment/jogamp-next/javadoc/gluegen/javadoc/)
- [GlueGen Native Data & Function Mapping for Java™](../GlueGen_Mapping.html)
- [GlueGen Project Page](https://jogamp.org/gluegen/www/)
- [How To Build](../HowToBuild.html)

## <span id="Chapter1">Chapter 1 - Introduction</span>

### <span id="SecIntroduction">Introduction</span>

GlueGen is a tool which automatically generates the Java and JNI code
necessary to call C libraries. It reads as input ANSI C header files and
separate configuration files which provide control over many aspects of
the glue code generation. GlueGen uses a complete ANSI C parser and an
internal representation (IR) capable of representing all C types to
represent the APIs for which it generates interfaces. It has the ability
to perform significant transformations on the IR before glue code
emission. GlueGen is currently powerful enough to bind even low-level
APIs such as the Java Native Interface (JNI) and the AWT Native
Interface (JAWT) back up to the Java programming language.

GlueGen is currently used to generate the JOGL interface to the OpenGL
3D graphics API and the JOAL interface to the OpenAL audio library. In
the case of JOGL, GlueGen is used not only to bind OpenGL to Java, but
also the low-level windowing system APIs on the Windows, X11 and Mac OS
X platforms. The implementation of the JOGL library is thereby written
in the Java programming language rather than in C, which has offered
considerable advantages during the development of the library.

GlueGen is designed in modular form and can be extended to alter the
glue code emission style or to generate interface code for other
languages than Java.

This manual describes how to use GlueGen to bind new C libraries to the
Java programming language.

### <span id="SecStructure">Structure of the Generated Glue Code</span>

GlueGen supports two basic styles of glue code generation: everything in
one class, or a separate interface and implementing class. The first
mode, "AllStatic", exposes the underlying C functions as a set of static
Java methods in a concrete class. This is a straightforward binding
mechanism, but has the disadvantage of tying users to a concrete class
(which may or may not be a problem) and makes it more difficult to
support certain kinds of call-through-function-pointer semantics
required by certain C APIs. The second mode, "InterfaceAndImpl", exposes
the C functions as methods in an interface and emits the implementation
of that interface into a separate class and package. The implementing
class is not intended to be in the public API; this more strongly
separates the user from the implementation of the API. Additionally,
because it is necessary to hold an instance of the implementing class in
order to access the underlying C routines, it is easier to support
situations where call-through-function-pointer semantics must be
followed, in particular where those function pointers might change from
instance to instance.

The generated glue code follows some basic rules in binding C APIs to
Java:

-   C primitive types are exposed as the corresponding Java primitive
    type.
-   Pointers to typed C primitives (`int*`, `float*`) are bound to
    java.nio Buffer subclasses (`IntBuffer`, `FloatBuffer`) and
    optionally to Java arrays (`int[]`, `float[]`).
    -   If a C function takes such a pointer as an outgoing argument,
        two method overloadings will generally be produced; one which
        accepts a Buffer, and one which accepts a primitive array plus
        an integer offset argument. The variant taking a Buffer may
        accept either a "direct" NIO Buffer or a non-direct one
        (wrapping a Java array). The exception is when such a routine is
        specified by the [NioDirectOnly](#NioDirectOnly) directive to
        keep a persistent pointer to the passed storage, in which case
        only the Buffer variant will be generated, and will only accept
        a direct Buffer as argument.
    -   If a C function returns such a pointer as its result, it will be
        exposed as the corresponding Buffer type. In this case it is
        also typically necessary to specify to GlueGen via the
        [ReturnValueCapacity](#ReturnValueCapacity) directive the number
        of addressable elements in the resulting array.
-   Pointers to `void*` are bound to java.nio.Buffer.
    -   By default any C function accepting a `void*` argument will
        allow either a direct or non-direct java.nio Buffer to be passed
        as argument. If the [NioDirectOnly](#NioDirectOnly) directive is
        specified, however, only a direct Buffer will be accepted.
    -   Similar rules for `void*` return values apply to those for
        pointers to typed primitives.
-   To avoid an explosion in the number of generated methods, if a
    particular API accepts more than one typed primitive pointer
    argument, only two overloadings continue to be produced: one
    accepting all arrays as arguments and one accepting all Buffers as
    arguments. When calling the variant accepting Buffers, all of the
    Buffers passed in a particular call must be either direct or
    non-direct. Mixing of direct and non-direct Buffers in a given
    function call is not supported.
-   When a java.nio Buffer is passed from Java to C, the position of the
    Buffer is taken into account. The resulting pointer passed to C is
    equal to the base address of the Buffer plus the position scaled
    appropriately for the size of the primitive elements in the Buffer.
    This feature is called "auto-slicing", as it mimics the behavior of
    calling Buffer.slice() without the overhead of explicit object
    creation.
-   Pointers to constant `char*` may be bound to java.lang.String using
    the [ArgumentIsString](#ArgumentIsString) or
    [ReturnsString](#ReturnsString) directives.
-   `#define` statements in header files mapping names to constant
    values are exposed as public static final constant values in either
    the generated interface or AllStatic class.
-   C structs encountered during the glue code generation process and
    referenced by the C functions are exposed as Java classes of the
    same name (typically the name to which the struct is typedefed).
    Each primitive field in the struct is exposed as two methods; a
    getter, which accepts no arguments, and a setter, which accepts as
    argument a primitive value of the type of the field. Static factory
    methods are exposed allowing allocation of these structs from Java
    code. The backing storage for these Java classes is a direct
    java.nio Buffer. GlueGen fully supports returning of pointers to C
    structs up to Java.

### <span id="SecUnique">Unique Features</span>

GlueGen contains several unique features making it both a powerful and
easy-to-use tool.

-   C structs are exposed as Java classes. The generated code for these
    classes supports both 32-bit and 64-bit platforms.
-   C structs containing function pointers are exposed as Java classes
    with methods. This makes it easy to interact with low-level C APIs
    such as the AWT Native Interface (JAWT) from the Java programming
    language level.
    -   In this context, GlueGen automatically detects which argument to
        the various function pointers indicates the "this" pointer,
        hiding it at the Java level and passing it automatically.
    -   GlueGen offers automatic handling of JNI-specific data types
        such as `JNIEnv*` and `jobject`. The tool understands that the
        `JNIEnv*` argument is implicit and that `jobject` maps to
        java.lang.Object at the Java programming language level. While
        this is most useful when binding JDK-internal APIs such as the
        JAWT to Java, there may be other JNI libraries which expose C
        functions taking these data types, and GlueGen can very easily
        bind to them.

### <span id="SecBackground">Background and Design Principles</span>

This section provides motivation for the design of the GlueGen tool and
is not necessary to understand how to use the tool.

There are many tools available for assisting in the autogeneration of
foreign function interfaces for various high-level languages. Only a few
examples include
[Header2Scheme](http://alumni.media.mit.edu/~kbrussel/Header2Scheme/),
an early tool allowing binding of a limited subset of C++ to the Scheme
programming language; [SWIG](http://www.swig.org/), a tool released at
roughly the same time as Header2Scheme which by now supports binding C
and C++ libraries to a variety of scripting languages;
[JNIWrapper](http://www.jniwrapper.com/), a commercial tool automating
the binding of C APIs to Java; and
[NoodleGlue](http://web.archive.org/web/20070419183658/http://www.noodleglue.org/noodleglue/noodleglue.html),
a recently-released tool automating the binding of C++ APIs to Java.
Other language-specific tools such as Perl's XS, Boost.Python and many
others exist.

GlueGen was designed with a few key principles in mind. The most
fundamental was to support binding of the lowest-level APIs on a given
platform up to the Java programming language. The intended goal, in the
context of the JOGL project, was to allow subsets of the Win32 and X11
APIs to be exposed to Java, and to use those APIs to write the
behind-the-scenes OpenGL context creation and management code in Java
instead of C. This informed several other design goals:

-   Avoid touching the C headers as much as possible. This makes it
    easier to upgrade to a more recent version of the C API just by
    copying in a new set of headers.
-   Avoid touching the generated glue code completely.
-   Avoid having to hand-write a lot of generated glue code. Instead,
    handle many complex constructs automatically and provide sufficient
    control over the glue code generation to avoid having to handwrite
    certain native methods where one or two lines of tweaking would
    suffice.
-   Support all C constructs in the parser and intermediate
    representation. The rationale is that it is acceptable to cut
    corners in the number of constructs supported in the Java binding,
    but not whether the tool can internally represent it in its C type
    system. This design goal implies starting with complete a ANSI C
    parser coupled with a complete C type system.
-   As the tool is targetting the Java programming language, build the
    tool in the Java programming language.

In order to make the problem more tractable, support for binding C++ to
the Java programming language was not considered. C++ adds many
constructs over ANSI C which make it much more difficult to reason about
and to find a useful subset to support binding to Java. Additionally, it
seems that there are relatively few C++-specific libraries in general
use which could be usefully bound to Java, although this may be a matter
of opinion.

GlueGen was designed with the Java programming language in mind, but is
not necessarily restricted to generating glue code for the Java
language. The tool is divided into separate parse and code generation
phases, and the internal representation is fairly easy to iterate over.
The core driver of GlueGen may therefore be useful in producing other
tools which autogenerate foreign function interfaces to C libraries for
other languages.

## <span id="Chapter2">Chapter 2 - Using GlueGen</span>

### <span id="SecAcquiring">Acquiring and Building GlueGen</span>

The source code for GlueGen may be obtained by cloning the Git
repository:

        $git clone --recursive git://jogamp.org/srv/scm/gluegen.git gluegen
                        

To build GlueGen, cd into the gluegen/make folder and invoke ant.

        $ant clean all test
                        

Ant 1.8 or later and a Java 6 compatible JDK is required.

#### <span id="SecCommon">Common Build Problems</span>

**CharScanner; panic: ClassNotFoundException: com.jogamp.gluegen.cgram.CToken**  
This occurs because ANTLR was dropped into the Extensions directory of
the JRE/JDK. On Windows and Linux, delete any ANTLR jars from
jre/lib/ext, and on Mac OS X, delete them from /Library/Java/Extensions.
Use the antlr.jar property in the build.xml to point to a JRE-external
location of this jar file.

### <span id="SecBasic">Basic Operation</span>

GlueGen can be run either as an executable jar file
(`java -jar gluegen.jar`; note that
antlr.jar must be in the same directory as gluegen.jar in order for this
invocation to work) or from within Ant as described in the following
section. When run from the command line, GlueGen accepts four kinds of
command-line arguments:

-   -I*dir* (optional) adds *dir* to the include path. Similarly to a C
    compiler or preprocessor, GlueGen scans a set of directories to
    locate header files it encounters in `#include` directives. Unlike
    most C preprocessors, however, GlueGen has no default include path,
    so it is typically necessary to supply at least one `-I` option on
    the command line in order to handle any `#include` directives in the
    file being parsed.
-   -E*emitterClassName* (optional) uses *emitterClassName* as the
    fully-qualified name of the emitter class which will be used by
    GlueGen to generate the glue code. The emitter class must implement
    the `com.jogamp.gluegen.GlueEmitter` interface. If this option is
    not specified, a `com.jogamp.gluegen.JavaEmitter` will be used by
    default.
-   -C*cfgFile* adds *cfgFile* to the list of configuration files used
    to set up the chosen emitter. This is the means by which a large
    number of options are passed in to the GlueGen tool and to the
    emitter in particular. Configuration files are discussed more in the
    following section.
-   \[ filename \| - \] selects the file or standard input from which
    GlueGen should read the C header file for which glue code should be
    generated. This must be the last command-line argument, and only one
    filename argument is supported. To cause multiple header files to be
    parsed, write a small .c file \#including the multiple headers and
    point GlueGen at the .c file.

### <span id="SecAnt">Running GlueGen as an Ant Task</span>

GlueGen can also be invoked as a subtask within Ant. In order to do so,
a path element should be defined as follows:

        <path id="gluegen.classpath">
            <pathelement location="${gluegen.jar}" />
            <pathelement location="${antlr.jar}" />
        </path>
                        

where the `gluegen.jar` and `antlr.jar` properties point to the
respective jar files. A taskdef defining the GlueGen task should then be
specified as follows:

    <taskdef name="gluegen"
        classname="com.jogamp.gluegen.ant.GlueGenTask"
        classpathref="gluegen.classpath" />
                        

At this point GlueGen may be invoked as follows:

    <gluegen src="[header to parse]" 
             config="[configuration file]"
             includeRefid="[dirset for include path]"
             emitter="com.jogamp.gluegen.JavaEmitter">
        <classpath refid="gluegen.classpath" />
    </gluegen>
                        

Please see the [JOGL](http://jogamp.org/jogl/) and
[JOAL](http://jogamp.org/joal/) build.xml files for concrete, though
non-trivial, examples of how to invoke GlueGen via Ant.

### <span id="SecJCPP">JCPP</span>

GlueGen contains and uses the [C preprocessor
JCPP](https://jogamp.org/cgit/jcpp.git/about/), see [original
homepage](https://www.anarres.org/projects/jcpp/).

Constant values intended for use by end users are defined in many C
libraries' headers using `#define`s rather than constant int
declarations. If the header would be processed by a full C preprocessor,
the `#define` statement's macro name become unavailable for processing
by the glue code generator. Using JCPP allows us to utilize the
`#define` macro names and values.

JCPP is largely an invisible part of the glue code generation process.
If GlueGen's output is not as expected and there is heavy use of the C
preprocessor in the header, run JCPP against the header directly (JCPP
takes simply the -I and filename arguments accepted by GlueGen) and
examine the output.

### <span id="SecStub">Stub Headers</span>

As much as is possible, GlueGen is intended to operate on unmodified C
header files, so that it is easy to upgrade the given C API being bound
to Java simply by dropping in a new set of header files. However, most C
headers contain references to standard headers like `stdio.h`, and if
this header is parsed by GlueGen, the tool will automatically attempt to
generate Java entry points for such routines as `fread` and `fwrite`,
among others. It is impractical to exclude these APIs on a case by case
basis. Therefore, the suggested technique to avoid polluting the binding
with these APIs is to "stub out" the headers.

GlueGen searches the include path for headers in the order the include
directories were specified to the tool. Placing another directory in
front of the one in which the bulk of the headers are found allows, for
example, an alternative `stdio.h` to be inserted which contains few or
no declarations but which satisfies the need of the dependent header to
find such a file.

GlueGen uses a complete ANSI and GNU C parser written by John Mitchell
and Monty Zukowski from the set of grammars available for the ANTLR tool
by Terrence Parr. As a complete C parser, this grammar requires all data
types encountered during the parse to be fully defined. Often a
particular header will be included by another one in order to pick up
data type declarations rather than API declarations. Stubbing out the
header with a smaller one providing a "fake" type declaration is a
useful technique for avoiding the binding of unnecessary APIs during the
glue code process.

Here's an example from the JOGL glue code generation process. The
`glext.h` header defining OpenGL extensions references `stddef.h` in
order to pick up the `ptrdiff_t` data type. We choose to not include the
real stddef.h but instead to swap in a stub header. The contents of this
header are therefore as follows:

        #if defined(_WIN64)
            typedef __int64 ptrdiff_t;
        #elif defined(__ia64__) || defined(__x86_64__)
            typedef long int ptrdiff_t;
        #else
            typedef int ptrdiff_t;
        #endif
                        

This causes the ptrdiff_t data type to be defined appropriately for the
current architecture. It will be referenced during the glue code
generation and cause a Java value of the appropriate type (int or long)
to be used to represent it.

This is not the best example because it involves a data type which
changes size between 32- and 64-bit platforms, and there are otner
considerations to take into account in these situations (see the section
[32- and 64-bit considerations](#Sec32)). Here's another example, again
from the JOGL source tree. JOGL binds the AWT Native Interface, or JAWT,
up to the Java programming language so that the low-level code which
binds OpenGL contexts to Windows device contexts may be written in Java.
The JDK's `jawt_md.h` on the Windows platform includes `windows.h` to
pick up the definitions of data types such as `HWND` (window handle) and
`HDC` (handle to device context). However, it is undesirable to try to
parse the real `windows.h` just to pick up these typedefs; not only does
this header contain thousands of unneeded APIs, but it also uses certain
macro constructs not supported by GlueGen's contained [C
preprocessor](#SecJCPP). To avoid these problems, a "stub" `windows.h`
header is placed in GlueGen's include path containing only the necessary
typedefs:

        typedef struct _handle*     HANDLE;
        typedef HANDLE              HDC;
        typedef HANDLE              HWND;
                        

Note that it is essential that the type being specified to GlueGen is
compatible at least in semantics with the real definition of the HANDLE
typedef in the real `windows.h`, so that during compilation of GlueGen's
autogenerated C code, when the real `windows.h` is referenced by the C
compiler, the autogenerated code will compile correctly.

This example is not really complete as it also requires [consideration
of the size of data types on 32- and 64-bit platforms](#Sec32) as well
as a discussion of how certain [opaque data types](#SecOpaque) are
described to GlueGen and exposed in its autogenerated APIs. Nonetheless,
it illustrates at a basic level why using a stub header is necessary and
useful in certain situations.

### <span id="Sec32">32- and 64-bit Considerations</span>

When binding C functions to the Java programming language, it is
important that the resulting Java code support execution on a 64-bit
platform if the associated native methods are compiled appropriately. In
other words, the public Java API should not change if the underlying C
data types change to another data model such as LP64 (in which longs and
pointers become 64-bit).

GlueGen internally maintains two descriptions of the underlying C data
model: one for 32-bit architectures and one for 64-bit architectures.
These machine descriptions are used when deciding the mapping between
integral C types such as int and long and the corresponding Java types,
as well as when laying out C structs for access by the Java language.
For each autogenerated C struct accessor, both a 32-bit and 64-bit
variant are generated behind the scenes, ensuring that the resulting
Java code will run correctly on both 32-bit and 64-bit architectures.

When generating the main class containing the bulk of the method
bindings, GlueGen uses the 64-bit machine description to map C data
types to Java data types. This ensures that the resulting code will run
properly on 64-bit platforms. Note that it also generally means that C
`long`s will be mapped to Java `long`s, since an LP64 data model is
assumed.

If [Opaque directives](#SecOpaque) are used to cause a given C integer
or pointer data type to be mapped directly to a Java primitive type,
care should be taken to make sure that the Java primitive type is wide
enough to hold all of the data even on 64-bit platforms. Even if the
data type is defined in the header file as being only a 32-bit C
integer, if there is a chance that on a 64-bit platform the same header
may define the data type as a 64-bit C integer or long, the Opaque
directive should map the C type to a Java long.

### <span id="SecOpaque">Opaque Directives</span>

Complex header files may contain declarations for certain data types
that are either too complex for GlueGen to handle or unnecessarily
complex from the standpoint of glue code generation. In these situations
a stub header may be used to declare a suitably compatible typedef for
the data type. An [Opaque](#Opaque) directive can be used to map the
resulting typedef to a Java primitive type if it is undesirable to
expose it as a full-blown Java wrapper class.

GlueGen hashes all typedefs internally down to their underlying
primitive type. (This is probably not really correct according to the C
type system, but is correct enough from a glue code generation
standpoint, where if the types are compatible they are considered
equivalent.) This means that if the parser encounters

        typedef void* LPVOID;
                        

then an Opaque directive stating

        Opaque long LPVOID
                        

will cause all `void*` or `LPVOID` arguments in the API to be mapped to
Java longs, which is almost never desirable. Unfortunately, it is not
currently possible to distinguish between the LPVOID typedef and the
underlying `void*` data type in this situation.

A similar problem occurs for other data types for which Opaque
directives may be desired. For example, a Windows HANDLE equates to a
typedef to `void*`, but performing this typedef in a stub header and
then adding the Opaque directive

        Opaque long HANDLE
                        

will cause all void\* arguments to be exposed as Java longs instead of
Buffers, which is again undesirable. Attempting to work around the
problem by typedef'ing HANDLE to an integral type, as in:

        typedef long HANDLE;
                        

may itself have problems, because GlueGen will assume the two integral
types are compatible and not perform any intermediate casts between
HANDLE and jlong in the autogenerated C code. (When casting between a
pointer type and a JNI integral type such as jlong in C code, GlueGen
automatically inserts casts to convert the pointer first to an
"intptr_t" and then to the appropriate JNI type, in order to silence
compiler warnings and/or errors.)

What is desired is to produce a new type name distinct from all others
but still compatible with the pointer semantics of the original type.
Then an Opaque directive can be used to map the new type name to, for
example, a Java long.

To implement this in the context of the HANDLE example, the following
typedef may be inserted into the stub header:

        typedef struct _handle*     HANDLE;
                        

This uses a pointer to an anonymous struct name to produce a new pointer
type. This is legal ANSI C and is supported by GlueGen's parser without
having seen a declaration for "struct \_handle". Subsequently, an Opaque
directive can be used to map the HANDLE data type to a Java long:

        Opaque long HANDLE
                        

Now HANDLEs are exposed to Java as longs as desired. A similar technique
is used to expose XIDs on the X11 platform as Java longs.

### <span id="SecSubstitution">Argument Name Substitution</span>

Certain configuration file directives allow the insertion of Java or C
code at various places in the generated glue code, to both eliminate the
need to hand-edit the generated glue code as well as to minimize the
hand-writing of glue code, which sidesteps the GlueGen process. In some
situations the inserted code may reference incoming arguments to compute
some value or perform some operation. Examples of directives supporting
this substitution include [ReturnValueCapacity](#ReturnValueCapacity)
and [ReturnedArrayLength](#ReturnedArrayLength).

The expressions in these directives may contain Java MessageFormat
expressions like `{0}` which refer to the incoming argument names to the
function. `{0}` refers to the first incoming argument.

Strongly-typed C primitive pointers such as `int*`, which ordinarily
expand to overloaded Java methods taking e.g. `int[]` as well as
`IntBuffer`, present a problem. The expansion to `int[] arr` also
generates an `int arr_offset` argument to be able to pass a pointer into
the middle of the array down to C. To allow the same MessageFormat
expression to be used for both cases, the subsitution that occurs when
such a primitive array is referenced is the string
`arr, arr_offset`; in other words, the
subtituted string contains a comma. This construct may be used in the
following way: the code being manually inserted may itself contain a
method call taking e.g. `{3}` (the incoming argument index of the
primitive array or buffer). The user should supply two overloaded
versions of this method, one taking a strongly-typed Buffer and one
taking e.g. an `int[] arr` and `int arr_offset` argument. The
implementation of `RangeCheck`s for primitive arrays and strongly-typed
buffers uses this construct.

It should be noted that in the autogenerated C code the offset argument
is expressed in bytes while at the Java level it is expressed in
elements. Most uses of GlueGen will probably not have to refer to the
primitive array arguments in C code so this slight confusion should be
minor.

### <span id="SecConfiguration">Configuration File Directives</span>

In addition to the C headers, GlueGen requires a certain amount of
metadata in the form of configuration files in order to produce its glue
code. There are three basic reasons for this: first, GlueGen must be
informed into which Java classes the C methods are to be bound; second,
there are many configuration options for the generated glue code, and
passing them all on the command line is infeasible; and third, there are
ambiguities in many constructs in the C programming language which must
be resolved before a Java binding can be produced.

The contents of the configuration file are dependent on the class of
emitter specified to GlueGen. Currently there are three built-in emitter
classes: JavaEmitter, which produces a basic, static Java binding of C
functions; ProcAddressEmitter, which extends JavaEmitter by calling the
underlying C functions through function pointers, resulting in more
dynamic behavior and supporting C APIs with optional functionality; and
GLEmitter, which specializes ProcAddressEmitter to support some
OpenGL-specific constructs. The GLEmitter will be ignored in this manual
as it is specialized for JOGL and provides very little additional
functionality beyond the ProcAddressEmitter. The JavaEmitter and
ProcAddressEmitter support many options in their configuration files. As
the ProcAddressEmitter is a subclass of JavaEmitter, all of the
constructs in the JavaEmitter's configuration files are also legal in
the ProcAddressEmitter's configuration files.

The configuration files have a very simple line-by-line structure, and
are parsed by a very rudimentary, hand-written parser. Each
non-whitespace and non-comment line (note: comment lines begin with '#')
contains a directive like `Package`, `Style` or `JavaClass` followed by
arguments to that directive. There are a certain set of directives that
are required for any code generation; others are optional and their
omission results in some default behavior. Directives are
case-insensitive.

The following is an exhaustive list of the options currently supported
by each of these emitters' configuration files. It is difficult to see
exactly how to use the tool based simply on these descriptions, so the
[examples](#Chapter3) may be more helpful in seeing exactly how to
structure a configuration file for proper glue code generation.

#### <span id="SecJavaEmitter">JavaEmitter Configuration</span>

Note that only a very few of the following directives are specified as
being "required" rather than "optional"; these indicate the minimal
directives needed for a valid configuration file to begin to get glue
code to be produced. In general, these are [Package](#Package),
[ImplPackage](#ImplPackage), [JavaClass](#JavaClass),
[ImplJavaClass](#ImplJavaClass), and [Style](#Style). Other directives
such as [NioDirectOnly](#NioDirectOnly) are required in some
circumstances for the glue code to be correct, and some such as
[ReturnedArrayLength](#ReturnedArrayLength),
[ReturnValueCapacity](#ReturnValueCapacity), and
[ReturnValueLength](#ReturnValueLength) should be specified in some
situations in order for certain return values to be useful at the Java
level.

The following directives are specified in alphabetical order, although
this is not necessarily the best semantic order.

**<span id="AccessControl">AccessControl</span>**  
Syntax:
`AccessControl [method name] [ PUBLIC | PROTECTED | PRIVATE | PACKAGE_PRIVATE ]`  
(optional) Controls the access control of a certain Java method
corresponding to a C function. The access control of all APIs defaults
to public. This is useful when using the C binding of a particular
function only as one implementation strategy of the real public API and
using [CustomJavaCode](#CustomJavaCode) to write the exposed API. In
this case is most useful in conjunction with
[RenameJavaMethod](#RenameJavaMethod).

**<span id="ArgumentIsString">ArgumentIsString</span>**  
Syntax:
`ArgumentIsString [function name] [indices...]`
where the first argument index is 0  
(optional) For a C function with one or more outgoing `char*` (or
compatible data type) arguments, indicates that those arguments are
semantically null-terminated C strings rather than arbitrary arrays of
bytes. The generated glue code will be modified to emit those arguments
as java.lang.String objects rather than `byte[]` or `ByteBuffer`.

**<span id="ArgumentIsPascalString">ArgumentIsPascalString</span>**  
Syntax:
`ArgumentIsPascalString [function name] [indice-tuples...]`,
with each tuple being the argument-index for the '`int length`' and the
'`char* value`' argument with index 0 for the the first argument  
(optional) For a C function with one or more outgoing '`int length`' and
'`char* value`' (or compatible data type) arguments, indicates that
those arguments are semantically non-null-terminated Pascal strings
rather than null-terminated C strings or arbitrary arrays of bytes. The
generated glue code will be modified to emit those arguments as
java.lang.String objects rather than `byte[]` or `ByteBuffer`
as well as dropping the redundant '`int length`' argument on the Java side.

**<span id="ClassJavadoc">ClassJavadoc</span>**  
Syntax: `ClassJavadoc [class name] [code...]`  
(optional) Causes the specified line of code to be emitted in the
appropriate place in the generated code to become the per-class Javadoc
for the specified class. By default GlueGen produces no Javadoc for its
generated classes, so this is the mechanism by which a user can emit
Javadoc for these classes. The specified Javadoc undergoes no
transformation by GlueGen, so the initial `/**` and trailing `*/` must
be included in the correct place. Each line of Javadoc is emitted in the
order encountered during parsing of the configuration files. See also :
[MethodJavadoc](#MethodJavadoc)

**<span id="CustomCCode">CustomCCode</span>**  
Syntax: `CustomCCode [code...]`  
(optional) Causes the specified line of C code to be emitted into the
generated native code for the implementing class. Currently there is no
way (and no real need) to be able to emit custom C code into any other
generated .c file, so the class name in the
[CustomJavaCode](#CustomJavaCode) directive is omitted.

**<span id="CustomJavaCode">CustomJavaCode</span>**  
Syntax: `CustomJavaCode [class name] [code...]`  
(optional) Causes the specified line of Java code to be emitted into the
specified generated Java class. Can be used to emit code into any
generated class: the public interface, the implementing class, the sole
concrete class (in the case of the AllStatic [Style](#Style)), or any of
the Java classes corresponding to referenced C structs in the parsed
headers. This usage is somewhat verbose, and the [IncludeAs](#IncludeAs)
directive provides a more concise way of including large bodies of Java
code into the generated code.

**<span id="CustomJNICode">CustomJNICode</span>**  
Syntax: `CustomJNICode [class name] [code...]`  
(optional) Causes the specified line of C code to be emitted into the
generated JNI code related of specified Java class. Can be used to emit
JNI code related of any generated class: the public interface, the
implementing class, the sole concrete class (in the case of the
AllStatic [Style](#Style)), or any of the Java classes corresponding to
referenced C structs in the parsed headers. This usage is somewhat
verbose, and the [IncludeAs](#IncludeAs) directive provides a more
concise way of including large bodies of C code into the generated code.

**<span id="DelegateImplementation">DelegateImplementation</span>**
Syntax: `DelegateImplementation [delegated method name] [method name for original implementation]`
(optional) Causes the specified method will not be bind to related C
function but will be bind to second method name provided.
The first method name provided shall be manually delegated by the end user
with [CustomJavaCode](#CustomJavaCode) (except if [Style](#Style) is
defined with `InterfaceOnly` ofc)

**<span id="EmitStruct">EmitStruct</span>**  
Syntax: `EmitStruct [C struct type name]`  
(optional) Forces a Java class to be emitted for the specified C struct.
Normally only those structs referenced directly by the parsed C APIs
have corresponding Java classes emitted.

**<span id="GlueGenRuntimePackage">GlueGenRuntimePackage</span>**  
Syntax:
`GlueGenRuntimePackage [package name, like com.jogamp.gluegen.runtime]`  
(optional) Changes the package in which the generated glue code expects
to find its run-time helper classes (like Buffers, CPU, StructAccessor).
Defaults to `com.jogamp.gluegen.runtime` (no quotes). This is useful if
you want to bundle the runtime classes in your application without the
possibility of interfering with other versions elsewhere in the system.

**<span id="ExtendedInterfaceSymbolsIgnore">ExtendedInterfaceSymbolsIgnore</span>**  
Syntax: `ExtendedInterfaceSymbolsIgnore [Java file]`  
(optional) Causes all autogenerated Java interface ignore all symbols
from interface declared inside named Java source file.  
This directive can be used with [Extends](#Extends) directive.  
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#oo-style-example)

**<span id="ExtendedInterfaceSymbolsOnly">ExtendedInterfaceSymbolsOnly</span>**  
Syntax: `ExtendedInterfaceSymbolsOnly [Java file]`  
(optional) Causes all autogenerated Java interface generate only symbols
from interface declared inside named Java source file.  
This directive can be used with [Extends](#Extends) directive.  
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#oo-style-example)

**<span id="ExtendedImplementationSymbolsIgnore">ExtendedImplementationSymbolsIgnore</span>**  
Syntax: `ExtendedImplementationSymbolsIgnore [Java file]`  
(optional) Causes all autogenerated Java classes ignore all symbols from
interface or classe declared inside named Java source file.  
This directive can be used with [ParentClass](#ParentClass) directive.  
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#oo-style-example)

**<span id="ExtendedImplementationSymbolsOnly">ExtendedImplementationSymbolsOnly</span>**  
Syntax: `ExtendedImplementationSymbolsOnly [Java file]`  
(optional) Causes all autogenerated Java classes generate only symbols
from interface or classe declared inside named Java source file.  
This directive can be used with [ParentClass](#ParentClass) directive.  
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#oo-style-example)

**<span id="ExtendedIntfAndImplSymbolsIgnore">ExtendedIntfAndImplSymbolsIgnore</span>**  
Syntax: `ExtendedIntfAndImplSymbolsIgnore [Java file]`  
(optional) Causes all autogenerated Java interface and classes ignore
all symbols from interface or classe declared inside named Java source
file.  
This directive can be used with [Extends](#Extends) or
[ParentClass](#ParentClass) directives.  
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#oo-style-example)

**<span id="ExtendedIntfAndImplSymbolsOnly">ExtendedIntfAndImplSymbolsOnly</span>**  
Syntax: `ExtendedIntfAndImplSymbolsOnly [Java file]`  
(optional) Causes all autogenerated Java interface and classes generate
only symbols from interface or classe declared inside named Java source
file.  
This directive can be used with [Extends](#Extends) or
[ParentClass](#ParentClass) directives.  
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#oo-style-example)

**<span id="Extends">Extends</span>**  
Syntax: `Extends [Java interface name] [interface name to extend] `  
(optional) Causes the specified autogenerated Java interface to declare
that it extends another one. This directive may only be applied to
autogenerated interfaces, not concrete classes. For concrete classes,
use [Implements](#Implements) directive or [ParentClass](#ParentClass)
directive.

**<span id="HierarchicalNativeOutput">HierarchicalNativeOutput</span>**  
Syntax: `HierarchicalNativeOutput true`  
(optional) If "true", makes subdirectories for the generated native code
matching the package names of the associated classes. This is typically
not needed (or desired, as it complicates the compilation process for
this native code) and defaults to false.

**<span id="Ignore">Ignore</span>**  
Syntax: `Ignore [regexp]`  
(optional) Ignores one or more functions or data types matching the
regexp argument which are encountered during parsing of the C headers.
By default GlueGen will emit all encountered C functions as well as Java
classes corresponding to all C structs referenced by those functions.
Related directives are [IgnoreNot](#IgnoreNot), [Unignore](#Unignore)
and [EmitStruct](#EmitStruct).

**<span id="IgnoreField">IgnoreField</span>**  
Syntax: `IgnoreField [struct type name] [field name]`  
(optional) Causes the specified field of the specified struct type to be
ignored during code generation, typically because it is too complex for
GlueGen to handle.

**<span id="IgnoreNot">IgnoreNot</span>**  
Syntax: see [Ignore](#Ignore). (optional) Similar to the
[Ignore](#Ignore) directive, but evaluates the negation of the passed
regexp when deciding whether to ignore the given function or data type.
The [Unignore](#Unignore) mechanism may be used with IgnoreNot as well.
NOTE: the IgnoreNot mechanism may ultimately turn out to be superfluous;
the authors do not have sufficient experience with regular expressions
to know whether general negation of a regexp is possible. Feedback in
this area would be appreciated.

**<span id="Implements">Implements</span>**  
Syntax:
`Implements [Java class name] [interface name to implement]`  
(optional) Causes the specified autogenerated Java concrete class to
declare that it implements the specified interface. This directive may
only be applied to autogenerated concrete classes, not interfaces. For
interfaces, use the [Extends](#Extends) directive.

**<span id="ImplJavaClass">ImplJavaClass</span>**  
Syntax: `ImplJavaClass [class name]`  
(optional) Specifies the name of the typically non-public,
implementation Java class which contains the concrete Java and native
methods for the glue code. If the emission style is AllStatic, there is
no distinction between the public and implementation class and
ImplJavaClass should not be specified. Otherwise, if the ImplJavaClass
is unspecified, it defaults to the JavaClass name plus "Impl". (If both
are unspecified in this configuration, an error is reported.) See also
[JavaClass](#JavaClass).

**<span id="ImplPackage">ImplPackage</span>**  
Syntax: `ImplPackage [package name]`  
(optional) Specifies the package name into which the implementing class
containing the concrete Java and native methods will be emitted,
assuming an emission style of InterfaceAndImpl or ImplOnly. If
AllStatic, there is no separate implementing class from the public
interface. If the emission style is not AllStatic and the ImplPackage is
not specified, it defaults to the Package plus ".impl". See also
[Package](#Package).

**<span id="Import">Import</span>**  
Syntax: `Import [package name]` (no trailing semicolon)  
(optional) Adds an import statement at the top of each generated Java
source file.

**<span id="ImmutableAccess">ImmutableAccess</span>**
Syntax: `ImmutableAccess [C struct or field of C struct (Struct and field are dot separated)]`
(optional) Suppress generating setter in the Java code and hence also
reduces the footprint of the generated Java class for such struct.
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#immutableaccess-symbol)

**<span id="Include">Include</span>**  
Syntax: `Include [filename]`  
(optional) Causes another configuration file to be read at the current
point in parsing the current configuration file. The filename argument
may be either absolute or relative; in the latter case it is specified
relative to the location of the current configuration file.

**<span id="IncludeAs">IncludeAs</span>**  
Syntax: `IncludeAs [prefix tokens] [filename]`  
(optional) Similar to the [Include](#Include) directive, but prepends
the specified prefix tokens on to every line of the file to be read. The
last token parsed is the name of the file to be read. This allows, for
example, [CustomJavaCode](#CustomJavaCode) to be stored as Java source
rather than in the configuration file; in this example the configuration
file might contain
`IncludeAs CustomJavaCode MyClass MyClass-CustomJavaCode.java`.

**<span id="JavaCallbackDef">JavaCallbackDef</span>**
Syntax: `JavaCallbackDef  [<SetCallbackFunctionName>] [<SetCallback-UserParamIndex>] [<CallbackFunctionType>] [<CallbackFunction-UserParamIndex>] [[<Callback-UserParamClass>] [[<Callback-KeyClass>]]]`
(optional) Adds Java interface allow to create Java callback for
Java binding of setCallbackFunctionName. Callback supplied can receive
asynchronous  and off-thread natives event (converted on the fly before
passing converted value to Java callback)
Must be used with [LibraryOnLoad](#LibraryOnLoad).
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#java-callback)

**<span id="JavaCallbackKey">JavaCallbackKey</span>**
Syntax: `JavaCallbackKey  [<SetCallbackFunctionName>] [<SetCallback-ParamIndex>]* [<CallbackFunctionType>] [<CallbackFunction-ParamIndex>]*]`
(optional) Adds callback key for Java binding of setCallbackFunctionName.
Must be used with [JavaCallbackDef](#JavaCallbackDef).
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#java-callback)

**<span id="JavaClass">JavaClass</span>**  
Syntax: `JavaClass [class name]`  
(optional / required) Specifies the name of the public,
non-implementation Java class or interface into which the glue code will
be generated. If the emission style is not ImplOnly, the JavaClass
directive is required. See also [ImplJavaClass](#ImplJavaClass).

**<span id="JavaEpilogue">JavaEpilogue</span>**  
Syntax: `JavaEpilogue [C function name] [code...]`  
(optional) Adds the specified code as an epilogue in the Java method for
the specified C function; this code is run after the underlying C
function has been called via the native method but before any result is
returned. As in the [ReturnedArrayLength](#ReturnedArrayLength) and
other directives, [argument name substitution](#SecSubstitution) is
performed on MessageFormat expressions in the specified code. See also
[JavaPrologue](#JavaPrologue).

**<span id="JavaOutputDir">JavaOutputDir</span>**  
Syntax: `JavaOutputDir [directory name]`  
(optional) Specifies the root directory into which the emitted Java code
will be produced. Subdirectories for the packages of the associated Java
classes will be automatically created. If unspecified, defaults to the
current working directory.

**<span id="JavaPrologue">JavaPrologue</span>**  
Syntax: `JavaPrologue [C function name] [code...]`  
(optional) Adds the specified code as a prologue in the Java method for
the specified C function; this code is run before the underlying C
function is called via the native method. As in the
[ReturnedArrayLength](#ReturnedArrayLength) and other directives,
[argument name substitution](#SecSubstitution) is performed on
MessageFormat expressions in the specified code. See also
[JavaEpilogue](#JavaEpilogue).

**<span id="LibraryOnLoad">LibraryOnLoad</span>**  
Syntax: `LibraryOnLoad [Library base name]
(optional) Indicates to GlueGen to produce native JNI code to handle
the `JavaVM*` instance. In particular, it is required when
[Java™ callback methods are used](../GlueGen_Mapping.html#java-callback).
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#libraryonload-librarybasename-for-jni_onload-)

**<span id="ManuallyImplement">ManuallyImplement</span>**  
Syntax: `ManuallyImplement [function name]`  
(optional) Indicates to GlueGen to not produce a method into the
implementing class for the specified C function; the user must provide
one via the [CustomJavaCode](#CustomJavaCode) directive. If the emission
style is InterfaceAndImpl or InterfaceOnly, a public method will still
be generated for the specified function.

**<span id="MaxOneElement">MaxOneElement</span>**  
Syntax: `MaxOneElement [function name]`  
(optional) Indicates that the specified C function/attribute which
returns a single element instead a ByteBuffer if signature or compatible
type actually returns a pointer like int\* but isn't an array.  
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#gluegen-struct-settings)

**<span id="MethodJavadoc">MethodJavadoc</span>**
Syntax: `MethodJavadoc [method name] [code...]`  
(optional) Causes the specified line of code to be emitted in the
appropriate place in the generated code to become the per-method Javadoc
for the specified method. By default GlueGen produces basic Javadoc for its
generated method, notably name of c binding related, so this is the
mechanism by which a user can emit Javadoc for these methods. The
specified Javadoc undergoes no transformation by GlueGen,so the initial
`/**` and trailing `*/` must  be included in the correct place. Each
line of Javadoc is emitted in the order encountered during parsing
of the configuration files. See also : [ClassJavadoc](#ClassJavadoc)

**<span id="NativeOutputDir">NativeOutputDir</span>**  
Syntax: `NativeOutputDir [directory name]`  
(optional) Specifies the root directory into which the emitted JNI code
will be produced. If unspecified, defaults to the current working
directory. See also
[HierarchicalNativeOutput](#HierarchicalNativeOutput).

**<span id="NioDirectOnly">NioDirectOnly</span>**  
Syntax: `NioDirectOnly [function name|__ALL__]`  
(required when necessary) When passing a pointer down to a C API, it is
semantically undefined whether the underlying C code expects to treat
that pointer as a persistent pointer, living past the point of return of
the function call, or whether the pointer is used only during the
duration of the function call. For APIs taking C primitive pointers such
as `void*`, `float*`, etc., GlueGen will typically generate up to two
overloaded Java methods, one taking a `Buffer` or `Buffer` subclass such
as `FloatBuffer`, and one taking a primitive array such as `float[]`.
(In the case of `void*` outgoing arguments, GlueGen produces only one
variant taking a Buffer.) Normally the generated glue code accepts
either a "direct" or non-"direct" buffer (according to the New I/O APIs)
as argument. However, if the semantics of the C function are that it
either expects to hold on to this pointer past the point of the function
call, or if it can block while holding on to the pointer, the
`NioDirectOnly` directive **must** be specified for this C function in
order for the generated glue code to be correct. Failing to observe this
requirement may cause JVM hangs or crashes. See also [NIOOnly](#NIOOnly).

**<span id="NIOOnly">NIOOnly</span>**
Syntax: `NIOOnly [function name|__ALL__]`
(optional) Cause fonction specified (or all) should only create a
`java.nio` variant, and no array variants, for `void*` and other
C primitive pointers. NIO only still allows usage of array backed
not direct Buffers. See also : [NioDirectOnly](#NioDirectOnly), same
effect but disallow usage of array backed not direct Buffers.

**<span id="Opaque">Opaque</span>**  
Syntax:
`Opaque [Java primitive data type] [C data type]`  
(optional) Causes a particular C data type to be exposed in opaque form
as a Java primitive type. This is most useful for certain pointer types
for which it is not desired to generate full Java classes but instead
expose them to Java as e.g. `long`s. It is also useful for forcing
certain integral C data types to be exposed as e.g. `long` to Java to
ensure 64-bit cleanliness of the generated glue code. See the
[examples](#Chapter3). The C data type may be a multiple-level pointer
type; for example `Opaque long void**`. Note that it is not currently
supported to make a given data type opaque for just a few functions; the
Opaque directive currently applies to all C functions in the headers
being parsed. This means that sweeping Opaque declarations like
`Opaque long void*` will likely have unforseen and undesirable
consequences.

**<span id="Package">Package</span>**  
Syntax: `Package [package name]` (no trailing semicolon)  
(optional / required) Specifies the package into which the public
interface or class for the autogenerated glue code will be generated.
Required whenever the emission style is not ImplOnly. See also
[ImplPackage](#ImplPackage).

**<span id="ParentClass">ParentClass</span>**  
Syntax: `ParentClass [Java class name] [class name to extend] `  
(optional) Causes the specified autogenerated Java classe to declare
that it extends another one. This directive may only be applied to
autogenerated classes, not interface. For interfaces, use the
[Extends](#Extends) directive.

**<span id="RangeCheck">RangeCheck</span>**  
Syntax: `RangeCheck [C function name] [argument number] [expression]`  
(optional) Causes a range check to be performed on the specified array
or Buffer argument of the specified autogenerated Java method. This
range check ensures, for example, that a certain number of elements are
remaining in the passed Buffer, knowing that the underlying C API will
access no more than that number of elements. For range checks that
should be expressed in terms of a number of bytes rather than a number
of elements, see the [RangeCheckBytes](#RangeCheckBytes) directive. As
in the [ReturnedArrayLength](#ReturnedArrayLength) and other directives,
[argument name substitution](#SecSubstitution) is performed on
MessageFormat expressions.

**<span id="RangeCheckBytes">RangeCheckBytes</span>**  
Syntax:
`RangeCheckBytes [C function name] [argument number] [expression]`  
(optional) Same as the [RangeCheck](#RangeCheck) directive, but the
specified expression is treated as a minimum number of bytes remaining
rather than a minimum number of elements remaining. This directive may
not be used with primitive arrays.

**<span id="RenameJavaMethod">RenameJavaMethod</span>**  
Syntax: `RenameJavaMethod [from name] [to name]`  
(optional) Causes the specified C function to be emitted under a
different name in the Java binding. This is most useful in conjunction
with the [AccessControl](#AccessControl) directive when the C function
being bound to Java is only one potential implementation of the public
API, or when a considerable amount of Java-side custom code is desired
to wrap the underlying C native method entry point.

**<span id="RenameJavaSymbol">RenameJavaSymbol</span>**
Syntax: `RenameJavaSymbol [from name] [to name]`
(optional) Cause the specified C symbol to be emitted under a
different name in the Java binding. (Like [RenameJavaMethod](#RenameJavaMethod))

**<span id="RenameJavaType">RenameJavaType</span>**  
Syntax: `RenameJavaType [from name] [to name]`  
(optional) Causes the specified C struct to be exposed as a Java class
under a different name. This only applies to autogenerated classes
corresponding to C structs encountered during glue code generation; full
control is provided over the name of the top-level classes associated
with the set of C functions via the [JavaClass](#JavaClass) and
[ImplJavaClass](#ImplJavaClass) directives.

**<span id="RelaxedEqualSemanticsTest">RelaxedEqualSemanticsTest</span>**
Syntax: `RelaxedEqualSemanticsTest [true|false]`
(optional) Define if binding must shall attempt to perform a relaxed semantic
equality test, e.g. skip the `const` and `volatile` qualifier or not (by default).

**<span id="ReturnedArrayLength">ReturnedArrayLength</span>**  
Syntax:
`ReturnedArrayLength [C function name] [expression]`
where `expression` is a legal Java expression with MessageFormat
specifiers such as "{0}". These specifiers will be replaced in the
generated glue code with the incoming argument names where the first
argument to the method is numbered 0. See the section on [argument name
substitution](#SecSubstitution).  
(optional) For a function returning a compound C pointer type such as an
`XVisualInfo*`, indicates that the returned pointer is to be treated as
an array and specifies the length of the returned array as a function of
the arguments passed to the function. Note that this directive differs
subtly from [ReturnValueCapacity](#ReturnValueCapacity) and
ReturnValueLength. It is also sometimes most useful in conjunction with
the [TemporaryCVariableDeclaration](#TemporaryCVariableDeclaration) and
TemporaryCVariableAssignment directives.

**<span id="ReturnsOpaque">ReturnsOpaque</span>**
Syntax: `ReturnsOpaque [function name]`  
(optional) Indicates that the specified C function which returns an opaque pointer,
so return value of method are exposed as `long` in Java generated API.
Cf here for more information about opaque pointers : [Opaque](#Opaque)

**<span id="ReturnsString">ReturnsString</span>**  
Syntax: `ReturnsString [function name]`  
(optional) Indicates that the specified C function which returns a
`char*` or compatible type actually returns a null-terminated C string
which should be exposed as a java.lang.String. NOTE: currently does not
properly handle the case where this storage needs to be freed by the end
user. In these situations the data should be returned as a direct
ByteBuffer, the ByteBuffer converted to a String using custom Java code,
and the ByteBuffer freed manually using another function bound to Java.

**<span id="ReturnsStringOnly">ReturnsStringOnly</span>**  
Syntax: `ReturnsStringOnly [function name]`  
(optional) Like the [ReturnsString](#ReturnsString) instruction, but
without the classic getters and setters with ByteBuffer.  
Cf here for more information :
[GlueGen_Mapping](../GlueGen_Mapping.html#gluegen-struct-settings)

**<span id="ReturnValueCapacity">ReturnValueCapacity</span>**  
Syntax:
`ReturnValueCapacity [C function name] [expression]`  
(optional) Specifies the capacity of a java.nio `Buffer` or subclass
wrapping a C primitive pointer such as `char*` or `float*` being
returned from a C function. Typically necessary in order to properly use
such pointer return results from Java. As in the
[ReturnedArrayLength](#ReturnedArrayLength) directive, [argument name
substitution](#SecSubstitution) is performed on MessageFormat
expressions.

**<span id="ReturnValueLength">ReturnValueLength</span>**  
Syntax: `ReturnValueLength [C function name] [expression]`  
(optional) Specifies the length of a returned array of pointers,
typically to C structs, from a C function. This differs from the
[ReturnedArrayLength](#ReturnedArrayLength) directive in the pointer
indirection to the array elements. The
[ReturnedArrayLength](#ReturnedArrayLength) directive handles slicing up
of a linear array of structs, while the ReturnValueLength directive
handles boxing of individual elements of the array (which are pointers)
in to the Java class which wraps that C struct type. See the
[examples](#Chapter3) for a concrete example of usage. As in the
[ReturnedArrayLength](#ReturnedArrayLength) directive, [argument name
substitution](#SecSubstitution) is performed on MessageFormat
expressions.

**<span id="RuntimeExceptionType">RuntimeExceptionType</span>**  
Syntax: `RuntimeExceptionType [class name]`  
(optional) Specifies the class name of the exception type which should
be thrown when run-time related exceptions occur in the generated glue
code, for example if a non-direct Buffer is passed to a method for which
[NioDirectOnly](#NioDirectOnly) was specified. Defaults to
`RuntimeException`.

**<span id="StructMachineDataInfoIndex">StructMachineDataInfoIndex</span>**
Syntax: `StructMachineDataInfoIndex [C struct type name] [code...]`
(optional) Specify `mdIdx`  i.e. the index of the static MachineDescriptor
index for structs. If undefined, code generation uses the default expression:
`private static final int mdIdx = MachineDataInfoRuntime.getStatic().ordinal();`

**<span id="StructPackage">StructPackage</span>**  
Syntax:
`StructPackage [C struct type name] [package name]`.
Package name contains no trailing semicolon.  
(optional) Indicates that the specified Java class corresponding to the
specified C struct should be placed in the specified package. By
default, these autogenerated Java classes corresponding to C structs are
placed in the main package (that defined by
[PackageName](#PackageName)).

**<span id="Style">Style</span>**  
Syntax:
` Style [ AllStatic | InterfaceAndImpl |InterfaceOnly | ImplOnly ] `  
(optional) Defines how the Java API for the parsed C headers is
structured. If AllStatic, one concrete Java class will be generated
containing static methods corresponding to the C entry points. If
InterfaceAndImpl, a public Java interface will be generated into the
[Package](#Package) with non-static methods corresponding to the C
functions, and an "implementation" concrete Java class implementing this
interface will be generated into the [ImplPackage](#ImplPackage). If
InterfaceOnly, the InterfaceAndImpl code generation style will be
followed, but only the interface will be generated. If ImplOnly, the
InterfaceAndImpl code generation style will be followed, but only the
concrete implementing class will be generated. The latter two options
are useful when generating a public API in which certain operations are
unimplemented on certain platforms; platform-specific implementation
classes can be generated which implement or leave unimplemented various
parts of the API.

**<span id="TagNativeBinding">TagNativeBinding</span>**
Syntax: `TagNativeBinding [true|false]`
(optional) Define if  the comment of a native method binding will include
a `@native` tag  to allow taglets to augment the javadoc with additional
information regarding the mapped C function. Defaults to false.

**<span id="TemporaryCVariableAssignment">TemporaryCVariableAssignment</span>**  
Syntax: `TemporaryCVariableAssignment [C function name][code...]`  
(optional) Inserts a C variable assignment declared using the
[TemporaryCVariableDeclaration](#TemporaryCVariableDeclaration)
directive in to the body of a particular autogenerated native method.
The assignment is performed immediately after the call to the underlying
C function completes. This is typically used in conjunction with the
[ReturnValueCapacity](#ReturnValueCapacity) or
[ReturnValueLength](#ReturnValueLength) directives to capture the size
of a returned C buffer or array of pointers. See the
[examples](#Chapter3) for a concrete example of usage of this directive.
Note that unlike, for example, the
[ReturnedArrayLength](#ReturnedArrayLength) directive, no substitution
is performed on the supplied code, so the user must typically have
previously looked at the generated code and seen what work needed to be
done and variables needed to be examined at exactly that line.

**<span id="TemporaryCVariableDeclaration">TemporaryCVariableDeclaration</span>**  
Syntax:
`TemporaryCVariableDeclaration [C function name] [code...]`  
(optional) Inserts a C variable declaration in to the body of a
particular autogenerated native method. This is typically used in
conjunction with the
[TemporaryCVariableAssignment](#TemporaryCVariableAssignment) and
[ReturnValueCapacity](#ReturnValueCapacity) or
[ReturnValueLength](#ReturnValueLength) directives to capture the size
of a returned C buffer or array of pointers. See the
[examples](#Chapter3) for a concrete example of usage of this directive.

**<span id="Unignore">Unignore</span>**  
Syntax: `Unignore [regexp]`  
(optional) Removes a previously-defined [Ignore](#Ignore) directive.
This is useful when one configuration file includes another and wishes
to disable some of the Ignores previously specified.

**<span id="Unimplemented">Unimplemented</span>**  
Syntax: `Unimplemented [regexp]`  
(optional) Causes the binding for the functions matching the passed
regexp to have bodies generated which throw the stated
[RuntimeExceptionType](#RuntimeExceptionType) indicating that this
function is unimplemented. This is most useful when an API contains
certain functions that are not supported on all platforms and there are
multiple implementing classes being generated, one per platform.

**<span id="UnsupportedExceptionType">UnsupportedExceptionType</span>**
Syntax `UnsupportedExceptionType [type]`
(optional) Change type of exception thrown by binding when some method
are unsupported (if native method are missing in ABI for example). Default
exception type are (java.lang.)UnsupportedOperationException.

#### <span id="SecProcAddressEmitter">ProcAddressEmitter Configuration</span>

The ProcAddressEmitter is a subclass of the core JavaEmitter which knows
how to call C functions through function pointers. In particular, the
ProcAddressEmitter detects certain constructs in C header files which
imply that the APIs are intended to be called through function pointers,
and generates the glue code appropriately to support that.

The ProcAddressEmitter detects pairs of functions and function pointer
typedefs in a set of header files. If it finds a matching pair, it
converts the glue code emission style for that API to look for the
function to call in an autogenerated table called a ProcAddressTable
rather than linking the autogenerated JNI code directly to the function.
It then changes the calling convention of the underlying native method
to pass the function pointer from Java down to C, where the
call-through-function-pointer is performed.

The ProcAddressEmitter discovers the function and function pointer pairs
by being informed of the mapping between their names by the user. In the
OpenGL and OpenAL libraries, there are fairly simple mappings between
the functions and function pointers. For example, in the OpenGL
`glext.h` header file, one may find the following pair:

        GLAPI void APIENTRY glFogCoordf (GLfloat);
        ...
        typedef void (APIENTRYP PFNGLFOGCOORDFPROC) (GLfloat coord);
                        

Therefore the mapping rule between the function name and the function
pointer typedef for the OpenGL extension header file is "PFN +
Uppercase(funcname) + PROC". Similarly, in the OpenAL 1.1 header files,
one may find the following pair:

        AL_API void AL_APIENTRY alEnable( ALenum capability );
        ...
        typedef void           (AL_APIENTRY *LPALENABLE)( ALenum capability );
                        

Therefore the mapping rule between the function name and the function
pointer typedef for the OpenAL header files is "LP +
Uppercase(funcname)".

These are the two principal function pointer-based APIs toward which the
GlueGen tool has currently been applied. It may turn out to be that this
simple mapping heuristic is insufficient, in which case it will need to
be extended in a future version of the GlueGen tool.

Note that it is currently the case that in order for the
ProcAddressEmitter to notice that a given function should be called
through a function pointer, it must see both the function prototype as
well as the function pointer typedef. Some headers, in particular the
OpenAL headers, have their `#ifdefs` structured in such a way that
either the declaration or the typedef is visible, but not both
simultaneously. Because the [JCPP](#SecJCPP) C preprocessor GlueGen uses
obeys `#ifdefs`, it is in a situation like this that the headers would
have to be modified to allow GlueGen to see both declarations.

The following directives are specified in alphabetical order, although
this is not necessarily the best semantic order. The ProcAddressEmitter
also accepts all of the directives supported by the JavaEmitter. The
required directives are
[GetProcAddressTableExpr](#GetProcAddressTableExpr) and
[ProcAddressNameExpr](#ProcAddressNameExpr).

**<span id="EmitProcAddressTable">EmitProcAddressTable</span>**  
Syntax: `EmitProcAddressTable [true | false]`  
(optional) Indicates whether to emit the ProcAddressTable during glue
code generation. Defaults to false.

**<span id="ForceProcAddressGen">ForceProcAddressGen</span>**  
Syntax: `ForceProcAddressGen [function name]`  
(optional) Indicates that a ProcAddressTable entry should be produced
for the specified function even though it does not have an associated
function pointer typedef in the header. This directive does not
currently cause the autogenerated Java and C code to change to
call-through-function-pointer style, which should probably be considered
a bug. (FIXME)

**<span id="GetProcAddressTableExpr">GetProcAddressTableExpr</span>**  
Syntax: `GetProcAddressTableExpr [expression]`  
(required) Defines the Java code snippet used by the generated glue code
to fetch the ProcAddressTable containing the function pointers for the
current API. It is up to the user to decide where to store the
ProcAddressTable. Common places for it include in an instance field of
the implementing class, in an associated object with which there is a
one-to-one mapping, or in a static field of another class accessed by a
static method. In the JOGL project, for example, each GLImpl instance
has an associated GLContext in an instance field called "\_context", so
the associated directive is
`GetProcAddressTableExpr _context.getGLProcAddressTable()`. In the JOAL
project, the ProcAddressTables are currently held in a separate class
accessed via static methods, so one of the associated directives is
`GetProcAddressTableExpr ALProcAddressLookup.getALCProcAddressTable()`.

**<span id="ProcAddressNameExpr">ProcAddressNameExpr</span>**  
Syntax: `ProcAddressNameExpr [expression]`  
(required) Defines the mapping from function name to function pointer
typedef to be able to properly identify this function as needing
call-through-function-pointer semantics. The supplied expression uses a
set of simple commands to describe certain operations on the function
name:

-   `$UpperCase(arg)` converts the argument to uppercase. "UpperCase" is
    case-insensitive.
-   `$LowerCase(arg)` converts the argument to lowercase. "LowerCase" is
    case-insensitive.
-   `{0}` represents the name of the function.
-   Any other string represents a constant string.
-   Concatenation is implicit.

The corresponding ProcAddressNameExpr for the OpenGL extension functions
as described at the start of this section is
`PFN $UPPERCASE({0}) PROC`. The
ProcAddressNameExpr for the OpenAL functions as described at the start
of this section is `LP $UPPERCASE({0})`.

**<span id="ProcAddressTableClassName">ProcAddressTableClassName</span>**  
Syntax: `ProcAddressTableClassName [class name]`  
(optional) Specifies the class name into which the table containing the
function pointers will be emitted. Defaults to "ProcAddressTable".

**<span id="ProcAddressTablePackage">ProcAddressTablePackage</span>**  
Syntax:
`ProcAddressTablePackage [package name] (no trailing semicolon)`
(optional) Specifies the package into which to produce the
ProcAddressTable for the current set of APIs. Defaults to the
implementation package specified by the [ImplPackage](#ImplPackage)
directive.

**<span id="SkipProcAddressGen">SkipProcAddressGen</span>**
Syntax: `SkipProcAddressGen [function name]`
(optional) Indicates that the default behavior of
call-through-function-pointer should be skipped for this function
despite the fact that it has an associated function pointer typedef in
the header.

## <span id="Chapter3">Chapter 3 - Configuration File Examples</span>

### <span id="SecSimplest">Simplest possible example</span>

Files:

- [function.c](example1/function.c)
- [function.h](example1/function.h)
- [function.cfg](example1/function.cfg)
- [gen.sh](example1/gen.sh)

This example shows the simplest possible usage of GlueGen; a single
routine taking as arguments and returning only primitive types. The
signature of the C function we are interested in binding is

        int one_plus(int a);
                        

To bind this function to Java, we only need a configuration file with
very basic settings, indicating the style of glue code emission, the
package and class into which the glue code will be generated, and the
output directories for the Java and native code. The contents of the
configuration file are as follows:

        Package testfunction
        Style AllStatic
        JavaClass TestFunction
        JavaOutputDir   gensrc/java
        NativeOutputDir gensrc/native
                        

GlueGen can then be invoked with approximately the following command
line:

        java -cp gluegen.jar:antlr.jar com.jogamp.gluegen.GlueGen \
            -I. -Ecom.jogamp.gluegen.JavaEmitter -Cfunction.cfg function.h
                        

The resulting Java and native code needs to be compiled, and the
application needs to load the native library for the Java binding before
attempting to invoke the native method by calling `System.load()` or
`System.loadLibrary()`.

### <span id="SecArrays">Arrays and buffers</span>

Files:

-   [function.c](example2/function.c)
-   [function.h](example2/function.h)
-   [function.cfg](example2/function.cfg)
-   [gen.sh](example2/gen.sh)

This example shows how C primitive arrays are bound to Java. The header
file contains three functions to bind:

        float process_data(float* data, int n);
        void set_global_data(float* data);
        float process_global_data(int n);
                        

The semantics of `process_data` are that it takes in a pointer to a set
of primitive `float` values and the number of elements in the array and
performs some operation on them, returning a floating-point value as the
result. Afterward the passed data is no longer referenced.

`set_global_data`, on the other hand, takes a pointer to the data and
stores it persistently in the C code. `process_global_data` then accepts
as argument the number of elements to process from the previously-set
global data, performs this processing and returns a result. The global
data may be accessed again afterward. As an example, these kinds of
semantics are used in certain places in the OpenGL API.

From a Java binding standpoint, `process_data` may accept data stored
either inside the Java heap (in the form of a `float[]` or non-direct
`FloatBuffer`) or outside the Java heap (in the form of a direct
`FloatBuffer`), because it does not access the data after the function
call has completed and therefore would not be affected if garbage
collection moved the data after the function call was complete. However,
`set_global_data` can cause the passed data to be accessed after the
function call is complete, if `process_global_data` is called. Therefore
the data passed to `set_global_data` may not reside in the Java
garbage-collected heap, but must reside outside the heap in the form of
a direct `FloatBuffer`.

It is straightforward to take into account these differences in
semantics in the configuration file using the
[NioDirectOnly](#NioDirectOnly) directive:

        # The semantics of set_global_data imply that
        # only direct Buffers are legal
        NioDirectOnly set_global_data
                        

Note the differences in the generated Java-side overloadings for the two
functions:

        public static void process_data(java.nio.FloatBuffer data, int n) {...}
        public static void process_data(float[] data, int data_offset, int n) {...}
        public static void set_global_data(java.nio.FloatBuffer data) {...}
                        

No overloading is produced for `set_global_data` taking a `float[]`, as
it can not handle data residing in the Java heap. Further, the generated
glue code will verify that any `FloatBuffer` passed to this routine is
direct, throwing a `RuntimeException` if not. The type of the exception
thrown in this and other cases may be changed with the
[RuntimeExceptionType](#RuntimeExceptionType) directive.

### <span id="SecString">String handling</span>

Files:

-   [function.h](example3/function.h)
-   [function.cfg](example3/function.cfg)
-   [gen.sh](example3/gen.sh)

This example shows how to pass and return C strings. The functions
involved are a bit contrived, as nobody would ever need to bind the C
library's string handling routines to Java, but they do illustrate
situations in which Java strings might need to be passed to C and C
strings returned to Java. As an example, both styles of function are
present in the OpenGL and OpenAL APIs.

The included source code exposes two functions to Java:

        size_t strlen(const char* str);
        char*  strstr(const char* str1, const char* str2);
                        

Note that we might just as easily parse the C standard library's
`string.h` header file to pick up these function declarations. However
for the purposes of this example it is easier to extract just the
functions we need.

Note that the [function.h](example3/function.h) header file contains a
typedef for `size_t`. This is needed because GlueGen does not inherently
know about this data type. An equivalent data type for the purposes of
this example is `int`, so we choose to tell GlueGen to use that data
type in place of `size_t` while generating glue code.

The following directive in the configuration file tells GlueGen that
`strlen` takes a string as argument 0 (the first argument):

        ArgumentIsString strlen 0
                        

The following directive tells GlueGen that `strstr` takes two strings as
its arguments:

        ArgumentIsString strstr 0 1
                        

Finally, the following directive tells GlueGen that `strstr` returns a
string instead of an array of bytes:

        ReturnsString strstr
                        

We also use the [CustomCCode](#CustomCCode) directive to cause the
`string.h` header file to be \#included in the generated glue code:

        CustomCCode /* Include string.h header */
        CustomCCode #include <string.h>
                        

Now the bindings of these two functions to Java look as expected:

        public static native int strlen(java.lang.String str);
        public static native java.lang.String strstr(java.lang.String str1, java.lang.String str2);
                        

Note that the [ReturnsString](#ReturnsString) directive does not
currently correctly handle the case where the `char*` returned from C
needs to be explicitly freed. As an example, a binding of the C function
`strdup` using a ReturnsString directive would cause a C heap memory
leak.

### <span id="SecMemory">Memory allocation</span>

Files:

-   [function.c](example4/function.c)
-   [function.h](example4/function.h)
-   [function.cfg](example4/function.cfg)
-   [gen.sh](example4/gen.sh)

This example shows how memory allocation is handled when binding C to
Java. It gives the example of a custom memory allocator being bound to
Java; this is a construct that at least at one point was present in
OpenGL in the NV_vertex_array_range extension.

The two functions we are exposing to Java are as follows:

        void* custom_allocate(int num_bytes);
        void  custom_free(void* data);
                        

The Java-side return type of `custom_allocate` will necessarily be a
`ByteBuffer`, as that is the only useful way of interacting with
arbitrary memory produced by C. The question is how to inform the glue
code generator of the size of the returned sequence of memory. The
semantics of `custom_allocate` are obvious to the programmer; the
incoming `num_bytes` argument specifies the amount of returned memory.
We tell GlueGen this fact using the
[ReturnValueCapacity](#ReturnValueCapacity) directive:

        # The length of the returned ByteBuffer from custom_allocate is
        # specified as the argument
        ReturnValueCapacity custom_allocate {0}
                        

Note that we name incoming argument 0 with the MessageFormat specifier
"{0}" rather than the explicit name of the parameter ("num_bytes") for
generality, in case the header file is changed later.

Because `custom_free` will only ever receive Buffers produced by
custom_allocate, we use the [NioDirectOnly](#NioDirectOnly) directive to
prevent accidental usage with the wrong kind of Buffer:

        # custom_free will only ever receive a direct Buffer
        NioDirectOnly custom_free
                        

The generated Java APIs for these functions are as follows:

        public static java.nio.ByteBuffer custom_allocate(int num_bytes) {...}
        public static void custom_free(java.nio.Buffer data) {...}
                        

### <span id="SecStructs">Ingoing and outgoing structs</span>

Files:

-   [function.c](example5/function.c)
-   [function.h](example5/function.h)
-   [function.cfg](example5/function.cfg)
-   [gen.sh](example5/gen.sh)

This example shows how GlueGen provides access to C structs and supports
both passing them to and returning them from C functions. The header
file defines a sample data structure that might describe the bit depth
of a given screen:

        typedef struct {
            int redBits;
            int greenBits;
            int blueBits;
        } ScreenInfo;
                        

Two functions are defined which take and return this data type:

        ScreenInfo* default_screen_depth();
        void set_screen_depth(ScreenInfo* info);
                        

The semantics of `default_screen_depth()` are that it returns a pointer
to some static storage which does not need to be freed, which describes
the default screen depth. `set_screen_depth()` is a hypothetical
function which would take a newly-allocated `ScreenInfo` and cause the
primary display to switch to the specified bit depth.

The only additional information we need to tell GlueGen, beyond that in
the header file, is how much storage is returned from
`default_screen_depth()`. Note the semantic ambiguity, where it might
return a pointer to a single `ScreenInfo` or a pointer to an array of
`ScreenInfo`s. We tell GlueGen that the return value is a single value
with the [ReturnValueCapacity](#ReturnValueCapacity) directive,
similarly to the [memory allocation](#SecMemory) example above:

        # Tell GlueGen that default_screen_depth() returns a pointer to a
        # single ScreenInfo
        ReturnValueCapacity default_screen_depth sizeof(ScreenInfo)
                        

Note that if `default_screen_depth` had returned newly-allocated
storage, it would be up to the user to expose a `free()` function to
Java and call it when necessary.

GlueGen automatically generates a Java-side `ScreenInfo` class which
supports not only access to any such objects returned from C, but also
allocation of new `ScreenInfo` structs which can be passed
(persistently) down to C. The Java API for the ScreenInfo class looks
like this:

        public abstract class ScreenInfo {
            public static ScreenInfo create();
            public abstract ScreenInfo redBits(int val);
            public abstract int redBits();
            ...
        }
                        

The `create()` method allocates a new ScreenInfo struct which may be
passed, even persistently, out to C. Its C-heap storage will be
automatically reclaimed when the Java-side ScreenInfo object is no
longer reachable, as it is backed by a direct New I/O `ByteBuffer`. The
fields of the struct are exposed as methods which supply both getters
and setters.

### <span id="SecStructArrays">Returned arrays of structs</span>

Files:

-   [function.h](example6/function.h)
-   [function.cfg](example6/function.cfg)
-   [gen.sh](example6/gen.sh)

This example, taken from JOGL's X11 binding, illustrates how to return
an array of structs from C to Java. The `XGetVisualInfo` function from
the X library has the following signature:

        XVisualInfo *XGetVisualInfo(
            Display*     display,
            long         vinfo_mask,
            XVisualInfo* vinfo_template,
            int*         nitems_return
        );
                        

Note that the `XVisualInfo` data structure itself contains many
elements, including a pointer to the current visual. We use the
following trick in the header file to cause GlueGen to treat the
`Display*` in the above signature as well as the `Visual*` in the
`XVisualInfo` as opaque pointers:

        typedef struct {}     Display;
        typedef struct {}     Visual;
        typedef unsigned long VisualID;

        typedef struct {
            Visual *visual;
            VisualID visualid;
            int screen;
            int depth;
            int c_class; /* C++ */
            unsigned long red_mask;
            unsigned long green_mask;
            unsigned long blue_mask;
            int colormap_size;
            int bits_per_rgb;
        } XVisualInfo;
                        

`XGetVisualInfo` returns all of the available pixel formats in the form
of `XVisualInfo`s which match a given template. `display` is the current
connection to the X server. `vinfo_mask` indicates which fields from the
template to match against. `vinfo_template` is a partially filled-in
`XVisualInfo` specifying the characteristics to match. `nitems_return`
is a pointer to an integer indicating how many `XVisualInfo`s were
returned. The return value, rather than being a pointer to a single
`XVisualInfo`, is a pointer to the start of an array of `XVisualInfo`
data structures.

There are two basic steps to being able to return this array properly to
Java using GlueGen. The first is creating a direct ByteBuffer of the
appropriate size in the autogenerated JNI code. The second is slicing up
this ByteBuffer appropriately in order to return an `XVisualInfo[]` at
the Java level.

In the autogenerated JNI code, after the call to `XGetVisualInfo` is
made, the outgoing `nitems_return` value points to the number of
elements in the returned array, which indicates the size of the direct
ByteBuffer which would need to wrap these elements. However, if we look
at the implementation of one of the generated glue code variants for
this method (specifically, the one taking an `int[]` as the third
argument), we can see a problem in trying to access this value in the C
code:

        JNIEXPORT jobject JNICALL
        Java_testfunction_TestFunction_XGetVisualInfo1__Ljava_nio_ByteBuffer_2JLjava_nio_ByteBuffer_2Ljava_lang_Object_2I(
            JNIEnv *env, jclass _unused, jobject arg0, jlong arg1, jobject arg2, jobject arg3, jint arg3_byte_offset) {
            ...
            int * _ptr3 = NULL;
            ...
            if (arg3 != NULL) {
                _ptr3 = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, arg3, NULL)) + arg3_byte_offset);
            }
            _res = XGetVisualInfo((Display *) _ptr0, (long) arg1, (XVisualInfo *) _ptr2, (int *) _ptr3);
            if (arg3 != NULL) {
                (*env)->ReleasePrimitiveArrayCritical(env, arg3, _ptr3, 0);
            }
            if (_res == NULL) return NULL;
            return (*env)->NewDirectByteBuffer(env, _res,  ??? What to put here ???);
        }
                        

Note that at the point of the statement "What to put here?" the pointer
to the storage of the `int[]`, `_ptr3`, has already been released via
`ReleasePrimitiveArrayCritical`. This means that it may not be
referenced at the point needed in the code.

To solve this problem we use the
[TemporaryCVariableDeclaration](#TemporaryCVariableDeclaration) and
[TemporaryCVariableAssignment](#TemporaryCVariableAssignment)
directives. We want to declare a persistent integer variable down in the
C code and assign the returned array length to that variable before the
primitive array is released. While in order to do this we unfortunately
need to know something about the structure of the autogenerated JNI
code, at least we don't have to hand-edit it afterward. We add the
following directives to the configuration file:

        # Get returned array's capacity from XGetVisualInfo to be correct
        TemporaryCVariableDeclaration XGetVisualInfo   int count;
        TemporaryCVariableAssignment  XGetVisualInfo   count = _ptr3[0];
                        

Now in the autogenerated JNI code the variable "count" will contain the
number of elements in the returned array. We can then reference this
variable in a [ReturnValueCapacity](#ReturnValueCapacity) directive:

        ReturnValueCapacity XGetVisualInfo   count * sizeof(XVisualInfo)
                        

At this point the `XGetVisualInfo` binding will return a Java-side
`XVisualInfo` object whose backing ByteBuffer is the correct size. We
now have to inform GlueGen that the underlying ByteBuffer represents not
a single `XGetVisualInfo` struct, but an array of them, using the
[ReturnedArrayLength](#ReturnedArrayLength) directive. This conversion
is performed on the Java side of the autogenerated code. Here, the first
element of either the passed `IntBuffer` or `int[]` contains the number
of elements in the returned array. (Alternatively, we could examine the
length of the ByteBuffer returned from C to Java and divide by
`XVisualInfo.size()`.) Because there are two overloadings produced by
GlueGen for this method, if we reference the `nitems_return` argument in
a [ReturnedArrayLength](#ReturnedArrayLength) directive, we need to
handle not only the differing data types properly (`IntBuffer` vs.
`int[]`), but also the fact that both the integer array and its offset
value are substituted for any reference to the fourth argument.

To solve this problem, we define a pair of private helper functions
whose purpose is to handle this overloading.

        CustomJavaCode TestFunction  private static int getFirstElement(IntBuffer buf) {
        CustomJavaCode TestFunction    return buf.get(buf.position());
        CustomJavaCode TestFunction  }
        CustomJavaCode TestFunction  private static int getFirstElement(int[] arr,
        CustomJavaCode TestFunction                                     int offset) {
        CustomJavaCode TestFunction    return arr[offset];
        CustomJavaCode TestFunction  }
                        

Now we can simply write for the returned array length:

        ReturnedArrayLength XGetVisualInfo  getFirstElement({3})
                        

That's all that is necessary. GlueGen will then produce the following
Java-side overloadings for this function:

        public static XVisualInfo[] XGetVisualInfo(Display arg0,
                                                   long arg1,
                                                   XVisualInfo arg2,
                                                   java.nio.IntBuffer arg3);
        public static XVisualInfo[] XGetVisualInfo(Display arg0,
                                                  long arg1,
                                                  XVisualInfo arg2,
                                                  int[] arg3, int arg3_offset);
                        

As it happens, we don't really need the Display and Visual data
structures to be produced; they can be treated as `long`s on the Java
side. Therefore we can add the following directives to the configuration
file:

        # We don't need the Display and Visual data structures to be
        # explicitly exposed
        Opaque long Display *
        Opaque long Visual *
        # Ignore the empty Display and Visual data structures (though made
        # opaque, the references from XVisualInfo and elsewhere are still
        # traversed)
        Ignore Display
        Ignore Visual
                        

The final generated Java API is the following:

        public static XVisualInfo[] XGetVisualInfo(long arg0,
                                                   long arg1,
                                                   XVisualInfo arg2,
                                                   java.nio.IntBuffer arg3);
        public static XVisualInfo[] XGetVisualInfo(long arg0,
                                                   long arg1,
                                                   XVisualInfo arg2,
                                                   int[] arg3, int arg3_offset);
                        

### <span id="SecPointerArrays">Returned arrays of pointers</span>

Files:

-   [function.h](example7/function.h)
-   [function.cfg](example7/function.cfg)
-   [gen.sh](example7/gen.sh)

As with the [example above](#SecStructArrays), this example is taken
from JOGL's X11 binding. Here we show how to expose to Java a C routine
returning an array of pointers to a data structure.

The declaration of the function we are binding is as follows:

        typedef struct __GLXFBConfigRec *GLXFBConfig;

        GLXFBConfig *glXChooseFBConfig( Display *dpy, int screen,
                                        const int *attribList, int *nitems );
                        

This function is used during allocation of a hardware-accelerated
off-screen surface ("pbuffer") on X11 platforms; its exact meaning is
not important. The semantics of the arguments and return value are as
follows. As in the [previous example](#SecStructArrays), it accepts a
connection to the current X display as one argument. The screen of this
display is the second argument. The `attribList` is a zero-terminated
list of integer attributes; because it is zero-terminated, the length of
this list is not passed to the function. As in the previous example, the
`nitems` argument points to an integer into which the number of returned
`GLXFBConfig` objects is placed. The return value is an array of
`GLXFBConfig` objects.

Because the `GLXFBConfig` data type is typedefed as a pointer to an
opaque (undefined) struct, the construct `GLXFBConfig*` is implicitly a
"pointer-to-pointer" type. GlueGen automatically assumes this is
convertible to a Java-side array of accessors to structs. The only
configuration necessary is to tell GlueGen the length of this array.

As in the previous example, we use the
[TemporaryCVariableDeclaration](#TemporaryCVariableDeclaration) and
[TemporaryCVariableAssignment](#TemporaryCVariableAssignment) directives
to capture the length of the returned array:

TemporaryCVariableDeclaration glXChooseFBConfig int count;
TemporaryCVariableAssignment glXChooseFBConfig count = \_ptr3\[0\];

The structure of the generated glue code for the return value is subtly
different than in the previous example. The question in this case is not
whether the return value is a pointer to a single object vs. a pointer
to an array of objects; it is what the length of the returned array is,
since we already know that the return type is pointer-to-pointer and is
therefore an array. We use the [ReturnValueLength](#ReturnValueLength)
directive for this case:

        ReturnValueLength glXChooseFBConfig   count
                        

We add similar Opaque directives to the previous example to yield the
resulting Java bindings for this function:

        public static GLXFBConfig[] glXChooseFBConfig(long dpy,
                                                      int screen,
                                                      java.nio.IntBuffer attribList,
                                                      java.nio.IntBuffer nitems);
        public static GLXFBConfig[] glXChooseFBConfig(long dpy,
                                                      int screen,
                                                      int[] attribList, int attribList_offset,
                                                      int[] nitems, int nitems_offset);
                        

Note that because the GLXFBConfig data type is returned as an element of
an array, we can not use the Opaque directive to erase this data type to
`long` as we did with the `Display` data type.

