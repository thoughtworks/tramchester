package com.tramchester.graph;

public class GraphStaticKeys {

    // ID Common across both
    public static final String ID = "id";
    public static final String STATION_ID = "station_id";
    public static final String PLATFORM_ID = "platform_id";

    // relationship properties
    public static final String DAYS = "days";
    public static final String COST = "cost";

    // time on a node
    public static final String HOUR = "hour";
    public static final String TIME = "time";

    // time(s) on a relationship
    public static final String TIMES = "times";

    public static final String TRIPS = "trips";

    public static final String SERVICE_ID = "service_id";

    public static final String SERVICE_START_DATE = "start_date";
    public static final String SERVICE_END_DATE = "end_date";
    public static final String SERVICE_EARLIEST_TIME ="earliest";
    public static final String SERVICE_LATEST_TIME ="latest";

    // experimental
    public static final String TRIP_ID = "trip_id";

    @Deprecated
    public static final String DEPART_TIME = "time";
    public static final String ROUTE_ID = "route_id";

    public static class RouteStation {
        public static final String ROUTE_NAME = "route_name";
        public static final String STATION_NAME = "station_name";
    }

    // station properties
    public static class Station {
        public static final String NAME = "name";
        public static final String LAT = "latitude";
        public static final String LONG = "longitude";
    }
}
