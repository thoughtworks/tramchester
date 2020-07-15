package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
import com.tramchester.dataimport.datacleanse.TransportDataWriter;
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

    private enum Columns {
        stop_id,stop_code,stop_name,stop_lat,stop_lon
    }

    public static String tramStation = " (Manchester Metrolink)";
    private final boolean includeAll;
    private final Set<String> stopIds;

    public StopDataMapper(Set<String> stopIds) {
        includeAll = stopIds.isEmpty();
        this.stopIds = stopIds;
    }

    @Override
    public void writeHeader(TransportDataWriter writer) {
        Columns[] cols = Columns.values();
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i>0) {
                header.append(',');
            }
            header.append(cols[i].name());
        }
        writer.writeLine(header.toString());
    }

    @Override
    public void initColumnIndex(CSVRecord csvRecord) {
        List<String> headers = new ArrayList<>(csvRecord.size());
        csvRecord.forEach(headers::add);
        indexStopId = findIndexOf(headers, Columns.stop_id);
        indexStopCode = findIndexOf(headers, Columns.stop_code);
        indexStopName = findIndexOf(headers, Columns.stop_name);
        indexStopLon = findIndexOf(headers, Columns.stop_lon);
        indexStopLat = findIndexOf(headers, Columns.stop_lat);
    }

    private int findIndexOf(List<String> headers, Columns column) {
        return headers.indexOf(column.name());
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