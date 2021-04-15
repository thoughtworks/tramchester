package com.tramchester.graph;

import com.tramchester.graph.graphbuild.GraphBuilder;

public enum GraphPropertyKey {
    STATION_ID("station_id"),
    PLATFORM_ID("platform_id"),
    ROUTE_STATION_ID("route_station_id"),
    TRIP_ID("trip_id"),
    ROUTE_ID("route_id"),
    SERVICE_ID("service_id"),
    TRANSPORT_MODE("transport_mode"),
    TRANSPORT_MODES("transport_modes"),

    COST("cost"),
    HOUR("hour"),
    TIME("time"),
    DAY_OFFSET("day_offset"),
    TRIPS("trips"),
    TOWARDS_STATION_ID("towards_id"),
    LATITUDE("latitude"),
    LONGITUDE("longitude"),
    WALK_ID("walk_id"),
    STOP_SEQ_NUM("stop_seq_number");

    private final String text;

    GraphPropertyKey(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static GraphPropertyKey keyForLabel(GraphBuilder.Labels label) {
        return switch (label) {
            case TRAIN_STATION, BUS_STATION, TRAM_STATION, FERRY_STATION, SUBWAY_STATION, GROUPED -> STATION_ID;
            case ROUTE_STATION -> ROUTE_STATION_ID;
            case SERVICE -> SERVICE_ID;
            case PLATFORM -> PLATFORM_ID;
            case QUERY_NODE -> WALK_ID;
            case HOUR -> HOUR;
            case MINUTE -> TIME;
            default -> throw new RuntimeException("No Key for label" + label);
        };
    }

}
