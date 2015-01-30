#ifndef __gluegen_stdint_h
#define __gluegen_stdint_h

#ifdef __GLUEGEN__
    #error "This file is not intended to be used for GlueGen code generation, use the gluegen/make/stub_includes/gluegen variation instead!"
#endif

#if (defined(__STDC_VERSION__) && __STDC_VERSION__ >= 199901L) || defined(__GNUC__) || defined(__SCO__) || defined(__USLC__)
    #include <stdint.h>
#elif defined(WIN32) && defined(__GNUC__)
    #include <stdint.h>
#elif defined(_WIN64)
    typedef signed        char   int8_t;
    typedef unsigned      char  uint8_t;
    typedef signed       short  int16_t;
    typedef unsigned     short uint16_t;
    typedef            __int32  int32_t;
    typedef unsigned   __int32 uint32_t;
    typedef            __int64  int64_t;
    typedef unsigned   __int64 uint64_t;

    typedef           __int64  intptr_t;
    typedef unsigned  __int64 uintptr_t;
#elif defined(_WIN32)
    typedef signed        char   int8_t;
    typedef unsigned      char  uint8_t;
    typedef signed       short  int16_t;
    typedef unsigned     short uint16_t;
    typedef            __int32  int32_t;
    typedef unsigned   __int32 uint32_t;
    typedef            __int64  int64_t;
    typedef unsigned   __int64 uint64_t;

    typedef            __int32  intptr_t;
    typedef unsigned   __int32 uintptr_t;
#elif defined(__LP64__) || defined(__ia64__) || defined(__x86_64__) || defined(__aarch64__)
    typedef signed        char   int8_t;
    typedef unsigned      char  uint8_t;
    typedef signed       short  int16_t;
    typedef unsigned     short uint16_t;
    typedef signed         int  int32_t;
    typedef unsigned       int uint32_t;
    typedef signed        long  int64_t;
    typedef unsigned      long uint64_t;

    typedef               long  intptr_t;
    typedef unsigned      long uintptr_t;
#else
    typedef signed        char   int8_t;
    typedef unsigned      char  uint8_t;
    typedef signed       short  int16_t;
    typedef unsigned     short uint16_t;
    typedef signed         int  int32_t;
    typedef unsigned       int uint32_t;
    typedef signed   long long  int64_t;
    typedef unsigned long long uint64_t;

    typedef                int  intptr_t;
    typedef unsigned       int uintptr_t;
#endif

#endif /* __gluegen_stdint_h */
