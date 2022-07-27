package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

public class RouteInterchanges {
    private final RoutePair routePair;
    private final Set<Station> interchangeStations;

    public RouteInterchanges(RoutePair routePair, Set<Station> interchangeStations) {
        this.routePair = routePair;
        this.interchangeStations = interchangeStations;
    }

    public Set<Station> getInterchangeStations() {
        return interchangeStations;
    }

    @Override
    public String toString() {
        return "RouteInterchanges{" +
                "routePair=" + routePair +
                ", interchangeStations=" + HasId.asIds(interchangeStations) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteInterchanges that = (RouteInterchanges) o;
        return routePair.equals(that.routePair) && interchangeStations.equals(that.interchangeStations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routePair, interchangeStations);
    }

    public boolean availableAt(LocalDate date, TimeRange time) {
        if (routePair.isAvailableOn(date)) {
            return interchangeStations.stream().anyMatch(station -> validForDate(station, date, time));
        }
        return false;
    }

    private boolean validForDate(Station station, LocalDate date, TimeRange time) {
        if (routePair.isAvailableOn(date)) {
            return station.servesRouteDropOff(routePair.getFirst(), date, time) &&
                    station.servesRoutePickup(routePair.getSecond(), date, time);
        }
        return false;
    }
}
