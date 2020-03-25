package com.tramchester.graph;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Stops;

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
    public Stops filterStops(Stops stops) {
        return stops;
    }
}
