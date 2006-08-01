/* Portions extracted from Solaris dlfcn.h */

/*
 * Copyright 2005 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 *
 *	Copyright (c) 1989 AT&T
 *	  All Rights Reserved
 *
 */

/*
 * Valid values for handle argument to dlsym(3x).
 */
#define	RTLD_NEXT		-1LL	/* look in `next' dependency */
#define	RTLD_DEFAULT		-2LL	/* look up symbol from scope */
						/*	of current object */
#define	RTLD_SELF		-3LL	/* look in `ourself' */
#define	RTLD_PROBE		-4LL	/* look up symbol from scope */
						/*	of current object, */
						/*	using currently */
						/*	loaded objects only. */

/*
 * Valid values for mode argument to dlopen.
 */
#define	RTLD_LAZY		0x00001		/* deferred function binding */
#define	RTLD_NOW		0x00002		/* immediate function binding */
#define	RTLD_NOLOAD		0x00004		/* don't load object */

#define	RTLD_GLOBAL		0x00100		/* export symbols to others */
#define	RTLD_LOCAL		0x00000		/* symbols are only available */
						/*	to group members */
#define	RTLD_PARENT		0x00200		/* add parent (caller) to */
						/*	a group dependencies */
#define	RTLD_GROUP		0x00400		/* resolve symbols within */
						/*	members of the group */
#define	RTLD_WORLD		0x00800		/* resolve symbols within */
						/*	global objects */
#define	RTLD_NODELETE		0x01000		/* do not remove members */
#define	RTLD_FIRST		0x02000		/* only first object is */
						/*	available for dlsym */

extern void	*dlopen(const char *, int);
extern void   	*dlsym(void *, const char *);
extern int	dlclose(void *);
extern char	*dlerror(void);
