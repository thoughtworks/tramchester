package com.tramchester.domain;

public class Station {
    public static String METROLINK_PREFIX = "9400ZZ";

    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private boolean isTram;
    private String proximityGroup;

    @Override
    public String toString() {
        return String.format("Station: [id:%s name:%s]",id, name);
    }

    public Station(String id, String area, String stopName, double latitude, double longitude, boolean isTram) {
        this.id = id;
        if (isTram) {
            this.name = stopName;
        } else if (area.isEmpty()) {
            this.name = stopName;
        } else {
            this.name = String.format("%s,%s", area, stopName);
        }
        this.latitude = latitude;
        this.longitude = longitude;
        this.isTram = isTram;
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

    public void setProximityGroup(String proximityGroup) {
        this.proximityGroup = proximityGroup;
    }

    public String getProximityGroup() {
        return proximityGroup;
    }

    public static String formId(String rawId) {
        if (rawId.startsWith(METROLINK_PREFIX)) {
            // metrolink station ids include platform as final digit, remove to give id of station itself
            int index = rawId.length()-1;
            return rawId.substring(0,index);
        }
        return rawId;
    }

    public boolean isTram() {
        return isTram;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Station station = (Station) o;

        return id.equals(station.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
