package com.tramchester.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

import java.util.Set;

public interface WriteableTransportData {
    void dispose();

    void reportNumbers();

    void addRouteStation(RouteStation routeStation);

    void addAgency(Agency agency);

    void addRoute(Route route);

    @Deprecated
    void addRouteToAgency(Agency agency, Route route);

    void addStation(Station station);

    void addPlatform(Platform platform);

    void addService(Service service);

    void addTrip(Trip trip);

    void addDataSourceInfo(DataSourceInfo dataSourceInfo);

    void addFeedInfo(DataSourceID name, FeedInfo feedInfo);

    ////

    boolean hasAgency(IdFor<Agency> agencyId);
    boolean hasTripId(IdFor<Trip> tripId);
    boolean hasPlatformId(IdFor<Platform> id);
    boolean hasRouteStationId(IdFor<RouteStation> routeStationId);
    boolean hasStationId(IdFor<Station> stationId);

    //Set<Service> getServices();

    Platform getPlatform(IdFor<Platform> id);
    Service getServiceById(IdFor<Service> serviceId);


    Set<Service> getServicesWithoutCalendar();

    IdSet<Service> getServicesWithZerpDays();
}
