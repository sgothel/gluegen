
#include <stdio.h>
#include <stdint.h>

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
    long        v;
} struct_alignment_long;

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
    printf("sizeof/alignment int8_t:       %d / %d / %d\n", sizeof(int8_t), sizeof( struct_alignment_int8 ), alignment(sizeof( struct_alignment_int8 ), sizeof(int8_t)));
    printf("sizeof/alignment int16_t:      %d / %d / %d\n", sizeof(int16_t), sizeof( struct_alignment_int16 ), alignment(sizeof( struct_alignment_int16 ), sizeof(int16_t)));
    printf("sizeof/alignment int32_t:      %d / %d / %d\n", sizeof(int32_t), sizeof( struct_alignment_int32 ), alignment(sizeof( struct_alignment_int32 ), sizeof(int32_t)));
    printf("sizeof/alignment int64_t:      %d / %d / %d\n", sizeof(int64_t), sizeof( struct_alignment_int64 ), alignment(sizeof( struct_alignment_int64 ), sizeof(int64_t)));
    printf("sizeof/alignment long:         %d / %d / %d\n", sizeof(long), sizeof( struct_alignment_long ), alignment(sizeof( struct_alignment_long ), sizeof(long)));
    printf("sizeof/alignment pointer:      %d / %d / %d\n", sizeof(void *), sizeof( struct_alignment_pointer ), alignment(sizeof( struct_alignment_pointer ), sizeof(void *)));
    printf("sizeof/alignment float:        %d / %d / %d\n", sizeof(float), sizeof( struct_alignment_float ), alignment(sizeof( struct_alignment_float ), sizeof(float)));
    printf("sizeof/alignment double:       %d / %d / %d\n", sizeof(double), sizeof( struct_alignment_double ), alignment(sizeof( struct_alignment_double ), sizeof(double)));
    printf("sizeof/alignment long double:  %d / %d / %d\n", sizeof(long double), sizeof( struct_alignment_longdouble ), alignment(sizeof( struct_alignment_longdouble ), sizeof(long double)));

    return 0;
}
