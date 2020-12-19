package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
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

        IdMap<Agency> allAgencies = preloadAgencys(dataSource.agencies);
        IdSet<Route> excludedRoutes = populateRoutes(buildable, dataSource.routes, allAgencies, sourceConfig);
        logger.info("Excluding " + excludedRoutes.size()+" routes ");
        allAgencies.clear();

        TripAndServices tripsAndServices = loadTripsAndServices(buildable, dataSource.trips, excludedRoutes);
        excludedRoutes.clear();

        IdMap<Station> allStations = preLoadStations(dataSource.stops);
        IdMap<Service> services = populateStopTimes(buildable, dataSource.stopTimes, allStations, tripsAndServices.trips);
        allStations.clear();

        populateCalendars(buildable, dataSource.calendars, dataSource.calendarsDates, services);
        tripsAndServices.clear();

        buildable.updateTimesForServices();
        buildable.reportNumbers();

        // update svcs where calendar data is missing
        buildable.getServices().stream().filter(Service::HasMissingDates).forEach(
                svc -> logger.warn(format("source %s Service %s has missing date data or runs on zero days",
                        sourceName, svc.getId()))
        );
        dataSource.closeAll();

        loaded = true;
        logger.info("Finishing Loading data for " + sourceName);
        //logger.info("Bounds for loaded stations " + stationLocations.getBounds());
    }

    private void populateCalendars(TransportDataContainer buildable, Stream<CalendarData> calendars,
                                   Stream<CalendarDateData> calendarsDates, IdMap<Service> services) {

        logger.info("Loading calendars");
        IdSet<Service> missingCalendar = services.getIds();
        calendars.forEach(calendarData -> {
            IdFor<Service> serviceId = calendarData.getServiceId();
            Service service = buildable.getService(serviceId);

            if (service != null) {
                missingCalendar.remove(serviceId);
                service.setDays(
                        calendarData.isMonday(),
                        calendarData.isTuesday(),
                        calendarData.isWednesday(),
                        calendarData.isThursday(),
                        calendarData.isFriday(),
                        calendarData.isSaturday(),
                        calendarData.isSunday()
                );
                service.setServiceDateRange(calendarData.getStartDate(), calendarData.getEndDate());
            }
        });
        if (!missingCalendar.isEmpty()) {
            logger.warn("Failed to match service id " + missingCalendar.toString() + " for calendar");
        }

        logger.info("Loading calendar dates");
        IdSet<Service> missingCalendarDates = services.getIds();
        calendarsDates.forEach(date -> {
            IdFor<Service> serviceId = date.getServiceId();
            Service service = buildable.getService(serviceId);
            if (service != null) {
                missingCalendarDates.remove(serviceId);
                int exceptionType = date.getExceptionType();
                if (exceptionType==CalendarDateData.ADDED || exceptionType==CalendarDateData.REMOVED) {
                    service.addExceptionDate(date.getDate(), exceptionType);
                } else {
                    logger.warn("Unexpected exception type " + exceptionType + " for service " + serviceId + " and date " + date.getDate());
                }
            }
        });
        if (!missingCalendarDates.isEmpty()) {
            logger.warn("Failed to find service id " + missingCalendarDates.toString() + " for calendar_dates");
        }
    }

    private IdMap<Service> populateStopTimes(TransportDataContainer buildable, Stream<StopTimeData> stopTimes,
                                   IdMap<Station> stations, IdMap<Trip> trips) {
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

                addStation(buildable, route, station);

                StopCall stopCall = createStopCall(buildable, stopTimeData, route, station);
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
                                    Route route, Station station) {
        StopCall stopCall;
        IdFor<Platform> platformId = stopTimeData.getPlatformId();
        switch (route.getTransportMode()) {
            case Tram:
                if (buildable.hasPlatformId(platformId)) {
                    Platform platform = buildable.getPlatform(platformId);
                    platform.addRoute(route);
                } else {
                    logger.error("Missing platform " + platformId);
                }
                Platform platform = buildable.getPlatform(platformId);
                stopCall = new TramStopCall(platform, station, stopTimeData);
                break;
            case Bus:
                stopCall = new BusStopCall(station, stopTimeData);
                break;
            case Train:
                stopCall = new TrainStopCall(station, stopTimeData);
                break;
            default:
                throw new RuntimeException("Unexpected transport mode " + route.getTransportMode());

        }

        return stopCall;
    }

    private void addStation(TransportDataContainer buildable, Route route, Station station) {
        station.addRoute(route);

        IdFor<Station> stationId = station.getId();
        if (!buildable.hasStationId(stationId)) {
            buildable.addStation(station);
            if (station.hasPlatforms()) {
                station.getPlatforms().forEach(buildable::addPlatform);
            }
        }

        if (!buildable.hasRouteStationId(RouteStation.formId(stationId, route.getId()))) {
            RouteStation routeStation = new RouteStation(station, route);
            buildable.addRouteStation(routeStation);
        }
    }

    private TripAndServices loadTripsAndServices(TransportData transportData, Stream<TripData> tripDataStream,
                                                 IdSet<Route> excludedRoutes) {
        logger.info("Loading trips");
        IdMap<Trip> trips = new IdMap<>();
        IdMap<Service> services = new IdMap<>();

        AtomicInteger count = new AtomicInteger();

        tripDataStream.forEach((tripData) -> {
            IdFor<Service> serviceId = tripData.getServiceId();
            IdFor<Route> routeId = tripData.getRouteId();
            IdFor<Trip> tripId = tripData.getTripId();
            String tripHeadsign = tripData.getHeadsign();

            if (transportData.hasRouteId(routeId)) {
                Route route = transportData.getRouteById(routeId);

                Service service = services.getOrAdd(serviceId, () -> new Service(serviceId, route));
                trips.getOrAdd(tripId, () -> new Trip(tripId, tripHeadsign, service, route));
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

    private  IdMap<Agency> preloadAgencys(Stream<AgencyData> agencyDataStream) {
        logger.info("Loading all agencies");
        IdMap<Agency> agencies = new IdMap<>();
        agencyDataStream.forEach(agencyData -> agencies.add(new Agency(agencyData.getId(), agencyData.getName())));
        logger.info("Loaded " + agencies.size() + " agencies");
        return agencies;
    }

    private IdSet<Route> populateRoutes(TransportDataContainer buildable, Stream<RouteData> routeDataStream,
                                       IdMap<Agency> allAgencies, DataSourceConfig sourceConfig) {
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

            IdFor<Route> routeId = routeData.getId();

            GTFSTransportationType routeType = getTransportTypeWithDataWorkaround(routeData, agencyId);

            if (transportModes.contains(routeType)) {
                String routeName = routeData.getLongName();
                if (config.getRemoveRouteNameSuffix()) {
                    int indexOf = routeName.indexOf("(");
                    if (indexOf > -1) {
                        routeName = routeName.substring(0,indexOf).trim();
                    }
                }

                count.getAndIncrement();
                Agency agency = missingAgency ? createMissingAgency(allAgencies, agencyId) : allAgencies.get(agencyId);
                Route route = new Route(routeId, routeData.getShortName().trim(), routeName, agency,
                        TransportMode.fromGTFS(routeType), routeData.getRouteDirection());
                agency.addRoute(route);
                buildable.addAgency(agency);
                buildable.addRoute(route);
            } else {
                logger.info("Excluding route as no matched on transport mode :" + routeData);
                excludedRoutes.add(routeId);
            }
        });
        logger.info("Loaded " + count.get() + " routes of transport types " + transportModes + " excluded "+ excludedRoutes.size());
        return excludedRoutes;
    }

    private GTFSTransportationType getTransportTypeWithDataWorkaround(RouteData routeData, IdFor<Agency> agencyId) {
        // tfgm data issue workaround
        GTFSTransportationType routeType = routeData.getRouteType();
        if (Agency.IsMetrolink(agencyId) && routeType!=GTFSTransportationType.tram) {
            logger.error("METROLINK Agency seen with transport type " + routeType.name() + " for " + routeData);
            logger.warn("Setting transport type to " + GTFSTransportationType.tram.name() + " for " + routeData);
            routeType = GTFSTransportationType.tram;
        }
        return routeType;
    }

    private Agency createMissingAgency(IdMap<Agency> allAgencies, IdFor<Agency> agencyId) {
        Agency unknown = new Agency(agencyId.getGraphId(), "UNKNOWN");
        logger.error("Created " + unknown);
        allAgencies.add(unknown);
        return unknown;
    }

    private IdMap<Station> preLoadStations(Stream<StopData> stops) {
        logger.info("Loading stops within bounds");
        BoundingBox bounds = config.getBounds();

        IdMap<Station> allStations = new IdMap<>();

        stops.forEach((stopData) -> {
            GridPosition position = getGridPosition(stopData.getLatLong());
            if (bounds.contained(position)) {
                String stopId = stopData.getId();
                IdFor<Station> stationId = Station.formId(stopId);

                // NOTE: Tram data has unique positions for each platform
                // TODO What is the right position to use for a tram station?
                Station station = allStations.getOrAdd(stationId, () ->
                        new Station(stationId, stopData.getArea(), workAroundName(stopData.getName()),
                                stopData.getLatLong(), position));

                if (stopData.isTFGMTram()) {
                    Platform platform = formPlatform(stopData);
                    if (!station.getPlatforms().contains(platform)) {
                        station.addPlatform(platform);
                    }
                }
            } else {
                logger.info("Excluding stop outside of bounds" + stopData);
            }

        });
        logger.info("Pre Loaded " + allStations.size() + " stations");
        return allStations;
    }

    private GridPosition getGridPosition(LatLong latLong) {
        return CoordinateTransforms.getGridPosition(latLong);
    }

    private String workAroundName(String name) {
        if ("St Peters Square".equals(name)) {
            return "St Peter's Square";
        }
        return name;
    }

    private Platform formPlatform(StopData stop) {
        return new Platform(stop.getId(), stop.getName(), stop.getLatLong());
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
