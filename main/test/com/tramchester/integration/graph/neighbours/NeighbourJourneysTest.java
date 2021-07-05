package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class NeighbourJourneysTest {
    private StationRepository stationRepository;
    private static RouteCalculatorTestFacade routeCalculator;
    private static TramchesterConfig config;
    private Station shudehillTram;

    private static ComponentContainer componentContainer;
    private Transaction txn;
    private Station shudeHillStop;
    private LocationJourneyPlanner planner;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new NeighboursTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);
        stationRepository = componentContainer.get(StationRepository.class);

        CompositeStationRepository compositeStationRepository = componentContainer.get(CompositeStationRepository.class);

        CompositeStation shudehillCompositeBus = compositeStationRepository.findByName("Shudehill Interchange");

        Optional<Station> maybeStop = shudehillCompositeBus.getContained().stream().findAny();
        maybeStop.ifPresent(stop -> shudeHillStop = stop);

        shudehillTram = compositeStationRepository.getStationById(Shudehill.getId());

        txn = graphDatabase.beginTx();
        routeCalculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
        planner = componentContainer.get(LocationJourneyPlanner.class);
    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldFindMaxRouteHopsBetweenModes() {
        RouteToRouteCosts routeToRoute = componentContainer.get(RouteToRouteCosts.class);
        int hops = routeToRoute.maxRouteHops(shudehillTram, shudeHillStop);
        assertEquals(1, hops);
    }

    @Test
    void shouldHaveIntermodalNeighboursAsInterchanges() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        assertTrue(interchangeRepository.isInterchange(shudehillTram));
        assertTrue(interchangeRepository.isInterchange(shudeHillStop));
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourTramToBus() {
        validateDirectWalk(shudehillTram, shudeHillStop);
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourBusToTram() {
        validateDirectWalk(shudeHillStop, shudehillTram);
    }

    @Test
    void shouldTramNormally() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()),
                TramTime.of(11,53), false, 0, config.getMaxJourneyDuration(), 1);

        Set<Journey> journeys = routeCalculator.calculateRouteAsSet(Bury, Victoria, request);
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(Tram, stage.getMode());
        });
    }

    @Test
    void shouldTramThenWalk() {

        LocationJourneyPlannerTestFacade facade = new LocationJourneyPlannerTestFacade(planner, stationRepository, txn);

        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()),
                TramTime.of(11,53), false, 0, config.getMaxJourneyDuration(), 1);

        Set<Journey> allJourneys = facade.quickestRouteForLocation(Altrincham, TestEnv.nearStPetersSquare, request, 4);
        assertFalse(allJourneys.isEmpty(), "No journeys");

        Set<Journey> maybeTram = allJourneys.stream().
                filter(journey -> journey.getTransportModes().contains(Tram)).
                collect(Collectors.toSet());
        assertFalse(maybeTram.isEmpty(), "No tram " + allJourneys);

        maybeTram.forEach(journey -> {
            final List<TransportStage<?, ?>> stages = journey.getStages();

            TransportStage<?,?> firstStage = stages.get(0);
            assertEquals(Tram, firstStage.getMode(), firstStage.toString());

            TransportStage<?,?> last = stages.get(stages.size()-1);
            assertEquals(TransportMode.Walk, last.getMode(), last.toString());
        });
    }

    private void validateDirectWalk(Station start, Station end) {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()), TramTime.of(11,45),
                        false, 0, config.getMaxJourneyDuration(), 3);

        Set<Journey> allJourneys =  routeCalculator.calculateRouteAsSet(start, end, request);
        assertFalse(allJourneys.isEmpty(), "no journeys");

        Set<Journey> journeys = allJourneys.stream().filter(Journey::isDirect).collect(Collectors.toSet());

        journeys.forEach(journey -> {
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(TransportMode.Connect, stage.getMode());
        });
    }


}
