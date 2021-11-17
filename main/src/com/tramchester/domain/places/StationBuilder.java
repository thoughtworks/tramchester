package com.tramchester.domain.places;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;

public interface StationBuilder {
    void addRoute(Route route);
    void addPlatform(Platform platform);
}
