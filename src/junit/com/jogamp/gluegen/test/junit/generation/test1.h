
#if defined(__WIN32__)
#  if defined (__MYAPI_EXPORT_)
#    define MYAPI    __declspec(dllexport)
#  else
#    define MYAPI    __declspec(dllimport)
#  endif
#  define MYAPIENTRY_NOPE __stdcall /* we don't use __stdcall convention, ie @nn */
#  define MYAPIENTRY 
#else
#  if defined (__MYAPI_EXPORT_)
#    define MYAPI extern
#  else
#    define MYAPI
#  endif
#  define MYAPIENTRY
#endif

#ifndef MYAPIENTRYP
#define MYAPIENTRYP MYAPIENTRY *
#endif

#define CONSTANT_ONE 1
#define ARRAY_SIZE 8

#include <gluegen_stdint.h>
#include <gluegen_stddef.h>

typedef int Bool;
typedef uint64_t foo;
typedef void * APtr1Type;
typedef intptr_t APtr2Type;

/** Returns 42 */
MYAPI foo MYAPIENTRY nopTest();

//
// Different pointer type tests ..
//

MYAPI void * MYAPIENTRY createAPtrBlob ();
MYAPI void MYAPIENTRY releaseAPtrBlob (void * blob);
MYAPI intptr_t MYAPIENTRY getAPtrAddress (const void * immutable );
MYAPI void * MYAPIENTRY getAPtrMemory (const intptr_t address );

MYAPI void *   MYAPIENTRY arrayTestAVoidPtrTypeDim0 (const void * immutable );
MYAPI void     MYAPIENTRY arrayTestAVoidPtrTypeDim1Mutable  (void ** mutable );
MYAPI void *   MYAPIENTRY arrayTestAVoidPtrTypeDim1Immutable  (const void ** immutable );

MYAPI intptr_t MYAPIENTRY arrayTestAIntPtrTypeDim0  (intptr_t immutable);
MYAPI void     MYAPIENTRY arrayTestAIntPtrTypeDim1Mutable  (intptr_t * mutable );
MYAPI intptr_t MYAPIENTRY arrayTestAIntPtrTypeDim1Immutable(const intptr_t * immutable );

MYAPI APtr1Type MYAPIENTRY arrayTestAPtr1TypeDim0 (APtr1Type immutable );
MYAPI void MYAPIENTRY arrayTestAPtr1TypeDim1Mutable  (APtr1Type * mutable );
MYAPI APtr1Type MYAPIENTRY arrayTestAPtr1TypeDim1Immutable(const APtr1Type * immutable );

MYAPI APtr2Type MYAPIENTRY arrayTestAPtr2TypeDim0  (APtr2Type immutable );
MYAPI void      MYAPIENTRY arrayTestAPtr2TypeDim1Mutable  (APtr2Type * mutable );
MYAPI APtr2Type MYAPIENTRY arrayTestAPtr2TypeDim1Immutable(const APtr2Type * immutable );

/** Returns Sum(array) + context */
MYAPI int32_t MYAPIENTRY arrayTestInt32(int64_t context, int32_t * array );

/** Returns Sum(array) + context */
MYAPI int64_t MYAPIENTRY arrayTestInt64(int64_t context, int64_t * array );

/** Returns Sum(array) + context */
MYAPI foo MYAPIENTRY arrayTestFoo1(int64_t context, foo * array );

/** Returns a copy of the passed array, each element incr by 1 */
MYAPI foo * MYAPIENTRY arrayTestFoo2(const foo * array );

/** Increments each element of the passed array by 1 - IDENTITY */
MYAPI void MYAPIENTRY arrayTestFoo3(foo * array );

/** Returns a array-array of the passed array, split at ARRAY size - IDENTITY! */
MYAPI foo * * MYAPIENTRY arrayTestFoo3ArrayToPtrPtr(const foo * array);

