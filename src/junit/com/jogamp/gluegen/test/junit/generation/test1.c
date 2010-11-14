#define __MYAPI_EXPORT_ 1
#include "test1.h"

#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

MYAPI foo MYAPIENTRY nopTest() {
    return 42;
}

MYAPI int32_t MYAPIENTRY arrayTestInt32(int64_t context, int32_t * array) {
    int32_t r=0;
    int i;
    assert(NULL!=array);
    // printf("array test - %p sizeof(int32_t) %d\n", array, sizeof(int32_t));
    for(i=0; i<ARRAY_SIZE; i++) {
        r+=array[i];
        // printf("array[%d]: %d -> %d\n", i, array[i], r);
    }
    return r+context;
}

MYAPI int64_t MYAPIENTRY arrayTestInt64(int64_t context, int64_t * array) {
    int64_t r=0;
    int i;
    assert(NULL!=array);
    // printf("array test - %p sizeof(int64_t) %d\n", array, sizeof(int64_t));
    for(i=0; i<ARRAY_SIZE; i++) {
        r+=array[i];
        // printf("array[%d]: %d -> %d\n", i, array[i], r);
    }
    return r+context;
}

MYAPI foo * MYAPIENTRY arrayTestFoo2( foo * array ) {
    int i;
    foo * result = calloc(ARRAY_SIZE, sizeof(foo));
    assert(NULL!=array);
    for(i=0; i<ARRAY_SIZE; i++) {
        result[i] = array[i] + 1;
        // printf("array[%d]: %d -> %d\n", i, (int)array[i], (int)result[i]);
    }
    return result;
}

MYAPI foo * * MYAPIENTRY arrayTestFoo3ArrayToPtrPtr(foo * array) {
    int j;
    foo * * result = calloc(ARRAY_SIZE, sizeof(foo *));
    for(j=0; j<ARRAY_SIZE; j++) {
        result[j] = array + ARRAY_SIZE * j ;
    }
    return result;
}

MYAPI foo * * MYAPIENTRY arrayTestFoo3PtrPtr(foo * * array ) {
    int i,j;
    assert(NULL!=array);
    for(j=0; j<ARRAY_SIZE; j++) {
        for(i=0; i<ARRAY_SIZE; i++) {
            array[j][i] += 1;
        }
    }
    return array;
}

MYAPI foo MYAPIENTRY arrayTestFoo1(int64_t context, foo * array) {
    foo r=0;
    int i;
    assert(NULL!=array);
    // printf("array test - %p sizeof(foo) %d\n", array, sizeof(foo));
    for(i=0; i<ARRAY_SIZE; i++) {
        r+=array[i];
        // printf("array[%d]: %d -> %d\n", i, array[i], r);
    }
    return r+context;
}

MYAPI foo MYAPIENTRY bufferTest(void * object) {
    assert(NULL!=object);
    return *((foo *)object);
}

MYAPI foo MYAPIENTRY mixedTest(int64_t context, void * object, foo * array){
    assert(NULL!=object);
    assert(NULL!=array);
    return arrayTestFoo1(context, array) + bufferTest(object);
}

MYAPI foo MYAPIENTRY doubleTest(int64_t context, void * object1, foo * array1, void * object2, foo * array2) {
    assert(NULL!=object1);
    assert(NULL!=array1);
    assert(NULL!=object2);
    assert(NULL!=array2);
    return arrayTestFoo1(context, array1) + 
           arrayTestFoo1(      0, array2) + 
           bufferTest(object1) +
           bufferTest(object2);
}

MYAPI foo MYAPIENTRY arrayTestFooNioOnly(int64_t context, foo * array ) {
    return arrayTestFoo1(context, array);
}

MYAPI foo MYAPIENTRY bufferTestNioOnly(void * object) {
    return bufferTest(object);
}

MYAPI foo MYAPIENTRY mixedTestNioOnly(int64_t context, void * object, foo * array ) {
    return mixedTest(context, object, array);
}

MYAPI foo MYAPIENTRY doubleTestNioOnly(int64_t context, void * object1, foo * array1, void * object2, foo * array2 ) {
    return doubleTest(context, object1, array1, object2, array2);
}

MYAPI int MYAPIENTRY strToInt(const char * str) {
    return atoi(str);
}

MYAPI const char * MYAPIENTRY intToStr(int i) {
    static char singleton[200];
    snprintf(singleton, sizeof(singleton)-1, "%d", i);
    return singleton;
}

MYAPI int MYAPIENTRY stringArrayRead(const char *  *  strings, int num) {
    int i=0, l=0;
    if(NULL!=strings) {
        for(i=0; i<num; i++) {
            if(NULL!=strings[i]) {
                l+=strlen(strings[i]);
            }
        }
    }
    return l;
}

MYAPI int MYAPIENTRY intArrayRead(const int *  ints, int num) {
    int i=0, s=0;
    if(NULL!=ints) {
        for(i=0; i<num; i++) {
            s+=ints[i];
        }
    }
    return s;
}

/**
MYAPI int intArrayWrite(int * *  ints, int num) {
    int i=0, s=0;
    if(NULL!=ints) {
        for(i=0; i<num; i++) {
            *(ints[i]) = *(ints[i]) + 1;
            s+=*(ints[i]);
        }
    }
    return s;
} */

MYAPI MYAPIConfig  MYAPIENTRY typeTestAnonSingle(const MYAPIConfig a) {
    return (MYAPIConfig) ( ((void *)a) + 1 );
}

MYAPI MYAPIConfig *  MYAPIENTRY typeTestAnonPointer(const MYAPIConfig * a) {
    int j;
    MYAPIConfig * result = calloc(ARRAY_SIZE, sizeof(MYAPIConfig));
    for(j=0; j<ARRAY_SIZE; j++) {
        result[j] = (MYAPIConfig) ( ((void *)a[j]) + 1 );
    }
    return result;
}

MYAPI int32_t   MYAPIENTRY typeTestInt32T(const int32_t i1, int32_t i2) {
    return i1 + i2;
}

MYAPI uint32_t  MYAPIENTRY typeTestUInt32T(const uint32_t ui1, uint32_t ui2) {
    return ui1 + ui2;
}

MYAPI int64_t   MYAPIENTRY typeTestInt64T(const int64_t i1, int64_t i2) {
    return i1 + i2;
}

MYAPI uint64_t  MYAPIENTRY typeTestUInt64T(const uint64_t ui1, uint64_t ui2) {
    return ui1 + ui2;
}


MYAPI wchar_t   MYAPIENTRY typeTestWCharT(const wchar_t c1, wchar_t c2) {
    return c1 + c2;
}

MYAPI size_t    MYAPIENTRY typeTestSizeT(const size_t size1, size_t size2) {
    return size1 + size2;
}

MYAPI ptrdiff_t MYAPIENTRY typeTestPtrDiffT(const ptrdiff_t ptr1, ptrdiff_t ptr2) {
    return ptr1 + ptr2;
}

MYAPI intptr_t  MYAPIENTRY typeTestIntPtrT(const intptr_t ptr1, intptr_t ptr2) {
    return ptr1 + ptr2;
}

MYAPI uintptr_t MYAPIENTRY typeTestUIntPtrT(const uintptr_t ptr1, uintptr_t ptr2) {
    return ptr1 + ptr2;
}


