package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.domain.TripData;

public class TripParser implements CSVEntryParser<TripData> {
    public TripData parseEntry(String... data) {
        String routeId = data[0];
        String serviceId = data[1];
        String tripId = data[2];
        String tripHeadsign = data[3];

        return new TripData(routeId, serviceId, tripId, tripHeadsign);
    }
}