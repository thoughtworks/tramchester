package com.tramchester.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.input.BusStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TransportDataFromFilesBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFilesBuilder.class);

    private final List<TransportDataStreams> transportDataStreams;
    private final StationLocations stationLocations;
    private final TramchesterConfig config;

    private TransportDataContainer toBuild;

    public TransportDataFromFilesBuilder(List<TransportDataStreams> transportDataStreams, StationLocations stationLocations, TramchesterConfig config) {
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
        if(streams.hasFeedInfo()) {
            FeedInfo feedInfo = streams.feedInfo.findFirst().get();
            buildable.SetFeedInfo(feedInfo);
            buildable.SetVersion(feedInfo.getVersion());
        } else {
            // TODO Base on file mod time??
            buildable.SetVersion(UUID.randomUUID().toString());
            logger.warn("Do not have feedinfo for this data source");
        }

        Map<String, Agency> allAgencies = populateAgencies(streams.agencies);
        Set<String> excludedRoutes = populateRoutes(buildable, streams.routes, allAgencies);
        Map<String, Station> allStations = loadStations(streams.stops);

        ExcludedTripAndServices excludedTripsAndServices = populateTripsAndServices(buildable, streams.trips, excludedRoutes);
        populateStopTimes(buildable, streams.stopTimes, allStations, excludedTripsAndServices.excludedTrips);
        populateCalendars(buildable, streams.calendars, streams.calendarsDates, excludedTripsAndServices.excludedServices);
        buildable.updateTimesForServices();

        buildable.reportNumbers();

        // update svcs where calendar data is missing
        buildable.getServices().stream().filter(Service::HasMissingDates).forEach(
                svc -> logger.warn(format("Service %s has missing date data or runs on zero days", svc.getId()))
        );
        streams.closeAll();
    }

    private void populateCalendars(TransportDataContainer buildable, Stream<CalendarData> calendars,
                                   Stream<CalendarDateData> calendarsDates, Set<String> excludedServices) {

        logger.info("Loading calendars");
        Set<String> missingCalendar = new HashSet<>();
        calendars.forEach(calendar -> {
            String serviceId = calendar.getServiceId();
            Service service = buildable.getService(serviceId);

            if (service != null) {
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
            } else {
                if (!excludedServices.contains(serviceId)) {
                    missingCalendar.add(serviceId);
                }
            }
        });
        if (!missingCalendar.isEmpty()) {
            logger.warn("Failed to match service id " + missingCalendar.toString() + " for calendar");
        }

        logger.info("Loading calendar dates");
        Set<String> missingCalendarDates = new HashSet<>();
        calendarsDates.forEach(date -> {
            String serviceId = date.getServiceId();
            Service service = buildable.getService(serviceId);
            if (service != null) {
                service.addExceptionDate(date.getDate(), date.getExceptionType());
            } else {
                if (!excludedServices.contains(serviceId)) {
                    missingCalendarDates.add(serviceId);
                }
            }
        });
        if (!missingCalendarDates.isEmpty()) {
            logger.warn("Failed to find service id " + missingCalendarDates.toString() + " for calendar_dates");
        }
    }

    private void populateStopTimes(TransportDataContainer buildable, Stream<StopTimeData> stopTimes,
                                   Map<String, Station> allStations, Set<String> excludedTrips) {
        logger.info("Loading stop times");
        AtomicInteger count = new AtomicInteger();
        stopTimes.filter(stopTimeData -> !excludedTrips.contains(stopTimeData.getTripId())).forEach((stopTimeData) -> {
            Trip trip = buildable.getTrip(stopTimeData.getTripId());
            String platformId = stopTimeData.getStopId();
            String stationId = Station.formId(platformId);

            if (allStations.containsKey(stationId)) {
                Route route = trip.getRoute();
                Station station = allStations.get(stationId);
                addStation(buildable, route, station);

                byte stopSequence = Byte.parseByte(stopTimeData.getStopSequence());
                StopCall stop;
                if (route.isTram()) {
                    if (buildable.hasPlatformId(platformId)) {
                        Platform platform = buildable.getPlatform(platformId);
                        platform.addRoute(route);
                    } else {
                        logger.error("Missing platform " + platformId);
                    }
                    Platform platform = buildable.getPlatform(platformId);
                    stop = new TramStopCall(platform, station, stopSequence, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime());
                } else {
                    stop = new BusStopCall(station, stopSequence, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime());
                }
                count.getAndIncrement();

                trip.addStop(stop);
            } else {
                logger.warn(format("Cannot find station for Id '%s' for stopId '%s'", stationId, platformId));
            }
        });
        logger.info("Loaded " + count.get() + " stop times");
    }

    private void addStation(TransportDataContainer buildable, Route route, Station station) {
        String stationId = station.getId();
        stationLocations.addStation(station);
        station.addRoute(route);
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

    private ExcludedTripAndServices populateTripsAndServices(TransportDataContainer buildable, Stream<TripData> trips,
                                          Set<String> excludedRoutes) {
        logger.info("Loading trips");
        Set<String> excludedTrips = new HashSet<>();
        Set<String> excludedServices = new HashSet<>();
        AtomicInteger count = new AtomicInteger();

        trips.forEach((tripData) -> {
            String serviceId = tripData.getServiceId();
            String routeId = tripData.getRouteId();

            if (buildable.hasRouteId(routeId)) {
                Route route = buildable.getRoute(routeId);

                Service service = getOrInsertService(buildable, serviceId, route, excludedServices);
                Trip trip = getOrCreateTrip(buildable, tripData.getTripId(), tripData.getTripHeadsign(), service, route );
                count.getAndIncrement();
                service.addTrip(trip);
                route.addService(service);
                route.addHeadsign(trip.getHeadsign());
            } else {
                if (excludedRoutes.contains(routeId)) {
                    excludedTrips.add(tripData.getTripId());
                    if (!buildable.hasServiceId(serviceId)) {
                        excludedServices.add(serviceId);
                    }
                } else {
                    logger.warn(format("Unable to find RouteId '%s' for trip '%s", routeId, tripData));
                }
            }
        });
        logger.info("Loaded " + count.get());
        return new ExcludedTripAndServices(excludedTrips, excludedServices);
    }

    private  Map<String,Agency> populateAgencies(Stream<AgencyData> agencyDataStream) {
        logger.info("Loading all agencies");
        Map<String,Agency> agencies = new HashMap<>();
        agencyDataStream.forEach(agencyData -> agencies.put(agencyData.getId(), new Agency(agencyData.getId(), agencyData.getName())));
        logger.info("Loaded " + agencies.size() + " agencies");
        return agencies;
    }

    private Set<String> populateRoutes(TransportDataContainer buildable, Stream<RouteData> routeDataStream, Map<String,Agency> allAgencies) {
        List<GTFSTransportationType> transportModes = config.getTransportModes();
        AtomicInteger count = new AtomicInteger();

        logger.info("Loading routes for transport modes " + transportModes.toString());
        Set<String> excludedRoutes = new HashSet<>();
        routeDataStream.forEach(routeData -> {
            String agencyId = routeData.getAgency();
            if (!allAgencies.containsKey(agencyId)) {
                logger.error("Missing agency " + agencyId);
            }

            GTFSTransportationType routeType = GTFSTransportationType.getType(routeData.getRouteType());

            String routeName = routeData.getLongName();
            if (config.getRemoveRouteNameSuffix()) {
                int indexOf = routeName.indexOf("(");
                if (indexOf > -1) {
                    routeName = routeName.substring(0,indexOf).trim();
                }
            }

            if (transportModes.contains(routeType)) {
                count.getAndIncrement();
                Agency agency = allAgencies.get(agencyId);
                Route route = new Route(routeData.getId(), routeData.getShortName().trim(), routeName, agency,
                        TransportMode.fromGTFS(routeType));
                buildable.addAgency(agency);
                buildable.addRoute(route);
                buildable.addRouteToAgency(agency, route);
            } else {
                excludedRoutes.add(routeData.getId());
            }
        });
        logger.info("Loaded " + count.get() + " routes of transport types " + transportModes + " excluded "+ excludedRoutes.size());
        return excludedRoutes;
    }

    private HashMap<String, Station> loadStations(Stream<StopData> stops) {
        logger.info("Loading all stops");
        HashMap<String, Station> allStations = new HashMap<>();

        stops.forEach((stop) -> {
            if (unexpectedIdFormat(stop)) {
                logger.warn("Assumption all stations start with digit broken by " + stop.getId());
            }

            String stopId = stop.getId();
            Station station;
            String stationId = Station.formId(stopId);

            if (!allStations.containsKey(stationId)) {
                station = new Station(stationId, stop.getArea(), workAroundName(stop.getName()), stop.getLatLong(), stop.isTram());
                allStations.put(station.getId(), station);
            } else {
                station = allStations.get(stationId);
            }


            // TODO Trains?
            if (stop.isTram()) {
//                Platform platform;
//                if (!buildable.hasPlatformId(stopId)) {
//                    buildable.addPlatform(platform);
//                } else {
//                    platform = buildable.getPlatform(stopId);
//                }

                Platform platform = formPlatform(stop);
                if (!station.getPlatforms().contains(platform)) {
                    station.addPlatform(platform);
                }
            }

        });
        logger.info("Loaded " + allStations.size() + " stations");
        return allStations;
    }

    private boolean unexpectedIdFormat(StopData stop) {
        return !Character.isDigit(stop.getId().charAt(0));
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

    private Service getOrInsertService(TransportDataContainer buildable, String serviceId, Route route, Set<String> excludedServices) {
        if (!buildable.hasServiceId(serviceId)) {
            buildable.addService(new Service(serviceId, route));
            excludedServices.remove(serviceId);
        }
        Service service = buildable.getService(serviceId);
        service.addRoute(route);
        return service;
    }

    private Trip getOrCreateTrip(TransportDataContainer buildable, String tripId, String tripHeadsign, Service service, Route route) {
        if (buildable.hasTripId(tripId)) {
            Trip matched = buildable.getTrip(tripId);
            if ((!matched.getRoute().equals(route)) || !matched.getService().equals(service) || !matched.getHeadsign().equals(tripHeadsign)) {
                logger.error("Mismatch for trip id: " + tripId + " (mis)matched was " + matched);
            }
            return matched;
        }

        Trip trip = new Trip(tripId, tripHeadsign, service, route);
        buildable.addTrip(trip);
        return trip;
    }

    public static class TransportDataStreams {
        final Stream<StopData> stops;
        final Stream<RouteData> routes;
        final Stream<TripData> trips;
        final Stream<StopTimeData> stopTimes;
        final Stream<CalendarData> calendars;
        final Stream<FeedInfo> feedInfo;
        final Stream<CalendarDateData> calendarsDates;
        final Stream<AgencyData> agencies;
        final private boolean expectFeedInfo;

        public TransportDataStreams(Stream<AgencyData> agencies, Stream<StopData> stops, Stream<RouteData> routes, Stream<TripData> trips,
                                    Stream<StopTimeData> stopTimes, Stream<CalendarData> calendars,
                                    Stream<FeedInfo> feedInfo, Stream<CalendarDateData> calendarsDates, boolean expectFeedInfo) {
            this.agencies = agencies;
            this.stops = stops;
            this.routes = routes;
            this.trips = trips;
            this.stopTimes = stopTimes;
            this.calendars = calendars;
            this.feedInfo = feedInfo;
            this.calendarsDates = calendarsDates;
            this.expectFeedInfo = expectFeedInfo;
        }

        public void closeAll() {
            stops.close();
            routes.close();
            trips.close();
            stopTimes.close();
            calendars.close();
            feedInfo.close();
            calendarsDates.close();
            agencies.close();
        }

        public boolean hasFeedInfo() {
            return expectFeedInfo;
        }
    }

    private static class ExcludedTripAndServices {
        private final Set<String> excludedTrips;
        private final Set<String> excludedServices;

        public ExcludedTripAndServices(Set<String> excludedTrips, Set<String> excludedServices) {
            this.excludedTrips = excludedTrips;
            this.excludedServices = excludedServices;
        }
    }
}
