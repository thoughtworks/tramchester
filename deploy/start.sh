#!/bin/bash

target=tramchester-1.0
logger -s Start ./$target/bin/tramchester for "$PLACE"

LOGFILE=/home/ec2-user/server/logs/tramchester_local.log

if [ "$PLACE" == 'UAT' ]; then
  configFile=gm.yml
else
  configFile=gm.yml
fi

CONFIG=./$target/config/$configFile

logger -s Config is $CONFIG

until ./$target/bin/tramchester server $CONFIG 1> /dev/null; do
    logger ERROR tramchester Stopped
    if [ -f $LOGFILE ]; then
      logger tramchester last 5 lines of $LOGFILE
      tail -5 $LOGFILE | logger
    fi
    sleep 15
    logger ERROR tramchester Restarting
done