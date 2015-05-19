package com.tramchester.domain;

import java.util.Collection;

public interface TransportData {
    public Collection<Route> getRoutes();
    Route getRoute(String routeId);
}
