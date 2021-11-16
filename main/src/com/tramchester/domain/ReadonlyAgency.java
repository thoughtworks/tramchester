package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Collection;

public interface ReadonlyAgency extends HasId<ReadonlyAgency>, GraphProperty {
    static boolean IsMetrolink(IdFor<ReadonlyAgency> agencyId) {
        return Agency.METL.equals(agencyId);
    }

    Collection<Route> getRoutes();

    IdFor<ReadonlyAgency> getId();

    String getName();

    @Override
    GraphPropertyKey getProp();
}
