#ifndef __gluegen_stdint_h
#define __gluegen_stdint_h

#ifdef __GLUEGEN__
    #error "This file is not intended to be used for GlueGen code generation, use the gluegen/make/stub_includes/gluegen variation instead!"
#endif

#if (defined(__STDC_VERSION__) && __STDC_VERSION__ >= 199901L) || defined(__GNUC__) || defined(__SCO__) || defined(__USLC__)
    #include <stdint.h>
#elif defined(WIN32) && defined(__GNUC__)
    #include <stdint.h>
#elif defined(_WIN32)
    typedef __int32 int32_t;
    typedef unsigned __int32 uint32_t;
    typedef __int64 int64_t;
    typedef unsigned __int64 uint64_t;
#else
    typedef signed   int  int32_t;
    typedef unsigned int uint32_t;
    #if defined(__ia64__) || defined(__x86_64__)
        typedef signed   long int  int64_t;
        typedef unsigned long int uint64_t;
    #else
        typedef signed   long long int  int64_t;
        typedef unsigned long long int uint64_t;
    #endif
#endif

#endif /* __gluegen_stdint_h */
