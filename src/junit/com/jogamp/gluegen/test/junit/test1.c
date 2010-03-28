#include "test1.h"
#include <assert.h>
#include <stdlib.h>

foo nopTest() {
    return 42;
}

foo arrayTest(long context, foo * array) {
    foo r=0;
    int i;
    assert(NULL!=array);
    // printf("array test - %p\n", array);
    for(i=0; i<ARRAY_SIZE; i++) {
        r+=array[i];
    }
    return r+context;
}

foo bufferTest(void * object) {
    assert(NULL!=object);
    return *((foo *)object);
}

foo mixedTest(long context, void * object, foo * array){
    assert(NULL!=object);
    assert(NULL!=array);
    return arrayTest(context, array) + bufferTest(object);
}

foo doubleTest(long context, void * object1, foo * array1, void * object2, foo * array2) {
    assert(NULL!=object1);
    assert(NULL!=array1);
    assert(NULL!=object2);
    assert(NULL!=array2);
    return arrayTest(context, array1) + 
           arrayTest(      0, array2) + 
           bufferTest(object1) +
           bufferTest(object2);
}

foo arrayTestNioOnly(long context, foo * array ) {
    return arrayTest(context, array);
}

foo bufferTestNioOnly(void * object) {
    return bufferTest(object);
}

foo mixedTestNioOnly(long context, void * object, foo * array ) {
    return mixedTest(context, object, array);
}

foo doubleTestNioOnly(long context, void * object1, foo * array1, void * object2, foo * array2 ) {
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

