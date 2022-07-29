package com.tramchester.domain.places;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.GridPosition;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

public interface Station extends Location<Station> {

    @Override
    IdFor<Station> getId();

    @Override
    String getName();

    @Override
    LatLong getLatLong();

    @Override
    boolean hasPlatforms();

    @Override
    Set<TransportMode> getTransportModes();

    @Override
    DataSourceID getDataSourceID();

    Set<Platform> getPlatformsForRoute(Route route);

    boolean hasPlatform(IdFor<Platform> platformId);

    Set<Agency> getAgencies();

    @Override
    Set<Platform> getPlatforms();

    /***
     * Use version that takes a date
     * @param route
     * @return true if station serves given route
     */
    @Deprecated
    boolean servesRoutePickup(Route route);

    /***
     * Use version that takes a date
     * @param route
     * @return true if station serves given route
     */
    @Deprecated
    boolean servesRouteDropOff(Route route);

    GridPosition getGridPosition();

    boolean servesMode(TransportMode mode);

    Duration getMinChangeDuration();

    static IdFor<Station> createId(String text) {
        return StringIdFor.createId(text);
    }

}
