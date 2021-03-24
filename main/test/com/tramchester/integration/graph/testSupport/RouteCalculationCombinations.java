package com.tramchester.integration.graph.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteEndRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.domain.StationIdPair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RouteCalculationCombinations {

    private final TramchesterConfig config;
    private final GraphDatabase database;
    private final RouteCalculator calculator;
    private final StationRepository stationRepository;
    private final InterchangeRepository interchangeRepository;
    private final RouteEndRepository routeEndRepository;

    public RouteCalculationCombinations(ComponentContainer componentContainer, TramchesterConfig config) {
        this.config = config;
        this.database = componentContainer.get(GraphDatabase.class);
        this.calculator = componentContainer.get(RouteCalculator.class);
        this.stationRepository = componentContainer.get(StationRepository.class);
        this.interchangeRepository = componentContainer.get(InterchangeRepository.class);
        routeEndRepository = componentContainer.get(RouteEndRepository.class);
    }

    public Optional<Journey> findJourneys(Transaction txn, IdFor<Station> start, IdFor<Station> dest, JourneyRequest journeyRequest) {
        return calculator.calculateRoute(txn, stationRepository.getStationById(start),
                stationRepository.getStationById(dest), journeyRequest).limit(1).findAny();
    }

    public Map<StationIdPair, JourneyOrNot> validateAllHaveAtLeastOneJourney(
            LocalDate queryDate, Set<StationIdPair> stationIdPairs, TramTime queryTime) {

        Map<StationIdPair, JourneyOrNot> results = computeJourneys(queryDate, stationIdPairs, queryTime);
        assertEquals(stationIdPairs.size(), results.size(), "Not enough results");

        // check all results present, collect failures into a list
        List<RouteCalculationCombinations.JourneyOrNot> failed = results.values().stream().
                filter(RouteCalculationCombinations.JourneyOrNot::missing).
                collect(Collectors.toList());

        assertEquals(0L, failed.size(), format("Failed some of %s (finished %s) combinations %s",
                        results.size(), stationIdPairs.size(), displayFailed(failed)));

        return results;
    }

    public Map<StationIdPair, JourneyOrNot> validateAllHaveAtLeastOneJourney(Set<StationIdPair> stationIdPairs,
                                                                             JourneyRequest journeyRequest) {

        Map<StationIdPair, JourneyOrNot> results = computeJourneys(stationIdPairs, journeyRequest);
        assertEquals(stationIdPairs.size(), results.size(), "Not enough results");

        // check all results present, collect failures into a list
        List<RouteCalculationCombinations.JourneyOrNot> failed = results.values().stream().
                filter(RouteCalculationCombinations.JourneyOrNot::missing).
                collect(Collectors.toList());

        assertEquals(0L, failed.size(), format("Failed some of %s (finished %s) combinations %s",
                results.size(), stationIdPairs.size(), displayFailed(failed)));

        return results;
    }

    /***
     * use the version that takes journey request instead
     */
    @Deprecated
    @NotNull
    private Map<StationIdPair, JourneyOrNot> computeJourneys(LocalDate queryDate, Set<StationIdPair> combinations, TramTime queryTime) {
        JourneyRequest request = new JourneyRequest(new TramServiceDate(queryDate), queryTime, false, 3,
                config.getMaxJourneyDuration());

        return computeJourneys(combinations, request);
    }

    @NotNull
    private Map<StationIdPair, JourneyOrNot> computeJourneys(Set<StationIdPair> combinations, JourneyRequest request) {
        LocalDate queryDate = request.getDate().getDate();
        TramTime queryTime = request.getTime();
        return combinations.parallelStream().
                map(requested -> {
                    try (Transaction txn = database.beginTx()) {
                        Optional<Journey> optionalJourney = findJourneys(txn, requested.getBeginId(), requested.getEndId(), request);
                        JourneyOrNot journeyOrNot = new JourneyOrNot(requested, queryDate, queryTime, optionalJourney);
                        return Pair.of(requested, journeyOrNot);
                    }
                }).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private String displayFailed(List<JourneyOrNot> pairs) {
        StringBuilder stringBuilder = new StringBuilder();
        pairs.forEach(pair -> stringBuilder.append("[").
                append(pair.requested.getEndId()).
                append(" to ").append(pair.requested.getEndId()).
                append("] "));
        return stringBuilder.toString();
    }

    public Set<StationIdPair> EndOfRoutesToInterchanges(TransportMode mode) {
        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(mode);
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(interchanges, endRoutes);
    }

    public Set<StationIdPair> InterchangeToInterchange(TransportMode mode) {
        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(mode);
        return createJourneyPairs(interchanges, interchanges);
    }

    public Set<StationIdPair> InterchangeToEndRoutes(TransportMode mode) {
        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(mode);
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(endRoutes, interchanges);
    }

    public Set<StationIdPair> EndOfRoutesToEndOfRoutes(TransportMode mode) {
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(endRoutes, endRoutes);
    }

    private Set<StationIdPair> createJourneyPairs(IdSet<Station> starts, IdSet<Station> ends) {
        Set<StationIdPair> combinations = new HashSet<>();
        for (IdFor<Station> start : starts) {
            for (IdFor<Station> dest : ends) {
                if (!dest.equals(start)) {
                    combinations.add(new StationIdPair(start, dest));
                }
            }
        }
        return combinations;
    }

    public boolean betweenInterchange(StationIdPair pair) {
        return interchangeRepository.isInterchange(pair.getBeginId()) && interchangeRepository.isInterchange(pair.getEndId());
    }

    public boolean betweenEndsOfRoute(StationIdPair pair) {
        return routeEndRepository.isEndRoute(pair.getBeginId()) && routeEndRepository.isEndRoute(pair.getEndId());
    }

    public static class JourneyOrNot {
        private final StationIdPair requested;
        private final LocalDate queryDate;
        private final TramTime queryTime;
        private final Journey journey;

        public JourneyOrNot(StationIdPair requested, LocalDate queryDate, TramTime queryTime,
                            Optional<Journey> optionalJourney) {
            this.requested = requested;
            this.queryDate = queryDate;
            this.queryTime = queryTime;
            this.journey = optionalJourney.orElse(null);
        }

        public boolean missing() {
            return journey==null;
        }

        public void ifPresent(Consumer<Journey> action) {
            if (this.journey != null) {
                action.accept(this.journey);
            }
        }

        @Override
        public String toString() {
            return "JourneyOrNot{" +
                    " queryDate=" + queryDate +
                    ", queryTime=" + queryTime +
                    ", from=" + requested.getBeginId() +
                    ", to=" + requested.getEndId() +
                    '}';
        }

        public Journey get() {
            return journey;
        }
    }
}
