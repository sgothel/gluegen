
#ifndef MYAPIENTRY
#define MYAPIENTRY 
#endif
#ifndef MYAPIENTRYP
#define MYAPIENTRYP MYAPIENTRY *
#endif

#define MYAPI

#define CONSTANT_ONE 1

typedef unsigned long foo;

MYAPI int MYAPIENTRY nopTest();

MYAPI int MYAPIENTRY arrayTest(long context, foo * array );

MYAPI int MYAPIENTRY bufferTest(void * object);

MYAPI int MYAPIENTRY mixedTest(long context, void * object, foo * array );

MYAPI int MYAPIENTRY doubleTest(long context, void * object1, foo * array1, void * object2, foo * array2 );

