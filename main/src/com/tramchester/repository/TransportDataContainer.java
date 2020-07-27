package com.tramchester.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import org.picocontainer.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public class TransportDataContainer implements TransportData, Disposable {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataContainer.class);

    private final IdMap<Trip> trips = new IdMap<>();        // trip id -> trip
    private final IdMap<Station> stationsById = new IdMap<>();  // station id -> station
    private final IdMap<Service> services = new IdMap<>();  // service id -> service
    private final IdMap<Route> routes = new IdMap<>();      // route id -> route
    private final IdMap<Platform> platforms = new IdMap<>(); // platformId -> platform
    private final IdMap<RouteStation> routeStations = new IdMap<>(); // routeStationId - > RouteStation
    private final IdMap<Agency> agencies = new IdMap<>(); // agencyId -> agencies

    private final Map<String, Station> tramStationsByName = new HashMap<>();  // station id -> station
    private final Set<DataSourceInfo.NameAndVersion> nameAndVersions = new HashSet<>();
    private final Map<String, FeedInfo> feedInfoMap = new HashMap<>();

    @Override
    public void dispose() {
        trips.forEach(Trip::dispose);
        trips.clear();
        stationsById.clear();
        tramStationsByName.clear();
        services.clear();
        routes.clear();
        platforms.clear();
        routeStations.clear();
        agencies.clear();
        feedInfoMap.clear();
    }

    public void reportNumbers() {
        logger.info("From " + nameAndVersions.toString());
        logger.info(format("%s agencies", agencies.size()));
        logger.info(format("%s routes", routes.size()));
        logger.info(stationsById.size() + " stations " + platforms.size() + " platforms ");
        logger.info(format("%s route stations", routeStations.size()));
        logger.info(format("%s services", services.size()));
        logger.info(format("%s trips", trips.size()));
        logger.info(format("%s feedinfos", feedInfoMap.size()));
    }

    public Service getService(IdFor<Service> serviceId) {
        return services.get(serviceId);
    }

    @Override
    public boolean hasStationId(IdFor<Station> stationId) {
        return stationsById.hasId(stationId);
    }

    @Override
    public Station getStationById(IdFor<Station> stationId) {
        if (!stationsById.hasId(stationId)) {
            String msg = "Unable to find station from ID " + stationId;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return stationsById.get(stationId);
    }

    @Override
    public Set<Station> getStations() {
        return stationsById.getValues();
    }

    @Override
    public Set<RouteStation> getRouteStations() {
        return routeStations.getValues();
    }

    @Override
    public RouteStation getRouteStationById(IdFor<RouteStation> routeStationId) {
        return routeStations.get(routeStationId);
    }

    @Override
    public RouteStation getRouteStationById(Station station, Route route) {
        return getRouteStationById(IdFor.createId(station, route));
    }

    @Override
    public Set<Service> getServices() {
        return services.getValues();
    }

    @Override
    public Set<Trip> getTrips() {
        return trips.getValues();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasRouteStationId(IdFor<RouteStation> routeStationId) {
        return routeStations.hasId(routeStationId);
    }

    public void addRouteStation(RouteStation routeStation) {
       routeStations.add(routeStation);
    }

    public boolean hasPlatformId(IdFor<Platform> platformId) {
        return platforms.hasId(platformId);
    }

    public Platform getPlatform(IdFor<Platform> platformId) {
        return platforms.get(platformId);
    }

    @Override
    public Set<Platform> getPlatforms() {
        return platforms.getValues();
    }

    @Override
    public Route getRouteById(IdFor<Route> routeId) {
        return routes.get(routeId);
    }

    @Override
    public Set<Agency> getAgencies() {
        return agencies.getValues();
    }

    @Override
    public Service getServiceById(IdFor<Service>  serviceId) {
        return services.get(serviceId);
    }

    @Override
    public DataSourceInfo getDataSourceInfo() {
        return new DataSourceInfo(nameAndVersions);
    }

    @Override
    public boolean hasServiceId(IdFor<Service>  serviceId) {
        return services.hasId(serviceId);
    }

    public void addAgency(Agency agency) {
        agencies.add(agency);
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

    @Deprecated
    public void addRouteToAgency(Agency agency, Route route) {
        agency.addRoute(route);
//        agencies.get(agency).addRoute(route);
    }

    public void addStation(Station station) {
        stationsById.add(station);
        if (TransportMode.isTram(station)) {
            tramStationsByName.put(station.getName().toLowerCase(), station);
        }
    }

    public void addPlatform(Platform platform) {
        platforms.add(platform);
    }

    public void updateTimesForServices() {
        // Cannot do this until after all stops loaded into trips
        logger.info("Updating timings for services");
        services.forEach(Service::updateTimings);
    }

    public void addService(Service service) {
        services.add(service);
    }

    @Override
    public boolean hasTripId(IdFor<Trip> tripId) {
        return trips.hasId(tripId);
    }

    @Override
    public Trip getTripById(IdFor<Trip> tripId) {
        return trips.get(tripId);
    }

    @Override
    public Set<Route> getRoutes() {
        return routes.getValues();
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
    }

    @Override
    public Optional<Platform> getPlatformById(IdFor<Platform> platformId) {
        if (platforms.hasId(platformId)) {
            return Optional.of(platforms.get(platformId));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Platform> getPlatformById(String platformText) {
        IdFor<Platform> platformId = IdFor.createId(platformText);
        if (platforms.hasId(platformId)) {
            return Optional.of(platforms.get(platformId));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Station> getTramStationByName(String name) {
        String lowerCase = name.toLowerCase();
        if (tramStationsByName.containsKey(lowerCase)) {
            return Optional.of(tramStationsByName.get(lowerCase));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Set<Service> getServicesOnDate(TramServiceDate date) {
        return services.filter(item -> item.operatesOn(date.getDate()));
    }

    public boolean hasRouteId(IdFor<Route>  routeId) {
        return routes.hasId(routeId);
    }

    public void addNameAndVersion(DataSourceInfo.NameAndVersion nameAndVersion) {
        nameAndVersions.add(nameAndVersion);
    }

    @Override
    public Map<String, FeedInfo> getFeedInfos() {
        return feedInfoMap;
    }

    public void addFeedInfo(String name, FeedInfo feedInfo) {
        feedInfoMap.put(name, feedInfo);
    }

}
