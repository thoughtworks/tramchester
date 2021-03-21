package com.tramchester.integration.graph.trains;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.RouteReachable;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.List;

import static com.tramchester.testSupport.reference.TrainStations.Knutsford;
import static com.tramchester.testSupport.reference.TrainStations.Mobberley;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TrainRouteReachableTest {
    private static ComponentContainer componentContainer;

    private RouteReachable reachable;
    private StationRepository stationRepository;
    private Route route;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTrainTestConfig();
        componentContainer = new ComponentsBuilder<>().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        reachable = componentContainer.get(RouteReachable.class);

        route = routeRepository.findRouteByShortName(StringIdFor.createId("NT"), "NT:MAN->CTR");
    }

    @Test
    void shouldFindRouteCorrectly() {
        assertNotNull(route);
    }

    @Test
    void shouldHaveCorrectReachability() {
        IdSet<Station> fromMobberley = reachable.getReachableStations(createRouteStation(route, Mobberley));
        assertTrue(fromMobberley.contains(Knutsford.getId()));

        IdSet<Station> fromKnutsford = reachable.getReachableStations(createRouteStation(route, Knutsford));
        assertFalse(fromKnutsford.contains(Mobberley.getId())); // wrong direction
    }

    @Test
    void shouldHaveInterchangeReachable() {
        assertTrue(reachable.isInterchangeReachable(createRouteStation(route, Knutsford)));
        assertTrue(reachable.isInterchangeReachable(createRouteStation(route, Mobberley)));
    }

    private RouteStation createRouteStation(Route route, TrainStations station) {
        return new RouteStation(stationRepository.getStationById(station.getId()), route);
    }
}
