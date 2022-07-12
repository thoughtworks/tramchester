package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;

import java.time.LocalDate;
import java.util.Set;

public interface Location<TYPE extends Location<?>> extends HasId<TYPE>, IdForDTO, HasGridPosition, HasTransportModes,
        GraphProperty, GraphNode, CoreDomain {

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

    // can be used as a part of a journey, see also closed stations which are temporary changes
    boolean isActive();

    /***
     * Use version that takes a date?
     * @return all drop off routes for a station, regardless of date
     */
    @Deprecated
    Set<Route> getDropoffRoutes();

    /***
     * Use version that takes a date?
     * @return all pick up routes for a station, regardless of date
     */
    @Deprecated
    Set<Route> getPickupRoutes();

    Set<Route> getDropoffRoutes(LocalDate date);

    Set<Route> getPickupRoutes(LocalDate date);

    // marked as an interchange in the source data
    boolean isMarkedInterchange();

}
