THISDIR=`pwd`
GLUEGEN=$THISDIR/../../../gluegen
JOGL=$THISDIR/../../../jogl

bdir=build-x86_64

rm -rf temp
rm -rf output
mkdir output

#
# make fat
#
mkdir temp
cd temp
unzip -o $GLUEGEN/$bdir/gluegen-rt.jar 
unzip -o $GLUEGEN/$bdir/gluegen-rt-natives-linux-amd64.jar 
unzip -o $JOGL/$bdir/jar/jogl-all.jar
unzip -o $JOGL/$bdir/jar/jogl-all-natives-linux-amd64.jar
mkdir -p natives/linux-amd64
mv lib*.so natives/linux-amd64/
rm -rf META-INF
jar cf ../output/jogl-fat.jar *
cd ..
rm -rf temp

#
# make multi
#
mkdir temp
cd temp
unzip -o $GLUEGEN/$bdir/gluegen-rt.jar 
unzip -o $JOGL/$bdir/jar/jogl-all.jar
rm -rf META-INF
jar cf ../output/jogl-multi.jar *
rm -rf *
unzip -o $GLUEGEN/$bdir/gluegen-rt-natives-linux-amd64.jar 
unzip -o $JOGL/$bdir/jar/jogl-all-natives-linux-amd64.jar
mkdir -p natives/linux-amd64
mv lib*.so natives/linux-amd64/
rm -rf META-INF
jar cf ../output/jogl-multi-natives-linux-amd64.jar *

cd ..
rm -rf temp

#
# Just to run some tests
#
cp -a $JOGL/$bdir/jar/jogl-test.jar output/
cp -a $GLUEGEN/make/lib/junit.jar output/

