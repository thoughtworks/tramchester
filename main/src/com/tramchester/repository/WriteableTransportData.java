package com.tramchester.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;

import java.util.Set;

public interface WriteableTransportData {
    void dispose();

    void reportNumbers();

    void addRouteStation(RouteStation routeStation);

    void addAgency(Agency agency);

    void addRoute(MutableRoute route);

    void addStation(Station station);

    void addPlatform(MutablePlatform platform);

    void addService(MutableService service);

    void addTrip(MutableTrip trip);

    void addDataSourceInfo(DataSourceInfo dataSourceInfo);

    void addFeedInfo(DataSourceID name, FeedInfo feedInfo);

    ////

    boolean hasAgencyId(IdFor<Agency> agencyId);
    boolean hasTripId(IdFor<Trip> tripId);
    boolean hasRouteId(IdFor<Route> routeId);
    boolean hasPlatformId(IdFor<Platform> id);
    boolean hasRouteStationId(IdFor<RouteStation> routeStationId);
    boolean hasStationId(IdFor<Station> stationId);

    ////

    Set<Service> getServicesWithoutCalendar();
    IdSet<Service> getServicesWithZeroDays();

    ////

    MutablePlatform getMutablePlatform(IdFor<Platform> id);
    MutableService getServiceById(IdFor<Service> serviceId);
    MutableRoute getMutableRoute(IdFor<Route> id);
}
