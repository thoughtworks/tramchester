package com.tramchester.domain;

public class Station {
    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;

    @Override
    public String toString() {
        return String.format("Station: [id:%s name:%s]",id, name);
    }

    public Station(String id, String name, double latitude, double longitude) {

        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

}
