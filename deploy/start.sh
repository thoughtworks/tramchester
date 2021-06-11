#!/bin/bash

target=tramchester-1.0
logger Start ./$target/bin/tramchester

LOGFILE=/home/ec2-user/server/logs/tramchester_local.log

until ./$target/bin/tramchester server ./$target/config/local.yml 1> /dev/null; do
    logger ERROR tramchester Stopped
    if [ -f $LOGFILE ]; then
      logger tramchester last 5 lines of $LOGFILE
      tail -5 $LOGFILE | logger
    fi
    sleep 15
    logger ERROR tramchester Restarting
done