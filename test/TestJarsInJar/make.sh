rm -rf classes
rm -rf TestJarsInJar.jar
mkdir classes
mkdir classes/sub

javac -d classes *.java

cd classes
jar cf ClassInJar1.jar ClassInJar1.class 
jar cf sub/ClassInJar2.jar ClassInJar2.class 
jar cmf ../MANIFEST.MF ../TestJarsInJar.jar ClassInJar0.class ClassInJar1.jar sub/ClassInJar2.jar 
#jar cf ../TestJarsInJar.jar ClassInJar0.class ClassInJar1.jar sub/ClassInJar2.jar 
cd ..
rm -rf classes

jar tf TestJarsInJar.jar
