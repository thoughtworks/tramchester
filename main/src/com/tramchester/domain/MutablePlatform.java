package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphPropertyKey;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

public class MutablePlatform implements Platform {

    private final IdFor<Platform> id;
    private final String name;
    private final Set<Route> servesRoutes;
    private final String platformNumber;
    private final LatLong latLong;

    public MutablePlatform(IdFor<Platform> id, String stationName, String platformNumber, LatLong latLong) {
        this.id = id;
        this.platformNumber = platformNumber;
        this.name = format("%s platform %s", stationName, platformNumber);
        this.latLong = latLong;
        servesRoutes = new HashSet<>();
    }

    // test support
    public static Platform buildForTFGMTram(String id, String stationName, LatLong latLong) {
        String platformNumber = id.substring(id.length() - 1);
        return new MutablePlatform(StringIdFor.createId(id), stationName, platformNumber, latLong);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IdFor<Platform> getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Platform{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", platformNumber='" + platformNumber + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MutablePlatform platform = (MutablePlatform) o;

        return id.equals(platform.id);
    }

    @Override
    public String getPlatformNumber() {
        return platformNumber;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public void addRoute(Route route) {
        servesRoutes.add(route);
    }

    @Override
    public Set<Route> getRoutes() {
        return servesRoutes;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.PLATFORM_ID;
    }

    @Override
    public boolean servesRoute(Route route) {
        return servesRoutes.contains(route);
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }
}
