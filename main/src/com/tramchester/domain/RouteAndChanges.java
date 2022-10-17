package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;

import java.util.Objects;
import java.util.Set;

public class RouteAndChanges {
    private final RoutePair routePair;
    private final Set<Station> stations;

    public RouteAndChanges(RoutePair routePair, Set<Station> stations) {
        this.routePair = routePair;
        this.stations = stations;
    }

    public Set<Station> getStations() {
        return stations;
    }

    @Override
    public String toString() {
        return "RouteInterchanges{" +
                "routePair=" + routePair +
                ", interchangeStations=" + HasId.asIds(stations) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteAndChanges that = (RouteAndChanges) o;
        return routePair.equals(that.routePair) && stations.equals(that.stations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routePair, stations);
    }

    public RoutePair getRoutePair() {
        return routePair;
    }
}
