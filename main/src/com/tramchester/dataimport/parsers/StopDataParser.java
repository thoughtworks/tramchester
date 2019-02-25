package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopData;

public class StopDataParser implements CSVEntryParser<StopData> {

    public static String tramStation = " (Manchester Metrolink)";

    public StopData parseEntry(String... data) {
        String id = data[0];
        String code = data[1];
        String name = data[2];

        String[] nameParts = name.split(",");
        String area;
        String stopName;
        if (nameParts.length>=2) {
            area = nameParts[0].trim().replace("\"","");
            StringBuilder builder = new StringBuilder();
            for(int index = 1; index<nameParts.length; index++) {
                if (index>1) {
                    builder.append(",");
                }
                builder.append(nameParts[index]);
            }
            stopName = builder.toString().trim().replace("\"","");
        } else {
            area = "";
            stopName = nameParts[0].trim().replace("\"","");
        }
        boolean isTram = false;
        if (stopName.contains(tramStation)) {
            stopName = stopName.replace(tramStation,"");
            isTram = true;
        }

        double latitude = 0;
        String latStr = data[3];
        if (latStr.contains(".")) {
            latitude = Double.parseDouble(latStr);
        }

        double longitude = 0;
        String longStr = data[4];
        if (longStr.contains(".")) {
            longitude = Double.parseDouble(longStr);
        }

        return new StopData(id, code, area, stopName, latitude, longitude, isTram);
    }
}