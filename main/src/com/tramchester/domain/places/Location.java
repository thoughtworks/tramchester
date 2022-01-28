package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;

import java.util.Set;

public interface Location<TYPE extends Location<?>> extends HasId<TYPE>, IdForDTO, HasTransportModes, GraphProperty, GraphNode, CoreDomain {

    String getName();

    LatLong getLatLong();

    GridPosition getGridPosition();

    IdFor<NaptanArea> getAreaId();

    boolean hasPlatforms();

    Set<Platform> getPlatforms();

    LocationType getLocationType();

    DataSourceID getDataSourceID();

    boolean hasPickup();

    boolean hasDropoff();

    boolean isActive();

    // this default makes no sense for walking locations etc
//    default boolean isActive() {
//        return hasPickup() || hasDropoff();
//    }

    Set<Route> getDropoffRoutes();

    Set<Route> getPickupRoutes();

}
