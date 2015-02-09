package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopTimeData;
import org.joda.time.DateTime;


public class StopTimeDataParser implements CSVEntryParser<StopTimeData> {
    public StopTimeData parseEntry(String... data) {
        String tripId = data[0];
        DateTime arrivalTime = getDateTime(data[1]);
        DateTime departureTime = getDateTime(data[2]);
        int minutesFromMidnight = getMinutes(data[1]);
        String stopId = data[3].substring(0, data[3].length() - 1);
        String stopSequence = data[4];
        String pickupType = data[5];
        String dropOffType = data[6];

        return new StopTimeData(tripId, arrivalTime, departureTime, stopId, stopSequence, pickupType, dropOffType, minutesFromMidnight);
    }

    private int getMinutes(String time) {
        String[] split = time.split(":");
        int hour = Integer.parseInt(split[0]);
        int minute = Integer.parseInt(split[1]);
        return (hour * 60) + minute;
    }

    private DateTime getDateTime(String time) {
        String[] split = time.split(":");

        String hour = split[0];
        if (hour.equals("24") || hour.equals("25")) {
            hour = "00";
        }
        return new DateTime(2000, 1, 1, Integer.parseInt(hour), Integer.parseInt(split[1]), Integer.parseInt(split[2]));

    }
}