package com.tramchester.repository;

import com.tramchester.domain.Agency;
import com.tramchester.domain.DataSourceInfo;

import java.util.Set;

public interface TransportData extends StationRepository, ProvidesFeedInfo, ServiceRepository, TripRepository, RouteRepository,
        PlatformRepository, DataSourceRepository {

    Set<Agency> getAgencies();
    DataSourceInfo getDataSourceInfo();

}
