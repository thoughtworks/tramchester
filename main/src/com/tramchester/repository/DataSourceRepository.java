package com.tramchester.repository;

import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.TransportMode;

import java.time.LocalDateTime;
import java.util.Set;

public interface DataSourceRepository {
    Set<DataSourceInfo> getDataSourceInfo();
    LocalDateTime getNewestModTimeFor(TransportMode mode);
}
