package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.HasId.asIds;

public class InterchangeStation {
    private final Station station;

    private final Set<Route> connectedToRoutes;

    public InterchangeStation(Station station, Set<Route> connectedToRoutes) {
        this.station = station;
        this.connectedToRoutes = connectedToRoutes;
    }

    public boolean isMultiMode() {
        Set<TransportMode> routeModes = connectedToRoutes.stream().map(Route::getTransportMode).collect(Collectors.toSet());
        return routeModes.size()>1;
    }

    @Override
    public String toString() {
        return "InterchangeStation{" +
                "station=" + station.getId() +
                ", connectingRoutes=" + asIds(connectedToRoutes) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InterchangeStation that = (InterchangeStation) o;

        return station.equals(that.station);
    }

    @Override
    public int hashCode() {
        return station.hashCode();
    }

    public Set<Route> getSourceRoutes() {
        return station.getRoutes();
    }

    public Set<Route> getDestinationRoutes() {
        return connectedToRoutes;
    }

    public IdFor<Station> getStationId() {
        return station.getId();
    }

    public void addLinkedRoutes(Set<Route> additionalRoutes) {
        connectedToRoutes.addAll(additionalRoutes);
    }
}
