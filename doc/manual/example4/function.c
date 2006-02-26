#include <stdlib.h>
#include <malloc.h>

void* custom_allocate(int num_bytes) {
  return malloc(num_bytes);
}

void  custom_free(void* data) {
  free(data);
}
