package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.InvalidDurationException;
import com.tramchester.domain.dates.TramServiceDate;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCostCalculator;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.Summer2022;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Transaction;

import java.time.Duration;

import static com.tramchester.testSupport.TestEnv.assertMinutesEquals;
import static com.tramchester.testSupport.reference.TramStations.*;

@DataUpdateTest
class RouteCostCalculatorTest {

    private static ComponentContainer componentContainer;

    private RouteCostCalculator routeCostCalculator;
    private StationRepository stationRepository;
    private Transaction txn;
    private final TramServiceDate date = new TramServiceDate(TestEnv.testDay());
    private Station altrincham;
    private Station mediaCity;
    private Station airport;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeCostCalculator = componentContainer.get(RouteCostCalculator.class);
        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase database = componentContainer.get(GraphDatabase.class);
        txn = database.beginTx();

        altrincham = Altrincham.from(stationRepository);
        mediaCity = MediaCityUK.from(stationRepository);
        airport = ManAirport.from(stationRepository);
    }

    @AfterEach
    void afterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsAltyNavRoad() throws InvalidDurationException {
        assertMinutesEquals(3, routeCostCalculator.getAverageCostBetween(txn, NavigationRoad.from(stationRepository), altrincham, date));
        assertMinutesEquals(4, routeCostCalculator.getAverageCostBetween(txn, altrincham, NavigationRoad.from(stationRepository), date));
    }

    @Test
    void shouldComputeCostsForMediaCityAshton() throws InvalidDurationException {
        assertMinutesEquals(55, routeCostCalculator.getAverageCostBetween(txn, mediaCity, Ashton.from(stationRepository), date));
        assertMinutesEquals(52, routeCostCalculator.getAverageCostBetween(txn,  Ashton.from(stationRepository), mediaCity, date));
    }

    @Summer2022
    @Test
    void shouldComputeSimpleCostBetweenStationsAltyBury() throws InvalidDurationException {
        // changes regularly with timetable updates

        final Station bury = Bury.from(stationRepository);
        final Duration buryToAlty = routeCostCalculator.getAverageCostBetween(txn, bury, altrincham, date);
        final Duration altyToBury = routeCostCalculator.getAverageCostBetween(txn, altrincham, bury, date);

        // summer 2022 +14 and +19
        assertMinutesEquals(63+14, buryToAlty);
        assertMinutesEquals(63+19, altyToBury);
    }

    @Test
    void shouldComputeSimpleCostBetweenStationsMediaCityAirport() throws InvalidDurationException {
        assertMinutesEquals(58, routeCostCalculator.getAverageCostBetween(txn, mediaCity, airport, date));
        assertMinutesEquals(58, routeCostCalculator.getAverageCostBetween(txn, airport, mediaCity, date));
    }

    @Test
    void shouldComputeSimpleMaxCostBetweenStationsMediaCityAirport() throws InvalidDurationException {
        assertMinutesEquals(58, routeCostCalculator.getMaxCostBetween(txn, mediaCity, airport, date));
        assertMinutesEquals(58, routeCostCalculator.getMaxCostBetween(txn, airport, mediaCity, date));
    }

}
