package com.tramchester.repository;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.input.BusStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.Platform;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.dataimport.data.RouteData.BUS_TYPE;
import static com.tramchester.dataimport.data.RouteData.TRAM_TYPE;
import static java.lang.String.format;

public class TransportDataFromFiles implements TransportDataSource {
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
    private final StationLocations stationLocations;

    private final LinkedHashSet<AreaDTO> areas = new LinkedHashSet<>();
    private FeedInfo feedInfo = null;

    public TransportDataFromFiles(StationLocations stationLocations, Stream<StopData> stops, Stream<RouteData> rawRoutes,
                                  Stream<TripData> rawTrips, Stream<StopTimeData> stopTimes, Stream<CalendarData> calendars,
                                  Stream<FeedInfo> feedInfo, Stream<CalendarDateData> calendarsDates)  {
        this.transportDataStreams = new TransportDataStreams(stops, rawRoutes, rawTrips, stopTimes, calendars, feedInfo, calendarsDates);
        this.stationLocations = stationLocations;
        logger.info("Data load is complete");
    }

    @Override
    public void start() {
        logger.info("Loading transport data from files");

        Optional<FeedInfo> maybeFeedInfo = transportDataStreams.feedInfo.limit(1).findFirst();
        if (maybeFeedInfo.isPresent()) {
            this.feedInfo = maybeFeedInfo.get();
        } else {
            logger.warn("Did not find feedinfo");
        }

        populateStationsAndAreas(transportDataStreams.stops);
        populateRoutes(transportDataStreams.routes);
        populateTrips(transportDataStreams.trips);
        populateStopTimes(transportDataStreams.stopTimes);
        populateCalendars(transportDataStreams.calendars, transportDataStreams.calendarsDates);

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

    @Override
    public void stop() {

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
        areas.clear();
    }

    private void populateCalendars(Stream<CalendarData> calendars, Stream<CalendarDateData> calendarsDates) {
        calendars.forEach((calendar) -> {
            Service service = services.get(calendar.getServiceId());

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
                logger.warn("Failed to match service id " + calendar.getServiceId());
            }
        });
        calendarsDates.forEach(date -> {
            Service service = services.get(date.getServiceId());
            if (service != null) {
                service.addExceptionDate(date.getDate(), date.getExceptionType());
            } else {
                logger.warn("Failed to match service id " + date.getServiceId());
            }
        });
    }

    private void populateStopTimes(Stream<StopTimeData> stopTimes) {
        logger.info("Loading stop times");
        AtomicInteger count = new AtomicInteger();
        stopTimes.forEach((stopTimeData) -> {
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
                logger.warn(format("Unable to find RouteId '%s' for trip id '%s", routeId, tripData.getTripId()));
            }
        });
        logger.info("Loaded " + count.get());
    }

    private void populateRoutes(Stream<RouteData> routeDataStream) {
        logger.info("Loading routes and agencies");
        routeDataStream.forEach(routeData -> {
            String agencyId = routeData.getAgency();
            if (!agencies.containsKey(agencyId)) {
                logger.info("Adding agency " + agencyId);
                agencies.put(agencyId, new Agency(agencyId));
            }
            Agency agency = agencies.get(agencyId);
            Route route = new Route(routeData.getId(), routeData.getShortName().trim(), routeData.getLongName(), agency,
                    getMode(routeData.getRouteType()));
            routes.put(route.getId(), route);
            agencies.get(agencyId).addRoute(route);
        });
        logger.info("Loaded " + agencies.size() + " agencies " + routes.size() + " routes");
    }

    private TransportMode getMode(String routeType) {
        if (BUS_TYPE.equals(routeType)) {
            return TransportMode.Bus;
        }
        if (TRAM_TYPE.equals(routeType)) {
            return TransportMode.Tram;
        }
        throw new RuntimeException("Unexpected route type " + routeType);
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
            AreaDTO areaDTO = new AreaDTO(stop.getArea());
            areas.add(areaDTO); // a set, so no dups
        });
        logger.info("Loaded " + stationsById.size() + " stations " + platforms.size() + " tram platforms " +
                areas.size() + " areas");
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
        }
        Service matched = services.get(serviceId);
        if (matched.getRoute()!=route || matched.getId()!=serviceId) {
            logger.error("Mismatch on service id: " + serviceId);
        }
        return matched;
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
        Set<Station> stationList = new HashSet<>(stationsById.values());
        return stationList;
    }

    @Override
    public Collection<Agency> getAgencies() {
        return agencies.values();
    }

    @Override
    public Set<RouteStation> getRouteStations() {
        return new HashSet<>(routeStations.values());
    }

    @Override
    public RouteStation getRouteStation(String routeStationId) {
        return routeStations.get(routeStationId);
    }

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

    @Override
    public List<AreaDTO> getAreas() {
        return new LinkedList<>(areas);
    }

    public static class TransportDataStreams {
        final Stream<StopData> stops;
        final Stream<RouteData> routes;
        final Stream<TripData> trips;
        final Stream<StopTimeData> stopTimes;
        final Stream<CalendarData> calendars;
        final Stream<FeedInfo> feedInfo;
        final Stream<CalendarDateData> calendarsDates;

        public TransportDataStreams(Stream<StopData> stops, Stream<RouteData> routes, Stream<TripData> trips,
                                    Stream<StopTimeData> stopTimes, Stream<CalendarData> calendars,
                                    Stream<FeedInfo> feedInfo, Stream<CalendarDateData> calendarsDates) {
            this.stops = stops;
            this.routes = routes;
            this.trips = trips;
            this.stopTimes = stopTimes;
            this.calendars = calendars;
            this.feedInfo = feedInfo;
            this.calendarsDates = calendarsDates;
        }

        public void closeAll() {
            stops.close();
            routes.close();
            trips.close();
            stopTimes.close();
            calendars.close();
            feedInfo.close();
            calendarsDates.close();
        }
    }


}
