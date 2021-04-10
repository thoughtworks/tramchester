package com.tramchester.graph.filters;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

public interface ConfigurableGraphFilter {
    void addRoute(IdFor<Route> id);
    void addStation(IdFor<Station> id);
    void addAgency(IdFor<Agency> agencyId);
}
