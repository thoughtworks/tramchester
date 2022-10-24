package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.HasId.asId;
import static com.tramchester.domain.id.HasId.asIds;

public class InterchangeStation {

    public enum InterchangeType {
        FromSourceData,
        NumberOfLinks,
        CompositeLinks,
        NeighbourLinks,
        Multimodal,
        FromConfig
    }

    private final Station station;
    private final Set<Route> pickupFromInterchange;
    private final InterchangeType interchangeType;

    public InterchangeStation(Station station, Set<Route> pickupFromInterchange, InterchangeType interchangeType) {
        this.station = station;
        this.pickupFromInterchange = new HashSet<>(pickupFromInterchange);
        this.interchangeType = interchangeType;
    }

    public InterchangeStation(Station station, InterchangeType interchangeType) {
        this(station,  new HashSet<>(station.getPickupRoutes()), interchangeType);
    }

    public boolean isMultiMode() {
        Set<TransportMode> uniqueModes = pickupFromInterchange.stream().map(Route::getTransportMode).collect(Collectors.toSet());
        uniqueModes.addAll(station.getTransportModes());
        return uniqueModes.size()>1;
    }

    @Override
    public String toString() {
        return "InterchangeStation{" +
                "station=" + asId(station) +
                ", pickupFromInterchange=" + asIds(pickupFromInterchange) +
                ", interchangeType=" + interchangeType +
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

    public InterchangeType getType() {
        return interchangeType;
    }

    public Station getStation() {
        return station;
    }

}
