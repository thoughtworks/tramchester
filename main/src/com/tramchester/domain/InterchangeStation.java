package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.HasId.asIds;

public class InterchangeStation {
    private final Station station;

    private final Set<RouteReadOnly> connectedToRoutes;

    public InterchangeStation(Station station, Set<RouteReadOnly> connectedToRoutes) {
        this.station = station;
        this.connectedToRoutes = new HashSet<>(connectedToRoutes);
    }

    public boolean isMultiMode() {
        Set<TransportMode> uniqueModes = connectedToRoutes.stream().map(RouteReadOnly::getTransportMode).collect(Collectors.toSet());
        uniqueModes.addAll(station.getTransportModes());
        return uniqueModes.size()>1;
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

    public Set<RouteReadOnly> getSourceRoutes() {
        return station.getRoutes();
    }

    public Set<RouteReadOnly> getDestinationRoutes() {
        return Collections.unmodifiableSet(connectedToRoutes);
    }

    public IdFor<Station> getStationId() {
        return station.getId();
    }

    public void addLinkedRoutes(Set<RouteReadOnly> additionalRoutes) {
        connectedToRoutes.addAll(additionalRoutes);
    }
}
