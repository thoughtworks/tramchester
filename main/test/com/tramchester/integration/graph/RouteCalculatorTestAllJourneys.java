package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

class RouteCalculatorTestAllJourneys {

    // TODO this needs to be > longest running test which is far from ideal
    private static final int TXN_TIMEOUT_SECS = 4 * 60;
    private static Dependencies dependencies;
    private static GraphDatabase database;

    private static final boolean circleCi = TestEnv.isCircleci();

    private RouteCalculator calculator;
    private final LocalDate nextTuesday = TestEnv.nextTuesday(0);

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabase.class);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
    }

    @Test
    void shouldFindRouteEachStationToEveryOtherStream() {
        Assumptions.assumeFalse(circleCi);

        TransportData data = dependencies.get(TransportData.class);

        Set<Station> allStations = data.getStations();

        // pairs of stations to check
        Set<Pair<Station, Station>> combinations = allStations.stream().flatMap(start -> allStations.stream().
                map(dest -> Pair.of(start, dest))).
                filter(pair -> !pair.getRight().getId().equals(pair.getLeft().getId())).
                filter(pair -> !matches(pair, Stations.Interchanges)).
                filter(pair -> !matches(pair, Stations.EndOfTheLine)).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight())).
                collect(Collectors.toSet());

        Map<Pair<Station, Station>, Optional<Journey>> results = validateAllHaveAtLeastOneJourney(nextTuesday,
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

    private boolean matches(Pair<Station, Station> locationPair, List<Station> locations) {
        return locations.contains(locationPair.getLeft()) && locations.contains(locationPair.getRight());
    }

    private Map<Pair<Station, Station>, Optional<Journey>> validateAllHaveAtLeastOneJourney(
            LocalDate queryDate, Set<Pair<Station, Station>> combinations, TramTime queryTime) {

        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(queryDate), queryTime, false);

        final ConcurrentMap<Pair<Station, Station>, Optional<Journey>> results = new ConcurrentHashMap<>(combinations.size());
        combinations.forEach(pair -> results.put(pair, Optional.empty()));

        combinations.parallelStream().
                map(journey -> {
                    try(Transaction txn=database.beginTx()) {
                        return Pair.of(journey,
                            calculator.calculateRoute(txn, journey.getLeft(), journey.getRight(), journeyRequest).findAny());
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

        // TODO Diagnose threading issue so this can be removed
        List<Journey> retry;
        try(Transaction txn = database.beginTx(TXN_TIMEOUT_SECS, TimeUnit.SECONDS)) {
            retry = failed.stream().map(pair -> calculator.calculateRoute(txn, pair.getLeft(), pair.getRight(), journeyRequest)).
                    map(Stream::findAny).
                    filter(Optional::isPresent).
                    map(Optional::get).
                    collect(Collectors.toList());
        }

        Assertions.assertEquals(
                0L, failed.size(), format("Failed some of %s (finished %s) combinations %s retry is %s",
                        results.size(), combinations.size(), displayFailed(failed), retry));

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
