package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Set;

public interface RouteReadOnly extends HasId<RouteReadOnly>, HasTransportMode, GraphProperty {
    IdFor<RouteReadOnly> getId();

    String getName();

    Set<Service> getServices();

    Agency getAgency();

    String getShortName();

    TransportMode getTransportMode();

    @Override
    GraphPropertyKey getProp();

    Set<Trip> getTrips();
}
