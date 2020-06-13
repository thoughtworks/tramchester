package com.tramchester.graph;

import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.places.Station;

import java.util.HashSet;
import java.util.Set;

public class ActiveGraphFilter implements GraphFilter {
    private final Set<String> routeCodes;
    private final Set<String> serviceCodes;
    private final Set<Station> stations;

    public ActiveGraphFilter() {
        routeCodes = new HashSet<>();
        serviceCodes = new HashSet<>();
        stations = new HashSet<>();
    }

    public void addRoute(Route route) {
        routeCodes.add(route.getId());
    }

    public void addService(String serviceId) {
        serviceCodes.add(serviceId);
    }

    public void addStation(Station station) {
        stations.add(station);
    }

    public boolean shouldInclude(Route route) {
        if (routeCodes.isEmpty()) {
            return true;
        }
        return routeCodes.contains(route.getId());
    }

    public boolean shouldInclude(Service service) {
        if (serviceCodes.isEmpty()) {
            return true;
        }
        return serviceCodes.contains(service.getId());
    }

    public boolean shouldInclude(Station station) {
        if (stations.isEmpty()) {
            return true;
        }
        return stations.contains(station);
    }

    public boolean shouldInclude(StopCall call) {
        return shouldInclude(call.getStation());
    }

    @Override
    public boolean isFiltered() {
        return true;
    }
}
