package com.tramchester.integration.repository.buses;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.BusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.repository.common.InterchangeRepositoryTestSupport.RoutesWithInterchanges;
import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;


@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class InterchangeRepositoryBusTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository interchangeRepository;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder<>().create(new IntegrationBusTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
    }

    @BusTest
    @Test
    void shouldFindBusInterchanges() {
        IdSet<Station> interchanges = interchangeRepository.getInterchangesFor(Bus);
        assertFalse(interchanges.isEmpty());

        assertTrue(interchanges.contains(AltrinchamInterchange.getId()));
        assertTrue(interchanges.contains(StockportBusStation.getId()));

        assertFalse(interchanges.contains(StockportAtAldi.getId()));
        assertFalse(interchanges.contains(StockportNewbridgeLane.getId()));
    }

    @BusTest
    @Test
    void shouldHaveReachableInterchangeForEveryRoute() {
        Set<Route> routesWithInterchanges = RoutesWithInterchanges(interchangeRepository, stationRepository, Bus);
        Set<Route> all = routeRepository.getRoutes();

        // Note works for 2 links, not for 3 links
        // 2 = 1821 interchanges
        // 3 = 234  interchanges, but unreachable interchanges
        assertEquals(all, routesWithInterchanges);
    }



}
