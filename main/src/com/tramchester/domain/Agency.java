package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.Collection;
import java.util.Set;

public interface Agency extends HasId<Agency>, CoreDomain {
    static boolean IsMetrolink(IdFor<Agency> agencyId) {
        return MutableAgency.METL.equals(agencyId);
    }

    static IdFor<Agency> createId(String text) {
        return StringIdFor.createId(text, Agency.class);
    }

    Collection<Route> getRoutes();

    IdFor<Agency> getId();

    String getName();

    Set<TransportMode> getTransportModes();
    
}
