package com.tramchester.domain.places;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;

import java.util.Set;

public interface Station extends Location<Station> {

    @Override
    IdFor<Station> getId();

    @Override
    String getName();

    @Override
    LatLong getLatLong();

    @Override
    String getArea();

    @Override
    boolean hasPlatforms();

    boolean isComposite();

    @Override
    Set<TransportMode> getTransportModes();

    @Override
    LocationType getLocationType();

    @Override
    DataSourceID getDataSourceID();

    Set<Platform> getPlatformsForRoute(Route route);

    boolean hasPlatform(IdFor<Platform> platformId);

    Set<Route> getDropoffRoutes();

    Set<Route> getPickupRoutes();

    Set<Agency> getAgencies();

    @Override
    Set<Platform> getPlatforms();

    boolean servesRoutePickup(Route route);

    boolean servesRouteDropoff(Route route);

    GridPosition getGridPosition();

    boolean hasPlatformsForRoute(Route route);

    boolean servesMode(TransportMode mode);

    boolean isMarkedInterchange();

    int getMinimumChangeCost();

    static IdFor<Station> createId(String text) {
        return StringIdFor.createId(text);
    }
}
