package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

public interface Location<TYPE extends Location<?>> extends HasTransportMode, IdForDTO, HasId<TYPE>, GraphProperty {

    String getName();

    LatLong getLatLong();

    String getArea();

    boolean hasPlatforms();

    Set<Platform> getPlatforms();

    TransportMode getTransportMode();

    LocationType getLocationType();

}
