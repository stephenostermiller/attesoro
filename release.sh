#!/bin/bash

size=`ls -lah attesoro.jar`
size=${size:38:4}
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

FILES=$@
FILES=${FILES/package.html/} 
if [ "$FILES" ]
then
	echo Make: Uploading to web site: $FILES
    chmod -x install.sh
	scp -r $FILES deadsea@ostermiller.org:www/attesoro
    chmod +x install.sh
fi
