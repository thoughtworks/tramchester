package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;

public interface Location<TYPE extends Location<?>> extends HasTransportMode, IdForDTO, HasId<TYPE>, GraphProperty {

    String getName();

    LatLong getLatLong();

    String getArea();

    boolean hasPlatforms();

    List<Platform> getPlatforms();

    TransportMode getTransportMode();

}
