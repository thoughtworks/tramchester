package com.tramchester.graph;

public enum GraphPropertyKeys {
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
    LONGITUDE("longitude");

    private final String text;

    GraphPropertyKeys(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
