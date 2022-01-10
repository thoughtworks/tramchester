package com.tramchester.domain.places;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.HasTransportModes;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;

import java.util.Set;

public interface Location<TYPE extends Location<?>> extends HasId<TYPE>, IdForDTO, HasTransportModes, GraphProperty {

    String getName();

    LatLong getLatLong();

    GridPosition getGridPosition();

    String getArea();

    boolean hasPlatforms();

    Set<Platform> getPlatforms();

    LocationType getLocationType();

    DataSourceID getDataSourceID();

    boolean hasPickup();

    boolean hasDropoff();
}
