#! /bin/sh

#
# First unpack the OpenJDK (Temurin) package
# for amd64 and arm64.
# Then copy each lib-folder 'temurin-xy.jdk/Contents/Home/lib/'
# to their respective target lib-folder:
# - temurin-xy.jdk.amd64.lib/
# - temurin-xy.jdk.arm64.lib/
#
# Now we can run this script producing fat lipo dylib files,
# placed into 
# - temurin-xy.jdk.fat.lib
#

amd64_dir=$HOME/temurin-17.jdk.amd64.lib
arm64_dir=$HOME/temurin-17.jdk.arm64.lib
fat_dir=$HOME/temurin-17.jdk.fat.lib

rm -rf $fat_dir
mkdir $fat_dir

for i in $amd64_dir/*.dylib ; do
    bname=`basename $i`
    if [ -e $arm64_dir/$bname ] ; then
        lipo -create $i $arm64_dir/$bname -output $fat_dir/$bname
    else
        echo missing $arm64_dir/$bname
    fi
done
