package com.tramchester.domain.presentation;

import java.util.Objects;

public class LatLong {

    private double lat; // north/south
    private double lon; // east/west

    // for json
    public LatLong() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatLong latLong = (LatLong) o;
        return Double.compare(latLong.lat, lat) == 0 &&
                Double.compare(latLong.lon, lon) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon);
    }

    public LatLong(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    // for json
    public void setLat(double lat) {
        this.lat = lat;
    }

    // for json
    public void setLon(double lon) {
        this.lon = lon;
    }

    @Override
    public String toString() {
        return "LatLong{" +
                "lon=" + lon +
                ", lat=" + lat +
                '}';
    }

}
