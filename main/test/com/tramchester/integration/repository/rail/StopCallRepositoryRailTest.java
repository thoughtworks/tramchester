package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StopCallRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StopCallRepositoryRailTest {
    private static ComponentContainer componentContainer;

    private StopCallRepository stopCallRepository;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationRailTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTest() {
        stopCallRepository = componentContainer.get(StopCallRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldReproIssueWithCrossingMidnight() {
        Route route = routeRepository.getRouteById(StringIdFor.createId("SR:PERTH=>EDINBUR:5"));

        Station inverkeithing = stationRepository.getStationById(StringIdFor.createId("IVRKTHG"));
        Station haymarket = stationRepository.getStationById(StringIdFor.createId("HAYMRKT"));

        StopCallRepository.Costs costs = stopCallRepository.getCostsBetween(route, inverkeithing, haymarket);

        assertFalse(costs.isEmpty());

        // was getting costs > 23 hours due to crossing midnight
        assertTrue(costs.average()< 20, costs.toString());

    }
}
