package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;

public interface Location<TYPE extends Location<?>> extends IdForDTO, HasId<TYPE>, HasTransportModes, GraphProperty {

    String getName();

    LatLong getLatLong();

    String getArea();

    boolean hasPlatforms();

    Set<Platform> getPlatforms();

    LocationType getLocationType();

}
