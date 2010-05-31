#define GL_INVALID_INDEX             0xFFFFFFFFu
#define AL_FLANGER_DEFAULT_FEEDBACK (-0.5f)

typedef unsigned long foo;
typedef unsigned long size_t;

int arrayTest(long context, foo * array );

int bufferTest(void * object);

void pbTest(size_t * object);

int manyBuffersTest(void * object1, void * object2, void * object3, void * object4, void * object5);

int mixedTest(long context, void * object, foo * array );

int doubleTest(long context, void * object1, foo * array1, void * object2, foo * array2 );
