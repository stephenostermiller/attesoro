#!/bin/bash

set -e

files=$@

DICTFILE=./src/build/spell/attesoro.dict
TEMPFILE=./target/spell/dict.temp

mkdir -p target/spell

echo "Running spell check"

for file in $files
do
	ext="${file/*./}"
	if [ "$ext" == "bte" ] || [ "$ext" == "html" ] 
	then
		mode="sgml"
	else
		mode="url"
	fi
	if [ "$ext" != "java" ] || [ ! -e "${file/java/lex}" ]
	then
		cp "$file" $TEMPFILE
		aspell check --mode=$mode -x -p $DICTFILE $TEMPFILE
		if [ "`diff $TEMPFILE "$file"`" ] 
		then
			mv $TEMPFILE "$file"
		fi
	fi
done
head -n 1 $DICTFILE > $TEMPFILE
tail -n +2 $DICTFILE | sort | uniq >> $TEMPFILE
if [ "`diff $TEMPFILE $DICTFILE`" ] 
then
	cp $TEMPFILE $DICTFILE
fi

touch ./target/spell/checked
