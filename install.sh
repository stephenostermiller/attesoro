#!/bin/bash

directory=/usr/local/bin
jar=attesoro.jar
Attesoro=attesoro

if [ ! -z $1 ]
    then 
    if [ "$1" == "--help" ]
    then
        echo "Install Attesoro."
        echo "-f force"
        exit 0
    elif [ "$1" != "-f" ]
    then
        echo "Unknown option: $1" 
        exit 1
    fi
fi

workingdir=`pwd`

if [ ! -e $workingdir/$jar ]
then
    echo "Could not find '$jar'."
    echo "Make sure you execute this script from"
    echo "the directory that contains '$jar'."
    exit 1
fi

if [ ! -w "$directory" ]
then
    echo "You do not have permission to write in"
    echo "$directory"
    echo "Please become superuser."
    exit 1
fi

if [ ! -e $directory/$Attesoro ] || [ ! -z $1 ]
then
    echo "#!/bin/bash" > $directory/$Attesoro
    echo "java -classpath $workingdir/$jar com.Ostermiller.attesoro.Editor \"\$@\"" >> $directory/$Attesoro
    chmod 755 $directory/$Attesoro
    echo "$Attesoro installed."
else
    echo "$directory/$Attesoro already exists.  Use -f to overwrite."
fi
