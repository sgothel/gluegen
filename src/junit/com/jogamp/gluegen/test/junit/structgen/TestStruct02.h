//
// TestStruct02.h
//

#include "TestStruct01.h"

typedef struct {
    float r, g, b, a;
} Col4f;

typedef struct {
	Col4f color;
	Vec3f pos;
} Pixel;
