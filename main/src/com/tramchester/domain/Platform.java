package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;

import java.util.Set;

public interface Platform extends HasId<Platform>, GraphProperty, CoreDomain {

    String getPlatformNumber();

    String getName();

    Set<Route> getRoutes();

    LatLong getLatLong();

    static IdFor<Platform> createId(String text) {
        return StringIdFor.createId(text);
    }
}
