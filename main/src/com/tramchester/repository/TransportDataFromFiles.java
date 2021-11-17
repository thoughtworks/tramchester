package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.*;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class TransportDataFromFiles implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFiles.class);

    private final TransportDataStreams transportDataStreams;
    private final TramchesterConfig config;
    private final ProvidesNow providesNow;

    private final TransportDataContainer dataContainer;

    // NOTE: cannot inject GraphFilter here as circular dependency on being able to find routes which
    // needs transport data to be loaded....
    @Inject
    public TransportDataFromFiles(TransportDataStreams transportDataStreams,
                                  TramchesterConfig config, ProvidesNow providesNow) {
        this.transportDataStreams = transportDataStreams;
        this.config = config;
        this.providesNow = providesNow;
        dataContainer = new TransportDataContainer(providesNow, "TransportDataFromFiles");
    }

    @PreDestroy
    public void stop() {
        logger.info("stopping");
        dataContainer.dispose();
        logger.info("stopped");
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        transportDataStreams.forEach(transportDataStream -> load(transportDataStream, dataContainer));
        logger.info("started");
    }

    public TransportData getData() {
        return dataContainer;
    }

    private void load(TransportDataSource dataSource, WriteableTransportData buildable) {
        DataSourceInfo dataSourceInfo = dataSource.getDataSourceInfo();

        DataSourceID sourceName = dataSourceInfo.getID();
        GTFSSourceConfig sourceConfig = dataSource.getConfig();

        logger.info("Loading data for " + sourceName);

        if(sourceConfig.getHasFeedInfo()) {
            // replace version string (which is from mod time) with the one from the feedinfo file, if present
            Optional<FeedInfo> maybeFeedinfo = dataSource.getFeedInfoStream().findFirst();
            maybeFeedinfo.ifPresent(feedInfo -> {
                logger.info("Updating data source info from " + feedInfo);
                buildable.addDataSourceInfo(new DataSourceInfo(sourceName, feedInfo.getVersion(),
                        dataSourceInfo.getLastModTime(), dataSource.getDataSourceInfo().getModes()));
                buildable.addFeedInfo(sourceName, feedInfo);
            });

        } else {
            logger.warn("No feedinfo for " + sourceName);
            buildable.addDataSourceInfo(dataSourceInfo);
        }

        TransportEntityFactory entityFactory = dataSource.getEntityFactory();

        PreloadedStationsAndPlatforms allStations = preLoadStations(dataSource.stops, entityFactory);

        CompositeIdMap<Agency, MutableAgency> allAgencies = preloadAgencys(sourceName, dataSource.agencies, entityFactory);
        ExcludedRoutes excludedRoutes = populateRoutes(buildable, dataSource.routes, allAgencies,
                sourceConfig, entityFactory);
        logger.info("Excluding " + excludedRoutes.numOfExcluded() + " routes ");

        allAgencies.clear();

        TripAndServices tripsAndServices = loadTripsAndServices(buildable, dataSource.trips, excludedRoutes,
                entityFactory);

        IdMap<Service> services = populateStopTimes(buildable, dataSource.stopTimes, allStations,
                tripsAndServices, entityFactory,
                sourceConfig);

        excludedRoutes.clear();

        allStations.clear();

        populateCalendars(buildable, dataSource.calendars, dataSource.calendarsDates, services, sourceConfig, entityFactory);

        tripsAndServices.clear();

        buildable.reportNumbers();

        // update svcs where calendar data is missing

        //buildable.getServices().stream().
        buildable.getServicesWithoutCalendar().
                forEach(svc -> logger.warn(format("source %s Service %s has missing calendar", sourceName, svc.getId()))
        );

        reportZeroDaysServices(buildable);

        dataSource.closeAll();

        logger.info("Finishing Loading data for " + sourceName);
    }

    private void reportZeroDaysServices(WriteableTransportData buildable) {
        IdSet<Service> noDayServices = buildable.getServicesWithZeroDays();
        if (!noDayServices.isEmpty()) {
            logger.warn("The following services do no operate on any days per calendar.txt file " + noDayServices);
        }
    }

    private void populateCalendars(WriteableTransportData buildable, Stream<CalendarData> calendars,
                                   Stream<CalendarDateData> calendarsDates, IdMap<Service> services, GTFSSourceConfig config,
                                   TransportEntityFactory factory) {
        AtomicInteger countCalendars = new AtomicInteger(0);
        logger.info("Loading calendars for " + services.size() +" services ");

        IdSet<Service> missingCalendar = services.getIds();
        calendars.forEach(calendarData -> {
            IdFor<Service> serviceId = calendarData.getServiceId();
            MutableService service = buildable.getServiceById(serviceId);

            if (service != null) {
                countCalendars.getAndIncrement();
                missingCalendar.remove(serviceId);
                ServiceCalendar serviceCalendar = factory.createServiceCalendar(calendarData);
                service.setCalendar(serviceCalendar);
            } else {
                // legit, we filter services based on the route transport mode
                logger.debug("Unable to find service " + serviceId + " while populating Calendar");
            }
        });

        if (!missingCalendar.isEmpty()) {
            logger.warn("Failed to match service id " + missingCalendar + " for calendar");
        }
        logger.info("Loaded " + countCalendars.get() + " calendar entries");

        updateServiceDatesFromCalendarDates(buildable, calendarsDates, services, config.getNoServices());
    }

    private void updateServiceDatesFromCalendarDates(WriteableTransportData buildable, Stream<CalendarDateData> calendarsDates,
                                                     IdMap<Service> services, Set<LocalDate> noServices) {
        logger.info("Loading calendar dates "+ services.size() +" services with no services on " + noServices);
        IdSet<Service> missingCalendarDates = services.getIds();
        AtomicInteger countCalendarDates = new AtomicInteger(0);

        calendarsDates.forEach(date -> {
            IdFor<Service> serviceId = date.getServiceId();
            MutableService service = buildable.getServiceById(serviceId);
            if (service != null) {
                if (service.hasCalendar()) {
                    countCalendarDates.getAndIncrement();
                    missingCalendarDates.remove(serviceId);
                    addException(date, service.getCalendar(), serviceId, noServices);
                } else {
                    // TODO Create a one off entry? Auto populate based on all exceptions? i.e. days of week
                    logger.error("Missing calendar for service " + service.getId() + " so could not add " + date);
                }
            } else  {
                // legit, we filter services based on the route transport mode
                logger.debug("Unable to find service " + serviceId + " while populating CalendarDates");
            }
        });
        if (!missingCalendarDates.isEmpty()) {
            logger.info("calendar_dates: Failed to find service id " + missingCalendarDates);
        }
        addNoServicesDatesToAllCalendars(services, noServices, missingCalendarDates);

        logger.info("Loaded " + countCalendarDates.get() + " calendar date entries");
    }

    private void addNoServicesDatesToAllCalendars(IdMap<Service> services, Set<LocalDate> noServices, IdSet<Service> missingCalendarDates) {
        if (noServices.isEmpty()) {
            return;
        }
        logger.warn("Adding no service dates from source config " + noServices);
        services.forEach(service -> {
            if (!missingCalendarDates.contains(service.getId())) {
                noServices.forEach(noServiceDate -> service.getCalendar().excludeDate(noServiceDate));
            }
        });
        logger.info("Added service dates");

    }

    private void addException(CalendarDateData date, ServiceCalendar calendar, IdFor<Service> serviceId, Set<LocalDate> noServices) {
        int exceptionType = date.getExceptionType();
        LocalDate exceptionDate = date.getDate();
        if (exceptionType == CalendarDateData.ADDED) {
            if (!noServices.contains(exceptionDate)) {
                calendar.includeExtraDate(exceptionDate);
            } else {
                logDateIssue(exceptionDate, noServices);
            }
        } else if (exceptionType == CalendarDateData.REMOVED) {
            calendar.excludeDate(exceptionDate);
        } else {
            logger.warn("Unexpected exception type " + exceptionType + " for service " + serviceId + " and date " + exceptionDate);
        }
    }

    private void logDateIssue(LocalDate exceptionDate, Set<LocalDate> noServices) {
        LocalDate currentDate = providesNow.getDate();
        String msg = format("Ignoring extra date %s as configured as no service date from %s", exceptionDate, noServices);
        if (currentDate.isAfter(exceptionDate)) {
            logger.debug(msg);
        } else {
            logger.warn(msg);
        }
    }

    private IdMap<Service> populateStopTimes(WriteableTransportData buildable, Stream<StopTimeData> stopTimes,
                                             PreloadedStationsAndPlatforms preloadStations, TripAndServices tripAndServices,
                                             TransportEntityFactory factory, GTFSSourceConfig dataSourceConfig) {
        String sourceName = dataSourceConfig.getName();

        StopTimeDataLoader stopTimeDataLoader = new StopTimeDataLoader(buildable, preloadStations, factory, dataSourceConfig, tripAndServices);

        logger.info("Loading stop times for " + sourceName);
        stopTimes.
                filter(stopTimeData -> tripAndServices.hasId(stopTimeData.getTripId())).
                filter(this::isValid).
                forEach(stopTimeDataLoader::load);

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

    private TripAndServices loadTripsAndServices(WriteableTransportData buildable, Stream<TripData> tripDataStream,
                                                 ExcludedRoutes excludedRoutes,
                                                 TransportEntityFactory factory) {
        logger.info("Loading trips");
        TripAndServices results = new TripAndServices(factory);

        AtomicInteger count = new AtomicInteger();

        tripDataStream.forEach((tripData) -> {
            IdFor<Service> serviceId = tripData.getServiceId();
            IdFor<Route> routeId = factory.createRouteId(tripData.getRouteId());
            IdFor<Trip> tripId = tripData.getTripId();

            if (buildable.hasRouteId(routeId)) {
                Route route = buildable.getMutableRoute(routeId);
                Service service = results.getOrCreateService(serviceId);
                results.createTripIfMissing(tripId, tripData, service, route);
                count.getAndIncrement();
            } else {
                if (!excludedRoutes.wasExcluded(routeId)) {
                    logger.warn(format("Unable to find RouteId '%s' for trip '%s", routeId, tripData));
                }
            }
        });
        logger.info("Loaded " + count.get() + " trips");
        return results;
    }

    private CompositeIdMap<Agency, MutableAgency> preloadAgencys(DataSourceID dataSourceID, Stream<AgencyData> agencyDataStream,
                                                                 TransportEntityFactory factory) {
        logger.info("Loading all agencies for " + dataSourceID);
        CompositeIdMap<Agency, MutableAgency> agencies = new CompositeIdMap<>();
        agencyDataStream.forEach(agencyData -> agencies.add(factory.createAgency(dataSourceID, agencyData)));
        logger.info("Loaded " + agencies.size() + " agencies for " + dataSourceID);
        return agencies;
    }

    private ExcludedRoutes populateRoutes(WriteableTransportData buildable, Stream<RouteData> routeDataStream,
                                          CompositeIdMap<Agency, MutableAgency> allAgencies, GTFSSourceConfig sourceConfig,
                                          TransportEntityFactory factory) {
        Set<GTFSTransportationType> transportModes = sourceConfig.getTransportGTFSModes();
        AtomicInteger count = new AtomicInteger();

        ExcludedRoutes excludedRoutes = new ExcludedRoutes();

        logger.info("Loading routes for transport modes " + transportModes.toString());
        routeDataStream.forEach(routeData -> {
            IdFor<Agency> agencyId = routeData.getAgencyId();
            boolean missingAgency = !allAgencies.hasId(agencyId);
            if (missingAgency) {
                logger.error("Missing agency " + agencyId);
            }

            GTFSTransportationType routeType = factory.getRouteType(routeData, agencyId);

            if (transportModes.contains(routeType)) {
                DataSourceID dataSourceID = sourceConfig.getDataSourceId();
                MutableAgency agency = missingAgency ? createMissingAgency(dataSourceID, allAgencies, agencyId, factory)
                        : allAgencies.get(agencyId);

                MutableRoute route = factory.createRoute(routeType, routeData, agency);

                agency.addRoute(route);
                if (!buildable.hasAgencyId(agencyId)) {
                    buildable.addAgency(agency);
                }
                buildable.addRoute(route);

                count.getAndIncrement();

            } else {
                IdFor<Route> routeId = routeData.getId();
                excludedRoutes.excludeRoute(factory.createRouteId(routeId));
            }
        });
        excludedRoutes.recordInLog(transportModes);
        logger.info("Loaded " + count.get() + " routes of transport types " + transportModes + " excluded "+ excludedRoutes.numOfExcluded());
        return excludedRoutes;
    }



    private MutableAgency createMissingAgency(DataSourceID dataSourceID, CompositeIdMap<Agency, MutableAgency> allAgencies, IdFor<Agency> agencyId,
                                              TransportEntityFactory factory) {
        MutableAgency unknown = factory.createUnknownAgency(dataSourceID, agencyId);
        logger.error("Created agency" + unknown + " for " + dataSourceID);
        allAgencies.add(unknown);
        return unknown;
    }

    private PreloadedStationsAndPlatforms preLoadStations(Stream<StopData> stops, TransportEntityFactory factory) {
        logger.info("Loading stops within bounds");
        BoundingBox bounds = config.getBounds();

        PreloadedStationsAndPlatforms allStations = new PreloadedStationsAndPlatforms(factory);

        stops.forEach((stopData) -> {
            LatLong latLong = stopData.getLatLong();
            if (latLong.isValid()) {
                GridPosition position = getGridPosition(stopData.getLatLong());
                if (bounds.contained(position)) {
                    preLoadStation(allStations, stopData, position, factory);
                } else {
                    // Don't know which transport modes the station serves at this stage, so no way to filter further
                    logger.info("Excluding stop outside of bounds" + stopData);
                }
            } else {
                preLoadStation(allStations, stopData, GridPosition.Invalid, factory);
            }
        });
        logger.info("Pre Loaded " + allStations.size() + " stations");
        return allStations;
    }

    private void preLoadStation(PreloadedStationsAndPlatforms allStations, StopData stopData, GridPosition position,
                                TransportEntityFactory factory) {
        String stopId = stopData.getId();
        IdFor<Station> stationId = factory.formStationId(stopId);

        if (allStations.hasId(stationId)) {
            allStations.updateStation(stationId, stopData);
        } else {
            allStations.createAndAdd(stationId, stopData, position);
        }
    }

    private GridPosition getGridPosition(LatLong latLong) {
        return CoordinateTransforms.getGridPosition(latLong);
    }

    private static class TripAndServices  {
        private final CompositeIdMap<Service, MutableService> services;
        private final CompositeIdMap<Trip,MutableTrip> trips;
        private final TransportEntityFactory factory;

        public TripAndServices(TransportEntityFactory factory) {
            this.factory = factory;
            services = new CompositeIdMap<>();
            trips = new CompositeIdMap<>();
        }

        public void clear() {
            services.clear();
            trips.clear();
        }

        public boolean hasId(IdFor<Trip> id) {
            return trips.hasId(id);
        }

        public MutableTrip getTrip(IdFor<Trip> id) {
            return trips.get(id);
        }

        public MutableService getService(IdFor<Service> id) {
            return services.get(id);
        }

        public MutableService getOrCreateService(IdFor<Service> serviceId) {
            return services.getOrAdd(serviceId, () -> factory.createService(serviceId));
        }

        public void createTripIfMissing(IdFor<Trip> tripId, TripData tripData, Service service, Route route) {
            trips.getOrAdd(tripId, () -> factory.createTrip(tripData, service, route));
        }
    }

    private static class StopTimeDataLoader {
        private final IdMap<Service> addedServices;
        private final IdSet<Station> excludedStations;
        private final MissingPlatforms missingPlatforms;
        private final WriteableTransportData buildable;
        private final PreloadedStationsAndPlatforms preloadStations;
        private final TransportEntityFactory factory;
        private final GTFSSourceConfig dataSourceConfig;
        private final TripAndServices tripAndServices;
        private final AtomicInteger stopTimesLoaded;

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

        public void load(StopTimeData stopTimeData) {
            final String stopId = stopTimeData.getStopId();
            final IdFor<Station> stationId = factory.formStationId(stopId);
            final IdFor<Trip> stopTripId = stopTimeData.getTripId();

            if (preloadStations.hasId(stationId)) {
                final MutableTrip trip = tripAndServices.getTrip(stopTripId);
                final Route route = getRouteFrom(trip);
                final MutableStation station = preloadStations.get(stationId);

                if (expectedPlatformsForRoute(route) && !station.hasPlatforms()) {
                    missingPlatforms.record(stationId, stopTripId);
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
            addStationAndRouteStation(route, station);
            addPlatformsForStation(station);

            StopCall stopCall = createStopCall(stopTimeData, route, trip, station);

            trip.addStop(stopCall);

            if (!buildable.hasTripId(trip.getId())) {
                buildable.addTrip(trip); // seen at least one stop for this trip
            }

            final MutableService service = tripAndServices.getService(trip.getService().getId());

            final MutableRoute mutableRoute = buildable.getMutableRoute(route.getId());
            mutableRoute.addTrip(trip);
            mutableRoute.addService(service);

            buildable.addService(service);

            return service;
        }

        private void addStationAndRouteStation(Route route, MutableStation station) {
            station.addRoute(route);

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
            IdFor<Platform> platformId = stopTimeData.getPlatformId();
            TransportMode transportMode = route.getTransportMode();

            if (dataSourceConfig.getTransportModesWithPlatforms().contains(transportMode)) {
                if (buildable.hasPlatformId(platformId)) {
                    MutablePlatform platform = buildable.getMutablePlatform(platformId);
                    platform.addRoute(route);
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
    }

    private static class ExcludedRoutes {
        private final IdSet<Route> excludedRouteIds;

        private ExcludedRoutes() {
            excludedRouteIds = new IdSet<>();
        }

        public void excludeRoute(IdFor<Route> routeId) {
            excludedRouteIds.add(routeId);
        }

        public boolean wasExcluded(IdFor<Route> routeId) {
            return excludedRouteIds.contains(routeId);
        }

        public IdSet<Route> getExcluded() {
            return excludedRouteIds;
        }

        public int numOfExcluded() {
            return excludedRouteIds.size();
        }

        public void clear() {
            excludedRouteIds.clear();
        }

        public void recordInLog(Set<GTFSTransportationType> transportModes) {
            if (excludedRouteIds.isEmpty()) {
                return;
            }
            logger.info("Excluded the following route id's as did not match modes " + transportModes + " routes: " + excludedRouteIds);
        }
    }

    private static class PreloadedStationsAndPlatforms {
        private final CompositeIdMap<Station, MutableStation> stations;
        private final CompositeIdMap<Platform, MutablePlatform> platforms;
        private final TransportEntityFactory factory;

        private PreloadedStationsAndPlatforms(TransportEntityFactory factory) {
            stations = new CompositeIdMap<>();
            platforms = new CompositeIdMap<>();
            this.factory = factory;
        }

        public boolean hasId(IdFor<Station> stationId) {
            return stations.hasId(stationId);
        }

        public int size() {
            return stations.size();
        }

        public void clear() {
            stations.clear();
        }

        public MutableStation get(IdFor<Station> stationId) {
            return stations.get(stationId);
        }

        public void createAndAdd(IdFor<Station> stationId, StopData stopData, GridPosition position) {
            MutableStation mutableStation = factory.createStation(stationId, stopData, position);

            Optional<MutablePlatform> possiblePlatform = factory.maybeCreatePlatform(stopData);
            possiblePlatform.ifPresent(platform-> {
                platforms.add(platform);
                mutableStation.addPlatform(platform);
            });

            stations.add(mutableStation);
        }

        public void updateStation(IdFor<Station> stationId, StopData stopData) {
            Optional<MutablePlatform> possiblePlatform = factory.maybeCreatePlatform(stopData);
            possiblePlatform.ifPresent(platform-> {
                platforms.add(platform);
                stations.get(stationId).addPlatform(platform);
            });
        }

        public MutablePlatform getPlatform(IdFor<Platform> id) {
            return platforms.get(id);
        }
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
