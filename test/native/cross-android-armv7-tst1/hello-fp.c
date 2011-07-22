#include <stdio.h>
#include <math.h>

int main(int argc, char ** argv) {
    double d1 = 3.14;
    double d2 = 2.0;
    double dr;

    dr = d1 * d2;
    printf("hello %lf * %lf = %lf\n", d1, d2, dr);

    dr = sin(d1);
    printf("hello sin(%lf) = %lf\n", d1, dr);

    return 0;
}
