
#include <stdio.h>
#include <stdint.h>

typedef struct { 
    char c;    // 1
    int32_t i; // 4
    int64_t l; // 8
} struct_m1;

typedef struct { 
    char c1;
    struct_m1 s1;
    char c2;
    struct_m1 s2[2];
} struct_m2;

#define ADDR(a) ((long)((void *)&(a)))
#define ADDRD(a,b) ((long)(ADDR(a)-ADDR(b)))

void dumpStructMetrics01() {
    struct_m1 sm1;
    struct_m2 sm2;

    printf("sz m1 %ld\n", (long) sizeof(struct_m1));
    printf("sz m2 %ld\n", (long) sizeof(struct_m2));

    printf("m1: i-0: %ld\n", ADDRD((sm1.i),(sm1)));
    printf("m1: l-0: %ld\n", ADDRD((sm1.l),(sm1)));

    printf("m2: s1-0: %ld\n", ADDRD((sm2.s1),(sm2)));
    printf("m2: s1.c-0: %ld\n", ADDRD((sm2.s1.c),(sm2)));
    printf("m2: c2-s1[: %ld\n", ADDRD((sm2.c2),(sm2.s1.l))-8);
    printf("m2: s2-0: %ld\n", ADDRD((sm2.s2),(sm2)));
    printf("m2: sz(s2[2]): %ld\n", (long) sizeof(sm2.s2));
}

typedef struct { 
    int8_t      c1;
    int8_t      v;
} struct_alignment_int8;

typedef struct { 
    int8_t      c1;
    int16_t     v;
} struct_alignment_int16;

typedef struct { 
    int8_t      c1;
    int32_t       v;
} struct_alignment_int32;

typedef struct { 
    int8_t      c1;
    int64_t      v;
} struct_alignment_int64;

typedef struct { 
    int8_t      c1;
    void *     v;
} struct_alignment_pointer;

typedef struct { 
    int8_t      c1;
    float     v;
} struct_alignment_float;

typedef struct { 
    int8_t      c1;
    double     v;
} struct_alignment_double;

typedef struct { 
    char      c1;
    long double v;
} struct_alignment_longdouble;

size_t padding(size_t totalsize, size_t typesize)   { return totalsize - typesize - sizeof(char); }
size_t alignment(size_t totalsize, size_t typesize) { return totalsize - typesize; }

int main(int argc, char * argv[] ) {
    printf("Hello World\n");
    #ifdef __arm__
        #warning __arm__
        printf("__arm__\n");
    #endif
    #ifdef __thumb__
        #warning __thumb__
        printf("__thumb__\n");
    #endif
    #ifdef __ARM_EABI__
        #warning __ARM_EABI__
        printf("__ARM_EABI__\n");
    #endif
    #ifdef __ARMEL__
        #warning __ARMEL__
        printf("__ARMEL__\n");
    #endif
    #ifdef __VFP_FP__
        #warning __VFP_FP__
        printf("__VFP_FP__\n");
    #endif
    #ifdef __MAVERICK__
        #warning __MAVERICK__
        printf("__MAVERICK__\n");
    #endif
    #ifdef __SOFTFP__
        #warning __SOFTFP__
        printf("__SOFTFP__\n");
    #endif

    printf("sizeof/alignment int8_t:       %d / %d / %d\n", sizeof(int8_t), sizeof( struct_alignment_int8 ), alignment(sizeof( struct_alignment_int8 ), sizeof(int8_t)));
    printf("sizeof/alignment int16_t:      %d / %d / %d\n", sizeof(int16_t), sizeof( struct_alignment_int16 ), alignment(sizeof( struct_alignment_int16 ), sizeof(int16_t)));
    printf("sizeof/alignment int32_t:      %d / %d / %d\n", sizeof(int32_t), sizeof( struct_alignment_int32 ), alignment(sizeof( struct_alignment_int32 ), sizeof(int32_t)));
    printf("sizeof/alignment int64_t:      %d / %d / %d\n", sizeof(int64_t), sizeof( struct_alignment_int64 ), alignment(sizeof( struct_alignment_int64 ), sizeof(int64_t)));
    printf("sizeof/alignment pointer:      %d / %d / %d\n", sizeof(void *), sizeof( struct_alignment_pointer ), alignment(sizeof( struct_alignment_pointer ), sizeof(void *)));
    printf("sizeof/alignment float:        %d / %d / %d\n", sizeof(float), sizeof( struct_alignment_float ), alignment(sizeof( struct_alignment_float ), sizeof(float)));
    printf("sizeof/alignment double:       %d / %d / %d\n", sizeof(double), sizeof( struct_alignment_double ), alignment(sizeof( struct_alignment_double ), sizeof(double)));
    printf("sizeof/alignment long double:  %d / %d / %d\n", sizeof(long double), sizeof( struct_alignment_longdouble ), alignment(sizeof( struct_alignment_longdouble ), sizeof(long double)));

    dumpStructMetrics01();
    return 0;
}
