package com.tramchester.domain;

import com.tramchester.domain.presentation.LatLong;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Station implements Location {
    public static String METROLINK_PREFIX = "9400ZZ";

    private String area;
    private String id;
    private String name;
    private LatLong latLong;
    private boolean tram;
    private List<Platform> platforms;
    private Set<String> servesRoutes;

    public Station () {
        // deserialisation
    }

    public Station(String id, String area, String stopName, LatLong latLong, boolean tram) {
        this.id = id.intern();
        if (tram) {
            this.name = stopName.intern();
        } else if (area.isEmpty()) {
            this.name = stopName.intern();
        } else {
            this.name = String.format("%s,%s", area, stopName);
        }
        this.latLong = latLong;
        this.tram = tram;
        this.area = area;
        platforms = new LinkedList<>();
        servesRoutes = new HashSet<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }

    public static String formId(String rawId) {
        if (rawId.startsWith(METROLINK_PREFIX)) {
            // metrolink station ids include platform as final digit, remove to give id of station itself
            int index = rawId.length()-1;
            return rawId.substring(0,index);
        }
        return rawId;
    }

    @Override
    public boolean isTram() {
        return tram;
    }

    @Override
    public String getArea() {
        return area;
    }

    @Override
    public boolean hasPlatforms() {
        return !platforms.isEmpty();
    }

    @Override
    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void addPlatform(Platform platform) {
        if (!platforms.contains(platform)) {
            platforms.add(platform);
        }
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

    @Override
    public String toString() {
        return "Station{" +
                "area='" + area + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", tram=" + tram +
                //", platforms=" + platforms +
                //", servesRoutes=" + servesRoutes +
                '}';
    }

    public void addRoute(String routeId) {
        servesRoutes.add(routeId);
    }

    public Set<String> getRoutes() {
        return servesRoutes;
    }
}
