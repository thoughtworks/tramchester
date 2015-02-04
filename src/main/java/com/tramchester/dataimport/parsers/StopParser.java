package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.domain.StopData;

public class StopParser implements CSVEntryParser<StopData> {
    public StopData parseEntry(String... data) {
        String id = data[0];
        String code = data[1];
        String name = data[2].split(",")[0].replace("\"","");
        String latitude = data[4];
        String longitude = data[5];

        return new StopData(id, code, name, latitude, longitude);
    }
}