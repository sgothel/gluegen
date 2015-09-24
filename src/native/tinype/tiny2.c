#undef UNICODE
#define UNICODE
#include <windows.h>

const wchar_t * id = L"JogAmp Windows Universal Test PE Executable";
const wchar_t * cap = L"JogAmp";

// int __main()
// int main()
// int main( int argc, char* argv[] )
int WINAPI WinMain (HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nShowCmd)
{
    MessageBox(0, id, cap, MB_SETFOREGROUND | MB_OK);
    return 0;
}
