#!/bin/bash

files=$@

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
		cp "$file" temp
		aspell check --mode=$mode -x -p ./attesoro.dict temp
		if [ "`diff "temp" "$file"`" ] 
		then
			mv temp "$file"
		fi
	fi
done
head -n 1 "attesoro.dict" > temp
tail -n +2 "attesoro.dict" | sort | uniq >> temp
if [ "`diff "temp" "attesoro.dict"`" ] 
then
	mv temp "attesoro.dict"
fi
rm -f temp temp.bak
