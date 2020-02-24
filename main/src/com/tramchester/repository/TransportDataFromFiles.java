package com.tramchester.repository;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.Platform;
import com.tramchester.domain.time.DaysOfWeek;
import com.tramchester.domain.time.TramServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class TransportDataFromFiles implements TransportDataSource {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFiles.class);
    private static final String BUS_TYPE = "3";
    private static final String TRAM_TYPE = "0";

    private HashMap<String, Trip> trips = new HashMap<>();        // trip id -> trip
    private HashMap<String, Station> stationsById = new HashMap<>();  // station id -> station
    private HashMap<String, Station> stationsByName = new HashMap<>();  // station id -> station
    private HashMap<String, Service> services = new HashMap<>();  // service id -> service
    private HashMap<String, Route> routes = new HashMap<>();      // route id -> route
    private HashMap<String, Platform> platforms = new HashMap<>(); // platformId -> platform
    private HashMap<String, RouteStation> routeStations = new HashMap<>(); // routeStationId - > RouteStation
    private Set<String> agencies = new HashSet<>(); // agencies

    private LinkedHashSet<AreaDTO> areas = new LinkedHashSet<>();
    private FeedInfo feedInfo = null;

    public TransportDataFromFiles(Stream<StopData> stops, Stream<RouteData> rawRoutes, Stream<TripData> rawTrips,
                                  Stream<StopTimeData> stopTimes, Stream<CalendarData> calendars,
                                  Stream<FeedInfo> feedInfo)  {
        logger.info("Loading transport data from files");
        Optional<FeedInfo> maybeFeedInfo = feedInfo.limit(1).findFirst();
        if (maybeFeedInfo.isPresent()) {
            this.feedInfo = maybeFeedInfo.get();
        } else {
            logger.warn("Did not find feedinfo");
        }

        populateStationsAndAreas(stops);
        populateRoutes(rawRoutes);
        populateTrips(rawTrips);
        populateStopTimes(stopTimes);
        populateCalendars(calendars);

        logger.info(format("%s stations", stationsById.size()));
        logger.info(format("%s routes", this.routes.size()));
        logger.info(format("%s services", services.size()));
        logger.info(format("%s trips", this.trips.size()));

        // update svcs where calendar data is missing
        services.values().stream().filter(svc -> svc.getDays().get(DaysOfWeek.Monday) == null).forEach(svc -> {
            logger.warn(format("Service %s is missing calendar information", svc.getServiceId()));
            svc.setDays(false, false, false, false, false, false, false);
        });
        services.values().stream().filter(svc -> !svc.getDays().values().contains(true)).forEach(
                svc -> logger.warn(format("Service %s does not run on any days of the week", svc.getServiceId()))
        );

        logger.info("Data load is complete");
    }

    private void populateCalendars(Stream<CalendarData> calendars) {
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
            }
        });
    }

    private void populateStopTimes(Stream<StopTimeData> stopTimes) {
        stopTimes.forEach((stopTimeData) -> {
            Trip trip = getTrip(stopTimeData.getTripId());

            String stopId = stopTimeData.getStopId();
            String stationId = Station.formId(stopId);
            if (stationsById.containsKey(stationId)) {
                Route route = trip.getRoute(); //routes.get(trip.getRouteId());
                Station station = stationsById.get(stationId);
                station.addRoute(route);
                RouteStation routeStation = new RouteStation(station, route);
                if (!routeStations.containsKey(routeStation.getId())) {
                    routeStations.put(routeStation.getId(), routeStation);
                }
                if (platforms.containsKey(stopId)) {
                    Platform platform = platforms.get(stopId);
                    platform.addRoute(route);
                }

                int stopSequence = Integer.parseInt(stopTimeData.getStopSequence());
                Stop stop = new Stop(stopId, station, stopSequence, stopTimeData.getArrivalTime(), stopTimeData.getDepartureTime());
                trip.addStop(stop);
            } else {
                logger.warn(format("Cannot find station for Id '%s' for stopId '%s'", stationId, stopId));
            }
        });
    }

    private void populateTrips(Stream<TripData> trips) {
        trips.forEach((tripData) -> {
            String serviceId = tripData.getServiceId();
            String routeId = tripData.getRouteId();
            Route route = routes.get(routeId);

            Service service = getOrInsertService(serviceId, routeId);
            Trip trip = getOrCreateTrip(tripData.getTripId(), tripData.getTripHeadsign(), serviceId, route );
            if (route != null) {
                service.addTrip(trip);
                route.addService(service);
                route.addHeadsign(trip.getHeadsign());
            } else {
                logger.warn(format("Unable to find RouteId '%s' for trip '%s", routeId, trip));
            }
        });
    }

    private void populateRoutes(Stream<RouteData> routes) {
        routes.forEach((routeData) -> {
            String agency = routeData.getAgency();
            Route route = new Route(routeData.getId(), routeData.getShortName(), routeData.getLongName(), agency,
                    getMode(routeData.getRouteType()));
            this.routes.put(route.getId(), route);
            if (!agencies.contains(agency)) {
                agencies.add(agency);
            }
        });
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
        stops.forEach((stop) -> {
            String stopId = stop.getId();
            Station station;
            String stationId = Station.formId(stopId);

            if (!stationsById.containsKey(stationId)) {
                station = new Station(stationId, stop.getArea(), stop.getName(), stop.getLatLong(), stop.isTram());
                stationsById.put(stationId, station);
                stationsByName.put(stop.getName().toLowerCase(), station);
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
            if (!areas.contains(areaDTO)) {
                areas.add(areaDTO);
            }
        });
    }

    private Platform formPlatform(StopData stop) {
        return new Platform(stop.getId(), stop.getName());
    }

    private Trip getOrCreateTrip(String tripId, String tripHeadsign, String serviceId, Route route) {
        if (!trips.keySet().contains(tripId)) {
            trips.put(tripId, new Trip(tripId, tripHeadsign, serviceId, route));
        }

        Trip matched = trips.get(tripId);
        if ((!matched.getRoute().equals(route)) || matched.getServiceId()!=serviceId || matched.getHeadsign()!=tripHeadsign) {
            logger.error("Mismatch on trip id: " + tripId);
        }
        return matched;
    }

    private Service getOrInsertService(String serviceId, String routeId) {
        if (!services.keySet().contains(serviceId)) {
            services.put(serviceId, new Service(serviceId, routeId));
        }
        Service matched = services.get(serviceId);
        if (matched.getRouteId()!=routeId || matched.getServiceId()!=serviceId) {
            logger.error("Mismatch on service id: " + serviceId);
        }
        return matched;
    }

    public Collection<Route> getRoutes() {
        return Collections.unmodifiableCollection(routes.values());
    }

    @Override
    public Stream<Trip> getTripsByRoute(Route route) {
        return trips.values().stream().filter(t->t.getRoute().equals(route));
    }

    @Override
    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    public Trip getTrip(String tripId) {
        return trips.get(tripId);
    }

    public Set<Station> getStations() {
        Set<Station> stationList = new HashSet<>();
        stationList.addAll(stationsById.values());
        return stationList;
    }

    @Override
    public Set<String> getAgencies() {
        return agencies;
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
    public Optional<Station> getStation(String stationId) {
        if (stationsById.containsKey(stationId)) {
            return Optional.of(stationsById.get(stationId));
        } else {
            logger.warn("Unable to find station with ID:"+stationId);
            return Optional.empty();
        }
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
        DaysOfWeek day = date.getDay();
        return Collections.unmodifiableSet(services.values().stream().
                filter(svc -> svc.getDays().get(day)).
                filter(svc -> svc.operatesOn(date.getDate())).collect(Collectors.toSet()));

    }

    public Set<Trip> getTripsFor(String stationId) {
        Set<Trip> callingTrips = new HashSet<>();
        trips.values().forEach(trip -> {
            if (trip.callsAt(stationId)) {
                callingTrips.add(trip);
            }
        });
        return callingTrips;
    }

    @Override
    public List<AreaDTO> getAreas() {
        List<AreaDTO> list = new LinkedList<>(areas);
        return  list;
    }

}
