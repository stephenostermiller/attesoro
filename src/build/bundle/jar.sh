#!/bin/bash

set -e

rm -rf target/jar
mkdir -p target/jar/files

VERSION=`grep 'String version' src/java/com/Ostermiller/attesoro/Editor.java  | grep -oE '[0-9\.]+'`

cp copying.txt target/jar/files
cp -r src/resources/* target/jar/files
cp -r target/classes/* target/jar/files
cp -r target/depend/classes/* target/jar/files

jar cmf src/build/bundle/Attesoro.mf target/attesoro.$VERSION.jar -C target/jar/files com copying.txt

echo "Created target/attesoro.$VERSION.jar"
