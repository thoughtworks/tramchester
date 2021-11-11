package com.tramchester.graph.filters;

import com.tramchester.domain.Agency;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;

public interface ConfigurableGraphFilter {
    void addRoute(IdFor<RouteReadOnly> id);
    void addStation(IdFor<Station> id);
    void addAgency(IdFor<Agency> agencyId);

    default void addRoutes(IdSet<RouteReadOnly> ids) {
        ids.forEach(this::addRoute);
    }
}

