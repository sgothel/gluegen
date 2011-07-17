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

MYAPI void MYAPIENTRY arrayTestFoo3CopyPtrPtrA(foo * * dest, const foo * * src) {
    int i, j;
    assert(NULL!=dest);
    assert(NULL!=src);
    for(j=0; j<ARRAY_SIZE; j++) {
        dest[j] = calloc(ARRAY_SIZE, sizeof(foo));
        for(i=0; i<ARRAY_SIZE; i++) {
            dest[j][i] = src[j][i];
        }
    }
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

MYAPI int MYAPIENTRY arrayTestFoo3PtrPtrValidation(foo * * array, int startval) {
    int i,j,v,p=0;
    assert(NULL!=array);
    for(j=0; j<ARRAY_SIZE; j++) {
        for(i=0, v=startval; i<ARRAY_SIZE; i++, p++, v++) {
            if(array[j][i] != v) {
                return p;
            }
        }
    }
    return 0;
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

static TK_Dimension * _TK_getClip(TK_Surface * surface, int idx) {
    return & ( surface->clips[idx] ) ;
}

static int32_t _TK_render (int x, int y, int ps) {
    return x + y + ps ;
}

MYAPI TK_Surface * MYAPIENTRY createSurface() {
    TK_Surface * s = calloc(1, sizeof(TK_Surface));

    s->getClip = _TK_getClip;

    s->ctx = (void *) 0x123456789abcdef0UL;
    //s->engine = (TK_Engine *) calloc(1, sizeof(TK_Engine));
    //s->engine->ctx    = (void *) 0x123456789abcdef0UL;
    //s->engine->render = _TK_render;
    s->engine.ctx    = (void *) 0x123456789abcdef0UL;
    s->engine.render = _TK_render;

    s->bounds.x      = 0x11111111U;
    s->bounds.y      = 0x22222222U;
    s->bounds.width  = 0x33333333U;
    s->bounds.height = 0x44444444U;

    s->clipSize = 2;
    s->clips = (TK_Dimension *) calloc(2, sizeof(TK_Dimension));
    s->clips[0].x      = 0x55555555U;
    s->clips[0].y      = 0x66666666U;
    s->clips[0].width  = 0x77777777U;
    s->clips[0].height = 0x88888888U;
    s->clips[1].x      = 0x99999999U;
    s->clips[1].y      = 0xaaaaaaaaU;
    s->clips[1].width  = 0xbbbbbbbbU;
    s->clips[1].height = 0xccccccccU;

    return s;
}

MYAPI void MYAPIENTRY destroySurface(TK_Surface * surface) {
    free(surface->clips);
    // free(surface->engine);
    free(surface);
}

MYAPI TK_ComplicatedSuperSet * MYAPIENTRY createComplicatedSuperSet() {
    TK_ComplicatedSuperSet * s = calloc(1, sizeof(TK_ComplicatedSuperSet));

    s->bits1 = 0xA0U;
    s->sub1.bits1 = 0xA1U;
    s->sub1.id = 0x12345678U;
    s->sub1.bits2 = 0xA2U;
    s->sub1.long0 = 0x123456789abcdef0UL;
    s->sub1.bits3 = 0xA3U;
    s->sub1.real0 = 3.1415926535897932384626433832795L;
    s->sub1.bits4 = 0xA4U;
    s->bits2 = 0xB0U;
    s->sub2.bits1 = 0xB1U;
    s->sub2.id = 0x12345678U;
    s->sub2.bits2 = 0xB2U;
    s->sub2.long0 = 0x123456789abcdef0UL;
    s->sub2.bits3 = 0xB3U;
    s->sub2.real0 = 3.1415926535897932384626433832795L;
    s->sub2.bits4 = 0xB4U;
    s->bits3 = 0xC0U;

    fprintf(stderr, "TK_ComplicatedSubSet: sizeof(): %ld\n", (long) sizeof(TK_ComplicatedSubSet));
    fprintf(stderr, "TK_ComplicatedSubSet: bits2-s offset: %ld\n", (long) ((void *)(&s->sub1.bits2) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: bits3-s offset: %ld\n", (long) ((void *)(&s->sub1.bits3) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: bits4-s offset: %ld\n", (long) ((void *)(&s->sub1.bits4) - (void *)(&s->sub1)) );

    fprintf(stderr, "TK_ComplicatedSuperSet: sizeof(): %ld\n", (long) sizeof(TK_ComplicatedSuperSet));
    fprintf(stderr, "TK_ComplicatedSuperSet: bits2-s offset: %ld\n", (long) ((void *)(&s->bits2) - (void *)(s)) );
    fprintf(stderr, "TK_ComplicatedSuperSet: bits3-s offset: %ld\n", (long) ((void *)(&s->bits3) - (void *)(s)) );

    return s;
}

MYAPI void MYAPIENTRY destroyComplicatedSuperSet(TK_ComplicatedSuperSet * s) {
    free(s);
}

