package com.tramchester.integration.testSupport;

import com.tramchester.ComponentContainer;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteEndRepository;
import com.tramchester.repository.StationRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RouteCalculationCombinations {

    private final GraphDatabase database;
    private final RouteCalculator calculator;
    private final StationRepository stationRepository;
    private final InterchangeRepository interchangeRepository;
    private final RouteEndRepository routeEndRepository;
    private final ClosedStationsRepository closedStationsRepository;

    public RouteCalculationCombinations(ComponentContainer componentContainer) {
        this.database = componentContainer.get(GraphDatabase.class);
        this.calculator = componentContainer.get(RouteCalculator.class);
        this.stationRepository = componentContainer.get(StationRepository.class);
        this.interchangeRepository = componentContainer.get(InterchangeRepository.class);
        routeEndRepository = componentContainer.get(RouteEndRepository.class);
        closedStationsRepository = componentContainer.get(ClosedStationsRepository.class);
    }

    public Optional<Journey> findJourneys(Transaction txn, IdFor<Station> start, IdFor<Station> dest, JourneyRequest journeyRequest) {
        return calculator.calculateRoute(txn, stationRepository.getStationById(start),
                stationRepository.getStationById(dest), journeyRequest)
                .limit(1).findAny();
    }

    public Map<JourneyRequest, JourneyOrNot> checkIfJourney(IdFor<Station> start, IdFor<Station> dest, List<JourneyRequest> requests) {
        return requests.stream().map(request ->  {
                try (Transaction txn = database.beginTx()) {
                    Optional<Journey> optionalJourney = findJourneys(txn, start, dest, request);
                    JourneyOrNot journeyOrNot = new JourneyOrNot(start, dest, request.getDate(), request.getOriginalTime(), optionalJourney);
                    return Pair.of(request, journeyOrNot);
                }
            }).collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        }


    private Map<StationIdPair, JourneyOrNot> validateAllHaveAtLeastOneJourney(Set<StationIdPair> stationIdPairs,
                                                                             JourneyRequest journeyRequest, boolean check) {

        long openPairs = stationIdPairs.stream().filter(stationIdPair -> bothOpen(stationIdPair, journeyRequest)).count();
        assertNotEquals(0, openPairs);

        Map<StationIdPair, JourneyOrNot> results = computeJourneys(stationIdPairs, journeyRequest);
        assertEquals(openPairs, results.size(), "Not enough results");

        // check all results present, collect failures into a list
        List<RouteCalculationCombinations.JourneyOrNot> failed = results.values().stream().
                filter(RouteCalculationCombinations.JourneyOrNot::missing).
                collect(Collectors.toList());

        // TODO This should be in the tests, not here
        if (check) {
            assertEquals(0L, failed.size(), format("For %s Failed some of %s (finished %s) combinations %s",
                    journeyRequest, results.size(), stationIdPairs.size(), displayFailed(failed)));
        }

        return results;
    }

    public Map<StationIdPair, JourneyOrNot> getJourneysFor(Set<StationIdPair> stationIdPairs, JourneyRequest journeyRequest) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, false);
    }

    @Deprecated
    public Map<StationIdPair, JourneyOrNot> validateAllHaveAtLeastOneJourney(Set<StationIdPair> stationIdPairs,
                                                                             JourneyRequest journeyRequest) {
        return validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest, true);
    }

    @NotNull
    private Map<StationIdPair, JourneyOrNot> computeJourneys(Set<StationIdPair> combinations, JourneyRequest request) {
        TramDate queryDate = request.getDate();
        TramTime queryTime = request.getOriginalTime();
        return combinations.
                parallelStream().
                filter(stationIdPair -> bothOpen(stationIdPair, request)).
                map(pair -> {
                    try (Transaction txn = database.beginTx()) {
                        Optional<Journey> optionalJourney = findJourneys(txn, pair.getBeginId(), pair.getEndId(), request);
                        JourneyOrNot journeyOrNot = new JourneyOrNot(pair, queryDate, queryTime, optionalJourney);
                        return Pair.of(pair, journeyOrNot);
                    }
                }).
                collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private boolean bothOpen(final StationIdPair stationIdPair, final JourneyRequest request) {
        final TramDate date = request.getDate();
        if (closedStationsRepository.hasClosuresOn(date)) {
            return ! (closedStationsRepository.isClosed(stationIdPair.getBeginId(), date) ||
                    closedStationsRepository.isClosed(stationIdPair.getEndId(), date));
        }
        return true;

    }

    public String displayFailed(List<JourneyOrNot> pairs) {
        StringBuilder stringBuilder = new StringBuilder();
        pairs.forEach(pair -> stringBuilder.append("[").
                append(pair.requested.getBeginId()).
                append(" to ").append(pair.requested.getEndId()).
                append("] "));
        return stringBuilder.toString();
    }

    public Set<StationIdPair> EndOfRoutesToInterchanges(TransportMode mode) {
        IdSet<Station> interchanges = getInterchangesFor(mode);
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(interchanges, endRoutes);
    }

    public Set<StationIdPair> InterchangeToInterchange(TransportMode mode) {
        IdSet<Station> interchanges = getInterchangesFor(mode);
        return createJourneyPairs(interchanges, interchanges);
    }

    public Set<StationIdPair> InterchangeToEndRoutes(TransportMode mode) {
        IdSet<Station> interchanges = getInterchangesFor(mode);
        IdSet<Station> endRoutes = routeEndRepository.getStations(mode);
        return createJourneyPairs(endRoutes, interchanges);
    }

    private IdSet<Station> getInterchangesFor(TransportMode mode) {
        return interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStationId).
                filter(stationId -> stationRepository.getStationById(stationId).servesMode(mode)).
                collect(IdSet.idCollector());
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

    public boolean betweenInterchanges(Station start, Station dest) {
        return interchangeRepository.isInterchange(start) && interchangeRepository.isInterchange(dest);
    }

    public Set<StationIdPair> getMissing(Map<StationIdPair, JourneyOrNot> results) {
        return results.entrySet().stream().
                filter(entry -> entry.getValue().missing()).
                map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public static class JourneyOrNot {
        private final StationIdPair requested;
        private final LocalDate queryDate;
        private final TramTime queryTime;
        private final Journey journey;

        public JourneyOrNot(StationIdPair requested, TramDate queryDate, TramTime queryTime, Optional<Journey> optionalJourney) {
            this(requested, queryDate.toLocalDate(), queryTime, optionalJourney);
        }

        @Deprecated
        public JourneyOrNot(IdFor<Station> start, IdFor<Station> dest, TramServiceDate date, TramTime time, Optional<Journey> optionalJourney) {
            this(StationIdPair.of(start, dest), date.getDate(), time, optionalJourney);
        }

        public JourneyOrNot(IdFor<Station> start, IdFor<Station> dest, TramDate date, TramTime time, Optional<Journey> optionalJourney) {
            this(StationIdPair.of(start, dest), date, time, optionalJourney);
        }

        public JourneyOrNot(StationIdPair requested, LocalDate queryDate, TramTime queryTime, Optional<Journey> optionalJourney) {
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

    }
}
