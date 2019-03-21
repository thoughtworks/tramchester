package com.tramchester.graph;

import com.tramchester.domain.Location;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Stops;
import com.tramchester.domain.input.Trip;

import java.util.HashSet;
import java.util.Set;

public class GraphFilter {
    private Set<String> routeCodes;
    private Set<String> serviceCodes;
    private Set<String> stations;

    public GraphFilter() {
        routeCodes = new HashSet<>();
        serviceCodes = new HashSet<>();
        stations = new HashSet<>();
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

        return serviceCodes.contains(service.getServiceId());
    }

    public void addRoute(String routeCode) {
        routeCodes.add(routeCode);
    }

    public void addService(String serviceId) {
        serviceCodes.add(serviceId);
    }

    public void addStation(Location station) {
        stations.add(station.getId());
    }

    public Stops filterStops(Stops stops) {
        if (stations.size()==0) {
            return stops;
        }
        Stops filteredStops = new Stops();
        stops.forEach(stop -> {
            if (stations.contains(stop.getStation().getId())) {
                filteredStops.add(stop);
            }
        });
        return filteredStops;
    }
}
