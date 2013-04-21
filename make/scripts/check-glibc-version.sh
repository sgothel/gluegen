#! /bin/sh

if [ -z "$1" ] ; then 
    echo Usage $0 library-file
    exit 1
fi

objdump -T $1 | grep GLIBC | awk ' { print $5 " " $6 } ' | sort
