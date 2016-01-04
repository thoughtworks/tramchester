package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopData;

public class StopDataParser implements CSVEntryParser<StopData> {

    public static String tramStation = " (Manchester Metrolink)";

    public StopData parseEntry(String... data) {
        String id = data[0];
        String code = data[1];
        int latIndex = 3;
        int longIndex = 4;
        String name;
        if (data.length==7) {
            // todo becomes redundant
            name = String.format("\"%s,%s\"", data[2], data[3]);
            latIndex++;
            longIndex++;
        } else {
            name = data[2];
        }
        String[] nameParts = name.split(",");
        String area;
        String stopName;
        if (nameParts.length==2) {
            area = nameParts[0].trim().replace("\"","");
            stopName = nameParts[1].trim().replace("\"","");
        } else {
            area = "";
            stopName = nameParts[0].trim().replace("\"","");
        }
        boolean isTram = false;
        if (stopName.contains(tramStation)) {
            stopName = stopName.replace(tramStation,"");
            isTram = true;
        }
//        if (stopName.contains(tramStation)) {
//            if (name.contains(",")) {
//                name = name.split(",")[1].replace(tramStation, "");
//            }
//        }
//        name = name.replace("\"", "").trim();

        double latitude = 0;
        String latStr = data[latIndex];
        if (latStr.contains(".")) {
            latitude = Double.parseDouble(latStr);
        }

        double longitude = 0;
        String longStr = data[longIndex];
        if (longStr.contains(".")) {
            longitude = Double.parseDouble(longStr);
        }

        return new StopData(id, code, area, stopName, latitude, longitude, isTram);
    }
}