#include "test1.h"
#include <assert.h>
#include <stdlib.h>
#include <stdio.h>

foo nopTest() {
    return 42;
}

int32_t arrayTestInt32(int64_t context, int32_t * array) {
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

int64_t arrayTestInt64(int64_t context, int64_t * array) {
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

foo arrayTestFoo(int64_t context, foo * array) {
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

foo bufferTest(void * object) {
    assert(NULL!=object);
    return *((foo *)object);
}

foo mixedTest(int64_t context, void * object, foo * array){
    assert(NULL!=object);
    assert(NULL!=array);
    return arrayTestFoo(context, array) + bufferTest(object);
}

foo doubleTest(int64_t context, void * object1, foo * array1, void * object2, foo * array2) {
    assert(NULL!=object1);
    assert(NULL!=array1);
    assert(NULL!=object2);
    assert(NULL!=array2);
    return arrayTestFoo(context, array1) + 
           arrayTestFoo(      0, array2) + 
           bufferTest(object1) +
           bufferTest(object2);
}

foo arrayTestFooNioOnly(int64_t context, foo * array ) {
    return arrayTestFoo(context, array);
}

foo bufferTestNioOnly(void * object) {
    return bufferTest(object);
}

foo mixedTestNioOnly(int64_t context, void * object, foo * array ) {
    return mixedTest(context, object, array);
}

foo doubleTestNioOnly(int64_t context, void * object1, foo * array1, void * object2, foo * array2 ) {
    return doubleTest(context, object1, array1, object2, array2);
}

int strToInt(const char * str) {
    return atoi(str);
}

const char * intToStr(int i) {
    static char singleton[200];
    snprintf(singleton, sizeof(singleton)-1, "%d", i);
    return singleton;
}

int stringArrayRead(const char *  *  strings, int num) {
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

int intArrayRead(const int *  ints, int num) {
    int i=0, s=0;
    if(NULL!=ints) {
        for(i=0; i<num; i++) {
            s+=ints[i];
        }
    }
    return s;
}

/**
int intArrayWrite(int * *  ints, int num) {
    int i=0, s=0;
    if(NULL!=ints) {
        for(i=0; i<num; i++) {
            *(ints[i]) = *(ints[i]) + 1;
            s+=*(ints[i]);
        }
    }
    return s;
} */

