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

It reads ANSI C header files
and separate configuration files which provide control over many
aspects of the glue code generation. GlueGen uses a complete ANSI C
parser and an internal representation (IR) capable of representing all
C types to represent the APIs for which it generates interfaces. It
has the ability to perform significant transformations on the IR
before glue code emission. 

GlueGen can produce native foreign function bindings to Java as well as
map native data structures to be fully accessible from Java including 
potential calls to embedded function pointer.

GlueGen is also capable to bind even low-level APIs such as the Java Native Interface (JNI) and
the AWT Native Interface (JAWT) back up to the Java programming language.

GlueGen utilizes [JCPP](https://jogamp.org/cgit/jcpp.git/about/), migrated C preprocessor written in Java.

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
     
## Struct Mapping
A *Struct* is a C compound type declaration, which can be mapped to a Java class.

A *Struct* may utilize the following data types for its fields
* *Primitive*, i.e. *char*, *int32_t*, ...
  * See [*Primitive Mapping*](#primitive-mapping) above.
  * See [*String Mapping*](#string-mapping) above.
* *Struct*, i.e. another compound variable
* *Function Pointer*, a *typedef*'ed and set callable function pointer

A field may be a direct aggregation, i.e. instance, within the struct including an array
or a reference to a single element or array via a pointer.

Both, *primitive* and *struct* field type mappings only produce pure Java code, utilizing the *GlueGen Runtime*.
Hence no additional native code must be compiled nor a resulting additional library loaded to use the mapping.

Only when mapping *function-pointer* within *structs*, additional native glue-code is produced to 
call the underlying native function which has to be compiled and its library loaded.

The generated method `public static boolean usesNativeCode()` can be used to validate
whether the produced Java class requires a corresponding library for additional native code.

### GlueGen Struct Settings

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

### Struct Mapping Notes

* *ConstElemCount* via **ReturnedArrayLength \<int\>** implies *native ownership* for a *Pointer* referenced *native* memory
  if the expression is constant. Otherwise the *native* memory has *java ownership*.
  See [ReturnedArrayLength Setting](#returnedarraylength-symbol-expression) above.

* To release native memory with *java ownership*, i.e. a native ByteBuffer, `releaseVal()` can be used.
    
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
|         | int32_t val   | setVal(int v)                                                    | int getVal()                          |                                                | Static    |           |
|  const  | int32_t val   | *none*                                                           | int getVal()                          |                                                | Static    | Read only |
|         | int32_t val   | *none*                                                           | int getVal()                          | **ImmutableAccess**                            | Static    | Read only |
| [const] | int32_t* val  | setVal(int v) \[[1](#signature-int32_t--maxoneelement-java-owned)\]\[[2](#signature-const-int32_t--maxoneelement-java-owned)\] <br> releaseVal()                                  | int getVal() <br> boolean isValNull() <br> int getValElemCount()  | **MaxOneElement**  | Java      | Starts w/ null elements,<br>max 1 element |
|  const  | int32_t* val  | *none*                                                           | int getVal() <br> boolean isValNull() <br> static int getValElemCount()  | **ReturnedArrayLength 1** | Native | Const element count 1 |
|         | int32_t* val  | setVal(int v)                                                    | int getVal() <br> boolean isValNull() <br> static int getValElemCount()  | **ReturnedArrayLength 1** | Native | Const element count 1 |                     |
|         | int32_t val[3]| setVal(int[] src, int srcPos, int destPos, int len)              | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len)   | | Static |           |
|  const  | int32_t val[3]| *none*                                                           | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len)   | | Static | Read only |
|  const  | int32_t* val  | *none*                                                           | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() <br> static int getValElemCount() | **ReturnedArrayLength 3** | Native | Read only <br> Const element count 3 |
|         | int32_t* val  | setVal(int[] src, int srcPos, int destPos, int len)              | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() <br> static int getValElemCount() | **ReturnedArrayLength 3** | Native | Const element count 3 |
|         | int32_t* val  | setVal(boolean subset, int[] src, int srcPos, int destPos, int len) <br> releaseVal() | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() <br> int getValElemCount() |  | Java | Starts w/ null elements |
| const   | int32_t* val  | setVal(int[] src, int srcPos, int destPos, int len) <br> releaseVal() | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() <br> int getValElemCount() |  | Java | Starts w/ null elements |
| [const] | int32_t* val  | setVal(int[] src, int srcPos, int destPos, int len) <br> releaseVal() | IntBuffer getVal() <br> int[] getVal(int srcPos, int[] dest, int destPos, int len) <br> boolean isValNull() | **ReturnedArrayLength getValCount()** | *Ambiguous* | Variable element count<br>using field *valCount*,<br>which has getter and setter  |
| [const] | char* name    | setName(String srcVal) <br> releaseVal()                         | String getName() <br> boolean isNameNull() <br> int getNameElemCount() | **ReturnsStringOnly** | Java | String only, w/ EOS  |
| [const] | char* name    | setName(String srcVal) <br> setName(byte[] src, int srcPos, int destPos, int len) <br> releaseVal() | String getNameAsString() <br> ByteBuffer getName() <br> boolean isNameNull() <br> int getNameElemCount() | **ReturnsString** | Java | String and byte access, w/ EOS|

#### Signature `int32_t *` MaxOneElement, Java owned 
```
  /**
   * Setter for native field <code>variaInt32PointerMaxOneElem</code>, referencing an array with initial element count of <code>0</code>. Maximum element count is <code>1</code>.
   * <p>
   * NativeSig <code>(PointerType) 'int32_t *' -> (int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1</code>
   * </p>
   */
  public final TK_Field setVariaInt32PointerMaxOneElem(int src) { .. }
```

Will reuse memory if existing, otherwise allocating memory.

#### Signature `const int32_t *` MaxOneElement, Java owned 
```
  /**
   * Setter for native field <code>constInt32PointerMaxOneElem</code>, referencing an array with initial element count of <code>0</code>. Maximum element count is <code>1</code>.
   * <p>
   * NativeSig <code>(PointerType) 'int32_t *' -> (const int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1</code>
   * </p>
   */
  public final TK_Field setConstInt32PointerMaxOneElem(int src) { .. }
  
```

Always replaces memory due to `const` value modifier.

#### Signature `int32_t *` ConstElemCount 3, Natively owned 
```
  /**
   * Setter for native field <code>variaInt32PointerConstLen</code>, referencing a natively owned array with fixed element count of <code>3</code>.
   * <p>
   * NativeSig <code>(PointerType) 'int32_t *' -> (int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1</code>
   * </p>
   * <p>
   * Copies the given source elements into the respective field's existing memory.
   * </p>
   * @param src the source array of elements
   * @param srcPos starting element position within the source array with 'srcPos >= 0` &&  `srcPos + length <= src.length`, otherwise an {@link IndexOutOfBoundsException} is thrown
   * @param destPos starting element position within the destination with 'destPos >= 0` && `destPos + length <= elemCount`, otherwise an exception is thrown
   * @param length the element count to be copied with 'length >= 0` &&  `srcPos + length <= src.length` && `destPos + length <= elemCount`, otherwise an {@link IndexOutOfBoundsException} is thrown
   * @return this instance of chaining
   */
  public final TK_Field setVariaInt32PointerConstLen(int[] src, final int srcPos, final int destPos, final int length) { .. }
```

#### Signature `int32_t *` FreeSize, Java owned 
```
  /**
   * Setter for native field <code>variaInt32PointerVariaLen</code>, referencing an array with initial element count of <code>0</code>.
   * <p>
   * NativeSig <code>(PointerType) 'int32_t *' -> (int32_t) * , size [fixed false, lnx64 8], const[false], pointer*1</code>
   * </p>
   * <p>
   * Copies the given source elements into the respective field, either writing into the existing memory or creating a new memory and referencing it.
   * </p>
   * @param subset if `true` keeps the underlying memory and only allows to set up to `elemCount` elements. Otherwise may replace the underlying memory if `destPos + length != elemCount`.
   * @param src the source array of elements
   * @param srcPos starting element position within the source array with 'srcPos >= 0` &&  `srcPos + length <= src.length`, otherwise an {@link IndexOutOfBoundsException} is thrown
   * @param destPos starting element position within the destination with 'destPos >= 0`. If `subset == true`, `destPos + length <= elemCount` also must be be `true`. Otherwise an exception is thrown
   * @param length the element count to be copied with 'length >= 0` &&  `srcPos + length <= src.length`, otherwise an {@link IndexOutOfBoundsException} is thrown
   * @return this instance of chaining
   */
  public final TK_Field setVariaInt32PointerVariaLen(final boolean subset, int[] src, final int srcPos, final int destPos, final int length) { .. }  
```

### Struct Setter Pseudo-Code

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
