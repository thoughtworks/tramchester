package com.tramchester.graph;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;

public interface GraphFilter {
    boolean isFiltered();
    boolean shouldInclude(Route route);
    boolean shouldInclude(Service service);
    boolean shouldInclude(Station station);
    boolean shouldInclude(StopCall stopCall);}
