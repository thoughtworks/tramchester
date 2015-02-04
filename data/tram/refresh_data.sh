#!/bin/bash

curl -o data.zip http://odata.tfgm.com/opendata/downloads/TfGMgtfs.zip

unzip data.zip

grep ",9400" gtdf-out/stop_times.txt > stop_times.txt

grep "MET" gtdf-out/trips.txt > trips.txt

grep "9400" gtdf-out/stops.txt > stops.txt

mv gtdf-out/calendar.txt calendar.txt

mv gtdf-out/calendar_dates.txt calendar_dates.txt

rm -r gtdf-out

rm data.zip

git status

echo "The above files have changes, now you need to push them up to the repo!"