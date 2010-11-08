/*
 * Copyright (c) 1994
 *    The Regents of the University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * $FreeBSD: src/include/dlfcn.h,v 1.2.2.3 2000/01/21 02:31:40 jdp Exp $
 *
 * See: ftp://ftp.cs.berkeley.edu/pub/4bsd/README.Impt.License.Change
 */


/*
 * Special handle arguments for dlsym()/dlinfo().
 */
#define    RTLD_DEFAULT        0L  /* Use default search algorithm. */
#define    RTLD_NEXT         -1LL  /* Search subsequent objects. */
#define    RTLD_SELF         -3LL  /* Search the caller itself. */

/*
 * Modes and flags for dlopen().
 */
#define    RTLD_LAZY        0x1    /* Bind function calls lazily. */
#define    RTLD_NOW         0x2    /* Bind function calls immediately. */
#define    RTLD_MODEMASK    0x3
#define    RTLD_GLOBAL    0x100    /* Make symbols globally available. */
#define    RTLD_LOCAL       0x0    /* Opposite of RTLD_GLOBAL, and the default. */

extern void    *dlopen(const char *, int);
extern void    *dlsym(void *, const char *);
extern int      dlclose(void *);
extern char    *dlerror(void);

