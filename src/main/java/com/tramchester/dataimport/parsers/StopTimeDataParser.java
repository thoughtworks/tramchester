package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopTimeData;

import java.time.LocalTime;


public class StopTimeDataParser implements CSVEntryParser<StopTimeData> {
    public StopTimeData parseEntry(String... data) {
        String tripId = data[0];
        LocalTime arrivalTime = null;
        LocalTime departureTime = null;

        String fieldOne = data[1];
        if (fieldOne.contains(":")) {
            arrivalTime = getDateTime(fieldOne);
        }
        if (data[2].contains(":")) {
            departureTime = getDateTime(data[2]);
        }

//        int minutesFromMidnight = 0;
//        if (fieldOne.contains(":")) {
//            minutesFromMidnight = getMinutes(fieldOne);
//        }

        String stopId = data[3];

        String stopSequence = data[4];
        String pickupType = data[5];
        String dropOffType = data[6];

        return new StopTimeData(tripId, arrivalTime, departureTime, stopId, stopSequence, pickupType, dropOffType);
    }


    private LocalTime getDateTime(String time) {
        String[] split = time.split(":");

        String hour = split[0];
        if (hour.equals("24") || hour.equals("25")) {
            hour = "00";
        }
        String minutes = split[1];
        if (split.length==3) {
            return LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minutes), Integer.parseInt(split[2]));
        } else {
            return LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minutes));
        }
        //return new DateTime(2000, 1, 1, Integer.parseInt(hour), Integer.parseInt(split[1]), Integer.parseInt(split[2]));

    }
}