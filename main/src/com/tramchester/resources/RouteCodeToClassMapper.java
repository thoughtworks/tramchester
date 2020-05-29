package com.tramchester.resources;

import com.tramchester.domain.Route;

public class RouteCodeToClassMapper {
    private final String prefix = "RouteClass";

    // see tramchester.css

    public String map(Route route) {
        if (!route.isTram()) {
            return prefix + "Bus";
        }
        return prefix+route.getShortName();
    }


}
