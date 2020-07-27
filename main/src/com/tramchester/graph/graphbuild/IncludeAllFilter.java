package com.tramchester.graph.graphbuild;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;

public class IncludeAllFilter implements GraphFilter {
    @Override
    public boolean isFiltered() {
        return false;
    }

    @Override
    public boolean shouldInclude(Route route) {
        return true;
    }

    @Override
    public boolean shouldInclude(Service service) {
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

}
