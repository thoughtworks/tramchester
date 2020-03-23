package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Route;
import com.tramchester.testSupport.RouteCodesForTesting;
import com.tramchester.testSupport.TestConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Stations;
import com.tramchester.repository.TransportData;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class RouteCalculatorTestAllJourneys {

    // TODO this needs to be > longest running test which is far from ideal
    private static final int TXN_TIMEOUT_SECS = 4 * 60;
    private static Dependencies dependencies;
    private static GraphDatabaseService database;

    private static boolean circleCi = TestConfig.isCircleci();

    private RouteCalculator calculator;
    private LocalDate nextTuesday = TestConfig.nextTuesday(0);
    private Transaction tx;
    private Map<Long, Transaction> threadToTxnMap;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        TramchesterConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
        database = dependencies.get(GraphDatabaseService.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        tx = database.beginTx(TXN_TIMEOUT_SECS, TimeUnit.SECONDS);
        calculator = dependencies.get(RouteCalculator.class);
        threadToTxnMap = new HashMap<>();
    }

    @After
    public void afterEachTestRuns() {
        tx.close();
        // can't close transactions on other threads as neo4j uses thread local to cache the transaction
//        threadToTxnMap.values().forEach(Transaction::close);
        threadToTxnMap.clear();
    }

    @Test
    public void shouldFindRouteEachStationToEveryOtherStream() {
        assumeFalse(circleCi);

        TransportData data = dependencies.get(TransportData.class);

        Set<Station> allStations = data.getStations();

        // pairs of stations to check
        Set<Pair<String, Station>> combinations = allStations.stream().map(start -> allStations.stream().
                map(dest -> Pair.of(start, dest))).
                flatMap(Function.identity()).
                filter(pair -> !pair.getRight().getId().equals(pair.getLeft().getId())).
                filter(pair -> !matches(pair, Stations.Interchanges)).
                filter(pair -> !matches(pair, Stations.EndOfTheLine)).
                filter(this::notNewRoute).
                map(pair -> Pair.of(pair.getLeft().getId(), pair.getRight())).
                collect(Collectors.toSet());

        Map<Pair<String, Station>, Optional<Journey>> results = validateAllHaveAtLeastOneJourney(nextTuesday,
                combinations, TramTime.of(6, 5));

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

        assertTrue(maxNumberStops.isPresent());
        assertEquals(39, maxNumberStops.get().intValue());
    }

    //////
    // temporary remove by 22/3/2020
    /////
    private boolean notNewRoute(Pair<Station, Station> pair) {
        if (pair.getLeft().getRoutes().stream().map(Route::getId).anyMatch(RouteCodesForTesting.RouteSeven::contains)) {
            return false;
        }
        return pair.getRight().getRoutes().stream().map(Route::getId).noneMatch(RouteCodesForTesting.RouteSeven::contains);

    }

    private boolean matches(Pair<Station, Station> locationPair, List<Station> locations) {
        return locations.contains(locationPair.getLeft()) && locations.contains(locationPair.getRight());
    }

    private Map<Pair<String, Station>, Optional<Journey>> validateAllHaveAtLeastOneJourney(
            LocalDate queryDate, Set<Pair<String, Station>> combinations, TramTime queryTime) {

//        assertFalse("Need to remove filtering of new routes below",
//                queryDate.isAfter(LocalDate.of(2020,3,21)));

        final ConcurrentMap<Pair<String, Station>, Optional<Journey>> results = new ConcurrentHashMap<>(combinations.size());
        combinations.forEach(pair -> results.put(pair, Optional.empty()));

        combinations.parallelStream().
                map(this::checkForTx).
                map(journey -> Pair.of(journey,
                        calculator.calculateRoute(journey.getLeft(), journey.getRight(), queryTime,
                                new TramServiceDate(queryDate)).findAny())).
                forEach(stationsJourneyPair -> results.put(stationsJourneyPair.getLeft(), stationsJourneyPair.getRight()));

        assertEquals("Not enough results", combinations.size(), results.size());

        // check all results present, collect failures into a list
        List<Pair<String, Station>> failed = results.
                entrySet().stream().
                filter(journey -> journey.getValue().isEmpty()).
                map(Map.Entry::getKey).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight())).
                collect(Collectors.toList());
        List<Journey> retry = failed.stream().map(pair -> calculator.calculateRoute(pair.getLeft(), pair.getRight(), queryTime,
                new TramServiceDate(queryDate))).
                map(Stream::findAny).
                filter(Optional::isPresent).
                map(Optional::get).
                collect(Collectors.toList());
        assertEquals(format("Failed some of %s (finished %s) combinations %s retry is %s", results.size(), combinations.size(), displayFailed(failed), retry),
                0L, failed.size());

        return results;
    }

    private String displayFailed(List<Pair<String, Station>> pairs) {
        StringBuilder stringBuilder = new StringBuilder();
        pairs.forEach(pair -> {
            Station dest = pair.getRight();
            stringBuilder.append("[").
                append(pair.getLeft()).
                append(" to ").append(dest.getName()).
                append(" id=").append(dest.getId()).append("] "); });
        return stringBuilder.toString();
    }

    private <A,B> Pair<A, B>  checkForTx(Pair<A, B> journey) {
        long id = Thread.currentThread().getId();
        if (threadToTxnMap.containsKey(id)) {
            return journey;
        }

        try {
            database.getNodeById(1);
        }
        catch (NotInTransactionException noTxnForThisThread) {
            Transaction txn = database.beginTx(TXN_TIMEOUT_SECS, TimeUnit.SECONDS);
            threadToTxnMap.put(id, txn);
        }
//        catch(Exception uncaught) {
//            throw uncaught;
//        }
        return journey;
    }


}
