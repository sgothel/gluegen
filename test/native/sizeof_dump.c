#include <stdio.h>
#include <stdint.h>
#include <stddef.h>

int main(int argc, const char ** argv) {
    printf("sizeof int: %lu\n", sizeof(int));
    printf("sizeof long: %lu\n", sizeof(long));
    printf("sizeof long long: %lu\n", sizeof(long long));
    printf("sizeof intptr_t: %lu\n", sizeof(intptr_t));
    printf("sizeof uintptr_t: %lu\n", sizeof(uintptr_t));
    printf("sizeof ptrdiff_t: %lu\n", sizeof(ptrdiff_t));
    printf("sizeof size_t: %lu\n", sizeof(size_t));
    printf("sizeof float: %lu\n", sizeof(float));
    printf("sizeof double: %lu\n", sizeof(double));
    printf("sizeof long double: %lu\n", sizeof(long double));

    return 0;
}
