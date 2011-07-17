#ifndef __gluegen_types_h
#define __gluegen_types_h

/**
 * These are standard include replacement files
 * for gluegen processing only!
 *
 * Don't include this folder to your native compiler!
 *
 * Purpose of all files within this folder is to define a fixed bitsize
 * across all platforms to allow the resulting java type comfort all.
 * IE a 'intptr_t' shall always be 64bit.
 *
 * We use one size fits all.
 */

#ifndef __GLUEGEN__
    #error "This file is intended to be used for GlueGen code generation, not native compilation. Use the gluegen/make/stub_includes/platform variation instead!"
#endif

/**
 * Look in the GlueGen.java API documentation for the build-in types (terminal symbols) 
 * definition.
 * 
 * The following types are build-in:
 *
 * int8_t        - stdint.h
 * uint8_t       - stdint.h
 * int16_t       - stdint.h
 * uint16_t      - stdint.h
 * __int32       - windows
 * int32_t       - stdint.h
 * wchar_t       - stddef.h
 * uint32_t      - stdint.h
 * __int64       - windows
 * int64_t       - stdint.h
 * uint64_t      - stdint.h
 * ptrdiff_t     - stddef.h
 * intptr_t      - stdint.h
 * size_t        - stddef.h
 * uintptr_t     - stdint.h
 */

#define NULL ((void *)0)

#endif /* __gluegen_types_h */

