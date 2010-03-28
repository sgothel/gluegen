
#ifndef MYAPIENTRY
#define MYAPIENTRY 
#endif
#ifndef MYAPIENTRYP
#define MYAPIENTRYP MYAPIENTRY *
#endif

#define MYAPI

#define CONSTANT_ONE 1
#define ARRAY_SIZE 8

typedef unsigned long foo;

/** Returns 42 */
MYAPI foo MYAPIENTRY nopTest();

/** Returns Sum(array) + context */
MYAPI foo MYAPIENTRY arrayTest(long context, foo * array );

/** Returns *((foo *)object) */
MYAPI foo MYAPIENTRY bufferTest(void * object);

/** Returns Sum(array) + context + *((foo *)object) */
MYAPI foo MYAPIENTRY mixedTest(long context, void * object, foo * array );

/** Returns Sum(array1) + Sum(array2) + context + *((foo *)object1) + *((foo *)object2) */
MYAPI foo MYAPIENTRY doubleTest(long context, void * object1, foo * array1, void * object2, foo * array2 );

/** Returns Sum(array) + context */
MYAPI foo MYAPIENTRY arrayTestNioOnly(long context, foo * array );

/** Returns *((foo *)object) */
MYAPI foo MYAPIENTRY bufferTestNioOnly(void * object);

/** Returns Sum(array) + context + *((foo *)object) */
MYAPI foo MYAPIENTRY mixedTestNioOnly(long context, void * object, foo * array );

/** Returns Sum(array1) + Sum(array2) + context + *((foo *)object1) + *((foo *)object2) */
MYAPI foo MYAPIENTRY doubleTestNioOnly(long context, void * object1, foo * array1, void * object2, foo * array2 );

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

