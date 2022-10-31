package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;

import java.util.Collections;
import java.util.Set;

import static com.tramchester.domain.id.HasId.asId;

public class SimpleInterchangeStation implements InterchangeStation {

    private final Station station;
//    private final Set<Route> pickupFromInterchange;
    private final InterchangeType interchangeType;

//    public SimpleInterchangeStation(Station station, Set<Route> pickupFromInterchange, InterchangeType interchangeType) {
//        this.station = station;
//        this.pickupFromInterchange = new HashSet<>(pickupFromInterchange);
//        this.interchangeType = interchangeType;
//    }

    public SimpleInterchangeStation(Station station, InterchangeType interchangeType) {
//        this(station,  new HashSet<>(station.getPickupRoutes()), interchangeType);
        this.station = station;
        this.interchangeType = interchangeType;
    }

    @Override
    public boolean isMultiMode() {
        return station.getTransportModes().size() > 1;
//        Set<TransportMode> uniqueModes = pickupFromInterchange.stream().map(Route::getTransportMode).collect(Collectors.toSet());
//        uniqueModes.addAll(station.getTransportModes());
//        return uniqueModes.size()>1;
    }

    @Override
    public String toString() {
        return "InterchangeStation{" +
                "station=" + asId(station) +
                ", interchangeType=" + interchangeType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleInterchangeStation that = (SimpleInterchangeStation) o;

        return station.equals(that.station);
    }

    @Override
    public int hashCode() {
        return station.hashCode();
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return Collections.unmodifiableSet(station.getDropoffRoutes());
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return Collections.unmodifiableSet(station.getPickupRoutes());
    }

    @Override
    public IdFor<Station> getStationId() {
        return station.getId();
    }

//    public void addPickupRoutes(Set<Route> additionalPickupRoutes) {
//        pickupFromInterchange.addAll(additionalPickupRoutes);
//    }

    @Override
    public InterchangeType getType() {
        return interchangeType;
    }

    @Override
    public Station getStation() {
        return station;
    }

}
