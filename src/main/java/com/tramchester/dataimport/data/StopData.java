package com.tramchester.dataimport.data;

public class StopData {
    private String id;
    private String code;
    private String name;
    private double latitude;
    private double longitude;

    public StopData(String id, String code, String name, double latitude, double longitude) {
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
}
