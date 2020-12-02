package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.RouteData;
import com.tramchester.domain.reference.GTFSTransportationType;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.List;
import java.util.Set;

public class RouteDataMapper extends CSVEntryMapper<RouteData> {
    private static final Logger logger = LoggerFactory.getLogger(RouteDataMapper.class);
    private int indexOfId = -1;
    private int indexOfAgencyId = -1;
    private int indexOfShortName = -1;
    private int indexOfLongName = -1;
    private int indexOfType = -1;

    private static final String METROLINK = "MET";

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
        try {
            String id = data.get(indexOfId);
            String agency = data.get(indexOfAgencyId);
            String shortName = data.get(indexOfShortName);
            String longName = data.get(indexOfLongName);
            String routeTypeText = data.get(indexOfType);
            GTFSTransportationType transportationType = GTFSTransportationType.parser.parse(routeTypeText);

            // tfgm data issue workaround
            if (METROLINK.equals(agency) && transportationType!=GTFSTransportationType.tram) {
                logger.error("Agency " + METROLINK + " seen with transport type " + transportationType.name() + " for " + data.toString());
                logger.warn("Setting transport type to " + GTFSTransportationType.tram.name() + " for " + data.toString());
                transportationType = GTFSTransportationType.tram;
            }

            return new RouteData(id, agency, shortName, longName, transportationType);
        } catch (ParseException parseException) {
            String msg = "Unable to parse " + data.toString();
            logger.error(msg);
            throw new RuntimeException(msg, parseException);
        }
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return agencyFilter.contains(data.get(1));
    }

}