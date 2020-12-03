package com.tramchester.graph.graphbuild;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;

public class ActiveGraphFilter implements GraphFilter {
    private final IdSet<Route> routeCodes;
    private final IdSet<Service> serviceIds;
    private final IdSet<Station> stations;

    public ActiveGraphFilter() {
        routeCodes = new IdSet<>();
        serviceIds = new IdSet<>();
        stations = new IdSet<>();
    }

//    public void addRoute(IdFor<Route> routeId) {
//        routeCodes.add(routeId);
//    }

    public void addRoute(IdFor<Route> id) {
        routeCodes.add(id);
    }

    public void addService(Service service) {
        serviceIds.add(service.getId());
    }

    public void addStation(IdFor<Station> id) {
        stations.add(id);
    }

    public boolean shouldInclude(Route route) {
        if (routeCodes.isEmpty()) {
            return true;
        }
        return routeCodes.contains(route.getId());
    }

    public boolean shouldInclude(Service service) {
        if (serviceIds.isEmpty()) {
            return true;
        }
        return serviceIds.contains(service.getId());
    }

    public boolean shouldInclude(Station station) {
        return shouldInclude(station.getId());
    }

    public boolean shouldInclude(StopCall call) {
        return shouldInclude(call.getStation());
    }

    @Override
    public boolean shouldInclude(IdFor<Station> stationId) {
        if (stations.isEmpty()) {
            return true;
        }
        return stations.contains(stationId);
    }

    @Override
    public boolean isFiltered() {
        return true;
    }
}
