package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.domain.Stop;

public class StopParser implements CSVEntryParser<Stop> {
    public Stop parseEntry(String... data) {
        String id = data[0];
        String code = data[1];
        String name = data[2].split(",")[0];
        String latitude = data[3];
        String longitude = data[4];

        return new Stop(id, code, name, latitude, longitude);
    }
}