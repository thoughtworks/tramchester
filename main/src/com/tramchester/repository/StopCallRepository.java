package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@LazySingleton
public class StopCallRepository {
    private static final Logger logger = LoggerFactory.getLogger(StopCallRepository.class);

    private final TripRepository tripRepository;
    private final StationRepository stationRepository;
    private final ServiceRepository serviceRepository;
    private final Map<Station, Set<StopCall>> stopCalls;

    @Inject
    public StopCallRepository(TripRepository tripRepository, StationRepository stationRepository, ServiceRepository serviceRepository) {
        this.tripRepository = tripRepository;
        this.stationRepository = stationRepository;
        this.serviceRepository = serviceRepository;
        stopCalls = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        stationRepository.getStationStream().forEach(station -> stopCalls.put(station, new HashSet<>()));

        Set<Trip> allTrips = tripRepository.getTrips();
        allTrips.stream().
                flatMap(trip -> trip.getStopCalls().stream()).
                forEach(stopCall -> stopCalls.get(stopCall.getStation()).add(stopCall));

        long noStops = stopCalls.entrySet().stream().
                filter(entry -> entry.getValue().isEmpty()).
                count();

        logger.info("Added stopcalls for " + (stopCalls.size() - noStops) + " stations");
        if (noStops > 0) {
            logger.warn(noStops + " stations have no StopCalls");
        }
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        stopCalls.clear();
    }

    // visualisation of frequency support
    public Set<StopCall> getStopCallsFor(Station station, LocalDate date, TramTime begin, TramTime end) {
        IdSet<Service> runningOnDate = serviceRepository.getServicesOnDate(date);
        Set<StopCall> allForStation = stopCalls.get(station);

        return allForStation.stream().
                filter(stopCall -> stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular)).
                filter(stopCall -> runningOnDate.contains(stopCall.getServiceId())).
                filter(stopCall -> stopCall.getArrivalTime().between(begin, end)).
                collect(Collectors.toSet());
    }

    public Costs getCostsBetween(Route route, Station first, Station second) {

        List<Integer> allCosts = route.getTrips().stream().flatMap(trip -> trip.getStopCalls().getLegs().stream()).
                filter(leg -> leg.getFirstStation().equals(first) && leg.getSecondStation().equals(second)).
                map(StopCalls.StopLeg::getCost).collect(Collectors.toList());

        if (allCosts.isEmpty()) {
            String msg = String.format("Found no costs (stop legs) for stations %s and %s on route %s",
                    first, second, route);
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        final Costs costs = new Costs(allCosts, route.getId(), first.getId(), second.getId());

        return costs;
    }

    public static class Costs {

        private final List<Integer> costs;
        private final IdFor<Route> route;
        private final IdFor<Station> startId;
        private final IdFor<Station> endId;

        public Costs(List<Integer> costs, IdFor<Route> route, IdFor<Station> startId, IdFor<Station> endId) {
            this.costs = costs;
            this.route = route;
            this.startId = startId;
            this.endId = endId;
        }

        public int min() {
            return costs.stream().mapToInt(item -> item).min().orElse(0);
        }

        public int average() {
            double avg = costs.stream().mapToInt(item -> item).average().orElse(0D);
            return (int)Math.ceil(avg);
        }

        @Override
        public String toString() {
            return "Costs{" +
                    " route=" + route +
                    ", startId=" + startId +
                    ", endId=" + endId +
                    ", costs=" + costs +
                    '}';
        }

        public boolean isEmpty() {
            return costs.isEmpty();
        }

        public boolean consistent() {
            return costs.stream().distinct().count()==1L;
        }

    }
}
