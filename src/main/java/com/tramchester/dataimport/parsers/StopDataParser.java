package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopData;

import static java.lang.Character.isDigit;

public class StopDataParser implements CSVEntryParser<StopData> {
    public StopData parseEntry(String... data) {
        String id = data[0];
        if (isDigit(id.toCharArray()[id.length() - 1])) {
            id = id.substring(0, data[0].length() - 1);
        }
        String code = data[1];
        String name = data[2];
        if (name.contains(",")) {
            name = name.split(",")[1].replace(" (Manchester Metrolink)", "").replace("\"", "").trim();
        }

        double latitude = 0;
        if (data[3].contains(".")) {
            latitude = Double.parseDouble(data[3]);
        }

        double longitude = 0;
        if (data[4].contains(".")) {
            longitude = Double.parseDouble(data[4]);

        }

        return new StopData(id, code, name, latitude, longitude);
    }
}