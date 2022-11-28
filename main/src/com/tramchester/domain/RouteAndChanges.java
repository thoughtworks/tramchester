package com.tramchester.domain;

import com.tramchester.domain.places.InterchangeStation;

import java.util.Objects;
import java.util.Set;

public class RouteAndChanges {
    private final RoutePair routePair;
    private final Set<InterchangeStation> interchangeStations;

    public RouteAndChanges(RoutePair routePair, Set<InterchangeStation> stations) {
        this.routePair = routePair;
        this.interchangeStations = stations;
    }

    public Set<InterchangeStation> getInterchangeStations() {
        return interchangeStations;
    }

    @Override
    public String toString() {
        return "RouteInterchanges{" +
                "routePair=" + routePair +
                ", interchangeStations=" + interchangeStations +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteAndChanges that = (RouteAndChanges) o;
        return routePair.equals(that.routePair) && interchangeStations.equals(that.interchangeStations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routePair, interchangeStations);
    }

    public RoutePair getRoutePair() {
        return routePair;
    }
}
