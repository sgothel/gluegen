
PATH=`pwd`/../../../make/lib/linux-x86_64/arm-linux-gnueabi/bin:$PATH
export PATH

TEST_APP=arm-gcc-test

mkdir -p build/native
ssh jogamp@beagle01 mkdir -p projects/native-tst

gcc -o build/native/${TEST_APP} ${TEST_APP}.c
scp build/native/${TEST_APP} jogamp@beagle01:projects/native-tst/
ssh jogamp@beagle01 "cd projects/native-tst ; ./${TEST_APP}"

