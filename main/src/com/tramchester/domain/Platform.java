package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.presentation.LatLong;

import java.util.Set;

public interface Platform extends HasId<Platform>, GraphProperty {
    String getPlatformNumber();

    String getName();

    Set<Route> getRoutes();

    boolean servesRoute(Route route);

    LatLong getLatLong();
}
