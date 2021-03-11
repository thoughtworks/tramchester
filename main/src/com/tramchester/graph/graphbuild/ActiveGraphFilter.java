package com.tramchester.graph.graphbuild;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;

public class ActiveGraphFilter implements GraphFilter {
    private final IdSet<Route> routeCodes;
    private final IdSet<Service> serviceIds;
    private final IdSet<Station> stationsIds;
    private final IdSet<Agency> agencyIds;

    public ActiveGraphFilter() {
        routeCodes = new IdSet<>();
        serviceIds = new IdSet<>();
        stationsIds = new IdSet<>();
        agencyIds = new IdSet<>();
    }

    @Override
    public boolean isFiltered() {
        return true;
    }

    public void addRoute(IdFor<Route> id) {
        routeCodes.add(id);
    }

    public void addService(Service service) {
        serviceIds.add(service.getId());
    }

    public void addStation(IdFor<Station> id) {
        stationsIds.add(id);
    }

    public void addAgency(IdFor<Agency> agencyId) {
        agencyIds.add(agencyId);
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
    public boolean shouldInclude(Agency agency) {
        if (agencyIds.isEmpty()) {
            return true;
        }
        return agencyIds.contains(agency.getId());
    }

    @Override
    public String toString() {
        StringBuilder asString = new StringBuilder();
        asString.append("ActiveGraphFilter{");
        if (!agencyIds.isEmpty()) {
            asString.append(" Only agencies:").append(agencyIds).append(" ");
        }
        if (!routeCodes.isEmpty()) {
            asString.append(" Only routes:").append(routeCodes).append(" ");
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
