package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;

import java.util.Set;

public interface InterchangeStation {
    boolean isMultiMode();

    Set<Route> getDropoffRoutes();

    Set<Route> getPickupRoutes();

    IdFor<Station> getStationId();

    InterchangeType getType();

    Station getStation();
}
