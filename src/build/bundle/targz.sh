#!/bin/bash

set -e

VERSION=`grep 'String version' src/java/com/Ostermiller/attesoro/Editor.java  | grep -oE '[0-9\.]+'`
TGZDIR=target/tgz
VERSIONEDNAME=attesoro-$VERSION
BUILDDIR=$TGZDIR/$VERSIONEDNAME

rm -rf $TGZDIR
mkdir -p $BUILDDIR

cp -r .git .gitignore copying.txt Makefile src $BUILDDIR



tar cfz target/attesoro.$VERSION.src.tar.gz -C $TGZDIR $VERSIONEDNAME 

echo "Created target/attesoro.$VERSION.src.tar.gz"
