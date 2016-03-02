package com.tramchester.domain.presentation;


public class LatLong {

    private double lat;
    private double lon;

    // for json
    public LatLong() {

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
                "lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
