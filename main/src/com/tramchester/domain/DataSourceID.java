package com.tramchester.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum DataSourceID {
    internal, // for walks, MyLocation, etc
    tfgm, // transport for greater manchester
    nptg, // National Public Transport Gazetteer, locations data
    postcode,
    naptanxml, // Naptan (stops) data in xml form, cross references nptg
    naptanStopsCSV, // Naptan (stop) in csv format
    rail, // AToC rail timetable data
    naptanRailReferenceCSV, // Archived unmaintained copy of naptan RailReferences.csv, links rail data to naptan stops
    unknown;

    private static final Logger logger = LoggerFactory.getLogger(DataSourceID.class);

    public static DataSourceID findOrUnknown(String name) {
        try {
            return valueOf(name);
        }
        catch (IllegalArgumentException exception) {
            // TODO Rethrow as Runtime ??
            logger.error("Unknown DataSourceId " + name, exception);
            return unknown;
        }
    }

    @Deprecated
    public String getName() {
        return name();
    }
}
