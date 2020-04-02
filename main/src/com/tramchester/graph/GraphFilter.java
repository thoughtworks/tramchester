package com.tramchester.graph;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCalls;

public interface GraphFilter {
    boolean isFiltered();
    boolean shouldInclude(Route route);
    boolean shouldInclude(Service service);
    StopCalls filterStops(StopCalls stops);
}
