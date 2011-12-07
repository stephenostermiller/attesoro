#!/bin/sh

set -e

mkdir -p target/classes

find src/java -name *.java | xargs javac -classpath target/depend/classes -sourcepath src/java -Xlint:unchecked -d target/classes
