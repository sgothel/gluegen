
#define CL_SCHAR_MIN     (-127-1)

#define __YES__ 1

#if defined( __YES__ )
    #define TEST_A(_x)   GOOD_A
#elif defined( _WIN32) && (_MSC_VER)
    #define TEST_A(_x)   ERR_A_1
#elif defined( __unix__) || ( __sun__ )
    #define TEST_A(_x)   ERR_A_2
#else
   #define  TEST_A(_x)   ERR_A_3
#endif

#if defined( __NO__ )
    #define TEST_B   ERR_B_1
#elif defined( __YES__)
    #define TEST_B   GOOD_B
#elif defined( __unix__) || ( __sun__ )
    #define TEST_B   ERR_B_2
#else
   #define  TEST_B   ERR_B_3
#endif

#if defined( __NO__ )
    #define TEST_C   ERR_C_1
#elif defined( __NO__ )
    #define TEST_C   ERR_C_2
#elif defined( __unix__) || ( __sun__ )
    #define TEST_C   ERR_C_3
#else
   #define  TEST_C   GOOD_C
#endif

typedef char cl_char;
cl_char  TEST_A(2);
int TEST_B;
int TEST_C;

#ifndef __NO__
    #ifdef __YES__
        #ifdef CGDLL_EXPORTS
            #define ERR_D_1
        #elif defined (CG_LIB)
            #define ERR_D_2
        #else
            #define GOOD_D
        #endif
    #else
        #define ERR_D_3
    #endif
#endif

#ifdef GOOD_D
    int TEST_D_GOOD;
#elif
    int TEST_D_ERROR;
#endif

#if (defined(__NO__) && defined(__NOPE__))
    #define TEST_E_VAL ((long) 0x7FFFFFFFFFFFFFFFLL)
#else
    #define TEST_E_VAL ((long) 0x7FFFFFFFFFFFFFFFLL)
#endif

/***
 ** STD API file ..
 */

#ifndef __test_h_
#define __test_h_

#ifdef __cplusplus
extern "C" {
#endif

#if defined( __NANA__ )
    #if defined( __MINGW64__ )
        #include <cant_find_file_a.h>
    #elif defined( __NONO__ )
        #include <cant_find_file_b.h>
    #else
        #include <cant_find_file_c.h>
    #endif
    #if defined( __GNUC__ )
        #include <cant_find_file_d.h>
    #else
        #include <cant_find_file_e.h>
    #endif
#else
    #if defined( __MINGW64__ )
        #include <cant_find_file_a.h>
    #elif defined( __NONO__)
        #include <cant_find_file_b.h>
    #else
        #define TEST_F_VAL1 GOOD_F_1
    #endif
    #if defined( __GNUC__ )
        #include <cant_find_file_d.h>
    #else
        #define TEST_F_VAL2 GOOD_F_2
    #endif
#endif

#if defined( __YES__ )
    #if defined( __NONO__)
        #include <cant_find_file_a.h>
    #elif defined( __YES__)
        #define TEST_G_VAL GOOD_G
    #else
        #include <cant_find_file_b.h>
    #endif
#else
    #if defined( __MINGW64__ )
        #include <cant_find_file_a.h>
    #elif defined( __NONO__)
        #include <cant_find_file_b.h>
    #else
        #include <cant_find_file_c.h>
    #endif
#endif

int TEST_F_VAL1;
int TEST_F_VAL2;

int TEST_G_VAL;

#warning "Test warning with quotes - they must have quotes"

#ifdef __cplusplus
}
#endif

#include <cpptest-included.h>
#include <sub-inc/-cpptest-included2.h>

typedef enum SomeEnum {
  ConstEnumValue00 = 16,
  ConstEnumValue01 = (1 << ConstEnumValue00) - 1,
  ConstEnumValue02 = (10-1),
  ConstEnumValue03 = (10 - 2),
  ConstEnumValue04 = ( 10 - 3 ),
  ConstEnumValue05 = 10-4,
  ConstEnumValue06 = 10 - 11,
  ConstEnumValue07 = -2,
  ConstEnumValue08 = - 2,
  ConstEnumValueXX = 0
} SomeEnum;

#define ConstDefValue00 16
#define ConstDefValue01 ((1 << ConstDefValue00) - 1)
#define ConstDefValue02 (10-1)
#define ConstDefValue03 (10 - 2)
#define ConstDefValue04 ( 10 - 3 )
#define ConstDefValue05 10-4
#define ConstDefValue06 10 - 11
#define ConstDefValue07 -2
#define ConstDefValue08 - 2
#define ConstDefValueXX 0

const int constInt00 = ConstDefValue00;
const int constInt01 = ConstDefValue01;
const int constInt02 = ConstDefValue02;
const int constInt03 = ConstDefValue03;
const int constInt04 = ConstDefValue04;
const int constInt05 = ConstDefValue05;
const int constInt06 = ConstDefValue06;
const int constInt07 = ConstDefValue07;
const int constInt08 = ConstDefValue08;
const int constIntXX = ConstDefValueXX;

#endif /*  __test_h_ */

