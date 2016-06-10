package com.tramchester.domain.presentation;


import com.javadocmd.simplelatlng.LatLng;
import com.vividsolutions.jts.geom.Coordinate;

public class LatLong {

    private double lat; // north/south
    private double lon; // east/west

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
                "lon=" + lon +
                ", lat=" + lat +
                '}';
    }

    public static Coordinate getCoordinate(LatLong latLong) {
        return new Coordinate(latLong.getLon(), latLong.getLat());
    }

    public static LatLng getLatLng(LatLong latLong) {
        return new LatLng(latLong.getLat(), latLong.getLon());
    }
}
