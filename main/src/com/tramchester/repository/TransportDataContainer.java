package com.tramchester.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TransportDataContainer implements TransportData, WriteableTransportData {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataContainer.class);

    private final ProvidesNow providesNow;

    private final CompositeIdMap<Trip, MutableTrip> trips; // trip id -> trip
    private final CompositeIdMap<Station, MutableStation> stationsById;  // station id -> station
    private final CompositeIdMap<Service, MutableService> services;  // service id -> service
    private final CompositeIdMap<Route, MutableRoute> routes;  // route id -> route
    private final CompositeIdMap<Platform, MutablePlatform> platforms; // platformId -> platform
    private final IdMap<RouteStation> routeStations; // routeStationId - > RouteStation
    private final CompositeIdMap<Agency, MutableAgency> agencies; // agencyId -> agencies
    private final Set<DataSourceInfo> dataSourceInfos;

    // data source name -> feedinfo (if present)
    private final Map<DataSourceID, FeedInfo> feedInfoMap;
    private final String sourceName;

    /**
     * Not container managed due to test life cycle
     */
    public TransportDataContainer(ProvidesNow providesNow, String sourceName) {
        logger.info("Created for sourcename: " + sourceName);
        this.providesNow = providesNow;
        this.sourceName = sourceName;

        trips = new CompositeIdMap<>();
        stationsById = new CompositeIdMap<>();
        services = new CompositeIdMap<>();
        routes = new CompositeIdMap<>();
        platforms = new CompositeIdMap<>();
        routeStations = new IdMap<>();
        agencies = new CompositeIdMap<>();
        dataSourceInfos = new HashSet<>();
        feedInfoMap = new HashMap<>();

    }

    @Override
    public void dispose() {
        logger.info("stopping for " + sourceName);
        // clear's are here due to memory usage during testing
        trips.forEach(MutableTrip::dispose);
        trips.clear();
        stationsById.clear();
        services.clear();
        routes.clear();
        platforms.clear();
        routeStations.clear();
        agencies.clear();
        feedInfoMap.clear();
        logger.info("stopped");
    }

    @Override
    public void reportNumbers() {
        logger.info("From " + dataSourceInfos + " name:" + sourceName);
        logger.info(format("%s agencies", agencies.size()));
        logger.info(format("%s routes", routes.size()));
        logger.info(stationsById.size() + " stations " + platforms.size() + " platforms ");
        logger.info(format("%s route stations", routeStations.size()));
        logger.info(format("%s services", services.size()));
        logger.info(format("%s trips", trips.size()));
        logger.info(format("%s calling points", countStopCalls(trips)));
        logger.info(format("%s feedinfos", feedInfoMap.size()));
    }

    private int countStopCalls(CompositeIdMap<Trip,MutableTrip> trips) {
        Optional<Integer> count = trips.getValues().stream().
                map(trip -> trip.getStopCalls().numberOfCallingPoints()).
                reduce(Integer::sum);
        return count.orElse(Integer.MIN_VALUE);
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
        return Collections.unmodifiableSet(stationsById.getValues());
    }

    @Override
    public Stream<Station> getStationStream() {
        return stationsById.getValuesStream();
    }

    @Override
    public Set<Station> getStationsForMode(TransportMode mode) {
        return getStationsForModeStream(mode).collect(Collectors.toUnmodifiableSet());
    }

    private Stream<Station> getStationsForModeStream(TransportMode mode) {
        return stationsById.filterStream(item -> item.serves(mode));
    }

    @Override
    public int getNumberOfStations() {
        return stationsById.size();
    }

    @Override
    public Set<RouteStation> getRouteStations() {
        return Collections.unmodifiableSet(routeStations.getValues());
    }

    @Override
    public Set<RouteStation> getRouteStationsFor(IdFor<Station> stationId) {
        final Set<RouteStation> result = routeStations.getValuesStream().
                filter(routeStation -> routeStation.getStation().getId().equals(stationId)).
                collect(Collectors.toUnmodifiableSet());
        if (result.isEmpty()) {
            logger.warn("Found no route stations for " + stationId);
        }
        return result;
    }

    @Override
    public Stream<Station> getStationsFromSource(DataSourceID dataSourceID) {
        return this.stationsById.filterStream(station -> station.getDataSourceID()==dataSourceID);
    }

    @Override
    public RouteStation getRouteStationById(IdFor<RouteStation> routeStationId) {
        return routeStations.get(routeStationId);
    }

    @Override
    public RouteStation getRouteStation(Station station, Route route) {
        return getRouteStationById(RouteStation.createId(station.getId(), route.getId()));
    }

    @Override
    public Set<Service> getServices() {
        return Collections.unmodifiableSet(services.getValues());
    }

    @Override
    public Service getServiceById(IdFor<Service> serviceId) {
        return services.get(serviceId);
    }

    @Override
    public Set<Trip> getTrips() {
        return trips.getSuperValues();
    }

    @Override
    public boolean hasRouteStationId(IdFor<RouteStation> routeStationId) {
        return routeStations.hasId(routeStationId);
    }

    @Override
    public void addRouteStation(RouteStation routeStation) {
       routeStations.add(routeStation);
    }

    public boolean hasPlatformId(IdFor<Platform> platformId) {
        return platforms.hasId(platformId);
    }

    public Platform getPlatform(IdFor<Platform> platformId) {
        return platforms.get(platformId);
    }

    public MutablePlatform getMutablePlatform(IdFor<Platform> platformId) {
        return platforms.get(platformId);
    }

    @Override
    public Set<Platform> getPlatforms() {
        return Collections.unmodifiableSet(platforms.getValues());
    }

    @Override
    public Route getRouteById(IdFor<Route> routeId) {
        return routes.get(routeId);
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public Set<Agency> getAgencies() {
        return Collections.unmodifiableSet(agencies.getValues());
    }

    @Override
    public MutableService getMutableService(IdFor<Service> serviceId) {
        // logging into callers
        if (!services.hasId(serviceId)) {
            logger.debug("No such service " + serviceId);
        }
        return services.get(serviceId);
    }

    @Override
    public MutableRoute getMutableRoute(IdFor<Route> id) {
        return routes.get(id);
    }

    @Override
    public MutableAgency getMutableAgency(IdFor<Agency> agencyId) {
        return agencies.get(agencyId);
    }

    @Override
    public MutableTrip getMutableTrip(IdFor<Trip> tripId) {
        return trips.get(tripId);
    }

    @Override
    public void removeStations(Set<Station> toRemove) {
        IdSet<Station> ids = toRemove.stream().collect(IdSet.collector());
        stationsById.remove(ids);

    }

    @Override
    public Set<Service> getServicesWithoutCalendar() {
        return services.getValues().stream().filter(service -> !service.hasCalendar()).collect(Collectors.toSet());
    }

    @Override
    public IdSet<Service> getServicesWithZeroDays() {
        IdSet<Service> noDayServices = new IdSet<>();
        services.getValues().stream().filter(MutableService::hasCalendar).forEach(service -> {
                    ServiceCalendar calendar = service.getCalendar();
                    if (calendar.operatesNoDays()) {
                        // feedvalidator flags these as warnings also
                        noDayServices.add(service.getId());
                    }
                }
        );
        return noDayServices;
    }

    @Override
    public MutableStation getMutableStation(IdFor<Station> stationId) {
        return stationsById.get(stationId);
    }

    @Override
    public Set<DataSourceInfo> getDataSourceInfo() {
        return Collections.unmodifiableSet(dataSourceInfos);
    }

    @Override
    public LocalDateTime getNewestModTimeFor(TransportMode mode) {
        Optional<LocalDateTime> result = this.dataSourceInfos.stream().
                filter(info -> info.getModes().contains(mode)).
                map(DataSourceInfo::getLastModTime).max(Comparator.naturalOrder());
        if (result.isEmpty()) {
            logger.error("Cannot find latest mod time for transport mode " + mode);
            return providesNow.getDateTime();
        } else {
            LocalDateTime localDateTime = result.get();
            logger.info("Newest mode time for " + mode.name() + " is " + localDateTime);
            return localDateTime;
        }
    }

    @Override
    public boolean hasServiceId(IdFor<Service> serviceId) {
        return services.hasId(serviceId);
    }

    @Override
    public void addAgency(MutableAgency agency) {
        agencies.add(agency);
    }

    @Override
    public boolean hasAgencyId(IdFor<Agency> agencyId) {
        return agencies.hasId(agencyId);
    }

    @Override
    public void addRoute(MutableRoute route) {
        routes.add(route);
    }

    @Override
    public void addStation(MutableStation station) {
        stationsById.add(station);
    }

    @Override
    public void addPlatform(MutablePlatform platform) {
        platforms.add(platform);
    }

    @Override
    public void addService(MutableService service) {
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
        return routes.getSuperValues();
    }

    @Override
    public void addTrip(MutableTrip trip) {
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
    public Set<Service> getServicesOnDate(LocalDate date) {
        return services.filterStream(item -> item.getCalendar().operatesOn(date)).
                collect(Collectors.toSet());
    }

    @Override
    public Set<Route> getRoutesRunningOn(LocalDate date) {
        return routes.filterStream(route -> route.isAvailableOn(date)).collect(Collectors.toSet());
    }

    public boolean hasRouteId(IdFor<Route> routeId) {
        return routes.hasId(routeId);
    }

    @Override
    public int numberOfRoutes() {
        return routes.size();
    }

    @Override
    public Set<Route> findRoutesByShortName(IdFor<Agency> agencyId, String shortName) {
        return routes.getValues().stream().
                filter(route -> route.getAgency().getId().equals(agencyId)).
                filter(route -> route.getShortName().equals(shortName)).
                collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Route> findRoutesByName(IdFor<Agency> agencyId, String longName) {
        return routes.getValues().stream().
                filter(route -> route.getAgency().getId().equals(agencyId)).
                filter(route -> route.getName().equals(longName)).
                collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void addDataSourceInfo(DataSourceInfo dataSourceInfo) {
        dataSourceInfos.add(dataSourceInfo);
    }

    @Override
    public Map<DataSourceID, FeedInfo> getFeedInfos() {
        return feedInfoMap;
    }

    @Override
    public void addFeedInfo(DataSourceID name, FeedInfo feedInfo) {
        logger.info("Added " + feedInfo.toString());
        feedInfoMap.put(name, feedInfo);
    }

    @Override
    public String toString() {
        return "TransportDataContainer{" +
                "providesNow=" + providesNow +
                ",\n trips=" + trips +
                ",\n stationsById=" + stationsById +
                ",\n services=" + services +
                ",\n routes=" + routes +
                ",\n platforms=" + platforms +
                ",\n routeStations=" + routeStations +
                ",\n agencies=" + agencies +
                ",\n dataSourceInfos=" + dataSourceInfos +
                ",\n feedInfoMap=" + feedInfoMap +
                ",\n sourceName='" + sourceName + '\'' +
                '}';
    }

}
