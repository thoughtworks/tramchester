package com.tramchester.integration.graph.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.train.IntegrationTrainTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class TrainRouteToRouteCostsTest {
    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routeToRouteCosts;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTrainTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenStockportAndManPicc() {
        Station start = stationRepository.getStationById(TrainStations.Stockport.getId());
        Station end = stationRepository.getStationById(TrainStations.ManchesterPiccadilly.getId());

        assertEquals(0, routeToRouteCosts.getNumberOfChanges(start, end).getMin());
    }

    @Test
    void shouldGetNumberOfRouteHopsBetweenStockportAndKnutsford() {
        Station start = stationRepository.getStationById(TrainStations.LondonEuston.getId());
        Station end = stationRepository.getStationById(TrainStations.Knutsford.getId());

        assertEquals(1, routeToRouteCosts.getNumberOfChanges(start, end).getMin());
    }
}