/** Fills dest array ptr of ARRAY size with arrays (allocs) and copies content of src to it - COPY! */
MYAPI void MYAPIENTRY arrayTestFoo3CopyPtrPtrA(foo * * dest, const foo * * src);

/** Returns a the passed array-array, each element incr by 1 - IDENTITY !*/
MYAPI foo * * MYAPIENTRY arrayTestFoo3PtrPtr(foo * * array );

/** Returns 0 if ok, otherwise the linear position */
MYAPI int MYAPIENTRY arrayTestFoo3PtrPtrValidation(foo * * array, int startval);

/** Returns *((foo *)object) */
MYAPI foo MYAPIENTRY bufferTest(void * object);

/** Returns Sum(array) + context + *((foo *)object) */
MYAPI foo MYAPIENTRY mixedTest(int64_t context, void * object, foo * array );

/** Returns Sum(array1) + Sum(array2) + context + *((foo *)object1) + *((foo *)object2) */
MYAPI foo MYAPIENTRY doubleTest(int64_t context, void * object1, foo * array1, void * object2, foo * array2 );

/** Returns Sum(array) + context */
MYAPI foo MYAPIENTRY arrayTestFooNioOnly(int64_t context, foo * array );

/** Returns *((foo *)object) */
MYAPI foo MYAPIENTRY bufferTestNioOnly(void * object);

/** Returns *((foo *)object) */
MYAPI foo MYAPIENTRY bufferTestNioDirectOnly(void * object);

/** Returns Sum(array) + context + *((foo *)object) */
MYAPI foo MYAPIENTRY mixedTestNioOnly(int64_t context, void * object, foo * array );

/** Returns Sum(array1) + Sum(array2) + context + *((foo *)object1) + *((foo *)object2) */
MYAPI foo MYAPIENTRY doubleTestNioOnly(int64_t context, void * object1, foo * array1, void * object2, foo * array2 );

/** Returns atoi(str) */
MYAPI int MYAPIENTRY strToInt(const char* str);

/** Returns itoa(i) - not thread safe */
MYAPI const char * MYAPIENTRY intToStr(int i);

/** Returns the length of all strings, strings maybe NULL. */
MYAPI int MYAPIENTRY stringArrayRead(const char *  *  strings, int num);

/** Returns the number of 0xff bytes found in all binaries. */
MYAPI int MYAPIENTRY binaryArrayRead(const size_t * lengths, const unsigned char *  * binaries, int num);

/** Returns the sum of all integers, ints maybe NULL. */
MYAPI int MYAPIENTRY intArrayRead(const int *  ints, int num);

/** Copies num integer from src to dest. */
MYAPI int MYAPIENTRY intArrayCopy(int * dest, const int * src, int num);

/** Increases the elements by 1, and returns the sum 
MYAPI int MYAPIENTRY intArrayWrite(int *  *  ints, int num); */

typedef struct __MYAPIConfig * MYAPIConfig;

/** Returns the passed MYAPIConfig incremented by 1 */
MYAPI MYAPIConfig  MYAPIENTRY typeTestAnonSingle(const MYAPIConfig a);

/** Return a copy of the passed MYAPIConfig*, incremented by 1 */
MYAPI MYAPIConfig *  MYAPIENTRY typeTestAnonPointer(const MYAPIConfig * a);

#define DOUBLE_DEFINE_BRACKETS_1 ( ( int ) 1e51 )
#define DOUBLE_DEFINE_BRACKETS_2 ((int) 1e52)

#define HUGE_VALF_3        ((int) 1e53)
#define DOUBLE_DEFINE_BRACKETS_3 HUGE_VALF_3

size_t unsigned_size_t_1;
ptrdiff_t a_signed_pointer_t_1;

