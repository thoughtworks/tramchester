package com.tramchester.graph;

public class GraphStaticKeys {
    // node type and types
    public static final String STATION_TYPE = "station_type";

    // the three types
    public static final String STATION = "station";
    public static final String ROUTE_STATION = "route_station";
    public static final String QUERY = "query"; // a tmp node created during a compound (N to N) query


    // relationship properties
    public static final String DAYS = "days";
    public static final String COST = "cost";
    public static final String TIMES = "times";
    public static final String SERVICE_ID = "service_id";
    public static final String SERVICE_START_DATE = "start_date";
    public static final String SERVICE_END_DATE = "end_date";

    // ID Common across both
    public static final String ID = "id";

    public static class RouteStation {
        public static String IndexName = "route_stations" ;
        public static final String ROUTE_NAME = "route_name";
        public static final String ROUTE_ID = "route_id";
        public static final String STATION_NAME = "station_name";
    }

    // station properties
    public static class Station {
        public static final String NAME = "name";
        public static final String LAT = "lat";
        public static final String LONG = "lon";
        public static String IndexName = "stations";
    }
}
