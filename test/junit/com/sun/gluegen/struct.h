
typedef struct {
	float x, y, z;
} Vec;

typedef struct {
	Vec orig, dir;
} Camera;

typedef struct {
	unsigned int width, height;
	int superSamplingSize;
	int actvateFastRendering;
	int enableShadow;

	unsigned int maxIterations;
	float epsilon;
	float mu[4];
	float light[3];
	Camera camera;
} RenderingConfig;
