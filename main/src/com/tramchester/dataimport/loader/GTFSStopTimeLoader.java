package com.tramchester.dataimport.loader;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.*;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.WriteableTransportData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class GTFSStopTimeLoader {
    private static final Logger logger = LoggerFactory.getLogger(StopDataLoader.class);

    private final WriteableTransportData buildable;
    private final TransportEntityFactory factory;
    private final GTFSSourceConfig dataSourceConfig;

    public GTFSStopTimeLoader(WriteableTransportData buildable, TransportEntityFactory factory, GTFSSourceConfig dataSourceConfig) {
        this.buildable = buildable;
        this.factory = factory;
        this.dataSourceConfig = dataSourceConfig;
    }

    public IdMap<Service> load(Stream<StopTimeData> stopTimes, PreloadedStationsAndPlatforms preloadStations, TripAndServices tripAndServices) {
        String sourceName = dataSourceConfig.getName();

        StopTimeDataLoader stopTimeDataLoader = new StopTimeDataLoader(buildable, preloadStations, factory, dataSourceConfig, tripAndServices);

        logger.info("Loading stop times for " + sourceName);
        stopTimes.
                filter(this::isValid).
                filter(stopTimeData -> tripAndServices.hasId(stopTimeData.getTripId())).
                forEach(stopTimeDataLoader::loadStopTimeData);

        stopTimeDataLoader.close();
        return stopTimeDataLoader.getAddedServices();
    }

    private boolean isValid(StopTimeData stopTimeData) {
        if (stopTimeData.isValid()) {
            return true;
        }
        logger.warn("StopTimeData is invalid: " + stopTimeData);
        return false;
    }

    private static class StopTimeDataLoader {
        private static final Logger logger = LoggerFactory.getLogger(StopTimeDataLoader.class);

        private final IdMap<Service> addedServices;
        private final IdSet<Station> excludedStations;
        private final MissingPlatforms missingPlatforms;
        private final AtomicInteger stopTimesLoaded;

        private final WriteableTransportData buildable;
        private final PreloadedStationsAndPlatforms preloadStations;
        private final TransportEntityFactory factory;
        private final GTFSSourceConfig dataSourceConfig;
        private final TripAndServices tripAndServices;

        public StopTimeDataLoader(WriteableTransportData buildable, PreloadedStationsAndPlatforms preloadStations,
                                  TransportEntityFactory factory, GTFSSourceConfig dataSourceConfig, TripAndServices tripAndServices) {
            this.buildable = buildable;
            this.preloadStations = preloadStations;
            this.factory = factory;
            this.dataSourceConfig = dataSourceConfig;
            this.tripAndServices = tripAndServices;

            addedServices = new IdMap<>();
            excludedStations = new IdSet<>();
            missingPlatforms = new MissingPlatforms();
            stopTimesLoaded = new AtomicInteger();
        }

        public void loadStopTimeData(StopTimeData stopTimeData) {
            final String stopId = stopTimeData.getStopId();
            final IdFor<Station> stationId = factory.formStationId(stopId);
            final IdFor<Trip> stopTripId = Trip.createId(stopTimeData.getTripId());

            if (preloadStations.hasId(stationId)) {
                final MutableTrip trip = tripAndServices.getTrip(stopTripId);
                final Route route = getRouteFrom(trip);
                final MutableStation station = preloadStations.get(stationId);

                final boolean routePlatforms = expectedPlatformsForRoute(route);
                final boolean stationPlatforms = station.hasPlatforms();
                if (routePlatforms && !stationPlatforms) {
                    missingPlatforms.record(stationId, stopTripId);
                } else if (stationPlatforms && !routePlatforms) {
                    logger.error(format("Platform mismatch, Skipping. Station %s has platforms but route %s does not for stop time %s",
                            station.getId(), route.getId(), stopTimeData));
                } else {
                    Service added = addStopTimeData(stopTimeData, trip, station, route);
                    addedServices.add(added);
                    stopTimesLoaded.getAndIncrement();
                }
            } else {
                excludedStations.add(stationId);
                if (tripAndServices.hasId(stopTripId)) {
                    MutableTrip trip = tripAndServices.getTrip(stopTripId);
                    trip.setFiltered(true);
                } else {
                    logger.warn(format("No trip %s for filtered stopcall %s", stopTripId, stationId));
                }
            }
        }

        @NotNull
        private Route getRouteFrom(MutableTrip trip) {
            Route route = trip.getRoute();

            if (route == null) {
                throw new RuntimeException("Null route for " + trip.getId());
            }
            return route;
        }

        private boolean expectedPlatformsForRoute(Route route) {
            return dataSourceConfig.getTransportModesWithPlatforms().contains(route.getTransportMode());
        }

        private Service addStopTimeData(StopTimeData stopTimeData, MutableTrip trip, MutableStation station, Route route) {

            final MutableService service = tripAndServices.getService(trip.getService().getId());

            addStationAndRouteStation(route, station, stopTimeData);
            addPlatformsForStation(station);

            StopCall stopCall = createStopCall(stopTimeData, route, trip, station);

            trip.addStop(stopCall);

            if (!buildable.hasTripId(trip.getId())) {
                buildable.addTrip(trip); // seen at least one stop for this trip
            }

            final MutableRoute mutableRoute = buildable.getMutableRoute(route.getId());
            mutableRoute.addTrip(trip);
            mutableRoute.addService(service);

            buildable.addService(service);

            return service;
        }

        private void addStationAndRouteStation(Route route, MutableStation station, StopTimeData stopTimeData) {

            GTFSPickupDropoffType dropOffType = stopTimeData.getDropOffType();
            if (dropOffType.isDropOff()) {
                station.addRouteDropOff(route);
            }

            GTFSPickupDropoffType pickupType = stopTimeData.getPickupType();
            if (pickupType.isPickup()) {
                station.addRoutePickUp(route);
            }

            IdFor<Station> stationId = station.getId();
            if (!buildable.hasStationId(stationId)) {
                buildable.addStation(station);
                if (!station.getLatLong().isValid()) {
                    logger.warn("Station has invalid position " + station);
                }
            }

            if (!buildable.hasRouteStationId(RouteStation.createId(stationId, route.getId()))) {
                RouteStation routeStation = factory.createRouteStation(station, route);
                buildable.addRouteStation(routeStation);
            }
        }

        private void addPlatformsForStation(Station station) {
            station.getPlatforms().stream().
                    map(HasId::getId).
                    filter(platformId -> !buildable.hasPlatformId(platformId)).
                    map(preloadStations::getPlatform).
                    forEach(buildable::addPlatform);
        }

        private StopCall createStopCall(StopTimeData stopTimeData, Route route, Trip trip, Station station) {
            TransportMode transportMode = route.getTransportMode();

            if (dataSourceConfig.getTransportModesWithPlatforms().contains(transportMode)) {
                String stopId = stopTimeData.getStopId();

                IdFor<Platform> platformId = PlatformId.createId(station.getId(), stopId);

                if (buildable.hasPlatformId(platformId)) {
                    MutablePlatform platform = buildable.getMutablePlatform(platformId);

                    //Service service = trip.getService();

                    if (stopTimeData.getPickupType().isPickup()) {
                        platform.addRoutePickUp(route);
                    }
                    if (stopTimeData.getDropOffType().isDropOff()) {
                        platform.addRouteDropOff(route);
                    }

                    //platform.addRoute(route);

                    return factory.createPlatformStopCall(trip, platform, station, stopTimeData);
                } else {
                    IdFor<Route> routeId = route.getId();
                    logger.error("Missing platform " + platformId + " For transport mode " + transportMode + " and route " + routeId);
                    return factory.createNoPlatformStopCall(trip, station, stopTimeData);
                }
            } else {
                return factory.createNoPlatformStopCall(trip, station, stopTimeData);
            }
        }

        public IdMap<Service> getAddedServices() {
            return addedServices;
        }

        public void close() {
            String sourceName = dataSourceConfig.getName();
            if (!excludedStations.isEmpty()) {
                logger.warn("Excluded the following station ids (flagged out of area) : " + excludedStations + " for " + sourceName);
                excludedStations.clear();
            }
            missingPlatforms.recordInLog(dataSourceConfig);
            missingPlatforms.clear();

            logger.info("Loaded " + stopTimesLoaded.get() + " stop times for " + sourceName);
        }


        private static class MissingPlatforms {
            private final Map<IdFor<Station>, IdSet<Trip>> missingPlatforms;

            private MissingPlatforms() {
                missingPlatforms = new HashMap<>();
            }

            public void record(IdFor<Station> stationId, IdFor<Trip> stopTripId) {
                if (!missingPlatforms.containsKey(stationId)) {
                    missingPlatforms.put(stationId, new IdSet<>());
                }
                missingPlatforms.get(stationId).add(stopTripId);
            }

            public void recordInLog(GTFSSourceConfig gtfsSourceConfig) {
                if (missingPlatforms.isEmpty()) {
                    return;
                }
                missingPlatforms.forEach((stationId, tripIds) -> logger.error(
                        format("Did not find platform for stationId: %s TripId: %s source:'%s'",
                                stationId, tripIds, gtfsSourceConfig.getName())));
            }

            public void clear() {
                missingPlatforms.clear();
            }
        }
    }
}
