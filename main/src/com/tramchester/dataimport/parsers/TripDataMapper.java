package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.TripData;
import org.apache.commons.csv.CSVRecord;

import java.util.Set;

public class TripDataMapper extends CSVEntryMapper<TripData> {

    private final Set<String> routeCodes;
    private final boolean includeAll;

    public TripDataMapper(Set<String> routeCodes) {
        this.routeCodes = routeCodes;
        includeAll = routeCodes.isEmpty();
    }

    public TripData parseEntry(CSVRecord data) {
        String routeId = getRouteId(data);
        String serviceId = data.get(1);
        String tripId = data.get(2);

        String tripHeadsign = data.get(3);
        if (tripHeadsign.contains(",")) {
            tripHeadsign = data.get(3).
                    split(",")[1].
                    replace("(Manchester Metrolink)", "").
                    replace("\"", "").trim();
        }
        return new TripData(routeId, serviceId, tripId, tripHeadsign);
    }

    private String getRouteId(CSVRecord data) {
        return data.get(0);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return routeCodes.contains(getRouteId(data));
    }
}