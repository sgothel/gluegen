
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

typedef uint64_t foo;

/** Returns 42 */
MYAPI foo MYAPIENTRY nopTest();

/** Returns Sum(array) + context */
MYAPI int32_t MYAPIENTRY arrayTestInt32(int64_t context, int32_t * array );

/** Returns Sum(array) + context */
MYAPI int64_t MYAPIENTRY arrayTestInt64(int64_t context, int64_t * array );

/** Returns Sum(array) + context */
MYAPI foo MYAPIENTRY arrayTestFoo1(int64_t context, foo * array );

/** Returns a copy of the passed array, each element incr by 1 */
MYAPI foo * MYAPIENTRY arrayTestFoo2(foo * array );

/** Returns a array-array of the passed array, split at ARRAY size - IDENTITY! */
MYAPI foo * * MYAPIENTRY arrayTestFoo3ArrayToPtrPtr(foo * array);

/** Returns a the passed array-array, each element incr by 1 - IDENTITY !*/
MYAPI foo * * MYAPIENTRY arrayTestFoo3PtrPtr(foo * * array );

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

/** Returns Sum(array) + context + *((foo *)object) */
MYAPI foo MYAPIENTRY mixedTestNioOnly(int64_t context, void * object, foo * array );

/** Returns Sum(array1) + Sum(array2) + context + *((foo *)object1) + *((foo *)object2) */
MYAPI foo MYAPIENTRY doubleTestNioOnly(int64_t context, void * object1, foo * array1, void * object2, foo * array2 );

/** Returns atoi(str) */
MYAPI int MYAPIENTRY strToInt(const char* str);

/** Returns itoa(i) - not thread safe */
MYAPI const char * MYAPIENTRY intToStr(int i);

/** Returns the length of all strings */
MYAPI int MYAPIENTRY stringArrayRead(const char *  *  strings, int num);

/** Returns the sum of all integers */
MYAPI int MYAPIENTRY intArrayRead(const int *  ints, int num);

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

