#! /bin/sh

for i in *.jar ; do
    lstname=`basename $i .jar`.lst
    unzip -l $i > $lstname
done
