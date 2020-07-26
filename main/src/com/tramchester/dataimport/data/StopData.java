package com.tramchester.dataimport.data;

import com.tramchester.domain.presentation.LatLong;

import java.util.Objects;

public class StopData {
    private final String id;
    private final String code;
    private final String area;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final boolean isTram;

    public StopData(String id, String code, String area, String name, double latitude, double longitude,
                    boolean isTram) {
        this.id = id.intern();
        this.code = code.intern();
        this.area = area.intern();
        this.name = name.intern();
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

    public boolean isTFGMTram() {
        return isTram;
    }

    public LatLong getLatLong() {
        return new LatLong(latitude, longitude);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopData stopData = (StopData) o;
        return Objects.equals(id, stopData.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StopData{" +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", area='" + area + '\'' +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", isTram=" + isTram +
                '}';
    }
}
