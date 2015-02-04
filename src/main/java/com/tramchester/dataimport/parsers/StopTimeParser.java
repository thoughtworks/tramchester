package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.StopTime;


public class StopTimeParser implements CSVEntryParser<StopTime> {
    public StopTime parseEntry(String... data) {
        String tripId = data[0];
        String arrivalTime = data[1];
        String departureTime = data[2];
        String stopId = data[3];
        String stopSequence = data[4];
        String pickupType = data[5];
        String dropOffType = data[6];

        return new StopTime(tripId, arrivalTime, departureTime, stopId, stopSequence, pickupType, dropOffType);
    }
}