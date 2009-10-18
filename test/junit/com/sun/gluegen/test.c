#include "test.h"

int arrayTest(long context, foo * array) {
    return 42;
}

int bufferTest(void * object) {
    return 42;
}

int mixedTest(long context, void * object, foo * array){
    return 42;
}

int doubleTest(long context, void * object1, foo * array1, void * object2, foo * array2) {
    return 42;
}
