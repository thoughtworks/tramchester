package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.AgencyData;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Set;

public class AgencyDataMapper extends CSVEntryMapper<AgencyData> {

    private final Set<String> agencyFilter;
    private final boolean includeAll;
    private int indexOfId = -1;
    private int indexOfName = -1;

    public AgencyDataMapper(Set<String> agencyFilter) {
        this.agencyFilter = agencyFilter;
        includeAll = agencyFilter.isEmpty();
    }

    private enum Columns implements ColumnDefination {
        agency_id,agency_name
    }

    @Override
    public AgencyData parseEntry(CSVRecord data) {
        String id = data.get(indexOfId);
        String name = data.get(indexOfName);
        return new AgencyData(id, name);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return agencyFilter.contains(data.get(indexOfId));
    }

    @Override
    protected void initColumnIndex(List<String> headers) {
        indexOfId = findIndexOf(headers, Columns.agency_id);
        indexOfName = findIndexOf(headers, Columns.agency_name);
    }
}
