package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

public interface InterchangeStation extends HasId<Station> {
    boolean isMultiMode();

    Set<Route> getDropoffRoutes();

    Set<Route> getPickupRoutes();

    IdFor<Station> getStationId();

    InterchangeType getType();

    Station getStation();

    Set<TransportMode> getTransportModes();
}
