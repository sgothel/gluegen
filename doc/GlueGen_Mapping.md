<style>
table, th, td {
   border: 1px solid black;
}
</style>

# GlueGen Native Data & Function Mapping for Java™

## References

- [GlueGen Git Repo](https://jogamp.org/cgit/gluegen.git/about/)
- [GlueGen Java™ API-Doc](https://jogamp.org/deployment/jogamp-next/javadoc/gluegen/javadoc/)
- [GlueGen Manual](https://jogamp.org/gluegen/doc/manual/)
- [GlueGen Project Page](https://jogamp.org/gluegen/www/)
- [How To Build](https://jogamp.org/gluegen/doc/HowToBuild.html)

## Overview
[GlueGen](https://jogamp.org/gluegen/www/) is a compiler for function and data-structure declarations, 
generating Java and JNI C code offline at compile time 
and allows using native libraries within your Java application.

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
[map native data structures](#struct-mapping) to be fully accessible from Java™ including 
potential calls to [embedded function pointer](#struct-function-pointer-support).

GlueGen supports [registering Java™ callback methods](#java-callback-from-native-c-api-support)
to receive asynchronous and off-thread native toolkit events,
where a generated native callback function dispatches the events to Java™.

GlueGen also supports [producing an OO-Style API mapping](#oo-style-api-interface-mapping) like [JOGL's incremental OpenGL Profile API levels](../../jogl/doc/uml/html/index.html).

GlueGen is capable to bind low-level APIs such as the Java™ Native Interface (JNI) and
the AWT Native Interface (JAWT) back up to the Java programming language.

Further, GlueGen supports [generating `JNI_OnLoad*(..)` for dynamic and static libraries](#libraryonload-librarybasename-for-jni_onload-), also resolving off-thread `JNIEnv*` lookup.

GlueGen utilizes [JCPP](https://jogamp.org/cgit/jcpp.git/about/), migrated C preprocessor written in Java™.

GlueGen is used for the [JogAmp](https://jogamp.org) projects
[JOAL](https://jogamp.org/cgit/joal.git/about/),
[JOGL](https://jogamp.org/cgit/jogl.git/about/) and
[JOCL](https://jogamp.org/cgit/jocl.git/).

GlueGen is part of [the JogAmp project](https://jogamp.org).

## Primitive Mapping

Gluegen has build-in types (terminal symbols) for:

| type | java-bits | native-bits <br> x32   | native bits <br> x64 | type    | signed | origin |
|:-----|:----------|:-----------------------|:---------------------|:--------|:-------|:-------|
| void | 0         | 0                      | 0                    | void    | void   | ANSI-C |
| char | 8         | 8                      | 8                    | integer | any    | ANSI-C |
| short | 16 | 16 | 16 | integer | any | ANSI-C |
| int | 32 | 32 | 32 | integer | any | ANSI-C |
| long | 64 | 32 | **32**† | integer | any | ANSI-C - Windows |
| long | 64 | 32 | **64** | integer | any | ANSI-C - Unix |
| float | 32 | 32 | 32 | float | signed | ANSI-C |
| double | 64 | 64 | 64 | double | signed | ANSI-C |
| \_\_int32 | 32 | 32 | 32 | integer | any | windows |
| \_\_int64 | 64 | 64 | 64 | integer | any | windows |
| int8\_t | 8 | 8 | 8 | integer | signed | stdint.h |
| uint8\_t | 8 | 8 | 8 | integer | unsigned | stdint.h |
| int16\_t | 16 | 16 | 16 | integer | signed | stdint.h |
| uint16\_t | 16 | 16 | 16 | integer | unsigned | stdint.h |
| int32\_t | 32 | 32 | 32 | integer | signed | stdint.h |
| uint32\_t | 32 | 32 | 32 | integer | unsigned | stdint.h |
| int64\_t | 64 | 64 | 64 | integer | signed | stdint.h |
| uint64\_t | 64 | 64 | 64 | integer | unsigned | stdint.h |
| intptr\_t | 64 | 32 | 64 | integer | signed | stdint.h |
| uintptr\_t | 64 | 32 | 64 | integer | unsigned | stdint.h |
| ptrdiff\_t | 64 | 32 | 64 | integer | signed | stddef.h |
| size\_t | 64 | 32 | 64 | integer | unsigned | stddef.h |
| wchar\_t | 32 | 32 | 32 | integer | signed | stddef.h |

**Warning:** Try to avoid unspecified bit sized types, especially **long**, since it differs on Unix and Windows!  
**Notes:**

* † Type **long** will result in broken code on Windows, since we don't differentiate the OS and it's bit size is ambiguous.
* Anonymous void-pointer _void\*_ are mapped to NIO _Buffer_.
* Pointers to pointer-size types like _intptr\_t\*_, _uintptr\_t\*_, _ptrdiff\_t\*_ and _size\_t\*_ are mapped to _PointerBuffer_, to reflect the architecture depending storage size.

### Pointer Mapping
*Pointer* values itself are represented as `long` values on the Java side
while using the native pointer-size, e.g. 32-bit or 64-bit, on the native end.

They may simply be accessible via `long` or `long[]` primitives in Java,
or are exposed via `com.jogamp.common.nio.PointerBuffer`.

See [Struct Pointer-Pointer Support](#struct-pointer-pointer-support) below.

### String Mapping

#### Function return String values
Function return values are currently mapped from `char*` to Java String using *UTF-8*
via JNI function 
> `jstring NewStringUTF(JNIEnv *env, const char *bytes)`

*FIXME*: This might need more flexibility in case UTF-8 is not suitable for 8-bit wide `char` mappings
or wide characters, e.g. for UTF-16 needs to be supported.

#### Function argument String values
Function argument values are either mapped from `char*` to Java String using *UTF-8*
via JNI function 
> `const char * GetStringUTFChars(JNIEnv *env, jstring string, jboolean *isCopy)`.

Alternatively, if a 16-bit wide *character* type has been detected, i.e. *short*,
the native *character* are mapped to Java using *UTF-16* 
via JNI function 
> `void GetStringRegion(JNIEnv *env, jstring str, jsize start, jsize len, jchar *buf)`.


#### Struct String mapping

String value mapping for `Struct` fields is performed solely from the Java side using *Charset* and is hence most flexible.

By default, *UTF-8* is being used for getter and setter of String values.    
The *Struct* class provides two methods to get and set the used *Charset* for conversion
```
  /** Returns the Charset for this class's String mapping, default is StandardCharsets.UTF_8. */
  public static Charset getCharset() { return _charset; };

  /** Sets the Charset for this class's String mapping, default is StandardCharsets.UTF_8. */
  public static void setCharset(Charset cs) { _charset = cs; }

```

In case the String length has not been configured via `ReturnedArrayLength`,
it will be dynamically calculated via `strnlen(aptr, max_len)`.    
The maximum length default for the `strnlen(..)` operation is 8192 bytes and can be get and set using:
```
  /** Returns the maximum number of bytes to read to determine native string length using `strnlen(..)`, default is 8192. */
  public static int getMaxStrnlen() { return _max_strnlen; };

  /** Sets the maximum number of bytes to read to determine native string length using `strnlen(..)`, default is 8192. */
  public static void setMaxStrnlen(int v) { _max_strnlen = v; }
```
*FIXME*: This only works reliable using an 8-bit Charset encoding, e.g. the default *UTF-8*.

### Alignment for Compound Data

In general, depending on CPU and it's configuration (OS), alignment is set up for each type (char, short, int, long, ..).

Compounds (structures) are aligned naturally, i.e. their inner components are aligned  
and are itself aligned to it's largest element.

See:

*   [Wikipedia Data Structure Alignment](http://en.wikipedia.org/wiki/Data_structure_alignment)
*   [Wikipedia Data Structure Alignment - Padding](http://en.wikipedia.org/wiki/Data_structure_alignment#Data_structure_padding)
*   [Viva64 Data Alignment](http://www.viva64.com/en/l/0021/)
*   [Apple: Darwin 64bit Porting - Data Type Size & Alignment](http://developer.apple.com/library/mac/#documentation/Darwin/Conceptual/64bitPorting/transition/transition.html#//apple_ref/doc/uid/TP40001064-CH207-SW1)

#### Simple alignment arithmetic

Modulo operation, where the 2nd handles the case offset == alignment:

> padding = ( alignment - ( offset % alignment ) ) % alignment ;  
> aligned\_offset = offset + padding ;

Optimization utilizing alignment as a multiple of 2 `-> x % 2n == x & ( 2n - 1 )`

> remainder = offset & ( alignment - 1 ) ;  
> padding = ( remainder > 0 ) ? alignment - remainder : 0 ;  
> aligned\_offset = offset + padding ;

Without branching, using the 2nd modulo operation for the case offset == alignment:

> padding = ( alignment - ( offset & ( alignment - 1 ) ) ) & ( alignment - 1 ) ;  
> aligned\_offset = offset + padding ;

See `com.jogamp.gluegen.cgram.types.SizeThunk.align(..)`.

#### Type Size & Alignment for x86, x86\_64, armv6l-32bit-eabi and Window(mingw/mingw64)

Runtime query is implemented as follows:
```
   typedef struct {
     char   fill;  // nibble one byte
                   // padding to align s1: padding_0 
     type_t s1;    // 
   } test_struct_type_t;
  
             padding_0 = sizeof(test_struct_type_t) - sizeof(type_t) - sizeof(char) ;
   alignmentOf(type_t) = sizeof(test_struct_type_t) - sizeof(type_t) ;
```  

| type        | size <br> *32 bit* | alignment <br> *32 bit* | size <br> *64 bit* | alignment <br> *64 bit* |
|:------------|:-------------------|:------------------------|:-------------------|:------------------------|
| char        | 1            | 1           | 1        | 1         |
| short       | 2            | 2           | 2        | 2         |
| int         | 4            | 4           | 4        | 4         |
| float       | 4            | 4           | 4        | 4         |
| long        | 4            | 4           | 8†,4∗    | 8†,4∗     |
| pointer     | 4            | 4           | 8        | 8         |
| long long   | 8            | 4†,8∗+      | 8        | 8         |
| double      | 8            | 4†,8∗+      | 8        | 8         |
| long double | 12†∗,8+,16\- | 4†∗,8+,16\- | 16       | 16        |

† Linux, Darwin  
+armv7l-eabi  
\- MacOsX-32bit-gcc4  
∗ Windows
     
## OO-Style API Interface Mapping
GlueGen supports producing an OO-Style API mapping like [JOGL's incremental OpenGL Profile API levels](../../jogl/doc/uml/html/index.html).

### OO-Style Mapping Settings

* `ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL.java`

    Ignore all extended interface symbols from named Java source file.
    
    The named Java source file is parsed and a list of its symbols extracted,
    allowing GlueGen to ignore these in the generated interface (here GLES3).
    
    This complements `Extends` setting, see below.

* `Extends GLES3 GLES2`

    The generated interface GLES3 extends interface GLES2.
    
    This complements `ExtendedInterfaceSymbolsIgnore` setting, see above.

* `Implements GLES3Impl GLES3`

    The generated implementation GLES3Impl implements interface GLES3.

### OO-Style Example

Example snippet from JOGL's GLES3 interface config `gl-if-es3.cfg`
```
...

ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL2ES2.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GLES2.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL2ES3.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL3ES3.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL4ES3.java
ExtendedInterfaceSymbolsIgnore ../src/jogl/classes/com/jogamp/opengl/GLBase.java

Package com.jogamp.opengl
Style InterfaceOnly
JavaClass GLES3
Extends GLES3 GLES2
Extends GLES3 GL4ES3
...
```

Example snippet from JOGL's GLES3Impl implementation `gl-es3-impl.cfg`
```
...
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL2ES2.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GLES2.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL2ES3.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL3ES3.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GL4ES3.java
ExtendedInterfaceSymbolsIgnore ../build-temp/gensrc/classes/com/jogamp/opengl/GLES3.java
ExtendedInterfaceSymbolsIgnore ../src/jogl/classes/com/jogamp/opengl/GLBase.java

Style ImplOnly
ImplPackage jogamp.opengl.es3
ImplJavaClass GLES3Impl
Implements GLES3Impl GLES2
Implements GLES3Impl GLES3
...
```

Above produces the GLES3 interface and its implementation as visible in JOGL's UML document [about OpenGL Profiles](../../jogl/doc/uml/html/index.html).


## Struct Mapping
A *Struct* is a C compound type declaration, which can be mapped to a Java class.

A *Struct* may utilize the following data types for its fields
* *Primitive*, i.e. *char*, *int32_t*, ...
  * See [*Primitive Mapping*](#primitive-mapping) above.
  * See [*Opaque and `void*` notes*](#struct-mapping-notes) below.
  * See [*Pointer Mapping*](#pointer-mapping) for *pointer-to-pointer* values above and [Struct Pointer-Pointer Support](#struct-pointer-pointer-support) below.
  * See [*String Mapping*](#string-mapping) above.
* *Struct*, i.e. an aggregated or referenced compound variable
* *Function Pointer*, a *typedef*'ed and set callable function pointer, see [Struct Function-Pointer Support](#struct-function-pointer-support) below.
* *Java Callback from Native Code*, see [section below](#java-callback-from-native-c-api-support)

A field may be a direct aggregation, i.e. instance, within the struct including an array
or a reference to a single element or array via a pointer.

Both, *primitive*, *struct* and *pointer* field type mappings only produce pure Java code, utilizing the *GlueGen Runtime*.
Hence no additional native code must be compiled nor a resulting additional library loaded to use the mapping.

Only when mapping *function-pointer* within *structs*, additional native glue-code is produced to 
call the underlying native function which has to be compiled and its library loaded.

The generated method `public static boolean usesNativeCode()` can be used to validate
whether the produced Java class requires a corresponding library for additional native code.

### Struct Mapping Notes

* [`Opaque` configured pointer-types](#opaque-java-primitive-type-symbol) are treated as `long` values from the Java side    
  while maintaining their architecture dependent pointer size within native memory.
  
* Void pointers, i.e. `void*`, within a struct are handled as [`Opaque` configured pointer-types](#opaque-java-primitive-type-symbol).

* *ConstElemCount* via **ReturnedArrayLength \<int\>** implies *native ownership* for a *Pointer* referenced *native* memory
  if the expression is constant. Otherwise the *native* memory has *java ownership*.
  See [ReturnedArrayLength Setting](#returnedarraylength-symbol-expression) below.

* Utilizing a *flexible* *elemCount* via **ReturnedArrayLength getValElements()** renders us unable to determine ownership
  of pointer referenced *native* memory segment and hence renders ownership *mixed or ambiguous*, [see \[5\]](#signature-const-int32_t--customsize-ambiguous-java-owned). This is due to the fact, that native code may allocate memory and writes its *elemCount* into the designated field *valElements*. In such cases, the user being aware of the underlying API shall utilize `setVal(..)` and `releaseVal()` with care.
  
* To release native memory with *java ownership*, i.e. a native ByteBuffer, `releaseVal()` can be used.

### GlueGen Struct Settings

#### **Opaque** *Java-primitive-type* *symbol*

See also [Opaque section in manual](manual/index.html#SecOpaque).

* `Opaque long T2_UndefStruct*`

    Pointers to `T2_UndefStruct` will be handled opaque, 
    i.e. as `long` values from the Java side while maintaining their architecture dependent pointer size within native memory.

#### **ImmutableAccess** *symbol*
Immutable access can be set for a whole struct or a single field of a struct.

Immutable access will simply suppress generating setters in the Java code
and hence also reduces the footprint of the generated Java class for such struct.

* `ImmutableAccess TK_Struct`

    Immutable access for the whole struct `TK_Struct
    
    Sets pseudo-code flag *ImmutableAccess*, see below.

* `ImmutableAccess TK_Struct.val`

    Immutable access for the single field `val` within struct `TK_Struct`
    
    Sets pseudo-code flag *ImmutableAccess*, see below.

#### **MaxOneElement** *symbol*
* `MaxOneElement TK_Struct.val`

    Sets field pointer `val`
    to point to a array with a maximum of one element and unset initial value (zero elements).

    Sets pseudo-code flag *MaxOneElement*, see below.

#### **ReturnedArrayLength** *symbol* *expression*
* `ReturnedArrayLength TK_Struct.val 3`

    Sets field pointer `val` to point to a array with three elements.

    Sets pseudo-code flag *ConstElemCount*, see below.
    
    Having set *ConstElemCount* also implies *native ownership* for a *Pointer* referenced *native* memory.

* `ReturnedArrayLength TK_Struct.val 1`

    Sets field pointer `val` to point to a array with one element.

    Sets pseudo-code flags *ConstElemCount* and *MaxOneElement*, see below.
    
    Having set *ConstElemCount* also implies *native ownership* for a *Pointer* referenced *native* memory.

* `ReturnedArrayLength TK_Struct.val getValElements()`

    Sets field pointer `val` to point to a array with a variable length as described by the 
    field `valElements` retrievable via its getter `getValElements()`.

    Sets pseudo-code flag *VariaElemCount*, see below.

#### **ReturnsString** *symbol*
A direct C code `char` array or indirect array via pointer can be interpreted as a Java `String`.

* `ReturnsString TK_Struct.name`

    Sets field char-array or char-pointer `name` to be additionally interpreted as a Java `String`.
    Besides the `byte[]` and `ByteBuffer` getter and setter variants, a `String` variant will be added.

    Sets pseudo-code flags *String*, see below.
    
    See [*String Mapping*](#string-mapping) above.

#### **ReturnsStringOnly** *symbol*

* `ReturnsStringOnly TK_Struct.name`

    Sets field char-array or char-pointer `name` to be exclusively interpreted as a Java `String`.
    Instead of the `byte[]` and `ByteBuffer` getter and setter variants, a `String` variant will be produced.

    Sets pseudo-code flags *StringOnly*, see below.
    
    See [*String Mapping*](#string-mapping) above.

### Struct Setter Pseudo-Code
#### Overview
In general we have the following few cases
* Array owned by parent struct itself
  * `int32_t val[10]`
    * Setter of `val` within range, keeping memory
  * `const int32_t val[10]`
    * No setter allowed due to const value
    
* Referenced Memory (array) owned by Java
  * `int32_t* val`
    * Setter within range, keeping memory, or replacing memory
  * `const int32_t* val`
    * Setter replacing memory, since memory is non-const but value is const
* Referenced Memory (array) owned by Native Code due to set *ConstElemCount*
  * `int32_t* val`
    * Setter of `val` within range, keeping memory owned by native code
  * `const int32_t* val`
    * No setter allowed, since memory is owned by native code and value is const

#### Implemented Pseudo Code
* *ImmutableAccess*: Drops setter, immutable
* *Pointer* & *ConstValue* & *ConstElemCount*: Drops setter, native ownership on const-value
* *Array* & *ConstValue* : Drops setter, const-value array
* *Primitive*
  * Single aggregated instance 
    * Store value within *native* memory
  * *Array* | *Pointer* 
    * *MaxOneElement*
      * *Pointer*
        * *ConstValue*: Allocate new memory and store value
        * *VariaValue*:
          * *ConstElemCount*: Reuse *native* memory and store value with matching *elemCount 1*, otherwise Exception
          * *VariaElemCount*: Reuse *native* memory and store value with matching *elemCount 1*, otherwise allocates new memory (had *elemCount 0*)
      * *Array* & *VariaValue*: Reuse *native* memory and store value (has const *elemCount 1*)
      * *else*: *SKIP* setter for const single-primitive array
    * *AnyElementCount*
      * *String* & *isByteBuffer* & *Pointer*
        * *ConstElemCount*: Reuse *native* memory and store UTF-8 bytes with EOS with matching *elemCount*, otherwise Exception
          * *StringOnly*: End, no more setter for this field, otherwise continue
        * *VariaElemCount*: Allocate new *native* memory and store UTF-8 bytes with EOS
          * *StringOnly*: End, no more setter for this field, otherwise continue
      * *ConstValue*
        * *Pointer*
          * *VariaElemCount*: Allocates new *native* memory and store value
        * *else*: *SKIP* setter for const primitive array
      * *Array* | *ConstElemCount*: Reuse *native* memory and store value with <= *elemCount*, otherwise Exception
      * *Pointer* & *VariaElemCount*: Reuse *native* memory and store value with <= *elemCount*, otherwise allocate new *native* memory
* *Struct* ...

### Struct Java Signature Table

Please find below signature table as generated by the *C Declaration* including its *C Modifier*, 
e.g. `const` for constant, `[const]` for const and non-const and `empty` for non-const (variable).

Further, the *GlueGen Setting* (see above) impacts the code generation as well.

Below table demonstrates *primitive* types being mapped within a `struct` named `TK_Struct`.
A similar mapping is produced for `struct` types, i.e. *compounds*.


| C Mod   | C Declaration | Java Setter                                                      | Java Getter                           | GlueGen Setting                                | Ownership | Remarks   |
|:--------|:--------------|:-----------------------------------------------------------------|:--------------------------------------|:-----------------------------------------------|:----------|:----------|
|         |               |                                                                  | static boolean usesNativeCode()       |                                                |           | Java, static, <br> *true* if using native code |
|         |               |                                                                  | static int size()                     |                                                |           | Java, static, <br> native size in bytes |
|         |               |                                                                  | static TK_Struct create()             |                                                |           | Java, static ctor |
|         |               |                                                                  | static TK_Struct create(ByteBuffer)   |                                                |           | Java, static ctor <br> w/ existing ByteBuffer |
|         |               |                                                                  | static TK_Struct derefPointer(long addr) |                                                |           | Java, static ctor <br> dereferencing ByteBuffer <br> at native address of size() |
|         |               |                                                                  | ByteBuffer getBuffer()                |                                                |           | Java, <br> underlying ByteBuffer |
|         |               |                                                                  | long getDirectBufferAddress()         |                                                |           | Java, native address <br> of underlying getBuffer() |
|         | int32_t val   | setVal(int v)                                                    | int getVal()                          |                                                | Parent    |           |
|  const  | int32_t val   | *none*                                                           | int getVal()                          |                                                | Parent    | Read only |
|         | int32_t val   | *none*                                                           | int getVal()                          | **ImmutableAccess**                            | Parent    | Read only |
| [const] | int32_t* val  | setVal(int v) \[[1](#signature-int32_t--maxoneelement-java-owned)\]\[[2](#signature-const-int32_t--maxoneelement-java-owned)\] <br> releaseVal() | int getVal() <br> boolean isValNull() <br> int getValElemCount()  | **MaxOneElement**  | Java      | Starts w/ null elements,<br>max 1 element |
|  const  | int32_t* val  | *none*                                                           | int getVal() <br> boolean isValNull() <br> static int getValElemCount()  | **ReturnedArrayLength 1** | Native | Const element count 1 |
|         | int32_t* val  | setVal(int v)                                                    | int getVal() <br> boolean isValNull() <br> static int getValElemCount()  | **ReturnedArrayLength 1** | Native | Const element count 1 |                     |
|         | int32_t val[3]| setVal(int[] src, int srcPos, int destPos, int len) \[[3](#signature-int32_t3-constelemcount-3-parent-owned)\] | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len)   | | Parent | Reuses parent memory,<br>fixed size. |
|  const  | int32_t val[3]| *none*                                                           | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len)   | | Parent | Read only |
|  const  | int32_t* val  | *none*                                                           | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() <br> static int getValElemCount() | **ReturnedArrayLength 3** | Native | Read only <br> Const element count 3 |
|         | int32_t* val  | setVal(int[] src, int srcPos, int destPos, int len) \[[4](#signature-int32_t--constelemcount-3-natively-owned)\] | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() <br> static int getValElemCount() | **ReturnedArrayLength 3** | Native | Const element count 3.<br>Reuses native memory,<br>fixed size. |
|         | int32_t* val  | setVal(boolean subset, int[] src, int srcPos, int destPos, int len) \[[5](#signature-int32_t--freesize-java-owned)\] <br> releaseVal() | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() <br> int getValElemCount() |  | Java | Starts w/ null elements.<br>Reuses or replaces Java memory,<br>variable size.  |
|  const  | int32_t* val  | setVal(int[] src, int srcPos, int len) \[[6](#signature-const-int32_t--freesize-java-owned)\] <br> releaseVal() | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() <br> int getValElemCount() |  | Java | Starts w/ null elements.<br>Replaces Java memory,<br>variable size. |
|         | int32_t* val  | setVal(boolean subset, int[] src, int srcPos, int destPos, int len) \[[7](#signature-int32_t--customsize-ambiguous-ownership)\] <br> releaseVal() | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() | **ReturnedArrayLength getValCount()** | **Ambiguous** | Variable element count<br>using field *valCount*,<br>which has getter and setter  |
|  const  | int32_t* val  | setVal(int[] src, int srcPos, int len) \[[8](#signature-const-int32_t--customsize-ambiguous-ownership)\] <br> releaseVal() | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() | **ReturnedArrayLength getValCount()** | **Ambiguous** | Variable element count<br>using field *valCount*,<br>which has getter and setter  |
| [const] | char* name    | setName(String srcVal) <br> releaseVal()                         | String getName() <br> boolean isNameNull() <br> int getNameElemCount() | **ReturnsStringOnly** | Java | String only, w/ EOS  |
| [const] | char* name    | setName(String srcVal) <br> setName(byte[] src, int srcPos, int destPos, int len) <br> releaseVal() | String getNameAsString() <br> ByteBuffer getName() <br> boolean isNameNull() <br> int getNameElemCount() | **ReturnsString** | Java | String and byte access, w/ EOS|

### Struct Java Signature Examples

#### Signature `int32_t *` MaxOneElement, Java owned 

* `void com.jogamp.gluegen.test.junit.generation.TK_Field.setVariaInt32PointerMaxOneElemElemCount(int src)`

    Setter for native field variaInt32PointerMaxOneElem, referencing a Java owned array with variable element count of 0 initial elements.

    Maximum element count is 1.

    Native Signature:
    * field-type (PointerType) 'int32_t *' -> (int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1
    * referenced (IntType) typedef 'int32_t', size [fixed true, lnx64 4], const[false], int

    Will reuse memory if existing, otherwise allocating memory.

#### Signature `const int32_t *` MaxOneElement, Java owned 

* `TK_Field com.jogamp.gluegen.test.junit.generation.TK_Field.setConstInt32PointerMaxOneElem(int src)`

    Setter for native field variaInt32PointerMaxOneElem, referencing a Java owned array with variable element count of 0 initial elements.

    Maximum element count is 1.

    Native Signature:
    * field-type (PointerType) 'int32_t *' -> (const int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1
    * referenced (IntType) typedef 'int32_t', size [fixed true, lnx64 4], const[native, true], int

    Always replaces memory due to `const` value modifier.
    
#### Signature `int32_t[3]` ConstElemCount 3, Parent owned     
* `TK_Field com.jogamp.gluegen.test.junit.generation.TK_Field.setVariaInt32ArrayConstLen(int[] src, int srcPos, int destPos, int length)`

    Setter for native field variaInt32ArrayConstLen, being an array with fixed element count of 3 elements.

    Native Field Signature (ArrayType) 'int32_t *', size [fixed false, lnx64 12], const[false], array*1

    Copies the given source elements into the respective field's existing memory.

    Parameters:
    * src the source array of elements
    * srcPos starting element position within the source array with 'srcPos >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
    * destPos starting element position within the destination with 'destPos >= 0` && `destPos + length <= elemCount`, otherwise an exception is thrown
    * length the element count to be copied with 'length >= 0` && `srcPos + length <= src.length` && `destPos + length <= elemCount`, otherwise an IndexOutOfBoundsException is thrown
    
    Returns:
    * this instance of chaining

#### Signature `int32_t *` ConstElemCount 3, Natively owned 

* `TK_Field com.jogamp.gluegen.test.junit.generation.TK_Field.setVariaInt32PointerConstLen(int[] src, int srcPos, int destPos, int length)`

    Setter for native field variaInt32PointerConstLen, referencing a natively owned array with fixed element count of 3 elements.

    Native Signature:
    * field-type (PointerType) 'int32_t *' -> (int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1
    * referenced (IntType) typedef 'int32_t', size [fixed true, lnx64 4], const[false], int
    
    Copies the given source elements into the respective field's existing memory.

    Parameters:
    * src the source array of elements
    * srcPos starting element position within the source array with 'srcPos >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
    * destPos starting element position within the destination with 'destPos >= 0` && `destPos + length <= elemCount`, otherwise an exception is thrown
    * length the element count to be copied with 'length >= 0` && `srcPos + length <= src.length` && `destPos + length <= elemCount`, otherwise an IndexOutOfBoundsException is thrown
    
    Returns:
    * this instance of chaining

#### Signature `int32_t *` FreeSize, Java owned 
* `TK_Field com.jogamp.gluegen.test.junit.generation.TK_Field.setVariaInt32PointerVariaLen(boolean subset, int[] src, int srcPos, int destPos, int length)`

    Setter for native field variaInt32PointerVariaLen, referencing a Java owned array with variable element count of 0 initial elements.

    Native Signature:
    * field-type (PointerType) 'int32_t *' -> (int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1
    * referenced (IntType) typedef 'int32_t', size [fixed true, lnx64 4], const[false], int
    
    Copies the given source elements into the respective field, either writing into the existing memory or creating a new memory and referencing it.

    Parameters:
    * subset if `true` keeps the underlying memory and only allows to set up to `elemCount` elements. Otherwise may replace the underlying memory if `destPos + length != elemCount`.
    * src the source array of elements
    * srcPos starting element position within the source array with 'srcPos >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
    * destPos starting element position within the destination with 'destPos >= 0`. If `subset == true`, `destPos + length <= elemCount` also must be be `true`. Otherwise an exception is thrown
    * length the element count to be copied with 'length >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
    
    Returns:
    * this instance of chaining

#### Signature `const int32_t *` FreeSize, Java owned 
* `TK_Field com.jogamp.gluegen.test.junit.generation.TK_Field.setConstInt32PointerVariaLen(int[] src, int srcPos, int length)`

    Setter for native field constInt32PointerVariaLen, referencing a Java owned array with variable element count of 0 initial elements.

    Native Signature:
    * field-type (PointerType) 'int32_t *' -> (const int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1
    * referenced (IntType) typedef 'int32_t', size [fixed true, lnx64 4], const[native, true], int
    
    Replaces the respective field's memory with a new memory segment containing given source elements and referencing it.

    Parameters:
    * src the source array of elements
    * srcPos starting element position within the source array with 'srcPos >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
    * length the element count to be copied with 'length >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
    
    Returns:
    * this instance of chaining

#### Signature `int32_t *` CustomSize, Ambiguous ownership
* `TK_Field com.jogamp.gluegen.test.junit.generation.TK_Field.setVariaInt32PointerCustomLen(boolean subset, int[] src, int srcPos, int destPos, int length)`

    Setter for native field variaInt32PointerCustomLen, referencing a mixed and ambigously owned (warning) array with variable element count of getVariaInt32PointerCustomLenElemCount() elements.

    Native Signature:
    * field-type (PointerType) 'int32_t *' -> (int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1
    * referenced (IntType) typedef 'int32_t', size [fixed true, lnx64 4], const[false], int
    
    Copies the given source elements into the respective field, either writing into the existing memory or creating a new memory and referencing it.

    Parameters:
    * subset if `true` keeps the underlying memory and only allows to set up to `elemCount` elements. Otherwise may replace the underlying memory if `destPos + length != elemCount`.
    * src the source array of elements
    * srcPos starting element position within the source array with 'srcPos >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
    * destPos starting element position within the destination with 'destPos >= 0`. If `subset == true`, `destPos + length <= elemCount` also must be be `true`. Otherwise an exception is thrown
    * length the element count to be copied with 'length >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
    
    Returns:
    * this instance of chaining  
    
#### Signature `const int32_t *` CustomSize, Ambiguous ownership
* `TK_Field com.jogamp.gluegen.test.junit.generation.TK_Field.setConstInt32PointerCustomLen(int[] src, int srcPos, int length)`

  Setter for native field constIntxxPointerCustomLen, referencing a mixed and ambigously owned (**warning**) array with variable element count of getConstIntxxPointerCustomLenElemCount() elements.

  Native Signature:
  * field-type (PointerType) 'int32_t *' -> (const int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1
  * referenced (IntType) typedef 'int32_t', size [fixed true, lnx64 4], const[native, true], int
  
  Replaces the respective field's memory with a new memory segment containing given source elements and referencing it.

  Parameters:
  * src the source array of elements
  * srcPos starting element position within the source array with 'srcPos >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
  * length the element count to be copied with 'length >= 0` && `srcPos + length <= src.length`, otherwise an IndexOutOfBoundsException is thrown
  
  Returns:
  * this instance of chaining

### Struct Pointer-Pointer Support
See primitive [*Pointer Mapping*](#pointer-mapping) above.

*Pointer* are exposed in the following examples
```
typedef struct {
  int32_t* int32PtrArray[10];
  int32_t** int32PtrPtr;

  ...
} T2_PointerStorage;
```

or via and undefined forward-declared struct 
```
typedef struct T2_UndefStruct* T2_UndefStructPtr;

typedef struct {
  ...

  T2_UndefStructPtr undefStructPtr;
  T2_UndefStructPtr undefStructPtrArray[10];
  T2_UndefStructPtr* undefStructPtrPtr;
  const T2_UndefStructPtr* constUndefStructPtrPtr;
} T2_PointerStorage;
```

and the following GlueGen configuration
```
Opaque long T2_UndefStruct*
Ignore T2_UndefStruct
```

*TODO: Enhance documentation*

### Struct Function-Pointer Support
GlueGen supports function pointers as struct fields,    
generating function calls as methods as well function-pointer opaque getter and setter as `long` types.    
The latter only in case if mutable, i.e. non-const.

#### Example
Assume the following C Header file example:
```
typedef struct {
    int32_t balance;
} T2_UserData;

typedef int32_t ( * T2_CustomFuncA)(void* aptr);

typedef int32_t ( * T2_CustomFuncB)(T2_UserData* pUserData);

typedef struct {
  ...
  
  T2_CustomFuncA customFuncAVariantsArray[10];
  T2_CustomFuncA* customFuncAVariantsArrayPtr;

  T2_CustomFuncB customFuncBVariantsArray[10];
  T2_CustomFuncB* customFuncBVariantsArrayPtr;
} T2_PointerStorage;

typedef struct {
  ...
  
  const T2_CustomFuncA CustomFuncA1;
  T2_CustomFuncB CustomFuncB1;
} T2_InitializeOptions;
```

and the following GlueGen configuration
```
Opaque long void* 

EmitStruct T2_UserData
StructPackage T2_UserData com.jogamp.gluegen.test.junit.generation
    
EmitStruct T2_InitializeOptions
StructPackage T2_InitializeOptions com.jogamp.gluegen.test.junit.generation
```

This will lead to the following result for `const T2_CustomFuncA customFuncA1`
```
  /**
   * Getter for native field <code>CustomFuncA1</code>, being a <i>struct</i> owned function pointer.
   * <p>
   * Native Field Signature <code>(PointerType) typedef 'T2_CustomFuncA' -> int32_t (*)(void *  aptr), size [fixed false, lnx64 8], const[false], pointer*1, funcPointer</code>
   * </p>
   */
  public final long getCustomFuncA1() { .. }
  
  /** Interface to C language function: <br> <code>int32_t CustomFuncA1(void *  aptr)</code><br>   */
  public final int CustomFuncA1(long aptr)  { ... }  
```

and similar to `T2_CustomFuncB customFuncB1`
```
  /**
   * Setter for native field <code>CustomFuncB1</code>, being a <i>struct</i> owned function pointer.
   * <p>
   * Native Field Signature <code>(PointerType) typedef 'T2_CustomFuncB' -> int32_t (*)(T2_UserData *  pUserData), size [fixed false, lnx64 8], const[false], pointer*1, funcPointer</code>
   * </p>
   */
  public final T2_InitializeOptions setCustomFuncB1(long src) { .. }

  /**
   * Getter for native field <code>CustomFuncB1</code>, being a <i>struct</i> owned function pointer.
   * <p>
   * Native Field Signature <code>(PointerType) typedef 'T2_CustomFuncB' -> int32_t (*)(T2_UserData *  pUserData), size [fixed false, lnx64 8], const[false], pointer*1, funcPointer</code>
   * </p>
   */
  public final long getCustomFuncB1() { .. }
  
  /** Interface to C language function: <br> <code>int32_t CustomFuncB1(T2_UserData *  pUserData)</code><br>   */
  public final int CustomFuncB1(T2_UserData pUserData)  { .. }  
```

## Java Callback from Native C-API Support
GlueGen supports registering Java callback methods
to receive asynchronous and off-thread native toolkit events,
where a generated native callback function dispatches the events to Java.

### Required *LibraryOnLoad*
Note that [`LibraryOnLoad Bindingtest2`](#libraryonload-librarybasename-for-jni_onload-) must be specified in exactly one native code-unit.
It provides code to allow the generated native callback-function to attach the current thread to the `JavaVM*` generating a new `JNIEnv*`in daemon mode -
or just to retrieve the thread's `JNIEnv*`, if already attached to the `JavaVM*`.

### *JavaCallback* Configuration

Configuration directives are as follows:

    JavaCallbackDef  <SetCallbackFunctionName> <CallbackFunctionType> <CallbackFunction-UserParamIndex> [<SetCallback-KeyClassName>]    
    JavaCallbackKey  <SetCallbackFunctionName> (SetCallback-ParamIdx)*
    
`JavaCallbackDef` and `JavaCallbackKey` use the name of the `SetCallbackFunction` as its first attribute,
as it is core to the semantic mapping of all resources.

`JavaCallbackDef` attributes:
- `SetCallbackFunction`: `SetCallbackFunction` name of the native toolkit API responsible to set the callback
- `CallbackFunctionType`: The native toolkit API typedef-name of the function-pointer-type, aka the callback type name
- `CallbackFunction-UserParamIndex`: The `userParam` parameter-index of the `CallbackFunctionType`
- `SetCallback-KeyClassName`: Name of an optional user-implemented `SetCallback-KeyClass`, providing the hash-map-key - see below

The `SetCallbackFunction` is utilized to set the `CallbackFunction` as well as to remove it passing `null` for the `CallbackFunction`.

If mapping the `CallbackFunction` to keys, the user must specify the same key arguments when setting and removing the ``CallbackFunction`.

#### *JavaCallback* Key Definition

If no keys are defined via `JavaCallbackKey` or not manually injected using a custom `SetCallback-KeyClass`, see below,
the `CallbackFunction` has global scope.

In case keys are defined via `JavaCallbackKey` and no manually injected custom `SetCallback-KeyClass` used, 
a public `SetCallback-KeyClass` is being generated covering the defined keys.

Keys allow to limit the scope, i.e. map multiple `CallbackFunction` to the different keys.

Key arguments must match in `SetCallbackFunction` to remove a previously set `CallbackFunction`.

`JavaCallbackKey` attributes
- `SetCallbackFunction`: `SetCallbackFunction` name of the native toolkit API responsible to set the callback
- `SetCallback-ParamIdx`: List of parameter indices of the `SetCallbackFunction`, denoting the key(s) limiting the callback scope, i.e. the callback and all resources will be mapped to this key. The optional `SetCallback-KeyClass` may override this semantic.

#### *JavaCallback* Generated Interfaces, Classes and Methods

The public `CallbackFunction` interface is generated.

The default public `SetCallback-KeyClass` is generated if keys are used and no custom class is specified, see above.

The public toolkit API `SetCallbackFunction` method is being generated.

Additional public *maintenance* methods are generated. In case keys are being used, they expect `SetCallback-KeyClass` as an argument, otherwise they expect no argument for global scope.

In case a `SetCallback-KeyClass` is used, the additional *maintenance* methods are:
- *Set<`SetCallback-KeyClass`> get`SetCallbackFunctionName`Keys()*
- *boolean is`SetCallbackFunctionName`Mapped(`SetCallback-KeyClass`)* queries whether `SetCallbackFunctionName` is mapped to key.
- *`CallbackFunction` get`SetCallbackFunctionName`(`SetCallback-KeyClass`)* returns the mapped `CallbackFunction`, null if not mapped
- *Object get`SetCallbackFunctionName`UserParam(`SetCallback-KeyClass`)* returns the mapped `userParam` object, null if not mapped
- *void release`SetCallbackFunctionName`(`SetCallback-KeyClass`)* releases the mapped `CallbackFunction` data set associated via `SetCallbackFunctionName`.
- *int releaseAll`SetCallbackFunctionName`()* releases complete mapped `CallbackFunction` data set associated via `SetCallbackFunctionName`.

If no `SetCallback-KeyClass` is used, the additional *maintenance* methods are:
- *boolean is`SetCallbackFunctionName`Mapped()* queries whether `SetCallbackFunctionName` is mapped.
- *`CallbackFunction` get`SetCallbackFunctionName`()* returns the mapped `CallbackFunction`, null if not mapped
- *Object get`SetCallbackFunctionName`UserParam()* returns the mapped `userParam` object, null if not mapped
- *void release`SetCallbackFunctionName`()* releases the mapped `CallbackFunction` data set associated via `SetCallbackFunctionName`.

Note that the *release`SetCallbackFunctionName`(\*)* and *releaseAll`SetCallbackFunctionName`()* methods are not the *proper toolkit API way* to remove the callback, 
try to use original `SetCallbackFunctionName` API method instead using a `null` `CallbackFunction` reference.

#### Custom `SetCallback-KeyClass` 

The `SetCallback-KeyClass` is the optional user-written hash-map-key definition 
and shall handle all key parameter of the `SetCallbackFunction` as defined via `JavaCallbackKey`, see above.

`SetCallback-KeyClass` may be used to add external key-components, e.g. current-thread or a toolkit dependent context.

The `SetCallback-KeyClass` shall implement the following hash-map-key standard methods
- `boolean equals(Object)` 
- `int hashCode()`
- `SetCallback-KeyClassName(...)` constructor receiving all key parameter of `SetCallbackFunction` as defined via `JavaCallbackKey`, see above.


### *JavaCallback* Notes
Please consider the following *currently enabled* constraints using JavaCallback
- Only one interface callback-method binding is allowed for a native callback function, e.g. `T2_CallbackFunc01` (see above)
  - Implying that the native single function-pointer typedef must be mapped to a single Java method within its interface
  - Hence it must be avoided that multiple method variation are produced, e.g. due to `char*` to `byte[]` and `String` mapping etc.
- The native callback function can only return no-value, i.e. `void`, or a primitive type. Usually `void` is being used in toolkit APIs.
- The native callback function argument types must be convertible to JNI Java types as (previously) supported for function return values,
  using the same conversion function `CMethodBindingEmitter.emitBodyMapCToJNIType(..)`.
- To remove a JavaCallback the `SetCallbackFunction` must be called with `null` for the `CallbackFunction` argument
  but with the same [*key arguments* (see `JavaCallbackKey`)](#javacallback-key-definition) as previously called to set the callback.
- Exactly one native code-unit for the library must specify [`LibraryOnLoad libraryBasename`](#libraryonload-librarybasename-for-jni_onload-)
- `SetCallbackFunction`, all *maintenance* methods and the native callback dispatcher **is thread safe**
- ... 

### JavaCallback Example 1
This is a generic example.

The callback `T2_CallbackFunc01` has global scope, i.e. is not mapped to any key and can be only set globally.

C-API header snippet:
```
typedef void ( * T2_CallbackFunc01)(size_t id, const char* msg, void* usrParam);

/** Sets the given `cbFunc` and associated `usrParam` as the callback. Passing NULL for `func` _and_ same `usrParam` removes the callback and its associated resources. */
void MessageCallback01(T2_CallbackFunc01 cbFunc, void* usrParam);

void InjectMessageCallback01(size_t id, const char* msg);
```

and the following GlueGen configuration
```
# JavaCallback requires `JNI_OnLoad*(..)` and `JVMUtil_GetJNIEnv(..)`
LibraryOnLoad Bindingtest2
    
ArgumentIsString T2_CallbackFunc01 1
ArgumentIsString InjectMessageCallback01 1

# Define a JavaCallback.
#   Set JavaCallback via function `MessageCallback01` if `T2_CallbackFunc01` argument is non-null, otherwise removes the mapped callback and associated resources.
#
#   It uses the function-pointer argument `T2_CallbackFunc01` as the callback function type
#   and marks `T2_CallbackFunc01`s 3rd argument (index 2) as the mandatory user-param.
#
#   This callback has no keys defines, rendering it of global scope!
#
#   Explicit maintenance methods are generated, passing the keys as paramters
#   - `boolean isMessageCallback01Mapped()` queries whether `MessageCallback0` is mapped globally
#   - `T2_CallbackFunc01 getMessageCallback01()` returns the global T2_CallbackFunc01, null if not mapped
#   - `Object getMessageCallback01UserParam()` returns the global `usrParam` object, null if not mapped
#   - `void releaseMessageCallback01()` releases callback data skipping toolkit API. Favor passing `null` callback ref to `MessageCallback01(..)`
JavaCallbackDef  MessageCallback01 T2_CallbackFunc01 2
```

Note that [`LibraryOnLoad Bindingtest2`](#libraryonload-librarybasename-for-jni_onload-) must be specified in exactly one native code-unit.
It provides code to allow the generated native callback-function to attach the current thread to the `JavaVM*` generating a new `JNIEnv*`in daemon mode -
or just to retrieve the thread's `JNIEnv*`, if already attached to the `JavaVM*`.

This will lead to the following interface
```
public interface Bindingtest2 {

  /** JavaCallback interface: T2_CallbackFunc01 -> void (*T2_CallbackFunc01)(size_t id, const char *  msg, void *  usrParam) */
  public static interface T2_CallbackFunc01 {
    /** Interface to C language function: <br> <code>void callback(size_t id, const char *  msg, void *  usrParam)</code><br>Alias for: <code>T2_CallbackFunc01</code>     */
    public void callback(long id, String msg, Object usrParam);
  }

  ...
  
  /** Entry point (through function pointer) to C language function: <br> <code>void MessageCallback01(T2_CallbackFunc01 cbFunc, void *  usrParam)</code><br>   */
  public void MessageCallback01(T2_CallbackFunc01 cbFunc, Object usrParam);

  /** Returns if callback is mapped for <br> <code>  public void MessageCallback01(T2_CallbackFunc01 cbFunc, Object usrParam)</code> **/
  public boolean isMessageCallback01Mapped();

  /** Returns T2_CallbackFunc01 callback for <br> <code>  public void MessageCallback01(T2_CallbackFunc01 cbFunc, Object usrParam)</code> **/
  public T2_CallbackFunc01 getMessageCallback01();

  /** Returns user-param for <br> <code>  public void MessageCallback01(T2_CallbackFunc01 cbFunc, Object usrParam)</code> **/
  public Object getMessageCallback01UserParam();

  /** Releases callback data skipping toolkit API. Favor passing `null` callback ref to <br> <code>  public void MessageCallback01(T2_CallbackFunc01 cbFunc, Object usrParam)</code> **/
  public void releaseMessageCallback01();

  /** Entry point (through function pointer) to C language function: <br> <code>void InjectMessageCallback01(size_t id, const char *  msg)</code><br>   */
  public void InjectMessageCallback01(long id, String msg);
```

### JavaCallback Example 2a (Default *KeyClass*)

This examples is derived from OpenAL's `AL_SOFT_callback_buffer` extension.

The callback `ALBUFFERCALLBACKTYPESOFT` is mapped to `buffer` name, i.e. one callback can be set for each buffer.

C-API Header snipped
```
  typedef void ( * ALBUFFERCALLBACKTYPESOFT)(int buffer, void *userptr, int sampledata, int numbytes);
  
  void alBufferCallback0(int buffer /* key */, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, void *userptr);
  
  void alBufferCallback0Inject(int buffer, int sampledata, int numbytes);
```

and the following GlueGen configuration
```
  # Define a JavaCallback.
  #   Set JavaCallback via function `alBufferCallback0` if `ALBUFFERCALLBACKTYPESOFT` argument is non-null, otherwise removes the mapped callback and associated resources.
  #
  #   It uses the function-pointer argument `ALBUFFERCALLBACKTYPESOFT` as the callback function type
  #   and marks `ALBUFFERCALLBACKTYPESOFT`s 2nd argument (index 1) as the mandatory user-param.
  #
  #   This callback defines one key, `buffer`, index 0 of alBufferCallback0(..) parameter list, limiting it to buffer-name scope!
  #   The `buffer` key allows setting one callback per buffer-name, compatible with the `AL_SOFT_callback_buffer` spec.
  # 
  #   Explicit queries are generated, passing the keys as paramters
  #   - `Set<AlBufferCallback0Key> getAlBufferCallback0Keys()` returns set of Key { int buffer }
  #   - `boolean isAlBufferCallback0Mapped(AlBufferCallback0Key)` queries whether `alBufferCallback0` is mapped to `buffer`.
  #   - `ALBUFFERCALLBACKTYPESOFT getAlBufferCallback0(AlBufferCallback0Key)` returns the `buffer` mapped ALEVENTPROCSOFT, null if not mapped
  #   - `Object getAlBufferCallback0UserParam(AlBufferCallback0Key)` returns the `buffer` mapped `userptr` object, null if not mapped
  #   - `void releaseAllAlBufferCallback0()` releases all callback data mapped via Key { int buffer } skipping toolkit API. Favor passing `null` callback ref to `alBufferCallback0(..)`
  #   - `void releaseAlBufferCallback0(AlBufferCallback0Key)` releases callback data mapped to Key { int buffer } skipping toolkit API. Favor passing `null` callback ref to `alBufferCallback0(..)`
  JavaCallbackDef  alBufferCallback0 ALBUFFERCALLBACKTYPESOFT 1
  JavaCallbackKey  alBufferCallback0 0
```

leading to the following interface
```
  /** JavaCallback interface: ALBUFFERCALLBACKTYPESOFT -> void (*ALBUFFERCALLBACKTYPESOFT)(int buffer, void *  userptr, int sampledata, int numbytes) */
  public static interface ALBUFFERCALLBACKTYPESOFT {
    /** Interface to C language function: <br> <code>void callback(int buffer, void *  userptr, int sampledata, int numbytes)</code><br>Alias for: <code>ALBUFFERCALLBACKTYPESOFT</code>     */
    public void callback(int buffer, Object userptr, int sampledata, int numbytes);
  }
  
  ...
  
  /** Key { int buffer } for <br> <code>  public void alBufferCallback0(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, Object userptr)</code> **/
  public static class AlBufferCallback0Key {
    public final int buffer;
    public AlBufferCallback0Key(int buffer) {
      this.buffer = buffer;
    }
    @Override
    public boolean equals(final Object o) {
      if( this == o ) {
        return true;
      }
      if( !(o instanceof AlBufferCallback0Key) ) {
        return false;
      }
      final AlBufferCallback0Key o2 = (AlBufferCallback0Key)o;
      return buffer == o2.buffer;
    }
    @Override
    public int hashCode() {
      // 31 * x == (x << 5) - x
      int hash = buffer;
      return hash;
    }
  }
   
  ...

  /** Entry point (through function pointer) to C language function: <br> <code>void alBufferCallback0(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, void *  userptr)</code><br>   */
  public void alBufferCallback0(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, Object userptr);

  /** Returns set of Key { int buffer } for <br> <code>  public void alBufferCallback0(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, Object userptr)</code> **/
  public Set<AlBufferCallback0Key> getAlBufferCallback0Keys();

  /** Returns whether callback Key { int buffer } is mapped for <br> <code>  public void alBufferCallback0(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, Object userptr)</code> **/
  public boolean isAlBufferCallback0Mapped(AlBufferCallback0Key key);

  /** Returns ALBUFFERCALLBACKTYPESOFT callback mapped to Key { int buffer } for <br> <code>  public void alBufferCallback0(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, Object userptr)</code> **/
  public ALBUFFERCALLBACKTYPESOFT getAlBufferCallback0(AlBufferCallback0Key key);

  /** Returns user-param mapped to Key { int buffer } for <br> <code>  public void alBufferCallback0(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, Object userptr)</code> **/
  public Object getAlBufferCallback0UserParam(AlBufferCallback0Key key);

  /** Releases all callback data mapped via Key { int buffer } skipping toolkit API. Favor passing `null` callback ref to <br> <code>  public void alBufferCallback0(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, Object userptr)</code> **/
  public int releaseAllAlBufferCallback0();

  /** Releases callback data mapped to Key { int buffer } skipping toolkit API. Favor passing `null` callback ref to <br> <code>  public void alBufferCallback0(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, Object userptr)</code> **/
  public void releaseAlBufferCallback0(AlBufferCallback0Key key);

  /** Entry point (through function pointer) to C language function: <br> <code>void alEventCallbackInject(int eventType, int object, int param, const char *  msg)</code><br>   */
  public void alEventCallbackInject(int eventType, int object, int param, String msg);  
```

### JavaCallback Example 2b (Custom *KeyClass*)

Same as example 2a, but implementing a custom `SetCallback-KeyClass`.

Instead of `Callback0`, the unit `test2.*` uses `Callback1` to differentiate this case.

GlueGen configuration snippet with the added option attribute for the `SetCallback-KeyClass` in directive `JavaCallbackDef`.
```
JavaCallbackDef  alBufferCallback1 ALBUFFERCALLBACKTYPESOFT 1 com.jogamp.gluegen.test.junit.generation.Test4JavaCallback.CustomAlBufferCallback1Key
JavaCallbackKey  alBufferCallback1 0
```

Implementation utilizes a custom `SetCallback-KeyClass` implementation for `void alBufferCallback1(int buffer, int format, int freq, ALBUFFERCALLBACKTYPESOFT callback, Object userptr)`, 
which uses one key, i.e. `buffer`.
```
    public static class CustomAlBufferCallback1Key {
        private final int buffer;
        public CustomAlBufferCallback1Key(final int buffer) {
            this.buffer = buffer;
        }
        @Override
        public boolean equals(final Object o) {
            if( this == o ) {
                return true;
            }
            if( !(o instanceof CustomAlBufferCallback1Key) ) {
                return false;
            }
            final CustomAlBufferCallback1Key o2 = (CustomAlBufferCallback1Key)o;
            return buffer == o2.buffer;
        }
        @Override
        public int hashCode() {
            return buffer;
        }
        @Override
        public String toString() {
            return "CustomALKey[this "+toHexString(System.identityHashCode(this))+", buffer "+buffer+"]";
        }
    }
```

*TODO: Enhance documentation*

## Misc Configurations

### `LibraryOnLoad <LibraryBasename>` for `JNI_OnLoad*(..)` ...

`LibraryOnLoad <LibraryBasename>` generates native JNI code `JNI_OnLoad(..)` used for dynamic libraries, 
`JNI_OnLoad_<LibraryBasename>(..)` used for static libraries,
`JVMUtil_GetJNIEnv(..)` and the instance of `JavaVM* <LibraryBasename>_jvmHandle`.

The `JNI_OnLoad*(..)` methods set the `JavaVM* <LibraryBasename>_jvmHandle`, which in turn is utilized by 
`JVMUtil_GetJNIEnv(..)` to attach a new thread to the `JavaVM*` generating a new `JNIEnv*`in daemon mode -
or just to retrieve the thread's `JNIEnv*`, if already attached to the `JavaVM*`.

The `LibraryBasename` parameter is used to generate the `JNI_OnLoad_<LibraryBasename>(..)` variant for statically linked libraries.
`JNI_OnLoad(..)`, `JNI_OnLoad_<LibraryBasename>(..)` and `JVMUtil_GetJNIEnv(..)`

## Platform Header Files

GlueGen provides convenient platform headers,  
which can be included in your C header files for native compilation and GlueGen code generation.

Example:
```
   #include <gluegen_stdint.h>
   #include <gluegen_stddef.h>
 
   uint64_t test64;
   size_t size1;
   ptrdiff_t ptr1;
```

To compile this file you have to include the following folder to your compilers system includes, ie `-I`:
```
    gluegen/make/stub_includes/platform
```

To generate code for this file you have to include the following folder to your GlueGen `includeRefid` element:
```
    gluegen/make/stub_includes/gluegen
```

## Pre-Defined Macros

To identity a GlueGen code generation run, GlueGen defines the following macros:
```
     #define __GLUEGEN__ 2
``` 
