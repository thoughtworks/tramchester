package com.tramchester.domain.places;

import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Route;

public interface StationBuilder {
    void addRoute(Route route);
    void addPlatform(MutablePlatform platform);
}
