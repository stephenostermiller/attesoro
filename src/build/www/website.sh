#!/bin/bash 

set -e

VERSION=`grep 'String version' src/java/com/Ostermiller/attesoro/Editor.java  | grep -oE '[0-9\.]+'`

mkdir -p target/www

cp src/www/*/* src/www/apache/.htaccess target/www

cd target/www

bte *.bte

rm -f *.bte
cp ../*.tar.gz .
cp ../*.jar .
cp ../../src/resources/com/Ostermiller/attesoro/Editor.properties .
sed -i -r "s/VERSION/$VERSION/g" *.html
