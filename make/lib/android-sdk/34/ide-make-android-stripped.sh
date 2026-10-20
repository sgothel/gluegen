mkdir temp
cd temp/
unzip ../android.jar
rm -rf java
rm -rf junit
rm -rf META-INF
cd ..
jar --create --file android-stripped.jar -C temp/ .
rm -rf temp
