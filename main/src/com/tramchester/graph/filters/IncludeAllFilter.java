package com.tramchester.graph.filters;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.repository.RouteRepository;

import java.util.Set;

public class IncludeAllFilter implements GraphFilter {

    @Override
    public boolean isFiltered() {
        return false;
    }

    @Override
    public boolean shouldInclude(RouteRepository routeRepository, Route route) {
        return true;
    }

    @Override
    public boolean shouldInclude(RouteRepository routeRepository, Set<Route> route) {
        return true;
    }

    @Override
    public boolean shouldInclude(Station station) {
        return true;
    }

    @Override
    public boolean shouldInclude(StopCall stopCall) {
        return true;
    }

    @Override
    public boolean shouldInclude(IdFor<Station> stationId) {
        return true;
    }

    @Override
    public boolean shouldInclude(Agency agency) {
        return true;
    }

}
