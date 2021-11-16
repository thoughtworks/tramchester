package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
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

    Set<Route> getRoutes();

    Set<ReadonlyAgency> getAgencies();

    @Override
    Set<Platform> getPlatforms();

    boolean servesRoute(Route route);

    GridPosition getGridPosition();

    boolean hasPlatformsForRoute(Route route);

    boolean serves(TransportMode mode);
}
