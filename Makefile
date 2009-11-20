BITS=32

ifeq ($(BITS),32)
  CC=gcc32
  NS=win-x86
  SCFLAGS=
else
  BITS=64
  CC=gcc64
  NS=win-x64
  SCFLAGS=-D_MSC_VER=1400
endif

CFLAGS=-m$(BITS) -shared -c -fno-rtti -fPIC $(SCFLAGS) -I/usr/lib/jvm/java-6-sun/include -I../common/platform-libs/jre-include/win32
LFLAGS=-m$(BITS) -shared -fno-rtti -fPIC

SRC1=src/native/common/CPU.c
SRC2=src/native/windows/WindowsDynamicLinkerImpl_JNI.c

OBJ1=build/obj/$(NS)/CPU.o
OBJ2=build/obj/$(NS)/WindowsDynamicLinkerImpl_JNI.o

BIN=build/obj/$(NS)/gluegen-rt.dll

all: $(BIN)

$(BIN): $(OBJ1) $(OBJ2)
	$(CC) $(LFLAGS) $(OBJ1) $(OBJ2) -o $(BIN)

$(OBJ1): $(SRC1)
	$(CC) $(CFLAGS) $(SRC1) -o $(OBJ1)

$(OBJ2): $(SRC2)
	$(CC) $(CFLAGS) $(SRC2) -o $(OBJ2)

clean:
	rm -f $(BIN) $(OBJ1) $(OBJ2)

