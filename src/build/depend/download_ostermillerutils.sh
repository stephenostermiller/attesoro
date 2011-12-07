#!/bin/bash

set -e

VERSION=1.08.00

TOPDIR=`pwd`
BINFILE=ostermillerutils-$VERSION-bin.tar.gz
DEPENDDIR=target/depend
UTILSDIR=$DEPENDDIR/ostermillerutils
CLASSDIR=$DEPENDDIR/classes
UTILPATH=com/Ostermiller/util

mkdir -p $UTILSDIR
cd $UTILSDIR
if [ ! -e $BINFILE ]
then
    wget http://ostermiller.org/utils/$BINFILE
    tar xfz $BINFILE
    jar -xf ostermillerutils-$VERSION/ostermillerutils-1.08.00.jar
    rm -f  ostermillerutils.jar
    ln -s ostermillerutils-$VERSION/ostermillerutils-1.08.00.jar ostermillerutils.jar
fi

cd $TOPDIR
mkdir -p $CLASSDIR/$UTILPATH
cp $UTILSDIR/$UTILPATH/StringHelper* $UTILSDIR/$UTILPATH/UberProperties* \
    $UTILSDIR/$UTILPATH/PropertiesLexer* $UTILSDIR/$UTILPATH/PropertiesToken* \
    $UTILSDIR/$UTILPATH/Browser* $CLASSDIR/$UTILPATH
