package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;

import java.util.Set;

public interface Location<TYPE extends Location<?>> extends HasId<TYPE>, IdForDTO, HasTransportModes, GraphProperty, CoreDomain {

    String getName();

    LatLong getLatLong();

    GridPosition getGridPosition();

    @Deprecated
    String getArea();

    IdFor<NaptanArea> getAreaId();

    boolean hasPlatforms();

    Set<Platform> getPlatforms();

    LocationType getLocationType();

    DataSourceID getDataSourceID();

    boolean hasPickup();

    boolean hasDropoff();

    default boolean isActive() {
        return hasPickup() || hasDropoff();
    }
}
