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

    private final Set<Route> pickupFromInterchange;

    public InterchangeStation(Station station, Set<Route> pickupFromInterchange) {
        this.station = station;
        this.pickupFromInterchange = new HashSet<>(pickupFromInterchange);
    }

    public InterchangeStation(Station station) {
        this.station = station;
        this.pickupFromInterchange = new HashSet<>(station.getPickupRoutes());
    }

    public boolean isMultiMode() {
        Set<TransportMode> uniqueModes = pickupFromInterchange.stream().map(Route::getTransportMode).collect(Collectors.toSet());
        uniqueModes.addAll(station.getTransportModes());
        return uniqueModes.size()>1;
    }

    @Override
    public String toString() {
        return "InterchangeStation{" +
                "station=" + station.getId() +
                ", connectingRoutes=" + asIds(pickupFromInterchange) +
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

    public Set<Route> getDropoffRoutes() {
        return Collections.unmodifiableSet(station.getDropoffRoutes());
    }

    public Set<Route> getPickupRoutes() {
        return Collections.unmodifiableSet(pickupFromInterchange);
    }

    public IdFor<Station> getStationId() {
        return station.getId();
    }

    public void addPickupRoutes(Set<Route> additionalPickupRoutes) {
        pickupFromInterchange.addAll(additionalPickupRoutes);
    }
}
