THISDIR=`pwd`
GLUEGEN=$THISDIR/../../../gluegen
JOGL=$THISDIR/../../../jogl

DEST=jogamp01/lib/

bdir=build-x86_64

cp -av $GLUEGEN/$bdir/gluegen-rt.jar $DEST
cp -av $GLUEGEN/$bdir/gluegen-rt-natives-linux-amd64.jar $DEST
cp -av $JOGL/$bdir/jar/jogl-all.jar $DEST
cp -av $JOGL/$bdir/jar/jogl-all-natives-linux-amd64.jar $DEST

rm -f jogamp01/build/*

cd jogamp01
ant
cd ..
