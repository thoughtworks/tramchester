package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import org.apache.commons.csv.CSVRecord;

import java.util.Set;

public class RouteDataMapper implements CSVEntryMapper<RouteData> {

    private final Set<String> agencies;
    private boolean includeAll;

    public RouteDataMapper(Set<String> agencies) {
        this.agencies = agencies;
        if (agencies.size()==0) {
            includeAll = true;
        }
        else {
            includeAll = (agencies.size()==1) && (agencies.contains(DataCleanser.WILDCARD));
        }
    }

    public RouteData parseEntry(CSVRecord data) {
        String id = data.get(0);
        String agency = data.get(1);
        String shortName = data.get(2);
        String longName = data.get(3);

        return new RouteData(id, agency, shortName, longName);
    }

    @Override
    public boolean filter(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return agencies.contains(data.get(1));
    }
}