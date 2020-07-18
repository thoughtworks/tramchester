package com.tramchester.resources;

import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;

public class RouteCodeToClassMapper {
    private final String prefix = "RouteClass";

    // see tramchester.css

    public String map(Route route) {
        if (TransportMode.isTram(route)) {
            return prefix+route.getShortName();
        }
        return prefix + route.getTransportMode().name();
    }
}
