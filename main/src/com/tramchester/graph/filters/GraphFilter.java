package com.tramchester.graph.filters;

import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.RouteRepository;

import java.util.Set;

public interface GraphFilter {
    boolean isFiltered();
    boolean shouldIncludeRoute(Route route);
    boolean shouldIncludeRoute(IdFor<Route> route);
    boolean shouldIncludeRoutes(Set<Route> route);
    boolean shouldInclude(Station station);
    boolean shouldInclude(StopCall stopCall);
    boolean shouldInclude(IdFor<Station> stationId);
    boolean shouldIncludeAgency(Agency agency);
    boolean shouldIncludeAgency(IdFor<Agency> agencyId);
}
