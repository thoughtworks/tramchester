package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;

import java.util.Set;

public interface Location<TYPE extends Location<?>> extends HasId<TYPE>, HasGridPosition, HasTransportModes,
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
     * Use StationAvailabilityRepository if care about date and time
     * @return all drop off routes for a station, regardless of date
     */
    Set<Route> getDropoffRoutes();

    /***
     * Use StationAvailabilityRepository if care about date and time
     * @return all pick up routes for a station, regardless of date
     */
    Set<Route> getPickupRoutes();

    // marked as an interchange in the source data
    boolean isMarkedInterchange();

}
