
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

