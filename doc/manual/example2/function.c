float process_data(float* data, int n) {
  int i;
  float sum;
  for (i = 0; i < n; i++) {
    sum += data[i];
  }
  return sum;
}

float* global_data;
void set_global_data(float* data) {
  global_data = data;
}

float process_global_data(int n) {
  return process_data(global_data, n);
}
