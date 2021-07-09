package com.tramchester.repository;

public interface TransportData extends StationRepository, ProvidesFeedInfo, ServiceRepository, TripRepository, RouteRepository,
        PlatformRepository, DataSourceRepository, AgencyRepository {

    String getSourceName();

}
