#define __MYAPI_EXPORT_ 1
#include "test1.h"

#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define DEBUG 1

MYAPI XID MYAPIENTRY testXID(XID v) {
    return v;
}
MYAPI XID_2 MYAPIENTRY testXID_2(XID_2 v) {
    return v;
}
MYAPI AnonBuffer MYAPIENTRY testAnonBuffer(AnonBuffer v) {
    return v;
}

MYAPI const ShortBlob * MYAPIENTRY testShortBlob(const ShortBlob *v) {
    return v;
}
MYAPI const LPShortBlob0 MYAPIENTRY testLPShortBlob0(const LPShortBlob0 v) {
    return v;
}
MYAPI LPShortBlob1 MYAPIENTRY testLPShortBlob1(LPShortBlob1 v) {
    return v;
}
MYAPI const LPShortBlob2 MYAPIENTRY testLPShortBlob2(const LPShortBlob2 v) {
    return v;
}
MYAPI LPShortBlob3 MYAPIENTRY testLPShortBlob3(LPShortBlob3 v) {
    return v;
}
MYAPI const ShortBlobL1 * MYAPIENTRY testShortBlobL1(const ShortBlobL1 * v) {
    return v;
}
MYAPI ShortBlobL2 * MYAPIENTRY testShortBlobL2(ShortBlobL2 * v) {
    return v;
}
MYAPI struct Int32Struct * MYAPIENTRY testInt32Struct(struct Int32Struct * v) {
    return v;
}

MYAPI AnonBlob MYAPIENTRY testCreateAnonBlob() {
    return (AnonBlob) calloc(1, sizeof(char));
}
MYAPI void MYAPIENTRY testDestroyAnonBlob(AnonBlob v) {
    free(v);
}

MYAPI struct _AnonBlob2 * MYAPIENTRY testCreateAnonBlob2() {
    return (struct _AnonBlob2 *) calloc(1, sizeof(char));
}
MYAPI void MYAPIENTRY testDestroyAnonBlob2(struct _AnonBlob2 * v) {
    free(v);
}

MYAPI foo_ptr MYAPIENTRY testFooPtr(foo_ptr v) {
    return v;
}

MYAPI foo MYAPIENTRY nopTest() {
    return 42;
}

