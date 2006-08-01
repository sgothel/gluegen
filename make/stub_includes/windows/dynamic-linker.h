/* Windows #defines and typedefs required for processing of extracts
   from WINDOWS.H */

#define FAR
#define WINBASEAPI
#define WINAPI
#define CONST const
#define VOID void
typedef int                 BOOL;
typedef unsigned char       BYTE;
typedef unsigned int        DWORD;
typedef int                 INT;
typedef int                 INT32;
typedef __int64             INT64;
typedef float               FLOAT;
typedef struct _handle*     HANDLE;
typedef HANDLE              HMODULE;
typedef long                LONG;
typedef const char*         LPCSTR;
typedef void*               LPVOID;
typedef struct _proc*       PROC;
typedef unsigned int*       PUINT;
typedef unsigned int        UINT;
typedef unsigned short      USHORT;
typedef unsigned short      WORD;

// Dynamic loading routines
WINBASEAPI DWORD WINAPI GetLastError(VOID);
WINBASEAPI HMODULE WINAPI LoadLibraryA(LPCSTR lpLibFileName);
WINBASEAPI PROC WINAPI GetProcAddress(HMODULE hModule, LPCSTR lpProcName);
WINBASEAPI BOOL WINAPI FreeLibrary(HMODULE hLibModule);
