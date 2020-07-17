package com.tramchester.domain.places;

import com.tramchester.domain.*;
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
    private TransportMode transportMode;
    private final List<Platform> platforms;
    private final Set<Route> servesRoutes;
    private final Set<Agency> servesAgencies;

    public Station () {
        // deserialisation
        platforms = new LinkedList<>();
        servesRoutes = new HashSet<>();
        servesAgencies = new HashSet<>();
    }

    public Station(String id, String area, String stationName, LatLong latLong) {
        platforms = new LinkedList<>();
        servesRoutes = new HashSet<>();
        servesAgencies = new HashSet<>();

        this.id = id.intern();
        this.name = stationName;
        this.transportMode = TransportMode.NotSet; // can't determine reliably until know the route
        this.latLong = latLong;
        this.area = area;
    }

    public static Station forTest(String id, String area, String stationName, LatLong latLong, TransportMode mode) {
        Station station = new Station(id, area, stationName, latLong);
        station.transportMode = mode;
        return station;
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

    // form the station id from the longer id that includes the platform number
    // this id is know as the atcoCode in the live data api
    public static String formId(String atcoCode) {
        if (atcoCode.startsWith(METROLINK_PREFIX)) {
            // metrolink platform ids include platform as final digit, remove to give id of station itself
            int index = atcoCode.length()-1;
            return atcoCode.substring(0,index);
        }
        return atcoCode;
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

    @Override
    public TransportMode getTransportMode() {
        return transportMode;
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
                ", mode=" + transportMode +
                ", platforms=" + HasId.asIds(platforms) +
                ", servesRoutes=" + HasId.asIds(servesRoutes) +
                '}';
    }

    public void addRoute(Route route) {
        servesRoutes.add(route);
        if (transportMode.equals(TransportMode.NotSet)) {
            transportMode = route.getTransportMode();
        } else if (!transportMode.equals(route.getTransportMode())) {
            throw new RuntimeException("Here to detect if multi-mode stations exist");
        }

        servesAgencies.add(route.getAgency());
    }

    public Set<Route> getRoutes() {
        return servesRoutes;
    }

    public Set<Agency> getAgencies() {
        return servesAgencies;
    }

    public boolean servesRoute(Route route) {
        return servesRoutes.contains(route);
    }
}
