package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.tfgm.TFGMGTFSSourceTestConfig;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import com.tramchester.testSupport.testTags.VictoriaNov2022;
import com.tramchester.testSupport.testTags.WorkaroundsNov2022;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.IntegrationTestConfig.VictoriaClosureDate;
import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        config = new SubgraphConfig();
        TestEnv.deleteDBIfPresent(config);

        componentContainer = new ComponentsBuilder().
                configureGraphFilter(RouteCalculatorSubGraphMediaCityTest::configureFilter).
                create(config, TestEnv.NoopRegisterMetrics());

        componentContainer.initialise();

        database = componentContainer.get(GraphDatabase.class);
    }

    private static void configureFilter(ConfigurableGraphFilter toConfigure, RouteRepository routeRepository) {
        TramRouteHelper tramRouteHelper = new TramRouteHelper(routeRepository);

        tramStations.forEach(station -> toConfigure.addStation(station.getId()));
        toConfigure.addRoutes(tramRouteHelper.getId(AshtonUnderLyneManchesterEccles));
        toConfigure.addRoutes(tramRouteHelper.getId(RochdaleShawandCromptonManchesterEastDidisbury));
        toConfigure.addRoutes(tramRouteHelper.getId(EcclesManchesterAshtonUnderLyne));
        toConfigure.addRoutes(tramRouteHelper.getId(EastDidisburyManchesterShawandCromptonRochdale));
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        maxJourneyDuration = Duration.ofMinutes(config.getMaxJourneyDuration());
        stationRepository = componentContainer.get(StationRepository.class);
        txn = database.beginTx();
        combinations = new RouteCalculationCombinations(componentContainer);
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @WorkaroundsNov2022
    @Test
    void shouldHaveMediaCityToExchangeSquare() {
        validateAtLeastOneJourney(MediaCityUK, TramStations.Cornbrook, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(MediaCityUK, ExchangeSquare, TramTime.of(9,0), TestEnv.nextSaturday());

        // TODO
        //validateAtLeastOneJourney(MediaCityUK, ExchangeSquare, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @VictoriaNov2022
    @DataExpiryCategory
    @Test
    void shouldHaveJourneyFromEveryStationToEveryOtherNDaysAhead() {

        for (int i = 0; i < DAYS_AHEAD; i++) {
            TramDate day = when.plusDays(i);
            TramServiceDate serviceDate = new TramServiceDate(day);
            if (!serviceDate.isChristmasPeriod() && !VictoriaClosureDate.equals(day)) {
                JourneyRequest journeyRequest =
                        new JourneyRequest(new TramServiceDate(day), TramTime.of(9, 0), false,
                                3, maxJourneyDuration, 1, getRequestedModes());
                checkAllStations(journeyRequest);
            }
        }

    }

    @VictoriaNov2022
    @Test
    void shouldHaveJoruneyFromEveryStationToEveryOther() {

        final TramTime time = TramTime.of(8, 5);

        // 2 -> 4
        int maxChanges = 4;
        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, maxChanges,
                Duration.ofMinutes(config.getMaxJourneyDuration()), 1, Collections.emptySet());

        // pairs of stations to check
        checkAllStations(journeyRequest);
    }

    private void checkAllStations(JourneyRequest journeyRequest) {
        Set<Station> stations = tramStations.stream().map(tramStations -> tramStations.from(stationRepository)).collect(Collectors.toSet());
        Set<StationIdPair> stationIdPairs = stations.stream().flatMap(start -> stations.stream().
                        filter(dest -> !combinations.betweenInterchanges(start, dest)).
                        map(dest -> StationIdPair.of(start, dest))).
                filter(pair -> !pair.same()).
                filter(pair -> workaroundExchangeSquareDataIssue(pair, journeyRequest)).
                collect(Collectors.toSet());

        combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest);
    }

    private boolean workaroundExchangeSquareDataIssue(StationIdPair pair, JourneyRequest journeyRequest) {
        final TramDate date = journeyRequest.getDate().getDate();
        final IdFor<Station> exchangeSquareId = ExchangeSquare.getId();

        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return (!pair.getBeginId().equals(exchangeSquareId)) && (!pair.getEndId().equals(exchangeSquareId));
        }
        return true;
    }

    private Set<TransportMode> getRequestedModes() {
        return Collections.emptySet();
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
        JourneyRequest journeyRequest = new JourneyRequest(new TramServiceDate(when), TramTime.of(12, 0), false, 3,
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
            super("sub_mediacity_tramchester.db", Collections.emptyList());
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            List<StationClosures> closed = Collections.emptyList();

            IdSet<Station> additionalInterchanges = AdditionalTramInterchanges.stations();
            additionalInterchanges.add(Cornbrook.getId());
            additionalInterchanges.add(Broadway.getId());

            final Set<TransportMode> groupStationModes = Collections.singleton(TransportMode.Bus);
            TFGMGTFSSourceTestConfig gtfsSourceConfig = new TFGMGTFSSourceTestConfig("data/tram", GTFSTransportationType.tram,
                    TransportMode.Tram, additionalInterchanges, groupStationModes, closed, Duration.ofMinutes(45));

            return Collections.singletonList(gtfsSourceConfig);
        }
    }

    private void validateAtLeastOneJourney(TramStations start, TramStations dest, TramTime time, TramDate date) {
        JourneyRequest journeyRequest = new JourneyRequest(date, time, false, 5,
                maxJourneyDuration, 1, Collections.emptySet());
        Set<Journey> results = calculator.calculateRouteAsSet(start, dest, journeyRequest);
        assertFalse(results.isEmpty());
    }
}
