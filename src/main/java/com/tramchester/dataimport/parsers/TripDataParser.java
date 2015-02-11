package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.TripData;

public class TripDataParser implements CSVEntryParser<TripData> {
    public TripData parseEntry(String... data) {
        String routeId = data[0];
        String serviceId = data[1];
        String tripId = data[2];
        String tripHeadsign = data[3].split(",")[1].replace("(Manchester Metrolink)", "").replace("\"", "").trim();

        return new TripData(routeId, serviceId, tripId, tripHeadsign);
    }
}