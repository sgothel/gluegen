#!/bin/sh

set -x

sdir=`dirname $(readlink -f $0)`
rdir=$sdir/../..

pandoc_md2html_local.sh $rdir/doc/GlueGen_Mapping.md                   > $rdir/doc/GlueGen_Mapping.html
pandoc_md2html_local.sh $rdir/doc/manual/index.md                      > $rdir/doc/manual/index.html
pandoc_md2html_local.sh $rdir/doc/misc/macos_nativelib_dependencies.md > $rdir/doc/misc/macos_nativelib_dependencies.html
pandoc_md2html_local.sh $rdir/doc/JogAmpPlatforms.md                   > $rdir/doc/JogAmpPlatforms.html
pandoc_md2html_local.sh $rdir/doc/JogAmpPlatforms-2.6.0.md             > $rdir/doc/JogAmpPlatforms-2.6.0.html
