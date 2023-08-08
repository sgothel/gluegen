#pragma once

#ifndef CPPTEST_INCLUDED_H
    #ifdef PRAGMA_ONCE_ENABLED
        const int pragma_once_enabled = 1;
    #else
        #define CPPTEST_INCLUDED_H
        const int pragma_once_enabled = 0;
    #endif

    // pragma-once or macro-defined test, i.e. should not be included recursively
    #include <cpptest-included.h>

    #define EXAMPLE 42

    const int GOOD_H = EXAMPLE;
#endif
