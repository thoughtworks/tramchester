package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.input.*;
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

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class TransportDataFromFiles implements TransportDataProvider {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFiles.class);

    private final List<TransportDataSource> transportDataStreams;
    private final TramchesterConfig config;
    private boolean loaded = false;

    private final TransportDataContainer dataContainer;

    @Inject
    public TransportDataFromFiles(List<TransportDataSource> transportDataStreams,
                                  TramchesterConfig config, ProvidesNow providesNow) {
        this.transportDataStreams = transportDataStreams;
        this.config = config;
        dataContainer = new TransportDataContainer(providesNow);
    }

    public TransportData getData() {
        if (!loaded) {
            load();
        }
        return dataContainer;
    }

    private void load() {
        if (loaded) {
            logger.warn("Data already loaded");
            return;
        }
        logger.info("Loading transport data from files");
        transportDataStreams.forEach(transportDataStream -> load(transportDataStream, dataContainer));
        logger.info("Finished loading transport data");
        loaded = true;
    }

    private void load(TransportDataSource dataSource, TransportDataContainer buildable) {
        DataSourceInfo dataSourceInfo = dataSource.getNameAndVersion();

        String sourceName = dataSourceInfo.getName();
        DataSourceConfig sourceConfig = dataSource.getConfig();
        logger.info("Loading data for " + sourceName);

        if(sourceConfig.getHasFeedInfo()) {
            // replace version string (which is from mod time) with the one from the feedinfo file, if present
            FeedInfo feedInfo = dataSource.feedInfo.findFirst().get();
            buildable.addDataSourceInfo(new DataSourceInfo(sourceName, feedInfo.getVersion(),
                    dataSourceInfo.getLastModTime(), dataSource.getNameAndVersion().getModes()));
            buildable.addFeedInfo(sourceName, feedInfo);
        } else {
            logger.warn("No feedinfo for " + sourceName);
            buildable.addDataSourceInfo(dataSourceInfo);
        }

        TransportEntityFactory entityFactory = dataSource.getEntityFactory();

        IdMap<Agency> allAgencies = preloadAgencys(dataSource.agencies, entityFactory);
        IdSet<Route> excludedRoutes = populateRoutes(buildable, dataSource.routes, allAgencies, sourceConfig, entityFactory);
        logger.info("Excluding " + excludedRoutes.size()+" routes ");
        allAgencies.clear();

        TripAndServices tripsAndServices = loadTripsAndServices(buildable, dataSource.trips, excludedRoutes, entityFactory);
        excludedRoutes.clear();

        IdMap<Station> allStations = preLoadStations(dataSource.stops, entityFactory);
        IdMap<Service> services = populateStopTimes(buildable, dataSource.stopTimes, allStations, tripsAndServices.trips, entityFactory,
                sourceConfig);
        allStations.clear();

        populateCalendars(buildable, dataSource.calendars, dataSource.calendarsDates, services, sourceConfig, entityFactory);
        tripsAndServices.clear();

        buildable.updateTimesForServices();
        buildable.reportNumbers();

        // update svcs where calendar data is missing
        buildable.getServices().stream().filter(service -> !service.hasCalendar()).forEach(
                svc -> logger.warn(format("source %s Service %s has missing calendar", sourceName, svc.getId()))
        );

        reportZeroDaysServices(buildable);

        dataSource.closeAll();

        loaded = true;
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
            logger.warn("The following services do no operate on any days per calendar.txt file " + noDayServices.toString());
        }
    }

    private void populateCalendars(TransportDataContainer buildable, Stream<CalendarData> calendars,
                                   Stream<CalendarDateData> calendarsDates, IdMap<Service> services, DataSourceConfig config,
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
            logger.warn("Failed to match service id " + missingCalendar.toString() + " for calendar");
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
            logger.warn("Failed to find service id " + missingCalendarDates.toString() + " for calendar_dates");
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
                logger.warn(format("Ignoring extra data %s as configured as no service date from %s", exceptionDate, noServices));
            }
        } else if (exceptionType == CalendarDateData.REMOVED) {
            calendar.excludeDate(exceptionDate);
        } else {
            logger.warn("Unexpected exception type " + exceptionType + " for service " + serviceId + " and date " + exceptionDate);
        }
    }

    private IdMap<Service> populateStopTimes(TransportDataContainer buildable, Stream<StopTimeData> stopTimes,
                                             IdMap<Station> stations, IdMap<Trip> trips,
                                             TransportEntityFactory factory, DataSourceConfig dataSourceConfig) {
        logger.info("Loading stop times");
        IdMap<Service> addedServices = new IdMap<>();
        IdSet<Station> excludedStations = new IdSet<>();

        AtomicInteger count = new AtomicInteger();
        stopTimes.filter(stopTimeData -> trips.hasId(stopTimeData.getTripId())).forEach((stopTimeData) -> {
            String stopId = stopTimeData.getStopId();
            IdFor<Station> stationId = Station.formId(stopId);

            if (stations.hasId(stationId)) {
                Trip trip = trips.get(stopTimeData.getTripId());

                Station station = stations.get(stationId);
                Route route = trip.getRoute();

                addStation(buildable, route, station, factory);

                StopCall stopCall = createStopCall(buildable, stopTimeData, route, station, factory, dataSourceConfig);
                trip.addStop(stopCall);

                if (!buildable.hasTripId(trip.getId())) {
                    buildable.addTrip(trip); // seen at least one stop for this trip
                    //
                }

                Service service = trip.getService();
                service.addTrip(trip);

                route.addService(service);
                route.addHeadsign(trip.getHeadsign());

                addedServices.add(service);
                buildable.addService(service);
                count.getAndIncrement();

            } else {
                excludedStations.add(stationId);
            }
        });
        if (!excludedStations.isEmpty()) {
            logger.warn("Excluded the following station ids: " + excludedStations.toString());
        }
        logger.info("Loaded " + count.get() + " stop times");
        return addedServices;
    }

    private StopCall createStopCall(PlatformRepository buildable, StopTimeData stopTimeData,
                                    Route route, Station station, TransportEntityFactory factory,
                                    DataSourceConfig sourceConfig) {
        IdFor<Platform> platformId = stopTimeData.getPlatformId();
        TransportMode transportMode = route.getTransportMode();

        // TODO Should be HasPlatform
        if (sourceConfig.getTransportModesWithPlatforms().contains(transportMode)) {
            if (buildable.hasPlatformId(platformId)) {
                Platform platform = buildable.getPlatform(platformId);
                platform.addRoute(route);
            } else {
                logger.error("Missing platform " + platformId);
            }
            Platform platform = buildable.getPlatform(platformId);
            return factory.createPlatformStopCall(platform, station, stopTimeData);
        } else {
            return factory.createNoPlatformStopCall(station, stopTimeData);
        }
    }

    private void addStation(TransportDataContainer buildable, Route route, Station station, TransportEntityFactory factory) {
        station.addRoute(route);

        IdFor<Station> stationId = station.getId();
        if (!buildable.hasStationId(stationId)) {
            buildable.addStation(station);
            if (station.hasPlatforms()) {
                station.getPlatforms().forEach(buildable::addPlatform);
            }
        }

        if (!buildable.hasRouteStationId(RouteStation.formId(stationId, route.getId()))) {
            RouteStation routeStation = factory.createRouteStation(station, route);
            buildable.addRouteStation(routeStation);
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
            IdFor<Route> routeId = tripData.getRouteId();
            IdFor<Trip> tripId = tripData.getTripId();

            if (transportData.hasRouteId(routeId)) {
                Route route = transportData.getRouteById(routeId);

                Service service = services.getOrAdd(serviceId, () -> factory.createService(serviceId, route));
                trips.getOrAdd(tripId, () -> factory.createTrip(tripData, service, route));
                service.addRoute(route);
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

    private  IdMap<Agency> preloadAgencys(Stream<AgencyData> agencyDataStream, TransportEntityFactory factory) {
        logger.info("Loading all agencies");
        IdMap<Agency> agencies = new IdMap<>();
        agencyDataStream.forEach(agencyData -> agencies.add(factory.createAgency(agencyData)));
        logger.info("Loaded " + agencies.size() + " agencies");
        return agencies;
    }

    private IdSet<Route> populateRoutes(TransportDataContainer buildable, Stream<RouteData> routeDataStream,
                                       IdMap<Agency> allAgencies, DataSourceConfig sourceConfig,
                                        TransportEntityFactory factory) {
        Set<GTFSTransportationType> transportModes = sourceConfig.getTransportModes();
        AtomicInteger count = new AtomicInteger();

        logger.info("Loading routes for transport modes " + transportModes.toString());
        IdSet<Route> excludedRoutes = new IdSet<>();
        routeDataStream.forEach(routeData -> {
            IdFor<Agency> agencyId = routeData.getAgencyId();
            boolean missingAgency = !allAgencies.hasId(agencyId);
            if (missingAgency) {
                logger.error("Missing agency " + agencyId);
            }

            GTFSTransportationType routeType = getTransportTypeWithDataWorkaround(routeData, agencyId);

            if (transportModes.contains(routeType)) {
                Agency agency = missingAgency ? createMissingAgency(allAgencies, agencyId, factory) : allAgencies.get(agencyId);

                Route route = factory.createRoute(routeType, routeData, agency);

                agency.addRoute(route);
                buildable.addAgency(agency);
                buildable.addRoute(route);

                count.getAndIncrement();

            } else {
                IdFor<Route> routeId = routeData.getId();
                excludedRoutes.add(routeId);
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

    private GTFSTransportationType getTransportTypeWithDataWorkaround(RouteData routeData, IdFor<Agency> agencyId) {
        // tfgm data issue workaround
        GTFSTransportationType routeType = routeData.getRouteType();
        if (Agency.IsMetrolink(agencyId) && routeType!=GTFSTransportationType.tram) {
            logger.error("METROLINK Agency seen with transport type " + routeType.name() + " for " + routeData);
            logger.warn("Setting transport type to " + GTFSTransportationType.tram.name() + " for " + routeData);
            routeType = GTFSTransportationType.tram;
        }
        // train data workaround
        if (routeData.getRouteType().equals(GTFSTransportationType.aerialLift) &&
                routeData.getLongName().contains("replacement bus service")) {
            logger.warn("Route has incorrect transport type for replace bus service, will set to bus. Route: " + routeData);
            routeType = GTFSTransportationType.bus;
        }
        return routeType;
    }

    private Agency createMissingAgency(IdMap<Agency> allAgencies, IdFor<Agency> agencyId, TransportEntityFactory factory) {
        Agency unknown = factory.createUnknownAgency(agencyId);
        logger.error("Created " + unknown);
        allAgencies.add(unknown);
        return unknown;
    }

    private IdMap<Station> preLoadStations(Stream<StopData> stops, TransportEntityFactory factory) {
        logger.info("Loading stops within bounds");
        BoundingBox bounds = config.getBounds();

        IdMap<Station> allStations = new IdMap<>();

        stops.forEach((stopData) -> {
            GridPosition position = getGridPosition(stopData.getLatLong());
            if (position.isValid()) {
                if (bounds.contained(position)) {
                    preLoadStation(allStations, stopData, position, factory);
                } else {
                    logger.info("Excluding stop outside of bounds" + stopData);
                }
            } else {
                logger.warn("Stop has invalid postion " + stopData);
                preLoadStation(allStations, stopData, position, factory);
            }
        });
        logger.info("Pre Loaded " + allStations.size() + " stations");
        return allStations;
    }

    private void preLoadStation(IdMap<Station> allStations, StopData stopData, GridPosition position, TransportEntityFactory factory) {
        String stopId = stopData.getId();
        IdFor<Station> stationId = Station.formId(stopId);

        // NOTE: Tram data has unique positions for each platform
        // TODO What is the right position to use for a tram station?
        Station station = allStations.getOrAdd(stationId, () -> factory.createStation(stationId, stopData, position));

        if (stopData.hasPlatforms()) {
            Platform platform = formPlatform(stopData, factory);
            if (!station.getPlatforms().contains(platform)) {
                station.addPlatform(platform);
            }
        }
    }

    private GridPosition getGridPosition(LatLong latLong) {
        return CoordinateTransforms.getGridPosition(latLong);
    }

    private Platform formPlatform(StopData stop, TransportEntityFactory factory) {
        return factory.createPlatform(stop);
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
