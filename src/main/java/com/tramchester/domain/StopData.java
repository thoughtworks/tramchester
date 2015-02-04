package com.tramchester.domain;

public class StopData {
    private String id;
    private String code;
    private String name;
    private String latitude;
    private String longitude;

    private StopData() {
    }

    public StopData(String id, String code, String name, String latitude, String longitude) {
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
