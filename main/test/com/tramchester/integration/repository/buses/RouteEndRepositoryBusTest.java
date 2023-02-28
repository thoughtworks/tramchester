package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.RouteEndRepository;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

@BusTest
class RouteEndRepositoryBusTest {
    private static ComponentContainer componentContainer;
    private RouteEndRepository routeEndRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        routeEndRepository = componentContainer.get(RouteEndRepository.class);
    }

    @Test
    void shouldFindEndsOfRoutesForBuses() {
        IdSet<Station> result = routeEndRepository.getStations(TransportMode.Bus);

        assertTrue(result.contains(Station.createId("2500ACC0009"))); // Accrington, Bus Station (Stand 9)
        assertFalse(result.contains(Station.createId("2500LAA15791"))); // Accrington, opp Infant Street

        // TODO More here useful
    }

    @Test
    void shouldHaveExpectedNumberOfEndOfRouteStations() {
        IdSet<Station> result = routeEndRepository.getStations(TransportMode.Bus);

        assertEquals(1066, result.size());
    }
}
