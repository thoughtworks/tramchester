package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.*;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class RouteCalculatorSubGraphMediaCityTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();

    private static final List<TramStations> tramStations = Arrays.asList(
            ExchangeSquare,
            StPetersSquare,
            Deansgate,
            Cornbrook,
            Pomona,
            ExchangeQuay,
            SalfordQuay,
            Anchorage,
            HarbourCity,
            MediaCityUK,
            TraffordBar);
    private Transaction txn;

    private Duration maxJourneyDuration;
    private RouteCalculationCombinations combinations;
    private StationRepository stationRepository;
    private ClosedStationsRepository closedStationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
//        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorSubGraphMediaCityTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());

        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter toConfigure, RouteRepository routeRepository) {
        tramStations.forEach(station -> toConfigure.addStation(station.getId()));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
//        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        stationRepository = componentContainer.get(StationRepository.class);
        txn = database.beginTx();
        combinations = new RouteCalculationCombinations(componentContainer);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
        closedStationRepository = componentContainer.get(ClosedStationsRepository.class);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveMediaCityToExchangeSquare() {
        validateAtLeastOneJourney(MediaCityUK, Cornbrook, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(MediaCityUK, ExchangeSquare, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(MediaCityUK, ExchangeSquare, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @Test
    void shouldHaveJourneyFromEveryStationToEveryOtherNDaysAhead() {

        List<Pair<TramDate, List<StationIdPair>>> failed = TestEnv.getUpcomingDates().
                map(date -> new JourneyRequest(date, TramTime.of(9, 0), false,
                        3, maxJourneyDuration, 1, getRequestedModes())).
                map(journeyRequest -> Pair.of(journeyRequest.getDate(), getFailedPairedFor(journeyRequest))).
                filter(pair -> !pair.getRight().isEmpty()).
                collect(Collectors.toList());

        assertTrue(failed.isEmpty(), failed.toString());
    }

    @Test
    void shouldHaveExpectedInterchanges() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        IdSet<Station> interchangesIds = interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        assertTrue(interchangesIds.contains(Cornbrook.getId()));
        assertTrue(interchangesIds.contains(Pomona.getId()));
        assertTrue(interchangesIds.contains(Deansgate.getId()));

    }

    @Test
    void shouldHaveSalfordQuayToStPeters() {
        final TramTime time = TramTime.of(8, 5);

        // 2 -> 4
        int maxChanges = 4;
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, getRequestedModes());

        Set<Journey> results = calculator.calculateRouteAsSet(SalfordQuay.getId(), StPetersSquare.getId(), journeyRequest);

        assertFalse(results.isEmpty());
    }

    @Test
    void shouldHaveExpectedCostsToInterchanges() {
        RouteInterchangeRepository routeInterchangeRepository = componentContainer.get(RouteInterchangeRepository.class);

        Station salfordQuay = SalfordQuay.from(stationRepository);

        Set<Route> pickups = salfordQuay.getPickupRoutes();

        long routesWithInterchanges = pickups.stream().map(route -> stationRepository.getRouteStation(salfordQuay, route)).
                map(routeInterchangeRepository::costToInterchange).
                filter(duration -> !duration.isNegative()).count();

        assertNotEquals(0, routesWithInterchanges);
    }

    @Test
    void shouldHaveExpectedRouteOverlaps() {
        Station salfordQuay = SalfordQuay.from(stationRepository);
        Station stPetersSquare = StPetersSquare.from(stationRepository);

        Station cornbrook = Cornbrook.from(stationRepository);

        Set<Route> fromSalford = salfordQuay.getPickupRoutes();
        Set<Route> dropOffAtCornbrook = cornbrook.getDropoffRoutes();

        SetUtils.SetView<Route> salfordToCornbrook = SetUtils.union(fromSalford, dropOffAtCornbrook);

        assertFalse(salfordToCornbrook.isEmpty());

        Set<Route> salfordToCornbrookOnDate = salfordToCornbrook.stream().filter(route -> route.isAvailableOn(when)).collect(Collectors.toSet());

        assertFalse(salfordToCornbrookOnDate.isEmpty());

        Set<Route> fromCornbrook = cornbrook.getPickupRoutes();
        Set<Route> dropOffAtStPetersSquare = stPetersSquare.getDropoffRoutes();

        SetUtils.SetView<Route> cornbrookToStPetersSquare = SetUtils.union(fromCornbrook, dropOffAtStPetersSquare);

        assertFalse(cornbrookToStPetersSquare.isEmpty());

        Set<Route> cornbrookToStPetersSquareOnDate = cornbrookToStPetersSquare.stream().filter(route -> route.isAvailableOn(when)).collect(Collectors.toSet());

        assertFalse(cornbrookToStPetersSquareOnDate.isEmpty());

    }

    @Test
    void shouldHaveExpectedRouteConnections() {
        Station salfordQuay = SalfordQuay.from(stationRepository);
        Station stPetersSquare = StPetersSquare.from(stationRepository);

        RouteToRouteCosts routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);

        TimeRange timeRange = TimeRange.of(TramTime.of(8,5), TramTime.of(8,30));
        NumberOfChanges results = routeToRouteCosts.getNumberOfChanges(salfordQuay, stPetersSquare, getRequestedModes(), when, timeRange);

        assertEquals(results.getMin(), 0);
    }

    @Test
    void shouldHaveJoruneyFromEveryStationToEveryOther() {

        final TramTime time = TramTime.of(8, 5);

        // 2 -> 4
        int maxChanges = 4;
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, getRequestedModes());

        // pairs of stations to check
        List<StationIdPair> results = getFailedPairedFor(journeyRequest);
        assertTrue(results.isEmpty(), results + " failed for " + journeyRequest);
    }

    private List<StationIdPair> getFailedPairedFor(JourneyRequest journeyRequest) {
        TramDate date = journeyRequest.getDate();
        Set<Station> stations = tramStations.stream().
                map(tramStations -> tramStations.from(stationRepository)).
                filter(station -> !closedStationRepository.isClosed(station, date)).
                collect(Collectors.toSet());

        Set<StationIdPair> stationIdPairs = stations.stream().flatMap(start -> stations.stream().
                filter(dest -> !dest.getId().equals(start.getId())).
                filter(dest -> !combinations.betweenInterchanges(start, dest)).
                map(dest -> StationIdPair.of(start, dest))).
                filter(pair -> !pair.same()).
                collect(Collectors.toSet());

        Map<StationIdPair, RouteCalculationCombinations.JourneyOrNot> results = combinations.getJourneysFor(stationIdPairs, journeyRequest);

        return results.entrySet().stream().
                filter(entry -> entry.getValue().missing()).
                map(Map.Entry::getKey).
                collect(Collectors.toList());

    }

    private Set<TransportMode> getRequestedModes() {
        return TransportMode.TramsOnly;
    }

    @Test
    void reproduceMediaCityIssue() {
        validateAtLeastOneJourney(ExchangeSquare, MediaCityUK, TramTime.of(12,0), when);
    }

    @Test
    void reproduceMediaCityIssueSaturdays() {
        validateAtLeastOneJourney(ExchangeSquare, MediaCityUK, TramTime.of(9,0), TestEnv.nextSaturday());
    }

    @Test
    void shouldHaveSimpleJourney() {
        JourneyRequest journeyRequest = new JourneyRequest(when, TramTime.of(12, 0), false, 3,
                maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(TramStations.Pomona, MediaCityUK, journeyRequest);
        assertTrue(results.size()>0);
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_mediacity_trams.dot"), MediaCityUK.fake(), 100, true);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("sub_mediacity_tramchester.db", IntegrationTramTestConfig.CurrentClosures);
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {

            IdSet<Station> additionalInterchanges = AdditionalTramInterchanges.stations();
            additionalInterchanges.add(Cornbrook.getId());
            additionalInterchanges.add(Pomona.getId());
            additionalInterchanges.add(Deansgate.getId());

            final Set<TransportMode> groupStationModes = Collections.emptySet(); //Collections.singleton(TransportMode.Bus);

            List<StationClosures> currentClosures = IntegrationTramTestConfig.CurrentClosures;

            TFGMGTFSSourceTestConfig gtfsSourceConfig = new TFGMGTFSSourceTestConfig("data/tram", GTFSTransportationType.tram,
                    Tram, additionalInterchanges, groupStationModes, currentClosures,
                    Duration.ofMinutes(45));

            return Collections.singletonList(gtfsSourceConfig);
        }
    }

    private void validateAtLeastOneJourney(TramStations start, TramStations dest, TramTime time, TramDate date) {
        JourneyRequest journeyRequest = new JourneyRequest(date, time, false, 5,
                maxJourneyDuration, 1, getRequestedModes());
        Set<Journey> results = calculator.calculateRouteAsSet(start, dest, journeyRequest);
        assertFalse(results.isEmpty());
    }
}
