#ifndef __GLIBC_COMPAT_SYMBOLS_H__
#define __GLIBC_COMPAT_SYMBOLS_H__ 1
 
/**
 * add other architecures below
 */
#ifdef __amd64__
   #define GLIBC_COMPAT_SYMBOL(FFF) __asm__(".symver " #FFF "," #FFF "@GLIBC_2.2.5");
#else
   #define GLIBC_COMPAT_SYMBOL(FFF) __asm__(".symver " #FFF "," #FFF "@GLIBC_2.0");
#endif /*__amd64__*/

GLIBC_COMPAT_SYMBOL(memcpy)
 
#endif /*__GLIBC_COMPAT_SYMBOLS_H__*/
