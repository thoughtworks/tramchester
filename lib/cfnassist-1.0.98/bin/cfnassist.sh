#!/bin/bash

# get the bin directory the script is actually stored in; for details see: 
# http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do 
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" 
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# the home directory is the directory that the bin directory is in
CFNA_HOME=`dirname $DIR`

# run java
java -cp "$CFNA_HOME/lib/cfnassist.jar:$CFNA_HOME/conf:$CFNA_HOME/lib/*" tw.com.commandline.Main $*
