#define __MYAPI_EXPORT_ 1
#include "test1.h"

#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define DEBUG 1

MYAPI foo MYAPIENTRY nopTest() {
    return 42;
}

/**
 * new blob sizeof(void*) filled w/ 0xDEADBEEF
 */
MYAPI void * MYAPIENTRY createAPtrBlob () {
    uint32_t * mem = (uint32_t *) calloc(1, sizeof(void *));
    *mem = 0xDEADBEEF;
    return (void *)mem;
}
MYAPI void MYAPIENTRY releaseAPtrBlob (void * blob) {
    free(blob);
}
MYAPI intptr_t MYAPIENTRY getAPtrAddress (const void * immutable ) {
    return (intptr_t)immutable;
}
MYAPI void * MYAPIENTRY getAPtrMemory (const intptr_t address ) {
    return (void *)address;
}

MYAPI void *   MYAPIENTRY arrayTestAVoidPtrTypeDim0 (const void * immutable ) {
    return immutable;
}
MYAPI void     MYAPIENTRY arrayTestAVoidPtrTypeDim1Mutable  (void ** mutable ) {
    *mutable = createAPtrBlob ();
}
MYAPI void *   MYAPIENTRY arrayTestAVoidPtrTypeDim1Immutable  (const void ** immutable ) {
    return *immutable;
}

MYAPI intptr_t MYAPIENTRY arrayTestAIntPtrTypeDim0  (intptr_t immutable) {
    return immutable;
}
MYAPI void     MYAPIENTRY arrayTestAIntPtrTypeDim1Mutable  (intptr_t * mutable ) {
    *mutable = (intptr_t) createAPtrBlob ();
}
MYAPI intptr_t MYAPIENTRY arrayTestAIntPtrTypeDim1Immutable(const intptr_t * immutable ) {
    return *immutable;
}

MYAPI APtr1Type MYAPIENTRY arrayTestAPtr1TypeDim0 (APtr1Type immutable ) {
    return immutable;
}
MYAPI void MYAPIENTRY arrayTestAPtr1TypeDim1Mutable  (APtr1Type * mutable ) {
    *mutable = (APtr1Type) createAPtrBlob ();
}
MYAPI APtr1Type MYAPIENTRY arrayTestAPtr1TypeDim1Immutable(const APtr1Type * immutable ) {
    return *immutable;
}

