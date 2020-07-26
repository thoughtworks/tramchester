package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.places.Station;
import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;

import javax.swing.table.TableRowSorter;
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
    private static final String TRAM_STATION_POSTFIX = "(Manchester Metrolink)";
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

        boolean isTram = id.startsWith(Station.METROLINK_PREFIX);

        // remove quotes, if present
        name = name.replace("\"", "");
        if (isTram) {
            if (name.endsWith(TRAM_STATION_POSTFIX)) {
                name = name.substring(0, name.indexOf(TRAM_STATION_POSTFIX));
            }
        }
        name = name.trim();

        String area="";

        String[] nameParts = name.split(",");
        if (nameParts.length>=2) {
            area = nameParts[0].replace("," , "").trim();
        }

        String stopName;
        if (isTram) {
            stopName = name.replaceFirst(area+",", "").trim();
        } else {
            stopName = name;
        }

        double latitude = parseDouble(data, indexStopLat);
        double longitude = parseDouble(data, indexStopLon);

        return new StopData(id, code, area, stopName, latitude, longitude, isTram);
    }

    private double parseDouble(CSVRecord data, int indexStopLat) {
        double latitude = 0;
        String text = data.get(indexStopLat);
        if (text.contains(".")) {
            latitude = Double.parseDouble(text);
        }
        return latitude;
    }


    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return stopIds.contains(data.get(indexStopId));
    }

}