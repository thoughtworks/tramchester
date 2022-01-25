package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;

import java.util.Collection;

public interface Agency extends HasId<Agency>, CoreDomain {
    static boolean IsMetrolink(IdFor<Agency> agencyId) {
        return MutableAgency.METL.equals(agencyId);
    }

    Collection<Route> getRoutes();

    IdFor<Agency> getId();

    String getName();
    
}