MYAPI APtr2Type MYAPIENTRY arrayTestAPtr2TypeDim0  (APtr2Type immutable ) {
    return immutable;
}
MYAPI void      MYAPIENTRY arrayTestAPtr2TypeDim1Mutable  (APtr2Type * mutable ) {
    *mutable = (APtr2Type) createAPtrBlob ();
}
MYAPI APtr2Type MYAPIENTRY arrayTestAPtr2TypeDim1Immutable(const APtr2Type * immutable ) {
    return *immutable;
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

MYAPI foo * MYAPIENTRY arrayTestFoo2( const foo * array ) {
    int i;
    foo * result = calloc(ARRAY_SIZE, sizeof(foo));
    assert(NULL!=array);
    for(i=0; i<ARRAY_SIZE; i++) {
        result[i] = array[i] + 1;
        // printf("array[%d]: %d -> %d\n", i, (int)array[i], (int)result[i]);
    }
    return result;
}

MYAPI void MYAPIENTRY arrayTestFoo3( foo * array ) {
    int i;
    assert(NULL!=array);
    for(i=0; i<ARRAY_SIZE; i++) {
        array[i] += 1;
        // printf("array[%d]: %d -> %d\n", i, (int)array[i], (int)result[i]);
    }
}

MYAPI foo * * MYAPIENTRY arrayTestFoo3ArrayToPtrPtr(const foo * array) {
    int j;
    foo * * result = calloc(ARRAY_SIZE, sizeof(foo *));
    assert(NULL!=array);
    for(j=0; j<ARRAY_SIZE; j++) {
        result[j] = (foo *) ( array + ARRAY_SIZE * j ) ;
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

MYAPI foo MYAPIENTRY bufferTestNioDirectOnly(void * object) {
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

MYAPI int MYAPIENTRY binaryArrayRead(const size_t * lengths, const unsigned char *  * binaries, int num) {
    int i, j, n=0;
    for(i=0; i<num; i++) {
        for(j=0; j<lengths[i]; j++) {
            if(0xff==binaries[i][j]) {
                ++n;
            }
        }
    }
    return n;
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

MYAPI int MYAPIENTRY intArrayCopy(int * dest, const int * src, int num) {
    int i=0;
    if(NULL!=dest && NULL!=src) {
        for(i=0; i<num; i++) {
            dest[i] = src[i];
        }
    }
    return num;
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
    s->ctxWrapper.ctx = (void *) 0xA23456781abcdef0UL;
    //s->engine = (TK_Engine *) calloc(1, sizeof(TK_Engine));
    //s->engine->ctx    = (void *) 0x123456789abcdef0UL;
    //s->engine->render = _TK_render;
    s->engine.ctx    = (void *) 0xB23456782abcdef0UL;
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
    assert(NULL!=surface);
    free(surface->clips);
    // free(surface->engine);
    free(surface);
}

static void dumpTK_ComplicatedSuperSet(TK_ComplicatedSuperSet * s) {
    assert(NULL!=s);
    fprintf(stderr, "TK_ComplicatedSuperSet [\n");
    fprintf(stderr, "  cs.b1         0x%X\n", s->bits1);

    fprintf(stderr, "  cs.sub1.b1    0x%X\n", s->sub1.bits1);
    fprintf(stderr, "  cs.sub1.id    0x%X\n", s->sub1.id);
    fprintf(stderr, "  cs.sub1.b2    0x%X\n", s->sub1.bits2);
    fprintf(stderr, "  cs.sub1.long0 0x%lX\n", s->sub1.long0);
    fprintf(stderr, "  cs.sub1.b3    0x%X\n", s->sub1.bits3);
    fprintf(stderr, "  cs.sub1.real0 %G %A\n", s->sub1.real0, s->sub1.real0);
    fprintf(stderr, "  cs.sub1.b4    0x%X\n", s->sub1.bits4);
    fprintf(stderr, "  cs.sub1.real1 %G %A\n", (double)s->sub1.real1, (double)s->sub1.real1);
    fprintf(stderr, "  cs.sub1.b5    0x%X\n", s->sub1.bits5);
    fprintf(stderr, "  cs.sub1.longX 0x%lX\n", (int64_t)s->sub1.longX);
    fprintf(stderr, "  cs.sub1.b6    0x%X\n", s->sub1.bits6);

    fprintf(stderr, "  cs.b2         0x%X\n", s->bits2);

    fprintf(stderr, "  cs.sub2.b1    0x%X\n", s->sub2.bits1);
    fprintf(stderr, "  cs.sub2.id    0x%X\n", s->sub2.id);
    fprintf(stderr, "  cs.sub2.b2    0x%X\n", s->sub2.bits2);
    fprintf(stderr, "  cs.sub2.long0 0x%lX\n", s->sub2.long0);
    fprintf(stderr, "  cs.sub2.b3    0x%X\n", s->sub2.bits3);
    fprintf(stderr, "  cs.sub2.real0 %G %A\n", s->sub2.real0, s->sub2.real0);
    fprintf(stderr, "  cs.sub2.b4    0x%X\n", s->sub2.bits4);
    fprintf(stderr, "  cs.sub2.real1 %G %A\n", (double)s->sub2.real1, (double)s->sub2.real1);
    fprintf(stderr, "  cs.sub2.b5    0x%X\n", s->sub2.bits5);
    fprintf(stderr, "  cs.sub2.longX 0x%lX\n", (int64_t)s->sub2.longX);
    fprintf(stderr, "  cs.sub2.b6    0x%X\n", s->sub2.bits6);

    fprintf(stderr, "  cs.b3         0x%X\n", s->bits3);
    fprintf(stderr, "]\n\n");
}

MYAPI TK_ComplicatedSuperSet * MYAPIENTRY createComplicatedSuperSet() {
    TK_ComplicatedSuperSet * s = calloc(1, sizeof(TK_ComplicatedSuperSet));

    s->bits1 = 0xA0U;
    s->sub1.bits1 = 0xA1U;
    s->sub1.id = 0x12345678U;
    s->sub1.bits2 = 0xA2U;
    s->sub1.long0 = 0x123456789abcdef0UL;
    s->sub1.bits3 = 0xA3U;
    s->sub1.real0 = 3.1415926535897932384626433832795;
    s->sub1.bits4 = 0xA4U;
    s->sub1.real1 = 256.12345f;
    s->sub1.bits5 = 0xA5U;
    s->sub1.longX = (long) 0xdeadbeefU;
    s->sub1.bits6 = 0xA6U;
    s->bits2 = 0xB0U;
    s->sub2.bits1 = 0xB1U;
    s->sub2.id = 0x12345678U;
    s->sub2.bits2 = 0xB2U;
    s->sub2.long0 = 0x123456789abcdef0UL;
    s->sub2.bits3 = 0xB3U;
    s->sub2.real0 = 3.1415926535897932384626433832795;
    s->sub2.bits4 = 0xB4U;
    s->sub2.real1 = 256.12345f;
    s->sub2.bits5 = 0xB5U;
    s->sub2.longX = (long) 0xdeadbeefU;
    s->sub2.bits6 = 0xB6U;
    s->bits3 = 0xC0U;

    fprintf(stderr, "TK_ComplicatedSubSet: sizeof(): %ld\n", (long) sizeof(TK_ComplicatedSubSet));
    fprintf(stderr, "TK_ComplicatedSubSet: bits1-s offset: %ld\n", (long) ((void *)(&s->sub1.bits1) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet:    id-s offset: %ld\n", (long) ((void *)(&s->sub1.id)    - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: bits2-s offset: %ld\n", (long) ((void *)(&s->sub1.bits2) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: long0-s offset: %ld\n", (long) ((void *)(&s->sub1.long0) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: bits3-s offset: %ld\n", (long) ((void *)(&s->sub1.bits3) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: real0-s offset: %ld\n", (long) ((void *)(&s->sub1.real0) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: bits4-s offset: %ld\n", (long) ((void *)(&s->sub1.bits4) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: real1-s offset: %ld\n", (long) ((void *)(&s->sub1.real1) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: bits5-s offset: %ld\n", (long) ((void *)(&s->sub1.bits5) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: longX-s offset: %ld\n", (long) ((void *)(&s->sub1.longX) - (void *)(&s->sub1)) );
    fprintf(stderr, "TK_ComplicatedSubSet: bits6-s offset: %ld\n", (long) ((void *)(&s->sub1.bits6) - (void *)(&s->sub1)) );

    fprintf(stderr, "TK_ComplicatedSuperSet: sizeof(): %ld\n", (long) sizeof(TK_ComplicatedSuperSet));
    fprintf(stderr, "TK_ComplicatedSuperSet: bits1-s offset: %ld\n", (long) ((void *)(&s->bits1) - (void *)(s)) );
    fprintf(stderr, "TK_ComplicatedSuperSet:  sub1-s offset: %ld\n", (long) ((void *)(&s->sub1)  - (void *)(s)) );
    fprintf(stderr, "TK_ComplicatedSuperSet: bits2-s offset: %ld\n", (long) ((void *)(&s->bits2) - (void *)(s)) );
    fprintf(stderr, "TK_ComplicatedSuperSet:  sub2-s offset: %ld\n", (long) ((void *)(&s->sub2)  - (void *)(s)) );
    fprintf(stderr, "TK_ComplicatedSuperSet: bits3-s offset: %ld\n", (long) ((void *)(&s->bits3) - (void *)(s)) );

    #ifdef DEBUG
        fprintf(stderr, "createComplicatedSuperSet:\n");
        dumpTK_ComplicatedSuperSet(s);
    #endif
    return s;
}

MYAPI Bool MYAPIENTRY hasInitValues(TK_ComplicatedSuperSet * s) {
    assert(NULL!=s);
    Bool b =  s->bits1 == 0xA0U &&
            s->sub1.bits1 == 0xA1U &&
            s->sub1.id == 0x12345678U &&
            s->sub1.bits2 == 0xA2U &&
            s->sub1.long0 == 0x123456789abcdef0UL &&
            s->sub1.bits3 == 0xA3U &&
            s->sub1.real0 == 3.1415926535897932384626433832795 &&
            s->sub1.bits4 == 0xA4U &&
            s->sub1.real1 == 256.12345f &&
            s->sub1.bits5 == 0xA5U &&
            s->sub1.longX == (long) 0xdeadbeefU &&
            s->sub1.bits6 == 0xA6U &&
            s->bits2 == 0xB0U &&
            s->sub2.bits1 == 0xB1U &&
            s->sub2.id == 0x12345678U &&
            s->sub2.bits2 == 0xB2U &&
            s->sub2.long0 == 0x123456789abcdef0UL &&
            s->sub2.bits3 == 0xB3U &&
            s->sub2.real0 == 3.1415926535897932384626433832795 &&
            s->sub2.bits4 == 0xB4U &&
            s->sub2.real1 == 256.12345f &&
            s->sub2.bits5 == 0xB5U &&
            s->sub2.longX == (long) 0xdeadbeefU &&
            s->sub2.bits6 == 0xB6U &&
            s->bits3 == 0xC0U ;
    #ifdef DEBUG
        fprintf(stderr, "hasInitValues res %d:\n", b);
        dumpTK_ComplicatedSuperSet(s);
    #endif
    return b;
}

MYAPI void MYAPIENTRY destroyComplicatedSuperSet(TK_ComplicatedSuperSet * s) {
    assert(NULL!=s);
    free(s);
}

