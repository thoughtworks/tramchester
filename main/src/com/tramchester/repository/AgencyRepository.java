package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Agency;
import com.tramchester.domain.id.IdFor;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface AgencyRepository {
    Set<Agency> getAgencies();
    Agency get(IdFor<Agency> id);
}
