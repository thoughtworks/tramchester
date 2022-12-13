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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class StationAvailabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationAvailabilityRepository.class);

    private final Map<Location<?>, ServedRoute> pickupsForLocation;
    private final Map<Location<?>, ServedRoute> dropoffsForLocation;
    private final StationRepository stationRepository;
    private final ClosedStationsRepository closedStationsRepository;
    private final GraphFilterActive graphFilterActive;

    @Inject
    public StationAvailabilityRepository(StationRepository stationRepository, ClosedStationsRepository closedStationsRepository, GraphFilterActive graphFilterActive) {
        this.stationRepository = stationRepository;
        this.closedStationsRepository = closedStationsRepository;
        this.graphFilterActive = graphFilterActive;
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
        boolean graphFilter = graphFilterActive.isActive();
        if (!addFor(dropoffsForLocation, station, station.getDropoffRoutes(), StopCall::getArrivalTime)) {
            if (!graphFilter) {
                logger.info("No dropoffs for " + station.getId());
            }
            dropoffsForLocation.put(station, new ServedRoute());
        }
        if (!addFor(pickupsForLocation, station, station.getPickupRoutes(), StopCall::getDepartureTime)) {
            if (!graphFilter) {
                logger.info("No pickups for " + station.getId());
            }
            pickupsForLocation.put(station, new ServedRoute());
        }
    }

    private boolean addFor(Map<Location<?>, ServedRoute> forLocation, Station station, Set<Route> routes, Function<StopCall, TramTime> getTime) {
        // TODO more efficient to extract time range directly here, rather than populate it inside of ServedRoute
        Set<Trip> trips = routes.stream().flatMap(route -> route.getTrips().stream()).collect(Collectors.toSet());

        Stream<StopCall> stationStopCalls = trips.stream().
                map(Trip::getStopCalls).
                filter(stopCalls -> stopCalls.callsAt(station)).
                map(stopCalls -> stopCalls.getStopFor(station)).
                filter(StopCall::callsAtStation);

        int added = stationStopCalls.
                mapToInt(stopCall -> addFor(forLocation, station, stopCall, getTime)).sum();

        return added > 0;

    }

    private int addFor(Map<Location<?>, ServedRoute> forLocation, Station station, StopCall stopCall, Function<StopCall, TramTime> getTime) {
        TramTime time = getTime.apply(stopCall);
        if (!time.isValid()) {
            logger.warn(format("Invalid time %s for %s %s", time, station.getId(), stopCall));
            return 0;
        }
        addFor(forLocation, station, stopCall.getTrip().getRoute(), stopCall.getService(), time);
        return 1;
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
    
    public boolean isAvailable(Location<?> location, TramDate when, TimeRange timeRange, Set<TransportMode> requestedModes) {
        if (!pickupsForLocation.containsKey(location)) {
            throw new RuntimeException("Missing pickups for " + location.getId());
        }
        if (!dropoffsForLocation.containsKey(location)) {
            throw new RuntimeException("Missing dropoffs for " + location.getId());
        }
        return pickupsForLocation.get(location).anyAvailable(when, timeRange, requestedModes) &&
                dropoffsForLocation.get(location).anyAvailable(when, timeRange, requestedModes);
    }

    private boolean isAvailable(InterchangeStation interchangeStation, TramDate date, TimeRange timeRange, Set<TransportMode> requestedModes) {
        if (interchangeStation.getType()==InterchangeType.NeighbourLinks) {
            return isAvailable((LinkedInterchangeStation) interchangeStation, date, timeRange, requestedModes);
        }
        return isAvailable(interchangeStation.getStation(), date, timeRange, requestedModes);
    }

    private boolean isAvailable(LinkedInterchangeStation linkedInterchangeStation, TramDate date, TimeRange timeRange, Set<TransportMode> requestedModes) {
        ServedRoute servedRoute = dropoffsForLocation.get(linkedInterchangeStation.getStation());
        if (servedRoute.anyAvailable(date, timeRange, requestedModes)) {
            // origin half of the linked station matches the requirements
            return true;
        }
        // if not a match at the origin now check the "far end" linked stations
        return linkedInterchangeStation.getLinked().stream().
                filter(station -> TransportMode.intersects(station.getTransportModes(), requestedModes)).
                map(pickupsForLocation::get).
                anyMatch(linkedServedRoute -> linkedServedRoute.anyAvailable(date, timeRange, requestedModes));
        //return false;
    }

    public boolean isAvailable(RouteAndChanges routeAndChanges, TramDate date, TimeRange time, Set<TransportMode> requestedModes) {
        if (routeAndChanges.getRoutePair().bothAvailableOn(date)) {
            return routeAndChanges.getInterchangeStations().stream().
                    anyMatch(interchangeStation -> isAvailable(interchangeStation, date, time, requestedModes));
        }
        return false;
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

}