MYAPI int32_t   MYAPIENTRY typeTestInt32T(const int32_t i1, int32_t i2);
MYAPI uint32_t  MYAPIENTRY typeTestUInt32T(const uint32_t ui1, uint32_t ui2);
MYAPI int64_t   MYAPIENTRY typeTestInt64T(const int64_t i1, int64_t i2);
MYAPI uint64_t  MYAPIENTRY typeTestUInt64T(const uint64_t ui1, uint64_t ui2);

MYAPI wchar_t   MYAPIENTRY typeTestWCharT(const wchar_t c1, wchar_t c2);
MYAPI size_t    MYAPIENTRY typeTestSizeT(const size_t size1, size_t size2);
MYAPI ptrdiff_t MYAPIENTRY typeTestPtrDiffT(const ptrdiff_t ptr1, ptrdiff_t ptr2);
MYAPI intptr_t  MYAPIENTRY typeTestIntPtrT(const intptr_t ptr1, intptr_t ptr2);
MYAPI uintptr_t MYAPIENTRY typeTestUIntPtrT(const uintptr_t ptr1, uintptr_t ptr2);

#ifdef __GLUEGEN__
    #warning "Hello GlueGen"
#else
    #warning "Hello Native Compiler"
#endif

typedef struct {
    int32_t x;
    int32_t y;
    int32_t width;
    int32_t height;
} TK_Dimension;

typedef struct _TK_Context * TK_Context; // anonymous

typedef struct {
    TK_Context ctx;
    int32_t (MYAPIENTRY *render) (int x, int y, int ps);
} TK_Engine;

typedef struct {
    TK_Context ctx;
} TK_ContextWrapper;

typedef struct tk_Surface {
    TK_Context ctx;
    TK_ContextWrapper ctxWrapper;
    TK_Engine engine;
    TK_Dimension bounds;
    int32_t clipSize;
    TK_Dimension * clips;
    TK_Dimension * (MYAPIENTRY *getClip) (struct tk_Surface * ds, int idx);
} TK_Surface;

typedef struct {
    uint8_t bits1; // +1
                   // +3 (p64)
    int32_t id;    // +4
    uint8_t bits2; // +1
                   // +7 (p64)
    int64_t long0; // +8
    uint8_t bits3; // +1
                   // +7 (p64)
    double real0;  // +8
    uint8_t bits4; // +1
                   // +3 (p64)
    float  real1;  // +4
    uint8_t bits5; // +1
                   // +7 (p64) / +3 (windows andx 32bit)
    long   longX;  // +8 / +4 (windows andx 32bit)
    uint8_t bits6; // +1

                   // +7 (p64) (for next struct ..)

                   // 24 net 

                   // 48 gross 64bit/linux 
} TK_ComplicatedSubSet;

typedef struct {
    uint8_t bits1;             // + 1
                               // + 7 (p64)
    TK_ComplicatedSubSet sub1; // +48 (64bit)
    uint8_t bits2;             // + 1
                               // + 7 (p64)
    TK_ComplicatedSubSet sub2; // +48 (64bit)
    uint8_t bits3;             // + 1
                               // + 7 (p64)

                               //  51 net

                               // 120 gross 64bit/linux 
} TK_ComplicatedSuperSet;

typedef enum {
    ZERO = 0,
    ONE = 1,
    TWO = 2,
    THREE = 3
} TK_Numbers;

typedef struct {
    int32_t i1;
    TK_Dimension pair[TWO];
    int32_t i2;
} TK_DimensionPair;

MYAPI TK_Surface * MYAPIENTRY createSurface();
MYAPI void MYAPIENTRY destroySurface(TK_Surface * surface);

MYAPI TK_ComplicatedSuperSet * MYAPIENTRY createComplicatedSuperSet();
MYAPI Bool MYAPIENTRY hasInitValues(TK_ComplicatedSuperSet * s);
MYAPI void MYAPIENTRY destroyComplicatedSuperSet(TK_ComplicatedSuperSet * s);

//
// Call by Value !!!
//

