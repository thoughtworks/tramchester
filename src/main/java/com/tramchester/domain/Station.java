package com.tramchester.domain;

public class Station {
    private final String id;
    private final String code;
    private final String name;
    private final String latitude;
    private final String longitude;

    public Station(String id, String code, String name, String latitude, String longitude) {

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

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
}
