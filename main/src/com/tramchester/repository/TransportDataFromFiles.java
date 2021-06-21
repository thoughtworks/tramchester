package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
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

    private void load(TransportDataSource dataSource, TransportDataContainer buildable) {
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

        IdMap<Station> allStations = preLoadStations(dataSource.stops, entityFactory);

        IdMap<Agency> allAgencies = preloadAgencys(sourceName, dataSource.agencies, entityFactory);
        IdSet<Route> excludedRoutes = populateRoutes(buildable, dataSource.routes, allAgencies, allStations, sourceConfig, entityFactory);
        logger.info("Excluding " + excludedRoutes.size()+" routes ");

        allAgencies.clear();

        TripAndServices tripsAndServices = loadTripsAndServices(buildable, dataSource.trips, excludedRoutes, entityFactory);

        excludedRoutes.clear();

        IdMap<Service> services = populateStopTimes(buildable, dataSource.stopTimes, allStations, tripsAndServices.trips, entityFactory,
                sourceConfig);

        allStations.clear();

        populateCalendars(buildable, dataSource.calendars, dataSource.calendarsDates, services, sourceConfig, entityFactory);

        tripsAndServices.clear();

        buildable.reportNumbers();

        // update svcs where calendar data is missing
        buildable.getServices().stream().filter(service -> !service.hasCalendar()).forEach(
                svc -> logger.warn(format("source %s Service %s has missing calendar", sourceName, svc.getId()))
        );

        reportZeroDaysServices(buildable);

        dataSource.closeAll();

        logger.info("Finishing Loading data for " + sourceName);
    }

    private void reportZeroDaysServices(TransportDataContainer buildable) {
        IdSet<Service> noDayServices = new IdSet<>();
        buildable.getServices().stream().filter(Service::hasCalendar).forEach(service -> {
            ServiceCalendar calendar = service.getCalendar();
            if (calendar.operatesNoDays()) {
                // feedvalidator flags these as warnings also
                noDayServices.add(service.getId());
                }
            }
        );
        if (!noDayServices.isEmpty()) {
            logger.warn("The following services do no operate on any days per calendar.txt file " + noDayServices);
        }
    }

    private void populateCalendars(TransportDataContainer buildable, Stream<CalendarData> calendars,
                                   Stream<CalendarDateData> calendarsDates, IdMap<Service> services, GTFSSourceConfig config,
                                   TransportEntityFactory factory) {
        AtomicInteger countCalendars = new AtomicInteger(0);
        logger.info("Loading calendars for " + services.size() +" services ");

        IdSet<Service> missingCalendar = services.getIds();
        calendars.forEach(calendarData -> {
            IdFor<Service> serviceId = calendarData.getServiceId();
            Service service = buildable.getService(serviceId);

            if (service != null) {
                countCalendars.getAndIncrement();
                missingCalendar.remove(serviceId);
                ServiceCalendar serviceCalendar = factory.createServiceCalendar(calendarData);
                service.setCalendar(serviceCalendar);
            }
        });
        if (!missingCalendar.isEmpty()) {
            logger.warn("Failed to match service id " + missingCalendar + " for calendar");
        }
        logger.info("Loaded " + countCalendars.get() + " calendar entries");

        updateServiceDatesFromCalendarDates(buildable, calendarsDates, services, config.getNoServices());
    }

    private void updateServiceDatesFromCalendarDates(TransportDataContainer buildable, Stream<CalendarDateData> calendarsDates,
                                                     IdMap<Service> services, Set<LocalDate> noServices) {
        logger.info("Loading calendar dates "+ services.size() +" services with no services on " + noServices);
        IdSet<Service> missingCalendarDates = services.getIds();
        AtomicInteger countCalendarDates = new AtomicInteger(0);

        calendarsDates.forEach(date -> {
            IdFor<Service> serviceId = date.getServiceId();
            Service service = buildable.getService(serviceId);
            if (service != null) {
                if (service.hasCalendar()) {
                    countCalendarDates.getAndIncrement();
                    missingCalendarDates.remove(serviceId);
                    addException(date, service.getCalendar(), serviceId, noServices);
                } else {
                    // TODO Create a one off entry? Auto populate based on all exceptions? i.e. days of week
                    logger.error("Missing calendar for service " + service.getId() + " so could not add " + date);
                }
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

    private IdMap<Service> populateStopTimes(TransportDataContainer buildable, Stream<StopTimeData> stopTimes,
                                             IdMap<Station> preloadStations, IdMap<Trip> trips,
                                             TransportEntityFactory factory, GTFSSourceConfig dataSourceConfig) {
        String sourceName = dataSourceConfig.getName();
        logger.info("Loading stop times for " + sourceName);
        IdMap<Service> addedServices = new IdMap<>();
        IdSet<Station> excludedStations = new IdSet<>();

        AtomicInteger count = new AtomicInteger();
        stopTimes.
                filter(stopTimeData -> trips.hasId(stopTimeData.getTripId())).
                forEach((stopTimeData) -> {
            String stopId = stopTimeData.getStopId();
            IdFor<Station> stationId = factory.formStationId(stopId);

            if (preloadStations.hasId(stationId)) {
                Trip trip = trips.get(stopTimeData.getTripId());

                Station station = preloadStations.get(stationId);
                Route route = trip.getRoute();

                addStationTo(buildable, route, station, factory);

                StopCall stopCall = createStopCall(buildable, stopTimeData, route, station, factory, dataSourceConfig);
                trip.addStop(stopCall);

                if (!buildable.hasTripId(trip.getId())) {
                    buildable.addTrip(trip); // seen at least one stop for this trip
                }

                Service service = trip.getService();

                route.addTrip(trip);
                route.addService(service);

                addedServices.add(service);
                buildable.addService(service);
                count.getAndIncrement();

            } else {
                excludedStations.add(stationId);
            }
        });
        if (!excludedStations.isEmpty()) {
            logger.warn("Excluded the following station ids (flagged out of area) : " + excludedStations + " for " + sourceName);
            excludedStations.clear();
        }
        logger.info("Loaded " + count.get() + " stop times for " + sourceName);
        return addedServices;
    }

    private StopCall createStopCall(PlatformRepository buildable, StopTimeData stopTimeData,
                                    Route route, Station station, TransportEntityFactory factory,
                                    GTFSSourceConfig sourceConfig) {
        IdFor<Platform> platformId = stopTimeData.getPlatformId();
        TransportMode transportMode = route.getTransportMode();

        // TODO Should be HasPlatform
        if (sourceConfig.getTransportModesWithPlatforms().contains(transportMode)) {
            if (buildable.hasPlatformId(platformId)) {
                Platform platform = buildable.getPlatform(platformId);
                platform.addRoute(route);
            } else {
                IdFor<Route> routeId = route.getId();
                logger.warn("Missing platform " + platformId + " For transport mode " + transportMode + " and route " + routeId);
            }
            Platform platform = buildable.getPlatform(platformId);
            return factory.createPlatformStopCall(platform, station, stopTimeData);
        } else {
            return factory.createNoPlatformStopCall(station, stopTimeData);
        }
    }

    private void addStationTo(TransportDataContainer container, Route route, Station station, TransportEntityFactory factory) {
        station.addRoute(route);

        IdFor<Station> stationId = station.getId();
        if (!container.hasStationId(stationId)) {
            container.addStation(station);
            if (station.hasPlatforms()) {
                station.getPlatforms().forEach(container::addPlatform);
            }
        }

        if (!container.hasRouteStationId(RouteStation.createId(stationId, route.getId()))) {
            RouteStation routeStation = factory.createRouteStation(station, route);
            container.addRouteStation(routeStation);
        }
    }

    private TripAndServices loadTripsAndServices(TransportData transportData, Stream<TripData> tripDataStream,
                                                 IdSet<Route> excludedRoutes, TransportEntityFactory factory) {
        logger.info("Loading trips");
        IdMap<Trip> trips = new IdMap<>();
        IdMap<Service> services = new IdMap<>();

        AtomicInteger count = new AtomicInteger();

        tripDataStream.forEach((tripData) -> {
            IdFor<Service> serviceId = tripData.getServiceId();
            IdFor<Route> routeId = factory.createRouteId(tripData.getRouteId());
            IdFor<Trip> tripId = tripData.getTripId();

            if (transportData.hasRouteId(routeId)) {
                Route route = transportData.getRouteById(routeId);
                Service service = services.getOrAdd(serviceId, () -> factory.createService(serviceId));
                trips.getOrAdd(tripId, () -> factory.createTrip(tripData, service, route));
                count.getAndIncrement();
            } else {
                if (!excludedRoutes.contains(routeId)) {
                    logger.warn(format("Unable to find RouteId '%s' for trip '%s", routeId, tripData));
                }
            }
        });
        logger.info("Loaded " + count.get() + " trips");
        return new TripAndServices(services, trips);
    }

    private  IdMap<Agency> preloadAgencys(DataSourceID dataSourceID, Stream<AgencyData> agencyDataStream, TransportEntityFactory factory) {
        logger.info("Loading all agencies for " + dataSourceID);
        IdMap<Agency> agencies = new IdMap<>();
        agencyDataStream.forEach(agencyData -> agencies.add(factory.createAgency(dataSourceID, agencyData)));
        logger.info("Loaded " + agencies.size() + " agencies for " + dataSourceID);
        return agencies;
    }

    private IdSet<Route> populateRoutes(TransportDataContainer buildable, Stream<RouteData> routeDataStream,
                                        IdMap<Agency> allAgencies, IdMap<Station> allStations,
                                        GTFSSourceConfig sourceConfig, TransportEntityFactory factory) {
        Set<GTFSTransportationType> transportModes = sourceConfig.getTransportGTFSModes();
        AtomicInteger count = new AtomicInteger();

        logger.info("Loading routes for transport modes " + transportModes.toString());
        IdSet<Route> excludedRoutes = new IdSet<>();
        routeDataStream.forEach(routeData -> {
            IdFor<Agency> agencyId = routeData.getAgencyId();
            boolean missingAgency = !allAgencies.hasId(agencyId);
            if (missingAgency) {
                logger.error("Missing agency " + agencyId);
            }

            GTFSTransportationType routeType = factory.getRouteType(routeData, agencyId);

            if (transportModes.contains(routeType)) {
                DataSourceID dataSourceID = sourceConfig.getDataSourceId();
                Agency agency = missingAgency ? createMissingAgency(dataSourceID, allAgencies, agencyId, factory) : allAgencies.get(agencyId);

                Route route = factory.createRoute(routeType, routeData, agency, allStations);

                agency.addRoute(route);
                if (!buildable.hasAgency(agencyId)) {
                    buildable.addAgency(agency);
                }
                buildable.addRoute(route);

                count.getAndIncrement();

            } else {
                IdFor<Route> routeId = routeData.getId();
                excludedRoutes.add(factory.createRouteId(routeId));
            }
        });
        logExcludedRoutes(transportModes, excludedRoutes);
        logger.info("Loaded " + count.get() + " routes of transport types " + transportModes + " excluded "+ excludedRoutes.size());
        return excludedRoutes;
    }

    private void logExcludedRoutes(Set<GTFSTransportationType> transportModes, IdSet<Route> excludedRoutes) {
        if (excludedRoutes.isEmpty()) {
            return;
        }
        logger.info("Excluded the following route id's as did not match modes " + transportModes + " routes: " +
                excludedRoutes);
    }

    private Agency createMissingAgency(DataSourceID dataSourceID, IdMap<Agency> allAgencies, IdFor<Agency> agencyId, TransportEntityFactory factory) {
        Agency unknown = factory.createUnknownAgency(dataSourceID, agencyId);
        logger.error("Created agency" + unknown + " for " + dataSourceID);
        allAgencies.add(unknown);
        return unknown;
    }

    private IdMap<Station> preLoadStations(Stream<StopData> stops, TransportEntityFactory factory) {
        logger.info("Loading stops within bounds");
        BoundingBox bounds = config.getBounds();

        IdMap<Station> allStations = new IdMap<>();

        stops.forEach((stopData) -> {
            LatLong latLong = stopData.getLatLong();
            if (latLong.isValid()) {
                GridPosition position = getGridPosition(stopData.getLatLong());
                if (bounds.contained(position)) {
                    preLoadStation(allStations, stopData, position, factory);
                } else {
                    logger.info("Excluding stop outside of bounds" + stopData);
                }
            } else {
                logger.warn("Stop has invalid postion " + stopData);
                preLoadStation(allStations, stopData, GridPosition.Invalid, factory);
            }
        });
        logger.info("Pre Loaded " + allStations.size() + " stations");
        return allStations;
    }

    private void preLoadStation(IdMap<Station> allStations, StopData stopData, GridPosition position, TransportEntityFactory factory) {
        String stopId = stopData.getId();
        IdFor<Station> stationId = factory.formStationId(stopId);

        if (allStations.hasId(stationId)) {
            factory.updateStation(allStations.get(stationId), stopData);
        } else {
            allStations.add(factory.createStation(stationId, stopData, position));
        }
    }

    private GridPosition getGridPosition(LatLong latLong) {
        return CoordinateTransforms.getGridPosition(latLong);
    }

    private static class TripAndServices  {
        private final IdMap<Service> services;
        private final IdMap<Trip> trips;

        public TripAndServices(IdMap<Service> services, IdMap<Trip> trips) {
            this.services = services;
            this.trips = trips;
        }

        public void clear() {
            services.clear();
            trips.clear();
        }
    }
}
