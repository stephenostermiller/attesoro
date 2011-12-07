#!/bin/bash

set -e

VERSION=`grep 'String version' src/java/com/Ostermiller/attesoro/Editor.java  | grep -oE '[0-9\.]+'`
BINDIR="/usr/local/bin"
JARDIR="/opt/attesoro"
JARNAME="attesoro.$VERSION.jar"
JARFROM="target/$JARNAME"
JARTO="$JARDIR/$JARNAME"
COMMAND="attesoro"
COMMANDFILE="$BINDIR/$COMMAND"

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

if [ ! -e $JARFROM ]
then
    echo "Could not find '$JARFROM'."
    echo "Make sure you execute this script from"
    echo "the appropriate directory."
    exit 1
fi


if [ ! -w "$BINDIR" ]
then
    echo "You do not have permission to write in $BINDIR"
    echo "Please become superuser."
    exit 1
fi

if [ -e $JARTO ]
then
    if [ ! -z $1 ]
    then
        rm -fv $JARTO
    else
        echo "$JARTO already exists.  Use -f to overwrite."
        exit 1
    fi
fi

if [ -e $COMMANDFILE ]
then
    if [ ! -z $1 ]
    then
        rm -fv $COMMANDFILE
    else
        echo "$COMMANDFILE already exists.  Use -f to overwrite."
        exit 1
    fi
fi

mkdir -p $JARDIR
cp -v $JARFROM $JARTO
echo "Creating $COMMANDFILE"
echo "#!/bin/bash" > $COMMANDFILE
echo "java -classpath $JARTO com.Ostermiller.attesoro.Editor \"\$@\"" >> $COMMANDFILE
chmod 755 $COMMANDFILE
echo "$COMMAND installed."
