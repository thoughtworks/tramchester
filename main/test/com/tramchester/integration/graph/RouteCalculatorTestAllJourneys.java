package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.Dependencies;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.lang.String.format;

class RouteCalculatorTestAllJourneys {

    private static ComponentContainer componentContainer;
    private static GraphDatabase database;

    private static final boolean circleCi = TestEnv.isCircleci();
    private static IntegrationTramTestConfig testConfig;

    private RouteCalculator calculator;
    private final LocalDate when = TestEnv.testDay();

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new Dependencies();
        testConfig = new IntegrationTramTestConfig();
        componentContainer.initialise(testConfig);
        database = componentContainer.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        calculator = componentContainer.get(RouteCalculator.class);
    }

    @Test
    void shouldFindRouteEachStationToEveryOtherStream() {
        Assumptions.assumeFalse(circleCi);

        TransportData data = componentContainer.get(TransportData.class);

        Set<Station> allStations = data.getStations();

        // pairs of stations to check
        Set<Pair<Station, Station>> combinations = allStations.stream().flatMap(start -> allStations.stream().
                map(dest -> Pair.of(start, dest))).
                filter(pair -> !pair.getRight().getId().equals(pair.getLeft().getId())).
                filter(pair -> !interchanes(pair)).
                filter(pair -> !endsOfLine(pair)).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight())).
                collect(Collectors.toSet());

        Map<Pair<Station, Station>, Optional<Journey>> results = validateAllHaveAtLeastOneJourney(when,
                combinations, TramTime.of(8, 5));

        // now find longest journey
        Optional<Integer> maxNumberStops = results.values().stream().
                filter(Optional::isPresent).
                map(Optional::get).
                map(journey -> journey.getStages().stream().
                        map(TransportStage::getPassedStops).
                        reduce(Integer::sum)).
                filter(Optional::isPresent).
                map(Optional::get).
                max(Integer::compare);

        Assertions.assertTrue(maxNumberStops.isPresent());
        Assertions.assertEquals(39, maxNumberStops.get().intValue());
    }

    private boolean endsOfLine(Pair<Station, Station> pair) {
        return TramStations.isEndOfLine(pair.getLeft()) && TramStations.isEndOfLine(pair.getRight());
    }

    private boolean interchanes(Pair<Station, Station> pair) {
        return TramStations.isInterchange(pair.getLeft()) && TramStations.isInterchange(pair.getRight());
    }

    private Map<Pair<Station, Station>, Optional<Journey>> validateAllHaveAtLeastOneJourney(
            LocalDate queryDate, Set<Pair<Station, Station>> combinations, TramTime queryTime) {

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(queryDate), queryTime, false,
                3, testConfig.getMaxJourneyDuration());

        final ConcurrentMap<Pair<Station, Station>, Optional<Journey>> results = new ConcurrentHashMap<>(combinations.size());
        combinations.forEach(pair -> results.put(pair, Optional.empty()));

        combinations.parallelStream().
                map(journey -> {
                    try(Transaction txn=database.beginTx()) {
                        return Pair.of(journey,
                            calculator.calculateRoute(txn, journey.getLeft(), journey.getRight(), journeyRequest)
                                    .limit(1).findAny());
                        }
                }).
                forEach(stationsJourneyPair -> results.put(stationsJourneyPair.getLeft(), stationsJourneyPair.getRight()));

        Assertions.assertEquals(combinations.size(), results.size(), "Not enough results");

        // check all results present, collect failures into a list
        List<Pair<Station, Station>> failed = results.
                entrySet().stream().
                filter(journey -> journey.getValue().isEmpty()).
                map(Map.Entry::getKey).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight())).
                collect(Collectors.toList());

        Assertions.assertEquals(
                0L, failed.size(), format("Failed some of %s (finished %s) combinations %s",
                        results.size(), combinations.size(), displayFailed(failed)));

        return results;
    }

    private String displayFailed(List<Pair<Station, Station>> pairs) {
        StringBuilder stringBuilder = new StringBuilder();
        pairs.forEach(pair -> {
            Station dest = pair.getRight();
            stringBuilder.append("[").
                append(pair.getLeft()).
                append(" to ").append(dest.getName()).
                append(" id=").append(dest.getId()).append("] "); });
        return stringBuilder.toString();
    }

}
