package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.ReadonlyAgency;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface AgencyRepository {
    Set<ReadonlyAgency> getAgencies();
}
