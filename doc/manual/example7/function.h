typedef struct {}     Display;
typedef struct __GLXFBConfigRec *GLXFBConfig;

GLXFBConfig *glXChooseFBConfig( Display *dpy, int screen,
                                const int *attribList, int *nitems );
