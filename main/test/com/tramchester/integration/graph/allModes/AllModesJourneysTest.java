package com.tramchester.integration.graph.allModes;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.testSupport.AllModesTestConfig;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TrainStations.ManchesterPiccadilly;
import static com.tramchester.testSupport.reference.TrainStations.Stockport;
import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.Victoria;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class AllModesJourneysTest {
    private static RouteCalculatorTestFacade routeCalculator;
    private static TramchesterConfig config;

    private static ComponentContainer componentContainer;
    private Transaction txn;
    private CompositeStationRepository compositeStationRepository;
    private int maxJourneyDuration;
    private LocalDate when;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new AllModesTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        maxJourneyDuration = config.getMaxJourneyDuration();
        when = TestEnv.testDay();

        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);

        txn = graphDatabase.beginTx();
        routeCalculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldHaveBuryVictoriaTramJourney() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when),
                TramTime.of(11,53), false, 0, maxJourneyDuration, 1);

        Set<Journey> journeys = routeCalculator.calculateRouteAsSet(Bury, Victoria, request);
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(Tram, stage.getMode());
        });
    }

    @Test
    void shouldHaveStockToAltyBusJourney() {

        CompositeStation stockport = compositeStationRepository.findByName("Stockport Bus Station");
        CompositeStation alty = compositeStationRepository.findByName("Altrincham Interchange");

        TramTime travelTime = TramTime.of(9, 0);

        JourneyRequest requestA = new JourneyRequest(new TramServiceDate(when), travelTime, false, 0,
                maxJourneyDuration, 3);
        Set<Journey> journeys = routeCalculator.calculateRouteAsSet(stockport, alty, requestA);
        assertFalse(journeys.isEmpty());
    }

    @Test
    void shouldHaveStockportToManPicc() {
        TramTime travelTime = TramTime.of(8, 0);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(when), travelTime, false, 1,
                30, 1);

        Set<Journey> journeys = routeCalculator.calculateRouteAsSet(Stockport, ManchesterPiccadilly, request);
        assertFalse(journeys.isEmpty());

        // At least one direct
        List<Journey> direct = journeys.stream().filter(journey -> journey.getStages().size() == 1).collect(Collectors.toList());
        assertFalse(direct.isEmpty(), "No direct from " + Stockport + " to " + ManchesterPiccadilly);
    }

}
