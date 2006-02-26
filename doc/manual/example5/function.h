typedef struct {
  int redBits;
  int greenBits;
  int blueBits;
} ScreenInfo;

ScreenInfo* default_screen_depth();
void set_screen_depth(ScreenInfo* info);
