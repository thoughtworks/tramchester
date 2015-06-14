package com.tramchester.domain;

import java.util.Collection;
import java.util.List;

public interface TransportData {
    public Collection<Route> getRoutes();
    Route getRoute(String routeId);
    List<Station> getStations();
}
