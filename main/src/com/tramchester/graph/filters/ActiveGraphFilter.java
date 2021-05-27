package com.tramchester.graph.filters;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;

import java.util.Set;

public class ActiveGraphFilter implements GraphFilter, ConfigurableGraphFilter {
    private final IdSet<Route> routeIds;
    private final IdSet<Service> serviceIds;
    private final IdSet<Station> stationsIds;
    private final IdSet<Agency> agencyIds;

    public ActiveGraphFilter() {
        routeIds = new IdSet<>();
        serviceIds = new IdSet<>();
        stationsIds = new IdSet<>();
        agencyIds = new IdSet<>();
    }

    @Override
    public boolean isFiltered() {
        return true;
    }

    /***
     * Use with care, routes are likely to be duplicated, passing just one rarely correct for buses
     * @param id route id to add
     */
    @Override
    public void addRoute(IdFor<Route> id) {
        routeIds.add(id);
    }

    @Override
    public void addStation(IdFor<Station> id) {
        stationsIds.add(id);
    }

    @Override
    public void addAgency(IdFor<Agency> agencyId) {
        agencyIds.add(agencyId);
    }

    @Override
    public boolean shouldIncludeRoute(Route route) {
       return shouldIncludeRoute(route.getId());
    }

    @Override
    public boolean shouldIncludeRoute(IdFor<Route> routeId) {
        if (routeIds.isEmpty()) {
            return true;
        }
        return routeIds.contains(routeId);
    }

    @Override
    public boolean shouldIncludeRoutes(Set<Route> routes) {
        if (routeIds.isEmpty()) {
            return true;
        }
        return routes.stream().anyMatch(route -> routeIds.contains(route.getId()));
    }

    @Override
    public boolean shouldInclude(Station station) {
        // TODO ROUTE
        return shouldInclude(station.getId());
    }

    @Override
    public boolean shouldInclude(StopCall call) {
        // TODO route?
        return shouldInclude(call.getStationId());
    }

    @Override
    public boolean shouldInclude(IdFor<Station> stationId) {
        if (stationsIds.isEmpty()) {
            return true;
        }
        return stationsIds.contains(stationId);
    }

    @Override
    public boolean shouldIncludeAgency(Agency agency) {
        return shouldIncludeAgency(agency.getId());
    }

    @Override
    public boolean shouldIncludeAgency(IdFor<Agency> agencyId) {
        if (agencyIds.isEmpty()) {
            return true;
        }
        return agencyIds.contains(agencyId);
    }

    @Override
    public String toString() {
        StringBuilder asString = new StringBuilder();
        asString.append("ActiveGraphFilter{");
        if (!agencyIds.isEmpty()) {
            asString.append(" Only agencies:").append(agencyIds).append(" ");
        }
        if (!routeIds.isEmpty()) {
            asString.append(" Only routes:").append(routeIds).append(" ");
        }
        if (!serviceIds.isEmpty()) {
            asString.append(" Only services:").append(serviceIds).append(" ");
        }
        if (!stationsIds.isEmpty()) {
            asString.append(" Only stations:").append(stationsIds).append(" ");
        }
        asString.append("}");
        return asString.toString();
    }
}
