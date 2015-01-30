#ifndef __gluegen_stddef_h
#define __gluegen_stddef_h

#ifdef __GLUEGEN__
    #error "This file is not intended to be used for GlueGen code generation, use the gluegen/make/stub_includes/gluegen variation instead!"
#endif

#if (defined(__STDC_VERSION__) && __STDC_VERSION__ >= 199901L) || defined(__GNUC__) || defined(__SCO__) || defined(__USLC__)
    #include <stddef.h>
#elif defined(WIN32) && defined(__GNUC__)
    #include <stddef.h>
#elif defined(_WIN64)
    typedef          __int64 ptrdiff_t;
    typedef unsigned __int64 size_t;
#elif defined(_WIN32)
    typedef          __int32 ptrdiff_t;
    typedef unsigned __int32 size_t;
#elif defined(__LP64__) || defined(__ia64__) || defined(__x86_64__) || defined(__aarch64__)
    typedef             long ptrdiff_t;
    typedef unsigned    long size_t;
#else
    typedef              int ptrdiff_t;
    typedef unsigned     int size_t;
#endif

#ifndef NULL
    #define NULL ((void *)0)
#endif

#endif /* __gluegen_stddef_h */
