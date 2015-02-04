package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopTimeData;


public class StopTimeDataParser implements CSVEntryParser<StopTimeData> {
    public StopTimeData parseEntry(String... data) {
        String tripId = data[0];
        String arrivalTime = data[1];
        String departureTime = data[2];
        String stopId = data[3];
        String stopSequence = data[4];
        String pickupType = data[5];
        String dropOffType = data[6];

        return new StopTimeData(tripId, arrivalTime, departureTime, stopId, stopSequence, pickupType, dropOffType);
    }
}