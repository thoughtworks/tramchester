package com.tramchester.graph;

public class GraphStaticKeys {

    public static final String ID = "id";
    public static final String STATION_ID = "station_id";
    public static final String PLATFORM_ID = "platform_id";
    public static final String COST = "cost";

    // time on a node
    public static final String HOUR = "hour";
    public static final String TIME = "time";

    public static final String TRIPS = "trips"; // list of trip ids
    public static final String SERVICE_ID = "service_id";

    public static final String TRIP_ID = "trip_id";
    public static final String ROUTE_ID = "route_id";
    public static final String TOWARDS_STATION_ID = "towards_id";

    // start/end of walk properties
    public static class Walk {
        public static final String LAT = "latitude";
        public static final String LONG = "longitude";
    }
}
