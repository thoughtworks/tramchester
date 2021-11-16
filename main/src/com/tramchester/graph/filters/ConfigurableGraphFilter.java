package com.tramchester.graph.filters;

import com.tramchester.domain.ReadonlyAgency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;

public interface ConfigurableGraphFilter {
    void addRoute(IdFor<Route> id);
    void addStation(IdFor<Station> id);
    void addAgency(IdFor<ReadonlyAgency> agencyId);

    default void addRoutes(IdSet<Route> ids) {
        ids.forEach(this::addRoute);
    }
}

