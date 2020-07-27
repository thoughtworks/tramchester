package com.tramchester.graph;

public enum GraphPropertyKey {
    ID("id"),
    STATION_ID("station_id"),
    PLATFORM_ID("platform_id"),
    COST("cost"),
    HOUR("hour"),
    TIME("time"),
    TRIPS("trips"),
    SERVICE_ID("service_id"),
    TRIP_ID("trip_id"),
    ROUTE_ID("route_id"),
    ROUTE_STATION_ID("route_station_id"),
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

}
