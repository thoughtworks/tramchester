package com.tramchester.domain;

import com.tramchester.domain.presentation.LatLong;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Station implements Location {
    public static String METROLINK_PREFIX = "9400ZZ";

    private String area;
    private String id;
    private String name;
    private LatLong latLong;
    private boolean tram;
    private List<Platform> platforms;
    private Set<Route> servesRoutes;

    public Station () {
        // deserialisation
        platforms = new LinkedList<>();
        servesRoutes = new HashSet<>();
    }

    public Station(String id, String area, String stationName, LatLong latLong, boolean tram) {
        this.id = id.intern();
        if (tram) {
            this.name = stationName.intern();
        } else if (area.isEmpty()) {
            this.name = stationName.intern();
        } else {
            this.name = String.format("%s,%s", area, stationName);
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

    public static String formId(String platformId) {
        if (platformId.startsWith(METROLINK_PREFIX)) {
            // metrolink station ids include platform as final digit, remove to give id of station itself
            int index = platformId.length()-1;
            return platformId.substring(0,index);
        }
        return platformId;
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

    public List<Platform> getPlatformsForRoute(Route route) {
        return platforms.stream().filter(platform -> platform.getRoutes().contains(route)).collect(Collectors.toList());
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

    public void addRoute(Route route) {
        servesRoutes.add(route);
    }

    public Set<Route> getRoutes() {
        return servesRoutes;
    }
}
