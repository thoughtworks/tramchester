package com.tramchester.graph;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCalls;

import java.util.HashSet;
import java.util.Set;

public class ActiveGraphFilter implements GraphFilter {
    private Set<String> routeCodes;
    private Set<String> serviceCodes;
    private Set<String> stations;

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

    public void addStation(Location station) {
        stations.add(station.getId());
    }

    public void addStation(String stationId) {
        stations.add(stationId);
    }

    public boolean shouldInclude(Route route) {
        if (routeCodes.size()==0) {
            return true;
        }
        return routeCodes.contains(route.getId());
    }

    public boolean shouldInclude(Service service) {
        if (serviceCodes.size()==0) {
            return true;
        }
        return serviceCodes.contains(service.getId());
    }

    public StopCalls filterStops(StopCalls stops) {
        if (stations.size()==0) {
            return stops;
        }
        StopCalls filteredStops = new StopCalls();
        stops.forEach(stop -> {
            if (stations.contains(stop.getStation().getId())) {
                filteredStops.add(stop);
            }
        });
        return filteredStops;
    }

    @Override
    public boolean isFiltered() {
        return true;
    }
}
