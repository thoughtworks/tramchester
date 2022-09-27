package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.DiagramCreator;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.ConfigurableGraphFilter;
import com.tramchester.graph.search.RouteCalculator;
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
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RouteCalculatorSubGraphMediaCityTest {
    private static ComponentContainer componentContainer;
    private static GraphDatabase database;
    private static SubgraphConfig config;

    private RouteCalculatorTestFacade calculator;
    private final TramDate when = TestEnv.testDay();

    private static final List<TramStations> stations = Arrays.asList(
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

        stations.forEach(station -> toConfigure.addStation(station.getId()));
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
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        txn = database.beginTx();
        calculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @Test
    void shouldHaveMediaCityToExchangeSquare() {
        validateAtLeastOneJourney(MediaCityUK, TramStations.Cornbrook, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(MediaCityUK, ExchangeSquare, TramTime.of(9,0), TestEnv.nextSaturday());
        validateAtLeastOneJourney(MediaCityUK, ExchangeSquare, TramTime.of(9,0), TestEnv.nextSunday());
    }

    @DataExpiryCategory
    @Test
    void shouldHaveJourneyFromEveryStationToEveryOtherNDaysAhead() {
        List<String> failures = new LinkedList<>();

        for (TramStations start: stations) {
            for (TramStations destination: stations) {
                if (!start.equals(destination)) {
                    for (int i = 0; i < DAYS_AHEAD; i++) {
                        TramDate day = when.plusDays(i);
                        TramServiceDate serviceDate = new TramServiceDate(day);
                        if (!serviceDate.isChristmasPeriod()) {
                            JourneyRequest journeyRequest =
                                    new JourneyRequest(new TramServiceDate(day), TramTime.of(9, 0), false,
                                            3, maxJourneyDuration, 1, getRequestedModes());
                            Set<Journey> journeys = calculator.calculateRouteAsSet(start, destination, journeyRequest);
                            if (journeys.isEmpty()) {
                                failures.add(day.getDayOfWeek() + ": " + start + "->" + destination);
                            }
                        }
                    }
                }
            }
        }
        Assertions.assertTrue(failures.isEmpty());
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
        Assertions.assertTrue(results.size()>0);
    }

    @Test
    void produceDiagramOfGraphSubset() throws IOException {
        DiagramCreator creator = componentContainer.get(DiagramCreator.class);
        creator.create(Path.of("subgraph_mediacity_trams.dot"), MediaCityUK.fake(), 100, true);
    }

    private static class SubgraphConfig extends IntegrationTramTestConfig {
        public SubgraphConfig() {
            super("sub_mediacity_tramchester.db");
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            List<StationClosure> closed = Collections.emptyList();

            IdSet<Station> additionalInterchanges = AdditionalTramInterchanges.stations();
            additionalInterchanges.add(Cornbrook.getId());
            additionalInterchanges.add(Broadway.getId());

            final Set<TransportMode> groupStationModes = Collections.singleton(TransportMode.Bus);
            TFGMGTFSSourceTestConfig gtfsSourceConfig = new TFGMGTFSSourceTestConfig("data/tram", GTFSTransportationType.tram,
                    TransportMode.Tram, additionalInterchanges, groupStationModes, closed);

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
