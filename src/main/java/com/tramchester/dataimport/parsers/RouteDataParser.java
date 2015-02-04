package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.RouteData;

public class RouteDataParser implements CSVEntryParser<RouteData> {
    public RouteData parseEntry(String... data) {
        String id = data[0];
        String code = data[2];
        String name = data[3];

        return new RouteData(id, code, name);
    }
}