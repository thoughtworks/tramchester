package com.tramchester.repository;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.input.BusStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.geo.StationLocations;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import static java.lang.String.format;

public class TransportDataFromFiles implements TransportDataSource, Startable, Disposable {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFiles.class);

    private final TransportDataStreams transportDataStreams;
    private final HashMap<String, Trip> trips = new HashMap<>();        // trip id -> trip
    private final HashMap<String, Station> stationsById = new HashMap<>();  // station id -> station
    private final HashMap<String, Station> stationsByName = new HashMap<>();  // station id -> station
    private final HashMap<String, Service> services = new HashMap<>();  // service id -> service
    private final HashMap<String, Route> routes = new HashMap<>();      // route id -> route
    private final HashMap<String, Platform> platforms = new HashMap<>(); // platformId -> platform
    private final HashMap<String, RouteStation> routeStations = new HashMap<>(); // routeStationId - > RouteStation
    private final HashMap<String, Agency> agencies = new HashMap<>(); // agencyId -> agencies

    // keep track of excluded due to transport type filters so can log ones missing for other reasons
    private final Set<String> excludedRoutes;
    private final Set<String> excludedTrips;
    private final Set<String> excludedServices;

    private final StationLocations stationLocations;

    private FeedInfo feedInfo = null;
    private String version;

    public TransportDataFromFiles(StationLocations stationLocations, TransportDataStreams transportDataStreams) {
        this.stationLocations = stationLocations;
        this.transportDataStreams = transportDataStreams;
        this.excludedRoutes = new HashSet<>();
        this.excludedTrips = new HashSet<>();
        this.excludedServices = new HashSet<>();
    }

    @Override
    public void start() {
        logger.info("Loading transport data from files");

        if(transportDataStreams.hasFeedInfo()) {
            this.feedInfo = transportDataStreams.feedInfo.findFirst().get();
            this.version = feedInfo.getVersion();
        } else {
            // TODO Base on file mod time??
            this.version = UUID.randomUUID().toString();
            logger.warn("Do no have feedinfo for this data source");
        }

        populateAgencies(transportDataStreams.agencies);
        populateStationsAndAreas(transportDataStreams.stops);
        populateRoutes(transportDataStreams.routes);
        populateTrips(transportDataStreams.trips);
        populateStopTimes(transportDataStreams.stopTimes);
        populateCalendars(transportDataStreams.calendars, transportDataStreams.calendarsDates);
        updateTimesForServices();

        logger.info(format("%s stations", stationsById.size()));
        logger.info(format("%s routes", this.routes.size()));
        logger.info(format("%s services", services.size()));
        logger.info(format("%s trips", this.trips.size()));

        // update svcs where calendar data is missing
        services.values().stream().filter(Service::HasMissingDates).forEach(
                svc -> logger.warn(format("Service %s has missing date data or runs on zero days", svc.getId()))
        );
        transportDataStreams.closeAll();
        logger.info("Finished loading transport data");
    }

    private void updateTimesForServices() {
        // Cannot do this until after all stops loaded into trips
        logger.info("Updating timings for services");
        services.values().forEach(Service::updateTimings);
    }

    @Override
    public void stop() {
        // no op
    }

    @Override
    public void dispose() {
        logger.info("dispose");
        // testing overhead control
        trips.clear();
        stationsById.clear();
        stationsByName.clear();
        services.clear();
        platforms.clear();
        routeStations.clear();
        agencies.clear();
        routes.clear();
        excludedRoutes.clear();
        excludedTrips.clear();
        excludedServices.clear();
    }

    private void populateCalendars(Stream<CalendarData> calendars, Stream<CalendarDateData> calendarsDates) {

        Set<String> missingCalendar = new HashSet<>();
        calendars.forEach(calendar -> {
            String serviceId = calendar.getServiceId();
            Service service = services.get(serviceId);

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

        Set<String> missingCalendarDates = new HashSet<>();
        calendarsDates.forEach(date -> {
            String serviceId = date.getServiceId();
            Service service = services.get(serviceId);
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

    private void populateStopTimes(Stream<StopTimeData> stopTimes) {
        logger.info("Loading stop times");
        AtomicInteger count = new AtomicInteger();
        stopTimes.filter(stopTimeData -> !excludedTrips.contains(stopTimeData.getTripId())).forEach((stopTimeData) -> {
            Trip trip = getTrip(stopTimeData.getTripId());
            String platformId = stopTimeData.getStopId();
            String stationId = Station.formId(platformId);

            if (stationsById.containsKey(stationId)) {
                Route route = trip.getRoute();
                Station station = stationsById.get(stationId);
                station.addRoute(route);
                RouteStation routeStation = new RouteStation(station, route);
                if (!routeStations.containsKey(routeStation.getId())) {
                    routeStations.put(routeStation.getId(), routeStation);
                }
                byte stopSequence = Byte.parseByte(stopTimeData.getStopSequence());

                StopCall stop;
                if (route.isTram()) {
                    if (platforms.containsKey(platformId)) {
                        Platform platform = platforms.get(platformId);
                        platform.addRoute(route);
                    } else {
                        logger.error("Missing platform " +platformId);
                    }
                    Platform platform = platforms.get(platformId);
                    stop = new TramStopCall(platform, station, stopSequence, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime());
                } else
                {
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

    private void populateTrips(Stream<TripData> trips) {
        logger.info("Loading trips");
        AtomicInteger count = new AtomicInteger();

        trips.forEach((tripData) -> {
            String serviceId = tripData.getServiceId();
            String routeId = tripData.getRouteId();
            Route route = routes.get(routeId);

            if (route != null) {
                Service service = getOrInsertService(serviceId, route);
                Trip trip = getOrCreateTrip(tripData.getTripId(), tripData.getTripHeadsign(), service, route );
                count.getAndIncrement();
                service.addTrip(trip);
                route.addService(service);
                route.addHeadsign(trip.getHeadsign());
            } else {
                if (excludedRoutes.contains(routeId)) {
                    excludedTrips.add(tripData.getTripId());
                    if (!services.containsKey(serviceId)) {
                        excludedServices.add(serviceId);
                    }
                } else {
                    logger.warn(format("Unable to find RouteId '%s' for trip '%s", routeId, tripData));
                }
            }
        });
        logger.info("Loaded " + count.get());
    }

    private void populateAgencies(Stream<AgencyData> agencyDataStream) {
        logger.info("Loading agencies");
        agencyDataStream.forEach(agencyData -> agencies.put(agencyData.getId(), new Agency(agencyData.getId(), agencyData.getName())));
        logger.info("Loaded " + agencies.size());
    }

    private void populateRoutes(Stream<RouteData> routeDataStream) {
        logger.info("Loading routes");
        routeDataStream.forEach(routeData -> {
            String agencyId = routeData.getAgency();
            if (!agencies.containsKey(agencyId)) {
                logger.error("Missing agency " + agencyId);
            }

            GTFSTransportationType routeType = GTFSTransportationType.getType(routeData.getRouteType());

            if (GTFSTransportationType.supportedType(routeType)) {
                Agency agency = agencies.get(agencyId);
                Route route = new Route(routeData.getId(), routeData.getShortName().trim(), routeData.getLongName(), agency,
                        TransportMode.fromGTFS(routeType));
                routes.put(route.getId(), route);
                agencies.get(agencyId).addRoute(route);
            } else {
                excludedRoutes.add(routeData.getId());
                logger.info("Unsupported GTFS transport type: " + routeType + " agency:" + routeData.getAgency() + " routeId: " + routeData.getId());
            }
        });
        logger.info("Loaded " + routes.size() + " routes");
    }

    private void populateStationsAndAreas(Stream<StopData> stops) {
        logger.info("Loading stops");
        stops.forEach((stop) -> {
            String stopId = stop.getId();
            Station station;
            String stationId = Station.formId(stopId);

            if (!stationsById.containsKey(stationId)) {
                station = new Station(stationId, stop.getArea(), stop.getName(), stop.getLatLong(), stop.isTram());
                stationsById.put(stationId, station);
                stationsByName.put(stop.getName().toLowerCase(), station);
                stationLocations.addStation(station);
            } else {
                station = stationsById.get(stationId);
            }

            // TODO Trains?
            if (stop.isTram()) {
                Platform platform;
                if (!platforms.containsKey(stopId)) {
                    platform = formPlatform(stop);
                    platforms.put(stopId, platform);
                } else {
                    platform = platforms.get(stopId);
                }
                if (!station.getPlatforms().contains(platform)) {
                    station.addPlatform(platform);
                }
            }
        });
        logger.info("Loaded " + stationsById.size() + " stations " + platforms.size() + " tram platforms ");
    }

    private Platform formPlatform(StopData stop) {
        return new Platform(stop.getId(), stop.getName());
    }

    private Trip getOrCreateTrip(String tripId, String tripHeadsign, Service service, Route route) {
        if (trips.containsKey(tripId)) {
            Trip matched = trips.get(tripId);
            if ((!matched.getRoute().equals(route)) || !matched.getService().equals(service) || !matched.getHeadsign().equals(tripHeadsign)) {
                logger.error("Mismatch for trip id: " + tripId + " (mis)matched was " + matched);
            }
            return matched;
        }

        Trip trip = new Trip(tripId, tripHeadsign, service, route);
        trips.put(tripId, trip);
        return trip;
    }

    private Service getOrInsertService(String serviceId, Route route) {
        if (!services.containsKey(serviceId)) {
            services.put(serviceId, new Service(serviceId, route));
            excludedServices.remove(serviceId);
        }
        Service service = services.get(serviceId);
        service.addRoute(route);
        return service;
    }

    public Collection<Route> getRoutes() {
        return Collections.unmodifiableCollection(routes.values());
    }

    @Override
    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    public Trip getTrip(String tripId) {
        return trips.get(tripId);
    }

    public Set<Station> getStations() {
        return new HashSet<>(stationsById.values());
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
    public Set<RouteStation> getRouteStations() {
        return new HashSet<>(routeStations.values());
    }

    @Override
    public RouteStation getRouteStation(String routeStationId) {
        return routeStations.get(routeStationId);
    }

    @Deprecated
    @Override
    public FeedInfo getFeedInfo() {
        return feedInfo;
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
    public boolean hasStationId(String stationId) {
        return stationsById.containsKey(stationId);
    }

    @Override
    public Optional<Station> getStationByName(String name) {
        String lowerCase = name.toLowerCase();
        if (stationsByName.containsKey(lowerCase)) {
            return Optional.of(stationsByName.get(lowerCase));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Platform> getPlatformById(String platformId) {
        if (platforms.containsKey(platformId)) {
            return Optional.of(platforms.get(platformId));
        }
        return Optional.empty();
    }

    public Service getServiceById(String svcId) {
        if (!services.containsKey(svcId)) {
            logger.error("Unable to find service with id: " + svcId);
            throw new NoSuchElementException("Unable to find service " + svcId);
        }
        return services.get(svcId);
    }

    public Collection<Service> getServices() {
        return Collections.unmodifiableCollection(services.values());
    }

    public Collection<Trip> getTrips() {
        return Collections.unmodifiableCollection(trips.values());
    }

    @Override
    public Set<Service> getServicesOnDate(TramServiceDate date) {
        return services.values().stream().
                filter(svc -> svc.operatesOn(date.getDate())).collect(Collectors.toUnmodifiableSet());
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


}
