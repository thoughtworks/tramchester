package com.tramchester.domain;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

public class Platform implements HasId<Platform> {

    private final IdFor<Platform> id;
    private final String name;
    private final Set<Route> servesRoutes;
    private final String platformNumber;

    public String getPlatformNumber() {
        return platformNumber;
    }

    public Platform(String id, String name) {
        this.id = IdFor.createId(id);
        this.name = name.intern();
        platformNumber = id.substring(id.length()-1);
        servesRoutes = new HashSet<>();
    }

    public String getName() {
        return format("%s platform %s", name, platformNumber);
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

    public void addRoute(Route route) {
        servesRoutes.add(route);
    }

    public Set<Route> getRoutes() {
        return servesRoutes;
    }

}
