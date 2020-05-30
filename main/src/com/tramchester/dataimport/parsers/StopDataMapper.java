package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
import org.apache.commons.csv.CSVRecord;

import java.util.Set;

public class StopDataMapper extends CSVEntryMapper<StopData> {

    public static String tramStation = " (Manchester Metrolink)";
    private final boolean includeAll;
    private final Set<String> stopIds;

    public StopDataMapper(Set<String> stopIds) {
        includeAll = stopIds.isEmpty();
        this.stopIds = stopIds;
    }

    public StopData parseEntry(CSVRecord data) {
        String id = getStopId(data);
        String code = data.get(1);
        String name = data.get(2);

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
        // todo use prefix (9400ZZ) on stop id or agency name instead?
        boolean isTram = false;
        if (stopName.contains(tramStation)) {
            stopName = stopName.replace(tramStation,"");
            isTram = true;
        }

        double latitude = 0;
        String latStr = data.get(3);
        if (latStr.contains(".")) {
            latitude = Double.parseDouble(latStr);
        }

        double longitude = 0;
        String longStr = data.get(4);
        if (longStr.contains(".")) {
            longitude = Double.parseDouble(longStr);
        }

        return new StopData(id, code, area, stopName, latitude, longitude, isTram);
    }

    private String getStopId(CSVRecord data) {
        return data.get(0);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return stopIds.contains(getStopId(data));
    }
}