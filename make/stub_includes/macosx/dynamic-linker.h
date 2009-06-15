/* Portions extracted from Mac OS X dlfcn.h */

/*
 * Copyright (c) 2004-2005 Apple Computer, Inc. All rights reserved.
 *
 * @APPLE_LICENSE_HEADER_START@
 * 
 * This file contains Original Code and/or Modifications of Original Code
 * as defined in and that are subject to the Apple Public Source License
 * Version 2.0 (the 'License'). You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at
 * http://www.opensource.apple.com/apsl/ and read it before using this
 * file.
 * 
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 * Please see the License for the specific language governing rights and
 * limitations under the License.
 * 
 * @APPLE_LICENSE_HEADER_END@
 */

/*
  Based on the dlcompat work done by:
		Jorge Acereda  <jacereda@users.sourceforge.net> &
		Peter O'Gorman <ogorman@users.sourceforge.net>
*/

extern int dlclose(void * __handle);
extern char * dlerror(void);
extern void * dlopen(const char * __path, int __mode);
extern void * dlsym(void * __handle, const char * __symbol);

#define RTLD_LAZY	0x1
#define RTLD_NOW	0x2
#define RTLD_LOCAL	0x4
#define RTLD_GLOBAL	0x8

/*
 * Special handle arguments for dlsym().
 */
#define	RTLD_NEXT	-1LL	/* Search subsequent objects. */
#define	RTLD_DEFAULT	-2LL	/* Use default search algorithm. */
