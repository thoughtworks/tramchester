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
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.RouteCalculator;
import com.tramchester.integration.graph.testSupport.RouteCalculatorTestFacade;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
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
import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    }

    @AfterEach
    void onceAfterEachTestHasRun() {
        txn.close();
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
                TramTime.of(11,53), false, 2, config.getMaxJourneyDuration(), 1);

        Set<Journey> journeys = routeCalculator.calculateRouteAsSet(Bury, Shudehill, request);

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(Tram, stage.getMode());
        });
    }

    @Test
    void shouldTramThenWalk() {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()),
                TramTime.of(11,53), false, 0, config.getMaxJourneyDuration(), 1);

        Station startStation = stationRepository.getStationById(Bury.getId());
        Set<Journey> allJourneys = routeCalculator.calculateRouteAsSet(startStation, shudeHillStop, request);

        Set<Journey> maybeTram = allJourneys.stream().filter(journey -> journey.getStages().size()<=2).collect(Collectors.toSet());
        assertFalse(maybeTram.isEmpty());

        maybeTram.forEach(journey -> {
            final List<TransportStage<?, ?>> stages = journey.getStages();
            assertEquals(2, stages.size());
            TransportStage<?,?> first = stages.get(0);
            assertEquals(Tram, first.getMode());
            TransportStage<?,?> second = stages.get(1);
            assertEquals(TransportMode.Connect, second.getMode());
        });
    }

    private void validateDirectWalk(Station start, Station end) {

        JourneyRequest request = new JourneyRequest(new TramServiceDate(TestEnv.testDay()), TramTime.of(11,45),
                        false, 0, config.getMaxJourneyDuration(), 1);

        Set<Journey> journeys =  routeCalculator.calculateRouteAsSet(start, end, request);

        assertFalse(journeys.isEmpty());
        journeys.forEach(journey -> {
            assertEquals(1, journey.getStages().size(), journey.toString());
            TransportStage<?,?> stage = journey.getStages().get(0);
            assertEquals(TransportMode.Connect, stage.getMode());
        });
    }


}
