package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilterActive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class StationAvailabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationAvailabilityRepository.class);

    // NOTE: use routes here since they tend to have specific times ranges, whereas services end up 24x7 some stations
    private final Map<Location<?>, ServedRoute> pickupsForLocation;
    private final Map<Location<?>, ServedRoute> dropoffsForLocation;
    private final StationRepository stationRepository;
    private final ClosedStationsRepository closedStationsRepository;
    private final GraphFilterActive graphFilterActive;
    private final TripRepository tripRepository;

    private final Map<Location<?>, Set<Service>> servicesForLocation;

    @Inject
    public StationAvailabilityRepository(StationRepository stationRepository, ClosedStationsRepository closedStationsRepository,
                                         GraphFilterActive graphFilterActive, TripRepository tripRepository) {
        this.stationRepository = stationRepository;
        this.closedStationsRepository = closedStationsRepository;
        this.graphFilterActive = graphFilterActive;
        this.tripRepository = tripRepository;
        pickupsForLocation = new HashMap<>();
        dropoffsForLocation = new HashMap<>();

        servicesForLocation = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        // OLD
        Set<Station> stations = stationRepository.getStations();
        stations.forEach(this::addForStation);
        // NEW
        addServicesForStations();

        logger.info(format("started, from %s stations add entries %s for pickups and %s for dropoff",
                stations.size(), pickupsForLocation.size(), dropoffsForLocation.size()));
    }

    private void addServicesForStations() {
        stationRepository.getStations().forEach(station -> servicesForLocation.put(station, new HashSet<>()));
        tripRepository.getTrips().forEach(trip -> trip.getStopCalls().stream().
                filter(StopCall::callsAtStation).
                forEach(stopCall -> {
                    servicesForLocation.get(stopCall.getStation()).add(stopCall.getService());
                }));
    }

    private void addForStation(Station station) {
        boolean graphFilter = graphFilterActive.isActive();

        if (!addFor(dropoffsForLocation, station, station.getDropoffRoutes(), StopCall::getArrivalTime)) {
            if (!graphFilter) {
                logger.info("No dropoffs for " + station.getId());
            }
            dropoffsForLocation.put(station, new ServedRoute()); // empty
        }
        if (!addFor(pickupsForLocation, station, station.getPickupRoutes(), StopCall::getDepartureTime)) {
            if (!graphFilter) {
                logger.info("No pickups for " + station.getId());
            }
            pickupsForLocation.put(station, new ServedRoute()); // empty
        }
    }

    private boolean addFor(Map<Location<?>, ServedRoute> forLocations, Station station, Set<Route> routes, Function<StopCall, TramTime> getTime) {
        // TODO more efficient to extract time range directly here, rather than populate it inside of ServedRoute
        Set<Trip> trips = routes.stream().flatMap(route -> route.getTrips().stream()).collect(Collectors.toSet());

        Stream<StopCall> stationStopCalls = trips.stream().
                map(Trip::getStopCalls).
                filter(stopCalls -> stopCalls.callsAt(station)).
                map(stopCalls -> stopCalls.getStopFor(station)).
                filter(StopCall::callsAtStation);

        int added = stationStopCalls.
                mapToInt(stopCall -> addFor(forLocations, station, stopCall, getTime)).sum();

        return added > 0;

    }

    private int addFor(Map<Location<?>, ServedRoute> forLocations, Station station, StopCall stopCall, Function<StopCall, TramTime> getTime) {
        TramTime time = getTime.apply(stopCall);
        if (!time.isValid()) {
            logger.warn(format("Invalid time %s for %s %s", time, station.getId(), stopCall));
            return 0;
        }
        addFor(forLocations, station, stopCall.getTrip().getRoute(), stopCall.getService(), time);
        return 1;
    }

    private void addFor(Map<Location<?>, ServedRoute> forLocations, Station station, Route route, Service service, TramTime time) {
        if (!forLocations.containsKey(station)) {
            forLocations.put(station, new ServedRoute());
        }
        forLocations.get(station).add(route, service, time);
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        pickupsForLocation.clear();
        dropoffsForLocation.clear();
        logger.info("Stopped");
    }

    public boolean isAvailable(final Location<?> location, final TramDate date, final TimeRange timeRange,
                               final EnumSet<TransportMode> requestedModes) {
        if (!pickupsForLocation.containsKey(location)) {
            throw new RuntimeException("Missing pickups for " + location.getId());
        }
        if (!dropoffsForLocation.containsKey(location)) {
            throw new RuntimeException("Missing dropoffs for " + location.getId());
        }

        final Set<Service> services = servicesForLocation.get(location).stream().
                filter(service -> TransportMode.intersects(requestedModes, service.getTransportModes())).
                collect(Collectors.toSet());

        if (services.isEmpty()) {
            logger.warn("Found no services for " + location.getId() + " and " + requestedModes);
            return false;
        }

        // TODO is this worth it?
        boolean onDate = services.stream().anyMatch(service -> service.getCalendar().operatesOn(date));
        if (!onDate) {
            return false;
        }

        return pickupsForLocation.get(location).anyAvailable(date, timeRange, requestedModes) &&
                dropoffsForLocation.get(location).anyAvailable(date, timeRange, requestedModes);
    }

    public Set<Route> getPickupRoutesFor(Location<?> location, TramDate date, TimeRange timeRange, Set<TransportMode> modes) {
        if (closedStationsRepository.isClosed(location, date)) {
            ClosedStation closedStation = closedStationsRepository.getClosedStation(location, date);
            return getPickupRoutesFor(closedStation, date, timeRange, modes);
        }
        return pickupsForLocation.get(location).getRoutes(date, timeRange, modes);
    }

    public Set<Route> getDropoffRoutesFor(Location<?> location, TramDate date, TimeRange timeRange, Set<TransportMode> modes) {
        if (closedStationsRepository.isClosed(location, date)) {
            ClosedStation closedStation = closedStationsRepository.getClosedStation(location, date);
            return getDropoffRoutesFor(closedStation, date,timeRange, modes);
        }
        return dropoffsForLocation.get(location).getRoutes(date, timeRange, modes);
    }

    private Set<Route> getDropoffRoutesFor(ClosedStation closedStation, TramDate date, TimeRange timeRange, Set<TransportMode> modes) {
        logger.warn(closedStation.getStationId() + " is closed, using linked stations for dropoffs");
        return closedStation.getNearbyLinkedStation().stream().
                flatMap(linked -> dropoffsForLocation.get(linked).getRoutes(date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public Set<Route> getPickupRoutesFor(LocationSet locations, TramDate date, TimeRange timeRange, Set<TransportMode> modes) {
        return locations.stream().
                flatMap(location -> getPickupRoutesFor(location, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    private Set<Route> getPickupRoutesFor(ClosedStation closedStation, TramDate date, TimeRange timeRange, Set<TransportMode> modes) {
        logger.warn(closedStation.getStationId() + " is closed, using linked stations for pickups");
        return closedStation.getNearbyLinkedStation().stream().
                flatMap(linked -> pickupsForLocation.get(linked).getRoutes(date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public Set<Route> getDropoffRoutesFor(LocationSet locations, TramDate date, TimeRange timeRange, Set<TransportMode> modes) {
        return locations.stream().
                flatMap(location -> getDropoffRoutesFor(location, date, timeRange, modes).stream()).
                collect(Collectors.toSet());
    }

    public long size() {
        return pickupsForLocation.size() + dropoffsForLocation.size();
    }

//    private static class LocationService {
//
//        private final Location<?> location;
//        private final Service service;
//
//        public LocationService(Location<?> location, Service service) {
//            this.location = location;
//            this.service = service;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            LocationService that = (LocationService) o;
//            return location.equals(that.location) && service.equals(that.service);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(location, service);
//        }
//
//        @Override
//        public String toString() {
//            return "LocationService{" +
//                    "location=" + location.getId() +
//                    ", service=" + service.getId() +
//                    '}';
//        }
//    }

}
