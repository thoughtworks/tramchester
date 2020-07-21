package com.tramchester.repository;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.input.*;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TransportDataFromFilesBuilderGeoFilter {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFilesBuilderGeoFilter.class);

    private final List<TransportDataStreams> transportDataStreams;
    private final StationLocations stationLocations;
    private final TramchesterConfig config;

    private TransportDataContainer toBuild;

    public TransportDataFromFilesBuilderGeoFilter(List<TransportDataStreams> transportDataStreams, StationLocations stationLocations,
                                                  TramchesterConfig config) {
        this.transportDataStreams = transportDataStreams;
        this.stationLocations = stationLocations;
        this.config = config;
        toBuild = null;
    }

    public TransportDataSource getData() {
        return toBuild;
    }

    public void load() {
        toBuild = new TransportDataContainer();
        logger.info("Loading transport data from files");
        transportDataStreams.forEach(transportDataStream -> load(transportDataStream, toBuild));
        logger.info("Finished loading transport data");
    }

    private void load(TransportDataStreams streams, TransportDataContainer buildable) {
        String sourceName = streams.getNameAndVersion().getName();
        DataSourceConfig sourceConfig = streams.getConfig();
        logger.info("Loading data for " + sourceName);

        DataSourceInfo.NameAndVersion fromStreams = streams.getNameAndVersion();
        if(sourceConfig.getHasFeedInfo()) {
            FeedInfo feedInfo = streams.feedInfo.findFirst().get();
            String name = fromStreams.getName();
            buildable.addNameAndVersion(new DataSourceInfo.NameAndVersion(name, feedInfo.getVersion()));
            buildable.addFeedInfo(name, feedInfo);
        } else {
            logger.warn("No feedinfo for " + sourceName);
            buildable.addNameAndVersion(fromStreams);
        }

        IdMap<Agency> allAgencies = preloadAgencys(streams.agencies);
        Set<String> excludedRoutes = populateRoutes(buildable, streams.routes, allAgencies, sourceConfig);
        logger.info("Excluding " + excludedRoutes.size()+" routes ");
        allAgencies.clear();

        TripAndServices tripsAndServices = loadTripsAndServices(buildable, streams.trips, excludedRoutes);
        excludedRoutes.clear();

        IdMap<Station> allStations = preLoadStations(streams.stops);
        IdMap<Service> services = populateStopTimes(buildable, streams.stopTimes, allStations, tripsAndServices.trips);
        allStations.clear();

        populateCalendars(buildable, streams.calendars, streams.calendarsDates, services);
        tripsAndServices.clear();

        buildable.updateTimesForServices();
        buildable.reportNumbers();

        // update svcs where calendar data is missing
        buildable.getServices().stream().filter(Service::HasMissingDates).forEach(
                svc -> logger.warn(format("source %s Service %s has missing date data or runs on zero days",
                        sourceName, svc.getId()))
        );
        streams.closeAll();
        logger.info("Finishing Loading data for " + sourceName);
        logger.info("Bounds for loaded stations " + stationLocations.getBounds());
    }

    private void populateCalendars(TransportDataContainer buildable, Stream<CalendarData> calendars,
                                   Stream<CalendarDateData> calendarsDates, IdMap<Service> services) {

        logger.info("Loading calendars");
        Set<String> missingCalendar = services.getIds();
        calendars.forEach(calendar -> {
            String serviceId = calendar.getServiceId();
            Service service = buildable.getService(serviceId);

            if (service != null) {
                missingCalendar.remove(serviceId);
                service.setDays(
                        calendar.isMonday(),
                        calendar.isTuesday(),
                        calendar.isWednesday(),
                        calendar.isThursday(),
                        calendar.isFriday(),
                        calendar.isSaturday(),
                        calendar.isSunday()
                );
                service.setServiceDateRange(calendar.getStartDate(), calendar.getEndDate());
            }
        });
        if (!missingCalendar.isEmpty()) {
            logger.warn("Failed to match service id " + missingCalendar.toString() + " for calendar");
        }

        logger.info("Loading calendar dates");
        Set<String> missingCalendarDates = services.getIds();
        calendarsDates.forEach(date -> {
            String serviceId = date.getServiceId();
            Service service = buildable.getService(serviceId);
            if (service != null) {
                missingCalendarDates.remove(serviceId);
                service.addExceptionDate(date.getDate(), date.getExceptionType());
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
        Set<String> excludedStations = new HashSet<>();

        AtomicInteger count = new AtomicInteger();
        stopTimes.filter(stopTimeData -> trips.hasId(stopTimeData.getTripId())).forEach((stopTimeData) -> {
            Trip trip = trips.get(stopTimeData.getTripId());
            String stopId = stopTimeData.getStopId();
            String stationId = Station.formId(stopId);

            if (stations.hasId(stationId)) {
                Station station = stations.get(stationId);
                Route route = trip.getRoute();

                addStation(buildable, route, station);

                StopCall stopCall = createStopCall(buildable, stopTimeData, stopId, route, station);
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
                                    String stopId, Route route, Station station) {
        StopCall stopCall;
        switch (route.getTransportMode()) {
            case Tram:
                if (buildable.hasPlatformId(stopId)) {
                    Platform platform = buildable.getPlatform(stopId);
                    platform.addRoute(route);
                } else {
                    logger.error("Missing platform " + stopId);
                }
                Platform platform = buildable.getPlatform(stopId);
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
        stationLocations.addStation(station);
        station.addRoute(route);

        String stationId = station.getId();
        if (!buildable.hasStationId(stationId)) {
            buildable.addStation(station);
            if (station.hasPlatforms()) {
                station.getPlatforms().forEach(buildable::addPlatform);
            }
        }
        RouteStation routeStation = new RouteStation(station, route);
        if (!buildable.hasRouteStation(routeStation.getId())) {
            buildable.addRouteStation(routeStation);
        }
    }

    private TripAndServices loadTripsAndServices(TransportData transportData, Stream<TripData> tripDataStream,
                                                 Set<String> excludedRoutes) {
        logger.info("Loading trips");
        IdMap<Trip> trips = new IdMap<>();
        IdMap<Service> services = new IdMap<>();

        AtomicInteger count = new AtomicInteger();

        tripDataStream.forEach((tripData) -> {
            String serviceId = tripData.getServiceId();
            String routeId = tripData.getRouteId();
            String tripId = tripData.getTripId();
            String tripHeadsign = tripData.getTripHeadsign();

            if (transportData.hasRouteId(routeId)) {
                Route route = transportData.getRoute(routeId);

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
        logger.info("Loaded " + count.get());
        return new TripAndServices(services, trips);
    }

    private  IdMap<Agency> preloadAgencys(Stream<AgencyData> agencyDataStream) {
        logger.info("Loading all agencies");
        IdMap<Agency> agencies = new IdMap<>();
        agencyDataStream.forEach(agencyData -> agencies.add(new Agency(agencyData.getId(), agencyData.getName())));
        logger.info("Loaded " + agencies.size() + " agencies");
        return agencies;
    }

    private Set<String> populateRoutes(TransportDataContainer buildable, Stream<RouteData> routeDataStream,
                                       IdMap<Agency> allAgencies, DataSourceConfig sourceConfig) {
        Set<GTFSTransportationType> transportModes = sourceConfig.getTransportModes();
        AtomicInteger count = new AtomicInteger();

        logger.info("Loading routes for transport modes " + transportModes.toString());
        Set<String> excludedRoutes = new HashSet<>();
        routeDataStream.forEach(routeData -> {
            String agencyId = routeData.getAgency();
            if (!allAgencies.hasId(agencyId)) {
                logger.error("Missing agency " + agencyId);
            }

            GTFSTransportationType routeType = routeData.getRouteType();

            if (transportModes.contains(routeType)) {
                String routeName = routeData.getLongName();
                if (config.getRemoveRouteNameSuffix()) {
                    int indexOf = routeName.indexOf("(");
                    if (indexOf > -1) {
                        routeName = routeName.substring(0,indexOf).trim();
                    }
                }

                count.getAndIncrement();
                Agency agency = allAgencies.get(agencyId);
                Route route = new Route(routeData.getId(), routeData.getShortName().trim(), routeName, agency,
                        TransportMode.fromGTFS(routeType));
                agency.addRoute(route);
                buildable.addAgency(agency);
                buildable.addRoute(route);
            } else {
                excludedRoutes.add(routeData.getId());
            }
        });
        logger.info("Loaded " + count.get() + " routes of transport types " + transportModes + " excluded "+ excludedRoutes.size());
        return excludedRoutes;
    }

    private IdMap<Station> preLoadStations(Stream<StopData> stops) {
        logger.info("Loading stops within bounds");

        IdMap<Station> allStations = new IdMap<>();

        stops.forEach((stopData) -> {
            if (config.getBounds().contained(stopData.getLatLong())) {
                String stopId = stopData.getId();
                Station station;
                String stationId = Station.formId(stopId);

                if (!allStations.hasId(stationId)) {
                    station = new Station(stationId, stopData.getArea(), workAroundName(stopData.getName()), stopData.getLatLong());
                    allStations.add(station);
                } else {
                    station = allStations.get(stationId);
                }

                if (stopData.isTram()) {
                    Platform platform = formPlatform(stopData);
                    if (!station.getPlatforms().contains(platform)) {
                        station.addPlatform(platform);
                    }
                }
            } else {
                logger.info("Excluding stop " + stopData);
            }

        });
        logger.info("Loaded " + allStations.size() + " stations");
        return allStations;
    }

    private String workAroundName(String name) {
        if ("St Peters Square".equals(name)) {
            return "St Peter's Square";
        }
        return name;
    }

    private Platform formPlatform(StopData stop) {
        return new Platform(stop.getId(), stop.getName());
    }

//    @Deprecated
//    private Service getOrInsertService(TransportDataContainer buildable, String serviceId, Route route, Set<String> excludedServices) {
//        if (!buildable.hasServiceId(serviceId)) {
//            buildable.addService(new Service(serviceId, route));
//            excludedServices.remove(serviceId);
//        }
//        Service service = buildable.getService(serviceId);
//        service.addRoute(route);
//        return service;
//    }
//
//    @Deprecated
//    private Trip getOrCreateTrip(TransportDataContainer buildable, String tripId, String tripHeadsign, Service service, Route route) {
//        if (buildable.hasTripId(tripId)) {
//            Trip matched = buildable.getTrip(tripId);
//            if ((!matched.getRoute().equals(route)) || !matched.getService().equals(service) || !matched.getHeadsign().equals(tripHeadsign)) {
//                logger.error("Mismatch for trip id: " + tripId + " (mis)matched was " + matched);
//            }
//            return matched;
//        }
//
//        Trip trip = new Trip(tripId, tripHeadsign, service, route);
//        buildable.addTrip(trip);
//        return trip;
//    }


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
