package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.RouteAndInterchanges;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.ServedRoute;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

@LazySingleton
public class StationAvailabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationAvailabilityRepository.class);

    private final Map<Location<?>, ServedRoute> pickupsForLocation;
    private final Map<Location<?>, ServedRoute> dropoffsForLocation;
    private final StationRepository stationRepository;

    @Inject
    public StationAvailabilityRepository(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
        pickupsForLocation = new HashMap<>();
        dropoffsForLocation = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        Set<Station> stations = stationRepository.getStations();
        stations.forEach(this::addForStation);
        logger.info(format("started, from %s stations add entries %s for pickups and %s for dropoff",
                stations.size(), pickupsForLocation.size(), dropoffsForLocation.size()));
    }

    private void addForStation(Station station) {
        addFor(dropoffsForLocation, station, station.getDropoffRoutes(), StopCall::getArrivalTime);
        addFor(pickupsForLocation, station, station.getPickupRoutes(), StopCall::getDepartureTime);
    }

    private void addFor(Map<Location<?>, ServedRoute> forLocation, Station station, Set<Route> routes, Function<StopCall, TramTime> getTime) {
        // TODO more efficient to extract time range directly here, rather than populate it inside of ServedRoute
        Set<Trip> trips = routes.stream().flatMap(route -> route.getTrips().stream()).collect(Collectors.toSet());

        Set<StopCall> stationStopCalls = trips.stream().
                map(Trip::getStopCalls).
                filter(stopCalls -> stopCalls.callsAt(station)).
                map(stopCalls -> stopCalls.getStopFor(station)).
                filter(StopCall::callsAtStation).
                collect(Collectors.toSet());

        stationStopCalls.forEach(stopCall -> addFor(forLocation, station, stopCall, getTime));
    }

    private void addFor(Map<Location<?>, ServedRoute> forLocation, Station station, StopCall stopCall, Function<StopCall, TramTime> getTime) {
        TramTime time = getTime.apply(stopCall);
        if (!time.isValid()) {
            logger.warn(format("Invalid time %s for %s %s", time, station.getId(), stopCall));
        }
        addFor(forLocation, station, stopCall.getTrip().getRoute(), stopCall.getService(), time);
    }


    private void addFor(Map<Location<?>, ServedRoute> place, Station station, Route route, Service service, TramTime time) {
        if (!place.containsKey(station)) {
            place.put(station, new ServedRoute());
        }
        place.get(station).add(route, service, time);
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        pickupsForLocation.clear();
        dropoffsForLocation.clear();
        logger.info("Stopped");
    }
    
    public boolean isAvailable(Location<?> location, LocalDate when, TimeRange timeRange) {

        return pickupsForLocation.get(location).anyAvailable(when, timeRange) &&
                dropoffsForLocation.get(location).anyAvailable(when, timeRange);
    }

    public boolean isAvailable(RouteAndInterchanges routeAndInterchanges, LocalDate date, TimeRange time) {
        if (routeAndInterchanges.getRoutePair().isAvailableOn(date)) {
            return routeAndInterchanges.getInterchangeStations().stream().
                    anyMatch(station -> isAvailable(station, date, time));
        }
        return false;
    }


    public Set<Route> getPickupRoutesFor(Location<?> location, LocalDate date, TimeRange timeRange) {
        return pickupsForLocation.get(location).getRoutes(date, timeRange);
    }

    public Set<Route> getDropoffRoutesFor(Location<?> location, LocalDate date, TimeRange timeRange) {
        return dropoffsForLocation.get(location).getRoutes(date, timeRange);
    }

    public Set<Route> getPickupRoutesFor(LocationSet locations, LocalDate date, TimeRange timeRange) {
        return locations.stream().flatMap(location -> getPickupRoutesFor(location, date, timeRange).stream()).collect(Collectors.toSet());
    }

    public Set<Route> getDropoffRoutesFor(LocationSet locations, LocalDate date, TimeRange timeRange) {
        return locations.stream().flatMap(location -> getDropoffRoutesFor(location, date, timeRange).stream()).collect(Collectors.toSet());
    }

}
