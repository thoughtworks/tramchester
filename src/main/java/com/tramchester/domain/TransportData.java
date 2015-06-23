package com.tramchester.domain;

import java.util.Collection;
import java.util.List;

public interface TransportData {
    Collection<Route> getRoutes();
    Route getRoute(String routeId);
    List<Station> getStations();
    FeedInfo getFeedInfo();
}
