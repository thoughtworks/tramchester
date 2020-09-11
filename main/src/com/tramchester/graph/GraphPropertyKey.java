package com.tramchester.graph;

import com.tramchester.graph.graphbuild.GraphBuilder;

public enum GraphPropertyKey {
    STATION_ID("station_id"),
    PLATFORM_ID("platform_id"),
    ROUTE_STATION_ID("route_station_id"),
    TRIP_ID("trip_id"),
    ROUTE_ID("route_id"),
    SERVICE_ID("service_id"),

    COST("cost"),
    HOUR("hour"),
    TIME("time"),
    DAY_OFFSET("day_offset"),
    TRIPS("trips"),
    TOWARDS_STATION_ID("towards_id"),
    LATITUDE("latitude"),
    LONGITUDE("longitude"),
    WALK_ID("walk_id");

    private final String text;

    GraphPropertyKey(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static GraphPropertyKey keyForLabel(GraphBuilder.Labels label) {
        switch (label) {
            case TRAIN_STATION:
            case BUS_STATION:
            case TRAM_STATION:
                return STATION_ID;
            case ROUTE_STATION:
                return ROUTE_STATION_ID;
            case SERVICE:
                return SERVICE_ID;
            case PLATFORM:
                return PLATFORM_ID;
            case QUERY_NODE:
                return WALK_ID;
            case HOUR:
                return HOUR;
            case MINUTE:
                return TIME;
            default:
                throw new RuntimeException("Not Key for " + label);
        }
    }

}
