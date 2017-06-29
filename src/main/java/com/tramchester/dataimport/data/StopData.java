package com.tramchester.dataimport.data;

import com.tramchester.domain.presentation.LatLong;

public class StopData {
    private String id;
    private String code;
    private String area;
    private String name;
    private double latitude;
    private double longitude;
    private boolean isTram;

    public StopData(String id, String code, String area, String name, double latitude, double longitude,
                    boolean isTram) {
        this.id = id;
        this.code = code;
        this.area = area;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isTram = isTram;
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

    public String getArea() {
        return area;
    }

    public boolean isTram() {
        return isTram;
    }

    public LatLong getLatLong() {
        return new LatLong(latitude, longitude);
    }
}
