THISDIR=`pwd`
GLUEGEN=$THISDIR/../../../gluegen
JOGL=$THISDIR/../../../jogl

bdir=build-x86_64

rm -rf tmp
mkdir tmp

cd tmp
unzip $THISDIR/lala02.orig.jar

cp -av $GLUEGEN/$bdir/gluegen-rt.jar .
cp -av $GLUEGEN/$bdir/gluegen-rt-natives-linux-amd64.jar .
cp -av $JOGL/$bdir/jar/jogl-all.jar .
cp -av $JOGL/$bdir/jar/jogl-all-natives-linux-amd64.jar .

jar cmf META-INF/MANIFEST.MF ../lala02.jar *jar *class org
cd ..

rm -rf tmp
