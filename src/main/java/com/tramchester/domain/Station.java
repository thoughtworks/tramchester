package com.tramchester.domain;

public class Station {
    private final String id;
    private final String code;
    private final String name;
    private final double latitude;
    private final double longitude;
    private String proximityGroup;

    public Station(String id, String code, String name, double latitude, double longitude) {

        this.id = id;
        this.code = code;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
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

    public void setProximityGroup(String proximityGroup) {
        this.proximityGroup = proximityGroup;
    }

    public String getProximityGroup() {
        return proximityGroup;
    }
}
