package com.tramchester.domain;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Location;

import java.util.Set;

public interface Platform extends GraphProperty, CoreDomain, Location<Platform> {

    String getPlatformNumber();

    //String getName();

    @Deprecated
    Set<Route> getRoutes();

//    LatLong getLatLong();

    static IdFor<Platform> createId(String text) {
        return StringIdFor.createId(text);
    }
}
