package com.tramchester.graph.filters;

import com.tramchester.domain.Agency;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;

import java.util.Set;

public class IncludeAllFilter implements GraphFilter {

    @Override
    public boolean isFiltered() {
        return false;
    }

    @Override
    public boolean shouldIncludeRoute(RouteReadOnly route) {
        return true;
    }

    @Override
    public boolean shouldIncludeRoute(IdFor<RouteReadOnly> route) {
        return true;
    }

    @Override
    public boolean shouldIncludeRoutes(Set<RouteReadOnly> route) {
        return true;
    }

    @Override
    public boolean shouldInclude(Station station) {
        return true;
    }

    @Override
    public boolean shouldInclude(CompositeStation compositeStation) {
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
