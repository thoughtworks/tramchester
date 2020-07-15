package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.RouteData;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.tramchester.dataimport.data.RouteData.BUS_TYPE;
import static com.tramchester.dataimport.data.RouteData.TRAM_TYPE;
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

    private final Set<String> agencies;
    private final boolean includeAll;

    @Override
    protected void initColumnIndex(List<String> headers) {
        indexOfId = findIndexOf(headers, Columns.route_id);
        indexOfAgencyId = findIndexOf(headers, Columns.agency_id);
        indexOfShortName = findIndexOf(headers, Columns.route_short_name);
        indexOfLongName = findIndexOf(headers, Columns.route_long_name);
        indexOfType = findIndexOf(headers, Columns.route_type);
    }

    public RouteDataMapper(Set<String> agencies, boolean cleaning) {
        this.agencies = agencies;
        if (agencies.size()==0) {
            if (cleaning) {
                logger.warn("Loading all agencies and routes");
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

        if (! ( BUS_TYPE.equals(routeType) || TRAM_TYPE.equals(routeType))) {
            String msg = format("Unexected tram type %s from %s ", routeType, data);
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
        return agencies.contains(data.get(1));
    }

    @Override
    protected ColumnDefination[] getColumns() {
        return Columns.values();
    }


}