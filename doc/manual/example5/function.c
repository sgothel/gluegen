static ScreenInfo default;
static int initialized = 0;

ScreenInfo* default_screen_depth() {
  if (!initialized) {
    default.redBits = 8;
    default.greenBits = 8;
    default.blueBits = 8;
    initialized = 1;
  }
  return &default;
}

void set_screen_depth(ScreenInfo* info) {
  /* Do something ... */
}
