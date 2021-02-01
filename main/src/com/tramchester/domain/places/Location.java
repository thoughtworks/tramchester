package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.presentation.LatLong;

import java.util.Set;

public interface Location<TYPE extends Location<?>> extends IdForDTO, HasId<TYPE>, HasTransportModes, GraphProperty {

    String getName();

    LatLong getLatLong();

    String getArea();

    boolean hasPlatforms();

    Set<Platform> getPlatforms();

    LocationType getLocationType();

}
