package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.TripData;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Set;

public class TripDataMapper extends CSVEntryMapper<TripData> {

    private final Set<String> routeCodes;
    private final boolean includeAll;
    private int indexOfRoute = -1;
    private int indexOfService = -1;
    private int indexOfTrip = -1;
    private int indexOfHeadsign = -1;

    private enum Columns implements ColumnDefination {
        route_id,service_id,trip_id,trip_headsign
    }

    public TripDataMapper(Set<String> routeCodes) {
        this.routeCodes = routeCodes;
        includeAll = routeCodes.isEmpty();
    }

    @Override
    protected void initColumnIndex(List<String> headers) {
        indexOfRoute = findIndexOf(headers, Columns.route_id);
        indexOfService = findIndexOf(headers, Columns.service_id);
        indexOfTrip = findIndexOf(headers, Columns.trip_id);
        indexOfHeadsign = findIndexOf(headers, Columns.trip_headsign);
    }

    public TripData parseEntry(CSVRecord data) {
        String routeId = data.get(indexOfRoute);
        String serviceId = data.get(indexOfService);
        String tripId = data.get(indexOfTrip);

        String tripHeadsign = data.get(indexOfHeadsign);
        if (tripHeadsign.contains(",")) {
            tripHeadsign = data.get(3).
                    split(",")[1].
                    replace("(Manchester Metrolink)", "").
                    replace("\"", "").trim();
        }
        return new TripData(routeId, serviceId, tripId, tripHeadsign);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return routeCodes.contains(data.get(indexOfRoute));
    }


}