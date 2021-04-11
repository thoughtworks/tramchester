package com.tramchester.graph.filters;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;

import java.util.Set;

public class IncludeAllFilter implements GraphFilter {

    @Override
    public boolean isFiltered() {
        return false;
    }

    @Override
    public boolean shouldIncludeRoute(Route route) {
        return true;
    }

    @Override
    public boolean shouldIncludeRoute(IdFor<Route> route) {
        return true;
    }

    @Override
    public boolean shouldIncludeRoutes(Set<Route> route) {
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
    public boolean shouldIncludeAgency(Agency agency) {
        return true;
    }

    @Override
    public boolean shouldIncludeAgency(IdFor<Agency> agencyId) {
        return true;
    }

}
