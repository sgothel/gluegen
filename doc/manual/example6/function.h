typedef struct {}     Display;
typedef struct {}     Visual;
typedef unsigned long VisualID;

typedef struct {
  Visual *visual;
  VisualID visualid;
  int screen;
  int depth;
  int c_class;					/* C++ */
  unsigned long red_mask;
  unsigned long green_mask;
  unsigned long blue_mask;
  int colormap_size;
  int bits_per_rgb;
} XVisualInfo;

XVisualInfo *XGetVisualInfo(
    Display*		/* display */,
    long		/* vinfo_mask */,
    XVisualInfo*	/* vinfo_template */,
    int*		/* nitems_return */
);
