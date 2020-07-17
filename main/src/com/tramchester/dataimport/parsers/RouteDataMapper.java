package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.domain.GTFSTransportationType;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static java.lang.String.format;

public class RouteDataMapper extends CSVEntryMapper<RouteData> {
    private static final Logger logger = LoggerFactory.getLogger(RouteDataMapper.class);
    private int indexOfId = -1;
    private int indexOfAgencyId = -1;
    private int indexOfShortName = -1;
    private int indexOfLongName = -1;
    private int indexOfType = -1;

    private enum Columns implements ColumnDefination {
        route_id,agency_id,route_short_name,route_long_name,route_type
    }

    private final Set<String> agencyFilter;
    private final boolean includeAll;

    @Override
    protected void initColumnIndex(List<String> headers) {
        indexOfId = findIndexOf(headers, Columns.route_id);
        indexOfAgencyId = findIndexOf(headers, Columns.agency_id);
        indexOfShortName = findIndexOf(headers, Columns.route_short_name);
        indexOfLongName = findIndexOf(headers, Columns.route_long_name);
        indexOfType = findIndexOf(headers, Columns.route_type);
    }

    public RouteDataMapper(Set<String> agencyFilter, boolean cleaning) {
        this.agencyFilter = agencyFilter;
        if (agencyFilter.size()==0) {
            if (cleaning) {
                logger.warn("Loading all routes");
            }
            includeAll = true;
        } else {
            includeAll = false;
        }

    }

    public RouteData parseEntry(CSVRecord data) {
        String id = data.get(indexOfId);
        String agency = data.get(indexOfAgencyId);
        String shortName = data.get(indexOfShortName);
        String longName = data.get(indexOfLongName);
        String routeType = data.get(indexOfType);

        if (!GTFSTransportationType.validType(routeType)) {
            String msg = format("Unexpected transport type %s from %s ", routeType, data);
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return new RouteData(id, agency, shortName, longName, routeType);
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return agencyFilter.contains(data.get(1));
    }

}