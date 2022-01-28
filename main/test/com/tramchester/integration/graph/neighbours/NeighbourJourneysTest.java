package com.tramchester.integration.graph.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.integration.testSupport.RouteCalculatorTestFacade;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.LocationJourneyPlanner;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Transaction;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.BusStations.KnutsfordStationStand3;
import static com.tramchester.testSupport.reference.BusStations.StockportNewbridgeLane;
import static com.tramchester.testSupport.reference.KnownLocations.nearStPetersSquare;
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
    private Station shudehillBusStop;
    private LocationJourneyPlanner planner;
    private RouteToRouteCosts routeToRouteCosts;

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

        StationGroupsRepository stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
        StationRepository stationRepository = componentContainer.get(StationRepository.class);
        StationGroup shudehillCompositeBus = stationGroupsRepository.findByName("Shudehill Interchange");

        Optional<Station> maybeStop = shudehillCompositeBus.getContained().stream().findAny();
        maybeStop.ifPresent(stop -> shudehillBusStop = stop);

        shudehillTram = stationRepository.getStationById(Shudehill.getId());

        txn = graphDatabase.beginTx();
        routeCalculator = new RouteCalculatorTestFacade(componentContainer.get(RouteCalculator.class), stationRepository, txn);
        planner = componentContainer.get(LocationJourneyPlanner.class);

        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);

    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
    }

    @Test
    void shouldHaveTestStations() {
        assertNotNull(shudehillBusStop);
        assertNotNull(shudehillTram);
    }

    @Test
    void shouldHaveCorrectRouteToRouteHopsWhenNeighbours() {

        NumberOfChanges busToTramHops = routeToRouteCosts.getNumberOfChanges(shudehillBusStop, shudehillTram);
        assertEquals(1, busToTramHops.getMin());
        assertEquals(1, busToTramHops.getMax());

        NumberOfChanges tramToBusHops = routeToRouteCosts.getNumberOfChanges(shudehillTram, shudehillBusStop);
        assertEquals(1, tramToBusHops.getMin());
        assertEquals(1, tramToBusHops.getMax());
    }

    @Test
    void shouldHaveCorrectRouteToRouteHopsWhenNeighboursSets() {

        LocationSet trams = new LocationSet(Arrays.asList(Altrincham.from(stationRepository), HarbourCity.from(stationRepository)));

        LocationSet buses = new LocationSet(Arrays.asList(KnutsfordStationStand3.from(stationRepository), StockportNewbridgeLane.from(stationRepository)));

        NumberOfChanges busToTramHops = routeToRouteCosts.getNumberOfChanges(buses, trams);
        assertEquals(1, busToTramHops.getMin());
        assertEquals(2, busToTramHops.getMax());

        NumberOfChanges tramToBusHops = routeToRouteCosts.getNumberOfChanges(trams, buses);
        assertEquals(2, tramToBusHops.getMin());
        assertEquals(2, tramToBusHops.getMax());

        // now add neighbouring stops
        trams.add(shudehillTram);
        buses.add(shudehillBusStop);

        busToTramHops = routeToRouteCosts.getNumberOfChanges(buses, trams);
        assertEquals(0, busToTramHops.getMin());
        assertEquals(2, busToTramHops.getMax());

        tramToBusHops = routeToRouteCosts.getNumberOfChanges(trams, buses);
        assertEquals(0, tramToBusHops.getMin());
        assertEquals(2, tramToBusHops.getMax());
    }

    @Test
    void shouldFindMaxRouteHopsBetweenModes() {
        NumberOfChanges hops = routeToRouteCosts.getNumberOfChanges(shudehillTram, shudehillBusStop);
        assertEquals(1, hops.getMax());
    }

    @Test
    void shouldHaveIntermodalNeighboursAsInterchanges() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        assertTrue(interchangeRepository.isInterchange(shudehillTram));
        assertTrue(interchangeRepository.isInterchange(shudehillBusStop));
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourTramToBus() {
        validateDirectWalk(shudehillTram, shudehillBusStop);
    }

    @Test
    void shouldDirectWalkIfStationIsNeighbourBusToTram() {
        validateDirectWalk(shudehillBusStop, shudehillTram);
    }

    @Test
    void shouldTramNormally() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()),
                TramTime.of(11,53), false, 0, config.getMaxJourneyDuration(), 1);

        Set<Journey> journeys = routeCalculator.calculateRouteAsSet(Bury.from(stationRepository), Victoria.from(stationRepository), request);
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

        Set<Journey> allJourneys = facade.quickestRouteForLocation(Altrincham.from(stationRepository), nearStPetersSquare, request, 4);
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
