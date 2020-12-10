package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.reference.TransportMode;

import java.time.LocalDateTime;
import java.util.Set;

@ImplementedBy(TransportData.class)
public interface DataSourceRepository {
    Set<DataSourceInfo> getDataSourceInfo();
    LocalDateTime getNewestModTimeFor(TransportMode mode);
}
