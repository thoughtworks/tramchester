package com.tramchester.repository;

import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

import static java.lang.String.format;

public class TransportDataContainer implements TransportData {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataContainer.class);

    private final ProvidesNow providesNow;

    private final IdMap<Trip> trips = new IdMap<>();        // trip id -> trip
    private final IdMap<Station> stationsById = new IdMap<>();  // station id -> station
    private final IdMap<Service> services = new IdMap<>();  // service id -> service
    private final IdMap<Route> routes = new IdMap<>();      // route id -> route
    private final IdMap<Platform> platforms = new IdMap<>(); // platformId -> platform
    private final IdMap<RouteStation> routeStations = new IdMap<>(); // routeStationId - > RouteStation
    private final IdMap<Agency> agencies = new IdMap<>(); // agencyId -> agencies

    private final Map<String, Station> tramStationsByName = new HashMap<>();  // tram station name -> station
    private final Set<DataSourceInfo> dataSourceInfos = new HashSet<>();

    // data source name -> feedinfo (if present)
    private final Map<DataSourceID, FeedInfo> feedInfoMap = new HashMap<>();
    private final String sourceName;

    /**
     * Not container managed due to test life cycle
     */
    public TransportDataContainer(ProvidesNow providesNow, String sourceName) {
        logger.info("Created for " + sourceName);
        this.providesNow = providesNow;
        this.sourceName = sourceName;
    }

    public void dispose() {
        logger.info("Stopping");
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
        logger.info("stopped");
    }

    public void reportNumbers() {
        logger.info("From " + dataSourceInfos.toString());
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
    public Set<Station> getStationsForMode(TransportMode mode) {
        return stationsById.filter(item -> item.getTransportModes().contains(mode));
    }

    @Override
    public int getNumberOfStations() {
        return stationsById.size();
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
    public RouteStation getRouteStation(Station station, Route route) {
        return getRouteStationById(RouteStation.createId(station.getId(), route.getId()));
    }

    @Override
    public Set<Service> getServices() {
        return services.getValues();
    }

    @Override
    public Set<Trip> getTrips() {
        return trips.getValues();
    }

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
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public Set<Agency> getAgencies() {
        return agencies.getValues();
    }

    @Override
    public Service getServiceById(IdFor<Service> serviceId) {
        if (!services.hasId(serviceId)) {
            logger.warn("No such service " + serviceId);
        }
        return services.get(serviceId);
    }

    @Override
    public Set<DataSourceInfo> getDataSourceInfo() {
        return dataSourceInfos;
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

    public void addAgency(Agency agency) {
        logger.info("Added " + agency.getId());
        agencies.add(agency);
    }

    public boolean hasAgency(IdFor<Agency> agencyId) {
        return agencies.hasId(agencyId);
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

    @Deprecated
    public void addRouteToAgency(Agency agency, Route route) {
        agency.addRoute(route);
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
        return services.filter(item -> item.getCalendar().operatesOn(date.getDate()));
    }

    public boolean hasRouteId(IdFor<Route> routeId) {
        return routes.hasId(routeId);
    }

    public void addDataSourceInfo(DataSourceInfo dataSourceInfo) {
        dataSourceInfos.add(dataSourceInfo);
    }

    @Override
    public Map<DataSourceID, FeedInfo> getFeedInfos() {
        return feedInfoMap;
    }

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
                ",\n tramStationsByName=" + tramStationsByName +
                ",\n dataSourceInfos=" + dataSourceInfos +
                ",\n feedInfoMap=" + feedInfoMap +
                ",\n sourceName='" + sourceName + '\'' +
                '}';
    }

}
