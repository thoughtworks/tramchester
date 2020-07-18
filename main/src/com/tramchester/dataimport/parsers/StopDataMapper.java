package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StopDataMapper extends CSVEntryMapper<StopData> {
    private int indexStopId = -1;
    private int indexStopCode = -1;
    private int indexStopName = -1;
    private int indexStopLon = -1;
    private int indexStopLat = -1;

    private enum Columns implements ColumnDefination {
        stop_id, stop_code, stop_name, stop_lat, stop_lon
    }

    // not used
    @Deprecated
    private static String tramStation = " (Manchester Metrolink)";
    private final boolean includeAll;
    private final Set<String> stopIds;

    public StopDataMapper(Set<String> stopIds) {
        includeAll = stopIds.isEmpty();
        this.stopIds = stopIds;
    }

    @Override
    protected void initColumnIndex(List<String> headers) {
        indexStopId = findIndexOf(headers, Columns.stop_id);
        indexStopCode = findIndexOf(headers, Columns.stop_code);
        indexStopName = findIndexOf(headers, Columns.stop_name);
        indexStopLon = findIndexOf(headers, Columns.stop_lon);
        indexStopLat = findIndexOf(headers, Columns.stop_lat);
    }

    public StopData parseEntry(CSVRecord data) {
        String id = data.get(indexStopId);
        String code = data.get(indexStopCode);
        String name = data.get(indexStopName);

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
        String latStr = data.get(indexStopLat);
        if (latStr.contains(".")) {
            latitude = Double.parseDouble(latStr);
        }

        double longitude = 0;
        String longStr = data.get(indexStopLon);
        if (longStr.contains(".")) {
            longitude = Double.parseDouble(longStr);
        }

        return new StopData(id, code, area, stopName, latitude, longitude, isTram);
    }


    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return stopIds.contains(data.get(indexStopId));
    }

}