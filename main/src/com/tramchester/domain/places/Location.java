package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;

import java.util.Set;

public interface Location<TYPE extends Location<?>> extends IdForDTO, HasId<TYPE>, HasTransportModes, GraphProperty {

    String getName();

    LatLong getLatLong();

    GridPosition getGridPosition();

    String getArea();

    boolean hasPlatforms();

    Set<Platform> getPlatforms();

    LocationType getLocationType();

}
