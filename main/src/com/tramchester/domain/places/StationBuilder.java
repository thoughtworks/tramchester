package com.tramchester.domain.places;

import com.tramchester.domain.Platform;
import com.tramchester.domain.RouteReadOnly;

public interface StationBuilder {
    void addRoute(RouteReadOnly route);
    void addPlatform(Platform platform);
}
