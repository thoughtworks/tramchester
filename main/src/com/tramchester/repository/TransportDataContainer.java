package com.tramchester.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TransportDataContainer implements TransportDataSource {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataContainer.class);

    private final HashMap<String, Trip> trips = new HashMap<>();        // trip id -> trip
    private final HashMap<String, Station> stationsById = new HashMap<>();  // station id -> station
    private final HashMap<String, Station> tramStationsByName = new HashMap<>();  // station id -> station
    private final HashMap<String, Service> services = new HashMap<>();  // service id -> service
    private final HashMap<String, Route> routes = new HashMap<>();      // route id -> route
    private final HashMap<String, Platform> platforms = new HashMap<>(); // platformId -> platform
    private final HashMap<String, RouteStation> routeStations = new HashMap<>(); // routeStationId - > RouteStation
    private final HashMap<String, Agency> agencies = new HashMap<>(); // agencyId -> agencies
    private FeedInfo feedInfo = null;
    private String version;

    @Override
    public void dispose() {
        trips.clear();
        stationsById.clear();
        trips.clear();
        tramStationsByName.clear();
        trips.clear();
        services.clear();
        routes.clear();
        services.clear();
        platforms.clear();
        agencies.clear();
    }

    public void SetFeedInfo(FeedInfo feedInfo) {
        this.feedInfo = feedInfo;
    }

    public void SetVersion(String version) {
        this.version = version;
    }

    public void reportNumbers() {
        logger.info(format("%s agencies", agencies.size()));
        logger.info(format("%s routes", routes.size()));
        logger.info(stationsById.size() + " stations " + platforms.size() + " platforms ");
        logger.info(format("%s route stations", routeStations.size()));
        logger.info(format("%s services", services.size()));
        logger.info(format("%s trips", trips.size()));
    }

    @Override
    public Collection<Service> getServices() {
        return services.values();
    }

    @Override
    public Collection<Trip> getTrips() {
        return trips.values();
    }

    public Service getService(String serviceId) {
        return services.get(serviceId);
    }

    @Override
    public boolean hasStationId(String stationId) {
        return stationsById.containsKey(stationId);
    }

    @Override
    public Station getStation(String stationId) {
        if (!stationsById.containsKey(stationId)) {
            String msg = "Unable to find station from ID " + stationId;
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        return stationsById.get(stationId);
    }

    @Override
    public Set<Station> getStations() {
        return new HashSet<>(stationsById.values());
    }

    @Override
    public Set<RouteStation> getRouteStations() {
        return new HashSet<>(routeStations.values());
    }

    @Override
    public RouteStation getRouteStation(String routeStationId) {
        return routeStations.get(routeStationId);
    }

    public boolean hasRouteStation(String routeStationId) {
        return routeStations.containsKey(routeStationId);
    }

    public void addRouteStation(RouteStation routeStation) {
       routeStations.put(routeStation.getId(), routeStation);
    }

    public boolean hasPlatformId(String platformId) {
        return platforms.containsKey(platformId);
    }

    public Platform getPlatform(String platformId) {
        return platforms.get(platformId);
    }

    @Override
    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    @Override
    public Collection<Agency> getAgencies() {
        return agencies.values();
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public Service getServiceById(String serviceId) {
        return services.get(serviceId);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasServiceId(String serviceId) {
        return services.containsKey(serviceId);
    }

    public void addAgency(Agency agency) {
        agencies.put(agency.getId(), agency);
    }

    public void addRoute(Route route) {
        routes.put(route.getId(), route);
    }

    public void addRouteToAgency(Agency agency, Route route) {
        agencies.get(agency.getId()).addRoute(route);
    }

    public void addStation(Station station) {
        String stationId = station.getId();
        stationsById.put(stationId, station);
        if (TransportMode.isTram(station)) {
            tramStationsByName.put(station.getName().toLowerCase(), station);
        }
    }

    public void addPlatform(Platform platform) {
        platforms.put(platform.getId(), platform);
    }

    public void updateTimesForServices() {
        // Cannot do this until after all stops loaded into trips
        logger.info("Updating timings for services");
        services.values().forEach(Service::updateTimings);
    }

    public void addService(Service service) {
        services.put(service.getId(), service);
    }

    public boolean hasTripId(String tripId) {
        return trips.containsKey(tripId);
    }

    @Override
    public Trip getTrip(String tripId) {
        return trips.get(tripId);
    }

    @Override
    public Collection<Route> getRoutes() {
        return routes.values();
    }

    public void addTrip(Trip trip) {
        trips.put(trip.getId(), trip);
    }

    @Override
    public Optional<Platform> getPlatformById(String platformId) {
        if (platforms.containsKey(platformId)) {
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
    public FeedInfo getFeedInfo() {
        return feedInfo;
    }

    @Override
    public Set<Service> getServicesOnDate(TramServiceDate date) {
        return services.values().stream().
                filter(svc -> svc.operatesOn(date.getDate())).collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasRouteId(String routeId) {
        return routes.containsKey(routeId);
    }


}