MYAPI int32_t MYAPIENTRY testDelegate(int32_t v) {
   return v; 
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
    return (void*)immutable;
}
MYAPI void     MYAPIENTRY arrayTestAVoidPtrTypeDim1Mutable  (void ** mutable ) {
    *mutable = createAPtrBlob ();
}
MYAPI void *   MYAPIENTRY arrayTestAVoidPtrTypeDim1Immutable  (const void ** immutable ) {
    return (void*)(*immutable);
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
    assert(NULL!=surface->clips);
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

MYAPI TK_Dimension MYAPIENTRY getBoundsValue(int32_t x, int32_t y, int32_t width, int32_t height) {
    TK_Dimension r = { x, y, width, height };
    return r;
}
MYAPI TK_Surface MYAPIENTRY getSurfaceValue(TK_Dimension bounds) {
    TK_Surface s;
    memset(&s, 0, sizeof(s));
    s.bounds = bounds;
    return s;
}
MYAPI TK_Dimension MYAPIENTRY getSurfaceBoundsValue(TK_Surface s) {
    TK_Dimension res = s.bounds;
    fprintf(stderr, "XXX.getSurfaceBoundsValue: dim %d/%d %dx%d\n", res.x, res.y, res.width, res.height);
    return res;
}
MYAPI Bool MYAPIENTRY isSameInstanceByVal(TK_Dimension s1, TK_Dimension s2) {
    return &s1 == &s2;
}
MYAPI Bool MYAPIENTRY isSameInstanceByRef(const TK_Dimension *s1, const TK_Dimension *s2) {
    return s1 == s2;
}
MYAPI TK_Dimension MYAPIENTRY addDimensions(const TK_Dimension s[TWO]) {
    TK_Dimension r = { s[0].x + s[1].x, s[0].y + s[1].y, 
                       s[0].width + s[1].width, s[0].height + s[1].height };
    return r;
}
MYAPI TK_Dimension MYAPIENTRY addDimensionPair(const TK_DimensionPair s) {
    return addDimensions(s.pair);
}
MYAPI void MYAPIENTRY zeroDimensions(TK_Dimension s[2]) {
    s[0].x = 0;
    s[0].y = 0;
    s[0].width = 0;
    s[0].height = 0;
    s[1].x = 0;
    s[1].y = 0;
    s[1].width = 0;
    s[1].height = 0;
}

MYAPI void MYAPIENTRY copyPrimToDimensions(const int pos[2], const int size[2], TK_Dimension dest[1]) {
    dest[0].x = pos[0];
    dest[0].y = pos[1];
    dest[0].width = size[0];
    dest[0].height = size[1];
}
MYAPI void MYAPIENTRY copyDimensionsToPrim(TK_Dimension dim, int dpos[2], int dsize[2]) {
    dpos[0] = dim.x;
    dpos[1] = dim.y;
    dsize[0] = dim.width;
    dsize[1] = dim.height;
}
MYAPI int MYAPIENTRY rgbaToInt(const char rgba[4]) {
    return ((unsigned int)rgba[3] & 0xffU) << 24 | 
           ((unsigned int)rgba[2] & 0xffU) << 16 | 
           ((unsigned int)rgba[1] & 0xffU) << 8 | 
           ((unsigned int)rgba[0] & 0xffU);
}
MYAPI void MYAPIENTRY intToRgba(int irgba, char rgbaSink[4]) {
    rgbaSink[0] = (char) ( (irgba       ) & 0xff );
    rgbaSink[1] = (char) ( (irgba >>  8 ) & 0xff );
    rgbaSink[2] = (char) ( (irgba >> 16 ) & 0xff );
    rgbaSink[3] = (char) ( (irgba >> 24 ) & 0xff );
}
MYAPI void MYAPIENTRY addInt(const int summands[2], int result[1]) {
    result[0] = summands[0] + summands[1];
}
MYAPI void MYAPIENTRY addByte(const char summands[2], char result[1]) {
    result[0] = summands[0] + summands[1];
}

MYAPI TK_ModelMutable * MYAPIENTRY createModelMutable() {
    int i, j;
    TK_ModelMutable * s = calloc(1, sizeof(TK_ModelMutable));

    s->intxxArrayFixedLen[0]=1;
    s->intxxArrayFixedLen[1]=2;
    s->intxxArrayFixedLen[2]=3;
    
    s->intxxPointerCustomLen = calloc(3, sizeof(int));
    s->intxxPointerCustomLen[0] = 11;
    s->intxxPointerCustomLen[1] = 12;
    s->intxxPointerCustomLen[2] = 13;
    s->intxxPointerCustomLenVal=3;

    s->int32ArrayFixedLen[0] = 21;
    s->int32ArrayFixedLen[1] = 22;
    s->int32ArrayFixedLen[2] = 23;

    s->int32ArrayOneElem[0] = 30;
    
    s->int32PointerCustomLen = calloc(3, sizeof(int));
    s->int32PointerCustomLen[0] = 31;
    s->int32PointerCustomLen[1] = 32;
    s->int32PointerCustomLen[2] = 33;
    s->int32PointerCustomLenVal=3;

    s->int32PointerOneElem = calloc(1, sizeof(int));
    s->int32PointerOneElem[0] = 41;

    for(i=0; i<4; i++) {
        for(j=0; j<4; j++) {
            s->mat4x4[i][j] = i*4 + j;
        }
    }

    s->structArrayFixedLen[0].x      = 51;
    s->structArrayFixedLen[0].y      = 52;
    s->structArrayFixedLen[0].width  = 53;
    s->structArrayFixedLen[0].height = 54;
    s->structArrayFixedLen[1].x      = 61;
    s->structArrayFixedLen[1].y      = 62;
    s->structArrayFixedLen[1].width  = 63;
    s->structArrayFixedLen[1].height = 64;
    s->structArrayFixedLen[2].x      = 71;
    s->structArrayFixedLen[2].y      = 72;
    s->structArrayFixedLen[2].width  = 73;
    s->structArrayFixedLen[2].height = 74;

    s->structArrayOneElem[0].x      = 81;
    s->structArrayOneElem[0].y      = 82;
    s->structArrayOneElem[0].width  = 83;
    s->structArrayOneElem[0].height = 84;

    s->structPointerCustomLen = (TK_Dimension *) calloc(3, sizeof(TK_Dimension));
    s->structPointerCustomLen[0].x      = 91;
    s->structPointerCustomLen[0].y      = 92;
    s->structPointerCustomLen[0].width  = 93;
    s->structPointerCustomLen[0].height = 94;
    s->structPointerCustomLen[1].x      = 101;
    s->structPointerCustomLen[1].y      = 102;
    s->structPointerCustomLen[1].width  = 103;
    s->structPointerCustomLen[1].height = 104;
    s->structPointerCustomLen[2].x      = 111;
    s->structPointerCustomLen[2].y      = 112;
    s->structPointerCustomLen[2].width  = 113;
    s->structPointerCustomLen[2].height = 114;
    s->structPointerCustomLenVal = 3;

    s->structPointerOneElem = (TK_Dimension *) calloc(1, sizeof(TK_Dimension));
    s->structPointerOneElem[0].x      = 121;
    s->structPointerOneElem[0].y      = 122;
    s->structPointerOneElem[0].width  = 123;
    s->structPointerOneElem[0].height = 124;

    s->ctx = (void *) 0x123456789abcdef0UL;

    strncpy(s->modelNameArrayFixedLen, "Hello Array", sizeof(s->modelNameArrayFixedLen));

    s->modelNamePointerCString = calloc(13+1, sizeof(char));
    strncpy(s->modelNamePointerCString, "Hello CString", 13+1);

    s->modelNamePointerCustomLen = calloc(13+1, sizeof(char));
    strncpy(s->modelNamePointerCustomLen, "Hello Pointer", 13+1);
    s->modelNamePointerCustomLenVal = 13+1;

    return s;
}

MYAPI void MYAPIENTRY destroyModelMutable(TK_ModelMutable * s) {
    assert(NULL!=s);
    assert(NULL!=s->intxxPointerCustomLen);
    assert(NULL!=s->int32PointerCustomLen);
    assert(NULL!=s->int32PointerOneElem);
    assert(NULL!=s->structPointerCustomLen);
    free(s->intxxPointerCustomLen);
    free(s->int32PointerCustomLen);
    free(s->int32PointerOneElem);
    free(s->structPointerCustomLen);
    free(s->modelNamePointerCString);
    free(s->modelNamePointerCustomLen);
    free(s);
}

MYAPI TK_ModelConst * MYAPIENTRY createModelConst() {
    return (TK_ModelConst *)createModelMutable();
}
MYAPI void MYAPIENTRY destroyModelConst(TK_ModelConst * s) {
    destroyModelMutable((TK_ModelMutable *)s);
}
