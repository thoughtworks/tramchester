package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphPropertyKey;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

public class Platform implements HasId<Platform>, GraphProperty {

    private final IdFor<Platform> id;
    private final String name;
    private final Set<RouteReadOnly> servesRoutes;
    private final String platformNumber;
    private final LatLong latLong;

    public String getPlatformNumber() {
        return platformNumber;
    }

    public Platform(String id, String name, LatLong latLong) {
        this.id = StringIdFor.createId(id);
        platformNumber = id.substring(id.length()-1);

        this.name = format("%s platform %s", name, platformNumber);

        this.latLong = latLong;
        servesRoutes = new HashSet<>();
    }

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

        Platform platform = (Platform) o;

        return id.equals(platform.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public void addRoute(RouteReadOnly route) {
        servesRoutes.add(route);
    }

    public Set<RouteReadOnly> getRoutes() {
        return servesRoutes;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.PLATFORM_ID;
    }

    public boolean servesRoute(RouteReadOnly route) {
        return servesRoutes.contains(route);
    }

    public LatLong getLatLong() {
        return latLong;
    }
}
