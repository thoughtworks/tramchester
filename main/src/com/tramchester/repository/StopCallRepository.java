package com.tramchester.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.metrics.CacheMetrics;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@LazySingleton
public class StopCallRepository implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(StopCallRepository.class);

    private final TripRepository tripRepository;
    private final StationRepository stationRepository;
    private final ServiceRepository serviceRepository;
    private final GraphFilterActive graphFilter;
    private final Map<Station, Set<StopCall>> stopCalls;
    private final Cache<CacheKey, Costs> cachedCosts;

    @Inject
    public StopCallRepository(TripRepository tripRepository, StationRepository stationRepository,
                              ServiceRepository serviceRepository, CacheMetrics cacheMetrics,
                              GraphFilterActive graphFilter) {
        this.tripRepository = tripRepository;
        this.stationRepository = stationRepository;
        this.serviceRepository = serviceRepository;
        this.graphFilter = graphFilter;
        stopCalls = new HashMap<>();
        cachedCosts = Caffeine.newBuilder().maximumSize(20000).expireAfterAccess(10, TimeUnit.MINUTES).
                recordStats().build();
        cacheMetrics.register(this);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        stationRepository.getAllStationStream().forEach(station -> stopCalls.put(station, new HashSet<>()));

        Set<Trip> allTrips = tripRepository.getTrips();

        Set<StopCall> missingStations = allTrips.stream().flatMap(trip -> trip.getStopCalls().stream()).
                filter(stopCall -> !stationRepository.hasStationId(stopCall.getStationId())).
                collect(Collectors.toSet());

        if (!missingStations.isEmpty()) {
            final String message = "Missing stations found in stopscall " + missingStations.size();
            logger.error(message);
            throw new RuntimeException(message);
        }

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
        cachedCosts.cleanUp();
    }

    // visualisation of frequency support
    public Set<StopCall> getStopCallsFor(Station station, LocalDate date, TramTime begin, TramTime end) {
        Set<Service> runningOnDate = serviceRepository.getServicesOnDate(date);
        Set<StopCall> callsForStation = stopCalls.get(station);

        return callsForStation.stream().
                filter(stopCall -> stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular)).
                filter(stopCall -> runningOnDate.contains(stopCall.getService())).
                filter(stopCall -> stopCall.getArrivalTime().between(begin, end)).
                collect(Collectors.toSet());
    }

    public Costs getCostsBetween(Route route, Station first, Station second) {
        CacheKey key = new CacheKey(route, first, second);
        return cachedCosts.get(key, id -> calculateCosts(route, first, second));
    }

    @NotNull
    private Costs calculateCosts(Route route, Station first, Station second) {
        List<Integer> allCosts = route.getTrips().stream().
                flatMap(trip -> trip.getStopCalls().getLegs(graphFilter.isFiltered()).stream()).
                filter(leg -> leg.getFirstStation().equals(first) && leg.getSecondStation().equals(second)).
                map(StopCalls.StopLeg::getCost).collect(Collectors.toList());

        if (allCosts.isEmpty()) {
            String msg = String.format("Found no costs (stop legs) for stations %s and %s on route %s",
                    first, second, route);
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        return new Costs(allCosts, route.getId(), first.getId(), second.getId());
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        return Collections.singletonList(Pair.of("CachedCosts", cachedCosts.stats()));
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

        // todo return duration
        @Deprecated
        public int max() {
            return costs.stream().mapToInt(item -> item).max().orElse(0);
        }

        // todo return duration
        @Deprecated
        public int average() {
            double avg = costs.stream().mapToInt(item -> item).average().orElse(0D);
            return (int)Math.round(avg);
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

    private static class CacheKey {
        private final Route route;
        private final Station first;
        private final Station second;

        public CacheKey(Route route, Station first, Station second) {

            this.route = route;
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (!route.equals(cacheKey.route)) return false;
            if (!first.equals(cacheKey.first)) return false;
            return second.equals(cacheKey.second);
        }

        @Override
        public int hashCode() {
            int result = route.hashCode();
            result = 31 * result + first.hashCode();
            result = 31 * result + second.hashCode();
            return result;
        }
    }
}
