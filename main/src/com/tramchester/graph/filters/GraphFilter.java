package com.tramchester.graph.filters;

import com.tramchester.domain.ReadonlyAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;

import java.util.Set;

public interface GraphFilter {
    boolean isFiltered();

    boolean shouldIncludeAgency(ReadonlyAgency agency);
    boolean shouldIncludeAgency(IdFor<ReadonlyAgency> agencyId);

    boolean shouldIncludeRoute(Route route);
    boolean shouldIncludeRoute(IdFor<Route> route);
    boolean shouldIncludeRoutes(Set<Route> route);

    boolean shouldInclude(StopCall stopCall);

    boolean shouldInclude(Station station);
    boolean shouldInclude(CompositeStation compositeStation);
    boolean shouldInclude(IdFor<Station> stationId);

}
