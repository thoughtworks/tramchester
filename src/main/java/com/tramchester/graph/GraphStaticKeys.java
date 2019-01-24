package com.tramchester.graph;

public class GraphStaticKeys {

    // ID Common across both
    public static final String ID = "id";

    // relationship properties
//    public static final String DESTINATION = "route_station";
    public static final String DAYS = "days";
    public static final String COST = "cost";

    public static final String TIMES = "times";
    public static final String SERVICE_ID = "service_id";
    public static final String SERVICE_START_DATE = "start_date";
    public static final String SERVICE_END_DATE = "end_date";

    // experimental
    public static final String DEPART_TIME = "time";
    public static final String TRIP_ID = "trip_id";

    public static class RouteStation {
        public static final String ROUTE_NAME = "route_name";
        public static final String ROUTE_ID = "route_id";
        public static final String STATION_NAME = "station_name";
    }

    // station properties
    public static class Station {
        public static final String NAME = "name";
        public static final String LAT = "latitude";
        public static final String LONG = "longitude";
    }
}
