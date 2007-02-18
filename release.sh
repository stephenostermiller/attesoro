#!/bin/bash

size=`ls -lah attesoro.jar | grep -oE '[0-9]+[kK]'`
if [ -z "`grep -i $size download.bte`" ]
then
    echo "attesoro.jar size is $size but download.bte does not show that."
    exit 1
fi
if [ -z "`grep -i $size download.html`" ]
then
    echo "attesoro.jar size is $size but download.html does not show that."
    exit 1
fi

latestversion=`grep -oE 'attesoro_[0-9]+_[0-9]+.jar' download.bte | sort | tail -n 1`
cp attesoro.jar "$latestversion"

FILES="$@ $latestversion"
FILES=${FILES/package.html/} 
if [ "$FILES" ]
then
	echo Make: Uploading to web site: $FILES
    chmod -x install.sh
	cp -r $FILES /home/steveo/attesoro.org/
    chmod +x install.sh
fi

rm "$latestversion"
