package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;

import java.util.Set;

public interface Platform extends GraphProperty, CoreDomain, Location<Platform> {

    String getPlatformNumber();

    @Deprecated
    Set<Route> getRoutes();

    static IdFor<Platform> createId(String text) {
        return StringIdFor.createId(text);
    }

    public Station getStation();

}