MYAPI TK_Dimension MYAPIENTRY getBoundsValue(int32_t x, int32_t y, int32_t width, int32_t height);
MYAPI TK_Surface MYAPIENTRY getSurfaceValue(TK_Dimension bounds);
MYAPI TK_Dimension MYAPIENTRY getSurfaceBoundsValue(TK_Surface s);
MYAPI Bool MYAPIENTRY isSameInstanceByVal(TK_Dimension s1, TK_Dimension s2);
MYAPI Bool MYAPIENTRY isSameInstanceByRef(const TK_Dimension *s1, const TK_Dimension *s2);
MYAPI TK_Dimension MYAPIENTRY addDimensions(const TK_Dimension s[TWO]);
MYAPI TK_Dimension MYAPIENTRY addDimensionPair(const TK_DimensionPair s);
MYAPI void MYAPIENTRY zeroDimensions(TK_Dimension s[2]);

MYAPI void MYAPIENTRY copyPrimToDimensions(const int pos[2], const int size[2], TK_Dimension dest[1]);
MYAPI void MYAPIENTRY copyDimensionsToPrim(TK_Dimension dim, int dpos[2], int dsize[2]);
MYAPI int MYAPIENTRY rgbaToInt(const char rgba[4]);
MYAPI void MYAPIENTRY intToRgba(int irgba, char rgbaSink[4]);
MYAPI void MYAPIENTRY addInt(const int summands[2], int result[1]);
MYAPI void MYAPIENTRY addByte(const char summands[2], char result[1]);

typedef struct {
    const int intxxArrayFixedLen[3];
    
    const int * intxxPointerCustomLen;
    const int intxxPointerCustomLenVal;

    const int32_t int32ArrayFixedLen[3];
    const int32_t int32ArrayOneElem[1];
    
    const int32_t * int32PointerCustomLen;
    const int32_t int32PointerCustomLenVal;

    const int32_t * int32PointerOneElem;

    const float mat4x4[4][4];

    const TK_Dimension structArrayFixedLen[3];
    const TK_Dimension structArrayOneElem[1];
    
    const TK_Dimension * structPointerCustomLen;
    const int32_t structPointerCustomLenVal;
    const TK_Dimension * structPointerOneElem;

    const TK_Context ctx;

    const char modelNameArrayFixedLen[12]; /* 'Hello Array' len=11+1 */
    const char * modelNamePointerCString;    /* 'Hello CString' len=13+1 */
    const char * modelNamePointerCustomLen;  /* 'Hello Pointer' len=13+1 */
    const int modelNamePointerCustomLenVal;  /* 13+1 */

} TK_ModelConst;

typedef struct {
    int intxxArrayFixedLen[3];
    
    int * intxxPointerCustomLen;
    int intxxPointerCustomLenVal;

    int32_t int32ArrayFixedLen[3];
    int32_t int32ArrayOneElem[1];
    
    int32_t * int32PointerCustomLen;
    int32_t int32PointerCustomLenVal;

    int32_t * int32PointerOneElem;

    float mat4x4[4][4];

    TK_Dimension structArrayFixedLen[3];
    TK_Dimension structArrayOneElem[1];
    
    TK_Dimension * structPointerCustomLen;
    int32_t structPointerCustomLenVal;
    TK_Dimension * structPointerOneElem;

    TK_Context ctx;

    char modelNameArrayFixedLen[12]; /* 'Hello Array' len=11+1 */
    const char * modelNamePointerCString;    /* 'Hello CString' len=13+1 */
    char * modelNamePointerCustomLen;  /* 'Hello Pointer' len=13+1 */
    int modelNamePointerCustomLenVal;  /* 13+1 */

} TK_ModelMutable;

MYAPI TK_ModelConst * MYAPIENTRY createModelConst();
MYAPI void MYAPIENTRY destroyModelConst(TK_ModelConst * s);
MYAPI TK_ModelMutable * MYAPIENTRY createModelMutable();
MYAPI void MYAPIENTRY destroyModelMutable(TK_ModelMutable * s);
